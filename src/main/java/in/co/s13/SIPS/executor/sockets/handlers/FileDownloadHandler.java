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

import in.co.s13.SIPS.datastructure.FileDownQueReq;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
public class FileDownloadHandler implements Runnable {

    Socket submitter;

    public FileDownloadHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {
        try {
            DataInputStream dataInputStream = new DataInputStream(submitter.getInputStream());
            OutputStream outputStream = submitter.getOutputStream();
            DataOutputStream outToClient = new DataOutputStream(outputStream);
            //        BufferedOutputStream bos = new BufferedOutputStream(submitter.getOutputStream());
            String msg;
            int length = dataInputStream.readInt();                    // read length of incoming message
            byte[] message = new byte[length];

            if (length > 0) {
                dataInputStream.readFully(message, 0, message.length); // read the message
            }
            msg = new String(message);
            InetAddress inetAddress = submitter.getInetAddress();
            String ipAddress = inetAddress.getHostAddress();
            Thread.currentThread().setName("File Request handler for " + ipAddress);
            if (msg.length() > 1) {

                JSONObject messageJson = new JSONObject(msg);

                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Accepted Request From " + ipAddress + " : " + msg);
                //System.out.println(messageJson.toString(4) + " has command :" + messageJson.has("Command"));

                String command = messageJson.getString("Command");//msg.substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                //String body = messageJson.getString("Body");//msg.substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                JSONObject body = messageJson.getJSONObject("Body");
                if (command.trim().equalsIgnoreCase("downloadfile")) {
                    System.out.println("finding file");
                    String fileToSend = body.getString("FILE");//body.substring(body.indexOf("<FILE>") + 6, body.indexOf("</FILE>"));
                    String pid = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                    String cno = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                    String projectName = body.getString("PROJECT");//body.substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                    String checksum = body.getString("CHECKSUM");//body.substring(body.indexOf("<CHECKSUM>") + 10, body.indexOf("</CHECKSUM>"));
                    String ip = body.getString("IP");//body.substring(body.indexOf("<IP>") + 4, body.indexOf("</IP>"));
                    String nodeUUID = body.getString("UUID");
                    boolean notinQ = true; //for
                    {
                        FileDownQueReq downQue = GlobalValues.DOWNLOAD_QUEUE.get(fileToSend.trim() + "-" + pid.trim() + "-" + checksum.trim() + "-" + ip.trim());
                        if (downQue != null) {
                            boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(fileToSend.trim());
                            boolean b2 = (downQue.getId() == null ? (pid.trim()) == null : downQue.getId().equals(pid.trim()));
                            boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
                            boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
                            if (((b1) && (b2) && (b3) && (b4))) {
                                notinQ = false;
                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "REQUEST Already IN QUE : " + downQue.toString());
                                if (downQue.getFinished()) {
                                    String sendmsg = new JSONObject().put("MSG", "finished").toString(4);//"<MSG>finished</MSG>";
                                    byte[] bytes = sendmsg.getBytes("UTF-8");
                                    outToClient.writeInt(bytes.length);
                                    outToClient.write(bytes);
                                    outputStream.close();
                                    outToClient.close();
                                    submitter.close();
                                    Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "REQUEST Already Finished : " + downQue.toString() + " sending Message " + sendmsg);
                                } else {
                                    long rt = downQue.getRemainingTime();
                                    JSONObject sobj = new JSONObject();
                                    sobj.put("MSG", "inque");
                                    sobj.put("RT", rt);
                                    String sendmsg = sobj.toString();//"<MSG>inque</MSG><RT>" + rt + "</RT>";
                                    byte[] bytes = sendmsg.getBytes("UTF-8");
                                    outToClient.writeInt(bytes.length);
                                    outToClient.write(bytes);
                                    outputStream.close();
                                    outToClient.close();
                                    submitter.close();
                                    Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "REQUEST Already IN QUE wait for " + rt + " : " + downQue.toString() + " sending Message " + sendmsg);
                                }
                            }
                        }
                    }
                    if (notinQ) {
                        GlobalValues.DOWNLOAD_QUEUE.put(fileToSend.trim() + "-" + pid.trim() + "-" + checksum.trim() + "-" + ip.trim(),
                                new FileDownQueReq(ip, (pid),
                                        checksum, fileToSend, System.currentTimeMillis(), 100, 0, 0, false, body.toString(), nodeUUID, projectName));
                        String sendmsg = new JSONObject().put("MSG", "addedinq").toString(4);//"<MSG>addedinq</MSG>";
                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);
                        outputStream.close();
                        outToClient.close();
                        submitter.close();
                        FileDownQueReq downQue = GlobalValues.DOWNLOAD_QUEUE.get(fileToSend.trim() + "-" + pid.trim() + "-" + checksum.trim() + "-" + ip.trim());
                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "REQUEST Added IN QUE : " + downQue.toString() + " sending Message " + sendmsg);
                        if (downQue != null) {
                            {
                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Downloading File Now : " + downQue.toString());
                                try (Socket sock = new Socket(ip, GlobalValues.FILE_SERVER_PORT)) {
                                    Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Connecting To Download... : " + downQue.toString());
                                    try (OutputStream sockos = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(sockos)) {
                                        JSONObject downreqJsonObj = new JSONObject();
                                        downreqJsonObj.put("Command", "sendfile");
                                        JSONObject downreqBodyJsonObj = new JSONObject();
                                        downreqBodyJsonObj.put("PID", pid);
                                        downreqBodyJsonObj.put("CNO", cno);
                                        downreqBodyJsonObj.put("PROJECT", projectName);
                                        downreqBodyJsonObj.put("FILE", fileToSend);
                                        downreqBodyJsonObj.put("UUID", nodeUUID);
                                        downreqJsonObj.put("Body", downreqBodyJsonObj);
                                        String downreqsendmsg = downreqJsonObj.toString();//"<Command>sendfile</Command><Body><PID>" + pid + "</PID><CNO>" + cno + "</CNO><FILENAME>" + fname + "</FILENAME><FILE>" + fileToSend + "</FILE></Body>";
                                        bytes = downreqsendmsg.getBytes("UTF-8");
                                        outToServer.writeInt(bytes.length);
                                        outToServer.write(bytes);
                                        System.out.println("Sent Req To Download...");
                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sent Req To Download... : " + downQue.toString() + " sending Message " + downreqsendmsg);

                                        try (DataInputStream sockdin = new DataInputStream(sock.getInputStream())) {
                                            length = sockdin.readInt();                    // read length of incoming message
                                            message = new byte[length];

                                            if (length > 0) {
                                                sockdin.readFully(message, 0, message.length); // read the message
                                            }
                                            String reply = new String(message);
                                            File ipDir = new File("cache/" + nodeUUID + "/" + projectName);
                                            if (!ipDir.exists()) {
                                                ipDir.mkdirs();
                                            }
                                            File ip2Dir = new File(ipDir.getAbsolutePath() + "/" + fileToSend);

                                            String lchecksum = "";
                                            if (new File(ip2Dir.getAbsolutePath() + ".sha").exists()) {
                                                lchecksum = Util.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
                                            }
                                            if (reply.equalsIgnoreCase("foundfile")) {
                                                length = sockdin.readInt();                    // read length of incoming message
                                                message = new byte[length];

                                                if (length > 0) {
                                                    sockdin.readFully(message, 0, message.length); // read the message
                                                }
                                                String checksum2 = new String(message);
                                                System.out.println("CheckSum Recieved " + checksum2);
                                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "CheckSum Recieved  : " + checksum2 + " for Request " + downQue.toString());

                                                {
                                                    String nmsg = "";
                                                    if (lchecksum.trim().equalsIgnoreCase(checksum2.trim())) {
                                                        JSONObject replyJSON = new JSONObject();
                                                        replyJSON.put("REPLY", "foundLocal");
                                                        nmsg = replyJSON.toString();
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        sock.close();
                                                    } else {
                                                        JSONObject replyJSON = new JSONObject();
                                                        replyJSON.put("REPLY", "sendNew");
                                                        nmsg = replyJSON.toString();
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        File df = ip2Dir.getParentFile();
                                                        if (!df.exists()) {
                                                            df.mkdirs();
                                                        }
                                                        long fileLen, downData;
                                                        long starttime = System.currentTimeMillis();
                                                        try (FileOutputStream fos = new FileOutputStream(ip2Dir); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                                                            fileLen = sockdin.readLong();
                                                            downData = fileLen;
                                                            int n = 0;
                                                            byte[] buf = new byte[8192];
                                                            while (fileLen > 0 && ((n = sockdin.read(buf, 0, (int) Math.min(buf.length, fileLen))) != -1)) {
                                                                bos.write(buf, 0, n);
                                                                fileLen -= n;
                                                                downQue.setRemainingsize(fileLen);
                                                                Long elapsedTime = System.currentTimeMillis() - starttime;
                                                                Long allTimeForDownloading = (elapsedTime * (downData / (downData - fileLen)));
                                                                Long remainingTime = allTimeForDownloading - elapsedTime;
                                                                downQue.setRemainingTime(remainingTime);
                                                            }
                                                            bos.flush();
                                                        }
                                                        downQue.setFinished(true);
                                                        downQue.setChecksum(checksum2);
                                                        long endtime = System.currentTimeMillis();
                                                        downQue.setTotalTime(endtime - starttime);
                                                        downQue.setDownloadSpeed((double) ((double) endtime - starttime / (double) 1000));
                                                        System.out.println("File " + fileToSend + " downloaded (" + downData + " bytes read) in " + (endtime - starttime) + " ms");
                                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "File " + fileToSend
                                                                + " downloaded (" + downData + " bytes read) in " + (endtime - starttime) + " ms " + downQue.toString() + " sending Message " + sendmsg);

                                                        Util.saveCheckSum(ip2Dir.getAbsolutePath() + ".sha", checksum2);
                                                    }
                                                }
                                            } else {
                                                System.out.println("Couldn't find file");
                                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.ERROR, "Couldn't find file : " + downQue.toString());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (command.trim().equalsIgnoreCase("downloadObject")) {
                    String objToSend = body.getString("OBJECT");// body.substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                    String pid2 = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                    String cno2 = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                    String classname = body.getString("CLASSNAME");//body.substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                    int instance = body.getInt("INSTANCE");//body.substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                    String checksum = body.getString("CHECKSUM");//body.substring(body.indexOf("<CHECKSUM>") + 10, body.indexOf("</CHECKSUM>"));
                    String ip = body.getString("IP");//body.substring(body.indexOf("<IP>") + 4, body.indexOf("</IP>"));
                    String nodeUUID = body.getString("UUID");
                    String projectName = body.getString("PROJECT");
                    String pathtoFile = "data/" + pid2 + "/.simulated/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";
                    String lpathtoFile = "sim/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";
                    boolean notinQ = true;
                    FileDownQueReq downQue = GlobalValues.DOWNLOAD_QUEUE.get(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim());
                    if (downQue != null) {
                        boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(pathtoFile.trim());
                        boolean b2 = (downQue.getId() == null ? (pid2.trim()) == null : downQue.getId().equals(pid2.trim()));
                        boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
                        boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
                        if (((b1) && (b2) && (b3) && (b4))) {
                            notinQ = false;
                            if (downQue.getFinished()) {
                                String sendmsg = new JSONObject().put("MSG", "finished").toString();//"<MSG>finished</MSG>";
                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);
                                outToClient.write(bytes);
                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "" + downQue.toString() + " sending Message " + sendmsg);
                            } else {
                                long rt = downQue.getRemainingTime();
                                JSONObject sobj = new JSONObject();
                                sobj.put("MSG", "inque");
                                sobj.put("RT", rt);
                                String sendmsg = sobj.toString();// "<MSG>inque</MSG><RT>" + rt + "</RT>";
                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);
                                outToClient.write(bytes);
                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "" + downQue.toString() + " sending Message " + sendmsg);
                            }
                        }
                    }
                    if (notinQ) {
                        GlobalValues.DOWNLOAD_QUEUE.put(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim(), new FileDownQueReq(ip, (pid2),
                                checksum, pathtoFile, System.currentTimeMillis(), 100, 0, 0, false, body.toString(4), nodeUUID, projectName));
                        String sendmsg = new JSONObject().put("MSG", "addedinq").toString();//"<MSG>addedinq</MSG>";
                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);
                        outputStream.close();
                        outToClient.close();
                        submitter.close();
                        FileDownQueReq downQue2 = GlobalValues.DOWNLOAD_QUEUE.get(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim());
                        if (downQue2 != null) {
                            Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Added in Queue : " + downQue2.toString() + " sending Message " + sendmsg);
                            {

                                try (Socket sock = new Socket(ip.trim(), GlobalValues.FILE_SERVER_PORT)) {
                                    try (OutputStream sockos = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(sockos)) {
                                        JSONObject downreqJsonObj = new JSONObject();
                                        downreqJsonObj.put("Command", "resolveObject");
                                        JSONObject downreqBodyJsonObj = new JSONObject();
                                        downreqBodyJsonObj.put("PID", pid2);
                                        downreqBodyJsonObj.put("CNO", cno2);
                                        downreqBodyJsonObj.put("CLASSNAME", classname);
                                        downreqBodyJsonObj.put("OBJECT", objToSend);
                                        downreqBodyJsonObj.put("INSTANCE", instance);
                                        downreqBodyJsonObj.put("PROJECT", projectName);
                                        downreqBodyJsonObj.put("UUID", nodeUUID);
                                        downreqJsonObj.put("Body", downreqBodyJsonObj);
                                        String downreqsendmsg = downreqJsonObj.toString();
                                        bytes = downreqsendmsg.getBytes("UTF-8");
                                        outToServer.writeInt(bytes.length);
                                        outToServer.write(bytes);
                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "" + downQue2.toString() + " sending Message " + downreqsendmsg);
                                        try (DataInputStream sockdin = new DataInputStream(sock.getInputStream())) {
                                            length = sockdin.readInt();                    // read length of incoming message
                                            message = new byte[length];
                                            if (length > 0) {
                                                sockdin.readFully(message, 0, message.length); // read the message
                                            }
                                            String reply = new String(message);
                                            Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Recieved Reply Message: " + reply);
                                            File ipDir = new File("cache/" + nodeUUID + "/" + projectName);
                                            if (!ipDir.exists()) {
                                                ipDir.mkdirs();
                                            }
                                            File ip2Dir = new File(ipDir.getAbsolutePath() + "/" + lpathtoFile);
                                            String lchecksum = "";
                                            if (new File(ip2Dir.getAbsolutePath() + ".sha").exists()) {
                                                lchecksum = Util.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
                                            }
                                            if (reply.trim().equalsIgnoreCase("foundobj")) {
                                                length = sockdin.readInt();                    // read length of incoming message
                                                message = new byte[length];
                                                if (length > 0) {
                                                    sockdin.readFully(message, 0, message.length); // read the message
                                                }
                                                String checksum2 = new String(message);
                                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "CheckSum Recieved " + checksum2 + " for REQUEST : " + downQue2.toString());
                                                {
                                                    String nmsg = "";
                                                    if (lchecksum.trim().equalsIgnoreCase(checksum2.trim())) {
                                                        JSONObject replyJSON = new JSONObject();
                                                        replyJSON.put("REPLY", "foundLocal");
                                                        nmsg = replyJSON.toString();
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + replyJSON.toString() + " to " + ip);
                                                    } else {
                                                        JSONObject replyJSON = new JSONObject();
                                                        replyJSON.put("REPLY", "sendNew");
                                                        nmsg = replyJSON.toString();
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + replyJSON.toString() + " to " + ip);
                                                        File df = ip2Dir.getParentFile();
                                                        if (!df.exists()) {
                                                            df.mkdirs();
                                                        }
                                                        long fileLen, downData;
                                                        long starttime = System.currentTimeMillis();
                                                        try (FileOutputStream fos = new FileOutputStream(ip2Dir); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                                                            fileLen = sockdin.readLong();
                                                            downData = fileLen;
                                                            int n = 0;
                                                            byte[] buf = new byte[8192];
                                                            while (fileLen > 0 && ((n = sockdin.read(buf, 0, (int) Math.min(buf.length, fileLen))) != -1)) {
                                                                bos.write(buf, 0, n);
                                                                fileLen -= n;
                                                                downQue2.setRemainingsize(fileLen);
                                                                Long elapsedTime = System.currentTimeMillis() - starttime;
                                                                Long allTimeForDownloading = (elapsedTime * (downData / (downData - fileLen)));
                                                                Long remainingTime = allTimeForDownloading - elapsedTime;
                                                                downQue2.setRemainingTime(remainingTime);
//                                                                System.out.println("Remaining " + fileLen);
                                                            }
                                                            bos.flush();
                                                        }
                                                        downQue2.setFinished(true);
                                                        downQue2.setChecksum(checksum2);
                                                        long endtime = System.currentTimeMillis();
                                                        downQue2.setTotalTime(endtime - starttime);
                                                        downQue2.setDownloadSpeed((double) ((double) endtime - starttime / (double) 1000));
                                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "File " + pathtoFile + " downloaded (" + downData + " bytes read) in " + (endtime - starttime) + " ms" + downQue2.toString() + " sending Message " + sendmsg);
                                                        Util.saveCheckSum(ip2Dir.getAbsolutePath() + ".sha", checksum2);
                                                    }
                                                }
                                            } else {
                                                System.out.println("Couldn't find file");
                                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.ERROR, "Couldn't find file" + downQue2.toString());

                                            }
                                        }
                                    } catch (Exception ex) {
                                        System.err.println("Socket to " + ip + ":" + GlobalValues.FILE_SERVER_PORT + " Failed : " + ex.toString());
                                        Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                } catch (Exception ex) {
                                    //              Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.ERROR, "Couldn't find file" + downQue.toString());
                                    System.err.println("Socket to " + ip + ":" + GlobalValues.FILE_SERVER_PORT + " Failed : " + ex);
                                    Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
//                               
                            }
                        }
                    }
                    submitter.close();
                } else if (command.trim().equalsIgnoreCase("downloadResult")) {
                    String objToSend = body.getString("OBJECT");// body.substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                    String pid2 = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                    String cno2 = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                    String classname = body.getString("CLASSNAME");//body.substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                    int instance = body.getInt("INSTANCE");//body.substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                    String checksum = body.getString("CHECKSUM");//body.substring(body.indexOf("<CHECKSUM>") + 10, body.indexOf("</CHECKSUM>"));
                    String ip = body.getString("IP");//body.substring(body.indexOf("<IP>") + 4, body.indexOf("</IP>"));
                    String nodeUUID = body.getString("UUID");
                    String projectName = body.getString("PROJECT");
                    String pathtoFile = "data/" + pid2 + "/.result/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";
                    String lpathtoFile = ".result/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";
                    boolean notinQ = true;
                    FileDownQueReq downQue = GlobalValues.DOWNLOAD_QUEUE.get(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim());
                    if (downQue != null) {
                        boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(pathtoFile.trim());
                        boolean b2 = (downQue.getId() == null ? (pid2.trim()) == null : downQue.getId().equals(pid2.trim()));
                        boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
                        boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
                        if (((b1) && (b2) && (b3) && (b4))) {
                            notinQ = false;
                            if (downQue.getFinished()) {
                                String sendmsg = new JSONObject().put("MSG", "finished").toString();//"<MSG>finished</MSG>";
                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);
                                outToClient.write(bytes);
                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "" + downQue.toString() + " sending Message " + sendmsg);
                            } else {
                                long rt = downQue.getRemainingTime();
                                JSONObject sobj = new JSONObject();
                                sobj.put("MSG", "inque");
                                sobj.put("RT", rt);
                                String sendmsg = sobj.toString();// "<MSG>inque</MSG><RT>" + rt + "</RT>";
                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);
                                outToClient.write(bytes);
                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "" + downQue.toString() + " sending Message " + sendmsg);
                            }
                        }
                    }
                    if (notinQ) {
                        GlobalValues.DOWNLOAD_QUEUE.put(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim(), new FileDownQueReq(ip, (pid2),
                                checksum, pathtoFile, System.currentTimeMillis(), 100, 0, 0, false, body.toString(4), nodeUUID, projectName));
                        String sendmsg = new JSONObject().put("MSG", "addedinq").toString();//"<MSG>addedinq</MSG>";
                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);
                        outToClient.write(bytes);
                        outputStream.close();
                        outToClient.close();
                        submitter.close();
                        FileDownQueReq downQue2 = GlobalValues.DOWNLOAD_QUEUE.get(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim());
                        if (downQue2 != null) {
                            Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Added in Queue : " + downQue2.toString() + " sending Message " + sendmsg);
                            {
                                try (Socket sock = new Socket(ip.trim(), GlobalValues.FILE_SERVER_PORT)) {
                                    try (OutputStream sockos = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(sockos)) {
                                        JSONObject downreqJsonObj = new JSONObject();
                                        downreqJsonObj.put("Command", "resolveResult");
                                        JSONObject downreqBodyJsonObj = new JSONObject();
                                        downreqBodyJsonObj.put("PID", pid2);
                                        downreqBodyJsonObj.put("CNO", cno2);
                                        downreqBodyJsonObj.put("CLASSNAME", classname);
                                        downreqBodyJsonObj.put("OBJECT", objToSend);
                                        downreqBodyJsonObj.put("INSTANCE", instance);
                                        downreqBodyJsonObj.put("PROJECT", projectName);
                                        downreqBodyJsonObj.put("UUID", nodeUUID);
                                        downreqJsonObj.put("Body", downreqBodyJsonObj);
                                        String downreqsendmsg = downreqJsonObj.toString();
                                        bytes = downreqsendmsg.getBytes("UTF-8");
                                        outToServer.writeInt(bytes.length);
                                        outToServer.write(bytes);
                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "" + downQue2.toString() + " sending Message " + downreqsendmsg);
                                        try (DataInputStream sockdin = new DataInputStream(sock.getInputStream())) {
                                            length = sockdin.readInt();                    // read length of incoming message
                                            message = new byte[length];
                                            if (length > 0) {
                                                sockdin.readFully(message, 0, message.length); // read the message
                                            }
                                            String reply = new String(message);
                                            Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Recieved Reply Message: " + reply);
                                            File ipDir = new File("cache/" + nodeUUID + "/" + projectName);
                                            if (!ipDir.exists()) {
                                                ipDir.mkdirs();
                                            }
                                            //   String filename = new File(fileToSend).getName();
                                            File ip2Dir = new File(ipDir.getAbsolutePath() + "/" + lpathtoFile);
                                            String lchecksum = "";
                                            if (new File(ip2Dir.getAbsolutePath() + ".sha").exists()) {
                                                lchecksum = Util.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
                                            }
                                            if (reply.trim().equalsIgnoreCase("foundobj")) {
                                                // receive file
                                                length = sockdin.readInt();                    // read length of incoming message
                                                message = new byte[length];

                                                if (length > 0) {
                                                    sockdin.readFully(message, 0, message.length); // read the message
                                                }
                                                String checksum2 = new String(message);
                                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "CheckSum Recieved " + checksum2 + " for REQUEST : " + downQue2.toString());
                                                {
                                                    String nmsg = "";
                                                    if (lchecksum.trim().equalsIgnoreCase(checksum2.trim())) {
                                                        JSONObject replyJSON = new JSONObject();
                                                        replyJSON.put("REPLY", "foundLocal");
                                                        nmsg = replyJSON.toString();
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + replyJSON.toString() + " to " + ip);

                                                    } else {
                                                        JSONObject replyJSON = new JSONObject();
                                                        replyJSON.put("REPLY", "sendNew");
                                                        nmsg = replyJSON.toString();
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sending " + replyJSON.toString() + " to " + ip);
                                                        File df = ip2Dir.getParentFile();
                                                        if (!df.exists()) {
                                                            df.mkdirs();
                                                        }
                                                        long fileLen, downData;
                                                        long starttime = System.currentTimeMillis();
                                                        try (FileOutputStream fos = new FileOutputStream(ip2Dir); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                                                            fileLen = sockdin.readLong();
                                                            downData = fileLen;
                                                            int n = 0;
                                                            byte[] buf = new byte[8192];
                                                            while (fileLen > 0 && ((n = sockdin.read(buf, 0, (int) Math.min(buf.length, fileLen))) != -1)) {
                                                                bos.write(buf, 0, n);
                                                                fileLen -= n;
                                                                downQue2.setRemainingsize(fileLen);
                                                                Long elapsedTime = System.currentTimeMillis() - starttime;
                                                                Long allTimeForDownloading = (elapsedTime * (downData / (downData - fileLen)));
                                                                Long remainingTime = allTimeForDownloading - elapsedTime;
                                                                downQue2.setRemainingTime(remainingTime);
                                                            }
                                                            bos.flush();
                                                        }
                                                        downQue2.setFinished(true);
                                                        downQue2.setChecksum(checksum2);
                                                        long endtime = System.currentTimeMillis();
                                                        downQue2.setTotalTime(endtime - starttime);
                                                        downQue2.setDownloadSpeed((double) ((double) endtime - starttime / (double) 1000));
                                                        Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.OUTPUT, "File " + pathtoFile + " downloaded (" + downData + " bytes read) in " + (endtime - starttime) + " ms" + downQue2.toString() + " sending Message " + sendmsg);
                                                        Util.saveCheckSum(ip2Dir.getAbsolutePath() + ".sha", checksum2);
                                                    }
                                                }
                                            } else {
                                                System.out.println("Couldn't find file");
                                                Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.ERROR, "Couldn't find file" + downQue2.toString());
                                            }
                                        }
                                    } catch (Exception ex) {
                                        System.err.println("Socket to " + ip + ":" + GlobalValues.FILE_SERVER_PORT + " Failed : " + ex.toString());
                                        Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                } catch (Exception ex) {
                                    //              Util.appendToFileDownloadLog(GlobalValues.LOG_LEVEL.ERROR, "Couldn't find file" + downQue.toString());
                                    System.err.println("Socket to " + ip + ":" + GlobalValues.FILE_SERVER_PORT + " Failed : " + ex);
                                    Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                    submitter.close();
                } else {
                    submitter.close();
                }

            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
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
