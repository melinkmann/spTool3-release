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

package math;

public class PNCorrection {

  // M.E. spTool version. CHECKED!
  public static double calcReducedRate(double rateNPperSecTrue, double iclSec, double dtSec) {
    double correctedDeadTime = iclSec + 1.5 * dtSec;
    return rateNPperSecTrue * Math.exp(-rateNPperSecTrue * correctedDeadTime);
  }

  /**
   * CHECKED!
   * True rate at which the observed rate peaks: 1/a, with a = icl + 1.5*dt.
   */
  public static double maxTrue(double iclSec, double dtSec) {
    double correctedDeadTime = iclSec + 1.5 * dtSec;
    return 1.0 / correctedDeadTime;
  }

  /**
   * CHECKED!
   * Highest observed rate the model can produce.
   */
  public static double maxObserved(double iclSec, double dtSec) {
    double correctedDeadTime = iclSec + 1.5 * dtSec;
    return 1.0 / (Math.E * correctedDeadTime);
  }


  // CHECKED!
  public static double calcTrueRate(double rateNPperSecObserved, double iclSec, double dtSec) {
    final long maxIterations = 10_000_000L;
    // true rate beyond this value would reach into the paralysing branch to the right of the curve
    // spICP-MS should never steer into this territory, if it does, the measurement needs dilution not maths!
    double result = maxTrue(iclSec, dtSec);
    // observed rate cannot be larger than this (if it is, detection criteria affect detection,
    // e.g., dissecting peaks)
    double maxObservable = maxObserved(iclSec, dtSec);
    // if this is not the case, we cannot fix it anyway:
    if (rateNPperSecObserved < maxObservable) {

      double[] steps = {10.0, 1.0, 0.1, 0.01};
      double rate = 0.0;
      long iterations = 0;

      for (double stepSize : steps) {
        // reset for each step size, this max is just a safety measure against loop getting stuck
        iterations = 0;
        while (iterations <= maxIterations
            && calcReducedRate(rate + stepSize, iclSec, dtSec) <= rateNPperSecObserved) {
          rate += stepSize;
          iterations++;
        }
      }
      if (iterations <= maxIterations) {
        result = rate;
      }
    }
    return result;
  }

  /**
   * CHECKED:
   * True rate at which the observed rate has lost 'percent' %.
   * Percent is the percentage of missed events, i.e., (true-observed)/true, i.e., 1 - observed/true.
   * Thus, returns the true rate at which the observed rate has lost 'percent' % (0 <= percent <= 63.2).
   * Above 63.2 % (past the peak) it returns the peak position, maxTrue = 1/corrDT.
   */
  public static double trueRateAtLossPercent(double percent, double iclSec, double dtSec) {
    // Fallback: if sth fails, report the peak
    double result = maxTrue(iclSec, dtSec);

    // cap percent value, transform to fraction
    percent = Math.max(0, percent);
    percent = Math.min(100, percent);
    double frac = percent / 100.0;

    double correctedDeadTime = iclSec + 1.5 * dtSec;

    // log1p is better at log 1+x at small x; here log1p(-x) = log(1-x)
    double logDiffFrac = -Math.log1p(-frac);
    if (logDiffFrac <= 1) {
      result = logDiffFrac / correctedDeadTime;
    }
    return result;
  }

  /**
   * CHECKED:
   * Percent is the percentage of missed events, i.e., (true-observed)/true, i.e., 1 - observed/true.
   * Thus, returns the observed rate at which the observed rate has lost 'percent' % (0 <= percent <= 63.2).
   * Above 63.2 % (past the peak) it returns the peak value, maxObserved.
   *
   */
  public static double obsvRateAtLossPercent(double percent, double iclSec, double dtSec) {
    // Fallback: if sth fails, report the peak
    double result = maxObserved(iclSec, dtSec);

    // cap percent value, transform to fraction
    percent = Math.max(0, percent);
    percent = Math.min(100, percent);
    double frac = percent / 100.0;

    double correctedDeadTime = iclSec + 1.5 * dtSec;

    // log1p is better at log 1+x at small x; here log1p(-x) = log(1-x)
    double logDiffFrac = -Math.log1p(-frac);
    if (logDiffFrac <= 1) {
      result = (logDiffFrac * (1.0 - frac)) / correctedDeadTime;
    }
    return result;
  }


