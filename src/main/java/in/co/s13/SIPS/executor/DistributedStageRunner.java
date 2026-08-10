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

import java.util.Map;
import java.util.LinkedHashMap;
import java.io.IOException;
import in.co.s13.sips.lib.job.ChunkSpec;
import in.co.s13.sips.lib.manifest.TaskType;
import in.co.s13.sips.lib.protocol.Protocol.Feature;
import in.co.s13.sips.lib.protocol.Protocol;
import in.co.s13.sips.lib.common.SipsPaths;
import in.co.s13.SIPS.tools.JobPaths;
import in.co.s13.SIPS.datastructure.DistributionDBRow;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.common.datastructure.ParallelForLoop;
import in.co.s13.sips.lib.common.datastructure.ParallelForSENP;
import in.co.s13.sips.lib.job.Stage;
import in.co.s13.sips.lib.job.StageRunner;
import in.co.s13.sips.lib.common.datastructure.Node;
import in.co.s13.sips.scheduler.LoadScheduler;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/**
 * Runs a {@link Stage} by splitting it into chunks and distributing them, the
 * same way a single-loop job is distributed today.
 *
 * <h2>Chunk numbering</h2>
 *
 * <p>The distribution table is keyed {@code nodeUuid-chunkNumber} within a job
 * token, and the completion path that writes exit codes into it knows nothing
 * about stages. So chunk numbers are handed out from one counter across the
 * whole job rather than restarting at zero per stage: two stages that both
 * numbered from zero would land on the same key and one would overwrite the
 * other's result.
 *
 * <p>Each execution remembers its own keys, which is all polling needs — a
 * stage is complete when every chunk it was given has a real exit code.
 *
 * <h2>What is verified</h2>
 *
 * <p>The polling half is tested against a populated distribution table. The
 * distributing half needs live nodes and has not been exercised against a
 * cluster.
 */
public class DistributedStageRunner implements StageRunner {

    /** The exit code a chunk carries until a node reports back. */
    public static final int NOT_FINISHED = 9999;

    private final String jobToken;
    private final LoadScheduler scheduler;
    private final String schedulerName;
    private final JSONObject schedulerSettings;
    private final AtomicInteger nextChunkNumber = new AtomicInteger();

    public DistributedStageRunner(String jobToken, LoadScheduler scheduler, String schedulerName,
            JSONObject schedulerSettings) {
        if (jobToken == null || jobToken.isBlank()) {
            throw new IllegalArgumentException("jobToken must not be blank");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler must not be null");
        }
        this.jobToken = jobToken.trim();
        this.scheduler = scheduler;
        this.schedulerName = schedulerName == null ? "unknown" : schedulerName;
        this.schedulerSettings = schedulerSettings == null ? new JSONObject() : schedulerSettings;
    }

