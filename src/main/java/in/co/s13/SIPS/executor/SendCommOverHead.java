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
public class SendCommOverHead implements Runnable {

    String ipadd = "", ID = "", outPut = "", filename = "", value = "", cmd, chunkno;

    Socket s;

    public SendCommOverHead(String overheadName, String ip, String PID, String chunknumber, String Filename, String value) {
        ipadd = ip;
        ID = PID;
        filename = Filename;
        this.value = value;
        cmd = overheadName;
        chunkno = chunknumber;
    }

    @Override
    public void run() {
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(ipadd, GlobalValues.TASK_SERVER_PORT));
            try (OutputStream os = s.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(os)) {
                JSONObject sendmsgJsonObj = new JSONObject();
                sendmsgJsonObj.put("Command", cmd);
                JSONObject sendmsgBodyJsonObj = new JSONObject();
                sendmsgBodyJsonObj.put("PID", ID);
                sendmsgBodyJsonObj.put("UUID", in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID);
                sendmsgBodyJsonObj.put("CNO", chunkno);
                sendmsgBodyJsonObj.put("FILENAME", filename);
                sendmsgBodyJsonObj.put("OUTPUT", value);
                sendmsgJsonObj.put("Body", sendmsgBodyJsonObj);
                String sendmsg = sendmsgJsonObj.toString();
//                                                "<Command>" + cmd + "</Command>"
//                        + "<Body><PID>" + ID + "</PID>"
//                        + "<CNO>" + chunkno + "</CNO>"
//                        + "<FILENAME>" + filename + "</FILENAME>"
//                        + "<OUTPUT>" + value + "</OUTPUT>"
//                        + "</Body>";
                byte[] bytes = sendmsg.getBytes("UTF-8");
                outToServer.writeInt(bytes.length);
                outToServer.write(bytes);
                try (DataInputStream dIn = new DataInputStream(s.getInputStream())) {
                    int length = dIn.readInt();                    // read length of incoming message
                    byte[] message = new byte[length];

                    if (length > 0) {
                        dIn.readFully(message, 0, message.length); // read the message
                    }
//                    String reply = new String(message);
//                    if (reply.contains("OK")) {
//                    } else {
//                    }
                    s.close();

                } // read length of incoming message

                outToServer.close();
                //inFromServer.close();
            }

        } catch (IOException ex) {
            Logger.getLogger(SendOutput.class.getName()).log(Level.SEVERE, null, ex);
            try {
                s.close();
            } catch (IOException ex1) {
                Logger.getLogger(SendCommOverHead.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
