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
package in.co.s13.SIPS.tools;

import in.co.s13.SIPS.Scanner.ScheduledLiveNodeScanner;
import in.co.s13.SIPS.Scanner.ScheduledNodeScanner;
import in.co.s13.SIPS.datastructure.threadpools.FixedThreadPool;
import in.co.s13.SIPS.executor.sockets.APIServer;
import in.co.s13.SIPS.executor.sockets.FileDownloadServer;
import in.co.s13.SIPS.executor.sockets.FileServer;
import in.co.s13.SIPS.executor.sockets.JobServer;
import in.co.s13.SIPS.executor.sockets.PingServer;
import in.co.s13.SIPS.executor.sockets.TaskFinishListenerServer;
import in.co.s13.SIPS.executor.sockets.TaskServer;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.JOB_LIMIT;
import static in.co.s13.SIPS.settings.GlobalValues.PING_REQUEST_LIMIT;
import static in.co.s13.SIPS.settings.GlobalValues.PING_REQUEST_LIMIT_FOR_LIVE_NODES;
import static in.co.s13.SIPS.settings.GlobalValues.TASK_LIMIT;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class ServiceOperations {

    public static synchronized void initPingServerAtStartUp() {
        if (GlobalValues.PING_SERVER_ENABLED_AT_START) {
            startPingServer();
        }
    }

    public static synchronized void startPingServer() {
        GlobalValues.PING_SERVER_IS_RUNNING = true;
        if ((GlobalValues.PING_SERVER_SOCKET == null || GlobalValues.PING_SERVER_SOCKET.isClosed()) && (GlobalValues.PING_SERVER_THREAD == null || !GlobalValues.PING_SERVER_THREAD.isAlive())) {
            GlobalValues.PING_SERVER_THREAD = new Thread(new PingServer());
            GlobalValues.PING_SERVER_THREAD.start();
        }
    }

    public static synchronized void stopPingServer() {
        GlobalValues.PING_SERVER_IS_RUNNING = false;
//        if (GlobalValues.PING_SERVER_SOCKET != null && !GlobalValues.PING_SERVER_SOCKET.isClosed()) {
//            try {
//                GlobalValues.PING_SERVER_SOCKET.close();
//            } catch (IOException ex) {
//                Logger.getLogger(ServiceOperations.class.getName()).LOG(Level.SEVERE, null, ex);
//            }
//        }
    }

    public static synchronized void restartPingServer() {
        stopPingServer();
        startPingServer();
    }

    public static synchronized void initApiServerAtStartUp() {
        if (GlobalValues.API_SERVER_ENABLED_AT_START) {
            startApiServer();
        }
    }

    public static synchronized void startApiServer() {
        GlobalValues.API_SERVER_IS_RUNNING = true;
        if ((GlobalValues.API_SERVER_SOCKET == null || GlobalValues.API_SERVER_SOCKET.isClosed()) && (GlobalValues.API_SERVER_THREAD == null || !GlobalValues.API_SERVER_THREAD.isAlive())) {
            GlobalValues.API_SERVER_THREAD = new Thread(new APIServer());
            GlobalValues.API_SERVER_THREAD.start();
        }
    }

    public static synchronized void stopApiServer() {
        GlobalValues.API_SERVER_IS_RUNNING = false;
//        if (GlobalValues.API_SERVER_SOCKET != null && !GlobalValues.API_SERVER_SOCKET.isClosed()) {
//            try {
//                GlobalValues.API_SERVER_SOCKET.close();
//            } catch (IOException ex) {
//                Logger.getLogger(ServiceOperations.class.getName()).LOG(Level.SEVERE, null, ex);
//            }
//        }
    }

    public static synchronized void restartApiServer() {
        stopApiServer();
        startApiServer();
    }

    public static synchronized void initTaskServerAtStartUp() {
        if (GlobalValues.TASK_SERVER_ENABLED_AT_START) {
            startTaskServer();
        }
    }

    public static synchronized void startTaskServer() {
        if (GlobalValues.TASK_EXECUTOR == null || GlobalValues.TASK_EXECUTOR.isShutdown()) {
            GlobalValues.TASK_EXECUTOR = new FixedThreadPool(TASK_LIMIT);
        } else {
            GlobalValues.TASK_EXECUTOR.changeSize(TASK_LIMIT);
        }
        GlobalValues.TASK_SERVER_IS_RUNNING = true;
        if ((GlobalValues.TASK_SERVER_SOCKET == null || GlobalValues.TASK_SERVER_SOCKET.isClosed()) && (GlobalValues.TASK_SERVER_THREAD == null || !GlobalValues.TASK_SERVER_THREAD.isAlive())) {
            GlobalValues.TASK_SERVER_THREAD = new Thread(new TaskServer());
            GlobalValues.TASK_SERVER_THREAD.start();
        }
    }

    public static synchronized void stopTaskServer() {
        GlobalValues.TASK_SERVER_IS_RUNNING = false;
//        if (GlobalValues.TASK_SERVER_SOCKET != null && !GlobalValues.TASK_SERVER_SOCKET.isClosed()) {
//            try {
//                GlobalValues.TASK_SERVER_SOCKET.close();
//            } catch (IOException ex) {
//                Logger.getLogger(ServiceOperations.class.getName()).LOG(Level.SEVERE, null, ex);
//            }
//        }
    }

    public static synchronized void restartTaskServer() {
        stopTaskServer();
        startTaskServer();
    }
    
    
    public static synchronized void initJobServerAtStartUp() {
        if (GlobalValues.JOB_SERVER_ENABLED_AT_START) {
            startJobServer();
        }
    }

    public static synchronized void startJobServer() {
        if (GlobalValues.JOB_EXECUTOR == null || GlobalValues.JOB_EXECUTOR.isShutdown()) {
            GlobalValues.JOB_EXECUTOR = new FixedThreadPool(JOB_LIMIT);
        } else {
            GlobalValues.JOB_EXECUTOR.changeSize(JOB_LIMIT);
        }
        GlobalValues.JOB_SERVER_IS_RUNNING = true;
        if ((GlobalValues.JOB_SERVER_SOCKET == null || GlobalValues.JOB_SERVER_SOCKET.isClosed()) && (GlobalValues.JOB_SERVER_THREAD == null || !GlobalValues.JOB_SERVER_THREAD.isAlive())) {
            GlobalValues.JOB_SERVER_THREAD = new Thread(new JobServer());
            GlobalValues.JOB_SERVER_THREAD.start();
        }
    }

    public static synchronized void stopJobServer() {
        GlobalValues.JOB_SERVER_IS_RUNNING = false;
//        if (GlobalValues.TASK_SERVER_SOCKET != null && !GlobalValues.TASK_SERVER_SOCKET.isClosed()) {
//            try {
//                GlobalValues.TASK_SERVER_SOCKET.close();
//            } catch (IOException ex) {
//                Logger.getLogger(ServiceOperations.class.getName()).LOG(Level.SEVERE, null, ex);
//            }
//        }
    }

    public static synchronized void restartJobServer() {
        stopJobServer();
        startJobServer();
    }

    public static synchronized void initFileServerAtStartUp() {
        if (GlobalValues.FILE_SERVER_ENABLED_AT_START) {
            startFileServer();
        }
    }

    public static synchronized void startFileServer() {
        GlobalValues.FILE_SERVER_IS_RUNNING = true;
        if ((GlobalValues.FILE_SERVER_SOCKET == null || GlobalValues.FILE_SERVER_SOCKET.isClosed()) && (GlobalValues.FILE_SERVER_THREAD == null || !GlobalValues.FILE_SERVER_THREAD.isAlive())) {
            GlobalValues.FILE_SERVER_THREAD = new Thread(new FileServer());
            GlobalValues.FILE_SERVER_THREAD.start();
        }
    }

    public static synchronized void stopFileServer() {
        GlobalValues.FILE_SERVER_IS_RUNNING = false;
//        if (GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET != null && !GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.isClosed()) {
//            try {
//                GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.close();
//            } catch (IOException ex) {
//                Logger.getLogger(ServiceOperations.class.getName()).LOG(Level.SEVERE, null, ex);
//            }
//        }
    }

    public static synchronized void restartFileServer() {
        stopFileServer();
        startFileServer();
    }

    public static synchronized void initFileDownloadServerAtStartUp() {
        if (GlobalValues.FILE_DOWNLOAD_SERVER_ENABLED_AT_START) {
            startFileDownloadServer();
        }
    }

    public static synchronized void startFileDownloadServer() {
        GlobalValues.FILE_DOWNLOAD_SERVER_IS_RUNNING = true;
        if ((GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET == null || GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.isClosed()) && (GlobalValues.FILE_DOWNLOAD_SERVER_THREAD == null || !GlobalValues.FILE_DOWNLOAD_SERVER_THREAD.isAlive())) {
            GlobalValues.FILE_DOWNLOAD_SERVER_THREAD = new Thread(new FileDownloadServer());
            GlobalValues.FILE_DOWNLOAD_SERVER_THREAD.start();
        }
    }

    public static synchronized void stopFileDownloadServer() {
        GlobalValues.FILE_DOWNLOAD_SERVER_IS_RUNNING = false;
//        if (GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET != null && !GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.isClosed()) {
//            try {
//                GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.close();
//            } catch (IOException ex) {
//                Logger.getLogger(ServiceOperations.class.getName()).LOG(Level.SEVERE, null, ex);
//            }
//        }
    }

    public static synchronized void restartFileDownloadServer() {
        stopFileDownloadServer();
        startFileDownloadServer();
    }

    public static synchronized void initLiveNodeScannerAtStartUp() {
        if (GlobalValues.LIVE_NODE_SCANNER_ENABLED_AT_START) {
            startLiveNodeScanner();
        }
    }

    public static synchronized void startLiveNodeScanner() {
        //(GlobalValues.KEEP_LIVE_NODE_SCANNER_ALIVE == false) &&
        if (GlobalValues.NETWORK_EXECUTOR == null || GlobalValues.NETWORK_EXECUTOR.isShutdown()) {
            GlobalValues.NETWORK_EXECUTOR = new FixedThreadPool(GlobalValues.TOTAL_IP_SCANNING_THREADS);
        } else {
            GlobalValues.NETWORK_EXECUTOR.changeSize(GlobalValues.TOTAL_IP_SCANNING_THREADS);
        }
        if (GlobalValues.PING_REQUEST_EXECUTOR_FOR_LIVE_NODES == null || GlobalValues.PING_REQUEST_EXECUTOR_FOR_LIVE_NODES.isShutdown()) {
            GlobalValues.PING_REQUEST_EXECUTOR_FOR_LIVE_NODES = new FixedThreadPool(PING_REQUEST_LIMIT_FOR_LIVE_NODES);
        } else {
            GlobalValues.PING_REQUEST_EXECUTOR_FOR_LIVE_NODES.changeSize(PING_REQUEST_LIMIT_FOR_LIVE_NODES);
        }
        if ((GlobalValues.CHECK_LIVE_NODE_THREAD == null || !GlobalValues.CHECK_LIVE_NODE_THREAD.isAlive())) {
            GlobalValues.KEEP_LIVE_NODE_SCANNER_ALIVE = true;
            GlobalValues.CHECK_LIVE_NODE_THREAD = new Thread(new ScheduledLiveNodeScanner());
            GlobalValues.CHECK_LIVE_NODE_THREAD.setName("Scan Live Nodes Scheduled Thread");
            GlobalValues.CHECK_LIVE_NODE_THREAD.start();
        }
    }

    public static synchronized void stopLiveNodeScanner() {
        if (GlobalValues.KEEP_LIVE_NODE_SCANNER_ALIVE == true || GlobalValues.CHECK_LIVE_NODE_THREAD.isAlive()) {
            GlobalValues.KEEP_LIVE_NODE_SCANNER_ALIVE = false;
            while (GlobalValues.CHECK_LIVE_NODE_THREAD.isAlive()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServiceOperations.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static synchronized void restartLiveNodeScanner() {
        stopLiveNodeScanner();
        startLiveNodeScanner();
    }

    public static synchronized void initNodeScannerAtStartUp() {
        if (GlobalValues.NODE_SCANNER_ENABLED_AT_START) {
            startNodeScanner();
        }
    }

    public static synchronized void startNodeScanner() {
        //(GlobalValues.KEEP_NODE_SCANNER_ALIVE == false) &&
        if (GlobalValues.NETWORK_EXECUTOR == null || GlobalValues.NETWORK_EXECUTOR.isShutdown()) {
            GlobalValues.NETWORK_EXECUTOR = new FixedThreadPool(GlobalValues.TOTAL_IP_SCANNING_THREADS);
        } else {
            GlobalValues.NETWORK_EXECUTOR.changeSize(GlobalValues.TOTAL_IP_SCANNING_THREADS);
        }
        if (GlobalValues.PING_REQUEST_EXECUTOR == null || GlobalValues.PING_REQUEST_EXECUTOR.isShutdown()) {
            GlobalValues.PING_REQUEST_EXECUTOR = new FixedThreadPool(PING_REQUEST_LIMIT);
        } else {
            GlobalValues.PING_REQUEST_EXECUTOR.changeSize(PING_REQUEST_LIMIT);
        }
        if ((GlobalValues.NODE_SCANNING_THREAD == null || !GlobalValues.NODE_SCANNING_THREAD.isAlive())) {
            GlobalValues.KEEP_NODE_SCANNER_ALIVE = true;
            GlobalValues.NODE_SCANNING_THREAD = new Thread(new ScheduledNodeScanner());
            GlobalValues.NODE_SCANNING_THREAD.setName("Add Live Nodes Scheduled Thread");
            GlobalValues.NODE_SCANNING_THREAD.start();
        }
    }

    public static synchronized void stopNodeScanner() {
        if (GlobalValues.KEEP_NODE_SCANNER_ALIVE == true || GlobalValues.NODE_SCANNING_THREAD.isAlive()) {
            GlobalValues.KEEP_NODE_SCANNER_ALIVE = false;
            while (GlobalValues.NODE_SCANNING_THREAD.isAlive()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServiceOperations.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static synchronized void restartNodeScanner() {
        stopLiveNodeScanner();
        startLiveNodeScanner();
    }

    public static synchronized void initTaskFinishListenerServerAtStartUp() {
        if (GlobalValues.TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START) {
            startTaskFinishListenerServer();
        }
    }

    public static synchronized void startTaskFinishListenerServer() {
        GlobalValues.TASK_FINISH_SERVER_IS_RUNNING = true;
        if ((GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET == null || GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET.isClosed()) && (GlobalValues.TASK_FINISH_LISTENER_SERVER_THREAD == null || !GlobalValues.TASK_FINISH_LISTENER_SERVER_THREAD.isAlive())) {
            GlobalValues.TASK_FINISH_LISTENER_SERVER_THREAD = new Thread(new TaskFinishListenerServer());
            GlobalValues.TASK_FINISH_LISTENER_SERVER_THREAD.start();
        }
    }

    public static synchronized void stopTaskFinishListenerServer() {
        GlobalValues.TASK_FINISH_SERVER_IS_RUNNING = false;
//        if (GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET != null && !GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET.isClosed()) {
//            try {
//                GlobalValues.TASK_FINISH_LISTENER_SERVER_SOCKET.close();
//            } catch (IOException ex) {
//                Logger.getLogger(ServiceOperations.class.getName()).LOG(Level.SEVERE, null, ex);
//            }
//        }
    }

    public static synchronized void restartTaskFinishListenerServer() {
        stopTaskFinishListenerServer();
        startTaskFinishListenerServer();
    }

    public static synchronized void initLogRotateAtStartUp() {
        if (GlobalValues.LOG_ROTATE_ENABLED_AT_START) {
            startLogRotate();
        }
    }

    public static synchronized void startLogRotate() {
        if ((GlobalValues.LOG_ROTATE_THREAD == null || !GlobalValues.LOG_ROTATE_THREAD.isAlive())) {
            GlobalValues.KEEP_LOG_ROTATE_ALIVE = true;
            GlobalValues.LOG_ROTATE_THREAD = new Thread(new LogRotate());
            GlobalValues.LOG_ROTATE_THREAD.setName("Log Rotate Scheduled Thread");
            GlobalValues.LOG_ROTATE_THREAD.start();
        }
    }

    public static synchronized void stopLogRotate() {
        if (GlobalValues.KEEP_LOG_ROTATE_ALIVE == true || GlobalValues.LOG_ROTATE_THREAD.isAlive()) {
            GlobalValues.KEEP_LOG_ROTATE_ALIVE = false;
            while (GlobalValues.LOG_ROTATE_THREAD.isAlive()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServiceOperations.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static synchronized void restartLogRotate() {
        stopLogRotate();
        startLogRotate();
    }
    
    public static synchronized void initCleanResultDBAtStartUp() {
        if (GlobalValues.CLEAN_RESULT_DB_ENABLED_AT_START) {
            startCleanResultDB();
        }
    }

    public static synchronized void startCleanResultDB() {
        if ((GlobalValues.CLEAN_RESULT_DB_THREAD == null || !GlobalValues.CLEAN_RESULT_DB_THREAD.isAlive())) {
            GlobalValues.KEEP_CLEAN_RESULT_DB_THREAD_ALIVE = true;
            GlobalValues.CLEAN_RESULT_DB_THREAD = new Thread(new CleanResultDB());
            GlobalValues.CLEAN_RESULT_DB_THREAD.setName("Result DB Cleaner Scheduled Thread");
            GlobalValues.CLEAN_RESULT_DB_THREAD.start();
        }
    }

    public static synchronized void stopCleanResultDB() {
        if (GlobalValues.KEEP_CLEAN_RESULT_DB_THREAD_ALIVE == true || GlobalValues.LOG_ROTATE_THREAD.isAlive()) {
            GlobalValues.KEEP_CLEAN_RESULT_DB_THREAD_ALIVE = false;
            while (GlobalValues.CLEAN_RESULT_DB_THREAD.isAlive()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServiceOperations.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static synchronized void restartCleanResultDB() {
        stopCleanResultDB();
        startCleanResultDB();
    }

}
