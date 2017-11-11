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
package in.co.s13.SIPS.datastructure;

import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class IPHostnameCombo {

    private String hostname, ip;

    public IPHostnameCombo() {
    }

    public IPHostnameCombo(String hostname, String ip) {
        this.hostname = hostname;
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean bothSame() {
        return hostname.trim().equalsIgnoreCase(ip.trim());
    }

    @Override
    public String toString() {
        JSONObject result = new JSONObject();
        result.put("hostname", hostname);
        result.put("ip", ip);
        return result.toString(4);
    }

}
