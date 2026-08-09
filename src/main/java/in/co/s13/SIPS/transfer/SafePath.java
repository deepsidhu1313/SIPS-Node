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

/**
 * Resolves untrusted path fragments inside a directory they may not leave.
 *
 * <p>File names arrive inside the task payload and are chosen by whoever sent
 * the chunk. Concatenating them onto a base directory, as the code used to,
 * lets {@code ../} walk out of the job sandbox and write anywhere the node's
 * user can write.
 */
public final class SafePath {

    private SafePath() {
    }

    /**
     * Resolves {@code untrustedName} inside {@code baseDirectory}.
     *
     * <p>A leading separator is stripped rather than rejected: the distributor
     * sends names like {@code /src/Main.java} that are rooted-looking but mean
     * "relative to the chunk directory".
     *
     * @return the resolved, normalised, absolute path
     * @throws IllegalArgumentException if the name escapes the base directory
     */
    public static Path resolve(Path baseDirectory, String untrustedName) {
        if (untrustedName == null || untrustedName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        Path base = baseDirectory.toAbsolutePath().normalize();

        String relative = untrustedName.strip().replace('\\', '/');
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        Path resolved = base.resolve(relative).normalize();
        // normalize() has collapsed any ".." by now, so a path that still sits
        // outside the base was trying to escape it.
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException(
                    "Refusing path outside the job directory: " + untrustedName);
        }
        return resolved;
    }

    /** Convenience overload for callers holding a directory as a string. */
    public static Path resolve(String baseDirectory, String untrustedName) {
        return resolve(Paths.get(baseDirectory), untrustedName);
    }

    /** Whether the name resolves inside the base directory, without throwing. */
    public static boolean isConfined(Path baseDirectory, String untrustedName) {
        try {
            resolve(baseDirectory, untrustedName);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
