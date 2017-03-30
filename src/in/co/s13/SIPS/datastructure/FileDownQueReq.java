/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.datastructure;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class FileDownQueReq {

    private String ip, checksum, filename, reqmsg;
    private int id;
    private boolean finished;
    private long starttime, remainingTime;
    private double size, remainingsize;

    public FileDownQueReq(String ip, int id, String checksum, String filename, long starttime, long remainingTime, double size, double remainingsize, boolean finished, String reqmsg) {
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
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setFilename(String ip) {
        this.filename = ip;
    }

    public String getFilename() {
        return filename;
    }

    public void setChecksum(String ip) {
        this.checksum = ip;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setReqmsg(String ip) {
        this.reqmsg = ip;
    }

    public String getReqmsg() {
        return reqmsg;
    }

    public void setId(int ip) {
        this.id = ip;
    }

    public int getId() {
        return id;
    }

    public void setStarttime(long ip) {
        this.starttime = ip;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setRemainingTime(long ip) {
        this.remainingTime = ip;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public void setSize(Double ip) {
        this.size = ip;
    }

    public double getSize() {
        return size;
    }

    public void setRemainingsize(Double ip) {
        this.remainingsize = ip;
    }

    public double getRemainingsize() {
        return remainingsize;
    }

    public void setFinished(boolean ip) {
        this.finished = ip;
    }

    public boolean getFinished() {
        return finished;
    }

    public void setRemainingsize(long fileLen) {
        this.remainingsize = fileLen;

    }

}
