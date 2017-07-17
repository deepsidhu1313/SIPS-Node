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
package jnt.scimark2;

/**

	Provides a stopwatch to measure elapsed time.

<P>
<DL>
<DT><B>Example of use:</B></DT>
<DD>
<p>
<pre>
	Stopwatch Q = new Stopwatch;
<p>
	Q.start();
	//
	// code to be timed here ...
	//
	Q.stop();
	System.out.println("elapsed time was: " + Q.read() + " seconds.");
</pre>	

@author Roldan Pozo
@version 14 October 1997, revised 1999-04-24
*/
public class Stopwatch 
{
    private boolean running;
    private double last_time;
	private double total;


/** 
	Return system time (in seconds)

*/
	public final static double seconds()
	{
		return (System.currentTimeMillis() * 0.001);
	}
		
/** 
	Return system time (in seconds)

*/
	public void reset() 
	{ 
		running = false; 
		last_time = 0.0; 
		total=0.0; 
	}
	
    public Stopwatch()
	{
		reset();
	}
    

/** 
	Start (and reset) timer

*/
  	public  void start() 
	{ 
		if (!running)
		{
			running = true;
			total = 0.0;
			last_time = seconds(); 
		}
	}
   
/** 
	Resume timing, after stopping.  (Does not wipe out
		accumulated times.)

*/
  	public  void resume() 
	{ 
		if (!running)
		{
			last_time = seconds(); 
			running = true;
		}
	}
   
   
/** 
	Stop timer

*/
   public  double stop()  
	{ 
		if (running) 
        {
			total += seconds() - last_time; 
            running = false;
        }
        return total; 
    }
  
 
/** 
	Display the elapsed time (in seconds)

*/
   public  double read()   
	{  
		if (running) 
       	{
			total += seconds() - last_time;
			last_time = seconds();
		}
        return total;
	}
		
}

    

            
