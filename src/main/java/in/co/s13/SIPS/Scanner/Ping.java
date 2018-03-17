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
package in.co.s13.SIPS.Scanner;

import in.co.s13.sips.lib.common.datastructure.Hop;
import in.co.s13.sips.lib.common.datastructure.UniqueElementList;

import in.co.s13.SIPS.datastructure.LiveDBRow;
import in.co.s13.SIPS.settings.GlobalValues;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import static in.co.s13.SIPS.settings.GlobalValues.CURRENTLY_SCANNING;
import static in.co.s13.SIPS.settings.GlobalValues.LIVE_DB_EXECUTOR;
import static in.co.s13.SIPS.settings.GlobalValues.LIVE_NODE_ADJ_DB;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.common.datastructure.Node;
import static in.co.s13.sips.lib.common.settings.GlobalValues.ADJACENT_NODES_TABLE;
import static in.co.s13.sips.lib.common.settings.GlobalValues.NON_ADJACENT_NODES_TABLE;

class Ping implements Runnable {

    InetAddress adrss;
    String IPadress, UUID;

    Ping(String ip, String uuid) {
        IPadress = ip.trim();
        if (IPadress.contains("%")) {
            IPadress = IPadress.substring(0, IPadress.indexOf("%"));
        }
        UUID = uuid;
    }

    public void run() {
        Thread.currentThread().setName("Ping-" + IPadress.trim());
        scan();
    }

