/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.settings;

import in.co.s13.SIPS.db.SQLiteJDBC;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author nika
 */
public class GlobalValues {

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static int OS_Name = 0;
    public static String PWD = "";
    public static String dir_workspace = "";
    public static String dir_appdb = "";
    public static String dir_temp = "";
    public static int total_threads = 1;
    public static int process_id = 0;
    public static int PROCESS_LIMIT = Runtime.getRuntime().availableProcessors() - 1;
    public static int PROCESS_WAITING = 0;
    public static int FILES_RESOLVER_LIMIT = 30;
    public static int PING_HANDLER_LIMIT = 100;
    public static int PROCESS_HANDLER_LIMIT = 100;
    public static ExecutorService processExecutor = Executors.newFixedThreadPool(PROCESS_LIMIT);
    public static ExecutorService processDBExecutor = Executors.newFixedThreadPool(1);
    public static SQLiteJDBC procDB = new SQLiteJDBC();
    public static String HOST_NAME = "DummySlave";
    public static long MEM_SIZE = 0L;
    public static String CPU_NAME = "";
    public static double CPU_LOAD_AVG = 0.0;
    public static ExecutorService netExecutor;
    public static boolean VERBOSE, DUMP_LOG;
}
