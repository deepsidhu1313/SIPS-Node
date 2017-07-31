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

import com.sun.management.OperatingSystemMXBean;
import in.co.s13.SIPS.datastructure.UniqueElementList;
import in.co.s13.SIPS.executor.sockets.Server;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.DUMP_LOG;
import static in.co.s13.SIPS.settings.GlobalValues.MEM_SIZE;
import static in.co.s13.SIPS.settings.GlobalValues.OS;
import static in.co.s13.SIPS.settings.GlobalValues.OS_Name;
import static in.co.s13.SIPS.settings.GlobalValues.PWD;
import static in.co.s13.SIPS.settings.GlobalValues.VERBOSE;
import static in.co.s13.SIPS.settings.GlobalValues.err;
import static in.co.s13.SIPS.settings.GlobalValues.log;
import static in.co.s13.SIPS.settings.GlobalValues.out;
import in.co.s13.SIPS.settings.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileSystemView;
import org.json.JSONObject;

/**
 * Utility methods for jDiskMark
 */
public class Util {

    static final DecimalFormat DF = new DecimalFormat("###.##");

    /**
     * Returns a pseudo-random number between min and max, inclusive. The
     * difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value. Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    /*
     * Not used kept here for reference.
     */
    static public void readPhysicalDrive() throws FileNotFoundException, IOException {
        File diskRoot = new File("\\\\.\\PhysicalDrive0");
        RandomAccessFile diskAccess = new RandomAccessFile(diskRoot, "r");
        byte[] content = new byte[1024];
        diskAccess.readFully(content);
        System.out.println("done reading fully");
        System.out.println("content " + Arrays.toString(content));
    }