    public void scan() {
        if (CURRENTLY_SCANNING.contains(IPadress.trim())) {
            Util.appendToPingLog(GlobalValues.LOG_LEVEL.OUTPUT, IPadress + " is Already In Scan List");
            return;
        } else {
            CURRENTLY_SCANNING.put(IPadress.trim(), IPadress);
        }
        Util.appendToPingLog(GlobalValues.LOG_LEVEL.OUTPUT, "Currently Scanning : " + CURRENTLY_SCANNING.toString());
        try {
            adrss = InetAddress.getByName(IPadress);
            if (adrss.isReachable(5000)) {
                Util.appendToPingLog(GlobalValues.LOG_LEVEL.OUTPUT, IPadress + " is Reachable");
            } else {
                Util.appendToPingLog(GlobalValues.LOG_LEVEL.ERROR, IPadress + " is not Reachable");
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
            Util.appendToPingLog(GlobalValues.LOG_LEVEL.ERROR, ex.toString());
        } catch (IOException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
            Util.appendToPingLog(GlobalValues.LOG_LEVEL.ERROR, ex.toString());
        }
        Socket s = new Socket();
        try {
            long startTime = System.currentTimeMillis();
            s.connect(new InetSocketAddress(IPadress, GlobalValues.PING_SERVER_PORT));
            String hostname;
            String osname, cpuname, uuid;
            long ram, freeRam, hdd_size, hdd_free;
            int task_limit, task_waiting;
            try (OutputStream os = s.getOutputStream(); DataInputStream dIn = new DataInputStream(s.getInputStream()); DataOutputStream outToServer = new DataOutputStream(os)) {
                JSONObject pingRequest = new JSONObject();
                pingRequest.put("Command", "ping");
                JSONObject pingRequestBody = new JSONObject();
                pingRequestBody.put("UUID",in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID);
                pingRequest.put("Body", pingRequestBody);
                String sendmsg = pingRequest.toString(4);
                byte[] bytes = sendmsg.getBytes("UTF-8");
                outToServer.writeInt(bytes.length);
                outToServer.write(bytes);

                int length = dIn.readInt();                    // read length of incoming message
                byte[] message = new byte[length];

                if (length > 0) {
                    dIn.readFully(message, 0, message.length); // read the message
                }
                long endTime = System.currentTimeMillis();
                JSONObject reply = new JSONObject(new String(message));
                osname = reply.getString("OS");
                hostname = reply.getString("HOSTNAME");
                task_limit = reply.getInt("TASK_LIMIT");
                task_waiting = reply.getInt("TASK_WAITING");
                ram = reply.getLong("TMEM");
                freeRam = reply.getInt("MEM_FREE");
                hdd_free = reply.getLong("HDD_FREE");
                hdd_size = reply.getLong("HDD_SIZE");
                uuid = reply.getString("UUID");
                double cpuload=reply.getDouble("CPULOAD", Double.MIN_VALUE);
                            
                long processingTime = reply.getLong("PROCESS_TIME");
                long distance = ((endTime - startTime) - (processingTime)) / 2;
                JSONObject adjacentNodes = reply.getJSONObject("ADJ_NODES");
                JSONObject nonAdjacentNodes = reply.getJSONObject("NON_ADJ_NODES");
                JSONObject benchmarks = reply.getJSONObject("BENCHMARKS");
                JSONObject liveNodes = reply.getJSONObject("LIVE_NODES");
                JSONObject nonAdjLiveNodes = reply.getJSONObject("NON_ADJ_LIVE_NODES");
                /**
                 * Time to send and recieve message - Processing time of request
                 * on that node Not very accurate to measure the distance
                 * between nodes in terms of time but very useful
                 */
                // String cpuload = reply.substring(reply.indexOf("<CPULOAD>") + 9, reply.indexOf("</CPULOAD>"));
                cpuname = reply.getString("CPUNAME");

                ArrayList<String> ips = new ArrayList<String>();
                JSONArray ipsJSONArray = reply.getJSONArray("IP_ADDRESSES", new JSONArray());
                for (int i = 0; i < ipsJSONArray.length(); i++) {
                    JSONObject ifaces = ipsJSONArray.getJSONObject(i);
                    ips.add(ifaces.getString("hostname"));
                    ips.add(ifaces.getString("ip"));
                }
                Util.appendToPingLog(GlobalValues.LOG_LEVEL.OUTPUT, "Reply from " + IPadress + " :" + reply.toString(4));
                Util.appendToPingLog(GlobalValues.LOG_LEVEL.OUTPUT, "Port Opened On " + IPadress);
                LIVE_DB_EXECUTOR.execute(() -> {

                    boolean updatedRecord = false;
                    if (LIVE_NODE_ADJ_DB.containsKey(IPadress.trim())) {
                        LIVE_NODE_ADJ_DB.remove(IPadress.trim());
                    }
                    // using endTime as Last Checked ON timestamp, as that is the time when we hear back from node
                    if (LIVE_NODE_ADJ_DB.containsKey(uuid)) {
                        LiveDBRow liveDBRow = new LiveDBRow(uuid, hostname, osname, cpuname, (task_limit), (task_waiting), ram, freeRam, hdd_size, hdd_free, benchmarks, endTime,cpuload);
                        Node toReplaced = LIVE_NODE_ADJ_DB.replace(uuid, liveDBRow);
                        updatedRecord = true;
                    } else {
                        LiveDBRow liveDBRow = new LiveDBRow(uuid, hostname, osname, cpuname, (task_limit), (task_waiting), ram, freeRam, hdd_size, hdd_free, benchmarks, endTime,cpuload);
                        LIVE_NODE_ADJ_DB.put(uuid, liveDBRow);

                    }
                    Node live = LIVE_NODE_ADJ_DB.get(uuid);
                    live.getIP(IPadress).setDistance(distance);
                    live.getIP(IPadress).incrementPingScore();
                    for (int i = 0; i < ips.size(); i++) {
                        String get = ips.get(i);
                        live.addIP(get);
                    }
//                    System.OUT.println("Live Node DB " + LIVE_NODE_ADJ_DB.toString());
                    if (ADJACENT_NODES_TABLE.containsKey(uuid)) {
                        ADJACENT_NODES_TABLE.replace(uuid, new Hop(uuid, distance));
                    } else {
                        ADJACENT_NODES_TABLE.put(uuid, new Hop(uuid, distance));
                    }
                    Iterator<String> keys = adjacentNodes.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        long dis = adjacentNodes.getLong(key, Long.MIN_VALUE);
//                        if (!ADJACENT_NODES_TABLE.containsKey(key)) 
                        {
                            if (NON_ADJACENT_NODES_TABLE.containsKey(key)) {
                                NON_ADJACENT_NODES_TABLE.get(key).addHop(new Hop(uuid, dis + distance));
                            } else {
                                NON_ADJACENT_NODES_TABLE.put(key, new UniqueElementList(new Hop(uuid, dis + distance)));
                            }
                        }
                    }

                    Iterator<String> keys2 = nonAdjacentNodes.keys();
                    while (keys2.hasNext()) {
                        String key = keys2.next();
                        JSONObject nonAdjacentNode = nonAdjacentNodes.getJSONObject(key);
                        long dis = nonAdjacentNode.getLong("distance", Long.MIN_VALUE);
//                        if (!ADJACENT_NODES_TABLE.containsKey(key)) 
                        {
                            if (NON_ADJACENT_NODES_TABLE.containsKey(key)) {
                                NON_ADJACENT_NODES_TABLE.get(key).addHop(new Hop(uuid, dis + distance));
                            } else {
                                NON_ADJACENT_NODES_TABLE.put(key, new UniqueElementList(new Hop(uuid, dis + distance)));
                            }
                        }
                    }

                    Iterator<String> keys3 = liveNodes.keys();
                    while (keys3.hasNext()) {
                        String key = keys3.next();
                        JSONObject othLiveNo = liveNodes.getJSONObject(key);
                        if (ADJACENT_NODES_TABLE.containsKey(key)) {
                            if (LIVE_NODE_ADJ_DB.containsKey(key)) {
                                Node livenode = LIVE_NODE_ADJ_DB.get(key);
                                if (livenode.getLastCheckAgo() > othLiveNo.getLong("lastCheckAgo", Long.MIN_VALUE)) {
                                    LIVE_NODE_ADJ_DB.replace(key, new LiveDBRow(othLiveNo));
                                }
                            } else {
                                LIVE_NODE_ADJ_DB.put(key, new LiveDBRow(othLiveNo));

                            }
//                            if (GlobalValues.LIVE_NODE_NON_ADJ_DB.containsKey(key)) {
//                                GlobalValues.LIVE_NODE_NON_ADJ_DB.remove(key);
//                            }

                        } else {
                            if (GlobalValues.LIVE_NODE_NON_ADJ_DB.containsKey(key)) {
                                Node livenode = GlobalValues.LIVE_NODE_NON_ADJ_DB.get(key);
                                if (livenode.getLastCheckAgo() > othLiveNo.getLong("lastCheckAgo", Long.MIN_VALUE)) {
                                    GlobalValues.LIVE_NODE_NON_ADJ_DB.replace(key, new LiveDBRow(othLiveNo));
                                }
                            } else {
                                GlobalValues.LIVE_NODE_NON_ADJ_DB.put(key, new LiveDBRow(othLiveNo));

                            }
                        }
                    }

                    Iterator<String> keys4 = nonAdjLiveNodes.keys();
                    while (keys4.hasNext()) {
                        String key = keys4.next();
                        JSONObject othLiveNo = nonAdjLiveNodes.getJSONObject(key);
                        if (ADJACENT_NODES_TABLE.containsKey(key)) {
                            if (LIVE_NODE_ADJ_DB.containsKey(key)) {
                                Node livenode = LIVE_NODE_ADJ_DB.get(key);
                                if (livenode.getLastCheckAgo() > othLiveNo.getLong("lastCheckAgo", Long.MIN_VALUE)) {
                                    LIVE_NODE_ADJ_DB.replace(key, new LiveDBRow(othLiveNo));
                                }
                            } else {
                                LIVE_NODE_ADJ_DB.put(key, new LiveDBRow(othLiveNo));

                            }

//                            if (GlobalValues.LIVE_NODE_NON_ADJ_DB.containsKey(key)) {
//                                GlobalValues.LIVE_NODE_NON_ADJ_DB.remove(key);
//                            }
                        } else {
                            if (GlobalValues.LIVE_NODE_NON_ADJ_DB.containsKey(key)) {
                                Node livenode = GlobalValues.LIVE_NODE_NON_ADJ_DB.get(key);
                                if (livenode.getLastCheckAgo() > othLiveNo.getLong("lastCheckAgo", Long.MIN_VALUE)) {
                                    GlobalValues.LIVE_NODE_NON_ADJ_DB.replace(key, new LiveDBRow(othLiveNo));
                                }
                            } else {
                                GlobalValues.LIVE_NODE_NON_ADJ_DB.put(key, new LiveDBRow(othLiveNo));

                            }
                        }
                    }

                });
                Util.appendToPingLog(GlobalValues.LOG_LEVEL.OUTPUT, "\n\n**************************Live Nodes******************* \n" + Util.getAllLiveNodesInJSON().toString(4));
            }


            s.close();

        } catch (IOException ex) {
            Util.appendToPingLog(GlobalValues.LOG_LEVEL.ERROR, IPadress + " is dead:" + ex);
            Node node = LIVE_NODE_ADJ_DB.get(UUID.trim());
            if (node != null) {
                if (node.getIpAddresses().values().size() > 1) {
                    node.getIpAddresses().get(IPadress.trim()).decrementPingScore();
                    LIVE_NODE_ADJ_DB.replace(UUID.trim(), node);
                } else {
                    LIVE_NODE_ADJ_DB.remove(UUID.trim());
                    ADJACENT_NODES_TABLE.remove(UUID.trim());
                }
            }
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        CURRENTLY_SCANNING.remove(IPadress.trim());

    }

}
