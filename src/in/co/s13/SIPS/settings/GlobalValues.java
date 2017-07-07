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

import in.co.s13.SIPS.db.OLDSQLiteJDBC;
import in.co.s13.SIPS.db.SQLiteJDBC;
import in.co.s13.SIPS.virtualdb.IPAddress;
import in.co.s13.SIPS.virtualdb.LiveNode;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONObject;

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
    public static PrintStream out, err, log;
    public static ExecutorService liveDBExecutor = Executors.newFixedThreadPool(1);
    public static JSONObject BENCHMARKING;

    public static ExecutorService pingExecutor;

    public static OLDSQLiteJDBC alldb = new OLDSQLiteJDBC("appdb/all.db");

    public static ExecutorService nodeDBExecutor = Executors.newFixedThreadPool(1);
    public static ObservableList<LiveNode> liveNodeDB = FXCollections.observableArrayList();
    public static ObservableList<IPAddress> allNodeDB = FXCollections.observableArrayList();

}
