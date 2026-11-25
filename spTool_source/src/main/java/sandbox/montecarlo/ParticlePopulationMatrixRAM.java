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

import analysis.AnalysisUtils;
import core.SpTool3Main;
import dataModelNew.TraceMC;
import dataModelNew.mz.Element;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.DataList.DataPoint;
import sandbox.montecarlo.ParticleInstructions.RandomPair;
import util.ArrUtils;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;
import visualizer.styles.MarkerStyle;
import visualizer.styles.OkabeItoColors;

public class ParticlePopulationMatrixRAM implements ParticlePopulationMatrix, Serializable {

  /*
  Readme:
  In case that we merge m/z, I do not think we should consider this here.
  Why?
  1) Most of the values in this class cannot be merged (plasma velocity, ...).
  It is much better to store the "original simulation outcome" in case one needs it.
  This is a bit unfortunate but due to the resampling we have to accept that there is
  a "barrier" between what was simulated and what we detect.

  Now, we could in theory merge area, take the longest width, and so forth.
  For height, it already gets difficult, as we do not where if both peaks align.
  For the width & number of points, we lose the detail that larger peaks appear wider
  as their flanks may be boosted, neither do we consider imperfect alignment of peaks, i.e.,
  they may actually be wider than we think.
  This would have to happen at a higher level, where we retrieve event data.
  However, this requires 2 things:
  1) We must store the relevant decisions (ConflictOptions)
  2) The Trace (?) or some other class needs to handle the conflict resolution algorithm
     each time.
  In some regards, I think, this might exactly what we want. The sim represents the pristine
  expectation and then the experiment comes and jeopardizes it, by randomness and spectral
  resolution.

   */

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(ParticlePopulationMatrixRAM.class);

  // All the data for a particle except for the integration of the peak.

  public static final double THR = 1; // 1 cts is a conservative threshold

  private final String label;

  private final Colors color;
  private final MarkerStyle marker;

  private final int numberOfEvents;

  private final double[] plasmaVelocities;
  private final double[] yPositions;

  private final HashMap<Element, double[]> arrivalTimeMap;

  private final HashMap<Element, double[]> plasmaDiffusionDMap;

  private final HashMap<Isotope, double[]> intensityMap;

  // Dummy
  public ParticlePopulationMatrixRAM() {
    this.label = "New population matrix";
    this.color = OkabeItoColors.BLACK;
    this.marker = MarkerStyle.CIRCLE;
    this.numberOfEvents = 0;
    this.plasmaVelocities = new double[0];
    this.yPositions = new double[0];
    this.arrivalTimeMap = new HashMap<>();
    this.plasmaDiffusionDMap = new HashMap<>();
    this.intensityMap = new HashMap<>();
  }

  public ParticlePopulationMatrixRAM(String label, Colors color, MarkerStyle marker,
                                     int numberOfEvents, List<Element> elements) {
    this.label = label;
    this.color = color;
    this.marker = marker;
    this.numberOfEvents = numberOfEvents;
    this.plasmaVelocities = new double[numberOfEvents];
    this.yPositions = new double[numberOfEvents];
    this.arrivalTimeMap = new LinkedHashMap<>();
    this.plasmaDiffusionDMap = new LinkedHashMap<>();
    this.intensityMap = new LinkedHashMap<>();
    //
    createMap(arrivalTimeMap, elements, numberOfEvents);
    createMap(plasmaDiffusionDMap, elements, numberOfEvents);
    createMap(intensityMap, Element.getIsotopes(elements), numberOfEvents);
  }

  private static <K> void createMap(HashMap<K, double[]> map, List<K> keys, int numberOfEvents) {
    for (K key : keys) {
      map.put(key, new double[numberOfEvents]);
    }
  }


  // Deep copy for the case where both types of matrix may be passed
  public ParticlePopulationMatrixRAM(ParticlePopulationMatrix matrix) {
    this(matrix.getLabel(),
        matrix.getColor(),
        matrix.getMarker(),
        matrix.getNumberOfEvents(),
        matrix.getPlasmaVelocities(),
        matrix.getYPositions(),
        matrix.getArrivalTimeMap(),
        matrix.getPlasmaDiffusionDMap(),
        matrix.getIntensityMap());
  }

