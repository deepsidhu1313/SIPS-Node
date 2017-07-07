/*
 * Copyright (C) 2017 nika
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
package in.co.s13.SIPS.benchmarks;

import in.co.s13.SIPS.tools.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import jnt.scimark2.Constants;
import jnt.scimark2.Random;
import jnt.scimark2.kernel;
import org.json.JSONObject;

/**
 *
 * @author nika
 */
public class Benchmarks {

    public static String benchmarkCPU() {
        JSONObject result = new JSONObject();
        int FFT_size = Constants.LG_FFT_SIZE;
        int SOR_size = Constants.LG_SOR_SIZE;
        int Sparse_size_M = Constants.LG_SPARSE_SIZE_M;
        int Sparse_size_nz = Constants.LG_SPARSE_SIZE_nz;
        int LU_size = Constants.LG_LU_SIZE;
        double min_time = Constants.RESOLUTION_DEFAULT;
        double res[] = new double[6];
        Random R = new Random(Constants.RANDOM_SEED);

        res[1] = kernel.measureFFT(FFT_size, min_time, R);
        res[2] = kernel.measureSOR(SOR_size, min_time, R);
        res[3] = kernel.measureMonteCarlo(min_time, R);
        res[4] = kernel.measureSparseMatmult(Sparse_size_M,
                Sparse_size_nz, min_time, R);
        res[5] = kernel.measureLU(LU_size, min_time, R);

        res[0] = (res[1] + res[2] + res[3] + res[4] + res[5]) / 5;

        // print out results
        result.put("suite", "SciMark 2.0a");
        result.put("Composite Score", res[0]);
        result.put("FFT", res[1]);
//        if (res[1] == 0.0) {
//            System.out.println(" ERROR, INVALID NUMERICAL RESULT!");
//        } else {
//            System.out.println(res[1]);
//        }

        result.put("SOR", res[2]);
        result.put("Monte Carlo", res[3]);
        result.put("Sparse matmult", res[4]);
        result.put("LU ", res[5]);
//        if (res[5] == 0.0) {
//            System.out.println(" ERROR, INVALID NUMERICAL RESULT!");
//        } else {
//            System.out.println(res[5]);
//        }

        // print out System info
//        System.out.println();
        result.put("java.vendor: ", System.getProperty("java.vendor"));
        result.put("java.version: ", System.getProperty("java.version"));
        result.put("os.arch: ", System.getProperty("os.arch"));
        result.put("os.name: ", System.getProperty("os.name"));
        result.put("os.version: ", System.getProperty("os.version"));

        return result.toString(4);
    }

