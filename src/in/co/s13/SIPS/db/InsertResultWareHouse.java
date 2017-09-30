/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.db;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class InsertResultWareHouse implements Runnable {

    public static int counter = 1;
    String projectName;
    SQLiteJDBC resWH = new SQLiteJDBC();
    int SCHEDULER, PID;
    String STARTTIME, ENDTIME, TOTALTIME, NOH, POH, CHUNKSIZE, TCHUNKS, TNODES, FINISHED,AVGWAITINQ,AVGSLEEP;
    double PRFM;

    public InsertResultWareHouse(int PID, String Project, int SCHEDULER, String STARTTIME, String ENDTIME, String TOTALTIME, String NOH, String POH, String CHUNKSIZE, String TCHUNKS, String TNODES, double PRFM, String FINISHED,String AVGWAITINQ,String AVGSLEEP) {

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
        this.AVGSLEEP=AVGSLEEP;
        this.AVGWAITINQ=AVGWAITINQ;
    }

    @Override
    public void run() {
         Thread.currentThread().setName("InsertResDBWHThread");
        
        String sql = "";
        if (counter == 1) {
            sql = "CREATE TABLE RESULTWH"
                    + "(ID INTEGER PRIMARY KEY   AUTOINCREMENT  NOT NULL ,"
                    + "PID INT,"
                    + "PROJECT TEXT ,"
                    + "SCHEDULER INT,"
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
                    + "TIMESTAMP DATE);";
            resWH.createtable("appdb/dw-result.db", sql);
            resWH.closeConnection();
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
                + " FINISHED ,AVGWAITINQ,AVGSLEEP,"
                + " TIMESTAMP)" + " VALUES("
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
                + "','" + dateFormat.format(date) + "');";
        resWH.insert("appdb/dw-result.db", sql);
        resWH.closeConnection();
        counter++;
    }

}
