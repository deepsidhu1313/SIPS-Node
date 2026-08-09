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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.json.JSONObject;

/**
 * Byte-exact encoding of a file for transport inside the task JSON.
 *
 * <p>Task payloads travel as JSON, so content has to survive a string round
 * trip. Text is carried as UTF-8 so source stays readable on the wire and in
 * logs; anything that is not valid UTF-8 text is carried as Base64.
 *
 * <p>The distinction is made from the bytes, never the file extension: a
 * {@code .java} file containing invalid UTF-8 must survive, and a
 * {@code .dat} file that happens to be text need not pay the 33% Base64
 * overhead.
 *
 * <p>Payloads written by older nodes carry no {@link #ENCODING} field and are
 * decoded as UTF-8 text, so a mixed-version cluster keeps working.
 */
public final class FilePayload {

    public static final String FILENAME = "FILENAME";
    public static final String CONTENT = "CONTENT";
    public static final String ENCODING = "ENCODING";

    public static final String UTF8 = "utf-8";
    public static final String BASE64 = "base64";

    private FilePayload() {
    }

    /** Encodes a file, using its file name as the payload name. */
    public static JSONObject encode(Path file) throws IOException {
        return encode(file.getFileName().toString(), Files.readAllBytes(file));
    }

    /**
     * Encodes raw bytes under the given name.
     *
     * @param name    name the receiving node will write the content to
     * @param content exact bytes to transport
     */
    public static JSONObject encode(String name, byte[] content) {
        JSONObject payload = new JSONObject();
        payload.put(FILENAME, name);
        if (isBinary(content)) {
            payload.put(ENCODING, BASE64);
            payload.put(CONTENT, Base64.getEncoder().encodeToString(content));
        } else {
            payload.put(ENCODING, UTF8);
            payload.put(CONTENT, new String(content, StandardCharsets.UTF_8));
        }
        return payload;
    }

    /**
     * Recovers the exact bytes from a payload.
     *
     * @param payload a payload produced by {@link #encode}, or a legacy payload
     *                carrying no {@link #ENCODING} field
     */
    public static byte[] decode(JSONObject payload) {
        String content = payload.optString(CONTENT, "");
        // Legacy payloads predate the encoding field and were always text.
        String encoding = payload.optString(ENCODING, UTF8);
        return BASE64.equalsIgnoreCase(encoding)
                ? Base64.getDecoder().decode(content)
                : content.getBytes(StandardCharsets.UTF_8);
    }

    /** The name the content should be written to. */
    public static String nameOf(JSONObject payload) {
        return payload.optString(FILENAME, "");
    }

    /**
     * Whether these bytes must be Base64 encoded to survive transport.
     *
     * <p>True when the content is not valid UTF-8, or contains a NUL byte.
     * NUL decodes cleanly but never appears in real source, and its presence is
     * the conventional signal that a file is binary.
     */
    public static boolean isBinary(byte[] content) {
        for (byte b : content) {
            if (b == 0) {
                return true;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return false;
        } catch (CharacterCodingException ex) {
            return true;
        }
    }
}
