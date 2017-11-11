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

import in.co.s13.SIPS.settings.GlobalValues;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;

/**
 *
 * @author nika
 */
public class IPInfo implements Runnable {

    public IPInfo() {
    }

    @Override
    public void run() {
        Thread.currentThread().setName("IP Information Thread");
        try {
            GlobalValues.IP_ADDRESSES = new JSONArray(Util.getLocalHostLANAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(IPInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
