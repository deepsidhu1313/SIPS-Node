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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.function.Function;
import org.json.JSONObject;

/**
 * A worker that dials the master, because it cannot be dialled.
 *
 * <p>Every other path in SIPS assumes a node can be connected to: the master
 * opens a socket to the task port and pushes a chunk. A phone cannot accept
 * that. Carrier NAT means nothing inbound arrives, and the address changes with
 * the cell — which rules out the entire fleet that federated averaging was
 * invented for, since the private data worth training on is on handsets.
 *
 * <p>So the worker opens the connection and keeps it, and work travels down a
 * channel the worker established. One connection carries many chunks: a connect
 * per chunk is a radio wake-up and a handshake per chunk, which on a phone
 * costs more than the work does.
 *
 * <p>This is the same design for any worker that cannot be reached — a laptop
 * on hotel wifi, a container with no ingress — and phones are only its most
 * demanding case.
 *
 * <h2>Idle disconnect</h2>
 *
 * <p>JPPF's Android node takes the opposite position: it disconnects before
 * <em>every</em> task and reconnects only to submit results and fetch the
 * next one, specifically to conserve battery — a background socket is exactly
 * what Android's Doze and App Standby power management throttles or kills.
 * That is the right instinct and the wrong mechanism for us: reconnecting per
 * chunk reintroduces the radio wake-up and handshake this class exists to
 * avoid, and a shard here is typically seconds to minutes of work, not a
 * microtask. {@link #idleTimeout} is the synthesis — the connection survives
 * a steady stream of chunks and only lets itself go once it has genuinely
 * had nothing to do for a while, which is the situation JPPF's design is
 * actually protecting against.
 */
public final class OutboundWorker implements AutoCloseable {

    private final String workerId;
    private final String masterHost;
    private final int masterPort;

    private volatile Function<JSONObject, byte[]> work = task -> new byte[0];
    private volatile JSONObject announcement = new JSONObject();
    private volatile Socket socket;
    private volatile Thread pump;
    private volatile boolean closing;
    private volatile long idleTimeoutMillis;

    public OutboundWorker(String workerId, String masterHost, int masterPort) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("A worker needs an id the master can address");
        }
        if (masterHost == null || masterHost.isBlank()) {
            throw new IllegalArgumentException("A worker needs a master to dial");
        }
        this.workerId = workerId;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    /** What this worker does with a chunk. */
    public OutboundWorker onWork(Function<JSONObject, byte[]> handler) {
        this.work = handler;
        return this;
    }

    /**
     * What this worker tells the master about itself.
     *
     * <p>Battery, power and temperature: the master needs to know a phone is
     * on battery before handing it an hour of training, and the phone is the
     * only one that can say.
     */
    public OutboundWorker announcing(JSONObject about) {
        this.announcement = about == null ? new JSONObject() : about;
        return this;
    }

    /**
     * How long the connection may sit with no work arriving before it lets
     * itself go. Unset (the default) means the connection is held
     * indefinitely, right for a worker with reliable power.
     */
    public OutboundWorker idleTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("An idle timeout must be a positive "
                    + "duration; to hold the connection indefinitely, do not set one");
        }
        this.idleTimeoutMillis = timeout.toMillis();
        return this;
    }

    /** Dials the master and starts answering work on a background thread. */
    public void connect() throws IOException {
        Socket dialled = new Socket();
        dialled.connect(new InetSocketAddress(masterHost, masterPort), 30_000);
        this.socket = dialled;

        DataOutputStream out = new DataOutputStream(dialled.getOutputStream());
        DataInputStream in = new DataInputStream(dialled.getInputStream());
        WorkerFrames.write(out, new JSONObject()
                .put("TYPE", WorkerFrames.HELLO)
                .put("WORKER", workerId)
                .put("ABOUT", announcement), null);

        // Set only after hello: the handshake itself must not race the idle
        // clock, and connect() already has its own 30s connect timeout.
        long idle = idleTimeoutMillis;
        if (idle > 0) {
            dialled.setSoTimeout((int) idle);
        }

        pump = new Thread(() -> serve(dialled, in, out), "outbound-worker-" + workerId);
        pump.setDaemon(true);
        pump.start();
    }

    private void serve(Socket dialled, DataInputStream in, DataOutputStream out) {
        while (!closing) {
            WorkerFrames.Frame frame;
            try {
                frame = WorkerFrames.read(in);
            } catch (SocketTimeoutException idle) {
                // Nothing arrived while we waited: let go rather than hold a
                // background socket open for nothing. The caller reconnects
                // when it has something to contribute again.
                closeQuietly(dialled);
                return;
            } catch (IOException hungUp) {
                closeQuietly(dialled);
                return;
            }
            if (!WorkerFrames.WORK.equals(frame.type())) {
                continue;
            }
            String request = frame.header().optString("REQUEST", "");
            try {
                JSONObject task = frame.header().optJSONObject("TASK");
                byte[] answer = work.apply(task == null ? new JSONObject() : task);
                WorkerFrames.write(out, new JSONObject()
                        .put("TYPE", WorkerFrames.RESULT)
                        .put("REQUEST", request), answer == null ? new byte[0] : answer);
            } catch (IOException cannotReply) {
                return;
            } catch (RuntimeException | Error failed) {
                // Reported rather than swallowed: a worker that simply stopped
                // answering would hold the round open until its deadline,
                // whereas saying so lets the shard be re-issued now.
                try {
                    WorkerFrames.write(out, new JSONObject()
                            .put("TYPE", WorkerFrames.FAILED)
                            .put("REQUEST", request)
                            .put("REASON", String.valueOf(failed.getMessage())), null);
                } catch (IOException gone) {
                    return;
                }
            }
        }
    }

    /** The id the master knows this worker by. */
    public String workerId() {
        return workerId;
    }

    /** Whether the connection is still up. */
    public boolean connected() {
        Socket current = socket;
        return current != null && current.isConnected() && !current.isClosed();
    }

    @Override
    public void close() throws IOException {
        closing = true;
        Socket current = socket;
        if (current != null) {
            current.close();
        }
        Thread current_pump = pump;
        if (current_pump != null) {
            current_pump.interrupt();
        }
    }

    /**
     * Closes a socket the pump thread is retiring on its own — an idle
     * timeout or a hang-up, neither of which is a request to stop
     * reconnecting for good, so {@code closing} is left alone.
     */
    private static void closeQuietly(Socket dialled) {
        try {
            dialled.close();
        } catch (IOException alreadyGone) {
            // Going away either way.
        }
    }
}
