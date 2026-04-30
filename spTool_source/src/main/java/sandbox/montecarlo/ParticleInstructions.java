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

import dataModelNew.mz.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import gui.dialog.notification.NotificationFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ArrUtils;
import visualizer.styles.Colors;
import visualizer.styles.MarkerStyle;

/**
 * Just plain variables with the relevant values from the Method. These should not include any
 * randomization, only the instructions how to derive things.
 */

public class ParticleInstructions {

  private static final Logger LOGGER = LogManager.getLogger(ParticleInstructions.class);

  // General particle population level
  private final List<RandomElementInstructions> randElements;

  private final String label;

  // for plotting
  private final Colors color;
  private final MarkerStyle marker;

  private final double lastTimeStampInMacroFrame;

  private final double microDT;
  private final double rate;

  private final CheckedDistribution massDistribution;
  private final double minimumPeakHeightExpDistr;
  private static final int SAFETY_COUNTER_LIMIT = (int) 1E6;

  // Ion cloud related
  private final CheckedDistribution plasma_velocity_distr;

  private final CheckedDistribution plasma_yPos_distr;

  // Specific
  private final double delayUncertaintyMicroseconds;

  private final double elementFractionUncertainty;

  private final double isotopeFractionUncertainty;

  // Specific for evenly spaced peaks
  private final boolean fixPeakDistance;
  private final double estimPeakWidth;


  public ParticleInstructions(
      String label,
      Colors color,
      MarkerStyle marker,
      double rate,
      boolean fixPeakDistance,
      double estimPeakWidth,
      double lastTimeStamp,
      double microDT,
      double meanSignal,
      double signalSD,
      double paretoScale,
      double paretoShape,
      GettablePDF massDistributionModel,
      double minimumPeakHeightExpDistr,
      double plasma_velocity_mu,
      double plasma_velocity_sd,
      double plasma_yPos_mu,
      double plasma_yPos_sd,
      GettablePDF yPos_PDF,
      double delayUncertaintyMicroseconds,
      double elementFractionUncertainty,
      double isotopeFractionUncertainty) {

    this.randElements = new ArrayList<>();

    this.label = label;
    this.color = color;
    this.marker = marker;

    this.rate = rate;
    this.estimPeakWidth = estimPeakWidth;
    this.fixPeakDistance = fixPeakDistance;
    this.lastTimeStampInMacroFrame = lastTimeStamp;
    this.microDT = microDT;

    //
    this.massDistribution = massDistributionModel.get(meanSignal, signalSD,
        paretoScale, paretoShape);

    this.minimumPeakHeightExpDistr = minimumPeakHeightExpDistr;

    this.plasma_velocity_distr = GettablePDF.GAUSSIAN.get(plasma_velocity_mu,
        plasma_velocity_sd, 0, 0);

    this.plasma_yPos_distr = yPos_PDF.get(plasma_yPos_mu, plasma_yPos_sd);

    this.delayUncertaintyMicroseconds = delayUncertaintyMicroseconds;

    this.elementFractionUncertainty = elementFractionUncertainty;

    this.isotopeFractionUncertainty = isotopeFractionUncertainty;
  }

  // Fill
  public void addElement(
      Element element,
      double signalFraction,
      double plasma_d_mu,
      double plasma_d_sd,
      double delay) {

    this.randElements.add(new RandomElementInstructions(
        element,
        signalFraction,
        plasma_d_mu,
        plasma_d_sd,
        delay));
  }

