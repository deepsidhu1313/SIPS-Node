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

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.sips.lib.loop.EarlyExit;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.net.ServerSocketFactory;
import java.net.ServerSocket;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The break commands, delivered over a real socket to a real handler.
 *
 * <p>This is the gap that made {@code breakLoop} useless for years: the client
 * sent a message, and nothing on the node listened. Compiling proves nothing
 * here — only delivering the command and observing the state change does.
 */
class EarlyExitHandlerTest {

    private static final String JOB = "job-earlyexit-test";

    @BeforeEach
    void clearState() {
        GlobalValues.EARLY_EXIT.remove(JOB);
    }

    /** Delivers one command to a TaskHandler and returns its reply. */
    private static String deliver(JSONObject message) throws Exception {
        try (ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0)) {
            Thread node = new Thread(() -> {
                try {
                    new TaskHandler(server.accept()).run();
                } catch (IOException done) {
                    // test finished
                }
            });
            node.setDaemon(true);
            node.start();

            try (Socket client = new Socket("127.0.0.1", server.getLocalPort());
                 DataOutputStream out = new DataOutputStream(client.getOutputStream());
                 DataInputStream in = new DataInputStream(client.getInputStream())) {

                byte[] bytes = message.toString().getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();

                int length = in.readInt();
                byte[] reply = new byte[length];
                in.readFully(reply);
                return new String(reply, StandardCharsets.UTF_8);
            }
        }
    }

    private static JSONObject command(String name, long index, String reason, String value) {
        JSONObject body = new JSONObject();
        body.put("PID", JOB);
        body.put("CNO", "0");
        body.put("INDEX", index);
        body.put("REASON", reason);
        if (value != null) {
            body.put("VALUE", value);
        }
        JSONObject message = new JSONObject();
        message.put("Command", name);
        message.put("Body", body);
        return message;
    }

    @Test
    @Timeout(20)
    void breakAllStopsTheJobAndKeepsTheValue() throws Exception {
        assertEquals("OK", deliver(command("breakAll", 42, "found it", "key-0xCAFE")));

        EarlyExit exit = GlobalValues.EARLY_EXIT.get(JOB);
        assertNotNull(exit, "the handler should have recorded state for the job");
        assertTrue(exit.isStopped());
        assertEquals("key-0xCAFE", exit.result().orElseThrow());
        assertEquals(42L, exit.foundAt().orElseThrow());
        assertFalse(exit.shouldRunChunk(0, 10), "no chunk should still run");
    }

    @Test
    @Timeout(20)
    void breakAfterBoundsTheJobWithoutCancellingThePrefix() throws Exception {
        assertEquals("OK", deliver(command("breakAfter", 100, "converged", null)));

        EarlyExit exit = GlobalValues.EARLY_EXIT.get(JOB);
        assertNotNull(exit);
        assertTrue(exit.shouldRunChunk(0, 50), "the prefix is still part of the answer");
        assertTrue(exit.shouldRunChunk(90, 110), "a straddling chunk must still run");
        assertFalse(exit.shouldRunChunk(101, 200), "beyond the boundary is unwanted");
    }

    @Test
    @Timeout(20)
    void breakAllWithoutAValueStillStops() throws Exception {
        deliver(command("breakAll", 7, "done", null));

        EarlyExit exit = GlobalValues.EARLY_EXIT.get(JOB);
        assertTrue(exit.isStopped());
        assertTrue(exit.result().isEmpty());
    }

    @Test
    @Timeout(20)
    void twoBreakAftersKeepTheTighterBound() throws Exception {
        deliver(command("breakAfter", 500, "first", null));
        deliver(command("breakAfter", 100, "tighter", null));

        assertEquals(100L, GlobalValues.EARLY_EXIT.get(JOB).boundary().orElseThrow());
    }

    @Test
    @Timeout(20)
    void aBreakAllAfterABreakAfterCancelsEverything() throws Exception {
        deliver(command("breakAfter", 100, "converged", null));
        deliver(command("breakAll", 5, "found", "answer"));

        EarlyExit exit = GlobalValues.EARLY_EXIT.get(JOB);
        assertFalse(exit.shouldRunChunk(0, 50),
                "a definite answer makes even the prefix unnecessary");
    }

    @Test
    @Timeout(20)
    void oneJobsBreakDoesNotStopAnother() throws Exception {
        deliver(command("breakAll", 1, "found", "x"));

        assertNotNull(GlobalValues.EARLY_EXIT.get(JOB));
        assertFalse(GlobalValues.EARLY_EXIT.containsKey("some-other-job"),
                "early exit must be scoped to its own job");
    }
}
