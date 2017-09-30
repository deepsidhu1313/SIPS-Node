/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.datastructure;

import java.util.Comparator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class DistributionDBRow {

    private SimpleIntegerProperty id;
    private SimpleStringProperty ip;
    private SimpleDoubleProperty prfm;
    private SimpleIntegerProperty pid, cno, vartype, scheduler, exitcode;
    private SimpleLongProperty lstarttime, lendtime, lexctime, nexecutiontime, noh, poh,entrinq,startinq,waitinq,sleeptime;
    private SimpleStringProperty chunksize, lowlimit, uplimit, counter;

    public DistributionDBRow(int id, String ip, int pid, int cno, int vartype, int scheduler,
            long lstarttime, long lendtime, long lexctime, long nexecutiontime, long noh, long poh,
            long entrinq,long startinq,long waitinq,long sleeptime,
            String chunksize, String lowlimit, String uplimit, String counter, double prfm, int exitcode) {
        this.id = new SimpleIntegerProperty(id);
        this.ip = new SimpleStringProperty(ip);
        this.pid = new SimpleIntegerProperty(pid);
        this.cno = new SimpleIntegerProperty(cno);
        this.vartype = new SimpleIntegerProperty(vartype);
        this.scheduler = new SimpleIntegerProperty(scheduler);
        this.lstarttime = new SimpleLongProperty(lstarttime);
        this.lendtime = new SimpleLongProperty(lendtime);
        this.lexctime =new SimpleLongProperty( lexctime);
        this.nexecutiontime =new SimpleLongProperty( nexecutiontime);
        this.noh = new SimpleLongProperty(noh);
        this.poh = new SimpleLongProperty(poh);
        this.chunksize = new SimpleStringProperty(chunksize);
        this.lowlimit = new SimpleStringProperty(lowlimit);
        this.uplimit = new SimpleStringProperty(uplimit);
        this.counter = new SimpleStringProperty(counter);
        this.prfm = new SimpleDoubleProperty(prfm);
        this.exitcode = new SimpleIntegerProperty(exitcode);
         this.entrinq=new SimpleLongProperty(entrinq);
         this.startinq=new SimpleLongProperty(startinq);
         this.waitinq=new SimpleLongProperty(waitinq);
         this.sleeptime=new SimpleLongProperty(sleeptime);
    }

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getIp() {
        return ip.get();
    }

    public void setIp(String ip) {
        this.ip.set(ip);
    }

    public int getPid() {
        return pid.get();
    }

    public void setPid(int pid) {
        this.pid.set(pid);
    }

    public int getCno() {
        return cno.get();
    }

    public void setCno(int cno) {
        this.cno.set(cno);
    }

    public int getVartype() {
        return vartype.get();
    }

    public void setVartype(int vartype) {
        this.vartype.set(vartype);
    }

    public int getScheduler() {
        return scheduler.get();
    }

    public void setScheduler(int scheduler) {
        this.scheduler.set(scheduler);
    }

    public long getLstarttime() {
        return lstarttime.get();
    }

    public void setLstarttime(long lstarttime) {

        this.lstarttime.set(lstarttime);
    }

    public long getLendtime() {
        return lendtime.get();
    }

    public void setLendtime(long lendtime) {
        this.lendtime.set(lendtime);
    }

    public long getLexctime() {
        return lexctime.get();
    }

    public void setLexctime(long lexctime) {
        this.lexctime.set(lexctime);
    }

    public long getNexecutiontime() {
        return nexecutiontime.get();
    }

    public void setNexecutiontime(long nexecutiontime) {
        this.nexecutiontime.set(nexecutiontime);
    }

    public long getNoh() {
        return noh.get();
    }

    public void setNoh(long noh) {
        this.noh.set(noh);
    }

    public long getPoh() {
        return poh.get();
    }

    public void setPoh(long poh) {
        this.poh.set(poh);
    }

    
    
    
    public void setStartinq(long poh) {
        this.startinq.set(poh);
    }

    public long getStartinq() {
        return startinq.get();
    }
    
    
    public void setEntrinq(long poh) {
        this.entrinq.set(poh);
    }

    public long getEntrinq() {
        return entrinq.get();
    }
    
    
    public void setWaitinq(long poh) {
        this.waitinq.set(poh);
    }

    public long getWaitinq() {
        return waitinq.get();
    }
    
    public void setSleeptime(long poh) {
        this.sleeptime.set(poh);
    }

    public long getSleeptime() {
        return sleeptime.get();
    }
    
    
    
    
    public String getChunksize() {
        return chunksize.get();
    }

    
    
    
    public void setChunksize(String chunksize) {
        this.chunksize.set(chunksize);
    }

    public String getLowlimit() {
        return lowlimit.get();
    }

    public void setLowlimit(String lowlimit) {
        this.lowlimit.set(lowlimit);
    }

    public String getUplimit() {
        return uplimit.get();
    }

    public void setUplimit(String uplimit) {
        this.uplimit.set(uplimit);
    }

    public String getCounter() {
        return counter.get();
    }

    public void setCounter(String counter) {
        this.counter.set(counter);
    }

    public double getPrfm() {
        return prfm.get();
    }

    public void setPrfm(double prfm) {
        this.prfm.set(prfm);
    }

    public int getExitcode() {
        return exitcode.get();
    }

    public void setExitcode(int exitcode) {
        this.exitcode.set(exitcode);
    }

    @Override
    public String toString() {
        return "" + id.get() + "\t" + ip.get() + "\t" + pid.get() + "\t" + cno.get() + "\t"
                + vartype.get() + "\t" + scheduler.get() + "\t" + lstarttime.get() + "\t" + lendtime.get() + "\t"
                + lexctime.get() + "\t" + nexecutiontime.get() + "\t" + noh.get() + "\t" + poh.get() + "\t" + chunksize.get() + "\t"
                + lowlimit.get() + "\t" + uplimit.get() + "\t" + counter.get() + "\t" + prfm.get() + "\t" + exitcode.get() + "\n";

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
                        return Integer.valueOf(o1.getPid()).compareTo(o2.getPid());
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