  public ParticlePopulationMatrix simulateParticleMatrix() {
    // Which elements does the particle consist of?
    List<Element> elements = listElements();

    // notify to inform user if an element was added twice accidentally.
    if (elements.stream().distinct().count() != elements.size()) {
      String elementList = elements.stream().map(Element::getShortName).collect(Collectors.joining(" "));
      NotificationFactory.openInfo("The same element was found twice for a particle material." +
          "This produces inconsistent results. Please remove duplicate instances from composition:\n" + elementList);
    }

    // [1] How many events are there? --> For each NP from this population, we draw an arrival time.
    // Note: Later, each isotope gets a specific delay on top of the arrival time.
    double[] arrivalTimes;

    if (!fixPeakDistance) {
      // [1a] How many events are there? --> For each NP from this population, we draw an arrival time.
      // Note: Later, each isotope gets a specific delay on top of the arrival time.
      if (rate > 0) {
        arrivalTimes = Statistics.expWaitTimes(1 / rate, lastTimeStampInMacroFrame);
      } else {
        arrivalTimes = new double[0];
      }
    } else {
      // [1b] w chatGPT: put peaks with even spacing between them (and some jitter to account for shifting
      // against the grid)
      double extraSpacing = estimPeakWidth / 2.0;       // extra space to avoid overlap

      double deltaBase = estimPeakWidth + extraSpacing;
      List<Double> arrTimes = new ArrayList<>(10000);

      arrTimes.add(Math.random() * deltaBase);

      int n = 0;
      double sum = 0;
      while (sum < lastTimeStampInMacroFrame) {
        n++;
        double slot = deltaBase;
        arrTimes.add(arrTimes.get(n - 1) + estimPeakWidth + Math.random() * (slot - estimPeakWidth));
        sum = arrTimes.get(n);
      }

      arrivalTimes = ArrUtils.doubleListToArr(arrTimes);
    }


    // [2] Now that we know how many NPs we have, we can initialize the matrix.
    ParticlePopulationMatrixRAM matrix = new ParticlePopulationMatrixRAM(label, color,
        marker, arrivalTimes.length, elements);

    // Next: draw random total particle signal intensities:
    // "How much total signal does the NP have?"
    // Instead of generating a norm.rand each time, get them all in one go.
    // We only get one entry per event (and not for each isotope!), i.e., relatively RAM inexpensive
    double[] randomTotalIntensities = getRandomIntensities(arrivalTimes.length);


    // [2.1] EXPERIMENTAL: Include Plasma Sampling Noise
    /*
    NOTE: Does not seem to work as such.
    Why? I made the resampling symmetric to that the mean particle area would be conserved.
    But, in that case, values are equally likely transferred to the higher or lower side of the
    distribution when just multiplying with a percent value. The SD seems to be conserved as well under these
    circumstances.
    Instead, a Poisson-like resampling should be more adequate.
    Why Poisson? It includes heteroscedasticity and easily handles nonzero value policy close to zero.
     */
    // double[] percents = new double[randomTotalIntensities.length];
    // for (int i = 0; i < percents.length; i++) {
    //   double val = -1;
    //   while (val < 0) {
    //     val = Statistics.randomifyPercent(1, 0.4);
    //   }
    //   percents[i] = val;
    // }
    // randomTotalIntensities = ArrUtils.multiply(randomTotalIntensities, percents);
    /*
    NOTE: Even adding this Poisson step does not alter much.
    It seems that most of the total variance on particles comes from the o.g. distribution
    AND the detector resampling itself. Coupling two Poisson-like resampling steps after another
    _should_ increase variance but apparently only to a rather small degree. Making a variance budget
    would be not fully straight forward (analytically) since we have the lognormal particle intensity,
    then the peak shape with 3 Gaussians defining shape, then the dwell time grid with random arrival
    and finally the Compound Poisson resampling process.
     */
    // Statistics.resamplePoissrnd(randomTotalIntensities);
    /*
    WHAT'S THE LESSON?
    1) When we have a lognormal counts distribution, we can modify DT, conc, BG, ... as we like
    2) For the background, we know that we sufficiently describe it with (overdispersed)
       Poisson or Compound Poisson --> there is no need for a Plasma sampling model.
    3) What we cannot do: Use the "randomTotalIntensities" as a prior, e.g., to input
       TEM-based particle distribution and compare the result with ICP-MS abd expect it to be spot-on.
       Why? We do not know the variance (the shape factor of the lognormal).
       In all other cases we use the empirical value for it.

       Peculiar: This empirical Var already includes the detector statistics ("resampling").
       The fact that we can do it that way may imply that the variance boost kind of cancels out
       and using the empirical variance for the particle SD is not overly boosted by the
       following resampling process. (Actually, I am not 100% sure why:
        a) Maybe it is because we only resample once and for one step not such a big increase is expected.
        b) Maybe the o.g. lognormal of the particles dominates (?)
        c) Maybe the fact that we have the area, then slice it in pieces, resample these pieces each with
           the detector statistics and THEN compute the sum again, ...
           Maybe somewhere in there, cancelling effects are present that lead to a miniscule increase?
           Keep in mind: The peak slicing has DT raster aliasing (random wait time),
           the peak itself has 2 Gaussians and one Lognormal/Gaussian. So the entire process may be hard
           to tackle analytically?
      4) In summary: Assume that the generator does not reveal the full point-spread function of an ICP-MS.
     */


    // [3] Iterate over the individual NP events,
    // to randomize their composition & assign icl shapes.
    for (int eventIdx = 0; eventIdx < arrivalTimes.length; eventIdx++) {

      // [3.1] Calculate the randomized fractional composition for each element in the NP.
      // For each NP, we randomize its composition with respect to the chemical element, and then normalize.
      double[] elementSignalLevelFractions = new double[randElements.size()];

      for (int i = 0; i < randElements.size(); i++) {
        RandomElementInstructions randElement = randElements.get(i);
        double fraction = randElement.getSignalFraction();
        fraction = Statistics.randomifyPercent(fraction, elementFractionUncertainty);
        elementSignalLevelFractions[i] = fraction;
      }

      // Normalize random fractions to 1 and also multiply with the actual signal
      ArrUtils.normalizeBySumOverriding(elementSignalLevelFractions);
      ArrUtils.multiplyOverriding(elementSignalLevelFractions, randomTotalIntensities[eventIdx]);

      // [4] Now that we know the randomized elemental composition,
      // move down to the isotope level.
      // This way, we do not need to integrate for each isotope
      // but only for each element.

      // Collections
      List<RandomPair<Element, Double>> peakTimesWithDelays = new ArrayList<>();
      List<RandomPair<Element, Double>> diffusionCoefficients = new ArrayList<>();
      List<RandomPair<Isotope, Double>> isotopeSignals = new ArrayList<>();

      // [4.1] To prepare ICL simulation: Draw random plasma parameters -> Avoid negative numbers!
      double randYPos = sampleStrictlyPositive(plasma_yPos_distr, plasma_yPos_distr.getExpectedValue());

      double randVelocity = sampleStrictlyPositive(plasma_velocity_distr,
          plasma_velocity_distr.getExpectedValue());

      for (int elementIdx = 0; elementIdx < randElements.size(); elementIdx++) {
        RandomElementInstructions randElement = randElements.get(elementIdx);

        // [4.2] Calculate the arrival times including delay.
        double arrivalTime = arrivalTimes[eventIdx];

        double peakTimeWithDelays = getDelayedArrivalTime(arrivalTime, randElement.getDelay(),
            microDT, lastTimeStampInMacroFrame);
        peakTimesWithDelays.add(new RandomPair<>(randElement.getElement(), peakTimeWithDelays));

        // [4.3] To prepare ICL simulation: Draw random plasma parameters - avoid negative numbers
        double randDiffusion = sampleStrictlyPositive(randElement.getPlasmaDiffusionDistr(),
            randElement.getPlasmaDiffusionDistr().getExpectedValue());
        diffusionCoefficients.add(new RandomPair<>(randElement.getElement(), randDiffusion));

        // [4.4] Randomify the signal per isotope based on the
        // mass per element that was calculated above.
        double elementSignalLevel = elementSignalLevelFractions[elementIdx];

        // randomify the isotopic percentage
        List<Isotope> isotopes = randElement.getElement().getIsotopes();
        double[] fractions = new double[isotopes.size()];

        // Calculate random isotopic composition; then normalize
        for (int i = 0; i < isotopes.size(); i++) {
          Isotope isotope = isotopes.get(i);
          fractions[i] = Statistics.randomifyPercent(isotope.getAbundance(),
              isotopeFractionUncertainty);
        }
        ArrUtils.normalizeBySumOverriding(fractions);
        ArrUtils.multiplyOverriding(fractions, elementSignalLevel);

        for (int i = 0; i < isotopes.size(); i++) {
          isotopeSignals.add(new RandomPair<>(isotopes.get(i), fractions[i]));
        }
      }

      // Finally, add the event
      matrix.addEvent(
          eventIdx,
          randVelocity,
          randYPos,
          peakTimesWithDelays,
          diffusionCoefficients,
          isotopeSignals);
    }
    LOGGER.trace("Population " + label + ": created particle matrix.");
    return matrix.getNewHddInstance();
  }

