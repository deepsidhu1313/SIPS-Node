/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor.sockets.handlers;

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.SIPS.virtualdb.UpdateDistDBaftExecVirtual;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class TaskFinishListenerHandler implements Runnable {

    Socket submitter;
    int pnum;
    String simsql = "";
    long pdelay = 10;

    public TaskFinishListenerHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {

        try {
            try (DataInputStream dIn = new DataInputStream(submitter.getInputStream()); OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                int length = dIn.readInt();                    // read length of incoming message
                byte[] message = new byte[length];

                if (length > 0) {
                    dIn.readFully(message, 0, message.length); // read the message
                }
                String s = new String(message);
                JSONObject msg = new JSONObject(s);

                InetAddress inetAddress = submitter.getInetAddress();
                String ipAddress = inetAddress.getHostAddress();
                if (msg.length() > 1) {
                    //Settings.outPrintln("hurray cond 1");
                    Util.outPrintln("IP adress of sender is " + ipAddress);

                    Util.outPrintln("" + msg);

                    String command = msg.getString("Command");//substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                    JSONObject body = msg.getJSONObject("Body");;//substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                    //     Settings.outPrintln(msg);
                    if (command.equalsIgnoreCase("Finished")) {
                        String pid = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String content = body.getString("OUTPUT");//substring(body.indexOf("<OUTPUT>") + 8, body.indexOf("</OUTPUT>"));
                        String ExitCode = body.getString("EXTCODE");//substring(body.indexOf("<EXTCODE>") + 9, body.indexOf("</EXTCODE>"));

                        int p = Integer.parseInt(pid);
                        {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        System.out.println("size of master dist db " + MASTER_DIST_DB.size());
                        Thread t = new Thread(new UpdateDistDBaftExecVirtual(System.currentTimeMillis(), Long.parseLong(content), fname, ipAddress, pid, cno, ExitCode));
                        GlobalValues.DIST_DB_EXECUTOR.execute(t);
                        submitter.close();
                    } else if (command.contains("Error")) {
                        String pid = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String content = body.getString("OUTPUT");//substring(body.indexOf("<OUTPUT>") + 8, body.indexOf("</OUTPUT>"));
                        String ExitCode = body.getString("EXTCODE");//substring(body.indexOf("<EXTCODE>") + 9, body.indexOf("</EXTCODE>"));
                        int p = Integer.parseInt(pid);
                        {
                            String sendmsg = "OK";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                        }
                        submitter.close();
                        Thread t = new Thread(new UpdateDistDBaftExecVirtual(System.currentTimeMillis(), Long.parseLong(content), fname, ipAddress, pid, cno, ExitCode));
                        GlobalValues.DIST_DB_EXECUTOR.execute(t);
                    } else {
                        submitter.close();

                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TaskFinishListenerHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (!submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(TaskFinishListenerHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
