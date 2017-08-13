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

import in.co.s13.SIPS.datastructure.Hop;
import in.co.s13.SIPS.datastructure.UniqueElementList;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.*;
import static in.co.s13.SIPS.tools.Util.errPrintln;
import static in.co.s13.SIPS.tools.Util.outPrintln;
import in.co.s13.SIPS.virtualdb.LiveDBRow;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

class Ping implements Runnable {

    InetAddress adrss;
    String IPadress, UUID;
    String msg = "";
    Boolean live = false;

    Ping(String ip, String uuid) {
        IPadress = ip.trim();
        UUID = uuid;
    }

    public void run() {
        Thread.currentThread().setName("Ping-" + IPadress.trim());
        scan();
    }

    public void scan() {
        if (scanning.contains(IPadress.trim())) {
            outPrintln(IPadress + " is Already In Scan List");
            return;
        } else {
            scanning.put(IPadress.trim(), IPadress);
        }
        System.out.println("Scanning List:"+scanning.toString());
        try {
            adrss = InetAddress.getByName(IPadress);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if (adrss.isReachable(5000)) {
                outPrintln(IPadress + " is Reachable");
            } else {
                outPrintln(IPadress + " is not Reachable");
            }
        } catch (IOException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
        }
        Socket s = new Socket();
        try {
            long startTime = System.currentTimeMillis();
            s.connect(new InetSocketAddress(IPadress, GlobalValues.PING_SERVER_PORT));
            String hostname;
            String osname, cpuname, uuid;
            long ram, freeRam, hdd_size, hdd_free;
            int plimit, pwait;
            try (OutputStream os = s.getOutputStream(); DataInputStream dIn = new DataInputStream(s.getInputStream()); DataOutputStream outToServer = new DataOutputStream(os)) {
                JSONObject pingRequest = new JSONObject();
                pingRequest.put("Command", "ping");
                JSONObject pingRequestBody = new JSONObject();
                pingRequestBody.put("UUID", GlobalValues.NODE_UUID);
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
                plimit = reply.getInt("PLIMIT");
                pwait = reply.getInt("PWAIT");
                ram = reply.getLong("TMEM");
                freeRam = reply.getInt("MEM_FREE");
                hdd_free = reply.getLong("HDD_FREE");
                hdd_size = reply.getLong("HDD_SIZE");
                uuid = reply.getString("UUID");
                long processingTime = reply.getLong("PROCESS_TIME");
                long distance = ((endTime - startTime) - (processingTime)) / 2;
                JSONObject adjacentNodes = reply.getJSONObject("ADJ_NODES");
                JSONObject nonAdjacentNodes = reply.getJSONObject("NON_ADJ_NODES");
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
                    JSONObject ifaces=ipsJSONArray.getJSONObject(i);
                    ips.add(ifaces.getString("hostname"));
                    ips.add(ifaces.getString("ip"));
                }
                outPrintln("" + reply.toString(4));
                System.out.println(reply);
                outPrintln("Port Opened On " + IPadress);
                liveDBExecutor.execute(() -> {
                    /*   String sql3 = "UPDATE LIVE SET "
                                 + "PRF ='" + prf2 + "',"
                                 + "CN ='" + cnm + "',"
                                 + "HN ='" + hostname + "',"
                                 + "OS ='" + osname + "',"
                                 + "CPU ='" + cpuname + "',"
                                 + "QL ='" + plimit + "',"
                                 + "QW ='" + pwait + "',"
                                 + "RAM ='" + ram + "' WHERE IP ='" + IPadress + "';";
                     */
                    //            SQLiteJDBC livedb2 = new SQLiteJDBC();
                    //                livedb2.Update("appdb/live.db", sql3);
                    //               livedb2.closeConnection();
                    boolean updatedRecord = false;
                    if (liveNodeDB.containsKey(IPadress.trim())) {
                        liveNodeDB.remove(IPadress.trim());
                    }

                    if (liveNodeDB.containsKey(uuid)) {
                        liveNodeDB.replace(uuid, new LiveDBRow(uuid, hostname, osname, cpuname, (plimit), (pwait), ram, freeRam, hdd_size, hdd_free));
                        updatedRecord = true;
                    } else {
                        liveNodeDB.put(uuid, new LiveDBRow(uuid, hostname, osname, cpuname, (plimit), (pwait), ram, freeRam, hdd_size, hdd_free));

                    }
                    LiveDBRow live = liveNodeDB.get(uuid);
                    for (int i = 0; i < ips.size(); i++) {
                        String get = ips.get(i);
                        live.addIp(get);
                    }

                    if (ADJACENT_NODES_TABLE.containsKey(uuid)) {
                        ADJACENT_NODES_TABLE.replace(uuid, distance);
                    } else {
                        ADJACENT_NODES_TABLE.put(uuid, distance);
                    }
                    Iterator<String> keys = adjacentNodes.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        long dis = adjacentNodes.getLong(key, Long.MIN_VALUE);
                        if (NON_ADJACENT_NODES_TABLE.containsKey(key)) {
                             NON_ADJACENT_NODES_TABLE.get(key).addHop(new Hop(uuid, dis+distance));
                        } else {
                            NON_ADJACENT_NODES_TABLE.put(key, new UniqueElementList(new Hop(uuid, dis+distance)));
                        }
                    }
                    
                    Iterator<String> keys2 = nonAdjacentNodes.keys();
                    while (keys2.hasNext()) {
                        String key = keys2.next();
                        long dis = nonAdjacentNodes.getLong(key, Long.MIN_VALUE);
                        if (NON_ADJACENT_NODES_TABLE.containsKey(key)) {
                             NON_ADJACENT_NODES_TABLE.get(key).addHop(new Hop(uuid, dis+distance));
                        } else {
                            NON_ADJACENT_NODES_TABLE.put(key, new UniqueElementList(new Hop(uuid, dis+distance)));
                        }
                    }

                });
                boolean isinlist = false;
                nodeDBExecutor.execute(() -> {
//                    //     boolean isinlist1 = false;
//                    try {
//                        String sql = "SELECT * FROM ALLN WHERE IP='" + IPadress.trim() + "';";
//                        try (ResultSet rs = alldb.select(sql)) {
//                            String clustername = "";
//                            Double prf = 0.1;
//                            while (rs.next()) {
//                                clustername = rs.getString("CLUSTER");
//                                prf = rs.getDouble("PRFM");
//                            }
//                            if (!(clustername.trim().length() > 0) || clustername.equalsIgnoreCase(" ")) {
//                                clustername = "Default";
//                            }
//                            final double prf2 = prf;
//                            final String cnm = clustername;
//
//                        }
//                        alldb.closeStatement();
//                    } catch (SQLException ex) {
//                        Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                });

            }

            s.close();

        } catch (IOException ex) {
            msg = "" + IPadress + " is dead";
            errPrintln(IPadress + " " + ex);
            liveNodeDB.remove(IPadress.trim());
            liveNodeDB.remove(UUID.trim());
            ADJACENT_NODES_TABLE.remove(UUID.trim());
            try {
                s.close();
            } catch (IOException ex1) {
                Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        scanning.remove(IPadress.trim());

    }

    public boolean isLive() {

        return live;
    }

}
