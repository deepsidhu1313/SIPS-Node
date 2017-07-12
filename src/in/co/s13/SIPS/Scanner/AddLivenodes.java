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
import java.util.ArrayList;
import static in.co.s13.SIPS.settings.GlobalValues.*;
import static in.co.s13.SIPS.tools.Util.outPrintln;
public class AddLivenodes implements Runnable {

    public AddLivenodes() {

    }

    @Override
    public void run() {
        Thread.currentThread().setName("AddLiveNodeThread");
        int threads = total_threads;
//        ArrayList hst = new ArrayList(NetScanner.hosts);
        int nodes = GlobalValues.hosts.size();
        //static boolean addnodes = true;

        int lastUpper = 0;
        int size = nodes / threads;
        for (int i = 1; i <= threads; i++) {
            System.out.println("Total nodes:" + nodes + " Total Threads:" + threads + " i:" + i);
            int lower;
            if (i == 1) {
                lower = lastUpper;
            } else {
                lower = lastUpper + 1;

            }
            int upper = lastUpper + size;
            lastUpper = upper;
            netExecutor.execute(new RangePinger(lower, upper, hosts));

        }

        outPrintln("\nFinished all threads");
    }
}
