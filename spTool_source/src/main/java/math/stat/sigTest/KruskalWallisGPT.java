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

package math.stat.sigTest;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// chatGPT guided to using Apache Commons wherever possible.

public abstract class KruskalWallisGPT {

  public static class Result {
    public final double hStatistic;
    public final double pValue;
    public final int degreesOfFreedom;

    public Result(double hStatistic, double pValue, int degreesOfFreedom) {
      this.hStatistic = hStatistic;
      this.pValue = pValue;
      this.degreesOfFreedom = degreesOfFreedom;
    }
  }

  // Claude Sonnet 4.6 correction: rank() now returns both the midranks and the
  // tie correction sum, so tie detection is performed exactly once and the
  // correction factor C is guaranteed to be consistent with the midranks used
  // to compute H.
  private static class RankResult {
    final double[] ranks;
    final double tieCorrection;

    RankResult(double[] ranks, double tieCorrection) {
      this.ranks = ranks;
      this.tieCorrection = tieCorrection;
    }
  }

  public static Result kruskalWallis(List<double[]> groups) {
    List<Double> allValues = new ArrayList<>();
    List<Integer> groupLabels = new ArrayList<>();

    // Flatten all groups and label each value
    for (int i = 0; i < groups.size(); i++) {
      for (double val : groups.get(i)) {
        allValues.add(val);
        groupLabels.add(i);
      }
    }

    int N = allValues.size();

    // Rank all values with midranks; tie correction sum returned together
    RankResult rr = rank(allValues);
    double[] ranks = rr.ranks;

    // Step 1: Sum of ranks per group
    double[] rankSums = new double[groups.size()];
    int[] groupSizes = new int[groups.size()];

    for (int i = 0; i < ranks.length; i++) {
      int group = groupLabels.get(i);
      rankSums[group] += ranks[i];
      groupSizes[group]++;
    }

    // Step 2: Compute uncorrected H statistic
    double hNumerator = 0.0;
    for (int i = 0; i < groups.size(); i++) {
      double Ri = rankSums[i];
      int ni = groupSizes[i];
      hNumerator += (Ri * Ri) / ni;
    }

    double H = (12.0 / (N * (N + 1))) * hNumerator - 3 * (N + 1);

    // Step 3: Tie correction — reuse value from rank() instead of re-traversing
    double C = 1.0;
    if (rr.tieCorrection > 0) {
      C = 1.0 - rr.tieCorrection / ((double) N * N * N - N);
    }

    double H_corrected = H / C;

    // Step 4: Degrees of freedom
    int df = groups.size() - 1;

    // Step 5: p-value
    ChiSquaredDistribution chiDist = new ChiSquaredDistribution(df);
    double pValue = 1.0 - chiDist.cumulativeProbability(H_corrected);

    return new Result(H_corrected, pValue, df);
  }

  // Assign midranks for ties; also accumulates tie correction sum (Σ t³-t)
  private static RankResult rank(List<Double> values) {
    int n = values.size();
    Integer[] indices = new Integer[n];
    for (int i = 0; i < n; i++) indices[i] = i;

    Arrays.sort(indices, Comparator.comparingDouble(values::get));

    double[] ranks = new double[n];
    double tieCorrection = 0.0;
    int i = 0;
    while (i < n) {
      int j = i;
      // Claude Sonnet 4.6 correction: replaced .equals() with Double.compare()
      // to avoid fragile boxed-Double equality on computed floating-point values.
      while (j + 1 < n && Double.compare(values.get(indices[j + 1]), values.get(indices[i])) == 0) {
        j++;
      }
      double rank = (i + j + 2) / 2.0;
      for (int k = i; k <= j; k++) {
        ranks[indices[k]] = rank;
      }
      int t = j - i + 1;
      if (t > 1) {
        tieCorrection += (double) t * t * t - t;
      }
      i = j + 1;
    }
    return new RankResult(ranks, tieCorrection);
  }
}