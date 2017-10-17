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
package in.co.s13.SIPS.tools;

import in.co.s13.SIPS.datastructure.IPHostnameCombo;

/**
 *
 * @author nika
 */
public class Format {
/**
 * Experimental
 * @param toParse
 * @return 
 */
    public static IPHostnameCombo getIPHostname(String toParse) {
        String parsed[] = toParse.trim().split("/");
        IPHostnameCombo ipHostnameCombo =new IPHostnameCombo();
        if (parsed.length == 2) {
            ipHostnameCombo.setHostname(parsed[0]);
            ipHostnameCombo.setIp(parsed[1]);
        } else if (parsed.length == 1) {
            ipHostnameCombo.setHostname("");
            ipHostnameCombo.setIp(parsed[0]);
        }
        return ipHostnameCombo;
    }
}
