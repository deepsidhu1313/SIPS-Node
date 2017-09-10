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

import in.co.s13.SIPS.executor.ParallelProcess;
import in.co.s13.SIPS.executor.sockets.TaskServer;
import in.co.s13.SIPS.settings.GlobalValues;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import static in.co.s13.SIPS.settings.GlobalValues.TASK_DB;
import static in.co.s13.SIPS.settings.GlobalValues.TASK_DB_EXECUTOR;

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
                    System.out.println("IP adress of sender is " + ipAddress);

//                    System.out.println("" + messageString);
                    String command = messageJson.getString("Command");//messageString.substring(messageString.indexOf("<Command>") + 9, messageString.indexOf("</Command>"));
                    JSONObject body = messageJson.getJSONObject("Body");//messageString.substring(messageString.indexOf("<Body>") + 6, messageString.indexOf("</Body>"));
                    System.out.println(messageString);
                    if (command.contains("createprocess")) {
                        GlobalValues.TASK_WAITING++;
                        GlobalValues.TASK_EXECUTOR.submit(new ParallelProcess(body, ipAddress));
                        System.out.println("created process");

                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                    } else if (command.contains("kill")) {
                        String pid = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));

                        if (TaskServer.alienprocessID.contains("" + ipAddress + "-ID-" + pid + "c" + cno)) {

                            TASK_DB_EXECUTOR.execute(() -> {
                                try {
                                    String sql = "SELECT * FROM PROC WHERE  ALIENID = '" + pid + "' AND CNO ='" + cno + "' AND IP ='" + ipAddress + "';";

                                    ResultSet rs = TASK_DB.select(GlobalValues.dir_etc+"/proc.db", sql);
                                    int n = 9999;
                                    while (rs.next()) {
                                        n = rs.getInt("ID");
                                    }
                                    TASK_DB.closeConnection();

                                    if (TaskServer.p[n].isAlive()) {
                                        TaskServer.p[n].destroy();
                                    }
                                } catch (SQLException ex) {
                                    Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });

                        }
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();

                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (submitter != null && !submitter.isClosed() ) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(TaskHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