  // Deep copy specifically for HDD cases
  public ParticlePopulationMatrixRAM(ParticlePopulationMatrixHDD matrix) {
    // If buffer, all arrays will be instantiated freshly in the getters
    this.label = matrix.getLabel();
    this.color = matrix.getColor();
    this.marker = matrix.getMarker();
    this.numberOfEvents = matrix.getNumberOfEvents();
    this.plasmaVelocities = matrix.getPlasmaVelocities();
    this.yPositions = matrix.getYPositions();
    this.arrivalTimeMap = matrix.getArrivalTimeMap();
    this.plasmaDiffusionDMap = matrix.getPlasmaDiffusionDMap();
    this.intensityMap = matrix.getIntensityMap();
  }

  // Deep copy
  public ParticlePopulationMatrixRAM(String label, Colors color, MarkerStyle marker,
                                     int numberOfEvents, double[] plasmaVelocities, double[] yPositions,
                                     HashMap<Element, double[]> arrivalTimeMap,
                                     HashMap<Element, double[]> plasmaDiffusionDMap,
                                     HashMap<Isotope, double[]> intensityMap) {

    HashMap<Element, double[]> copyArrivalTimeMap = new LinkedHashMap<>();
    arrivalTimeMap.keySet()
        .forEach(e -> copyArrivalTimeMap.put(e, ArrUtils.copy(arrivalTimeMap.get(e))));

    HashMap<Element, double[]> copyPlasmaDiffusionDMap = new LinkedHashMap<>();
    plasmaDiffusionDMap.keySet()
        .forEach(e -> copyPlasmaDiffusionDMap.put(e, ArrUtils.copy(plasmaDiffusionDMap.get(e))));

    HashMap<Isotope, double[]> copyIntensityMap = new LinkedHashMap<>();
    intensityMap.keySet()
        .forEach(e -> copyIntensityMap.put(e, ArrUtils.copy(intensityMap.get(e))));

    this.label = label;
    this.color = color;
    this.marker = marker;
    this.numberOfEvents = numberOfEvents;
    this.plasmaVelocities = ArrUtils.copy(plasmaVelocities);
    this.yPositions = ArrUtils.copy(yPositions);
    this.arrivalTimeMap = copyArrivalTimeMap;
    this.plasmaDiffusionDMap = copyPlasmaDiffusionDMap;
    this.intensityMap = copyIntensityMap;
  }

  // DEEP COPY including arrays.
  // This is meant for, e.g., SoftReferences where we do not want any modification to the original.
  public ParticlePopulationMatrixRAM copy() {
    return new ParticlePopulationMatrixRAM(
        new String(label),
        new SpColor(color.get()),
        marker,
        numberOfEvents,
        plasmaVelocities,
        yPositions,
        arrivalTimeMap,
        plasmaDiffusionDMap,
        intensityMap
    );
  }

  // Fill
  public void addEvent(
      int eventIndex,
      double velocity,
      double yPos,
      List<RandomPair<Element, Double>> peakTimesWithDelays,
      List<RandomPair<Element, Double>> diffusionCoefficients,
      List<RandomPair<Isotope, Double>> isotopeSignals) {
    plasmaVelocities[eventIndex] = velocity;
    yPositions[eventIndex] = yPos;

    for (RandomPair<Element, Double> pair : peakTimesWithDelays) {
      Element element = pair.getKey();
      if (arrivalTimeMap.containsKey(element)) {
        this.arrivalTimeMap.get(element)[eventIndex] = pair.getValue();
      }

    }

    for (RandomPair<Element, Double> pair : diffusionCoefficients) {
      Element element = pair.getKey();
      if (plasmaDiffusionDMap.containsKey(element)) {
        this.plasmaDiffusionDMap.get(element)[eventIndex] = pair.getValue();
      }
    }

    for (RandomPair<Isotope, Double> pair : isotopeSignals) {
      Isotope isotope = pair.getKey();
      if (intensityMap.containsKey(isotope)) {
        this.intensityMap.get(isotope)[eventIndex] = pair.getValue();
      }
    }
  }

