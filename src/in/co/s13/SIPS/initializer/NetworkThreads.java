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
package in.co.s13.SIPS.initializer;

import in.co.s13.SIPS.Scanner.AddLivenodes;
import in.co.s13.SIPS.Scanner.CheckLiveNodes;
import in.co.s13.SIPS.Scanner.NetScanner;
import in.co.s13.SIPS.datastructure.threadpools.ScheduledThreadPool;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.IPInfo;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class NetworkThreads {

    public NetworkThreads() {
        GlobalValues.netExecutor.changeSize(GlobalValues.total_threads);
        NetScanner ns = new NetScanner();
        Thread t = new Thread(ns);
        t.setName("NetScanner Thread");
        t.start();
        ScheduledExecutorService executorService3 = Executors.newScheduledThreadPool(1);
        executorService3.scheduleAtFixedRate(new IPInfo(), 0, 90, TimeUnit.SECONDS);
//        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
//        executorService.scheduleAtFixedRate(new AddLivenodes(), 2, 60, TimeUnit.SECONDS);
//        ScheduledExecutorService executorService2 = Executors.newScheduledThreadPool(1);
//        executorService2.scheduleAtFixedRate(new CheckLiveNodes(), 15, 90, TimeUnit.SECONDS);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
                } catch (InterruptedException ex) {
                    Logger.getLogger(ScheduledThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                }
                while (true) {

                    GlobalValues.nodeScannerExecutor.submit(new AddLivenodes());
                    int noOfHost = GlobalValues.HOSTS.size();
                    long interval = ((noOfHost * 10) < 60) ? 60 : (noOfHost * 10);
                    try {
                        Thread.sleep(TimeUnit.MILLISECONDS.convert(interval, TimeUnit.SECONDS));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ScheduledThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        });
        thread.setName("Add Live Nodes Scheduled Thread");
        thread.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
                } catch (InterruptedException ex) {
                    Logger.getLogger(ScheduledThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                }
                while (true) {

                    GlobalValues.nodeScannerExecutor.submit(new CheckLiveNodes());
                    int noOfHost = GlobalValues.LIVE_NODE_DB.size();
                    long interval = ((noOfHost * 5) < 60) ? 60 : (noOfHost * 5);
                    try {
                        Thread.sleep(TimeUnit.MILLISECONDS.convert(interval, TimeUnit.SECONDS));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ScheduledThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        });
        thread2.setName("Scan Live Nodes Scheduled Thread");
        thread2.start();
//        ScheduledThreadPool stp = new ScheduledThreadPool(aln, 2, 2, aln.hostList(), TimeUnit.SECONDS);
        //        CheckLiveNodes chkLN = new CheckLiveNodes();
        //        ScheduledThreadPool stp2 = new ScheduledThreadPool(chkLN, 15, 5, chkLN.hostList(), TimeUnit.SECONDS);

    }

}
