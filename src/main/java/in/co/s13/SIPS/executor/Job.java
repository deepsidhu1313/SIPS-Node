/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor;

import in.co.s13.SIPS.db.SQLiteJDBC;
import in.co.s13.SIPS.settings.GlobalValues;
import in.co.s13.SIPS.tools.GetDBFiles;
import in.co.s13.SIPS.tools.Util;
import in.co.s13.sips.lib.ParallelForSENP;
import in.co.s13.sips.lib.common.datastructure.ParallelForLoop;
import in.co.s13.sips.scheduler.LoadScheduler;
import in.co.s13.sips.schedulers.Chunk;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

    public Job(String jobToken) {
        this.jobToken = jobToken.trim();
    }

    @Override
    public void run() {
        System.out.println("Starting Job : " + jobToken);
        JSONObject manifestJSON = Util.readJSONFile("data/" + jobToken + "/manifest.json");
        JSONObject schedulerJSON = manifestJSON.getJSONObject("SCHEDULER", new JSONObject());
        String schedulerName = schedulerJSON.getString("Name", "NotFound");
        if (schedulerName.equals("NotFound")) {
            GlobalValues.RESULT_DB_EXECUTOR.submit(() -> {
                GlobalValues.RESULT_DB.get(jobToken).setStatus("No Scheduler Defined in Manifest");
            });
            return;
        }
        LoadScheduler loadScheduler = null;
        if (schedulerName.startsWith("in.co.s13.sips.schedulers.")) {
            if (schedulerName.endsWith("Chunk")) {
                loadScheduler = new LoadScheduler(new Chunk());
                System.out.println("Using Chunk Scheduler For " + jobToken);
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
                    String init, updatevalue, compare, type = null, varinit = null, limit = null;
                    boolean reverseLoop = false;
                    Object min = null, max = null, diff = null;
                    int datatype = 0;

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

                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(varinit)) {
                                    varinit = "" + rs98.getString("VALUE");
                                    min = Byte.parseByte(varinit);
                                }
                            }
                            rs99.close();
                            rs98.close();
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Byte.parseByte(limit);
                        } catch (NumberFormatException e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(limit)) {
                                    limit = "" + rs98.getString("VALUE");
                                    max = Byte.parseByte(limit);
                                }
                            }
                            rs99.close();
                            rs98.close();
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

                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(varinit)) {
                                    varinit = "" + rs98.getString("VALUE");
                                    min = Short.parseShort(varinit);
                                }
                            }
                            rs99.close();
                            rs98.close();
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Short.parseShort(limit);
                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(limit)) {
                                    limit = "" + rs98.getString("VALUE");
                                    max = Short.parseShort(limit);
                                }
                            }
                            rs99.close();
                            rs98.close();
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

                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(varinit)) {
                                    varinit = "" + rs98.getString("VALUE");
                                    min = Integer.parseInt(varinit);
                                }
                            }
                            rs99.close();
                            rs98.close();
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Integer.parseInt(limit);
                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(limit)) {
                                    limit = "" + rs98.getString("VALUE");
                                    max = Integer.parseInt(limit);
                                }
                            }
                            rs99.close();
                            rs98.close();
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

                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(varinit)) {
                                    varinit = "" + rs98.getString("VALUE");
                                    min = Long.parseLong(varinit);
                                }
                            }
                            rs99.close();
                            rs98.close();
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Long.parseLong(limit);
                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(limit)) {
                                    limit = "" + rs98.getString("VALUE");
                                    max = Long.parseLong(limit);
                                }
                            }
                            rs99.close();
                            rs98.close();
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

                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(varinit)) {
                                    varinit = "" + rs98.getString("VALUE");
                                    min = Float.parseFloat(varinit);
                                }
                            }
                            rs99.close();
                            rs98.close();
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Float.parseFloat(limit);
                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(limit)) {
                                    limit = "" + rs98.getString("VALUE");
                                    max = Float.parseFloat(limit);
                                }
                            }
                            rs99.close();
                            rs98.close();
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

                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine <= " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(varinit)) {
                                    varinit = "" + rs98.getString("VALUE");
                                    min = Integer.parseInt(varinit);
                                }
                            }
                            rs99.close();
                            rs98.close();
                            parsedDB.closeConnection();
                            simDB.closeConnection();
                        }
                        try {
                            max = Integer.parseInt(limit);
                        } catch (Exception e) {
                            sql = "SELECT * FROM SAVVAL WHERE BeginLine < " + (parallel4BL + 1) + " ;";
                            ResultSet rs99 = parsedDB.select(parsedDBLoc, sql);
                            int id = 1;
                            while (rs99.next()) {
                                id = rs99.getInt("ID");
                            }
                            sql = "SELECT * FROM VAL" + id + "";
                            ResultSet rs98 = simDB.select(simDBLoc, sql);
                            while (rs98.next()) {
                                if (rs98.getString("NAME").equals(limit)) {
                                    limit = "" + rs98.getString("VALUE");
                                    max = Integer.parseInt(limit);
                                }
                            }
                            rs99.close();
                            rs98.close();
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
                    System.out.println("Live Nodes AL:"+Util.getAllLiveNodesInArrayList());
                    System.out.println("Live Nodes :"+GlobalValues.LIVE_NODE_ADJ_DB);
                    ParallelForLoop parallelForLoop = new ParallelForLoop(min, max, diff, datatype, reverseLoop);
                    ArrayList<ParallelForSENP> al = loadScheduler.scheduleParallelFor(Util.getAllLiveNodesInArrayList(), parallelForLoop);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
