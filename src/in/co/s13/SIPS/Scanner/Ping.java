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

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.settings.Settings;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

class Ping implements Runnable {

    InetAddress adrss;
    String IPadress, UUID;
    String msg = "";
    Boolean live = false;
    static int dbcounter = 1;
    String hostname;
    String osname, cpuname, uuid;
    long ram, freeRam;
    int plimit, pwait;

    // static ArrayList livehosts = new ArrayList();
    Ping(String ip, String uuid) {
        IPadress = ip.trim();
        UUID = uuid;
        // scan();
    }

    public void run() {
//scan(IPadress);
        Thread.currentThread().setName("Ping-" + IPadress.trim());
        if (dbcounter == 1) {

        }
        scan();
        dbcounter++;
    }

    public void scan() {
        if (scanning.contains(IPadress.trim())) {
            outPrintln(IPadress + " is Already In Scan List");
            return;
        } else {
            scanning.put(IPadress.trim(), IPadress);
        }

        try {
            adrss = InetAddress.getByName(IPadress);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if (adrss.isReachable(5000)) {
                outPrintln(IPadress + " is Reachable");
                //    System.out.println("Reachable");
            } else {
                outPrintln(IPadress + " is not Reachable");
                /*  nodeDBExecutor.execute(new UpdateAllNodeDB("UPDATE ALLN SET"
                 + " STATUS ='NOT REACHABLE'"
                 + "WHERE IP='" + IPadress.trim() + "';"));
                 */
                // System.out.println("Not Reachable");
//                nodeDBExecutor.execute(() -> {
//                    allNodeDB.stream().filter((get) -> (get.getFirstName().trim().equalsIgnoreCase(IPadress.trim()))).forEach((get) -> {
//                        get.setNStatus("NOT REACHABLE");
//                    });
//                });

            }
        } catch (IOException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
        }
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(IPadress, 13131));

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
                JSONObject reply = new JSONObject(new String(message));
                osname = reply.getString("OS");
                hostname = reply.getString("HOSTNAME");
                plimit = reply.getInt("PLIMIT");
                pwait = reply.getInt("PWAIT");
                ram = reply.getInt("TMEM");
                uuid = reply.getString("UUID");
                // String cpuload = reply.substring(reply.indexOf("<CPULOAD>") + 9, reply.indexOf("</CPULOAD>"));
                cpuname = reply.getString("CPUNAME");
                outPrintln("" + reply.toString(4));
                System.out.println(reply);
                outPrintln("Port Opened On " + IPadress);

                boolean isinlist = false;
                nodeDBExecutor.execute(() -> {
                    //     boolean isinlist1 = false;
                    try {
                        String sql = "SELECT * FROM ALLN WHERE IP='" + IPadress.trim() + "';";
                        try (ResultSet rs = alldb.select(sql)) {
                            String clustername = "";
                            Double prf = 0.1;
                            while (rs.next()) {
                                clustername = rs.getString("CLUSTER");
                                prf = rs.getDouble("PRFM");
                            }
                            if (!(clustername.trim().length() > 0) || clustername.equalsIgnoreCase(" ")) {
                                clustername = "Default";
                            }
                            final double prf2 = prf;
                            final String cnm = clustername;
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
                                    liveNodeDB.replace(uuid, new LiveDBRow(uuid, hostname, osname, cpuname, (plimit), (pwait), ram, freeRam));
                                    updatedRecord = true;
                                } else {

                                }

//                                for (LiveDBRow liveNodeDB : liveNodeDB) {
//                                    if (liveNodeDB.getName().trim().equalsIgnoreCase(IPadress.trim())) {
//                                        liveNodeDB.setSalary(prf2);
//                                        liveNodeDB.setClusterName(cnm);
//                                        liveNodeDB.setHostName(hostname);
//                                        liveNodeDB.setOsName(osname);
//                                        liveNodeDB.setCpuName(cpuname);
//                                        liveNodeDB.setPLimit(Integer.parseInt(plimit));
//                                        liveNodeDB.setPWait(Integer.parseInt(pwait));
//                                        liveNodeDB.setTMem(Long.parseLong(ram));
//                                        updatedRecord = true;
//                                    }
//                                }
//
//                                if (!updatedRecord) {
//                                    liveNodeDB.add(new LiveNode(IPadress, prf2, cnm, hostname, osname, cpuname, Integer.parseInt(plimit), Integer.parseInt(pwait), Long.parseLong(ram)));
//                                }
                                /*
                                 sql3 = "INSERT INTO LIVE "
                                 + "(IP, "
                                 + "PRF,"
                                 + "CN,"
                                 + "HN,"
                                 + "OS,"
                                 + "CPU,"
                                 + "QL,"
                                 + "QW,"
                                 + "RAM) VALUES('" + IPadress + "','"
                                 + prf2 + "','"
                                 + cnm + "','"
                                 + hostname + "','"
                                 + osname + "','"
                                 + cpuname + "','"
                                 + plimit + "','"
                                 + pwait + "','"
                                 + ram + "');";
                                 //         livedb2.insert("appdb/live.db", sql3);
                                 //        livedb2.closeConnection();*/
                            });

                            /*for (int i=0;i< NetScanner.liveNodes.size();i++) {
                             LiveNode get = NetScanner.liveNodes.get(i);
                             if (IPadress.trim().equalsIgnoreCase(get.getName().trim()));
                             {
                             isinlist1 = true;
                             /*     get.setOsName(osname);
                             get.setHostName(hostname);
                             get.setPLimit(Integer.parseInt(plimit));
                             get.setPWait(Integer.parseInt(pwait));
                             get.setTMem(Long.parseLong(ram));
                             get.setCpuName(cpuname);
                             get.setClusterName(clustername);
                             get.setSalary(prf);
                             get.setSalaryPercentage(prf);
                                  
                             NetScanner.liveNodes.remove(i);
                             break;
                             }
                             }
                             //if (!isinlist1)
                             {
                             NetScanner.liveNodes.add(new LiveNode(IPadress, prf,  clustername, hostname, osname, cpuname, Integer.parseInt(plimit), Integer.parseInt(pwait), Long.parseLong(ram)));
                             }*/
                        }
                        alldb.closeStatement();
                    } catch (SQLException ex) {
                        Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

            }

            s.close();

            /* nodeDBExecutor.execute(new UpdateAllNodeDB("UPDATE ALLN SET"
             + " LEN ='" + IPadress.trim().length() + "',"
             + " STATUS ='ONLINE',"
             + "OS ='" + osname + "',"
             + "HOST ='" + hostname + "',"
             + "QLEN ='" + plimit + "',"
             + "QWAIT ='" + pwait + "',"
             + "RAM ='" + ram + "',"
             + "PROCESSOR ='" + cpuname + "' WHERE IP='" + IPadress.trim() + "';"));
             */
