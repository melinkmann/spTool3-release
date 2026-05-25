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

package analysis;

import core.SpTool3Main;
import dataModelNew.*;
import io.SampleSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import math.units.Unit;
import math.units.enums.IntensityUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.SignalConversionOption;
import processing.parameterSets.impl.DTGroupParams;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;

public class RawProcessingUtils {

  private static final Logger LOGGER = LogManager.getLogger(RawProcessingUtils.class.getName());

  // FLAG


  /**
   * PlasmaQuant version, where DT is not guaranteed to be constant.
   * <p>
   * Math: 1000 cps * 0.001 s/DT = 1cts/DT.
   */
  public static double[] cpsToCts(double[] time, double[] intensity) {
    double[] cts;
    if (time.length == intensity.length && time.length > 2) {
      cts = new double[intensity.length];
      // First time stamp is usually not reliable
      cts[0] = intensity[0] * (time[2] - time[1]);
      cts[1] = intensity[1] * (time[2] - time[1]);

      for (int i = 2; i < intensity.length; i++) {
        cts[i] = intensity[i] * (time[i] - time[i - 1]);
      }

    } else {
      cts = intensity;
      LOGGER.error("Cannot convert to cts as time and intensity are of unequal length "
          + "or too short. Time length: " + time.length + ". Intensity length: " + intensity.length
          + ".");
    }
    return cts;
  }

  public static List<Double> cpsToCts(List<Double> time, List<Double> intensity) {
    List<Double> cts;
    if (time.size() == intensity.size() && time.size() > 2) {
      cts = new ArrayList<>(intensity.size());

      // First time stamp is usually not reliable
      cts.add(intensity.get(0) * (time.get(2) - time.get(1)));
      cts.add(intensity.get(1) * (time.get(2) - time.get(1)));

      for (int i = 2; i < intensity.size(); i++) {
        cts.add(intensity.get(i) * (time.get(i) - time.get(i - 1)));
      }

    } else {
      cts = new ArrayList<>(intensity);
      LOGGER.error("Cannot convert to cts as time and intensity are of unequal length "
          + "or too short. Time length: " + time.size()
          + ". Intensity length: " + intensity.size() + ".");
    }
    return cts;
  }

  /**
   * Agilent/iCAP version, where DT is usually constant except first time stamp.
   * <p>
   * Math: 1000 cps * 0.001 s/DT = 1cts/DT.
   */
  public static double[] cpsToCts(double[] intensity, double dwellTime) {
    double[] cts = new double[intensity.length];
    for (int i = 0; i < intensity.length; i++) {
      cts[i] = intensity[i] * dwellTime;
    }
    return cts;
  }

  public static boolean checkConversion(SignalConversionOption conversion, Unit unit,
                                        double[] signal) {
    boolean convertToCts;
    if (conversion.equals(SignalConversionOption.CONVERT)) {
      convertToCts = true;
    } else if (conversion.equals(SignalConversionOption.KEEP_AS_IS)) {
      convertToCts = false;
    } else if (conversion.equals(SignalConversionOption.AUTOMATIC)) {
      if (unit.equals(IntensityUnit.CPS)) {
        convertToCts = true;
      } else if (unit.equals(IntensityUnit.CTS)) {
        convertToCts = false;
        // Likely "N/A"
      } else {
        if (RawProcessingUtils.isCountData(signal)) {
          convertToCts = false;
        } else {
          convertToCts = true;
        }
      }
    } else {
      convertToCts = false;
    }
    return convertToCts;
  }


  /**
   * Tries to estimate if a icp-ms signal array is based on cts/DT data or cps data.
   */
  public static boolean isCountData(double[] signal) {
    boolean isCounts = false;
    double[] testArray = ArrUtils.nonzero(signal);
    Arrays.sort(testArray);

    int[] diffArr = new int[testArray.length - 1];
    for (int i = 1; i < testArray.length; i++) {
      diffArr[i - 1] = (int) (Math.round(testArray[i]) - Math.round(testArray[i - 1]));
    }

    Set<Integer> uniqueDifferences = new HashSet<>(ArrUtils.arrToList(diffArr));
    List<Integer> differenceList = new ArrayList<>(uniqueDifferences);
    Collections.sort(differenceList);

    if (differenceList.size() > 2) {
      double sum = 0;
      for (int i = 0; i < 3; i++) {
        sum += differenceList.get(i);
      }
      // if we have counts, the first three differences should be sth like 1,2,3,4,5 ... 5+4+3 = 12
      isCounts = sum <= 12;
    } else {
      isCounts = differenceList.stream().mapToDouble(Integer::doubleValue).sum() < 10;
    }

    return isCounts;
  }

  /// ///////////////////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////////////////////////////////////////////////////////////////////////////

