/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.co.s13.SIPS.Scanner;

import java.util.ArrayList;
import static in.co.s13.SIPS.settings.GlobalValues.*;
/**
 *
 * @author Nika
 */
public class RangePinger implements Runnable {

    int low, up;
    ArrayList<String> temp;

    public RangePinger(int min, int max, ArrayList al) {
        low = min;
        up = max;
        temp = new ArrayList(al);
        if (up == temp.size()) {
            up--;
            System.out.println("UP equals to array , decremented");
        }
        if (up > temp.size()) {
            up = temp.size() - 1;

            System.out.println("Size equals to array , decremented");
        }
        System.out.println("Executing Ping on AL" + al + " from " + low + " to " + up);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("RangePingerThread");
        for (int i = low; i <= up; i++) {

            Thread p1 = new Thread(new Ping(temp.get(i)));
        //    p1.setPriority(Thread.NORM_PRIORITY - 1);
            pingExecutor.execute(p1);

        }
    }

}
