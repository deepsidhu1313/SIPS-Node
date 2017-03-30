/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor.sockets;

import in.co.s13.SIPS.executor.sockets.handlers.PingHandler;
import in.co.s13.SIPS.settings.GlobalValues;
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
public class PingServer implements Runnable {

    public static ServerSocket ss;
    public static boolean serverisRunning = false;
    public static ExecutorService executorService = Executors.newFixedThreadPool(GlobalValues.PING_HANDLER_LIMIT);
    
    public PingServer(boolean serverisrunning,int Mode) throws IOException {
        serverisRunning = serverisrunning;
        
    }

   
    public static void main(String[] args) {
    }

    @Override
    public void run() {
        try {
            if (ss == null || ss.isClosed()) {
                ss = new ServerSocket(13139);
            }
        } catch (IOException ex) {
            Logger.getLogger(PingServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        while (serverisRunning) {
            try {
                Socket s = ss.accept();
                System.out.println("Server is running");
                Thread t= new Thread(new PingHandler(s));
                //t.setPriority(Thread.NORM_PRIORITY+1);
                executorService.execute(t);

            } catch (IOException ex) {
                Logger.getLogger(PingServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            ss.close();

        } catch (IOException ex) {
            Logger.getLogger(PingServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
