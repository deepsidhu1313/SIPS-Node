/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
     */
    public static void main(String[] args) throws IOException {
        new Settings();
        if (args.length > 0) {
            ArrayList<String> arguments = new ArrayList<>();
            Collections.addAll(arguments, args);
            if (arguments.contains("--mode")) {
                int mode = Integer.parseInt(arguments.get(arguments.indexOf("--mode") + 1));
                switch (mode) {
                    case 0:
                        Thread pserver = new Thread(new PingServer(true,0));
                        pserver.start();
                        break;
                    case 1:
                        Thread server = new Thread(new Server(true));
                        server.start();
                        Thread dqserver = new Thread(new FileReqQueServer(true));
                        dqserver.start();

                }
            }

        } else {
            Thread server = new Thread(new Server(true));
            server.start();
            Thread pserver = new Thread(new PingServer(true,3));
            pserver.start();
            Thread dqserver = new Thread(new FileReqQueServer(true));
            dqserver.start();
        }
    }

}
