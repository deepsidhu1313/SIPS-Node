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
package in.co.s13.SIPS.tools;

import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.sips.lib.common.settings.GlobalValues.ADJACENT_NODES_TABLE;
import static in.co.s13.sips.lib.common.settings.GlobalValues.NODE_EXPIRY_TIME;
import static in.co.s13.sips.lib.common.settings.GlobalValues.NON_ADJACENT_NODES_TABLE;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author nika
 */
public class RemoveExpiredNodes implements Runnable {

    public RemoveExpiredNodes() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(this, 30, 45, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        ADJACENT_NODES_TABLE.forEach((t, u) -> {
            if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - u.getTimestamp()) >= NODE_EXPIRY_TIME) {
                ADJACENT_NODES_TABLE.remove(u.getId());
            }
        });

        NON_ADJACENT_NODES_TABLE.forEach((t, u) -> {
            u.removeExpiredElements();
        });

        GlobalValues.LIVE_NODE_ADJ_DB.forEach((t, u) -> {
            if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - u.getLastCheckAgo()) >= NODE_EXPIRY_TIME) {
                GlobalValues.LIVE_NODE_ADJ_DB.remove(u.getUuid());
            }
        });

        GlobalValues.LIVE_NODE_NON_ADJ_DB.forEach((t, u) -> {
            if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - u.getLastCheckAgo()) >= NODE_EXPIRY_TIME) {
                GlobalValues.LIVE_NODE_NON_ADJ_DB.remove(u.getUuid());
            }
        });
    }

}
