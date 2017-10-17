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
package in.co.s13.SIPS.executor;


import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class DownloadFile {

    public final static int SOCKET_PORT = 13133;      // you may change this
    String SERVER = "";  // localhost
    public final static int FILE_SIZE = 999999999;
    ArrayList<String> logmsg = new ArrayList<>();
    int MAX_THREADSLEEP = 100000, sleepcounter = 0;

    public DownloadFile(String IP, String id, String cno, String projectname, String localFolder, ArrayList<String> FileList) {
        SERVER = IP;
        ExecutorService rfExecutor = Executors.newFixedThreadPool(5);
        FileList.stream().forEach((_item) -> {
            if (_item.trim().length() > 0) {
                Thread rt = new Thread(() -> {

                    int bytesRead;
                    int current = 0;
                    // fos = null;
                    // bos = null;
                    // sock = null;
                    File ipDir, ip2Dir = null;
                    String lchecksum = "";
                    String checksum = "";
                    {
                        try (Socket sock = new Socket(SERVER, SOCKET_PORT)) {
                            System.out.println("Connecting...");
                            try (OutputStream os = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(os)) {
                                 JSONObject sendMsgJSON= new JSONObject();
                                 sendMsgJSON.put("Command", "sendfileChecksum");
                                 JSONObject sendMsgJSONBody= new JSONObject();
                                 
                                 sendMsgJSONBody.put("PID", id);
                                 sendMsgJSONBody.put("CNO", cno);
                                 sendMsgJSONBody.put("FILENAME", projectname);
                                 sendMsgJSONBody.put("FILE", _item);
                                 sendMsgJSON.put("Body", sendMsgJSONBody);
                                String sendmsg = sendMsgJSON.toString();
                                byte[] bytes = sendmsg.getBytes("UTF-8");
                                outToServer.writeInt(bytes.length);
                                outToServer.write(bytes);
                                try (DataInputStream dIn = new DataInputStream(sock.getInputStream())) {
                                    int length = dIn.readInt();                    // read length of incoming message
                                    byte[] message = new byte[length];

                                    if (length > 0) {
                                        dIn.readFully(message, 0, message.length); // read the message
                                    }
                                    String reply = new String(message);
                                    ipDir = new File("cache/" + IP);
                                    if (!ipDir.exists()) {
                                        ipDir.mkdirs();
                                    }
                                    //String filename = new File(_item).getName();
                                    ip2Dir = new File(ipDir.getAbsolutePath() + "/" + _item);
                                    if (ip2Dir.exists()) {
                                        lchecksum = Util.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
                                    }
                                    if (reply.equalsIgnoreCase("foundfile")) {
                                        // receive file
                                        length = dIn.readInt();                    // read length of incoming message
                                        message = new byte[length];

                                        if (length > 0) {
                                            dIn.readFully(message, 0, message.length); // read the message
                                        }
                                        checksum = new String(message);
                                        System.out.println("CheckSum Recieved " + checksum);
                                        sock.close();
                                    } else {
                                        System.out.println("Couldn't find file");
                                        logmsg.add("Couldn't Find File on Master, Plz check file exists in Frameworks data directory " + _item);
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(DownloadFile.class.getName()).log(Level.SEVERE, null, ex);
                        }
//InputStream is = sock.getInputStream();
                        //if (lchecksum.trim().length() > 0)
                        boolean Ndownloaded = true;
                        long starttime = System.currentTimeMillis();

                        while (Ndownloaded) {
                            String nmsg = "";
                            if (new File(ip2Dir.getAbsolutePath() + ".sha").exists()) {
                                lchecksum = Util.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
                            }
                            if (lchecksum.trim().equalsIgnoreCase(checksum.trim())) {
                                Util.copyFileUsingStream(ip2Dir.getAbsolutePath(), localFolder + "/" + _item);
                                Ndownloaded = false;
                            } else {

                                try (Socket sock = new Socket("127.0.0.1", GlobalValues.FILE_DOWNLOAD_SERVER_PORT)) {
                                    //System.out.println("Connecting...");
                                    try (OutputStream os = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(os)) {
                                         JSONObject sendMsgJSON= new JSONObject();
                                 sendMsgJSON.put("Command", "downloadfile");
                                 JSONObject sendMsgJSONBody= new JSONObject();
                                 
                                 sendMsgJSONBody.put("PID", id);
                                 sendMsgJSONBody.put("CNO", cno);
                                 sendMsgJSONBody.put("FILENAME", projectname);
                                 sendMsgJSONBody.put("FILE", _item);
                                 sendMsgJSONBody.put("IP", SERVER);
                                 sendMsgJSONBody.put("CHECKSUM", checksum);
                                 sendMsgJSON.put("Body", sendMsgJSONBody);
                                
                                        String sendmsg = sendMsgJSON.toString();//"<Command>downloadfile</Command><Body><PID>" + id + "</PID><CNO>" + cno + "</CNO><FILENAME>" + projectname + "</FILENAME><FILE>" + _item + "</FILE><IP>" + SERVER + "</IP><CHECKSUM>" + checksum + "</CHECKSUM></Body>";
                                        byte[] bytes = sendmsg.getBytes("UTF-8");
                                        outToServer.writeInt(bytes.length);
                                        outToServer.write(bytes);
                                        try (DataInputStream dIn = new DataInputStream(sock.getInputStream())) {
                                            int length = dIn.readInt();                    // read length of incoming message
                                            byte[] message = new byte[length];

                                            if (length > 0) {
                                                dIn.readFully(message, 0, message.length); // read the message
                                            }
                                            JSONObject reply = new JSONObject(new String(message));
                                            String rpl = reply.getString("MSG");//substring(reply.indexOf("<MSG>") + 5, reply.indexOf("</MSG>"));
                                            if (rpl.equalsIgnoreCase("finished")) {
                                                // receive file

                                                sock.close();
                                                if (new File(ip2Dir.getAbsolutePath() + ".sha").exists()) {
                                                    lchecksum = Util.LoadCheckSum(ip2Dir.getAbsolutePath() + ".sha");
                                                }
                                                if (lchecksum.trim().equalsIgnoreCase(checksum.trim())) {
                                                    Util.copyFileUsingStream(ip2Dir.getAbsolutePath(), localFolder + "/" + _item);
                                                    Ndownloaded = false;
                                                }

                                            } else if (rpl.equalsIgnoreCase("inque")) {
//                                                String vl = reply.substring(reply.indexOf("<RT>") + 4, reply.indexOf("</RT>"));
                                                sock.close();
                                                double valts = reply.getDouble("RT");
                                                if (valts < 1000) {
                                                    valts = 1000.0;
                                                }
                                                long stime = (long) ((valts * 0.13) + 13);
                                                long start = System.currentTimeMillis();
                                                Thread.sleep(stime);
                                                long end = System.currentTimeMillis();
                                                Thread eiq = new Thread(new sendSleeptime("sleeptime", IP, id, cno, projectname, "" + (end - start)));
                                                eiq.setPriority(Thread.NORM_PRIORITY + 1);
                                                GlobalValues.SEND_SLEEPTIME_EXECUTOR_SERVICE.execute(eiq);

                                            } else if (rpl.equalsIgnoreCase("addedinq")) {
                                                sock.close();
                                                long start = System.currentTimeMillis();

                                                Thread.currentThread().sleep(500);
                                                long end = System.currentTimeMillis();
                                                Thread eiq = new Thread(new sendSleeptime("sleeptime", IP, id, cno, projectname, "" + (end - start)));
                                                eiq.setPriority(Thread.NORM_PRIORITY + 1);
                                                GlobalValues.SEND_SLEEPTIME_EXECUTOR_SERVICE.execute(eiq);

                                            } else {
                                                System.out.println("Couldn't find file");
                                                logmsg.add("Couldn't Find File on Master, Plz check file exists in Frameworks data directory " + _item);
                                            }
                                        } catch (InterruptedException ex) {
                                            Logger.getLogger(DownloadFile.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(DownloadFile.class.getName()).log(Level.SEVERE, null, ex);
                                }

                            }

                            if (sleepcounter > MAX_THREADSLEEP) {
                                break;
                            }
                            sleepcounter++;
                        }
                        long endtime = System.currentTimeMillis();
                        Thread t2 = new Thread(new sendCommOverHead("ComOH", IP, id, cno, projectname, "" + (endtime - starttime)));
                        t2.start();

                    } // read length of incoming message
                }
                );
                rfExecutor.submit(rt);
            }
        });

        rfExecutor.shutdown();
        try {
            rfExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        } catch (InterruptedException ex) {
            Logger.getLogger(DownloadFile.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public ArrayList<String> getFileLog() {
        return logmsg;
    }

}
