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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Given the range of each parameter, returns the combinations of parameter values to simulate.
 *
 * <pre>
 * List<DoePlanner.Param> params = List.of(
 *     new DoePlanner.Param(2, 60),
 *     new DoePlanner.Param(0, 10));
 *
 * List<DoePlanner.ParamCondition> a = DoePlanner.sobol(params);
 * List<DoePlanner.ParamCondition> b = DoePlanner.latinHypercube(params);
 * a.get(0).values().get(0);   // value of the first parameter in the first condition
 * </pre>
 *
 * The order of the values in a condition is the order of the parameters.
 * Both methods are deterministic: the same input always gives the same conditions.
 */
public final class DoePlanner {

  /** Range of one parameter that shall be tested. */
  public record Param(double min, double max) {
  }

  /** One test condition: one value per parameter, in the order of the parameter list. */
  public record ParamCondition(List<Double> values) {
  }

  private static final long SEED = 42;
  private static final int MAX_SOBOL_DIMENSIONS = 32;

  // ========================================================================================
  // Sobol
  // ========================================================================================

  /**
   * Scrambled Sobol sequence (low-discrepancy, up to 32 parameters).
   * Number of conditions is chosen for you: the next power of 2 that gives at least
   * 10 per parameter (6 parameters -> 64), at least 16.
   */
  public static List<ParamCondition> sobol(List<Param> params) {
    int wanted = Math.max(16, 10 * (params == null ? 0 : params.size()));
    int n = Integer.highestOneBit(wanted);
    if (n < wanted) {
      n <<= 1;
    }
    return sobol(params, n);
  }

  /**
   * Scrambled Sobol sequence with your own number of conditions.
   * Use a power of 2 (16, 32, 64, 128, ...): only then are all lower-dimensional projections
   * perfectly balanced. Other numbers work, but are slightly less even.
   */
  public static List<ParamCondition> sobol(List<Param> params, int nConditions) {
    validate(params, nConditions);
    int k = params.size();
    if (k > MAX_SOBOL_DIMENSIONS) {
      throw new IllegalArgumentException(
          "Sobol supports up to " + MAX_SOBOL_DIMENSIONS + " parameters, got " + k
              + ". Use latinHypercube() instead.");
    }

    long[][] ints = sobolInts(nConditions, k);

    // Random digital shift (scrambling): XOR each dimension with random bits.
    // Without it, the first point would sit exactly in the corner (all parameters at min).
    Random rnd = new Random(SEED);
    long[] shift = new long[k];
    for (int j = 0; j < k; j++) {
      shift[j] = rnd.nextInt() & 0xFFFFFFFFL;
    }

    double[][] unit = new double[nConditions][k];
    for (int i = 0; i < nConditions; i++) {
      for (int j = 0; j < k; j++) {
        unit[i][j] = (ints[i][j] ^ shift[j]) / 4294967296.0; // in [0, 1)
      }
    }
    return toConditions(params, unit);
  }

  // Joe-Kuo direction numbers for dimensions 2..32 (dimension 1 is the van der Corput sequence):
  // { degree s, polynomial coefficients a, initial numbers m_1 ... m_s }
  private static final int[][] SOBOL_TABLE = {
      {1, 0, 1},
      {2, 1, 1, 3},
      {3, 1, 1, 3, 1},
      {3, 2, 1, 1, 1},
      {4, 1, 1, 1, 3, 3},
      {4, 4, 1, 3, 5, 13},
      {5, 2, 1, 1, 5, 5, 17},
      {5, 4, 1, 1, 5, 5, 5},
      {5, 7, 1, 1, 7, 11, 19},
      {5, 11, 1, 1, 5, 1, 1},
      {5, 13, 1, 1, 1, 3, 11},
      {5, 14, 1, 3, 5, 5, 31},
      {6, 1, 1, 3, 3, 9, 7, 49},
      {6, 13, 1, 1, 1, 15, 21, 21},
      {6, 16, 1, 3, 1, 13, 27, 49},
      {6, 19, 1, 1, 1, 15, 7, 5},
      {6, 22, 1, 3, 1, 15, 13, 25},
      {6, 25, 1, 1, 5, 5, 19, 61},
      {7, 1, 1, 3, 7, 11, 23, 15, 103},
      {7, 4, 1, 3, 7, 13, 13, 15, 69},
      {7, 7, 1, 1, 3, 13, 7, 35, 63},
      {7, 8, 1, 3, 5, 9, 1, 25, 53},
      {7, 14, 1, 3, 1, 13, 9, 35, 107},
      {7, 19, 1, 3, 1, 5, 27, 61, 31},
      {7, 21, 1, 1, 5, 11, 19, 41, 61},
      {7, 28, 1, 3, 5, 3, 3, 13, 69},
      {7, 31, 1, 1, 7, 13, 1, 19, 1},
      {7, 32, 1, 3, 7, 5, 13, 19, 59},
      {7, 37, 1, 1, 3, 9, 25, 29, 41},
      {7, 41, 1, 3, 5, 13, 23, 1, 55},
      {7, 42, 1, 3, 7, 3, 13, 59, 17}
  };

  private static final int BITS = 32;

