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
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class sendOverHead implements Runnable {

    String ipadd = "", ID = "", outPut = "", filename = "", value = "", cmd, chunkno, exitCode;

    public sendOverHead(String overheadName, String ip, String PID, String chunknumber, String Filename, String value, String ExitCode) {
        ipadd = ip;
        ID = PID;
        filename = Filename;
        this.value = value;
        cmd = overheadName;
        chunkno = chunknumber;
        exitCode = ExitCode;
    }

    @Override
    public void run() {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(ipadd, 13135));
            OutputStream os = s.getOutputStream();
            try (DataInputStream dIn = new DataInputStream(s.getInputStream()) //inFromServer.close();
                    ; DataOutputStream outToServer = new DataOutputStream(os)) {
                JSONObject sendmsgJsonObj = new JSONObject();
                sendmsgJsonObj.put("Command", cmd);
                JSONObject sendmsgBodyJsonObj = new JSONObject();
                sendmsgBodyJsonObj.put("PID", ID);
                sendmsgBodyJsonObj.put("CNO", chunkno);
                sendmsgBodyJsonObj.put("FILENAME", filename);
                sendmsgBodyJsonObj.put("OUTPUT", value);
                sendmsgBodyJsonObj.put("EXTCODE", exitCode);
                sendmsgJsonObj.put("BODY", sendmsgBodyJsonObj);

                String sendmsg = sendmsgJsonObj.toString();

//                        "<Command>" + cmd + "</Command>"
//                        + "<Body><PID>" + ID + "</PID>"
//                        + "<CNO>" + chunkno + "</CNO>"
//                        + "<FILENAME>" + filename + "</FILENAME>"
//                        + "<OUTPUT>" + value + "</OUTPUT>"
//                        + "<EXTCODE>" + exitCode + "</EXTCODE></Body>";
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
                s.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(sendOutput.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
