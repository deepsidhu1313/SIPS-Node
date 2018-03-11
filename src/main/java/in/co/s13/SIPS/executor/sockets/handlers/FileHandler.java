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
package in.co.s13.SIPS.executor.sockets.handlers;

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class FileHandler implements Runnable {

    Socket submitter;
    int pnum;
    String simsql = "";
    long pdelay = 10;
    private FileInputStream fis;
    private BufferedInputStream bis;
    private String FILE_TO_SEND;

    public FileHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {
        try {
            try (DataInputStream dIn = new DataInputStream(submitter.getInputStream()); OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                //        BufferedOutputStream bos = new BufferedOutputStream(submitter.getOutputStream());

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
//                      Util.outPrintln("IP adress of sender is " + ipAddress);

                    Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Accepted Request " + msg + " from " + ipAddress);

                    String command = msg.getString("Command");//substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                    JSONObject body = msg.getJSONObject("Body");//.substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                    Util.outPrintln(msg.toString());
                    if (command.trim().equalsIgnoreCase("sendfile")) {
                        Util.outPrintln("finding file");
                        String fileToSend = body.getString("FILE");//substring(body.indexOf("<FILE>") + 6, body.indexOf("</FILE>"));
                        String pid = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String projectName = body.getString("PROJECT");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String nodeUUID = body.getString("UUID");
//                        System.out.println("Accepted connection : " + submitter);
                        // send file
                        File myFile = new File("data/" + pid  + "/" + fileToSend);

                        if (myFile.getAbsolutePath().trim().contains("data/" + pid) && myFile.exists()) {
                            String sendmsg = "foundfile";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);

                            outToClient.write(bytes);

                            File fsha = new File(myFile.getAbsolutePath().trim() + ".sha");
                            if (fsha.exists()) {
                                sendmsg = "" + Util.LoadCheckSum(myFile.getAbsolutePath().trim() + ".sha");
                            } else {
                                sendmsg = "" + Util.getCheckSum(myFile.getAbsolutePath().trim());
                            }
                            if (sendmsg.trim().length() < 1) {
                                sendmsg = "" + Util.getCheckSum(myFile.getAbsolutePath().trim());
                            }
//                            System.out.println("Sending CheckSUm" + sendmsg);
                            bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                            length = dIn.readInt();                    // read length of incoming message
                            message = new byte[length];

                            if (length > 0) {
                                dIn.readFully(message, 0, message.length); // read the message
                            }
                            s = new String(message);
                            msg = new JSONObject(s);
                            if (msg.getString("REPLY").trim().equalsIgnoreCase("foundLocal")) {

                            } else if (msg.getString("REPLY").trim().equalsIgnoreCase("sendNew")) {
                                long flength = myFile.length();
                                outToClient.writeLong(flength);

                                // byte[] mybytearray = new byte[(int) myFile.length()];
                                fis = new FileInputStream(myFile);
                                bis = new BufferedInputStream(fis);
                                int theByte = 0;
                                Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + fileToSend + "(" + myFile.length() + " bytes)");
                                /* while ((theByte = bis.read()) != -1) {
                                outToClient.write(theByte);
                                // bos.flush();
                                }*/

                                int count;
                                byte[] mybytearray = new byte[16 * 1024];
                                BufferedOutputStream bos = new BufferedOutputStream(os);
                                while ((count = bis.read(mybytearray)) > -1) {
                                    bos.write(mybytearray, 0, count);
                                }
                                bos.flush();
                                bos.close();
                                bis.close();
                                fis.close();
                            }

                        }
                    } else if (command.trim().equalsIgnoreCase("resolveObject")) {
//                        System.out.println("finding Object");
                        String objToSend = body.getString("OBJECT");//substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                        String pid2 = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno2 = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String classname = body.getString("CLASSNAME");//substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                        int instance = body.getInt("INSTANCE");//substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                        String projectName = body.getString("PROJECT");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String nodeUUID = body.getString("UUID");
//                        System.out.println("Accepted connection : " + submitter);
                        // send file
                        File myFile2 = new File("data/" + pid2 +  "/.simulated/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");

                        if (myFile2.getAbsolutePath().trim().contains("data/" + pid2) && myFile2.exists()) {
                            String sendmsg = "foundobj";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                            File fsha = new File(myFile2.getAbsolutePath().trim() + ".sha");
                            if (fsha.exists()) {
                                sendmsg = "" + Util.LoadCheckSum(myFile2.getAbsolutePath().trim() + ".sha");
                            } else {
                                sendmsg = "" + Util.getCheckSum(myFile2.getAbsolutePath().trim());
                            }
                            if (sendmsg.trim().length() < 1) {
                                sendmsg = "" + Util.getCheckSum(myFile2.getAbsolutePath().trim());
                            }

                            bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                            //msg = "";
                            length = dIn.readInt();                    // read length of incoming message
                            message = new byte[length];

                            if (length > 0) {
                                dIn.readFully(message, 0, message.length); // read the message
                            }
                            s = new String(message);
                            msg = new JSONObject(s);
                            if (msg.getString("REPLY").trim().equalsIgnoreCase("foundLocal")) {

                            } else if (msg.getString("REPLY").trim().equalsIgnoreCase("sendNew")) {

                                long flength = myFile2.length();
                                outToClient.writeLong(flength);

                                // byte[] mybytearray = new byte[(int) myFile.length()];
                                fis = new FileInputStream(myFile2);
                                bis = new BufferedInputStream(fis);
                                int theByte = 0;
                                Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + objToSend + " (" + myFile2.length() + " bytes)");
                                /* while ((theByte = bis.read()) != -1) {
                                outToClient.write(theByte);
                                // bos.flush();
                                }*/

                                int count;
                                byte[] mybytearray = new byte[16 * 1024];
                                try (BufferedOutputStream bos = new BufferedOutputStream(os)) {
                                    while ((count = bis.read(mybytearray)) > -1) {
                                        bos.write(mybytearray, 0, count);
                                    }
                                    bos.flush();
                                }
                                bis.close();
                                fis.close();
                            }
                        } else {
                            String sendmsg = "error";
                            System.out.println(myFile2.getAbsolutePath() + " is not present");
                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "Sending " + sendmsg + " to " + ipAddress);

                        }

                    } else if (command.trim().equalsIgnoreCase("resolveObjectChecksum")) {
                        String objToSend = body.getString("OBJECT");//substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                        String pid2 = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno2 = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String classname = body.getString("CLASSNAME");//substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                        int instance = body.getInt("INSTANCE");//substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                        String projectName = body.getString("PROJECT");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));

                        // send file
                        File myFile2 = new File("data/" + pid2  + "/.simulated/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");

                        if (myFile2.getAbsolutePath().trim().contains("data/" + pid2) && myFile2.exists()) {
                            String sendmsg = "foundobj";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                            File fsha = new File(myFile2.getAbsolutePath().trim() + ".sha");
                            if (fsha.exists()) {
                                sendmsg = "" + Util.LoadCheckSum(myFile2.getAbsolutePath().trim() + ".sha");
                            } else {
                                sendmsg = "" + Util.getCheckSum(myFile2.getAbsolutePath().trim());
                            }
                            if (sendmsg.trim().length() < 1) {
                                sendmsg = "" + Util.getCheckSum(myFile2.getAbsolutePath().trim());
                            }

                            bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            os.close();
                            outToClient.close();
                            submitter.close();
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                        } else {
                            String sendmsg = "error";
                            System.out.println(myFile2.getAbsolutePath() + " is not present");
                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            os.close();
                            outToClient.close();
                            submitter.close();
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "Sending " + sendmsg + " to " + ipAddress);

                        }

                    } else if (command.trim().equalsIgnoreCase("sendfileChecksum")) {
                        Util.outPrintln("finding file");
                        String fileToSend = body.getString("FILE");//substring(body.indexOf("<FILE>") + 6, body.indexOf("</FILE>"));
                        String pid = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String projectName = body.getString("PROJECT");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));

                        System.out.println("Accepted connection : " + submitter);
                        // send file
                        File myFile = new File("data/" + pid + "/" +  fileToSend);

                        if (myFile.getAbsolutePath().trim().contains("data/" + pid) && myFile.exists()) {
                            String sendmsg = "foundfile";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);

                            outToClient.write(bytes);

                            File fsha = new File(myFile.getAbsolutePath().trim() + ".sha");
                            if (fsha.exists()) {
                                sendmsg = "" + Util.LoadCheckSum(myFile.getAbsolutePath().trim() + ".sha");
                            } else {
                                sendmsg = "" + Util.getCheckSum(myFile.getAbsolutePath().trim());
                            }
                            if (sendmsg.trim().length() < 1) {
                                sendmsg = "" + Util.getCheckSum(myFile.getAbsolutePath().trim());
                            }
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                            bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        } else {

                            String sendmsg = "FileNotFound";

                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);

                            outToClient.write(bytes);
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "Sending " + sendmsg + " to " + ipAddress);
                            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "File doesnot exist:"+myFile.getAbsolutePath());

                        }
                    }

                }
            }

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, ex.toString());

        } catch (IOException ex) {
            Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
            Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, ex.toString());
        }
        {
            try {
                if (submitter != null && !submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
