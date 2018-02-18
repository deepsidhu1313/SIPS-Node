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

import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.executor.Job;
import in.co.s13.SIPS.executor.ParallelProcess;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.SIPS.virtualdb.UpdateResultDBbefExecVirtual;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
public class JobHandler implements Runnable {

    Socket submitter;

    public JobHandler(Socket connection) {
        submitter = connection;
    }

    @Override
    public void run() {
        try {
            String messageString;
            JSONObject messageJson;
            try (DataInputStream dataInputStream = new DataInputStream(submitter.getInputStream())) {
                messageString = "";
                int messageLength = dataInputStream.readInt();                    // read length of incoming message
                byte[] messageBytes = new byte[messageLength];
                if (messageLength > 0) {
                    dataInputStream.readFully(messageBytes, 0, messageBytes.length); // read the message
                }
                messageString = new String(messageBytes);
                messageJson = new JSONObject(messageString);
                InetAddress inetAddress = submitter.getInetAddress();
                String ipAddress = inetAddress.getHostAddress();
                Thread.currentThread().setName("Process handler for " + ipAddress);
                if (messageString.length() > 1) {
                    System.out.println("IP adress of sender is " + ipAddress);

//                    System.OUT.println("" + messageString);
                    String command = messageJson.getString("Command");//messageString.substring(messageString.indexOf("<Command>") + 9, messageString.indexOf("</Command>"));
                    JSONObject body = messageJson.getJSONObject("Body");//messageString.substring(messageString.indexOf("<Body>") + 6, messageString.indexOf("</Body>"));
                    System.out.println(messageString);
                    if (command.equals("START_JOB")) {
                        GlobalValues.JOB_WAITING.incrementAndGet();
                        String jobToken = body.getString("JOB_TOKEN");
                        GlobalValues.JOB_EXECUTOR.submit(new Job(jobToken));
                        System.out.println("Created Job");

                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            JSONObject replyJSON = new JSONObject();
                            JSONObject replyBody = new JSONObject();
                            JSONObject response = new JSONObject();
                            response.put("Message", "Job Started");
                            replyBody.put("Response", response);
                            replyJSON.put("Body", replyBody);
                            String sendmsg = replyJSON.toString(0);
                            byte[] bytes = sendmsg.getBytes("UTF8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                    } else if (command.equals("GET_JOB_STATUS")) {
                        String jobToken = body.getString("JOB_TOKEN");

                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            JSONObject replyJSON = new JSONObject();
                            JSONObject replyBody = new JSONObject();
                            JSONObject response = new JSONObject();
                            Result result = GlobalValues.RESULT_DB.get(jobToken);
                            if (result != null) {
                                response.put("Message", result);
                            } else {
                                response.put("Message", new JSONObject().put("Error!","No Result Found For Token "+jobToken).toString());
                            
                            }
                            replyBody.put("Response", response);
                            replyJSON.put("Body", replyBody);
                            String sendmsg = replyJSON.toString(0);
                            byte[] bytes = sendmsg.getBytes("UTF8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();
                    } else if (command.equals("CREATE_JOB_TOKEN")) {
                        String submitterUUID = body.getString("UUID");//.substring(body.indexOf("<PID>") + 5, body.indexOf("</PID>"));
                        String jobname = body.getString("JOB_NAME");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String scheduler = body.getString("SCHEDULER");//substring(body.indexOf("<FILENAME>") + 10, body.indexOf("</FILENAME>"));
                        String jobToken = Util.generateJobToken();
                        GlobalValues.RESULT_DB_EXECUTOR.submit(new UpdateResultDBbefExecVirtual(jobname, jobToken, scheduler, submitterUUID));
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            JSONObject replyJSON = new JSONObject();
                            JSONObject replyBody = new JSONObject();
                            JSONObject response = new JSONObject();
                            response.put("Token", jobToken);
                            replyBody.put("Response", response);
                            replyJSON.put("Body", replyBody);
                            String sendmsg = replyJSON.toString(0);
                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);

                        }
                        submitter.close();

                    } else if (command.equals("UPLOAD_FILE")) {
                        String submitterUUID = body.getString("UUID");
                        String jobname = body.getString("JOB_NAME");
                        String jobToken = body.getString("JOB_TOKEN");
                        String filePath = body.getString("PATH");
                        String sha = body.getString("SHA");
                        try (OutputStream os = submitter.getOutputStream(); DataOutputStream outToClient = new DataOutputStream(os)) {
                            JSONObject replyJSON = new JSONObject();
                            JSONObject replyBody = new JSONObject();
                            JSONObject response = new JSONObject();
                            boolean foundLocal = false;
                            File cachedFile = new File("cache/" + submitterUUID + "/" + jobname + "/" + filePath);
                            if (cachedFile.exists()) {
                                if (Util.LoadCheckSum(cachedFile.getAbsolutePath() + ".sha").trim().equalsIgnoreCase(sha.trim())) {
                                    response.put("Message", "FOUND_LOCAL");
                                    foundLocal = true;
                                } else {
                                    response.put("Message", "SEND_NEW");

                                }
                            } else {
                                response.put("Message", "SEND_NEW");

                            }
                            replyBody.put("Response", response);
                            replyJSON.put("Body", replyBody);
                            String sendmsg = replyJSON.toString(0);
                            byte[] bytes = sendmsg.getBytes("UTF-8");
                            outToClient.writeInt(bytes.length);
                            outToClient.write(bytes);
                            if (foundLocal) {
                                File copyTo = new File("data/" + jobToken + "/" + filePath);
                                Util.copyFileUsingStream(cachedFile, copyTo);
                                Util.getCheckSum(copyTo.getAbsolutePath());
                            } else {
                                File toDownload = new File("data/" + jobToken + "/" + filePath);
                                toDownload.getParentFile().mkdirs();
                                try (FileOutputStream fos = new FileOutputStream(toDownload); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                                    long fileLen, downData;
                                    fileLen = dataInputStream.readLong();
                                    System.out.println("Prepared to recieve File of Size " + fileLen);
                                    downData = fileLen;
                                    int n = 0;
                                    byte[] buf = new byte[8192];
                                    while (fileLen > 0 && ((n = dataInputStream.read(buf, 0, (int) Math.min(buf.length, fileLen))) != -1)) {
                                        bos.write(buf, 0, n);
                                        fileLen -= n;
                                    }
                                    bos.flush();
                                }

                                Util.getCheckSum(toDownload.getAbsolutePath());
                                Util.copyFileUsingStream(toDownload, cachedFile);
                                Util.getCheckSum(cachedFile.getAbsolutePath());

                            }
                        }
                        submitter.close();
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(JobHandler.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (submitter != null && !submitter.isClosed()) {
                    submitter.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(JobHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

}
