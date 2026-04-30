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

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.hipparchus.linear.ArrayRealVector;
import util.ArrUtils;

public abstract class Background {


  /*
        ################################################################################################
         */
  public static double[] getSteadyBackground(double mean, int n) {
    return Statistics.poissrnd(mean, n);
  }

  public static void addSteadyBackground(double mean, double[] signal) {
    if (mean > 0) {
      // PoissonDistribution poissonDistribution = new PoissonDistribution(mean);
      for (int i = 0; i < signal.length; i++) {
        // signal[i] = signal[i] + poissonDistribution.sample();
        signal[i] = signal[i] + mean;
      }
    }
  }

  /**
   * @param T:         If T=5, you get one full sine swing (up and down) to reach 0 again at t=5
   *                   sec.
   * @param amplitude: Should be 0 < a < 1 to guarantee I > 0;
   */
  public static double[] getSineBackground(double mean, int n, double T, double amplitude,
                                           double dwellTime) {
    // e.g. amplitude = 0.5
    // T = 20

    double[] data = new double[n];
    for (int i = 0; i < n; i++) {
      double mu = mean * (1 + amplitude * Math.sin(1 / T * 2 * Math.PI * i * dwellTime));
      if (mu > 0) {
        data[i] = Statistics.poissrnd(mu);
      }
    }
    return data;
  }

  public static void addSineBackground(double mean, double[] signal,
                                       double T, double amplitude, double dwellTime) {

    PoissonDistribution poissonDistribution = new PoissonDistribution(mean);

    for (int i = 0; i < signal.length; i++) {

      double mu = poissonDistribution.sample() * (1 + amplitude * Math
          .sin(1 / T * 2 * Math.PI * i * dwellTime));
      if (mu > 0) {
        signal[i] = signal[i] + mu;
      } else {
        signal[i] = signal[i] + 0;
      }
    }
  }

  // Sawtooth?
// https://stackoverflow.com/questions/1073606/is-there-a-one-line-function-that-generates-a-triangle-wave

//  https://stackoverflow.com/questions/16930581/fast-sine-and-cosine-function-in-java

  public static void addSpittingSineBackgroundFast(double mean, double[] signal,
                                                   double T, double amplitude, double dwellTime) {

    // Outcome should be more or less similar: Let's keep one Poisson distribution
    // and let's not put the current state of the sine into the Poisson.
    PoissonDistribution poissonDistribution = new PoissonDistribution(mean);

    for (int i = 0; i < signal.length; i++) {

      // We know the period is 5. Thus, we only need what "goes beyond one period"
      double argument = 1 / T * 2 * Math.PI * i * dwellTime;
      argument = argument % T;

      // double mu = poissonDistribution.sample() * (1 + amplitude * fastSin(argument));
      double mu = mean * (1 + amplitude * fasterSine(argument));
      signal[i] = signal[i] + mu;
    }
  }

  /**
   * Created with chatGPT and tested.
   * The background:
   * starts at value mean,
   * asymptotically approaches factor·mean
   */
  public static void addExponentialBackground(double[] signal, double m, double f) {
    int n = signal.length;
    if (n == 0) {
      return;
    }

    double start = m;
    double end = f * m;
    double amplitude = end - start;

    double xMax = 5.0;

    double[] background = new double[n];
    double sum = 0.0;

    for (int i = 0; i < n; i++) {
      double x = xMax * i / (n - 1.0);
      double value = start + amplitude * (1.0 - Math.exp(-x));
      background[i] = value;
      sum += value;
    }

    double meanBackground = sum / n;

    // Likely exponential makes the BG too large -> correct by division (not by const. offset!)
    double ratio = meanBackground / m;
    background = ArrUtils.divide(background, ratio);

    for (int i = 0; i < n; i++) {
      signal[i] += background[i];
    }
  }

  /**
   * Created with chatGP and tested.
   * The background:
   * starts at value mean,
   * approaches factor·mean
   */
  public static void addLinearBackground(double[] signal, double m, double f) {
    int n = signal.length;
    if (n == 0) {
      return;
    }

    double start = m;
    double end = f * m;
    double amplitude = end - start;

    double[] background = new double[n];
    double sum = 0.0;

    for (int i = 0; i < n; i++) {
      double t = (n == 1) ? 0.0 : i / (n - 1.0);
      double value = start + amplitude * t;
      background[i] = value;
      sum += value;
    }

    double meanBackground = sum / n;
    double offset = m - meanBackground;

    for (int i = 0; i < n; i++) {
      signal[i] += background[i] + offset;
    }
  }

  public static void addSineBackgroundFast(double mean, double[] signal,
                                           double T, double amplitude, double dwellTime) {

    for (int i = 0; i < signal.length; i++) {

      // We know the period is 5. Thus, we only need what "goes beyond one period"
      double x = i * dwellTime;
      // When x=T, the argument becomes 2PI, i.e., 1 cycle of sine.
      // Thus, we need to modulo to find out, how many incomplete cycles there are.
      // Note that the approximated sine is only accurate within one cycle.
      x = x % T;
      double argument = 1 / T * 2 * Math.PI * x;

      // double mu = poissonDistribution.sample() * (1 + amplitude * fastSin(argument));
      double mu = mean * (1 + amplitude * fasterSine(argument));
      signal[i] = signal[i] + mu;
    }
  }

  // Idea from chatgpt. Seems fine for ONE sine wave!
  private static final double ERROR_SICP = 0.01;
  private static final double ERROR_FAST = 0.5;

  // Recursive function to approximate sine: https://thejavamathematician.blogspot.com/2015/01/recursive-sine-approximation.html
  public static double fastSin(double x) {

    if (Math.abs(x) < ERROR_SICP) {
      return x; // Base case
    } else {
      double y = fastSin(x / 3); // Recursive call
      return 3 * y - 4 * Math.pow(y, 3); // Approximation
    }
  }

  // Constants chosen to minimize rounding error
  private static final double INV_TWO_PI = 0.15915494309189533577; // 1 / (2π)
  private static final double TWO_PI = 6.28318530717958647692;

  // Idea from chatgpt.
  public static double fasterSine(double x) {
    // Range reduction to [-π, π] using nearest integer
    x = x - Math.rint(x * INV_TWO_PI) * TWO_PI;

    double x2 = x * x;

    // 7th-order minimax-style polynomial
    return x * (1.0
        - x2 * (1.0 / 6.0
        - x2 * (1.0 / 120.0
        - x2 * (1.0 / 5040.0))));
  }

}