    /*
     * Not used kept here for reference.
     */
    public static void sysStats() {
        /* Total number of processors or cores available to the JVM */
        System.out.println("Available processors (cores): "
                + Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        System.out.println("Free memory (bytes): "
                + Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): "
                + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently available to the JVM */
        System.out.println("Total memory available to JVM (bytes): "
                + Runtime.getRuntime().totalMemory());

        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();

        /* For each filesystem root, print some info */
        for (File root : roots) {
            System.out.println("File system root: " + root.getAbsolutePath());
            System.out.println("Total space (bytes): " + root.getTotalSpace());
            System.out.println("Free space (bytes): " + root.getFreeSpace());
            System.out.println("Usable space (bytes): " + root.getUsableSpace());
            System.out.println("Drive Type: " + getDriveType(root));
        }
    }

    public static String displayString(double num) {
        return DF.format(num);
    }

    /**
     * Gets the drive type string for a root file such as C:\
     *
     * @param file
     * @return
     */
    public static String getDriveType(File file) {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        return fsv.getSystemTypeDescription(file);
    }

    /**
     * Get OS specific disk info based on the drive the path is mapped to.
     *
     * @param dataDir the data directory being used in the run.
     * @return Disk info if available.
     */
    public static String getDiskInfo(File dataDir) {
        // System.out.println("os: "+System.getProperty("os.name"));
        Path dataDirPath = Paths.get(dataDir.getAbsolutePath());
        String osName = System.getProperty("os.name");
        if (osName.contains("Linux")) {
            // get disk info for linux
            String devicePath = Util.getDeviceFromPath(dataDirPath);
            String deviceModel = Util.getDeviceModel(devicePath);
            String deviceSize = Util.getDeviceSize(devicePath);
            return deviceModel + " (" + deviceSize + ")";
        } else if (osName.contains("Mac OS X")) {
            // get disk info for max os x
            String devicePath = Util.getDeviceFromPathOSX(dataDirPath);
            String deviceModel = Util.getDeviceModelOSX(devicePath);
            return deviceModel;
        } else if (osName.contains("Windows")) {
            // get disk info for windows
            String driveLetter = dataDirPath.getRoot().toFile().toString().split(":")[0];
            return Util.getModelFromLetter2(driveLetter);
        }
        return "OS not supported";
    }

    /**
     * This method became obsolete with an updated version of windows 10. A
     * newer version of the method is used.
     *
     * Get the drive model description based on the windows drive letter. Uses
     * the powershell script disk-model.ps1
     *
     * This appears to be the output of the original ps script before the
     * update:
     *
     * d:\>powershell -ExecutionPolicy ByPass -File tmp.ps1
     *
     * DiskSize : 128034708480 RawSize : 117894545408 FreeSpace : 44036825088
     * Disk : \\.\PHYSICALDRIVE1 DriveLetter : C: DiskModel : SanDisk
     * SD6SF1M128G VolumeName : OS_Install Size : 117894541312 Partition : Disk
     * #1, Partition #2
     *
     * DiskSize : 320070320640 RawSize : 320070836224 FreeSpace : 29038071808
     * Disk : \\.\PHYSICALDRIVE2 DriveLetter : E: DiskModel : TOSHIBA External
     * USB 3.0 USB Device VolumeName : TOSHIBA EXT Size : 320070832128 Partition
     * : Disk #2, Partition #0
     *
     * We should be able to modify the new parser to detect the output type and
     * adjust parsing as needed.
     *
     * @param driveLetter The single character drive letter.
     * @return Disk Drive Model description or empty string if not found.
     */
    @Deprecated
    public static String getModelFromLetter(String driveLetter) {
        try {
            Process p = Runtime.getRuntime().exec("powershell -ExecutionPolicy ByPass -File disk-model.ps1");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();

            String curDriveLetter = null;
            String curDiskModel = null;
            while (line != null) {
                System.out.println(line);
                if (line.trim().isEmpty()) {
                    if (curDriveLetter != null && curDiskModel != null
                            && curDriveLetter.equalsIgnoreCase(driveLetter)) {
                        return curDiskModel;
                    }
                }
                if (line.contains("DriveLetter : ")) {
                    curDriveLetter = line.split(" : ")[1].substring(0, 1);
                    System.out.println("current letter=" + curDriveLetter);
                }
                if (line.contains("DiskModel   : ")) {
                    curDiskModel = line.split(" : ")[1];
                    System.out.println("current model=" + curDiskModel);
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
        }
        return null;
    }

    /**
     * Get the drive model description based on the windows drive letter. Uses
     * the powershell script disk-model.ps1
     *
     * Parses output such as the following:
     *
     * DiskModel DriveLetter --------- ----------- ST31500341AS ATA Device D:
     * Samsung SSD 850 EVO 1TB ATA Device C:
     *
     * Tested on Windows 10 on 3/6/2017
     *
     * @param driveLetter
     * @return
     */
    public static String getModelFromLetter2(String driveLetter) {
        try {
            Process p = Runtime.getRuntime().exec("powershell -ExecutionPolicy ByPass -File disk-model.ps1");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();

            while (line != null) {
                System.out.println(line);
                if (line.trim().endsWith(driveLetter + ":")) {
                    String model = line.split(driveLetter + ":")[0];
                    System.out.println("model is: " + model);
                    return model;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("IO exception retrieveing disk info");
        }
        return null;
    }

    /**
     * On Linux OS get the device path when given a file path. eg. filePath =
     * /home/james/Desktop/jDiskMarkData devicePath = /dev/sda
     *
     * @param path the file path
     * @return the device path
     */
    static public String getDeviceFromPath(Path path) {
        try {
            Process p = Runtime.getRuntime().exec("df " + path.toString());
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            String curDevice;
            while (line != null) {
                //System.out.println(line);
                if (line.contains("/dev/")) {
                    curDevice = line.split(" ")[0];
                    // strip the partition digit if it is numeric
                    if (curDevice.substring(curDevice.length() - 1).matches("[0-9]{1}")) {
                        curDevice = curDevice.substring(0, curDevice.length() - 1);
                    }
                    return curDevice;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
        }
        return null;
    }

    /**
     * On Linux OS use the lsblk command to get the disk model number for a
     * specific Device ie. /dev/sda
     *
     * @param devicePath path of the device
     * @return the disk model number
     */
    static public String getDeviceModel(String devicePath) {
        try {
            Process p = Runtime.getRuntime().exec("lsblk " + devicePath + " --output MODEL");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                //System.out.println(line);
                if (!line.equals("MODEL") && !line.trim().isEmpty()) {
                    return line;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
        }
        return null;
    }

    /**
     * On Linux OS use the lsblk command to get the disk size for a specific
     * Device ie. /dev/sda
     *
     * @param devicePath path of the device
     * @return the size of the device
     */
    static public String getDeviceSize(String devicePath) {
        try {
            Process p = Runtime.getRuntime().exec("lsblk " + devicePath + " --output SIZE");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                //System.out.println(line);
                if (!line.contains("SIZE") && !line.trim().isEmpty()) {
                    return line;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
        }
        return null;
    }

    static public String getDeviceFromPathOSX(Path path) {
        try {
            Process p = Runtime.getRuntime().exec("df " + path.toString());
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            String curDevice;
            while (line != null) {
                //System.out.println(line);
                if (line.contains("/dev/")) {
                    curDevice = line.split(" ")[0];
                    return curDevice;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
        }
        return null;
    }

    static public String getDeviceModelOSX(String devicePath) {
        try {
            String command = "diskutil info " + devicePath;
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("Device / Media Name:")) {
                    return line.split("Device / Media Name:")[1].trim();
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
        }
        return null;
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

    public static long getMemorySize() {
        long MEMORY_SIZE;
        OperatingSystemMXBean osMBean
                = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        MEMORY_SIZE = osMBean.getTotalPhysicalMemorySize();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): "
                + (MEMORY_SIZE == Long.MAX_VALUE ? "no limit" : MEMORY_SIZE));

        return MEMORY_SIZE;
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

    public static void write(String path, String text) {
        try (FileWriter fw = new FileWriter(new File(path));
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

    public static void outPrintln(String sout) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(System.currentTimeMillis()));
        if (VERBOSE) {
            System.out.println(sout);
        }
        if (DUMP_LOG) {
            log.append("\n" + "[" + timestamp + "] " + sout);

        }
        out.append("\n" + "[" + timestamp + "] " + sout);
    }

    public static void errPrintln(String sout) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(System.currentTimeMillis()));
        if (VERBOSE) {
            System.err.println(sout);
        }
        if (DUMP_LOG) {
            log.append("\n" + "[" + timestamp + "] " + sout);
        }
        err.append("\n" + "[" + timestamp + "] " + sout);

    }

    public static String generateNodeUUID() {
        return java.util.UUID.randomUUID() + ":" + java.util.UUID.randomUUID();
    }

    /**
     * Returns an <code>InetAddress</code> object encapsulating what is most
     * likely the machine's LAN IP address.
     * <p/>
     * This method is intended for use as a replacement of JDK method
     * <code>InetAddress.getLocalHost</code>, because that method is ambiguous
     * on Linux systems. Linux systems enumerate the loopback network interface
     * the same way as regular LAN network interfaces, but the JDK
     * <code>InetAddress.getLocalHost</code> method does not specify the
     * algorithm used to select the address returned under such circumstances,
     * and will often return the loopback address, which is not valid for
     * network communication. Details
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
     * <p/>
     * This method will scan all IP addresses on all network interfaces on the
     * host machine to determine the IP address most likely to be the machine's
     * LAN address. If the machine has multiple IP addresses, this method will
     * prefer a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually
     * IPv4) if the machine has one (and will return the first site-local
     * address if the machine has more than one), but if the machine does not
     * hold a site-local address, this method will return simply the first
     * non-loopback address found (IPv4 or IPv6).
     * <p/>
     * If this method cannot find a non-loopback address using this selection
     * algorithm, it will fall back to calling and returning the result of JDK
     * method <code>InetAddress.getLocalHost</code>.
     * <p/>
     *
     * @throws UnknownHostException If the LAN address of the machine cannot be
     * found.
     */
    public static ArrayList<InetAddress> getLocalHostLANAddress() throws UnknownHostException {
        ArrayList<InetAddress> list = new ArrayList<>();
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            list.add(inetAddr);
                        } else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                list.add(candidateAddress);
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            list.add(jdkSuppliedAddress);
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
        return list;
    }

    public static void copyFileUsingStream(File source, File dest) {
        if (dest.exists()) {
            dest.delete();
        }
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        if (!source.exists()) {
            try {
                System.out.println("" + source.getCanonicalPath() + " does not exist");
                return;
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            System.out.println("" + source.getAbsolutePath() + " copied to " + dest.getAbsolutePath() + " ");

            try {
                is.close();
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

        } finally {
            try {
                is.close();
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void copyFileUsingStream(String pathtosrc, String pathtodest) {
        File source = new File(pathtosrc);
        File dest = new File(pathtodest);
        if (dest.exists()) {
            dest.delete();
        }
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        if (!source.exists()) {
            System.out.println("" + pathtosrc + " does not exist");
            return;
        }
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            System.out.println("" + source.getAbsolutePath() + " copied to " + dest.getAbsolutePath() + " ");
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static synchronized JSONObject getAdjacentTableInJSON() {
        JSONObject json = new JSONObject();
        Enumeration<String> en = GlobalValues.ADJACENT_NODES_TABLE.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            Long value = GlobalValues.ADJACENT_NODES_TABLE.get(key);
            json.put(key, value);
        }
        return json;
    }

    public static synchronized JSONObject getNonAdjacentTableInJSON() {
        JSONObject json = new JSONObject();
        Enumeration<String> en = GlobalValues.NON_ADJACENT_NODES_TABLE.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            UniqueElementList value = GlobalValues.NON_ADJACENT_NODES_TABLE.get(key);
            json.put(key, value.getNearestHop().getDistance());
        }
        return json;
    }

    
    public static void main(String[] args) {
        Util.sysStats();

    }
}
