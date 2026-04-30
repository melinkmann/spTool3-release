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

import dataModelNew.TISeries;
import dataModelNew.Trace;

import java.util.*;

import math.SavitzkyGolay;
import math.Smoothing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.SmoothType;
import util.*;

public abstract class SearchUtils {

  private static final Logger LOGGER = LogManager.getLogger(SearchUtils.class);

  /*
   Note: Thresholds are not ceiled. Thus, use " > " and not " >= ".
   In non-integer context as with compound Poisson, this makes much more sense.
   Also it stops at THR = 0.
   */


  public static MainEventCollection windowSplitCorrectSearch(Trace trace, TISeries tiSeries,
                                                             ThresholdSupplier startThr,
                                                             ThresholdSupplier stopThr,
                                                             ThresholdSupplier heightThr,
                                                             double windowMicroseconds,
                                                             double bonusPointsAtMeanMicrosec) {

    double[] yData = tiSeries.getIntensity();
    List<Integer> currentEventIndices = new ArrayList<>();
    // double currentPeakHeight = 0; // This is checked in the checkForOverlaps method

    int lastIndex = tiSeries.size() - 1;

    // Assuming average peak width of 500, extend by 10% of peak
    double microSecDT = 1E6 * tiSeries.getDT();
    int dtBonus = (int) Math.ceil(windowMicroseconds / microSecDT);
    int bonusPointsAtMean = (int) Math.ceil(bonusPointsAtMeanMicrosec / microSecDT);
    if (bonusPointsAtMean < 0) {
      LOGGER.info("Cannot use bonus points smaller than 0. Used bonus points = 0 instead.");
      bonusPointsAtMean = 0;
    }

    MainEventCollection collection = new MainEventCollection(trace);

    for (int i = 0; i < yData.length; i++) {
      double yVal = yData[i];

      // If we assume QMS data, round the value to skim off potential imprecision ('5.02 cts' or so)
      if (tiSeries.isInteger()) {
        yVal = Math.round(yVal);
      }

      // Condition: either >start, or the current event has indices (has started) and we are >stop.
      double startThrVal = startThr.interpolateProtected(i, yData.length);
      double stopThrVal = stopThr.interpolateProtected(i, yData.length);
      if ((yVal >= startThrVal)
          || (!currentEventIndices.isEmpty() && (yVal >= stopThrVal) && yVal > 0)) {

        /*
        Idea: height/mean would prefer heigh peaks but higher peaks do not need so much window.
        Instead, low peaks do!
        Since we only allow yValues above the startThr, the ratio is guaranteed to be 0 < r < 1

          clearvars
          clc
          close all

          mu = 11;
          plus = 10;

          n = 1E4;

          intensity = poissrnd(mu, n,1);

          intensity = intensity(intensity>=mu);

          ratio = max(1,mu)./intensity;

          % ratio = ratio.^2;

          ratio = ratio .*plus;

          figure
          subplot 121
          histogram(ratio)
          subplot 122
          scatter(intensity, ratio)
          */


        // Poisson "fuzziness": µ will be noninteger, but y is integer (for QMS)!
        // --> rescale to 1 by ceiling µ
        double ratio = Math.max(1, startThrVal) / (Math.max(1, yVal));

        // after rescaling, ensure that ratio does not exceed 1
        ratio = Math.min(ratio, 1);

        // optional scaling using power: increase the punishment for large peaks
        // ratio = Math.pow(ratio, 2);

        // multiply with bonus p
        int bonusPoints = (int) Math.ceil(ratio * bonusPointsAtMean);

        int window = dtBonus + bonusPoints;

        //        System.out.println(" ");
        //        System.out.println("dtBonus: " + dtBonus);
        //        System.out.println("bonusPoints: " + bonusPoints);
        //        System.out.println("window: " + window);

        int extIdxBefore = Math.max(0, i - window);
        int extIdxAfter = Math.min(lastIndex, i + window);

        for (int idx = extIdxBefore; idx <= extIdxAfter; idx++) {
          currentEventIndices.add(idx);
          //currentPeakHeight = Math.max(currentPeakHeight, yVal); // This is checked in the
          // checkForOverlaps method
        }

        // check if event ended due to end of tISeries (i.e. last index reached) -> add it
        if (i == lastIndex) {
          // if (currentPeakHeight > heightThr.interpolate(i, yData.length)) {
          //  collection.add(new NpEvent(collection, currentEventIndices));
          // }
          // always add, apply height filter after merging
          collection.add(new NpEvent(collection, currentEventIndices));
          // reset the event indices after storing them
          currentEventIndices.clear();
          //currentPeakHeight = 0; // This is checked in the checkForOverlaps method
        }

        // idx is no event (equals "yVal <= cutoff")
      } else {
        // check if event ended (i.e. there are indices stored in the list)
        if (!currentEventIndices.isEmpty()) {
          // if (currentPeakHeight > heightThr.interpolate(i, yData.length)) {
          //  collection.add(new NpEvent(collection, currentEventIndices));
          // }
          // always add, apply height filter after merging
          collection.add(new NpEvent(collection, currentEventIndices));
          // reset the event indices after storing them
          currentEventIndices.clear();
          //currentPeakHeight = 0; // This is checked in the checkForOverlaps method
        }
        // anyway, ad the reading at idx=i (which is < threshold) to the background
        // backgSeries.add(i); --> this becomes obsolete as we calc the BG from "not NP".
      }
    }

    // Precalculate the event peak index
    collection.getNpEvents().forEach(e -> e.calcPeakIndex(yData));

    // check for overlaps
    collection = checkForOverlaps(collection, yData, heightThr
    );
    return collection;
  }

