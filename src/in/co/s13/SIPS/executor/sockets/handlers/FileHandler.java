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
                    //settings.outPrintln("hurray cond 1");
                    Util.outPrintln("IP adress of sender is " + ipAddress);
                    
                    Util.outPrintln("" + msg);
                    
                    String command = msg.getString("Command");//substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                    JSONObject body = msg.getJSONObject("Body");//.substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                    Util.outPrintln(msg.toString());
                    if (command.trim().equalsIgnoreCase("sendfile")) {
                        Util.outPrintln("finding file");
                        String fileToSend = body.getString("FILE");//substring(body.indexOf("<FILE>") + 6, body.indexOf("</FILE>"));
                        String pid = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        
                        System.out.println("Accepted connection : " + submitter);
                        // send file
                        File myFile = new File("data/" + pid + "/" + fileToSend);
                        
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
                            System.out.println("Sending CheckSUm" + sendmsg);
                            bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            
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
                                System.out.println("Sending " + fileToSend + "(" + myFile.length() + " bytes)");
                                /* while ((theByte = bis.read()) != -1) {
                                outToClient.write(theByte);
                                // bos.flush();
                                }*/
                                
                                int count;
                                byte[] mybytearray = new byte[16*1024];
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
                        System.out.println("finding Object");
                        String objToSend = body.getString("OBJECT");//substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                        String pid2 = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno2 = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String classname = body.getString("CLASSNAME");//substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                        String instance = body.getString("INSTANCE");//substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                        
                        System.out.println("Accepted connection : " + submitter);
                        // send file
                        File myFile2 = new File("data/" + pid2 + "/sim/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");
                        
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
                                System.out.println("Sending " + objToSend + "(" + myFile2.length() + " bytes)");
                                /* while ((theByte = bis.read()) != -1) {
                                outToClient.write(theByte);
                                // bos.flush();
                                }*/
                                
                                int count;
                                byte[] mybytearray = new byte[16*1024];
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
                        }
                        
                        System.out.println("Done.");
                    } else if (command.trim().equalsIgnoreCase("resolveObjectChecksum")) {
                        System.out.println("finding Object");
                        String objToSend = body.getString("OBJECT");//substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                        String pid2 = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno2 = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String classname = body.getString("CLASSNAME");//substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                        String instance = body.getString("INSTANCE");//substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                        
                        System.out.println("Accepted connection : " + submitter);
                        // send file
                        File myFile2 = new File("data/" + pid2 + "/sim/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");
                        
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
                            
                        } else {
                            String sendmsg = "error";
                            System.out.println(myFile2.getAbsolutePath() + " is not present");
                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            os.close();
                            outToClient.close();
                            submitter.close();
                            
                        }
                        
                        System.out.println("Done.");
                    } else if (command.trim().equalsIgnoreCase("sendfileChecksum")) {
                        Util.outPrintln("finding file");
                        String fileToSend = body.getString("FILE");//substring(body.indexOf("<FILE>") + 6, body.indexOf("</FILE>"));
                        String pid = body.getString("PID");//substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String cno = body.getString("CNO");//substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                        String fname = body.getString("FILENAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                          
                        System.out.println("Accepted connection : " + submitter);
                        // send file
                        File myFile = new File("data/" + pid + "/" + fileToSend);
                        
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
                            
                            bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            
                        }
                    }
                    
                }
            }
            
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        {
            try {
                submitter.close();
            } catch (IOException ex) {
                Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
