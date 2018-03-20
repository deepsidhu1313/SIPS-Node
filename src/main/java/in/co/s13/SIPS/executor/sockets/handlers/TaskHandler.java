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
package in.co.s13.SIPS.executor.sockets.handlers;

import in.co.s13.SIPS.datastructure.DistributionDBRow;
import in.co.s13.SIPS.datastructure.TaskDBRow;
import in.co.s13.SIPS.executor.ParallelProcess;
import in.co.s13.SIPS.executor.PrintToFile;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Nika
 */
public class TaskHandler implements Runnable {

    Socket submitter;

    public TaskHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {
        try {
            String messageString;
            JSONObject messageJson;
            try (DataInputStream dataInputStream = new DataInputStream(submitter.getInputStream())) {
                messageString = "";
                int messageLength = dataInputStream.readInt();                    // read length of incoming message
                byte[] messageBytes = new byte[messageLength];
                if (messageLength > 0) {
                    dataInputStream.readFully(messageBytes, 0, messageBytes.length); // read the message
                }
                messageString = new String(messageBytes);
                messageJson = new JSONObject(messageString);
                InetAddress inetAddress = submitter.getInetAddress();
                String ipAddress = inetAddress.getHostAddress();
                Thread.currentThread().setName("Process handler for " + ipAddress);
                if (messageString.length() > 1) {
//                    System.out.println("IP adress of sender is " + ipAddress);

//                    System.OUT.println("" + messageString);
                    String command = messageJson.getString("Command");//messageString.substring(messageString.indexOf("<Command>") + 9, messageString.indexOf("</Command>"));
                    JSONObject body = messageJson.getJSONObject("Body");//messageString.substring(messageString.indexOf("<Body>") + 6, messageString.indexOf("</Body>"));
//                    System.out.println(messageString);
                    if (command.equalsIgnoreCase("createprocess")) {
                        GlobalValues.TASK_WAITING.incrementAndGet();

                        GlobalValues.TASK_EXECUTOR.submit(new ParallelProcess(body, ipAddress));
                        System.out.println("created process");

                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
//                        System.out.println("" + messageJson.toString(4));
                    } else if (command.equalsIgnoreCase("ComOH")) {
                        String pid = body.getString("PID");//.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String content = body.getString("OUTPUT");//.substring(body.indexOf("<OUTPUT>") + 8, body.indexOf("</OUTPUT>"));
                        String uuid = body.getString("UUID");
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                        GlobalValues.DIST_DB_EXECUTOR.execute(() -> {
                            int counter = 0;
                            boolean exist = false;
                            while (!exist && counter < 5) {
                                ConcurrentHashMap<String, DistributionDBRow> DistTable = MASTER_DIST_DB.get((pid.trim()));
                                if (DistTable != null) {
                                    exist = true;
                                    DistributionDBRow get = DistTable.get(uuid + "-" + cno.trim());
                                    if (get != null) {
                                        get.setNoh(get.getNoh() + Long.parseLong(content));
//                                System.out.println("Set Network OH in Q:\n" + get.toString(4));
                                    }
                                }
                                try {
                                    Thread.currentThread().sleep(1000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                counter++;
                            }
                        });

                    } else if (command.equalsIgnoreCase("CACHEHIT")) {
                        String pid = body.getString("PID");
                        String cno = body.getString("CNO");
                        String senderUuid = body.getString("SENDER_UUID");
                        long size = body.getLong("SIZE");
                        double speed = body.getDouble("SPEED");
                        String uuid = body.getString("UUID");
                        long commOH = body.getLong("COMM_OH");
                        long sleepTime = body.getLong("SLEEP_TIME");
                        submitter.close();
//                        System.out.println("" + messageJson.toString(4));
                        TaskDBRow taskDBRow = GlobalValues.TASK_DB.get("" + senderUuid + "-ID-" + pid + "-CN-" + cno);
                        taskDBRow.incrementCacheHit();
                        taskDBRow.setCachedData(taskDBRow.getCachedData() + size);
                        taskDBRow.addCommOH(commOH);
                        taskDBRow.addSleepTime(sleepTime);
                    } else if (command.equalsIgnoreCase("CACHEMISS")) {
                        String pid = body.getString("PID");
                        String cno = body.getString("CNO");
                        String senderUuid = body.getString("SENDER_UUID");
                        long size = body.getLong("SIZE");
                        double speed = body.getDouble("SPEED");
                        String uuid = body.getString("UUID");
                        long commOH = body.getLong("COMM_OH");
                        long sleepTime = body.getLong("SLEEP_TIME");
                        submitter.close();
                        System.out.println("" + messageJson.toString(4));
                        TaskDBRow taskDBRow = GlobalValues.TASK_DB.get("" + senderUuid + "-ID-" + pid + "-CN-" + cno);
                        taskDBRow.incrementCacheMiss();
                        taskDBRow.setDownloadData(taskDBRow.getDownloadData() + size);
                        taskDBRow.addDownloadSpeed(speed);
                        taskDBRow.addCommOH(commOH);
                        taskDBRow.addSleepTime(sleepTime);
                        taskDBRow.incrementReqRecieved();
                    } else if (command.equalsIgnoreCase("startinque")) {
                        String pid = body.getString("PID");//.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String content = body.getString("OUTPUT");//.substring(body.indexOf("<OUTPUT>") + 8, body.indexOf("</OUTPUT>"));
                        String uuid = body.getString("UUID");
                        //   String ExitCode = body.substring(body.indexOf("<EXTCODE>") + 9, body.indexOf("</EXTCODE>"));
//                        System.out.println("Recieved: " + messageJson);
//                        int p = Integer.parseInt(pid);
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                        GlobalValues.DIST_DB_EXECUTOR.execute(() -> {
                            ConcurrentHashMap<String, DistributionDBRow> DistTable = MASTER_DIST_DB.get((pid.trim()));
                            if (DistTable != null) {

                                DistributionDBRow get = DistTable.get(uuid + "-" + cno.trim());
                                if (get != null) {
                                    get.setStartinq(Long.parseLong(content));
//                                    System.out.println("Set start in Q:\n" + get.toString(4));
                                }
                            }
                        });

                    } else if (command.equalsIgnoreCase("enterinq")) {
                        String pid = body.getString("PID");//.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String content = body.getString("OUTPUT");//.substring(body.indexOf("<OUTPUT>") + 8, body.indexOf("</OUTPUT>"));
                        String uuid = body.getString("UUID");
                        System.out.println("Recieved: " + messageJson);
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";
                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                        GlobalValues.DIST_DB_EXECUTOR.execute(() -> {
                            int counter = 0;
                            boolean exist = false;
                            while (!exist && counter < 5) {
                                ConcurrentHashMap<String, DistributionDBRow> DistTable = MASTER_DIST_DB.get(pid.trim());
//                                System.out.println("\n\nRetriving DIST Table From:\n " + MASTER_DIST_DB.values());
                                if (DistTable != null) {
                                    exist = true;
                                    System.out.println("Retriving DIST Row");

                                    DistributionDBRow get = DistTable.get(uuid + "-" + cno.trim());
                                    if (get != null) {
                                        get.setEntrinq(Long.parseLong(content));
                                        break;
//                                        System.out.println("Set Enter in Q:\n" + get.toString(4));
                                    }
                                }
                                try {
                                    Thread.currentThread().sleep(1000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                counter++;
                            }
                        });

                    } else if (command.equalsIgnoreCase("sleeptime")) {
                        String pid = body.getString("PID");//.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String content = body.getString("OUTPUT");
                        String uuid = body.getString("UUID");
                        submitter.close();
                        GlobalValues.DIST_DB_EXECUTOR.execute(() -> {
                            int counter = 0;
                            boolean exist = false;
                            while (!exist && counter < 5) {
                                ConcurrentHashMap<String, DistributionDBRow> DistTable = MASTER_DIST_DB.get((pid.trim()));
                                if (DistTable != null) {
                                    exist = true;
                                    System.out.println("Retrived DIST Row");
                                    DistributionDBRow get = DistTable.get(uuid + "-" + cno.trim());
                                    if (get != null) {
                                        get.setSleeptime(get.getSleeptime() + Long.parseLong(content));
                                    }
                                }
                                try {
                                    Thread.currentThread().sleep(1000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                counter++;
                            }
                        });

                    } else if (command.equalsIgnoreCase("kill")) {
                        String pid = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String uuid = body.getString("UUID");

                        if (GlobalValues.TASK_DB.containsKey("" + uuid + "-ID-" + pid + "c" + cno)) {
                            Process p = GlobalValues.TASK_DB.get("" + uuid + "-ID-" + pid + "c" + cno).getProcess();
                            if (p.isAlive()) {
                                p.destroy();
                            }

                        }
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();

                    } else if (command.equalsIgnoreCase("printoutput")) {
                        String pid = body.getString("PID");//.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String content = body.getString("OUTPUT");//.substring(body.indexOf("<OUTPUT>") + 8, body.indexOf("</OUTPUT>"));
//                        int p = Integer.parseInt(pid);
                        String output = content;
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                        }

                        submitter.close();
                        PrintToFile outToFile = (new PrintToFile(fname, pid, cno, content));
                        GlobalValues.OUTPUT_WRITER_EXECUTOR.submit(outToFile);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (submitter != null && !submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
