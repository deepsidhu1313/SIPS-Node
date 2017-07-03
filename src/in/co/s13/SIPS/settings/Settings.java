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
package in.co.s13.SIPS.settings;

import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static in.co.s13.SIPS.settings.GlobalValues.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import org.json.JSONObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Nika
 */
public class Settings {

    public Settings() {
        System.out.println(OS);

        if (isWindows()) {
            System.out.println("This is Windows");
            OS_Name = 0;
        } else if (isMac()) {
            System.out.println("This is Mac");
            OS_Name = 1;
        } else if (isUnix()) {
            System.out.println("This is Unix or Linux");
            OS_Name = 2;
        } else if (isSolaris()) {
            System.out.println("This is Solaris");
            OS_Name = 3;
        } else {
            System.out.println("Your OS is not support!!");
            OS_Name = 4;

        }

        String workingDir = System.getProperty("user.dir");
        System.out.println("Current working directory : " + workingDir);
        PWD = workingDir;

        File f = new File(workingDir);
        System.out.println("" + f.getAbsolutePath());

        dir_temp = "var";
        File f4 = new File(workingDir + "/" + dir_temp);
        if (!f4.exists()) {
            if (!f4.mkdir()) {
                System.err.println("Directory for VAR couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }
        File f3 = new File("appdb");
        if (!f3.exists()) {
            if (!f3.mkdir()) {
                System.err.println("Directory for appdb couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }
        if (new File("appdb/settings.json").exists()) {
            loadSettings();
        } else {
            saveSettings();
        }
        processDBExecutor.execute(() -> {
            String sql = "CREATE TABLE PROC (ID    INT   PRIMARY KEY     NOT NULL,"
                    + " ALIENID  INT,"
                    + "FNAME     TEXT,"
                    + "CNO     INT,"
                    + "IP   TEXT);";
            File f1 = new File("appdb/proc.db");
            if (f1.exists()) {
                f1.delete();
            }
            procDB.createtable("appdb/proc.db", sql);
            procDB.closeConnection();
        });

        try {
            HOST_NAME = InetAddress.getLocalHost().getHostName().trim();
        } catch (UnknownHostException e) {
            System.err.println("Couldn't get Hostname"+e);
        }
        try (PrintStream procn = new PrintStream("procn.bat")) {
            procn.print("wmic cpu get name");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        OperatingSystemMXBean osMBean
                = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        MEM_SIZE = osMBean.getTotalPhysicalMemorySize();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): "
                + (MEM_SIZE == Long.MAX_VALUE ? "no limit" : MEM_SIZE));

        CPU_NAME = getCPUName();

    }

    public static void main(String[] args) {
        new Settings();
    }

    public static double getCPULoad() {
        OperatingSystemMXBean osMBean
                = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        RuntimeMXBean runtimeMBean = ManagementFactory.getRuntimeMXBean();

        double load = osMBean.getSystemLoadAverage();

        return load;
    }

    public static boolean isWindows() {

        return (OS.indexOf("win") >= 0);

    }

    public static boolean isMac() {

        return (OS.indexOf("mac") >= 0);

    }

    public static boolean isUnix() {

        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);

    }

    public static boolean isSolaris() {

        return (OS.indexOf("sunos") >= 0);

    }

    public static String getCPUName() {
        String CPUname = "";
        boolean success;
        try {
            ProcessBuilder pb = null;
            Process p;
            String cmd2 = "";
            File directory;
            if (OS_Name == 0) {
                String pwd = "" + PWD;
                pb = new ProcessBuilder();

                if ((directory = new File("c:\\windows\\system32\\")).exists()) {
                    pb.directory(directory);

                } else if ((directory = new File("d:\\windows\\system32\\")).exists()) {
                    pb.directory(directory);

                } else if ((directory = new File("e:\\windows\\system32\\")).exists()) {
                    pb.directory(directory);

                } else if ((directory = new File("f:\\windows\\system32\\")).exists()) {
                    pb.directory(directory);

                } else if ((directory = new File("g:\\windows\\system32\\")).exists()) {
                    pb.directory(directory);

                } else if ((directory = new File("h:\\windows\\system32\\")).exists()) {
                    pb.directory(directory);

                } else {
                    return CPUname = "Unidentified";
                }
                //"cd", "/d", "" + directory.getAbsolutePath(), "&",
                // String cmd[] = {"cd", "/d", "" + directory.getAbsolutePath(), "&", "wmic cpu get Name"};
                String cmd[] = {"procn.bat"};
                pb.command(cmd);
                p = null;
                p = pb.start();

                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                // read the output from the command
                System.out.println("Here is the standard output of the command:\n");

                String s = null;
                String output = "";
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                    CPUname = s;
                }
                System.out.println("Here is the standard error of the command (if any):\n");
                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                    success = false;

                }
                int exitValue = p.waitFor();
                System.out.println("\n\nExit Value is " + exitValue);
                p.destroy();

            } else if (OS_Name == 2) {
                String cmd[] = {"cat", "/proc/cpuinfo"};
                pb = new ProcessBuilder(cmd);
                pb.directory(new File("/"));
                p = null;
                try {
                    p = pb.start();

                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                    // read the output from the command
                    System.out.println("Here is the standard output of the command:\n");

                    String s = null;
                    String output = "";
                    while ((s = stdInput.readLine()) != null) {
                        System.out.println(s);
                        if (s.contains("model") && s.contains("name")) {
                            CPUname = s.substring(s.indexOf(":") + 1).trim();
                            break;
                        }
                    }
                    System.out.println("Here is the standard error of the command (if any):\n");
                    while ((s = stdError.readLine()) != null) {
                        System.out.println(s);
                        success = false;

                    }
                    int exitValue = p.waitFor();
                    System.out.println("\n\nExit Value is " + exitValue);
                    p.destroy();
                } catch (IOException ex) {
                    Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        return CPUname;

    }

    public static boolean deleteFile(String path) {
        boolean b = false;
        try {
            ArrayList<String> command = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            if (OS_Name == 0) {
                //command.add("del");
                //command.add("/f");
                //sb.append("cd /d ");
                //sb.append(path.substring(0,path.lastIndexOf("\\")));
                sb.append("cmd /c del /f ");
            } else {
                //sb.append("cd ");
                //sb.append(path.substring(path.lastIndexOf("/")));
                sb.append("rm -vf ");
            }
            //command.add(path);
            sb.append(path);
            Runtime rt = Runtime.getRuntime();
            ProcessBuilder pb = new ProcessBuilder();

            //   pb.command(sb.toString());
            Process p = rt.exec(sb.toString());

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");

            String s = null;
            String output = "";
            int c = 0;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                System.out.println(s);
                b = true;
            }

            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
                b = false;

            }
            int exitValue = p.waitFor();
            System.out.println("\n\nExit Value is " + exitValue);
            p.destroy();

            return b;
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        return b;
    }

    public static void copyFolder(File src, File dest) {

        if (src.isDirectory()) {

            //if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdirs();
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                copyFolder(srcFile, destFile);
            }

        } else {
            //if file, then copy it
            //Use bytes stream to support all file types
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {

                byte[] buffer = new byte[1024];

                int length;
                //copy the file content in bytes
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                System.out.println("File copied from " + src + " to " + dest);

            } catch (IOException ex) {
                Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return (directory.delete());
    }

    public static String getCheckSum(String datafile) {
        StringBuilder sb = new StringBuilder("");
        if (datafile.substring(datafile.lastIndexOf(".")).equalsIgnoreCase("sha")) {
            System.out.println("Didn't computed CheckSum for " + datafile);
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(datafile);
            byte[] dataBytes = new byte[1024];

            int nread = 0;

            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            };

            byte[] mdbytes = md.digest();

            //convert the byte to hex format
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            System.out.println("Digest(in hex format):: " + sb.toString());
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        saveCheckSum(datafile + ".sha", sb.toString());
        return sb.toString();
    }

    public static String LoadCheckSum(String ld) {
        return readFile(ld);
    }

    public static String readFile(String location) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(location))) {
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    public static void write(File f, String text) {
        try (FileWriter fw = new FileWriter(f);
                PrintWriter pw = new PrintWriter(fw)) {
            pw.print(text);
            pw.close();
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void saveCheckSum(String Filename, String con) {
        File f = new File(Filename);
        if (f.exists()) {
            f.delete();
        }
        write(f, con);

    }

    public static void generateCheckSumsInDirectory(String filename) {
        File f = new File(filename);
        if (f.isDirectory()) {
            //  generateCheckSumsInDirectory(f.getAbsolutePath());
            for (File listFile : f.listFiles()) {
                if (listFile.isDirectory()) {
                    generateCheckSumsInDirectory(listFile.getAbsolutePath());
                } else {
                    getCheckSum(listFile.getAbsolutePath());
                }
            }
        } else {
            getCheckSum(f.getAbsolutePath());

        }
    }

    void loadSettings() {
        JSONObject settings = new JSONObject(readFile("appdb/settings.json"));
        PROCESS_LIMIT = settings.getInt("MAX_PROCESS_ALLOWED_IN_PARALLEL");
        FILES_RESOLVER_LIMIT = settings.getInt("MAX_FILE_RESOLVE_IN_PARALLEL");
        PING_HANDLER_LIMIT = settings.getInt("MAX_PING_RESPONSES_IN_PARALLEL");
        PROCESS_HANDLER_LIMIT = settings.getInt("MAX_PROCESS_REQ_IN_PARALLEL");

    }

    void saveSettings() {
        JSONObject settings = new JSONObject();
        settings.put("MAX_PROCESS_ALLOWED_IN_PARALLEL", PROCESS_LIMIT);
        settings.put("MAX_FILE_RESOLVE_IN_PARALLEL", FILES_RESOLVER_LIMIT);
        settings.put("MAX_PING_RESPONSES_IN_PARALLEL", PING_HANDLER_LIMIT);
        settings.put("MAX_PROCESS_REQ_IN_PARALLEL", PROCESS_HANDLER_LIMIT);
        write(new File("appdb/settings.json"), settings.toString(4));
    }

    public static void outPrintln(String sout) {
        if (VERBOSE) {
            System.out.println(sout);
        }
        if (DUMP_LOG) {
            log.append("\n" + sout);

        }
        out.append("\n" + sout);
    }

    public static void errPrintln(String sout) {
        if (VERBOSE) {
            System.err.println(sout);
        }
        if (DUMP_LOG) {
            log.append("\n" + sout);
        }
        err.append("\n" + sout);

    }

}
