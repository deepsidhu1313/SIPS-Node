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

import in.co.s13.SIPS.datastructure.threadpools.FixedThreadPool;
import in.co.s13.SIPS.executor.sockets.handlers.TaskHandler;
import in.co.s13.SIPS.settings.GlobalValues;
import java.io.File;
import java.nio.file.Paths;
import in.co.s13.SIPS.tools.Platform;
import in.co.s13.SIPS.executor.ExecutorScripts;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class TaskServer implements Runnable {

//    public static int processcounter = 0;
//    public static ArrayList<Integer> LOCAL_PROCESS_ID = new ArrayList();
//    public static ArrayList<String> alienprocessID = new ArrayList();
//    public static Process[] p = new Process[1000];
    public TaskServer() {
        try {
            ExecutorScripts.install(Platform.current(), Paths.get(GlobalValues.dir_bin));
        } catch (IOException | IllegalStateException ex) {
            Logger.getLogger(TaskServer.class.getName()).log(Level.SEVERE,
                    "Could not install task executor scripts; this node cannot run tasks", ex);
        }
        File d2 = new File("proc");
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
        if (GlobalValues.TASK_HANDLER_EXECUTOR_SERVICE == null || GlobalValues.TASK_HANDLER_EXECUTOR_SERVICE.isShutdown()) {
            GlobalValues.TASK_HANDLER_EXECUTOR_SERVICE = new FixedThreadPool(GlobalValues.TASK_HANDLER_LIMIT);
        } else {
            GlobalValues.TASK_HANDLER_EXECUTOR_SERVICE.changeSize(GlobalValues.TASK_HANDLER_LIMIT);
        }
    }

    @Override
    public void run() {
        try {
            if (GlobalValues.TASK_SERVER_SOCKET == null || GlobalValues.TASK_SERVER_SOCKET.isClosed()) {
                GlobalValues.TASK_SERVER_SOCKET = new ServerSocket(GlobalValues.TASK_SERVER_PORT);
            }
        } catch (IOException ex) {
            Logger.getLogger(TaskServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        Thread.currentThread().setName("Task Server Thread");

        System.out.println("Server is running");
        while (GlobalValues.TASK_SERVER_IS_RUNNING) {
            try {
                Socket s = GlobalValues.TASK_SERVER_SOCKET.accept();
                GlobalValues.TASK_HANDLER_EXECUTOR_SERVICE.submit(new TaskHandler(s));

            } catch (IOException ex) {
                Logger.getLogger(TaskServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            if (GlobalValues.TASK_SERVER_SOCKET != null && !GlobalValues.TASK_SERVER_SOCKET.isClosed()) {
                GlobalValues.TASK_SERVER_SOCKET.close();
            }

        } catch (IOException ex) {
            Logger.getLogger(TaskServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
