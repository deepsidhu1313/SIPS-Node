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
import in.co.s13.SIPS.tools.Util;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class ScheduledNodeScanner implements Runnable {

    public ScheduledNodeScanner() {
    }

    @Override
    public void run() {
        try {
//         Util.outPrintln(""+Thread.currentThread().getName()+" is going to sleep for 2 sec");
            Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Logger.getLogger(ScheduledNodeScanner.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (GlobalValues.KEEP_NODE_SCANNER_ALIVE) {

            GlobalValues.NODE_SCANNER_EXECUTOR.submit(new AddLivenodes());
            int noOfHost = GlobalValues.HOSTS.size();
            long interval = ((noOfHost * 10) < 30) ? 30 : (noOfHost * 10);
            long intervalMillis = TimeUnit.MILLISECONDS.convert(interval, TimeUnit.SECONDS);
            while (GlobalValues.KEEP_NODE_SCANNER_ALIVE && (intervalMillis > 0)) {
//            Util.outPrintln(""+Thread.currentThread().getName()+" is going to sleep for 5 sec of remaining time "+intervalMillis+" ms");
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ScheduledLiveNodeScanner.class.getName()).log(Level.SEVERE, null, ex);
                }
                intervalMillis -= 5000L;
            }
        }
    }

}