  public static void groupDT(List<Sample> samples, List<Isotope> selIsotopes,
                             double newDT,
                             boolean exportSteps) {

    List<Sample> newSamples = new ArrayList<>();
    for (Sample sample : samples) {
      if (sample instanceof SampleImpl) {

        // Prepare: Only keep those traces from any sample that are selected.
        List<Trace> tracesInSample = new ArrayList<>(sample.getTraces());
        tracesInSample.removeIf(t ->
            selIsotopes.stream().noneMatch(iso -> iso.isEqual(t.getMzValue().getIsotope())));

        // Do the grouping
        HashMap<Trace, List<TISeries>> groupCollection = new HashMap<>();
        for (Trace trace : tracesInSample) {
          List<TISeries> newTISeries = groupDT(trace.getTISeries(), newDT, exportSteps);
          groupCollection.put(trace, newTISeries);
        }

        // check how many new samples, i.e., different DT
        int numberOfNewSamples = 0;
        for (Trace ogTrace : groupCollection.keySet()) {
          List<TISeries> tiSeriesList = groupCollection.get(ogTrace);
          numberOfNewSamples = Math.max(numberOfNewSamples, tiSeriesList.size());
        }

        // Arrange result as new Sample
        for (int i = 0; i < numberOfNewSamples; i++) {
          // Make a deep copy; method is deep copied internally
          Sample newSample = new SampleImpl(
              "DT adjusted " + sample.getNickName(),
              sample.isHighlight(),
              sample.getComment(),
              new SampleFile(1000 * newDT + " ms_" +
                  sample.getSampleFile().getNameWithinFile()),
              new LinkedHashMap<>(),
              new ArrayList<>(),
              new HashMap<>(),
              sample.getMethod(),
              sample.getQuant(),
              sample.getColor(),
              sample.getSampleDefaultIsotopes(),
              sample.getRemovedIsotopeInfo(),
              sample.getRecordedTofRange());
          for (Trace ogTrace : groupCollection.keySet()) {
            List<TISeries> tiSeriesList = groupCollection.get(ogTrace);
            if (i < tiSeriesList.size()) {
              TISeries series = tiSeriesList.get(i);
              Trace newTrace = new TraceImpl(newSample, ogTrace.getMzValue(), series);
              newSample.addTrace(newTrace);
            }
          }
          /*
           Do not follow this idea:
           a) we would also add this in normal method execution (bad)
           b) we add the DT grouping at the end (does not make sense, should happen before)
           c) when we process, the method is overridden
           "add the submethod the method within the sample
           newSample.getMethod().addSet(dtGroupParams);"
           */
          newSamples.add(newSample);
        }

        if (exportSteps) {
          SampleSet set = new SampleSet("Binned " + sample.getNickName(), newSamples);
          SpTool3Main.getRunTime().getSampleReg().addToExistingSetDirectly(set);
          newSamples.clear();
        }
      }

      // Add all binned samples as only the final result is obtained
      if (!exportSteps) {
        SampleSet set = new SampleSet("Binned samples", newSamples);
        SpTool3Main.getRunTime().getSampleReg().addToExistingSetDirectly(set);
      }
    }

  }


  public static List<TISeries> groupDT(TISeries originalData, double newDT,
                                       boolean exportSteps) {
    List<TISeries> seriesList = new ArrayList<>();

    double originalDT = originalData.getDT();

    int maxFactor = (int) Math.ceil(newDT / originalDT);
    int startFactor = exportSteps ? 2 : maxFactor;

    for (int i = startFactor; i <= maxFactor; i++) {
      seriesList.add(grp(originalData, i));
    }

    return seriesList;
  }

  // Modified from SpTool2 + some chatGPT discussion/checking
  private static TISeries grp(TISeries series, int groupSize) {
    double[] xData = series.getTime();
    double[] yData = series.getIntensity();

    List<Double> grpXData = new ArrayList<>();
    List<Double> grpYData = new ArrayList<>();

    if (groupSize < 2 || xData.length < groupSize) {
      LOGGER.info("Cannot group time series as group size or data length is too short.");
      return series;
    }

    // Cut away incomplete remaining bins. This is likely better als otherwise we would wrongly
    // estimate the cps from the cts/bin if bin size != const.
    int fullBins = xData.length / groupSize;

    for (int i = 0; i < fullBins * groupSize; i += groupSize) {
      double ySum = 0;
      for (int j = i; j < i + groupSize; j++) {
        ySum += yData[j];
      }

      grpXData.add(xData[i + groupSize - 1]);
      grpYData.add(ySum);
    }

    return new TISeriesHDD(grpXData, grpYData);
  }


