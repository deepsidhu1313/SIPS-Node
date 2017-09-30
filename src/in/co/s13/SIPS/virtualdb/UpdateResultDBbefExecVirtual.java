/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.virtualdb;

import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.settings.GlobalValues;
/**
 *
 * @author Nika
 */
public class UpdateResultDBbefExecVirtual implements Runnable {

    String dbloc, sql, fname, chunksize, pid, tchunks, tnodes, poh;
    Long startTime;
int Scheduler=0;
    public UpdateResultDBbefExecVirtual(String filename, String PID, Long Starttime, String ChunkSize, String TotalChunks, String TotalNodes, String ParsingOH,int schedule) {
        dbloc = "appdb/results.db";
        startTime = Starttime;
        fname = filename;
        chunksize = ChunkSize;
        tchunks = TotalChunks;
        tnodes = TotalNodes;
        poh = ParsingOH;
        pid = PID;
        Scheduler=schedule;
    }

    @Override
    public void run() {
       // SQLiteJDBC db = new SQLiteJDBC();
        sql = "INSERT INTO RESULT "
                + "("
                + "PID ,Filename,"
                + "SCHEDULER ,"
                + "StartTime ,"
                + "POH ,"
                + "CHUNKSIZE ,"
                + "TCHUNK ,"
                + "TNODES ,"
                + "FINISHED)"
                + " VALUES ('" + pid + "','" + fname + "','" + Scheduler + "','" + startTime + "','" + poh + "','" + chunksize + "','" + tchunks + "','" + tnodes + "','false');";
        //db.insert(dbloc, sql);
        GlobalValues.RESULT_DB.put(pid,new Result(fname, pid, Scheduler, ""+startTime, "", "", "", poh, chunksize, tchunks,tnodes, "","","", "false"));
        /*  sql = "UPDATE  RESULT set "
         + "PID ='"+pid+"',"
         + " StartTime ='"+startTime+"',"
         + " POH ='"+poh+"',"
         + " CHUNKSIZE ='"+chunksize+"' ,"
         + " TCHUNK ='"+tchunks+"',"
         + " TNODES ='"+tnodes+"' WHERE Filename='"+fname+"' ;";
         db.Update(dbloc, sql);
      
         */
        // db.closeConnection();
      
        
    }

}
