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

import org.apache.commons.math3.distribution.TDistribution;

import java.util.Arrays;

// chatGPT guided to using Apache Commons wherever possible.

public abstract class BrunnerMunzelGPT {

  public static class Result {
    public final double statistic;
    public final double pValue;

    public Result(double statistic, double pValue) {
      this.statistic = statistic;
      this.pValue = pValue;
    }
  }

  /**
   * Brunner-Munzel test for stochastic equality between two independent samples.
   * @param x first sample
   * @param y second sample
   * @return Brunner-Munzel statistic and two-tailed p-value
   */
  public static Result brunnerMunzel(double[] x, double[] y) {
    int nx = x.length;
    int ny = y.length;

    // Step 1: compute pairwise probabilities p_ij
    double[] p_i = new double[nx];
    double[] q_j = new double[ny];

    for (int i = 0; i < nx; i++) {
      double sum = 0.0;
      for (int j = 0; j < ny; j++) {
        if (x[i] > y[j]) sum += 1.0;
        else if (x[i] == y[j]) sum += 0.5;
      }
      p_i[i] = sum / ny;
    }

    for (int j = 0; j < ny; j++) {
      double sum = 0.0;
      for (int i = 0; i < nx; i++) {
        if (y[j] > x[i]) sum += 1.0;
        else if (y[j] == x[i]) sum += 0.5;
      }
      q_j[j] = sum / nx;
    }

    // Step 2: overall p_hat
    double p_hat = Arrays.stream(p_i).average().orElse(0.5);

    // Step 3: variance estimate
    double sX2 = 0.0;
    for (double pi : p_i) {
      sX2 += Math.pow(pi - p_hat, 2);
    }
    sX2 /= (nx - 1);

    double sY2 = 0.0;
    for (double qj : q_j) {
      sY2 += Math.pow(qj - p_hat, 2);
    }
    sY2 /= (ny - 1);

    double se = Math.sqrt(sX2 / nx + sY2 / ny);

    // Step 4: Brunner-Munzel statistic
    double W = (p_hat - 0.5) / se;

    // Step 5: Welch-Satterthwaite degrees of freedom
    double df = Math.pow(sX2 / nx + sY2 / ny, 2) /
        (Math.pow(sX2 / nx, 2) / (nx - 1) + Math.pow(sY2 / ny, 2) / (ny - 1));

    // Step 6: two-tailed p-value
    TDistribution tDist = new TDistribution(df);
    double pValue = 2 * (1 - tDist.cumulativeProbability(Math.abs(W)));

    return new Result(W, pValue);
  }




}