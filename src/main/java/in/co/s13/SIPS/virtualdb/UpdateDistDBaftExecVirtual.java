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

import in.co.s13.SIPS.datastructure.DistributionDBRow;
import static in.co.s13.SIPS.datastructure.DistributionDBRow.DistributionDBRowComparator.getComparator;
import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.db.InsertDistributionWareHouse;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.util.ArrayList;
import java.util.Collections;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Nika
 */
public class UpdateDistDBaftExecVirtual implements Runnable {

    String dbloc, sql, IP, fname, pid, performance, cno, exitCode, uuid;
    Long startTime, endtime, exectime, NOH, lexecTime;
    // ResultSet rs, rs2, rs3, rs4, rs5, rs6, rs7;

    ArrayList<String> nodeUUIDs = new ArrayList();
    ArrayList<String> CNOs = new ArrayList();
    ArrayList<Double> effList = new ArrayList();
    //ArrayList<String> IDs = new ArrayList();
    //ArrayList<BigDecimal> chunks = new ArrayList<>();
    //ArrayList<Long> lExecTime = new ArrayList<>();
    // BigDecimal chunkSize = BigDecimal.ZERO;

    int counter = 0, vartype;
    ConcurrentHashMap<String, DistributionDBRow> DistTable;
    double avgLoad;
    JSONObject taskRow;

    public UpdateDistDBaftExecVirtual(Long endTime, Long ExecTime, String filename, String ip, String PID, String CNO, String EXITCODE, String nodeUUID, double avgLoad, JSONObject taskRow) {
        dbloc = "data/" + PID + "/dist-db/dist-" + PID + ".db";
        endtime = endTime;
        exectime = ExecTime;
        IP = ip;
        fname = filename;
        pid = PID;
        cno = CNO;
        exitCode = EXITCODE;
        this.avgLoad = avgLoad;
        System.out.println("size of master dist db " + MASTER_DIST_DB.size());
        this.uuid = nodeUUID;
        this.taskRow = taskRow;

        System.out.println("UpdateDistDBaftExecVirtual Created For " + pid + " CNO" + cno);
        Util.appendToTasksLog(GlobalValues.LOG_LEVEL.OUTPUT, "UpdateDistDBaftExecVirtual Created For " + pid + " CNO" + cno);
    }

