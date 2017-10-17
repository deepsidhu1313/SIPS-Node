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
import in.co.s13.SIPS.Scanner.ScheduledLiveNodeScanner;
import in.co.s13.SIPS.Scanner.ScheduledNodeScanner;
import in.co.s13.SIPS.datastructure.threadpools.FixedThreadPool;
import in.co.s13.SIPS.datastructure.threadpools.ScheduledThreadPool;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.PING_REQUEST_LIMIT;
import in.co.s13.SIPS.tools.IPInfo;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static in.co.s13.SIPS.settings.GlobalValues.TASK_LIMIT;
import in.co.s13.SIPS.tools.ServiceOperations;
import java.security.Provider;

/**
 *
 * @author nika
 */
public class NetworkThreads {

    public NetworkThreads() {
        GlobalValues.NETWORK_EXECUTOR = new FixedThreadPool(TASK_LIMIT);
        GlobalValues.TASK_EXECUTOR = new FixedThreadPool(TASK_LIMIT);
        GlobalValues.PING_REQUEST_EXECUTOR = new FixedThreadPool(PING_REQUEST_LIMIT);
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

        ServiceOperations.initNodeScannerAtStartUp();
        ServiceOperations.initLiveNodeScannerAtStartUp();

    }

}
