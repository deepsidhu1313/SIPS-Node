/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package in.co.s13.SIPS.datastructure;

import java.util.Comparator;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
 public  class Result {

        private SimpleStringProperty fileName;
        private SimpleStringProperty PID;
        private SimpleStringProperty starttime;
        private SimpleStringProperty endTime;
        private SimpleStringProperty totalTime;
        private SimpleStringProperty parsingOH;
        private SimpleStringProperty networkOH;
        private SimpleStringProperty chunkSize;
        private SimpleStringProperty totalChunks;
        private SimpleStringProperty totalNodes;
        private SimpleStringProperty avgLoad;
        private SimpleStringProperty avgWaitinq;
        private SimpleStringProperty avgSleeptime;
        private SimpleStringProperty finished;
        private SimpleIntegerProperty scheduler;

        public Result(String fName, String id,int scheduler, String startTime, String EndTime,
                String TotalTime, String NOH, String ParsingOH,
                String CHUNKSIZE, String TCHUNK, String TNODES, 
                String cpuload,String avgSleeptime,String avgWaitinq, String FINISHED) {
            this.fileName = new SimpleStringProperty(fName);
            this.PID = new SimpleStringProperty(id);
            this.scheduler=new SimpleIntegerProperty(scheduler);
            this.starttime = new SimpleStringProperty(startTime);
            this.endTime = new SimpleStringProperty(EndTime);
            this.totalTime = new SimpleStringProperty(TotalTime);
            this.networkOH = new SimpleStringProperty(NOH);
            this.parsingOH = new SimpleStringProperty(ParsingOH);
            this.chunkSize = new SimpleStringProperty(CHUNKSIZE);
            this.totalChunks = new SimpleStringProperty(TCHUNK);
            this.totalNodes = new SimpleStringProperty(TNODES);
            this.avgLoad = new SimpleStringProperty(cpuload);
            this.finished = new SimpleStringProperty(FINISHED);
            this.avgWaitinq= new SimpleStringProperty(avgWaitinq);
            this.avgSleeptime= new SimpleStringProperty(avgSleeptime);
        }

        public String getFileName() {
            return fileName.get();
        }

        public void setFileName(String fName) {
            fileName.set(fName);
        }

        public String getPID() {
            return PID.get();
        }

        public void setPID(String fName) {
            PID.set(fName);
        }

        
        public int getScheduler() {
            return scheduler.get();
        }

        public void setScheduler(int fName) {
            scheduler.set(fName);
        }
        
        public String getStartTime() {
            return starttime.get();
        }

        public void setStartTime(String fName) {
            starttime.set(fName);
        }

        public String getEndTime() {
            return endTime.get();
        }

        public void setEndTime(String fName) {
            endTime.set(fName);
        }

        public String getTotalTime() {
            return totalTime.get();
        }

        public void setTotalTime(String fName) {
            totalTime.set(fName);
        }

        public String getNetworkOH() {
            return networkOH.get();
        }

        public void setNetworkOH(String fName) {
            networkOH.set(fName);
        }

        public String getParsingOH() {
            return this.parsingOH.get();
        }

        public void setParsingOH(String p) {
            this.parsingOH.set(p);
        }

        public String getChunkSize() {
            return chunkSize.get();
        }

        public void setChunkSize(String fName) {
            chunkSize.set(fName);
        }

        public String getTotalChunks() {
            return totalChunks.get();
        }

        public void setTotalChunks(String fName) {
            totalChunks.set(fName);
        }

        public String getTotalNodes() {
            return totalNodes.get();
        }

        public void setTotalNodes(String fName) {
            totalNodes.set(fName);
        }

         public String getAvgWaitinq() {
            return avgWaitinq.get();
        }

        public void setAvgWaitinq(String fName) {
            avgWaitinq.set(fName);
        }
        
         public String getAvgSleeptime() {
            return avgSleeptime.get();
        }

        public void setAvgSleeptime(String fName) {
            avgSleeptime.set(fName);
        }
        
        public String getAvgLoad() {
            return avgLoad.get();
        }

        public void setAvgLoad(String l) {
            avgLoad.set(l);
        }

        public String getFinished() {
            return finished.get();
        }

        public void setFinished(String fName) {
            finished.set(fName);
        }

         enum ResultComparator implements Comparator<Result> {
        PID_SORT {
                    public int compare(Result o1, Result o2) {
                        return (o1.getPID()).compareTo(o2.getPID());
                    }
                },
        FILENAME_SORT {
                    public int compare(Result o1, Result o2) {
                        return (o1.getFileName()).compareTo(o2.getFileName());
                    }
                },
        SCHEDULER_SORT {
                    public int compare(Result o1, Result o2) {
                        return Integer.valueOf(o1.getScheduler()).compareTo(o2.getScheduler());
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