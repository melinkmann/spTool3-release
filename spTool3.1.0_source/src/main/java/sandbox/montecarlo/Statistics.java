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

import com.google.common.collect.Iterables;
import core.SpTool3Main;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.util.Pair;
import math.stat.BeasleySpringerMoroInvNormCdf;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.statistics.distribution.GammaDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.CompoundPoissonModel;
import util.ArrUtils;

public class Statistics {

  private static final Logger LOGGER = LogManager.getLogger(Statistics.class);
  /**
   * Both are said to be thread-save. XoRoShiRo is supposed to be faster. Note that Apache
   * implementations use Well19937c internally, which is a faster variant of the MersenneTwister.
   * <p>
   * I considered adding a seed to these, but I was not sure how to make sure that we do not end up
   * with having the same sequence of e.g., exponential numbers for each population. I.e., we would
   * have to carefully decide when the reset the seed and when not. Seems, it is not worth it.
   */
  // private static final Random RANDOM = new Random();
  private static final XoRoShiRo128PlusRandom XORO_RANDOM = new XoRoShiRo128PlusRandom();
  private static final AtomicBoolean WAS_XORO_RANDOM_SEEDED = new AtomicBoolean(false);

  public static final double MIN_P_VALUE = 1E-15;
  private static final double DEFAULT_Z = 1;
  private static final double DEFAULT_ALPHA = Statistics.zToAlpha(DEFAULT_Z);

  /**
   * For the peak view pane.
   */
  public static void setXoroSeed(long seed) {
    XORO_RANDOM.setSeed(seed);
    WAS_XORO_RANDOM_SEEDED.set(true);
  }

  /**
   * For the simulation, we should avoid a de-randomization if the user clicked on the PeakView pane
   * before the simulation.
   */
  public static void resetXoroSeed() {
    if (WAS_XORO_RANDOM_SEEDED.get()) {
      XORO_RANDOM.setSeed(System.currentTimeMillis());
      WAS_XORO_RANDOM_SEEDED.set(false);
    }
  }

  /*
  #################################################################################################
   */

  public static double[] expWaitTimes(double mean, double maxValue) {
    ExponentialDistribution expDist = new ExponentialDistribution(mean);

    final List<Double> arrivalTimes = new ArrayList<>();
    double currentTime = 0;

    // Has the process exceeded its end? If not, draw next number and append.
    while (currentTime < maxValue) {
      // Draw next event
      currentTime += expDist.sample();

      // Check: Has the draw exceeded the end of the process? If not, add.
      if (currentTime < maxValue) {
        arrivalTimes.add(currentTime);
      } else {
        break;
      }
    }

    return ArrUtils.doubleListToArr(arrivalTimes);
  }

  /*
  #################################################################################################
   */

  /**
   * https://stackoverflow.com/questions/1241555/algorithm-to-generate-poisson-and-binomial-random-numbers".
   * Proposed by King Knuth. Fast for SMALL LAMBDA.
   */
  public static double knuthPoissrnd(double lambda) {
    if (lambda > 0) {
      double L = Math.exp(-lambda);
      double p = 1.0;
      int k = 0;

      do {
        k++;
        p *= XORO_RANDOM.nextDouble();
      } while (p > L);

      return k - 1;
    } else {
      return 0;
    }
  }

  public static void resampleKnuthPoissrnd(double[] data) {
    for (int i = 0; i < data.length; i++) {
      data[i] = knuthPoissrnd(data[i]);
    }
  }

  // #############################################################################


  /**
   * Uses Apache Commons PoissonDistribution class.
   */
  public static double poissrnd(double mu) {
    if (mu > 0) {
      // Set the mean (lambda) for the Poisson distribution

      // Create an instance of PoissonDistribution
      PoissonDistribution poissonDistribution = new PoissonDistribution(mu);

      // Generate a random Poisson-distributed number
      int randomPoisson = poissonDistribution.sample();

      return randomPoisson;
    } else {
      return 0;
    }
  }


  /**
   * Uses Apache Commons PoissonDistribution class.
   */
  public static double[] poissrnd(double mu, int n) {
    if (mu > 0) {
      PoissonDistribution poissonDistribution = new PoissonDistribution(mu);

      double[] randomPoisson = new double[n];
      for (int i = 0; i < n; i++) {
        int rand = poissonDistribution.sample();
        randomPoisson[i] = rand;
      }
      return randomPoisson;
    } else {
      return ArrUtils.fillArray(0, n);
    }
  }

  /**
   * Uses Apache Commons PoissonDistribution class.
   *
   * @return new array of data.
   */
  public static double[] poissrnd(double[] mu) {

    int n = mu.length;
    double[] randomPoisson = new double[n];
    for (int i = 0; i < n; i++) {
      if (mu[i] > 0) {
        PoissonDistribution poissonDistribution = new PoissonDistribution(mu[i]);
        int rand = poissonDistribution.sample();
        randomPoisson[i] = rand;
      } else {
        randomPoisson[i] = 0;
      }
    }
    return randomPoisson;
  }

  /**
   * Uses Apache Commons PoissonDistribution class.
   * Replaces the old array of data.
   */
  public static void resamplePoissrnd(double[] mu) {
    for (int i = 0; i < mu.length; i++) {
      if (mu[i] > 0) {
        PoissonDistribution poissonDistribution = new PoissonDistribution(mu[i]);
        mu[i] = poissonDistribution.sample();
      }
    }
  }

  // #############################################################################

  /**
   * Poisson random numbers by rejection method.
   * This implementation is not valid for small lambda.
   * https://www.johndcook.com/blog/2010/06/14/generating-poisson-random-values/
   */

  public static int rejectPoissrnd(double lambda) {
    double c = 0.767 - 3.36 / lambda;
    double beta = Math.PI / Math.sqrt(3.0 * lambda);
    double alpha = beta * lambda;
    double k = Math.log(c) - lambda - Math.log(beta);

    while (true) {
      double u = XORO_RANDOM.nextDouble();
      double x = (alpha - Math.log((1.0 - u) / u)) / beta;
      int n = (int) Math.floor(x + 0.5);
      if (n < 0) {
        continue;
      }
      double v = XORO_RANDOM.nextDouble();
      double y = alpha - beta * x;
      // double lhs = y + Math.log(v / Math.pow(1.0 + Math.exp(y), 2));
      // Numerically more stable: Split the log(v/den) to log(v) - log(den)
      //   then log(den) = logDen where log(a^2) = 2*log(a) and log(1+a) is computed as log1p(a)
      double logDen = 2.0 * Math.log1p(Math.exp(y));
      double lhs = y + Math.log(v) - logDen;
      double rhs = k + n * Math.log(lambda) - Gamma.logGamma(n + 1.0);
      if (lhs <= rhs) {
        return n;
      }
    }
  }

  public static void resampleRejectPoissrnd(double[] mu) {
    for (int i = 0; i < mu.length; i++) {
      if (mu[i] > 0) {
        mu[i] = rejectPoissrnd(mu[i]);
      } else {
        mu[i] = 0;
      }
    }
  }

  /**
   * LogFactorial needed for the rejection method.
   * Potential issue: may accumulate floating point errors from adding logs.
   */
  private static double logFactorial(int n) {
    double result = 0.0;
    for (int i = 2; i <= n; i++) {
      result += Math.log(i);
    }
    return result;
  }

  /**
   * Taken from chatGPT. Simple and fast but Sterling method. Yet, alternative is available: switched to
   * Apache Commons.
   */
  public static double logFactorialSterling(int n) {
    if (n < 20) {
      // table for small n
      double[] fact = {
          0.0, 0.0, 0.6931, 1.7918, 3.178, 4.7875, 6.5792, 8.5252,
          10.6046, 12.8018, 15.1044, 17.5023, 19.9872, 22.5522,
          25.1912, 27.8993, 30.6719, 33.5051, 36.3954, 39.3399
      };
      return fact[n];
    }
    // Stirling's approximation
    double x = n + 1;
    return (x - 0.5) * Math.log(x) - x + 0.5 * Math.log(2 * Math.PI);
  }


  /**
   * LogFactorial for the rejection method using the Gamma distribution.
   * Note: slightly slower but benefits: likely no issue with accumulating floating point
   * errors (e.g. adding logs) or Sterling bering incorrect at extreme tails.
   */
  private static double logFactorialUsingGamma(int n) {
    // Using logGamma for log(n!)
    return Gamma.logGamma(n + 1.0);
  }


  // #############################################################################

  /**
   * Poisson random numbers using a NormalApproximation.
   */
  public static int normalApproxPoissrnd(double lambda) {
    double stdNormalGaussian = XORO_RANDOM.nextGaussian();
    int rand = (int) Math.round(stdNormalGaussian * Math.sqrt(lambda) + lambda);
    return rand;
  }


  /**
   * Poisson random numbers using a NormalApproximation.
   */
  public static void resampleNormalApproxPoissrnd(double[] data) {
    for (int i = 0; i < data.length; i++) {
      data[i] = normalApproxPoissrnd(data[i]);
    }
  }


  /**
   * Method used to compare results for different Poisson methods.
   */
  public static void testPoisson() {
    StandardDeviation sd = new StandardDeviation();
    Mean m = new Mean();

    double[] mu = ArrUtils.fillArray(49, (int) 1E7);
    Statistics.resampleRejectPoissrnd(mu);
    System.out.println("Rejection: Mean= " + m.evaluate(mu) + ", SD = " + sd.evaluate(mu));

    mu = ArrUtils.fillArray(49, (int) 1E7);
    Statistics.resamplePoissrnd(mu);
    System.out.println("Apache: Mean= " + m.evaluate(mu) + ", SD = " + sd.evaluate(mu));

    mu = ArrUtils.fillArray(49, (int) 1E7);
    Statistics.resampleKnuthPoissrnd(mu);
    System.out.println("Knuth: Mean= " + m.evaluate(mu) + ", SD = " + sd.evaluate(mu));

    mu = ArrUtils.fillArray(49, (int) 1E7);
    Statistics.resampleNormalApproxPoissrnd(mu);
    System.out.println("NormalApprox: Mean= " + m.evaluate(mu) + ", SD = " + sd.evaluate(mu));

    /*
    Rejection: Mean= 49.001066699999996, SD = 6.998148859588492
    Apache: Mean= 48.99819880000004, SD = 6.998528020395578
    Knuth: Mean= 49.0018094, SD = 6.9984670195601515
    NormalApprox: Mean= 48.996686600000146, SD = 7.005543157196571
     */
  }

