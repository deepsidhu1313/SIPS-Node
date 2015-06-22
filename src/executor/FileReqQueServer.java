/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

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
public class FileReqQueServer implements Runnable {

    public static ServerSocket ss;
    public static boolean serverisRunning = false;
    public static ArrayList<FileDownQueReq> downQue = new ArrayList();
    public static ExecutorService executorService = Executors.newFixedThreadPool(25);

    public FileReqQueServer(boolean serverisrunning) throws IOException {
        serverisRunning = serverisrunning;

    }

    public static void main(String[] args) {
    }

    @Override
    public void run() {
        try {
            if (ss == null || ss.isClosed()) {
                ss = new ServerSocket(13136);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileReqQueServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        while (serverisRunning) {
            try {
                Socket s = ss.accept();
                System.out.println("File Download Que Server is running");
                Thread t= new Thread(new FileReqQueHandler(s));
                t.setPriority(Thread.NORM_PRIORITY+1);
                t.setName("FileHandlIngThread");
                executorService.submit(t);
              

            } catch (IOException ex) {
                Logger.getLogger(FileReqQueServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            ss.close();

        } catch (IOException ex) {
            Logger.getLogger(FileReqQueServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
