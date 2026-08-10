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
package in.co.s13.SIPS.executor.sockets.handlers;

import in.co.s13.sips.lib.protocol.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.net.ServerSocketFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A node announcing what it speaks, over a real socket.
 *
 * <p>Everything else about negotiation can be unit tested; whether the version
 * actually reaches the wire cannot. This is the same shape of test as the
 * early-exit handler's: a real socket, a real handler, and the reply parsed the
 * way the pinger parses it.
 */
class PingNegotiationTest {

    @BeforeAll
    static void identifyNode() {
        if (in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID == null) {
            in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID = UUID.randomUUID().toString();
        }
    }

    /** Sends one ping to a real PingHandler and returns its reply. */
    private static JSONObject ping() throws Exception {
        try (ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0)) {
            Thread node = new Thread(() -> {
                try {
                    new PingHandler(server.accept()).run();
                } catch (IOException done) {
                    // test finished
                }
            });
            node.setDaemon(true);
            node.start();

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort());
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    DataInputStream in = new DataInputStream(client.getInputStream())) {

                JSONObject request = new JSONObject();
                request.put("Command", "ping");
                request.put("Body", new JSONObject().put("UUID", "pinging-node"));
                byte[] bytes = request.toString().getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();

                int length = in.readInt();
                byte[] reply = new byte[length];
                in.readFully(reply);
                return new JSONObject(new String(reply, StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    @Timeout(30)
    void anodeAnnouncesItsProtocolOnEveryPing() throws Exception {
        JSONObject reply = ping();

        assertTrue(reply.has(Protocol.FIELD),
                "a peer cannot negotiate with a node that never says what it speaks: " + reply);
        assertEquals(Protocol.VERSION, Protocol.of(reply));
    }

    @Test
    @Timeout(30)
    void theVersionRidesAlongsideWhatANodeAlreadyAdvertises() throws Exception {
        // Carried on the ping rather than a handshake of its own, because ping
        // already runs once per discovery cycle and already says what a node
        // offers. A handshake per connection would cost a round trip on every
        // chunk sent.
        JSONObject reply = ping();

        assertTrue(reply.has("HOSTNAME"), reply.toString());
        assertTrue(reply.has("TASK_LIMIT"), reply.toString());
        assertTrue(reply.has("DEVICES"), reply.toString());
        assertTrue(reply.has(Protocol.FIELD), reply.toString());
    }

    @Test
    @Timeout(30)
    void anOlderPeerReadingThisReplyIsUnaffected() throws Exception {
        // The field is additive. A node from before negotiation reads the
        // fields it knows and never looks at PROTOCOL, so a new node in an old
        // cluster stays usable.
        JSONObject reply = ping();

        assertTrue(reply.has("UUID"));
        assertTrue(reply.has("CPUNAME"));
        assertTrue(reply.has("LIVE_NODES"));
    }
}