  public static MainEventCollection checkForOverlaps(MainEventCollection preliminaryCollection,
                                                     double[] yData, ThresholdSupplier heightThr) {

    Trace trace = preliminaryCollection.getTrace();
    MainEventCollection checkedCollection = new MainEventCollection(trace);

    if (!preliminaryCollection.getNpEvents().isEmpty()) {

      // Translate for easier merging
      List<TempEvent> tempEvents = new ArrayList<>();
      for (Event npEvent : preliminaryCollection.getNpEvents()) {
        tempEvents.add(new TempEvent(npEvent.getFirst(), npEvent.getLast()));
      }

      // sort
      tempEvents.sort(Comparator.comparingInt(e -> e.start));

      // merge
      List<TempEvent> mergedTempEvents = new ArrayList<>();
      TempEvent current = tempEvents.get(0);

      for (int i = 1; i < tempEvents.size(); i++) {
        TempEvent next = tempEvents.get(i);

        if (next.start <= current.end) {
          // Overlapping — merge
          current.end = Math.max(current.end, next.end);
        } else {
          // No overlap — add current and move to next
          mergedTempEvents.add(current);
          current = next;
        }
      }

      // Add the last one
      mergedTempEvents.add(current);

      // translate
      for (TempEvent mergedEvent : mergedTempEvents) {
        Event event = new NpEvent(checkedCollection, mergedEvent.start, mergedEvent.end);
        int peakIdx = event.getPeak();
        double currentPeakHeight = yData[peakIdx];
        if (currentPeakHeight > heightThr.interpolateProtected(peakIdx, yData.length)) {
          checkedCollection.add(event);
        }

      }
    }

    // Precalculate the event peak index
    checkedCollection.getNpEvents().forEach(e -> e.calcPeakIndex(yData));

    return checkedCollection;
  }

  public static class TempEvent {

    int start;
    int end;

    public TempEvent(int start, int end) {
      this.start = start;
      this.end = end;
    }

  }

  public static MainEventCollection splitCorrectSearch(Trace trace, TISeries tiSeries,
                                                       ThresholdSupplier startThr,
                                                       ThresholdSupplier stopThr,
                                                       ThresholdSupplier heightThr) {

    double[] yData = tiSeries.getIntensity();
    List<Integer> currentEventIndices = new ArrayList<>();
    double currentPeakHeight = 0;

    int lastIndex = tiSeries.size() - 1;

    MainEventCollection collection = new MainEventCollection(trace);

    for (int i = 0; i < yData.length; i++) {
      double yVal = yData[i];

      // If we assume QMS data, round the value to skim off potential imprecision ('5.02 cts' or so)
      if (tiSeries.isInteger()) {
        yVal = Math.round(yVal);
      }

      // Condition: either >start, or the current event has indices (has started) and we are >stop.
      double startThrVal = startThr.interpolateProtected(i, yData.length);
      double stopThrVal = stopThr.interpolateProtected(i, yData.length);

      if ((yVal >= startThrVal)
          || (!currentEventIndices.isEmpty() && (yVal >= stopThrVal) && yVal > 0)) {
        currentEventIndices.add(i);
        currentPeakHeight = Math.max(currentPeakHeight, yVal);
        // check if event ended due to end of tISeries (i.e. last index reached) -> add it
        if (i == lastIndex) {
          if (currentPeakHeight > heightThr.interpolateProtected(i, yData.length)) {
            collection.add(new NpEvent(collection, currentEventIndices));
          }
          // reset the event indices after storing them
          currentEventIndices.clear();
          currentPeakHeight = 0;
        }

        // idx is no event (equals "yVal <= cutoff")
      } else {
        // check if event ended (i.e. there are indices stored in the list)
        if (!currentEventIndices.isEmpty()) {
          // System.out.println("i\t"+i+"\tthr\t"+heightThr.interpolate(i, yData.length));
          if (currentPeakHeight > heightThr.interpolateProtected(i, yData.length)) {
            collection.add(new NpEvent(collection, currentEventIndices));
          }
          // reset the event indices after storing them
          currentEventIndices.clear();
          currentPeakHeight = 0;
        }
        // anyway, ad the reading at idx=i (which is < threshold) to the background
        // backgSeries.add(i); --> this becomes obsolete as we calc the BG from "not NP".
      }

    }

    // Precalculate the event peak index
    collection.getNpEvents().forEach(e -> e.calcPeakIndex(yData));
    return collection;
  }

