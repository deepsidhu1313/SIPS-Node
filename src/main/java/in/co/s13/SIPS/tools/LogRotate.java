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
import in.co.s13.SIPS.settings.Settings;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class LogRotate implements Runnable {

    public LogRotate() {
    }

    @Override
    public void run() {
        try {
            Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Logger.getLogger(LogRotate.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (GlobalValues.KEEP_LOG_ROTATE_ALIVE) {
            File logDir = new File("log/");
            File[] files = logDir.listFiles((File dir, String name) -> name.toLowerCase().endsWith(".log"));
            for (File file : files) {
                Util.outPrintln("Log Rotate Checking File name: " + file.getAbsolutePath());
                String nameSplit = "";
                if (file.getName().contains("-")) {
                    nameSplit = file.getName().split("-")[0];
                    try {
                        long timestamp = (Long.parseLong(nameSplit));
                        continue;
                    } catch (NumberFormatException e) {
                        Logger.getLogger(LogRotate.class.getName()).log(Level.WARNING, null, e);

                    }

                }

                if ((file.length() / 1024 > GlobalValues.LOG_FILE_SIZE_LIMIT)) {
                    Util.outPrintln("Log Rotate File name: " + file.getAbsolutePath());
                    Util.outPrintln(" Logrotate File met the criteria renaming "
                            + file.renameTo(new File(file.getParentFile().getAbsolutePath() + "/" + System.currentTimeMillis() + "-" + file.getName())));
                } else if ((TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - GlobalValues.LAST_ROTATED_ON) > GlobalValues.LOGROTATION_INTERVAL_IN_HOURS)) {
                    Util.outPrintln("Log Rotate File name: " + file.getAbsolutePath());
                    Util.outPrintln(" Logrotate File met the criteria renaming "
                            + file.renameTo(new File(file.getParentFile().getAbsolutePath() + "/" + System.currentTimeMillis() + "-" + file.getName())));
                    GlobalValues.LAST_ROTATED_ON = System.currentTimeMillis();
                    Settings.saveSettings();
                }
            }
            try {
                Thread.sleep(TimeUnit.MILLISECONDS.convert(GlobalValues.LOG_ROTATE_CHECK_FILES_EVERY, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                Logger.getLogger(LogRotate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
