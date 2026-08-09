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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Byte-exact transport of file contents inside the task JSON.
 *
 * <p>Files were previously read with {@code Util.readFile}, which uses a
 * {@code FileReader} on the platform default charset, rebuilds the content line
 * by line with the local line separator and appends a trailing newline. That is
 * lossy three ways even for source code — a UTF-8 file crossing machines with
 * different default charsets is corrupted — and destroys binary data outright,
 * which is what blocks image payloads.
 */
class FilePayloadTest {

    @Test
    void roundTripsAsciiText(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("Main.java");
        byte[] original = "class Main {}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);

        assertArrayEquals(original, FilePayload.decode(FilePayload.encode(file)));
    }

    @Test
    void preservesNonAsciiCharacters(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("Unicode.java");
        byte[] original = "// naïve café — ∑ 日本語\nclass A {}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);

        assertArrayEquals(original, FilePayload.decode(FilePayload.encode(file)));
    }

    @Test
    void preservesCarriageReturnsExactly(@TempDir Path dir) throws IOException {
        // Much of this codebase is CRLF. Rewriting line endings in transit
        // changes the bytes the remote node compiles.
        Path file = dir.resolve("Crlf.java");
        byte[] original = "line one\r\nline two\r\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);

        assertArrayEquals(original, FilePayload.decode(FilePayload.encode(file)));
    }

    @Test
    void doesNotInventATrailingNewline(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("NoEol.txt");
        byte[] original = "no trailing newline".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);

        assertArrayEquals(original, FilePayload.decode(FilePayload.encode(file)));
    }

    @Test
    void roundTripsBinaryContent(@TempDir Path dir) throws IOException {
        // A PNG signature followed by bytes that are not valid UTF-8.
        Path file = dir.resolve("tile.png");
        byte[] original = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            (byte) 0xFF, (byte) 0xFE, 0x00, 0x01, (byte) 0xC3, (byte) 0x28};
        Files.write(file, original);

        JSONObject payload = FilePayload.encode(file);
        assertEquals(FilePayload.BASE64, payload.getString(FilePayload.ENCODING));
        assertArrayEquals(original, FilePayload.decode(payload));
    }

    @Test
    void roundTripsAWholeRandomImageSizedBuffer(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("raw.bin");
        byte[] original = new byte[512 * 512 * 3];
        new Random(1313).nextBytes(original);
        Files.write(file, original);

        assertArrayEquals(original, FilePayload.decode(FilePayload.encode(file)));
    }

    @Test
    void marksTextAsUtf8SoItStaysReadableOnTheWire() throws IOException {
        JSONObject payload = FilePayload.encode("Main.java", "class Main {}".getBytes(StandardCharsets.UTF_8));

        assertEquals(FilePayload.UTF8, payload.getString(FilePayload.ENCODING));
        assertEquals("class Main {}", payload.getString(FilePayload.CONTENT));
    }

    @Test
    void carriesTheFileName(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("Alpha.java"), new byte[]{1, 2, 3});
        assertEquals("Alpha.java", FilePayload.encode(file).getString(FilePayload.FILENAME));
    }

    @Test
    void handlesEmptyFiles(@TempDir Path dir) throws IOException {
        Path file = Files.createFile(dir.resolve("Empty.java"));
        assertArrayEquals(new byte[0], FilePayload.decode(FilePayload.encode(file)));
    }

    /**
     * Payloads written by an older node carry no ENCODING field. They must keep
     * working, or a mixed-version cluster breaks.
     */
    @Test
    void decodesLegacyPayloadsWithoutAnEncodingField() {
        JSONObject legacy = new JSONObject();
        legacy.put(FilePayload.FILENAME, "Main.java");
        legacy.put(FilePayload.CONTENT, "class Main {}");

        assertArrayEquals("class Main {}".getBytes(StandardCharsets.UTF_8),
                FilePayload.decode(legacy));
    }

    @Test
    void base64PayloadsSurviveAJsonRoundTrip(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("x.bin"),
                new byte[]{(byte) 0x00, (byte) 0xFF, (byte) 0x80, (byte) 0x7F});

        // Exactly what happens in transit: serialise and reparse.
        JSONObject reparsed = new JSONObject(FilePayload.encode(file).toString());

        assertArrayEquals(Files.readAllBytes(file), FilePayload.decode(reparsed));
    }

    @Test
    void textPayloadsAreNotBase64Encoded(@TempDir Path dir) throws IOException {
        // Base64 inflates by a third; keeping source readable also keeps the
        // task JSON diagnosable by eye.
        Path file = Files.write(dir.resolve("Big.java"),
                "public class Big {}\n".repeat(100).getBytes(StandardCharsets.UTF_8));

        JSONObject payload = FilePayload.encode(file);
        assertEquals(FilePayload.UTF8, payload.getString(FilePayload.ENCODING));
        assertTrue(payload.getString(FilePayload.CONTENT).startsWith("public class Big"));
    }

    @Test
    void detectsBinaryByContentNotByExtension(@TempDir Path dir) throws IOException {
        // A .java file holding invalid UTF-8 must still survive.
        Path file = Files.write(dir.resolve("Weird.java"), new byte[]{'a', (byte) 0xC3, (byte) 0x28});

        JSONObject payload = FilePayload.encode(file);
        assertEquals(FilePayload.BASE64, payload.getString(FilePayload.ENCODING));
        assertArrayEquals(Files.readAllBytes(file), FilePayload.decode(payload));
    }

    @Test
    void isBinaryRecognisesValidUtf8AsText() {
        assertFalse(FilePayload.isBinary("café ∑".getBytes(StandardCharsets.UTF_8)));
        assertTrue(FilePayload.isBinary(new byte[]{(byte) 0xC3, (byte) 0x28}));
        // A NUL byte means binary even though it decodes cleanly.
        assertTrue(FilePayload.isBinary(new byte[]{'a', 0x00, 'b'}));
    }
}