  // MATH
  private double[] getRandomIntensities(int n) {

    /*
    DONE
      - apply limit only IF exp func
      - check: if SD == 0 --> use µ (implemented in the CheckedDistribution)
      - above: where constructing the distr, either set func to null (and then: if null ---> use µ)
     */
    double[] signal = new double[n];
    for (int i = 0; i < signal.length; i++) {
      double val;
      val = sampleStrictlyPositive(massDistribution, massDistribution.getExpectedValue());

      if (massDistribution.isExponential() && val < minimumPeakHeightExpDistr) {
        int counter = 0;
        while (val < minimumPeakHeightExpDistr) {
          val = massDistribution.sample();
          counter++;
          if (counter > SAFETY_COUNTER_LIMIT) {
            LOGGER.debug("Cannot sample random number "
                + "greater than the specified lower limit from distribution. "
                + "Please check inputs (e.g., mean, standard deviation, scale, lower limit, ...).");
            break;
          }
        }
      }
      signal[i] = val;

    }
    return signal;
  }

  private double getDelayedArrivalTime(
      double arrivalTime,
      double delayMicroseconds,
      double microDT,
      double finalTimeStamp) {
    // initialize 'as is'
    double delayedTime = arrivalTime;
    // Randomify with numbers in µs, i.e., from the interval approx. [0, 100] and not
    // in units of sec (i.e., all multiplied with 1E-6), just to be sure.
    delayMicroseconds = Statistics.randomifyDelay(delayMicroseconds,
        delayUncertaintyMicroseconds);
    delayedTime = arrivalTime + delayMicroseconds * 1E-6;
    // Make sure, delay is within the external DT time frame.
    delayedTime = Math.max(microDT, delayedTime);
    delayedTime = Math.min(finalTimeStamp, delayedTime);

    return delayedTime;
  }

