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
package in.co.s13.SIPS.executor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * The frames a dialled-in worker and its master exchange.
 *
 * <p>Length-prefixed, like every other message in SIPS, so the same reasoning
 * about truncation applies: a short read is a failure rather than a silently
 * empty message.
 *
 * <p>Split out from both ends because the two sides must agree exactly, and a
 * protocol described twice is a protocol that will eventually be described
 * differently.
 */
final class WorkerFrames {

    /** The worker introducing itself, once, immediately after connecting. */
    static final String HELLO = "hello";

    /** A chunk travelling down to the worker. */
    static final String WORK = "work";

    /** A finished chunk travelling back up. */
    static final String RESULT = "result";

    /** A chunk the worker could not do, so it can be re-issued now. */
    static final String FAILED = "failed";

    /** The largest frame accepted, so a bad length cannot exhaust memory. */
    static final int MAX_FRAME_BYTES = 64 * 1024 * 1024;

    private WorkerFrames() {
    }

    static void write(DataOutputStream out, JSONObject frame, byte[] payload)
            throws IOException {
        byte[] header = frame.toString().getBytes(StandardCharsets.UTF_8);
        synchronized (out) {
            out.writeInt(header.length);
            out.write(header);
            out.writeInt(payload == null ? 0 : payload.length);
            if (payload != null) {
                out.write(payload);
            }
            out.flush();
        }
    }

    /** A frame and whatever bytes came with it. */
    record Frame(JSONObject header, byte[] payload) {

        String type() {
            return header.optString("TYPE", "");
        }
    }

    static Frame read(DataInputStream in) throws IOException {
        int headerLength = in.readInt();
        if (headerLength < 1 || headerLength > MAX_FRAME_BYTES) {
            throw new IOException("Nonsensical frame header length: " + headerLength);
        }
        byte[] header = new byte[headerLength];
        in.readFully(header);

        int payloadLength = in.readInt();
        if (payloadLength < 0 || payloadLength > MAX_FRAME_BYTES) {
            throw new IOException("Nonsensical frame payload length: " + payloadLength);
        }
        byte[] payload = new byte[payloadLength];
        in.readFully(payload);

        try {
            return new Frame(new JSONObject(new String(header, StandardCharsets.UTF_8)), payload);
        } catch (org.json.JSONException notJson) {
            throw new IOException("Unreadable frame header", notJson);
        }
    }
}
