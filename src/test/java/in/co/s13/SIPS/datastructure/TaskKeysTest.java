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
package in.co.s13.SIPS.datastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The single source of truth for TASK_DB keys.
 *
 * <p>Regression cover for the defect where {@code TaskHandler}'s "kill" branch
 * looked up {@code uuid + "-ID-" + pid + "c" + cno} while every writer used
 * {@code uuid + "-ID-" + pid + "-CN-" + cno}. The lookup could never hit, so a
 * kill request silently did nothing and the runaway process kept running.
 */
class TaskKeysTest {

    @Test
    void buildsTheCanonicalKey() {
        assertEquals("node-1-ID-job-7-CN-3", TaskKeys.of("node-1", "job-7", "3"));
    }

    @Test
    void acceptsIntegerChunkNumbers() {
        assertEquals("node-1-ID-job-7-CN-3", TaskKeys.of("node-1", "job-7", 3));
    }

    @Test
    void stringAndIntegerChunkFormsAgree() {
        assertEquals(TaskKeys.of("u", "p", 12), TaskKeys.of("u", "p", "12"));
    }

    @Test
    void trimsSurroundingWhitespaceFromNetworkSuppliedValues() {
        // PID and CNO arrive over a socket and have historically carried padding;
        // callers used to .trim() inconsistently, producing two keys for one task.
        assertEquals("u-ID-p-CN-3", TaskKeys.of("  u  ", " p ", " 3 "));
    }

    @Test
    void doesNotCollideAcrossChunks() {
        assertNotEquals(TaskKeys.of("u", "p", 1), TaskKeys.of("u", "p", 2));
    }

    @Test
    void doesNotCollideAcrossSubmitters() {
        assertNotEquals(TaskKeys.of("a", "p", 1), TaskKeys.of("b", "p", 1));
    }

    /**
     * Pins the exact wire format. The old "kill" branch used a "c" separator;
     * if anyone reintroduces that shape this test fails.
     */
    @Test
    void rejectsTheLegacyKillKeyShape() {
        String canonical = TaskKeys.of("u", "p", "3");
        assertNotEquals("u-ID-p" + "c" + "3", canonical);
        assertEquals(true, canonical.contains("-CN-"), "separator must be -CN-");
    }
}
