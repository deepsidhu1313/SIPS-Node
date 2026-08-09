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
package in.co.s13.SIPS.transfer;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confinement of network-supplied paths to their job directory.
 *
 * <p>File names arrive inside the task payload and are chosen by whoever sent
 * the chunk. Resolving them against a base directory without checking lets
 * {@code ../} escape it and write anywhere the node's user can write.
 */
class SafePathTest {

    @Test
    void resolvesAnOrdinaryRelativeName(@TempDir Path base) {
        Path resolved = SafePath.resolve(base, "src/Main.java");
        assertTrue(resolved.startsWith(base.toAbsolutePath().normalize()));
        assertTrue(resolved.endsWith(Paths.get("src", "Main.java")));
    }

    @Test
    void toleratesTheLeadingSlashTheDistributorSends(@TempDir Path base) {
        // Distributor sends "/src/Main.java" — rooted-looking, but meant to be
        // relative to the chunk directory.
        Path resolved = SafePath.resolve(base, "/src/Main.java");
        assertTrue(resolved.startsWith(base.toAbsolutePath().normalize()));
        assertTrue(resolved.endsWith(Paths.get("src", "Main.java")));
    }

    @Test
    void rejectsParentTraversal(@TempDir Path base) {
        assertThrows(IllegalArgumentException.class,
                () -> SafePath.resolve(base, "../../../etc/passwd"));
    }

    @Test
    void rejectsTraversalHiddenMidPath(@TempDir Path base) {
        assertThrows(IllegalArgumentException.class,
                () -> SafePath.resolve(base, "src/../../../../tmp/evil.sh"));
    }

    /**
     * An absolute-looking name is reinterpreted as relative and confined rather
     * than rejected. It cannot be rejected outright — the distributor legitimately
     * sends "/src/Main.java" — and confining it is equally safe: the write lands
     * harmlessly inside the chunk directory instead of at the filesystem root.
     */
    @Test
    void treatsAnAbsolutePathAsRelativeAndConfinesIt(@TempDir Path base) {
        Path resolved = SafePath.resolve(base, "/etc/cron.d/backdoor");

        assertTrue(resolved.startsWith(base.toAbsolutePath().normalize()),
                "must not escape the job directory");
        assertEquals(false, resolved.equals(Paths.get("/etc/cron.d/backdoor")));
    }

    @Test
    void allowsTraversalThatStaysInside(@TempDir Path base) {
        // "src/../src/Main.java" normalises to "src/Main.java", still confined.
        Path resolved = SafePath.resolve(base, "src/../src/Main.java");
        assertTrue(resolved.endsWith(Paths.get("src", "Main.java")));
    }

    @Test
    void rejectsNullAndBlankNames(@TempDir Path base) {
        assertThrows(IllegalArgumentException.class, () -> SafePath.resolve(base, null));
        assertThrows(IllegalArgumentException.class, () -> SafePath.resolve(base, "  "));
    }

    @Test
    void isConfinedAnswersWithoutThrowing(@TempDir Path base) {
        assertTrue(SafePath.isConfined(base, "src/Main.java"));
        assertEquals(false, SafePath.isConfined(base, "../escape.txt"));
    }
}
