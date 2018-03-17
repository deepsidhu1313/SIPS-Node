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
package in.co.s13.SIPS.datastructure;

import org.json.JSONObject;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class FileDownQueReq {

    private String id, ip, checksum, filename, reqmsg, nodeUUID, projectName;
    private boolean finished;
    private long starttime, remainingTime, totalTime = 0;
    private double size, remainingsize, downloadSpeed = 00.0;

    public FileDownQueReq(String ip, String id, String checksum, String filename, long starttime, long remainingTime, double size, double remainingsize, boolean finished, String reqmsg, String nodeUUID, String projectName) {
        this.ip = ip;
        this.checksum = checksum;
        this.filename = filename;
        this.id = id;
        this.finished = finished;
        this.starttime = starttime;
        this.remainingTime = remainingTime;
        this.size = size;
        this.remainingsize = remainingsize;
        this.reqmsg = reqmsg;
        this.nodeUUID = nodeUUID;
        this.projectName = projectName;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setReqmsg(String Reqmsg) {
        this.reqmsg = Reqmsg;
    }

    public String getReqmsg() {
        return reqmsg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setRemainingTime(long remainingtime) {
        this.remainingTime = remainingtime;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public double getSize() {
        return size;
    }

    public void setRemainingsize(Double remainingSize) {
        this.remainingsize = remainingSize;
    }

    public double getRemainingsize() {
        return remainingsize;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean getFinished() {
        return finished;
    }

    public void setRemainingsize(long fileLen) {
        this.remainingsize = fileLen;

    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    public void setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public double getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(double downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject request = new JSONObject();
        request.put("ip", ip);
        request.put("checksum", checksum);
        request.put("filename", filename);
        request.put("id", id);
        request.put("finished", finished);
        request.put("starttime", starttime);
        request.put("remainingTime", remainingTime);
        request.put("totalTime", totalTime);
        request.put("size", size);
        request.put("remainingsize", remainingsize);
        request.put("reqmsg", reqmsg);
        request.put("nodeUUID", nodeUUID);
        request.put("downloadSpeed", downloadSpeed);
        request.put("projectName", projectName);
        return request;
    }

}
