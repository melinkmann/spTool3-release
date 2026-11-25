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

import sandbox.montecarlo.Statistics;
import util.ArrUtils;

import java.util.Random;

/**
 *
 * /// 100% Claude Sonnet 5.5 "High" with detailed instructions from me.
 * <p>
 * Faraday-cup detector model: expected ion counts in, measured counts out.
 *
 * <p>Two time scales are used (names as in the rest of the code):
 * <br>gridTime  = duration of one bin of gridSignal (e.g. 1 us)
 * <br>dwellTime = duration of one bin of signal (e.g. 100 us); dwellTime = binsPerDwell * gridTime
 *
 * <p><b>1) Where the Poisson noise is drawn.</b> At dwellTime (e.g. 100 us), not at gridTime
 * (e.g. 1 us): one draw per dwell per channel, on lambda_k = the sum of gridSignal inside dwell k.
 * A sum of independent Poisson draws is again Poisson, so this is statistically identical to
 * drawing at gridTime, with far fewer random numbers. What must NOT be done is drawing Poisson
 * after the filter. The shot noise passes through the amplifier like the signal does, so it has to
 * be filtered too. Poisson on the filtered mean gives white noise that is far too large (by a
 * factor of about sqrt(2*tau/dwellTime)) and not correlated between dwells.
 *
 * <p><b>2) How gridTime is used.</b> Only for the mean. gridSignal is filtered at gridTime (so the
 * shape of e.g. a nanoparticle peak is resolved exactly) and then summed into dwell bins. The noise
 * is generated and filtered at dwellTime only. The filter is linear, so
 * filter(N) = filter(lambda) + filter(N - lambda): mean at gridTime plus filtered noise at
 * dwellTime. The only approximation is that, for the noise, ions are treated as spread evenly
 * within one dwell.
 *
 * <p><b>3) How counts come out of the capacitor math.</b> An ion deposits charge e on the feedback
 * capacitor Cf, so the output voltage jumps by e/Cf and then decays through Rf with
 * tau = Rf*Cf:  V(t) = sum_i (e/Cf) * exp(-(t - t_i)/tau). Dividing by Rf*e gives an ion rate in
 * ions/s:  V(t)/(Rf*e) = sum_i (1/tau) * exp(-(t - t_i)/tau). The output "counts per dwell" is the
 * integral of that over the dwell. Each ion is spread over the following dwells and its weights
 * add up to exactly 1, so no ions are gained or lost. Rf and e cancel in this normalisation, which
 * is why the model takes tau directly and uses Rf only to size the Johnson noise. No volts appear
 * anywhere; the output has the same unit as the input.
 *
 * <p><b>4) Two (or more) exponentials.</b> A real resistor shows a fast decay (about Rf*Cf) plus a
 * small, slow tail. This is modelled as parallel single-pole filters: taus[i] with weights[i],
 * weights summing to 1 so the DC gain stays 1. Any number of components works. Fit taus and weights
 * to measured decay curves.
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * README:
 * 1) Why the Johnson noise goes in before the filter. Johnson noise acts like a tiny random current source
 * in parallel with the feedback resistor. It enters the same node as the ions, so it passes through the
 * same RC network and gets the same tail. If it were added after the filter, it would be uncorrelated from
 * dwell to dwell. Real data show a correlated wobble instead. Noise that enters after the amplifier, such
 * as ADC noise, is not modelled.
 * <p>
 * 2) What the “filter” does. It is the amplifier’s memory. After an ion arrives, the output doesn’t drop
 * back immediately but decays exponentially with time constant τ. So the output at any moment is a
 * weighted sum of all past arrivals, each weighted by exp(−Δt/τ). Signal processing calls any such
 * “weighted sum of the past” a filter. This one is a low-pass filter, the same idea as an exponential
 * moving average. The name is accurate but opaque. applyRingdown or amplifierResponse would say more, and
 * I’d rename it.
 * <p>
 * 3) The grouping. My loop duplicates your groupSignal. I did it inline because I need two sums, one
 * filtered and one unfiltered. We can call groupSignal twice and delete my loop. The blocks are identical
 * for block sizes of 2 or more, as I checked earlier, so blockSize would come from a single source.
 * <p>
 * 4) The resampled array.
 * <p>
 * It starts as lam, the expected ions per dwell.
 * The Poisson draw turns it into realized integer counts N.
 * I subtract lam, which leaves the zero-mean shot noise N − λ, and add the Johnson noise.
 * <p>
 * So the array holds the noise entering the amplifier, per dwell. I agree that dev is a bad name. I’d use
 * expectedPerDwell for lam, inputNoise for dev and filteredNoise for noise. The subtraction is needed
 * because the mean was already filtered separately in step 1.
 * <p>
 * 5) Why the filter runs twice. It is the same filter applied to two parts of the input. The expected
 * signal has detail inside a dwell, such as a nanoparticle peak, so it is filtered at 1 µs. The noise
 * exists only at dwell resolution, because we draw Poisson once per dwell, so it is filtered at dwell
 * resolution. The filter is linear, so the two outputs add up to the output for the full realized input.
 * This split is a workaround for not drawing at 1 µs, and it is the convoluted part you objected to earlier.
 */
