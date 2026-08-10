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

import in.co.s13.SIPS.tools.Util;
import in.co.s13.SIPS.transfer.FilePayload;
import in.co.s13.sips.lib.wasm.TestModules;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A chunk delivered as WebAssembly, taken through the real node path.
 *
 * <p>The point of the WASM path is that a chunk costs microseconds to start
 * rather than the hundreds of milliseconds of javac plus JVM startup that the
 * Ant path pays. That only holds if the node genuinely skips the Ant path, so
 * these tests assert the absence of the build file as much as the result.
 */
class ParallelProcessWasmTest {

    private final String uuid = UUID.randomUUID().toString();
    private ParallelProcess process;

    @BeforeAll
    static void identifyNode() {
        if (in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID == null) {
            in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID = UUID.randomUUID().toString();
        }
    }

    @AfterEach
    void removeChunkDirectory() {
        File dir = new File("proc/" + uuid);
        if (dir.exists()) {
            Util.deleteDirectory(dir);
        }
    }

    /** A module with no host imports, whose body is the given instructions. */
    private static byte[] moduleReturning(byte[] body) throws IOException {
        return java.nio.file.Files.readAllBytes(
                TestModules.bare(java.nio.file.Files.createTempDirectory("wasm"), "m.wasm", body));
    }

    /** Builds the task body a distributor would send for a WASM chunk. */
    private JSONObject wasmChunk(byte[] module, long first, long last) {
        JSONArray files = new JSONArray();
        files.put(FilePayload.encode("kernel.wasm", module));
        files.put(FilePayload.encode(ParallelProcess.CHUNK_RANGE_FILE,
                new JSONObject().put("FIRST", first).put("LAST", last)
                        .toString().getBytes(StandardCharsets.UTF_8)));

        JSONObject manifest = new JSONObject();
        manifest.put("PROJECT", "wasm-demo");
        manifest.put("TYPE", "wasm");
        manifest.put("WASM", new JSONObject().put("MODULE", "kernel.wasm").put("TIMEOUT", 30));

        JSONObject body = new JSONObject();
        body.put("PID", "job-wasm-test");
        body.put("CNO", "0");
        body.put("UUID", uuid);
        body.put("FILES", files);
        body.put("MANIFEST", manifest);
        return body;
    }

    @Test
    @Timeout(60)
    void aModuleReturningSuccessMarksTheChunkFinished() throws Exception {
        // i64.const 0
        process = new ParallelProcess(wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 1000), "127.0.0.1");
        process.run();

