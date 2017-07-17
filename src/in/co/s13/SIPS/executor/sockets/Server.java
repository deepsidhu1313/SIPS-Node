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
package in.co.s13.SIPS.executor.sockets;

import in.co.s13.SIPS.executor.sockets.handlers.Handler;
import in.co.s13.SIPS.settings.GlobalValues;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    public static ServerSocket ss;
    public static int processcounter = 0;
    public static boolean serverisRunning = false;
    public static ArrayList<Integer> localprocessID = new ArrayList();
    public static ArrayList<String> alienprocessID = new ArrayList();
    public static ExecutorService executorService = Executors.newFixedThreadPool(1000);
    public static Process[] p = new Process[1000];

    public Server(boolean serverisrunning) throws IOException {
        serverisRunning = serverisrunning;
        if (GlobalValues.OS_Name == 2) {
            File f = new File("process-executor.sh");

            if (f.exists()) {
                f.delete();
            }

            {
                try (PrintStream out = new PrintStream(f) //new AppendFileStream
                        ) {
                    out.println("#!/bin/bash ");
                    out.println("PATH=/bin:/usr/bin:/usr/local/bin");
                    out.println("WORK=${PWD}/");
                    out.println("cd  \"${WORK}${1}/\"");
                    out.println("bash ${WORK}ant/bin/ant");
                    //       out.println("bash process-executor.sh \"$3\"");
                }
            }
            System.out.println("Script is executable " + f.setExecutable(true));

            File f3 = new File("simulate.sh");
            if (f3.exists()) {
                f3.delete();
            }

            try (PrintStream out2 = new PrintStream(f3) //new AppendFileStream
                    ) {
                out2.println("#!/bin/bash ");
                out2.println("PATH=/bin:/usr/bin:/usr/local/bin");
                out2.println("WORK=${PWD}/");
                out2.println("cd  \"${WORK}${1}/\"");
                out2.println("bash ${WORK}ant/bin/ant -Darg1=$2");

//out2.println("bash  simulate.sh $2");
            }
            System.out.println("Script is executable " + f.setExecutable(true));

        } else {
            File f2 = new File("process-executor.bat");
            if (f2.exists()) {
                f2.delete();
            }
            {
                try (PrintStream out = new PrintStream(f2) //new AppendFileStream
                        ) {
                    out.println("@echo off ");
                    out.println("set PFRAMEWORK_HOME=%~dp0");
                    out.println("set arg1=%~1 ");
                    // out.println("set arg2=%2 ");
                    // out.println("set arg3=%3 ");
                    out.println("cd /d %PFRAMEWORK_HOME%%arg1%");
                    out.println("CALL %PFRAMEWORK_HOME%\\ant\\bin\\ant.bat");
                }

            }
            File f4 = new File("simulate.bat");
            if (f4.exists()) {
                f4.delete();
            }
            {
                PrintStream out = new PrintStream(f4); //new AppendFileStream
                out.println("@echo off ");
                out.println("set PFRAMEWORK_HOME=%~dp0");
                out.println("set arg1=%~1 ");
                out.println("set arg2=%2 ");
                out.println("set arg3=%3 ");
                out.println("set arg4=%4 ");
                //  out.println("java -jar lib1.jar 0 %arg4%");
                out.println("cd /d %PFRAMEWORK_HOME%%arg1%");
                out.println("CALL  %PFRAMEWORK_HOME%\\ant\\bin\\ant.bat  -Darg1= %arg2%");
                // out.println("java -cp .;%PFRAMEWORK_HOME%lib1.jar %arg3%");
                //  out.println(" cd %PFRAMEWORK_HOME%");
                //  out.println("java -jar lib1.jar 1 %arg4%");
                out.close();

            }
        }
        File d2 = new File("var");
        if (!d2.exists()) {
            d2.mkdir();
        }
        File d3 = new File("data");
        if (!d3.exists()) {
            d3.mkdir();
        }
        File d4 = new File("cache");
        if (!d4.exists()) {
            d4.mkdir();
        }

    }

   

    @Override
    public void run() {
        try {
            if (ss == null || ss.isClosed()) {
                ss = new ServerSocket(GlobalValues.MAIN_SERVER_PORT);
            }
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
