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

import com.google.common.math.Stats;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.distribution.TDistribution;
import util.ArrUtils;

/**
 * https://towardsdatascience.com/anomaly-detection-with-generalized-extreme-studentized-deviate-in-python-f350075900e2
 * but using the correct formula from https://www.itl.nist.gov/div898/handbook/eda/section3/eda35h3.htm
 */

public abstract class GESD {


  public static double tOne(int df, double alpha) {
    TDistribution tDist = new TDistribution(df);
    double p = tDist.inverseCumulativeProbability(1 - alpha);
    return p;
  }

  public static double tGESD(int size, double alpha, int iteration) {
    int df = dfGESD(size, iteration);
    // I assume the 1/2 for two sided is included here in the "2":
    double alphaMod = alpha / (2 * (size - iteration + 1));
    // Hence, call one sided version.
    double t = tOne(df, alphaMod);
    return t;
  }

  public static int dfGESD(int size, int iteration) {
    int df = size - iteration - 1;
    return df;
  }

  /**
   * Calculates the critical value, analogous to the z Value of the z Test.
   *
   * @param size      number of data points
   * @param alpha     significance level
   * @param iteration current iteration of the outlier removal
   * @return critical value
   */
  public static double critGESD(int size, double alpha, int iteration) {
    double t = tGESD(size, alpha, iteration);
    double enumerator = (size - iteration) * t;
    double denominator = Math.sqrt(
        (size - iteration - 1 + Math.pow(t, 2))
            * (size - iteration + 1)
    );
    double crit = enumerator / denominator;
    return crit;
  }

  /**
   * Works. But: Sorting and then only removing top or bottom should be way faster!
   */

  public static GesdStep testStat(Collection<Double> data, boolean isPoisson) {
    // Calc stat parameters
    double mean = Stats.meanOf(data);
    double sd;
    if (isPoisson) {
      sd = Math.pow(mean, 0.5); // essentially normal approximation to Poisson
    } else {
      sd = Stats.of(data).sampleStandardDeviation();
    }

    // Find observation with largest distance to the mean (both directions + & -)
    double minVal = Collections.min(data);
    double maxVal = Collections.max(data);
    double absMax = Math.max(Math.abs(minVal - mean), Math.abs(maxVal - mean));

    double testValue = absMax / sd;
    return new GesdStep(maxVal, testValue, mean, sd);
  }


  public static double tTwo(int df, double alpha) {
  /*
    Note:
    For single sided test: just use alpha
    For two-sided test: use alpha/2. Why? t-Distr is symmetric and each tail gets half of alpha.
    The symmetry also means that t(alpha) = - t(1-alpha),
    e.g.  -1.7081407612528856 and 1.7081407612528854
     */
    alpha = alpha / 2;
    TDistribution tDist = new TDistribution(df);
    double p = tDist.inverseCumulativeProbability(1 - alpha);
    return p;
  }

  public static List<Double> test(double[] dataArray, double alpha, int maxOutliers) {
    List<Double> data = ArrUtils.arrToList(dataArray);
    for (int i = 0; i < maxOutliers + 1; i++) {
      GesdStep testStat = testStat(data, false);
      double testValue = testStat.testValue;
      double dataValue = testStat.actualDataValue;
      double critVal = critGESD(data.size(), alpha, i);

      // if test_stat_of_valueY > calculate_critical_value: --> valueY is an outlier.
      if (testValue > critVal) {
        data.remove(dataValue);
      } else {
        break;
      }
    }
    return data;
  }

  public static class GesdStep {

    public final double actualDataValue;
    public final double testValue;
    public final double mean;
    public final double sd;

    public GesdStep(double actualDataValue, double testValue, double mean, double sd) {
      this.actualDataValue = actualDataValue;
      this.testValue = testValue;
      this.mean = mean;
      this.sd = sd;
    }
  }


}
