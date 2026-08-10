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

import in.co.s13.SIPS.tools.HDDInfo;
import in.co.s13.SIPS.tools.MemoryInfo;
import in.co.s13.sips.lib.ml.WarmModels;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author nika
 */
public class HardwareStatThreads {

    public HardwareStatThreads() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(new MemoryInfo(), 0, 60, TimeUnit.SECONDS);
        ScheduledExecutorService executorService2 = Executors.newScheduledThreadPool(1);
        executorService2.scheduleAtFixedRate(new HDDInfo(), 2, 90, TimeUnit.SECONDS);

        // A model kept warm for the next chunk is the point; a model kept warm
        // for a job that finished an hour ago is a leak with a good excuse.
        // WarmModels deliberately starts no thread of its own -- a library that
        // starts threads is hard to embed -- so the node runs the sweep.
        ScheduledExecutorService warmModels = Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "warm-model-sweeper");
            thread.setDaemon(true);
            return thread;
        });
        warmModels.scheduleAtFixedRate(WarmModels::releaseIdle, 5, 5, TimeUnit.MINUTES);
    }

}