  // #############################################################################

  /**
   * Decide Poisson method based on the mean value.
   */
  public static void resamplePoissonDynamically(double[] mu) {
    for (int i = 0; i < mu.length; i++) {
      if (mu[i] <= 0) {
        mu[i] = 0;
      } else if (mu[i] < 30) {
        mu[i] = knuthPoissrnd(mu[i]);
      } else if (mu[i] > 1000) {
        mu[i] = normalApproxPoissrnd(mu[i]);
      } else {
        mu[i] = rejectPoissrnd(mu[i]);
      }
    }
  }

  /**
   * Decide Poisson method based on the mean value.
   */
  public static double randomPoissonDynamically(double mu) {
    double val;
    if (mu <= 0) {
      val = 0;
    } else if (mu < 30) {
      val = knuthPoissrnd(mu);
    } else if (mu > 1000) {
      val = normalApproxPoissrnd(mu);
    } else {
      val = rejectPoissrnd(mu);
    }
    return val;
  }


  /*
  #################################################################################################
   */

  // Implementation of an overdispersed Poisson-Normal approximation
  // https://link.springer.com/article/10.1007/s00216-016-9509-9
  // \sigma =\sqrt{{\left(\xi \lambda \right)}^2+\beta \lambda }
  /*
  Note that when the µ gets quite low (unsure where exactly it starts),
  the Gaussian approximation actually yields lower values than the Poisson.
  This defeats the purpose of having over-dispersion.
  So hence, let's put a switch at 5 cts/DT.
   */

  public static void resampleOverdispersedPoisson(double[] mu, double shot, double flicker) {
    final int maxIter = 50;

    for (int i = 0; i < mu.length; i++) {

      double sd = FastMath.sqrt(shot * mu[i] + FastMath.pow(flicker * mu[i], 2));

      // Make sure, number is larger than 0
      double val = -1;
      int counter = 0;
      while (val < 0 && counter < maxIter) {
        val = mu[i] + sd * XORO_RANDOM.nextGaussian();
        counter++;
      }

      val = Math.round(val);

      // (1) 5 is somewhat arbitrary but from experience it is where Poiss-Normal Approx.
      // gets good enough (at 10 its excellent)
      // Intended to make sure that the Gaussian approximation
      // actually yields lower values than the Poisson.

      // (2) Also: Make sure, number is larger than 0
      if (mu[i] < 5 || val < 0) {
        double muPoiss = randomPoissonDynamically(mu[i]);
        mu[i] = Math.max(val, muPoiss);
      } else {
        mu[i] = val;
      }
    }
  }


  /*
  #################################################################################################
   */

  // Calculate the z-score for "P_TWO_SIDED" (two-tailed distribution)
  public static final double P_ONE_SIDED = 0.9999;  // Confidence level (99.99%) : 1 in 1E4
  public static final double P_TWO_SIDED = 1 - (1 - P_ONE_SIDED) / 2;

  private static final NormalDistribution STD_NORM = new NormalDistribution(0, 1);
  public static final double Z_SCORE = STD_NORM.inverseCumulativeProbability(P_TWO_SIDED);

  /**
   * Note: This function does not construct a NormalDistribution instance for each call!
   *
   * @param truePercentage:      expected value
   * @param relativeUncertainty: given as 0.5 (not 50 %)
   * @return normally distributed value around the mean with +- u/2
   */
  public static double randomifyPercent(double truePercentage, double relativeUncertainty) {
    double rand;

    if (relativeUncertainty == 0) {
      rand = truePercentage;
    } else {
      double uncertainty = relativeUncertainty * truePercentage;

      // Half-width of the interval
      double half_width = uncertainty / 2;

      // Calculate the standard deviation sigma
      double sigma = half_width / Z_SCORE;

      double stdNormalGaussian = XORO_RANDOM.nextGaussian();
      rand = stdNormalGaussian * sigma + truePercentage;
    }
    return rand;
  }

  /**
   * Note: This function does not construct a NormalDistribution instance for each call!
   */
  public static double randomifyDelay(double delay, double absolutePlusMinus) {
    double randDelay = delay;

    if (absolutePlusMinus != 0) {
      // Calculate the standard deviation sigma
      double sigma = absolutePlusMinus / Z_SCORE;
      double stdNormalGaussian = XORO_RANDOM.nextGaussian();
      double rand = stdNormalGaussian * sigma + delay;
      randDelay = rand;
    }

    return randDelay;
  }


    /*
  #################################################################################################
   */

  /**
   * Mostly adapted from chatGPT!
   * <p>
   * Uses a low-discrepancy sequence based on the golden ratio to simulate randomness.
   * The i * golden % 1 generates a fractional "pseudo-random" number that distributes well across the unit
   * interval.
   * Why golden ratio? It minimizes correlation and avoids repeating patterns — commonly used in
   * quasi-random sampling.
   * Result: Smoother and more even jitter across quantiles.
   * <p>
   * When to Use Which?
   * Use quantileSampleWithJitter (golden ratio) when you want:
   * - Deterministic output
   * - Smoothly distributed jitter
   * <p>
   * Use quantileSampleWithHashedJitter when:
   * - You want more chaotic/random-looking offsets
   * - You’re okay with potential hash variations across runs (or seed your own hash for consistency)
   */
  public static double[] quantileSampleWithGoldenRatioJitter(double[] data, int targetSize,
                                                             int jitterWindow) {
    if (targetSize >= data.length) {
      return Arrays.copyOf(data, data.length); // No reduction needed
    }

    double[] sorted = Arrays.copyOf(data, data.length);
    Arrays.sort(sorted);

    double[] result = new double[targetSize];
    final double golden = 0.6180339887; // golden ratio fraction for deterministic jitter

    for (int i = 0; i < targetSize; i++) {
      double quantileIndex = ((double) i / (targetSize - 1)) * (sorted.length - 1);

      // Compute jittered index deterministically within a small window
      int jitterRange = Math.max(1, jitterWindow); // size of jitter range
      int center = (int) Math.round(quantileIndex);

      // Deterministic offset based on golden ratio + index (bounded jitter)
      int offset = (int) Math.floor((i * golden % 1) * (2 * jitterRange + 1)) - jitterRange;
      int jitteredIndex = Math.min(sorted.length - 1, Math.max(0, center + offset));

      result[i] = sorted[jitteredIndex];
    }

    return result;
  }


  /**
   * Mostly adapted from chatGPT!
   * Purpose of jitterWindow:
   * Normally, if you sample quantiles from a sorted array, you'd pick evenly spaced indices.
   * But that can make the result too uniform or deterministic.
   * To break that up a bit — while keeping the sample roughly faithful to the distribution —
   * this method introduces a small random variation (jitter) around the expected quantile index.
   * <p>
   * How it works:
   * >center index:
   * This is the index you would use if you were doing standard quantile sampling
   * — no jitter, just evenly spaced samples from the sorted array.
   * <p>
   * > offset:
   * A jitter (random offset) is computed for each i, using a hash function:
   * > int offset = (hash % (2 * jitterWindow + 1)) - jitterWindow;
   * This gives an integer offset in the range:
   * <p>
   * [−jitterWindow, +jitterWindow]
   * So if jitterWindow = 2, your offset can be -2, -1, 0, 1, or 2.
   * <p>
   * > jitteredIndex:
   * This is the center index plus the offset, clamped to valid array bounds.
   * It introduces randomness in a bounded way.
   * <p>
   * Example:
   * If sorted.length = 1000, targetSize = 100, and jitterWindow = 2:
   * Without jitter: You might pick index 0, 10, 20, ..., 990.
   * With jitter: You might pick index 0±2, 10±2, 20±2, ..., 990±2.
   * So instead of always picking the exact quantile points, you sample around them — adding controlled
   * randomness.
   * This jitter helps introduce variation in the sampled values
   * while still representing the underlying distribution.
   * It's especially useful in visualizations or downsampling
   * large datasets without losing important distribution characteristics.
   */
  public static double[] quantileSampleWithHashedJitter(double[] data, int targetSize,
                                                        int jitterWindow) {
    if (targetSize >= data.length) {
      return Arrays.copyOf(data, data.length);
    }

    double[] sorted = Arrays.copyOf(data, data.length);
    Arrays.sort(sorted);

    double[] result = new double[targetSize];

    for (int i = 0; i < targetSize; i++) {
      double quantileIndex = ((double) i / (targetSize - 1)) * (sorted.length - 1);
      int center = (int) Math.round(quantileIndex);

      // Hash function (simple integer hash)
      int hash = Integer.hashCode(i); // or use something like MurmurHash if available
      int offset = (hash % (2 * jitterWindow + 1)) - jitterWindow;

      // Jittered index, clamped
      int jitteredIndex = Math.max(0, Math.min(sorted.length - 1, center + offset));

      result[i] = sorted[jitteredIndex];
    }

    return result;
  }


