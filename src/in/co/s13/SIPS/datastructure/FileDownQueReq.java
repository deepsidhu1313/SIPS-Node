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

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
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

}