  public static TISeries cutTime(TISeries originalSeries, double inclusiveStart,
                                 double inclusiveStop) {
    TISeries series = new TISeriesRAM();

    if (originalSeries.size() > 0 && inclusiveStart >= 0 && inclusiveStart < inclusiveStop) {

      if (originalSeries instanceof TISeriesRAM || originalSeries instanceof TISeriesHDD) {

        double[] time = originalSeries.getTime();

        if (inclusiveStart < time[time.length - 1]) {
          List<Double> x = new ArrayList<>();
          List<Double> y = new ArrayList<>();

          double[] intensity = originalSeries.getIntensity();
          for (int i = 0; i < time.length; i++) {
            if (inclusiveStart <= time[i] && time[i] <= inclusiveStop) {
              x.add(time[i]);
              y.add(intensity[i]);
            }
          }

          series = new TISeriesHDD(x, y);
        } else {
          LOGGER.error("Cannot cut time to limits when start > end of original time data.");
        }
      } else {
        // INSTANCE OF DTISeries
        double dt = originalSeries.getDT();
        int firstIndex = Math.max(0, (int) Math.round(inclusiveStart / dt) - 1);
        int lastIndex = Math.min(originalSeries.size() - 1, (int) Math.round(inclusiveStop / dt) - 1);
        List<int[]> selectedIndices = new ArrayList<>();
        selectedIndices.add(new int[]{firstIndex, lastIndex});
        if (originalSeries instanceof DTISeriesRAM) {
          ((DTISeriesRAM) originalSeries).setSelectedIndices(selectedIndices);
        } else if (originalSeries instanceof DTISeriesHDD) {
          ((DTISeriesHDD) originalSeries).setSelectedIndices(selectedIndices);
        }else {
          LOGGER.error("Unexpected type of TISeries Cannot handle it.");
        }
        series = originalSeries;
      }

    } else {
      series = originalSeries;
      LOGGER.error("Cannot cut time to limits when start > stop or time series is empty.");
    }

    return series;
  }

//  //  To-do: Make this function but for all traces, as traces share common matrices...
//
//  public static ParticlePopulationMatrix cutTime(
//      ParticlePopulationMatrix matrix,
//      double inclusiveStart,
//      double inclusiveStop) {
//
//    ParticlePopulationMatrix cutMatrix = new ParticlePopulationMatrixRAM();
//
//    if (matrix.getNumberOfEvents() > 0 && inclusiveStart > 0 && inclusiveStart < inclusiveStop) {
//
//      // Extract old entries
//      final String label = matrix.getLabel();
//      final Colors color = matrix.getColor();
//      final MarkerStyle marker = matrix.getMarker();
//      final HashMap<Element, double[]> arrivalTimeMap = matrix.getArrivalTimeMap();
//      final double[] plasmaVelocities = matrix.getPlasmaVelocities();
//      final double[] yPositions = matrix.getYPositions();
//      final HashMap<Element, double[]> plasmaDiffusionDMap = matrix.getPlasmaDiffusionDMap();
//      final HashMap<Isotope, double[]> intensityMap = matrix.getIntensityMap();
//
//      // new entries
//      final HashMap<Element, double[]> cutArrivalTimeMap = matrix.getArrivalTimeMap();
//      final double[] cutPlasmaVelocities = matrix.getPlasmaVelocities();
//      final double[] cutYPositions = matrix.getYPositions();
//      final HashMap<Element, double[]> cutPlasmaDiffusionDMap = matrix.getPlasmaDiffusionDMap();
//      final HashMap<Isotope, double[]> cutIntensityMap = matrix.getIntensityMap();
//
  // To-do: figure out, how we make sure that we get the longest time strech, i.e., end up with equal
  // length in all doule[] and also that this matches exactly the TISeries length??
//      // cut time
//      for (Element element : arrivalTimeMap.keySet()) {
//        int inclusiveStartIdx = -1;
//        int inclusiveStopIdx = -1;
//        double[] arrivalTimeStamps = arrivalTimeMap.get(element);
//        // We have to subtract the offset start time.
//        arrivalTimeStamps = ArrUtils.subtract(arrivalTimeStamps, inclusiveStart);
//        // All negative times are out
//        for (int i = 0; i < arrivalTimeStamps.length; i++) {
//          if (arrivalTimeStamps[i] >= 0) {
//            inclusiveStartIdx = i;
//            break;
//          }
//        }
//        // from start, find the first exceeding the endTime
//        for (int i = inclusiveStartIdx; i < arrivalTimeStamps.length; i++) {
//          if (arrivalTimeStamps[i] > inclusiveStop) {
//            inclusiveStopIdx = i;
//            break;
//          }
//        }
//
//        // Ensure if successful identification
//        if (inclusiveStartIdx < inclusiveStopIdx) {
//          // get sub arrays
//          arrivalTimeStamps = Arrays.copyOfRange(arrivalTimeStamps, inclusiveStartIdx, inclusiveStopIdx +
//          1);
//          cutArrivalTimeMap.put(element, arrivalTimeStamps);
//        }
//
//      }
//
//      LOGGER.error("Cannot cut time to limits when start > end of original time data.");
//
//    } else {
//      LOGGER.error("Cannot cut time to limits when start > stop or population is empty.");
//    }
//
//    return cutMatrix;
//  }
}
