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
package in.co.s13.SIPS.datastructure.threadpools;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class ScheduledThreadPool {

    private Runnable task;
    private long initialDelay;
    private float delayFactor;
    private TimeUnit timeUnit;
    private boolean shutdown;
    private ArrayList al;

    public ScheduledThreadPool(Runnable task, long initialDelay, float delayFactor, ArrayList bindToSize, TimeUnit timeUnit) {
        this.task = task;
        this.initialDelay = initialDelay;
        this.delayFactor = delayFactor;
        this.timeUnit = timeUnit;
        this.al = bindToSize;
        this.run();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    private void run() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(initialDelay, timeUnit));
                } catch (InterruptedException ex) {
                    Logger.getLogger(ScheduledThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                }
                while (!shutdown) {

                    task.run();

                    try {
                        Thread.sleep(TimeUnit.MILLISECONDS.convert((long) (delayFactor * al.size()), timeUnit));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ScheduledThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        t.setName("Scheduled Thread: " + task.toString() + " InitialDelay:" + initialDelay + " DelayFactor:" + delayFactor);
        t.start();

    }

}