  /** Unscrambled Sobol points as 32-bit integers (Gray-code order, first point = 0). */
  static long[][] sobolInts(int n, int d) {
    long[][] v = new long[d][BITS];
    for (int i = 0; i < BITS; i++) {
      v[0][i] = 1L << (BITS - 1 - i);
    }
    for (int j = 1; j < d; j++) {
      int[] row = SOBOL_TABLE[j - 1];
      int s = row[0];
      int a = row[1];
      for (int i = 0; i < s; i++) {
        v[j][i] = ((long) row[2 + i]) << (BITS - 1 - i);
      }
      for (int i = s; i < BITS; i++) {
        long x = v[j][i - s] ^ (v[j][i - s] >>> s);
        for (int k = 1; k < s; k++) {
          if (((a >> (s - 1 - k)) & 1) == 1) {
            x ^= v[j][i - k];
          }
        }
        v[j][i] = x;
      }
    }

    long[][] out = new long[n][d];
    long[] x = new long[d];
    for (int i = 0; i < n; i++) {
      if (i > 0) {
        int c = Integer.numberOfTrailingZeros(~(i - 1)); // lowest zero bit of (i - 1)
        for (int j = 0; j < d; j++) {
          x[j] ^= v[j][c];
        }
      }
      System.arraycopy(x, 0, out[i], 0, d);
    }
    return out;
  }

  // ========================================================================================
  // Latin hypercube
  // ========================================================================================

  /**
   * Maximin Latin hypercube (any number of parameters).
   * Number of conditions is chosen for you: about 10 per parameter, at least 16.
   */
  public static List<ParamCondition> latinHypercube(List<Param> params) {
    return latinHypercube(params, Math.max(16, 10 * (params == null ? 0 : params.size())));
  }

  /**
   * Maximin Latin hypercube with your own number of conditions (any number of 2 or more).
   * Each parameter range is cut into n equal slices and every slice is used exactly once.
   * The combinations are then rearranged so that the conditions are spread as far apart from each
   * other as possible.
   */
  public static List<ParamCondition> latinHypercube(List<Param> params, int nConditions) {
    validate(params, nConditions);
    return toConditions(params, spreadOutLatinHypercube(nConditions, params.size()));
  }

  private static double[][] spreadOutLatinHypercube(int n, int k) {
    Random rnd = new Random(SEED);
    double[][] best = null;
    double bestScore = Double.POSITIVE_INFINITY;

    for (int restart = 0; restart < 5; restart++) {
      // start: random Latin hypercube (each column is a shuffled list of slice centers)
      double[][] x = new double[n][k];
      for (int j = 0; j < k; j++) {
        List<Integer> slices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
          slices.add(i);
        }
        Collections.shuffle(slices, rnd);
        for (int i = 0; i < n; i++) {
          x[i][j] = (slices.get(i) + 0.5) / n;
        }
      }

      // improve: swap two entries within a column, keep the swap if points move further apart
      // (capped: one swap costs ~4*n*k operations, so large designs would otherwise take minutes)
      long affordable = 100_000_000L / (4L * n * k);
      int iterations = (int) Math.max(1000, Math.min(2000L * k * Math.max(1, n / 10), affordable));
      for (int it = 0; it < iterations; it++) {
        int col = rnd.nextInt(k);
        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n - 1);
        if (b >= a) {
          b++;
        }
        double before = closenessOfPairsWith(x, a, b, n, k);
        swap(x, a, b, col);
        if (closenessOfPairsWith(x, a, b, n, k) >= before) {
          swap(x, a, b, col); // no improvement: undo
        }
      }

      double score = 0;
      for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
          score += closeness(x[i], x[j], k);
        }
      }
      if (score < bestScore) {
        bestScore = score;
        best = x;
      }
    }
    return best;
  }

  private static void swap(double[][] x, int a, int b, int col) {
    double t = x[a][col];
    x[a][col] = x[b][col];
    x[b][col] = t;
  }

  /** Sum of "closeness" over all pairs that include point a or b (each pair once). */
  private static double closenessOfPairsWith(double[][] x, int a, int b, int n, int k) {
    double s = 0;
    for (int i = 0; i < n; i++) {
      if (i != a) {
        s += closeness(x[a], x[i], k);
      }
      if (i != b && i != a) {
        s += closeness(x[b], x[i], k);
      }
    }
    return s;
  }

  /** Large when two points are close: 1 / distance^50. */
  private static double closeness(double[] p, double[] q, int k) {
    double d2 = 0;
    for (int j = 0; j < k; j++) {
      double d = p[j] - q[j];
      d2 += d * d;
    }
    double d4 = d2 * d2;
    double d8 = d4 * d4;
    double d16 = d8 * d8;
    return 1.0 / (d16 * d8 * d2); // d2^25 = distance^50
  }

  // ========================================================================================
  // Shared
  // ========================================================================================

  private static void validate(List<Param> params, int nConditions) {
    if (params == null || params.isEmpty() || nConditions < 2) {
      throw new IllegalArgumentException("Need at least 1 parameter and 2 conditions");
    }
    for (Param p : params) {
      if (!(p.min() < p.max())) {
        throw new IllegalArgumentException("Parameter range needs min < max: " + p);
      }
    }
  }

  /** Scales points from the unit cube to the parameter ranges. */
  private static List<ParamCondition> toConditions(List<Param> params, double[][] unit) {
    List<ParamCondition> conditions = new ArrayList<>(unit.length);
    for (double[] point : unit) {
      List<Double> values = new ArrayList<>(params.size());
      for (int j = 0; j < params.size(); j++) {
        Param p = params.get(j);
        values.add(p.min() + point[j] * (p.max() - p.min()));
      }
      conditions.add(new ParamCondition(List.copyOf(values)));
    }
    return conditions;
  }
}
