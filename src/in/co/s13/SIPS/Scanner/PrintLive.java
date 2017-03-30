package in.co.s13.SIPS.Scanner;

import in.co.s13.SIPS.settings.Settings;


public class PrintLive {

    PrintLive() {

    }

    public void print() {
        Settings.outPrintln("Print Executing");

     /*   for (int i = 0; i <= NetScanner.livehosts.size() - 1; i++) {

            settings.outPrintln("" + NetScanner.livehosts.get(i) + " is Live");
            settings.outPrintln("Print Executing");

        }
*/
    }

    public static void main(String args[]) {

        PrintLive p = new PrintLive();
        p.print();
    }
}
