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
package in.co.s13.SIPS.datastructure;

import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class JobDBRow {

    private String jobToken, projectName, submitterUUID;
    private boolean finished=false;
    private long createdAt,startedAt,finishedAt,finishedAgo;
    public JobDBRow(String jobToken, String projectName, String submitterUUID) {
        this.jobToken = jobToken;
        this.projectName = projectName;
        this.submitterUUID = submitterUUID;
    }

    public String getJobToken() {
        return jobToken;
    }

    public void setJobToken(String jobToken) {
        this.jobToken = jobToken;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSubmitterUUID() {
        return submitterUUID;
    }

    public void setSubmitterUUID(String SubmitterUUID) {
        this.submitterUUID = SubmitterUUID;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }


    @Override
    public String toString() {
        return this.toJSON().toString(4);
    }

    public JSONObject toJSON() {
        JSONObject taskDBRow = new JSONObject();
        taskDBRow.put("taskID", jobToken);
        taskDBRow.put("projectName", projectName);
        taskDBRow.put("SubmitterUUID", submitterUUID);
        taskDBRow.put("Finished", finished);
        return taskDBRow;
    }

}
