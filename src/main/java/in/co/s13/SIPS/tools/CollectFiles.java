/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.tools;

import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author nika
 */
public class CollectFiles {

    public CollectFiles() {
    }

    public ArrayList<String> getFiles(String dir) {
        ArrayList<String> files = new ArrayList<>();
        File folder = new File(dir);
        File file[] = folder.listFiles();
        for (int i = 0; i < file.length; i++) {
            File file1 = file[i];
            if (file1.isDirectory()) {
                files.addAll(getFiles(file1.getAbsolutePath()));
            } else {
                files.add(file1.getAbsolutePath());
            }
        }
        return files;
    }
}
