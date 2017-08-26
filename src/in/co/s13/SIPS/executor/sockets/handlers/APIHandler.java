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
public class APIHandler implements Runnable {

    Socket submitter;

    public APIHandler(Socket connection) {
        submitter = connection;

    }

    @Override
    public void run() {
        //  boolean pingThread = false;
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
                if (msg.length() > 1) {
                    //System.out.println("hurray cond 1");
                    System.out.println("IP adress of sender is " + ipAddress);
                    int key_permissions = 4;
                    // System.out.println("" + msg);
                    String command = msg.getString("Command");
                    JSONObject pingRequestBody = msg.getJSONObject("Body");;
                    String clientUUID = pingRequestBody.getString("UUID");
                    String apiKey = pingRequestBody.getString("API_KEY");
                    if ((GlobalValues.BLACKLIST.containsKey(ipAddress) || GlobalValues.BLACKLIST.containsKey(clientUUID))
                            && (!GlobalValues.API_LIST.containsKey(clientUUID) || !GlobalValues.API_LIST.containsKey(ipAddress))) {
                        //send error message
                        // bad node no cookie for u
                        try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
                            JSONObject sendmsg2Json = new JSONObject();
                            sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                            JSONObject body = new JSONObject();
                            body.put("Response", "Error!!\n \tYou are not allowed.");
                            sendmsg2Json.put("Body", body);
                            String sendmsg2 = sendmsg2Json.toString();
                            byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                            outToClient2.writeInt(bytes2.length);
                            outToClient2.write(bytes2);
                        }

                        submitter.close();
                        return;
                    } else if ((!GlobalValues.BLACKLIST.containsKey(ipAddress) || !GlobalValues.BLACKLIST.containsKey(clientUUID))) {
                        JSONObject keyInfo = null;
                        if (GlobalValues.API_LIST.containsKey(clientUUID)) {
                            keyInfo = GlobalValues.API_LIST.get(clientUUID);
                        } else if (GlobalValues.API_LIST.containsKey(ipAddress)) {
                            keyInfo = GlobalValues.API_LIST.get(ipAddress);

                        }
                        String key = keyInfo.getString("key");
                        key_permissions = keyInfo.getInt("permissions");;
                        if (!key.equals(apiKey)) {
                            try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
                                JSONObject sendmsg2Json = new JSONObject();
                                sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                                JSONObject body = new JSONObject();
                                body.put("Response", "Error!!\n \tIncorrect API key.");
                                sendmsg2Json.put("Body", body);
                                String sendmsg2 = sendmsg2Json.toString();
                                byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                                outToClient2.writeInt(bytes2.length);
                                outToClient2.write(bytes2);
                            }

                            submitter.close();
                            return;
                        }
                    }

                    if (command.equalsIgnoreCase("TestConnection") && hasReadPermissions(key_permissions)) {

                        try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
                            JSONObject sendmsg2Json = new JSONObject();
                            sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                            JSONObject body = new JSONObject();
                            body.put("Response", "Connection Successful");
                            sendmsg2Json.put("Body", body);
                            String sendmsg2 = sendmsg2Json.toString();
                            byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                            outToClient2.writeInt(bytes2.length);
                            outToClient2.write(bytes2);
                        }
                        submitter.close();
                    } else {
                        try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
                            JSONObject sendmsg2Json = new JSONObject();
                            sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                            JSONObject body = new JSONObject();
                            body.put("Response", "Command not available!!");
                            sendmsg2Json.put("Body", body);
                            String sendmsg2 = sendmsg2Json.toString();
                            byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                            outToClient2.writeInt(bytes2.length);
                            outToClient2.write(bytes2);
                        }
                        submitter.close();

                    }

                }
            }

        } catch (IOException ex) {
            Logger.getLogger(APIHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (!submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(APIHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

    private boolean hasReadPermissions(int key_permissions) {
        return key_permissions == 4 || key_permissions == 5 || key_permissions == 6 || key_permissions == 7;
    }

    private boolean hasWritePermissions(int key_permissions) {
        return key_permissions == 2 || key_permissions == 3 || key_permissions == 6 || key_permissions == 7;
    }

    private boolean hasExecutePermissions(int key_permissions) {
        return key_permissions == 1 || key_permissions == 3 || key_permissions == 5 || key_permissions == 7;
    }
}
