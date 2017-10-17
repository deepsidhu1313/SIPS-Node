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
                JSONObject messageJson = new JSONObject(message);
                //settings.outPrintln("hurray cond 1");
                System.out.println("IP adress of sender is " + ipAddress);

                System.out.println("" + msg);

                String command = messageJson.getString("Command");//msg.substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                //String body = messageJson.getString("Body");//msg.substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                //     System.out.println(msg);
                JSONObject body = messageJson.getJSONObject("Body");
                if (command.trim().equalsIgnoreCase("downloadfile")) {
                    System.out.println("finding file");
                    String fileToSend = body.getString("FILE");//body.substring(body.indexOf("<FILE>") + 6, body.indexOf("</FILE>"));
                    String pid = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                    String cno = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                    String fname = body.getString("FILENAME");//body.substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                    String checksum = body.getString("CHECKSUM");//body.substring(body.indexOf("<CHECKSUM>") + 10, body.indexOf("</CHECKSUM>"));
                    String ip = body.getString("IP");//body.substring(body.indexOf("<IP>") + 4, body.indexOf("</IP>"));
                    System.out.println("Accepted connection : " + submitter);
                    // send file
                    boolean notinQ = true; //for
                    {
                        FileDownQueReq downQue = GlobalValues.DOWNLOAD_QUEUE.get(fileToSend.trim() + "-" + pid.trim() + "-" + checksum.trim() + "-" + ip.trim());
                        {
//                        boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(fileToSend.trim());
//                        boolean b2 = downQue.getId() == Integer.parseInt(pid.trim());
//                        boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
//                        boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
//                        System.out.println("Compairing  :\nFilename:" + downQue.getFilename().trim()
//                                + " with " + fileToSend.trim() + "\t" + b1
//                                + "\nPID:" + downQue.getId() + " with " + pid + "\t" + b2
//                                + "\nChecksum:" + downQue.getChecksum().trim() + " with " + checksum
//                                + "\t" + b3
//                                + "\nIP: " + downQue.getIp().trim() + " with " + ip + "\t" + b4);
//
//                        if (((b1) && (b2) && (b3) && (b4))) 
                            {
                                notinQ = false;
                                System.out.println("REQUEST Already IN QUE");
                                if (downQue.getFinished()) {
                                    String sendmsg = new JSONObject().put("MSG", "finished").toString(4);//"<MSG>finished</MSG>";

                                    byte[] bytes = sendmsg.getBytes("UTF-8");
                                    outToClient.writeInt(bytes.length);

                                    outToClient.write(bytes);

                                    outputStream.close();
                                    outToClient.close();
                                    submitter.close();
                                    System.out.println("REQUEST Already Finished");
                                    //    break;
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
                                    System.out.println("REQUEST Already IN QUE wait for " + rt);
                                    //  break;

                                }
                            }

                        }
                    }
                    if (notinQ) {
                        GlobalValues.DOWNLOAD_QUEUE.put(fileToSend.trim() + "-" + pid.trim() + "-" + checksum.trim() + "-" + ip.trim(),
                                new FileDownQueReq(ip, Integer.parseInt(pid),
                                        checksum, fileToSend, System.currentTimeMillis(), 100, 0, 0, false, body.toString()));

                        System.out.println("REQUEST Added IN QUE");

                        String sendmsg = new JSONObject().put("MSG", "addedinq").toString(4);//"<MSG>addedinq</MSG>";

                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);

                        outToClient.write(bytes);
                        outputStream.close();
                        outToClient.close();
                        submitter.close();

                        FileDownQueReq downQue = GlobalValues.DOWNLOAD_QUEUE.get(fileToSend.trim() + "-" + pid.trim() + "-" + checksum.trim() + "-" + ip.trim());
                        {
//                            boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(fileToSend.trim());
//                            boolean b2 = downQue.getId() == Integer.parseInt(pid.trim());
//                            boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
//                            boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
//                            System.out.println("Compairing For Download :\nFilename:" + downQue.getFilename().trim()
//                                    + " with " + fileToSend.trim() + "\t" + b1
//                                    + "\nPID:" + downQue.getId() + " with " + pid + "\t" + b2
//                                    + "\nChecksum:" + downQue.getChecksum().trim() + " with " + checksum
//                                    + "\t" + b3
//                                    + "\nIP: " + downQue.getIp().trim() + " with " + ip + "\t" + b4);
//
//                            if (((b1) && (b2) && (b3) && (b4))) 
                            {
                                System.out.println("Downloading File Now");
                                try (Socket sock = new Socket(ip, GlobalValues.FILE_SERVER_PORT)) {
                                    System.out.println("Connecting To Download...");
                                    try (OutputStream sockos = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(sockos)) {
                                        JSONObject downreqJsonObj = new JSONObject();
                                        downreqJsonObj.put("Command", "sendfile");
                                        JSONObject downreqBodyJsonObj = new JSONObject();
                                        downreqBodyJsonObj.put("PID", pid);
                                        downreqBodyJsonObj.put("CNO", cno);
                                        downreqBodyJsonObj.put("FILENAME", fname);
                                        downreqBodyJsonObj.put("FILE", fileToSend);
                                        downreqJsonObj.put("BODY", downreqBodyJsonObj);
                                        String downreqsendmsg = downreqJsonObj.toString();//"<Command>sendfile</Command><Body><PID>" + pid + "</PID><CNO>" + cno + "</CNO><FILENAME>" + fname + "</FILENAME><FILE>" + fileToSend + "</FILE></Body>";
                                        bytes = downreqsendmsg.getBytes("UTF-8");
                                        outToServer.writeInt(bytes.length);
                                        outToServer.write(bytes);
                                        System.out.println("Sent Req To Download...");
                                        try (DataInputStream sockdin = new DataInputStream(sock.getInputStream())) {
                                            length = sockdin.readInt();                    // read length of incoming message
                                            message = new byte[length];

                                            if (length > 0) {
                                                sockdin.readFully(message, 0, message.length); // read the message
                                            }
                                            String reply = new String(message);
                                            File ipDir = new File("cache/" + ip);
                                            if (!ipDir.exists()) {
                                                ipDir.mkdirs();
                                            }
                                            //   String filename = new File(fileToSend).getName();
                                            File ip2Dir = new File(ipDir.getAbsolutePath() + "/" + fileToSend);

                                            String lchecksum = "";
                                            if (new File(ip2Dir.getAbsolutePath() + ".sha").exists()) {
                                                lchecksum = Util.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
                                            }
                                            if (reply.equalsIgnoreCase("foundfile")) {
                                                // receive file
                                                length = sockdin.readInt();                    // read length of incoming message
                                                message = new byte[length];

                                                if (length > 0) {
                                                    sockdin.readFully(message, 0, message.length); // read the message
                                                }
                                                String checksum2 = new String(message);
                                                System.out.println("CheckSum Recieved " + checksum2);
                                                //InputStream is = sock.getInputStream();
                                                //if (lchecksum.trim().length() > 0)
                                                {
                                                    String nmsg = "";
                                                    if (lchecksum.trim().equalsIgnoreCase(checksum2.trim())) {
                                                        nmsg = "foundLocal";
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        sock.close();
                                                        //  Server.copyFileUsingStream(ip2Dir.getAbsolutePath(), localFolder + "/" + _item);

                                                    } else {
                                                        nmsg = "sendNew";
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
                                                            /*for (long j = 0; j <= fileLen; j++) {
                                                             int tempint = is.read();
                                                             bos.write(tempint);
                                                             }*/
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
                                                                //            System.out.println("Remaining "+fileLen);
                                                            }
                                                            bos.flush();
                                                        }
                                                        downQue.setFinished(true);
                                                        downQue.setChecksum(checksum2);
                                                        long endtime = System.currentTimeMillis();
                                                        System.out.println("File " + fileToSend
                                                                + " downloaded (" + downData + " bytes read) in " + (endtime - starttime) + " ms");
                                                        Util.saveCheckSum(ip2Dir.getAbsolutePath() + ".sha", checksum2);
                                                    }
                                                }
                                            } else {
                                                System.out.println("Couldn't find file");
                                                //    logmsg.add("Couldn't Find File on Master, Plz check file exists in Frameworks data directory " + _item);
                                            }
                                        } // read length of incoming message // read length of incoming message
                                    }
                                }
//                                break;
                            }
                        }
                    }
