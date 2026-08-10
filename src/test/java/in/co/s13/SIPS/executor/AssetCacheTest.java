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

import in.co.s13.SIPS.transfer.FilePayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeping a model on the node that already has it.
 *
 * <p>Inference sends the same model to every worker for every batch, and eight
 * chunks of one model used to mean eight base64 copies inside eight task
 * payloads. Keyed by content rather than by name, so the same model referenced
 * by two jobs is one copy on disk and a stale name can never resolve to the
 * wrong bytes.
 */
class AssetCacheTest {

    private static final byte[] MODEL = "the weights".getBytes(StandardCharsets.UTF_8);

    @AfterEach
    void clean() {
        AssetCache.forget(FilePayload.checksum(MODEL));
        AssetCache.forget(FilePayload.checksum(big()));
    }

    @Test
    void storesAnAssetUnderItsContentAddress() throws IOException {
        String checksum = AssetCache.store(MODEL);

        assertEquals(FilePayload.checksum(MODEL), checksum);
        assertTrue(AssetCache.has(checksum));
        assertArrayEquals(MODEL, Files.readAllBytes(AssetCache.path(checksum)));
    }

    @Test
    void anAssetAlreadyHeldIsNotStoredAgain() throws IOException {
        // The point of the whole exercise: the second chunk to want this model
        // costs nothing, and the eighth costs nothing.
        String checksum = AssetCache.store(MODEL);
        long firstWrite = Files.getLastModifiedTime(AssetCache.path(checksum)).toMillis();

        assertTrue(AssetCache.has(checksum));
        assertEquals(firstWrite,
                Files.getLastModifiedTime(AssetCache.path(checksum)).toMillis(),
                "an asset already held should not be rewritten");
    }

    @Test
    void refusesBytesThatAreNotWhatTheyClaimToBe() throws IOException {
        // A corrupt transfer stored under a checksum that says it is good
        // would be served to every later chunk, so one bad fetch would poison
        // every job on this node until someone cleared the cache by hand.
        String honest = FilePayload.checksum(MODEL);

        assertThrows(IOException.class,
                () -> AssetCache.store(honest, "different bytes".getBytes(StandardCharsets.UTF_8)));
        assertFalse(AssetCache.has(honest),
                "nothing should be left behind under a name it did not earn");
    }

    @Test
    void anAssetIsPlacedWhereAChunkExpectsIt() throws IOException {
        // The chunk asked for "model.bin"; the cache holds it under a
        // checksum. Something has to put it back under the name the job uses.
        String checksum = AssetCache.store(MODEL);
        Path chunkDirectory = Files.createTempDirectory("asset-cache-test");

        AssetCache.placeInto(checksum, chunkDirectory, "model.bin");

        assertArrayEquals(MODEL, Files.readAllBytes(chunkDirectory.resolve("model.bin")));
        in.co.s13.SIPS.tools.Util.deleteDirectory(chunkDirectory.toFile());
    }

    @Test
    void placingAnAssetIntoASubdirectoryCreatesIt() throws IOException {
        // Job files carry paths like src/models/model.bin, and the chunk
        // directory starts empty.
        String checksum = AssetCache.store(MODEL);
        Path chunkDirectory = Files.createTempDirectory("asset-cache-test");

        AssetCache.placeInto(checksum, chunkDirectory, "src/models/model.bin");

        assertArrayEquals(MODEL,
                Files.readAllBytes(chunkDirectory.resolve("src/models/model.bin")));
        in.co.s13.SIPS.tools.Util.deleteDirectory(chunkDirectory.toFile());
    }

    @Test
    void aNameThatEscapesTheChunkDirectoryIsRefused() throws IOException {
        // The name comes from the task payload, so it is chosen by whoever
        // submitted the job.
        String checksum = AssetCache.store(MODEL);
        Path chunkDirectory = Files.createTempDirectory("asset-cache-test");

        assertThrows(IllegalArgumentException.class,
                () -> AssetCache.placeInto(checksum, chunkDirectory, "../../escaped.bin"));
        in.co.s13.SIPS.tools.Util.deleteDirectory(chunkDirectory.toFile());
    }

    @Test
    void anAssetThatWasNeverStoredIsNotHeld() {
        assertFalse(AssetCache.has(FilePayload.checksum("never stored"
                .getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void placingSomethingTheCacheDoesNotHaveFailsRatherThanWritingNothing() throws IOException {
        Path chunkDirectory = Files.createTempDirectory("asset-cache-test");

        assertThrows(IOException.class, () -> AssetCache.placeInto(
                FilePayload.checksum("absent".getBytes(StandardCharsets.UTF_8)),
                chunkDirectory, "model.bin"));
        in.co.s13.SIPS.tools.Util.deleteDirectory(chunkDirectory.toFile());
    }

    @Test
    void aChecksumThatIsNotOneIsRefused() {
        // The key names a file on disk, so anything that is not a plain hex
        // digest has no business being turned into a path.
        assertThrows(IllegalArgumentException.class, () -> AssetCache.has("../../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> AssetCache.has(""));
        assertThrows(IllegalArgumentException.class, () -> AssetCache.has(null));
    }

    @Test
    void resolvingAnOrdinaryPayloadJustDecodesIt() throws IOException {
        // Most chunks ship a few kilobytes of source and should not touch the
        // cache or the network at all.
        JSONObject payload = FilePayload.encode("Main.java",
                "class Main {}\n".getBytes(StandardCharsets.UTF_8));

        byte[] resolved = AssetCache.resolve(payload, checksum -> {
            throw new IOException("should not have been asked to fetch anything");
        });

        assertArrayEquals("class Main {}\n".getBytes(StandardCharsets.UTF_8), resolved);
    }

    @Test
    void aReferencedAssetIsFetchedOnceAndReusedAfterwards() throws IOException {
        // The whole point. Eight chunks of one model on one node should cost
        // one transfer, not eight.
        byte[] model = big();
        JSONObject payload = FilePayload.encode("model.bin", model);
        List<String> fetches = new ArrayList<>();
        AssetCache.Fetcher counting = checksum -> {
            fetches.add(checksum);
            return model;
        };

        byte[] first = AssetCache.resolve(payload, counting);
        byte[] second = AssetCache.resolve(payload, counting);

        assertArrayEquals(model, first);
        assertArrayEquals(model, second);
        assertEquals(1, fetches.size(), "the second chunk should not re-fetch the model");
    }

    @Test
    void aFetchThatReturnsTheWrongBytesIsRefused() throws IOException {
        // The node that served it is the one that named it, so a mismatch
        // means a corrupt transfer -- and caching it would serve the same
        // corruption to every later chunk.
        JSONObject payload = FilePayload.encode("model.bin", big());

        assertThrows(IOException.class, () -> AssetCache.resolve(payload,
                checksum -> "not the model at all".getBytes(StandardCharsets.UTF_8)));
    }

    /** A payload big enough to travel as a reference. */
    private static byte[] big() {
        byte[] model = new byte[FilePayload.MAX_INLINE_BYTES + 1];
        for (int i = 0; i < model.length; i++) {
            model[i] = (byte) (i % 251);
        }
        return model;
    }
}
