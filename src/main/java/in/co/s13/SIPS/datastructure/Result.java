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

import java.util.Comparator;
import org.json.JSONObject;

public class Result {

    private String jobName;
    private String jobToken;
    private String submitterUUID;
    private long createdOn = Long.MIN_VALUE;
    private long starttime = Long.MIN_VALUE;
    private long endTime = Long.MIN_VALUE;
    private long totalTime = Long.MIN_VALUE;
    private long parsingOH = Long.MIN_VALUE;
    private long networkOH = Long.MIN_VALUE;
    private String chunkSize;
    private int totalChunks = Integer.MIN_VALUE;
    private int totalNodes = Integer.MIN_VALUE;
    private String avgLoad;
    private long avgWaitinq = Long.MIN_VALUE;
    private long avgSleeptime = Long.MIN_VALUE;
    private boolean finished;
    private String scheduler;
    private String status;

    public Result() {
    }
    
    
    public Result(String jobName, String jobToken, String submitterUUID) {
        this.jobName = jobName;
        this.jobToken = jobToken;
        this.submitterUUID = submitterUUID;
        this.status="Created";
        createdOn = System.currentTimeMillis();
        
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String fName) {
        jobName = (fName);
    }

    public String getJobToken() {
        return jobToken;
    }

    public void setJobToken(String fName) {
        jobToken = (fName);
    }

    public String getScheduler() {
        return scheduler;
    }

    public void setScheduler(String fName) {
        scheduler = (fName);
    }

    public String getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(String fName) {
        chunkSize = (fName);
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public String getAvgLoad() {
        return avgLoad;
    }

    public void setAvgLoad(String load) {
        avgLoad = (load);
    }

    public String getSubmitterUUID() {
        return submitterUUID;
    }

    public void setSubmitterUUID(String submitterUUID) {
        this.submitterUUID = submitterUUID;
    }

    public long getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(long createdOn) {
        this.createdOn = createdOn;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getParsingOH() {
        return parsingOH;
    }

    public void setParsingOH(long parsingOH) {
        this.parsingOH = parsingOH;
    }

    public long getNetworkOH() {
        return networkOH;
    }

    public void setNetworkOH(long networkOH) {
        this.networkOH = networkOH;
    }

    public long getAvgWaitinq() {
        return avgWaitinq;
    }

    public void setAvgWaitinq(long avgWaitinq) {
        this.avgWaitinq = avgWaitinq;
    }

    public long getAvgSleeptime() {
        return avgSleeptime;
    }

    public void setAvgSleeptime(long avgSleeptime) {
        this.avgSleeptime = avgSleeptime;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

   
    
    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int indentFactor) {
        return this.toJSON().toString(indentFactor);
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("jobName", jobName);
        result.put("jobToken", jobToken);
        result.put("submitterUUID", submitterUUID);
        result.put("starttime", starttime);
        result.put("endTime", endTime);
        result.put("totalTime", totalTime);
        result.put("parsingOH", parsingOH);
        result.put("networkOH", networkOH);
        result.put("chunkSize", chunkSize);
        result.put("totalChunks", totalChunks);
        result.put("totalNodes", totalNodes);
        result.put("avgLoad", avgLoad);
        result.put("avgWaitinq", avgWaitinq);
        result.put("avgSleeptime", avgSleeptime);
        result.put("finished", finished);
        result.put("scheduler", scheduler);
        result.put("status", status);
        result.put("createdOn", createdOn);
        return result;
    }

    enum ResultComparator implements Comparator<Result> {
        JOB_TOKEN_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getJobToken()).compareTo(o2.getJobToken());
            }
        },
        JOBNAME_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getJobName()).compareTo(o2.getJobName());
            }
        },
        SCHEDULER_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getScheduler()).compareTo(o2.getScheduler());
            }
        },
        STARTTIME_SORT {
            public int compare(Result o1, Result o2) {
                return Long.valueOf(o1.getStarttime()).compareTo(o2.getStarttime());
            }
        },
        CREATEDON_SORT {
            public int compare(Result o1, Result o2) {
                return Long.valueOf(o1.getCreatedOn()).compareTo(o2.getCreatedOn());
            }
        },
        ENDTIME_SORT {
            public int compare(Result o1, Result o2) {
                return Long.valueOf(o1.getEndTime()).compareTo(o2.getEndTime());
            }
        },
        TOTALTIME_SORT {
            public int compare(Result o1, Result o2) {
                return Long.valueOf(o1.getTotalTime()).compareTo(o2.getTotalTime());
            }
        },
        NOH_SORT {
            public int compare(Result o1, Result o2) {
                return Long.valueOf(o1.getNetworkOH()).compareTo(o2.getNetworkOH());
            }
        },
        POH_SORT {
            public int compare(Result o1, Result o2) {
                return Long.valueOf(o1.getParsingOH()).compareTo(o2.getParsingOH());
            }
        },
        CHUNKSIZE_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getChunkSize()).compareTo(o2.getChunkSize());
            }
        },
        TCHUNKS_SORT {
            public int compare(Result o1, Result o2) {
                return Integer.valueOf(o1.getTotalChunks()).compareTo(o2.getTotalChunks());
            }
        },
        TNODES_SORT {
            public int compare(Result o1, Result o2) {
                return Integer.valueOf(o1.getTotalNodes()).compareTo(o2.getTotalNodes());
            }
        },
        PRFM_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getAvgLoad()).compareTo(o2.getAvgLoad());
            }
        },
        FINISHED_SORT {
            public int compare(Result o1, Result o2) {
                return Boolean.valueOf(o1.isFinished()).compareTo(o2.isFinished());
            }
        };

        public static Comparator<Result> decending(final Comparator<Result> other) {
            return (Result o1, Result o2) -> -1 * other.compare(o1, o2);
        }

        public static Comparator<Result> getComparator(final ResultComparator... multipleOptions) {
            return (Result o1, Result o2) -> {
                for (ResultComparator option : multipleOptions) {
                    int result = option.compare(o1, o2);
                    if (result != 0) {
                        return result;
                    }
                }
                return 0;
            };
        }
    }

}
