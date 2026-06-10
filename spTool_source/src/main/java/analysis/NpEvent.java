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
import dataModelNew.TISeriesRAM;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import util.NF;
import util.SnF;

public class NpEvent implements Event, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(NpEvent.class.getName());

  /*
  Why store collection?
  a) it makes processing way easier
  b) for equals()/hash() we must feed the collection,
     else, events from different mz with equal indices look the same (calls for bugs!)
  c) Note: assume we have maybe 1E6 events (1 pointer is 32Bit int = 4 Bytes --> 4MB ... seems okay)
   */

  private final EventCollection collection;
  private final int startInclusive;
  private final int endInclusive;
  private int peak = -1;
  private double bgPerNP = 0;

  public NpEvent(EventCollection collection, List<Integer> indices) {
    this.collection = collection;
    if (!indices.isEmpty()) {
      Collections.sort(
          indices); // make sure we are in order (see e.g. window search which may interfere here)
      this.startInclusive = indices.get(0);
      this.endInclusive = indices.get(indices.size() - 1);
    } else {
      this.startInclusive = -1;
      this.endInclusive = -1;
    }
  }

  public NpEvent(EventCollection collection, int startInclusive, int endInclusive) {
    this.collection = collection;
    this.startInclusive = startInclusive;
    this.endInclusive = endInclusive;
  }

  // Deep copy
  public NpEvent(EventCollection collection, int startInclusive, int endInclusive, int peak,
                 double bgPerNP) {
    // do not deep copy this, as this is a pointer to the parent that needs to be pased!!
    this.collection = collection;
    this.startInclusive = startInclusive;
    this.endInclusive = endInclusive;
    this.peak = peak;
    this.bgPerNP = bgPerNP;
  }

  @Override
  public Event copy(EventCollection parentCollection) {
    return new NpEvent(parentCollection, startInclusive, endInclusive, peak, bgPerNP);
  }

  // Override equals: Otherwise we cannot compare/handle Populations or remove/add events specifically
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NpEvent npEvent = (NpEvent) o;
    return collection == npEvent.collection
        && startInclusive == npEvent.startInclusive && endInclusive == npEvent.endInclusive;
  }

  // Same idea: if we want to use hashMaps efficiently, the same event should yield the same hash.
  @Override
  public int hashCode() {
    return Objects.hash(collection, startInclusive, endInclusive);
  }

  public EventCollection getCollection() {
    return collection;
  }

  @Override
  public int getNoOfPoints() {
    return endInclusive - startInclusive + 1;
  }

  public void setBgPerNP(double bgPerNP) {
    if (Double.isFinite(bgPerNP)){
      this.bgPerNP = bgPerNP;
    }else {
      LOGGER.error("Background is not finite! Did not set background.");
    }
  }

  public double getBgPerNP() {
    return bgPerNP;
  }

  public boolean isEmpty() {
    return startInclusive == -1 && endInclusive == -1;
  }

  @Override
  public int getFirst() {
    return startInclusive;
  }

  @Override
  public int getLast() {
    return endInclusive;
  }

  public int[] getIndices() {
    int length = endInclusive - startInclusive + 1;
    int[] indices;

    if (startInclusive == endInclusive) {
      indices = new int[]{startInclusive};
    } else {
      indices = new int[length];
      for (int index = startInclusive; index <= endInclusive; index++) {
        indices[index - startInclusive] = index;
      }
    }
    return indices;
  }

  public List<Integer> getIndicesList() {
    int length = endInclusive - startInclusive + 1;
    List<Integer> indices = new ArrayList<>(length);

    if (startInclusive == endInclusive) {
      indices.add(startInclusive);
    } else {
      for (int index = startInclusive; index <= endInclusive; index++) {
        indices.add(index);
      }
    }
    return indices;
  }

  public int getStartIndexInclusive() {
    return startInclusive;
  }

  public int getEndIndexInclusive() {
    return endInclusive;
  }

  // Get the index. If by any chance, the peak is still not known, calculate.
  public int getPeak() {
    if (peak < 0) {
      calcPeakIndex(collection.getCheckedTISeries().getIntensity());
    }
    return peak;
  }

  // Call from outside for the whole collection. This avoid getting data from HDD each time.
  public void calcPeakIndex(double[] intensities) {
    // Start at -1 to cover the odd case where both are zero (seems to occur in the smooth search)
    double max = -1;
    int[] indices = getIndices();
    for (int i = 0; i < indices.length; i++) {
      int idx = indices[i];
      if (intensities[idx] > max) {
        max = intensities[idx];
        peak = idx;
      }
    }
  }

  public int getCenter() {
    return (int) Math.floor(0.5 * (endInclusive + startInclusive));
  }


  @Override
  public double get(EventParameter param) {
    TISeries tiSeries = collection.getCheckedTISeries();
    return get(param, tiSeries.getTime(), tiSeries.getIntensity());
  }

  @Override
  public double get(EventParameter param, double[] time, double[] intensity) {
    double val = 0;

    int[] indices = getIndices();
    double dwellTime = collection.getCheckedTISeries().getDT();

    switch (param) {
      case AREA -> {
        double sum = 0;
        for (int i = 0; i < indices.length; i++) {
          sum += intensity[indices[i]];
        }
        val = sum;
      }

      case NET_AREA -> {
        double sum = 0;
        for (int i = 0; i < indices.length; i++) {
          sum += intensity[indices[i]];
        }
        val = sum - getBgPerNP();
      }

      case HEIGHT -> {
        double max = 0;
        for (int i = 0; i < indices.length; i++) {
          max = Math.max(intensity[indices[i]], max);
        }
        val = max;
      }

      case NET_HEIGHT -> {
        double max = 0;
        for (int i = 0; i < indices.length; i++) {
          max = Math.max(intensity[indices[i]], max);
        }
        // Calc BG per DT
        val = max - getBgPerNP() / indices.length;
      }

      case DURATION -> {
        int startIdx = indices[0];
        int stopIdx = indices[indices.length - 1];

        // if event includes the first reading
        if (startIdx == 0) {
          stopIdx++;
          // rare edge case where all data is the event...
          if (stopIdx > time.length - 1) {
            val = 1E6 * dwellTime;
            break;
          }
        } else {
          // if standard event or last event
          startIdx--;
        }
        double start = time[startIdx];
        double endTime = time[stopIdx];
        double diff = endTime - start;
        val = 1E6 * diff;
      }
      case BACKGROUND_PER_NP -> val = getBgPerNP();
      case NO_OF_POINTS -> val = indices.length;

      case ASYMMETRY_FACTOR -> val = AnalysisUtils.computeAsymmetryFactor(indices.length,
          startInclusive, endInclusive, getPeak());

      case NO_OF_EVENTS -> val = 1;
      case CENTER_TIME -> val = time[getCenter()];
    }
    return val;
  }

  /**
   * Always adds 1 data point in order to  be able to draw a line at start and end.
   */
  @Override
  public TISeries getProfile() {
    TISeries data = new TISeriesRAM();

    if (!isEmpty()) {

      /*
       If we want to plot simulated data, do not use the potentially time-roi cut series.
       So far, we do not implement filtering the simulated events by a time roi
       and since the indices of the simulated events all refer to the original series,
       we would get totally wrong event indices.
       */
      TISeries tiSeries = collection.getCheckedTISeries();

      int seriesSize = tiSeries.size();
      double[] time = tiSeries.getTime();
      double[] mainSignal = tiSeries.getIntensity();

      // Cap to yield inclusive indices
      int start = Math.max(0, getStartIndexInclusive() - 1);
      int end = Math.min(seriesSize - 1, getEndIndexInclusive() + 1);

      List<Double> xData = new ArrayList<>();
      List<Double> yData = new ArrayList<>();

      for (int i = start; i <= end; i++) {
        xData.add(time[i]);
        yData.add(mainSignal[i]);
      }

      data = new TISeriesRAM(xData, yData);
    }
    return data;
  }

  @Override
  public TISeries getLogProfile() {
    TISeries data = new TISeriesRAM();

    if (!isEmpty()) {

      /*
       If we want to plot simulated data, do not use the potentially time-roi cut series.
       So far, we do not implement filtering the simulated events by a time roi
       and since the indices of the simulated events all refer to the original series,
       we would get totally wrong event indices.
       */
      TISeries tiSeries = collection.getCheckedTISeries();

      int seriesSize = tiSeries.size();
      double[] time = tiSeries.getTime();
      double[] mainSignal = tiSeries.getIntensity();

      // Cap to yield inclusive indices
      int start = Math.max(0, getStartIndexInclusive() - 1);
      int end = Math.min(seriesSize - 1, getEndIndexInclusive() + 1);

      List<Double> xData = new ArrayList<>();
      List<Double> yData = new ArrayList<>();

      for (int i = start; i <= end; i++) {
        xData.add(time[i]);
        yData.add(Math.log10(1+mainSignal[i]));
      }

      data = new TISeriesRAM(xData, yData);
    }
    return data;
  }

  @Override
  public TISeries getPreviousDP(int preview) {
    TISeries data = new TISeriesRAM();

    if (!isEmpty()) {

      /*
       If we want to plot simulated data, do not use the potentially time-roi cut series.
       So far, we do not implement filtering the simulated events by a time roi
       and since the indices of the simulated events all refer to the original series,
       we would get totally wrong event indices.
       */
      TISeries tiSeries = collection.getCheckedTISeries();

      int seriesSize = tiSeries.size();
      double[] time = tiSeries.getTime();
      double[] mainSignal = tiSeries.getIntensity();

      // Cap to yield inclusive indices
      int start = Math.max(0, getStartIndexInclusive() - 1 - preview);
      int end = Math.min(seriesSize - 1, getStartIndexInclusive() - 1);

      List<Double> xData = new ArrayList<>();
      List<Double> yData = new ArrayList<>();

      for (int i = start; i <= end; i++) {
        xData.add(time[i]);
        yData.add(mainSignal[i]);
      }

      data = new TISeriesRAM(xData, yData);
    }
    return data;
  }

  @Override
  public TISeries getFollowingDP(int preview) {
    TISeries data = new TISeriesRAM();

    if (!isEmpty()) {

      /*
       If we want to plot simulated data, do not use the potentially time-roi cut series.
       So far, we do not implement filtering the simulated events by a time roi
       and since the indices of the simulated events all refer to the original series,
       we would get totally wrong event indices.
       */
      TISeries tiSeries = collection.getCheckedTISeries();

      int seriesSize = tiSeries.size();
      double[] time = tiSeries.getTime();
      double[] mainSignal = tiSeries.getIntensity();

      // Cap to yield inclusive indices
      int start = Math.max(0, getEndIndexInclusive() + 1);
      int end = Math.min(seriesSize - 1, getEndIndexInclusive() + 1 + preview);

      List<Double> xData = new ArrayList<>();
      List<Double> yData = new ArrayList<>();

      for (int i = start; i <= end; i++) {
        xData.add(time[i]);
        yData.add(mainSignal[i]);
      }

      data = new TISeriesRAM(xData, yData);
    }
    return data;
  }

  @Override
  public TISeries getLogPreviousDP(int preview) {
    TISeries data = new TISeriesRAM();

    if (!isEmpty()) {

      /*
       If we want to plot simulated data, do not use the potentially time-roi cut series.
       So far, we do not implement filtering the simulated events by a time roi
       and since the indices of the simulated events all refer to the original series,
       we would get totally wrong event indices.
       */
      TISeries tiSeries = collection.getCheckedTISeries();

      int seriesSize = tiSeries.size();
      double[] time = tiSeries.getTime();
      double[] mainSignal = tiSeries.getIntensity();

      // Cap to yield inclusive indices
      int start = Math.max(0, getStartIndexInclusive() - 1 - preview);
      int end = Math.min(seriesSize - 1, getStartIndexInclusive() - 1);

      List<Double> xData = new ArrayList<>();
      List<Double> yData = new ArrayList<>();

      for (int i = start; i <= end; i++) {
        xData.add(time[i]);
        yData.add(Math.log10(1+mainSignal[i]));
      }

      data = new TISeriesRAM(xData, yData);
    }
    return data;
  }

  @Override
  public TISeries getLogFollowingDP(int preview) {
    TISeries data = new TISeriesRAM();

    if (!isEmpty()) {

      /*
       If we want to plot simulated data, do not use the potentially time-roi cut series.
       So far, we do not implement filtering the simulated events by a time roi
       and since the indices of the simulated events all refer to the original series,
       we would get totally wrong event indices.
       */
      TISeries tiSeries = collection.getCheckedTISeries();

      int seriesSize = tiSeries.size();
      double[] time = tiSeries.getTime();
      double[] mainSignal = tiSeries.getIntensity();

      // Cap to yield inclusive indices
      int start = Math.max(0, getEndIndexInclusive() + 1);
      int end = Math.min(seriesSize - 1, getEndIndexInclusive() + 1 + preview);

      List<Double> xData = new ArrayList<>();
      List<Double> yData = new ArrayList<>();

      for (int i = start; i <= end; i++) {
        xData.add(time[i]);
        yData.add(Math.log10(1+mainSignal[i]));
      }

      data = new TISeriesRAM(xData, yData);
    }
    return data;
  }

  @Override
  public String getLabel() {

    String label;

    TISeries tiSeries = collection.getCheckedTISeries();

    double grossArea = get(EventParameter.AREA, tiSeries.getTime(), tiSeries.getIntensity());
    double netArea = get(EventParameter.NET_AREA, tiSeries.getTime(), tiSeries.getIntensity());
    label = SnF.doubleToString(netArea, NF.D1C1, NF.D1C1Exp) +
        "\n(" + SnF.doubleToString(grossArea, NF.D1C1, NF.D1C1Exp) + ")";

    return label;
  }
}
