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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Workers that dial in, because they cannot be dialled.
 *
 * <p>Every other path in SIPS assumes a node can be connected to: the master
 * opens a socket to port 13133 and sends a chunk. A phone cannot accept that.
 * Mobile networks put handsets behind carrier NAT, the address changes with the
 * cell, and nothing inbound arrives — which rules out the entire fleet the
 * federated-averaging design exists for.
 *
 * <p>So the worker opens the connection and keeps it, and work travels down a
 * channel the worker established. One connection, many chunks: the alternative
 * is a connect per chunk, and on a radio that is a wake-up and a handshake
 * every time.
 *
 * <p>These run over loopback against the real listener. What loopback cannot
 * show is the thing that motivates it — a carrier NAT, a sleeping radio, a
 * handset that changes address mid-round — so this proves the protocol, not the
 * deployment.
 */
class OutboundWorkerTest {

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

    @Test
    @Timeout(30)
    void aWorkerThatDialsInIsAvailableForWork() throws Exception {
        int port = listening();

        try (OutboundWorker worker = new OutboundWorker("phone-1", "127.0.0.1", port)) {
            worker.connect();

            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
            assertEquals(List.of("phone-1"), master.connected());
        }
    }

    @Test
    @Timeout(30)
    void workDeliveredDownTheWorkersOwnConnectionComesBackAnswered() throws Exception {
        // The whole point: the master never opens a socket to the phone, and
        // the phone still does work.
        int port = listening();

        try (OutboundWorker worker = new OutboundWorker("phone-1", "127.0.0.1", port)) {
            worker.onWork(task -> ("answered " + task.getString("shard"))
                    .getBytes(StandardCharsets.UTF_8));
            worker.connect();
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));

            byte[] result = master.send("phone-1", new JSONObject().put("shard", "3"),
                    10, TimeUnit.SECONDS);

            assertArrayEquals("answered 3".getBytes(StandardCharsets.UTF_8), result);
        }
    }

    @Test
    @Timeout(30)
    void oneConnectionCarriesManyChunks() throws Exception {
        // A connect per chunk is a radio wake-up and a handshake per chunk,
        // which on a phone costs more than the work does.
        int port = listening();

        try (OutboundWorker worker = new OutboundWorker("phone-1", "127.0.0.1", port)) {
            worker.onWork(task -> String.valueOf(task.getInt("n") * 2)
                    .getBytes(StandardCharsets.UTF_8));
            worker.connect();
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));

            for (int n = 1; n <= 5; n++) {
                byte[] result = master.send("phone-1", new JSONObject().put("n", n),
                        10, TimeUnit.SECONDS);
                assertEquals(String.valueOf(n * 2), new String(result, StandardCharsets.UTF_8));
            }
            assertEquals(1, master.connectionsAccepted(),
                    "five chunks should not have cost five connections");
        }
    }

    @Test
    @Timeout(30)
    void aWorkerThatHangsUpStopsBeingOffered() throws Exception {
        // A phone that walks out of range must leave the roster, or the
        // scheduler keeps handing shards to nobody.
        int port = listening();

        try (OutboundWorker worker = new OutboundWorker("phone-1", "127.0.0.1", port)) {
            worker.connect();
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
        }

        assertTrue(master.awaitDeparture("phone-1", 10, TimeUnit.SECONDS));
        assertFalse(master.connected().contains("phone-1"));
    }

    @Test
    @Timeout(30)
    void workForAWorkerThatIsNotThereFailsRatherThanWaits() throws Exception {
        listening();

        assertThrows(IOException.class, () -> master.send("never-connected",
                new JSONObject(), 2, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(30)
    void aWorkerThatFailsOnAChunkReportsItRatherThanGoingSilent() throws Exception {
        // A worker that simply stopped answering would hold the round open
        // until the deadline. Saying "I could not" lets it be re-issued now.
        int port = listening();

        try (OutboundWorker worker = new OutboundWorker("phone-1", "127.0.0.1", port)) {
            worker.onWork(task -> {
                throw new IllegalStateException("out of memory");
            });
            worker.connect();
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));

            IOException failed = assertThrows(IOException.class, () -> master.send("phone-1",
                    new JSONObject(), 10, TimeUnit.SECONDS));

            assertTrue(failed.getMessage().contains("out of memory"), failed.getMessage());
        }
    }

    @Test
    @Timeout(30)
    void aWorkerAnnouncesWhatItIsFitFor() throws Exception {
        // The master needs to know a phone is on battery before it hands it an
        // hour of training, and the phone is the only one who can say.
        int port = listening();

        try (OutboundWorker worker = new OutboundWorker("phone-1", "127.0.0.1", port)) {
            worker.announcing(new JSONObject().put("BATTERY", 87).put("MAINS", false));
            worker.connect();
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));

            Optional<JSONObject> announced = master.announcementOf("phone-1");

            assertEquals(87, announced.orElseThrow().getInt("BATTERY"));
            assertFalse(announced.orElseThrow().getBoolean("MAINS"));
        }
    }

    @Test
    @Timeout(30)
    void twoWorkersAreKeptApart() throws Exception {
        int port = listening();

        try (OutboundWorker one = new OutboundWorker("phone-1", "127.0.0.1", port);
                OutboundWorker other = new OutboundWorker("phone-2", "127.0.0.1", port)) {
            one.onWork(task -> "from one".getBytes(StandardCharsets.UTF_8));
            other.onWork(task -> "from two".getBytes(StandardCharsets.UTF_8));
            one.connect();
            other.connect();
            assertTrue(master.awaitWorker("phone-1", 10, TimeUnit.SECONDS));
            assertTrue(master.awaitWorker("phone-2", 10, TimeUnit.SECONDS));

            assertEquals("from one", new String(master.send("phone-1", new JSONObject(),
                    10, TimeUnit.SECONDS), StandardCharsets.UTF_8));
            assertEquals("from two", new String(master.send("phone-2", new JSONObject(),
                    10, TimeUnit.SECONDS), StandardCharsets.UTF_8));
        }
    }

    @Test
    @Timeout(30)
    void nonsenseIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new OutboundWorker(" ", "127.0.0.1", 1234));
        assertThrows(IllegalArgumentException.class,
                () -> new OutboundWorker("phone", " ", 1234));
    }
}
