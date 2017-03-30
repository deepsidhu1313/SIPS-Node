package in.co.s13.SIPS.Scanner;

import in.co.s13.SIPS.settings.Settings;
import java.util.ArrayList;
import static in.co.s13.SIPS.settings.GlobalValues.*;
class AddLivenodes implements Runnable {

    AddLivenodes() {

    }

    @Override
    public void run() {
        Thread.currentThread().setName("AddLiveNodeThread");
        int threads = total_threads;
        ArrayList hst = new ArrayList(NetScanner.hosts);
        int nodes = hst.size();
        //static boolean addnodes = true;

        int lastUpper = 0;
        int size = nodes / threads;
        for (int i = 1; i <= threads; i++) {
            System.out.println("Total nodes:" + nodes + " Total Threads:" + threads + " i:" + i);
            int lower;
            if (i == 1) {
                lower = lastUpper;
            } else {
                lower = lastUpper + 1;

            }
            int upper = lastUpper + size;
            lastUpper = upper;
            netExecutor.execute(new RangePinger(lower, upper, hst));

        }

        Settings.outPrintln("\nFinished all threads");
    }
}
