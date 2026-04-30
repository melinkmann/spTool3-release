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

import analysis.Population;
import analysis.PopulationID;
import dataModelNew.TISeries;
import dataModelNew.TISeriesRAM;
import dataModelNew.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import math.stat.MeasureOfLocation;
import math.stat.MeasureOfStat;
import math.stat.PreFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.PopulationType;
import util.ArrUtils;

public class AverageUtils {


  private static final Logger LOGGER = LogManager.getLogger(AverageUtils.class);

  public static TISeries average(Trace trace,
                                 double widthSec,
                                 MeasureOfStat measure,
                                 PreFilter preFilter) {

    TISeries result;

    // (double) check if window is larger than the DT, else return original
    double dt = trace.getTISeries().getDT();

    if (dt < widthSec) {

      // raw data averaging
      double[] rawTime = trace.getTISeries().getTime();
      double[] rawIntensity = trace.getTISeries().getIntensity();

      // Create new time frame
      double startTime = rawTime.length > 0 ? rawTime[0] : 0;
      double finalTimeStamp = rawTime.length > 0 ? rawTime[rawTime.length - 1] : 0;
      double[] newTime = getTime(startTime, widthSec, finalTimeStamp);
      double[] centerTimeStamps = getCenterTime(newTime, widthSec);

      // Bin
      List<double[]> bins = binIntensities(rawTime, rawIntensity, newTime);

      // check last bin -> should have at least 10 data points or else will look like outlier
      if (bins.size() > 2 && bins.get(bins.size() - 1).length < 10) {
        double[] last = bins.get(bins.size() - 1);
        double[] penultimate = bins.get(bins.size() - 2);
        double[] newLast = ArrUtils.merge(penultimate, last);
        bins.remove(last);
        bins.remove(penultimate);
        bins.add(newLast);
      }

      // filter
      if (!preFilter.equals(PreFilter.NONE)) {
        for (int i = 0; i < bins.size(); i++) {
          bins.set(i, preFilter.filter(bins.get(i)));
        }
      }

      //
      /*
       1. Outlier test only within the subunits or else SD -> INF
       2. Outlier testing BEFORE averaging!
       3. If there are no data points left after filtering, the default value (0)
          is actually correct.
       */

      double[] newData = new double[bins.size()];
      for (int i = 0; i < bins.size(); i++) {
        newData[i] = measure.calc(bins.get(i));
      }

      if (newData.length != 0) {
        // there may be one more center stamp than time stamps due to fuzzy double < / >
        int maxLen = Math.min(centerTimeStamps.length, newData.length);
        result = new TISeriesRAM(Arrays.copyOfRange(centerTimeStamps, 0, maxLen), newData);
      } else {
        result = new TISeriesRAM();
      }

    } else {
      result = trace.getTISeries();
    }
    return result;
  }


  public static TISeries averageDrift(Trace trace, PopulationID populationID, double windowSec) {
    TISeries tiSeries = new TISeriesRAM();

    List<TISeries> average = AverageUtils.average(trace,
        populationID,
        EventParameter.NO_OF_EVENTS,
        EventParameter.HEIGHT,
        windowSec,
        MeasureOfLocation.MEAN,
        PreFilter.NONE);
    if (average.size() == 2) {
      tiSeries = average.get(1);
    }
    return tiSeries;
  }

