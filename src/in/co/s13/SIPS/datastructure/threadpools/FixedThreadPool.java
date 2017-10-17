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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class FixedThreadPool {

    private ExecutorService executor;
    private int size;
    private ArrayList<Runnable> list = new ArrayList<>();
    private boolean shutdownInitiated = false;

    public FixedThreadPool(int size) {
        this.size = size;
        init();
    }

    private void init() {
        System.out.println("Initializing Threadpool");
        executor = Executors.newFixedThreadPool(size);
        shutdownInitiated = false;
    }

    public void changeSize(int size) {
        this.shutdown();
        this.size = size;
        restart();
    }

    public void restart() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!executor.isShutdown()) {
                    try {
                        System.out.println("Waiting for threadpool to shutdown");
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FixedThreadPool.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                init();
                for (int i = 0; i < list.size(); i++) {
                    Runnable get = list.get(i);
                    System.out.println("Adding " + get + " to pool after restart");
                    submit(get);
                }
            }
        });
        thread.start();
    }

    public void submit(Runnable task) {
        if (shutdownInitiated && !executor.isShutdown()) {
            list.add(task);
            System.out.println("Adding task " + task + " to List as Thread Pool is restarting");
        } else {
            executor.execute(task);
        }
    }

    public void shutdown() {
        shutdownInitiated = true;
        executor.shutdown();
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public boolean isTerminated() {
        return executor.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, TimeUnit.DAYS);
    }
    public List<Runnable> shutdownNow(){
    return executor.shutdownNow();
    }

}