    @Override
    public StageExecution start(Stage stage) {
        ConcurrentHashMap<String, Node> live = Util.getAllLiveNodes();
        if (live.isEmpty()) {
            // Distributing to nobody would leave a stage that never reports and
            // never times out unless a timeout was set. Fail it now instead.
            throw new IllegalStateException("No live nodes to run stage '" + stage.name() + "'");
        }

        // A node running an older build accepts a pipeline chunk and then fails
        // it, so it is left out here rather than found out after the round trip.
        Feature required = stage.taskType() == TaskType.WASM
                ? Feature.WASM_TASKS
                : Feature.STAGED_JOBS;
        ConcurrentHashMap<String, Node> nodes = NodeCapabilities.capableOf(required, live);
        String leftOut = NodeCapabilities.summarise(required, live.values());
        if (!leftOut.isEmpty()) {
            Util.appendToJobLog(GlobalValues.LOG_LEVEL.OUTPUT, leftOut);
        }
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No node can run stage '" + stage.name()
                    + "'. " + Protocol.refusalReason(required, Protocol.UNKNOWN));
        }

        // What the stages this one reads produced, staged where each chunk's
        // copy will pick it up. Done before scheduling so a stage whose inputs
        // are missing fails here rather than on a node.
        List<String> inputs;
        try {
            inputs = StageOutputs.stageInputs(jobToken, stage);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not give stage '" + stage.name()
                    + "' its inputs: " + ex.getMessage(), ex);
        }

        List<ParallelForSENP> chunks = scheduler.scheduleParallelFor(nodes,
                new ParallelForLoop(stage.firstIndex(), stage.lastIndexExclusive(),
                        stage.iterationCount(), 3, false),
                schedulerSettings);
        scheduler.getOutputs().forEach(out ->
                Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.OUTPUT, out));
        scheduler.getErrors().forEach(err ->
                Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, err));

        if (chunks.isEmpty()) {
            throw new IllegalStateException("Scheduler produced no chunks for stage '"
                    + stage.name() + "'");
        }

        ConcurrentHashMap<String, DistributionDBRow> distTable =
                GlobalValues.MASTER_DIST_DB.computeIfAbsent(jobToken,
                        token -> new ConcurrentHashMap<>());

        // Put each shard back where its data already is, swapping only within
        // the nodes the scheduler chose so its balance survives.
        Map<Integer, String> scheduled = new LinkedHashMap<>();
        for (int shard = 0; shard < chunks.size(); shard++) {
            scheduled.put(shard, chunks.get(shard).getNodeUUID());
        }
        Map<Integer, String> placed = ShardAffinity.apply(jobToken, scheduled, nodes);
        for (int shard = 0; shard < chunks.size(); shard++) {
            chunks.get(shard).setNodeUUID(placed.get(shard));
        }

        Set<String> keys = new LinkedHashSet<>();
        Map<Integer, Integer> shardsByChunk = new LinkedHashMap<>();
        List<Node> backupNodes = scheduler.getBackupNodes();
        for (int shard = 0; shard < chunks.size(); shard++) {
            ParallelForSENP chunk = chunks.get(shard);
            int chunkNumber = nextChunkNumber.getAndIncrement();
            chunk.setChunkNo(chunkNumber);
            shardsByChunk.put(chunkNumber, shard);
            ShardAffinity.record(jobToken, shard, chunk.getNodeUUID());
            copyStageSources(stage, chunk, chunkNumber);
            writeChunkSpec(stage, chunk, chunkNumber, shard, inputs);

            Distributor distributor = new Distributor(chunk.getNodeUUID(),
                    String.valueOf(chunkNumber), jobToken);
            if (!upload(distributor, chunk, chunkNumber, backupNodes)) {
                Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR,
                        "Failed to distribute chunk " + chunkNumber + " of stage '"
                        + stage.name() + "'");
            }

            String key = chunk.getNodeUUID() + "-" + chunkNumber;
            distTable.put(key, new DistributionDBRow(0, chunk.getNodeUUID(), jobToken,
                    chunkNumber, 3, schedulerName, System.currentTimeMillis(),
                    0, 0, 0, 0, 0, 0, 0, 0, 0,
                    chunk.getDiff(), chunk.getStart(), chunk.getEnd(), "0", 0,
                    NOT_FINISHED, distributor.getToIPAddress(), distributor.getHostName(),
                    0, chunks.size()));
            keys.add(key);
        }
        return new DistributedStage(stage, keys, shardsByChunk);
    }

    /**
     * Gives this chunk its own copy of the stage's sources, in the directory the
     * distributor uploads from.
     */
    private void copyStageSources(Stage stage, ParallelForSENP chunk, int chunkNumber) {
        File source = new File(SipsPaths.join(JobPaths.job(jobToken), "stages",
                stage.name(), "src"));
        if (!source.isDirectory()) {
            // Stages that share the job's single source tree, which is the
            // common case for a WASM pipeline shipping one module.
            source = new File(JobPaths.source(jobToken));
        }
        Util.copyFolder(source, new File(JobPaths.chunkSource(jobToken,
                chunk.getNodeUUID(), chunkNumber)));
    }

    /**
     * Writes what this chunk in particular is being asked to do.
     *
     * <p>The manifest is one file for the whole job, so the slice, the inputs
     * and the output name -- all per chunk -- travel here instead.
     */
    private void writeChunkSpec(Stage stage, ParallelForSENP chunk, int chunkNumber,
            int shard, List<String> inputs) {
        ChunkSpec.Builder spec = ChunkSpec
                .range(Long.parseLong(chunk.getStart()), Long.parseLong(chunk.getEnd()))
                .stage(stage.name())
                .shard(shard);
        stage.output().ifPresent(pattern -> spec.output(stage.outputFor(shard)));
        inputs.forEach(spec::input);
        Util.write(new File(SipsPaths.join(
                JobPaths.chunkSource(jobToken, chunk.getNodeUUID(), chunkNumber),
                ChunkSpec.FILE)), spec.build().toJSON().toString());
    }

    private boolean upload(Distributor distributor, ParallelForSENP chunk, int chunkNumber,
            List<Node> backupNodes) {
        if (distributor.upload()) {
            return true;
        }
        // The same retry-then-fall-back-to-a-spare-node shape the single-loop
        // path uses; a node can die between being scheduled and being sent to.
        for (Node backup : new ArrayList<>(backupNodes)) {
            chunk.setNodeUUID(backup.getUuid());
            if (new Distributor(backup.getUuid(), String.valueOf(chunkNumber), jobToken).upload()) {
                return true;
            }
        }
        return false;
    }

    /** How many chunk numbers have been handed out across every stage so far. */
    public int chunksDistributed() {
        return nextChunkNumber.get();
    }

    /**
     * One started stage, watched through the chunks it was given.
     *
     * <p>Visible for testing the polling rules without a cluster: a test can
     * build one over keys it has put in the distribution table itself.
     */
    public class DistributedStage implements StageExecution {

        private final Stage stage;
        private final Set<String> chunkKeys;
        private final Map<Integer, Integer> shardsByChunk;
        private String failure;

        DistributedStage(Stage stage, Set<String> chunkKeys,
                Map<Integer, Integer> shardsByChunk) {
            this.stage = stage;
            this.chunkKeys = Set.copyOf(chunkKeys);
            this.shardsByChunk = Map.copyOf(shardsByChunk);
        }

        @Override
        public Outcome poll() {
            ConcurrentHashMap<String, DistributionDBRow> distTable =
                    GlobalValues.MASTER_DIST_DB.get(jobToken);
            if (distTable == null) {
                failure = "the distribution table for " + jobToken + " is gone";
                return Outcome.FAILED;
            }

            boolean allFinished = true;
            for (String key : chunkKeys) {
                DistributionDBRow row = distTable.get(key);
                if (row == null) {
                    failure = "chunk " + key + " of stage '" + stage.name()
                            + "' vanished from the distribution table";
                    return Outcome.FAILED;
                }
                int exitCode = row.getExitcode();
                if (exitCode == NOT_FINISHED) {
                    allFinished = false;
                } else if (exitCode != 0) {
                    // One bad chunk is a bad stage: whatever depends on it would
                    // be reading an incomplete result.
                    failure = "chunk " + row.getCno() + " of stage '" + stage.name()
                            + "' exited " + exitCode;
                    return Outcome.FAILED;
                }
            }
            if (!allFinished) {
                return Outcome.RUNNING;
            }
            try {
                // Gathered here rather than by the next stage, so a result that
                // never came home is reported against the stage that owed it.
                StageOutputs.collect(jobToken, stage, shardsByChunk);
            } catch (IOException ex) {
                failure = ex.getMessage();
                return Outcome.FAILED;
            }
            return Outcome.COMPLETE;
        }

        @Override
        public Optional<String> failureReason() {
            return Optional.ofNullable(failure);
        }

        @Override
        public void cancel() {
            // Best effort: tell the nodes holding this stage's chunks to stop.
            ConcurrentHashMap<String, DistributionDBRow> distTable =
                    GlobalValues.MASTER_DIST_DB.get(jobToken);
            if (distTable == null) {
                return;
            }
            for (String key : chunkKeys) {
                DistributionDBRow row = distTable.get(key);
                if (row != null && row.getExitcode() == NOT_FINISHED) {
                    KillChunk.send(row.getIpAddress(), jobToken,
                            String.valueOf(row.getCno()), row.getUuid());
                }
            }
        }

        /** The distribution-table keys this stage is waiting on. */
        public Set<String> chunkKeys() {
            return chunkKeys;
        }
    }
}
