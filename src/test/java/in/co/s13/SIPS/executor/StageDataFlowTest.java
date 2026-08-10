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

import in.co.s13.sips.lib.common.datastructure.LiveNode;
import in.co.s13.sips.lib.common.datastructure.Node;
import in.co.s13.sips.lib.job.Job;
import in.co.s13.sips.lib.job.Stage;
import in.co.s13.sips.lib.ml.Tensors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moving what one stage produced to where the next stage reads it.
 *
 * <p>Without this a pipeline can say that {@code average} follows {@code train}
 * and still give it nothing to average. Every round of federated averaging is
 * exactly this move, which is why it is the last thing between a training run
 * and a cluster.
 */
class StageDataFlowTest {

    private static final String JOB = "job-dataflow-test";

    @AfterEach
    void clean() {
        StageOutputs.forget(JOB);
        ShardAffinity.forget(JOB);
        ChunkResults.forget(JOB);
    }

    private static Job trainThenAverage() {
        Job job = new Job(JOB);
        Stage train = job.parallelFor("train-1", 0, 3).writes("model-{index}.bin");
        job.single("average-1").reads(train).writes("weights.bin");
        return job;
    }

    /** What a finished chunk leaves behind: its result, carried home inline. */
    private static void chunkFinished(int chunkNumber, byte[] result) {
        ChunkResults.record(JOB, String.valueOf(chunkNumber), ChunkResults.encode(result));
    }

    @Test
    void aStagesResultsAreGatheredByShard() throws IOException {
        // Collected by shard, not chunk number: chunk numbers run across the
        // whole job so two stages cannot collide in the distribution table,
        // which makes round two's worker 0 chunk number 8. A stage asking for
        // "the model from worker 0" would find nothing under that name.
        Job job = trainThenAverage();
        chunkFinished(5, Tensors.toBytes(new float[]{1f, 2f}));
        chunkFinished(6, Tensors.toBytes(new float[]{3f, 4f}));
        chunkFinished(7, Tensors.toBytes(new float[]{5f, 6f}));
        Map<Integer, Integer> shards = new LinkedHashMap<>();
        shards.put(5, 0);
        shards.put(6, 1);
        shards.put(7, 2);

        int collected = StageOutputs.collect(JOB, job.stage("train-1").orElseThrow(), shards);

        assertEquals(3, collected);
        Path out = Path.of(StageOutputs.outputDirectory(JOB, "train-1"));
        assertArrayEquals(new float[]{1f, 2f},
                Tensors.fromBytes(Files.readAllBytes(out.resolve("0.bin"))));
        assertArrayEquals(new float[]{5f, 6f},
                Tensors.fromBytes(Files.readAllBytes(out.resolve("2.bin"))));
    }

    @Test
    void aMissingResultIsNamedRatherThanSkipped() {
        // Averaging seven of eight models produces a subtly wrong answer, not
        // an obvious failure. It has to stop.
        Job job = trainThenAverage();
        chunkFinished(5, Tensors.toBytes(new float[]{1f}));
        Map<Integer, Integer> shards = new LinkedHashMap<>();
        shards.put(5, 0);
        shards.put(6, 1);

        String message = assertThrows(IOException.class,
                () -> StageOutputs.collect(JOB, job.stage("train-1").orElseThrow(), shards))
                .getMessage();

        assertTrue(message.contains("[1]"), message);
        assertTrue(message.contains("train-1"), message);
    }

    @Test
    void theNextStageIsGivenWhatThePreviousOneProduced() throws IOException {
        Job job = trainThenAverage();
        chunkFinished(5, Tensors.toBytes(new float[]{1f}));
        chunkFinished(6, Tensors.toBytes(new float[]{2f}));
        chunkFinished(7, Tensors.toBytes(new float[]{3f}));
        Map<Integer, Integer> shards = new LinkedHashMap<>();
        shards.put(5, 0);
        shards.put(6, 1);
        shards.put(7, 2);
        StageOutputs.collect(JOB, job.stage("train-1").orElseThrow(), shards);

        List<String> placed = StageOutputs.stageInputs(JOB, job.stage("average-1").orElseThrow());

        assertEquals(List.of("train-1-0.bin", "train-1-1.bin", "train-1-2.bin"), placed);
        Path src = Path.of(StageOutputs.sourceDirectory(JOB, "average-1"));
        assertArrayEquals(new float[]{2f},
                Tensors.fromBytes(Files.readAllBytes(src.resolve("train-1-1.bin"))));
    }

    @Test
    void inputsAreOrderedByShardNotByName() throws IOException {
        // "10.bin" sorts before "2.bin" as text, which would hand the averager
        // its models in an order that quietly changes the result.
        Job job = new Job(JOB);
        Stage train = job.parallelFor("train-1", 0, 11).writes("m-{index}.bin");
        job.single("average-1").reads(train);
        Map<Integer, Integer> shards = new LinkedHashMap<>();
        for (int shard = 0; shard < 11; shard++) {
            chunkFinished(shard, Tensors.toBytes(new float[]{shard}));
            shards.put(shard, shard);
        }
        StageOutputs.collect(JOB, train, shards);

        List<String> placed = StageOutputs.stageInputs(JOB, job.stage("average-1").orElseThrow());

        assertEquals("train-1-0.bin", placed.get(0));
        assertEquals("train-1-2.bin", placed.get(2));
        assertEquals("train-1-10.bin", placed.get(10), "10 must come after 9, not after 1");
    }

