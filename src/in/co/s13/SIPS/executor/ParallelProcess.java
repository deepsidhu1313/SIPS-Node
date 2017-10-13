/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.datastructure.TaskDBRow;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

    public ParallelProcess(JSONObject body, String ipadd) throws FileNotFoundException {
        ip = ipadd;
        pid = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
        cno = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
        files = body.getJSONArray("FILES");//body.substring(body.indexOf("<FILES>") + 7, body.indexOf("</FILES>"));
        //String[] filesList = (files.split("$@r@r@$"));
        for (int i = 0; i < files.length(); i++) {
            JSONObject filesList1 = files.getJSONObject(i);
            fname.add(filesList1.getString("FILENAME"));
            content.add(filesList1.getString("CONTENT"));
//            if (filesList1.contains("<FILENAME>")) {
//                fname.add(filesList1.substring(filesList1.indexOf("<FILENAME>") + 10, filesList1.indexOf("</FILENAME>")).trim());
//            }
//            if (filesList1.contains("<CONTENT>")) {
//                content.add(filesList1.substring(filesList1.indexOf("<CONTENT>") + 9, filesList1.indexOf("</CONTENT>")).trim());
//            }
        }

        manifest = body.getJSONObject("MANIFEST");//substring(body.indexOf("<MANIFEST>") + 10, body.indexOf("</MANIFEST>"));
        counter = GlobalValues.TASK_ID.get();
        main = manifest.getString("MAIN");//substring(manifest.indexOf("<MAIN>") + 6, manifest.indexOf("</MAIN>"));
        projectName = manifest.getString("PROJECT");//substring(manifest.indexOf("<PROJECT>") + 9, manifest.indexOf("</PROJECT>"));

        //String[] lines = manifest.split("\n");
        //for (String line : lines) 
        {
            if (manifest.has("LIB")) {
                JSONArray tmp = manifest.getJSONArray("LIB");//line.substring(line.indexOf("<LIB>") + 5, line.indexOf("</LIB>"));
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        libList.add(tmp.getString(i));
                    }
                }
            }
            if (manifest.has("ATTCH")) {
                JSONArray tmp = manifest.getJSONArray("ATTCH");//line.substring(line.indexOf("<LIB>") + 5, line.indexOf("</LIB>"));
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        attachments.add(tmp.getString(i));
                    }
                }
            }
            if (manifest.has("ARGS")) {
                JSONArray tmp = manifest.getJSONArray("ARGS");//line.substring(line.indexOf("<LIB>") + 5, line.indexOf("</LIB>"));
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        args.add(tmp.getString(i));
                    }
                }
            }
            if (manifest.has("JVMARGS")) {
                JSONArray tmp = manifest.getJSONArray("JVMARGS");//line.substring(line.indexOf("<LIB>") + 5, line.indexOf("</LIB>"));
                if (tmp.length() > 0) {
                    for (int i = 0; i < tmp.length(); i++) {
                        double reqMem = 0, avalMem = GlobalValues.MEM_SIZE;
                        String tmp2 = tmp.getString(i);
                        if (tmp2.trim().contains("-Xmx")) {
                            if (tmp2.trim().substring(tmp2.trim().length() - 1).equalsIgnoreCase("m")) {
                                reqMem = Double.parseDouble(tmp2.trim().substring(tmp2.trim().indexOf("-Xmx") + 4, tmp2.trim().lastIndexOf("m")));
                                avalMem /= (1024 * 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem - 500;
                                    tmp2 = "-Xmx" + reqMem + "m";

                                }
                            } else if (tmp2.trim().substring(tmp2.trim().length() - 1).equalsIgnoreCase("g")) {
                                reqMem = Double.parseDouble(tmp2.trim().substring(tmp2.trim().indexOf("-Xmx") + 4, tmp2.trim().lastIndexOf("g")));
                                avalMem /= (1024 * 1024 * 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem - 1.2;
                                    tmp2 = "-Xmx" + reqMem + "g";

                                }
                            }
                        } else if (tmp2.trim().contains("-Xms")) {
                            if (tmp2.trim().substring(tmp2.trim().length() - 1).equalsIgnoreCase("m")) {
                                reqMem = Double.parseDouble(tmp2.trim().substring(tmp2.trim().indexOf("-Xms") + 4, tmp2.trim().lastIndexOf("m")));
                                avalMem /= (1024 * 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem - 500;
                                    tmp2 = "-Xms" + reqMem + "m";
                                }
                            } else if (tmp2.trim().substring(tmp2.trim().length() - 1).equalsIgnoreCase("g")) {
                                reqMem = Double.parseDouble(tmp2.trim().substring(tmp2.trim().indexOf("-Xms") + 4, tmp2.trim().lastIndexOf("g")));
                                avalMem /= (1024 * 1024 * 1024);
                                if (reqMem >= avalMem) {
                                    reqMem = avalMem - 1.2;
                                    tmp2 = "-Xms" + reqMem + "g";
                                }
                            }
                        }
                        jvmargs.add(tmp2);
                    }
                }
            }
//            if (line.contains("<OUTPUTFREQUENCY>")) {
//                String tmp = line.substring(line.indexOf("<OUTPUTFREQUENCY>") + 17, line.indexOf("</OUTPUTFREQUENCY>"));
//            }
            opfrequecy = manifest.getInt("OUTPUTFREQUENCY", opfrequecy);//Integer.parseInt(tmp.trim());

        }
        GlobalValues.TASK_ID.incrementAndGet();
        GlobalValues.TASK_DB.put("" + ip + "-ID-" + pid + "c" + cno, new TaskDBRow(pid, projectName, ipadd, (int) counter, process));
        //GlobalValues.localprocessID.add(counter);
