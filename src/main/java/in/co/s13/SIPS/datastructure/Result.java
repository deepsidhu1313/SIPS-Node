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

public class Result {

    private String jobName;
    private String jobToken;
    private String starttime;
    private String endTime;
    private String totalTime;
    private String parsingOH;
    private String networkOH;
    private String chunkSize;
    private String totalChunks;
    private String totalNodes;
    private String avgLoad;
    private String avgWaitinq;
    private String avgSleeptime;
    private String finished;
    private String scheduler;

    public Result(String fName, String jobToken, String scheduler, String startTime, String EndTime,
            String TotalTime, String NOH, String ParsingOH,
            String CHUNKSIZE, String TCHUNK, String TNODES,
            String cpuload, String avgSleeptime, String avgWaitinq, String FINISHED) {
        this.jobName = fName;
        this.jobToken = jobToken;
        this.scheduler = scheduler;
        this.starttime = startTime;
        this.endTime = EndTime;
        this.totalTime = TotalTime;
        this.networkOH = NOH;
        this.parsingOH = ParsingOH;
        this.chunkSize = CHUNKSIZE;
        this.totalChunks = TCHUNK;
        this.totalNodes = TNODES;
        this.avgLoad = cpuload;
        this.finished = FINISHED;
        this.avgWaitinq = avgWaitinq;
        this.avgSleeptime = avgSleeptime;
    }

    public String getFileName() {
        return jobName;
    }

    public void setFileName(String fName) {
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

    public String getStartTime() {
        return starttime;
    }

    public void setStartTime(String fName) {
        starttime = (fName);
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String fName) {
        endTime = (fName);
    }

    public String getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(String fName) {
        totalTime = (fName);
    }

    public String getNetworkOH() {
        return networkOH;
    }

    public void setNetworkOH(String fName) {
        networkOH = (fName);
    }

    public String getParsingOH() {
        return this.parsingOH;
    }

    public void setParsingOH(String p) {
        this.parsingOH = (p);
    }

    public String getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(String fName) {
        chunkSize = (fName);
    }

    public String getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(String fName) {
        totalChunks = (fName);
    }

    public String getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(String fName) {
        totalNodes = (fName);
    }

    public String getAvgWaitinq() {
        return avgWaitinq;
    }

    public void setAvgWaitinq(String fName) {
        avgWaitinq = (fName);
    }

    public String getAvgSleeptime() {
        return avgSleeptime;
    }

    public void setAvgSleeptime(String fName) {
        avgSleeptime = (fName);
    }

    public String getAvgLoad() {
        return avgLoad;
    }

    public void setAvgLoad(String l) {
        avgLoad = (l);
    }

    public String getFinished() {
        return finished;
    }

    public void setFinished(String fName) {
        finished = (fName);
    }

    enum ResultComparator implements Comparator<Result> {
        PID_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getJobToken()).compareTo(o2.getJobToken());
            }
        },
        FILENAME_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getFileName()).compareTo(o2.getFileName());
            }
        },
        SCHEDULER_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getScheduler()).compareTo(o2.getScheduler());
            }
        },
        STARTTIME_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getStartTime()).compareTo(o2.getStartTime());
            }
        },
        ENDTIME_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getEndTime()).compareTo(o2.getEndTime());
            }
        },
        TOTALTIME_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getTotalTime()).compareTo(o2.getTotalTime());
            }
        },
        NOH_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getNetworkOH()).compareTo(o2.getNetworkOH());
            }
        },
        POH_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getParsingOH()).compareTo(o2.getParsingOH());
            }
        },
        CHUNKSIZE_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getChunkSize()).compareTo(o2.getChunkSize());
            }
        },
        TCHUNKS_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getTotalChunks()).compareTo(o2.getTotalChunks());
            }
        },
        TNODES_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getTotalNodes()).compareTo(o2.getTotalNodes());
            }
        },
        PRFM_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getAvgLoad()).compareTo(o2.getAvgLoad());
            }
        },
        FINISHED_SORT {
            public int compare(Result o1, Result o2) {
                return (o1.getFinished()).compareTo(o2.getFinished());
            }
        };

        public static Comparator<Result> decending(final Comparator<Result> other) {
            return new Comparator<Result>() {
                public int compare(Result o1, Result o2) {
                    return -1 * other.compare(o1, o2);
                }
            };
        }

        public static Comparator<Result> getComparator(final ResultComparator... multipleOptions) {
            return new Comparator<Result>() {
                public int compare(Result o1, Result o2) {
                    for (ResultComparator option : multipleOptions) {
                        int result = option.compare(o1, o2);
                        if (result != 0) {
                            return result;
                        }
                    }
                    return 0;
                }
            };
        }
    }

}