  /**
   * Essentially same as above but uses better hashing function (murmurhash3) and has protection
   * against clamping poorly at the edges.
   */
  public static double[] quantileSampleWithMurmurHashedJitter(
      double[] data, int targetSize, int jitterWindow) {
    if (data == null || data.length == 0 || targetSize <= 0) {
      return new double[0];
    }

    if (targetSize >= data.length) {
      return Arrays.copyOf(data, data.length);
    }

    double[] sorted = Arrays.copyOf(data, data.length);
    Arrays.sort(sorted);

    // return "median" (central value)
    if (targetSize == 1) {
      return new double[]{sorted[sorted.length / 2]};
    }

    double[] result = new double[targetSize];

    for (int i = 0; i < targetSize; i++) {
      double quantileIndex = ((double) i / (targetSize - 1)) * (sorted.length - 1);
      int center = (int) Math.round(quantileIndex);

      // Use MurmurHash3 on the integer i to get a stable 32‑bit hash
      byte[] bytes = new byte[]{
          (byte) (i >>> 24),
          (byte) (i >>> 16),
          (byte) (i >>> 8),
          (byte) (i)
      };
      int hash = MurmurHash3.hash32x86(bytes, 0, bytes.length, 0);

      // Constrain jitter so we stay within valid index bounds
      int minOffset = -Math.min(jitterWindow, center);
      int maxOffset = Math.min(jitterWindow, sorted.length - 1 - center);

      int range = maxOffset - minOffset + 1;
      int offset = minOffset + Math.floorMod(hash, range);

      int jitteredIndex = center + offset;
      result[i] = sorted[jitteredIndex];
    }

    return result;
  }

  /*
  #################################################################################################
   */

  public static double zToAlpha(double z) {
    NormalDistribution nd = new NormalDistribution(0, 1); // standard normal

    // When testing, it showed that precision is way better when getting cdf(-z) than 1-cdf(z)
    // Compare values at: https://en.wikipedia.org/wiki/Standard_normal_table
    double alpha = nd.cumulativeProbability(-z); // alpha = P(Z > z)
    return alpha;
  }

  public static double alphaToZ(double alpha) {
    // Precision is better for low alphas (the 0.000000001 is better than the 99.999999X case).
    // As the normal distribution is symmetric, we call with small alpha, than take Math.abs();
    // Compare values at: https://en.wikipedia.org/wiki/Standard_normal_table
    double z = Math.abs(BeasleySpringerMoroInvNormCdf.invNormCdfApprox(alpha));
    return z;
  }



  /*
  #################################################################################################
   */

  public static double mean(double[] values) {
    double mean = StatUtils.mean(values);
    return mean;
  }

  /*
  #################################################################################################
  https://stackoverflow.com/questions/78078842/correct-way-to-generate-random-numbers-from-a-log-normal
  -distribution-in-r
   */

  /**
   * @param mu empirical mean describing the desired mean value of the resulting data set.
   * @param sd desired empirical standard deviation of the resulting data set.
   * @return the parameter mu for a lognormal distribution that yields a dataset with mean equal to
   * the empirical mu.
   */
  public static double lognormalMu(double mu, double sd) {
    return Math.log((Math.pow(mu, 2)) / Math.sqrt(Math.pow(sd, 2) + Math.pow(mu, 2)));
  }

  /**
   * @param mu empirical mean describing the desired mean value of the resulting data set.
   * @param sd desired empirical standard deviation of the resulting data set.
   * @return the parameter sigma for a lognormal distribution that yields a dataset with standard
   * deviation equal to the empirical mu.
   */
  public static double lognormalSD(double mu, double sd) {
    return Math.sqrt(Math.log(1 + Math.pow(sd, 2) / Math.pow(mu, 2)));
  }

  /**
   * Wikipedia: Takes the log space m and s and returns the expected value mu.
   *
   * @param m log-space mean (μ) of ln(X)
   * @param s log-space standard deviation (σ) of ln(X)
   * @return the empirical mean E[X] of the lognormal distribution
   */
  public static double inverseLognormalMu(double m, double s) {
    double s2 = s * s;
    return Math.exp(m + 0.5 * s2);
  }

  /**
   * Wikipedia:Takes the log space m and s and returns the standard deviation SD.
   *
   * @param m log-space mean (μ) of ln(X)
   * @param s log-space standard deviation (σ) of ln(X)
   * @return the empirical standard deviation SD(X) of the lognormal distribution
   */
  public static double inverseLognormalSD(double m, double s) {
    double s2 = s * s;
    return Math.sqrt(Math.expm1(s2) * Math.exp(2 * m + s2));
  }

  /**
   * This function will provide a location parameter for a SIA lognormal model that uses sigma to
   * model the shape and chooses mu so that the expected value is one. This means that when sampling
   * the inverse CDF of the SIA n times, the average of the compound poisson data is still n (albeit
   * fuzzy and non-integer).
   *
   * @param sigma "shape" parameter of the lognormal distribution.
   * @return location parameter mu of a lognormal distribution that has an expected value of v=1.
   */
  public static double getLognormalMuNormalized(double sigma) {
    double mu = Math.log(1) - 0.5 * Math.pow(sigma, 2);
    return mu;
  }


  /*
   * Idea for Aggregates: add a right click menu in the UI: generate aggregates
   *       This item checks if Particle parameters are selected.
   *       If yes, it will generate copies with the respective µ and SD
   *       computed for different N. It also sets BG to zero and adjusts the rate
   *       according to some selectable distribution.
   */

  /**
   * Computes the resulting mean (mass-based) of an agglomerate consisting of n particles from a Gaussian
   * distribution with mean mu and SD sd.
   */
  public static double gaussianAggregateMu(double mu, int n) {
    return mu * n;
  }

  /**
   * Computes the resulting SD (mass-based) of an agglomerate consisting of n particles from a Gaussian
   * distribution with mean mu and SD sd.
   */
  public static double gaussianAggregateSD(double sd, int n) {
    return sd * Math.sqrt(n);
  }

  /**
   * Computes the resulting empirical mean (mass-based) of an agglomerate consisting of n particles from a
   * lognormal distribution with mean mu and SD sd.
   * The log space versions of the parameters only exist internally.
   * Fenton-Wilkinson approximation is used implemented by chatGPT.
   */
  public static double lognormalAggregateMu(double mu, double sd, int n) {
    double mu_Log = lognormalMu(mu, sd);
    double sd_Log = lognormalSD(mu, sd);
    double fentonApproxSD =
        Math.sqrt(Math.log(1 + (Math.exp(Math.pow(sd_Log, 2)) - 1) / n));
    double fentonApproxMean =
        Math.log(n) + mu_Log + (Math.pow(sd_Log, 2) - Math.pow(fentonApproxSD, 2)) / 2;
    // convert back, as spTool logic expects "particle space" (and not log space)
    return Statistics.inverseLognormalMu(fentonApproxMean, fentonApproxSD);
  }

  /**
   * Computes the resulting empirical SD (mass-based) of an agglomerate consisting of n particles from a
   * lognormal distribution with mean mu and SD sd.
   * The log space versions of the parameters only exist internally.
   * Fenton-Wilkinson approximation is used implemented by chatGPT.
   */
  public static double lognormalAggregateSD(double mu, double sd, int n) {
    double mu_Log = lognormalMu(mu, sd);
    double sd_Log = lognormalSD(mu, sd);
    double fentonApproxSD =
        Math.sqrt(Math.log(1 + (Math.exp(Math.pow(sd_Log, 2)) - 1) / n));
    double fentonApproxMean =
        Math.log(n) + mu_Log + (Math.pow(sd_Log, 2) - Math.pow(fentonApproxSD, 2)) / 2;
    // convert back, as spTool logic expects "particle space" (and not log space)
    return Statistics.inverseLognormalSD(fentonApproxMean, fentonApproxSD);
  }

  /**
   * Resamples Poisson data with Compound Poisson distribution.
   * This method is the "most obvious" implementation but also slowest.
   * (!!!) It uses a Lognormal distribution. (!!!)
   * Currently, we only use it for the peak viewer where few data points are computed and speed is not
   * limited.
   */
  public static void resampleCompoundPoisson(double[] purePoisson, double logNormSigma) {

    // This is a bit annoying when it shows up in the peak viewer
    //LOGGER.trace("Using Apache Commons Compound Poisson algorithm.");

    double logNormMu = getLognormalMuNormalized(logNormSigma);
    final LogNormalDistribution logNormDist = new LogNormalDistribution(logNormMu, logNormSigma);
    // Go through the data points
    for (int d = 0; d < purePoisson.length; d++) {
      //      if (d % 1000 == 0) {
      //        System.out.println(d + "/" + purePoisson.length);
      //      }
      double noOfIons = purePoisson[d];
      double cmpSum = 0;
      if (noOfIons > 0) {
        // for each ion, sample the sia
        for (int i = 0; i < noOfIons; i++) {
          cmpSum += logNormDist.sample();
        }
        purePoisson[d] = cmpSum;
      }
    }
  }


  /**
   * Resamples Poisson data with Compound Poisson distribution.
   * This method is the "most obvious" implementation but also slowest.
   * (!!!) It uses a Lognormal distribution and sets the RNG seed (!!!)
   * Currently, we only use it for the peak viewer where few data points are computed and speed is not
   * limited.
   */
  public static void resampleCompoundPoisson(double[] purePoisson, double logNormSigma, long seed) {

    double logNormMu = getLognormalMuNormalized(logNormSigma);
    final LogNormalDistribution logNormDist = new LogNormalDistribution(logNormMu, logNormSigma);
    logNormDist.reseedRandomGenerator(seed);
    // Go through the data points
    for (int d = 0; d < purePoisson.length; d++) {
      double noOfIons = purePoisson[d];
      double cmpSum = 0;
      if (noOfIons > 0) {
        // for each ion, sample the sia
        for (int i = 0; i < noOfIons; i++) {
          cmpSum += logNormDist.sample();
        }
        purePoisson[d] = cmpSum;
      }
    }
  }


