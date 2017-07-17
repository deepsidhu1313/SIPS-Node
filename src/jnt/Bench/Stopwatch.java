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
   Provides a stopwatch to measure elapsed time.
<DL>
<DT><B>Example of use:</B></DT>
<DD>
<pre>
	Stopwatch Q = new Stopwatch;
	Q.start();
	//
	// code to be timed here ...
	//
	Q.stop();
	System.out.println("elapsed time was: " + Q.read() + " seconds.");
</pre>	

@author Roldan Pozo
@version 14 October 1997
*/
public class Stopwatch {
  private boolean running;
  private long last_time;
  private long total;

  public Stopwatch() {
    reset(); }
		
  /** 
    * Return system time (in seconds)
    */
  public void reset() { 
    running = false; 
    last_time = 0; 
    total=0; }

  /** 
    * Resume timer.
    */
  public void resume() { 
    if (!running) { 
      last_time = System.currentTimeMillis(); 
      running = true; }}

  /** 
    * Start (and reset) timer
    */
  public void start() { 
    total=0;
    last_time = System.currentTimeMillis(); 
    running = true; }
   
  /** 
    * Stop timer
    */
  public double stop() { 
    if (running) {
      total += System.currentTimeMillis() - last_time; 
      running = false; }
    return total*0.001; }
 
  /** 
    * Return the elapsed time (in seconds)
    */
  public double read() {  
    if (running) {
      long now = System.currentTimeMillis();
      total += now - last_time;
      last_time = now; }
    return total*0.001; }
}

    

            
