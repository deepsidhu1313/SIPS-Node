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

        pump = new Thread(() -> serve(in, out), "outbound-worker-" + workerId);
        pump.setDaemon(true);
        pump.start();
    }

    private void serve(DataInputStream in, DataOutputStream out) {
        while (!closing) {
            WorkerFrames.Frame frame;
            try {
                frame = WorkerFrames.read(in);
            } catch (IOException hungUp) {
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
}
