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
import in.co.s13.SIPS.executor.sockets.handlers.PingHandler;
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
public class PingServer implements Runnable {
    
    public PingServer()  {
        if (GlobalValues.PING_HANDLER_EXECUTOR_SERVICE == null || GlobalValues.PING_HANDLER_EXECUTOR_SERVICE.isShutdown()) {
            GlobalValues.PING_HANDLER_EXECUTOR_SERVICE = new FixedThreadPool(GlobalValues.PING_HANDLER_LIMIT);
        } else {
            GlobalValues.PING_HANDLER_EXECUTOR_SERVICE.changeSize(GlobalValues.PING_HANDLER_LIMIT);
            
        }
        
    }
    
    @Override
    public void run() {
        try {
            if (GlobalValues.PING_SERVER_SOCKET == null || GlobalValues.PING_SERVER_SOCKET.isClosed()) {
                GlobalValues.PING_SERVER_SOCKET = new ServerSocket(GlobalValues.PING_SERVER_PORT);
            }
        } catch (IOException ex) {
            Logger.getLogger(PingServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("PING Server is running");
        Thread.currentThread().setName("Ping Server Thread");
        while (!GlobalValues.PING_SERVER_SOCKET.isClosed()) {
            try {
                Socket s = GlobalValues.PING_SERVER_SOCKET.accept();
                
                Thread t = new Thread(new PingHandler(s));
                //t.setPriority(Thread.NORM_PRIORITY+1);
                GlobalValues.PING_HANDLER_EXECUTOR_SERVICE.submit(t);
                
            } catch (IOException ex) {
                Logger.getLogger(PingServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            GlobalValues.PING_SERVER_SOCKET.close();
            
        } catch (IOException ex) {
            Logger.getLogger(PingServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
