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

package io;

import analysis.SearchUtils;
import core.SpTool3Main;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import math.stat.Mode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.special.Erf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.CompoundPoissonModel;
import sandbox.montecarlo.Statistics;
import util.ArrUtils;

import java.util.*;

public class SiaShapeEstimationBruker {

  private static final Logger LOGGER = LogManager.getLogger(SiaShapeEstimationBruker.class);

  public static double computeShapeTIMSTOF(double[] intensityTrueZeros, double[] intensityTrimmed) {
    double shape = Double.NaN;
    if (intensityTrueZeros.length > 0 && intensityTrimmed.length == intensityTrueZeros.length) {

      // parameters
      int requiredZeros = 15;

      // from the config
      double alpha = SpTool3Main.getRunTime().getConfParams().getSiaParticleFilterAlpha().getValue();
      double increasedAlpha = SpTool3Main.getRunTime().getConfParams().getSiaParticleFilterAlphaLowerLimit()
          .getValue();

      // Detect NP events ONCE on the trimmed data (clean ion signals) and apply the same
      // bgIndexMask-mask to both arrays, so lambda and the moments come from the same time points.
      boolean[] bgIndexMask = createKeepMaskIteratively(intensityTrimmed, alpha, increasedAlpha, 0);
      double[] filteredDataTrim = applyMask(intensityTrimmed, bgIndexMask);

      // check zeros (on the unfiltered true-zeros data)
      double[] nonzeroDataTZ = ArrUtils.nonzero(intensityTrueZeros);
      int zerosInDataTZ = intensityTrueZeros.length - nonzeroDataTZ.length;

      if (zerosInDataTZ >= requiredZeros) {

        // remove the NP signals via the mask which was determined for the trimmed data
        double[] filteredDataTZ = applyMask(intensityTrueZeros, bgIndexMask);
        double[] nonzeroFilteredDataTZ = ArrUtils.nonzero(filteredDataTZ);

        // We do not know the underlying Poisson lambda.
        // --> estimate it from the number of zeros for the Poisson component:
        // P(k=0) = exp(-lambda)  →  lambda = -ln(nZeros / nTotal)
        // This is essentially a MLE of lambda using only the zero-count observations.
        // The true zeros are used here, so the noisy low-signal ions are kept as ion arrivals.
        int nZeros = filteredDataTZ.length - nonzeroFilteredDataTZ.length;
        double pZero = (double) nZeros / (double) filteredDataTZ.length;  // P(k=0)
        double lambda = -Math.log(pZero);

        /*
        Math:

        Shape parameter sigma:
        sigma = sqrt(ln(Var(Y) × lambda / E[Y]²))

        Thus, we compute:
        ratio = Var(Y) * lambda / E[Y]^2 = exp(sigma^2)

        Note: ratio must be > 1 for sigma^2 > 0. If ratio <= 1 the data are under-dispersed
        relative to a compound Poisson, indicating numerical issue or too few spikes removed.
        */

        // moments from the trimmed data (noisy ions removed)
        double dataMean = MeasureOfLocation.MEAN.calc(filteredDataTrim);
        double dataVar = Math.pow(MeasureOfSpread.SD.calc(filteredDataTrim), 2);

        double ratio = dataVar * lambda / (dataMean * dataMean);
        if (ratio > 1.0) {
          shape = Math.sqrt(Math.log(ratio));
        }

        // NOT ENOUGH ZEROS!
      } else {
        /*
         * This path only works accurately, if the cts data has been normalised to mean = 1 properly.
         * If instead, the SIA is not captured accurately for each isotope, this will likely not work
         * totally accurate...
         */
        // there are not enough zeros, thus, work with the trimmed data only
        double dataMean = MeasureOfLocation.MEAN.calc(filteredDataTrim);
        double dataVar = Math.pow(MeasureOfSpread.SD.calc(filteredDataTrim), 2);

        double lambda = dataMean;

        double ratio = dataVar * lambda / (dataMean * dataMean);
        if (ratio > 1.0) {
          shape = Math.sqrt(Math.log(ratio));
        }
      }
    }
    return shape;
  }

  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________


