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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class InsertResultWareHouse implements Runnable {

    public static boolean created = false;
    String projectName;
    SQLiteJDBC resWH = new SQLiteJDBC();
    String SCHEDULER, PID;
    String STARTTIME, ENDTIME, TOTALTIME, NOH, POH, CHUNKSIZE, TCHUNKS, TNODES, FINISHED, AVGWAITINQ, AVGSLEEP;
    double PRFM;
    double avgCacheHitMissRatio = 0;
    long avgDownloadData = 0;
    double avgDownloadSpeed = 0;
    int avgReqSent = 0;
    long avgUploadData = 0;
    double avgUploadSpeed = 0;
    int avgReqRecieved = 0;
    long avgCachedData = 0;
    int selectedNodes = 0,duplicates=0;
    long schedulingOH=0;
    public InsertResultWareHouse(String PID, String Project, String SCHEDULER, String STARTTIME, String ENDTIME, String TOTALTIME, String NOH, String POH, String CHUNKSIZE, String TCHUNKS, String TNODES, double PRFM, String FINISHED, String AVGWAITINQ, String AVGSLEEP,
            double avgCacheHitMissRatio,
            long avgDownloadData,
            double avgDownloadSpeed,
            int avgReqSent,
            long avgUploadData,
            double avgUploadSpeed,
            int avgReqRecieved,
            long avgCachedData, int selectedNodes,int duplicates,long schedulingOH) {

        this.projectName = Project;
        this.STARTTIME = STARTTIME;
        this.ENDTIME = ENDTIME;
        this.TOTALTIME = TOTALTIME;
        this.NOH = NOH;
        this.POH = POH;
        this.CHUNKSIZE = CHUNKSIZE;
        this.TCHUNKS = TCHUNKS;
        this.TNODES = TNODES;
        this.FINISHED = FINISHED;
        this.PRFM = PRFM;
        this.SCHEDULER = SCHEDULER;
        this.PID = PID;
        this.AVGSLEEP = AVGSLEEP;
        this.AVGWAITINQ = AVGWAITINQ;

        this.avgCacheHitMissRatio = avgCacheHitMissRatio;
        this.avgDownloadData = avgDownloadData;
        this.avgDownloadSpeed = avgDownloadSpeed;
        this.avgReqSent = avgReqSent;
        this.avgUploadData = avgUploadData;
        this.avgUploadSpeed = avgUploadSpeed;
        this.avgReqRecieved = avgReqRecieved;
        this.avgCachedData = avgCachedData;
        this.selectedNodes = selectedNodes;
        this.duplicates=duplicates;
        this.schedulingOH=schedulingOH;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("InsertResDBWHThread");

        String sql = "";
        if (!created) {
            created = createTable();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
        Date date = new Date();
        sql = "INSERT INTO RESULTWH ("
                + "PID,"
                + " PROJECT,"
                + " SCHEDULER,"
                + " STARTTIME,"
                + " ENDTIME,"
                + " TOTALTIME,"
                + " NOH,"
                + " POH,"
                + " CHUNKSIZE,"
                + " TCHUNKS,"
                + " TNODES,"
                + " PRFM ,"
                + " FINISHED ,"
                + " AVGWAITINQ,"
                + " AVGSLEEP,"
                + "avgCacheHitMissRatio,"
                + "avgDownloadData,"
                + "avgDownloadSpeed,"
                + "avgReqSent,"
                + "avgUploadData,"
                + "avgUploadSpeed,"
                + "avgReqRecieved,"
                + "avgCachedData,"
                + "selectedNodes,"
                + "duplicates,"
                + "schedulingOH,"
                + " TIMESTAMP)"
                + " VALUES("
                + "'" + PID
                + "','" + projectName
                + "','" + SCHEDULER
                + "','" + STARTTIME
                + "','" + ENDTIME
                + "','" + TOTALTIME
                + "','" + NOH
                + "','" + POH
                + "','" + CHUNKSIZE
                + "','" + TCHUNKS
                + "','" + TNODES
                + "','" + PRFM
                + "','" + FINISHED
                + "','" + AVGWAITINQ
                + "','" + AVGSLEEP
                + "','" + avgCacheHitMissRatio
                + "','" + avgDownloadData
                + "','" + avgDownloadSpeed
                + "','" + avgReqSent
                + "','" + avgUploadData
                + "','" + avgUploadSpeed
                + "','" + avgReqRecieved
                + "','" + avgCachedData
                + "','" + selectedNodes
                + "','" + duplicates
                + "','" + schedulingOH
                + "','" + dateFormat.format(date) + "');";
        resWH.insert("log/dw-result.db", sql);
        resWH.closeConnection();
    }

    private boolean createTable() {

        String sql = "CREATE TABLE RESULTWH"
                + "(ID INTEGER PRIMARY KEY   AUTOINCREMENT  NOT NULL ,"
                + "PID TEXT,"
                + "PROJECT TEXT ,"
                + "SCHEDULER TEXT,"
                + "STARTTIME TEXT,"
                + "ENDTIME TEXT,"
                + "TOTALTIME TEXT,"
                + "NOH TEXT,"
                + "POH TEXT,"
                + "CHUNKSIZE TEXT,"
                + "TCHUNKS TEXT,"
                + "TNODES TEXT,"
                + "PRFM DOUBLE,"
                + "FINISHED TEXT,"
                + "AVGWAITINQ TEXT,"
                + "AVGSLEEP TEXT,"
                + "avgCacheHitMissRatio DOUBLE,"
                + "avgDownloadData LONG,"
                + "avgDownloadSpeed DOUBLE,"
                + "avgReqSent INT,"
                + "avgUploadData LONG,"
                + "avgUploadSpeed DOUBLE,"
                + "avgReqRecieved INT,"
                + "avgCachedData LONG,"
                + "selectedNodes INT,"
                + "duplicates INT,"
                + "schedulingOH LONG,"
                + "TIMESTAMP DATE);";
        boolean created = resWH.createtable("log/dw-result.db", sql);
        resWH.closeConnection();
        if (!created) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
            Date date = new Date();
            File existing = new File("log/dw-result.db");
            if (existing.exists()) {
                existing.renameTo(new File("log/dw-result-" + dateFormat.format(date) + ".db"));
            }
            created = resWH.createtable("log/dw-result.db", sql);
            resWH.closeConnection();
        }
        return created;
    }
}
