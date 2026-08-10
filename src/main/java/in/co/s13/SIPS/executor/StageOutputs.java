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
import in.co.s13.sips.lib.common.SipsPaths;
import in.co.s13.sips.lib.job.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Moves what one stage produced to where the next stage can read it.
 *
 * <p>Without this a pipeline can express that {@code register} follows
 * {@code bias} and still have no way for the second to see the first's output.
 * That gap is what kept task graphs to sequencing rather than actual data flow,
 * and it is the last thing standing between a training run and a cluster: every
 * round of federated averaging is exactly this move — many models in, one
 * averaged model out, back to many workers.
 *
 * <h2>Where things live</h2>
 *
 * <pre>
 * data/&lt;job&gt;/stages/&lt;stage&gt;/out/&lt;shard&gt;.bin   what a stage produced
 * data/&lt;job&gt;/stages/&lt;stage&gt;/src/              what its chunks are given
 * </pre>
 *
 * <p>Collected by shard rather than by chunk number. Chunk numbers run across
 * the whole job so two stages cannot collide in the distribution table, which
 * makes round two's worker 0 chunk number 8 — and a stage that wants "the model
 * from worker 0" would find nothing under that name. Shards are what the
 * algorithm counts in.
 *
 * <h2>What is carried, and what is not</h2>
 *
 * <p>Results ride home inside the finish message, so anything up to
 * {@link ChunkResults#MAX_INLINE_BYTES} arrives without a second round trip.
 * That is right for what a call returns and wrong for a model — anything with
 * a hidden layer is megabytes — so a larger result stays in the sandbox that
 * produced it and is fetched by name through {@link ResultFetch}. The inline
 * path is tried first, so only the shards that need one cost a round trip.
 */
public final class StageOutputs {

    /** Everything a stage produced or was given. */
    public static final String STAGES = "stages";

    /**
     * Collects one chunk's result from wherever it actually is.
     *
     * <p>An interface rather than a direct call so collection can be tested
     * without a cluster, and so a caller that already holds the bytes — a
     * single-machine run, a replay — does not have to pretend to be a node.
     */
    @FunctionalInterface
    public interface Fetcher {

        /** Never used: a caller that has no way to reach the producing node. */
        Fetcher NONE = (chunkNumber, fileName) -> {
            throw new IOException("no way to reach the node that produced it");
        };

        /**
         * @param fileName the output name the stage declared for this shard
         * @throws IOException if the result cannot be had
         */
        byte[] fetch(int chunkNumber, String fileName) throws IOException;
    }

    private StageOutputs() {
    }

    /** Where a stage's collected outputs live on the master. */
    public static String outputDirectory(String jobToken, String stageName) {
        return SipsPaths.join(JobPaths.job(jobToken), STAGES, stageName, "out");
    }

    /** Where a stage's chunk sources are staged before distribution. */
    public static String sourceDirectory(String jobToken, String stageName) {
        return SipsPaths.join(JobPaths.job(jobToken), STAGES, stageName, "src");
    }

    /** Gathers a finished stage's results, using only what came home inline. */
    public static int collect(String jobToken, Stage stage, Map<Integer, Integer> shardsByChunk)
            throws IOException {
        return collect(jobToken, stage, shardsByChunk, Fetcher.NONE);
    }

    /**
     * Gathers a finished stage's results into its output directory.
     *
     * @param shardsByChunk which shard each of the stage's chunk numbers was
     * @param fetcher how to collect a result too large to have ridden home
     * @return how many results were collected
     * @throws IOException if any shard's result cannot be had, or written
     */
    public static int collect(String jobToken, Stage stage, Map<Integer, Integer> shardsByChunk,
            Fetcher fetcher) throws IOException {
        Path directory = Path.of(outputDirectory(jobToken, stage.name()));
        Files.createDirectories(directory);

        int collected = 0;
        Map<Integer, String> missing = new TreeMap<>();
        for (Map.Entry<Integer, Integer> entry : new TreeMap<>(shardsByChunk).entrySet()) {
            int chunkNumber = entry.getKey();
            int shard = entry.getValue();
            byte[] result = ChunkResults.of(jobToken, String.valueOf(chunkNumber)).orElse(null);
            if (result == null) {
                try {
                    result = fetch(stage, fetcher, chunkNumber, shard);
                } catch (IOException unreachable) {
                    missing.put(shard, unreachable.getMessage());
                    continue;
                }
            }
            Files.write(directory.resolve(shard + ".bin"), result);
            collected++;
        }

        if (!missing.isEmpty()) {
            // Named rather than swallowed: a stage that averages seven of eight
            // models produces a subtly wrong answer, not an obvious failure.
            throw new IOException("Stage '" + stage.name() + "' produced no collectable result "
                    + "for shard(s) " + new ArrayList<>(missing.keySet()) + ": "
                    + String.join("; ", missing.values()));
        }
        return collected;
    }

    /** Asks the producing node for a result that did not ride home. */
    private static byte[] fetch(Stage stage, Fetcher fetcher, int chunkNumber, int shard)
            throws IOException {
        if (stage.output().isEmpty()) {
            // Nothing to ask for by name, so an empty result here is genuinely
            // missing rather than merely too large to have been carried.
            throw new IOException("stage declares no output to collect");
        }
        return fetcher.fetch(chunkNumber, stage.outputFor(shard));
    }

    /**
     * Places the outputs of the stages this one reads into its source
     * directory, ready to be copied into each chunk.
     *
     * @return the file names placed, in shard order — what a chunk should be
     *         told to open
     */
    public static List<String> stageInputs(String jobToken, Stage stage) throws IOException {
        Path source = Path.of(sourceDirectory(jobToken, stage.name()));
        Files.createDirectories(source);

        List<String> placed = new ArrayList<>();
        for (Stage producer : stage.inputs()) {
            Path from = Path.of(outputDirectory(jobToken, producer.name()));
            if (!Files.isDirectory(from)) {
                throw new IOException("Stage '" + stage.name() + "' reads '" + producer.name()
                        + "', which has produced nothing. It either did not run or its results "
                        + "were never collected.");
            }
            List<Path> outputs;
            try (var listing = Files.list(from)) {
                outputs = listing.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(path -> shardOf(path.getFileName().toString())))
                        .toList();
            }
            for (Path output : outputs) {
                // Prefixed by producer, so a stage reading two producers cannot
                // have one silently overwrite the other's shard 0.
                String name = producer.name() + "-" + output.getFileName();
                Files.copy(output, source.resolve(name),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                placed.add(name);
            }
        }
        return placed;
    }

    /** Sorts {@code 10.bin} after {@code 2.bin} rather than before it. */
    private static Integer shardOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        try {
            return Integer.parseInt(dot < 0 ? fileName : fileName.substring(0, dot));
        } catch (NumberFormatException notAShard) {
            return Integer.MAX_VALUE;
        }
    }

    /** Forgets a job's staged files once it is finished with. */
    public static void forget(String jobToken) {
        File stages = new File(SipsPaths.join(JobPaths.job(jobToken), STAGES));
        if (stages.isDirectory()) {
            in.co.s13.SIPS.tools.Util.deleteDirectory(stages);
        }
    }
}
