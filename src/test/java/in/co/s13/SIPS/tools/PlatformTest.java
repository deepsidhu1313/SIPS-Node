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
package in.co.s13.SIPS.tools;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Platform detection and per-platform command construction.
 *
 * <p>Regression cover for the defect where {@code ParallelProcess.run()} only
 * built a {@code ProcessBuilder} for Windows and Linux, leaving {@code pb} null
 * on macOS and Solaris and throwing a bare NullPointerException.
 */
class PlatformTest {

    @ParameterizedTest
    @ValueSource(strings = {"Windows 10", "Windows Server 2019", "windows 11"})
    void detectsWindows(String osName) {
        assertEquals(Platform.WINDOWS, Platform.detect(osName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mac OS X", "darwin", "macos"})
    void detectsMac(String osName) {
        assertEquals(Platform.MACOS, Platform.detect(osName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Linux", "FreeBSD", "aix"})
    void detectsUnix(String osName) {
        assertEquals(Platform.LINUX, Platform.detect(osName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SunOS", "Solaris"})
    void detectsSolaris(String osName) {
        assertEquals(Platform.SOLARIS, Platform.detect(osName));
    }

    @Test
    void unrecognisedPlatformIsUnknownRatherThanMisdetected() {
        assertEquals(Platform.UNKNOWN, Platform.detect("TempleOS"));
    }

    @Test
    void nullOsNameDoesNotThrow() {
        assertEquals(Platform.UNKNOWN, Platform.detect(null));
    }

    /**
     * Every POSIX platform must produce a runnable command. macOS and Solaris
     * are the two that previously produced none.
     */
    @ParameterizedTest
    @ValueSource(strings = {"MACOS", "LINUX", "SOLARIS"})
    void posixPlatformsBuildAShellCommand(String name) {
        Platform platform = Platform.valueOf(name);
        List<String> cmd = platform.executorCommand("/opt/sips", "proc/uuid/job/0");

        assertTrue(platform.isPosix(), name + " should be treated as POSIX");
        assertEquals("/bin/sh", cmd.get(0));
        assertEquals("/opt/sips/bin/process-executor.sh", cmd.get(1));
        assertEquals("proc/uuid/job/0", cmd.get(2));
        assertEquals(3, cmd.size());
    }

    @Test
    void windowsBuildsABatchCommandWithNoStrayWhitespace() {
        List<String> cmd = Platform.WINDOWS.executorCommand("C:\\sips", "proc\\uuid\\job\\0");

        assertEquals("cmd.exe", cmd.get(0));
        assertEquals("/c", cmd.get(1));
        assertEquals("C:\\sips\\bin\\process-executor.bat", cmd.get(2));
        assertEquals("proc\\uuid\\job\\0", cmd.get(3));
        // The original code built "bin/process-executor.bat " with a trailing space.
        cmd.forEach(part -> assertEquals(part.strip(), part, "no element may carry padding"));
    }

    @Test
    void unknownPlatformFailsLoudlyInsteadOfNullPointer() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> Platform.UNKNOWN.executorCommand("/opt/sips", "proc/x"));
        assertTrue(thrown.getMessage().toLowerCase().contains("unsupported"),
                "message should name the problem, was: " + thrown.getMessage());
    }
}
