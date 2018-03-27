/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import in.co.s13.SIPS.datastructure.DistributionDBRow;
import in.co.s13.SIPS.datastructure.Result;
import in.co.s13.SIPS.db.SQLiteJDBC;
import in.co.s13.SIPS.executor.parser.ModASTParallelFor;
import in.co.s13.SIPS.settings.GlobalValues;
import static in.co.s13.SIPS.settings.GlobalValues.MASTER_DIST_DB;
import static in.co.s13.SIPS.settings.GlobalValues.RESULT_DB;
import in.co.s13.SIPS.tools.GetDBFiles;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.ParallelForSENP;
import in.co.s13.sips.lib.common.datastructure.Node;
import in.co.s13.sips.lib.common.datastructure.ParallelForLoop;
import in.co.s13.sips.scheduler.LoadScheduler;
import in.co.s13.sips.schedulers.Chunk;
import in.co.s13.sips.schedulers.Factoring;
import in.co.s13.sips.schedulers.GA;
import in.co.s13.sips.schedulers.GA2;
import in.co.s13.sips.schedulers.GSS;
import in.co.s13.sips.schedulers.QSS;
import in.co.s13.sips.schedulers.TSS;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class Job implements Runnable {

    private String jobToken;

    private ArrayList<Integer> parallelForBeginLine = new ArrayList<>();
    private ArrayList<Integer> parallelForEndLine = new ArrayList<>();
    private ArrayList<ParallelForSENP> loopChunks;
    private String parent = "", file = "";
    String init, updatevalue, compare, type = null, varinit = null, limit = null;
    boolean reverseLoop = false;
    Object min = null, max = null, diff = null;
    int datatype = 0;

    public Job(String jobToken) {
        this.jobToken = jobToken.trim();
    }

    @Override
    public void run() {
        long parsingStartTime = System.currentTimeMillis();
        System.out.println("Starting Job : " + jobToken);
        JSONObject manifestJSON = Util.readJSONFile("data/" + jobToken + "/manifest.json");
        JSONObject schedulerJSON = manifestJSON.getJSONObject("SCHEDULER", new JSONObject());
        String schedulerName = schedulerJSON.getString("Name", "NotFound");
        if (schedulerName.equals("NotFound")) {
            GlobalValues.RESULT_DB_EXECUTOR.submit(() -> {
                Thread.currentThread().setName("Result DB executor thread");
                GlobalValues.RESULT_DB.get(jobToken).setStatus("No Scheduler Defined in Manifest");
            });
            return;
        }
        LoadScheduler loadScheduler = null;
        if (schedulerName.startsWith("in.co.s13.sips.schedulers.")) {
            if (schedulerName.endsWith("Chunk")) {
                loadScheduler = new LoadScheduler(new Chunk());
                System.out.println("Using Chunk Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("GA")) {
                loadScheduler = new LoadScheduler(new GA());
                System.out.println("Using GA Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("GA2")) {
                loadScheduler = new LoadScheduler(new GA2());
                System.out.println("Using GA Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("Factoring")) {
                loadScheduler = new LoadScheduler(new Factoring());
                System.out.println("Using Factoring Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("GSS")) {
                loadScheduler = new LoadScheduler(new GSS());
                System.out.println("Using GSS Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("QSS")) {
                loadScheduler = new LoadScheduler(new QSS());
                System.out.println("Using TSS Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("TSS")) {
                loadScheduler = new LoadScheduler(new TSS());
                System.out.println("Using TSS Scheduler For " + jobToken);
            }
        } else {
            try {
                loadScheduler = (LoadScheduler) Util.deserialize("data/" + jobToken + "/.simulated/" + schedulerName + ".obj");
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                GlobalValues.RESULT_DB_EXECUTOR.submit(() -> {
                    GlobalValues.RESULT_DB.get(jobToken).setStatus("Exception occured while loading scheduler");
                });
            }
        }
        if (loadScheduler == null) {
            GlobalValues.RESULT_DB_EXECUTOR.submit(() -> {
                GlobalValues.RESULT_DB.get(jobToken).setStatus("No Scheduler Loaded either missing the custom object or something wrong with inbuild schdulers");
            });
            return;
        }
        try {
            GetDBFiles getDBFiles = new GetDBFiles();
            ArrayList<String> dbfiles = getDBFiles.getDBFiles("data/" + jobToken + "/.parsed/");
            for (int i = 0; i < dbfiles.size(); i++) {

                String parsedDBLoc = dbfiles.get(i);
                String simDBLoc = parsedDBLoc.replace(".parsed", ".simulated").replace("parsed", "sim");
                System.out.println("Parsed DB Loc : " + parsedDBLoc
                        + "\nSimlated DB Loc : " + simDBLoc
                        + "\nSimulated DB Loc File Exist : " + (new File(simDBLoc).exists()) + "\n");
                SQLiteJDBC parsedDB = new SQLiteJDBC();
                SQLiteJDBC simDB = new SQLiteJDBC();
                String sql = "SELECT * FROM SYNTAX WHERE Category LIKE '%ParallelFor%';";
                ResultSet rs = parsedDB.select(parsedDBLoc, sql);
                while (rs.next()) {
                    parallelForBeginLine.add(rs.getInt("BeginLine"));
                    parallelForEndLine.add(rs.getInt("EndLine"));
                }

                parsedDB.closeConnection();
                for (int j = 0; j < parallelForBeginLine.size(); j++) {
                    int parallel4BL = parallelForBeginLine.get(j);
                    sql = "SELECT * FROM FORLOOP WHERE BeginLine = '" + (parallel4BL + 1) + "'; ";
                    ResultSet rs2 = parsedDB.select(parsedDBLoc, sql);

                    while (rs2.next()) {
                        init = rs2.getString("Init");
                        updatevalue = rs2.getString("UpdateValue");
                        compare = rs2.getString("Compare");
                    }
                    //rs2.close();
                    parsedDB.closeConnection();
                    sql = "SELECT * FROM VARIABLES ;";

                    ResultSet var = parsedDB.select(parsedDBLoc, sql);

                    while (var.next()) {
                        if (var.getInt("BeginLine") == (parallel4BL + 1)) {
                            type = var.getString("Type");
                        }
                    }

                    type = type.toUpperCase();
                    //rs3.close();
                    //sqldb.closeConnection();
                    sql = "SELECT * FROM VARDEC WHERE BeginLine = '" + (parallel4BL + 1) + "' ;";
                    ResultSet rs4 = parsedDB.select(parsedDBLoc, sql);
                    while (rs4.next()) {
                        varinit = rs4.getString("INIT");
                    }

                    //rs4.close();
                    //sqldb.closeConnection();
                    sql = "SELECT * FROM BINARYEXP  ;";
                    ResultSet rs5 = parsedDB.select(parsedDBLoc, sql);
                    while (rs5.next()) {
                        if (rs5.getInt("BeginLine") == (parallel4BL + 1)) {
                            limit = rs5.getString("Right");
                        }
                    }
                    if (type.contains("BYTE")) {
                        try {
                            min = Byte.parseByte(varinit);

                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(varinit)) {
                                            varinit = "" + rs98.getString("VALUE");
                                            min = Byte.parseByte(varinit);
                                        }
                                    }
                                }
                            }
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Byte.parseByte(limit);
                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(limit)) {
                                            limit = "" + rs98.getString("VALUE");
                                            max = Byte.parseByte(limit);
                                        }
                                    }
                                }
                            }
                            simDB.closeConnection();
                            parsedDB.closeConnection();
                        }
                        if ((byte) min > (byte) max) {
                            reverseLoop = true;
                            diff = (byte) ((byte) min - (byte) max);
                        } else {
                            diff = (byte) ((byte) max - (byte) min);
                        }
                        datatype = 0;
                    } else if (type.contains("SHORT")) {
                        try {
                            min = Short.parseShort(varinit);

                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(varinit)) {
                                            varinit = "" + rs98.getString("VALUE");
                                            min = Short.parseShort(varinit);
                                        }
                                    }
                                }
                            }
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Short.parseShort(limit);
                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(limit)) {
                                            limit = "" + rs98.getString("VALUE");
                                            max = Short.parseShort(limit);
                                        }
                                    }
                                }
                            }
                            simDB.closeConnection();
                            parsedDB.closeConnection();
                        }
                        if ((short) min > (short) max) {
                            reverseLoop = true;
                            diff = (short) ((short) min - (short) max);
                        } else {
                            diff = (short) ((short) max - (short) min);
                        }
                        datatype = 1;
                    } else if (type.contains("INT")) {
                        try {
                            min = Integer.parseInt(varinit);

                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(varinit)) {
                                            varinit = "" + rs98.getString("VALUE");
                                            min = Integer.parseInt(varinit);
                                        }
                                    }
                                }
                            }
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Integer.parseInt(limit);
                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(limit)) {
                                            limit = "" + rs98.getString("VALUE");
                                            max = Integer.parseInt(limit);
                                        }
                                    }
                                }
                            }
                            simDB.closeConnection();
                            parsedDB.closeConnection();
                        }
                        if ((int) min > (int) max) {
                            reverseLoop = true;
                            diff = (int) ((int) min - (int) max);
                        } else {
                            diff = (int) ((int) max - (int) min);
                        }
                        datatype = 2;
                    } else if (type.contains("LONG")) {
                        try {
                            min = Long.parseLong(varinit);

                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(varinit)) {
                                            varinit = "" + rs98.getString("VALUE");
                                            min = Long.parseLong(varinit);
                                        }
                                    }
                                }
                            }
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Long.parseLong(limit);
                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(limit)) {
                                            limit = "" + rs98.getString("VALUE");
                                            max = Long.parseLong(limit);
                                        }
                                    }
                                }
                            }
                            simDB.closeConnection();
                            parsedDB.closeConnection();
                        }
                        if ((long) min > (long) max) {
                            reverseLoop = true;
                            diff = (long) ((long) min - (long) max);
                        } else {
                            diff = (long) ((long) max - (long) min);
                        }
                        datatype = 3;
                    } else if (type.contains("FLOAT")) {
                        try {
                            min = Float.parseFloat(varinit);

                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(varinit)) {
                                            varinit = "" + rs98.getString("VALUE");
                                            min = Float.parseFloat(varinit);
                                        }
                                    }
                                }
                            }
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Float.parseFloat(limit);
                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(limit)) {
                                            limit = "" + rs98.getString("VALUE");
                                            max = Float.parseFloat(limit);
                                        }
                                    }
                                }
                            }
                            simDB.closeConnection();
                            parsedDB.closeConnection();
                        }
                        if ((float) min > (float) max) {
                            reverseLoop = true;
                            diff = (float) ((float) min - (float) max);
                        } else {
                            diff = (float) ((float) max - (float) min);
                        }
                        datatype = 4;
                    } else if (type.contains("DOUBLE")) {
                        try {
                            min = Integer.parseInt(varinit);

                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(varinit)) {
                                            varinit = "" + rs98.getString("VALUE");
                                            min = Integer.parseInt(varinit);
                                        }
                                    }
                                }
                            }
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Integer.parseInt(limit);
                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            try (ResultSet rs99 = parsedDB.select(parsedDBLoc, sql)) {
                                int id = 1;
                                while (rs99.next()) {
                                    id = rs99.getInt("ID");
                                }
                                sql = "SELECT * FROM VAL" + id + "";
                                try (ResultSet rs98 = simDB.select(simDBLoc, sql)) {
                                    while (rs98.next()) {
                                        if (rs98.getString("NAME").equals(limit)) {
                                            limit = "" + rs98.getString("VALUE");
                                            max = Integer.parseInt(limit);
                                        }
                                    }
                                }
                            }
                            simDB.closeConnection();
                            parsedDB.closeConnection();
                        }
                        if ((double) min > (double) max) {
                            reverseLoop = true;
                            diff = (double) ((double) min - (double) max);
                        } else {
                            diff = (double) ((double) max - (double) min);
                        }
                        datatype = 5;
                    }
                    ParallelForLoop parallelForLoop = new ParallelForLoop(min, max, diff, datatype, reverseLoop);

                    loopChunks = loadScheduler.scheduleParallelFor(Util.getAllLiveNodes(), parallelForLoop, schedulerJSON);
                    System.out.println("Parallel For Loop Chunks: " + loopChunks.toString());
                    sql = "SELECT * FROM META;";
                    ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);

                    while (rs99.next()) {
                        parent = rs99.getString("PARENT");
                        file = rs99.getString("FILE");
                    }
                    ConcurrentHashMap<String, DistributionDBRow> DistTable = new ConcurrentHashMap<>();
                    ArrayList<Node> backupNodes = loadScheduler.getBackupNodes();
                    ExecutorService jobUploadExecutor = Executors.newFixedThreadPool(GlobalValues.TASK_LIMIT*2);

                    for (int k = 0; k < loopChunks.size(); k++) {
                        final int l = k;
//                        Future<DistributionDBRow> fut = 
                        jobUploadExecutor.submit(() -> {

                            try {
                                ParallelForSENP get = loopChunks.get(l);
                                Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + l + "/src/"));
                                ModASTParallelFor ma = new ModASTParallelFor((parallel4BL + 1), datatype, get.getStart(), get.getEnd(), "" + diff);
                                try (FileInputStream in = new FileInputStream(new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + l + "/src/" + parent + "/" + file))) {
                                    CompilationUnit cu = JavaParser.parse(in);
                                    ma.visit(cu, null);
                                    //                        System.out.println("Modified AST: " + cu.toString());
                                    Util.write("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + l + "/src/" + parent + "/" + file, cu.toString());
                                    Distributor dist = new Distributor(get.getNodeUUID(), "" + l, jobToken);
                                    boolean uploaded = dist.upload();
                                    int maxTries = 3;
                                    int tries = 0;
                                    int triedBackUpNodes = 0;
                                    while (!uploaded && triedBackUpNodes < 3) {
                                        uploaded = dist.upload();
                                        tries++;
                                        if (tries == maxTries) {
                                            if (!backupNodes.isEmpty()) {
                                                get.setNodeUUID(backupNodes.get(Util.getRandomNumberInRange(0, backupNodes.size())).getUuid());
                                                dist = new Distributor(get.getNodeUUID(), "" + l, jobToken);
                                                tries = 0;
                                                triedBackUpNodes++;
                                            } else {
                                                triedBackUpNodes = 3;
                                                Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
                                                break;
                                            }
                                        }
                                    }
                                    DistTable.put(get.getNodeUUID() + "-" + l, new DistributionDBRow(0, get.getNodeUUID(), jobToken, l, datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getDiff(), get.getStart(), get.getEnd(), "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0, loopChunks.size()));
                                    if (l != 0) {
                                        MASTER_DIST_DB.replace(jobToken.trim(), DistTable);
                                    } else {
                                        MASTER_DIST_DB.put(jobToken.trim(), DistTable);
                                    }
                                }
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
//                        ParallelForSENP get = loopChunks.get(k);
//                        Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + k + "/src/"));
//                        ModASTParallelFor ma = new ModASTParallelFor((parallel4BL + 1), datatype, get.getStart(), get.getEnd(), "" + diff);
//                        FileInputStream in = new FileInputStream(new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + k + "/src/" + parent + "/" + file));
//                        CompilationUnit cu = JavaParser.parse(in);
//
//                        ma.visit(cu, null);
////                        System.out.println("Modified AST: " + cu.toString());
//                        Util.write("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + k + "/src/" + parent + "/" + file, cu.toString());
//                        Distributor dist = new Distributor(get.getNodeUUID(), "" + k, jobToken);
//                        boolean uploaded = dist.upload();
//                        int maxTries = 3;
//                        int tries = 0;
//                        int triedBackUpNodes = 0;
//                        while (!uploaded && triedBackUpNodes < 3) {
//                            uploaded = dist.upload();
//                            tries++;
//                            if (tries == maxTries) {
//                                if (!backupNodes.isEmpty()) {
//                                    get.setNodeUUID(backupNodes.get(Util.getRandomNumberInRange(0, backupNodes.size())).getUuid());
//                                    dist = new Distributor(get.getNodeUUID(), "" + k, jobToken);
//                                    tries = 0;
//                                    triedBackUpNodes++;
//                                } else {
//                                    triedBackUpNodes = 3;
//                                    Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
//                                    break;
//                                }
//                            }
//                        }
//                        DistTable.put(get.getNodeUUID() + "-" + k, new DistributionDBRow(i, get.getNodeUUID(), jobToken, k, datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getDiff(), get.getStart(), get.getEnd(), "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0));
////                        if (k != 0) {
////                            MASTER_DIST_DB.replace(jobToken.trim(), DistTable);
////                        } else {
////                            MASTER_DIST_DB.put(jobToken.trim(), DistTable);
////                        }
                    }
                    jobUploadExecutor.shutdown();
                    jobUploadExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    MASTER_DIST_DB.replace(jobToken.trim(), DistTable);
//                    MASTER_DIST_DB.put(jobToken.trim(), DistTable);
                }
            }
            Result resultDBEntry = RESULT_DB.get(jobToken.trim());
            long parsingEndTime = System.currentTimeMillis();
            if (resultDBEntry != null) {
                resultDBEntry.setTotalChunks(MASTER_DIST_DB.get(jobToken.trim()).size());
                resultDBEntry.setTotalNodes(loadScheduler.getTotalNodes());
                resultDBEntry.setStarttime(System.currentTimeMillis());
                resultDBEntry.setParsingOH(parsingEndTime - parsingStartTime);
                resultDBEntry.setStatus("Job Distributed and Started");
            }
        } catch (SQLException | InterruptedException ex) {
            Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