//        processDBExecutor.execute(() -> {
//            String sql = "INSERT INTO PROC (ID,"
//                    + " ALIENID ,"
//                    + "FNAME,"
//                    + "CNO     ,"
//                    + "IP   ) VALUES('" + counter + "','" + pid + "','" + projectName + "','" + cno + "','" + ip + "');";
//
//            procDB.insert("appdb/proc.db", sql);
//            procDB.closeConnection();
//        });
        createProcess(ip, pid, fname, content);

    }

    public void createProcess(String ip, String PID, ArrayList<String> filename, ArrayList<String> Content) throws FileNotFoundException {
        loc = "var/" + ip + "-ID-" + PID + "c" + cno;
        File d2 = new File("var");
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
        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(libList);
        temp.addAll(attachments);
        generateScript(loc, main);

        if (!temp.isEmpty()) {
            DownloadFile recieveFile = new DownloadFile(ip, pid, cno, projectName, loc, temp);
            fileLog = recieveFile.getFileLog();
        }

        for (int i = 0; i < Content.size(); i++) {
            File f = new File("var/" + ip + "-ID-" + PID + "c" + cno + "/" + filename.get(i));
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            try (PrintStream out = new PrintStream("var/" + ip + "-ID-" + PID + "c" + cno + "/" + filename.get(i)) //new AppendFileStream
                    ) {
                out.print(Content.get(i));
                out.close();
            }
        }
    }

    public void generateScript(String id, String main) {
        File f = new File(id + "/build.xml");
        {
            PrintStream out = null;
            if (f.exists()) {
                f.delete();
            }

            try {
                out = new PrintStream(f); //new AppendFileStream
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
                        + "<project default=\"run\" basedir=\".\" name=\"" + id.trim() + "\">\n"
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
                        + "     <fileset dir=\"libs\">                                                                                                                          \n"
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
        Thread.currentThread().setName("ParallelProcessThread" + ip + "-" + pid);
        Thread eiq = new Thread(new sendStartInQue("startinque", ip, pid, cno, projectName, "" + System.currentTimeMillis()));
        eiq.start();
        try {
            GlobalValues.TASK_WAITING.decrementAndGet();
            ProcessBuilder pb = null;
            String cmd2 = "";
            Long startTime = System.currentTimeMillis();
            if (GlobalValues.OS_Name == 0) {
                String pwd = "" + GlobalValues.PWD;
                String cmd[] = {"process-executor.bat ", loc};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                Util.outPrintln("" + cmd2);

                pb = new ProcessBuilder(cmd);
                pb.directory(new File(GlobalValues.PWD));

            } else if (GlobalValues.OS_Name == 2) {
                String workingDir = System.getProperty("user.dir");
                String scriptloc = "" + workingDir + "/process-executor.sh";
                String cmd[] = {"/bin/bash", scriptloc, loc};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                Util.outPrintln("" + cmd2);
                pb = new ProcessBuilder(cmd);
                pb.directory(new File(GlobalValues.PWD));
            }

            process = null;
            try {
                process = pb.start();
            } catch (IOException ex) {
                Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
            }

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // read the output from the command
            Util.outPrintln("Here is the standard output of the command:\n");

            String s = null;
            String output = "";
            output = fileLog.stream().map((fileLog1) -> "\n" + fileLog1).reduce(output, String::concat);
            int ocounter = 0;
            while ((s = stdInput.readLine()) != null) {
                ocounter++;
                Util.outPrintln(s);

                if (ocounter == opfrequecy) {

                    output += "\n" + s;
                    Thread outputThread = new Thread(new sendOutput(ip, pid, cno, projectName, output));
                    outputThread.start();
                    ocounter = 0;
                    output = "";
                } else {

                    output += "\n" + s;
                }
            }
            Thread outputThread = new Thread(new sendOutput(ip, pid, cno, projectName, output));
            outputThread.start();
            output = "\n";

            ocounter = 0;
            // read any errors from the attempted command
            Util.outPrintln("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                ocounter++;
                Util.outPrintln(s);
                success = false;
                if (ocounter == opfrequecy) {

                    output += "\n" + s;
                    Thread outputThread2 = new Thread(new sendOutput(ip, pid, cno, projectName, output));
                    outputThread2.start();
                    ocounter = 0;
                    output = "\n";
                } else {
                    output += "\n" + s;
                }
            }
            Thread outputThread3 = new Thread(new sendOutput(ip, pid, cno, projectName, output));
            outputThread3.start();
            ////settings.outPrintln("Process executed");
            int exitValue = process.waitFor();
            Util.outPrintln("\n\nExit Value is " + exitValue);
            Long stopTime = System.currentTimeMillis();
            totalTime = stopTime - startTime;
            stdError.close();
            stdInput.close();
            process.destroy();

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (success) {
            Thread t2 = new Thread(new sendOverHead("Finished", ip, pid, cno, projectName, "" + totalTime, "0"));
            t2.start();
        } else {
            Thread t2 = new Thread(new sendOverHead("Error", ip, pid, cno, projectName, "" + totalTime, "1"));
            t2.start();

        }
        System.gc();
    }

}
