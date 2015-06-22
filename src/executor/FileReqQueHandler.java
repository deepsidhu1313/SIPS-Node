/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import controlpanel.settings;
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

/**
 *
 * @author Nika
 */
public class FileReqQueHandler implements Runnable {

    Socket submitter;
    int pnum;
    String simsql = "";
    long pdelay = 10;
    private FileInputStream fis;
    private BufferedInputStream bis;
    //  private String FILE_TO_SEND;

    public FileReqQueHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {
        try {
            DataInputStream dIn = new DataInputStream(submitter.getInputStream());
            OutputStream os = submitter.getOutputStream();
            DataOutputStream outToClient = new DataOutputStream(os);
            //        BufferedOutputStream bos = new BufferedOutputStream(submitter.getOutputStream());
            String msg = "";
            int length = dIn.readInt();                    // read length of incoming message
            byte[] message = new byte[length];

            if (length > 0) {
                dIn.readFully(message, 0, message.length); // read the message
            }
            String s = new String(message);
            msg = "" + s;

            InetAddress inetAddress = submitter.getInetAddress();
            String ipAddress = inetAddress.getHostAddress();
            if (msg.length() > 1) {
                //settings.outPrintln("hurray cond 1");
                System.out.println("IP adress of sender is " + ipAddress);

                System.out.println("" + msg);

                String command = msg.substring(msg.indexOf("<Command>") + 9, msg.indexOf("</Command>"));
                String body = msg.substring(msg.indexOf("<Body>") + 6, msg.indexOf("</Body>"));
                //     System.out.println(msg);
                if (command.trim().equalsIgnoreCase("downloadfile")) {
                    System.out.println("finding file");
                    String fileToSend = body.substring(body.indexOf("<FILE>") + 6, body.indexOf("</FILE>"));
                    String pid = body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                    String cno = body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                    String fname = body.substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                    String checksum = body.substring(body.indexOf("<CHECKSUM>") + 10, body.indexOf("</CHECKSUM>"));
                    String ip = body.substring(body.indexOf("<IP>") + 4, body.indexOf("</IP>"));
                    System.out.println("Accepted connection : " + submitter);
                    // send file
                    boolean notinQ = true;
                    for (FileDownQueReq downQue : FileReqQueServer.downQue) {boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(fileToSend.trim());
                            boolean b2 = downQue.getId() == Integer.parseInt(pid.trim());
                            boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
                            boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
                            System.out.println("Compairing  :\nFilename:" + downQue.getFilename().trim()
                                    + " with " + fileToSend.trim() + "\t" + b1
                                    + "\nPID:" + downQue.getId() + " with " + pid + "\t" + b2
                                    + "\nChecksum:" + downQue.getChecksum().trim() + " with " + checksum
                                    + "\t" + b3
                                    + "\nIP: " + downQue.getIp().trim() + " with " + ip + "\t" + b4);

                            if (((b1) && (b2) && (b3) && (b4))) {
                            notinQ = false;
                            System.out.println("REQUEST Already IN QUE");
                            if (downQue.getFinished()) {
                                String sendmsg = "<MSG>finished</MSG>";

                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);

                                outToClient.write(bytes);

                                os.close();
                                outToClient.close();
                                submitter.close();
                                System.out.println("REQUEST Already Finished");

                            } else {
                                long rt = downQue.getRemainingTime();
                                String sendmsg = "<MSG>inque</MSG><RT>" + rt + "</RT>";

                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);

                                outToClient.write(bytes);

                                os.close();
                                outToClient.close();
                                submitter.close();
                                System.out.println("REQUEST Already IN QUE wait for " + rt);

                            }
                        }

                    }
                    if (notinQ) {
                        FileReqQueServer.downQue.add(new FileDownQueReq(ip, Integer.parseInt(pid),
                                checksum, fileToSend, System.currentTimeMillis(), 100, 0, 0, false, body));

                        System.out.println("REQUEST Added IN QUE");

                        String sendmsg = "<MSG>addedinq</MSG>";

                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);

                        outToClient.write(bytes);
                        os.close();
                        outToClient.close();
                        submitter.close();

                        for (FileDownQueReq downQue : FileReqQueServer.downQue) {
                            boolean b1 = downQue.getFilename().trim().equalsIgnoreCase(fileToSend.trim());
                            boolean b2 = downQue.getId() == Integer.parseInt(pid.trim());
                            boolean b3 = downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim());
                            boolean b4 = downQue.getIp().trim().equalsIgnoreCase(ip.trim());
                            System.out.println("Compairing For Download :\nFilename:" + downQue.getFilename().trim()
                                    + " with " + fileToSend.trim() + "\t" + b1
                                    + "\nPID:" + downQue.getId() + " with " + pid + "\t" + b2
                                    + "\nChecksum:" + downQue.getChecksum().trim() + " with " + checksum
                                    + "\t" + b3
                                    + "\nIP: " + downQue.getIp().trim() + " with " + ip + "\t" + b4);

                            if (((b1) && (b2) && (b3) && (b4))) {
                                System.out.println("Downloading File Now");
                                try (Socket sock = new Socket(ip, 13133)) {
                                    System.out.println("Connecting To Download...");
                                    try (OutputStream sockos = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(sockos)) {
                                        String downreqsendmsg = "<Command>sendfile</Command><Body><PID>" + pid + "</PID><CNO>" + cno + "</CNO><FILENAME>" + fname + "</FILENAME><FILE>" + fileToSend + "</FILE></Body>";
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
                                                lchecksum = settings.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
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
                                                        settings.saveCheckSum(ip2Dir.getAbsolutePath() + ".sha", checksum2);
                                                    }
                                                }
                                            } else {
                                                System.out.println("Couldn't find file");
                                                //    logmsg.add("Couldn't Find File on Master, Plz check file exists in Frameworks data directory " + _item);
                                            }
                                        } // read length of incoming message
                                    }
                                }
                                break;
                            }
                        }
                    } else {

                    }
                    //File myFile = new File("data/" + pid + "/" + fileToSend);

                } else if (command.trim().equalsIgnoreCase("downloadObject")) {
                    System.out.println("finding Object");
                    String objToSend = body.substring(body.indexOf("<OBJECT>") + 8, body.indexOf("</OBJECT>"));
                    String pid2 = body.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                    String cno2 = body.substring(body.indexOf("<CNO>") + 5, body.indexOf("</CNO>"));
                    String classname = body.substring(body.indexOf("<CLASSNAME>") + 11, body.indexOf("</CLASSNAME>"));
                    String instance = body.substring(body.indexOf("<INSTANCE>") + 10, body.indexOf("</INSTANCE>"));
                    String checksum = body.substring(body.indexOf("<CHECKSUM>") + 10, body.indexOf("</CHECKSUM>"));
                    String ip = body.substring(body.indexOf("<IP>") + 4, body.indexOf("</IP>"));

                    System.out.println("Accepted connection : " + submitter);
                    // send file
                    String pathtoFile = "data/" + pid2 + "/sim/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";
                    String lpathtoFile = "sim/" + classname + "/" + objToSend + "-instance-" + instance + ".obj";

                    File myFile2 = new File(pathtoFile);

                    boolean notinQ = true;
                    for (FileDownQueReq downQue : FileReqQueServer.downQue) {
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

                            if (((b1) && (b2) && (b3) && (b4))) 
                             {
                            notinQ = false;
                            if (downQue.getFinished()) {
                                String sendmsg = "<MSG>finished</MSG>";

                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);

                                outToClient.write(bytes);

                            } else {
                                long rt = downQue.getRemainingTime();
                                String sendmsg = "<MSG>inque</MSG><RT>" + rt + "</RT>";

                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToClient.writeInt(bytes.length);

                                outToClient.write(bytes);

                            }
                        }

                    }
                    if (notinQ) {
                        FileReqQueServer.downQue.add(new FileDownQueReq(ip, Integer.parseInt(pid2),
                                checksum, pathtoFile, System.currentTimeMillis(), 100, 0, 0, false, body));

                        String sendmsg = "<MSG>addedinq</MSG>";

                        byte[] bytes = sendmsg.getBytes("UTF-8");
                        outToClient.writeInt(bytes.length);

                        outToClient.write(bytes);
                        os.close();
                        outToClient.close();
                        submitter.close();

                        for (FileDownQueReq downQue : FileReqQueServer.downQue) {
   
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

                            if (((b1) && (b2) && (b3) && (b4))) 
                            
                            /*if (downQue.getFilename().trim().equalsIgnoreCase(pathtoFile.trim()) && (downQue.getId() == Integer.parseInt(pid2.trim()))
                                    && (downQue.getChecksum().trim().equalsIgnoreCase(checksum.trim())) && (downQue.getIp().trim().equalsIgnoreCase(ip.trim()))) 
                            */
                            {

                                try (Socket sock = new Socket(ip, 13133)) {
                                    System.out.println("Connecting...");
                                    try (OutputStream sockos = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(sockos)) {
                                        String downreqsendmsg = "<Command>resolveObject</Command>"
                                                + "<Body><PID>" + pid2 + "</PID>"
                                                + "<CNO>" + cno2 + "</CNO>"
                                                + "<CLASSNAME>" + classname + "</CLASSNAME>"
                                                + "<OBJECT>" + objToSend + "</OBJECT>"
                                                + "<INSTANCE>" + instance + "</INSTANCE></Body>";
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
                                                lchecksum = settings.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
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
                                                        System.out.println("File " + pathtoFile
                                                                + " downloaded (" + downData + " bytes read) in " + (endtime - starttime) + " ms");
                                                        settings.saveCheckSum(ip2Dir.getAbsolutePath() + ".sha", checksum2);
                                                    }
                                                }
                                            } else {
                                                System.out.println("Couldn't find file");
                                                //    logmsg.add("Couldn't Find File on Master, Plz check file exists in Frameworks data directory " + _item);
                                            }
                                        } // read length of incoming message
                                    }
                                }
                                break;
                            }
                        }
                    } else {

                    }
                    submitter.close();
                    System.out.println("Done.");
                }

            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FileReqQueHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileReqQueHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        {
            try {
                submitter.close();
            } catch (IOException ex) {
                Logger.getLogger(FileReqQueHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
