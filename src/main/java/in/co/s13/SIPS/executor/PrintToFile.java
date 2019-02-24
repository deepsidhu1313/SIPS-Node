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
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.tools.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrintToFile implements Runnable {

    private String content;
    private File f;

    public PrintToFile(String Filename, String PID, String cno, String output) {
        f = new File("data/" + PID + "/" + PID + "c" + cno + ".output");
        content = output;
    }

    @Override
    public void run() {
        try (PrintStream out = new PrintStream(f)) {
            String existing=Util.readFile(f.getAbsolutePath());
            out.append( existing+ "\n" + content);
            out.flush();
            out.close();
            Util.outPrintln("Printed "+content);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintToFile.class.getName()).log(Level.SEVERE, null, ex);
            Util.errPrintln(ex.toString());
        }
    }

}
