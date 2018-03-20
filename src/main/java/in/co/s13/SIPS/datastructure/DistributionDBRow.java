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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;

/**
 *
 * @author NAVDEEP SINGH SIDHU <navdeepsingh.sidhu95@gmail.com>
 */
public class DistributionDBRow {

    private String uuid;
    private Double prfm = 0.0, avgLoad = 0.0;
    private Integer  id, cno, vartype, exitcode,cacheHit = 0, cacheMiss = 0, reqsSent = 0, reqsRecieved = 0,totalChunks=0;
    private Long lstarttime = 0l, lendtime = 0l, lexctime = 0l, nexecutiontime = 0l, noh = 0l, poh = 0l, entrinq = 0l, startinq = 0l, waitinq = 0l, sleeptime = 0l, uploadedData = 0l, downloadedData = 0l;
    private String pid, chunksize, lowlimit, scheduler, uplimit, counter, ipAddress, hostName;
    private Long cachedData = 0l;
    private DecimalFormat df = new DecimalFormat("##.##");
    private ArrayList<Double> uploadSpeed = new ArrayList<>(), downloadSpeed = new ArrayList<>();

    public DistributionDBRow(int id, String uuid, String pid, int cno, int vartype, String scheduler,
            long lstarttime, long lendtime, long lexctime, long nexecutiontime, long noh, long poh,
            long entrinq, long startinq, long waitinq, long sleeptime,
            String chunksize, String lowlimit, String uplimit, String counter, double prfm, int exitcode, String ipAddress, String hostname, double avgLoad,int totalChunks) {
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
        this.totalChunks=totalChunks;
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
        OptionalDouble avgUploadSpeed = uploadSpeed.parallelStream()
                .mapToDouble(a -> a)
                .average();
        return avgUploadSpeed.isPresent() ? Double.parseDouble(df.format(avgUploadSpeed.getAsDouble())) : 0;
    }

    public void addUploadSpeed(Double uploadSpeed) {
        this.uploadSpeed.add(uploadSpeed);
    }

    public Double getAvgDownloadSpeed() {
        OptionalDouble avgDownloadSpeed = downloadSpeed.parallelStream()
                .mapToDouble(a -> a)
                .average();
        return avgDownloadSpeed.isPresent() ? Double.parseDouble(df.format(avgDownloadSpeed.getAsDouble())) : 0;
    }

    public void addDownloadSpeed(Double downloadSpeed) {
        this.downloadSpeed.add(downloadSpeed);
    }

    public Long getUploadedData() {
        return uploadedData;
    }

    public void setUploadedData(Long uploadedData) {
        this.uploadedData = uploadedData;
    }

    public Long getDownloadedData() {
        return downloadedData;
    }

    public void setDownloadedData(Long downloadedData) {
        this.downloadedData = downloadedData;
    }

    public Integer getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Integer cacheHit) {
        this.cacheHit = cacheHit;
    }

    public int addCacheHit(int cacheHit) {
        return this.cacheHit += (cacheHit);
    }

    public int incrementCacheHit() {
        return cacheHit++;
    }

    public Integer getCacheMiss() {
        return cacheMiss;
    }

    public void setCacheMiss(Integer cacheMiss) {
        this.cacheMiss = cacheMiss;
    }

    public int addCacheMiss(int cacheMiss) {
        return this.cacheMiss += (cacheMiss);
    }

    public int incrementCacheMiss() {
        return cacheMiss++;
    }

    public double getCacheHitMissRatio() {
        return (double) cacheHit / (double) (cacheMiss < 1 ? 1 : cacheMiss);
    }

    public int getReqsSent() {
        return reqsSent;
    }

    public void incrementReqsSent() {
        this.reqsSent++;
    }

    public int getReqsRecieved() {
        return reqsRecieved;
    }

    public void incrementReqsRecieved() {
        this.reqsRecieved++;
    }

    public long getCachedData() {
        return cachedData;
    }

    public long addCachedData(Long delta) {
        return this.cachedData += (delta);
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
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
        result.put("totalChunks", totalChunks);
        result.put("lowlimit", lowlimit);
        result.put("scheduler", scheduler);
        result.put("uplimit", uplimit);
        result.put("counter", counter);
        result.put("avgLoad", avgLoad);
        result.put("avgDownloadSpeed", getAvgDownloadSpeed());
        result.put("avgUploadSpeed", getAvgUploadSpeed());
        result.put("uploadData", uploadedData);
        result.put("downloadData", downloadedData);
        result.put("cacheHit", cacheHit);
        result.put("cacheMiss", cacheMiss);
        result.put("cachedData", cachedData);
        result.put("cacheHitMissRatio", getCacheHitMissRatio());
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