  // MATH
  public HashMap<Isotope, IndexBufferCollection> integratePeaks(
      double microDT,
      double finalTimeStamp,
      int totalTimeFrameSize,
      AtomicBoolean wasStopped) {

    // For the actual integration (avoid instantiation for each peak)
    // final BaseAbstractUnivariateIntegrator trapezoidIntegrator = new TrapezoidIntegrator();
    final BaseAbstractUnivariateIntegrator integrator = new RombergIntegrator(); // faster than trapezoid!
    // final BaseAbstractUnivariateIntegrator integrator = new SimpsonIntegrator();

    HashMap<Isotope, IndexBufferCollection> dataMap = new LinkedHashMap<>();
    List<Element> elements = new ArrayList<>(plasmaDiffusionDMap.keySet());
    List<Isotope> isotopes = Element.getIsotopes(elements);
    // pre-fill the map
    isotopes.forEach(isotope -> dataMap.put(isotope, new IndexBufferCollection()));

    // Integrate (only for each element! -  isotopes are calculated via normalization)
    mainLoop:
    for (Element element : elements) {
      LOGGER.trace("Integrating element: " + element.getLongName() + ".");

      // For each event, integrate
      for (int index = 0; index < numberOfEvents; index++) {
        if (wasStopped.get()) {
          break mainLoop;
        }

        DataList<Integer> peakDataPoints = integratePeak(
            index,
            element,
            microDT,
            finalTimeStamp,
            totalTimeFrameSize,
            integrator);

        // Now, for each single peak (not the array with all peaks!):
        // Iterate over the isotopes and normalize signals.
        for (Isotope isotope : element.getIsotopes()) {
          // Normalizes a peak so that the signal sum matches the signal stated for the peak for the
          // respective isotope.

          double isotopeSignal = intensityMap.get(isotope)[index];
          DataList<Integer> isotopeDPs = PeakFunction.normalizeArea(peakDataPoints, isotopeSignal);
          // HashMap should contain an IndexBufferCollection for each Isotope already!
          dataMap.get(isotope).addData(isotopeDPs);
        }
      }
    }
    return dataMap;
  }

  /*
   This is for the visualizer/exports.
   Note: We should not mix data from before and after randomization.
   Why? At low signal, spread kicks in, and we may get smaller gross than net values and such nonsense.
   --> "Simulation" population shows expected values according to the pre-resampling stage.
       This serves only as an "overview" and maybe for later comparison with ML models.
   --> Randomized data can only really be compared after evaluation.
    Thus, here we must not return, e.g., tiSeries intensity @ index of peak as "gross height"
    while reporting the integrated height w/o BG as "net". As stated, net my be randomized to be larger
    than gross.
    What we can do, is take the net and add the BG. This way, we report and compare strictly before
    randomization.
    Any weird effect is due to that fact and it is controllable and expectable.
   */

