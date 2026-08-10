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

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small chunk results carried home inside the finish message.
 *
 * <p>A batch job leaves its output on the node that produced it and someone
 * fetches it later. A call cannot: the caller is blocked waiting for bytes, and
 * a second round trip to collect a two-kilobyte answer would cost more than the
 * work did.
 *
 * <p>So results below {@link #MAX_INLINE_BYTES} ride along with the message that
 * says the chunk finished. Anything larger stays on disk where the existing
 * file-fetch path can reach it — inlining a volume would put it through JSON,
 * base64 and a socket that was sized for status messages.
 */
public final class ChunkResults {

    /**
     * The largest result carried inline.
     *
     * <p>Big enough for what a call actually returns — a thumbnail, an
     * inference result, a checksum — and small enough that base64 through a
     * status message stays reasonable.
     */
    public static final int MAX_INLINE_BYTES = 256 * 1024;

    /** Results waiting to be collected, keyed by job token and chunk number. */
    private static final ConcurrentHashMap<String, byte[]> RESULTS = new ConcurrentHashMap<>();

    private ChunkResults() {
    }

    /** Whether a result of this size travels inline. */
    public static boolean fitsInline(byte[] result) {
        return result != null && result.length > 0 && result.length <= MAX_INLINE_BYTES;
    }

    /** Encodes a result for the finish message, or null if it does not fit. */
    public static String encode(byte[] result) {
        return fitsInline(result) ? Base64.getEncoder().encodeToString(result) : null;
    }

    /** Records a result that arrived with a finish message. */
    public static void record(String jobToken, String chunkNumber, String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return;
        }
        try {
            RESULTS.put(key(jobToken, chunkNumber), Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException ex) {
            // Corrupt payloads are dropped rather than thrown: the chunk itself
            // still finished, and losing its status too would be worse.
            RESULTS.remove(key(jobToken, chunkNumber));
        }
    }

    /** The result of a chunk, if one came back inline. */
    public static Optional<byte[]> of(String jobToken, String chunkNumber) {
        byte[] result = RESULTS.get(key(jobToken, chunkNumber));
        return Optional.ofNullable(result).map(byte[]::clone);
    }

    /** Forgets a job's results, once whoever wanted them has them. */
    public static void forget(String jobToken) {
        RESULTS.keySet().removeIf(key -> key.startsWith(jobToken + "-CN-"));
    }

    /** How many results are being held. */
    public static int size() {
        return RESULTS.size();
    }

    private static String key(String jobToken, String chunkNumber) {
        return jobToken + "-CN-" + chunkNumber;
    }
}