    @Test
    void readingAStageThatProducedNothingSaysSo() {
        Job job = trainThenAverage();

        assertTrue(assertThrows(IOException.class,
                () -> StageOutputs.stageInputs(JOB, job.stage("average-1").orElseThrow()))
                .getMessage().contains("train-1"));
    }

    @Test
    void twoProducersCannotOverwriteEachOthersShardZero() throws IOException {
        // Both write a shard 0. Prefixing by producer is what keeps one from
        // silently replacing the other.
        Job job = new Job(JOB);
        Stage left = job.parallelFor("left", 0, 1).writes("l-{index}.bin");
        Stage right = job.parallelFor("right", 0, 1).writes("r-{index}.bin");
        Stage merge = job.single("merge").reads(left).reads(right);

        chunkFinished(0, Tensors.toBytes(new float[]{1f}));
        StageOutputs.collect(JOB, left, Map.of(0, 0));
        chunkFinished(1, Tensors.toBytes(new float[]{2f}));
        StageOutputs.collect(JOB, right, Map.of(1, 0));

        List<String> placed = StageOutputs.stageInputs(JOB, merge);

        assertEquals(2, placed.size(), placed.toString());
        assertTrue(placed.contains("left-0.bin") && placed.contains("right-0.bin"), placed
                .toString());
    }

    // ---- shard affinity ----

    private static Node node(String uuid) {
        return new LiveNode(uuid, "host-" + uuid, "linux", "cpu", 4, 0,
                8L << 30, 4L << 30, 512L << 30, 256L << 30, new JSONObject(),
                System.currentTimeMillis(), 0.1);
    }

    private static Map<String, Node> cluster(String... uuids) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (String uuid : uuids) {
            nodes.put(uuid, node(uuid));
        }
        return nodes;
    }

    @Test
    void aShardGoesBackToTheNodeThatHasItsData() {
        // The point: round one pulled shard 3 across the network. Round two
        // should not pull it again.
        ShardAffinity.record(JOB, 0, "node-a");
        ShardAffinity.record(JOB, 1, "node-b");
        Map<Integer, String> scheduled = new LinkedHashMap<>();
        scheduled.put(0, "node-b");
        scheduled.put(1, "node-a");

        Map<Integer, String> placed = ShardAffinity.apply(JOB, scheduled,
                cluster("node-a", "node-b"));

        assertEquals("node-a", placed.get(0));
        assertEquals("node-b", placed.get(1));
    }

    @Test
    void theSchedulersBalanceSurvives() {
        // Only swaps within the chosen set, so each node keeps the same number
        // of chunks. Honouring a preference by adding work to a node the
        // scheduler did not pick would trade a transfer for a hot spot.
        ShardAffinity.record(JOB, 0, "node-a");
        ShardAffinity.record(JOB, 1, "node-a");
        Map<Integer, String> scheduled = new LinkedHashMap<>();
        scheduled.put(0, "node-a");
        scheduled.put(1, "node-b");

        Map<Integer, String> placed = ShardAffinity.apply(JOB, scheduled,
                cluster("node-a", "node-b"));

        assertEquals(1, placed.values().stream().filter("node-a"::equals).count());
        assertEquals(1, placed.values().stream().filter("node-b"::equals).count());
    }

    @Test
    void aRememberedNodeThatIsGoneIsIgnored() {
        // A preference, never a requirement. Pinning to a node that has died
        // would trade a transfer for a stall.
        ShardAffinity.record(JOB, 0, "node-gone");
        Map<Integer, String> scheduled = new LinkedHashMap<>();
        scheduled.put(0, "node-a");

        assertEquals("node-a",
                ShardAffinity.apply(JOB, scheduled, cluster("node-a")).get(0));
    }

    @Test
    void theFirstRoundHasNothingToRemember() {
        Map<Integer, String> scheduled = new LinkedHashMap<>();
        scheduled.put(0, "node-a");
        scheduled.put(1, "node-b");

        assertEquals(scheduled, ShardAffinity.apply(JOB, scheduled, cluster("node-a", "node-b")));
        assertEquals(0, ShardAffinity.remembered(JOB));
    }

    @Test
    void oneJobsPlacementsAreNotAnothers() {
        ShardAffinity.record(JOB, 0, "node-a");
        ShardAffinity.record("other-job", 0, "node-b");

        assertEquals("node-a",
                ShardAffinity.preferredNode(JOB, 0, cluster("node-a", "node-b")).orElseThrow());
        ShardAffinity.forget("other-job");
    }
}
