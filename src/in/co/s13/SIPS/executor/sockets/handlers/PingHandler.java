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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class PingHandler implements Runnable {

    private Socket submitter;

    private long startTime;

    public PingHandler(Socket connection) {
        submitter = connection;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {

        try {
            try (DataInputStream dataInputStream = new DataInputStream(submitter.getInputStream())) {
                JSONObject msg;
                int length = dataInputStream.readInt();                    // read length of incoming message
                byte[] message = new byte[length];

                if (length > 0) {
                    dataInputStream.readFully(message, 0, message.length); // read the message
                }
                msg = new JSONObject(new String(message));

                InetAddress inetAddress = submitter.getInetAddress();
                String ipAddress = inetAddress.getHostAddress();
                Thread.currentThread().setName("Ping handler for " + ipAddress);
                if (msg.length() > 1) {
//                    System.out.println("IP adress of sender is " + ipAddress);

                    // System.out.println("" + msg);
                    String command = msg.getString("Command");
                    JSONObject pingRequestBody = msg.getJSONObject("Body");
                    String clientUUID = pingRequestBody.getString("UUID");
                    if (GlobalValues.BLACKLIST.containsKey(clientUUID) || GlobalValues.BLACKLIST.containsKey(ipAddress)) {
                        //send error message
                        // bad node no cookie for u
                    }

                    if (command.equalsIgnoreCase("ping")) {

                        try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
                            JSONObject sendmsg2Json = new JSONObject();
                            sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                            sendmsg2Json.put("OS", GlobalValues.OS);
                            sendmsg2Json.put("HOSTNAME", GlobalValues.HOST_NAME);
                            sendmsg2Json.put("TASK_LIMIT", GlobalValues.TASK_LIMIT);
                            sendmsg2Json.put("TASK_WAITING", GlobalValues.TASK_WAITING);
                            sendmsg2Json.put("TMEM", GlobalValues.MEM_SIZE);
                            sendmsg2Json.put("MEM_FREE", GlobalValues.MEM_FREE);
                            sendmsg2Json.put("HDD_SIZE", GlobalValues.HDD_SIZE);
                            sendmsg2Json.put("HDD_FREE", GlobalValues.HDD_FREE);
                            sendmsg2Json.put("CPULOAD", Util.getCPULoad());
                            sendmsg2Json.put("CPUNAME", GlobalValues.CPU_NAME);
                            sendmsg2Json.put("IP_ADDRESSES", GlobalValues.ipAddresses);
                            sendmsg2Json.put("BENCHMARKS", GlobalValues.BENCHMARKING);
                            sendmsg2Json.put("PROCESS_TIME", (System.currentTimeMillis() - startTime));
                            sendmsg2Json.put("ADJ_NODES", Util.getAdjacentTableInJSON());
                            sendmsg2Json.put("NON_ADJ_NODES", Util.getNonAdjacentTableInJSON());
                            sendmsg2Json.put("LIVE_NODES", Util.getAllLiveNodesInJSON());
                            sendmsg2Json.put("NON_ADJ_LIVE_NODES", Util.getNonAdjLiveNodesInJSON());
                            String sendmsg2 = sendmsg2Json.toString();
                            byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                            outToClient2.writeInt(bytes2.length);
                            outToClient2.write(bytes2);
                        }
//                        System.out.println("Ping Recieved");
                        //                pingThread = true;
                        submitter.close();
                    } else {
                        submitter.close();

                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(PingHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (!submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(PingHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
