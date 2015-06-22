/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class sendOutput implements Runnable {

    String ipadd = "", ID = "", outPut = "", filename = "", cno = "";

    public sendOutput(String ip, String id, String chunknumber, String fname, String output) {
        ipadd = ip;
        ID = id;
        outPut = output;
        filename = fname;
        cno = chunknumber;
    }

    @Override
    public void run() {
        try {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(ipadd, 13131));
                try (OutputStream os = s.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(os); DataInputStream dIn = new DataInputStream(s.getInputStream())) {
                    String sendmsg = "<Command>printoutput</Command><Body><PID>" + ID + "</PID><CNO>" + cno + "</CNO><FILENAME>" + filename + "</FILENAME><OUTPUT>" + outPut + "</OUTPUT></Body>";
                    byte[] bytes = sendmsg.getBytes("UTF-8");
                    outToServer.writeInt(bytes.length);
                    outToServer.write(bytes);

                    int length = dIn.readInt();                    // read length of incoming message
                    byte[] message = new byte[length];

                    if (length > 0) {
                        dIn.readFully(message, 0, message.length); // read the message
                    }
                    String reply = new String(message);
                    if (reply.contains("OK")) {
                    } else {
                    }

                }

            }
        } catch (IOException ex) {
            Logger.getLogger(sendOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