  public double[] integrateForHeightWidthAndPoints(
      int indexOfEvent,
      double macroDT,
      double finalTimeStamp,
      int totalTimeFrameSize,
      TraceMC traceMC) {

    // We can use this BG to estimate net/gross conversion
    Isotope isotope = traceMC.getChannel().getIsotope();
    double bg = traceMC.getEmpiricalMeanBG(); // includes sine oscillation. [not time-resolved!]

    // Integrate the peak
    final BaseAbstractUnivariateIntegrator integrator = new RombergIntegrator(); // faster than trapezoid!
    DataList<Integer> peakDataPoints = integratePeak(
        indexOfEvent,
        isotope.getElement(),
        macroDT,
        finalTimeStamp,
        totalTimeFrameSize,
        integrator);

    // Apply normalization
    double isotopeSignal = intensityMap.get(isotope)[indexOfEvent];
    DataList<Integer> isotopeDPs = PeakFunction.normalizeArea(peakDataPoints, isotopeSignal);

    /*
    For area, we could report the random intensity value,
    i.e., the drawn value from the particle intensity distribution.
    This does not include additional noise from the intensity randomization!

    However, in this definition, it is hard to estimate "number of points"
    for the net/gross conversion, where BG is multiplied with the number of points.

    Hence, let us just use the "above threshold" definition here.
    Benefit: at worst, we lose sth around 2-4 counts;
    But as a benefit, we are consistent in what we report; i.e.,
    signal intensity also includes the additional noise from the resampling randomization.
     */

    // if THR = 1, we get problems with tiny peaks!
    double areaPct = SpTool3Main.getRunTime().getConfParams()
        .getGroundTruthPeakIntegrationAreaPercent().getValue();
    areaPct = Math.max(areaPct, Math.nextUp(0d));
    areaPct = Math.min(areaPct, Math.nextDown(100d));
    double areaPercentileThr = getThresholdByAreaPercent(isotopeDPs.getY(), areaPct);
    double thr = Math.min(THR, areaPercentileThr);
    thr = Math.max(thr, Math.nextUp(0d));

    double netHeight = 0;
    double grossHeight;
    double netArea = 0;
    double grossArea;
    int noOfPoints = 0;
    int startIdx = -1;
    int endIdx = -1;
    int peakIdx = -1;

    List<DataPoint<Integer>> dataPoints = isotopeDPs.getData();

    for (DataPoint<Integer> dp : dataPoints) {
      double y = dp.getY();
      if (y >= thr) {
        netArea += y;
        netHeight = Math.max(netHeight, y);
        if (y == netHeight) {
          peakIdx = dp.getX();
        }
        noOfPoints++;
        if (startIdx < 0) {
          startIdx = dp.getX();
        }
      } else {
        if (startIdx >= 0) {
          // endIdx = dp.getX(); // exclusive: end is the first non-Event index after event
          endIdx = dp.getX() - 1;   // last above-threshold point (inclusive)
          break;
        }
      }
    }

    // very long DT -> never goes below thr, endIdx is never set
    if (endIdx < 0) {
      endIdx = dataPoints.get(dataPoints.size() - 1).getX();
    }

    // very low signal -> only one DP
    if (startIdx < 0 || endIdx < 0) {
      double yMax = 0;
      for (DataPoint<Integer> dp : dataPoints) {
        if (dp.getY() > yMax) {
          yMax = dp.getY();
          startIdx = dp.getX();
          endIdx = dp.getX();
          peakIdx = dp.getX();
        }
      }
    }

    // end must be one after start or else no event

    grossArea = netArea + noOfPoints * bg;
    grossHeight = netHeight + bg;
    double duration = 1E6 * noOfPoints * macroDT;

    double asymmetry = AnalysisUtils.computeAsymmetryFactor(noOfPoints,
        startIdx, endIdx, peakIdx);

    return new double[]{
        grossArea,
        netArea,
        grossHeight,
        netHeight,
        duration,
        noOfPoints,
        bg * noOfPoints,
        startIdx,
        endIdx,
        (int) Math.floor(0.5 * (startIdx + endIdx)),
        asymmetry
    };
  }


  /**
   * @param index              index of the event
   * @param microDT            desired "micro dwell time"
   * @param finalTimeStamp     last time stamp of the corresponding time frame
   * @param totalTimeFrameSize length, i.e., number of data points in the time frame
   */
  public DataList<Integer> integratePeak(
      int index,
      Element element,
      double microDT,
      double finalTimeStamp,
      int totalTimeFrameSize,
      BaseAbstractUnivariateIntegrator integrator) {

    // Build the function
    PeakFunction peakFunction = new PeakFunction(
        plasmaDiffusionDMap.get(element)[index],
        yPositions[index],
        plasmaVelocities[index]);

    // Integrate
    DataList<Integer> peakData = peakFunction.integrateEntirePeak(
        integrator,
        microDT,
        finalTimeStamp,
        totalTimeFrameSize,
        arrivalTimeMap.get(element)[index]);

    return peakData;
  }


  public double[] getMeanArrivalTimes() {
    double[] averagedArrivalTimes = new double[numberOfEvents];
    for (int index = 0; index < numberOfEvents; index++) {
      double summedTime = 0;
      for (Element element : arrivalTimeMap.keySet()) {
        double[] arrivalTime = arrivalTimeMap.get(element);
        summedTime += arrivalTime[index];
      }
      averagedArrivalTimes[index] = summedTime / arrivalTimeMap.size();
    }
    return averagedArrivalTimes;
  }

