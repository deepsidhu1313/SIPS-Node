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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author nika
 */
public class NetworkThreads {

    public NetworkThreads() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(new AddLivenodes(), 2, 60, TimeUnit.SECONDS);
        ScheduledExecutorService executorService2 = Executors.newScheduledThreadPool(1);
        executorService2.scheduleAtFixedRate(new CheckLiveNodes(), 15, 90, TimeUnit.SECONDS);
    }

}