  /**
   * Claude Sonnet 5.5: Lognormal fit of the single-ion area distribution (compound Poisson-lognormal,
   * left-truncated
   * at the trim threshold). Lambda comes from the true zeros, the shape from the trimmed values.
   *
   * @return {mu, sigma}, both NaN if the fit fails
   */
  public static double[] fitSiaLognormalTIMSTOFMLE(double[] intensityTrueZeros, double[] intensityTrimmed,
                                                   double trimThreshold) {
    double[] failed = {Double.NaN, Double.NaN};
    // Both arrays must describe the same time slices, and we need a valid cut-off value.
    if (intensityTrueZeros.length == 0 || intensityTrimmed.length != intensityTrueZeros.length
        || !(trimThreshold > 0)) {
      return failed;
    }

    double alpha = SpTool3Main.getRunTime().getConfParams().getSiaParticleFilterAlpha().getValue();
    double increasedAlpha =
        SpTool3Main.getRunTime().getConfParams().getSiaParticleFilterAlphaLowerLimit().getValue();

    // STEP 1: find the time slices that are nanoparticle events (not single-ion background).
    // keep[i] == true means "slice i is background". Same slices are removed from both arrays.
    boolean[] keep = createKeepMaskIteratively(intensityTrimmed, alpha, increasedAlpha, 0);
    double[] filteredTZ = applyMask(intensityTrueZeros, keep);
    double[] filteredTrim = applyMask(intensityTrimmed, keep);

    // STEP 2: lambda = average number of ions per time slice.
    // P(slice is empty) = exp(-lambda)  ->  lambda = -ln(fraction of zeros).
    // With too few zeros this is unreliable, so fall back to lambda = mean signal
    // (only valid if the data are normalised so that one ion has an average signal of 1).
    int zerosTZ = filteredTZ.length - ArrUtils.nonzero(filteredTZ).length;
    double lambda = zerosTZ >= 15 ? -Math.log((double) zerosTZ / filteredTZ.length)
        : MeasureOfLocation.MEAN.calc(filteredTrim);
    if (!(lambda > 0) || Double.isInfinite(lambda)) {
      return failed;
    }

    // STEP 3: take only the nonzero trimmed signals and put them on the ln scale,
    // where a lognormal distribution becomes an ordinary bell curve.
    double[] positives = ArrUtils.positiveNonzero(filteredTrim);
    int n = positives.length;
    if (n < 10) {
      return failed;   // too little data for a fit
    }
    double[] logY = new double[n];
    double mean = 0;
    for (int i = 0; i < n; i++) {
      logY[i] = Math.log(positives[i]);
      mean += logY[i];
    }
    mean /= n;
    double ss = 0;
    for (double ly : logY) {
      ss += (ly - mean) * (ly - mean);
    }
    // Mean and spread of ln(signal): a rough first guess for (mu, sigma).
    // It ignores multi-ion slices and the cut-off, so it is only the optimizer's starting point.
    double sd = Math.sqrt(ss / n);

    final double lam = lambda;
    final double logT = Math.log(trimThreshold);
    // Largest number of ions per slice that we still consider (more ions are practically impossible
    // at this lambda). 8 is enough for small lambda; it grows for larger lambda, capped at 60.
    // CUT this down to 30 to speed up
    // final int nMax = (int) Math.min(30, Math.max(8, Math.ceil(lam + 8 * Math.sqrt(lam) + 2)));
    //*REPLACED BY*

    // --------------------- ..................
    // Precompute Poisson weights (independent of mu and sigma) and estimate max iters
    // Poisson weights P(k ions | slice has >= 1 ion), added up until the remaining tail is negligible.
    // nMax therefore adapts to lambda: ~3-5 for tiny lambda, larger for large lambda.
    final int K_CAP = 60;                  // hard upper limit, in case lambda is huge
    final double TAIL = 1e-8;              // probability mass we are willing to ignore
    double[] w = new double[K_CAP + 1];

    double logLambda = Math.log(lam);
    double norm = -Math.expm1(-lam);       // 1 - exp(-lambda)
    double logFact = 0.0;
    double cum = 0.0;
    int kMaxTmp = K_CAP;
    for (int k = 1; k <= K_CAP; k++) {
      logFact += Math.log(k);
      w[k] = Math.exp(k * logLambda - lam - logFact) / norm;
      cum += w[k];
      if (cum >= 1.0 - TAIL) {
        kMaxTmp = k;
        break;
      }
    }
    final int nMax = kMaxTmp;              // final, because the lambda expression below captures it
    if (nMax == K_CAP && cum < 1.0 - TAIL) {
      LOGGER.info("SIA MLE: Poisson tail truncated at K_CAP, lambda = " + lam);
    }

    double[] m = new double[nMax + 1];
    double[] s = new double[nMax + 1];
    final double[] wFit = Arrays.copyOf(w, nMax + 1);
    //.................. ---------------------

    // STEP 4: let the optimizer search for the (mu, sigma) with the lowest "misfit score".
    try {
      SimplexOptimizer optimizer = new SimplexOptimizer(1e-5, 1e-5);
      PointValuePair res = optimizer.optimize( //1e-10; 1e-7
          new MaxEval(3000),
          // sigma must stay positive; otherwise return a huge penalty so the optimizer backs off
          new ObjectiveFunction(p -> p[1] <= 1e-6 ? 1e300 : negLogLikTruncated(logY, lam, p[0], p[1], logT,
              nMax, m, s, wFit)),
          GoalType.MINIMIZE,
          new InitialGuess(new double[]{mean, sd}),
          new NelderMeadSimplex(new double[]{0.1, 0.05}));   // initial step sizes for mu and sigma
      // System.out.println("nEval "+optimizer.getEvaluations()); --> only 30-80; negLogLik() itself is slow
      return res.getPoint();   // {mu, sigma}
    } catch (RuntimeException e) {
      LOGGER.info("MLE reached max. eval. " + "Message: " + ExceptionUtils.getMessage(e)
          + "Stack trace: " + ExceptionUtils.getStackTrace(e));
      return failed;
    }
  }

