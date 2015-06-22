/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package executor;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class RecieveFileBak {

    public final static int SOCKET_PORT = 13133;      // you may change this
    String SERVER = "";  // localhost
    public final static int FILE_SIZE = 999999999;
    ArrayList<String> logmsg = new ArrayList<>();

    public RecieveFileBak(String IP, String id, String cno, String projectname, String localFolder, ArrayList<String> FileList) {
        SERVER = IP;
        ExecutorService rfExecutor = Executors.newFixedThreadPool(5);
        FileList.stream().forEach((_item) -> {
            if (_item.trim().length() > 0) {
                Thread rt = new Thread(() -> {

                    int bytesRead;
                    int current = 0;
                    try {
                        try (Socket sock = new Socket(SERVER, SOCKET_PORT)) {
                            System.out.println("Connecting...");
                            try (OutputStream os = sock.getOutputStream(); DataOutputStream outToServer = new DataOutputStream(os)) {
                                String sendmsg = "<Command>sendfile</Command><Body><PID>" + id + "</PID><CNO>" + cno + "</CNO><FILENAME>" + projectname + "</FILENAME><FILE>" + _item + "</FILE></Body>";
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
                                    if (reply.equalsIgnoreCase("foundfile")) {
                                        // receive file
                                        //     InputStream is = sock.getInputStream();
                                        File df = new File(localFolder + "/" + _item).getParentFile();
                                        if (!df.exists()) {
                                            df.mkdirs();
                                        }
                                        long fileLen, downData;
                                        long starttime = System.currentTimeMillis();

                                        try (FileOutputStream fos = new FileOutputStream(localFolder + "/" + _item); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                                            fileLen = dIn.readLong();
                                            downData = fileLen;
                                            /*for (long j = 0; j <= fileLen; j++) {
                                             int tempint = is.read();
                                             bos.write(tempint);
                                             }*/
                                            int n = 0;
                                            byte[] buf = new byte[8092];
                                            while (fileLen > 0 && ((n = dIn.read(buf, 0, (int) Math.min(buf.length, fileLen))) != -1)) {
                                                bos.write(buf, 0, n);
                                                fileLen -= n;
                                            }
                                            bos.flush();
                                        }

                                        System.out.println("File " + _item
                                                + " downloaded (" + downData + " bytes read)");
                                        logmsg.add("File " + _item
                                                + " downloaded (" + downData + " bytes read)");
                                        long endtime = System.currentTimeMillis();
                                        Thread t2 = new Thread(new sendCommOverHead("ComOH", IP, id, cno, projectname, "" + (endtime - starttime)));
                                        t2.start();
                                    } else {
                                        System.out.println("Couldn't find file");
                                        logmsg.add("Couldn't Find File on Master, Plz check file exists in Frameworks data directory " + _item);
                                    }
                                } // read length of incoming message
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(RecieveFileBak.class.getName()).log(Level.SEVERE, null, ex);
                    }

                });
                rfExecutor.submit(rt);
            }
        });

        rfExecutor.shutdown();
        try {
            rfExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(RecieveFileBak.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public ArrayList<String> getFileLog() {
        return logmsg;
    }

}
