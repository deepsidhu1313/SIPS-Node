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
package sips.node;

import in.co.s13.SIPS.benchmarks.Benchmarks;
import in.co.s13.SIPS.settings.Settings;
import in.co.s13.SIPS.initializer.HardwareStatThreads;
import in.co.s13.SIPS.initializer.NetworkThreads;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.HOST_NAME;
import in.co.s13.SIPS.tools.Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import static in.co.s13.SIPS.settings.GlobalValues.dir_etc;
import in.co.s13.SIPS.tools.ServiceOperations;
import java.util.concurrent.ConcurrentHashMap;
import static in.co.s13.SIPS.settings.GlobalValues.HAS_SHARED_STORAGE;

/**
 *
 * @author Nika
 */
public class SIPSNode {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        Settings loadSettings = new Settings();
        System.out.println("" + Util.getDeviceFromPath(new File(".").toPath()));
        Thread.currentThread().setName("Main");
        if (args.length > 0) {
            ArrayList<String> arguments = new ArrayList<>();
            Collections.addAll(arguments, args);
            if (arguments.contains("-h") || arguments.contains("--help")) {
                loadSettings.init();
                Util.outPrintln("Usage:\n"
                        + "\t java -jar SIPS-Node.jar <options>\n"
                        + "\noptions:"
                        + "\t\t-h or --help: to show help menu\n"
                        + "\t\t--mode <MODE>: specify mode in which SIPS node supposed to run\n"
                        + "\t\t\t MODE:\n"
                        + "\t\t\t\t 0:Run with Ping Server (Default Mode if no mode is pecified)\n"
                        + "\t\t\t\t 1:Run Without Ping Server (Private Mode)\n"
                        + "\t\t\t\t 2:Run Without Ping Server And Task Server (Master Only Mode)\n"
                        + "\n\t\t--generate-app-uuid:\n"
                        + "\t\t\tGenerates an unique id(UUID) for this node and replace the existing one.\n"
                        + "\n\t\t--benchmark:\n"
                        + "\t\t\tRun Benchmarks on resources before booting up node\n"
                        + "\n\t\t--set-process-limit <LIMIT>:\n"
                        + "\t\t\tLimit the number of processes run in parallel on this node. For example if processor has 4 cores then set this limit to 4 to maintain balance between speedup and hardware utilization.\n"
                        + "\n\t\t--set-file-resolvers <LIMIT>:\n"
                        + "\t\t\tLimit the number of threads run in parallel on this node to download data from submitter/other nodes. \n"
                        + "\n\t\t--set-ping-handlers <LIMIT>:\n"
                        + "\t\t\tLimit the number of threads run in parallel on this node to respond to ping request(s) from other nodes.\n"
                        + "\n\t\t--set-process-handlers <LIMIT>:\n"
                        + "\t\t\tLimit the number of threads run in parallel on this node to handle submission of new tasks.\n"
                        + "\n\t\t--add-ip <N> IP1 IP2 ... IPN:\n"
                        + "\t\t\tAdd N number of hosts.\n"
                        + "\n\t\t--add-network <N> NET1 NET2 :\n"
                        + "\t\t\tAdd N number of networks.\n"
                        + "\n\t\t--blacklist <N> ROUGE-IP ROUGE.FQDN ROUGE-UUID :\n"
                        + "\t\t\t Blacklist N number of Nodes identified by UUIDs, IPS or FQDNs .\n"
                        + "\n\t\t--gen-api 192.168.1.12 7"
                        + "\n\t\tgenerate api key for IPs/hostname/UUID followed by permissions"
                        + "\n\t\t\tPermissions:"
                        + "\n\t\t\t4: Read : Read Data From Server "
                        + "\n\t\t\t2: Write : Change Settings of server"
                        + "\n\t\t\t1: Execute : Execute special operations (i.e. shutdown, reboot etc)"
                        + "\n\t\t\t4+2: Read + Write "
                        + "\n\t\t\t4+1: Read + Execute "
                        + "\n\t\t\t4+2+1: Read + Write + Execute "
                        + "\n\t\t\t2+1: Write + Execute "
                        + "");
                System.exit(0);
            }

            if (arguments.contains("--clean")) {
                Util.deleteDirectory(new File("data/"));
                Util.deleteDirectory(new File("proc/"));
            }

            /**
             * *
             * Important flag
             */
            if (arguments.contains("--shared-storage")) {
                GlobalValues.HAS_SHARED_STORAGE = true;
                loadSettings.init();
                Settings.saveSettings();
            } else {
                loadSettings.init();

            }

            /**
             * *
             * add new IP addresses or Hosts to the API LIST
             */
            GlobalValues.API_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_API_KEYS) ? "common" : HOST_NAME) + "-" : "") + "api.json");
            Iterator<String> it = GlobalValues.API_JSON.keys();
            GlobalValues.API_LIST = new ConcurrentHashMap<>();
            while (it.hasNext()) {
                String key = it.next();
                GlobalValues.API_LIST.put(key.trim(), GlobalValues.API_JSON.getJSONObject(key));
            }
            if (arguments.contains("--gen-api")) {
                int index = 0;
                String client = "", permissions = "4";
                try {
                    index = arguments.indexOf("--gen-api");
                    client = (arguments.get(index + 1));
                    permissions = (arguments.get(index + 2));
                } catch (Exception e) {
                    Util.errPrintln(""
                            + "\n Example: --gen-api 192.168.1.12 7"
                            + "\nto generate api key for IPs/hostname/UUID followed by permissions"
                            + "\n Permissions:"
                            + "\n\t4: Read : Read Data From Server "
                            + "\n\t2: Write : Change Settings of server"
                            + "\n\t1: Execute : Execute special operations (i.e. shutdown, reboot etc)"
                            + "\nException: " + e);
                    System.exit(1);
                }
                String key = Util.generateAPIKey();
                JSONObject info = new JSONObject();
                info.put("key", key);
                info.put("permissions", permissions);
                GlobalValues.API_JSON.put(client, info);
                Util.write(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_API_KEYS) ? "common" : HOST_NAME) + "-" : "") + "api.json", GlobalValues.API_JSON.toString(4));
                System.out.println(info.toString(4));
                System.exit(0);
            }

            if (arguments.contains("--benchmark")) {
                benchmark();
            } else {
                preBenchmarkingChecks();
            }

            if (arguments.contains("--generate-app-uuid")) {
                in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID = Util.generateNodeUUID();
                Settings.saveSettings();
            }

            if (arguments.contains("--set-process-limit")) {
                int limit = GlobalValues.TASK_LIMIT;
                try {
                    limit = Integer.parseInt(arguments.get(arguments.indexOf("--set-process-limit") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify limit"
                            + "\n Example: --set-process-limit 10 to set limit to 10 "
                            + "Exception: " + e);
                    System.exit(1);

                }
                GlobalValues.TASK_LIMIT = limit;
                loadSettings.saveSettings();
            }

            if (arguments.contains("--set-file-resolvers")) {
                int limit = GlobalValues.FILES_RESOLVER_LIMIT;
                try {
                    limit = Integer.parseInt(arguments.get(arguments.indexOf("--set-file-resolvers") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify limit"
                            + "\n Example: --set-file-resolvers 10 to set limit to 10 "
                            + "Exception: " + e);
                    System.exit(1);

                }
                GlobalValues.FILES_RESOLVER_LIMIT = limit;
                loadSettings.saveSettings();
            }

            if (arguments.contains("--set-ping-handlers")) {
                int limit = GlobalValues.PING_HANDLER_LIMIT;
                try {
                    limit = Integer.parseInt(arguments.get(arguments.indexOf("--set-ping-handlers") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify limit"
                            + "\n Example: --set-ping-handlers 10 to set limit to 10 "
                            + "Exception: " + e);
                    System.exit(1);

                }
                GlobalValues.PING_HANDLER_LIMIT = limit;
                loadSettings.saveSettings();
            }
            if (arguments.contains("--set-api-handlers")) {
                int limit = GlobalValues.API_HANDLER_LIMIT;
                try {
                    limit = Integer.parseInt(arguments.get(arguments.indexOf("--set-api-handlers") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify limit"
                            + "\n Example: --set-api-handlers 10 to set limit to 10 "
                            + "Exception: " + e);
                    System.exit(1);

                }
                GlobalValues.API_HANDLER_LIMIT = limit;
                loadSettings.saveSettings();
            }

            if (arguments.contains("--set-process-handlers")) {
                int limit = GlobalValues.TASK_HANDLER_LIMIT;
                try {
                    limit = Integer.parseInt(arguments.get(arguments.indexOf("--set-process-handlers") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify limit"
                            + "\n Example: --set-process-handlers 10 to set limit to 10 "
                            + "Exception: " + e);
                    System.exit(1);

                }
                GlobalValues.TASK_HANDLER_LIMIT = limit;
                loadSettings.saveSettings();
            }

            /**
             * *
             * add new IP addresses or Hosts
             */
            GlobalValues.IPs_TO_SCAN_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_IP_LIST) ? "common" : HOST_NAME) + "-" : "") + "ips.json");
            if (arguments.contains("--add-ip")) {
                int listSize = 0, index = 0;
                try {
                    index = arguments.indexOf("--add-ip");
                    listSize = Integer.parseInt(arguments.get(index + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify list"
                            + "\n Example: --add-ip 3 192.168.1.12 192.168.1.30 compute-node.your-domain-name.com "
                            + "\nto set size of list to 3 and IPs/hostname followed "
                            + "\nException: " + e);
                    System.exit(1);

                }
                JSONArray jsonArray = GlobalValues.IPs_TO_SCAN_JSON.getJSONArray("ips", new JSONArray());
                for (int i = 0; i < listSize; i++) {
                    jsonArray.put(arguments.get(index + i + 2));
                }
                GlobalValues.IPs_TO_SCAN_JSON = new JSONObject();
                GlobalValues.IPs_TO_SCAN_JSON.put("ips", jsonArray);
                Util.write(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_IP_LIST) ? "common" : HOST_NAME) + "-" : "") + "ips.json", GlobalValues.IPs_TO_SCAN_JSON.toString(4));
            }

            /**
             * *
             * add new networks to scan
             */
            GlobalValues.NETWORKS_TO_SCAN_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_NETWORK_LIST) ? "common" : HOST_NAME) + "-" : "") + "networks.json");
            if (arguments.contains("--add-network")) {
                int listSize = 0, index = 0;
                try {
                    index = arguments.indexOf("--add-network");
                    listSize = Integer.parseInt(arguments.get(index + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify list"
                            + "\n Example: --add-network 3 192.168.1.0 192.168.3.0 10.10.100.0 "
                            + "\nto set size of list to 3 and networks followed  "
                            + "\nException: " + e);
                    System.exit(1);

                }
                JSONArray jsonArray = GlobalValues.NETWORKS_TO_SCAN_JSON.getJSONArray("networks", new JSONArray());
                for (int i = 0; i < listSize; i++) {
                    jsonArray.put(arguments.get(index + i + 2));
                }
                GlobalValues.NETWORKS_TO_SCAN_JSON = new JSONObject();
                GlobalValues.NETWORKS_TO_SCAN_JSON.put("networks", jsonArray);
                Util.write(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_NETWORK_LIST) ? "common" : HOST_NAME) + "-" : "") + "networks.json", GlobalValues.NETWORKS_TO_SCAN_JSON.toString(4));
            }

            /**
             * *
             * blacklist nodes Put these on Raymond Reddington's List
             */
            GlobalValues.BLACKLIST_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_BLACKLIST) ? "common" : HOST_NAME) + "-" : "") + "blacklist.json");
            if (arguments.contains("--blacklist")) {
                int listSize = 0, index = 0;
                try {
                    index = arguments.indexOf("--blacklist");
                    listSize = Integer.parseInt(arguments.get(index + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify list"
                            + "\n Example: --blacklist 4 192.168.1.30 192.168.3.250 rouge-node.yourdomain.com rouge-uuid"
                            + "\nto set size of list to 4 and hosts followed identfied by IP, FQDN or UUID  "
                            + "\nException: " + e);
                    System.exit(1);

                }
                JSONArray jsonArray = GlobalValues.BLACKLIST_JSON.getJSONArray("blacklist", new JSONArray());
                for (int i = 0; i < listSize; i++) {
                    jsonArray.put(arguments.get(index + i + 2));
                }
                GlobalValues.BLACKLIST_JSON = new JSONObject();
                GlobalValues.BLACKLIST_JSON.put("blacklist", jsonArray);
                Util.write(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_BLACKLIST) ? "common" : HOST_NAME) + "-" : "") + "blacklist.json", GlobalValues.BLACKLIST_JSON.toString(4));
            }

            /**
             * *
             * Time to fire up every engine
             */
            if (arguments.contains("--mode")) {
                int mode = 0;
                try {
                    mode = Integer.parseInt(arguments.get(arguments.indexOf("--mode") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify mode"
                            + "\n Example: --mode 0 "
                            + "Exception: " + e);
                    System.exit(1);

                }
                switch (mode) {
                    /**
                     * *
                     * Default mode
                     */
                    case 0:
                    default:
                        new HardwareStatThreads();
                        new NetworkThreads();
                        ServiceOperations.initApiServerAtStartUp();
                        ServiceOperations.initPingServerAtStartUp();
                        ServiceOperations.initFileServerAtStartUp();
                        ServiceOperations.initTaskServerAtStartUp();
                        ServiceOperations.initTaskFinishListenerServerAtStartUp();
                        ServiceOperations.initJobServerAtStartUp();
                        ServiceOperations.initFileDownloadServerAtStartUp();
                        ServiceOperations.initLogRotateAtStartUp();
//                        ServiceOperations.initCleanResultDBAtStartUp();
                        break;
                    /**
                     * *
                     * Private Mode
                     */
                    case 1:

                        new HardwareStatThreads();
                        new NetworkThreads();
                        ServiceOperations.initApiServerAtStartUp();
                        ServiceOperations.initFileServerAtStartUp();
                        ServiceOperations.initTaskServerAtStartUp();
                        ServiceOperations.initTaskFinishListenerServerAtStartUp();
                        ServiceOperations.initFileDownloadServerAtStartUp();
                        ServiceOperations.initLogRotateAtStartUp();
                        ServiceOperations.initJobServerAtStartUp();
//                        ServiceOperations.initCleanResultDBAtStartUp();
                        break;
                    /**
                     * *
                     * Private Mode
                     */
                    case 2:

                        new HardwareStatThreads();
                        new NetworkThreads();
                        ServiceOperations.initApiServerAtStartUp();
                        ServiceOperations.initFileServerAtStartUp();
                        ServiceOperations.initTaskFinishListenerServerAtStartUp();
                        ServiceOperations.initFileDownloadServerAtStartUp();
                        ServiceOperations.initLogRotateAtStartUp();
                        ServiceOperations.initJobServerAtStartUp();
//                        ServiceOperations.initCleanResultDBAtStartUp();
                        break;
                }
            } else {
                preBenchmarkingChecks();
                new HardwareStatThreads();
                new NetworkThreads();
                ServiceOperations.initApiServerAtStartUp();
                ServiceOperations.initPingServerAtStartUp();
                ServiceOperations.initFileServerAtStartUp();
                ServiceOperations.initTaskServerAtStartUp();
                ServiceOperations.initTaskFinishListenerServerAtStartUp();
                ServiceOperations.initFileDownloadServerAtStartUp();
                ServiceOperations.initLogRotateAtStartUp();
                ServiceOperations.initJobServerAtStartUp();
//                ServiceOperations.initCleanResultDBAtStartUp();
            }

        } else {
            loadSettings.init();
            preBenchmarkingChecks();
            GlobalValues.BLACKLIST_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_BLACKLIST) ? "common" : HOST_NAME) + "-" : "") + "blacklist.json");
            GlobalValues.NETWORKS_TO_SCAN_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_NETWORK_LIST) ? "common" : HOST_NAME) + "-" : "") + "networks.json");
            GlobalValues.IPs_TO_SCAN_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_IP_LIST) ? "common" : HOST_NAME) + "-" : "") + "ips.json");
            GlobalValues.API_JSON = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? ((GlobalValues.HAS_COMMON_API_KEYS) ? "common" : HOST_NAME) + "-" : "") + "api.json");
            new HardwareStatThreads();
            new NetworkThreads();
            ServiceOperations.initApiServerAtStartUp();
            ServiceOperations.initPingServerAtStartUp();
            ServiceOperations.initFileServerAtStartUp();
            ServiceOperations.initTaskServerAtStartUp();
            ServiceOperations.initTaskFinishListenerServerAtStartUp();
            ServiceOperations.initFileDownloadServerAtStartUp();
            ServiceOperations.initLogRotateAtStartUp();