    @Override
    public void run() {
        System.out.println("UpdateDistDBaftExecVirtual Started For " + pid + " CNO" + cno);
        DistTable = MASTER_DIST_DB.get((pid.trim()));
        Thread.currentThread().setName("UpdateDistDBaftExecVirtual For " + pid + " CNO" + cno);
        int tries = 0;
        while (DistTable == null) {
            DistTable = MASTER_DIST_DB.get((pid.trim()));

            if (tries == 50) {
                break;
            }
            tries++;
            try {
                System.out.println("UpdateDistDBaftExecVirtual Sleep to get Table " + pid + " CNO" + cno);
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Logger.getLogger(UpdateDistDBaftExecVirtual.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        {
            if (DistTable != null) {
                DistributionDBRow get = DistTable.get(uuid + "-" + cno.trim());
                tries = 0;
                while (get == null) {
                    if (tries == 50) {
                        break;
                    }
                    DistTable = MASTER_DIST_DB.get((pid.trim()));
                    get = DistTable.get(uuid + "-" + cno.trim());

                    tries++;
                    try {
                        System.out.println("UpdateDistDBaftExecVirtual Sleep to get Row " + pid + " CNO" + cno);
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(UpdateDistDBaftExecVirtual.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (get != null) {
                    startTime = get.getLstarttime();
                    vartype = get.getVartype();
                    get.setEntrinq(taskRow.getLong("EnteredInQueue"));
                    get.setStartinq(taskRow.getLong("StartedInQueue"));
                    get.setSleeptime(taskRow.getLong("SleepTime"));
                    get.setNoh(taskRow.getLong("COMM_OH"));
//                    Util.outPrintln("StartTime is " + startTime + " for ip " + IP + " with chunkno " + cno);
                    NOH = (endtime - startTime) - exectime;
                    lexecTime = endtime - startTime;
                    get.setLendtime(endtime);
                    get.setNexecutiontime(exectime);
                    get.setLexctime(lexecTime);
                    get.setNoh(get.getNoh() + NOH);
                    get.setAvgLoad(avgLoad);
                    get.addCachedData(taskRow.getLong("CachedData"));
                    get.addUploadSpeed(taskRow.getDouble("AvgDownloadSpeed"));
                    get.setUploadedData(get.getUploadedData() + taskRow.getLong("DownloadData"));
                    get.addCacheHit(taskRow.getInt("CacheHit"));
                    get.addCacheMiss(taskRow.getInt("CacheMiss"));
                    get.addReqsRecieved(taskRow.getInt("ReqSent"));
                    get.addReqsSent(taskRow.getInt("ReqRecieved"));
                    get.setExitcode(Integer.parseInt(exitCode.trim()));
                    ArrayList<DistributionDBRow> tempDist = new ArrayList<>();
                    tempDist.addAll(DistTable.values());
                    Collections.sort(tempDist, getComparator(DistributionDBRow.DistributionDBRowComparator.EXITCODE_SORT).reversed());
                    Boolean isFinished = true;
                    for (DistributionDBRow get2 : tempDist) {
                        int code = get2.getExitcode();
                        if (code == 9999) {
                            isFinished = false;
                            break;
                        }
                        if (code != 0) {
                            continue;
                        }
                    }

                    if (isFinished) {
                        for (DistributionDBRow get3 : tempDist) {
                            nodeUUIDs.add(get3.getUuid());
                            CNOs.add(" " + get3.getCno());
                            counter++;
                        }

                        isFinished = (tempDist.size() == get.getTotalChunks());

                    }

                    tempDist.clear();
                    tempDist = null;

                    Util.outPrintln("Job Completed " + isFinished);
                    {
                        String Node = "";
                        String PID = "";
                        Integer CNO = 0;
                        Integer VARTYPE = 0;
                        String SCHEDULER = "";
                        Long LStart = 0L;
                        Long Lend = 0L;
                        Long Lexec = 0L;
                        String CS = "";
                        String LOWL = "";
                        String UPL = "";
                        String COUNTER = "";
                        Long Nexec = 0L;
                        Long CommOH = 0L;
                        Long ParOH = 0L;
                        Long ENTERINQ = 0L;
                        Long STARTINQ = 0L;
                        Long WAITINQ = 0L;
                        Long SLEEP = 0L;
                        Double PRFM = 0.0;
                        Integer XTC = 0;

                        double avgCacheHitMissRatio = 0;
                        long avgDownloadData = 0;
                        double avgDownloadSpeed = 0;
                        int avgReqSent = 0;
                        long avgUploadData = 0;
                        double avgUploadSpeed = 0;
                        int avgReqRecieved = 0;
                        long avgCachedData = 0;
                        {
//                    for ()
                            {
                                DistributionDBRow distRow = DistTable.get(uuid.trim() + "-" + cno.trim());
                                if (distRow != null) {
                                    CommOH = (distRow.getNoh());
                                    double d = distRow.getPrfm();
                                    Node = (distRow.getUuid());
                                    PID = (distRow.getPid());
                                    CNO = (distRow.getCno());
                                    VARTYPE = (distRow.getVartype());
                                    SCHEDULER = (distRow.getScheduler());
                                    LStart = (distRow.getLstarttime());
                                    Lend = (distRow.getLendtime());
                                    Lexec = (distRow.getLexctime());
                                    CS = ((distRow.getChunksize()));
                                    LOWL = ((distRow.getLowlimit()));
                                    UPL = ((distRow.getUplimit()));
                                    COUNTER = ((distRow.getCounter()));
                                    Nexec = ((distRow.getNexecutiontime()));
                                    CommOH = (distRow.getNoh());
                                    ParOH = (distRow.getPoh());
                                    ENTERINQ = (distRow.getEntrinq());
                                    STARTINQ = (distRow.getStartinq());
                                    WAITINQ = (distRow.getWaitinq());
                                    SLEEP = (distRow.getSleeptime());
                                    PRFM = (distRow.getAvgLoad());
                                    XTC = (distRow.getExitcode());
                                    avgCacheHitMissRatio = distRow.getCacheHitMissRatio();
                                    avgDownloadData = distRow.getDownloadedData();
                                    avgDownloadSpeed = distRow.getAvgDownloadSpeed();
                                    avgReqSent = distRow.getReqsSent();
                                    avgUploadData = distRow.getUploadedData();
                                    avgUploadSpeed = distRow.getAvgUploadSpeed();
                                    avgReqRecieved = distRow.getReqsRecieved();
                                    avgCachedData = distRow.getCachedData();
//                            break;
                                }
                            }
                        }
                        GlobalValues.DIST_WH_DB_EXECUTOR.submit(new InsertDistributionWareHouse(Node, PID, CNO, VARTYPE, SCHEDULER, LStart, Lend, Lexec, CS, LOWL, UPL, COUNTER, Nexec, CommOH, ParOH, ENTERINQ, STARTINQ, WAITINQ, SLEEP, PRFM, XTC, fname, avgCacheHitMissRatio,
                                avgDownloadData,
                                avgDownloadSpeed,
                                avgReqSent,
                                avgUploadData,
                                avgUploadSpeed,
                                avgReqRecieved,
                                avgCachedData));

                    }

                    //Evaluate performance of all nodes after execution
                    if (isFinished) {
//                        GlobalValues.NODE_DB_EXECUTOR.submit(() -> {
                        if (counter > nodeUUIDs.size()) {
                            counter = nodeUUIDs.size();
                        }
                        /* for (int i = 0; i < nodeUUIDs.size(); i++) {
//                                sql = "SELECT * FROM ALLN WHERE IP='" + nodeUUIDs.get(i) + "'";
                                for (IPAddress nodeIP : allNodeDB) {
                                    if (nodeIP.getFirstName().trim().equalsIgnoreCase(nodeUUIDs.get(i).trim())) {
                                        double tempprfm = nodeIP.getCpuLoad();
                                        double tempprfm2 = (tempprfm + ((double) 1 * ((double) counter / (double) (nodeUUIDs.size())))) / 2;
                                        final String ip = nodeUUIDs.get(i);
                                        final String cn = CNOs.get(i);
                                        System.OUT.println("IP adress: " + ip + " CNO: " + cn + " PrevPRF "
                                                + tempprfm + " NewPrfm:" + ((double) 1 * (double) ((double) counter / (double) (nodeUUIDs.size()))) + " Avg is:" + tempprfm2 + " counter:" + counter + " IPsSize:" + nodeUUIDs.size());
                                        if (tempprfm2 < 0.1) {
                                            tempprfm2 = 0.1;
                                        }

                                        int p = Integer.parseInt(pid.trim());
                                        for (DistributionDBRow get : DistTable) {
                                            if (get.getUuid().trim().equalsIgnoreCase(ip.trim()) && (get.getCno() == (Integer.parseInt(cn.trim())))) {
                                                get.setPrfm(tempprfm2);
                                            }
                                        }
                                        double prfmt = tempprfm2;

                                        for (IPAddress allGet : allNodeDB) {
                                            if (allGet.getFirstName().trim().equalsIgnoreCase(ip.trim())) {
                                                prfmt += allGet.getCpuLoad();
                                                prfmt /= 2;

                                            }

                                            if (prfmt < 0.1) {
                                                prfmt = 0.1;
                                            }
                                            // sql = "UPDATE ALLN SET PRFM='" + prfmt + "' WHERE IP='" + ip + "';";
                                            //   Settings.alldb.Update(sql);
                                            //  Settings.alldb.closeStatement();
                                        }
                                        for (IPAddress allGet : allNodeDB) {
                                            if (allGet.getFirstName().trim().equalsIgnoreCase(ip.trim())) {
                                                allGet.setCpuLoad(prfmt);
                                            }

                                        }
                                        counter--;
                                    }

                                }
                            }*/

                        GlobalValues.RESULT_DB_EXECUTOR.submit(() -> {
                            long temp = Long.MIN_VALUE;
                            Result result = GlobalValues.RESULT_DB.get(pid.trim());
                            if (result != null) {
                                temp = result.getStarttime();
                            }
                            Long StartTime = temp;
                            Long ttime = endtime - StartTime;
                            Long tempNOH = 0L;
                            long tempavgWaitinQ = 0, tempavgSleeptime = 0;
                            double tempload = 0.0;

                            double avgCacheHitMissRatio = 0;
                            long avgDownloadData = 0;
                            double avgDownloadSpeed = 0;
                            int avgReqSent = 0;
                            long avgUploadData = 0;
                            double avgUploadSpeed = 0;
                            int avgReqRecieved = 0;
                            long avgCachedData = 0;
                            System.out.println("Calculating Average " + pid + " CNO" + cno);
                            int c = 0;
                            {
                                for (DistributionDBRow distTableRow : DistTable.values()) {
                                    tempNOH += distTableRow.getNoh();
                                    double d = distTableRow.getAvgLoad();
                                    tempavgSleeptime += distTableRow.getSleeptime();
                                    tempavgWaitinQ += distTableRow.getWaitinq();
                                    tempload += d;

                                    avgCacheHitMissRatio += distTableRow.getCacheHitMissRatio();
                                    avgDownloadData += distTableRow.getDownloadedData();
                                    avgDownloadSpeed += distTableRow.getAvgDownloadSpeed();
                                    avgReqSent += distTableRow.getReqsSent();
                                    avgUploadData += distTableRow.getUploadedData();
                                    avgUploadSpeed += distTableRow.getAvgUploadSpeed();
                                    avgReqRecieved += distTableRow.getReqsRecieved();
                                    avgCachedData += distTableRow.getCachedData();
                                    c++;
                                }
                            }
                            tempload /= c;
                            tempNOH /= c;
                            tempavgSleeptime /= c;
                            tempavgWaitinQ /= c;

                            avgCacheHitMissRatio /= c;
                            avgDownloadData /= c;
                            avgDownloadSpeed /= c;
                            avgReqSent /= c;
                            avgUploadData /= c;
                            avgUploadSpeed /= c;
                            avgReqRecieved /= c;
                            avgCachedData /= c;
                            System.out.println("Calculated Average " + pid + " CNO" + cno);

                            GlobalValues.RESULT_WH_DB_EXECUTOR.submit(new UpdateResultDBafterExecVirtual(pid, endtime, ttime, tempNOH, tempload, tempavgWaitinQ, tempavgSleeptime, avgCacheHitMissRatio,
                                    avgDownloadData,
                                    avgDownloadSpeed,
                                    avgReqSent,
                                    avgUploadData,
                                    avgUploadSpeed,
                                    avgReqRecieved,
                                    avgCachedData));
                            //  controlpanel.Settings.distDWDBExecutor.execute(new InsDistWareHouse(Node, PID, CNO, VARTYPE, SCHEDULER, LStart, Lend, Lexec, CS, LOWL, UPL, COUNTER, Nexec, CommOH, ParOH, PRFM, XTC, fname));
                        });
//                        });

                    }
                }
            }
        }
    }

}
