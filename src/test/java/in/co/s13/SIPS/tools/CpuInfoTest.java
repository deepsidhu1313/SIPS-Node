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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPU model detection.
 *
 * <p>{@code Util.getCPUName()} only handled Windows and Linux, so macOS and
 * Solaris nodes reported an empty CPU name. That name feeds the benchmark
 * record which the schedulers rank nodes by, so an empty value quietly skewed
 * scheduling.
 */
class CpuInfoTest {

    @Test
    void everySupportedPlatformHasALookupCommand() {
        for (Platform platform : List.of(Platform.WINDOWS, Platform.MACOS,
                Platform.LINUX, Platform.SOLARIS)) {
            List<String> command = CpuInfo.command(platform);
            assertFalse(command.isEmpty(), platform + " must have a CPU lookup command");
        }
    }

    @Test
    void macUsesSysctl() {
        assertEquals(List.of("sysctl", "-n", "machdep.cpu.brand_string"),
                CpuInfo.command(Platform.MACOS));
    }

    @Test
    void solarisUsesPsrinfo() {
        assertTrue(CpuInfo.command(Platform.SOLARIS).contains("psrinfo"));
    }

    @Test
    void parsesLinuxCpuinfo() {
        String output = """
                        processor	: 0
                        vendor_id	: GenuineIntel
                        model name	: Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz
                        cpu MHz		: 2600.000
                        """;
        assertEquals("Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz",
                CpuInfo.parse(Platform.LINUX, output));
    }

    @Test
    void parsesMacSysctlOutput() {
        assertEquals("Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz",
                CpuInfo.parse(Platform.MACOS, "Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz\n"));
    }

    @Test
    void parsesWindowsOutputIgnoringTheHeader() {
        assertEquals("Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz",
                CpuInfo.parse(Platform.WINDOWS, "Name\r\nIntel(R) Core(TM) i7-9750H CPU @ 2.60GHz\r\n\r\n"));
    }

    @Test
    void parsesSolarisPsrinfoOutput() {
        String output = """
                        The physical processor has 8 cores and 16 virtual processors
                          Intel(R) Xeon(R) CPU E5-2660 v4 @ 2.00GHz
                        """;
        assertEquals("Intel(R) Xeon(R) CPU E5-2660 v4 @ 2.00GHz",
                CpuInfo.parse(Platform.SOLARIS, output));
    }

    @Test
    void emptyOutputYieldsUnknownRatherThanBlank() {
        // A blank name silently skews node ranking; an explicit marker does not.
        assertEquals("Unknown CPU", CpuInfo.parse(Platform.LINUX, ""));
        assertEquals("Unknown CPU", CpuInfo.parse(Platform.MACOS, "   \n "));
        assertEquals("Unknown CPU", CpuInfo.parse(Platform.LINUX, null));
    }

    /**
     * Runs against the machine actually executing the suite.
     */
    @Test
    void detectsSomethingOnThisHost() {
        String name = CpuInfo.detect();
        assertFalse(name.isBlank(), "CPU name must never be blank");
    }
}
