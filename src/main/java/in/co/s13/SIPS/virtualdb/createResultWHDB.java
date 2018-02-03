/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.virtualdb;

import in.co.s13.SIPS.db.SQLiteJDBC;
import java.io.File;

/**
 *
 * @author Nika
 */
public class createResultWHDB implements Runnable {

    String dbloc;
    SQLiteJDBC db = new SQLiteJDBC();


    public createResultWHDB() {
        dbloc = "appdb/results.db";
    }

    @Override
    public void run() {
        File f = new File(dbloc);

        String sql = "CREATE TABLE RESULT "
                + "(ID INT PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "Jobname TEXT,"
                + "JobToken TEXT,"
                + "SCHEDULER TEXT,"
                + "StartTime LONG,"
                + "EndTime LONG,"
                + "TotalTime LONG,"
                + "NOH LONG,"
                + "POH LONG,"
                + "CHUNKSIZE TEXT,"
                + "TCHUNK INT,"
                + "TNODES INT,"
                + "PRFM DOUBLE,"
                + "FINISHED BOOLEAN"
                + ");";

        if (!f.exists()) {
            db.createtable(dbloc, sql);
            db.closeConnection();
        }
    }

}
