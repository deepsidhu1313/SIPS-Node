/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor.sockets;


import in.co.s13.SIPS.datastructure.threadpools.FixedThreadPool;
import in.co.s13.SIPS.executor.sockets.handlers.FileHandler;
import in.co.s13.SIPS.settings.GlobalValues;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class FileServer implements Runnable {


    public FileServer() throws IOException {
          GlobalValues.FILE_HANDLER_EXECUTOR_SERVICE = new FixedThreadPool(GlobalValues.FILES_RESOLVER_LIMIT);
    
    }

    public static void main(String[] args) {
    }

    @Override
    public void run() {
 try {
            if (GlobalValues.FILE_SERVER_SOCKET == null || GlobalValues.FILE_SERVER_SOCKET.isClosed()) {
                GlobalValues.FILE_SERVER_SOCKET = new ServerSocket(GlobalValues.FILE_SERVER_PORT);

            }
        } catch (IOException ex) {
            Logger.getLogger(FileDownloadServer.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        while (GlobalValues.FILE_SERVER_IS_RUNNING) {
            try {
                Socket s = GlobalValues.FILE_SERVER_SOCKET.accept();
                Thread t = new Thread(new FileHandler(s));
                t.setPriority(Thread.NORM_PRIORITY + 1);
                t.setName("FileHandlIngThread");
                GlobalValues.FILE_HANDLER_EXECUTOR_SERVICE.submit(t);

            } catch (IOException ex) {
                Logger.getLogger(FileServer.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            GlobalValues.FILE_SERVER_SOCKET.close();

        } catch (IOException ex) {
            Logger.getLogger(FileServer.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
}