//            nodeDBExecutor.execute(() -> {
//
//                System.out.println("Checking if " + IPadress + " exists in All");
//                for (IPAddress get : allNodeDB) {
//                    if (get.getFirstName().trim().equalsIgnoreCase(IPadress.trim())) {
//                        get.setNStatus("ONLINE");
//                        get.setOperatingSystem(osname);
//                        get.setHostName(hostname);
//                        get.setProcessLimit(Integer.parseInt(plimit));
//                        get.setTotalMem(Long.parseLong(ram));
//                        get.setProcessWaiting(Integer.parseInt(pwait));
//                        get.setCpuName(cpuname);
//                        System.out.println("Set OnLine on " + IPadress);
//                    }
//                }
//            });
        } catch (IOException ex) {
            msg = "" + IPadress + " is dead";
            errPrintln(IPadress + " " + ex);
            /*            nodeDBExecutor.execute(new UpdateAllNodeDB("UPDATE ALLN SET"
             + " STATUS ='Missing Framework'"
             + " WHERE IP='" + IPadress.trim() + "';"));
             */
//            nodeDBExecutor.execute(() -> {
//                allNodeDB.stream().filter((get) -> (get.getFirstName().trim().equalsIgnoreCase(IPadress.trim()))).forEach((get) -> {
//                    get.setNStatus("Missing Framework");
//                });
//            });
//
//            for (int i = 0; i < liveNodeDB.size(); i++) {
//                if (liveNodeDB.get(i).getName().trim().equalsIgnoreCase(IPadress.trim())) {
//                    liveNodeDB.remove(i);
//                    System.out.println("Removing  " + IPadress + " from index" + i);
//                }
//
//            }
            liveNodeDB.remove(IPadress.trim());
            liveNodeDB.remove(UUID.trim());
            /*
             liveDBExecutor.execute(() -> {
             String sql = "DELETE FROM LIVE WHERE IP='" + IPadress + "';";
             System.out.println("Checking if " + IPadress + " exists in Live");

               

             //   SQLiteJDBC livedb = new SQLiteJDBC();
             //    livedb.delete("appdb/live.db", sql);
             //    livedb.closeConnection();
             });

             for (int i = 0; i < NetScanner.liveNodes.size(); i++) {
             LiveNode get = NetScanner.liveNodes.get(i);
             if (get.getName().trim().equalsIgnoreCase(IPadress.trim())) {
             NetScanner.liveNodes.remove(i);
             break;
             }
             }
             */
            try {
                s.close();
            } catch (IOException ex1) {
                Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
//        if (scanning.contains(IPadress.trim())) {
        scanning.remove(IPadress.trim());
//        }

    }

    public boolean isLive() {

        return live;
    }

    public static void main(String args[]) {

    }
}
