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

/**
 * Builds the keys used to index {@code GlobalValues.TASK_DB}.
 *
 * <p>Every reader and writer of TASK_DB must go through here. Hand-rolling the
 * key at each call site is what allowed the "kill" command to look up a key
 * shape ({@code -ID-<pid>c<cno>}) that nothing ever wrote, so kill requests
 * were accepted and then silently did nothing.
 */
public final class TaskKeys {

    private static final String SUBMITTER_SEPARATOR = "-ID-";
    private static final String CHUNK_SEPARATOR = "-CN-";

    private TaskKeys() {
    }

    /**
     * @param submitterUUID UUID of the node that submitted the chunk
     * @param jobToken      job identifier, referred to as PID on the wire
     * @param chunkNumber   chunk index, referred to as CNO on the wire
     * @return the canonical TASK_DB key
     */
    public static String of(String submitterUUID, String jobToken, String chunkNumber) {
        return trim(submitterUUID) + SUBMITTER_SEPARATOR + trim(jobToken)
                + CHUNK_SEPARATOR + trim(chunkNumber);
    }

    public static String of(String submitterUUID, String jobToken, int chunkNumber) {
        return of(submitterUUID, jobToken, Integer.toString(chunkNumber));
    }

    /**
     * PID and CNO arrive over a socket and have historically carried padding.
     * Normalising here means a padded and an unpadded request address the same
     * task rather than creating a second, orphaned row.
     */
    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
