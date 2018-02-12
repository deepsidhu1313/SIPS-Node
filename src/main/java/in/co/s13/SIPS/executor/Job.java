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
import in.co.s13.sips.scheduler.LoadScheduler;
import in.co.s13.sips.scheduler.Scheduler;
import in.co.s13.sips.schedulers.Chunk;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
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
                    String init, updatevalue, compare, type = null, varinit, limit;
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
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
