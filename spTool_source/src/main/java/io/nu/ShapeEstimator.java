package io.nu;

import core.SpTool3Main;

import java.util.Arrays;
import java.util.List;

/**
 * Estimates the lognormal shape parameter (sigma) of the Single Ion Area (SIA)
 * distribution from raw ICP-TOFMS intensity data.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * PHYSICAL BACKGROUND
 * ═══════════════════════════════════════════════════════════════════════════════
 * In ICP-TOFMS, the detector (typically a micro-channel plate) does not count
 * discrete ions like a quadrupole. Instead, each ion that strikes the detector
 * produces an analogue pulse whose area (Single Ion Area, SIA) is drawn from a
 * continuous lognormal distribution.
 *
 * Because spectra are binned (typically 3+ raw spectra per saved data point),
 * each saved intensity value is the SUM of the signals from however many ions
 * arrived during that bin. The number of arriving ions k follows a Poisson
 * distribution with rate lambda (the mean ionic background). This two-level
 * random process is called a COMPOUND POISSON process:
 *
 *   k   ~ Poisson(lambda)                 ... how many ions arrived
 *   Y   = SIA_1 + SIA_2 + ... + SIA_k    ... total signal for that bin
 *   SIA ~ LogNormal(mu, sigma^2)          ... each ion's individual contribution
 *   Y   = 0 when k = 0
 *
 * sigma is the key parameter for compound Poisson detection thresholds per
 *   Lockwood, Schlatt & Clases, J. Anal. At. Spectrom., 2025, 40, 130-136.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * ESTIMATION STRATEGY — PERCENTILE WINDOW + SIGMA-CLIPPING
 * ═══════════════════════════════════════════════════════════════════════════════
 * We cannot directly observe individual SIA values in normal sample data, because
 * most bins contain contributions from 0, 1, 2, ... superimposed ions. However,
 * bins that received EXACTLY ONE ion (k=1) have Y = SIA_1, so their distribution
 * IS the SIA lognormal we want to fit.
 *
 * Under the compound Poisson model, the CDF of Y is a mixture:
 *   F_Y(y) = P(k=0) [zero mass] + P(k=1)*F_LN(y) + P(k=2)*F_LN2(y) + ...
 *
 * Therefore the k=1 events occupy a contiguous band in the globally sorted
 * distribution between quantiles P(k=0) and P(k=0)+P(k=1). We extract the
 * values in that band and fit a lognormal to them by MLE with iterative
 * sigma-clipping to remove the small fraction of leaked k>=2 events.
 *
 * Known limitations:
 *   - Very low lambda (< ~0.1): the k=1 band is extremely narrow; nanoparticle
 *     spikes distort the empirical percentile boundaries → sigma overestimated.
 *   - Very high lambda (> ~3.0): the k=2 lower tail overlaps the k=1 band;
 *     sigma-clipping removes too much genuine right tail → sigma underestimated.
 *   - clipSigma must be set generously (see config): the percentile window
 *     already handles most multi-ion contamination, so over-clipping truncates
 *     the genuine lognormal right tail and causes systematic underestimation.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * BLANKER REGIONS
 * ═══════════════════════════════════════════════════════════════════════════════
 * Some acquisition sequences include "blanker" periods where the ion beam is
 * physically blocked. During these periods lambda is very low, so nearly all
 * non-zero bins are k=1 events — ideal for SIA estimation.
 * If >50 non-zero points exist in the blanker region, use it exclusively.
 * Otherwise exclude it and use the rest (mixing lambda regimes would corrupt
 * the percentile boundaries and the lambda estimate).
 */
public class ShapeEstimator {

  private static final int NONZERO_BLANKER_THRESHOLD = 50;
  // clipSigma is intentionally NOT hardcoded here — it is read from config at
  // runtime (see fitShape). A value around 3.5-5.0 is recommended: lower values
  // (e.g. 2.5) clip the genuine lognormal right tail and cause underestimation.
  // private static final double CLIP_SIGMA = 5; //2.5 //3.5 seems OKish, still a bit too low
  private static final int MAX_ITER = 20;
  private static final int MIN_SURVIVORS = 5;

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Convenience overload – no blanker information available.
   */
  public static double computeShape(double[] intensity) {
    return computeShape(intensity, null);
  }

