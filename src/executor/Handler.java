/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import controlpanel.settings;
import static controlpanel.settings.procDB;
import static controlpanel.settings.processDBExecutor;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nika
 */
public class Handler implements Runnable {

    Socket submitter;
   
    public Handler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {
        try {
            String msg;
            try (DataInputStream dIn = new DataInputStream(submitter.getInputStream())) {
                msg = "";
                int length = dIn.readInt();                    // read length of incoming message
                byte[] message = new byte[length];
                if (length > 0) {
                    dIn.readFully(message, 0, message.length); // read the message
                }   String s = new String(message);
                msg = "" + s;
            
            InetAddress inetAddress = submitter.getInetAddress();
            String ipAddress = inetAddress.getHostAddress();
            if (msg.length() > 1) {
                System.out.println("IP adress of sender is " + ipAddress);

                System.out.println("" + msg);

                String command = msg.substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                String body = msg.substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                System.out.println(msg);
                if (command.contains("createprocess")) {
                    settings.PROCESS_WAITING++;
                    settings.processExecutor.execute(new ParallelProcess(body, ipAddress));
                    System.out.println("created process");

                    try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                        String sendmsg = "OK";

                        byte[] bytes = sendmsg.getBytes("UTF8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);

                    }
                    submitter.close();
                } else if (command.contains("ping")) {

                    try (OutputStream os2 = submitter.getOutputStream(); DataOutputStream outToClient2 = new DataOutputStream(os2)) {
                        String sendmsg2 = "<OS>" + controlpanel.settings.OS + "</OS><HOSTNAME>" + controlpanel.settings.HOST_NAME + "</HOSTNAME><PLIMIT>" + controlpanel.settings.PROCESS_LIMIT
                                + "</PLIMIT><PWAIT>" + controlpanel.settings.PROCESS_WAITING + "</PWAIT><TMEM>" + controlpanel.settings.MEM_SIZE + "</TMEM><CPULOAD>" + controlpanel.settings.getCPULoad() + "</CPULOAD><CPUNAME>" + controlpanel.settings.CPU_NAME + "</CPUNAME>";

                        byte[] bytes2 = sendmsg2.getBytes("UTF8");
                        outToClient2.writeInt(bytes2.length);
                        outToClient2.write(bytes2);
                    }
                    submitter.close();

                } else if (command.contains("kill")) {
                    String pid = body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));

                        if (Server.alienprocessID.contains("" + ipAddress + "-ID-" + pid + "c" + cno)) {

                            processDBExecutor.execute(() -> {
                                try {
                                    String sql = "SELECT * FROM PROC WHERE  ALIENID = '" + pid + "' AND CNO ='" + cno + "' AND IP ='" + ipAddress + "';";
                                    
                                    ResultSet rs = procDB.select("appdb/proc.db", sql);
                                    int n = 9999;
                                    while (rs.next()) {
                                        n = rs.getInt("ID");
                                    }
                                    procDB.closeConnection();
                                    
                                    if (Server.p[n].isAlive()) {
                                        Server.p[n].destroy();
                                    }
                                } catch (SQLException ex) {
                                    Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });

                        }
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
         
                }}
            }
        } catch (IOException ex) {
            Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (!submitter.isClosed() && submitter != null) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
