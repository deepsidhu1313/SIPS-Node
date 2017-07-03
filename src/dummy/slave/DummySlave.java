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

import in.co.s13.SIPS.settings.Settings;
import in.co.s13.SIPS.executor.sockets.FileReqQueServer;
import in.co.s13.SIPS.executor.sockets.PingServer;
import in.co.s13.SIPS.executor.sockets.Server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
        new Settings();
        if (args.length > 0) {
            ArrayList<String> arguments = new ArrayList<>();
            Collections.addAll(arguments, args);
            if (arguments.contains("-h")||arguments.contains("--help")) {
                System.out.println("Usage:\n"
                        + "\t java -jar SIPS-Node.jar <options>\n"
                        + "\noptions:"
                        + "\t\t-h or --help: to show help menu\n"
                        + "\t\t--mode <MODE>: specify mode in which SIPS node supposed to run\n"
                        + "\t\t\t MODE:\n"
                        + "\t\t\t\t 0:Run with Ping Server (Default Mode if no mode is pecified)\n"
                        + "\t\t\t\t 1:Run Without Ping Server (Private Mode)\n"
                        + "\t\t\t\t 0:\n");
            }else if (arguments.contains("--mode")) {
                int mode = Integer.parseInt(arguments.get(arguments.indexOf("--mode") + 1));
                switch (mode) {
                    /***
                     * Default mode
                     */
                    case 0:default:
                        Thread pingServer = new Thread(new PingServer(true,0));
                        pingServer.start();
                        Thread server = new Thread(new Server(true));
                        server.start();
                        Thread downloadQueueServer = new Thread(new FileReqQueServer(true));
                        downloadQueueServer.start();
                        break;
                        /***
                         * Private Mode
                         */
                    case 1:
                        Thread server2 = new Thread(new Server(true));
                        server2.start();
                        Thread downloadQueueServer2 = new Thread(new FileReqQueServer(true));
                        downloadQueueServer2.start();
                        break;
                }
            }

        } else {
            Thread server = new Thread(new Server(true));
            server.start();
            Thread pingServer = new Thread(new PingServer(true,3));
            pingServer.start();
            Thread downloadQueServer = new Thread(new FileReqQueServer(true));
            downloadQueServer.start();
        }
    }

}
