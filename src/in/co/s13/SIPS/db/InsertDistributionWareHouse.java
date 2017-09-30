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
public class InsertDistributionWareHouse implements Runnable {

    public static boolean created = false;
    String Node;
    Integer PID;
    Integer CNO;
    Integer VARTYPE;
    Integer SCHEDULER;
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
    Integer XTC;
    Long ENTERINQ;
    Long STARTINQ;
    Long WAITINQ;
    Long SLEEP;
    String projectName;
    SQLiteJDBC distWH = new SQLiteJDBC();

    public InsertDistributionWareHouse(String Node,
            Integer PID,
            Integer CNO,
            Integer VARTYPE,
            Integer SCHEDULER,
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
            Integer XTC, String Project) {

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
        this.XTC = XTC;
        this.projectName = Project;
        this.ENTERINQ = ENTERINQ;
        this.STARTINQ = STARTINQ;
        this.WAITINQ = WAITINQ;
        this.SLEEP = SLEEP;

    }

    @Override
    public void run() {
        Thread.currentThread().setName("InsertDistDBWHThread");
        String sql = "";
        if (!created) {
            sql = "CREATE TABLE DISTWH"
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
                    + "TIMESTAMP DATE);";
            distWH.createtable("appdb/dw-dist.db", sql);
            distWH.closeConnection();
            created = true;
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
                    + "EXITCODE,TIMESTAMP"
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
                    + "','" + CS.toString()
                    + "','" + LOWL.toString()
                    + "','" + UPL.toString()
                    + "','" + COUNTER.toString()
                    + "','" + Nexec
                    + "','" + CommOH
                    + "','" + ParOH
                    + "','" + ENTERINQ
                    + "','" + STARTINQ
                    + "','" + WAITINQ
                    + "','" + SLEEP
                    + "','" + PRFM
                    + "','" + XTC
                    + "','" + dateFormat.format(date) + "');";
            distWH.insert("appdb/dw-dist.db", sql);
        }
        distWH.closeConnection();

    }

}
