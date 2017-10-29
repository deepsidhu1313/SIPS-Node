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
import in.co.s13.SIPS.db.InsertDistributionWareHouse;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Nika
 */
public class UpdateDistDBaftExecVirtual implements Runnable {

    String dbloc, sql, IP, fname, pid, performance, cno, exitCode;
    Long startTime, endtime, exectime, NOH, lexecTime;
    // ResultSet rs, rs2, rs3, rs4, rs5, rs6, rs7;

    ArrayList<String> IPs = new ArrayList();
    ArrayList<String> CNOs = new ArrayList();
    ArrayList<Double> effList = new ArrayList();
    //ArrayList<String> IDs = new ArrayList();
    //ArrayList<BigDecimal> chunks = new ArrayList<>();
    //ArrayList<Long> lExecTime = new ArrayList<>();
    // BigDecimal chunkSize = BigDecimal.ZERO;

    int counter = 0, vartype;
    ConcurrentHashMap<String, DistributionDBRow> DistTable;

    public UpdateDistDBaftExecVirtual(Long endTime, Long ExecTime, String filename, String ip, String PID, String CNO, String EXITCODE) {
        dbloc = "data/" + PID + "/dist-db/dist-" + PID + ".db";
        endtime = endTime;
        exectime = ExecTime;
        IP = ip;
        fname = filename;
        pid = PID;
        cno = CNO;
        exitCode = EXITCODE;
        System.out.println("size of master dist db " + MASTER_DIST_DB.size());
        while (MASTER_DIST_DB.size() <= Integer.parseInt(PID.trim())) {
            try {
                System.out.println("Waiting for Master DB to Create table " + PID + " Current SIze:" + MASTER_DIST_DB.size());
                Thread.currentThread().sleep(10000);

            } catch (InterruptedException ex) {
                Logger.getLogger(UpdateDistDBaftExecVirtual.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        DistTable = MASTER_DIST_DB.get((PID.trim()));
    }

    @Override
    public void run() {
        {

            sql = "SELECT * FROM DIST ;";
            // rs = db.select(dbloc, sql);
            // Settings.outPrintln(rs.getString("LStartTime"));
            //while (rs.next())
//            for () {
            DistributionDBRow get = DistTable.get(IP + "-" + cno.trim());
            startTime = get.getLstarttime();
            vartype = get.getVartype();
            //      chunkSize = new BigDecimal(get.getChunksize());

//            }
            //   db.closeConnection();
            Util.outPrintln("StartTime is " + startTime + " for ip " + IP + " with chunkno " + cno);
            NOH = (endtime - startTime) - exectime;
            lexecTime = endtime - startTime;
            /*    //BigDecimal tempbd = new BigDecimal(lexecTime);
             //BigDecimal tempEff = tempbd.divide(chunkSize);
             //double eff = tempbd.divide(chunkSize).doubleValue();
             // sql = "UPDATE DIST set LEndTime='" + endtime + "', NExecutionTime='" + exectime
             //       + "', LExcTime='" + lexecTime + "',NOH='" + NOH + "', EXITCODE='" + exitCode + "' WHERE IP= '" + IP + "' AND CNO='" + cno + "';";
             //db2.Update(dbloc, sql);
             //db2.closeConnection();
             */
//            DistributionDBRow get2 = DistTable.get(IP.trim()+"-"+cno.trim());
//            for (int i = 0; i < DistTable.size(); i++) {
//                
//
//                if (get.getIp().trim().equalsIgnoreCase() && (get.getCno() == (Integer.parseInt()))) {
//                    
//                    break;
//                }
//            }

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
//                effList.add((double) (get.getLexctime()));
                //IPs.add(get.getIp());
                // CNOs.add(" " + get.getCno());
                //    counter++;
            }

            if (isFinished) {
                for (DistributionDBRow get3 : tempDist) {

//                effList.add((double) (get.getLexctime()));
                    IPs.add(get3.getIp());
                    CNOs.add(" " + get3.getCno());
                    counter++;
                }

            }

            tempDist.clear();
            tempDist = null;
            /*   sql = "SELECT * FROM DIST ORDER BY LExcTime ASC;";
             rs2 = db3.select(dbloc, sql);
             while (rs2.next()) {
             int code = rs2.getInt("EXITCODE");
             if (code == 9999) {
             isFinished = false;
             break;
             }
             if (code != 0) {
             continue;
             }
             effList.add(rs2.getDouble("LExcTime"));
             IPs.add(rs2.getString("IP"));
             CNOs.add(rs2.getString("CNO"));
             }
             db3.closeConnection();
             rs.close();
             rs2.close();
             */
            Util.outPrintln("Job Completed " + isFinished);
            {
                /*
                ArrayList<String> Node = new ArrayList<>();
                ArrayList<Integer> PID = new ArrayList<>();
                ArrayList<Integer> CNO = new ArrayList<>();
                ArrayList<Integer> VARTYPE = new ArrayList<>();
                ArrayList<Integer> SCHEDULER = new ArrayList<>();
                ArrayList<Long> LStart = new ArrayList<>();
                ArrayList<Long> Lend = new ArrayList<>();
                ArrayList<Long> Lexec = new ArrayList<>();
                ArrayList<BigDecimal> CS = new ArrayList<>();
                ArrayList<BigDecimal> LOWL = new ArrayList<>();
                ArrayList<BigDecimal> UPL = new ArrayList<>();
                ArrayList<BigDecimal> COUNTER = new ArrayList<>();
                ArrayList<Long> Nexec = new ArrayList<>();
                ArrayList<Long> CommOH = new ArrayList<>();
                ArrayList<Long> ParOH = new ArrayList<>();
                ArrayList<Long> ENTERINQ = new ArrayList<>();
                ArrayList<Long> STARTINQ = new ArrayList<>();
                ArrayList<Long> WAITINQ = new ArrayList<>();
                ArrayList<Long> SLEEP = new ArrayList<>();
                ArrayList<Double> PRFM = new ArrayList<>();
                ArrayList<Integer> XTC = new ArrayList<>();
                 */
                String Node = "";
                Integer PID = 0;
                Integer CNO = 0;
                Integer VARTYPE = 0;
                Integer SCHEDULER = 0;
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
                        DistributionDBRow DistTable1 = DistTable.get(IP.trim() + "-" + cno.trim());
//                                if (DistTable1.getIp().trim().equalsIgnoreCase(IP) && DistTable1.getCno() == Integer.parseInt(cno.trim())) 
                        {
                            CommOH = (DistTable1.getNoh());
                            double d = DistTable1.getPrfm();
                            Node = (DistTable1.getIp());
                            PID = (DistTable1.getPid());
                            CNO = (DistTable1.getCno());
                            VARTYPE = (DistTable1.getVartype());
                            SCHEDULER = (DistTable1.getScheduler());
                            LStart = (DistTable1.getLstarttime());
                            Lend = (DistTable1.getLendtime());
                            Lexec = (DistTable1.getLexctime());
                            CS = ((DistTable1.getChunksize()));
                            LOWL = ((DistTable1.getLowlimit()));
                            UPL = ((DistTable1.getUplimit()));
                            COUNTER = ((DistTable1.getCounter()));
                            Nexec = ((DistTable1.getNexecutiontime()));
                            CommOH = (DistTable1.getNoh());
                            ParOH = (DistTable1.getPoh());
                            ENTERINQ = (DistTable1.getEntrinq());
                            STARTINQ = (DistTable1.getStartinq());
                            WAITINQ = (DistTable1.getWaitinq());
                            SLEEP = (DistTable1.getSleeptime());
                            PRFM = (DistTable1.getPrfm());
                            XTC = (DistTable1.getExitcode());
//                            break;
                        }
                    }
                }
                GlobalValues.DIST_DB_EXECUTOR.execute(new InsertDistributionWareHouse(Node, PID, CNO, VARTYPE, SCHEDULER, LStart, Lend, Lexec, CS, LOWL, UPL, COUNTER, Nexec, CommOH, ParOH, ENTERINQ, STARTINQ, WAITINQ, SLEEP, PRFM, XTC, fname));

            }
            /* 
            Evaluate performance of all nodes after execution
            
            if (isFinished) {
                nodeDBExecutor.execute(() -> {
                    if (counter > IPs.size()) {
                        counter = IPs.size();
                    }
                    for (int i = 0; i < IPs.size(); i++) {
                        sql = "SELECT * FROM ALLN WHERE IP='" + IPs.get(i) + "'";
                        for (IPAddress nodeIP : allNodeDB) {
                            if (nodeIP.getFirstName().trim().equalsIgnoreCase(IPs.get(i).trim())) {
                                double tempprfm = nodeIP.getCpuLoad();
                                double tempprfm2 = (tempprfm + ((double) 1 * ((double) counter / (double) (IPs.size())))) / 2;
                                final String ip = IPs.get(i);
                                final String cn = CNOs.get(i);
                                System.OUT.println("IP adress: " + ip + " CNO: " + cn + " PrevPRF "
                                        + tempprfm + " NewPrfm:" + ((double) 1 * (double) ((double) counter / (double) (IPs.size()))) + " Avg is:" + tempprfm2 + " counter:" + counter + " IPsSize:" + IPs.size());
                                if (tempprfm2 < 0.1) {
                                    tempprfm2 = 0.1;
                                }

                                int p = Integer.parseInt(pid.trim());
                                for (DistributionDBRow get : DistTable) {
                                    if (get.getIp().trim().equalsIgnoreCase(ip.trim()) && (get.getCno() == (Integer.parseInt(cn.trim())))) {
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
                        resultDBExecutor.execute(() -> {
                            String temp = "";
                            for (Result resultDB : resultDB) {
                                if (resultDB.getPID().trim().equalsIgnoreCase(pid.trim())) {
                                    temp = resultDB.getStartTime();
                                }
                            }
                            Long StartTime = Long.parseLong(temp.trim());
                            Long ttime = endtime - StartTime;
                            Long tempNOH = 0L;
                            long tempavgWaitinQ = 0, tempavgSleeptime = 0;
                            double tempload = 0.0;
                            int c = 0;
                            {
                                for (DistributionDBRow DistTable1 : DistTable) {
                                    tempNOH += DistTable1.getNoh();
                                    //PID = rs4.getString("PID");
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
                            resultDBExecutor.execute(new UpdateResultDBafterExecVirtual(pid, "" + endtime, "" + ttime, "" + tempNOH, "" + tempload, "" + tempavgWaitinQ, "" + tempavgSleeptime));
                            //  controlpanel.Settings.distDWDBExecutor.execute(new InsDistWareHouse(Node, PID, CNO, VARTYPE, SCHEDULER, LStart, Lend, Lexec, CS, LOWL, UPL, COUNTER, Nexec, CommOH, ParOH, PRFM, XTC, fname));
                        });
                    }
                });

            }*/

        }
        //          Thread t = new Thread(new distDBBrowser());
        //        t.start();
    }

}
