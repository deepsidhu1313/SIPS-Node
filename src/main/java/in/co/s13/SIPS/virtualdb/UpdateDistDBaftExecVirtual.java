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
import static in.co.s13.SIPS.datastructure.DistributionDBRow.DistributionDBRowComparator.LEXCTIME_SORT;
import static in.co.s13.SIPS.datastructure.DistributionDBRow.DistributionDBRowComparator.getComparator;
import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.db.InsertDistributionWareHouse;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.util.ArrayList;
import java.util.Collections;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import java.util.concurrent.ConcurrentHashMap;

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

    public UpdateDistDBaftExecVirtual(Long endTime, Long ExecTime, String filename, String ip, String PID, String CNO, String EXITCODE, String nodeUUID) {
        dbloc = "data/" + PID + "/dist-db/dist-" + PID + ".db";
        endtime = endTime;
        exectime = ExecTime;
        IP = ip;
        fname = filename;
        pid = PID;
        cno = CNO;
        exitCode = EXITCODE;
        System.out.println("size of master dist db " + MASTER_DIST_DB.size());
        this.uuid = nodeUUID;
        DistTable = MASTER_DIST_DB.get((PID.trim()));
    }

    @Override
    public void run() {
        {
            if (DistTable != null) {
                DistributionDBRow get = DistTable.get(uuid + "-" + cno.trim());
                if (get != null) {
                    startTime = get.getLstarttime();
                    vartype = get.getVartype();
                    Util.outPrintln("StartTime is " + startTime + " for ip " + IP + " with chunkno " + cno);
                    NOH = (endtime - startTime) - exectime;
                    lexecTime = endtime - startTime;
                    get.setLendtime(endtime);
                    get.setNexecutiontime(exectime);
                    get.setLexctime(lexecTime);
                    get.setNoh(get.getNoh() + NOH);
                    get.setExitcode(Integer.parseInt(exitCode.trim()));
                    ArrayList<DistributionDBRow> tempDist = new ArrayList<>();
                    tempDist.addAll(DistTable.values());
                    Collections.sort(tempDist, getComparator(LEXCTIME_SORT));
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
                                    PRFM = (distRow.getPrfm());
                                    XTC = (distRow.getExitcode());
//                            break;
                                }
                            }
                        }
                        GlobalValues.DIST_DB_EXECUTOR.submit(new InsertDistributionWareHouse(Node, PID, CNO, VARTYPE, SCHEDULER, LStart, Lend, Lexec, CS, LOWL, UPL, COUNTER, Nexec, CommOH, ParOH, ENTERINQ, STARTINQ, WAITINQ, SLEEP, PRFM, XTC, fname));

                    }

                    //Evaluate performance of all nodes after execution
                    if (isFinished) {
                        GlobalValues.NODE_DB_EXECUTOR.submit(() -> {
                            if (counter > nodeUUIDs.size()) {
                                counter = nodeUUIDs.size();
                            }
                            for (int i = 0; i < nodeUUIDs.size(); i++) {
//                                sql = "SELECT * FROM ALLN WHERE IP='" + nodeUUIDs.get(i) + "'";
                                /* for (IPAddress nodeIP : allNodeDB) {
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
                                    int c = 0;
                                    {
                                        for (DistributionDBRow DistTable1 : DistTable.values()) {
                                            tempNOH += DistTable1.getNoh();
                                            double d = DistTable1.getPrfm();
                                            if (d < 0.1) {
                                                d = 0.1;
                                            }
                                            tempavgSleeptime += DistTable1.getSleeptime();
                                            tempavgWaitinQ += DistTable1.getWaitinq();
                                            tempload += d;
                                            c++;
                                        }
                                    }
                                    tempload /= c;
                                    tempNOH /= c;
                                    tempavgSleeptime /= c;
                                    tempavgWaitinQ /= c;
                                    GlobalValues.RESULT_DB_EXECUTOR.submit(new UpdateResultDBafterExecVirtual(pid, endtime, ttime, tempNOH, "" + tempload, tempavgWaitinQ, tempavgSleeptime));
                                    //  controlpanel.Settings.distDWDBExecutor.execute(new InsDistWareHouse(Node, PID, CNO, VARTYPE, SCHEDULER, LStart, Lend, Lexec, CS, LOWL, UPL, COUNTER, Nexec, CommOH, ParOH, PRFM, XTC, fname));
                                });
                            }
                        });

                    }
                }
            }
        }
    }

}
