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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the host CPU model name, on every supported platform.
 *
 * <p>The name is recorded in the node's benchmark file, which schedulers rank
 * nodes by. The previous implementation covered only Windows and Linux, so
 * macOS and Solaris nodes advertised an empty model.
 */
public final class CpuInfo {

    public static final String UNKNOWN = "Unknown CPU";

    private CpuInfo() {
    }

    /** The command that prints CPU information on the given platform. */
    public static List<String> command(Platform platform) {
        return switch (platform) {
            case WINDOWS -> List.of("cmd.exe", "/c", "wmic cpu get name");
            case MACOS -> List.of("sysctl", "-n", "machdep.cpu.brand_string");
            case LINUX -> List.of("cat", "/proc/cpuinfo");
            case SOLARIS -> List.of("psrinfo", "-pv");
            case UNKNOWN -> List.of();
        };
    }

    /**
     * Extracts the model name from a command's output.
     *
     * @return the model name, or {@link #UNKNOWN} if none could be read
     */
    public static String parse(Platform platform, String output) {
        if (output == null || output.isBlank()) {
            return UNKNOWN;
        }
        List<String> lines = output.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
        if (lines.isEmpty()) {
            return UNKNOWN;
        }
        return switch (platform) {
            case LINUX -> lines.stream()
                    .filter(line -> line.toLowerCase().startsWith("model name"))
                    .map(line -> line.substring(line.indexOf(':') + 1).strip())
                    .findFirst()
                    .orElse(UNKNOWN);
            // wmic echoes a "Name" header before the value.
            case WINDOWS -> lines.stream()
                    .filter(line -> !line.equalsIgnoreCase("Name"))
                    .findFirst()
                    .orElse(UNKNOWN);
            // psrinfo prints a summary line first, then the model.
            case SOLARIS -> lines.get(lines.size() - 1);
            default -> lines.get(0);
        };
    }

    /** Reads the CPU name of the machine this node runs on. */
    public static String detect() {
        Platform platform = Platform.current();
        List<String> command = command(platform);
        if (command.isEmpty()) {
            return UNKNOWN;
        }
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output;
            try (var in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return UNKNOWN;
            }
            return parse(platform, output);
        } catch (IOException ex) {
            Logger.getLogger(CpuInfo.class.getName()).log(Level.WARNING, "Could not read CPU name", ex);
            return UNKNOWN;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return UNKNOWN;
        }
    }
}
