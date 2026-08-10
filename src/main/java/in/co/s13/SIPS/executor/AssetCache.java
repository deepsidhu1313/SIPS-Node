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
import in.co.s13.SIPS.transfer.FilePayload;
import in.co.s13.SIPS.transfer.SafePath;
import in.co.s13.sips.lib.common.SipsPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.json.JSONObject;

/**
 * Files a node keeps because it will be asked for them again.
 *
 * <p>Every chunk carries its file set inside the task payload, which is right
 * for the few kilobytes of source most chunks ship. A model is not that:
 * inference sends the same weights to every worker for every batch, and eight
 * chunks of a 50 MB model used to mean eight base64 copies through JSON. Files
 * over {@link FilePayload#MAX_INLINE_BYTES} now travel as a reference, and this
 * is what a reference resolves against.
 *
 * <h2>Addressed by content</h2>
 *
 * <p>Keyed by the checksum of the bytes rather than by a name, so the same
 * model referenced from two jobs is one copy on disk, a name can never resolve
 * to stale bytes, and there is nothing to invalidate — an asset either is the
 * bytes it claims to be or is not in here at all.
 *
 * <p>Which is why {@link #store(String, byte[])} verifies before it keeps
 * anything. An asset written under a checksum it does not match would be
 * handed to every later chunk that asks, so one corrupt fetch would poison
 * every job on this node until someone cleared the cache by hand.
 *
 * <h2>Concurrency</h2>
 *
 * <p>Two chunks arriving at once can both find an asset absent and both fetch
 * it, so this saves a transfer per <em>chunk after the first</em> rather than
 * strictly one transfer per node. Left that way deliberately: the duplicate is
 * one wasted transfer at the start of a job, and the alternative is a lock held
 * across a network fetch, which would stall every other chunk behind whichever
 * sender is slowest to answer. The write is safe either way — both fetches
 * verify, and the move into place is atomic.
 */
public final class AssetCache {

    /** Where assets live, beside the per-sender file cache. */
    public static final String ASSETS = "assets";

    /**
     * How to get an asset this node does not hold.
     *
     * <p>An interface rather than a direct call so resolution can be tested
     * without a cluster, and so the cache knows nothing about sockets.
     */
    @FunctionalInterface
    public interface Fetcher {

        /** @param checksum the content address to fetch, which also verifies it */
        byte[] fetch(String checksum) throws IOException;
    }

    private AssetCache() {
    }

    /**
     * The bytes a task payload stands for, fetching them if this is the first
     * chunk on this node to want them.
     *
     * <p>An ordinary payload carries its content and is simply decoded — most
     * chunks ship a few kilobytes of source and should touch neither the cache
     * nor the network.
     */
    public static byte[] resolve(JSONObject payload, Fetcher fetcher) throws IOException {
        if (!FilePayload.isReference(payload)) {
            return FilePayload.decode(payload);
        }
        String checksum = FilePayload.checksumOf(payload);
        if (!has(checksum)) {
            // store() verifies before keeping, so a corrupt transfer fails
            // here rather than being served to every later chunk.
            store(checksum, fetcher.fetch(checksum));
        }
        return read(checksum);
    }

    /** The directory assets are kept in. */
    public static String directory() {
        return SipsPaths.join(JobPaths.CACHE, ASSETS);
    }

    /** Where an asset lives, if this node has it. */
    public static Path path(String checksum) {
        return Path.of(SipsPaths.join(directory(), validated(checksum)));
    }

    /** Whether this node already holds an asset. */
    public static boolean has(String checksum) {
        return Files.isRegularFile(path(checksum));
    }

    /** Keeps some bytes, returning the address they are held under. */
    public static String store(byte[] content) throws IOException {
        String checksum = FilePayload.checksum(content);
        store(checksum, content);
        return checksum;
    }

    /**
     * Keeps some bytes under the address they were promised to have.
     *
     * @throws IOException if the bytes are not what the checksum says, or
     *         cannot be written
     */
    public static void store(String checksum, byte[] content) throws IOException {
        String expected = validated(checksum);
        String actual = FilePayload.checksum(content);
        if (!expected.equalsIgnoreCase(actual)) {
            throw new IOException("Refusing to cache " + content.length + " bytes as " + expected
                    + ": they are " + actual + ". The transfer was corrupted.");
        }
        Path destination = path(expected);
        if (Files.isRegularFile(destination)) {
            // Already held. Rewriting it would be wasted work at best, and at
            // worst would truncate a file another chunk is reading right now.
            return;
        }
        Files.createDirectories(destination.getParent());

        // Written aside and moved into place, so a crash halfway through
        // cannot leave a partial file under an address that promises whole
        // ones -- the one corruption this design could not otherwise detect.
        Path partial = Files.createTempFile(destination.getParent(), expected, ".partial");
        try {
            Files.write(partial, content);
            Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    /** The bytes of a held asset. */
    public static byte[] read(String checksum) throws IOException {
        Path asset = path(checksum);
        if (!Files.isRegularFile(asset)) {
            throw new IOException("Asset " + checksum + " is not held by this node");
        }
        return Files.readAllBytes(asset);
    }

    /**
     * Puts a held asset where a chunk expects to find it.
     *
     * @param name the path the job knows it by, resolved through
     *        {@link SafePath} because it comes from the task payload
     */
    public static void placeInto(String checksum, Path chunkDirectory, String name)
            throws IOException {
        Path asset = path(checksum);
        if (!Files.isRegularFile(asset)) {
            throw new IOException("Cannot place asset " + checksum
                    + ": this node does not hold it");
        }
        Path destination = SafePath.resolve(chunkDirectory, name);
        Files.createDirectories(destination.getParent());
        Files.copy(asset, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Forgets an asset. Mainly for tests; a cache that is wrong is cleared. */
    public static void forget(String checksum) {
        try {
            Files.deleteIfExists(path(checksum));
        } catch (IOException | IllegalArgumentException ignored) {
            // Nothing useful to do: the asset is either gone or was never
            // addressable, and both mean it is not held.
        }
    }

    /**
     * Checks that a checksum is one, since it names a file on disk.
     *
     * <p>Without this the address is a path fragment from the task payload,
     * and {@code ../../} in it would read and write wherever the node's user
     * can.
     */
    private static String validated(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            throw new IllegalArgumentException("An asset checksum is required");
        }
        String trimmed = checksum.strip();
        for (int i = 0; i < trimmed.length(); i++) {
            char character = trimmed.charAt(i);
            boolean hex = (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F');
            if (!hex) {
                throw new IllegalArgumentException(
                        "Not an asset checksum: '" + checksum + "'");
            }
        }
        return trimmed.toLowerCase(java.util.Locale.ROOT);
    }
}
