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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
 *
 * <p>A worker id is expected to reconnect over the life of a session — an idle
 * timeout ({@code OutboundWorker.idleTimeout}) makes that the normal case
 * rather than a rare one. {@link #awaitWorker} and {@link #awaitDeparture}
 * are therefore polled against the live {@link #workers} map rather than
 * signalled with a per-id {@code CountDownLatch}: a latch fires once and
 * stays fired, so after a worker's first arrival ever, a latch-based
 * {@code awaitWorker} would report it connected forever after — including in
 * the gap after it left and before it came back, which is exactly the window
 * a caller asking the question is trying to detect.
 */
public final class WorkerConnections implements AutoCloseable {

    /** How often a wait for arrival or departure re-checks the live state. */
    private static final long POLL_MILLIS = 15;

    private final int requestedPort;
    private final Map<String, Connection> workers = new ConcurrentHashMap<>();
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
        private final Map<String, BlockingQueue<Object>> waiting = new ConcurrentHashMap<>();

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
        Connection connection = null;
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
            connection = new Connection(dialled, out,
                    about == null ? new JSONObject() : about);
            workers.put(workerId, connection);

            pump(in, connection);
        } catch (IOException hungUp) {
            // Expected whenever a worker goes away, which is the normal end of
            // every one of these connections.
        } finally {
            if (workerId != null && connection != null) {
                // Conditional on this thread's own Connection instance, not
                // just the id: if a newer connection for this id has already
                // replaced this one in the map (a fast reconnect racing this
                // thread's own unwind), this stale finally-block must not
                // evict it. Connection has no equals() override, so this
                // compares by reference -- exactly "is it still mine".
                workers.remove(workerId, connection);
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
            BlockingQueue<Object> waiting = connection.waiting.remove(request);
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
        // Holds one element rather than handing off directly. A SynchronousQueue
        // drops the value when no consumer is already blocked in poll, so a
        // worker that replied before this thread reached its poll would have
        // its answer thrown away and then be reported as never answering.
        BlockingQueue<Object> answer = new ArrayBlockingQueue<>(1);
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

    /**
     * Waits for a worker to be connected right now.
     *
     * <p>Returns immediately if it already is — a caller re-checking a
     * worker it just confirmed must not be made to wait a poll interval for
     * no reason. Polls the live {@link #workers} map rather than a per-id
     * latch, so a worker that reconnects after an idle disconnect is
     * correctly seen as newly arrived rather than reported from whatever its
     * very first connection, ever, happened to signal.
     */
    public boolean awaitWorker(String workerId, long timeout, TimeUnit unit)
            throws InterruptedException {
        return awaitCondition(() -> workers.containsKey(workerId), timeout, unit);
    }

    /** Waits for a worker to not be connected right now, by the same reasoning. */
    public boolean awaitDeparture(String workerId, long timeout, TimeUnit unit)
            throws InterruptedException {
        return awaitCondition(() -> !workers.containsKey(workerId), timeout, unit);
    }

    private static boolean awaitCondition(java.util.function.BooleanSupplier condition,
            long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                return false;
            }
            Thread.sleep(POLL_MILLIS);
        }
        return true;
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