public final class FaradayCupModel {
  private static final double E = 1.602176634e-19;   // C
  private static final double KB = 1.380649e-23;     // J/K

  /**
   * Turns the expected ion counts of one Faraday channel into the counts this channel would
   * measure: amplifier ringdown, shot noise and Johnson noise included.
   *
   * <p>Local variables:
   * <br>n            number of dwells in the output (gridSignal.length / binsPerDwell)
   * <br>dwellTime    dwell time in s (binsPerDwell * gridTime)
   * <br>sigmaJ       Johnson noise per dwell in counts (rms), added before the filter
   * <br>gridFiltered gridSignal after the filter, still at gridTime resolution (counts per grid bin)
   * <br>signal       first the filtered mean per dwell, at the end mean + filtered noise
   * (= return value)
   * <br>lam          unfiltered expected ions per dwell, i.e. the mean of the Poisson draw
   * <br>dev          zero-mean noise per dwell before filtering: Poisson draw - lam + Johnson noise
   * <br>noise        dev after the same filter as the signal
   *
   * @param gridSignal   expected ions per grid bin (gridTime resolution, e.g. 1 us); length =
   *                     number of dwells * binsPerDwell. Plain expected values (fractions are
   *                     fine), NOT Poisson-resampled. Not modified. The array should start on
   *                     baseline, not inside a particle event: the amplifier is assumed to have
   *                     been sitting at the level gridSignal[0] before the array begins.
   * @param binsPerDwell grid bins per dwell, e.g. dwellTime 100 us / gridTime 1 us = 100.
   *                     Trailing grid bins that do not fill a whole dwell are ignored.
   * @param gridTime     duration of one grid bin in s, e.g. 1e-6
   * @param rf           feedback resistor in ohm, e.g. 1e11 or 1e12. Only sets the size of the
   *                     Johnson noise (larger Rf = less noise in counts). The decay speed comes
   *                     from taus.
   * @param taus         decay time constants in s, one per exponential component, e.g.
   *                     {0.08, 1.5}. The fast one is roughly Rf*Cf. Fit to measured decay curves.
   * @param weights      share of each component, same length as taus, summing to 1, e.g.
   *                     {0.97, 0.03}
   * @param tempK        resistor temperature in K, e.g. 300. Only used for the Johnson noise;
   *                     0 = off
   * @return signal: counts per dwell, length = gridSignal.length / binsPerDwell, same unit as
   * gridSignal summed per dwell. Real numbers, not integers; can be slightly negative
   * because of Johnson noise.
   */
  public static double[] applyFromExpected(double[] gridSignal, int binsPerDwell, double gridTime, double rf,
                                           double[] taus, double[] weights, double tempK) {
    Random rng = Statistics.getXoroRandom();
    return applyFromExpected(gridSignal, binsPerDwell, gridTime, rf, taus, weights, tempK, rng);
  }

  public static double[] applyFromExpected(double[] gridSignal, int binsPerDwell, double gridTime, double rf,
                                           double[] taus, double[] weights, double tempK, Random rng) {
    int n = gridSignal.length / binsPerDwell;
    double dwellTime = binsPerDwell * gridTime;
    // Johnson noise in counts per dwell (input-referred, white); enters before the filter
    double sigmaJ = Math.sqrt(4 * KB * tempK / rf / (2 * dwellTime)) * dwellTime / E;

    // (2) mean: exact response at gridTime, summed to dwell bins
    double[] gridFiltered = applyRingdown(gridSignal, gridTime, taus, weights, gridSignal[0]);
    double[] signal = new double[n];   // mean, later mean + noise
    double[] lam = new double[n];      // expected ions per dwell, unfiltered
    for (int k = 0; k < n; k++) {
      double s = 0, l = 0;
      for (int j = k * binsPerDwell; j < (k + 1) * binsPerDwell; j++) {
        l += gridSignal[j];
        s += gridFiltered[j];
      }
      signal[k] = s;
      lam[k] = l;
    }

    // (1) one Poisson draw per dwell; keep only the zero-mean deviation, plus Johnson noise
    double[] dev = new double[n];
    System.arraycopy(lam, 0, dev, 0, n);
    Statistics.resamplePoissonDynamically(dev);                 // dev[k] = N_k ~ Poisson(lam[k])
    for (int k = 0; k < n; k++) dev[k] += -lam[k] + sigmaJ * rng.nextGaussian();

    // (2) the noise goes through the same filter at dwellTime, then is added to the mean
    double[] noise = applyRingdown(dev, dwellTime, taus, weights, 0.0);
    for (int k = 0; k < n; k++) signal[k] += noise[k];

    // spurious negative values
    ArrUtils.replaceNegativeWithZero(signal);
    return signal;
  }

