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
 * Estimate Pi by approximating the area of a circle.
 *
 * How: generate N random numbers in the unit square, (0,0) to (1,1) and see how
 * are within a radius of 1 or less, i.e.
 * <pre>  *
 * sqrt(x^2 + y^2) < r
 *
 * </pre> since the radius is 1.0, we can square both sides and avoid a sqrt()
 * computation:
 * <pre>
 *
 * x^2 + y^2 <= 1.0
 *
 * </pre> this area under the curve is (Pi * r^2)/ 4.0, and the area of the unit
 * of square is 1.0, so Pi can be approximated by
 * <pre>
 * # points with x^2+y^2 < 1
 * Pi =~ 		--------------------------  * 4.0
 * total # points
 *
 * </pre>
 *
 */
public class MonteCarlo {

    final static int SEED = 113;

    public static final double num_flops(int Num_samples) {
        // 3 flops in x^2+y^2 and 1 flop in random routine

        return ((double) Num_samples) * 4.0;

    }

    public static final double integrate(int Num_samples) {

        Random R = new Random(SEED);

        int under_curve = 0;
        for (int count = 0; count < Num_samples; count++) {
            double x = R.nextDouble();
            double y = R.nextDouble();

            if (x * x + y * y <= 1.0) {
                under_curve++;
            }

        }

        return ((double) under_curve / Num_samples) * 4.0;
    }

}
