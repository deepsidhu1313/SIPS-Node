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

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.SIPS.virtualdb.UpdateDistDBaftExecVirtual;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class TaskFinishListenerHandler implements Runnable {

    Socket submitter;
    int pnum;
    String simsql = "";
    long pdelay = 10;

    public TaskFinishListenerHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {

        try {
            try (DataInputStream dIn = new DataInputStream(submitter.getInputStream()); OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                int length = dIn.readInt();                    // read length of incoming message
                byte[] message = new byte[length];

                if (length > 0) {
                    dIn.readFully(message, 0, message.length); // read the message
                }
                String s = new String(message);
                JSONObject msg = new JSONObject(s);

                InetAddress inetAddress = submitter.getInetAddress();
                String ipAddress = inetAddress.getHostAddress();
                if (msg.toString().length() > 1) {
                    //                    Util.outPrintln("IP adress of sender is " + ipAddress);

                    Util.appendToTasksLog(GlobalValues.LOG_LEVEL.OUTPUT, "Recieved " + msg);
                    System.out.println("Task Finish Handler Recieved:" + msg);
                    String command = msg.getString("Command");
                    JSONObject body = msg.getJSONObject("Body");;
                    //     Settings.outPrintln(msg);
                    if (command.equalsIgnoreCase("Finished")) {
                        String pid = body.getString("PID");
                        String cno = body.getString("CNO");
                        String fname = body.getString("FILENAME");
                        String content = body.getString("OUTPUT");
                        String ExitCode = body.getString("EXTCODE");
                        String uuid = body.getString("UUID");
                        double avgLoad = body.getDouble("AVGLOAD", Double.MAX_VALUE);
                        {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                        Thread t = new Thread(new UpdateDistDBaftExecVirtual(System.currentTimeMillis(), Long.parseLong(content), fname, ipAddress, pid, cno, ExitCode, uuid,avgLoad));
                        GlobalValues.DIST_DB_EXECUTOR.submit(t);
                        Util.appendToTasksLog(GlobalValues.LOG_LEVEL.OUTPUT, "size of master dist db " + MASTER_DIST_DB.size());

                    } else if (command.contains("Error")) {
                        String pid = body.getString("PID");
                        String cno = body.getString("CNO");
                        String fname = body.getString("FILENAME");
                        String content = body.getString("OUTPUT");
                        String ExitCode = body.getString("EXTCODE");
                        String uuid = body.getString("UUID");
                        double avgLoad = body.getDouble("AVGLOAD", Double.MAX_VALUE);
                        {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                        }
                        submitter.close();
                        Thread t = new Thread(new UpdateDistDBaftExecVirtual(System.currentTimeMillis(), Long.parseLong(content), fname, ipAddress, pid, cno, ExitCode, uuid,avgLoad));
                        GlobalValues.DIST_DB_EXECUTOR.submit(t);
                    } else {
                        submitter.close();

                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TaskFinishListenerHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (submitter != null && !submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(TaskFinishListenerHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