//            ServiceOperations.initCleanResultDBAtStartUp();
            ServiceOperations.initJobServerAtStartUp();

        }

    }

    public static void benchmark() {
        JSONObject benchmarkResults = new JSONObject();
        JSONObject cpu = new JSONObject();
        JSONObject hdd = new JSONObject();
        cpu.put("Name", GlobalValues.CPU_NAME);
        cpu.put("Benchmarks", new JSONObject(Benchmarks.benchmarkCPU()));
        benchmarkResults.put("CPU", cpu);
        hdd.put("Label", Util.getDeviceModel(Util.getDeviceFromPath(new File(".").toPath())).trim());
        hdd.put("Benchmarks", new JSONObject(Benchmarks.benchmarkHDD()));
        benchmarkResults.put("HDD", hdd);
        benchmarkResults.put("MEMORY", GlobalValues.MEM_SIZE);
        benchmarkResults.put("TIMESTAMP", System.currentTimeMillis());
        GlobalValues.BENCHMARKING = benchmarkResults;
        Util.write(new File(dir_etc + "/" + (HAS_SHARED_STORAGE ? HOST_NAME + "-" : "") + "benchmarks.json"), benchmarkResults.toString(4));

    }

    public static void preBenchmarkingChecks() {
        if (new File(dir_etc + "/" + (HAS_SHARED_STORAGE ? HOST_NAME + "-" : "") + "benchmarks.json").exists()) {
            JSONObject benchmarkResults = Util.readJSONFile(dir_etc + "/" + (HAS_SHARED_STORAGE ? HOST_NAME + "-" : "") + "benchmarks.json");
            long timestamp = benchmarkResults.getLong("TIMESTAMP", 0l);
            if ((TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)) > 1) {
                benchmark();
            } else {
                GlobalValues.BENCHMARKING = benchmarkResults;
            }
        } else {
            benchmark();
        }

    }

}
