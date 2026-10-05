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

package math.stat;

import java.util.Arrays;

// 100% Claude Sonnet 5.5 (instructed closely!)
public final class GridSimilarity {

  /**
   * Doane's number of bins: k = 1 + log2(n) + log2(1 + |g1| / sigma_g1).
   */
  public static int doaneBins(double[] data) {
    int n = data.length;
    if (n < 3) return 1;

    double mean = Arrays.stream(data).average().orElse(0);
    double m2 = 0, m3 = 0;
    for (double v : data) {
      double d = v - mean;
      m2 += d * d;
      m3 += d * d * d;
    }
    m2 /= n;
    m3 /= n;
    if (m2 <= 0) return 1;

    double g1 = m3 / Math.pow(m2, 1.5);
    double sigmaG1 = Math.sqrt(6.0 * (n - 2) / ((n + 1.0) * (n + 3.0)));

    double k = 1 + log2(n) + log2(1 + Math.abs(g1) / sigmaG1);
    return Math.max(1, (int) Math.ceil(k));
  }

  private static double log2(double v) {
    return Math.log(v) / Math.log(2);
  }

  /**
   * Bin width for one dataset: its own range divided by its Doane bin count.
   */
  private static double doaneWidth(double[] data) {
    double range = max(data) - min(data);
    return range / doaneBins(data);
  }

  private static double min(double[] a) {
    return Arrays.stream(a).min().getAsDouble();
  }

  private static double max(double[] a) {
    return Arrays.stream(a).max().getAsDouble();
  }

  /**
   * Similarity score: sum over all grid cells of |p1 - p2|, where p is the
   * fraction of a dataset's points in that cell.
   * 0 = identical distributions, 2 = completely disjoint.
   */
  public static double score(double[] x1, double[] y1, double[] x2, double[] y2) {
    if (x1.length != y1.length || x2.length != y2.length)
      throw new IllegalArgumentException("x and y must have the same length within each dataset.");

    // Drop points where x or y is NaN/Infinite (pairs removed together).
    // Survivors are written to new arrays, so the caller's arrays stay untouched.
    double[] fx1 = new double[x1.length];
    double[] fy1 = new double[x1.length];
    int n1 = 0;
    for (int i = 0; i < x1.length; i++) {
      if (Double.isFinite(x1[i]) && Double.isFinite(y1[i])) {
        fx1[n1] = x1[i];
        fy1[n1] = y1[i];
        n1++;
      }
    }
    x1 = new double[n1];
    y1 = new double[n1];
    System.arraycopy(fx1, 0, x1, 0, n1);
    System.arraycopy(fy1, 0, y1, 0, n1);

    double[] fx2 = new double[x2.length];
    double[] fy2 = new double[x2.length];
    int n2 = 0;
    for (int i = 0; i < x2.length; i++) {
      if (Double.isFinite(x2[i]) && Double.isFinite(y2[i])) {
        fx2[n2] = x2[i];
        fy2[n2] = y2[i];
        n2++;
      }
    }
    x2 = new double[n2];
    y2 = new double[n2];
    System.arraycopy(fx2, 0, x2, 0, n2);
    System.arraycopy(fy2, 0, y2, 0, n2);

    if (x1.length == 0 || x2.length == 0)
      throw new IllegalArgumentException("Datasets must not be empty.");

    // Common grid extent (union of both datasets) so cells align.
    double xMin = Math.min(min(x1), min(x2));
    double xMax = Math.max(max(x1), max(x2));
    double yMin = Math.min(min(y1), min(y2));
    double yMax = Math.max(max(y1), max(y2));

    // Coarsest spacing of the two datasets, per dimension.
    double dx = Math.max(doaneWidth(x1), doaneWidth(x2));
    double dy = Math.max(doaneWidth(y1), doaneWidth(y2));

    int nx = dx > 0 ? Math.max(1, (int) Math.ceil((xMax - xMin) / dx)) : 1;
    int ny = dy > 0 ? Math.max(1, (int) Math.ceil((yMax - yMin) / dy)) : 1;

    int[][] c1 = bin(x1, y1, xMin, yMin, xMax, yMax, nx, ny);
    int[][] c2 = bin(x2, y2, xMin, yMin, xMax, yMax, nx, ny);

    double sum = 0;
    for (int i = 0; i < nx; i++) {
      for (int j = 0; j < ny; j++) {
        sum += Math.abs((double) c1[i][j] / x1.length - (double) c2[i][j] / x2.length);
      }
    }
    return sum;
  }

  private static int[][] bin(double[] x, double[] y,
                             double xMin, double yMin, double xMax, double yMax,
                             int nx, int ny) {
    int[][] counts = new int[nx][ny];
    double wx = (xMax - xMin) / nx;
    double wy = (yMax - yMin) / ny;

    for (int k = 0; k < x.length; k++) {
      int i = wx > 0 ? (int) ((x[k] - xMin) / wx) : 0;
      int j = wy > 0 ? (int) ((y[k] - yMin) / wy) : 0;
      // Maximum value lands on the upper edge: put it in the last cell.
      if (i >= nx) i = nx - 1;
      if (j >= ny) j = ny - 1;
      counts[i][j]++;
    }
    return counts;
  }

