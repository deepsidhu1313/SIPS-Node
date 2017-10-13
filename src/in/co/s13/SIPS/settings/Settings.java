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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import static in.co.s13.SIPS.settings.GlobalValues.*;
import in.co.s13.SIPS.tools.Util;
import static in.co.s13.SIPS.tools.Util.getCPUName;
import static in.co.s13.SIPS.tools.Util.isMac;
import static in.co.s13.SIPS.tools.Util.isSolaris;
import static in.co.s13.SIPS.tools.Util.isUnix;
import static in.co.s13.SIPS.tools.Util.isWindows;
import static in.co.s13.SIPS.tools.Util.readFile;
import static in.co.s13.SIPS.tools.Util.write;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

public class Settings {

    public Settings() {

    }

    public void init() {
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
            System.out.println("Your OS is not supported!!");
            OS_Name = 4;

        }

        String workingDir = System.getProperty("user.dir");
        System.out.println("Current working directory : " + workingDir);
        PWD = workingDir;

        File f = new File(workingDir);
        System.out.println("" + f.getAbsolutePath());

        File f4 = new File(workingDir + "/" + dir_temp);
        if (!f4.exists()) {
            if (!f4.mkdir()) {
                Util.errPrintln("Directory for VAR couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }
        File detc = new File(dir_etc);
        if (!detc.exists()) {
            if (!detc.mkdir()) {
                Util.errPrintln("Directory for etc couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }

        File dlog = new File(dir_log);
        if (!dlog.exists()) {
            if (!dlog.mkdir()) {
                Util.errPrintln("Directory for log couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }

        File dbin = new File(dir_bin);
        if (!dbin.exists()) {
            if (!dbin.mkdir()) {
                Util.errPrintln("Directory for log couldnot be created !\n"
                        + "Please create a dir with this name");
            }
        }
        if (new File(dir_etc + "/" + (SHARED_STORAGE ? HOST_NAME : "") + "settings.json").exists()) {
            loadSettings();
        } else {
            saveSettings();
        }
        alldb = new OLDSQLiteJDBC(dir_etc + "/all.db");
//        TASK_DB_EXECUTOR.execute(() -> {
//            String sql = "CREATE TABLE PROC (ID    INT   PRIMARY KEY     NOT NULL,"
//                    + " ALIENID  INT,"
//                    + "FNAME     TEXT,"
//                    + "CNO     INT,"
//                    + "IP   TEXT);";
//            File f1 = new File(dir_etc + "/proc.db");
//            if (f1.exists()) {
//                f1.delete();
//            }
//            TASK_DB.createtable(dir_etc + "/proc.db", sql);
//            TASK_DB.closeConnection();
//        });
        try {
            ipAddresses = new JSONArray(Util.getLocalHostLANAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            HOST_NAME = InetAddress.getLocalHost().getHostName().trim();
        } catch (UnknownHostException e) {
            System.err.println("Couldn't get Hostname" + e);
            JSONObject ipHostnameCombo = ipAddresses.getJSONObject(0);
            HOST_NAME = ipHostnameCombo.getString("hostname", HOST_NAME);
        }
        try (PrintStream procn = new PrintStream(dir_bin + "/procn.bat")) {
            procn.print("wmic cpu get name");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        MEM_SIZE = Util.getMemorySize();
        CPU_NAME = getCPUName();

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(System.currentTimeMillis()));
            String prevContent = Util.readFile(ERR_FILE);
            GlobalValues.err = new PrintStream(GlobalValues.ERR_FILE);
            GlobalValues.err.println(prevContent);
            GlobalValues.err.println("\n\n****************************************************************"
                    + "\n**************** " + timestamp + " ************************"
                    + "\n****************************************************************\n");
            prevContent = Util.readFile(OUT_FILE);
            GlobalValues.out = new PrintStream(GlobalValues.OUT_FILE);
            GlobalValues.out.println(prevContent);
            GlobalValues.out.println("\n\n****************************************************************"
                    + "\n**************** " + timestamp + " ************************"
                    + "\n****************************************************************\n");
            prevContent = Util.readFile(LOG_FILE);
            GlobalValues.log = new PrintStream(GlobalValues.LOG_FILE);
            GlobalValues.log.println(prevContent);
            GlobalValues.log.println("\n\n****************************************************************"
                    + "\n**************** " + timestamp + " ************************"
                    + "\n****************************************************************\n");

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }

        saveSettings();
    }

    void loadSettings() {
        JSONObject settings = Util.readJSONFile(dir_etc + "/" + (SHARED_STORAGE ? HOST_NAME : "") + "settings.json");
        NODE_UUID = settings.getString("UUID", "");

        if (NODE_UUID.length() < 1) {
            NODE_UUID = Util.generateNodeUUID();
        }
        DUMP_LOG = settings.getBoolean("DUMP_LOG", true);
        VERBOSE = settings.getBoolean("VERBOSE", true);
        SHARED_STORAGE = settings.getBoolean("SHARED_STORAGE", SHARED_STORAGE);
        JSONObject serviceSettings = Util.readJSONFile(dir_etc + "/" + (SHARED_STORAGE ? HOST_NAME : "") + "service_settings.json");
        TASK_LIMIT = serviceSettings.getInt("MAX_TASK_ALLOWED_IN_PARALLEL", 2);
        FILES_RESOLVER_LIMIT = serviceSettings.getInt("MAX_FILE_RESOLVE_IN_PARALLEL", 3);
        PING_HANDLER_LIMIT = serviceSettings.getInt("MAX_PING_RESPONSES_IN_PARALLEL", 3);
        API_HANDLER_LIMIT = serviceSettings.getInt("MAX_API_RESPONSES_IN_PARALLEL", 3);
        TASK_HANDLER_LIMIT = serviceSettings.getInt("MAX_TASK_REQ_IN_PARALLEL", 3);
        PING_REQUEST_LIMIT = serviceSettings.getInt("MAX_PING_REQUESTS_IN_PARALLEL", 3);
        PING_SERVER_ENABLED_AT_START = serviceSettings.getBoolean("PING_SERVER_ENABLED_AT_START", PING_SERVER_ENABLED_AT_START);
        API_SERVER_ENABLED_AT_START = serviceSettings.getBoolean("API_SERVER_ENABLED_AT_START", API_SERVER_ENABLED_AT_START);
        FILE_DOWNLOAD_SERVER_ENABLED_AT_START = serviceSettings.getBoolean("FILE_DOWNLOAD_SERVER_ENABLED_AT_START", FILE_DOWNLOAD_SERVER_ENABLED_AT_START);
        FILE_SERVER_ENABLED_AT_START = serviceSettings.getBoolean("FILE_SERVER_ENABLED_AT_START", FILE_SERVER_ENABLED_AT_START);
        TASK_SERVER_ENABLED_AT_START = serviceSettings.getBoolean("TASK_SERVER_ENABLED_AT_START", TASK_SERVER_ENABLED_AT_START);
        NODE_SCANNER_ENABLED_AT_START = serviceSettings.getBoolean("NODE_SCANNER_ENABLED_AT_START", NODE_SCANNER_ENABLED_AT_START);
        LIVE_NODE_SCANNER_ENABLED_AT_START = serviceSettings.getBoolean("LIVE_NODE_SCANNER_ENABLED_AT_START", LIVE_NODE_SCANNER_ENABLED_AT_START);
        LIVE_NODE_SCANNER_INTIAL_DELAY = serviceSettings.getLong("LIVE_NODE_SCANNER_INTIAL_DELAY", LIVE_NODE_SCANNER_INTIAL_DELAY);
        NODE_SCANNER_INTIAL_DELAY = serviceSettings.getLong("NODE_SCANNER_INTIAL_DELAY", NODE_SCANNER_INTIAL_DELAY);
        LIVE_NODE_SCANNER_PERIODIC_DELAY = serviceSettings.getLong("LIVE_NODE_SCANNER_PERIODIC_DELAY", LIVE_NODE_SCANNER_PERIODIC_DELAY);
        NODE_SCANNER_PERIODIC_DELAY = serviceSettings.getLong("NODE_SCANNER_PERIODIC_DELAY", NODE_SCANNER_PERIODIC_DELAY);
        TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START=serviceSettings.getBoolean("TASK_FINISH_LISTENER_ENABLED_AT_START", TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START);
    }

    public void saveSettings() {
        JSONObject settings = new JSONObject();

        if (NODE_UUID.length() < 1) {
            NODE_UUID = Util.generateNodeUUID();
        }
        settings.put("UUID", NODE_UUID);

        settings.put("DUMP_LOG", DUMP_LOG);
        settings.put("VERBOSE", VERBOSE);
        settings.put("SHARED_STORAGE", SHARED_STORAGE);
        write(new File(dir_etc + "/" + (SHARED_STORAGE ? HOST_NAME : "") + "settings.json"), settings.toString(4));

        JSONObject serviceSettings = new JSONObject();
        serviceSettings.put("MAX_TASK_ALLOWED_IN_PARALLEL", TASK_LIMIT);
        serviceSettings.put("MAX_FILE_RESOLVE_IN_PARALLEL", FILES_RESOLVER_LIMIT);
        serviceSettings.put("MAX_PING_RESPONSES_IN_PARALLEL", PING_HANDLER_LIMIT);
        serviceSettings.put("MAX_PING_REQUESTS_IN_PARALLEL", PING_REQUEST_LIMIT);
        serviceSettings.put("MAX_API_RESPONSES_IN_PARALLEL", API_HANDLER_LIMIT);
        serviceSettings.put("MAX_TASK_REQ_IN_PARALLEL", TASK_HANDLER_LIMIT);
        serviceSettings.put("PING_SERVER_ENABLED_AT_START", PING_SERVER_ENABLED_AT_START);
        serviceSettings.put("API_SERVER_ENABLED_AT_START", API_SERVER_ENABLED_AT_START);
        serviceSettings.put("FILE_DOWNLOAD_SERVER_ENABLED_AT_START", FILE_DOWNLOAD_SERVER_ENABLED_AT_START);
        serviceSettings.put("FILE_SERVER_ENABLED_AT_START", FILE_SERVER_ENABLED_AT_START);
        serviceSettings.put("TASK_SERVER_ENABLED_AT_START", TASK_SERVER_ENABLED_AT_START);
        serviceSettings.put("TASK_FINISH_LISTENER_ENABLED_AT_START", TASK_FINISH_LISTENER_SERVER_ENABLED_AT_START);
        serviceSettings.put("NODE_SCANNER_ENABLED_AT_START", NODE_SCANNER_ENABLED_AT_START);
        serviceSettings.put("LIVE_NODE_SCANNER_ENABLED_AT_START", LIVE_NODE_SCANNER_ENABLED_AT_START);
        serviceSettings.put("LIVE_NODE_SCANNER_INTIAL_DELAY", LIVE_NODE_SCANNER_INTIAL_DELAY);
        serviceSettings.put("NODE_SCANNER_INTIAL_DELAY", NODE_SCANNER_INTIAL_DELAY);
        serviceSettings.put("LIVE_NODE_SCANNER_PERIODIC_DELAY", LIVE_NODE_SCANNER_PERIODIC_DELAY);
        serviceSettings.put("NODE_SCANNER_PERIODIC_DELAY", NODE_SCANNER_PERIODIC_DELAY);

        write(new File(dir_etc + "/" + (SHARED_STORAGE ? HOST_NAME : "") + "service_settings.json"), serviceSettings.toString(4));

    }

}
