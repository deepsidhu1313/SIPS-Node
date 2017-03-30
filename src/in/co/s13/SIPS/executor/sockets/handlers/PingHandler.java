/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor.sockets.handlers;

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.settings.Settings;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class PingHandler implements Runnable {

    Socket submitter;
    int pnum;
    String simsql = "";
    long pdelay = 10;

    public PingHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {
      //  boolean pingThread = false;
        try {
            try (DataInputStream dataInputStream = new DataInputStream(submitter.getInputStream())) {
                String msg ;
                int length = dataInputStream.readInt();                    // read length of incoming message
                byte[] message = new byte[length];

                if (length > 0) {
                    dataInputStream.readFully(message, 0, message.length); // read the message
                }
                msg =  new String(message);

                InetAddress inetAddress = submitter.getInetAddress();
                String ipAddress = inetAddress.getHostAddress();
                if (msg.length() > 1) {
                    //System.out.println("hurray cond 1");
                    System.out.println("IP adress of sender is " + ipAddress);

                   // System.out.println("" + msg);

                    String command = msg.substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                    String body = msg.substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                    //     System.out.println(msg);
//                    if (command.contains("createprocess")) {
//                        GlobalValues.PROCESS_WAITING++;
//                        GlobalValues.processExecutor.execute(new ParallelProcess(body, ipAddress));
//                        System.out.println("created process");
//
//                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
//                            String sendmsg = "OK";
//                            byte[] bytes = sendmsg.getBytes("UTF-8");
//                            outToClient.writeInt(bytes.length);
//                            outToClient.write(bytes);
//                        }
//
//                        submitter.close();
//                    } else
                        if (command.contains("ping")) {

                        try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
                            JSONObject sendmsg2Json = new JSONObject();
                            sendmsg2Json.put("OS", GlobalValues.OS);
                            sendmsg2Json.put("HOSTNAME", GlobalValues.HOST_NAME);
                            sendmsg2Json.put("PLIMIT", GlobalValues.PROCESS_LIMIT);
                            sendmsg2Json.put("PWAIT", GlobalValues.PROCESS_WAITING);
                            sendmsg2Json.put("TMEM", GlobalValues.MEM_SIZE);
                            sendmsg2Json.put("CPULOAD", Settings.getCPULoad());
                            sendmsg2Json.put("CPUNAME", GlobalValues.CPU_NAME);
                            
                            String sendmsg2 = sendmsg2Json.toString();
                            byte[] bytes2 = sendmsg2.getBytes("UTF-8");
                            outToClient2.writeInt(bytes2.length);
                            outToClient2.write(bytes2);
                        }
                        System.out.println("Ping Recieved");
        //                pingThread = true;
                        submitter.close();
                    } else {
                        submitter.close();

                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(PingHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (!submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(PingHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        
    }

}
