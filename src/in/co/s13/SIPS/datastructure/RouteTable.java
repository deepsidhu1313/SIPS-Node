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

import java.util.Hashtable;

/**
 *
 * @author nika
 */
public class RouteTable {

    /****
     * Purpose of This data structure to maintain a table of destination IPs/uid's from current node
     * and the next node which gives them shortest distance to destination.
     */
    
    private Hashtable<String, Hop> table = new Hashtable<>();

    public RouteTable() {
    }
     
    public void addHop(String uid, Hop value) {
        table.put(uid, value);
    }
    
    public void updateHop(String uid, Hop value) {
        table.replace(uid, value);
    }
    
    public void removeHop(String uid) {
        table.remove(uid);
    }
    
    public Hop getNextHop(String destUid){
    return table.getOrDefault(destUid, new Hop("no-route", -999));
    }
}
