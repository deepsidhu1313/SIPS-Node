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

import java.text.SimpleDateFormat;
import java.util.Date;

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
        this.EXITCODE = XTC;
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
                    + "','" + EXITCODE
                    + "','" + dateFormat.format(date) + "');";
            distWH.insert("appdb/dw-dist.db", sql);
        }
        distWH.closeConnection();

    }

}
