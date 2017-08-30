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
import static in.co.s13.SIPS.settings.GlobalValues.*;
import in.co.s13.SIPS.tools.Util;
import static in.co.s13.SIPS.tools.Util.outPrintln;
import java.net.InetAddress;
import org.json.JSONArray;
import org.json.JSONObject;

public class NetScanner implements Runnable {

    InetAddress adrss;

    public NetScanner() {
//     livehosts = new ArrayList();
//        hosts = new ArrayList();
//        scanning = new ArrayList();
        addip("127.0.0.1");

        JSONArray blacklistIPArray = blacklistJSON.getJSONArray("blacklist", new JSONArray());
        for (int i = 0; i < blacklistIPArray.length(); i++) {
            String val = blacklistIPArray.getString(i);
            GlobalValues.BLACKLIST.put(val, val);
        }
        JSONArray networksArray = networksToScanJSON.getJSONArray("networks", new JSONArray());
        for (int i = 0; i < networksArray.length(); i++) {
            String val = networksArray.getString(i);
            addnetwork(val);
        }
        for (int i = 0; i < GlobalValues.ipAddresses.length(); i++) {
            JSONObject iface = GlobalValues.ipAddresses.getJSONObject(i);
            String ip = iface.getString("ip");
            if (!ip.startsWith("127")) {
                addnetwork(ip);
            }

        }
        JSONArray ipsArray = ipToScanJSON.getJSONArray("ips", new JSONArray());
        for (int i = 0; i < ipsArray.length(); i++) {
            String val = ipsArray.getString(i);
            addip(val);
        }
    }

    @Override
    public void run() {

//        Thread.currentThread().setName("NetworkScannerThread");
//        nodeDBExecutor.execute(() -> {
//            {
//
//                Thread.currentThread().setName("SelectFromALLNDBThread");
//                try {
//                    String sql = "SELECT  * FROM ALLN ;";
//                    ResultSet rs = alldb.select(sql);
//                    while (rs.next()) {
//                        addip(rs.getString("IP"));
//                    }
//                } catch (SQLException ex) {
//                    Logger.getLogger(NetScanner.class.getName()).log(Level.SEVERE, null, ex);
//                }
//
//            }
//            alldb.closeStatement();
//        });
//        liveDBExecutor.execute(() -> {
//
//            Thread.currentThread().setName("LIVEDBcreateThread");
//            String sql = "CREATE TABLE LIVE "
//                    + "(IP TEXT PRIMARY KEY     NOT NULL, "
//                    + "PRF DOUBLE,"
//                    + "CN TEXT,"
//                    + "HN TEXT,"
//                    + "OS TEXT,"
//                    + "CPU TEXT,"
//                    + "QL LONG,"
//                    + "QW LONG,"
//                    + "RAM LONG);";
//
//            // SQLiteJDBC livedb = new SQLiteJDBC();
//            new File("appdb/live.db").delete();
//          //  livedb.createtable("appdb/live.db", sql);
//            // livedb.closeConnection();
//        });
//        nodeDBExecutor.execute(() -> {
//
//            Thread.currentThread().setName("InsertNodeDBThread");
//            for (String host : hosts) {
//                String sql = "INSERT INTO ALLN (IP ,"
//                        + " LEN ,"
//                        + "OS ,"
//                        + "HOST ,"
//                        + "QLEN ,"
//                        + "QWAIT,"
//                        + "RAM ,"
//                        + "PRFM ,"
//                        + "PROCESSOR ,"
//                        + "CLUSTER ) "
//                        + "VALUES ('" + host.trim() + "', '" + host.trim().length() + "', '', '', '','','','1.0','','' );";
//                //   alldb.insert(sql);
//            }
//            alldb.closeStatement();
//        });
//        nodeDBExecutor.execute(() -> {
//            {
//
//                Thread.currentThread().setName("SELECTNodeDBThread");
//                try {
//                    String sql = "SELECT  * FROM ALLN ;";
//                    ResultSet rs = alldb.select(sql);
//                    while (rs.next()) {
//                        addip(rs.getString("IP"));
//                        allNodeDB.add(new IPAddress(rs.getString("IP"), rs.getString("STATUS"), rs.getString("OS"), rs.getString("HOST"),
//                                rs.getInt("QLEN"), rs.getInt("QWAIT"), rs.getLong("RAM"), rs.getDouble("PRFM"),
//                                rs.getString("PROCESSOR"), rs.getString("CLUSTER")));
//                    }
//                } catch (SQLException ex) {
//                    Logger.getLogger(NetScanner.class.getName()).log(Level.SEVERE, null, ex);
//                }
//
//            }
//            alldb.closeStatement();
//        });
//        ScheduledExecutorService executorService3 = Executors.newScheduledThreadPool(1);
//        executorService3.scheduleAtFixedRate(new LiveNodeTreeTable(), 3, 6, TimeUnit.SECONDS);
//
//        ScheduledExecutorService executorService4 = Executors.newScheduledThreadPool(1);
//        executorService4.scheduleAtFixedRate(new GarbageCollector(), 15, 15, TimeUnit.MINUTES);
    }

//    public static void removeip(String ip) {
//        Collections.sort(hosts, NodeDBRow.NodeDBRowComparator.getComparator(NodeDBRow.NodeDBRowComparator.UUID_SORT));
//        hosts.remove((ip));
////        if (hosts.contains(ip)) {
////            
////            
////        }
//        /*     if (livehosts.contains(ip)) {
//         livehosts.remove(hosts.indexOf(ip));
//         Collections.sort(livehosts);
//         }*/
//    }
    public static void addip(String ip) {
        if (!hosts.contains(ip)) {
            hosts.add(ip);

            outPrintln("" + ip + " is added to list");
            //  Collections.sort(hosts);
        }
//        nodeDBExecutor.execute(() -> {
//            {
//                String sql = "INSERT INTO ALLN (IP ,"
//                        + " LEN ,"
//                        + "OS ,"
//                        + "HOST ,"
//                        + "QLEN ,"
//                        + "QWAIT,"
//                        + "RAM ,"
//                        + "PRFM ,"
//                        + "PROCESSOR ,"
//                        + "CLUSTER ) "
//                        + "VALUES ('" + ip.trim() + "', '" + ip.trim().length() + "', '', '', '','','','1.0','','' );";
//                //      Settings.alldb.insert(sql);
//            }
//            alldb.closeStatement();
//        });

    }

    public static void addnetwork(String ip) {
        String str = "" + ip;
        outPrintln(ip);
        if (!ip.matches("(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\."
                + "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\."
                + "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\."
                + "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])")) {
            Util.errPrintln("IP Format not supported: \"" + ip + "\'");
            return;
        }
        int ind1 = str.indexOf(".");
        int ind3 = str.lastIndexOf('.');
        int ind2 = (str.substring(ind1 + 1, ind3).indexOf(".")) + (ind1 + 1);

        outPrintln("ind1= " + ind1);
        outPrintln("ind2= " + ind2);
        outPrintln("ind3= " + ind3);

        String ip1 = str.substring(0, ind1);
        String ip2 = str.substring(ind1 + 1, ind2);
        String ip3 = str.substring(ind2 + 1, ind3);
        String ip4 = str.substring(ind3 + 1);

        outPrintln(ip1);
        outPrintln(ip2);
        outPrintln(ip3);
        outPrintln(ip4);

        for (int i = 0; i <= 255; i++) {
            addip("" + ip1 + "." + ip2 + "." + ip3 + "." + i);
            outPrintln("" + ip1 + "." + ip2 + "." + ip3 + "." + i);
        }

    }

}
