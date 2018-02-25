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

import in.co.s13.SIPS.datastructure.DistributionDBRow;
import in.co.s13.SIPS.datastructure.FileDownQueReq;
import in.co.s13.SIPS.datastructure.Resource;
import in.co.s13.SIPS.datastructure.threadpools.FixedThreadPool;
import in.co.s13.SIPS.db.OLDSQLiteJDBC;
import in.co.s13.SIPS.datastructure.NodeDBRow;
import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.datastructure.TaskDBRow;
import in.co.s13.sips.lib.common.datastructure.Node;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class GlobalValues {

    /**
     * Meta Info Operating System
     */
    /**
     * *
     * Operating System Name in lowercase string from JVM property
     */
    public static String OS = System.getProperty("os.name").toLowerCase();

    /**
     * Integer representation of Operating System Name<br> 0 = Windows<br>
     * 1 = Mac<br>
     * 2 = *nix<br>
     * 3 = Solaris<br>
     */
    public static int OS_Name = 0;

    /**
     * **
     * Directories
     */
    /**
     * Present Working Directory
     */
    public static String PWD = "";

    /**
     * *
     * Workspace Directory where all the code can be saved
     */
    public static String dir_workspace = "workspace";
    /**
     * Linux like etc directory to store config files
     */
    public static String dir_etc = "etc";
    /**
     * Linux like bin directory to store scripts and binaries
     */
    public static String dir_bin = "bin";
    /**
     * LOG directory to store all the LOG files
     */
    public static String dir_log = "log";
    /**
     * Linux like proc directory to store information of running tasks
     */
    public static String dir_temp = "proc";

    /**
     * *
     * Resource Stats
     */
    /**
     * Variable to hold Local task counter
     */
    public static AtomicLong TASK_ID = new AtomicLong(0);
//    public static SQLiteJDBC TASK_DB = new SQLiteJDBC();
    /**
     * Variable to hold Hostname of the node
     */
    public static String HOST_NAME = "SIPS-Node";
    /**
     * UUID of current Node which is required to identify machine on the SIPS
     * system.
     */
    public static String NODE_UUID = "";
    /**
     * Total Memory Size
     */
    public static long MEM_SIZE = 0L;
    /**
     * Free Memory
     */
    public static long MEM_FREE = 0L;
    /**
     * Storage Disk size
     */
    public static long HDD_SIZE = 0L;
    /**
     * Free storage
     */
    public static long HDD_FREE = 0L;

    /**
     * Name of CPU
     */
    public static String CPU_NAME = "";

    /**
     * Linux CPU load avg of last 1 min
     */
    public static double CPU_LOAD_AVG = 0.0;

    /**
     * Benchmarking results of CPU and HDD
     */
    public static JSONObject BENCHMARKING;

    /**
     * *
     * Resource like CPU , HDD and GPU and their details
     */
    public static ConcurrentHashMap<String, Resource> resources = new ConcurrentHashMap<>(5);
    /**
     * IP Addresses associated with current Node
     */

    public static JSONArray IP_ADDRESSES = new JSONArray();
    /**
     * Boolean to store if storage between multiple Nodes is shared or not
     */
    public static boolean HAS_SHARED_STORAGE = false;

    /**
     * Boolean to store if API keys between multiple Nodes are shared or not
     */
    public static boolean HAS_COMMON_API_KEYS = true;

    /**
     * Boolean to store if IP to scan between multiple Nodes are shared or not
     */
    public static boolean HAS_COMMON_IP_LIST = true;

    /**
     * Boolean to store if Networks to scan between multiple Nodes are shared or
     * not
     */
    public static boolean HAS_COMMON_NETWORK_LIST = true;

    /**
     * Boolean to store if Blacklist between multiple Nodes are shared or not
     */
    public static boolean HAS_COMMON_BLACKLIST = true;

    /**
     * Log Files and variables
     */
    /**
     * Flag to give verbose output
     */
    public static boolean VERBOSE = false;
    /**
     * Flag to Dump Log
     */
    public static boolean DUMP_LOG = false;

    public static String OUT_FILE,
            ERR_FILE,
            LOG_FILE,
            API_LOG_FILE,
            FILE_DOWNLOAD_LOG_FILE,
            FILE_SERVER_LOG_FILE,
            PING_SERVER_LOG_FILE,
            TASK_LOG_FILE,
            JOB_LOG_FILE,
            PING_REQ_LOG_FILE;
    public static PrintStream OUT,
            ERR,
            LOG,
            API_LOG_PRINTER,
            FILE_DOWNLOAD_QUE_LOG_PRINTER,
            FILE_SERVER_LOG_PRINTER,
            PING_SERVER_LOG_PRINTER,
            TASK_LOG_PRINTER,
            JOB_LOG_PRINTER,
            PING_LOG_PRINTER;

    /**
     * all Node DB
     */
    public static OLDSQLiteJDBC alldb;

    public static enum LOG_LEVEL {
        ERROR, OUTPUT,
    };

    /**
     * Executor Limits
     */
    public static int FILES_RESOLVER_LIMIT = 10;
    public static int FILE_HANDLER_LIMIT = 10;
    public static int PING_HANDLER_LIMIT = 2;
    public static int API_HANDLER_LIMIT = 10;
    public static int TASK_HANDLER_LIMIT = 10;
    public static int JOB_HANDLER_LIMIT = 10;
    public static int TASK_FINISH_LISTENER_HANDLER_LIMIT = 10;
    public static int TASK_LIMIT = (Runtime.getRuntime().availableProcessors() - 2) < 1 ? 1 : (Runtime.getRuntime().availableProcessors() - 2);
    public static int JOB_LIMIT = TASK_LIMIT;
    public static int TOTAL_IP_SCANNING_THREADS = TASK_LIMIT;
    public static int PING_REQUEST_LIMIT = TASK_LIMIT;
    public static int PING_REQUEST_LIMIT_FOR_LIVE_NODES = TASK_LIMIT;
    public static AtomicLong TASK_WAITING = new AtomicLong(0);
    public static AtomicLong JOB_WAITING = new AtomicLong(0);
    /**
     * *
     * Executors
     */
    //    public static ExecutorService NETWORK_EXECUTOR;
    //    public static ExecutorService TASK_EXECUTOR = Executors.newFixedThreadPool(TASK_LIMIT);
    public static ExecutorService NODE_SCANNER_EXECUTOR = Executors.newFixedThreadPool(2);
    public static FixedThreadPool NETWORK_EXECUTOR;
    public static FixedThreadPool TASK_EXECUTOR;
    public static FixedThreadPool JOB_EXECUTOR;
    public static FixedThreadPool PING_REQUEST_EXECUTOR;
    public static FixedThreadPool PING_REQUEST_EXECUTOR_FOR_LIVE_NODES;
    public static ExecutorService NODE_DB_EXECUTOR = Executors.newFixedThreadPool(1);
    public static ExecutorService TASK_DB_EXECUTOR = Executors.newFixedThreadPool(1);
    public static ExecutorService JOB_DB_EXECUTOR = Executors.newFixedThreadPool(1);
    public static ExecutorService LIVE_DB_EXECUTOR = Executors.newFixedThreadPool(1);
    public static ExecutorService DIST_DB_EXECUTOR = Executors.newFixedThreadPool(TASK_LIMIT);
    public static ExecutorService DIST_WH_DB_EXECUTOR = Executors.newFixedThreadPool(1);
    public static ExecutorService RESULT_DB_EXECUTOR = Executors.newFixedThreadPool(TASK_LIMIT);
    public static ExecutorService RESULT_WH_DB_EXECUTOR = Executors.newFixedThreadPool(1);
    public static ExecutorService LOG_IO_EXECUTOR = Executors.newFixedThreadPool(1);
    public static ExecutorService SEND_SLEEPTIME_EXECUTOR_SERVICE = Executors.newFixedThreadPool(1);

    /**
     * Server ThreadPools
     */
    public static FixedThreadPool API_HANDLER_EXECUTOR_SERVICE, PING_HANDLER_EXECUTOR_SERVICE, FILE_DOWNLOAD_HANDLER_EXECUTOR_SERVICE, FILE_HANDLER_EXECUTOR_SERVICE, TASK_HANDLER_EXECUTOR_SERVICE, JOB_HANDLER_EXECUTOR_SERVICE, TASK_FINISH_LISTENER_HANDLER_EXECUTOR_SERVICE;

    /**
     * *
     * Server Threads
     */
    public static Thread TASK_SERVER_THREAD, JOB_SERVER_THREAD, API_SERVER_THREAD, PING_SERVER_THREAD, FILE_DOWNLOAD_SERVER_THREAD, TASK_FINISH_LISTENER_SERVER_THREAD, FILE_SERVER_THREAD;

    /**
     * Server sockets
     */
    public static ServerSocket API_SERVER_SOCKET, FILE_DOWNLOAD_SERVER_SOCKET, FILE_SERVER_SOCKET, PING_SERVER_SOCKET, TASK_SERVER_SOCKET, JOB_SERVER_SOCKET, TASK_FINISH_LISTENER_SERVER_SOCKET;

    /**
     * Socket Ports
     */
    public static int PING_SERVER_PORT = 13131, FILE_DOWNLOAD_SERVER_PORT = 13132, TASK_SERVER_PORT = 13133, TASK_FINISH_LISTENER_SERVER_PORT = 13134, FILE_SERVER_PORT = 13135, JOB_SERVER_PORT = 13136, API_SERVER_PORT = 13139;

    /**
     * Server Flags
     */
    public static boolean API_SERVER_IS_RUNNING = true, FILE_DOWNLOAD_SERVER_IS_RUNNING = true, FILE_SERVER_IS_RUNNING = true, PING_SERVER_IS_RUNNING = true, TASK_SERVER_IS_RUNNING = true, JOB_SERVER_IS_RUNNING = true, TASK_FINISH_SERVER_IS_RUNNING = true;
    /**
     * Network Scheduled Thread Conditions
     */
    public static boolean KEEP_LIVE_NODE_SCANNER_ALIVE = true, KEEP_NODE_SCANNER_ALIVE = true, SCANNING_LOCAL_NETWORK_FOR_RESOURCES = false;

    /**
     * *
     * Network Scanning threads
     */
    public static Thread CHECK_LIVE_NODE_THREAD, NODE_SCANNING_THREAD;

    /**
     * Services Vars
     */
    public static boolean PING_SERVER_ENABLED_AT_START = true, LOG_ROTATE_ENABLED_AT_START = true, CLEAN_RESULT_DB_ENABLED_AT_START = true, API_SERVER_ENABLED_AT_START = true, FILE_DOWNLOAD_SERVER_ENABLED_AT_START = true, FILE_SERVER_ENABLED_AT_START = true, TASK_SERVER_ENABLED_AT_START = true, JOB_SERVER_ENABLED_AT_START = true, NODE_SCANNER_ENABLED_AT_START = true, LIVE_NODE_SCANNER_ENABLED_AT_START = true, TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START = true;

    public static long NODE_SCANNER_INTIAL_DELAY = 2L, LIVE_NODE_SCANNER_INTIAL_DELAY = 2L, NODE_SCANNER_PERIODIC_DELAY = 5L, LIVE_NODE_SCANNER_PERIODIC_DELAY = 5L;
    /**
     *
     * Storage Data Structure
     */
    /**
     *
     * Networking Vars
     */
    //  public static ArrayList<String> livehosts = new ArrayList();
    //  public static ObservableList<LiveNode> liveNodes = FXCollections.observableArrayList();
    public static ConcurrentHashMap<String, Node> LIVE_NODE_ADJ_DB = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Node> LIVE_NODE_NON_ADJ_DB = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, NodeDBRow> ALL_NODE_DB = new ConcurrentHashMap<>();
    public static ArrayList<String> HOSTS = new ArrayList<>();
    public static ConcurrentHashMap<String, String> CURRENTLY_SCANNING = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> BLACKLIST = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, JSONObject> API_LIST = new ConcurrentHashMap<>();
    public static boolean IS_WRITING = false;
    public static JSONObject BLACKLIST_JSON, NETWORKS_TO_SCAN_JSON, IPs_TO_SCAN_JSON, API_JSON;
    public static ConcurrentHashMap<String, String> ROUTING_TABLE = new ConcurrentHashMap<>();
//    public static ConcurrentHashMap<String, Hop> ADJACENT_NODES_TABLE = new ConcurrentHashMap<>();
//    public static ConcurrentHashMap<String, UniqueElementList> NON_ADJACENT_NODES_TABLE = new ConcurrentHashMap<>();

    /**
     * Task Storage
     */
    public static ConcurrentHashMap<String, ConcurrentHashMap<String, DistributionDBRow>> MASTER_DIST_DB = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Result> RESULT_DB = new ConcurrentHashMap<>();

    /**
     * Task Server Vars
     */
//    public static ConcurrentHashMap<String,String> ALIEN_PROCESS_ID= new ConcurrentHashMap<>();
    public static ArrayList<Integer> LOCAL_PROCESS_ID = new ArrayList<>();
    public static ConcurrentHashMap<String, TaskDBRow> TASK_DB = new ConcurrentHashMap<>();

    /**
     * Download Server Que
     */
    public static ConcurrentHashMap<String, FileDownQueReq> DOWNLOAD_QUEUE = new ConcurrentHashMap<>();

    /**
     * Logrotate FILE_SIZE_LIMIT in kb LOGROTATION_INTERVAL_IN_HOURS (hrs)
     * LAST_ROTATED_ON timestamp in millis LOG_ROTATE_CHECK_FILES_EVERY in secs
     */
    public static long LOG_FILE_SIZE_LIMIT = 512,
            LOGROTATION_INTERVAL_IN_HOURS = 24,
            LAST_ROTATED_ON = System.currentTimeMillis(),
            LOG_ROTATE_CHECK_FILES_EVERY = 300, CLEAN_RESULT_DB_EVERY = 30;
    public static boolean KEEP_LOG_ROTATE_ALIVE = true;
    public static Thread LOG_ROTATE_THREAD;

    public static boolean KEEP_CLEAN_RESULT_DB_THREAD_ALIVE = true;
    public static Thread CLEAN_RESULT_DB_THREAD;

}