  /**
   * Resamples Poisson data with Compound Poisson distribution.
   * (!!!) This version is sped up by using XoRoShiRo algorithm to generate Gaussians,
   * which are converted to lognormals with FastMath.exp(logNormMu + logNormSigma * n) (!!!)
   */
  public static void resampleCompoundPoissonFast(double[] purePoisson, double logNormSigma) {

    //LOGGER.trace("Using standard Compound Poisson algorithm (using XoRoShiRo128PlusRandom).");

    double logNormMu = getLognormalMuNormalized(logNormSigma);

    // Go through the data points
    for (int d = 0; d < purePoisson.length; d++) {
      //      if (d % 1000 == 0) {
      //       System.out.println(d + "/" + purePoisson.length);
      //      }
      double noOfIons = purePoisson[d];
      double cmpdSum = 0;
      if (noOfIons > 0) {
        // for each ion, sample the sia
        for (int i = 0; i < noOfIons; i++) {
          final double gaussianRand = XORO_RANDOM.nextGaussian();
          cmpdSum += FastMath.exp(logNormMu + logNormSigma * gaussianRand);
        }
        purePoisson[d] = cmpdSum;
      }
    }
  }


  /**
   * [ADDS INDIVIDUAL NUMBERS]
   * Prepare 3 iterators of the same list and use time stamp to create some randomness.
   * Still add each instance by itself.
   */
  public static void resampleCompoundPoissonFaster(double[] purePoisson, double logNormSigma) {
    LOGGER.trace("Using REDUCED Compound Poisson algorithm (using XoRoShiRo128PlusRandom).");

    double logNormMu = getLognormalMuNormalized(logNormSigma);

    // Simply, use a pre-calculated list. In all of the Poisson noise, this will not matter much.
    // If the list is longer than the DT, we do not run into issues with aliasing.
    int reduce = (int) 5E5;
    List<Double> lognormalRands1 = new ArrayList<>(reduce);
    List<Double> lognormalRands2 = new ArrayList<>(reduce);
    List<Double> lognormalRands3 = new ArrayList<>(reduce);
    for (int i = 0; i < reduce; i++) {
      final double norm = XORO_RANDOM.nextGaussian();
      double lognormal = FastMath.exp(logNormMu + logNormSigma * norm);
      lognormalRands1.add(lognormal);

      lognormal = FastMath.exp(logNormMu + logNormSigma * norm);
      lognormalRands2.add(lognormal);
      lognormal = FastMath.exp(logNormMu + logNormSigma * norm);
      lognormalRands3.add(lognormal);
    }
    Iterable<Double> infinite1 = Iterables.cycle(lognormalRands1);
    Iterable<Double> infinite2 = Iterables.cycle(lognormalRands2);
    Iterable<Double> infinite3 = Iterables.cycle(lognormalRands3);
    Iterator<Double> infiniteIterator1 = infinite1.iterator();
    Iterator<Double> infiniteIterator2 = infinite2.iterator();
    Iterator<Double> infiniteIterator3 = infinite3.iterator();

    // Go through the data points
    for (int d = 0; d < purePoisson.length; d++) {
      double noOfIons = purePoisson[d];
      double cmpSum = 0;
      if (noOfIons > 0) {
        // for each ion, sample the sia
        for (int i = 0; i < noOfIons; i++) {
          // introduce some randomness to avoid aliasing if the counts/DT are similar to the "buffer size"
          if (System.currentTimeMillis() % 3 == 0) {
            cmpSum += infiniteIterator3.next();
          } else if (System.currentTimeMillis() % 2 == 0) {
            cmpSum += infiniteIterator2.next();
          } else {
            cmpSum += infiniteIterator1.next();
          }
        }
        purePoisson[d] = cmpSum;
      }
    }
  }

  /**
   * [MERGES RANDOM NUMBERS INTO CHUNKS OF 10]
   * Prepare 3 iterators of the same list and use time stamp to create some randomness.
   * The lists contain a prepared set of sums of 10 lognormals for faster access,
   * essentially accelerating everything by an order of magnitude as we add sums of 10s already.
   */
  public static void resampleCompoundPoissonFastest(double[] purePoisson, double logNormSigma) {
    LOGGER.trace("Using doubly REDUCED Compound Poisson algorithm (using XoRoShiRo128PlusRandom).");

    double logNormMu = getLognormalMuNormalized(logNormSigma);

    // Simply, use a pre-calculated list. In all of the Poisson noise, this will not matter much.
    // If the list is longer than the DT, we do not run into issues with aliasing.
    int reduce = (int) 5E5;
    int chunkSize = 10;
    List<Double> lognormalRands1 = new ArrayList<>(reduce);
    List<Double> lognormalRands2 = new ArrayList<>(reduce);
    List<Double> lognormalRands3 = new ArrayList<>(reduce);

    for (int i = 0; i < reduce; i++) {
      double sum1 = 0;
      double sum2 = 0;
      double sum3 = 0;
      for (int n = 0; n < chunkSize; n++) {
        sum1 += FastMath.exp(logNormMu + logNormSigma * XORO_RANDOM.nextGaussian());
        sum2 += FastMath.exp(logNormMu + logNormSigma * XORO_RANDOM.nextGaussian());
        sum3 += FastMath.exp(logNormMu + logNormSigma * XORO_RANDOM.nextGaussian());
      }
      lognormalRands1.add(sum1);
      lognormalRands2.add(sum2);
      lognormalRands3.add(sum3);
    }
    Iterable<Double> infinite1 = Iterables.cycle(lognormalRands1);
    Iterable<Double> infinite2 = Iterables.cycle(lognormalRands2);
    Iterable<Double> infinite3 = Iterables.cycle(lognormalRands3);
    Iterator<Double> infiniteIterator1 = infinite1.iterator();
    Iterator<Double> infiniteIterator2 = infinite2.iterator();
    Iterator<Double> infiniteIterator3 = infinite3.iterator();

    // Go through the data points
    for (int d = 0; d < purePoisson.length; d++) {
      double noOfIons = purePoisson[d];
      double noOfChunks = Math.floor(noOfIons / chunkSize);
      double cmpSum = 0;
      if (noOfIons > 0) {
        // for each ion, sample the sia
        for (int i = 0; i < noOfChunks; i++) {
          // introduce some randomness to avoid aliasing if the counts/DT are similar to the "buffer size"
          if (System.currentTimeMillis() % 3 == 0) {
            cmpSum += infiniteIterator3.next();
          } else if (System.currentTimeMillis() % 2 == 0) {
            cmpSum += infiniteIterator2.next();
          } else {
            cmpSum += infiniteIterator1.next();
          }
        }
        // fill
        int missingNumbers = (int) noOfIons - (int) (noOfChunks * chunkSize);
        for (int n = 0; n < missingNumbers; n++) {
          cmpSum += FastMath.exp(logNormMu + logNormSigma * XORO_RANDOM.nextGaussian());
        }

        purePoisson[d] = cmpSum;
      }
    }
  }


// ###########################################################################################

  /**
   * Upper-tail quantile of normal(μ, σ)
   * returns F^{-1}(1 − alpha)
   */
  public static double getOneSidedGaussianQuantile(double mu, double sigma, double alpha) {
    double z = 0;
    if (alpha == 0) {
      LOGGER.error("Gaussian: Cannot compute alpha=0. Replaced with smallest positive double greater than " +
          "zero.");
      alpha = Math.nextUp(0d);
    }
    // seems to become unstable somewhere between 15 and 16: better safe than sorry
    if (alpha > 1E-15) {
      NormalDistribution nd = new NormalDistribution(0, 1);
      z = Math.abs(nd.inverseCumulativeProbability(1.0 - alpha));
    } else {
      z = Math.abs(BeasleySpringerMoroInvNormCdf.invNormCdfApprox(alpha));
    }
    // check which side
    double quantile;
    // more than 50% false positive, value sits left of mean
    if (alpha > 0.5) {
      quantile = mu - sigma * z;
    } else {
      quantile = mu + sigma * z;
    }


    return quantile;
  }


// ###########################################################################################

//  /**
//   * Performs quite similar to ApacheCommons, however less precise. chatGPTs proposal to calculate
//   * the cdf. Kept here as a reminder not to try implementing Abramowitz & Stegun formula 7.1.26 again :)
//   */
//  public static double normalCDFApprox(double z) {
//    // Abramowitz & Stegun formula 7.1.26
//    final double p = 0.2316419;
//    final double[] b = {0.319381530, -0.356563782, 1.781477937,
//        -1.821255978, 1.330274429};
//
//    double t = 1.0 / (1.0 + p * Math.abs(z));
//    double sum = b[0];
//    for (int i = 1; i < b.length; i++) {
//      sum += b[i] * Math.pow(t, i);
//    }
//    double pdf = Math.exp(-0.5 * z * z) / Math.sqrt(2 * Math.PI);
//    double cdf = 1.0 - pdf * sum;
//
//    return z >= 0 ? cdf : 1.0 - cdf;
//  }

  // ###########################################################################################

  /**
   * Upper-tail quantile of LogNormal(μ, σ):
   * returns F^{-1}(1 − alpha)
   */
  public static double getOneSidedLognormalQuantile(double muLog, double sigmaLog, double alpha) {
    double z = 0;
    double result;

    if (alpha == 0) {
      LOGGER.error("LogNormalDistribution: Cannot compute alpha=0. Replaced with smallest positive double " +
          "greater than zero.");
      alpha = Math.nextUp(0d);
    }

    // LogNormalDistribution seems to become unstable somewhere between 14 and 16: better safe than sorry
    if (alpha > 1E-15) {
      LogNormalDistribution upperLn = new LogNormalDistribution(muLog, sigmaLog);
      result = upperLn.inverseCumulativeProbability(1 - alpha);
    } else {
      /*
       alpha is small:
       - in the right tail, we only need to consider the case mu+sigma
       - using BSM approximation as z via inverse normal cdf is not reliable anymore
       */
      z = Math.abs(BeasleySpringerMoroInvNormCdf.invNormCdfApprox(alpha));
      result = Math.exp(muLog + sigmaLog * z);
    }
    return result;
  }