  /// ///

  // Add these to GridSimilarity (keeps the old score(...) untouched).
// Needs: java.util.Arrays

  private static final int MIN_POINTS = 10;       // minimum finite points per dataset
  private static final int MAX_BINS_PER_DIM = 40; // cap so the grid never explodes

  /**
   * Reference-based 2D grid distance (total variation distance).
   * <p>
   * The grid is defined ONLY by the reference dataset (x1/y1 = sample A), so every test
   * sample is scored on identical cells and scores are comparable across samples.
   * Test points outside the reference extent fall into underflow/overflow cells instead
   * of stretching the grid.
   * <p>
   * Result is in [0, 1]: 0 = identical distribution over the cells, 1 = fully disjoint.
   * Lower is better. Note: asymmetric, score(ref, test) != score(test, ref).
   * Input arrays are never modified.
   *
   * @throws IllegalArgumentException on null input, length mismatch, or too few finite points
   */
  public static double referenceScore(double[] xRef, double[] yRef, double[] xTest, double[] yTest) {
    if (xRef == null || yRef == null || xTest == null || yTest == null) {
      throw new IllegalArgumentException("Input arrays must not be null.");
    }
    if (xRef.length != yRef.length || xTest.length != yTest.length) {
      throw new IllegalArgumentException("x and y must have the same length within each dataset.");
    }

    double[][] ref = finitePairs(xRef, yRef);
    double[][] test = finitePairs(xTest, yTest);
    double[] rx = ref[0], ry = ref[1];
    double[] tx = test[0], ty = test[1];

    if (rx.length < MIN_POINTS) {
      throw new IllegalArgumentException(
          "Reference has too few finite points: " + rx.length + " (min " + MIN_POINTS + ").");
    }
    if (tx.length < MIN_POINTS) {
      throw new IllegalArgumentException(
          "Test dataset has too few finite points: " + tx.length + " (min " + MIN_POINTS + ").");
    }

    // Full extent of the reference (min/max). No trimming: all reference points,
    // including outliers, are inside the grid.
    double xLo = min(rx), xHi = max(rx);
    double yLo = min(ry), yHi = max(ry);

    // Bin counts from the reference only (Doane), clamped
    int nx = Math.min(MAX_BINS_PER_DIM, Math.max(1, doaneBins(rx)));
    int ny = Math.min(MAX_BINS_PER_DIM, Math.max(1, doaneBins(ry)));

    // A zero-width dimension (all reference values equal) collapses to one inner cell
    if (!(xHi > xLo)) nx = 1;
    if (!(yHi > yLo)) ny = 1;

    double wx = (xHi - xLo) / nx;
    double wy = (yHi - yLo) / ny;

    // index 0 = underflow, 1..n = inner cells, n+1 = overflow
    int[][] cRef = new int[nx + 2][ny + 2];
    int[][] cTest = new int[nx + 2][ny + 2];
    for (int k = 0; k < rx.length; k++) {
      cRef[cellIndex(rx[k], xLo, xHi, wx, nx)][cellIndex(ry[k], yLo, yHi, wy, ny)]++;
    }
    for (int k = 0; k < tx.length; k++) {
      cTest[cellIndex(tx[k], xLo, xHi, wx, nx)][cellIndex(ty[k], yLo, yHi, wy, ny)]++;
    }

    double sum = 0;
    for (int i = 0; i < nx + 2; i++) {
      for (int j = 0; j < ny + 2; j++) {
        sum += Math.abs((double) cRef[i][j] / rx.length - (double) cTest[i][j] / tx.length);
      }
    }
    return sum / 2.0; // total variation distance, 0..1
  }

  /** Maps a value to a cell index: 0 = underflow, 1..n = inner, n+1 = overflow. */
  private static int cellIndex(double v, double lo, double hi, double width, int n) {
    if (v < lo) return 0;
    if (v > hi) return n + 1;
    if (width <= 0) return 1;
    return 1 + Math.min(n - 1, (int) ((v - lo) / width));
  }

  /** Keeps only pairs where both x and y are finite. Returns new arrays {x, y}. */
  private static double[][] finitePairs(double[] x, double[] y) {
    double[] fx = new double[x.length];
    double[] fy = new double[x.length];
    int n = 0;
    for (int i = 0; i < x.length; i++) {
      if (Double.isFinite(x[i]) && Double.isFinite(y[i])) {
        fx[n] = x[i];
        fy[n] = y[i];
        n++;
      }
    }
    return new double[][]{Arrays.copyOf(fx, n), Arrays.copyOf(fy, n)};
  }


}