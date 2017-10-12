/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.tools.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PrintToFile implements Runnable {

    String content;
    File f;

    public PrintToFile(String Filename, String PID, String cno, String output) {
        f = new File("data/" + PID + "/" + PID + "c" + cno + ".output");
        content = output;
    }

    @Override
    public void run() {
        try (PrintStream out = new PrintStream(f)) {
            out.append(Util.readFile(f.getAbsolutePath())+"\n"+content);
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintToFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
