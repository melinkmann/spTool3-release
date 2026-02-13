/*
 *  Copyright 2026 Matthias Elinkmann, spTool3
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.Statistics;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class QuantileData_V2 {

  private static final Logger LOGGER = LogManager.getLogger(QuantileData_V2.class);

  private final boolean isValid;

  private final double[][][] thresholdMatrix;
  private final double[] lambdas;
  private final double[] sigmas;
  private final double[] ys;
  // private final double[] quantiles;

  public QuantileData_V2() {

    this.lambdas = loadDoubleArrayFromBinary("/lambdas.dat"); // n = 71
    this.sigmas = loadDoubleArrayFromBinary("/sigmas.dat"); // n = 71
    this.ys = loadDoubleArrayFromBinary("/ys.dat"); // n = 101
    double[] quantiles = loadFloatArrayFromBinary("/quantiles.dat"); // this is the thr data

    this.thresholdMatrix = new double[lambdas.length][sigmas.length][ys.length];

    int idx = 0;

    // build matrix from quantile array (structure below)
    for (int i = 0; i < lambdas.length; i++) {
      for (int j = 0; j < sigmas.length; j++) {
        for (int k = 0; k < ys.length; k++) {
          thresholdMatrix[i][j][k] = quantiles[idx];
          idx = idx + 1;
        }
      }
    }

    /*
    Structure
    [lambda0|sigma0|y0]     [lambda0|sigma0|y1]     [lambda0|sigma0|y2]   ...   [lambda0|sigma0|y100]
    [lambda0|sigma1|y100]   [lambda0|sigma1|y100]   [lambda0|sigma2|y100] ...   [lambda0|sigma70|y100]
    [lambda1|sigma70|y100]  [lambda2|sigma70|y100]  [lambda3|sigma70|y100] ...  [lambda70|sigma70|y100]
     */

    this.isValid = lambdas.length > 0
        && sigmas.length > 0
        && ys.length > 0
        && quantiles.length > 0;
  }

  public boolean isValid() {
    return isValid;
  }

  public double getThr(double muBG, double shapeParam, double alpha) {
    double thr = -1;

    if (isValid) {
      try {
        // table uses quantile instead of alpha
        double q = (1 - alpha);
        // table uses zero truncated quantiles
        double y0 = Statistics.zeroTruncatedQuantile(muBG, q);

        // lookup and interpolate across all 3D
        double[] lookupArr = interpolate3D(
            new double[]{muBG},
            new double[]{shapeParam},
            new double[]{y0},
            lambdas, sigmas, ys, thresholdMatrix);

        // extract first entry
        if (lookupArr.length == 1) {
          thr = lookupArr[0];
        }
      } catch (Exception e) {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
      }
    }


    return thr;
  }

  // Reverse operation
  // Easiest and fastest for starters is just binary search lookup
  public double getPValue(double muBG, double shapeParam, double value) {
    final double precisionInSignal = 1e-6;
    final int MAX_ITER = 100;

    double lowAlpha = Math.nextUp(0d);     // small alpha: large thr
    double highAlpha = Math.nextDown(1d);  // large alpha: small thr

    double p;

    // edge cases
    double thrMin = getThr(muBG, shapeParam, Math.nextDown(1d));
    double thrMax = getThr(muBG, shapeParam, Math.nextUp(0d));

    if (value < thrMin) {
      p =  Math.nextDown(1d); // p ≈ 1
    } else if (value > thrMax) {
      p=  Math.nextUp(0d);   // p ≈ 0
    } else {

      // do search
      double thrMid = 0;

      int i;
      for (i = 0; i < MAX_ITER; i++) {
        double midAlpha = 0.5 * (lowAlpha + highAlpha);
        thrMid = getThr(muBG, shapeParam, midAlpha);

        if (Math.abs(thrMid - value) <= precisionInSignal) {
          break;
        }

        // MONOTONIC DECREASING case
        if (thrMid > value) {
          lowAlpha = midAlpha;    // increase alpha → decrease thr
        } else {
          highAlpha = midAlpha;   // decrease alpha → increase thr
        }
      }
      if (i == MAX_ITER) {
        LOGGER.info("Binary search exhausted " + MAX_ITER + " iterations." +
            " Precision was not reached as target: " + value
            + " was not within " + precisionInSignal + " to the lookup of " + thrMid);
      }
      // System.out.println("i -> "+i); // 20 - 40

      double alpha = 0.5 * (lowAlpha + highAlpha);
      p = Math.max(alpha, Math.nextUp(0d));
    }
    return p;
  }


  /**
   * Cubic (actually trilinear) interpolation of (x, y, z) for 3D data.
   *
   * @param x    x positions to interpolate (can be length 1 or more)
   * @param y    y positions to interpolate
   * @param z    z positions to interpolate
   * @param xs   known x values
   * @param ys   known y values
   * @param zs   known z values
   * @param data 3D array of known values, shape [xs.length][ys.length][zs.length]
   * @return interpolated values
   */
  public double[] interpolate3D(double[] x, double[] y, double[] z,
                                double[] xs, double[] ys, double[] zs,
                                double[][][] data) {

    if (x.length != y.length || x.length != z.length) {
      throw new IllegalArgumentException("x, y, z must be same length");
    }

    if (data.length != xs.length || data[0].length != ys.length || data[0][0].length != zs.length) {
      throw new IllegalArgumentException("data shape must match xs, ys, zs lengths");
    }

    double[] result = new double[x.length];

    for (int i = 0; i < x.length; i++) {
      // searchSorted returns "right" item (higher); -1 thus give lower
      int idx0 = searchSorted(xs, x[i]) - 1;
      int idy0 = searchSorted(ys, y[i]) - 1;
      int idz0 = searchSorted(zs, z[i]) - 1;

      // cap lower AND upper so idx0+1 is always valid
      idx0 = Math.max(0, Math.min(idx0, xs.length - 2));
      idy0 = Math.max(0, Math.min(idy0, ys.length - 2));
      idz0 = Math.max(0, Math.min(idz0, zs.length - 2));

      int idx1 = idx0 + 1;
      int idy1 = idy0 + 1;
      int idz1 = idz0 + 1;

      double xd = (x[i] - xs[idx0]) / (xs[idx1] - xs[idx0]);
      double yd = (y[i] - ys[idy0]) / (ys[idy1] - ys[idy0]);
      double zd = (z[i] - zs[idz0]) / (zs[idz1] - zs[idz0]);

      double c00 = data[idx0][idy0][idz0] * (1.0 - xd) + data[idx1][idy0][idz0] * xd;
      double c01 = data[idx0][idy0][idz1] * (1.0 - xd) + data[idx1][idy0][idz1] * xd;
      double c10 = data[idx0][idy1][idz0] * (1.0 - xd) + data[idx1][idy1][idz0] * xd;
      double c11 = data[idx0][idy1][idz1] * (1.0 - xd) + data[idx1][idy1][idz1] * xd;

      double c0 = c00 * (1.0 - yd) + c10 * yd;
      double c1 = c01 * (1.0 - yd) + c11 * yd;

      result[i] = c0 * (1.0 - zd) + c1 * zd;
    }

    return result;
  }

  /**
   * Helper function: find insertion index (like np.searchsorted with side="right")
   */
  private int searchSorted(double[] arr, double val) {
    int low = 0;
    int high = arr.length;
    while (low < high) {
      int mid = (low + high) / 2;
      if (val < arr[mid]) {
        high = mid;
      } else {
        low = mid + 1;
      }
    }
    return low;
  }


  private double[] loadDoubleArrayFromBinary(String resourcePath) {
    double[] arr = new double[0];

    InputStream is = QuantileData_V2.class.getResourceAsStream(resourcePath);
    if (is != null) {
      try {
        // Read all bytes from the InputStream
        byte[] bytes = is.readAllBytes();

        // Wrap in ByteBuffer
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // assumes little-endian,

        // determine size
        int count = buffer.remaining() / Double.BYTES;
        double[] result = new double[count];

        for (int i = 0; i < count; i++) {
          result[i] = buffer.getDouble();
        }

        arr = result;
      } catch (Exception ex) {
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }
    }
    return arr;
  }

  private double[] loadFloatArrayFromBinary(String resourcePath) {
    double[] arr = new double[0];

    InputStream is = QuantileData_V2.class.getResourceAsStream(resourcePath);
    if (is != null) {
      try {
        // Read all bytes from the InputStream
        byte[] bytes = is.readAllBytes();

        // Wrap in ByteBuffer
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // assumes little-endian,

        // determine size
        int count = buffer.remaining() / Float.BYTES;
        double[] result = new double[count];

        for (int i = 0; i < count; i++) {
          result[i] = buffer.getFloat();
        }

        arr = result;
      } catch (Exception ex) {
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }
    }
    return arr;
  }


}