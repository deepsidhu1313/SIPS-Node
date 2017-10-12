/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor.sockets;

import in.co.s13.SIPS.datastructure.threadpools.FixedThreadPool;
import in.co.s13.SIPS.executor.sockets.handlers.TaskFinishListenerHandler;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.io.IOException;
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
public class TaskFinishListenerServer implements Runnable {

    public TaskFinishListenerServer() {
        if (GlobalValues.TASK_FINISH_LISTENER_HANDLER_EXECUTOR_SERVICE == null || GlobalValues.TASK_FINISH_LISTENER_HANDLER_EXECUTOR_SERVICE.isShutdown()) {
            GlobalValues.TASK_FINISH_LISTENER_HANDLER_EXECUTOR_SERVICE = new FixedThreadPool(GlobalValues.TASK_FINISH_LISTENER_HANDLER_LIMIT);
        } else {
            GlobalValues.TASK_FINISH_LISTENER_HANDLER_EXECUTOR_SERVICE.changeSize(GlobalValues.TASK_FINISH_LISTENER_HANDLER_LIMIT);
        }
    }

    @Override
    public void run() {
        try {
            if (GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET == null || GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET.isClosed()) {
                GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET = new ServerSocket(GlobalValues.TASK_FINISH_LISTENER_SERVER_PORT);
            }
        } catch (IOException ex) {
            Logger.getLogger(TaskFinishListenerServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        Util.outPrintln("Finish Server is running");
        while (GlobalValues.TASK_FINISH_SERVER_IS_RUNNING) {
            try {
                Socket s = GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET.accept();
                Thread t = new Thread(new TaskFinishListenerHandler(s));
                t.setPriority(Thread.MAX_PRIORITY);
                t.setName("FinishServerThread");
                GlobalValues.TASK_FINISH_LISTENER_HANDLER_EXECUTOR_SERVICE.submit(t);

            } catch (IOException ex) {
                Logger.getLogger(TaskFinishListenerServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET.close();

        } catch (IOException ex) {
            Logger.getLogger(TaskFinishListenerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