//                    else {
//
//                    }
                    //File myFile = new File("data/" + pid + "/" + fileToSend);

                } else if (command.trim().equalsIgnoreCase("downloadObject")) {
                    System.out.println("finding Object");
                    String objToSend = body.getString("OBJECT");// body.substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                    String pid2 = body.getString("PID");//body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                    String cno2 = body.getString("CNO");//body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                    String classname = body.getString("CLASSNAME");//body.substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                    String instance = body.getString("INSTANCE");//body.substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                    String checksum = body.getString("CHECKSUM");//body.substring(body.indexOf("<CHECKSUM>") + 10, body.indexOf("</CHECKSUM>"));
                    String ip = body.getString("IP");//body.substring(body.indexOf("<IP>") + 4, body.indexOf("</IP>"));

                    System.out.println("Accepted connection : " + submitter);
                    // send file
                    String pathtoFile = "data/" + pid2 + "/sim/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";
                    String lpathtoFile = "sim/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";

                    //        File myFile2 = new File(pathtoFile);
                    boolean notinQ = true;
                    FileDownQueReq downQue = GlobalValues.DOWNLOAD_QUEUE.get(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim());
                    {
                        boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(pathtoFile.trim());
                        boolean b2 = downQue.getId() == Integer.parseInt(pid2.trim());
                        boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
                        boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
                        System.out.println("Compairing For Download :\nFilename:" + downQue.getFilename().trim()
                                + " with " + pathtoFile.trim() + "\t" + b1
                                + "\nPID:" + downQue.getId() + " with " + pid2 + "\t" + b2
                                + "\nChecksum:" + downQue.getChecksum().trim() + " with " + checksum
                                + "\t" + b3
                                + "\nIP: " + downQue.getIp().trim() + " with " + ip + "\t" + b4);

                        if (((b1) && (b2) && (b3) && (b4))) {
                            notinQ = false;
                            if (downQue.getFinished()) {
                                String sendmsg = new JSONObject().put("MSG", "finished").toString();//"<MSG>finished</MSG>";

                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);

                                outToClient.write(bytes);

                            } else {
                                long rt = downQue.getRemainingTime();

                                JSONObject sobj = new JSONObject();
                                sobj.put("MSG", "inque");
                                sobj.put("RT", rt);
                                String sendmsg = sobj.toString();// "<MSG>inque</MSG><RT>" + rt + "</RT>";

                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);

                                outToClient.write(bytes);

                            }
                        }

                    }
                    if (notinQ) {
                        GlobalValues.DOWNLOAD_QUEUE.put(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim(), new FileDownQueReq(ip, Integer.parseInt(pid2),
                                checksum, pathtoFile, System.currentTimeMillis(), 100, 0, 0, false, body.toString(4)));

                        String sendmsg = new JSONObject().put("MSG", "addedinq").toString();//"<MSG>addedinq</MSG>";

                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);

                        outToClient.write(bytes);
                        outputStream.close();
                        outToClient.close();
                        submitter.close();

                        FileDownQueReq downQue2 = GlobalValues.DOWNLOAD_QUEUE.get(pathtoFile.trim() + "-" + pid2.trim() + "-" + checksum.trim() + "-" + ip.trim());

                        {

//                            boolean b1 = downQue2.getFilename().trim().equalsIgnoreCase(pathtoFile.trim());
//                            boolean b2 = downQue2.getId() == Integer.parseInt(pid2.trim());
//                            boolean b3 = downQue2.getChecksum().trim().equalsIgnoreCase(checksum.trim());
//                            boolean b4 = downQue2.getIp().trim().equalsIgnoreCase(ip.trim());
//                            System.out.println("Compairing For Download :\nFilename:" + downQue2.getFilename().trim()
//                                    + " with " + pathtoFile.trim() + "\t" + b1
//                                    + "\nPID:" + downQue2.getId() + " with " + pid2 + "\t" + b2
//                                    + "\nChecksum:" + downQue2.getChecksum().trim() + " with " + checksum
//                                    + "\t" + b3
//                                    + "\nIP: " + downQue2.getIp().trim() + " with " + ip + "\t" + b4);
//
//                            if (((b1) && (b2) && (b3) && (b4))) /*if (downQue.getFilename().trim().equalsIgnoreCase(pathtoFile.trim()) && (downQue.getId() == Integer.parseInt(pid2.trim()))
//                                    && (downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim())) && (downQue.getIp().trim().equalsIgnoreCase(ip.trim()))) 
                            {

                                try (Socket sock = new Socket(ip, GlobalValues.FILE_SERVER_PORT)) {
                                    System.out.println("Connecting...");
                                    try (OutputStream sockos = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(sockos)) {
                                        JSONObject downreqJsonObj = new JSONObject();
                                        downreqJsonObj.put("Command", "resolveObject");
                                        JSONObject downreqBodyJsonObj = new JSONObject();
                                        downreqBodyJsonObj.put("PID", pid2);
                                        downreqBodyJsonObj.put("CNO", cno2);
                                        downreqBodyJsonObj.put("CLASSNAME", classname);
                                        downreqBodyJsonObj.put("INSTANCE", instance);
                                        downreqJsonObj.put("BODY", downreqBodyJsonObj);
                                        String downreqsendmsg = downreqJsonObj.toString();
//                                        String downreqsendmsg = "<Command>resolveObject</Command>"
//                                                + "<Body><PID>" + pid2 + "</PID>"
//                                                + "<CNO>" + cno2 + "</CNO>"
//                                                + "<CLASSNAME>" + classname + "</CLASSNAME>"
//                                                + "<OBJECT>" + objToSend + "</OBJECT>"
//                                                + "<INSTANCE>" + instance + "</INSTANCE></Body>";
                                        bytes = downreqsendmsg.getBytes("UTF-8");
                                        outToServer.writeInt(bytes.length);
                                        outToServer.write(bytes);
                                        try (DataInputStream sockdin = new DataInputStream(sock.getInputStream())) {
                                            length = sockdin.readInt();                    // read length of incoming message
                                            message = new byte[length];

                                            if (length > 0) {
                                                sockdin.readFully(message, 0, message.length); // read the message
                                            }
                                            String reply = new String(message);
                                            File ipDir = new File("cache/" + ip);
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
                                                System.out.println("CheckSum Recieved " + checksum2);
                                                //InputStream is = sock.getInputStream();
                                                //if (lchecksum.trim().length() > 0)
                                                {
                                                    String nmsg = "";
                                                    if (lchecksum.trim().equalsIgnoreCase(checksum2.trim())) {
                                                        nmsg = "foundLocal";
                                                        bytes = nmsg.getBytes("UTF-8");
                                                        outToServer.writeInt(bytes.length);
                                                        outToServer.write(bytes);
                                                        //  Server.copyFileUsingStream(ip2Dir.getAbsolutePath(), localFolder + "/" + _item);

                                                    } else {
                                                        nmsg = "sendNew";
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
                                                            /*for (long j = 0; j <= fileLen; j++) {
                                                             int tempint = is.read();
                                                             bos.write(tempint);
                                                             }*/
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
                                                                //            System.out.println("Remaining "+fileLen);
                                                            }
                                                            bos.flush();
                                                        }
                                                        downQue2.setFinished(true);
                                                        downQue2.setChecksum(checksum2);
                                                        long endtime = System.currentTimeMillis();
                                                        System.out.println("File " + pathtoFile
                                                                + " downloaded (" + downData + " bytes read) in " + (endtime - starttime) + " ms");
                                                        Util.saveCheckSum(ip2Dir.getAbsolutePath() + ".sha", checksum2);
                                                    }
                                                }
                                            } else {
                                                System.out.println("Couldn't find file");
                                                //    logmsg.add("Couldn't Find File on Master, Plz check file exists in Frameworks data directory " + _item);
                                            }
                                        } // read length of incoming message // read length of incoming message
                                    }
                                }
//                                break;
                            }
                        }
                    } else {

                    }
                    submitter.close();
                    System.out.println("Done.");
                }

            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        {
            try {
                submitter.close();
            } catch (IOException ex) {
                Logger.getLogger(FileDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
