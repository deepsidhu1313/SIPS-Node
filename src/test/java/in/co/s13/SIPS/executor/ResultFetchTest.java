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

import in.co.s13.SIPS.executor.sockets.handlers.FileHandler;
import in.co.s13.SIPS.tools.JobPaths;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.ml.Tensors;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fetching a chunk result too large to ride home inside the finish message.
 *
 * <p>A result under {@link ChunkResults#MAX_INLINE_BYTES} comes back with the
 * message that says the chunk finished. A model with a hidden layer does not:
 * anything real is megabytes, so without this a training run works on the
 * sample and fails on the thing anyone would actually train.
 *
 * <p>These run against a real {@link FileHandler} over a real socket, because
 * every interesting failure here is a wire failure — a node that answers
 * halfway, or does not answer at all, is what turns a round into a hung
 * pipeline.
 */
class ResultFetchTest {

    private static final String JOB = "job-fetch-test";
    private static final String NODE = "node-fetch-test";

    private ServerSocket server;
    private ExecutorService serving;

    @AfterEach
    void stop() throws IOException {
        if (server != null) {
            server.close();
        }
        if (serving != null) {
            serving.shutdownNow();
        }
        Util.deleteDirectory(new java.io.File(JobPaths.chunkWorkingDirectory(NODE, JOB, 0)));
        Util.deleteDirectory(new java.io.File(
                JobPaths.chunkWorkingDirectory(Distributor.senderUuid(), JOB, 0)));
    }

    /** Starts the real file handler on an ephemeral port and returns it. */
    private int realNode() throws IOException {
        server = new ServerSocket(0);
        serving = Executors.newCachedThreadPool();
        serving.submit(() -> {
            while (!server.isClosed()) {
                Socket accepted = server.accept();
                serving.submit(new FileHandler(accepted));
            }
            return null;
        });
        return server.getLocalPort();
    }

    /** Starts a node that behaves badly in the given way. */
    private int brokenNode(java.util.function.Consumer<Socket> misbehave) throws IOException {
        server = new ServerSocket(0);
        serving = Executors.newCachedThreadPool();
        serving.submit(() -> {
            while (!server.isClosed()) {
                Socket accepted = server.accept();
                serving.submit(() -> misbehave.accept(accepted));
            }
            return null;
        });
        return server.getLocalPort();
    }

    /** Writes a chunk's declared output where the chunk actually ran. */
    private static byte[] resultOnDisk(String name, int bytes) throws IOException {
        Path sandbox = Path.of(JobPaths.chunkWorkingDirectory(NODE, JOB, 0));
        Files.createDirectories(sandbox);
        byte[] content = new byte[bytes];
        new Random(42).nextBytes(content);
        Files.write(sandbox.resolve(name), content);
        return content;
    }

    @Test
    @Timeout(60)
    void fetchesAResultTooLargeToRideHome() throws IOException {
        // Four times the inline cap: the case the inline path refuses, which
        // is every model that is not a toy.
        byte[] expected = resultOnDisk("model-0.bin", 4 * ChunkResults.MAX_INLINE_BYTES);

        byte[] fetched = ResultFetch.from("127.0.0.1", realNode(), JOB, NODE, 0, "model-0.bin");

        assertArrayEquals(expected, fetched);
    }

    @Test
    @Timeout(60)
    void anEmptyResultIsStillAResult() throws IOException {
        // A worker whose shard produced nothing writes an empty file. Treating
        // that as "missing" would fail a round that actually succeeded.
        resultOnDisk("model-0.bin", 0);

        assertArrayEquals(new byte[0],
                ResultFetch.from("127.0.0.1", realNode(), JOB, NODE, 0, "model-0.bin"));
    }

    @Test
    @Timeout(60)
    void aMissingResultSaysSoInsteadOfHanging() throws IOException {
        int port = realNode();

        IOException failed = assertThrows(IOException.class,
                () -> ResultFetch.from("127.0.0.1", port, JOB, NODE, 0, "model-0.bin"));

        assertTrue(failed.getMessage().contains("model-0.bin"), failed.getMessage());
    }

    @Test
    @Timeout(60)
    void aTruncatedResultIsRefusedRatherThanAveraged() throws IOException {
        // The failure this whole class exists to prevent. Half a model is
        // still a well-formed float array, so averaging it produces a wrong
        // answer rather than an error, and the run looks like it worked.
        byte[] full = resultOnDisk("model-0.bin", 64 * 1024);
        int port = brokenNode(socket -> {
            try (socket; DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                new java.io.DataInputStream(socket.getInputStream()).readInt();
                String header = new org.json.JSONObject()
                        .put("MSG", "found")
                        .put("BYTES", full.length)
                        .put("CHECKSUM", ResultFetch.checksumOf(full))
                        .toString();
                byte[] bytes = header.getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.write(full, 0, full.length / 2);
            } catch (IOException expected) {
                // The client hangs up when it refuses; nothing to do.
            }
        });

        assertThrows(IOException.class,
                () -> ResultFetch.from("127.0.0.1", port, JOB, NODE, 0, "model-0.bin"));
    }

    @Test
    @Timeout(60)
    void aCorruptedResultIsRefusedRatherThanAveraged() throws IOException {
        // Right length, wrong bytes. Only the checksum catches this one.
        byte[] full = resultOnDisk("model-0.bin", 8 * 1024);
        int port = brokenNode(socket -> {
            try (socket; DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                new java.io.DataInputStream(socket.getInputStream()).readInt();
                String header = new org.json.JSONObject()
                        .put("MSG", "found")
                        .put("BYTES", full.length)
                        .put("CHECKSUM", ResultFetch.checksumOf(full))
                        .toString();
                byte[] bytes = header.getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.write(new byte[full.length]);
            } catch (IOException expected) {
                // As above.
            }
        });

        IOException failed = assertThrows(IOException.class,
                () -> ResultFetch.from("127.0.0.1", port, JOB, NODE, 0, "model-0.bin"));
        assertTrue(failed.getMessage().toLowerCase().contains("checksum"), failed.getMessage());
    }

    @Test
    @Timeout(60)
    void aNodeThatNeverAnswersDoesNotHangTheRun() throws IOException {
        // A node that died between finishing the chunk and being asked for it.
        // Without a read timeout the master waits forever and the whole job
        // stops -- and no test would ever finish either.
        int port = brokenNode(socket -> {
            try {
                Thread.sleep(java.util.concurrent.TimeUnit.MINUTES.toMillis(5));
            } catch (InterruptedException stopping) {
                Thread.currentThread().interrupt();
            }
        });

        assertThrows(IOException.class, () -> ResultFetch.from("127.0.0.1", port, JOB, NODE, 0,
                "model-0.bin", 2000));
    }

    @Test
    @Timeout(60)
    void aResultIsFetchedFromTheSandboxTheSenderNamed() throws IOException {
        // A chunk sandbox is named after the node that SENT the work, not the
        // one running it: the distributor stamps its own uuid into the task
        // message and the worker builds proc/<that>/<job>/<chunk> from it, the
        // same "per sender" convention the file cache uses. A master asking
        // under the worker's uuid looks in a directory that never existed, and
        // every fetch of a large result misses.
        Path sandbox = Path.of(JobPaths.chunkWorkingDirectory(
                Distributor.senderUuid(), JOB, 0));
        Files.createDirectories(sandbox);
        byte[] expected = Tensors.toBytes(new float[]{1f, 2f, 3f});
        Files.write(sandbox.resolve("model-0.bin"), expected);

        byte[] fetched = DistributedStageRunner.fetchResult("127.0.0.1", realNode(),
                JOB, 0, "model-0.bin");

        assertArrayEquals(expected, fetched);
    }

    @Test
    @Timeout(60)
    void aResultOutsideTheChunkSandboxIsRefused() throws IOException {
        // The name comes from the job manifest, so it is chosen by whoever
        // submitted the job, not by the node serving it.
        int port = realNode();

        assertThrows(IOException.class, () -> ResultFetch.from("127.0.0.1", port, JOB, NODE, 0,
                "../../../../etc/passwd"));
    }

    @Test
    @Timeout(60)
    void anAbsurdLengthIsRefusedBeforeItIsAllocated() throws IOException {
        // A corrupt or hostile header should not be able to ask the master to
        // allocate more memory than it has.
        int port = brokenNode(socket -> {
            try (socket; DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                new java.io.DataInputStream(socket.getInputStream()).readInt();
                String header = new org.json.JSONObject()
                        .put("MSG", "found")
                        .put("BYTES", Long.MAX_VALUE)
                        .put("CHECKSUM", "whatever")
                        .toString();
                byte[] bytes = header.getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
            } catch (IOException expected) {
                // As above.
            }
        });

        IOException failed = assertThrows(IOException.class,
                () -> ResultFetch.from("127.0.0.1", port, JOB, NODE, 0, "model-0.bin"));
        assertTrue(failed.getMessage().contains("too large"), failed.getMessage());
    }
}
