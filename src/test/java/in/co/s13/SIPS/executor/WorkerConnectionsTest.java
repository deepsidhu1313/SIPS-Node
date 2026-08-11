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
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code awaitWorker} and {@code awaitDeparture} across more than one
 * connection from the same worker id.
 *
 * <p>A dialled-in worker is expected to reconnect over the life of a fleet
 * session — idle timeouts (see {@code OutboundWorker.idleTimeout}) make that
 * the normal case rather than a rare one. Each of these must be a fresh,
 * correctly-observed event, not a cached true from whatever happened the
 * first time this worker id was ever seen.
 */
class WorkerConnectionsTest {

    private WorkerConnections master;

    @AfterEach
    void stop() throws IOException {
        if (master != null) {
            master.close();
        }
    }

    private int listening() throws IOException {
        master = new WorkerConnections(0);
        master.start();
        return master.port();
    }

    /**
     * A bare hello over a fresh socket — no {@code OutboundWorker} involved,
     * so this pins {@code WorkerConnections}' own contract rather than
     * exercising it through another class. Reuses {@link WorkerFrames#write}
     * for the wire format itself; hand-encoding that here would test a
     * frame format nobody actually sends.
     */
    private Socket dial(int port, String workerId) throws IOException {
        Socket socket = new Socket("127.0.0.1", port);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        WorkerFrames.write(out, new JSONObject()
                .put("TYPE", WorkerFrames.HELLO)
                .put("WORKER", workerId)
                .put("ABOUT", new JSONObject()), null);
        return socket;
    }

    @Test
    @Timeout(20)
    void awaitWorkerDoesNotReportAWorkerThatHasNotReconnectedYet() throws Exception {
        // The actual bug: countDown() on an already-zero latch is a silent
        // no-op, so once ANY connection from this id has ever arrived, a
        // latch-based awaitWorker returns true forever after -- including in
        // this gap, where phone-1 is genuinely not connected to anyone. A
        // test that only asserts true-after-reconnect cannot see this: a
        // stale latch would pass it too. Asserting false here is what pins it.
        int port = listening();

        Socket first = dial(port, "phone-1");
        assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
        first.close();
        assertTrue(master.awaitDeparture("phone-1", 10, TimeUnit.SECONDS));

        assertFalse(master.awaitWorker("phone-1", 300, TimeUnit.MILLISECONDS),
                "phone-1 is not connected to anyone right now");
    }

    @Test
    @Timeout(20)
    void awaitWorkerSeesASecondArrivalOfTheSameId() throws Exception {
        int port = listening();

        Socket first = dial(port, "phone-1");
        assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
        first.close();
        assertTrue(master.awaitDeparture("phone-1", 10, TimeUnit.SECONDS));

        Socket second = dial(port, "phone-1");
        try {
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
            assertTrue(master.connected().contains("phone-1"));
        } finally {
            second.close();
        }
    }

    @Test
    @Timeout(20)
    void awaitDepartureDoesNotReportAWorkerThatIsStillConnected() throws Exception {
        // The symmetric case: after the first departure's latch has fired
        // once, a stale implementation reports every LATER connection as
        // already departed too -- including one that is still up.
        int port = listening();

        Socket first = dial(port, "phone-1");
        assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
        first.close();
        assertTrue(master.awaitDeparture("phone-1", 10, TimeUnit.SECONDS));

        Socket second = dial(port, "phone-1");
        try {
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));

            assertFalse(master.awaitDeparture("phone-1", 300, TimeUnit.MILLISECONDS),
                    "phone-1's second connection is still up");
        } finally {
            second.close();
        }
    }

    @Test
    @Timeout(20)
    void awaitDepartureSeesASecondDepartureOfTheSameId() throws Exception {
        int port = listening();

        Socket first = dial(port, "phone-1");
        assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
        first.close();
        assertTrue(master.awaitDeparture("phone-1", 10, TimeUnit.SECONDS));

        Socket second = dial(port, "phone-1");
        assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));

        second.close();
        assertTrue(master.awaitDeparture("phone-1", 10, TimeUnit.SECONDS));
        assertFalse(master.connected().contains("phone-1"));
    }

    @Test
    @Timeout(20)
    void awaitWorkerReturnsImmediatelyIfAlreadyConnected() throws Exception {
        int port = listening();
        Socket socket = dial(port, "phone-1");
        try {
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));

            long start = System.currentTimeMillis();
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
            assertTrue(System.currentTimeMillis() - start < 2000,
                    "a worker already connected should not make the caller wait");
        } finally {
            socket.close();
        }
    }
}
