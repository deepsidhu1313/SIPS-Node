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

import in.co.s13.sips.lib.job.JobManifest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Choosing the pipeline path, and the scheduler it runs on.
 *
 * <p>The riskiest thing about adding a second job path is the first one. Every
 * manifest in existence goes through the old one, and a dispatch rule that
 * misfires would break jobs nobody touched — so what a manifest <em>without</em>
 * stages does is as much the subject here as what one with them does.
 */
class StagedJobTest {

    @Test
    void aManifestWithStagesTakesThePipelinePath() {
        JSONObject manifest = new JSONObject()
                .put("PROJECT", "pipeline")
                .put("STAGES", new JSONArray().put(
                        new JSONObject().put("NAME", "only").put("KIND", "single")));

        assertTrue(JobManifest.hasStages(manifest));
    }

    @Test
    void everyManifestWrittenBeforeStagesExistedTakesTheOldPath() {
        // The dispatch rule in JobHandler turns on exactly this.
        assertFalse(JobManifest.hasStages(new JSONObject()
                .put("PROJECT", "mandelbrot")
                .put("MAIN", "Mandelbrot")
                .put("SCHEDULER", new JSONObject().put("Name", "in.co.s13.sips.schedulers.GSS"))));
    }

    @Test
    void anEmptyStageListIsNotAPipeline() {
        assertFalse(JobManifest.hasStages(
                new JSONObject().put("STAGES", new JSONArray())));
    }

    // ---- scheduler selection ----

    @Test
    void theSchedulersTheSingleLoopPathOffersAreTheOnesAPipelineGets() {
        // Diverging here would mean a pipeline silently scheduling differently
        // from the loop the user tested.
        assertNotNull(StagedJob.load("in.co.s13.sips.schedulers.Chunk"));
        assertNotNull(StagedJob.load("in.co.s13.sips.schedulers.GSS"));
        assertNotNull(StagedJob.load("in.co.s13.sips.schedulers.TSS"));
        assertNotNull(StagedJob.load("in.co.s13.sips.schedulers.QSS"));
        assertNotNull(StagedJob.load("in.co.s13.sips.schedulers.Factoring"));
        assertNotNull(StagedJob.load("in.co.s13.sips.schedulers.GA"));
        assertNotNull(StagedJob.load("in.co.s13.sips.schedulers.GATDS"));
    }

    @Test
    void eachNameLoadsTheSchedulerItActuallyNames() {
        // GATDS ends with the letters of no other name, but GA is a suffix
        // check away from matching it -- and swapping a dependency-aware
        // scheduler for one that is not would show up only as worse timings.
        assertEquals("GATDS", schedulerBehind("in.co.s13.sips.schedulers.GATDS"));
        assertEquals("GA", schedulerBehind("in.co.s13.sips.schedulers.GA"));
        assertEquals("GSS", schedulerBehind("in.co.s13.sips.schedulers.GSS"));
        assertEquals("QSS", schedulerBehind("in.co.s13.sips.schedulers.QSS"));
        assertEquals("TSS", schedulerBehind("in.co.s13.sips.schedulers.TSS"));
        assertEquals("Chunk", schedulerBehind("in.co.s13.sips.schedulers.Chunk"));
        assertEquals("Factoring", schedulerBehind("in.co.s13.sips.schedulers.Factoring"));
    }

    private static String schedulerBehind(String name) {
        return StagedJob.load(name).scheduler().getClass().getSimpleName();
    }

    @Test
    void anUnknownSchedulerIsRefusedRatherThanSubstituted() {
        // Quietly falling back would run the job on a scheduler nobody chose and
        // report timings for it.
        assertNull(StagedJob.load("com.example.MyScheduler"));
        assertNull(StagedJob.load("in.co.s13.sips.schedulers.NotAThing"));
        assertNull(StagedJob.load(""));
        assertNull(StagedJob.load(null));
    }

    @Test
    void aJobNeedsAToken() {
        assertThrows(IllegalArgumentException.class, () -> new StagedJob(" "));
        assertThrows(IllegalArgumentException.class, () -> new StagedJob(null));
    }

    @Test
    void aPipelineHasNoProgressBeforeItStarts() {
        assertNull(new StagedJob("job-1").sequencer());
    }
}
