/* 
 * Copyright (C) 2017 Navdeep Singh Sidhu
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

import in.co.s13.sips.lib.job.ChunkSpec;
import in.co.s13.sips.lib.common.SipsPaths;
import in.co.s13.SIPS.tools.JobPaths;
import com.sun.management.OperatingSystemMXBean;
import in.co.s13.SIPS.datastructure.TaskDBRow;
import in.co.s13.SIPS.datastructure.TaskKeys;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.JavaTarget;
import in.co.s13.SIPS.tools.Platform;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.SIPS.transfer.FilePayload;
import in.co.s13.SIPS.transfer.SafePath;
import in.co.s13.sips.lib.manifest.TaskType;
import in.co.s13.sips.lib.wasm.WasmHost;
import in.co.s13.sips.lib.wasm.WasmRunner;
import in.co.s13.sips.lib.wasm.WasmTask;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class ParallelProcess implements Runnable {

    /**
     * Per-chunk file describing what this chunk was asked to do.
     *
     * @deprecated the name and the shape now belong to {@link ChunkSpec#FILE},
     *         which carries inputs and an output as well as the range.
     */
    @Deprecated
    public static final String CHUNK_RANGE_FILE = ChunkSpec.FILE;

    /** Where a module's result lands when the manifest does not name a file. */
    public static final String DEFAULT_WASM_OUTPUT_FILE = "output.bin";

    /** Long enough that a real chunk finishes; short enough that a hung one is noticed. */
    static final long DEFAULT_WASM_TIMEOUT_SECONDS = 600;

    String ip, pid, cno, main, projectName;
    ArrayList<String> args = new ArrayList<>(), jvmargs = new ArrayList<>();
    ArrayList<String> fname = new ArrayList<>();
    ArrayList<String> fileLog = new ArrayList<>();
    ArrayList<byte[]> content = new ArrayList<>();
    ArrayList<String> libList = new ArrayList<>();
    ArrayList<String> attachments = new ArrayList<>();
    ArrayList<String> libListLocal = new ArrayList<>();
    ArrayList<String> attachmentsLocal = new ArrayList<>();
    JSONArray files = new JSONArray();
    JSONObject manifest;
    TaskType taskType;
    JSONObject wasm;
    /** What a WASM module wrote, kept so a small result can travel home inline. */
    byte[] moduleOutput = new byte[0];
    String loc;
    boolean success = true;
    long counter = 0;
    Long totalTime;
    int opfrequecy = 250000;
    private Process process;
    double loadAvg = 0;
    String uuid;
    TaskDBRow taskDBRow;

    // Widened from FileNotFoundException: building a chunk may now have to
    // fetch an asset the sender referenced instead of inlining, and a chunk
    // whose model could not be collected must not start on a file that is not
    // there.
    public ParallelProcess(JSONObject body, String ipadd) throws IOException {

        ip = ipadd;
        pid = body.getString("PID");
        cno = body.getString("CNO");
        uuid = body.getString("UUID");
        files = body.getJSONArray("FILES");
        for (int i = 0; i < files.length(); i++) {
            JSONObject filesList1 = files.getJSONObject(i);
            fname.add(filesList1.getString("FILENAME"));
            // Decoded to bytes so binary inputs survive; legacy senders that
            // omit the encoding field are read as UTF-8 text. A file too large
            // to have been inlined arrives as a reference instead, and is
            // fetched from the sender once per node rather than once per chunk.
            content.add(resolve(filesList1, ipadd));
        }
        manifest = body.getJSONObject("MANIFEST");
        counter = GlobalValues.TASK_ID.get();
        // How the chunk runs is declared, not inferred from which other fields
        // happen to be set: a manifest that names no executor it meant gets a
        // sentence back, not a surprise.
        taskType = TaskType.of(manifest.optString("TYPE", null));
        if (taskType == TaskType.WASM) {
            wasm = manifest.optJSONObject("WASM");
            if (wasm == null) {
                throw new IllegalArgumentException("A " + taskType.manifestValue()
                        + " job needs a WASM block naming its module");
            }
            main = "";
        } else {
            main = manifest.getString("MAIN");
        }
        projectName = manifest.getString("PROJECT");

        GlobalValues.TASK_ID.incrementAndGet();
        GlobalValues.TASK_DB.put(TaskKeys.of(uuid, pid, cno), new TaskDBRow(pid, projectName, uuid, Integer.parseInt(cno)));
        taskDBRow = GlobalValues.TASK_DB.get(TaskKeys.of(uuid, pid, cno));

        taskDBRow.setEnteredInQueue(System.currentTimeMillis());

        {
            if (manifest.has("LIB")) {
                JSONArray tmp = manifest.getJSONArray("LIB");
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        libList.add(SipsPaths.canonicalJoin("lib", tmp.getString(i)));
                    }
                }
            }
            if (manifest.has("ATTCH")) {
                JSONArray tmp = manifest.getJSONArray("ATTCH");
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        attachments.add(tmp.getString(i));
                    }
                }
            }
            if (manifest.has("ARGS")) {
                JSONArray tmp = manifest.getJSONArray("ARGS");
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        args.add(tmp.getString(i));
                    }
                }
            }
            if (manifest.has("JVMARGS")) {
                JSONArray tmp = manifest.getJSONArray("JVMARGS");
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        double reqMem = 0, avalMem = GlobalValues.MEM_SIZE;
                        String tmp2 = tmp.getString(i);
                        if (tmp2.trim().contains("-Xmx")) {
                            if (tmp2.trim().endsWith("m") || tmp2.trim().endsWith("M")) {
                                reqMem = Double.parseDouble(tmp2.trim().substring(tmp2.trim().indexOf("-Xmx") + 4, tmp2.trim().length() - 1));
                                avalMem /= ( 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem - 500;
                                    tmp2 = "-Xmx" + ((int) Math.ceil(reqMem)) + "M";

                                }
                            } else if (tmp2.trim().endsWith("g") || tmp2.trim().endsWith("G")) {
                                reqMem = Double.parseDouble(tmp2.trim().substring(tmp2.trim().indexOf("-Xmx") + 4, tmp2.trim().length() - 1));
                                avalMem /= (1024 * 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem;
                                    tmp2 = "-Xmx" + ((int) Math.ceil(reqMem)) + "G";

                                }
                            }
                        } else if (tmp2.trim().contains("-Xms")) {
                            System.out.println("Test 5");
                            if (tmp2.trim().endsWith("m") || tmp2.trim().endsWith("M")) {
                                reqMem = Double.parseDouble(tmp2.trim().substring(tmp2.trim().indexOf("-Xms") + 4, tmp2.trim().length() - 1));
                                avalMem /= ( 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem - 500;
                                    tmp2 = "-Xms" + ((int) Math.ceil(reqMem)) + "M";
                                }
                            } else if (tmp2.trim().endsWith("g") || tmp2.trim().endsWith("G")) {
                                String val = tmp2.trim().substring(tmp2.trim().indexOf("-Xms") + 4, tmp2.trim().length() - 1);
                                reqMem = Double.parseDouble(val);
                                avalMem /= (1024 * 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem;
                                    tmp2 = "-Xms" + ((int) Math.ceil(reqMem)) + "G";
                                }
                            }
                        }
                        jvmargs.add(tmp2);
                    }
                }
            }
            opfrequecy = manifest.getInt("OUTPUTFREQUENCY", opfrequecy);

        }

        createProcess(ip, pid, fname, content, uuid);

    }

    public void createProcess(String ip, String PID, ArrayList<String> filename, ArrayList<byte[]> Content, String uuid) throws FileNotFoundException {
        loc = JobPaths.chunkWorkingDirectory(uuid, PID, cno);
        File d2 = new File("proc");
        if (!d2.exists()) {
            d2.mkdir();
        }
        File d = new File(loc);
        if (d.exists()) {
            Util.deleteDirectory(d);
            d.mkdir();
        } else {
            d.mkdir();
        }

        JSONObject meta = new JSONObject();
        meta.put("JOB_TOKEN", pid);
        meta.put("SENDER_IP", ip);
        meta.put("CHUNK_NO", cno);
        meta.put("SENDER_UUID", uuid);
        meta.put("UUID", in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID);
        meta.put("PROJECT", projectName);
        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(libList);
        temp.addAll(attachments);
        Util.write(new File(SipsPaths.join(loc, "task.json")), meta.toString());
        if (taskType == TaskType.JAVA) {
            generateScript(loc, main);
        }

        if (!temp.isEmpty()) {
            DownloadFile recieveFile = new DownloadFile(ip, pid, cno, projectName, loc, temp, uuid);
            fileLog = recieveFile.getFileLog();
        }

        for (int i = 0; i < Content.size(); i++) {
            try {
                // FILENAME arrives over the network, so it is confined to the
                // chunk directory before anything is written.
                Path target = SafePath.resolve(Paths.get(loc), filename.get(i));
                // Written as raw bytes: re-encoding here would undo the
                // byte-exact transport and corrupt binary chunk inputs.
                Files.createDirectories(target.getParent());
                Files.write(target, Content.get(i));
            } catch (IOException | IllegalArgumentException ex) {
                Logger.getLogger(ParallelProcess.class.getName())
                        .log(Level.SEVERE, "Rejected or failed file " + filename.get(i), ex);
                success = false;
            }
        }
    }

    
    static String getVersion() {
        return JavaTarget.forVersion(System.getProperty("java.version"));
    }
    public void generateScript(String location, String main) {
        File f = new File(SipsPaths.join(location, "build.xml"));
        {
            PrintStream out = null;
            if (f.exists()) {
                f.delete();
            }

            try {
                out = new PrintStream(f);
                StringBuilder ARGS = new StringBuilder();
                StringBuilder JVMARGS = new StringBuilder();
                for (String arg1 : args) {
                    ARGS.append("           <arg line=\"");
                    ARGS.append(arg1);
                    ARGS.append("\"/>\n");
                }

                for (String arg1 : jvmargs) {
                    JVMARGS.append("         <jvmarg value=\"");
                    JVMARGS.append(arg1);
                    JVMARGS.append("\"/>\n");
                }
                String version = getVersion();
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                        + "<project default=\"run\" basedir=\".\" name=\"" + location.trim() + "\">\n"
                        + "  <!--this file was created by Eclipse Runnable JAR Export Wizard-->\n"
                        + "  <!--ANT 1.7 is required                                        -->\n"
                        + "\n"
                        + "  <target name=\"compile\">\n"
                        + "    <javac srcdir=\"src\" destdir=\"src\" includes=\"**/*.java\" target=\""+version+"\">\n"
                        + "\n"
                        + "        <classpath refid=\"classpath.base\" />\n"
                        + "    </javac>\n"
                        + "\n"
                        + "  </target>\n"
                        + "<target name=\"run\"  depends=\"compile\">\n"
                        + "      <java fork=\"true\" failonerror=\"yes\" classname=\"" + main.trim() + "\">\n"
                        + JVMARGS.toString()
                        + ARGS.toString()
                        + "        <classpath refid=\"classpath.base\" />\n"
                        + "      <classpath>\n"
                        + "        <pathelement path=\"${classpath.base}\"/>\n"
                        + "        <pathelement location=\"src\"/>\n"
                        + "    </classpath></java>\n"
                        + "   </target>"
                        + "  <!-- Libraries on which your code depends -->\n"
                        + "  <path id=\"classpath.base\">                                                                                                                           \n"
                        + "     <fileset dir=\"lib\">                                                                                                                          \n"
                        + "         <include name=\"**/*.jar\" />                                                                                                          \n"
                        + "     </fileset>\n"
                        + "</path>  \n"
                        + "</project>");

            } catch (FileNotFoundException ex) {
                System.err.println(ex.toString());
                Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (out != null) {
                out.close();
            }
        }

    }

    /**
     * Runs the chunk as a WebAssembly module instead of compiling and forking a
     * JVM.
     *
     * <p>The saving is the whole point: the Ant path costs hundreds of
     * milliseconds of javac and JVM startup before the first iteration runs,
     * which puts a floor under how small a chunk can usefully be. A module
     * starts in microseconds, so a scheduler is free to hand out chunks small
     * enough to actually balance a ragged workload.
     *
     * <p>The module and its range travel in the per-chunk directory, the same
     * channel that already carries generated sources, so nothing else in the
     * distribution path changes.
     */
    private void runWasm() {
        Long startTime = System.currentTimeMillis();
        try (WasmRunner runner = new WasmRunner()) {
            ChunkSpec spec = chunkSpec();
            // MODULE arrives over the network like FILENAME does, so it is
            // confined to the chunk directory before it is opened.
            WasmTask task = new WasmTask(pid, Integer.parseInt(cno),
                    SafePath.resolve(Paths.get(loc), wasm.getString("MODULE")),
                    wasm.optString("ENTRY", null),
                    spec.firstIndex(), spec.lastIndexExclusive());

            // The module reads whatever the chunk directory holds under INPUT
            // and its result is written back beside it, so a WASM chunk gets
            // its data through the same per-chunk channel a Java chunk does.
            WasmHost host = WasmHost.builder()
                    .input(chunkInput())
                    .log(this::report)
                    .earlyExit(GlobalValues.EARLY_EXIT.computeIfAbsent(pid,
                            in.co.s13.sips.lib.loop.EarlyExit::new))
                    .build();

            long status = runner.run(task, host,
                    Duration.ofSeconds(wasm.optLong("TIMEOUT", DEFAULT_WASM_TIMEOUT_SECONDS)));
            byte[] result = host.output();
            moduleOutput = result;
            success = status == WasmTask.SUCCESS;
            writeChunkOutput(result);
            report(success
                    ? "Module finished " + task.iterationCount() + " iterations, "
                        + result.length + " bytes out"
                    : "Module returned status " + status);
        } catch (RuntimeException | IOException ex) {
            Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
            success = false;
            report(String.valueOf(ex.getMessage()));
        }
        totalTime = System.currentTimeMillis() - startTime;
    }

    /** What this chunk in particular was asked to do. */
    private ChunkSpec chunkSpec() {
        return ChunkSpec.read(Util.readJSONFile(SipsPaths.join(loc, ChunkSpec.FILE)));
    }

    /**
     * The bytes of one payload, fetching it from the sender if it was too
     * large to inline and this node does not hold it yet.
     *
     * <p>Fetched from the node that sent the work, which keeps the same asset
     * under the same content address. The second chunk of a job to want a model
     * finds it already here, so a model crosses to a node once rather than once
     * per chunk.
     *
     * @param master the address the task arrived from
     */
    private static byte[] resolve(JSONObject payload, String master) throws IOException {
        return AssetCache.resolve(payload, checksum -> ResultFetch.asset(master,
                GlobalValues.FILE_SERVER_PORT, checksum));
    }

    /**
     * Reads back the file a chunk was told to produce, if it produced one.
     *
     * <p>Only for the Java path: a module's output is already in hand. Quiet
     * when there is nothing to read, because most chunks are not pipeline
     * stages and have nothing to return.
     *
     * <p>An output too large to ride home is left alone rather than read: it
     * stays in this sandbox for {@link ResultFetch} to collect, and reading a
     * model into memory only to have the encoder refuse it would cost the
     * worker a copy of every large result it ever produces.
     */
    private void readDeclaredOutput() {
        if (moduleOutput.length > 0) {
            return;
        }
        try {
            java.util.Optional<String> declared = chunkSpec().output();
            if (declared.isEmpty()) {
                return;
            }
            Path output = SafePath.resolve(Paths.get(loc), declared.get());
            if (Files.exists(output) && Files.size(output) <= ChunkResults.MAX_INLINE_BYTES) {
                moduleOutput = Files.readAllBytes(output);
            }
        } catch (RuntimeException | IOException ex) {
            Util.appendToTasksLog(GlobalValues.LOG_LEVEL.ERROR,
                    "Could not read the declared output of chunk " + cno + ": " + ex);
        }
    }

    /**
     * The bytes this chunk operates on, named by {@code INPUT} in the WASM
     * block. A chunk that computes purely from its index range has none.
     */
    private byte[] chunkInput() throws IOException {
        String name = wasm.optString("INPUT", "");
        if (name.isBlank()) {
            return new byte[0];
        }
        Path input = SafePath.resolve(Paths.get(loc), name);
        return Files.exists(input) ? Files.readAllBytes(input) : new byte[0];
    }

    /**
     * Writes what the module produced into the chunk directory, where the
     * existing collection path can find it.
     */
    private void writeChunkOutput(byte[] output) throws IOException {
        if (output.length == 0) {
            return;
        }
        Files.write(SafePath.resolve(Paths.get(loc),
                wasm.optString("OUTPUT", DEFAULT_WASM_OUTPUT_FILE)), output);
    }

    /** Sends one line of chunk output back to the submitter. */
    private void report(String line) {
        Util.outPrintln(line);
        GlobalValues.SEND_OUTPUT_EXECUTOR_SERVICE
                .submit(new SendOutput(ip, pid, cno, projectName, line));
    }

    @Override
    public void run() {
        taskDBRow.setStartedInQueue(System.currentTimeMillis());
        Thread.currentThread().setName("ParallelProcessThread" + ip + "-" + pid);

        // A break may have arrived while this chunk sat in the queue. Killing a
        // started chunk is TaskHandler's job; not starting one is cheaper, and
        // for a WASM chunk it is the only chance -- a module in flight cannot be
        // interrupted, only timed out.
        in.co.s13.sips.lib.loop.EarlyExit exit = GlobalValues.EARLY_EXIT.get(pid);
        if (exit != null && !exit.shouldRunChunk(taskDBRow.getChunkNo(), taskDBRow.getChunkNo())) {
            GlobalValues.TASK_WAITING.decrementAndGet();
            report("Chunk " + cno + " skipped: the job stopped early");
            GlobalValues.SEND_FINISH_EXECUTOR_SERVICE.submit(new SendFinishMessage("Finished",
                    ip, pid, cno, projectName, "0", "0", loadAvg, uuid));
            return;
        }

        if (taskType == TaskType.WASM) {
            GlobalValues.TASK_WAITING.decrementAndGet();
            runWasm();
            SendFinishMessage done = new SendFinishMessage(success ? "Finished" : "Error",
                    ip, pid, cno, projectName, "" + totalTime, success ? "0" : "1", loadAvg, uuid,
                    ChunkResults.encode(moduleOutput));
            GlobalValues.SEND_FINISH_EXECUTOR_SERVICE.submit(done);
            return;
        }

        try {
            GlobalValues.TASK_WAITING.decrementAndGet();
            Long startTime = System.currentTimeMillis();
            List<String> command = Platform.current().executorCommand(GlobalValues.PWD, loc);
            Util.outPrintln(String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(GlobalValues.PWD));

            try {
                process = pb.start();

                taskDBRow.setProcess(process);
            } catch (IOException ex) {
                Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
            }

            try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream())); BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                // read the output from the command
                Util.outPrintln("Here is the standard output of the command:\n");

                String s = null;
                String output = "";
                OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                Runtime runtime = Runtime.getRuntime();
                int noOfCores = runtime.availableProcessors();
                output = fileLog.stream().map((fileLog1) -> "\n" + fileLog1).reduce(output, String::concat);
                int ocounter = 0;
                long loadCounter = 0;
                while ((s = stdInput.readLine()) != null) {
                    ocounter++;
                    loadAvg += osBean.getSystemLoadAverage();
                    loadCounter++;
                    Util.outPrintln(s);
                    if (ocounter != opfrequecy) {

                        output += "\n" + s;
                        
                    } else {

                        output += "\n" + s;
                        SendOutput outputThread = (new SendOutput(ip, pid, cno, projectName, output));
                        GlobalValues.SEND_OUTPUT_EXECUTOR_SERVICE.submit(outputThread);
                        ocounter = 0;
                        output = "";
                    }
                }
                SendOutput outputThread = (new SendOutput(ip, pid, cno, projectName, output));
                GlobalValues.SEND_OUTPUT_EXECUTOR_SERVICE.submit(outputThread);
                output = "\n";

                ocounter = 0;
                // read any errors from the attempted command
                Util.outPrintln("Here is the standard error of the command (if any):\n");
                while ((s = stdError.readLine()) != null) {
                    ocounter++;
                    loadAvg += osBean.getSystemLoadAverage();
                    loadCounter++;
                    Util.outPrintln(s);
                    success = false;
                    if (ocounter != opfrequecy) {

                        output += "\n" + s;
                        
                         } else {
                        output += "\n" + s;
                        SendOutput outputThread2 = (new SendOutput(ip, pid, cno, projectName, output));
                        GlobalValues.SEND_OUTPUT_EXECUTOR_SERVICE.submit(outputThread2);
                        ocounter = 0;
                        output = "\n";
                   
                    }
                }
                SendOutput outputThread3 = (new SendOutput(ip, pid, cno, projectName, output));
                GlobalValues.SEND_OUTPUT_EXECUTOR_SERVICE.submit(outputThread3);
                loadAvg /= loadCounter;

                int exitValue = process.waitFor();
                Util.outPrintln("\n\nExit Value is " + exitValue);
                Long stopTime = System.currentTimeMillis();
                totalTime = stopTime - startTime;
            }

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        // A pipeline stage written in Java leaves its result as a file; read it
        // back so it travels home the same way a module's does. Without this a
        // Java stage could produce output that no later stage could ever read.
        readDeclaredOutput();

        SendFinishMessage finished = new SendFinishMessage(success ? "Finished" : "Error",
                ip, pid, cno, projectName, "" + totalTime, success ? "0" : "1", loadAvg, uuid,
                ChunkResults.encode(moduleOutput));
        GlobalValues.SEND_FINISH_EXECUTOR_SERVICE.submit(finished);

    }

}
