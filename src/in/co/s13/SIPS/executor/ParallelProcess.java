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

import in.co.s13.SIPS.executor.sockets.TaskServer;
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
import static in.co.s13.SIPS.settings.GlobalValues.TASK_DB;
import static in.co.s13.SIPS.settings.GlobalValues.TASK_DB_EXECUTOR;

/**
 *
 * @author Nika
 */
public class ParallelProcess implements Runnable {

    String ip, pid, cno, manifest, files, main, projectName, args;
    ArrayList<String> fname = new ArrayList<>();
    ArrayList<String> fileLog = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayList<String> libList = new ArrayList<>();
    ArrayList<String> attachments = new ArrayList<>();
    ArrayList<String> libListLocal = new ArrayList<>();
    ArrayList<String> attachmentsLocal = new ArrayList<>();

    String loc;
    boolean success = true;
    int counter = 0;
    Long totalTime;

    public ParallelProcess(JSONObject body, String ipadd) throws FileNotFoundException {
        ip = ipadd;
        pid = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
        cno = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
        JSONArray files = body.getJSONArray("FILES");//body.substring(body.indexOf("<FILES>") + 7, body.indexOf("</FILES>"));
        // String[] filesList = (files.split("$@r@r@$"));
        for (int i = 0; i < files.length(); i++) {
            JSONObject object = files.getJSONObject(i);
            fname.add(object.getString("FILENAME"));
            content.add(object.getString("CONTENT"));
        }

//       for (String filesList1 : filesList) {
//            if (filesList1.contains("<FILENAME>")) {
//                fname.add(filesList1.substring(filesList1.indexOf("<FILENAME>") + 10, filesList1.indexOf("</FILENAME>")).trim());
//            }
//            if (filesList1.contains("<CONTENT>")) {
//                content.add(filesList1.substring(filesList1.indexOf("<CONTENT>") + 9, filesList1.indexOf("</CONTENT>")).trim());
//            }
//        }
        JSONObject manifest = body.getJSONObject("MANIFEST");//body.substring(body.indexOf("<MANIFEST>") + 10, body.indexOf("</MANIFEST>"));
        counter = TaskServer.processcounter;
        main = body.getString("MAIN");//manifest.substring(manifest.indexOf("<MAIN>") + 6, manifest.indexOf("</MAIN>"));
        projectName = body.getString("PROJECT");//manifest.substring(manifest.indexOf("<PROJECT>") + 9, manifest.indexOf("</PROJECT>"));

        
        JSONArray lib= manifest.getJSONArray("LIB");
        JSONArray attach= manifest.getJSONArray("ATTCH");
        args=manifest.getString("ARGS");
        
//        String[] lines = manifest.split("\n");
//        for (String line : lines) {
//            if (line.contains("<LIB>")) {
//                String tmp = line.substring(line.indexOf("<LIB>") + 5, line.indexOf("</LIB>"));
//                if (tmp.trim().length() > 0) {
//                    libList.add(tmp);
//                }
//            }
//            if (line.contains("<ATTCH>")) {
//                String tmp = line.substring(line.indexOf("<ATTCH>") + 7, line.indexOf("</ATTCH>"));
//                if (tmp.trim().length() > 0) {
//                    attachments.add(tmp);
//                }
//            }
//            if (line.contains("<ARGS>")) {
//                String tmp = line.substring(line.indexOf("<ARGS>") + 6, line.indexOf("</ARGS>"));
//                args = tmp;
//            }
//        }
        TaskServer.processcounter++;
        TaskServer.alienprocessID.add("" + ip + "-ID-" + pid + "c" + cno);
        TaskServer.localprocessID.add(counter);
        TASK_DB_EXECUTOR.execute(() -> {
            String sql = "INSERT INTO PROC (ID,"
                    + " ALIENID ,"
                    + "FNAME,"
                    + "CNO     ,"
                    + "IP   ) VALUES('" + counter + "','" + pid + "','" + projectName + "','" + cno + "','" + ip + "');";

            TASK_DB.insert("appdb/proc.db", sql);
            TASK_DB.closeConnection();
        });
        createProcess(ip, pid, fname, content);

    }

    public void createProcess(String ip, String PID, ArrayList<String> filename, ArrayList<String> Content) throws FileNotFoundException {
        loc = "var/" + ip + "-ID-" + PID + "c" + cno;
//        File d2 = new File("var");
//        if (!d2.exists()) {
//            d2.mkdir();
//        }
        File d = new File(loc);
        if (d.exists()) {
            Util.deleteDirectory(d);
            d.mkdirs();
        }
        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(libList);
        temp.addAll(attachments);
        generateScript(loc, args, main);

        if (!temp.isEmpty()) {
            System.out.println("Downloading files : " + temp);
            RecieveFileBak recieveFile = new RecieveFileBak(ip, pid, cno, projectName, loc, temp);
            fileLog = recieveFile.getFileLog();
        } else {
            System.out.println("No File To Download");
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

    public void generateScript(String id, String arg, String main) {
        File f = new File(id + "/build.xml");
        {
            PrintStream out = null;
            if (f.exists()) {
                f.delete();
            }

            try {
                out = new PrintStream(f); //new AppendFileStream
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
                        + "         <arg line=\"" + arg.trim() + "\"/>\n"
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
        try {
            GlobalValues.TASK_WAITING--;
            ProcessBuilder pb = null;
            String cmd2 = "";
            Long startTime = System.currentTimeMillis();
            if (GlobalValues.OS_Name == 0) {
                String pwd = "" + GlobalValues.PWD;
                String cmd[] = {"process-executor.bat ", loc};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                System.out.println("" + cmd2);

                pb = new ProcessBuilder(cmd);
                pb.directory(new File(GlobalValues.PWD));

            } else if (GlobalValues.OS_Name == 2) {
                String workingDir = System.getProperty("user.dir");
                String scriptloc = "" + workingDir + "/process-executor.sh";
                String cmd[] = {"/bin/bash", scriptloc, loc};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                System.out.println("" + cmd2);
                pb = new ProcessBuilder(cmd);
                pb.directory(new File(GlobalValues.PWD));
            }

            TaskServer.p[counter] = null;
            try {
                TaskServer.p[counter] = pb.start();
            } catch (IOException ex) {
                Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
            }

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(TaskServer.p[counter].getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(TaskServer.p[counter].getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");

            String s = null;
            String output = "";
            output = fileLog.stream().map((fileLog1) -> "\n" + fileLog1).reduce(output, String::concat);
            int ocounter = 0;
            while ((s = stdInput.readLine()) != null) {
                ocounter++;
                System.out.println(s);

                if (ocounter == 250000) {

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
            output = "";

            ocounter = 0;
            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                ocounter++;
                System.out.println(s);
                success = false;
                if (ocounter == 250000) {

                    output += "\n" + s;
                    Thread outputThread2 = new Thread(new sendOutput(ip, pid, cno, projectName, output));
                    outputThread2.start();
                    ocounter = 0;
                    output = "";
                } else {
                    output += "\n" + s;
                }
            }
            Thread outputThread3 = new Thread(new sendOutput(ip, pid, cno, projectName, output));
            outputThread3.start();
            ////System.out.println("Process executed");
            int exitValue = TaskServer.p[counter].waitFor();
            System.out.println("\n\nExit Value is " + exitValue);
            Long stopTime = System.currentTimeMillis();
            totalTime = stopTime - startTime;
            stdError.close();
            stdInput.close();
            TaskServer.p[counter].destroy();

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

    }

}
