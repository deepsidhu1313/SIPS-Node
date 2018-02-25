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
import static in.co.s13.SIPS.tools.Util.outPrintln;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class NetScanner implements Runnable {

    InetAddress adrss;

    public NetScanner() {
//     livehosts = new ArrayList();
//        HOSTS = new ArrayList();
//        CURRENTLY_SCANNING = new ArrayList();
        addip("127.0.0.1");

        JSONArray ipsArray = IPs_TO_SCAN_JSON.getJSONArray("ips", new JSONArray());
        for (int i = 0; i < ipsArray.length(); i++) {
            String val = ipsArray.getString(i);
            addip(val);
        }
        JSONArray blacklistIPArray = BLACKLIST_JSON.getJSONArray("blacklist", new JSONArray());
        for (int i = 0; i < blacklistIPArray.length(); i++) {
            String val = blacklistIPArray.getString(i);
            GlobalValues.BLACKLIST.put(val, val);
        }
        JSONArray networksArray = NETWORKS_TO_SCAN_JSON.getJSONArray("networks", new JSONArray());
        for (int i = 0; i < networksArray.length(); i++) {
            String val = networksArray.getString(i);
            addnetwork(val);
        }
        if (GlobalValues.SCANNING_LOCAL_NETWORK_FOR_RESOURCES) {
            for (int i = 0; i < GlobalValues.IP_ADDRESSES.length(); i++) {
                JSONObject iface = GlobalValues.IP_ADDRESSES.getJSONObject(i);
                String ip = iface.getString("ip");
                if (!ip.startsWith("127")) {
                    addnetwork(ip);
                }

            }
        }
    }

    @Override
    public void run() {

    }

    public static void addip(String ip) {
        if (!HOSTS.contains(ip)) {
            HOSTS.add(ip);

            outPrintln("" + ip + " is added to list");
        }

    }

    public static void addnetwork(String ip) {
        try {
            outPrintln(ip);
            InetAddress address = InetAddress.getByName(ip);
            if (address instanceof Inet6Address) {
                // It's ipv6
            } else if (address instanceof Inet4Address) {
                // It's ipv4
                addIPv4Network(ip);
            }

        } catch (UnknownHostException ex) {
            Logger.getLogger(NetScanner.class.getName()).log(Level.SEVERE, null, ex);
        }

        //            if (ip.matches("(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\."
        //+ "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\."
//                    + "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\."
//                    + "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])")) {
//                addIPv4Network(ip);
//            } else if (ip.matches("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}")) {
//                
//            }
    }

    public static void addIPv4Network(String ip) {
        String str = "" + ip;
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
