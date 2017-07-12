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

import in.co.s13.SIPS.executor.sendOutput;
import in.co.s13.SIPS.executor.sockets.Server;
import in.co.s13.SIPS.settings.GlobalValues;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nika
 */
public class HDDInfo implements Runnable {

    public HDDInfo() {

    }

    @Override
    public void run() {
        try {
            ProcessBuilder pb = null;
            String currentDrive = null;
            if (Util.isUnix()) {
                String cmd[] = {"df", "", "."};
                pb = new ProcessBuilder(cmd);
            } else if (Util.isWindows()) {
                String cmd[] = {"wmic logicaldisk get size,freespace,caption"};
                pb = new ProcessBuilder(cmd);
                File file = new File(".").getAbsoluteFile();
                File root = file.getParentFile();
                while (root.getParentFile() != null) {
                    root = root.getParentFile();
                }
                System.out.println("Drive is: " + root.getPath());
                currentDrive = root.getPath();
            }
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String s = null;
            //       String output = "";
            while ((s = stdInput.readLine()) != null) {
//                System.out.println(s);
                //  output += "\n" + s;
                if (Util.isUnix()) {
                    if (s.contains("/dev/")) {
                        String dat[] = s.split(" ");
                        GlobalValues.HDD_SIZE = Long.parseLong(dat[1]) * 1024;
                        GlobalValues.HDD_FREE = Long.parseLong(dat[3]) * 1024;
                    }
                } else if (Util.isWindows()) {
                    if (s.contains("" + currentDrive.toString())) {
                        String dat[] = s.split(" ");
                        GlobalValues.HDD_SIZE = Long.parseLong(dat[1]);
                        GlobalValues.HDD_FREE = Long.parseLong(dat[2]);
                    }
                }
            }

            // output = "";
            while ((s = stdError.readLine()) != null) {
                //Util.errPrintln(s);
                //   output += "\n" + s;
            }
            ////System.out.println("Process executed");
            int exitValue = p.waitFor();
//            System.out.println("\n\nExit Value is " + exitValue);
            stdError.close();
            stdInput.close();
            p.destroy();

        } catch (IOException ex) {
            Logger.getLogger(HDDInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(HDDInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
