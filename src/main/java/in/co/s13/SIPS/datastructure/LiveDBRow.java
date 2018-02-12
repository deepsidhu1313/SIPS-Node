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

import in.co.s13.SIPS.Scanner.NetScanner;
import in.co.s13.sips.lib.common.datastructure.LiveNode;
import in.co.s13.sips.lib.common.datastructure.Node;
import org.json.JSONArray;
import org.json.JSONObject;

public class LiveDBRow extends LiveNode implements Node{

    public LiveDBRow(String uuid, String host, String os, String processor, int task_limit,
            int qwait, long ram, long free_memory, long hdd_size, long hdd_free, JSONObject benchmarking_results, long lastCheckedOn) {
        super(uuid, host, os, processor, task_limit, qwait, ram, free_memory, hdd_size, hdd_free, benchmarking_results, lastCheckedOn);
    }

    public LiveDBRow(JSONObject livedbRow) {
        super(livedbRow);
        JSONArray array = livedbRow.getJSONArray("ipAddresses");
        for (int i = 0; i < array.length(); i++) {
            String ip = array.getString(i);
            //addIp(ip);
            NetScanner.addip(ip);
        }
    }
}
