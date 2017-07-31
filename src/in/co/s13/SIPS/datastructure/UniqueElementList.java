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

import java.util.ArrayList;
import java.util.Collections;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class UniqueElementList {

    private ArrayList<Hop> arrayList = new ArrayList<>();

    public UniqueElementList() {
    }

    public UniqueElementList(Hop ... h) {
        for (int i = 0; i < h.length; i++) {
            Hop hop = h[i];
            this.addHop(hop);
        }
    }

    public boolean addHop(Hop e) {
        for (int i = 0; i < arrayList.size(); i++) {
            Hop get = arrayList.get(i);
            if (get.getId().equals(e.getId())) {
                get.setDistance(e.getDistance());
                return true;
            }
        }

        return arrayList.add(e); //To change body of generated methods, choose Tools | Templates.
    }

    public void remove(String id) {
        for (int i = 0; i < arrayList.size(); i++) {
            Hop get = arrayList.get(i);
            if (get.getId().equals(id)) {
                arrayList.remove(i);
                return;
            }
        }
    }

    public ArrayList<Hop> getArrayList() {
        return arrayList;
    }

    public void sortElementsInAscendingOrderDistance() {
        Collections.sort(arrayList, Hop.HopComparator.DISTANCE_SORT);
    }

    public Hop getNearestHop() {
        this.sortElementsInAscendingOrderDistance();
        if (arrayList.size() > 0) {
            return arrayList.get(0);
        }
        return null;
    }

}