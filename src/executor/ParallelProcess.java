/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import com.sun.scenario.Settings;
import controlpanel.settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class ParallelProcess implements Runnable {

    String ip, pid, fname, content;
    String loc;
    boolean success = true;
    public static Process[] p = new Process[1000];
    int counter = 0;
    Long totalTime;

    public ParallelProcess(String body, String ipadd) throws FileNotFoundException {
        ip = ipadd;
        pid = body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
        fname = body.substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
        content = body.substring(body.indexOf("<CONTENT>") + 9, body.indexOf("</CONTENT>"));
        counter = Server.processcounter;
        Server.processcounter++;
        Server.alienprocessID.add("" + ip + "-ID-" + pid);
        Server.localprocessID.add(counter);

        createProcess(ip, pid, fname, content);
    }

    public void createProcess(String ip, String PID, String filename, String Content) throws FileNotFoundException {
        loc = "var/" + ip + "-ID-" + PID;
        File d2 = new File("var");
        if (!d2.exists()) {
            d2.mkdir();
        }
        File d = new File(loc);
        if (!d.exists()) {
            d.mkdir();
        }
        PrintStream out = new PrintStream("var/" + ip + "-ID-" + PID + "/" + filename); //new AppendFileStream
        out.print(Content);
        out.close();
    }

    @Override
    public void run() {
        try {
            settings.PROCESS_WAITING--;
            ProcessBuilder pb = null;
            String cmd2 = "";
            Long startTime = System.currentTimeMillis();
            if (controlpanel.settings.OS_Name == 0) {
                String pwd = "" + controlpanel.settings.PWD;
                String cmd[] = {"process-executor.bat ", loc, fname, fname.substring(0, fname.indexOf("."))};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                System.out.println("" + cmd2);

                pb = new ProcessBuilder(cmd);
                pb.directory(new File(controlpanel.settings.PWD));

            } else if (controlpanel.settings.OS_Name == 2) {
                String workingDir = System.getProperty("user.dir");
                String scriptloc = "" + workingDir + "/process-executor.sh";
                String cmd[] = {"/bin/bash", scriptloc, loc, fname, fname.substring(0, fname.indexOf("."))};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                System.out.println("" + cmd2);
                pb = new ProcessBuilder(cmd);
                pb.directory(new File(controlpanel.settings.PWD));
            }

            p[counter] = null;
            try {
                p[counter] = pb.start();
            } catch (IOException ex) {
                Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
            }

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p[counter].getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p[counter].getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");

            String s = null;
            String output = "";
            int ocounter = 0;
            while ((s = stdInput.readLine()) != null) {
                ocounter++;
                System.out.println(s);

                if (ocounter == 250000) {

                    output += "\n" + s;
                    Thread outputThread = new Thread(new sendOutput(ip, pid, fname, output));
                    outputThread.start();
                    ocounter = 0;
                    output = "";
                } else {
                    output += "\n" + s;
                }
            }
            Thread outputThread = new Thread(new sendOutput(ip, pid, fname, output));
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
                    Thread outputThread2 = new Thread(new sendOutput(ip, pid, fname, output));
                    outputThread2.start();
                    ocounter = 0;
                    output = "";
                } else {
                    output += "\n" + s;
                }
            }
            Thread outputThread3 = new Thread(new sendOutput(ip, pid, fname, output));
            outputThread3.start();
            ////System.out.println("Process executed");
            int exitValue = p[counter].waitFor();
            System.out.println("\n\nExit Value is " + exitValue);
            Long stopTime = System.currentTimeMillis();
            totalTime = stopTime - startTime;

        } catch (IOException ex) {
            Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (success) {
            Thread t2 = new Thread(new sendOverHead("Finished", ip, pid, fname, "" + totalTime));
            t2.start();
        } else {
            Thread t2 = new Thread(new sendOverHead("Error", ip, pid, fname, "" + totalTime));
            t2.start();

        }

    }

}
