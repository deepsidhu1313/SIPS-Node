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
import in.co.s13.SIPS.tools.Commentator;
import in.co.s13.SIPS.tools.GetDBFiles;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.common.datastructure.FileCoverage;
import in.co.s13.sips.lib.common.datastructure.ParallelForSENP;
import in.co.s13.sips.lib.common.datastructure.Node;
import in.co.s13.sips.lib.common.datastructure.ParallelForLoop;
import in.co.s13.sips.lib.common.datastructure.SIPSTask;
import in.co.s13.sips.scheduler.LoadScheduler;
import in.co.s13.sips.schedulers.Chunk;
import in.co.s13.sips.schedulers.Factoring;
import in.co.s13.sips.schedulers.GA;
import in.co.s13.sips.schedulers.GATDS;
import in.co.s13.sips.schedulers.GSS;
import in.co.s13.sips.schedulers.QSS;
import in.co.s13.sips.schedulers.TSS;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class Job implements Runnable {

    private String jobToken;

    private LoadScheduler loadScheduler = null;
    private ArrayList<Integer> parallelForBeginLine = new ArrayList<>();
    private ArrayList<Integer> parallelForEndLine = new ArrayList<>();
    private ArrayList<ParallelForSENP> loopChunks;
    private String parent = "", file = "";
    String init, updatevalue, compare, type = null, varinit = null, limit = null;
    boolean reverseLoop = false;
    Object min = null, max = null, diff = null;
    int datatype = 0, duplicates = 0;
    private long schedulingOHStart = Long.MIN_VALUE;

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
        if (schedulerName.startsWith("in.co.s13.sips.schedulers.")) {
            if (schedulerName.endsWith("Chunk")) {
                loadScheduler = new LoadScheduler(new Chunk());
                System.out.println("Using Chunk Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("GA")) {
                loadScheduler = new LoadScheduler(new GA());
                System.out.println("Using GA Scheduler For " + jobToken);
            } else if (schedulerName.endsWith("GATDS")) {
                loadScheduler = new LoadScheduler(new GATDS());
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
                GlobalValues.RESULT_DB.get(jobToken).setStatus("No Scheduler Loaded either missing the custom object or something wrong with inbuild schedulers");
            });
            return;
        }
        try {
            GetDBFiles getDBFiles = new GetDBFiles();
            ArrayList<String> dbfiles = getDBFiles.getDBFiles("data/" + jobToken + "/.parsed/");
            for (int i = 0; i < dbfiles.size(); i++) {
                String parsedDBLoc = dbfiles.get(i);
                if (!parsedDBLoc.endsWith("tasks.db")) {
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
                        schedulingOHStart = System.currentTimeMillis();
                        loopChunks = loadScheduler.scheduleParallelFor(Util.getAllLiveNodes(), parallelForLoop, schedulerJSON);
                        System.out.println("Parallel For Loop Chunks: " + loopChunks.toString());
                        sql = "SELECT * FROM META;";
                        ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);

                        while (rs99.next()) {
                            parent = rs99.getString("PARENT");
                            file = rs99.getString("FILE");
                        }
                        ConcurrentHashMap<String, DistributionDBRow> distTable = new ConcurrentHashMap<>();
                        ArrayList<Node> backupNodes = loadScheduler.getBackupNodes();

                        ExecutorService jobUploadExecutor = Executors.newFixedThreadPool(GlobalValues.TASK_LIMIT * 2);
                        ArrayList<ParallelForSENP> withDuplicates = loopChunks.stream().filter(l -> l.hasDuplicates()).collect(Collectors.toCollection(ArrayList::new));
                        ArrayList<ParallelForSENP> withoutDuplicates = loopChunks.stream().filter(l -> !(l.hasDuplicates())).collect(Collectors.toCollection(ArrayList::new));
                        ArrayList<ParallelForSENP> duplicates = loopChunks.stream().filter(l -> (l.isDuplicate())).collect(Collectors.toCollection(ArrayList::new));
                        for (int k = 0; k < withoutDuplicates.size(); k++) {
                            final int l = k;
//                        Future<DistributionDBRow> fut = 
                            jobUploadExecutor.submit(() -> {

                                try {
                                    ParallelForSENP get = withoutDuplicates.get(l);
                                    Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/"));
                                    ModASTParallelFor ma = new ModASTParallelFor((parallel4BL + 1), datatype, get.getStart(), get.getEnd(), "" + diff);
                                    try (FileInputStream in = new FileInputStream(new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/" + parent + "/" + file))) {
                                        CompilationUnit cu = JavaParser.parse(in);
                                        ma.visit(cu, null);
                                        //                        System.out.println("Modified AST: " + cu.toString());
                                        Util.write("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/" + parent + "/" + file, cu.toString());
                                        Distributor dist = new Distributor(get.getNodeUUID(), "" + get.getChunkNo(), jobToken);
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
                                                    dist = new Distributor(get.getNodeUUID(), "" + get.getChunkNo(), jobToken);
                                                    tries = 0;
                                                    triedBackUpNodes++;
                                                } else {
                                                    triedBackUpNodes = 3;
                                                    Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
                                                    break;
                                                }
                                            }
                                        }
                                        DistributionDBRow distRow = new DistributionDBRow(0, get.getNodeUUID(), jobToken, get.getChunkNo(), datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getDiff(), get.getStart(), get.getEnd(), "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0, loadScheduler.getTotalChunks());
                                        distTable.put(get.getNodeUUID() + "-" + get.getChunkNo(), distRow);
                                        if (l != 0) {
                                            MASTER_DIST_DB.replace(jobToken.trim(), distTable);
                                        } else {
                                            MASTER_DIST_DB.put(jobToken.trim(), distTable);
                                        }
                                    }
                                } catch (FileNotFoundException ex) {
                                    Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                    Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });
                        }
                        MASTER_DIST_DB.replace(jobToken.trim(), distTable);
                        for (int k = 0; k < withDuplicates.size(); k++) {
                            final int l = k;
//                        Future<DistributionDBRow> fut = 
                            jobUploadExecutor.submit(() -> {

                                try {
                                    ParallelForSENP get = withDuplicates.get(l);
                                    Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/"));
                                    ModASTParallelFor ma = new ModASTParallelFor((parallel4BL + 1), datatype, get.getStart(), get.getEnd(), "" + diff);
                                    try (FileInputStream in = new FileInputStream(new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/" + parent + "/" + file))) {
                                        CompilationUnit cu = JavaParser.parse(in);
                                        ma.visit(cu, null);
                                        //                        System.out.println("Modified AST: " + cu.toString());
                                        Util.write("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/" + parent + "/" + file, cu.toString());
                                        Distributor dist = new Distributor(get.getNodeUUID(), "" + get.getChunkNo(), jobToken);
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
                                                    dist = new Distributor(get.getNodeUUID(), "" + get.getChunkNo(), jobToken);
                                                    tries = 0;
                                                    triedBackUpNodes++;
                                                } else {
                                                    triedBackUpNodes = 3;
                                                    Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
                                                    break;
                                                }
                                            }
                                        }
                                        DistributionDBRow distRow = new DistributionDBRow(0, get.getNodeUUID(), jobToken, get.getChunkNo(), datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getDiff(), get.getStart(), get.getEnd(), "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0, loadScheduler.getTotalChunks());
                                        get.getDuplicates().stream().forEach(value -> distRow.addDuplicate(value));
                                        distTable.put(get.getNodeUUID() + "-" + get.getChunkNo(), distRow);
                                        if (l != 0) {
                                            MASTER_DIST_DB.replace(jobToken.trim(), distTable);
                                        } else {
                                            MASTER_DIST_DB.put(jobToken.trim(), distTable);
                                        }
                                    }
                                } catch (FileNotFoundException ex) {
                                    Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                    Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });
                        }
                        MASTER_DIST_DB.replace(jobToken.trim(), distTable);
                        this.duplicates = duplicates.size();
                        for (int k = 0; k < duplicates.size(); k++) {
                            final int l = k;
//                        Future<DistributionDBRow> fut = 
                            jobUploadExecutor.submit(() -> {

                                try {
                                    ParallelForSENP get = duplicates.get(l);
                                    Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/"));
                                    ModASTParallelFor ma = new ModASTParallelFor((parallel4BL + 1), datatype, get.getStart(), get.getEnd(), "" + diff);
                                    try (FileInputStream in = new FileInputStream(new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/" + parent + "/" + file))) {
                                        CompilationUnit cu = JavaParser.parse(in);
                                        ma.visit(cu, null);
                                        //                        System.out.println("Modified AST: " + cu.toString());
                                        Util.write("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getChunkNo() + "/src/" + parent + "/" + file, cu.toString());
                                        Distributor dist = new Distributor(get.getNodeUUID(), "" + get.getChunkNo(), jobToken);
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
                                                    dist = new Distributor(get.getNodeUUID(), "" + get.getChunkNo(), jobToken);
                                                    tries = 0;
                                                    triedBackUpNodes++;
                                                } else {
                                                    triedBackUpNodes = 3;
                                                    Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
                                                    break;
                                                }
                                            }
                                        }
                                        DistributionDBRow distRow = new DistributionDBRow(0, get.getNodeUUID(), jobToken, get.getChunkNo(), datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getDiff(), get.getStart(), get.getEnd(), "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0, loadScheduler.getTotalChunks());
                                        distRow.setDuplicateOf(get.getDuplicateOf());
                                        distTable.put(get.getNodeUUID() + "-" + get.getChunkNo(), distRow);
                                        if (l != 0) {
                                            MASTER_DIST_DB.replace(jobToken.trim(), distTable);
                                        } else {
                                            MASTER_DIST_DB.put(jobToken.trim(), distTable);
                                        }
                                    }
                                } catch (FileNotFoundException ex) {
                                    Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                    Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });
                        }
                        MASTER_DIST_DB.replace(jobToken.trim(), distTable);
                        jobUploadExecutor.shutdown();
                        jobUploadExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

//                    MASTER_DIST_DB.put(jobToken.trim(), DistTable);
                    }
                } else {
                    SQLiteJDBC parsedDB = new SQLiteJDBC();
                    ResultSet rs = parsedDB.select(parsedDBLoc, "SELECT * FROM TASKS;");
                    ConcurrentHashMap<String, SIPSTask> tasks = new ConcurrentHashMap<>();
                    while (rs.next()) {
                        String name = (rs.getString("Name"));
                        if (tasks.containsKey(name)) {
                            SIPSTask task = tasks.get(name);
                            JSONArray resources = new JSONArray(rs.getString("Resources"));
                            for (int j = 0; j < resources.length(); j++) {
                                String object = resources.getString(j);
                                task.addResource(object);
                            }
                            task.addFile(new FileCoverage(rs.getString("File"), rs.getInt("BeginLine"), rs.getInt("BeginColumn"), rs.getInt("EndLine"), rs.getInt("EndColumn")));
                            task.setLength(new BigDecimal(rs.getDouble("Length")));
                            task.setTimeout(new BigInteger(rs.getString("Timeout")));

                            tasks.replace(name, task);

                        } else {
                            SIPSTask task = new SIPSTask(rs.getInt("ID"), name);
                            JSONArray resources = new JSONArray(rs.getString("Resources"));
                            for (int j = 0; j < resources.length(); j++) {
                                String object = resources.getString(j);
                                task.addResource(object);
                            }
                            task.addFile(new FileCoverage(rs.getString("File"), rs.getInt("BeginLine"), rs.getInt("BeginColumn"), rs.getInt("EndLine"), rs.getInt("EndColumn")));
                            task.setLength(new BigDecimal(rs.getDouble("Length")));
                            task.setTimeout(new BigInteger(rs.getString("Timeout")));

                            tasks.put(name, task);
                        }
                    }
                    parsedDB.closeConnection();
                    schedulingOHStart = System.currentTimeMillis();
                    ArrayList<SIPSTask> result = loadScheduler.schedule(Util.getAllLiveNodes(), tasks, schedulerJSON);
                    ArrayList<SIPSTask> withDuplicates = result.stream().filter(l -> l.hasDuplicates()).collect(Collectors.toCollection(ArrayList::new));
                    ArrayList<SIPSTask> withoutDuplicates = result.stream().filter(l -> !(l.hasDuplicates())).collect(Collectors.toCollection(ArrayList::new));
                    ArrayList<SIPSTask> duplicates = result.stream().filter(l -> (l.isDuplicate())).collect(Collectors.toCollection(ArrayList::new));
                    ConcurrentHashMap<String, DistributionDBRow> DistTable = new ConcurrentHashMap<>();
                    ExecutorService jobUploadExecutor = Executors.newFixedThreadPool(GlobalValues.TASK_LIMIT * 2);
                    ArrayList<Node> backupNodes = loadScheduler.getBackupNodes();

                    for (int k = 0; k < withoutDuplicates.size(); k++) {
                        final int l = k;
                        jobUploadExecutor.submit(() -> {

                            SIPSTask get = withoutDuplicates.get(l);
                            Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getId() + "/src/"));
                            for (int j = 0; j < result.size(); j++) {
                                SIPSTask get1 = result.get(j);
                                if (!get.equals(get1)) {
                                    ArrayList<FileCoverage> filecoverages = get1.getFiles();
                                    for (int m = 0; m < filecoverages.size(); m++) {
                                        FileCoverage get2 = filecoverages.get(m);
                                        Commentator commentator = new Commentator("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getId() + "/src/" + get2.getPath(), get2.getBeginLine(), get2.getBeginColumn(), get2.getEndLine(), get2.getEndColumn());
                                    }
                                }
                            }

                            Distributor dist = new Distributor(get.getNodeUUID(), "" + get.getId(), jobToken);
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
                                        dist = new Distributor(get.getNodeUUID(), "" + get.getId(), jobToken);
                                        tries = 0;
                                        triedBackUpNodes++;
                                    } else {
                                        triedBackUpNodes = 3;
                                        Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
                                        break;
                                    }
                                }
                            }
                            DistributionDBRow distRow = new DistributionDBRow(0, get.getNodeUUID(), jobToken, get.getId(), datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getLength().toString(), "", "", "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0, loadScheduler.getTotalChunks());
                            DistTable.put(get.getNodeUUID() + "-" + get.getId(), distRow);
                            if (l != 0) {
                                MASTER_DIST_DB.replace(jobToken.trim(), DistTable);
                            } else {
                                MASTER_DIST_DB.put(jobToken.trim(), DistTable);
                            }

                        });
                    }
                    MASTER_DIST_DB.replace(jobToken.trim(), DistTable);

                    for (int k = 0; k < duplicates.size(); k++) {
                        final int l = k;
                        jobUploadExecutor.submit(() -> {

                            SIPSTask get = duplicates.get(l);
                            Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getId() + "/src/"));
                            for (int j = 0; j < result.size(); j++) {
                                SIPSTask get1 = result.get(j);
                                if (!get.equals(get1)) {
                                    ArrayList<FileCoverage> filecoverages = get1.getFiles();
                                    for (int m = 0; m < filecoverages.size(); m++) {
                                        FileCoverage get2 = filecoverages.get(m);
                                        Commentator commentator = new Commentator("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getId() + "/src/" + get2.getPath(), get2.getBeginLine(), get2.getBeginColumn(), get2.getEndLine(), get2.getEndColumn());
                                    }
                                }
                            }

                            Distributor dist = new Distributor(get.getNodeUUID(), "" + get.getId(), jobToken);
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
                                        dist = new Distributor(get.getNodeUUID(), "" + get.getId(), jobToken);
                                        tries = 0;
                                        triedBackUpNodes++;
                                    } else {
                                        triedBackUpNodes = 3;
                                        Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
                                        break;
                                    }
                                }
                            }
                            DistributionDBRow distRow = new DistributionDBRow(0, get.getNodeUUID(), jobToken, get.getId(), datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getLength().toString(), "", "", "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0, loadScheduler.getTotalChunks());
                            DistTable.put(get.getNodeUUID() + "-" + get.getId(), distRow);
                            if (l != 0) {
                                MASTER_DIST_DB.replace(jobToken.trim(), DistTable);
                            } else {
                                MASTER_DIST_DB.put(jobToken.trim(), DistTable);
                            }

                        });
                    }
                    MASTER_DIST_DB.replace(jobToken.trim(), DistTable);

                    for (int k = 0; k < withDuplicates.size(); k++) {
                        final int l = k;
                        jobUploadExecutor.submit(() -> {

                            SIPSTask get = withDuplicates.get(l);
                            Util.copyFolder(new File("data/" + jobToken + "/src/"), new File("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getId() + "/src/"));
                            for (int j = 0; j < result.size(); j++) {
                                SIPSTask get1 = result.get(j);
                                if (!get.equals(get1)) {
                                    ArrayList<FileCoverage> filecoverages = get1.getFiles();
                                    for (int m = 0; m < filecoverages.size(); m++) {
                                        FileCoverage get2 = filecoverages.get(m);
                                        Commentator commentator = new Commentator("data/" + jobToken + "/dist/" + get.getNodeUUID() + ":CN:" + get.getId() + "/src/" + get2.getPath(), get2.getBeginLine(), get2.getBeginColumn(), get2.getEndLine(), get2.getEndColumn());
                                    }
                                }
                            }

                            Distributor dist = new Distributor(get.getNodeUUID(), "" + get.getId(), jobToken);
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
                                        dist = new Distributor(get.getNodeUUID(), "" + get.getId(), jobToken);
                                        tries = 0;
                                        triedBackUpNodes++;
                                    } else {
                                        triedBackUpNodes = 3;
                                        Util.appendToJobDistributorLog(GlobalValues.LOG_LEVEL.ERROR, "Failed To Distribute Job " + jobToken);
                                        break;
                                    }
                                }
                            }
                            DistributionDBRow distRow = new DistributionDBRow(0, get.getNodeUUID(), jobToken, get.getId(), datatype, schedulerName, System.currentTimeMillis(), 0, 0, 0, 0, 0, 0, 0, 0, 0, get.getLength().toString(), "", "", "0", 0, 9999, dist.getToIPAddress(), dist.getHostName(), 0, loadScheduler.getTotalChunks());
                            DistTable.put(get.getNodeUUID() + "-" + get.getId(), distRow);
                            if (l != 0) {
                                MASTER_DIST_DB.replace(jobToken.trim(), DistTable);
                            } else {
                                MASTER_DIST_DB.put(jobToken.trim(), DistTable);
                            }

                        });
                    }
                    MASTER_DIST_DB.replace(jobToken.trim(), DistTable);
                    jobUploadExecutor.shutdown();
                    jobUploadExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

                }
            }
            Result resultDBEntry = RESULT_DB.get(jobToken.trim());
            long parsingEndTime = System.currentTimeMillis();
            if (resultDBEntry != null) {
                resultDBEntry.setTotalChunks(MASTER_DIST_DB.get(jobToken.trim()).size());
                resultDBEntry.setTotalNodes(loadScheduler.getTotalNodes());
                resultDBEntry.setSelectedNodes(loadScheduler.getSelectedNodes());
                resultDBEntry.setStarttime(System.currentTimeMillis());
                resultDBEntry.setParsingOH(parsingEndTime - parsingStartTime);
                resultDBEntry.setSchedulingOH(parsingEndTime - schedulingOHStart);
                resultDBEntry.setDuplicates(duplicates);
                resultDBEntry.setStatus("Job Distributed and Started");
            }
        } catch (SQLException | InterruptedException ex) {
            Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
