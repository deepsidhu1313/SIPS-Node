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

    String dbloc, sql, jobToken, performance; 
    long stoptime, totaltime, NOH;
    long avgWaitInQ;
    long avgSleepTime;

    public UpdateResultDBafterExecVirtual(String jobToken, long STOPTIME, long TOTALTIME, long NetOH, String PERFORM, long avgWaitInQ, long avgSleepTime) {
        dbloc = "appdb/results.db";
        this.jobToken = jobToken;
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
                + " FINISHED ='true' WHERE PID='" + jobToken + "' ;";
        // db.Update(dbloc, sql);
//        for () 
        {
            Result resultDBEntry = RESULT_DB.get(jobToken.trim());
//        if (resultDBEntry.getPID().trim().equalsIgnoreCase())
            {
                resultDBEntry.setEndTime(stoptime);
                resultDBEntry.setTotalTime(totaltime);
                resultDBEntry.setNetworkOH(NOH);
                resultDBEntry.setAvgLoad(performance);
                resultDBEntry.setAvgSleeptime(avgSleepTime);
                resultDBEntry.setAvgWaitinq(avgWaitInQ);
                resultDBEntry.setFinished(true);
//                if (!insertedResultIntoWH[Integer.parseInt(jobToken)]) 
                {
                    GlobalValues.RESULT_DB_EXECUTOR.execute(new InsertResultWareHouse((resultDBEntry.getJobToken()), resultDBEntry.getJobName(),
                            resultDBEntry.getScheduler(),
                            "" + resultDBEntry.getStarttime(),
                            "" + resultDBEntry.getEndTime(),
                            "" + resultDBEntry.getTotalTime(),
                            "" + resultDBEntry.getNetworkOH(),
                            "" + resultDBEntry.getParsingOH(),
                            "" + resultDBEntry.getChunkSize(),
                            "" + resultDBEntry.getTotalChunks(),
                            "" + resultDBEntry.getTotalNodes(),
                            Double.parseDouble(resultDBEntry.getAvgLoad()),
                            "" + resultDBEntry.isFinished(), "" + resultDBEntry.getAvgWaitinq(), "" + resultDBEntry.getAvgSleeptime()));
//                    insertedResultIntoWH[Integer.parseInt(jobToken)] = true;
                }
//                break;
            }

        }
        // db.closeConnection();
        //    if(Integer.parseInt(jobToken.trim())<1)
//        {
//        Thread t = new Thread(new createResultTable());
//        t.start();
//        }  
//        
    }

}
