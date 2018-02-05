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
import in.co.s13.SIPS.executor.sockets.handlers.JobHandler;
import in.co.s13.SIPS.executor.sockets.handlers.TaskHandler;
import in.co.s13.SIPS.settings.GlobalValues;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class JobServer implements Runnable {

    public JobServer() {
        
        if (GlobalValues.JOB_HANDLER_EXECUTOR_SERVICE == null || GlobalValues.JOB_HANDLER_EXECUTOR_SERVICE.isShutdown()) {
            GlobalValues.JOB_HANDLER_EXECUTOR_SERVICE = new FixedThreadPool(GlobalValues.JOB_HANDLER_LIMIT);
        } else {
            GlobalValues.JOB_HANDLER_EXECUTOR_SERVICE.changeSize(GlobalValues.JOB_HANDLER_LIMIT);
        }
    }

    @Override
    public void run() {
        try {
            if (GlobalValues.JOB_SERVER_SOCKET == null || GlobalValues.JOB_SERVER_SOCKET.isClosed()) {
                GlobalValues.JOB_SERVER_SOCKET = new ServerSocket(GlobalValues.JOB_SERVER_PORT);
            }
        } catch (IOException ex) {
            Logger.getLogger(JobServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        Thread.currentThread().setName("Job Server Thread");

        System.out.println("Job Server is running");
        while (GlobalValues.JOB_SERVER_IS_RUNNING) {
            try {
                Socket s = GlobalValues.JOB_SERVER_SOCKET.accept();
                GlobalValues.JOB_HANDLER_EXECUTOR_SERVICE.submit(new JobHandler(s));

            } catch (IOException ex) {
                Logger.getLogger(JobServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            if (GlobalValues.JOB_SERVER_SOCKET != null && !GlobalValues.JOB_SERVER_SOCKET.isClosed()) {
                GlobalValues.JOB_SERVER_SOCKET.close();
            }

        } catch (IOException ex) {
            Logger.getLogger(JobServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
