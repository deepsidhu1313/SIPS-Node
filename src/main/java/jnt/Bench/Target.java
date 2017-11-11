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
package jnt.Bench;

/**
 * Interface for a Benchmark Target. Code to be measured by the Bench framework
 * should provide a class implementing this interface. Place the code to be
 * measured in the execute method.
 *
 * @author Bruce R. Miller (bruce.miller@nist.gov)
 * @author Contribution of the National Institute of Standards and Technology,
 * @author not subject to copyright.
 */
public interface Target {

    /**
     * The code to be measured is placed in this method.
     *
     * @return null lets jnt.Bench.Bench handle the timings. Otherwise, return
     * an array containing the one or more measured values.
     * @see jnt.Bench.Bench [start|stop|reset]Timer methods for measurement
     * tools.
     */
    public double[] execute(Bench bench) throws Exception;
}
