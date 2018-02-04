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
import in.co.s13.SIPS.settings.Settings;
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
//            System.out.println("IP adress of sender is " + ipAddress);
            Thread.currentThread().setName("API handler for " + ipAddress);
            Util.appendToApiLog(GlobalValues.LOG_LEVEL.OUTPUT, "API Request From " + ipAddress);
            if (msg.length() > 1) {
                //System.OUT.println("hurray cond 1");
                int key_permissions = 0;//default value to 0, no harm done by malformed key
                Util.appendToApiLog(GlobalValues.LOG_LEVEL.OUTPUT, "Recieved API Request " + msg.toString(4));
                String command = msg.getString("Command");
                JSONObject requestBody = msg.getJSONObject("Body");;
                String clientUUID = requestBody.getString("UUID");
                JSONArray args = requestBody.getJSONArray("ARGS");
//                System.out.println("ARGS : " + args.toString());
                String apiKey = requestBody.getString("API_KEY");
                if ((GlobalValues.BLACKLIST.containsKey(ipAddress) || GlobalValues.BLACKLIST.containsKey(clientUUID))
                        && (!GlobalValues.API_LIST.containsKey(clientUUID) || !GlobalValues.API_LIST.containsKey(ipAddress))) {
                    //send error message
                    // bad node no cookie for u

                    JSONObject sendmsg2Json = new JSONObject();
                    sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                    JSONObject body = new JSONObject();
                    JSONObject response = new JSONObject();
                    response.put("Message", "Error!!\n \tYou are not allowed.");
                    body.put("Response", response);
                    sendmsg2Json.put("Body", body);
                    String sendmsg2 = sendmsg2Json.toString();
                    Util.appendToApiLog(GlobalValues.LOG_LEVEL.OUTPUT, sendmsg2);
                    byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                    outToClient2.writeInt(bytes2.length);
                    outToClient2.write(bytes2);

                    submitter.close();
                    return;
                } else if ((!GlobalValues.BLACKLIST.containsKey(ipAddress) || !GlobalValues.BLACKLIST.containsKey(clientUUID))) {
                    JSONObject keyInfo = null;
                    if (GlobalValues.API_LIST.containsKey(clientUUID.trim())) {
                        keyInfo = GlobalValues.API_LIST.get(clientUUID.trim());
                        if (keyInfo == null) {
                            keyInfo = GlobalValues.API_JSON.getJSONObject(clientUUID.trim());
                        }
                    } else if (GlobalValues.API_LIST.containsKey(ipAddress)) {
                        keyInfo = GlobalValues.API_LIST.get(ipAddress);
                        if (keyInfo == null) {
                            keyInfo = GlobalValues.API_JSON.getJSONObject(ipAddress);
                        }

                    } else {
                        JSONObject sendmsg2Json = new JSONObject();
                        sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                        JSONObject body = new JSONObject();
                        JSONObject response = new JSONObject();
                        response.put("Message", "Error!!\n \tYou are not allowed.");
                        body.put("Response", response);
                        sendmsg2Json.put("Body", body);
                        String sendmsg2 = sendmsg2Json.toString();
                        byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                        outToClient2.writeInt(bytes2.length);
                        outToClient2.write(bytes2);
                        submitter.close();
                        Util.appendToApiLog(GlobalValues.LOG_LEVEL.OUTPUT, sendmsg2);
                        return;
                    }
                    System.out.println("" + GlobalValues.API_LIST.toString());
                    String key = keyInfo.getString("key");
                    key_permissions = keyInfo.getInt("permissions");;
                    if (!key.trim().equalsIgnoreCase(apiKey.trim())) {
                        JSONObject sendmsg2Json = new JSONObject();
                        sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                        JSONObject body = new JSONObject();
                        JSONObject response = new JSONObject();
                        response.put("Message", "Error!!\n \tIncorrect API key.");
                        body.put("Response", response);
                        sendmsg2Json.put("Body", body);
                        String sendmsg2 = sendmsg2Json.toString();
                        byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                        outToClient2.writeInt(bytes2.length);
                        outToClient2.write(bytes2);
                        submitter.close();
                        Util.appendToApiLog(GlobalValues.LOG_LEVEL.OUTPUT, sendmsg2);
                        return;
                    }
                }
                JSONObject sendmsg2Json = new JSONObject();
                sendmsg2Json.put("UUID", GlobalValues.NODE_UUID);
                JSONObject body = new JSONObject();
                JSONObject response = new JSONObject();
                if (command.equalsIgnoreCase("TestConnection")) {
                    body.put("Response", "Connection Successful");
                } else if (command.equalsIgnoreCase("blacklist")) {
                    if (args.length() == 1 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getBlackListInJSON());
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getBlackListInJSON());
                    } else if (!hasReadPermissions(key_permissions)) {
                        response.put("Message", "Error!!\n \tIncorrect permissions.");
                        body.put("Response", response);
                    } else {
                        response.put("Message", "Error!!\n \tIncorrect arguments.");
                        body.put("Response", response);
                    }
                } else if (command.equalsIgnoreCase("adjacent")) {
                    if (args.length() == 1 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getAdjacentTableInJSON());
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getAdjacentTableInJSON());
                    } else if (!hasReadPermissions(key_permissions)) {
                        response.put("Message", "Error!!\n \tIncorrect permissions.");
                        body.put("Response", response);
                    } else {
                        response.put("Message", "Error!!\n \tIncorrect arguments.");
                        body.put("Response", response);
                    }
                } else if (command.equalsIgnoreCase("service")) {

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
                        } else if (args.getString(0).equalsIgnoreCase("LOG-ROTATE")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                ServiceOperations.startLogRotate();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                ServiceOperations.stopLogRotate();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                ServiceOperations.restartLogRotate();
                            }
                            response.put("LOG-ROTATE", GlobalValues.LOG_ROTATE_THREAD.isAlive());
                        } else if (args.getString(0).equalsIgnoreCase("CLEAN-RESULT-DB")) {
                            if (args.getString(1).equalsIgnoreCase("start")) {
                                ServiceOperations.startCleanResultDB();
                            } else if (args.getString(1).equalsIgnoreCase("stop")) {
                                ServiceOperations.stopCleanResultDB();
                            } else if (args.getString(1).equalsIgnoreCase("restart")) {
                                ServiceOperations.restartCleanResultDB();
                            }
                            response.put("CLEAN-RESULT-DB", GlobalValues.CLEAN_RESULT_DB_THREAD.isAlive());
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
                        response.put("LOG-ROTATE", GlobalValues.LOG_ROTATE_THREAD.isAlive());
                        body.put("Response", response);

                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        response.put("PING-SERVER", !GlobalValues.PING_SERVER_SOCKET.isClosed());
                        response.put("FILE-SERVER", !GlobalValues.FILE_SERVER_SOCKET.isClosed());
                        response.put("FILE-DOWNLOAD-SERVER", !GlobalValues.FILE_DOWNLOAD_SERVER_SOCKET.isClosed());
                        response.put("TASK-SERVER", !GlobalValues.TASK_SERVER_SOCKET.isClosed());
                        response.put("API-SERVER", !GlobalValues.API_SERVER_SOCKET.isClosed());
                        response.put("LIVE-NODE-SCANNER", GlobalValues.CHECK_LIVE_NODE_THREAD.isAlive());
                        response.put("NODE-SCANNER", GlobalValues.NODE_SCANNING_THREAD.isAlive());
                        response.put("LOG-ROTATE", GlobalValues.LOG_ROTATE_THREAD.isAlive());
                        body.put("Response", response);
                    } else {
                        body.put("Response", "Incorrect permissions or arguments!!");

                    }
                } else if (command.equalsIgnoreCase("non-adjacent")) {
                    if (args.length() > 0 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getNonAdjacentTableInJSON());
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        body.put("Response", Util.getNonAdjacentTableInJSON());
                    } else if (!hasReadPermissions(key_permissions)) {
                        response.put("Message", "Error!!\n \tIncorrect permissions.");
                        body.put("Response", response);
                    } else {
                        response.put("Message", "Error!!\n \tIncorrect arguments.");
                        body.put("Response", response);
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
                    } else if (args.length() == 2
                            && args.getString(0).equalsIgnoreCase("show")
                            && args.getString(1).equalsIgnoreCase("adj")
                            && hasReadPermissions(key_permissions)) {
                        //nodes show adj
                        body.put("Response", Util.getAdjLiveNodesInJSON());
                    } else if (args.length() == 2
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
                    } else if (!hasReadPermissions(key_permissions)) {
                        response.put("Message", "Error!!\n \tIncorrect permissions.");
                        body.put("Response", response);
                    } else {
                        response.put("Message", "Error!!\n \tIncorrect arguments.");
                        body.put("Response", response);
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
                                    + "\t\t\t\t\t\tdesc: ordered in decending order\n"
                                    + "\tset:\n"
                                    + "\tformat:set <key> <value>\n"
                                    + "\tExamples:\n"
                                    + "");
                    if (args.length() > 0 && args.getString(0).equalsIgnoreCase("show") && hasReadPermissions(key_permissions)) {
                        response.put("Message", helpMessage.toString());
                        body.put("Response", response);
                    } else if (args.length() == 0 && hasReadPermissions(key_permissions)) {
                        response.put("Message", helpMessage.toString());
                        body.put("Response", response);
                    } else if (!hasReadPermissions(key_permissions)) {
                        response.put("Message", "Error!!\n \tIncorrect permissions.");
                        body.put("Response", response);
                    } else {
                        response.put("Message", "Error!!\n \tIncorrect arguments.");
                        body.put("Response", response);
                    }
                } else if (command.equalsIgnoreCase("set")) {

                    if (args.length() == 2 && args.getString(0).equalsIgnoreCase("VERBOSE") && hasWritePermissions(key_permissions)) {
                        GlobalValues.VERBOSE = args.getBoolean(1);
                        response.put("VERBOSE", GlobalValues.VERBOSE);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("DUMP_LOG") && hasWritePermissions(key_permissions)) {
                        GlobalValues.DUMP_LOG = args.getBoolean(1);
                        response.put("DUMP_LOG", GlobalValues.DUMP_LOG);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("TOTAL_IP_SCANNING_THREADS") && hasWritePermissions(key_permissions)) {
                        GlobalValues.TOTAL_IP_SCANNING_THREADS = args.getInt(1);
                        response.put("TOTAL_IP_SCANNING_THREADS", GlobalValues.TOTAL_IP_SCANNING_THREADS);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("FILES_RESOLVER_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.FILES_RESOLVER_LIMIT = args.getInt(1);
                        response.put("FILES_RESOLVER_LIMIT", GlobalValues.FILES_RESOLVER_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("FILE_HANDLER_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.FILE_HANDLER_LIMIT = args.getInt(1);
                        response.put("FILE_HANDLER_LIMIT", GlobalValues.FILE_HANDLER_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("PING_HANDLER_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.PING_HANDLER_LIMIT = args.getInt(1);
                        response.put("PING_HANDLER_LIMIT", GlobalValues.PING_HANDLER_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("PING_REQUEST_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.PING_REQUEST_LIMIT = args.getInt(1);
                        response.put("PING_REQUEST_LIMIT", GlobalValues.PING_REQUEST_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("API_HANDLER_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.API_HANDLER_LIMIT = args.getInt(1);
                        response.put("API_HANDLER_LIMIT", GlobalValues.API_HANDLER_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("TASK_HANDLER_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.TASK_HANDLER_LIMIT = args.getInt(1);
                        response.put("TASK_HANDLER_LIMIT", GlobalValues.TASK_HANDLER_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("TASK_FINISH_LISTENER_HANDLER_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.TASK_FINISH_LISTENER_HANDLER_LIMIT = args.getInt(1);
                        response.put("TASK_FINISH_LISTENER_HANDLER_LIMIT", GlobalValues.TASK_FINISH_LISTENER_HANDLER_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("TASK_LIMIT") && hasWritePermissions(key_permissions)) {
                        GlobalValues.TASK_LIMIT = args.getInt(1);
                        response.put("TASK_LIMIT", GlobalValues.TASK_LIMIT);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("PING_SERVER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.PING_SERVER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("PING_SERVER_ENABLED_AT_START", GlobalValues.PING_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("LOG_ROTATE_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.LOG_ROTATE_ENABLED_AT_START = args.getBoolean(1);
                        response.put("LOG_ROTATE_ENABLED_AT_START", GlobalValues.LOG_ROTATE_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("API_SERVER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.API_SERVER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("API_SERVER_ENABLED_AT_START", GlobalValues.API_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("FILE_DOWNLOAD_SERVER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.FILE_DOWNLOAD_SERVER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("FILE_DOWNLOAD_SERVER_ENABLED_AT_START", GlobalValues.FILE_DOWNLOAD_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("FILE_SERVER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.FILE_SERVER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("FILE_SERVER_ENABLED_AT_START", GlobalValues.FILE_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("TASK_SERVER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.TASK_SERVER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("TASK_SERVER_ENABLED_AT_START", GlobalValues.TASK_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("NODE_SCANNER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.NODE_SCANNER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("NODE_SCANNER_ENABLED_AT_START", GlobalValues.NODE_SCANNER_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("LIVE_NODE_SCANNER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.LIVE_NODE_SCANNER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("LIVE_NODE_SCANNER_ENABLED_AT_START", GlobalValues.LIVE_NODE_SCANNER_ENABLED_AT_START);

                    } else if (args.length() == 2 && args.getString(0).equalsIgnoreCase("TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START") && hasWritePermissions(key_permissions)) {
                        GlobalValues.TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START = args.getBoolean(1);
                        response.put("TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START", GlobalValues.TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START);

                    } else if (!hasWritePermissions(key_permissions)) {
                        response.put("Message", "Error!!\n \tIncorrect permissions.");
                    } else {
                        response.put("Message", "Error!!\n \tIncorrect arguments.");
                    }
                    body.put("Response", response);
                    Settings.saveSettings();
                } else if (command.equalsIgnoreCase("get")) {

                    if (args.length() == 1 && args.getString(0).equalsIgnoreCase("VERBOSE") && hasReadPermissions(key_permissions)) {
                        response.put("VERBOSE", GlobalValues.VERBOSE);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("DUMP_LOG") && hasReadPermissions(key_permissions)) {
                        response.put("DUMP_LOG", GlobalValues.DUMP_LOG);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("TOTAL_IP_SCANNING_THREADS") && hasReadPermissions(key_permissions)) {
                        response.put("TOTAL_IP_SCANNING_THREADS", GlobalValues.TOTAL_IP_SCANNING_THREADS);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("FILES_RESOLVER_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("FILES_RESOLVER_LIMIT", GlobalValues.FILES_RESOLVER_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("FILE_HANDLER_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("FILE_HANDLER_LIMIT", GlobalValues.FILE_HANDLER_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("PING_HANDLER_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("PING_HANDLER_LIMIT", GlobalValues.PING_HANDLER_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("PING_REQUEST_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("PING_REQUEST_LIMIT", GlobalValues.PING_REQUEST_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("API_HANDLER_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("API_HANDLER_LIMIT", GlobalValues.API_HANDLER_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("TASK_HANDLER_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("TASK_HANDLER_LIMIT", GlobalValues.TASK_HANDLER_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("TASK_FINISH_LISTENER_HANDLER_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("TASK_FINISH_LISTENER_HANDLER_LIMIT", GlobalValues.TASK_FINISH_LISTENER_HANDLER_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("TASK_LIMIT") && hasReadPermissions(key_permissions)) {
                        response.put("TASK_LIMIT", GlobalValues.TASK_LIMIT);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("PING_SERVER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("PING_SERVER_ENABLED_AT_START", GlobalValues.PING_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("LOG_ROTATE_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("LOG_ROTATE_ENABLED_AT_START", GlobalValues.LOG_ROTATE_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("API_SERVER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("API_SERVER_ENABLED_AT_START", GlobalValues.API_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("FILE_DOWNLOAD_SERVER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("FILE_DOWNLOAD_SERVER_ENABLED_AT_START", GlobalValues.FILE_DOWNLOAD_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("FILE_SERVER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("FILE_SERVER_ENABLED_AT_START", GlobalValues.FILE_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("TASK_SERVER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("TASK_SERVER_ENABLED_AT_START", GlobalValues.TASK_SERVER_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("NODE_SCANNER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("NODE_SCANNER_ENABLED_AT_START", GlobalValues.NODE_SCANNER_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("LIVE_NODE_SCANNER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("LIVE_NODE_SCANNER_ENABLED_AT_START", GlobalValues.LIVE_NODE_SCANNER_ENABLED_AT_START);

                    } else if (args.length() == 1 && args.getString(0).equalsIgnoreCase("TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START") && hasReadPermissions(key_permissions)) {
                        response.put("TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START", GlobalValues.TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START);

                    } else if (!hasReadPermissions(key_permissions)) {
                        response.put("Message", "Error!!\n \tIncorrect permissions.");
                    } else {
                        response.put("VERBOSE", GlobalValues.VERBOSE);
                        response.put("DUMP_LOG", GlobalValues.DUMP_LOG);
                        response.put("TOTAL_IP_SCANNING_THREADS", GlobalValues.TOTAL_IP_SCANNING_THREADS);
                        response.put("FILES_RESOLVER_LIMIT", GlobalValues.FILES_RESOLVER_LIMIT);
                        response.put("FILE_HANDLER_LIMIT", GlobalValues.FILE_HANDLER_LIMIT);
                        response.put("PING_HANDLER_LIMIT", GlobalValues.PING_HANDLER_LIMIT);
                        response.put("PING_REQUEST_LIMIT", GlobalValues.PING_REQUEST_LIMIT);
                        response.put("API_HANDLER_LIMIT", GlobalValues.API_HANDLER_LIMIT);
                        response.put("TASK_HANDLER_LIMIT", GlobalValues.TASK_HANDLER_LIMIT);
                        response.put("TASK_FINISH_LISTENER_HANDLER_LIMIT", GlobalValues.TASK_FINISH_LISTENER_HANDLER_LIMIT);
                        response.put("TASK_LIMIT", GlobalValues.TASK_LIMIT);
                        response.put("PING_SERVER_ENABLED_AT_START", GlobalValues.PING_SERVER_ENABLED_AT_START);
                        response.put("LOG_ROTATE_ENABLED_AT_START", GlobalValues.LOG_ROTATE_ENABLED_AT_START);
                        response.put("API_SERVER_ENABLED_AT_START", GlobalValues.API_SERVER_ENABLED_AT_START);
                        response.put("FILE_DOWNLOAD_SERVER_ENABLED_AT_START", GlobalValues.FILE_DOWNLOAD_SERVER_ENABLED_AT_START);
                        response.put("FILE_SERVER_ENABLED_AT_START", GlobalValues.FILE_SERVER_ENABLED_AT_START);
                        response.put("TASK_SERVER_ENABLED_AT_START", GlobalValues.TASK_SERVER_ENABLED_AT_START);
                        response.put("NODE_SCANNER_ENABLED_AT_START", GlobalValues.NODE_SCANNER_ENABLED_AT_START);
                        response.put("LIVE_NODE_SCANNER_ENABLED_AT_START", GlobalValues.LIVE_NODE_SCANNER_ENABLED_AT_START);
                        response.put("TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START", GlobalValues.TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START);

                    }
                    body.put("Response", response);
                } else {
                    body.put("Response", "Command not available!!");
                }
                sendmsg2Json.put("Body", body);
                String sendmsg2 = sendmsg2Json.toString();
                byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                outToClient2.writeInt(bytes2.length);
                outToClient2.write(bytes2);
                Util.appendToApiLog(GlobalValues.LOG_LEVEL.OUTPUT, sendmsg2);
            }
            submitter.close();

        } catch (IOException ex) {
            Logger.getLogger(APIHandler.class.getName()).log(Level.SEVERE, null, ex);
            Util.appendToApiLog(GlobalValues.LOG_LEVEL.ERROR, ex.toString());
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
