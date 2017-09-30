/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.virtualdb;

import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.db.InsertResultWareHouse;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.RESULT_DB;


/**
 *
 * @author Nika
 */
public class UpdateResultDBafterExecVirtual implements Runnable {
    
    String dbloc, sql, PID, stoptime, totaltime, NOH, performance;
    String avgWaitInQ;
    String avgSleepTime;
    
    public UpdateResultDBafterExecVirtual(String pid, String STOPTIME, String TOTALTIME, String NetOH, String PERFORM, String avgWaitInQ, String avgSleepTime) {
        dbloc = "appdb/results.db";
        PID = pid;
        stoptime = STOPTIME;
        totaltime = TOTALTIME;
        NOH = NetOH;
        performance = PERFORM;
        this.avgSleepTime = avgSleepTime;
        this.avgWaitInQ = avgWaitInQ;
    }
    
    @Override
    public void run() {
        // SQLiteJDBC db = new SQLiteJDBC();
        sql = "UPDATE  RESULT set "
                + " EndTime ='" + stoptime + "',"
                + " TotalTime ='" + totaltime + "' ,"
                + " NOH ='" + NOH + "',"
                + " PRFM ='" + performance + "',"
                + " FINISHED ='true' WHERE PID='" + PID + "' ;";
        // db.Update(dbloc, sql);
//        for () 
        {
        Result resultDBEntry = RESULT_DB.get(PID.trim());
//        if (resultDBEntry.getPID().trim().equalsIgnoreCase())
        {
                resultDBEntry.setEndTime(stoptime);
                resultDBEntry.setTotalTime(totaltime);
                resultDBEntry.setNetworkOH(NOH);
                resultDBEntry.setAvgLoad(performance);
                resultDBEntry.setAvgSleeptime(avgSleepTime);
                resultDBEntry.setAvgWaitinq(avgWaitInQ);
                resultDBEntry.setFinished("true");
//                if (!insertedResultIntoWH[Integer.parseInt(PID)]) 
                {
                    GlobalValues.RESULT_DB_EXECUTOR.execute(new InsertResultWareHouse(Integer.parseInt(resultDBEntry.getPID()), resultDBEntry.getFileName(),
                            resultDBEntry.getScheduler(),
                            "" + resultDBEntry.getStartTime(),
                            "" + resultDBEntry.getEndTime(),
                            "" + resultDBEntry.getTotalTime(),
                            "" + resultDBEntry.getNetworkOH(),
                            "" + resultDBEntry.getParsingOH(),
                            "" + resultDBEntry.getChunkSize(),
                            "" + resultDBEntry.getTotalChunks(),
                            "" + resultDBEntry.getTotalNodes(),
                            Double.parseDouble(resultDBEntry.getAvgLoad()),
                            "" + resultDBEntry.getFinished(),""+resultDBEntry.getAvgWaitinq(),""+resultDBEntry.getAvgSleeptime()));
//                    insertedResultIntoWH[Integer.parseInt(PID)] = true;
                }
//                break;
            }
            
        }
        // db.closeConnection();
    //    if(Integer.parseInt(PID.trim())<1)
//        {
//        Thread t = new Thread(new createResultTable());
//        t.start();
//        }  
//        
    }
    
}
