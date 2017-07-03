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
import in.co.s13.SIPS.executor.sockets.Server;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.settings.Settings;
import static in.co.s13.SIPS.settings.GlobalValues.procDB;
import static in.co.s13.SIPS.settings.GlobalValues.processDBExecutor;
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

/**
 *
 * @author Nika
 */
public class Handler implements Runnable {

    Socket submitter;

    public Handler(Socket connection) {
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
                if (messageString.length() > 1) {
                    System.out.println("IP adress of sender is " + ipAddress);

//                    System.out.println("" + messageString);

                    String command = messageJson.getString("Command");//messageString.substring(messageString.indexOf("<Command>") + 9, messageString.indexOf("</Command>"));
                    JSONObject body = messageJson.getJSONObject("Body");//messageString.substring(messageString.indexOf("<Body>") + 6, messageString.indexOf("</Body>"));
                    System.out.println(messageString);
                    if (command.contains("createprocess")) {
                        GlobalValues.PROCESS_WAITING++;
                        GlobalValues.processExecutor.execute(new ParallelProcess(body, ipAddress));
                        System.out.println("created process");

                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                    }
//                    else if (command.contains("ping")) {
//
//                        try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
//                            JSONObject sendmsg2Json = new JSONObject();
//                            sendmsg2Json.put("OS", GlobalValues.OS);
//                            sendmsg2Json.put("HOSTNAME", GlobalValues.HOST_NAME);
//                            sendmsg2Json.put("PLIMIT", GlobalValues.PROCESS_LIMIT);
//                            sendmsg2Json.put("PWAIT", GlobalValues.PROCESS_WAITING);
//                            sendmsg2Json.put("TMEM", GlobalValues.MEM_SIZE);
//                            sendmsg2Json.put("CPULOAD", Settings.getCPULoad());
//                            sendmsg2Json.put("CPUNAME", GlobalValues.CPU_NAME);
//                            
//                            String sendmsg2 = sendmsg2Json.toString();
////                                    "<OS>" + controlpanel.GlobalValues.OS + "</OS>"
////                                    + "<HOSTNAME>" + controlpanel.GlobalValues.HOST_NAME + "</HOSTNAME>"
////                                    + "<PLIMIT>" + controlpanel.GlobalValues.PROCESS_LIMIT + "</PLIMIT>"
////                                    + "<PWAIT>" + controlpanel.GlobalValues.PROCESS_WAITING + "</PWAIT>"
////                                    + "<TMEM>" + controlpanel.GlobalValues.MEM_SIZE + "</TMEM>"
////                                    + "<CPULOAD>" + controlpanel.Settings.getCPULoad() + "</CPULOAD>"
////                                    + "<CPUNAME>" + controlpanel.GlobalValues.CPU_NAME + "</CPUNAME>";
//
//                            byte[] bytes2 = sendmsg2.getBytes("UTF8");
//                            outToClient2.writeInt(bytes2.length);
//                            outToClient2.write(bytes2);
//                        }
//                        submitter.close();
//
//                    } 
                    else if (command.contains("kill")) {
                        String pid = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));

                        if (Server.alienprocessID.contains("" + ipAddress + "-ID-" + pid + "c" + cno)) {

                            processDBExecutor.execute(() -> {
                                try {
                                    String sql = "SELECT * FROM PROC WHERE  ALIENID = '" + pid + "' AND CNO ='" + cno + "' AND IP ='" + ipAddress + "';";

                                    ResultSet rs = procDB.select("appdb/proc.db", sql);
                                    int n = 9999;
                                    while (rs.next()) {
                                        n = rs.getInt("ID");
                                    }
                                    procDB.closeConnection();

                                    if (Server.p[n].isAlive()) {
                                        Server.p[n].destroy();
                                    }
                                } catch (SQLException ex) {
                                    Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (!submitter.isClosed() && submitter != null) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
