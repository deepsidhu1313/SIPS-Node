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

import com.sun.management.OperatingSystemMXBean;
import in.co.s13.SIPS.datastructure.TaskDBRow;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class ParallelProcess implements Runnable {

    String ip, pid, cno, main, projectName;
    ArrayList<String> args = new ArrayList<>(), jvmargs = new ArrayList<>();
    ArrayList<String> fname = new ArrayList<>();
    ArrayList<String> fileLog = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayList<String> libList = new ArrayList<>();
    ArrayList<String> attachments = new ArrayList<>();
    ArrayList<String> libListLocal = new ArrayList<>();
    ArrayList<String> attachmentsLocal = new ArrayList<>();
    JSONArray files = new JSONArray();
    JSONObject manifest;
    String loc;
    boolean success = true;
    long counter = 0;
    Long totalTime;
    int opfrequecy = 250000;
    private Process process;
    double loadAvg = 0;
    String uuid;
    TaskDBRow taskDBRow;

    public ParallelProcess(JSONObject body, String ipadd) throws FileNotFoundException {

        ip = ipadd;
        pid = body.getString("PID");
        cno = body.getString("CNO");
        uuid = body.getString("UUID");
        files = body.getJSONArray("FILES");
        for (int i = 0; i < files.length(); i++) {
            JSONObject filesList1 = files.getJSONObject(i);
            fname.add(filesList1.getString("FILENAME"));
            content.add(filesList1.getString("CONTENT"));
        }
        manifest = body.getJSONObject("MANIFEST");
        counter = GlobalValues.TASK_ID.get();
        main = manifest.getString("MAIN");
        projectName = manifest.getString("PROJECT");

        GlobalValues.TASK_ID.incrementAndGet();
        GlobalValues.TASK_DB.put("" + uuid + "-ID-" + pid + "-CN-" + cno, new TaskDBRow(pid, projectName, uuid, Integer.parseInt(cno)));
        taskDBRow = GlobalValues.TASK_DB.get("" + uuid + "-ID-" + pid + "-CN-" + cno);

        taskDBRow.setEnteredInQueue(System.currentTimeMillis());

        {
            if (manifest.has("LIB")) {
                JSONArray tmp = manifest.getJSONArray("LIB");
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        libList.add("lib/" + tmp.getString(i));
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

    public void createProcess(String ip, String PID, ArrayList<String> filename, ArrayList<String> Content, String uuid) throws FileNotFoundException {
        loc = "proc/" + uuid + "/" + PID + "/" + cno;
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
        Util.write(new File(loc + "/task.json"), meta.toString());
        generateScript(loc, main);

        if (!temp.isEmpty()) {
            DownloadFile recieveFile = new DownloadFile(ip, pid, cno, projectName, loc, temp, uuid);
            fileLog = recieveFile.getFileLog();
        }

        for (int i = 0; i < Content.size(); i++) {
            Util.write(loc + "/" + filename.get(i), Content.get(i));
        }
    }

    public void generateScript(String location, String main) {
        File f = new File(location + "/build.xml");
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

                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                        + "<project default=\"run\" basedir=\".\" name=\"" + location.trim() + "\">\n"
                        + "  <!--this file was created by Eclipse Runnable JAR Export Wizard-->\n"
                        + "  <!--ANT 1.7 is required                                        -->\n"
                        + "\n"
                        + "  <target name=\"compile\">\n"
                        + "    <javac srcdir=\"src\" destdir=\"src\" includes=\"**/*.java\" target=\"1.8\">\n"
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

    @Override
    public void run() {
        taskDBRow.setStartedInQueue(System.currentTimeMillis());
        Thread.currentThread().setName("ParallelProcessThread" + ip + "-" + pid);

        try {
            GlobalValues.TASK_WAITING.decrementAndGet();
            ProcessBuilder pb = null;
            String cmd2 = "";
            Long startTime = System.currentTimeMillis();
            if (GlobalValues.OS_Name == 0) {
                String pwd = "" + GlobalValues.PWD;
                String cmd[] = {"bin/process-executor.bat ", loc};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                Util.outPrintln("" + cmd2);

                pb = new ProcessBuilder(cmd);
                pb.directory(new File(GlobalValues.PWD));

            } else if (GlobalValues.OS_Name == 2) {
                String workingDir = System.getProperty("user.dir");
                String scriptloc = "" + workingDir + "/bin/process-executor.sh";
                String cmd[] = {"/bin/bash", scriptloc, loc};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                Util.outPrintln("" + cmd2);
                pb = new ProcessBuilder(cmd);
                pb.directory(new File(GlobalValues.PWD));
            }

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
                    if (ocounter == opfrequecy) {

                        output += "\n" + s;
                        SendOutput outputThread = (new SendOutput(ip, pid, cno, projectName, output));
                        GlobalValues.SEND_OUTPUT_EXECUTOR_SERVICE.submit(outputThread);
                        ocounter = 0;
                        output = "";
                    } else {

                        output += "\n" + s;
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
                    if (ocounter == opfrequecy) {

                        output += "\n" + s;
                        SendOutput outputThread2 = (new SendOutput(ip, pid, cno, projectName, output));
                        GlobalValues.SEND_OUTPUT_EXECUTOR_SERVICE.submit(outputThread2);
                        ocounter = 0;
                        output = "\n";
                    } else {
                        output += "\n" + s;
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
        if (success) {
            SendFinishMessage t2 = (new SendFinishMessage("Finished", ip, pid, cno, projectName, "" + totalTime, "0", loadAvg, uuid));
            GlobalValues.SEND_FINISH_EXECUTOR_SERVICE.submit(t2);
        } else {
            SendFinishMessage t2 = (new SendFinishMessage("Error", ip, pid, cno, projectName, "" + totalTime, "1", loadAvg, uuid));
            GlobalValues.SEND_FINISH_EXECUTOR_SERVICE.submit(t2);

        }

    }

}