  public static MainEventCollection smoothAndSplitCorrectSearch(Trace trace, TISeries tiSeries,
                                                                ThresholdSupplier startThr,
                                                                ThresholdSupplier stopThr,
                                                                ThresholdSupplier heightThr,
                                                                SmoothType smoothType,
                                                                double smoothWidthMicroseconds) {

    // TODO: we should compute the stats THR for the smoothed data...

    double[] yData = tiSeries.getIntensity();

    int window = (int) Math.ceil(smoothWidthMicroseconds
        / (1E6 * trace.getTISeries().getDT()));
    window = Math.max(window, 2);

    double[] smooth = yData;
    if (smoothType.equals(SmoothType.SAVITZKY_GOLAY)) {
      smooth = SavitzkyGolay.smoothSG(trace.getTISeries().getIntensity(), window, 3);
      // smooth = KernelSmooth.gaussianSmooth(smooth, window);
          /*
          Unsure if Gaussian is a good idea, as this will introduce peak shapes where there are actually no
           peaks.
           */
    } else if (smoothType.equals(SmoothType.MOAV)) {
      smooth = Smoothing.moavSmooth(yData, window);
    }

    ArrUtils.ceilOverriding(smooth);

    List<Integer> currentEventIndices = new ArrayList<>();
    double currentPeakHeight = 0;

    int lastIndex = tiSeries.size() - 1;

    MainEventCollection collection = new MainEventCollection(trace);

    for (int i = 0; i < yData.length; i++) {

      // We need both as we need the non-smooth height for statistical filtering by height.
      // Smooth is needed to stabilize start/stop criterion at base.
      double yVal = yData[i];
      // ceil smooth as smoothing lowers peaks substantially
      double yMax = Math.max(Math.ceil(smooth[i]), yVal);

      // If we assume QMS data, round the value to skim off potential imprecision ('5.02 cts' or so)
      if (tiSeries.isInteger()) {
        yVal = Math.round(yVal);
        yMax = Math.round(yMax);
      }

      // Condition: either >start, or the current event has indices (has started) and we are >stop.
      double startThrVal = startThr.interpolateProtected(i, yData.length);
      double stopThrVal = stopThr.interpolateProtected(i, yData.length);
      if ((yMax >= startThrVal)
          || (!currentEventIndices.isEmpty() && (yMax >= stopThrVal) && yMax > 0)) {
        currentEventIndices.add(i);
        currentPeakHeight = Math.max(currentPeakHeight, yVal);
        // check if event ended due to end of tISeries (i.e. last index reached) -> add it
        if (i == lastIndex) {
          // if (currentPeakHeight > heightThr.interpolate(i, yData.length)) {
          //  collection.add(new NpEvent(collection, currentEventIndices));
          // }
          // always add, apply height filter after merging
          collection.add(new NpEvent(collection, currentEventIndices));
          // reset the event indices after storing them
          currentEventIndices.clear();
          currentPeakHeight = 0;
        }

        // idx is no event (equals "yVal <= cutoff")
      } else {
        // check if event ended (i.e. there are indices stored in the list)
        if (!currentEventIndices.isEmpty()) {
          // System.out.println("i\t"+i+"\tthr\t"+heightThr.interpolate(i, yData.length));
          // if (currentPeakHeight > heightThr.interpolate(i, yData.length)) {
          //  collection.add(new NpEvent(collection, currentEventIndices));
          // }
          // always add, apply height filter after merging
          collection.add(new NpEvent(collection, currentEventIndices));
          // reset the event indices after storing them
          currentEventIndices.clear();
          currentPeakHeight = 0;
        }
        // anyway, ad the reading at idx=i (which is < threshold) to the background
        // backgSeries.add(i); --> this becomes obsolete as we calc the BG from "not NP".
      }

    }

    // Precalculate the event peak index
    collection.getNpEvents().forEach(e -> e.calcPeakIndex(yData));

    // check for overlaps
    collection = checkForOverlaps(collection, yData, heightThr);
    return collection;
  }


  public static MainEventCollection simpleSearch(Trace trace, TISeries tiSeries,
                                                 ThresholdSupplier thr) {

    double[] yData = tiSeries.getIntensity();

    MainEventCollection collection = new MainEventCollection(trace);

    for (int i = 0; i < yData.length; i++) {
      double yVal = yData[i];

      // If we assume QMS data, round the value to skim off potential imprecision ('5.02 cts' or so)
      if (tiSeries.isInteger()) {
        yVal = Math.round(yVal);
      }

      if (yVal > thr.interpolateProtected(i, yData.length) && yVal > 0) {
        collection.add(new NpEvent(collection, Collections.singletonList(i)));
      }
    }

    // Precalculate the event peak index
    collection.getNpEvents().forEach(e -> e.calcPeakIndex(yData));
    return collection;
  }


}
