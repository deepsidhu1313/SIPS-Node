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
package in.co.s13.SIPS.db;

import in.co.s13.SIPS.tools.Util;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class InsertDistributionWareHouse implements Runnable {

    public static boolean created = false;
    String Node;
    String PID;
    Integer CNO;
    Integer VARTYPE;
    String SCHEDULER;
    Long LStart;
    Long Lend;
    Long Lexec;
    String CS;
    String LOWL;
    String UPL;
    String COUNTER;
    Long Nexec;
    Long CommOH;
    Long ParOH;
    Double PRFM;
    Integer EXITCODE;
    Long ENTERINQ;
    Long STARTINQ;
    Long WAITINQ;
    Long SLEEP;
    String projectName;
    SQLiteJDBC distWH = new SQLiteJDBC();
    double avgCacheHitMissRatio;
    long avgDownloadData;
    double avgDownloadSpeed;
    int avgReqSent;
    long avgUploadData;
    double avgUploadSpeed;
    int avgReqRecieved;
    long avgCachedData;
    private JSONArray cacheHits = new JSONArray();
    private JSONArray cacheMisses = new JSONArray();

    public InsertDistributionWareHouse(String Node,
            String PID,
            Integer CNO,
            Integer VARTYPE,
            String SCHEDULER,
            Long LStart,
            Long Lend,
            Long Lexec,
            String CS,
            String LOWL,
            String UPL,
            String COUNTER,
            Long Nexec,
            Long CommOH,
            Long ParOH,
            Long ENTERINQ,
            Long STARTINQ,
            Long WAITINQ,
            Long SLEEP,
            Double PRFM,
            Integer XTC, String Project,
            double avgCacheHitMissRatio,
            long avgDownloadData,
            double avgDownloadSpeed,
            int avgReqSent,
            long avgUploadData,
            double avgUploadSpeed,
            int avgReqRecieved,
            long avgCachedData, JSONArray cacheHits, JSONArray cacheMisses) {

        this.Node = Node;
        this.PID = PID;
        this.CNO = CNO;
        this.VARTYPE = VARTYPE;
        this.SCHEDULER = SCHEDULER;
        this.LStart = LStart;
        this.Lend = Lend;
        this.Lexec = Lexec;
        this.CS = CS;
        this.LOWL = LOWL;
        this.UPL = UPL;
        this.COUNTER = COUNTER;
        this.Nexec = Nexec;
        this.CommOH = CommOH;
        this.ParOH = ParOH;
        this.PRFM = PRFM;
        this.EXITCODE = XTC;
        this.projectName = Project;
        this.ENTERINQ = ENTERINQ;
        this.STARTINQ = STARTINQ;
        this.WAITINQ = WAITINQ;
        this.SLEEP = SLEEP;

        this.avgCacheHitMissRatio = avgCacheHitMissRatio;
        this.avgDownloadData = avgDownloadData;
        this.avgDownloadSpeed = avgDownloadSpeed;
        this.avgReqSent = avgReqSent;
        this.avgUploadData = avgUploadData;
        this.avgUploadSpeed = avgUploadSpeed;
        this.avgReqRecieved = avgReqRecieved;
        this.avgCachedData = avgCachedData;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("InsertDistDBWHThread");
        String sql = "";
        if (!created) {
            created = createTable();
        }
        //for (int i = 0; i < Node.size(); i++)
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
            Date date = new Date();
            sql = "INSERT INTO DISTWH"
                    + "( IP,"
                    + "PROJECT,"
                    + "PID,"
                    + "CNO,"
                    + "VARTYPE,"
                    + "SCHEDULER,"
                    + "LStartTime,"
                    + "LEndTime,"
                    + "LExcTime,"
                    + "CHUNKSIZE ,"
                    + "LOWLIMIT,"
                    + "UPLIMIT,"
                    + "COUNTER,"
                    + "NExecutionTime,"
                    + "NOH,"
                    + "POH,"
                    + "ENTERINQ,"
                    + "STARTINQ,"
                    + "WAITINQ,"
                    + "SLEEPTIME,"
                    + "PRFM,"
                    + "avgCacheHitMissRatio,"
                    + "avgDownloadData,"
                    + "avgDownloadSpeed,"
                    + "avgReqSent,"
                    + "avgUploadData,"
                    + "avgUploadSpeed,"
                    + "avgReqRecieved,"
                    + "avgCachedData,"
                    + "cacheHits,"
                    + "cacheMisses,"
                    + "EXITCODE,"
                    + "TIMESTAMP"
                    + ") VALUES ("
                    + "'" + Node
                    + "','" + projectName
                    + "','" + PID
                    + "','" + CNO
                    + "','" + VARTYPE
                    + "','" + SCHEDULER
                    + "','" + LStart
                    + "','" + Lend
                    + "','" + Lexec
                    + "','" + CS
                    + "','" + LOWL
                    + "','" + UPL
                    + "','" + COUNTER
                    + "','" + Nexec
                    + "','" + CommOH
                    + "','" + ParOH
                    + "','" + ENTERINQ
                    + "','" + STARTINQ
                    + "','" + WAITINQ
                    + "','" + SLEEP
                    + "','" + PRFM
                    + "','" + avgCacheHitMissRatio
                    + "','" + avgDownloadData
                    + "','" + avgDownloadSpeed
                    + "','" + avgReqSent
                    + "','" + avgUploadData
                    + "','" + avgUploadSpeed
                    + "','" + avgReqRecieved
                    + "','" + avgCachedData
                    + "','" + cacheHits.toString()
                    + "','" + cacheMisses.toString()
                    + "','" + EXITCODE
                    + "','" + dateFormat.format(date) + "');";
            distWH.insert("log/dw-dist.db", sql);
        }
        distWH.closeConnection();

    }

    private boolean createTable() {
        String sql = "CREATE TABLE DISTWH"
                + "(ID INTEGER PRIMARY KEY  AUTOINCREMENT   NOT NULL,"
                + " IP TEXT Not Null,"
                + "PROJECT TEXT ,"
                + "PID INT,"
                + "CNO INT,"
                + "VARTYPE INT,"
                + "SCHEDULER INT,"
                + "LStartTime LONG,"
                + "LEndTime LONG,"
                + "LExcTime LONG,"
                + "CHUNKSIZE DECIMAL,"
                + "LOWLIMIT DECIMAL,"
                + "UPLIMIT DECIMAL,"
                + "COUNTER DECIMAL,"
                + "NExecutionTime LONG,"
                + "NOH LONG,"
                + "POH LONG,"
                + "ENTERINQ LONG,"
                + "STARTINQ LONG,"
                + "WAITINQ LONG,"
                + "SLEEPTIME LONG,"
                + "PRFM DOUBLE,"
                + "EXITCODE INT,"
                + "avgCacheHitMissRatio DOUBLE,"
                + "avgDownloadData LONG,"
                + "avgDownloadSpeed DOUBLE,"
                + "avgReqSent INT,"
                + "avgUploadData LONG,"
                + "avgUploadSpeed DOUBLE,"
                + "avgReqRecieved INT,"
                + "avgCachedData LONG,"
                + "cacheHits TEXT ,"
                + "cacheMisses TEXT ,"
                + "TIMESTAMP DATE);";
        boolean created = distWH.createtable("log/dw-dist.db", sql);
        distWH.closeConnection();
        if (!created) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
            Date date = new Date();
            File existing = new File("log/dw-dist.db");
            if (existing.exists()) {
                existing.renameTo(new File("log/dw-dist-" + dateFormat.format(date) + ".db"));
            }
        }
        created = distWH.createtable("log/dw-dist.db", sql);
        distWH.closeConnection();
        return created;
    }

}