  public HashMap<Isotope, int[]> getPeakMarkerMacroIndices(double macroDT) {
    HashMap<Isotope, int[]> markers = new LinkedHashMap<>();

    // If DT<0, this means that the corresponding TISeries was a Dummy (i.e., the element does not exist in
    // the sample)
    if (macroDT > 0) {
      for (Isotope isotope : intensityMap.keySet()) {
        int[] indices = new int[numberOfEvents];
        Element element = isotope.getElement();
        for (int index = 0; index < numberOfEvents; index++) {
          double microFrameStamp = arrivalTimeMap.get(element)[index];
          // -1: µ=10µs, m=1000µs --> e.g., t=50µs is in the first window, i.e., at index i=0.
          int macroFrameIdx = (int) Math.ceil(microFrameStamp / macroDT) - 1;
          // macroFrameIdx = (int) Math.floor(microFrameStamp / macroDT); // TODO: testing
          double macroFrameStamp = macroDT * (macroFrameIdx + 1);
          // Somehow, sometimes this ends up showing the peak tip marker to the left of the population
          // marker...
          if (macroFrameStamp < microFrameStamp) {
            LOGGER.trace("The event peak time stamp was smaller than the particle arrival time. "
                + "microFrameStamp: " + microFrameStamp + ". macroFrameStamp:" + macroFrameStamp
                + ". Macro frame index was incremented by one to compensate.");
            macroFrameIdx++;
          }
          indices[index] = macroFrameIdx;
        }
        markers.put(isotope, indices);
      }
    }
    return markers;
  }

  // Claude Sonnet 5.5: try estimate thresholds from peak area to cut at 99%, (thr=mean of borders)
  static double getThresholdByAreaPercent(java.util.List<Double> data, double pPercent) {
    double result = 1;
    int n = 1;
    double p = 1;

    n = data.size();
    p = Math.max(0, Math.min(100, pPercent));

    if (n > 0) {
      int peak = 1;
      double sum = 1;
      int i = 1;

      // 1) highest value, 2) sum of all points
      peak = 0;
      sum = 0;
      for (i = 0; i < n; i++) {
        sum += data.get(i);
        if (data.get(i) > data.get(peak)) {
          peak = i;
        }
      }

      // 3) grow the window around the peak until p percent of the sum is covered
      double target = 1;
      int lo = 1;
      int hi = 1;
      double covered = 1;
      double left = 1;
      double right = 1;

      target = sum * p / 100.0;
      lo = peak;
      hi = peak;
      covered = data.get(peak);

      while (covered < target && (lo > 0 || hi < n - 1)) {
        left = lo > 0 ? data.get(lo - 1) : Double.NEGATIVE_INFINITY;
        right = hi < n - 1 ? data.get(hi + 1) : Double.NEGATIVE_INFINITY;

        if (left >= right) {
          lo--;
          covered += left;
        } else {
          hi++;
          covered += right;
        }
      }

      // 4) mean of the two bordering points
      result = (data.get(lo) + data.get(hi)) / 2.0;
    }

    return result;
  }


  // Retrieve/getter
  @Override
  public String getLabel() {
    return label;
  }

  public Colors getColor() {
    return color;
  }

  public MarkerStyle getMarker() {
    return marker;
  }

  @Override
  public int getNumberOfEvents() {
    return numberOfEvents;
  }

  public double[] getPlasmaVelocities() {
    return plasmaVelocities;
  }

  public double[] getYPositions() {
    return yPositions;
  }

  public HashMap<Element, double[]> getArrivalTimeMap() {
    return arrivalTimeMap;
  }

  public HashMap<Element, double[]> getPlasmaDiffusionDMap() {
    return plasmaDiffusionDMap;
  }

  public HashMap<Isotope, double[]> getIntensityMap() {
    return intensityMap;
  }

  @Override
  public List<Element> listElements() {
    return new ArrayList<>(arrivalTimeMap.keySet());
  }

  @Override
  public List<Isotope> listIsotopes() {
    return new ArrayList<>(intensityMap.keySet());
  }


  @Override
  public boolean hasIsotope(Isotope isotope) {
    return intensityMap.containsKey(isotope);
  }

  @Override
  public boolean hasElement(Element element) {
    return arrivalTimeMap.containsKey(element);
  }

  @Override
  public ParticlePopulationMatrixHDD getNewHddInstance() {
    return new ParticlePopulationMatrixHDD(this);
  }

  @Override
  public ParticlePopulationMatrixRAM getNewRamInstance() {
    return new ParticlePopulationMatrixRAM(this);
  }


}
