package io.nu;

import core.SpTool3Main;

import java.util.Arrays;
import java.util.List;

/**
 * Claude sonnet 4.6 sketch based on my instructions.
 * <p>
 * Estimates the lognormal shape parameter (sigma) of the Single Ion Area (SIA)
 * distribution from raw ICP-TOFMS intensity data.
 * <p>
 * ═══════════════════════════════════════════════════════════════════════════════
 * PHYSICAL BACKGROUND
 * ═══════════════════════════════════════════════════════════════════════════════
 * In ICP-TOFMS, the detector (typically a micro-channel plate) does not count
 * discrete ions like a quadrupole. Instead, each ion that strikes the detector
 * produces an analogue pulse whose area (Single Ion Area, SIA) is drawn from a
 * continuous lognormal distribution.
 * <p>
 * Because spectra are binned (typically 3+ raw spectra per saved data point),
 * each saved intensity value is the SUM of the signals from however many ions
 * arrived during that bin. The number of arriving ions k follows a Poisson
 * distribution with rate lambda (the mean ionic background). This two-level
 * random process is called a COMPOUND POISSON process:
 * <p>
 * k   ~ Poisson(lambda)                 ... how many ions arrived
 * Y   = SIA_1 + SIA_2 + ... + SIA_k    ... total signal for that bin
 * SIA ~ LogNormal(mu, sigma^2)          ... each ion's individual contribution
 * Y   = 0 when k = 0
 * <p>
 * sigma is the key parameter for compound Poisson detection thresholds per
 * Lockwood, Schlatt & Clases, J. Anal. At. Spectrom., 2025, 40, 130-136.
 * <p>
 * ═══════════════════════════════════════════════════════════════════════════════
 * TWO COMPLEMENTARY ESTIMATION METHODS
 * ═══════════════════════════════════════════════════════════════════════════════
 * <p>
 * METHOD A — Percentile window + sigma-clipping (reliable for mid-range lambda)
 * ─────────────────────────────────────────────────────────────────────────────
 * Under the compound Poisson model, the CDF of Y is a mixture:
 * F_Y(y) = P(k=0) [zero mass] + P(k=1)*F_LN(y) + P(k=2)*F_LN2(y) + ...
 * <p>
 * Therefore the k=1 (single-ion) events occupy a contiguous band in the sorted
 * distribution between global quantiles P(k=0) and P(k=0)+P(k=1). We extract
 * values in that band and fit a lognormal by MLE with iterative sigma-clipping
 * to remove the small fraction of leaked k>=2 events.
 * <p>
 * Limitation: at extreme lambda this degrades:
 * - Very low lambda (< 0.1): the k=1 band is extremely narrow; nanoparticle
 * spikes distort the empirical percentile boundaries → sigma overestimated.
 * - Very high lambda (> 3.0): the k=2 lower tail heavily overlaps the k=1
 * band; clipping removes too much genuine right tail → sigma underestimated.
 * <p>
 * METHOD B — Variance moment matching (reliable for all lambda)
 * ─────────────────────────────────────────────────────────────────────────────
 * For a compound Poisson process, the mean and variance of Y are:
 * E[Y]   = lambda * exp(mu + sigma^2/2)
 * Var(Y) = lambda * exp(2*mu + 2*sigma^2)
 * <p>
 * Dividing to eliminate the unknown mu:
 * Var(Y) * lambda / E[Y]^2 = exp(sigma^2)
 * sigma = sqrt( ln( Var(Y) * lambda / E[Y]^2 ) )
 * <p>
 * This is mu-free and correct regardless of detector gain or m/z. The naive
 * formula sigma^2 = ln(Var/lambda) incorrectly assumes mean(SIA) = 1 in raw
 * ADC units, causing systematic underestimation — this formula avoids that.
 * <p>
 * Nanoparticle spikes inflate Var(Y), so they are removed first by iterative
 * trimming at mean + TRIM_K * std on non-zero values. Zeros are kept throughout
 * because they are genuine k=0 outcomes of the Poisson process.
 * <p>
 * COMBINATION STRATEGY
 * ─────────────────────────────────────────────────────────────────────────────
 * Both estimates are computed and combined based on lambda:
 * lambda < LAMBDA_LO or > LAMBDA_HI  →  Method B only
 * lambda in [LAMBDA_LO, LAMBDA_HI]   →  smooth blend (cubic Hermite),
 * A dominates at centre, B at edges
 * <p>
 * ═══════════════════════════════════════════════════════════════════════════════
 * BLANKER REGIONS
 * ═══════════════════════════════════════════════════════════════════════════════
 * Some acquisition sequences include "blanker" periods where the ion beam is
 * physically blocked. During these periods lambda is very low, so nearly all
 * non-zero bins are k=1 events — ideal for SIA estimation.
 * If >50 non-zero points exist in the blanker region, use it exclusively.
 * Otherwise exclude it and use the rest (mixing lambda regimes corrupts both
 * the percentile calculation and the variance estimate).
 */
