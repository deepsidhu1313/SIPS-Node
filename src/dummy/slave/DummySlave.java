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
package dummy.slave;

import in.co.s13.SIPS.benchmarks.Benchmarks;
import in.co.s13.SIPS.settings.Settings;
import in.co.s13.SIPS.executor.sockets.FileReqQueServer;
import in.co.s13.SIPS.executor.sockets.PingServer;
import in.co.s13.SIPS.executor.sockets.Server;
import in.co.s13.SIPS.initializer.HardwareStatThreads;
import in.co.s13.SIPS.initializer.NetworkThreads;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.dir_appdb;
import in.co.s13.SIPS.tools.Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import jdk.nashorn.internal.runtime.GlobalConstants;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class DummySlave {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        Settings loadSettings = new Settings();
        if (args.length > 0) {
            ArrayList<String> arguments = new ArrayList<>();
            Collections.addAll(arguments, args);
            if (arguments.contains("-h") || arguments.contains("--help")) {
                Util.outPrintln("Usage:\n"
                        + "\t java -jar SIPS-Node.jar <options>\n"
                        + "\noptions:"
                        + "\t\t-h or --help: to show help menu\n"
                        + "\t\t--mode <MODE>: specify mode in which SIPS node supposed to run\n"
                        + "\t\t\t MODE:\n"
                        + "\t\t\t\t 0:Run with Ping Server (Default Mode if no mode is pecified)\n"
                        + "\t\t\t\t 1:Run Without Ping Server (Private Mode)\n"
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
                        + "");
                return;
            }

            if (arguments.contains("--benchmark")) {
                benchmark();
            } else {
                preBenchmarkingChecks();
            }

            if (arguments.contains("--generate-app-uuid")) {
                GlobalValues.NODE_UUID = Util.generateNodeUUID();
                loadSettings.saveSettings();
            }

            if (arguments.contains("--set-process-limit")) {
                int limit = GlobalValues.PROCESS_LIMIT;
                try {
                    limit = Integer.parseInt(arguments.get(arguments.indexOf("--set-process-limit") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify limit"
                            + "\n Example: --set-process-limit 10 to set limit to 10 "
                            + "Exception: " + e);
                    return;
                }
                GlobalValues.PROCESS_LIMIT = limit;
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
                    return;
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
                            + "\n Example: --set-process-handlers 10 to set limit to 10 "
                            + "Exception: " + e);
                    return;
                }
                GlobalValues.PING_HANDLER_LIMIT = limit;
                loadSettings.saveSettings();
            }

            if (arguments.contains("--set-process-handlers")) {
                int limit = GlobalValues.PROCESS_HANDLER_LIMIT;
                try {
                    limit = Integer.parseInt(arguments.get(arguments.indexOf("--set-process-handlers") + 1));
                } catch (NumberFormatException e) {
                    Util.errPrintln("Use number to specify limit"
                            + "\n Example: --set-process-handlers 10 to set limit to 10 "
                            + "Exception: " + e);
                    return;
                }
                GlobalValues.PROCESS_HANDLER_LIMIT = limit;
                loadSettings.saveSettings();
            }

            /**
             * *
             * add new IP addresses or Hosts
             */
            GlobalValues.ipToScanJSON = new JSONObject(Util.readFile(dir_appdb + "/ips.json"));
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
                    return;
                }
                JSONArray jsonArray = GlobalValues.ipToScanJSON.getJSONArray("ips", new JSONArray());
                for (int i = 0; i < listSize; i++) {
                    jsonArray.put(arguments.get(index + i + 2));
                }
                GlobalValues.ipToScanJSON = new JSONObject();
                GlobalValues.ipToScanJSON.put("ips", jsonArray);
                Util.write(dir_appdb + "/ips.json", GlobalValues.ipToScanJSON.toString(4));
            }

            /**
             * *
             * add new networks to scan
             */
            GlobalValues.networksToScanJSON = new JSONObject(Util.readFile(dir_appdb + "/networks.json"));
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
                    return;
                }
                JSONArray jsonArray = GlobalValues.networksToScanJSON.getJSONArray("networks", new JSONArray());
                for (int i = 0; i < listSize; i++) {
                    jsonArray.put(arguments.get(index + i + 2));
                }
                GlobalValues.networksToScanJSON = new JSONObject();
                GlobalValues.networksToScanJSON.put("networks", jsonArray);
                Util.write(dir_appdb + "/networks.json", GlobalValues.networksToScanJSON.toString(4));
            }

            /**
             * *
             * blacklist nodes Put these on Raymond Reddington's List
             */
            GlobalValues.blacklistJSON = new JSONObject(Util.readFile(dir_appdb + "/blacklist.json"));
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
                    return;
                }
                JSONArray jsonArray = GlobalValues.blacklistJSON.getJSONArray("blacklist", new JSONArray());
                for (int i = 0; i < listSize; i++) {
                    jsonArray.put(arguments.get(index + i + 2));
                }
                GlobalValues.blacklistJSON = new JSONObject();
                GlobalValues.blacklistJSON.put("blacklist", jsonArray);
                Util.write(dir_appdb + "/blacklist.json", GlobalValues.blacklistJSON.toString(4));
            }

            if (arguments.contains("--shared-storage")) {
                GlobalValues.SHARED_STORAGE = true;
                loadSettings.saveSettings();
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
                    return;
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
                        Thread pingServer = new Thread(new PingServer(true, 0));
                        pingServer.start();
                        Thread server = new Thread(new Server(true));
                        server.start();
                        Thread downloadQueueServer = new Thread(new FileReqQueServer(true));
                        downloadQueueServer.start();
                        break;
                    /**
                     * *
                     * Private Mode
                     */
                    case 1:

                        new HardwareStatThreads();
                        new NetworkThreads();
                        Thread server2 = new Thread(new Server(true));
                        server2.start();
                        Thread downloadQueueServer2 = new Thread(new FileReqQueServer(true));
                        downloadQueueServer2.start();
                        break;
                }
            } else {
                preBenchmarkingChecks();
                new HardwareStatThreads();
                new NetworkThreads();
                Thread server = new Thread(new Server(true));
                server.start();
                Thread pingServer = new Thread(new PingServer(true, 3));
                pingServer.start();
                Thread downloadQueServer = new Thread(new FileReqQueServer(true));
                downloadQueServer.start();
            }

        }

    }

    public static void benchmark() {
        JSONObject benchmarkResults = new JSONObject();
        JSONObject cpu = new JSONObject();
        JSONObject hdd = new JSONObject();
        cpu.put("Name", GlobalValues.CPU_NAME);
        cpu.put("Benchmarks", Benchmarks.benchmarkCPU());
        benchmarkResults.put("CPU", cpu);
        hdd.put("Label", Util.getDeviceModel(Util.getDeviceFromPath(new File(".").toPath())));
        hdd.put("Benchmarks", Benchmarks.benchmarkHDD());
        benchmarkResults.put("HDD", hdd);
        benchmarkResults.put("MEMORY", GlobalValues.MEM_SIZE);
        benchmarkResults.put("TIMESTAMP", System.currentTimeMillis());
        GlobalValues.BENCHMARKING = benchmarkResults;
        Util.write(new File(dir_appdb + "/benchmarks.json"), benchmarkResults.toString(4));

    }

    public static void preBenchmarkingChecks() {
        if (new File(dir_appdb + "/benchmarks.json").exists()) {
            JSONObject benchmarkResults = new JSONObject(Util.readFile(dir_appdb + "/benchmarks.json"));
            long timestamp = benchmarkResults.getLong("TIMESTAMP");
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
