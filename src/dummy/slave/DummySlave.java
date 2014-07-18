/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dummy.slave;

import controlpanel.settings;
import executor.Server;
import java.io.IOException;

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
        Thread server= new Thread(new Server(true));
        server.start();
        // TODO code application logic here
    }
    
}
