/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class Server implements Runnable {

    ServerSocket ss;
    public static int processcounter = 0;
    public static boolean serverisRunning = false;
    public static ArrayList<Integer> localprocessID = new ArrayList();
    public static ArrayList<String> alienprocessID = new ArrayList();
public ExecutorService executorService = Executors.newFixedThreadPool(1000);
    public Server(boolean serverisrunning) throws IOException {
        serverisRunning = serverisrunning;
        if (controlpanel.settings.OS_Name == 2) {
            File f = new File("process-executor.sh");
            if (!f.exists()) {
                PrintStream out = new PrintStream(f); //new AppendFileStream
                out.println("#!/bin/bash ");
                out.println("PATH=/bin:/usr/bin:/usr/local/bin");
                out.println("WORK=${PWD}/");
                out.println("cd  \"${WORK}${1}/\"");
                out.println("javac -cp .:${WORK}lib1.jar $2");
                out.println("java -cp .:${WORK}lib1.jar $3");
                out.close();
                System.out.println("Script is executable "+  f.setExecutable(true));
            }
        } else {
            File f2 = new File("process-executor.bat");
            if (!f2.exists()) {
                PrintStream out = new PrintStream(f2); //new AppendFileStream
                out.println("@echo off ");
                out.println("set PFRAMEWORK_HOME=%~dp0");
                out.println("set arg1=%~1 ");
                out.println("set arg2=%2 ");
                out.println("set arg3=%3 ");
                out.println("cd /d %PFRAMEWORK_HOME%%arg1%");
                out.println("javac -cp .;%PFRAMEWORK_HOME%lib1.jar %arg2% ");
                out.println("java -cp .;%PFRAMEWORK_HOME%lib1.jar %arg3% \n cd %PFRAMEWORK_HOME%");
                out.close();

            }
        }
        File d2 = new File("var");
        if (!d2.exists()) {
            d2.mkdir();
        }
    }

    public static void main(String[] args) {
    }

    @Override
    public void run() {
        try {
            ss = new ServerSocket(13131);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        while (serverisRunning) {
            try {
                Socket s = ss.accept();
                System.out.println("Server is running");
                executorService.execute(new Handler(s));
                
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            ss.close();

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
