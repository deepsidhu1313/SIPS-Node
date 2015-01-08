/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class sendOverHead implements Runnable {

    String ipadd = "", ID = "", outPut = "", filename = "", value = "", cmd;
    Socket s;

    public sendOverHead(String overheadName, String ip, String PID, String Filename, String value) {
        ipadd = ip;
        ID = PID;
        filename = Filename;
        this.value = value;
        cmd = overheadName;
    }

    @Override
    public void run() {
        try {
             s = new Socket();
            s.connect(new InetSocketAddress(ipadd, 13131));
            OutputStream os = s.getOutputStream();
            DataOutputStream outToServer = new DataOutputStream(os);
            String sendmsg = "<Command>" + cmd + "</Command><Body><PID>" + ID + "</PID><FILENAME>" + filename + "</FILENAME><OUTPUT>" + value + "</OUTPUT><CPULOAD>" + controlpanel.settings.getCPULoad() + "</CPULOAD></Body>";
            byte[] bytes = sendmsg.getBytes("UTF8");
            outToServer.writeInt(bytes.length);
            outToServer.write(bytes);
            DataInputStream dIn = new DataInputStream(s.getInputStream());

            int length = dIn.readInt();                    // read length of incoming message
            byte[] message = new byte[length];

            if (length > 0) {
                dIn.readFully(message, 0, message.length); // read the message
            }
            String reply = new String(message);
            if (reply.contains("OK")) {
            } else {
            }
            s.close();
            outToServer.close();
            dIn.close();
            //inFromServer.close();
        } catch (IOException ex) {
            Logger.getLogger(sendOutput.class.getName()).log(Level.SEVERE, null, ex);
            try {
                s.close();
            } catch (IOException ex1) {
                Logger.getLogger(sendOverHead.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