  /**
   * The amplifier ringdown as a linear filter (see points 3 and 4 in the class comment). It takes
   * a series of counts per bin and returns what the amplifier output, expressed in counts, would
   * read per bin: every ion is smeared over the following bins with an exponential decay
   * (several exponentials with the given weights), so a sharp peak turns into a peak with a
   * long tail. Total counts are conserved; only their timing is smeared.
   *
   * <p>Why "linear": the feedback network (Rf in parallel with Cf) is a passive RC element, and
   * its output obeys dV/gridDT = -V/tau + I/Cf, an equation that is linear in V and I. Linear means
   * two things: (a) scaling: twice the input gives exactly twice the output; (b) superposition:
   * the output for a sum of inputs is the sum of the outputs for each input alone. Physically,
   * each ion leaves its own decaying tail, independent of all other ions, and the measured signal
   * is just all these tails added up. The code relies on this in three places:
   * <br>- one ion, a nanoparticle peak and the background all use the same filter, with no
   * dependence on signal level;
   * <br>- the signal can be split into mean and noise, filter(N) = filter(lambda) +
   * filter(N - lambda), which is why applyFromExpected filters the mean at gridTime and the
   * noise at dwellTime separately;
   * <br>- several exponentials can be run in parallel and simply added (weights summing to 1).
   * The model assumes the decay does not change with the signal. It would stop being valid if the
   * amplifier saturates, or if Rf or Cf depend on the voltage (e.g. dielectric absorption that
   * grows with signal). A nonlinear tail cannot be described by taus and weights alone.
   *
   * <p>Time scale: the function works on whatever bin length gridDT it is given and knows nothing
   * about gridTime or dwellTime. applyFromExpected calls it twice:
   * <br>- on gridSignal (gridDT = gridTime, e.g. 1 us) for the expected signal, so that structure
   * inside a dwell, such as a nanoparticle peak, is resolved;
   * <br>- on the per-dwell noise (gridDT = dwellTime, e.g. 100 us).
   * Inside one bin the input is taken as constant (ions spread evenly over the bin). For that
   * input the response is calculated exactly (no sub-stepping): the state is carried from bin to
   * bin and the returned value is the average of the output over the bin, which is what a
   * detector integrating over that bin would report. The result is therefore only as detailed in
   * time as the bins you pass in. It is accurate when the bins are short compared with the
   * structure in signal or with the shortest tau.
   *
   * <p>Local variables:
   * <br>a   fraction of the state left after one bin, exp(-gridDT/tau)
   * <br>g   factor that turns "state at start of bin minus input" into the bin average,
   * (tau/gridDT)*(1-a)
   * <br>y   state of the amplifier output at the start of the current bin
   * <br>d   y - signal[i]: how far the state is from the input in this bin
   * <br>out summed over the components; out[i] is the bin average of the output
   *
   * @param signal  input per bin (counts per bin)
   * @param gridDT  bin duration in s (gridTime for the mean, dwellTime for the noise)
   * @param taus    decay time constants in s, one per component
   * @param weights share of each component, summing to 1
   * @param y0      where the amplifier output sits just before the array begins, in the same
   *                unit as signal (counts per bin). For the mean this is gridSignal[0], i.e. the
   *                baseline (background) level, assuming the record starts between particle
   *                events. For the noise it is 0: no noise memory before the start.
   * @return filtered counts per bin, same unit as signal
   */
  private static double[] applyRingdown(double[] signal, double gridDT, double[] taus, double[] weights,
                                        double y0) {
    double[] ringingSignal = new double[signal.length];
    // for each exponential distribution, iterate: they add linearly
    for (int k = 0; k < taus.length; k++) {
      double a = Math.exp(-gridDT / taus[k]);          // decay over one bin
      double g = (taus[k] / gridDT) * (1 - a);         // bin average of the decaying state
      double y = y0;                               // state at start of bin
      // apply the current exponential to each data point
      for (int i = 0; i < signal.length; i++) {
        double d = y - signal[i];
        ringingSignal[i] += weights[k] * (signal[i] + d * g);   // bin average
        y = signal[i] + d * a;                        // state at end of bin
      }
    }
    return ringingSignal;
  }
}