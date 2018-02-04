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

    String projectName, jobToken, submitterUUID;
    String Scheduler = "";

    public UpdateResultDBbefExecVirtual(String projectName, String jobToken, String scheduler, String submitterUUID) {
        this.projectName = projectName;
        this.jobToken = jobToken;
        Scheduler = scheduler;
        this.submitterUUID = submitterUUID;
    }

    @Override
    public void run() {
        Result res = new Result(projectName, jobToken, submitterUUID);
        res.setScheduler(Scheduler);
        res.setFinished(false);
        GlobalValues.RESULT_DB.put(jobToken, res);
    }

}
