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
                            "" + resultDBEntry.getFinished(), "" + resultDBEntry.getAvgWaitinq(), "" + resultDBEntry.getAvgSleeptime()));
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
