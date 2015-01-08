package controlpanel;

import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Nika
 */
public class settings {

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static int OS_Name = 0;
    public static String PWD = "";
    public static String dir_workspace = "";
    public static String dir_appdb = "";
    public static String dir_temp = "";
    public static int total_threads = 1;
    public static int process_id = 0;
    public static int PROCESS_LIMIT = Runtime.getRuntime().availableProcessors()-1;
    public static int PROCESS_WAITING = 0;
    public static ExecutorService processExecutor = Executors.newFixedThreadPool(PROCESS_LIMIT);
    public static ExecutorService processDBExecutor = Executors.newFixedThreadPool(1);
    public static String HOST_NAME = "DummySlave";
    public static long MEM_SIZE = 0L;
    public static String CPU_NAME = "";
    public static double CPU_LOAD_AVG = 0.0;

    public settings() {
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
        switch (OS_Name) {
            case 0:

                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;

        }


        dir_temp = "var";
        File f4 = new File(workingDir + "/" + dir_temp);
        if (!f4.exists()) {
            if (!f4.mkdir()) {
                System.err.println("Directory for VAR couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        } File f3 = new File("appdb");
        if (!f3.exists()) {
            if (!f3.mkdir()) {
                System.err.println("Directory for appdb couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }

        
        try {
            HOST_NAME = InetAddress.getLocalHost().getHostName().trim().toLowerCase();
        } catch (Exception e) {
            System.err.println("Couldn't get Hostname");
        }


        OperatingSystemMXBean osMBean
                = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        MEM_SIZE = osMBean.getTotalPhysicalMemorySize();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): "
                + (MEM_SIZE == Long.MAX_VALUE ? "no limit" : MEM_SIZE));


        CPU_LOAD_AVG = osMBean.getSystemLoadAverage();
        CPU_NAME=getCPUName();

        
    }

    public static void main(String[] args) {
        new settings();
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
            if (controlpanel.settings.OS_Name == 0) {
                String pwd = "" + controlpanel.settings.PWD;
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
                String cmd[] = {"cd","/d",""+directory.getAbsolutePath(),"&","wmic cpu get Name"};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    cmd2 += cmd[i];
                }
                System.out.println("" + cmd2);
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
                    CPUname=s;
                }
                System.out.println("Here is the standard error of the command (if any):\n");
                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                    success = false;

                }
                int exitValue = p.waitFor();
                System.out.println("\n\nExit Value is " + exitValue);
                p.destroy();

            } else if (controlpanel.settings.OS_Name == 2) {
                String cmd[] = {"cat", "/proc/cpuinfo"};
                for (int i = 0; i <= cmd.length - 1; i++) {
                    //   cmd2 += cmd[i];
                }
                System.out.println("" + cmd2);
                pb = new ProcessBuilder(cmd);
                pb.directory(new File("/"));
                p = null;
                try {
                    p = pb.start();
                } catch (IOException ex) {
                    Logger.getLogger(settings.class.getName()).log(Level.SEVERE, null, ex);
                }

                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                // read the output from the command
                System.out.println("Here is the standard output of the command:\n");

                String s = null;
                String output = "";
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                if(s.contains("model") && s.contains("name")){
                CPUname= s.substring(s.indexOf(":")+1).trim();
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

            }

        } catch (IOException ex) {
            Logger.getLogger(settings.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        return CPUname;

    }
}