  private static double sampleStrictlyPositive(CheckedDistribution dist, double defaultValue) {
    double randomDraw = dist.sample();
    int safetyCounter = 0;
    while (randomDraw < 0 && safetyCounter < SAFETY_COUNTER_LIMIT) {
      safetyCounter++;
      randomDraw = dist.sample();
    }
    if (randomDraw < 0) {
      randomDraw = dist.getExpectedValue();
      if (randomDraw < 0) {
        randomDraw = defaultValue;
        LOGGER.debug("Cannot sample positive random number from distribution. "
            + "Please check inputs (mean and standard deviation).");
      }
    }
    return randomDraw;
  }

  /*
| Percentage Above Zero | P(X ≤ 0) | Z-Score | μ/σ   | σ/μ (RSD) |
| --------------------- | -------- | ------- | ----- | --------- |
| 60%                   | 0.40     | -0.253  | 0.253 | 3.95      |
| 70%                   | 0.30     | -0.524  | 0.524 | 1.91      |
| 80%                   | 0.20     | -0.842  | 0.842 | 1.19      |
| 90%                   | 0.10     | -1.282  | 1.282 | 0.780     |
| 95%                   | 0.05     | -1.645  | 1.645 | 0.608     |
| 97.5%                 | 0.025    | -1.960  | 1.960 | 0.510     |
| 98%                   | 0.02     | -2.054  | 2.054 | 0.487     |
| 99%                   | 0.01     | -2.326  | 2.326 | 0.430     |
| 99.5%                 | 0.005    | -2.576  | 2.576 | 0.388     |
| 99.9%                 | 0.001    | -3.090  | 3.090 | 0.324     |
| 99.99%                | 0.0001   | -3.719  | 3.719 | 0.269     |
   */


  // GETTERS
  public List<Element> listElements() {
    List<Element> asElement = randElements.stream()
        .map(RandomElementInstructions::getElement)
        .collect(Collectors.toList());
    return asElement;
  }

  public String getLabel() {
    return label;
  }

  // STATIC INTERNAL CLASS: ### ### ### ### ### ### ### ### ### ### ### ### ### ### ###

  /**
   * Note that we only need instructions on the level of element. For isotopes, the abundance is
   * known by the element, while the abundance uncertainty is specified for the particle
   * population.
   */
  public static class RandomElementInstructions {

    private final Element element;
    private final double signalFraction;

    private final CheckedDistribution plasma_d_distr;

    private final double delay;

    public RandomElementInstructions(
        Element element,
        double signalFraction,
        double plasma_d_mu,
        double plasma_d_sd,
        double delay) {
      this.element = element;
      this.signalFraction = signalFraction;

      this.plasma_d_distr = GettablePDF.GAUSSIAN.get(plasma_d_mu, plasma_d_sd, 0, 0);

      this.delay = delay;
    }

    public Element getElement() {
      return element;
    }

    public CheckedDistribution getPlasmaDiffusionDistr() {
      return plasma_d_distr;
    }

    public double getSignalFraction() {
      return signalFraction;
    }

    public double getDelay() {
      return delay;
    }
  }

  public static class RandomPair<K, V> {

    private final K key;
    private final V value;

    public RandomPair(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }
  }

}
