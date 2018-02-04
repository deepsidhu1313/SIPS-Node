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

import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.settings.Settings;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class CleanResultDB implements Runnable {

    public CleanResultDB() {
    }

    @Override
    public void run() {
        try {
            Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Logger.getLogger(CleanResultDB.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (GlobalValues.KEEP_CLEAN_RESULT_DB_THREAD_ALIVE) {

            GlobalValues.RESULT_DB_EXECUTOR.submit(() -> {
                for (Result result : GlobalValues.RESULT_DB.values()) {
                    if ((TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - result.getCreatedOn()) > 24)&&(!result.isFinished()|| (result.getStarttime()==Long.MIN_VALUE))) {
                        GlobalValues.RESULT_DB.remove(result.getJobToken());
                    }
                }
            });

            try {
                Thread.sleep(TimeUnit.MILLISECONDS.convert(GlobalValues.CLEAN_RESULT_DB_EVERY, TimeUnit.MINUTES));
            } catch (InterruptedException ex) {
                Logger.getLogger(CleanResultDB.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
