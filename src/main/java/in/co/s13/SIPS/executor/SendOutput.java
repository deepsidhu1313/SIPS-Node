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
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.settings.GlobalValues;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class SendOutput implements Runnable {

    String ipadd = "", ID = "", outPut = "", filename = "", cno = "";

    public SendOutput(String ip, String id, String chunknumber, String fname, String output) {
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
                s.connect(new InetSocketAddress(ipadd, GlobalValues.JOB_SERVER_PORT));
                try (OutputStream os = s.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(os); DataInputStream dIn = new DataInputStream(s.getInputStream())) {
                    JSONObject sendmsgJsonObj = new JSONObject();
                    sendmsgJsonObj.put("Command", "printoutput");
                    JSONObject sendmsgBodyJsonObj = new JSONObject();
                    sendmsgBodyJsonObj.put("PID", ID);
                    sendmsgBodyJsonObj.put("UUID", in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID);
                    sendmsgBodyJsonObj.put("CNO", cno);
                    sendmsgBodyJsonObj.put("FILENAME", filename);
                    sendmsgBodyJsonObj.put("OUTPUT", outPut);
                    sendmsgJsonObj.put("Body", sendmsgBodyJsonObj);

                    String sendmsg = sendmsgJsonObj.toString();//"<Command>printoutput</Command><Body><PID>" + ID + "</PID><CNO>" + cno + "</CNO><FILENAME>" + filename + "</FILENAME><OUTPUT>" + outPut + "</OUTPUT></Body>";
                    byte[] bytes = sendmsg.getBytes("UTF-8");
                    outToServer.writeInt(bytes.length);
                    outToServer.write(bytes);

                    int length = dIn.readInt();                    // read length of incoming message
                    byte[] message = new byte[length];

                    if (length > 0) {
                        dIn.readFully(message, 0, message.length); // read the message
                    }
//                    String reply = new String(message);
//                    if (reply.contains("OK")) {
//                    } else {
//                    }

                }

            }
        } catch (IOException ex) {
            Logger.getLogger(SendOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
