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

import in.co.s13.sips.lib.common.SipsPaths;

/**
 * Where a node keeps a job's files.
 *
 * <p>The layout used to be spelled out at every call site as
 * {@code "data/" + jobToken + "/dist/" + uuid + ":CN:" + n + "/src/"}, in about
 * forty places. That is how it came to be wrong in two different ways at once,
 * and why fixing it anywhere did not fix it anywhere else.
 */
public final class JobPaths {

    /** Everything about a job lives under here. */
    public static final String DATA = "data";

    /** Files fetched from other nodes. */
    public static final String CACHE = "cache";

    /** One directory per chunk being executed. */
    public static final String PROC = "proc";

    /**
     * Separates a node from a chunk number in a staging directory's name.
     *
     * <p>This was {@code ":CN:"}, and a colon is not a legal filename character
     * on Windows — the directory could not be created there at all, so no chunk
     * was ever staged and nothing said why. The name is local to the master:
     * only the part of each file's path from {@code src} onward is sent to a
     * node, so changing it affects nothing on the wire.
     */
    public static final String CHUNK_MARKER = "-CN-";

    private JobPaths() {
    }

    /** A job's directory on the master. */
    public static String job(String jobToken) {
        return SipsPaths.join(DATA, jobToken);
    }

    /** A job's manifest. */
    public static String manifest(String jobToken) {
        return SipsPaths.join(job(jobToken), "manifest.json");
    }

    /** The sources a job was submitted with. */
    public static String source(String jobToken) {
        return SipsPaths.join(job(jobToken), "src");
    }

    /**
     * Where one chunk's copy of the sources is staged before it is uploaded.
     */
    public static String chunkStaging(String jobToken, String nodeUuid, Object chunkNumber) {
        return SipsPaths.join(job(jobToken), "dist", nodeUuid + CHUNK_MARKER + chunkNumber);
    }

    /** The {@code src} directory inside a chunk's staging area. */
    public static String chunkSource(String jobToken, String nodeUuid, Object chunkNumber) {
        return SipsPaths.join(chunkStaging(jobToken, nodeUuid, chunkNumber), "src");
    }

    /** The sandbox a chunk actually executes in. */
    public static String chunkWorkingDirectory(String nodeUuid, String jobToken,
            Object chunkNumber) {
        return SipsPaths.join(PROC, nodeUuid, jobToken, String.valueOf(chunkNumber));
    }

    /** Files fetched from another node, kept per sender. */
    public static String cache(String nodeUuid, String... below) {
        return SipsPaths.join(SipsPaths.join(CACHE, nodeUuid), SipsPaths.canonicalJoin(below));
    }

    /**
     * The name a staged file is sent under: its path from {@code src} onward,
     * in the portable form.
     *
     * <p>Was {@code path.substring(path.lastIndexOf("/src/"))}, which finds
     * nothing on Windows and then hands {@code substring} a -1.
     */
    public static String nameForTransfer(String stagedFilePath) {
        return SipsPaths.canonical("src/"
                + SipsPaths.relativeToAncestor(stagedFilePath, "src").orElse(""));
    }
}