        assertTrue(process.success, "status 0 is success");
    }

    @Test
    @Timeout(60)
    void aNonZeroStatusFailsTheChunk() throws Exception {
        // i64.const 3 -- a module reporting its own failure
        process = new ParallelProcess(wasmChunk(moduleReturning(new byte[]{0x42, 3}), 0, 10), "127.0.0.1");
        process.run();

        assertFalse(process.success, "a module that reports failure must not be marked Finished");
    }

    @Test
    @Timeout(60)
    void aTrappingModuleFailsRatherThanKillingTheNode() throws Exception {
        // unreachable -- stands in for any trap: bad memory access, div by zero
        process = new ParallelProcess(wasmChunk(moduleReturning(new byte[]{0x00, 0x42, 0}), 0, 10), "127.0.0.1");
        process.run();

        assertFalse(process.success);
    }

    @Test
    @Timeout(60)
    void noBuildFileIsGeneratedForAWasmChunk() throws Exception {
        process = new ParallelProcess(wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 10), "127.0.0.1");

        assertFalse(new File("proc/" + uuid + "/job-wasm-test/0/build.xml").exists(),
                "generating a build file would mean the Ant path was still in play");
        assertTrue(new File("proc/" + uuid + "/job-wasm-test/0/kernel.wasm").exists(),
                "the module travels in the per-chunk directory like any other input");
    }

    @Test
    @Timeout(60)
    void theChunkRangeReachesTheModule() throws Exception {
        // local.get 1; local.get 0; i64.sub -- returns the iteration count, so a
        // zero-length range is the only one reporting success.
        byte[] countingModule = moduleReturning(new byte[]{0x20, 1, 0x20, 0, (byte) 0x7d});

        process = new ParallelProcess(wasmChunk(countingModule, 500, 500), "127.0.0.1");
        process.run();
        assertTrue(process.success, "an empty range should hand the module first == last");

        ParallelProcess wide = new ParallelProcess(wasmChunk(countingModule, 0, 250), "127.0.0.1");
        wide.run();
        assertFalse(wide.success, "a 250-iteration range must not arrive as an empty one");
    }

    @Test
    @Timeout(60)
    void aMissingModuleFailsTheChunkInsteadOfHanging() throws Exception {
        JSONObject body = wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 10);
        body.getJSONObject("MANIFEST").getJSONObject("WASM").put("MODULE", "absent.wasm");

        process = new ParallelProcess(body, "127.0.0.1");
        process.run();

        assertFalse(process.success);
    }

    @Test
    @Timeout(60)
    void aChunkCancelledWhileQueuedNeverStarts() throws Exception {
        // A module in flight cannot be interrupted, so refusing to start one is
        // the only cancellation the WASM path has. Chunk 0 would fail if it ran
        // -- it returns a non-zero status -- so success means it did not.
        try {
            in.co.s13.sips.lib.loop.EarlyExit exit =
                    new in.co.s13.sips.lib.loop.EarlyExit("job-wasm-test");
            exit.breakAll(0, "found");
            in.co.s13.SIPS.settings.GlobalValues.EARLY_EXIT.put("job-wasm-test", exit);

            process = new ParallelProcess(wasmChunk(moduleReturning(new byte[]{0x42, 3}), 0, 10),
                    "127.0.0.1");
            process.run();

            assertTrue(process.success, "a chunk nobody wants is not a failed chunk");
        } finally {
            in.co.s13.SIPS.settings.GlobalValues.EARLY_EXIT.remove("job-wasm-test");
        }
    }

    @Test
    @Timeout(60)
    void aChunkReadsItsInputFileAndItsResultLandsInTheChunkDirectory() throws Exception {
        // The whole point of the host interface, seen from the node: real bytes
        // in, real bytes out, through the per-chunk directory.
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wasm-io");
        byte[] echo = java.nio.file.Files.readAllBytes(TestModules.hosted(dir, "echo.wasm", 1,
                TestModules.concat(
                        TestModules.readAllInput(TestModules.FIRST_LOCAL),
                        TestModules.i32(0), TestModules.localGet(TestModules.FIRST_LOCAL),
                        TestModules.call(TestModules.OUTPUT_WRITE),
                        TestModules.i64(0))));

        JSONObject body = wasmChunk(echo, 0, 16);
        body.getJSONArray("FILES").put(FilePayload.encode("tile.bin",
                "a tile of pixels".getBytes(StandardCharsets.UTF_8)));
        body.getJSONObject("MANIFEST").getJSONObject("WASM").put("INPUT", "tile.bin");

        process = new ParallelProcess(body, "127.0.0.1");
        process.run();

        assertTrue(process.success);
        java.nio.file.Path result = java.nio.file.Paths.get("proc", uuid, "job-wasm-test", "0",
                ParallelProcess.DEFAULT_WASM_OUTPUT_FILE);
        assertEquals("a tile of pixels",
                new String(java.nio.file.Files.readAllBytes(result), StandardCharsets.UTF_8));
    }

    @Test
    @Timeout(60)
    void aModuleCanStopTheWholeJobFromInsideAChunk() throws Exception {
        // break_all reaching the node's own early-exit state is what makes a
        // WASM search chunk a first-class citizen rather than a calculator.
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wasm-break");
        byte[] finds = java.nio.file.Files.readAllBytes(TestModules.hosted(dir, "finds.wasm", 1,
                TestModules.concat(
                        TestModules.readAllInput(TestModules.FIRST_LOCAL),
                        TestModules.localGet(0), TestModules.i32(0),
                        TestModules.localGet(TestModules.FIRST_LOCAL),
                        TestModules.call(TestModules.BREAK_ALL),
                        TestModules.i64(0))));

        JSONObject body = wasmChunk(finds, 900, 1000);
        body.getJSONArray("FILES").put(FilePayload.encode("key.bin",
                "0xCAFE".getBytes(StandardCharsets.UTF_8)));
        body.getJSONObject("MANIFEST").getJSONObject("WASM").put("INPUT", "key.bin");

        try {
            process = new ParallelProcess(body, "127.0.0.1");
            process.run();

            in.co.s13.sips.lib.loop.EarlyExit exit =
                    in.co.s13.SIPS.settings.GlobalValues.EARLY_EXIT.get("job-wasm-test");
            assertTrue(exit.isStopped());
            assertEquals("0xCAFE",
                    new String((byte[]) exit.result().orElseThrow(), StandardCharsets.UTF_8));
        } finally {
            in.co.s13.SIPS.settings.GlobalValues.EARLY_EXIT.remove("job-wasm-test");
        }
    }

    @Test
    @Timeout(60)
    void aJavaChunkStillGetsItsBuildFile() throws Exception {
        // The WASM path is additive; the existing path must be untouched.
        JSONObject body = wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 10);
        body.getJSONObject("MANIFEST").remove("WASM");
        body.getJSONObject("MANIFEST").put("TYPE", "java");
        body.getJSONObject("MANIFEST").put("MAIN", "Demo");

        process = new ParallelProcess(body, "127.0.0.1");

        assertTrue(new File("proc/" + uuid + "/job-wasm-test/0/build.xml").exists());
    }

    @Test
    @Timeout(60)
    void aManifestWithNoTypeIsStillAJavaJob() throws Exception {
        // Every manifest written before TYPE existed must keep working.
        JSONObject body = wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 10);
        body.getJSONObject("MANIFEST").remove("WASM");
        body.getJSONObject("MANIFEST").remove("TYPE");
        body.getJSONObject("MANIFEST").put("MAIN", "Demo");

        process = new ParallelProcess(body, "127.0.0.1");

        assertTrue(new File("proc/" + uuid + "/job-wasm-test/0/build.xml").exists());
    }

    @Test
    @Timeout(60)
    void aJavaChunkStillRequiresAnEntryClass() throws Exception {
        JSONObject body = wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 10);
        body.getJSONObject("MANIFEST").remove("WASM");
        body.getJSONObject("MANIFEST").put("TYPE", "java");

        assertThrows(org.json.JSONException.class,
                () -> new ParallelProcess(body, "127.0.0.1"),
                "only a WASM job may omit MAIN");
    }

    @Test
    @Timeout(60)
    void aWasmJobWithoutAModuleSaysSo() throws Exception {
        JSONObject body = wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 10);
        body.getJSONObject("MANIFEST").remove("WASM");

        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new ParallelProcess(body, "127.0.0.1"))
                .getMessage().contains("WASM"));
    }

    @Test
    @Timeout(60)
    void anUnknownTypeIsRefusedRatherThanGuessedAt() throws Exception {
        JSONObject body = wasmChunk(moduleReturning(new byte[]{0x42, 0}), 0, 10);
        body.getJSONObject("MANIFEST").put("TYPE", "lambda");

        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new ParallelProcess(body, "127.0.0.1"))
                .getMessage().contains("lambda"));
    }
}
