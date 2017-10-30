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
import static in.co.s13.SIPS.settings.GlobalValues.*;
import in.co.s13.SIPS.datastructure.LiveDBRow;
import in.co.s13.SIPS.tools.Util;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Nika
 */
public class CheckLiveNodes implements Runnable {

    public static boolean livenodechecker = true;

    public CheckLiveNodes() {

    }

    @Override
    public void run() {

        Thread.currentThread().setName("CheckLiveNodeThread");
        {
            if (!GlobalValues.IS_WRITING) {
                LIVE_DB_EXECUTOR.execute(() -> {
                    Enumeration<String> keys = GlobalValues.LIVE_NODE_ADJ_DB.keys();
                    while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        LiveDBRow liveNode = GlobalValues.LIVE_NODE_ADJ_DB.get(key);
                        if (TimeUnit.SECONDS.convert(liveNode.getLastCheckAgo(), TimeUnit.MILLISECONDS) > 10) {
                            ArrayList<String> ips = liveNode.getIpAddresses();
                            for (int i = 0; i < ips.size(); i++) {
                                String get = ips.get(i);
                                Thread p1 = new Thread(new Ping(get, liveNode.getUuid()));
                                p1.setPriority(Thread.NORM_PRIORITY + 2);
                                PING_REQUEST_EXECUTOR_FOR_LIVE_NODES.submit(p1);
                                Util.appendToPingLog(LOG_LEVEL.OUTPUT, "Submitted Ping request for Adjacent " + liveNode.getUuid() + " on IP :" + get);
                            }
                        }

                    }

                    Enumeration<String> keys2 = GlobalValues.LIVE_NODE_NON_ADJ_DB.keys();
                    while (keys2.hasMoreElements()) {
                        String key = keys2.nextElement();
                        LiveDBRow liveNode = GlobalValues.LIVE_NODE_NON_ADJ_DB.get(key);
                        if (TimeUnit.SECONDS.convert(liveNode.getLastCheckAgo(), TimeUnit.MILLISECONDS) > 10) {
                            ArrayList<String> ips = liveNode.getIpAddresses();
                            for (int i = 0; i < ips.size(); i++) {
                                String get = ips.get(i);
                                Thread p1 = new Thread(new Ping(get, liveNode.getUuid()));
                                p1.setPriority(Thread.NORM_PRIORITY + 2);
                                PING_REQUEST_EXECUTOR_FOR_LIVE_NODES.submit(p1);
                                Util.appendToPingLog(LOG_LEVEL.OUTPUT, "Submitted Ping request for Non-Adjacent " + liveNode.getUuid() + " on IP :" + get);
                            }
                        }

                    }

                });

            }
        }
    }
}
