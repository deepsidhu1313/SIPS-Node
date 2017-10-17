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
import in.co.s13.SIPS.tools.ServiceOperations;
import static in.co.s13.SIPS.tools.ServiceOperations.restartApiServer;
import static in.co.s13.SIPS.tools.ServiceOperations.restartFileServer;
import static in.co.s13.SIPS.tools.ServiceOperations.restartLiveNodeScanner;
import static in.co.s13.SIPS.tools.ServiceOperations.restartNodeScanner;
import static in.co.s13.SIPS.tools.ServiceOperations.restartPingServer;
import static in.co.s13.SIPS.tools.ServiceOperations.restartTaskServer;
import static in.co.s13.SIPS.tools.ServiceOperations.startApiServer;
import static in.co.s13.SIPS.tools.ServiceOperations.startFileServer;
import static in.co.s13.SIPS.tools.ServiceOperations.startLiveNodeScanner;
import static in.co.s13.SIPS.tools.ServiceOperations.startNodeScanner;
import static in.co.s13.SIPS.tools.ServiceOperations.startPingServer;
import static in.co.s13.SIPS.tools.ServiceOperations.startTaskServer;
import static in.co.s13.SIPS.tools.ServiceOperations.stopApiServer;
import static in.co.s13.SIPS.tools.ServiceOperations.stopFileServer;
import static in.co.s13.SIPS.tools.ServiceOperations.stopLiveNodeScanner;
import static in.co.s13.SIPS.tools.ServiceOperations.stopNodeScanner;
import static in.co.s13.SIPS.tools.ServiceOperations.stopPingServer;
import static in.co.s13.SIPS.tools.ServiceOperations.stopTaskServer;
import in.co.s13.SIPS.tools.Util;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
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

        try (DataInputStream dataInputStream = new DataInputStream(submitter.getInputStream()); OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
            JSONObject msg;
            int length = dataInputStream.readInt();                    // read length of incoming message
            byte[] message = new byte[length];

            if (length > 0) {
                dataInputStream.readFully(message, 0, message.length); // read the message
            }
            msg = new JSONObject(new String(message));

            InetAddress inetAddress = submitter.getInetAddress();
            String ipAddress = inetAddress.getHostAddress();
            System.out.println("IP adress of sender is " + ipAddress);
            Thread.currentThread().setName("API handler for " + ipAddress);
            if (msg.length() > 1) {
                //System.out.println("hurray cond 1");
                int key_permissions = 0;//default value to 0, no harm done by malformed key
                System.out.println("" + msg.toString(4));
                String command = msg.getString("Command");
                JSONObject requestBody = msg.getJSONObject("Body");;
                String clientUUID = requestBody.getString("UUID");
                JSONArray args = requestBody.getJSONArray("ARGS");
                System.out.println("ARGS : " + args.toString());
                String apiKey = requestBody.getString("API_KEY");
                if ((GlobalValues.BLACKLIST.containsKey(ipAddress) || GlobalValues.BLACKLIST.containsKey(clientUUID))
                        && (!GlobalValues.API_LIST.containsKey(clientUUID) || !GlobalValues.API_LIST.containsKey(ipAddress))) {
                    //send error message
                    // bad node no cookie for u

                    JSONObject sendmsg2Json = new JSONObject();
                    sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                    JSONObject body = new JSONObject();
                    body.put("Response", "Error!!\n \tYou are not allowed.");
                    sendmsg2Json.put("Body", body);
                    String sendmsg2 = sendmsg2Json.toString();
                    byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                    outToClient2.writeInt(bytes2.length);
                    outToClient2.write(bytes2);

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
                        JSONObject sendmsg2Json = new JSONObject();
                        sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                        JSONObject body = new JSONObject();
                        body.put("Response", "Error!!\n \tIncorrect API key.");
                        sendmsg2Json.put("Body", body);
                        String sendmsg2 = sendmsg2Json.toString();
                        byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                        outToClient2.writeInt(bytes2.length);
                        outToClient2.write(bytes2);

                        submitter.close();
                        return;
                    }
                }
                JSONObject sendmsg2Json = new JSONObject();
                sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                JSONObject body = new JSONObject();

                if (command.equalsIgnoreCase("TestConnection")) {
                    body.put("Response", "Connection Successful");
                } else if (command.equalsIgnoreCase("blacklist")) {
                    if (args.length() == 1 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getBlackListInJSON());
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getBlackListInJSON());
                    } else {
                        body.put("Response", "Incorrect permissions or arguments!!");

                    }
                } else if (command.equalsIgnoreCase("adjacent")) {
                    if (args.length() == 1 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getAdjacentTableInJSON());
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getAdjacentTableInJSON());
                    } else {
                        body.put("Response", "Incorrect permissions or arguments!!");

                    }
                } else if (command.equalsIgnoreCase("service")) {
                    JSONObject response = new JSONObject();
                    if (args.length() == 2 && hasExecutePermissions(key_permissions)) {
                        if (args.getString(0).equalsIgnoreCase("PING-SERVER")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                startPingServer();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                stopPingServer();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                restartPingServer();
                            }
                            response.put("PING-SERVER", !GlobalValues.PING_SERVER_SOCKET.isClosed());
                        } else if (args.getString(0).equalsIgnoreCase("FILE-DOWNLOAD-SERVER")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                ServiceOperations.startFileDownloadServer();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                ServiceOperations.stopFileDownloadServer();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                ServiceOperations.restartFileDownloadServer();
                            }
                            response.put("FILE-DOWNLOAD-SERVER", !GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.isClosed());
                        } else if (args.getString(0).equalsIgnoreCase("FILE-SERVER")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                startFileServer();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                stopFileServer();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                restartFileServer();
                            }
                            response.put("FILE-SERVER", !GlobalValues.FILE_SERVER_SOCKET.isClosed());
                        } else if (args.getString(0).equalsIgnoreCase("TASK-SERVER")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                startTaskServer();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                stopTaskServer();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                restartTaskServer();
                            }
                            response.put("TASK-SERVER", !GlobalValues.TASK_SERVER_SOCKET.isClosed());
                        } else if (args.getString(0).equalsIgnoreCase("API-SERVER")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                startApiServer();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                stopApiServer();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                restartApiServer();
                            }
                            response.put("API-SERVER", !GlobalValues.API_SERVER_SOCKET.isClosed());
                        } else if (args.getString(0).equalsIgnoreCase("LIVE-NODE-SCANNER")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                startLiveNodeScanner();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                stopLiveNodeScanner();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                restartLiveNodeScanner();
                            }
                            response.put("LIVE-NODE-SCANNER", GlobalValues.CHECK_LIVE_NODE_THREAD.isAlive());
                        } else if (args.getString(0).equalsIgnoreCase("NODE-SCANNER")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                startNodeScanner();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                stopNodeScanner();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                restartNodeScanner();
                            }
                            response.put("NODE-SCANNER", GlobalValues.NODE_SCANNING_THREAD.isAlive());
                        } else {
                            response.put("Error !!!", "Unknown Service!");

                        }
                        body.put("Response", response);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("status") && hasReadPermissions(key_permissions)) {
                        response.put("PING-SERVER", !GlobalValues.PING_SERVER_SOCKET.isClosed());
                        response.put("FILE-SERVER", !GlobalValues.FILE_SERVER_SOCKET.isClosed());
                        response.put("FILE-DOWNLOAD-SERVER", !GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.isClosed());
                        response.put("TASK-SERVER", !GlobalValues.TASK_SERVER_SOCKET.isClosed());
                        response.put("API-SERVER", !GlobalValues.API_SERVER_SOCKET.isClosed());
                        response.put("LIVE-NODE-SCANNER", GlobalValues.CHECK_LIVE_NODE_THREAD.isAlive());
                        response.put("NODE-SCANNER", GlobalValues.NODE_SCANNING_THREAD.isAlive());
                        body.put("Response", response);

                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        response.put("PING-SERVER", !GlobalValues.PING_SERVER_SOCKET.isClosed());
                        response.put("FILE-SERVER", !GlobalValues.FILE_SERVER_SOCKET.isClosed());
                        response.put("FILE-DOWNLOAD-SERVER", !GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.isClosed());
                        response.put("TASK-SERVER", !GlobalValues.TASK_SERVER_SOCKET.isClosed());
                        response.put("API-SERVER", !GlobalValues.API_SERVER_SOCKET.isClosed());
                        response.put("LIVE-NODE-SCANNER", GlobalValues.CHECK_LIVE_NODE_THREAD.isAlive());
                        response.put("NODE-SCANNER", GlobalValues.NODE_SCANNING_THREAD.isAlive());
                        body.put("Response", response);
                    } else {
                        body.put("Response", "Incorrect permissions or arguments!!");

                    }
                } else if (command.equalsIgnoreCase("non-adjacent")) {
                    if (args.length() > 0 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getNonAdjacentTableInJSON());
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getNonAdjacentTableInJSON());
                    } else {
                        body.put("Response", "Incorrect permissions or arguments!!");

                    }
                } else if (command.equalsIgnoreCase("nodes")) {
                    if (args.length() == 0
                            && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getAllLiveNodesInJSON());
                    } else if (args.length() == 4
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("adj")
                            && args.getString(2).equalsIgnoreCase("sort")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show adj sort IP desc
                        body.put("Response", Util.getLiveNodesInJSON(0, args.getString(4).equalsIgnoreCase("desc"), args.getString(3)));
                    } else if (args.length() == 4
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("non-adj")
                            && args.getString(2).equalsIgnoreCase("sort")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show non-adj sort IP desc
                        body.put("Response", Util.getLiveNodesInJSON(1, args.getString(4).equalsIgnoreCase("desc"), args.getString(3)));
                    } else if (args.length() == 3
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("adj")
                            && args.getString(2).equalsIgnoreCase("sort")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show adj sort IP
                        body.put("Response", Util.getLiveNodesInJSON(0, false, args.getString(3)));
                    } else if (args.length() == 3
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("non-adj")
                            && args.getString(2).equalsIgnoreCase("sort")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show non-adj sort IP
                        body.put("Response", Util.getLiveNodesInJSON(1, false, args.getString(3)));
                    } else if (args.length() == 3
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("sort")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show sort IP desc
                        body.put("Response", Util.getLiveNodesInJSON(2, args.getString(3).equalsIgnoreCase("desc"), args.getString(2)));
                    } else if (args.length() == 2
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("sort")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show sort IP desc
                        body.put("Response", Util.getLiveNodesInJSON(2, false, args.getString(2)));
                    } else if (args.length() == 1
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("adj")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show adj
                        body.put("Response", Util.getAdjLiveNodesInJSON());
                    } else if (args.length() == 1
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("non-adj")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show non-adj
                        body.put("Response", Util.getNonAdjLiveNodesInJSON());
                    } else if (args.length() == 1
                            && args.getString(0).equalsIgnoreCase("show")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show
                        body.put("Response", Util.getAllLiveNodesInJSON());
                    } else {
                        body.put("Response", "Incorrect permissions or arguments!!");

                    }
                } else if (command.equalsIgnoreCase("help")) {
                    StringBuilder helpMessage = new StringBuilder();
                    helpMessage.append("Manual of SIPS-Node API on UUID:")
                            .append(GlobalValues.NODE_UUID)
                            .append("\n\nCommands Supported:\n"
                                    + "\tnodes:\n"
                                    + "\tformat: nodes <option1> <option2> <option3> <option4> <option5>\n"
                                    + "\tExamples:\n"
                                    + "\t\tnodes show sort IP desc\n"
                                    + "\t\tnodes show adj sort IP desc\n"
                                    + "\t\tnodes show non-adj sort IP desc\n"
                                    + "\tOptions:\n"
                                    + "\t\tshow: list all the nodes, can be used with adj and non-adj option to specify the list to show otherwise all nodes will be listed\n"
                                    + "\t\t\tadj: show adjacent nodes\n"
                                    + "\t\t\tnon-adj: show non-adjacent nodes\n"
                                    + "\t\t\t\tsort <key> <order>: sort list\n"
                                    + "\t\t\t\t\tkeys:\n"
                                    + "\t\t\t\t\t\tIP,OS,HOST,QLEN,QWAIT,RAM,RAM_FREE,HDD_FREE,"
                                    + "HDD,HDD_READ_SPEED,HDD_WRITE_SPEED,CPU_COMPOSITE_SCORE,"
                                    + "CPU_MONTE_CARLO,CPU_FFT,CPU_LU,CPU_SOR,CPU_SPARSE_MAT_MUL,PROCESSOR\n"
                                    + "\t\t\t\t\torder:\n"
                                    + "\t\t\t\t\t\tasc: (default) ordered in ascending order\n"
                                    + "\t\t\t\t\t\tdesc: ordered in decending order\n");
                    if (args.length() > 0 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        body.put("Response", helpMessage.toString());
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        body.put("Response", helpMessage.toString());
                    } else {
                        body.put("Response", "Incorrect permissions or arguments!!");

                    }
                } else {
                    body.put("Response", "Command not available!!");
                }
                sendmsg2Json.put("Body", body);
                String sendmsg2 = sendmsg2Json.toString();
                byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                outToClient2.writeInt(bytes2.length);
                outToClient2.write(bytes2);
            }
            submitter.close();

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
