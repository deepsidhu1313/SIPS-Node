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
package in.co.s13.SIPS.virtualdb;

import in.co.s13.SIPS.datastructure.DistributionDBRow;
import in.co.s13.SIPS.db.SQLiteJDBC;
import java.io.File;
import java.util.ArrayList;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Nika
 */
public class createDistributionDBVirtual implements Runnable {

    String PID;
    ArrayList<String> ip = new ArrayList<>();
    ArrayList<String> startTime = new ArrayList<>();
    ArrayList<String> poh = new ArrayList<>();
    ArrayList<String> cno = new ArrayList<>();
    ArrayList<String> chunksize = new ArrayList<>();
    ArrayList<String> low = new ArrayList<>();
    ArrayList<String> up = new ArrayList<>();
    String dbloc;
    SQLiteJDBC db = new SQLiteJDBC();
    int counter = 0;
    int vartype = 0;
    String Scheduler = "";
    ConcurrentHashMap<String, DistributionDBRow> DistDBTable = new ConcurrentHashMap<>();

    public createDistributionDBVirtual(String fileName, String pid, ArrayList IP, ArrayList starttime, ArrayList parsingoverhead, ArrayList chunksze, int vart, ArrayList lower, ArrayList upper, String scheduler) {
        dbloc = "data/" + pid + "/dist-db/dist-" + pid + ".db";
        PID = pid;
        ip = IP;
        startTime = starttime;
        poh = parsingoverhead;
        chunksize = chunksze;
        vartype = vart;
        low = lower;
        up = upper;
        Scheduler = scheduler;
        File df = new File(dbloc).getParentFile();
        if (!df.exists()) {
            df.mkdirs();
        }
    }

    @Override
    public void run() {
        File f = new File(dbloc);
        if (f.exists()) {
            f.delete();
        }

        /*    String sql = "CREATE TABLE DIST "
         + "(ID INT PRIMARY KEY     NOT NULL,"
         + " IP TEXT Not Null,"
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
         + "PRFM DOUBLE,"
         + "EXITCODE INT"
         + ");";

         // db.createtable(dbloc, sql);
         // db.closeConnection();
         */
        for (int i = 0; i < ip.size(); i++) {
            /*    sql = "INSERT INTO DIST (ID , IP , PID ,"
             + "CNO,"
             + "VARTYPE ,"
             + "SCHEDULER ,"
             + "LStartTime ,"
             + "LEndTime ,"
             + "LExcTime ,"
             + "CHUNKSIZE ,"
             + "LOWLIMIT ,"
             + "UPLIMIT ,"
             + "COUNTER ,"
             + "NExecutionTime ,"
             + "NOH ,"
             + "POH ,"
             + "PRFM ,"
             + "EXITCODE "
             + ")"
             + " VALUES ('"
             + i + "','"
             + ip.get(i) + "','"
             + PID + "','"
             + i + "','"
             + vartype + "','"
             + Scheduler + "','"
             + startTime.get(i)
             + "','0','0','"
             + chunksize.get(i) + "','"
             + low.get(i) + "','"
             + up.get(i) + "','"
             + "0','0','0','"
             + poh.get(i) + "','"
             + "0','9999');";
             //     db.insert(dbloc, sql);
             */ DistDBTable.put(ip.get(i) + "-" + i, new DistributionDBRow(i, ip.get(i), (PID.trim()),
                    i, vartype, Scheduler, Long.parseLong(startTime.get(i)),
                    0, 0, 0, 0,
                    Long.parseLong(poh.get(i)), 0, 0, 0, 0,
                    (chunksize.get(i)),
                    (low.get(i)), (up.get(i)), "0", 0, 9999));

        }
        System.out.println("Creating DIST DB :" + PID);
        MASTER_DIST_DB.put("" + Integer.parseInt(PID), DistDBTable);
        System.out.println("Added DIST DB :" + PID);
//        if (Integer.parseInt(PID.trim()) < 1) {
//            Thread t = new Thread(new distDBBrowser());
//            t.start();
//        }

        // db.closeConnection();
    }

}
