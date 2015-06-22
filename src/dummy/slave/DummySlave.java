/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dummy.slave;

import controlpanel.settings;
import executor.FileReqQueServer;
import executor.PingServer;
import executor.Server;
import java.io.IOException;
import java.util.concurrent.Executors;

/**
 *
 * @author Nika
 */
public class DummySlave {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new settings();
        if (args.length > 0) {
            if ( (! (args[0].length()<=0))) {
                settings.PROCESS_LIMIT = Integer.parseInt(args[0]);
                settings.processExecutor = Executors.newFixedThreadPool(settings.PROCESS_LIMIT);
            } else {
            }
        }
        Thread server = new Thread(new Server(true));
        server.start();
        Thread pserver = new Thread(new PingServer(true));
        pserver.start();
        Thread dqserver = new Thread(new FileReqQueServer(true));
        dqserver.start();
        // TODO code application logic here
    }

}
