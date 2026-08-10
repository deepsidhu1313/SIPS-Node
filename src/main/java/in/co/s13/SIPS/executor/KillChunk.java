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

import in.co.s13.SIPS.settings.GlobalValues;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * Tells a node to stop running one chunk.
 *
 * <p>The node has understood the {@code kill} command for years; nothing on this
 * side ever sent it, which is why a stage that outlives its timeout could be
 * given up on but not actually stopped. Without this, a chunk on a wedged node
 * keeps a core busy until someone notices.
 *
 * <p>Best effort by design. A node that has already finished, crashed or gone
 * off the network is not an error worth propagating — the point is to stop what
 * can be stopped, not to guarantee it.
 */
public final class KillChunk {

    private KillChunk() {
    }

    /**
     * Asks {@code host} to stop the given chunk.
     *
     * @param nodeUuid the node that was given the chunk, as the task table keys it
     * @return whether the node acknowledged
     */
    public static boolean send(String host, String jobToken, String chunkNumber, String nodeUuid) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String target = host.contains("%") ? host.substring(0, host.indexOf('%')) : host;

        JSONObject body = new JSONObject();
        body.put("PID", jobToken);
        body.put("CNO", chunkNumber);
        body.put("UUID", nodeUuid);
        JSONObject request = new JSONObject();
        request.put("Command", "kill");
        request.put("Body", body);

        try (Socket socket = new Socket(target, GlobalValues.TASK_SERVER_PORT);
                OutputStream os = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(os);
                DataInputStream in = new DataInputStream(socket.getInputStream())) {

            byte[] bytes = request.toString().getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();

            int length = in.readInt();
            byte[] reply = new byte[Math.max(0, length)];
            if (length > 0) {
                in.readFully(reply);
            }
            return "OK".equals(new String(reply, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            // A node we cannot reach is a node that is not running our chunk.
            return false;
        }
    }
}