  // ###########################################################################################


//  // Returns "alpha" by brute force inverting the beasley springer moro approximation
//  public static double invertBSM(double targetZ, double zTolerance) {
//    // Clear problem: Math.nextDown(1d); is just approx. 1-1E-16, whereas  Math.nextUp(0d); is 1E-324 or so.
//
//    double lo = Math.nextUp(0d);
//    double hi = Math.nextDown(1d);
//    double mid;
//
//    targetZ = -targetZ;
//
//    while (true) {
//      mid = 0.5 * (lo + hi);
//      double z = -Math.abs(BeasleySpringerMoroInvNormCdf.invNormCdfApprox(mid));
//      double error = z - targetZ;
//
//      if (Math.abs(error) <= zTolerance || mid == 0 || mid == 1) {
//        return mid;
//      }
//
//      if (z > targetZ) {
//        hi = mid;
//      } else {
//        lo = mid;
//      }
//
//      // if (hi - lo < 1e-16) {
//      //  return 1.0 - mid;
//      // }
//    }
//  }

  // ###########################################################################################


  /**
   * One-sided (right) quantile of poisson(μ)
   */
  public static int getOneSidedPoissonQuantile(double lambda, double alpha) {
    int quantile = 0;

    if (lambda <= 0) {
      LOGGER.error("Poisson lambda <= 0. Returning 0 as quantile.");
      quantile = 0;
      // return to kill the pathological lambda right away
      return quantile;
    }

    if (alpha == 0) {
      LOGGER.error("Poisson: Cannot compute alpha=0. Replaced with smallest positive double greater than " +
          "zero.");
      alpha = Math.nextUp(0d);
    }

    if (alpha > 1E-16) {
      PoissonDistribution poisson = new PoissonDistribution(lambda);
      quantile = poisson.inverseCumulativeProbability(1.0 - alpha);
    } else {
      // this one maxes out at some point around a=1E-18 or so
      Pair<Boolean, Integer> poissonQuantileResult = binarySearchSurvivalPoissonUpperTailQuantile(lambda,
          alpha);

      if (poissonQuantileResult.getKey()) {
        quantile = poissonQuantileResult.getValue();
      } else {
       /*
       This works well, however, undershoots. Empirically, seems like +10 puts it in a better place.
       It also introduces continuity, would be weird if Threshold began to shrink at higher alpha.
       Note that this is a practical fix, not a mathematically exact solution!
       */
        double gaussianApprox = getOneSidedGaussianQuantile(lambda, Math.sqrt(lambda), alpha);
        quantile = ((int) Math.ceil(gaussianApprox)) + 10;
        LOGGER.warn("Requested very low alpha for Poisson. " +
            "Reverting to using Gaussian approximation and add '+10' for upper Poisson tail. This is a " +
            "practical fix, not a mathematically exact solution! Consider manual thresholding or " +
            "other thresholding options.");
      }
    }
    return quantile;
  }


// #####################################################################################################

  /**
   * Requested binary search implementation from chatGP, and it seems to work quite fine.
   * Note: Works but maxes out at 1E-18 or so if we do not include an approach using logspace.
   * Given some millions of observations in spICP-MS and these wild alphas,
   * I think we can agree that these are only computational tools without perfect statistical rigor.
   * In addition, I'd prefer a smooth output that does not fail with unknown outcomes.
   * <p>
   * Computes the smallest integer k such that P(X >= k) <= alpha for Poisson(lambda). Uses normal
   * approx for initial guess and binary search to refine.
   */
  public static Pair<Boolean, Integer> binarySearchSurvivalPoissonUpperTailQuantile(double lambda,
                                                                                    double alpha) {

    // Poisson is not defined for lambda=0
    if (lambda > 0) {

      // gpt suggests binary search converges at log() efficiency (max=50), so 500 should be fine
      final int maxIter = 500;
      int iter = 0;

      PoissonDistribution poisson = new PoissonDistribution(lambda);

      // Step 1: Initial guess via normal approximation
      double z = BeasleySpringerMoroInvNormCdf.invNormCdfApprox(alpha);
      int low = 0;
      // +50k cushion for large tails
      int high = Math.max(low + +50_000, (int) Math.ceil(lambda + z * Math.sqrt(lambda)) + 50_000);

      // Narrow down high to a reasonable bound: cdf() seems a bit more numerically stable than invCdf()
      // which is why we call it here instead
      while (1.0 - poisson.cumulativeProbability(high - 1) > alpha) {
        high *= 2;  // Expand upper bound
        if (high > Integer.MAX_VALUE / 2) {
          LOGGER.error("Required quantile too large for binary search in 32 bit integer environment.");
          return new Pair<>(false, 0);
        }
        iter++;
        if (iter > maxIter) {
          return new Pair<>(false, 0);
        }
      }

      // Step 2: Binary search between low and high
      iter = 0;
      while (low < high) {
        int mid = (low + high) >>> 1;
        // Note: cdf() seems a bit more numerically stable than invCdf(). Thus, we call it here
        // although we are in an environment where alpha<1E-16 and Poisson.invCdf() was excluded previously.
        double survival = 1.0 - poisson.cumulativeProbability(mid - 1);

        // If it still fails compute via poissonSurvival in log space: inserted to prevent zero
        if (survival == 0.0) {
          double logSurvival = poissonSurvivalLog(lambda, mid);
          survival = Math.exp(logSurvival);
        }

        if (survival > alpha) {
          low = mid + 1;
        } else {
          high = mid;
        }
        iter++;
        if (iter > maxIter) {
          return new Pair<>(false, 0);
        }
      }

      // Failure points seems to be when the cdf returns 1
      boolean success = (1.0 - poisson.cumulativeProbability(low - 1)) > 0
          || Math.exp(poissonSurvivalLog(lambda, low - 1)) > 0;

      // Added "-1". Logic overshoots by 1 when compared to ApacheCommons.
      return new Pair<>(success, low - 1);
    } else {
      return new Pair<>(false, 0);
    }
  }

  /**
   * Log of the Poisson survival function P(X >= k), computed via
   * truncated summation of the PMF in log space.
   * <p>
   * Accurate when the truncation captures sufficient tail mass;
   * converges fastest when k >= lambda.
   * <p>
   * Can be used to approximate the inverse Poisson for very small alpha
   * by finding the smallest k such that exp(poissonSurvivalLog(lambda, k)) <= alpha.
   * <p>
   * Modified on a sketch suggested by chatGPT.
   */
  public static double poissonSurvivalLog(double lambda, int k) {

    // max terms depends on magnitude of lambda and where the desired value k sits relative to it.
    // large lambda will need more terms, and values of k close to the mean
    // may require large enough proportion of the tail to be included
    // in order to actually be close to the mean of the distribution.
    // max=50 is already very large (for log space); 50 should also be fine.
    // For very large lambda (e.g., 12C) maybe we need another approximation, e.g.,
    // "saddlepoint approximation method" or really just use GaussianApproximation.

    int mode = (int) Math.floor(lambda);
    int maxTerms;
    if (k >= lambda) {
      // Upper tail, decaying terms
      maxTerms = 50;  // conservative
    } else {
      // Lower tail: needs to cover from k up to mode + roughly 5 sigma
      maxTerms = (mode - k) + (int) (5 * Math.sqrt(lambda));
      maxTerms = Math.max(maxTerms, 50); // ensure a minimum
    }

    double logLambda = Math.log(lambda);
    // double logP = -lambda;  // e^-λ from Poisson PMF // not needed anymore after revision
    double logSum = Double.NEGATIVE_INFINITY;

    for (int j = k; j < k + maxTerms; j++) {
      // double logTerm = -lambda + j * logLambda - logFactorialGPT(j);
      double logTerm = -lambda + j * logLambda - Gamma.logGamma(j + 1.0);
      logSum = logAdd(logSum, logTerm);

      // early exit if next term is much smaller
      if (logTerm - logSum < -40) {
        break;
      }
    }

    return logSum;
  }

  public static double logAdd(double logX, double logY) {
    if (Double.isInfinite(logX)) {
      return logY;
    }
    if (Double.isInfinite(logY)) {
      return logX;
    }
    if (logX < logY) {
      return logY + Math.log1p(Math.exp(logX - logY));
    } else {
      return logX + Math.log1p(Math.exp(logY - logX));
    }
  }


