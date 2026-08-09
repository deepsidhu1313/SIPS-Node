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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * File deletion.
 *
 * <p>{@code deleteFile} used to build a shell string ("rm -vf " + path, or
 * "cmd /c del /f " + path) and hand it to {@code Runtime.exec(String)}. That is
 * a command injection sink, since paths derive from network-supplied job and
 * chunk identifiers, and it behaved differently on every platform. These tests
 * pin the portable behaviour.
 */
class UtilFileOpsTest {

    @Test
    void deletesAnExistingFile(@TempDir Path dir) throws IOException {
        Path file = Files.createFile(dir.resolve("chunk.out"));
        assertTrue(Util.deleteFile(file.toString()));
        assertFalse(Files.exists(file));
    }

    @Test
    void reportsFailureForAMissingFile(@TempDir Path dir) {
        assertFalse(Util.deleteFile(dir.resolve("never-existed").toString()));
    }

    @Test
    void handlesPathsContainingSpaces(@TempDir Path dir) throws IOException {
        // The old shell-string form silently deleted the wrong thing here.
        Path file = Files.createFile(dir.resolve("my job output.txt"));
        assertTrue(Util.deleteFile(file.toString()));
        assertFalse(Files.exists(file));
    }

    /**
     * The injection case: a crafted job token must not be able to smuggle a
     * second command past the deletion.
     */
    @Test
    void doesNotExecuteShellMetacharactersInThePath(@TempDir Path dir) throws IOException {
        Path canary = Files.createFile(dir.resolve("canary.txt"));
        String malicious = dir.resolve("target.txt") + "; rm -rf " + canary;

        Util.deleteFile(malicious);

        assertTrue(Files.exists(canary), "a shell was invoked and ran the injected command");
    }

    @Test
    void doesNotDeleteANonEmptyDirectory(@TempDir Path dir) throws IOException {
        Path sub = Files.createDirectory(dir.resolve("chunk"));
        Files.createFile(sub.resolve("inner.txt"));

        assertFalse(Util.deleteFile(sub.toString()));
        assertTrue(Files.exists(sub));
    }

    @Test
    void toleratesNullAndBlankPaths() {
        assertFalse(Util.deleteFile(null));
        assertFalse(Util.deleteFile("   "));
    }

    // ---- readFile: content preservation ----

    /**
     * readFile used a FileReader on the platform default charset and rebuilt the
     * content line by line with System.lineSeparator(), so it altered encoding,
     * line endings and the final newline. Job stdout and JSON config both pass
     * through it.
     */
    @Test
    void readFilePreservesUtf8Characters(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("out.log");
        String original = "café ∑ 日本語 done";
        Files.writeString(file, original, java.nio.charset.StandardCharsets.UTF_8);

        assertEquals(original, Util.readFile(file.toString()));
    }

    @Test
    void readFileDoesNotAppendATrailingNewline(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("out.log");
        Files.writeString(file, "no trailing newline");

        assertEquals("no trailing newline", Util.readFile(file.toString()));
    }

    @Test
    void readFilePreservesCarriageReturns(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("out.log");
        Files.writeString(file, "one\r\ntwo\r\n");

        assertEquals("one\r\ntwo\r\n", Util.readFile(file.toString()));
    }

    @Test
    void readFileReturnsEmptyForAMissingFile(@TempDir Path dir) {
        assertEquals("", Util.readFile(dir.resolve("absent").toString()));
    }
}