  /// P.E.P. JAAS.

  // TODO: Claude provided most derived eq. here. Compare with notes and check if OK.

  public static double observedExact(double rateNPperSecTrue, double iclSec, double dtSec) {
    return Math.exp(-2.0 * rateNPperSecTrue * dtSec)
        * Math.expm1(rateNPperSecTrue * dtSec)
        * Math.exp(-rateNPperSecTrue * iclSec)
        * 1 / dtSec;
  }

  /**
   * Cladue:
   * True rate at which the observed rate of the exact model peaks:
   * ln((icl + 2 dt)/(icl + dt)) / dt.
   */
  public static double maxTrueExact(double iclSec, double dtSec) {
    return Math.log1p(dtSec / (iclSec + dtSec)) / dtSec;
  }

  /**
   * Claude:
   * Highest observed rate the exact model can produce:
   * exp(-(icl + dt) * maxTrueExact) / (icl + 2 dt).
   */
  public static double maxObservedExact(double iclSec, double dtSec) {
    double maxTrue = maxTrueExact(iclSec, dtSec);
    return Math.exp(-(iclSec + dtSec) * maxTrue) / (iclSec + 2.0 * dtSec);
  }

  /**
   * Observed/true ratio of the exact model: exp(-r icl) * exp(-u) * (1 - exp(-u)) / u, u = r dt.
   */
  private static double survivingFraction(double rate, double iclSec, double dtSec) {
    double u = rate * dtSec;
    double result = 1.0;
    if (u > 0) {
      result = Math.exp(-rate * iclSec) * Math.exp(-u) * (-Math.expm1(-u)) / u;
    }
    return result;
  }

  /**
   * Claude:
   * Observed rate (exact model) at which the observed rate has lost 'percent' %.
   * Valid up to the loss reached at the peak (about 63-64 %, depends on icl and dt).
   * Beyond that (past the peak) it returns the peak value, maxObservedExact.
   */
  public static double obsvRateAtLossPercentExact(double percent, double iclSec, double dtSec) {
    percent = Math.max(0, percent);
    percent = Math.min(100, percent);

    double result = maxObservedExact(iclSec, dtSec);
    double surviving = 1.0 - percent / 100.0;
    double maxTrue = maxTrueExact(iclSec, dtSec);
    if (survivingFraction(maxTrue, iclSec, dtSec) <= surviving) { // loss lies on the left branch
      double low = 0.0;
      double high = maxTrue;
      for (int i = 0; i < 100; i++) {
        double mid = 0.5 * (low + high);
        if (survivingFraction(mid, iclSec, dtSec) > surviving) {
          low = mid;
        } else {
          high = mid;
        }
      }
      result = 0.5 * (low + high) * surviving; // observed = true * surviving fraction
    }
    return result;
  }

  /**
   * Claude: Observed rate of the exact model: (exp(-p r) - exp(-q r)) / dt, p = icl + dt, q = icl + 2 dt.
   */
  public static double calcReducedRateExact(double rateNPperSecTrue, double iclSec, double dtSec) {
    double u = rateNPperSecTrue * dtSec;
    return Math.exp(-rateNPperSecTrue * iclSec) * Math.exp(-u) * (-Math.expm1(-u)) / dtSec;
  }

  /// Claude:
  public static double calcTrueRateExact(double rateNPperSecObserved, double iclSec, double dtSec) {
    final long maxIterations = 10_000_000L;
    double result = maxTrueExact(iclSec, dtSec);
    if (rateNPperSecObserved < maxObservedExact(iclSec, dtSec)) {
      double[] steps = {10.0, 1.0, 0.1, 0.01};
      double rate = 0.0;
      long iterations = 0;
      for (double step : steps) {
        while (iterations <= maxIterations
            && calcReducedRateExact(rate + step, iclSec, dtSec) <= rateNPperSecObserved) {
          rate += step;
          iterations++;
        }
      }
      if (iterations <= maxIterations) {
        result = rate;
      }
    }
    return result;
  }


}
