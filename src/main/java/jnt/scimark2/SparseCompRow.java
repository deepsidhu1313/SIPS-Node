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

public class SparseCompRow {

    /* multiple iterations used to make kernel have roughly
		same granulairty as other Scimark kernels. */
    public static double num_flops(int N, int nz, int num_iterations) {
        /* Note that if nz does not divide N evenly, then the
		   actual number of nonzeros used is adjusted slightly.
         */
        int actual_nz = (nz / N) * N;
        return ((double) actual_nz) * 2.0 * ((double) num_iterations);
    }


    /* computes  a matrix-vector multiply with a sparse matrix
		held in compress-row format.  If the size of the matrix
		in MxN with nz nonzeros, then the val[] is the nz nonzeros,
		with its ith entry in column col[i].  The integer vector row[]
		is of size M+1 and row[i] points to the begining of the
		ith row in col[].  
     */
    public static void matmult(double y[], double val[], int row[],
            int col[], double x[], int NUM_ITERATIONS) {
        int M = row.length - 1;

        for (int reps = 0; reps < NUM_ITERATIONS; reps++) {

            for (int r = 0; r < M; r++) {
                double sum = 0.0;
                int rowR = row[r];
                int rowRp1 = row[r + 1];
                for (int i = rowR; i < rowRp1; i++) {
                    sum += x[col[i]] * val[i];
                }
                y[r] = sum;
            }
        }
    }

}
