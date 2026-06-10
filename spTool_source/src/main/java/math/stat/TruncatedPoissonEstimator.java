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
 */

package math.stat;

import util.ArrUtils;


public class TruncatedPoissonEstimator {

  public static double estimateMean(double[] data) {
    double deltaFine = 0.05;
    double deltaCoarse = 1;
    int maxIter = (int) 1E6;

    int lengthAll = data.length;
    // at least 10% zeros
    int zeroTruncationThreshold = (int) Math.floor(0.25 * lengthAll); // 25% is debatable

    double[] nonzeros = ArrUtils.nonzero(data);
    double mode = Mode.calculateMode(nonzeros);
    double mean = MeasureOfLocation.MEAN.calc(nonzeros);
    double estimMu = (2*mean / 3d + mode / 3); // weighted mean - this is debatable but seems to work
    // (as in gives net signal scattering around the mean)

    // check if enough zeros to estimate via zeros: TRUNCATION_ZERO_THRESHOLD is chosen arbitrary
    double maxValue = ArrUtils.getMax(nonzeros);
    int nonzeroCount = nonzeros.length;
    int zeroCount = lengthAll - nonzeroCount;

    if (zeroCount > zeroTruncationThreshold && nonzeroCount > 0) {
      int truncationThr = (int) Math.floor(ArrUtils.getMin(nonzeros));

      if (truncationThr > 0) {
        double target = zeroCount / (double) lengthAll;
        double lambda = maxValue;
        double cdf = poissonCDF((int) (double) truncationThr - 1, lambda);

        int iter = 0;
        while (cdf < target && iter < maxIter) {
          lambda -= deltaCoarse;
          iter++;
          cdf = poissonCDF((int) (double) truncationThr - 1, lambda);
        }

        lambda += deltaCoarse;
        iter = 0;
        cdf = poissonCDF((int) (double) truncationThr - 1, lambda);
        while (cdf < target && iter < maxIter) {
          lambda -= deltaFine;
          iter++;
          cdf = poissonCDF((int) (double) truncationThr - 1, lambda);
        }

        estimMu = lambda;
      } else {
        estimMu = Math.max(0.5, estimMu);
      }
    } else {
      estimMu = Math.max(0.5, estimMu);
    }


    return estimMu;
  }


  /**
   * Poisson CDF: P(X <= k) for Poisson(lambda)
   */
  public static double poissonCDF(int k, double lambda) {
    if (k < 0) return 0.0;
    double sum = 0.0;
    double term = Math.exp(-lambda); // P(X=0)
    for (int i = 0; i <= k; i++) {
      sum += term;
      term *= lambda / (i + 1);
    }
    return Math.min(sum, 1.0);
  }

  /**
   * Poisson PMF: P(X = k) for Poisson(lambda)
   */
  public static double poissonPMF(int k, double lambda) {
    if (k < 0) return 0.0;
    // Use log to avoid overflow for large k
    double logP = -lambda + k * Math.log(lambda) - logFactorial(k);
    return Math.exp(logP);
  }

  private static double logFactorial(int n) {
    double result = 0.0;
    for (int i = 2; i <= n; i++) result += Math.log(i);
    return result;
  }


}