  public static List<TISeries> average(Trace trace,
                                       PopulationID populationID,
                                       EventParameter npPar,
                                       EventParameter bgPar,
                                       double widthSec,
                                       MeasureOfStat measure,
                                       PreFilter rawPreFilter) {

    List<TISeries> result = new ArrayList<>();

    double[] rawTime = trace.getTISeries().getTime();

    double startTime = rawTime.length > 0 ? rawTime[0] : 0;
    double finalTimeStamp = trace.getTISeries().getLastTimeStamp();

    // Create new time frame
    double[] newTime = getTime(startTime, widthSec, finalTimeStamp);
    double[] centerTimeStamps = getCenterTime(newTime, widthSec);

    /*
    NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP NP
     */
    double[] npTimeStamps = trace.get(populationID, EventType.NP, EventParameter.CENTER_TIME);
    double[] npValue = trace.get(populationID, EventType.NP, npPar);

    if (npValue != null && npValue.length > 0) {
      // Bin
      List<double[]> bins = binIntensities(npTimeStamps, npValue, newTime);

      // filter
      if (!rawPreFilter.equals(PreFilter.NONE) && !npPar.equals(EventParameter.NO_OF_EVENTS)) {
        for (int i = 0; i < bins.size(); i++) {
          bins.set(i, rawPreFilter.filter(bins.get(i)));
        }
      }

      /*
       1. Outlier test only within the subunits or else SD -> INF
       2. Outlier testing BEFORE averaging!
       3. If there are no data points left after filtering, the default value (0)
          is actually correct.
       */

      double[] newData = new double[bins.size()];
      for (int i = 0; i < bins.size(); i++) {
        if (npPar.equals(EventParameter.NO_OF_EVENTS)) {
          double sum = ArrUtils.doubleSum(bins.get(i));
          newData[i] = sum / widthSec;
        } else {
          newData[i] = measure.calc(bins.get(i));
        }
      }

      TISeries series;
      if (newData.length != 0) {
        // there may be one more center stamp than time stamps due to fuzzy double < / >
        int maxLen = Math.min(centerTimeStamps.length, newData.length);
        series = new TISeriesRAM(Arrays.copyOfRange(centerTimeStamps, 0, maxLen), newData);
      } else {
        series = new TISeriesRAM();
      }
      result.add(series);
    } else {
      // just to have a placeholder on the pane later
      result.add(new TISeriesRAM());
    }


    /*
    BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG BG
    */

    if (!populationID.getType().equals(PopulationType.SIMULATION)
        && !populationID.getType().equals(PopulationType.EXTERNAL)
        && trace.hasType(populationID)
        && trace.getPopulation(populationID) != null) {

      Population population = trace.getPopulation(populationID);

      double[] bgTimeStamps = population.getEvents().get(EventType.BG, EventParameter.CENTER_TIME);
      double[] bgValue = population.getEvents().get(EventType.BG, bgPar);

      if (bgValue != null && bgValue.length > 0) {
        // Bin
        List<double[]> bgBins = binIntensities(bgTimeStamps, bgValue, newTime);

        // check last bin -> should have at least 10 data points or else will look like outlier
        if (bgBins.size() > 2 && bgBins.get(bgBins.size() - 1).length < 10) {
          double[] last = bgBins.get(bgBins.size() - 1);
          double[] penultimate = bgBins.get(bgBins.size() - 2);
          double[] newLast = ArrUtils.merge(penultimate, last);
          bgBins.remove(last);
          bgBins.remove(penultimate);
          if (last.length != 0) {
            bgBins.add(newLast);
          }
        }

        // filter
        if (!rawPreFilter.equals(PreFilter.NONE)) {
          for (int i = 0; i < bgBins.size(); i++) {
            bgBins.set(i, rawPreFilter.filter(bgBins.get(i)));
          }
        }

        //
      /*
       1. Outlier test only within the subunits or else SD -> INF
       2. Outlier testing BEFORE averaging!
       3. If there are no data points left after filtering, the default value (0)
          is actually correct.
       4. Normally, if we were reporting cps, we would have to divide the sum of bg counts
          by the net BG time, i.e., window-summedNPtime. However, if we just report mean counts,
          there is no need for that.
       */

        double[] newData = new double[bgBins.size()];
        for (int i = 0; i < bgBins.size(); i++) {
          newData[i] = measure.calc(bgBins.get(i));
        }

        TISeries series;
        if (newData.length != 0) {
          // there may be one more center stamp than time stamps due to fuzzy double < / >
          int maxLen = Math.min(centerTimeStamps.length, newData.length);
          series = new TISeriesRAM(Arrays.copyOfRange(centerTimeStamps, 0, maxLen), newData);
        } else {
          series = new TISeriesRAM();
        }
        result.add(series);
      }
    } else {
      // just to have a placeholder on the pane later
      result.add(new TISeriesRAM());
    }

    return result;
  }


  public static double[] getCenterTime(double[] averagedTimeArray, double widthSec) {
    double[] centers = new double[averagedTimeArray.length];
    for (int i = 0; i < averagedTimeArray.length; i++) {
      centers[i] = averagedTimeArray[i] - widthSec / 2;
    }
    return centers;
  }


  private static double[] getTime(double startTime, double widthSec, double finalTimeStamp) {
    final double maxTime = 1E4;
    final List<Double> newTime = new ArrayList<>();
    double currentTime = startTime;
    // Calc and add while still smaller:
    while (currentTime < finalTimeStamp) {
      if (currentTime > maxTime) {
        LOGGER.warn("Stopped - Averaging time window exceeded limit of " + maxTime + "s.");
        break;
      }
      currentTime += widthSec;
      newTime.add(currentTime);
    }
    return ArrUtils.doubleListToArr(newTime);
  }

  /*
   Index handling checked with chatGPT. Note: time has to be sorted in ascending order!
   */

  public static List<double[]> binIntensities(double[] time, double[] intensity, double[] newTime) {

    // add a zero to make new time behave like proper bin edges
    double[] extNewTime = new double[newTime.length + 1];
    extNewTime[0] = 0.0;
    System.arraycopy(newTime, 0, extNewTime, 1, newTime.length);
    newTime = extNewTime;

    int numBins = newTime.length - 1;

    // Temporary storage using lists before converting to arrays
    List<List<Double>> tempBins = new ArrayList<>(numBins);
    for (int i = 0; i < numBins; i++) {
      tempBins.add(new ArrayList<>());
    }

    int binIndex = 0;

    for (int i = 0; i < time.length; i++) {
      double t = time[i];

      // Advance binIndex until t fits into [newTime[binIndex], newTime[binIndex+1])
      while (binIndex < numBins && t >= newTime[binIndex + 1]) {
        binIndex++;
      }

      // Check if t is within current bin
      if (binIndex < numBins && t >= newTime[binIndex] && t < newTime[binIndex + 1]) {
        tempBins.get(binIndex).add(intensity[i]);
      }
    }

    // Convert List<List<Double>> to List<double[]>
    List<double[]> result = new ArrayList<>(numBins);
    for (List<Double> bin : tempBins) {
      result.add(ArrUtils.doubleListToArr(bin));
    }

    return result;
  }

}
