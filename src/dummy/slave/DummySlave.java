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
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.dir_appdb;
import in.co.s13.SIPS.tools.Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
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
                System.out.println("Usage:\n"
                        + "\t java -jar SIPS-Node.jar <options>\n"
                        + "\noptions:"
                        + "\t\t-h or --help: to show help menu\n"
                        + "\t\t--mode <MODE>: specify mode in which SIPS node supposed to run\n"
                        + "\t\t\t MODE:\n"
                        + "\t\t\t\t 0:Run with Ping Server (Default Mode if no mode is pecified)\n"
                        + "\t\t\t\t 1:Run Without Ping Server (Private Mode)\n"
                        + "\n\t\t--benchmark:\n"
                        + "\t\t\tRun Benchmarks on resources before booting up node\n"
                        + "\t\t\t\t 0:\n"
                        + "");
                return;
            }

            if (arguments.contains("--benchmark")) {
                benchmark();
            } else {
                preBenchmarkingChecks();
            }

            if (arguments.contains("--mode")) {
                int mode = 0;
                try {
                    mode = Integer.parseInt(arguments.get(arguments.indexOf("--mode") + 1));
                } catch (NumberFormatException e) {
                    System.err.println("Use number to specify mode"
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
                        Thread server2 = new Thread(new Server(true));
                        server2.start();
                        Thread downloadQueueServer2 = new Thread(new FileReqQueServer(true));
                        downloadQueueServer2.start();
                        break;
                }
            } else {
                preBenchmarkingChecks();
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
        Util.write(new File(dir_appdb+"/benchmarks.json"), benchmarkResults.toString(4));

    }

    public static void preBenchmarkingChecks() {
        if (new File(dir_appdb+"/benchmarks.json").exists()) {
            JSONObject benchmarkResults = new JSONObject(Util.readFile(dir_appdb+"/benchmarks.json"));
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