  // ################################################################################


  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////    Compound Poisson   ///////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  public static double getCompoundThreshold(CompoundPoissonModel model, double lambda,
                                            double sigmaLog,
                                            double alpha) {
    double thr = 0;
    if (lambda > 0) {
      if (model.equals(CompoundPoissonModel.LOG_APPROX)) {
        thr = getOneSidedCompoundPoissonLnApproxQuantile(lambda, sigmaLog, alpha);
      } else {
        if (SpTool3Main.getRunTime().getCompoundPoissonQuantiles().isValid()) {
          thr = SpTool3Main.getRunTime().getCompoundPoissonQuantiles().getThr(lambda,
              sigmaLog, alpha);
          // check if sth failed, e.g., QuantileTable was bad (then returns -1)
          if (thr < 0) {
            LOGGER.error("Quantile data have reported bad value. Reverting to Lognormal " +
                "approximation.");
            thr = getOneSidedCompoundPoissonLnApproxQuantile(lambda, sigmaLog, alpha);
          }
        } else {
          LOGGER.error("Quantile data have not been loaded successfully. Reverting to Lognormal " +
              "approximation.");
          thr = getOneSidedCompoundPoissonLnApproxQuantile(lambda, sigmaLog, alpha);
        }
      }
    }
    return thr;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////    Fenton-Wilkinson Approximation   /////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Sum of `n` identical independent log-normal distributions. The sum is approximated by another
   * log-normal distribution, defined by the returned parameters. By defaults, the Fenton-Wilkinson
   * approximation is used for good right-tail accuracy [3]. Returns the new logMus and logSigmas of
   * the lognormal that is obtained after summing lognormals.
   * <p>
   * Fenton-Wilkinson approximation, and the result is approximate, especially for small n or large sigma.
   * Especially the tail may not be perfect (not ideal for critical limits but here we are).
   *
   * @param nVals array of ints
   * @param mu    log mean of the underlying distributions
   * @param sigma log stddev of the underlying distributions
   * @return mu, sigma of the log-normal approximation
   * <p>
   * References: .. [3] L. F. Fenton, "The sum of lognormal probability distributions in scatter
   * transmission systems", IRE Trans. Commun. Syst., vol. CS-8, pp. 57-67, 1960.
   * https://doi.org/10.1109/TCOM.1960.1097606 .. C. F. Lo, "The sum and Difference of Two Lognormal
   * Random Variables", Journal of Applied Mathematics, 2012, 838397.
   * https://doi.org/10.1155/2012/838397
   */
  public static double[][] sumIidLognormalsFW(int[] nVals, double mu, double sigma) {
    double[] mus = new double[nVals.length];
    double[] sigmas = new double[nVals.length];

    for (int i = 0; i < nVals.length; i++) {
      int n = nVals[i];
      double sigma2_x = FastMath.log((FastMath.exp(sigma * sigma) - 1.0) / n + 1.0);
      double mu_x = FastMath.log(n * FastMath.exp(mu)) + 0.5 * (sigma * sigma - sigma2_x);
      mus[i] = mu_x;
      sigmas[i] = FastMath.sqrt(sigma2_x);
    }

    return new double[][]{mus, sigmas};
  }


  /**
   * The other option according to Lo's paper. Claimed to be better for smaller n.
   * For large sigma (>0.5) and tail estimation, apparently Fenton-Wilkinson performs better.
   */

  public static double[][] sumIidLognormalsLo(int[] nVals, double mu, double sigma) {
    double[] mus = new double[nVals.length];
    double[] sigmas = new double[nVals.length];

    for (int i = 0; i < nVals.length; i++) {
      int n = nVals[i];

      double expMuSigma2 = FastMath.exp(mu + 0.5 * sigma * sigma);
      double Sp = n * expMuSigma2;

      double sigma2_s = (n / (Sp * Sp)) * sigma * sigma * expMuSigma2 * expMuSigma2;
      double mu_s = FastMath.log(Sp) - 0.5 * sigma2_s;

      mus[i] = mu_s;
      sigmas[i] = FastMath.sqrt(sigma2_s);
    }
    return new double[][]{mus, sigmas};
  }

  /**
   * Returns the zero-truncated Poisson quantile.
   *
   * @param lambda Poisson rate parameter(s)
   * @param q      quantile(s) of non-truncated dist
   * @return quantile(s) of the zero-truncated dist
   */
  public static double zeroTruncatedQuantile(double lambda, double q) {
    // Adjusts q for zero-truncated Poisson CDF
    double k0 = FastMath.exp(-lambda); // P(X=0)
    return Math.max((q - k0) / (1.0 - k0), 0.0);
  }

  /**
   * Returns the quantile of the non-truncated Poisson distribution
   * corresponding to a zero-truncated Poisson quantile.
   *
   * @param lambda Poisson rate parameter
   * @param q0     quantile of the zero-truncated distribution
   * @return quantile of the non-truncated distribution
   */
  public static double invertTruncatedQuantile(double lambda, double q0) {
    double k0 = FastMath.exp(-lambda); // P(X=0)
    return Math.max(q0 * (1.0 - k0) + k0, 0.0);
  }

  /**
   * Remember:
   * (1) Machine epsilon for doubles is roughly 2.22E-16. That's why the cdf fails at small a.
   * <p>
   * (2) There is another issue with this model. At very low µB, e.g., 0.02 and high alphas, e.g., 5%,
   * the threshold is computed as zero. That does not make that much sense. I suggest, ceiling all
   * to at least 1 (ideally somewhere at higher level).
   */

  // We estimate upperPoissonTerm uk using ALPHA/10 --> the smallest alpha 1E-14 not 1E-15.
  public static final double SMALLEST_LNAPPROX_ALPHA = 1E-14;

  public static double getOneSidedCompoundPoissonLnApproxQuantile(
      double lambda,
      double sigmaLog,
      double alpha) {

    double muLog = Statistics.getLognormalMuNormalized(sigmaLog);

    double result;

    if (!(lambda > 0)) {
      result = 0;
      // LOGGER.trace("Lambda, i.e., mu, was not strictly positive: " + lambda);
    } else {

      if (alpha > SMALLEST_LNAPPROX_ALPHA) {
        result = oneSidedCompoundPoissonBNSLnApproxQuantile(
            1 - alpha,
            lambda,
            muLog,
            sigmaLog);
      } else {
        LOGGER.warn("You are requesting a Compound Poisson threshold for a very low alpha error! " +
            "Since this is not possible to compute with those algorithms currently implemented," +
            "a rough approximation is returned.");
        double highestPossibleCompoundValue = oneSidedCompoundPoissonBNSLnApproxQuantile(
            1 - SMALLEST_LNAPPROX_ALPHA,
            lambda,
            muLog,
            sigmaLog);

          /*
          We cannot compute the compound Poisson for such low alpha.
          The alternative idea is to compute the lowest possible alpha
          and then check by how much the compound is larger than the Poisson.
          Next, compute Poisson for the actual alpha and add the offset.
          This will get worse when going far beyond the smallest alpha.
          However, when alpha is <<< 1E-16, e.g., 1E-19,
          this very likely is not meaningful anymore but clearly indicates that background
          statistics have wrong assumptions.
           */

        double correspondingPoissonValue = Statistics.getOneSidedPoissonQuantile(lambda,
            SMALLEST_LNAPPROX_ALPHA);

        double offset = highestPossibleCompoundValue - correspondingPoissonValue;

        double poissonReplacement = Statistics.getOneSidedPoissonQuantile(lambda, alpha);
        poissonReplacement += offset;
        result = poissonReplacement;
      }
    }

    return result;
  }


  public static double oneSidedCompoundPoissonLnApproxQuantile(double q, double lambda,
                                                               double muLog, double sigmaLog) {
    if (lambda > 0) {

      // Prepare estimate of upper k-value
      PoissonDistribution poisson = new PoissonDistribution(lambda);

      /*
      For the upper k limit, we still need to consider the "full range" of possible values
      or at least we must not cut at p=0.5. In that case, we would discard much of the upper
      half of the underlying Poisson distribution which would heavily bias the estimates!

      In compound Poisson, usually the contribution of the lognormal is greater
      (one outlier there has more impact than one outlier in the Poisson).
      So, for starters, we could make purePoissonTruncationAlpha 1 order of magnitude smaller to ensure
      enough
      LNs are summed to get an idea on about the tail.

      Then cap it to be alpha > MIN_ALPHA
      strictly 0 < alpha
       */

      double alpha = 1 - q;
      double purePoissonTruncationAlpha = alpha / 10;

      /*
       Original implementation in spCal assumes:
       "A reasonable overestimate of the upper value: uk = poisson.quantile(1.0 - 1e-12, lam)".
       Here, we just take at least 1e-12 or smaller but non-zero!
       */
      purePoissonTruncationAlpha = Math.min(purePoissonTruncationAlpha, 1E-12);
      purePoissonTruncationAlpha = Math.max(purePoissonTruncationAlpha, Math.nextUp(0d));

      int uk = Statistics.getOneSidedPoissonQuantile(lambda, purePoissonTruncationAlpha);
      // clamp uk: sanity limit to avoid huge allocations: poisson this many counts is unlikely.
      int maxUk = (int) 1E9; // used to be 1E6
      uk = Math.min(Math.max(uk, 1), maxUk);

      // extract the expected number of ions
      List<Integer> kVals = new ArrayList<>(uk + 1);
      List<Double> pdf = new ArrayList<>(uk + 1);
      for (int i = 0; i <= uk; i++) {
        double prob = poisson.probability(i);
        // check in case math gets ill for large values
        if (Double.isFinite(prob)) {
          kVals.add(i);
          pdf.add(prob);
        }
      }

      //  Calculate the zero-truncated quantile
      double q0 = zeroTruncatedQuantile(lambda, q);

      // The quantile is in the zero portion: stop. But then THR is = 0...
      if (q0 <= 0.0) {
        return 0.0;
      }

      // Remove k=0 and Re-normalize weights
      double[] weights = new double[pdf.size() - 1];
      int[] kTruncated = new int[kVals.size() - 1];
      double weightSum = 0;

      for (int i = 1; i < pdf.size(); i++) {
        double prob = pdf.get(i);
        weights[i - 1] = prob;
        weightSum += prob; // prepare the sum as well in the same loop for speed
        kTruncated[i - 1] = kVals.get(i); // "kTruncated[i - 1] = i" ignores isFinite() checks above
      }

      // normalize
      for (int i = 0; i < weights.length; i++) {
        weights[i] = weights[i] / weightSum;
      }

      // Get the sum LN for each value of the Poisson.
      double[][] musSigmas = sumIidLognormalsFW(kTruncated, muLog, sigmaLog);
      double[] mus = musSigmas[0];
      double[] sigmas = musSigmas[1];


      // case distinction between left or right side
      double linspaceStart;
      double linspaceStop;
      if (q < 0.5) {
        // left side!

        // Note that the custom lognormal function uses alpha=1-q and not q (quantile)!
        double q0Alpha = Math.max(Math.nextUp(0d), q0); // clamp alpha > 0; left tail uses q0

        // Estimate lower bound from lowest-mean lognormal:
        // The quantile of the first log-normal, can only be lower than this.
        double lowerQ = Statistics.getOneSidedLognormalQuantile(mus[0], sigmas[0], q0Alpha);
        linspaceStart = lowerQ;
        linspaceStop = 2 * lambda; // extend beyond the mean to capture more mass as distr is skewed
      } else {
        // right side!

        // Note that the custom lognormal function uses alpha=1-q and not q (quantile)!
        double q0Alpha = Math.max(Math.nextUp(0d), 1 - q0); // clamp alpha > 0; right tail uses 1-q0

        // Estimate upper bound from highest-mean lognormal:
        // The quantile of the last log-normal, can only be lower than this.
        double upperQ = Statistics.getOneSidedLognormalQuantile(mus[mus.length - 1],
            sigmas[sigmas.length - 1], q0Alpha);
        linspaceStart = lambda / 2; // start well below the mean to account for skew
        linspaceStop = upperQ;
      }


      // Now, we finally put together the summed distribution, i.e., the LN that is a sum of LNs.
      // We construct a ECDF and search linearly between lambda and upperQ
      int steps = 10_000;
      double[] xs = ArrUtils.linspace(linspaceStart, linspaceStop, steps);

      // Compute CDF mixture: This is the bottleneck!
      // a) constructs many LNs, b) computes many ln.cdf() with respect to speed
      // c) computes a cumulativeProbability() which can only approach 1 to epsilon = 1E-16 (precision))
      // ## Note: the CDF is constructed as a sum... we cannot simply break; when cdf[j] > q0
      // as we do not have to full picture, yet. Maybe there is a way to narrow it down, but so far
      // keep as is.
      double[] cdf = new double[steps];
      for (int i = 0; i < kTruncated.length; i++) {
        LogNormalDistribution ln = new LogNormalDistribution(mus[i], sigmas[i]);
        for (int j = 0; j < steps; j++) {
        /*
         Note:  cdf[j] += weights[i] * Statistics.lognormalCDF(xs[j],mus[i], sigmas[i]);
         and other shenanigans do not really help. The limit here is that the smallest number
         below 1 in 64 bit is just slightly more than 1E-16 smaller than one.
         i.e., the "digital epsilon" is our smallest alpha.
         */
          cdf[j] += weights[i] * ln.cumulativeProbability(xs[j]);
        }
      }

      //
      for (int i = 0; i < steps; i++) {
        if (cdf[i] > q0) {
          return xs[i];
        }
      }

      /*
      Faster alternative: Binary search.

      double left;
      double right;
      if (q < 0.5) {
        // left side!

        // Note that the custom lognormal function uses alpha=1-q and not q (quantile)!
        double q0Alpha = Math.max(Math.nextUp(0d), q0); // clamp alpha > 0; left tail uses q0

        // Estimate lower bound from lowest-mean lognormal:
        // The quantile of the first log-normal, can only be lower than this.
        double lowerQ = Statistics.getOneSidedLognormalQuantile(mus[0], sigmas[0], q0Alpha);
        left = lowerQ;
        right = 2 * lambda; // extend beyond the mean to capture more mass as distr is skewed
      } else {
        // right side!

        // Note that the custom lognormal function uses alpha=1-q and not q (quantile)!
        double q0Alpha = Math.max(Math.nextUp(0d), 1 - q0); // clamp alpha > 0; right tail uses 1-q0

        // Estimate upper bound from highest-mean lognormal:
        // The quantile of the last log-normal, can only be lower than this.
        double upperQ = Statistics.getOneSidedLognormalQuantile(mus[mus.length - 1],
            sigmas[sigmas.length - 1], q0Alpha);
        left = lambda / 2; // start well below the mean to account for skew
        right = upperQ;
      }

      double tolerance = 1e-6;
      double quantile = 0.0;

      while (right - left > tolerance) {
        double mid = (left + right) / 2.0;
        double cdfMid = 0.0;

        // ECDF mixture
        for (int i = 0; i < kTruncated.length; i++) {
          LogNormalDistribution ln = new LogNormalDistribution(mus[i], sigmas[i]);
          cdfMid += weights[i] * ln.cumulativeProbability(mid);
        }

        if (cdfMid < q0) {
          left = mid;  // need larger x to reach q0
        } else {
          right = mid; // x might be too large
        }
      }

      quantile = (left + right) / 2.0;
      return quantile;
       */

      return 0;
    } else {
      return 0;// Fallback: Poisson is not defined for lambda = 0...
    }
  }

  /*
  Uses binary search to accelerate.
   */
  public static double oneSidedCompoundPoissonBNSLnApproxQuantile(double q, double lambda,
                                                                  double muLog, double sigmaLog) {
    if (lambda > 0) {

      // Prepare estimate of upper k-value
      PoissonDistribution poisson = new PoissonDistribution(lambda);

      /*
      For the upper k limit, we still need to consider the "full range" of possible values
      or at least we must not cut at p=0.5. In that case, we would discard much of the upper
      half of the underlying Poisson distribution which would heavily bias the estimates!

      In compound Poisson, usually the contribution of the lognormal is greater
      (one outlier there has more impact than one outlier in the Poisson).
      So, for starters, we could make purePoissonTruncationAlpha 1 order of magnitude smaller to ensure
      enough
      LNs are summed to get an idea on about the tail.

      Then cap it to be alpha > MIN_ALPHA
      strictly 0 < alpha
       */

      double alpha = 1 - q;
      double purePoissonTruncationAlpha = alpha / 10;

      /*
       Original implementation in spCal assumes:
       "A reasonable overestimate of the upper value: uk = poisson.quantile(1.0 - 1e-12, lam)".
       Here, we just take at least 1e-12 or smaller but non-zero!
       */
      purePoissonTruncationAlpha = Math.min(purePoissonTruncationAlpha, 1E-12);
      purePoissonTruncationAlpha = Math.max(purePoissonTruncationAlpha, Math.nextUp(0d));

      int uk = Statistics.getOneSidedPoissonQuantile(lambda, purePoissonTruncationAlpha);
      // clamp uk: sanity limit to avoid huge allocations: poisson this many counts is unlikely.
      int maxUk = (int) 1E9; // used to be 1E6
      uk = Math.min(Math.max(uk, 1), maxUk);

      // extract the expected number of ions
      List<Integer> kVals = new ArrayList<>(uk + 1);
      List<Double> pdf = new ArrayList<>(uk + 1);
      for (int i = 0; i <= uk; i++) {
        double prob = poisson.probability(i);
        // check in case math gets ill for large values
        if (Double.isFinite(prob)) {
          kVals.add(i);
          pdf.add(prob);
        }
      }

      //  Calculate the zero-truncated quantile
      double q0 = zeroTruncatedQuantile(lambda, q);

      // The quantile is in the zero portion: stop. But then THR is = 0...
      if (q0 <= 0.0) {
        return 0.0;
      }

      // Remove k=0 and Re-normalize weights
      double[] weights = new double[pdf.size() - 1];
      int[] kTruncated = new int[kVals.size() - 1];
      double weightSum = 0;

      for (int i = 1; i < pdf.size(); i++) {
        double prob = pdf.get(i);
        weights[i - 1] = prob;
        weightSum += prob; // prepare the sum as well in the same loop for speed
        kTruncated[i - 1] = kVals.get(i); // "kTruncated[i - 1] = i" ignores isFinite() checks above
      }

      // normalize
      for (int i = 0; i < weights.length; i++) {
        weights[i] = weights[i] / weightSum;
      }

      // Get the sum LN for each value of the Poisson.
      double[][] musSigmas = sumIidLognormalsFW(kTruncated, muLog, sigmaLog);
      double[] mus = musSigmas[0];
      double[] sigmas = musSigmas[1];


      double left;
      double right;
      if (q < 0.5) {
        // left side!

        // Note that the custom lognormal function uses alpha=1-q and not q (quantile)!
        double q0Alpha = Math.max(Math.nextUp(0d), q0); // clamp alpha > 0; left tail uses q0

        // Estimate lower bound from lowest-mean lognormal:
        // The quantile of the first log-normal, can only be lower than this.
        double lowerQ = Statistics.getOneSidedLognormalQuantile(mus[0], sigmas[0], q0Alpha);
        left = lowerQ;
        right = 2 * lambda; // extend beyond the mean to capture more mass as distr is skewed
      } else {
        // right side!

        // Note that the custom lognormal function uses alpha=1-q and not q (quantile)!
        double q0Alpha = Math.max(Math.nextUp(0d), 1 - q0); // clamp alpha > 0; right tail uses 1-q0

        // Estimate upper bound from highest-mean lognormal:
        // The quantile of the last log-normal, can only be lower than this.
        double upperQ = Statistics.getOneSidedLognormalQuantile(mus[mus.length - 1],
            sigmas[sigmas.length - 1], q0Alpha);
        left = lambda / 2; // start well below the mean to account for skew
        right = upperQ;
      }

      double tolerance = 1e-3; // 3 digits should be fine
      int maxIter = 10_000; // worst performance is as good as linspace with 10k entries
      int counter = 0; // protection
      double quantile = 0.0;

      while (right - left > tolerance) {
        double mid = (left + right) / 2.0;
        double cdfMid = 0.0;

        // ECDF mixture
        for (int i = 0; i < kTruncated.length; i++) {
          LogNormalDistribution ln = new LogNormalDistribution(mus[i], sigmas[i]);
          cdfMid += weights[i] * ln.cumulativeProbability(mid);
        }

        if (cdfMid < q0) {
          left = mid;  // need larger x to reach q0
        } else {
          right = mid; // x might be too large
        }

        counter++;
        if (counter > maxIter) {
          break;
        }
      }

      quantile = (left + right) / 2.0;
      return quantile;


    } else {
      return 0;// Fallback: Poisson is not defined for lambda = 0...
    }
  }


  // ################################################################################################


  public static double getCompoundPoissonLnApproxSurvivalPValue(CompoundPoissonModel model,
                                                                double value,
                                                                double lambda,
                                                                double muLog,
                                                                double sigmaLog) {

    double pValue;
    if (model.equals(CompoundPoissonModel.LOG_APPROX)) {
      /*
       * Note that this function does not protect against too large values and pValues may well become
       * unreliable when exceeding 1E-14 to 1E-15.
       * Note that under these circumstances, it cannot be guaranteed that the lognormal approximation
       * produces strictly monotonous results (smaller p for larger values)! So far, a range from
       * value [0.01, 1000] and p [0.99, 1.0E-17] was tested and it looks sufficiently monotonous.
       * So keep this in mind!
       */
      pValue = compoundPoissonLnApproxSurvivalPValue(value, lambda, muLog, sigmaLog);
    } else {
      // Only ensures that p>0. No further checking of caps internally in this function:
      pValue = SpTool3Main.getRunTime().getCompoundPoissonQuantiles().getPValue(lambda, sigmaLog, value);
    }

    return pValue;
  }


  public static double compoundPoissonLnApproxSurvivalPValue(double value,
                                                             double lambda,
                                                             double muLog,
                                                             double sigmaLog) {
    if (lambda > 0) {

      // (1) Prepare estimate of upper k-value
      org.apache.commons.statistics.distribution.PoissonDistribution poisson =
          org.apache.commons.statistics.distribution.PoissonDistribution.of(lambda);

      /*
        (1a) For pour Poisson case: estimate p(X >= value),

        One way to do this is using pValue which is 1-alpha.
        The pVal:
        double purePoissonPValue = poisson.survivalProbability((int) Math.floor(value));

        However: note that in double precision, we can get closer to zero than to one.
        Our main problem is the right tail, where alpha is small and pVal is large.
        Hence, it gives more leeway to use alpha instead:
        double alpha = poisson.cumulativeProbability((int) Math.floor(value));
        In addition, we have established some checks and workarounds for getting Poisson statistics
        based on alpha already, so let us use these.

        Besides, using floor to achieve consistent casting to int
       */
      double purePoissonTruncationAlpha = poisson.cumulativeProbability((int) Math.floor(value));

      /*
       1b) In compound Poisson, usually the contribution of the lognormal is greater
       (one outlier there has more impact than one outlier in the Poisson).
       So, for starters, we could make purePoissonTruncationAlpha 1 order of magnitude smaller to ensure
       enough
       LNs are summed to get an idea on about the tail.

       Then cap it to be alpha > MIN_ALPHA
       strictly 0 < alpha
       */
      purePoissonTruncationAlpha = purePoissonTruncationAlpha / 10;

      /*
       Original implementation in spCal assumes:
       "A reasonable overestimate of the upper value: uk = poisson.quantile(1.0 - 1e-12, lam)".
       Here, we just take at least 1e-12 or smaller but non-zero!
       */
      purePoissonTruncationAlpha = Math.min(purePoissonTruncationAlpha, 1E-12);
      purePoissonTruncationAlpha = Math.max(purePoissonTruncationAlpha, Math.nextUp(0d));

      //
      int uk = Statistics.getOneSidedPoissonQuantile(lambda, purePoissonTruncationAlpha);
      // clamp uk: sanity limit to avoid huge allocations: poisson this many counts is unlikely.
      int maxUk = (int) 1E9; // used to be 1E6
      uk = Math.min(Math.max(uk, 1), maxUk);

      // extract the expected number of ions
      List<Integer> kVals = new ArrayList<>(uk + 1);
      List<Double> pdf = new ArrayList<>(uk + 1);
      for (int i = 0; i <= uk; i++) {
        double prob = poisson.probability(i);
        // check in case math gets ill for large values
        if (Double.isFinite(prob)) {
          kVals.add(i);
          pdf.add(prob);
        }
      }

      // Remove k=0 and Re-normalize weights
      double[] weights = new double[pdf.size() - 1];
      int[] kTruncated = new int[kVals.size() - 1];

      double weightSum = 0;
      for (int i = 1; i < pdf.size(); i++) {
        double prob = pdf.get(i);
        weightSum += prob; // prepare the sum as well in the same loop for speed
        weights[i - 1] = prob;
        kTruncated[i - 1] = kVals.get(i); // "kTruncated[i - 1] = i" ignores isFinite() checks above
      }

      // normalize weights
      for (int i = 0; i < weights.length; i++) {
        weights[i] = weights[i] / weightSum;
      }

      // Get the sum LN for each value of the Poisson: Fenton-Wilkinson approximation.
      double[][] musSigmas = sumIidLognormalsFW(kTruncated, muLog, sigmaLog);
      double[] mus = musSigmas[0];
      double[] sigmas = musSigmas[1];

      // Compute the mixture survival function at the observed value to obtain pValue
      double pValue0 = 0.0;
      for (int i = 0; i < kTruncated.length; i++) {
        // Use numerically stable survival function
        org.apache.commons.statistics.distribution.LogNormalDistribution ln =
            org.apache.commons.statistics.distribution.LogNormalDistribution.of(mus[i], sigmas[i]);
        pValue0 += weights[i] * ln.survivalProbability(value);
      }
      // This pValue is quasi-zeroTruncated! --> convert to unconditional survival probability
      double pValue = (1.0 - Math.exp(-lambda)) * pValue0;
      // clamp value
      return Math.min(pValue, 1);
    } else {
      return 1;
    }
  }


//################################################################################################################


  /**
   * Interpolate to find the y for the x: this could be used for the lookup table compound Poisson approach.
   * Filled in by chatGPT.
   */
  public static double interpolate(double[] x, double[] y, double xInterp) {
    int index = Arrays.binarySearch(x, xInterp);

    if (index >= 0) {
      // Exact match found
      return y[index];
    } else {
      // Get insertion point
      int insertionPoint = -index - 1;

      // Interpolate between x[insertionPoint - 1] and x[insertionPoint]
      int i = insertionPoint - 1;

      double x0 = x[i];
      double x1 = x[i + 1];
      double y0 = y[i];
      double y1 = y[i + 1];

      double t = (xInterp - x0) / (x1 - x0);
      return y0 * (1 - t) + y1 * t;

      /*
      Works!
        System.out.println( Statistics.interpolate(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4}, 2));
        System.out.println( Statistics.interpolate(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4}, 1
        .5));
        System.out.println( Statistics.interpolate(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4}, 2
        .5));
        System.out.println( Statistics.interpolate(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4}, 4));
        System.out.println( Statistics.interpolate(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4}, 1));
        System.out.println( Statistics.interpolate(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4}, 2
        .75));
       */
    }
  }

  /**
   * Interpolate to find the y for the x: this is used for the lookup table compound Poisson approach.
   * Filled in by chatGPT.
   */
  // Checked in Excel, works; also works for xInterp outside of x1 or x2.
  public static double interpolate_1D(double x1, double x2, double y1, double y2, double xInterp) {
    if (x1 == x2) {
      // If identical , return the mean. Probably most fail-safe
      return (y1 + y2) / 2;
    }

    double t = (xInterp - x1) / (x2 - x1);
    return y1 * (1 - t) + y2 * t;
  }

  /*
Uses the fact that most DP are BG.
If we assume that all H that would originate under the null are within (n+1)*10,
then (accepting bias through pre-selection) the millions of BG data points may let us
calibrate the p-Test in situ. (just an idea)
This may be better than hoping that the distribution of the BLN have correct mean, and so forth.
This may also work better for pure Poisson case (although that does not make a lot of sense as TOFs
always will produce compound poisson data.
 */
  public static double[] estimateGammaP(int nIsotopes, double[] H) {
    // empirical "limit": discard large H (particles)
    // double upperLimit = (nIsotopes + 1) * 10; // works well empirically
    // chatGPT says: double check later E[H]=2n,Var[H]=4n
    // Nice property: does not depend on empirical SD and so fort as we just assume:
    // under the null, H should be chiSquare, and therefore we take that as a rough prox
    //  to cap H (although the whole point of this method is NOT to use ChiSquare).
    double upperLimit = 2 * nIsotopes + 5 * 2 * Math.sqrt(nIsotopes);

    List<Double> hInLimit = new ArrayList<>(H.length);
    for (int i = 0; i < H.length; i++) {
      if (H[i] < upperLimit) {
        hInLimit.add(H[i]);
      }
    }

    // Step 2: Estimate gamma parameters from mean & std
    DescriptiveStatistics stats = new DescriptiveStatistics();
    for (double d : hInLimit) {
      stats.addValue(d);
    }
    double mu_data = stats.getMean();
    double sigma_data = stats.getStandardDeviation();

    // Method-of-moments for gamma: computed from wiki moments as A. alpha, B. theta
    double b_gamma = sigma_data * sigma_data / mu_data;
    double a_gamma = mu_data / b_gamma;

    // Create gamma distribution object
    GammaDistribution pd = GammaDistribution.of(a_gamma, b_gamma);

    // Step 3: Compute p-values (CDF for each data point) in the original H array
    double[] p_values = new double[H.length];
    for (int i = 0; i < H.length; i++) {
      double combinedPValue = 1 - (pd.cumulativeProbability(H[i]));
      // we should not report p=0 here;
      combinedPValue = Math.max(combinedPValue, MIN_P_VALUE);
      p_values[i] = combinedPValue;
    }

    return p_values;
  }

  public static double[] computeChiSquareP(int nIsotopes, double[] H) {
    // Chi-square distribution with 2k degrees of freedom where k is number of combined isotopes
    ChiSquaredDistribution chiSquared = new ChiSquaredDistribution(2 * nIsotopes);
    double[] pVals = new double[H.length];
    for (int i = 0; i < H.length; i++) {
      double combinedPValue = 1.0 - chiSquared.cumulativeProbability(H[i]);
      // we should not report p=0 here;
      combinedPValue = Math.max(combinedPValue, MIN_P_VALUE);
      pVals[i] = combinedPValue;
    }
    return pVals;
  }

}