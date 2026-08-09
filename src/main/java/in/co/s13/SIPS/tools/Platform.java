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
import java.util.Locale;

/**
 * The host operating system, and how to launch a task executor script on it.
 *
 * <p>Replaces the {@code GlobalValues.OS_Name} integer, which encoded macOS as
 * 1 and Solaris as 3 but was only ever switched on for 0 (Windows) and 2
 * (Linux). On any other platform the ProcessBuilder was left null and task
 * execution died with a bare NullPointerException.
 */
public enum Platform {

    WINDOWS("\\", "process-executor.bat"),
    MACOS("/", "process-executor.sh"),
    LINUX("/", "process-executor.sh"),
    SOLARIS("/", "process-executor.sh"),
    UNKNOWN("/", null);

    /**
     * POSIX sh rather than bash: Solaris and minimal container images do not
     * ship bash, and the executor script is deliberately POSIX-clean.
     */
    private static final String POSIX_SHELL = "/bin/sh";

    private final String separator;
    private final String scriptName;

    Platform(String separator, String scriptName) {
        this.separator = separator;
        this.scriptName = scriptName;
    }

    /**
     * Classifies the value of the {@code os.name} system property.
     *
     * @return the matching platform, or {@link #UNKNOWN} if unrecognised.
     *         Never null, and never guesses.
     */
    public static Platform detect(String osName) {
        if (osName == null || osName.isBlank()) {
            return UNKNOWN;
        }
        String name = osName.toLowerCase(Locale.ROOT);
        // Checked before Windows on purpose: "darwin" contains "win".
        if (name.contains("mac") || name.contains("darwin")) {
            return MACOS;
        }
        if (name.contains("windows")) {
            return WINDOWS;
        }
        if (name.contains("sunos") || name.contains("solaris")) {
            return SOLARIS;
        }
        if (name.contains("nix") || name.contains("nux") || name.contains("aix")
                || name.contains("bsd")) {
            return LINUX;
        }
        return UNKNOWN;
    }

    /** Classifies the platform this JVM is running on. */
    public static Platform current() {
        return detect(System.getProperty("os.name"));
    }

    public boolean isPosix() {
        return this == MACOS || this == LINUX || this == SOLARIS;
    }

    public boolean isSupported() {
        return this != UNKNOWN;
    }

    /**
     * Builds the command that runs one task chunk.
     *
     * @param workingDir installation root holding {@code bin/}
     * @param jobLocation path of the chunk directory, passed to the script
     * @return an argument list ready for ProcessBuilder, never null
     * @throws IllegalStateException if the platform is not supported, so the
     *         failure names itself instead of surfacing as a NullPointerException
     */
    public List<String> executorCommand(String workingDir, String jobLocation) {
        if (!isSupported()) {
            throw new IllegalStateException("Unsupported operating system: "
                    + System.getProperty("os.name")
                    + ". SIPS supports Windows, macOS, Linux and Solaris.");
        }
        String script = workingDir + separator + "bin" + separator + scriptName;
        return this == WINDOWS
                ? List.of("cmd.exe", "/c", script, jobLocation)
                : List.of(POSIX_SHELL, script, jobLocation);
    }
}