    /**
     * *
     * Code from
     * https://gitlab.com/jamesmarkchan/jDiskMark/blob/master/src/jdiskmark/DiskWorker.java
     * Credits:
     *
     * @return
     */
    public static String benchmarkHDD() {
        JSONObject result = new JSONObject();
        String mode = "rwd";
        File testFile;
        int MEGABYTE = 1024 * 1024;
        int KILOBYTE = 1024;
        int blockSize = 512 * KILOBYTE;    // size of a block in KBs
        byte[] blockArr = new byte[blockSize];
        double maxReadSpeed = 0, minReadSpeed = 0, avgReadSpeed = 0, sumReadSpeed = 0, maxWriteSpeed = 0, minWriteSpeed = 0, avgWriteSpeed = 0, sumWriteSpeed = 0;
        for (int b = 0; b < blockArr.length; b++) {
            if (b % 2 == 0) {
                blockArr[b] = (byte) 0xFF;
            }
        }
        int wUnitsComplete = 0,
                rUnitsComplete = 0,
                unitsComplete;

        int numOfBlocks = 32;     // desired number of blocks
        int numOfIterations = 25;      // desired number of marks
        int wUnitsTotal = true ? numOfBlocks * numOfIterations : 0;
        int rUnitsTotal = true ? numOfBlocks * numOfIterations : 0;
        int unitsTotal = wUnitsTotal + rUnitsTotal;

        float percentComplete;
        int startFileNum = 1;
        for (int m = startFileNum; m < startFileNum + numOfIterations; m++) {
            testFile = new File("test" + File.separator + "testdata" + m + ".jdm");
            testFile.delete();//System.out.println("" + );
            long totalBytesWrittenInMark = 0;
            long startTime = System.nanoTime();

            try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, mode)) {
                for (int b = 0; b < numOfBlocks; b++) {
//                if (App.randomEnable) {
//                    int rLoc = Util.randInt(0, numOfBlocks - 1);
//                    rAccFile.seek(rLoc * blockSize);
//                } else {
                    rAccFile.seek(b * blockSize);
//                }
                    rAccFile.write(blockArr, 0, blockSize);
                    totalBytesWrittenInMark += blockSize;
                    wUnitsComplete++;
                    unitsComplete = rUnitsComplete + wUnitsComplete;
                    percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Benchmarks.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Benchmarks.class.getName()).log(Level.SEVERE, null, ex);
            }
            long endTime = System.nanoTime();
            long elapsedTimeNs = endTime - startTime;
            double sec = (double) elapsedTimeNs / (double) 1000000000;
            double mbWritten = (double) totalBytesWrittenInMark / (double) MEGABYTE;
            double bwMbSec = (mbWritten / sec);
//            System.out.println("Write IO is " + bwMbSec + " MB/s"
//                    + "(MB written " + mbWritten + " in " + sec + " sec)");
            maxWriteSpeed = bwMbSec > maxWriteSpeed ? bwMbSec : maxWriteSpeed;
            minWriteSpeed = (m == 1) ? bwMbSec
                    : (bwMbSec < minWriteSpeed) ? bwMbSec : minWriteSpeed;
            sumWriteSpeed += bwMbSec;
        }
        avgWriteSpeed = sumWriteSpeed / numOfIterations;
        //Read Test
        for (int m = startFileNum; m < startFileNum + numOfIterations; m++) {

            testFile = new File("test" + File.separator + "testdata" + m + ".jdm");
            long startTime = System.nanoTime();
            long totalBytesReadInMark = 0;
            try {
                try (RandomAccessFile rAccFile = new RandomAccessFile(testFile, "r")) {
                    for (int b = 0; b < numOfBlocks; b++) {
//                        if (App.blockSequence == DiskRun.BlockSequence.RANDOM) {
//                            int rLoc = Util.randInt(0, numOfBlocks - 1);
//                            rAccFile.seek(rLoc * blockSize);
//                        } else {
                        rAccFile.seek(b * blockSize);
//                        }
                        rAccFile.readFully(blockArr, 0, blockSize);
                        totalBytesReadInMark += blockSize;
                        rUnitsComplete++;
                        unitsComplete = rUnitsComplete + wUnitsComplete;
                        percentComplete = (float) unitsComplete / (float) unitsTotal * 100f;

                    }
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Benchmarks.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Benchmarks.class.getName()).log(Level.SEVERE, null, ex);
            }
            long endTime = System.nanoTime();
            long elapsedTimeNs = endTime - startTime;
            double sec = (double) elapsedTimeNs / (double) 1000000000;
            double mbRead = (double) totalBytesReadInMark / (double) MEGABYTE;
            double bwMbSec = mbRead / sec;
//            System.out.println("m:" + m + " READ IO is " + bwMbSec + " MB/s    "
//                    + "(MBread " + mbRead + " in " + sec + " sec)");
            maxReadSpeed = bwMbSec > maxReadSpeed ? bwMbSec : maxReadSpeed;
            minReadSpeed = ((m == 1) ? bwMbSec
                    : (bwMbSec < minReadSpeed) ? bwMbSec : minReadSpeed);
            sumReadSpeed += bwMbSec;
        }
        avgReadSpeed = sumReadSpeed / numOfIterations;
        result.put("MaxWrite", maxWriteSpeed + " MB/s");
        result.put("MinWrite", minWriteSpeed + " MB/s");
        result.put("AvgWrite", avgWriteSpeed + " MB/s");
        result.put("MaxRead", maxReadSpeed + " MB/s");
        result.put("MinRead", minReadSpeed + " MB/s");
        result.put("AvgRead", avgReadSpeed + " MB/s");
        return result.toString(4);
    }

    public static void main(String[] args) {
        System.out.println("CPU Benchmark "+benchmarkCPU());
        System.out.println("\nHDD Benchmark "+benchmarkHDD());
    }
}
