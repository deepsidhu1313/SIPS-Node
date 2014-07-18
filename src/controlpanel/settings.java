package controlpanel;


import java.io.File;
import static javafx.application.Application.STYLESHEET_MODENA;

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
    public static int total_threads=1;
    public static int process_id=0;
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

        dir_appdb = "appdb";
        File f2 = new File(workingDir + "/" + dir_appdb);
        if (!f2.exists()) {
            if (!f2.mkdir()) {
                System.err.println("Directory for appdb couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }

        dir_workspace = "workspace";
        File f3 = new File(workingDir + "/" + dir_workspace);
        if (!f3.exists()) {
            if (!f3.mkdir()) {
                System.err.println("Directory for Workspace couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }

        dir_temp = "var";
        File f4 = new File(workingDir + "/" + dir_temp);
        if (!f4.exists()) {
            if (!f4.mkdir()) {
                System.err.println("Directory for VAR couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }
        File f7= new File("appdb/results.db");
        if(f7.exists()){
        f7.delete();
        }
        
    }

    public static void main(String[] args) {
        new settings();
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

}
