/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.CollectFiles;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.common.datastructure.IPAddress;
import in.co.s13.sips.lib.common.datastructure.Node;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class Distributor {

    private String nodeUUID, chunkNumber, jobToken, toIPAddress, hostName;

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

    public String getToIPAddress() {
        return toIPAddress;
    }

    public void setToIPAddress(String toIPAddress) {
        this.toIPAddress = toIPAddress;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public boolean upload() {
        Node node = GlobalValues.LIVE_NODE_ADJ_DB.get(nodeUUID);
        if (node != null) {
            this.hostName = node.getHostname();
            ArrayList<IPAddress> ips = new ArrayList<>(node.getIpAddresses().values());
            Collections.sort(ips, IPAddress.IPAddressComparator.DISTANCE.thenComparing(IPAddress.IPAddressComparator.PING_SCORE.reversed()));
            JSONObject body = new JSONObject();
            body.put("PID", jobToken);
            body.put("CNO", chunkNumber);
            body.put("UUID", in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID);
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
            manifest.getJSONObject("MASTER", new JSONObject()).remove("API-KEY");
            body.put("MANIFEST", manifest);

            for (int i = 0; i < ips.size(); i++) {
                String get = ips.get(i).getIp();
                if (get.startsWith("127") && (!in.co.s13.sips.lib.node.settings.GlobalValues.NODE_UUID.equals(nodeUUID))) {
                    continue;
                }
                if (sendTask(get, body)) {
                    Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.OUTPUT, "Sent " + this.jobToken + " " + this.chunkNumber + " to " + get + " UUID " + nodeUUID);
                    return true;
                } else {
                    Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed to Sent " + this.jobToken + " " + this.chunkNumber + " to " + get + " UUID " + nodeUUID);
                }
            }
        }
        return false;
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
                    toIPAddress = host;
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
