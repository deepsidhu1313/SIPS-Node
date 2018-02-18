/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.CollectFiles;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.common.datastructure.Node;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class Distributor {

    private String nodeUUID, chunkNumber, jobToken;

    public Distributor(String nodeUUID, String chunkNumber, String jobToken) {
        this.nodeUUID = nodeUUID;
        this.chunkNumber = chunkNumber;
        this.jobToken = jobToken;
    }

    public String getJobToken() {
        return jobToken;
    }

    public void setJobToken(String jobToken) {
        this.jobToken = jobToken;
    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    public void setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
    }

    public String getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(String chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public void upload() {
        Node node = GlobalValues.LIVE_NODE_ADJ_DB.get(nodeUUID);
        if (node != null) {
            ArrayList<String> ips = node.getIpAddresses();
            JSONObject body = new JSONObject();
            body.put("PID", jobToken);
            body.put("CNO", chunkNumber);
            body.put("UUID", GlobalValues.NODE_UUID);
            JSONArray files = new JSONArray();
            CollectFiles collectFiles = new CollectFiles();
            ArrayList<String> toSend = collectFiles.getFiles("data/" + jobToken + "/dist/" + nodeUUID + ":CN:" + chunkNumber + "/src");
            for (int i = 0; i < toSend.size(); i++) {
                String filePath = toSend.get(i);
                JSONObject file = new JSONObject();
                file.put("FILENAME", filePath.substring(filePath.lastIndexOf("/src/")));
                file.put("CONTENT", Util.readFile(filePath));
                files.put(file);
            }
            body.put("FILES", files);
            JSONObject manifest = Util.readJSONFile("data/" + jobToken + "/manifest.json");
            /**
             * Remove sensitive info from manifest
             */
            manifest.getJSONObject("MASTER",new JSONObject()).remove("API-KEY");
            body.put("MANIFEST", manifest);

            for (int i = 0; i < ips.size(); i++) {
                String get = ips.get(i);
                if (sendTask(get, body)) {
                    break;
                }
            }
        }
    }

    public boolean sendTask(String host, JSONObject body) {
        if (host.contains("%")) {
            host = host.substring(0, host.indexOf("%"));
        }
        try (Socket socket = new Socket(host, GlobalValues.TASK_SERVER_PORT)) {

            try (OutputStream os = socket.getOutputStream(); DataInputStream dIn = new DataInputStream(socket.getInputStream()); DataOutputStream outToServer = new DataOutputStream(os)) {
                JSONObject requestJson = new JSONObject();
                requestJson.put("Command", "createprocess");
                requestJson.put("Body", body);
                String sendmsg = requestJson.toString();
                System.out.println("Sending " + jobToken + " chunk no:" + chunkNumber + " to " + host);
                byte[] bytes = sendmsg.getBytes("UTF-8");
                outToServer.writeInt(bytes.length);
                outToServer.write(bytes);

                int length = dIn.readInt();                    // read length of incoming message
                byte[] message = new byte[length];

                if (length > 0) {
                    dIn.readFully(message, 0, message.length); // read the message
                }
                String reply = (new String(message));
                if (reply.equals("OK")) {
                    return true;
                }
            } catch (Exception e) {
                Logger.getLogger(Distributor.class
                        .getName()).log(Level.SEVERE, null, e);

            }
        } catch (IOException ex) {
            Logger.getLogger(Distributor.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
}
