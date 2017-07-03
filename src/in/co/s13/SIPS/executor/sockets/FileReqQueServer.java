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

import in.co.s13.SIPS.datastructure.FileDownQueReq;
import in.co.s13.SIPS.executor.sockets.handlers.FileReqQueHandler;
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
public class FileReqQueServer implements Runnable {

    public static ServerSocket ss;
    public static boolean serverisRunning = false;
    public static ArrayList<FileDownQueReq> downQue = new ArrayList();
    public static ExecutorService executorService = Executors.newFixedThreadPool(GlobalValues.FILES_RESOLVER_LIMIT);

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
