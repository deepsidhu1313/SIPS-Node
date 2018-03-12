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
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class DistributionDBRow {

    private Integer id;
    private String uuid;
    private Double prfm, avgLoad, avgUploadSpeed, avgDownloadSpeed;
    private Integer cno, vartype, exitcode;
    private Long lstarttime, lendtime, lexctime, nexecutiontime, noh, poh, entrinq, startinq, waitinq, sleeptime, uploadedDataInKB, downloadedDataInKB;
    private String pid, chunksize, lowlimit, scheduler, uplimit, counter, ipAddress, hostName;
    private AtomicInteger cacheHit= new AtomicInteger(0), cacheMiss= new AtomicInteger(0);

    public DistributionDBRow(int id, String uuid, String pid, int cno, int vartype, String scheduler,
            long lstarttime, long lendtime, long lexctime, long nexecutiontime, long noh, long poh,
            long entrinq, long startinq, long waitinq, long sleeptime,
            String chunksize, String lowlimit, String uplimit, String counter, double prfm, int exitcode, String ipAddress, String hostname, double avgLoad) {
        this.id = (id);
        this.uuid = (uuid);
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
        this.ipAddress = ipAddress;
        this.hostName = hostname;
        this.avgLoad = avgLoad;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Double getPrfm() {
        return prfm;
    }

    public void setPrfm(Double prfm) {
        this.prfm = prfm;
    }

    public Integer getCno() {
        return cno;
    }

    public void setCno(Integer cno) {
        this.cno = cno;
    }

    public Integer getVartype() {
        return vartype;
    }

    public void setVartype(Integer vartype) {
        this.vartype = vartype;
    }

    public Integer getExitcode() {
        return exitcode;
    }

    public void setExitcode(Integer exitcode) {
        this.exitcode = exitcode;
    }

    public Long getLstarttime() {
        return lstarttime;
    }

    public void setLstarttime(Long lstarttime) {
        this.lstarttime = lstarttime;
    }

    public Long getLendtime() {
        return lendtime;
    }

    public void setLendtime(Long lendtime) {
        this.lendtime = lendtime;
    }

    public Long getLexctime() {
        return lexctime;
    }

    public void setLexctime(Long lexctime) {
        this.lexctime = lexctime;
    }

    public Long getNexecutiontime() {
        return nexecutiontime;
    }

    public void setNexecutiontime(Long nexecutiontime) {
        this.nexecutiontime = nexecutiontime;
    }

    public Long getNoh() {
        return noh;
    }

    public void setNoh(Long noh) {
        this.noh = noh;
    }

    public Long getPoh() {
        return poh;
    }

    public void setPoh(Long poh) {
        this.poh = poh;
    }

    public Long getEntrinq() {
        return entrinq;
    }

    public void setEntrinq(Long entrinq) {
        this.entrinq = entrinq;
    }

    public Long getStartinq() {
        return startinq;
    }

    public void setStartinq(Long startinq) {
        this.startinq = startinq;
    }

    public Long getWaitinq() {
        return (this.waitinq < 0 ? startinq - entrinq : waitinq);
    }

    public void setWaitinq(Long waitinq) {
        this.waitinq = waitinq;
    }

    public Long getSleeptime() {
        return sleeptime;
    }

    public void setSleeptime(Long sleeptime) {
        this.sleeptime = sleeptime;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getChunksize() {
        return chunksize;
    }

    public void setChunksize(String chunksize) {
        this.chunksize = chunksize;
    }

    public String getLowlimit() {
        return lowlimit;
    }

    public void setLowlimit(String lowlimit) {
        this.lowlimit = lowlimit;
    }

    public String getScheduler() {
        return scheduler;
    }

    public void setScheduler(String scheduler) {
        this.scheduler = scheduler;
    }

    public String getUplimit() {
        return uplimit;
    }

    public void setUplimit(String uplimit) {
        this.uplimit = uplimit;
    }

    public String getCounter() {
        return counter;
    }

    public void setCounter(String counter) {
        this.counter = counter;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Double getAvgLoad() {
        return avgLoad;
    }

    public void setAvgLoad(Double avgLoad) {
        this.avgLoad = avgLoad;
    }

    public Double getAvgUploadSpeed() {
        return avgUploadSpeed;
    }

    public void setAvgUploadSpeed(Double avgUploadSpeed) {
        this.avgUploadSpeed = avgUploadSpeed;
    }

    public Double getAvgDownloadSpeed() {
        return avgDownloadSpeed;
    }

    public void setAvgDownloadSpeed(Double avgDownloadSpeed) {
        this.avgDownloadSpeed = avgDownloadSpeed;
    }

    public Long getUploadedDataInKB() {
        return uploadedDataInKB;
    }

    public void setUploadedDataInKB(Long uploadedDataInKB) {
        this.uploadedDataInKB = uploadedDataInKB;
    }

    public Long getDownloadedDataInKB() {
        return downloadedDataInKB;
    }

    public void setDownloadedDataInKB(Long downloadedDataInKB) {
        this.downloadedDataInKB = downloadedDataInKB;
    }

    public AtomicInteger getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(AtomicInteger cacheHit) {
        this.cacheHit = cacheHit;
    }

    public AtomicInteger getCacheMiss() {
        return cacheMiss;
    }

    public void setCacheMiss(AtomicInteger cacheMiss) {
        this.cacheMiss = cacheMiss;
    }

    public double getCacheHitMissRatio() {
        return this.getCacheHit().get() / this.getCacheMiss().get();
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int indentFactor) {
        return toJSON().toString(indentFactor);
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("id", id);
        result.put("uuid", uuid);
        result.put("ipAddress", ipAddress);
        result.put("hostname", hostName);
        result.put("prfm", prfm);
        result.put("cno", cno);
        result.put("vartype", vartype);
        result.put("exitcode", exitcode);
        result.put("lstarttime", lstarttime);
        result.put("lendtime", lendtime);
        result.put("lexctime", lexctime);
        result.put("nexecutiontime", nexecutiontime);
        result.put("noh", noh);
        result.put("poh", poh);
        result.put("entrinq", entrinq);
        result.put("startinq", startinq);
        result.put("waitinq", getWaitinq());
        result.put("sleeptime", sleeptime);
        result.put("pid", pid);
        result.put("chunksize", chunksize);
        result.put("lowlimit", lowlimit);
        result.put("scheduler", scheduler);
        result.put("uplimit", uplimit);
        result.put("counter", counter);
        result.put("avgLoad", avgLoad);
        result.put("avgDownloadSpeed", avgDownloadSpeed);
        result.put("avgUploadSpeed", avgUploadSpeed);
        result.put("uploadDataInKB", uploadedDataInKB);
        result.put("downloadDataInKB", downloadedDataInKB);
        result.put("cacheHit", cacheHit);
        result.put("cacheMiss", cacheMiss);
        result.put("cacheHitMissRatio", this.getCacheHitMissRatio());
        return result;
    }

    public enum DistributionDBRowComparator implements Comparator<DistributionDBRow> {

        ID_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getId().compareTo(o2.getId());
            }
        },
        IP_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getUuid().compareTo(o2.getUuid());
            }
        },
        PID_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return (o1.getPid()).compareTo(o2.getPid());
            }
        },
        CNO_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getCno().compareTo(o2.getCno());
            }
        },
        VARTYPE_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getVartype().compareTo(o2.getVartype());
            }
        },
        SCHEDULER_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getVartype().compareTo(o2.getVartype());
            }
        },
        LSTARTTIME_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getLstarttime().compareTo(o2.getLstarttime());
            }
        },
        LENDTIME_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getLendtime().compareTo(o2.getLendtime());
            }
        },
        LEXCTIME_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getLexctime().compareTo(o2.getLexctime());
            }
        },
        NEXECUTIONTIME_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getNexecutiontime().compareTo(o2.getNexecutiontime());
            }
        },
        NOH_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getNoh().compareTo(o2.getNoh());
            }
        },
        POH_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getPoh().compareTo(o2.getPoh());
            }
        },
        CHUNKSIZE_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getChunksize().compareTo(o2.getChunksize());
            }
        },
        LOWLIMIT_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getLowlimit().compareTo(o2.getLowlimit());
            }
        },
        UPLIMIT_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getUplimit().compareTo(o2.getUplimit());
            }
        },
        COUNTER_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getCounter().compareTo(o2.getCounter());
            }
        },
        PRFM_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getPrfm().compareTo(o2.getPrfm());
            }
        },
        EXITCODE_SORT {
            @Override
            public int compare(DistributionDBRow o1, DistributionDBRow o2) {
                return o1.getExitcode().compareTo(o2.getExitcode());
            }
        };

        public static Comparator<DistributionDBRow> decending(final Comparator<DistributionDBRow> other) {
            return (DistributionDBRow o1, DistributionDBRow o2) -> -1 * other.compare(o1, o2);
        }

        public static Comparator<DistributionDBRow> getComparator(final DistributionDBRowComparator... multipleOptions) {
            return (DistributionDBRow o1, DistributionDBRow o2) -> {
                for (DistributionDBRowComparator option : multipleOptions) {
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