public class ShapeEstimator2 {

  private static final int NONZERO_BLANKER_THRESHOLD = 50;
  // private static final double CLIP_SIGMA                = 5;    // now read from config
  private static final int MAX_ITER = 20;
  private static final int MIN_SURVIVORS = 5;

  // Lambda range where Method A (percentile window) is reliable.
  // Below 0.1 the k=1 window is too narrow and particle spikes corrupt it.
  // Above 3.0 the k=2 overlap causes clipping to remove too much genuine right tail.
  private static final double LAMBDA_LO = 0.1;
  private static final double LAMBDA_HI = 3.0;

  // Spike-removal threshold for Method B.
  // Non-zero values above (mean + TRIM_K * std) are treated as particle events.
  // 5.0 is conservative: removes only clear spikes, leaves the background tail intact.
  private static final double TRIM_K = 5.0;
  private static final int TRIM_ITER = 20;

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
        // Its lower lambda would corrupt the percentile and variance calculations.
        return fitShape(extractSubset(intensity, inBlanker, false));
      }
    }

    return fitShape(intensity);
  }

  // ── Core fitting routine ──────────────────────────────────────────────────

  private static double fitShape(double[] data) {

    // ── Step 1: Count zeros ───────────────────────────────────────────────
    // Exact zeros = bins where k=0 ions arrived. We need enough of them to estimate
    // lambda reliably. Fewer than ~50 gives an unstable log estimate.
    int nTotal = data.length;
    int nZeros = 0;
    for (double v : data) if (v == 0.0) nZeros++;

    if (nZeros <= 50) return Double.NaN;   // need zeros to estimate lambda

    // ── Step 2: Estimate lambda from the zero fraction ────────────────────
    // P(k=0) = exp(-lambda)  →  lambda = -ln(nZeros / nTotal)
    // This is the MLE of lambda using only the zero-count observations.
    // It reflects the mean ionic BACKGROUND rate, not any particle signal.
    double pZero = (double) nZeros / nTotal;
    double lambda = -Math.log(pZero);

    // ── Step 3: Run both methods and combine based on lambda regime ───────
    // Each method has a different reliability profile (see class Javadoc).
    // Computing both and blending gives the best result across all lambda values.
    double sigmaA = estimateByPercentileWindow(data, lambda, pZero);
    double sigmaB = estimateByVarianceMoment(data, lambda);

    boolean aValid = Double.isFinite(sigmaA) && sigmaA > 0;
    boolean bValid = Double.isFinite(sigmaB) && sigmaB > 0;

    if (!aValid && !bValid) return Double.NaN;
    if (!aValid) return sigmaB;
    if (!bValid) return sigmaA;

    if (lambda < LAMBDA_LO || lambda > LAMBDA_HI) {
      // Outside Method A's reliable range → use B only
      return sigmaB;
    }

    // Inside reliable range: smooth blend, A dominates at centre, B at edges.
    // Cubic Hermite weight ensures no discontinuity at the boundaries.
    double wA = blendWeight(lambda, LAMBDA_LO, LAMBDA_HI);
    return wA * sigmaA + (1.0 - wA) * sigmaB;
  }

  // ── Method A: Percentile window + iterative sigma-clipping MLE ───────────

  /**
   * Locates the k=1 band in the sorted non-zero distribution using Poisson
   * probabilities, then fits LogNormal(mu, sigma^2) by free-mu MLE with
   * sigma-clipping to remove leaked k>=2 events.
   * <p>
   * The k=1 band occupies the global CDF interval [P(k=0), P(k=0)+P(k=1)].
   * CLIP_SIGMA is read from config (typically 3.5, not 2.5): the percentile
   * window already handles most multi-ion contamination, so tighter clipping
   * truncates the genuine lognormal right tail and underestimates sigma.
   * <p>
   * mu is left free (not fixed to ln(1)=0 or any other value) because fixing
   * mu to the wrong value adds (mu_true - mu_fixed)^2 as a squared bias to
   * sigma^2. mu is a nuisance parameter; only sigma is returned.
   */
  private static double estimateByPercentileWindow(double[] data, double lambda, double pZero) {

    // clipSigma is configurable so it can be tuned per instrument without recompiling
    final double clipSigma = SpTool3Main.getRunTime().getConfParams().getSiaSigmaClip().getValue();

    // CDF band for exactly k=1 events:
    //   lower boundary = P(k=0)            = pZero
    //   upper boundary = P(k=0) + P(k=1)  = pZero + lambda*exp(-lambda)
    double p1 = lambda * Math.exp(-lambda);
    double cdfLo = pZero;
    double cdfHi = pZero + p1;

    // Sort non-zero values — they span the global CDF from pZero to 1.0
    double[] nonZero = Arrays.stream(data).filter(v -> v > 0.0).sorted().toArray();
    int nNonZero = nonZero.length;
    if (nNonZero == 0) return Double.NaN;

    // Map the global CDF boundaries to index positions within the nonZero array.
    // A global quantile q maps to: frac = (q - pZero) / (1 - pZero), idx = frac * nNonZero
    double pNonZero = 1.0 - pZero;
    int idxLo = Math.max(0, (int) Math.floor(((cdfLo - pZero) / pNonZero) * nNonZero));
    int idxHi = Math.min(nNonZero - 1, (int) Math.ceil(((cdfHi - pZero) / pNonZero) * nNonZero));
    if (idxLo >= idxHi) return Double.NaN;

    // Extract the single-ion window by amplitude thresholds at the boundary indices.
    // This window is mostly k=1 events but has some k>=2 leakage at the upper end,
    // which the sigma-clipping below removes.
    final double threshLo = nonZero[idxLo];
    final double threshHi = nonZero[idxHi];
    double[] singleIon = Arrays.stream(nonZero)
        .filter(v -> v >= threshLo && v <= threshHi).toArray();
    if (singleIon.length < MIN_SURVIVORS) return Double.NaN;

    // Work in log-space: if x ~ LogNormal(mu, sigma^2) then ln(x) ~ Normal(mu, sigma^2)
    double[] logVals = Arrays.stream(singleIon).filter(v -> v > 0.0).map(Math::log).toArray();
    if (logVals.length < MIN_SURVIVORS) return Double.NaN;

    boolean[] keep = new boolean[logVals.length];
    Arrays.fill(keep, true);
    double shape = Double.NaN;

    for (int iter = 0; iter < MAX_ITER; iter++) {
      // MLE mu_hat = mean of log-values over currently kept points
      double sum = 0.0;
      int n = 0;
      for (int i = 0; i < logVals.length; i++)
        if (keep[i]) {
          sum += logVals[i];
          n++;
        }
      if (n < MIN_SURVIVORS) return Double.NaN;
      double mu = sum / n;

      // MLE sigma_hat = RMS deviation from mu over kept points
      double sumSq = 0.0;
      for (int i = 0; i < logVals.length; i++)
        if (keep[i]) sumSq += (logVals[i] - mu) * (logVals[i] - mu);
      double sigma = Math.sqrt(sumSq / n);
      double threshold = clipSigma * sigma;

      // Reject outliers: points more than clipSigma standard deviations from mu.
      // These are predominantly leaked k>=2 multi-ion events sitting in the
      // upper tail of the window. Iterate until membership is stable.
      boolean changed = false;
      for (int i = 0; i < logVals.length; i++) {
        boolean ok = Math.abs(logVals[i] - mu) <= threshold;
        if (keep[i] != ok) {
          keep[i] = ok;
          changed = true;
        }
      }
      shape = sigma;
      if (!changed) break;   // converged
    }
    return shape;
  }

  // ── Method B: Variance moment matching ───────────────────────────────────

  /**
   * Estimates sigma from the variance of the spike-trimmed background data.
   * <p>
   * For a compound Poisson process:
   * E[Y]   = lambda * exp(mu + sigma^2/2)
   * Var(Y) = lambda * exp(2*mu + 2*sigma^2)
   * <p>
   * Dividing the variance by the square of the mean eliminates the unknown mu:
   * Var(Y) * lambda / E[Y]^2 = exp(sigma^2)
   * sigma = sqrt( ln( Var(Y) * lambda / E[Y]^2 ) )
   * <p>
   * Important: the naive formula sigma^2 = ln(Var(Y)/lambda) incorrectly
   * assumes mean(SIA) = 1 in raw ADC units. That is only a normalisation
   * convention used for thresholding — not a physical fact. The actual SIA
   * mean depends on detector gain and m/z, so using the naive formula causes
   * systematic underestimation. This formula is mu-free and always correct.
   * <p>
   * Zeros are kept in the variance computation because they are genuine k=0
   * observations that contribute to Var(Y) under the compound Poisson model.
   * Nanoparticle spikes are removed first (they would massively inflate Var(Y)).
   */
  private static double estimateByVarianceMoment(double[] data, double lambda) {

    // ── Spike removal ─────────────────────────────────────────────────────
    // Iteratively trim non-zero values above (mean + TRIM_K * std).
    // Particle events sit far above the background mean, so TRIM_K=5 reliably
    // excludes them without biting into the background distribution tail.
    // Zeros are never flagged as spikes — they belong to the background.
    boolean[] isSpike = new boolean[data.length];

    for (int iter = 0; iter < TRIM_ITER; iter++) {
      double sum = 0.0, sumSq = 0.0;
      int n = 0;
      for (int i = 0; i < data.length; i++) {
        if (!isSpike[i] && data[i] > 0.0) {
          sum += data[i];
          sumSq += data[i] * data[i];
          n++;
        }
      }
      if (n < MIN_SURVIVORS) return Double.NaN;
      double mean = sum / n;
      double std = Math.sqrt(sumSq / n - mean * mean);
      double cutoff = mean + TRIM_K * std;

      boolean changed = false;
      for (int i = 0; i < data.length; i++) {
        if (!isSpike[i] && data[i] > cutoff) {
          isSpike[i] = true;
          changed = true;
        }
      }
      if (!changed) break;   // spike set is stable
    }

    // ── Compute mean and variance over ALL non-spike data (zeros included) ─
    double sum = 0.0, sumSq = 0.0;
    int n = 0;
    for (int i = 0; i < data.length; i++) {
      if (!isSpike[i]) {
        sum += data[i];
        sumSq += data[i] * data[i];
        n++;
      }
    }
    if (n < MIN_SURVIVORS) return Double.NaN;

    double mean = sum / n;
    double varY = sumSq / n - mean * mean;   // E[Y^2] - E[Y]^2

    // ── Moment-matching formula ───────────────────────────────────────────
    // ratio = Var(Y) * lambda / E[Y]^2 = exp(sigma^2)
    // Must be > 1 for sigma^2 > 0. If <= 1 the data are under-dispersed
    // relative to a compound Poisson — numerical issue or too few spikes removed.
    double ratio = varY * lambda / (mean * mean);
    if (ratio <= 1.0) return Double.NaN;

    return Math.sqrt(Math.log(ratio));
  }

  // ── Blending ──────────────────────────────────────────────────────────────

  /**
   * Smooth weight for Method A in [lo, hi].
   * Returns 1.0 at the centre (Method A fully trusted), tapering to 0.0 at
   * the boundaries via the cubic Hermite polynomial 1 - (3t^2 - 2t^3).
   * This prevents a hard discontinuity when lambda crosses LAMBDA_LO or LAMBDA_HI.
   */
  private static double blendWeight(double lambda, double lo, double hi) {
    double mid = 0.5 * (lo + hi);
    double half = 0.5 * (hi - lo);
    double t = Math.max(0.0, Math.min(1.0, Math.abs(lambda - mid) / half));
    return 1.0 - (3.0 * t * t - 2.0 * t * t * t);
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