  /**
   * Estimates the lognormal shape parameter sigma.
   *
   * @param intensity     full intensity array (zeros allowed)
   * @param blankedRanges list of [startInclusive, endInclusive] index pairs marking
   *                      blanker regions; may be null or empty
   * @return sigma > 0, or NaN if estimation fails
   */
  public static double computeShape(double[] intensity, List<int[]> blankedRanges) {

    boolean hasBlanker = blankedRanges != null && !blankedRanges.isEmpty();

    if (hasBlanker) {
      boolean[] inBlanker = buildBlankerMask(intensity.length, blankedRanges);
      double[] blankerData = extractSubset(intensity, inBlanker, true);

      if (countNonZero(blankerData) > NONZERO_BLANKER_THRESHOLD) {
        // Blanker region is signal-rich – use it exclusively.
        // Low lambda during blanking → near-pure k=1 events → cleanest SIA window.
        return fitShape(blankerData);
      } else {
        // Blanker region is sparse – exclude it and use the rest.
        // Its different (lower) lambda would corrupt the percentile calculation.
        return fitShape(extractSubset(intensity, inBlanker, false));
      }
    }

    return fitShape(intensity);
  }

  // ── Core fitting routine ──────────────────────────────────────────────────

  private static double fitShape(double[] data) {

    // clipSigma controls how aggressively outliers are rejected in Step 5.
    // It is read from config so it can be tuned per instrument without recompiling.
    // Higher values (less clipping) are better: the percentile window (Step 4)
    // already removes most multi-ion contamination, so tight clipping only
    // truncates the genuine lognormal right tail and underestimates sigma.
    final double clipSigma = SpTool3Main.getRunTime().getConfParams().getSiaSigmaClip().getValue();

    // ── Step 1: Count zeros ───────────────────────────────────────────────
    // Exact zeros = bins where k=0 ions arrived (the beam was on but no ion
    // happened to land). We need enough of them to estimate lambda reliably
    // in Step 2. Fewer than ~50 gives an unstable logarithm.
    int nTotal = data.length;
    int nZeros = 0;
    for (double v : data) if (v == 0.0) nZeros++;

    if (nZeros <= 50) return Double.NaN;   // need zeros to estimate lambda

    // ── Step 2: Estimate lambda from the zero fraction ────────────────────
    // Under a Poisson process: P(k=0) = exp(-lambda)
    // Solving for lambda given the observed zero fraction:
    //   pZero = nZeros / nTotal  →  lambda = -ln(pZero)
    // This is the MLE of lambda from zero-count data. It reflects the mean
    // ionic BACKGROUND rate, not any particle signal.
    double pZero = (double) nZeros / nTotal;
    double lambda = -Math.log(pZero);

    // ── Step 3: Locate the k=1 percentile band ────────────────────────────
    // The CDF of the compound Poisson mixture over ALL data points is:
    //   F_Y(y) = P(k=0)                        [zero-mass spike at y=0]
    //          + P(k=1) * F_LN(y; mu, sigma^2) [single-ion component]
    //          + P(k=2) * F_LN2(y)             [two-ion component, shifted right]
    //          + ...
    //
    // The k=1 component occupies the global CDF interval:
    //   lower boundary = P(k=0)           = pZero      = cdfLo
    //   upper boundary = P(k=0) + P(k=1) = pZero + p1 = cdfHi
    //
    // where P(k=1) = lambda * exp(-lambda)
    double p1 = lambda * Math.exp(-lambda);
    double cdfLo = pZero;
    double cdfHi = pZero + p1;

    // Sort the strictly positive values — they span the global CDF from pZero to 1.0
    double[] nonZero = Arrays.stream(data)
        .filter(v -> v > 0.0)
        .sorted()
        .toArray();

    int nNonZero = nonZero.length;
    if (nNonZero == 0) return Double.NaN;

    // Map the global CDF boundaries to index positions within the nonZero array.
    // The non-zero values collectively span global CDF [pZero, 1.0], so a global
    // quantile q maps to fractional position (q - pZero) / (1 - pZero) in the array.
    double pNonZero = 1.0 - pZero;
    int idxLo = Math.max(0, (int) Math.floor(((cdfLo - pZero) / pNonZero) * nNonZero));
    int idxHi = Math.min(nNonZero - 1, (int) Math.ceil(((cdfHi - pZero) / pNonZero) * nNonZero));

    if (idxLo >= idxHi) return Double.NaN;

    // ── Step 4: Extract the single-ion window ─────────────────────────────
    // Use the intensity values at the boundary indices as amplitude thresholds.
    // Values in this window are predominantly k=1 single-ion events, but contain
    // some leakage from k>=2 events (whose lower tails overlap the k=1 region).
    // The sigma-clipping in Step 5 removes this leakage.
    final double threshLo = nonZero[idxLo];
    final double threshHi = nonZero[idxHi];

    double[] singleIon = Arrays.stream(nonZero)
        .filter(v -> v >= threshLo && v <= threshHi)
        .toArray();

    if (singleIon.length < MIN_SURVIVORS) return Double.NaN;

    // ── Step 5: Free-mu MLE with iterative sigma-clipping ─────────────────
    // MODEL: ln(x) ~ Normal(mu, sigma^2)  for x in the single-ion window.
    //
    // MLE (free mu, per iteration):
    //   mu_hat    = mean( ln(x_i) )
    //   sigma_hat = sqrt( mean[ (ln(x_i) - mu_hat)^2 ] )
    //
    // WHY FREE mu?
    //   Fixing mu to any wrong value adds (mu_true - mu_fixed)^2 as a squared
    //   bias to sigma_hat^2. The true SIA peak position varies with detector gain
    //   and m/z, so we estimate mu from the data. It is a nuisance parameter —
    //   only sigma is returned.
    //
    // SIGMA-CLIPPING:
    //   Each iteration removes points where |ln(x_i) - mu_hat| > clipSigma * sigma_hat.
    //   These are predominantly leaked k>=2 multi-ion events sitting in the upper
    //   tail of the window. The loop repeats until membership is stable (converged).
    double[] logVals = Arrays.stream(singleIon)
        .filter(v -> v > 0.0)
        .map(Math::log)
        .toArray();

    if (logVals.length < MIN_SURVIVORS) return Double.NaN;

    boolean[] keep = new boolean[logVals.length];
    Arrays.fill(keep, true);   // start with all points included
    double shape = Double.NaN;

    for (int iter = 0; iter < MAX_ITER; iter++) {

      // MLE mu_hat = mean of log-values over currently kept points
      double sum = 0.0;
      int n = 0;
      for (int i = 0; i < logVals.length; i++) {
        if (keep[i]) {
          sum += logVals[i];
          n++;
        }
      }
      if (n < MIN_SURVIVORS) return Double.NaN;
      double mu = sum / n;

      // MLE sigma_hat = RMS deviation from mu over kept points
      double sumSq = 0.0;
      for (int i = 0; i < logVals.length; i++) {
        if (keep[i]) {
          sumSq += (logVals[i] - mu) * (logVals[i] - mu);
        }
      }
      double sigma = Math.sqrt(sumSq / n);
      double threshold = clipSigma * sigma;
      boolean changed = false;

      // Reject outliers — points more than clipSigma standard deviations from mu
      for (int i = 0; i < logVals.length; i++) {
        boolean ok = Math.abs(logVals[i] - mu) <= threshold;
        if (keep[i] != ok) {
          keep[i] = ok;
          changed = true;
        }
      }

      shape = sigma;
      if (!changed) break;   // membership stable → converged
    }

    System.out.println("SHAPE: " + shape);
    return shape;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * True at every index that falls inside at least one blanker range.
   */
  private static boolean[] buildBlankerMask(int n, List<int[]> blankedRanges) {
    boolean[] mask = new boolean[n];
    for (int[] range : blankedRanges) {
      int lo = Math.max(0, range[0]);
      int hi = Math.min(n - 1, range[1]);
      for (int i = lo; i <= hi; i++) mask[i] = true;
    }
    return mask;
  }

  /**
   * Returns elements of intensity where inBlanker[i] == wantBlanker.
   */
  private static double[] extractSubset(double[] intensity, boolean[] inBlanker,
                                        boolean wantBlanker) {
    int count = 0;
    for (boolean b : inBlanker) if (b == wantBlanker) count++;
    double[] out = new double[count];
    int idx = 0;
    for (int i = 0; i < intensity.length; i++)
      if (inBlanker[i] == wantBlanker) out[idx++] = intensity[i];
    return out;
  }

  /**
   * Counts strictly positive values.
   */
  private static int countNonZero(double[] data) {
    int c = 0;
    for (double v : data) if (v > 0.0) c++;
    return c;
  }
}