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

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class DistributionDBRow {

    private Integer id;
    private String ip;
    private Double prfm;
    private Integer cno, vartype, exitcode;
    private Long lstarttime, lendtime, lexctime, nexecutiontime, noh, poh, entrinq, startinq, waitinq, sleeptime;
    private String pid, chunksize, lowlimit, scheduler, uplimit, counter;

    public DistributionDBRow(int id, String ip, String pid, int cno, int vartype, String scheduler,
            long lstarttime, long lendtime, long lexctime, long nexecutiontime, long noh, long poh,
            long entrinq, long startinq, long waitinq, long sleeptime,
            String chunksize, String lowlimit, String uplimit, String counter, double prfm, int exitcode) {
        this.id = (id);
        this.ip = (ip);
        this.pid = (pid);
        this.cno = (cno);
        this.vartype = (vartype);
        this.scheduler = (scheduler);
        this.lstarttime = (lstarttime);
        this.lendtime = (lendtime);
        this.lexctime = (lexctime);
        this.nexecutiontime = (nexecutiontime);
        this.noh = (noh);
        this.poh = (poh);
        this.chunksize = (chunksize);
        this.lowlimit = (lowlimit);
        this.uplimit = (uplimit);
        this.counter = (counter);
        this.prfm = (prfm);
        this.exitcode = (exitcode);
        this.entrinq = (entrinq);
        this.startinq = (startinq);
        this.waitinq = (waitinq);
        this.sleeptime = (sleeptime);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = (id);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = (ip);
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = (pid);
    }

    public int getCno() {
        return cno;
    }

    public void setCno(int cno) {
        this.cno = (cno);
    }

    public int getVartype() {
        return vartype;
    }

    public void setVartype(int vartype) {
        this.vartype = (vartype);
    }

    public String getScheduler() {
        return scheduler;
    }

    public void setScheduler(String scheduler) {
        this.scheduler = scheduler;
    }

    public long getLstarttime() {
        return lstarttime;
    }

    public void setLstarttime(long lstarttime) {

        this.lstarttime = (lstarttime);
    }

    public long getLendtime() {
        return lendtime;
    }

    public void setLendtime(long lendtime) {
        this.lendtime = (lendtime);
    }

    public long getLexctime() {
        return lexctime;
    }

    public void setLexctime(long lexctime) {
        this.lexctime = (lexctime);
    }

    public long getNexecutiontime() {
        return nexecutiontime;
    }

    public void setNexecutiontime(long nexecutiontime) {
        this.nexecutiontime = (nexecutiontime);
    }

    public long getNoh() {
        return noh;
    }

    public void setNoh(long noh) {
        this.noh = (noh);
    }

    public long getPoh() {
        return poh;
    }

    public void setPoh(long poh) {
        this.poh = (poh);
    }

    public void setStartinq(long poh) {
        this.startinq = (poh);
    }

    public long getStartinq() {
        return startinq;
    }

    public void setEntrinq(long poh) {
        this.entrinq = (poh);
    }

    public long getEntrinq() {
        return entrinq;
    }

    public void setWaitinq(long poh) {
        this.waitinq = (poh);
    }

    public long getWaitinq() {
        return waitinq;
    }

    public void setSleeptime(long poh) {
        this.sleeptime = (poh);
    }

    public long getSleeptime() {
        return sleeptime;
    }

    public String getChunksize() {
        return chunksize;
    }

    public void setChunksize(String chunksize) {
        this.chunksize = (chunksize);
    }

    public String getLowlimit() {
        return lowlimit;
    }

    public void setLowlimit(String lowlimit) {
        this.lowlimit = (lowlimit);
    }

    public String getUplimit() {
        return uplimit;
    }

    public void setUplimit(String uplimit) {
        this.uplimit = (uplimit);
    }

    public String getCounter() {
        return counter;
    }

    public void setCounter(String counter) {
        this.counter = (counter);
    }

    public double getPrfm() {
        return prfm;
    }

    public void setPrfm(double prfm) {
        this.prfm = (prfm);
    }

    public int getExitcode() {
        return exitcode;
    }

    public void setExitcode(int exitcode) {
        this.exitcode = (exitcode);
    }

    @Override
    public String toString() {
        return "" + id + "\t" + ip + "\t" + pid + "\t" + cno + "\t"
                + vartype + "\t" + scheduler + "\t" + lstarttime + "\t" + lendtime + "\t"
                + lexctime + "\t" + nexecutiontime + "\t" + noh + "\t" + poh + "\t" + chunksize + "\t"
                + lowlimit + "\t" + uplimit + "\t" + counter + "\t" + prfm + "\t" + exitcode + "\n";

    }

    public enum DistributionDBRowComparator implements Comparator<DistributionDBRow> {

        ID_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Integer.valueOf(o1.getId()).compareTo(o2.getId());
            }
        },
        IP_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getIp().compareTo(o2.getIp());
            }
        },
        PID_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return (o1.getPid()).compareTo(o2.getPid());
            }
        },
        CNO_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Integer.valueOf(o1.getCno()).compareTo(o2.getCno());
            }
        },
        VARTYPE_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Integer.valueOf(o1.getVartype()).compareTo(o2.getVartype());
            }
        },
        SCHEDULER_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Integer.valueOf(o1.getVartype()).compareTo(o2.getVartype());
            }
        },
        LSTARTTIME_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Long.valueOf(o1.getLstarttime()).compareTo(o2.getLstarttime());
            }
        },
        LENDTIME_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Long.valueOf(o1.getLendtime()).compareTo(o2.getLendtime());
            }
        },
        LEXCTIME_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Long.valueOf(o1.getLexctime()).compareTo(o2.getLexctime());
            }
        },
        NEXECUTIONTIME_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Long.valueOf(o1.getNexecutiontime()).compareTo(o2.getNexecutiontime());
            }
        },
        NOH_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Long.valueOf(o1.getNoh()).compareTo(o2.getNoh());
            }
        },
        POH_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Long.valueOf(o1.getPoh()).compareTo(o2.getPoh());
            }
        },
        CHUNKSIZE_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getChunksize().compareTo(o2.getChunksize());
            }
        },
        LOWLIMIT_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getLowlimit().compareTo(o2.getLowlimit());
            }
        },
        UPLIMIT_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getUplimit().compareTo(o2.getUplimit());
            }
        },
        COUNTER_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getCounter().compareTo(o2.getCounter());
            }
        },
        PRFM_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Double.valueOf(o1.getPrfm()).compareTo(o2.getPrfm());
            }
        },
        EXITCODE_SORT {
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return Integer.valueOf(o1.getExitcode()).compareTo(o2.getExitcode());
            }
        };

        public static Comparator<DistributionDBRow> decending(final Comparator<DistributionDBRow> other) {
            return new Comparator<DistributionDBRow>() {
                public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                    return -1 * other.compare(o1, o2);
                }
            };
        }

        public static Comparator<DistributionDBRow> getComparator(final DistributionDBRowComparator... multipleOptions) {
            return new Comparator<DistributionDBRow>() {
                public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                    for (DistributionDBRowComparator option : multipleOptions) {
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
