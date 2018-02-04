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
import in.co.s13.SIPS.settings.GlobalValues;

/**
 *
 * @author Nika
 */
public class UpdateResultDBbefExecVirtual implements Runnable {

    String dbloc, sql, projectName, chunksize, jobToken,  submitterUUID;
    Long startTime;
    String Scheduler = "";
    long poh;
    int  tchunks,tnodes;

    public UpdateResultDBbefExecVirtual(String projectName, String jobToken,  String scheduler,String submitterUUID) {
        dbloc = "appdb/results.db";
        this.projectName = projectName;
        this.jobToken = jobToken;
        Scheduler = scheduler;
        this.submitterUUID=submitterUUID;
    }

    @Override
    public void run() {
        // SQLiteJDBC db = new SQLiteJDBC();
//        sql = "INSERT INTO RESULT "
//                + "("
//                + "JOBTOKEN ,Projectname,"
//                + "SCHEDULER ,"
//                + "StartTime ,"
//                + "POH ,"
//                + "CHUNKSIZE ,"
//                + "TCHUNK ,"
//                + "TNODES ,"
//                + "FINISHED)"
//                + " VALUES ('" + jobToken + "','" + projectName + "','" + Scheduler + "','" + startTime + "','" + poh + "','" + chunksize + "','" + tchunks + "','" + tnodes + "','false');";
        //db.insert(dbloc, sql);
        Result res=new Result(projectName, jobToken,submitterUUID);
        res.setScheduler(Scheduler);
        //res.setStarttime(startTime);
        //res.setParsingOH(poh);
        //res.setChunkSize(chunksize);
        //res.setTotalChunks(tchunks);
        //res.setTotalNodes(tnodes);
        res.setFinished(false);
        GlobalValues.RESULT_DB.put(jobToken, res);
        /*  sql = "UPDATE  RESULT set "
         + "PID ='"+jobToken+"',"
         + " StartTime ='"+startTime+"',"
         + " POH ='"+poh+"',"
         + " CHUNKSIZE ='"+chunksize+"' ,"
         + " TCHUNK ='"+tchunks+"',"
         + " TNODES ='"+tnodes+"' WHERE Filename='"+projectName+"' ;";
         db.Update(dbloc, sql);
      
         */
        // db.closeConnection();

    }

}
