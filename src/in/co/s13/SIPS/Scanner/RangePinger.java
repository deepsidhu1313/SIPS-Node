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

import java.util.ArrayList;
import static in.co.s13.SIPS.settings.GlobalValues.*;
import in.co.s13.SIPS.virtualdb.LiveDBRow;
import java.util.Hashtable;
/**
 *
 * @author Nika
 */
public class RangePinger implements Runnable {

    int low, up;
    Hashtable<String,LiveDBRow> temp;

    public RangePinger(int min, int max, Hashtable<String,LiveDBRow> al) {
        low = min;
        up = max;
        temp = new Hashtable<>(al);
        if (up == temp.size()) {
            up--;
            System.out.println("UP equals to array , decremented");
        }
        if (up > temp.size()) {
            up = temp.size() - 1;

            System.out.println("Size equals to array , decremented");
        }
        System.out.println("Executing Ping on AL" + al + " from " + low + " to " + up);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("RangePingerThread");
        for (int i = low; i <= up; i++) {

            Thread p1 = new Thread(new Ping(temp.get(i)));
        //    p1.setPriority(Thread.NORM_PRIORITY - 1);
            pingExecutor.execute(p1);

        }
    }

}