  /**
   * Claude Sonnet 5.5: "Misfit score" (negative log-likelihood): lower = the model (mu, sigma, lambda)
   * explains
   * the observed signals better. The optimizer minimises this.
   */
  private static double negLogLikTruncated(double[] logY, double lambda, double mu, double sigma,
                                           double logT, int nMax, double[] m, double[] s, double[] w) {
    double s2 = sigma * sigma;
    double expS2m1 = Math.expm1(s2);       // exp(sigma^2) - 1, numerically stable
    // double norm = -Math.expm1(-lambda);    // 1 - exp(-lambda) = P(slice has at least 1 ion)

    // double[] w = new double[nMax + 1];     // w[k]: P(exactly k ions | slice is nonzero)
    // double[] m = new double[nMax + 1];     // m[k], s[k]: centre and width (on ln scale) of the
    // double[] s = new double[nMax + 1];     //             signal curve of a slice with k ions
    // double logFact = 0;                    // ln(k!)
    double surv = 0;                       // fraction of the whole curve that lies ABOVE the cut-off

    for (int k = 1; k <= nMax; k++) {

      // Poisson probability of k ions, rescaled because we only look at nonzero slices
      // logFact += Math.log(k);
      // w[k] = Math.exp(k * Math.log(lambda) - lambda - logFact) / norm;

      // Sum of k lognormal ion signals ~ one lognormal with the same mean and variance.
      // (Fenton-Wilkinson approximation. Several ions average out, so the curve gets narrower.)
      double sk2 = Math.log1p(expS2m1 / k);
      s[k] = Math.sqrt(sk2);
      m[k] = Math.log(k) + mu + s2 / 2.0 - sk2 / 2.0;

      // Share of the k-ion curve above the cut-off (area under a bell curve beyond logT),
      // weighted by how common k-ion slices are.
      surv += w[k] * 0.5 * Erf.erfc((logT - m[k]) / (s[k] * Math.sqrt(2)));
    }
    if (!(surv > 0)) {
      return 1e300;   // model puts essentially nothing above the cut-off: reject these parameters
    }

    // Each observed value is only possible above the cut-off, so every probability is divided
    // by surv. In log form that is the "+ n * ln(surv)" below.
    double nll = logY.length * Math.log(surv);

    for (double ly : logY) {
      // Probability density of this observed signal: sum over "was it 1 ion? 2 ions? ..."
      // of (weight) x (bell curve for k ions evaluated at this signal).
      // Constant factors are dropped, because they do not change which mu/sigma is best.
      double dens = 0;
      for (int k = 1; k <= nMax; k++) {
        double z = (ly - m[k]) / s[k];                      // distance from the centre, in widths
        dens += w[k] * Math.exp(-0.5 * z * z) / s[k];
      }
      // Using the log turns the product over all data points into a sum.
      // Negative sign: high probability = low score.
      nll -= Math.log(Math.max(dens, 1e-300));
    }
    return nll;
  }

  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ______________________________________ Helpers methods _________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________
  // ________________________________________________________________________________________________


