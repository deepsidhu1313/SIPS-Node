/*
 * Copyright (C) 2026 Navdeep Singh Sidhu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.tools.JobPaths;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.job.Job;
import in.co.s13.sips.lib.job.JobManifest;
import in.co.s13.sips.lib.job.JobRunner;
import in.co.s13.sips.lib.job.JobSequencer;
import in.co.s13.sips.lib.job.StageRunner;
import in.co.s13.sips.scheduler.LoadScheduler;
import in.co.s13.sips.schedulers.Chunk;
import in.co.s13.sips.schedulers.Factoring;
import in.co.s13.sips.schedulers.GA;
import in.co.s13.sips.schedulers.GATDS;
import in.co.s13.sips.schedulers.GSS;
import in.co.s13.sips.schedulers.QSS;
import in.co.s13.sips.schedulers.TSS;
import java.time.Duration;
import org.json.JSONObject;

/**
 * Runs a manifest that declares {@code STAGES} as a pipeline instead of a single
 * loop.
 *
 * <p>Everything interesting is elsewhere and on purpose: {@link JobManifest}
 * reads the graph, {@link JobRunner} decides what runs when, and
 * {@link DistributedStageRunner} turns a stage into chunks on nodes. What is
 * left here is the wiring — which scheduler, and where the result goes.
 *
 * <p>The existing single-loop {@link Job} path is untouched. A manifest without
 * {@code STAGES} — which is almost every manifest ever written — still goes
 * through it, and {@link JobManifest#hasStages} is how the two are told apart.
 */
public class StagedJob implements Runnable {

    /** How often the pipeline checks whether a stage has finished. */
    static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private final String jobToken;
    private JobSequencer sequencer;

    public StagedJob(String jobToken) {
        if (jobToken == null || jobToken.isBlank()) {
            throw new IllegalArgumentException("jobToken must not be blank");
        }
        this.jobToken = jobToken.trim();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("StagedJob-" + jobToken);
        JSONObject manifest = Util.readJSONFile(JobPaths.manifest(jobToken));
        JSONObject schedulerJson = manifest.getJSONObject("SCHEDULER", new JSONObject());
        String schedulerName = schedulerJson.getString("Name", "");

        LoadScheduler scheduler = load(schedulerName);
        if (scheduler == null) {
            setStatus("No usable scheduler in the manifest: '" + schedulerName + "'");
            return;
        }

        Job job;
        try {
            job = JobManifest.read(jobToken, manifest);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // A malformed pipeline is a mistake in the file. Saying so beats
            // starting the stages that happen to parse.
            setStatus("Bad pipeline: " + ex.getMessage());
            return;
        }

        StageRunner stageRunner = new DistributedStageRunner(jobToken, scheduler,
                schedulerName, schedulerJson);
        JobRunner runner = new JobRunner(job, stageRunner);
        this.sequencer = runner.sequencer();

        try {
            runner.run(POLL_INTERVAL);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            setStatus("Interrupted: " + sequencer.progress());
            return;
        }

        setStatus(sequencer.isSuccessful() ? "Finished" : failureSummary());
        Util.appendToJobLog(GlobalValues.LOG_LEVEL.OUTPUT, sequencer.progress());
    }

    /** Names the stage that failed and why, so nobody has to read a log to find out. */
    private String failureSummary() {
        StringBuilder summary = new StringBuilder("Failed:");
        sequencer.stagesIn(JobSequencer.State.FAILED).forEach(stage ->
                summary.append(" '").append(stage.name()).append("' ")
                        .append(sequencer.failureReason(stage).orElse("for an unknown reason")));
        int skipped = sequencer.stagesIn(JobSequencer.State.SKIPPED).size();
        if (skipped > 0) {
            summary.append("; ").append(skipped).append(" stage(s) never ran");
        }
        return summary.toString();
    }

    /**
     * The scheduler named in the manifest.
     *
     * <p>The same set the single-loop path offers. A custom scheduler shipped as
     * a serialised object is not supported here yet, and returning null says so
     * rather than silently substituting one.
     */
    static LoadScheduler load(String schedulerName) {
        if (schedulerName == null || !schedulerName.startsWith("in.co.s13.sips.schedulers.")) {
            return null;
        }
        if (schedulerName.endsWith("Chunk")) {
            return new LoadScheduler(new Chunk());
        }
        if (schedulerName.endsWith("Factoring")) {
            return new LoadScheduler(new Factoring());
        }
        if (schedulerName.endsWith("GATDS")) {
            return new LoadScheduler(new GATDS());
        }
        if (schedulerName.endsWith("GA")) {
            return new LoadScheduler(new GA());
        }
        if (schedulerName.endsWith("GSS")) {
            return new LoadScheduler(new GSS());
        }
        if (schedulerName.endsWith("QSS")) {
            return new LoadScheduler(new QSS());
        }
        if (schedulerName.endsWith("TSS")) {
            return new LoadScheduler(new TSS());
        }
        return null;
    }

    private void setStatus(String status) {
        GlobalValues.RESULT_DB_EXECUTOR.submit(() -> {
            in.co.s13.SIPS.datastructure.Result result = GlobalValues.RESULT_DB.get(jobToken);
            if (result != null) {
                result.setStatus(status);
                result.setFinished(sequencer != null && sequencer.isFinished());
            }
        });
    }

    /** Where the pipeline got to, for anything watching it. */
    public JobSequencer sequencer() {
        return sequencer;
    }
}
