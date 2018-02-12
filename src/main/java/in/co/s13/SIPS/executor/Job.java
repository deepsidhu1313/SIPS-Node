/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.executor;

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

    public Job(String jobToken) {
        this.jobToken = jobToken.trim();
    }

    @Override
    public void run() {
        System.out.println("Starting Job : "+jobToken);
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
                System.out.println("Using Chunk Scheduler For "+jobToken);
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

        GetDBFiles getDBFiles = new GetDBFiles();
        ArrayList<String> dbfiles = getDBFiles.getDBFiles("data/" + jobToken + "/.parsed/");
        for (int i = 0; i < dbfiles.size(); i++) {
            String parsedDBLoc = dbfiles.get(i);
            String simDBLoc = parsedDBLoc.replace(".parsed", ".simulated").replace("parsed", "sim");
            System.out.println("Parsed DB Loc : "+parsedDBLoc
                    +"\nSimlated DB Loc : "+simDBLoc
                    +"\nSimulated DB Loc File Exist : "+(new File(simDBLoc).exists())+"\n");
        }

    }

}