  /**
   * Same iterative NP removal as filterDataIteratively, but it tracks the original indices
   * and returns a keep-mask over the input array (true = background, false = event).
   */
  public static boolean[] createKeepMaskIteratively(double[] target, double alpha, double increasedAlpha,
                                                    double meanCriterion) {

    // prepare keep mask
    int n = target.length;
    boolean[] isBGMask = new boolean[n];
    Arrays.fill(isBGMask, true);

    if (n > 0) {

      // work on copy of the og data
      double[] bgData = ArrUtils.copy(target);

      // Fine-tune alpha: if more than 0.001 * length is lost in a first pass, NPs are likely present
      double npCandidateDPs = findEventIndices(target, alpha, meanCriterion).size();
      if (npCandidateDPs / n > 0.001) {
        alpha = increasedAlpha;
      }

      // original index of each element still in `data`
      int[] aliveOriginalIndices = new int[n];
      for (int i = 0; i < n; i++) {
        aliveOriginalIndices[i] = i;
      }


      // iterative check for outliers
      while (bgData.length > 0) {
        // identify NP events: break while look when no NPs are found anymore
        Set<Integer> events = findEventIndices(bgData, alpha, meanCriterion);
        if (events.isEmpty()) {
          break;
        }

        // find the remaining BG data --> = length of og data - #events
        int[] nextAliveBGIndices = new int[aliveOriginalIndices.length - events.size()];
        double[] nextBGData = new double[nextAliveBGIndices.length];
        int k = 0;
        for (int i = 0; i < aliveOriginalIndices.length; i++) {
          if (events.contains(i)) {
            // isBGMask: these are the event outliers so set them to false!
            isBGMask[aliveOriginalIndices[i]] = false;
          } else {
            // the index was not part of an event and thus is kept as BG
            nextAliveBGIndices[k] = aliveOriginalIndices[i];
            nextBGData[k] = bgData[i];
            k++;
          }
        }
        // update index mask and data
        aliveOriginalIndices = nextAliveBGIndices;
        bgData = nextBGData;
      }

    }
    return isBGMask;
  }

  /**
   * Indices (into targetData) that belong to NP events.
   */
  private static Set<Integer> findEventIndices(double[] targetData, double alpha, double meanCriterion) {

    // find percent of nonzeros
    double[] nonzeroTargetData = ArrUtils.positiveNonzero(targetData);
    double pNonzero = (double) nonzeroTargetData.length / targetData.length;

    // estimate mean via mode (will undershoot but is stable)
    double mode = Mode.calcNonzero(ArrUtils.round(targetData, 2));
    // corrects mode for being drawn from nonzero
    double corrMode = pNonzero * mode;

    // mean
    double mean = MeasureOfLocation.MEAN.calc(targetData);

    // if mean and mode are close, use the mean (it is likely the more accurate of the two),
    // else: use mode, because the mean is the one to deviate and then mode is better.
    double meanEstimator;
    if (Math.abs(mean - corrMode) / corrMode > meanCriterion) {
      meanEstimator = corrMode;
    } else {
      meanEstimator = mean;
    }

    double thr = Statistics.getCompoundThreshold(CompoundPoissonModel.LOOKUP_TABLE,
        meanEstimator,
        0.45, // default value that roughly seems to agree well with experimental data
        alpha);

    // This uses that events occur in peaks, and connected points are likely an event
    List<Integer> eventIndices = SearchUtils.splitEventSearch(targetData, meanEstimator, meanEstimator, thr);
    return new HashSet<>(eventIndices);
  }


  /**
   * Returns the elements of data where keep[i] == true.
   */
  private static double[] applyMask(double[] data, boolean[] keep) {
    int count = 0;
    for (boolean k : keep) {
      if (k) {
        count++;
      }
    }
    double[] result = new double[count];
    int idx = 0;
    for (int i = 0; i < data.length; i++) {
      if (keep[i]) {
        result[idx++] = data[i];
      }
    }
    return result;
  }
}
