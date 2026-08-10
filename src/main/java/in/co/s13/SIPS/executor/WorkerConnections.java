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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;

/**
 * The master's side of workers that dial in.
 *
 * <p>Holds one long-lived connection per worker and sends chunks down it. The
 * master never opens a socket to these workers — it cannot, which is the whole
 * reason they dial — so this is the only channel to them, and it stays up
 * across chunks rather than being rebuilt per chunk.
 *
 * <p>A worker that hangs up leaves the roster immediately. Otherwise the
 * scheduler keeps handing shards to a phone that walked out of range, and every
 * one of them waits out its deadline before anyone notices.
 */
public final class WorkerConnections implements AutoCloseable {

    private final int requestedPort;
    private final Map<String, Connection> workers = new ConcurrentHashMap<>();
    private final Map<String, CountDownLatch> arrivals = new ConcurrentHashMap<>();
    private final Map<String, CountDownLatch> departures = new ConcurrentHashMap<>();
    private final AtomicInteger accepted = new AtomicInteger();
    private final AtomicLong requestIds = new AtomicLong();

    private ServerSocket listener;
    private ExecutorService serving;
    private volatile boolean closing;

    public WorkerConnections(int port) {
        this.requestedPort = port;
    }

    /** One dialled-in worker, and the replies it owes. */
    private static final class Connection {

        private final Socket socket;
        private final DataOutputStream out;
        private final JSONObject announcement;
        private final Map<String, SynchronousQueue<Object>> waiting = new ConcurrentHashMap<>();

        Connection(Socket socket, DataOutputStream out, JSONObject announcement) {
            this.socket = socket;
            this.out = out;
            this.announcement = announcement;
        }
    }

    /** Starts listening. */
    public void start() throws IOException {
        listener = new ServerSocket(requestedPort);
        serving = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "worker-connections");
            thread.setDaemon(true);
            return thread;
        });
        serving.submit(this::accept);
    }

    /** The port actually listening, which matters when zero was asked for. */
    public int port() {
        return listener.getLocalPort();
    }

    private void accept() {
        while (!closing && !listener.isClosed()) {
            try {
                Socket dialled = listener.accept();
                accepted.incrementAndGet();
                serving.submit(() -> welcome(dialled));
            } catch (IOException closed) {
                return;
            }
        }
    }

    private void welcome(Socket dialled) {
        String workerId = null;
        try {
            DataInputStream in = new DataInputStream(dialled.getInputStream());
            DataOutputStream out = new DataOutputStream(dialled.getOutputStream());

            WorkerFrames.Frame hello = WorkerFrames.read(in);
            if (!WorkerFrames.HELLO.equals(hello.type())) {
                dialled.close();
                return;
            }
            workerId = hello.header().getString("WORKER");
            JSONObject about = hello.header().optJSONObject("ABOUT");
            Connection connection = new Connection(dialled, out,
                    about == null ? new JSONObject() : about);
            workers.put(workerId, connection);
            arrivals.computeIfAbsent(workerId, key -> new CountDownLatch(1)).countDown();

            pump(in, connection);
        } catch (IOException hungUp) {
            // Expected whenever a worker goes away, which is the normal end of
            // every one of these connections.
        } finally {
            if (workerId != null) {
                workers.remove(workerId);
                departures.computeIfAbsent(workerId, key -> new CountDownLatch(1)).countDown();
            }
            try {
                dialled.close();
            } catch (IOException alreadyGone) {
                // Nothing to do; it is going away either way.
            }
        }
    }

    /** Reads replies until the worker hangs up. */
    private void pump(DataInputStream in, Connection connection) throws IOException {
        while (!closing) {
            WorkerFrames.Frame frame = WorkerFrames.read(in);
            String request = frame.header().optString("REQUEST", "");
            SynchronousQueue<Object> waiting = connection.waiting.remove(request);
            if (waiting == null) {
                continue;
            }
            if (WorkerFrames.FAILED.equals(frame.type())) {
                waiting.offer(new IOException("Worker could not do the chunk: "
                        + frame.header().optString("REASON", "no reason given")));
            } else {
                waiting.offer(frame.payload());
            }
        }
    }

    /**
     * Sends a chunk to a dialled-in worker and waits for its answer.
     *
     * @throws IOException if the worker is not connected, does not answer in
     *         time, or says it could not do the work
     */
    public byte[] send(String workerId, JSONObject task, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        Connection connection = workers.get(workerId);
        if (connection == null) {
            // Said now rather than waited out: a scheduler holding a shard for
            // a worker that is not there should re-issue it immediately.
            throw new IOException("Worker '" + workerId + "' is not connected");
        }
        String request = String.valueOf(requestIds.incrementAndGet());
        SynchronousQueue<Object> answer = new SynchronousQueue<>();
        connection.waiting.put(request, answer);
        try {
            WorkerFrames.write(connection.out, new JSONObject()
                    .put("TYPE", WorkerFrames.WORK)
                    .put("REQUEST", request)
                    .put("TASK", task), null);

            Object result = answer.poll(timeout, unit);
            if (result == null) {
                throw new IOException("Worker '" + workerId + "' did not answer in time");
            }
            if (result instanceof IOException failed) {
                throw failed;
            }
            return (byte[]) result;
        } finally {
            connection.waiting.remove(request);
        }
    }

    /** The workers currently dialled in. */
    public List<String> connected() {
        return new ArrayList<>(workers.keySet());
    }

    /** What a worker said about itself when it arrived. */
    public Optional<JSONObject> announcementOf(String workerId) {
        Connection connection = workers.get(workerId);
        return connection == null ? Optional.empty() : Optional.of(connection.announcement);
    }

    /** How many connections have been accepted, ever. */
    public int connectionsAccepted() {
        return accepted.get();
    }

    /** Waits for a worker to dial in. */
    public boolean awaitWorker(String workerId, long timeout, TimeUnit unit)
            throws InterruptedException {
        return arrivals.computeIfAbsent(workerId, key -> new CountDownLatch(1))
                .await(timeout, unit);
    }

    /** Waits for a worker to go away. */
    public boolean awaitDeparture(String workerId, long timeout, TimeUnit unit)
            throws InterruptedException {
        return departures.computeIfAbsent(workerId, key -> new CountDownLatch(1))
                .await(timeout, unit);
    }

    @Override
    public void close() throws IOException {
        closing = true;
        if (listener != null) {
            listener.close();
        }
        for (Connection connection : workers.values()) {
            try {
                connection.socket.close();
            } catch (IOException alreadyGone) {
                // Going away either way.
            }
        }
        workers.clear();
        if (serving != null) {
            serving.shutdownNow();
        }
    }
}
