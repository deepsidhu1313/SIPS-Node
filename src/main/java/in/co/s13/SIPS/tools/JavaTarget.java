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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the {@code target} attribute written into each generated Ant build
 * file.
 *
 * <p>The previous implementation did
 * {@code version.substring(0, version.indexOf('.'))}, which throws
 * StringIndexOutOfBoundsException on a dotless version string such as "17", and
 * pinned every generated build to 1.8 regardless of the JDK actually present.
 */
public final class JavaTarget {

    /** Matches the leading feature number of any JEP 223 version string. */
    private static final Pattern FEATURE = Pattern.compile("^(\\d+)");

    /** Matches legacy "1.x" versions, where the feature number is the second part. */
    private static final Pattern LEGACY = Pattern.compile("^1\\.(\\d+)");

    private JavaTarget() {
    }

    /**
     * @param reportedVersion the value of the {@code java.version} property
     * @return a javac-legal target: "1.8" for Java 8 and below, otherwise the
     *         bare feature number. Falls back to {@link #current()} when the
     *         input cannot be parsed, so a task always compiles against
     *         something valid rather than failing.
     */
    public static String forVersion(String reportedVersion) {
        if (reportedVersion == null || reportedVersion.isBlank()) {
            return current();
        }
        String version = reportedVersion.trim();

        Matcher legacy = LEGACY.matcher(version);
        if (legacy.find()) {
            return toTarget(Integer.parseInt(legacy.group(1)));
        }
        Matcher feature = FEATURE.matcher(version);
        if (feature.find()) {
            return toTarget(Integer.parseInt(feature.group(1)));
        }
        return current();
    }

    /** The target matching the JVM running this node. */
    public static String current() {
        return toTarget(Runtime.version().feature());
    }

    /**
     * javac spells Java 8 and earlier "1.N" and everything from 9 onward "N".
     */
    private static String toTarget(int featureVersion) {
        return featureVersion <= 8 ? "1." + featureVersion : Integer.toString(featureVersion);
    }
}
