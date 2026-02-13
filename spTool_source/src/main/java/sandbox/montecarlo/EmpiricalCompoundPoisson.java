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

package sandbox.montecarlo;

import com.google.common.math.Stats;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

public class EmpiricalCompoundPoisson {

  /**
   * This is correct, however not ideal. It would be better to simply take TRA data, normalize to
   * the mean for each element, and construct the empirical CDF from there.
   * <p>
   * So far, just leave this here as a guide how to resample from an empirical CDF.
   */

  // CDF and X arrays
  private static final double[] NU_SIA_X = new double[]{
      0.338235329,
      0.367647066,
      0.536764691,
      0.713235269,
      0.830882375,
      1.0,
      1.191176487,
      1.301470559,
      1.507352954,
      1.705882395,
      1.933823573,
      2.205882395,
      2.404411756,
      2.705882395,
      3.191176487,
      3.838235329,
      5.080882415,
      6.316176547,
  };

  private static final double[] NU_SIA_CDF = new double[]{
      0.0006006,
      0.008808812,
      0.070870876,
      0.169969976,
      0.286886891,
      0.409609611,
      0.530730732,
      0.642042042,
      0.737337337,
      0.816216216,
      0.878478479,
      0.92272272,
      0.957357358,
      0.981181186,
      0.992192196,
      0.997397402,
      0.9997998,
      1.0,
  };


  public static double[] applyCompoundSampling(double[] purePoissArr) {
    /*
    The units of the SIA are given in mV or sth similar.
    We expect the x value (mV) of the histogram that corresponds to the mean value of all
    input data of the histogram to represent the expected value of n ions.
    Thus, we should normalize to the mean; i.e. mean (mV) -> 1 count.
    We ran a couple of random number simulations to check if this works for different numbers
    of ions, i.e., 1, 5, 50, 100 ions. The result can be fitted with a linear function to
    calculate from summed mV -> summed counts.
    The "weird offset" seems to be an issue with the linear fit.
    When the offset is added, the fit is subjectively better. Whatever. Somethings not perfect.
    With
    double normSlope = 0.8088358850621609;
    double normIntercept = 0.40988897367460453;
    double normWeirdOffset = 0.1;
    The final result mean is always (regardless mu) 0.5 too high.
    Thus, subtract.
     */
    int nPurePoiss = purePoissArr.length;

    double normSlope = 0.8088358850621609;
    double normIntercept = 0.40988897367460453;
    double normWeirdOffset = 0.1;
    double empiricalOffset = 0.5;

    double[] cmpdArray = new double[nPurePoiss];

    LinearInterpolator inverseCdf = new LinearInterpolator();  // Placeholder for interpolation
    PolynomialSplineFunction invCdfFuncInterpolator = inverseCdf.interpolate(NU_SIA_CDF, NU_SIA_X);
    RandomGenerator randomGenerator = new MersenneTwister(); // Random number generator

    // For each Poisson random number, draw mu times from the random number compound Poisson.
    for (int idxPurePoiss = 0; idxPurePoiss < nPurePoiss; idxPurePoiss++) {
      double mu = purePoissArr[idxPurePoiss];
      int intMuPurePoiss = (int) mu; // Convert to integer

      // For each ion (e.g. I = 50 -> 50 ions): Generate uniform random numbers (1 for each ion i the DT)
      double[] randN = new double[intMuPurePoiss];
      for (int i = 0; i < intMuPurePoiss; i++) {
        randN[i] = randomGenerator.nextDouble(); // Generates uniform numbers between 0 and 1
      }

      // Apply detection threshold: Assume, we know the true Poisson rate, then we probably do
      // want to exclude cases where the detector does not register an ion at all. Thus, when the
      // random number is below the smallest observed DP in the histogram, we just set it to the
      // smallest mV value observed in order to avoid giving more "precision" at the low end than
      // the detection circuitry actually has.
      for (int i = 0; i < randN.length; i++) {
        if (randN[i] < 0.00061) {
          randN[i] = NU_SIA_X[0]; // smallest entry
        }
      }

      // Interpolate using the inverse CDF
      // Note: we need the sum of ions for a random Poisson process with e.g. 50 ions per DT.
      double sumCompound = 0;
      for (int i = 0; i < intMuPurePoiss; i++) {
        // Or: Use custom method to do the interpolation
        double xValue = invCdfFuncInterpolator.value(randN[i]);
        sumCompound += xValue;
      }

      // Apply normalization (unit is not given in counts)
      // Note that this normalization causes pure Poisson and Compound Poisson to have to same mean.
      sumCompound = sumCompound * normSlope + normIntercept + normWeirdOffset - empiricalOffset;
      sumCompound = Math.max(0, sumCompound); // Ensure non-negative value
      cmpdArray[idxPurePoiss] = sumCompound;

      // Notification for progress
      // Util.notify(idxPurePoiss, nPurePoiss);

    }
    return cmpdArray;
  }

  public static void printMonteCarloForNormalizing() {
    System.out.println("Start Compound Model");

    double[] means = new double[]{0.05, 0.1, 1, 2, 5, 7.5, 10, 15, 20, 25, 30, 35, 50, 75, 100};
    int N = (int) 1E7;

    for (double mean : means) {
      double[] poissrnds = Statistics.poissrnd(mean, N);
      double[] cmpsrnds = EmpiricalCompoundPoisson.applyCompoundSampling(poissrnds);
      Percentile percentile = new Percentile();
      percentile.setData(cmpsrnds);
      double percentileValue1E6 = percentile.evaluate(99.9999);
      double percentileValue1E4 = percentile.evaluate(99.99);
      double percentileValueMedian = percentile.evaluate(50);
      double percentileMean = Stats.meanOf(cmpsrnds);
      System.out.println("mu," + mean + "," + "1E6pect," + percentileValue1E6 + ","
          + "1E4pect," + percentileValue1E4 + ","
          + "median," + percentileValueMedian + ","
          + "percentileMean," + percentileMean + ",");
    }
  }


}


