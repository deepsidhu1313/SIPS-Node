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
import java.io.FileOutputStream;
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
    private String FILE_TO_SEND;

    public FileHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {

        try (DataInputStream dataInputStream = new DataInputStream(submitter.getInputStream()); OutputStream outputStream = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(outputStream)) {
            //        BufferedOutputStream bos = new BufferedOutputStream(submitter.getOutputStream());

            int length = dataInputStream.readInt();                    // read length of incoming message
            byte[] message = new byte[length];

            if (length > 0) {
                dataInputStream.readFully(message, 0, message.length); // read the message
            }
            String s = new String(message);
            JSONObject msg = new JSONObject(s);

            InetAddress inetAddress = submitter.getInetAddress();
            String ipAddress = inetAddress.getHostAddress();
            if (msg.length() > 1) {
//                      Util.outPrintln("IP adress of sender is " + ipAddress);

                Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Accepted Request " + msg + " from " + ipAddress);

                String command = msg.getString("Command");
                JSONObject body = msg.getJSONObject("Body");
                Util.outPrintln(msg.toString());
                if (command.trim().equalsIgnoreCase("sendfile")) {
                    Util.outPrintln("finding file");
                    String filenameToSend = body.getString("FILE");
                    String pid = body.getString("PID");
                    String cno = body.getString("CNO");
                    String projectName = body.getString("PROJECT");
                    String nodeUUID = body.getString("UUID");
//                        System.out.println("Accepted connection : " + submitter);
                    // send file
                    File fileToSend = new File("data/" + pid + "/" + filenameToSend);

                    if (fileToSend.getAbsolutePath().trim().contains("data/" + pid) && fileToSend.exists()) {
                        String sendmsg = "foundfile";

                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);

                        outToClient.write(bytes);

                        File fsha = new File(fileToSend.getAbsolutePath().trim() + ".sha");
                        if (fsha.exists()) {
                            sendmsg = "" + Util.LoadCheckSum(fileToSend.getAbsolutePath().trim() + ".sha");
                        } else {
                            sendmsg = "" + Util.getCheckSum(fileToSend.getAbsolutePath().trim());
                        }
                        if (sendmsg.trim().length() < 1) {
                            sendmsg = "" + Util.getCheckSum(fileToSend.getAbsolutePath().trim());
                        }
//                            System.out.println("Sending CheckSUm" + sendmsg);
                        bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);
                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                        length = dataInputStream.readInt();                    // read length of incoming message
                        message = new byte[length];

                        if (length > 0) {
                            dataInputStream.readFully(message, 0, message.length); // read the message
                        }
                        s = new String(message);
                        msg = new JSONObject(s);
                        if (msg.getString("REPLY").trim().equalsIgnoreCase("foundLocal")) {

                        } else if (msg.getString("REPLY").trim().equalsIgnoreCase("sendNew")) {
                            long flength = fileToSend.length();
                            outToClient.writeLong(flength);

                            try ( // byte[] mybytearray = new byte[(int) myFile.length()];
                                    FileInputStream fis = new FileInputStream(fileToSend); BufferedInputStream bis = new BufferedInputStream(fis)) {
                                int theByte = 0;
                                Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + filenameToSend + "(" + fileToSend.length() + " bytes)");
                                /* while ((theByte = bis.read()) != -1) {
                                    outToClient.write(theByte);
                                    // bos.flush();
                                    }*/
                                int count;
                                byte[] mybytearray = new byte[1024];
                                long start = System.currentTimeMillis();
                                try (BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
                                    while ((count = bis.read(mybytearray)) > -1) {
                                        bos.write(mybytearray, 0, count);
                                    }
                                    bos.flush();
                                }

                                long end = System.currentTimeMillis();

                            }
                        }

                    }
                } else if (command.trim().equalsIgnoreCase("resolveObject")) {
//                        System.out.println("finding Object");
                    String objToSend = body.getString("OBJECT");
                    String pid2 = body.getString("PID");
                    String cno2 = body.getString("CNO");
                    String classname = body.getString("CLASSNAME");
                    int instance = body.getInt("INSTANCE");
                    String projectName = body.getString("PROJECT");
                    String nodeUUID = body.getString("UUID");
//                        System.out.println("Accepted connection : " + submitter);
                    // send file
                    File myFile2 = new File("data/" + pid2 + "/.simulated/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");

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
                        length = dataInputStream.readInt();                    // read length of incoming message
                        message = new byte[length];

                        if (length > 0) {
                            dataInputStream.readFully(message, 0, message.length); // read the message
                        }
                        s = new String(message);
                        msg = new JSONObject(s);
                        if (msg.getString("REPLY").trim().equalsIgnoreCase("foundLocal")) {

                        } else if (msg.getString("REPLY").trim().equalsIgnoreCase("sendNew")) {

                            long flength = myFile2.length();
                            outToClient.writeLong(flength);

                            try (FileInputStream fis = new FileInputStream(myFile2); BufferedInputStream bis = new BufferedInputStream(fis)) {
                                Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + objToSend + " (" + myFile2.length() + " bytes)");

                                int count;
                                byte[] mybytearray = new byte[1024];

                                try (BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
                                    while ((count = bis.read(mybytearray)) > -1) {
                                        bos.write(mybytearray, 0, count);
                                    }
                                    bos.flush();
                                }

                            }
                        }
                    } else {
                        String sendmsg = "error";
                        System.out.println(myFile2.getAbsolutePath() + " is not present");
                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);
                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "Sending " + sendmsg + " to " + ipAddress);

                    }

                } else if (command.trim().equalsIgnoreCase("resolveResult")) {
//                        System.out.println("finding Object");
                    String objToSend = body.getString("OBJECT");
                    String pid2 = body.getString("PID");
                    String cno2 = body.getString("CNO");
                    String classname = body.getString("CLASSNAME");
                    int instance = body.getInt("INSTANCE");
                    String projectName = body.getString("PROJECT");
                    String nodeUUID = body.getString("UUID");
//                        System.out.println("Accepted connection : " + submitter);
                    // send file
                    File myFile2 = new File("data/" + pid2 + "/.result/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");

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
                        length = dataInputStream.readInt();                    // read length of incoming message
                        message = new byte[length];

                        if (length > 0) {
                            dataInputStream.readFully(message, 0, message.length); // read the message
                        }
                        s = new String(message);
                        msg = new JSONObject(s);
                        if (msg.getString("REPLY").trim().equalsIgnoreCase("foundLocal")) {

                        } else if (msg.getString("REPLY").trim().equalsIgnoreCase("sendNew")) {
                            long flength = myFile2.length();
                            outToClient.writeLong(flength);
                            try (FileInputStream fis = new FileInputStream(myFile2); BufferedInputStream bis = new BufferedInputStream(fis); BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
                                Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + objToSend + " (" + myFile2.length() + " bytes)");
                                int count;
                                byte[] mybytearray = new byte[1024];
                                {
                                    while ((count = bis.read(mybytearray)) > -1) {
//                                        System.out.println("Sending " + count + " bytes");
                                        bos.write(mybytearray, 0, count);

                                    }
                                    bos.flush();
                                }
                            }
                        }
                    } else {
                        String sendmsg = "error";
                        System.out.println(myFile2.getAbsolutePath() + " is not present");
                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);
                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "Sending " + sendmsg + " to " + ipAddress);

                    }

                } else if (command.trim().equalsIgnoreCase("UPLOAD_RESULT")) {
                    String resultToReceive = body.getString("OBJECT");
                    String pid2 = body.getString("PID");
                    String cno2 = body.getString("CNO");
                    String classname = body.getString("CLASSNAME");
                    int instance = body.getInt("INSTANCE");
                    String projectName = body.getString("PROJECT");
                    String nodeUUID = body.getString("UUID");

                    File fileToSave = new File("data/" + pid2 + "/.result/" + classname + "/" + resultToReceive + "-instance-" + instance + ".obj");
                    fileToSave.getParentFile().mkdirs();
                    if (fileToSave.getAbsolutePath().trim().contains("data/" + pid2)) {
                        long fileLen, downData;
                        File tmpFile = new File(fileToSave.getAbsolutePath() + ".tmp");
                        try (FileOutputStream fos = new FileOutputStream(tmpFile); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                            fileLen = dataInputStream.readLong();

                            downData = fileLen;
                            int n = 0;
                            byte[] buf = new byte[1024];
                            while (fileLen > 0 && ((n = dataInputStream.read(buf, 0, (int) Math.min(buf.length, fileLen))) != -1)) {
                                bos.write(buf, 0, n);
                                fileLen -= n;
                            }
                            bos.flush();
                        }
                        tmpFile.renameTo(fileToSave);
                    }

                } else if (command.trim().equalsIgnoreCase("resolveObjectChecksum")) {
                    String objToSend = body.getString("OBJECT");
                    String pid2 = body.getString("PID");
                    String cno2 = body.getString("CNO");
                    String classname = body.getString("CLASSNAME");
                    int instance = body.getInt("INSTANCE");
                    String projectName = body.getString("PROJECT");

                    // send file
                    File myFile2 = new File("data/" + pid2 + "/.simulated/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");

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

                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                    } else {
                        String sendmsg = "error";
                        System.out.println(myFile2.getAbsolutePath() + " is not present");
                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);

                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "Sending " + sendmsg + " to " + ipAddress);

                    }

                } else if (command.trim().equalsIgnoreCase("resolveResultChecksum")) {
                    String objToSend = body.getString("OBJECT");
                    String pid2 = body.getString("PID");
                    String cno2 = body.getString("CNO");
                    String classname = body.getString("CLASSNAME");
                    int instance = body.getInt("INSTANCE");
                    String projectName = body.getString("PROJECT");

                    // send file
                    File myFile2 = new File("data/" + pid2 + "/.result/" + classname + "/" + objToSend + "-instance-" + instance + ".obj");

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

                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + sendmsg + " to " + ipAddress);

                    } else {
                        System.out.println(myFile2.getAbsolutePath() + " is not present");
                        JSONObject jsonReply = new JSONObject();
                        jsonReply.put("reply", "error");
                        jsonReply.put("message", "404 result not found");
                        String sendmsg = "" + jsonReply.toString();

                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);

                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "Sending " + sendmsg + " to " + ipAddress);

                    }

                } else if (command.trim().equalsIgnoreCase("sendfileChecksum")) {
                    Util.outPrintln("finding file");
                    String fileToSend = body.getString("FILE");
                    String pid = body.getString("PID");
                    String cno = body.getString("CNO");
                    String projectName = body.getString("PROJECT");

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
                        Util.appendToFileServerLog(GlobalValues.LOG_LEVEL.ERROR, "File doesnot exist:" + myFile.getAbsolutePath());

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

        try {
            if (submitter != null && !submitter.isClosed()) {
                submitter.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(FileHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
