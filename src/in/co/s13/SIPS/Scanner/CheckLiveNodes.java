/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.Scanner;

import static in.co.s13.SIPS.settings.GlobalValues.*;
import in.co.s13.SIPS.settings.Settings;
import java.util.ArrayList;

/**
 *
 * @author Nika
 */
public class CheckLiveNodes implements Runnable {

    public static boolean livenodechecker = true;

    public CheckLiveNodes() {

        Settings.outPrintln("thread started");
    }

    @Override
    public void run() {
        
        Thread.currentThread().setName("CheckLiveNodeThread");
        ArrayList<String> livehosts = new ArrayList<>();
        {
            if (!NetScanner.iswriting) {
                liveDBExecutor.execute(() -> {
                   /* String sql = "SELECT * FROM LIVE";
                    SQLiteJDBC livedb = new SQLiteJDBC();
                    try (ResultSet rs = livedb.select("appdb/live.db", sql)) {
                    while (rs.next()) {
                    livehosts.add(rs.getString("IP"));
                    }
                    } catch (SQLException ex) {
                    Logger.getLogger(CheckLiveNodes.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    livedb.closeConnection();
                     */
                    liveNodeDB.stream().forEach((liveget) -> {
                        livehosts.add(liveget.getName());
                    });

                    for (int i = 0; i <= livehosts.size() - 1; i++) {
                        String ip = livehosts.get(i).trim();
                        Thread p1 = new Thread(new Ping(ip));
                 //       p1.setPriority(Thread.NORM_PRIORITY - 1);
                        pingExecutor.execute(p1);

                    }

                });

            }
        }}

    

    

    

    public static void main(String args[]) {
        Thread chec = new Thread(new CheckLiveNodes());
        chec.start();

    }
}
