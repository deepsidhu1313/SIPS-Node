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

import in.co.s13.SIPS.datastructure.DistributionDBRow;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.sips.lib.job.Job;
import in.co.s13.sips.lib.job.Stage;
import in.co.s13.sips.lib.job.StageRunner.StageExecution;
import in.co.s13.sips.lib.job.StageRunner.StageExecution.Outcome;
import in.co.s13.sips.schedulers.Chunk;
import in.co.s13.sips.scheduler.LoadScheduler;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Watching a distributed stage through the distribution table.
 *
 * <p>The table is where chunk results actually land — a node reports back, the
 * exit code is written, and nothing calls anyone. Whether a stage is done is
 * therefore a question about rows, and getting it wrong in either direction is
 * expensive: declaring a stage complete too early starts its dependents on half
 * a result, and never declaring it complete hangs the pipeline.
 *
 * <p>Distribution itself needs live nodes and is not exercised here.
 */
class DistributedStageRunnerTest {

    private static final String JOB = "job-stage-runner-test";

    @AfterEach
    void clearTable() {
        GlobalValues.MASTER_DIST_DB.remove(JOB);
    }

    private static DistributedStageRunner runner() {
        return new DistributedStageRunner(JOB, new LoadScheduler(new Chunk()),
                "in.co.s13.sips.schedulers.Chunk", new JSONObject());
    }

    private static Stage aStage() {
        return new Job("j").parallelFor("segment", 0, 100);
    }

    /** Puts a chunk in the table with the given exit code and returns its key. */
    private static String chunk(String nodeUuid, int chunkNumber, int exitCode) {
        ConcurrentHashMap<String, DistributionDBRow> table =
                GlobalValues.MASTER_DIST_DB.computeIfAbsent(JOB, key -> new ConcurrentHashMap<>());
        String key = nodeUuid + "-" + chunkNumber;
        table.put(key, new DistributionDBRow(0, nodeUuid, JOB, chunkNumber, 3, "Chunk",
                System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0,
                "100", "0", "100", "0", 0, exitCode, "127.0.0.1", "localhost", 0, 1));
        return key;
    }

    private static StageExecution watching(String... keys) {
        Set<String> set = new LinkedHashSet<>(java.util.List.of(keys));
        // No shards, so collecting outputs is a no-op: these tests are about
        // when a stage is finished, not about what it produced.
        return runner().new DistributedStage(aStage(), set, java.util.Map.of());
    }

    @Test
    @Timeout(20)
    void aStageIsRunningUntilEveryChunkReportsBack() {
        StageExecution execution = watching(
                chunk("node-a", 0, 0),
                chunk("node-b", 1, DistributedStageRunner.NOT_FINISHED));

        assertEquals(Outcome.RUNNING, execution.poll(),
                "one chunk still out means the stage's result is incomplete");
    }

    @Test
    @Timeout(20)
    void aStageIsCompleteOnlyWhenEveryChunkSucceeded() {
        StageExecution execution = watching(
                chunk("node-a", 0, 0),
                chunk("node-b", 1, 0),
                chunk("node-c", 2, 0));

        assertEquals(Outcome.COMPLETE, execution.poll());
    }

    @Test
    @Timeout(20)
    void oneBadChunkFailsTheStage() {
        // Whatever depends on this stage would otherwise read a result with a
        // hole in it.
        StageExecution execution = watching(
                chunk("node-a", 0, 0),
                chunk("node-b", 7, 1));

        assertEquals(Outcome.FAILED, execution.poll());
        assertTrue(execution.failureReason().orElseThrow().contains("7"),
                "the reason should name the chunk: " + execution.failureReason());
    }

    @Test
    @Timeout(20)
    void aFailingChunkIsReportedEvenWhileOthersAreStillRunning() {
        // No point waiting out the rest of the stage: it cannot succeed now.
        StageExecution execution = watching(
                chunk("node-a", 0, DistributedStageRunner.NOT_FINISHED),
                chunk("node-b", 1, 137));

        assertEquals(Outcome.FAILED, execution.poll());
    }

    @Test
    @Timeout(20)
    void aChunkMissingFromTheTableFailsRatherThanHangs() {
        chunk("node-a", 0, 0);
        StageExecution execution = watching("node-a-0", "node-b-1");

        assertEquals(Outcome.FAILED, execution.poll());
        assertTrue(execution.failureReason().orElseThrow().contains("node-b-1"));
    }

    @Test
    @Timeout(20)
    void aVanishedTableFailsRatherThanHangs() {
        StageExecution execution = watching(chunk("node-a", 0, 0));
        GlobalValues.MASTER_DIST_DB.remove(JOB);

        assertEquals(Outcome.FAILED, execution.poll());
    }

    @Test
    @Timeout(20)
    void aStageOnlyWatchesItsOwnChunks() {
        // Chunk numbers run across the whole job, not per stage, precisely so
        // two stages cannot collide on a table key. Each still watches only what
        // it was given.
        String mine = chunk("node-a", 0, 0);
        chunk("node-b", 1, DistributedStageRunner.NOT_FINISHED);

        assertEquals(Outcome.COMPLETE, watching(mine).poll(),
                "another stage's unfinished chunk is not this stage's problem");
    }

    @Test
    @Timeout(20)
    void startRefusesWhenThereAreNoLiveNodes() {
        // Distributing to nobody produces a stage that never reports; failing
        // now says why.
        assertTrue(assertThrows(IllegalStateException.class,
                () -> runner().start(aStage()))
                .getMessage().contains("No live nodes"));
    }

    @Test
    void aRunnerNeedsAJobAndAScheduler() {
        assertThrows(IllegalArgumentException.class,
                () -> new DistributedStageRunner(" ", new LoadScheduler(new Chunk()), "c", null));
        assertThrows(IllegalArgumentException.class,
                () -> new DistributedStageRunner(JOB, null, "c", null));
    }

    @Test
    void chunkNumbersStartAtZeroAndAreHandedOutOnce() {
        assertEquals(0, runner().chunksDistributed());
    }
}
