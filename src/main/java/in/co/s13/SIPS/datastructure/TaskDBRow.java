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

import java.util.ArrayList;
import java.util.OptionalDouble;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class TaskDBRow {

    private String taskID, projectName, submitterUUID;
    private int chunkNo = Integer.MIN_VALUE;
    private Process process = null;
    private long downloadData = 0;
    private int reqSent = 0;
    private long uploadData = 0;
    private int reqRecieved = 0, cacheHit = 0, cacheMiss = 0;
    private long cachedData = 0;
    private ArrayList<Double> uploadSpeed = new ArrayList<>(), downloadSpeed = new ArrayList<>();

    public TaskDBRow(String taskID, String projectName, String submitterUUID, int chunkNo) {
        this.taskID = taskID;
        this.projectName = projectName;
        this.submitterUUID = submitterUUID;
        this.chunkNo = chunkNo;
    }

    public String getTaskID() {
        return taskID;
    }

    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSubmitterUUID() {
        return submitterUUID;
    }

    public void setSubmitterUUID(String SubmitterUUID) {
        this.submitterUUID = SubmitterUUID;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(int chunkNo) {
        this.chunkNo = chunkNo;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public double getCacheHitMissRatio() {
        return (double) cacheHit / (double) (cacheMiss < 1 ? 1 : cacheMiss);
    }

    public long getDownloadData() {
        return downloadData;
    }

    public void setDownloadData(long downloadData) {
        this.downloadData = downloadData;
    }

    public int incrementCacheHit() {
        return cacheHit++;
    }

    public int incrementCacheMiss() {
        return cacheMiss++;
    }

    public Double getAvgUploadSpeed() {
        OptionalDouble avgUploadSpeed = uploadSpeed.parallelStream()
                .mapToDouble(a -> a)
                .average();
        return avgUploadSpeed.isPresent() ? avgUploadSpeed.getAsDouble() : 0;
    }

    public void addUploadSpeed(Double uploadSpeed) {
        this.uploadSpeed.add(uploadSpeed);
    }

    public Double getAvgDownloadSpeed() {
        OptionalDouble avgDownloadSpeed = downloadSpeed.parallelStream()
                .mapToDouble(a -> a)
                .average();
        return avgDownloadSpeed.isPresent() ? avgDownloadSpeed.getAsDouble() : 0;
    }

    public void addDownloadSpeed(Double downloadSpeed) {
        this.downloadSpeed.add(downloadSpeed);
    }

    public int getReqSent() {
        return reqSent;
    }

    public int incrementReqSent() {
        return reqSent++;
    }

    public void setReqSent(int reqSent) {
        this.reqSent = reqSent;
    }

    public long getUploadData() {
        return uploadData;
    }

    public void setUploadData(long uploadData) {
        this.uploadData = uploadData;
    }

    public int getReqRecieved() {
        return reqRecieved;
    }

    public int incrementReqRecieved() {
        return reqRecieved++;
    }

    public void setReqRecieved(int reqRecieved) {
        this.reqRecieved = reqRecieved;
    }

    public long getCachedData() {
        return cachedData;
    }

    public void setCachedData(long cachedData) {
        this.cachedData = cachedData;
    }

    @Override
    public String toString() {
        return this.toJSON().toString(4);
    }

    public JSONObject toJSON() {
        JSONObject taskDBRow = new JSONObject();
        taskDBRow.put("TaskID", taskID);
        taskDBRow.put("ProjectName", projectName);
        taskDBRow.put("SubmitterUUID", submitterUUID);
        taskDBRow.put("ChunkNo", chunkNo);
        taskDBRow.put("CacheHitMissRatio", getCacheHitMissRatio());
        taskDBRow.put("DownloadData", downloadData);
        taskDBRow.put("AvgDownloadSpeed", getAvgDownloadSpeed());
        taskDBRow.put("ReqSent", reqSent);
        taskDBRow.put("UploadData", uploadData);
        taskDBRow.put("AvgUploadSpeed", getAvgUploadSpeed());
        taskDBRow.put("ReqRecieved", reqRecieved);
        taskDBRow.put("CachedData", cachedData);
        taskDBRow.put("CacheHit", cacheHit);
        taskDBRow.put("CacheMiss", cacheMiss);
        return taskDBRow;
    }

}
