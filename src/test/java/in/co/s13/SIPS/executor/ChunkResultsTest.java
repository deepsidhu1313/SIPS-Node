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

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bringing a small result home with the message that says the chunk finished.
 *
 * <p>A batch job can leave its output on the node that made it. A call cannot —
 * the caller is blocked on the bytes, and a second round trip to collect two
 * kilobytes would cost more than the work did.
 */
class ChunkResultsTest {

    private static final String JOB = "job-results-test";

    @AfterEach
    void clear() {
        ChunkResults.forget(JOB);
        ChunkResults.forget("other-job");
    }

    @Test
    void aSmallResultTravelsWithTheFinishMessage() {
        byte[] thumbnail = "a 2KB thumbnail".getBytes(StandardCharsets.UTF_8);

        ChunkResults.record(JOB, "0", ChunkResults.encode(thumbnail));

        assertArrayEquals(thumbnail, ChunkResults.of(JOB, "0").orElseThrow());
    }

    @Test
    void aLargeResultStaysOnDisk() {
        // Putting a volume through JSON, base64 and a socket sized for status
        // messages would be worse than fetching it properly.
        byte[] volume = new byte[ChunkResults.MAX_INLINE_BYTES + 1];

        assertFalse(ChunkResults.fitsInline(volume));
        assertNull(ChunkResults.encode(volume));
    }

    @Test
    void aResultExactlyAtTheLimitStillTravels() {
        assertTrue(ChunkResults.fitsInline(new byte[ChunkResults.MAX_INLINE_BYTES]));
    }

    @Test
    void aChunkThatProducedNothingCarriesNothing() {
        assertFalse(ChunkResults.fitsInline(new byte[0]));
        assertFalse(ChunkResults.fitsInline(null));
        assertNull(ChunkResults.encode(new byte[0]));
    }

    @Test
    void aMessageWithNoResultLeavesNothingBehind() {
        ChunkResults.record(JOB, "0", null);
        ChunkResults.record(JOB, "1", "");

        assertTrue(ChunkResults.of(JOB, "0").isEmpty());
        assertTrue(ChunkResults.of(JOB, "1").isEmpty());
    }

    @Test
    void aCorruptPayloadIsDroppedRatherThanThrown() {
        // The chunk itself still finished. Losing its status as well because the
        // payload was mangled would be the worse outcome.
        ChunkResults.record(JOB, "0", "not base64 at all!!!");

        assertTrue(ChunkResults.of(JOB, "0").isEmpty());
    }

    @Test
    void resultsAreScopedToTheirOwnJobAndChunk() {
        ChunkResults.record(JOB, "0", ChunkResults.encode(new byte[]{1}));
        ChunkResults.record(JOB, "1", ChunkResults.encode(new byte[]{2}));
        ChunkResults.record("other-job", "0", ChunkResults.encode(new byte[]{3}));

        assertArrayEquals(new byte[]{1}, ChunkResults.of(JOB, "0").orElseThrow());
        assertArrayEquals(new byte[]{2}, ChunkResults.of(JOB, "1").orElseThrow());
        assertArrayEquals(new byte[]{3}, ChunkResults.of("other-job", "0").orElseThrow());
    }

    @Test
    void forgettingAJobDoesNotForgetAnother() {
        // Otherwise a long-running cluster accumulates every result ever
        // returned, and clearing them takes the wrong ones with it.
        ChunkResults.record(JOB, "0", ChunkResults.encode(new byte[]{1}));
        ChunkResults.record("other-job", "0", ChunkResults.encode(new byte[]{2}));

        ChunkResults.forget(JOB);

        assertTrue(ChunkResults.of(JOB, "0").isEmpty());
        assertTrue(ChunkResults.of("other-job", "0").isPresent());
    }

    @Test
    void aStoredResultIsCopiedNotShared() {
        ChunkResults.record(JOB, "0", ChunkResults.encode(new byte[]{1, 2, 3}));

        ChunkResults.of(JOB, "0").orElseThrow()[0] = 99;

        assertArrayEquals(new byte[]{1, 2, 3}, ChunkResults.of(JOB, "0").orElseThrow());
    }

    @Test
    void askingForAResultThatNeverArrivedIsEmpty() {
        assertTrue(ChunkResults.of(JOB, "404").isEmpty());
    }
}
