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

    String dbloc, sql, jobToken;
    long stoptime, totaltime, NOH;
    long avgWaitInQ;
    long avgSleepTime;
    double avgload;
    double avgCacheHitMissRatio = 0;
    long avgDownloadData = 0;
    double avgDownloadSpeed = 0;
    int avgReqSent = 0;
    long avgUploadData = 0;
    double avgUploadSpeed = 0;
    int avgReqRecieved = 0;
    long avgCachedData = 0;

    public UpdateResultDBafterExecVirtual(String jobToken, long STOPTIME, long TOTALTIME, long NetOH, double avgload, long avgWaitInQ, long avgSleepTime, double avgCacheHitMissRatio,
            long avgDownloadData,
            double avgDownloadSpeed,
            int avgReqSent,
            long avgUploadData,
            double avgUploadSpeed,
            int avgReqRecieved,
            long avgCachedData) {
        dbloc = "appdb/results.db";
        this.jobToken = jobToken;
        stoptime = STOPTIME;
        totaltime = TOTALTIME;
        NOH = NetOH;
        this.avgload = avgload;
        this.avgSleepTime = avgSleepTime;
        this.avgWaitInQ = avgWaitInQ;

        this.avgCacheHitMissRatio = avgCacheHitMissRatio;
        this.avgDownloadData = avgDownloadData;
        this.avgDownloadSpeed = avgDownloadSpeed;
        this.avgReqSent = avgReqSent;
        this.avgUploadData = avgUploadData;
        this.avgUploadSpeed = avgUploadSpeed;
        this.avgReqRecieved = avgReqRecieved;
        this.avgCachedData = avgCachedData;
    }

    @Override
    public void run() {
        {
            Thread.currentThread().setName("UpdateResultDBaftExecVirtual Started For " + jobToken);

            Result resultDBEntry = RESULT_DB.get(jobToken.trim());
            {
//                System.out.println(" Task "+jobToken+" Finsihed");
                resultDBEntry.setEndTime(stoptime);
                resultDBEntry.setTotalTime(totaltime);
                resultDBEntry.setNetworkOH(NOH);
                resultDBEntry.setAvgLoad(avgload);
                resultDBEntry.setAvgSleeptime(avgSleepTime);
                resultDBEntry.setAvgWaitinq(avgWaitInQ);
                resultDBEntry.setFinished(true);
                resultDBEntry.setStatus("Job Finished");
                resultDBEntry.setAvgCacheHitMissRatio(avgCacheHitMissRatio);
                resultDBEntry.setAvgDownloadData(avgDownloadData);
                resultDBEntry.setAvgDownloadSpeed(avgDownloadSpeed);
                resultDBEntry.setAvgReqSent(avgReqSent);
                resultDBEntry.setAvgUploadData(avgUploadData);
                resultDBEntry.setAvgUploadSpeed(avgUploadSpeed);
                resultDBEntry.setAvgReqRecieved(avgReqRecieved);
                resultDBEntry.setAvgCachedData(avgCachedData);
                {
                    GlobalValues.RESULT_DB_EXECUTOR.submit(new InsertResultWareHouse((resultDBEntry.getJobToken()), resultDBEntry.getJobName(),
                            resultDBEntry.getScheduler(),
                            "" + resultDBEntry.getStarttime(),
                            "" + resultDBEntry.getEndTime(),
                            "" + resultDBEntry.getTotalTime(),
                            "" + resultDBEntry.getNetworkOH(),
                            "" + resultDBEntry.getParsingOH(),
                            "" + resultDBEntry.getChunkSize(),
                            "" + resultDBEntry.getTotalChunks(),
                            "" + resultDBEntry.getTotalNodes(),
                            (resultDBEntry.getAvgLoad()),
                            "" + resultDBEntry.isFinished(),
                            "" + resultDBEntry.getAvgWaitinq(),
                            "" + resultDBEntry.getAvgSleeptime(),
                            avgCacheHitMissRatio,
                            avgDownloadData,
                            avgDownloadSpeed,
                            avgReqSent,
                            avgUploadData,
                            avgUploadSpeed,
                            avgReqRecieved,
                            avgCachedData));
                }
            }

        }
    }

}
