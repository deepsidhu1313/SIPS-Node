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

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a node keeps a job's files.
 *
 * <p>This layout was spelled out at about forty call sites, which is how it came
 * to be wrong in two ways at once and why fixing it in one place fixed it
 * nowhere else.
 */
class JobPathsTest {

    private static final String JOB = "job-1";
    private static final String NODE = "3f2a-node";

    @Test
    void aChunkStagingDirectoryCanExistOnWindows() {
        // It could not. The name was "<uuid>:CN:<n>", and a colon is not a legal
        // filename character on Windows -- the directory was never created, no
        // chunk was ever staged, and nothing said why.
        String staging = JobPaths.chunkStaging(JOB, NODE, 7);

        assertDoesNotThrow(() -> Path.of(staging),
                staging + " must be a name every platform can create");
        assertEquals(-1, staging.indexOf(':'),
                staging + " still contains a colon");
        assertTrue(staging.contains(NODE + JobPaths.CHUNK_MARKER + "7"), staging);
    }

    @Test
    void everyPathUsesThisPlatformsSeparatorOnly() {
        char foreign = File.separatorChar == '/' ? '\\' : '/';
        for (String path : new String[]{
            JobPaths.job(JOB), JobPaths.manifest(JOB), JobPaths.source(JOB),
            JobPaths.chunkStaging(JOB, NODE, 0), JobPaths.chunkSource(JOB, NODE, 0),
            JobPaths.chunkWorkingDirectory(NODE, JOB, 3), JobPaths.cache(NODE, "project")}) {
            assertEquals(-1, path.indexOf(foreign), path + " mixes separators");
        }
    }

    @Test
    void aChunksSourcesSitInsideItsStagingDirectory() {
        assertTrue(JobPaths.chunkSource(JOB, NODE, 2)
                .startsWith(JobPaths.chunkStaging(JOB, NODE, 2)));
    }

    @Test
    void everythingForAJobSitsUnderItsOwnDirectory() {
        String job = JobPaths.job(JOB);

        assertTrue(JobPaths.manifest(JOB).startsWith(job));
        assertTrue(JobPaths.source(JOB).startsWith(job));
        assertTrue(JobPaths.chunkStaging(JOB, NODE, 0).startsWith(job));
    }

    @Test
    void aFileIsSentUnderItsNameFromSrcOnward() {
        // Was path.substring(path.lastIndexOf("/src/")), which finds nothing on
        // Windows and then hands substring a -1.
        String staged = JobPaths.chunkSource(JOB, NODE, 0)
                + File.separator + "pkg" + File.separator + "Main.java";

        assertEquals("src/pkg/Main.java", JobPaths.nameForTransfer(staged));
    }

    @Test
    void aTransferNameLooksTheSameWhicheverPlatformStagedIt() {
        // It is read by a node that may be running a different operating system.
        assertEquals("src/pkg/Main.java",
                JobPaths.nameForTransfer("C:\\sips\\data\\job\\dist\\n-CN-0\\src\\pkg\\Main.java"));
        assertEquals("src/pkg/Main.java",
                JobPaths.nameForTransfer("/sips/data/job/dist/n-CN-0/src/pkg/Main.java"));
    }

    @Test
    void aCachedFileIsScopedToTheNodeItCameFrom() {
        String cached = JobPaths.cache(NODE, "project", "pkg/Main.java");

        assertTrue(cached.startsWith(Path.of(JobPaths.CACHE, NODE).toString()), cached);
        assertTrue(cached.endsWith(Path.of("pkg", "Main.java").toString()), cached);
    }
}
