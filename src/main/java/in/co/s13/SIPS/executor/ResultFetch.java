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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.json.JSONObject;

/**
 * Collecting a chunk result that was too large to ride home.
 *
 * <p>Results below {@link ChunkResults#MAX_INLINE_BYTES} arrive inside the
 * message that says the chunk finished, which costs no round trip and is right
 * for what a call returns. A trained model is not that: anything with a hidden
 * layer is megabytes, so without this a training run works on the sample and
 * fails on the thing anyone would actually train.
 *
 * <p>The result stays in the sandbox the chunk ran in and the master asks for
 * it by name. That is deliberate — the worker does not have to guess which of
 * its outputs anyone wants, and a result nobody collects is removed with the
 * rest of the sandbox instead of accumulating somewhere central.
 *
 * <h2>Why not the existing file transfer</h2>
 *
 * <p>{@code sendfile} moves a job's sources out to the workers: it resolves
 * under {@code data/<job>}, which is the master's layout, and it runs through
 * the download queue with its checksum cache, retry bookkeeping and progress
 * estimates. None of that fits here. A chunk result lives in
 * {@code proc/<node>/<job>/<chunk>}, which {@code sendfile} cannot address at
 * all, and it is fetched once by one caller that is blocking on it.
 *
 * <h2>Failing rather than hanging</h2>
 *
 * <p>Every failure below is one that would otherwise be silent. A node that
 * died between finishing a chunk and being asked for it would leave the master
 * blocked on a read with the whole job behind it, so every read has a timeout.
 * A truncated or corrupted transfer would still deserialise — half a model is a
 * well-formed float array — and averaging it produces a wrong answer rather
 * than an error, so the length and the checksum are both verified before the
 * bytes are handed back.
 */
public final class ResultFetch {

    /** The command this speaks, understood by nodes from protocol version 2. */
    public static final String COMMAND = "sendresult";

    /**
     * The largest result fetched.
     *
     * <p>A guard against a corrupt or hostile header, not a model size policy.
     * Note that the master holds one of these per shard while it averages, so
     * the real ceiling is this times the number of workers.
     */
    public static final long MAX_RESULT_BYTES = 512L * 1024 * 1024;

    /** How long any single read may stall before the node is presumed gone. */
    public static final int DEFAULT_TIMEOUT_MILLIS = 30_000;

    private ResultFetch() {
    }

    /** Fetches a chunk's declared output from the node that produced it. */
    public static byte[] from(String host, int port, String jobToken, String nodeUuid,
            Object chunkNumber, String fileName) throws IOException {
        return from(host, port, jobToken, nodeUuid, chunkNumber, fileName,
                DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Fetches a chunk's declared output from the node that produced it.
     *
     * @param timeoutMillis how long to wait on any one read before giving up
     * @return the result bytes, verified against the checksum the node sent
     * @throws IOException if the node cannot be reached, does not have the
     *         result, or sends something that does not match what it promised
     */
    public static byte[] from(String host, int port, String jobToken, String nodeUuid,
            Object chunkNumber, String fileName, int timeoutMillis) throws IOException {
        String what = "chunk " + chunkNumber + " result '" + fileName + "' from " + host;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    DataInputStream in = new DataInputStream(socket.getInputStream())) {
                write(out, request(jobToken, nodeUuid, chunkNumber, fileName));

                JSONObject reply = read(in, what);
                if (!"found".equalsIgnoreCase(reply.optString("MSG"))) {
                    throw new IOException("Could not collect " + what + ": "
                            + reply.optString("REASON", "the node did not say why"));
                }

                long length = reply.optLong("BYTES", -1);
                if (length < 0) {
                    throw new IOException("Node offered " + what + " without saying how big");
                }
                if (length > MAX_RESULT_BYTES) {
                    throw new IOException("Refusing " + what + ": " + length
                            + " bytes is too large to collect (limit " + MAX_RESULT_BYTES + ")");
                }

                byte[] result = new byte[(int) length];
                in.readFully(result);

                String promised = reply.optString("CHECKSUM", "");
                String actual = checksumOf(result);
                if (!promised.equalsIgnoreCase(actual)) {
                    // Silent corruption is the expensive one: the bytes still
                    // deserialise, so the round produces a wrong model instead
                    // of a failure anyone would notice.
                    throw new IOException("Checksum mismatch collecting " + what
                            + ": expected " + promised + ", got " + actual);
                }
                return result;
            }
        } catch (java.io.EOFException truncated) {
            throw new IOException("Node stopped sending partway through " + what, truncated);
        } catch (java.net.SocketTimeoutException silent) {
            throw new IOException("Node did not answer within " + timeoutMillis
                    + "ms for " + what, silent);
        }
    }

    /** The checksum both ends compare. */
    public static String checksumOf(byte[] content) {
        return HexFormat.of().formatHex(digest().digest(content));
    }

    /**
     * The same checksum, over a file that is not worth holding in memory.
     *
     * <p>Costs the sender a second pass over the file, which is the price of
     * never having the whole result in a byte array on the node that produced
     * it — and the size of these is the entire reason this path exists.
     */
    public static String checksumOf(java.nio.file.Path file) throws IOException {
        MessageDigest digest = digest();
        byte[] buffer = new byte[64 * 1024];
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) > -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required of every JVM", impossible);
        }
    }

    /** The request a node answers with a result. */
    public static JSONObject request(String jobToken, String nodeUuid, Object chunkNumber,
            String fileName) {
        return new JSONObject()
                .put("Command", COMMAND)
                .put("Body", new JSONObject()
                        .put("PID", jobToken)
                        .put("UUID", nodeUuid)
                        .put("CNO", String.valueOf(chunkNumber))
                        .put("FILE", fileName));
    }

    private static void write(DataOutputStream out, JSONObject message) throws IOException {
        byte[] bytes = message.toString().getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    private static JSONObject read(DataInputStream in, String what) throws IOException {
        int length = in.readInt();
        if (length < 1 || length > 64 * 1024) {
            throw new IOException("Nonsensical reply header (" + length + " bytes) for " + what);
        }
        byte[] message = new byte[length];
        in.readFully(message);
        try {
            return new JSONObject(new String(message, StandardCharsets.UTF_8));
        } catch (org.json.JSONException notJson) {
            throw new IOException("Unreadable reply for " + what, notJson);
        }
    }
}
