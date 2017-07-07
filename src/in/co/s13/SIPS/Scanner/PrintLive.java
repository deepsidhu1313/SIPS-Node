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
package in.co.s13.SIPS.Scanner;

import static in.co.s13.SIPS.tools.Util.outPrintln;


public class PrintLive {

    PrintLive() {

    }

    public void print() {
        outPrintln("Print Executing");

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
