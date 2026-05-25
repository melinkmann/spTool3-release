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

package dataModelNew;

import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import util.ArrUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DTISeriesRAM implements Serializable, TISeries {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final double dwellTime; // dwell time
  private final boolean isInteger;
  private final double[] intensity;
  private final List<int[]> selectedIndices;

  private double mean;
  private double median;
  private double sd;
  private double madSD;

  // Dummy
  public DTISeriesRAM() {
    this.intensity = new double[0];
    this.dwellTime = 0;
    this.isInteger = true;
    this.selectedIndices = new ArrayList<>();
    this.mean = 0;
    this.median = 0;
    this.sd = 0;
    this.madSD = 0;
  }

  // For conversion: Not a copy constructor! Points to the same arrays.
  public DTISeriesRAM(DTISeriesHDD tiSeries) {
    this.intensity = tiSeries.getOriginalIntensity();
    this.dwellTime = tiSeries.getDT();
    this.isInteger = TISeries.checkInteger(this.intensity);
    this.selectedIndices = tiSeries.getSelectedIndices();
    //
    this.mean = tiSeries.getMeanIntensity();
    this.median = tiSeries.getMedianIntensity();
    this.sd = tiSeries.getSD();
    this.madSD = tiSeries.getMadSD();
  }
  public DTISeriesRAM(double dwellTime, double[] intensity) {
    this.intensity = intensity;
    this.dwellTime = dwellTime;
    this.isInteger = TISeries.checkInteger(intensity);
    this.selectedIndices = new ArrayList<>();
    //
    updateStatistics(getIntensity());
  }

  // Copy
  public DTISeriesRAM(double dwellTime, double[] intensity, boolean isInteger,
                      List<int[]> selectedIndices,
                      double mean, double median, double sd, double madSD) {
    this.dwellTime = dwellTime;
    this.isInteger = isInteger;
    this.intensity = ArrUtils.copy(intensity);
    this.selectedIndices = new ArrayList<>();
    for (int[] idxRange : selectedIndices) {
      this.selectedIndices.add(ArrUtils.copy(idxRange));
    }
    this.mean = mean;
    this.median = median;
    this.sd = sd;
    this.madSD = madSD;
  }

  @Override
  public TISeries copy() {
    return new DTISeriesRAM(dwellTime, intensity, isInteger, selectedIndices, mean, median, sd, madSD);
  }

  private void updateStatistics(double[] intensity) {
    this.mean = MeasureOfLocation.MEAN.calc(intensity);
    this.median = MeasureOfLocation.MEDIAN.calc(intensity);
    this.sd = MeasureOfSpread.SD.calc(intensity);
    this.madSD = MeasureOfSpread.MAD.calc(intensity);
  }

  public void setSelectedIndices(List<int[]> selectedIndices) {
    this.selectedIndices.clear();
    this.selectedIndices.addAll(selectedIndices);
    updateStatistics(getIntensity());
  }

  public List<int[]> getSelectedIndices() {
    return selectedIndices;
  }

  @Override
  public double[] getTime() {
    double[] intensity = getIntensity();
    double[] time = new double[intensity.length];
    if (selectedIndices.isEmpty()) {
      for (int i = 0; i < intensity.length; i++) {
        time[i] = (i + 1) * dwellTime;
      }
    } else {
      int pos = 0;
      for (int[] range : selectedIndices) {
        for (int i = range[0]; i <= range[1]; i++) {
          time[pos++] = (i + 1) * dwellTime;
        }
      }
    }
    return time;
  }

  @Override
  public double[] getTimeDifferences() {
    double[] result = ArrUtils.fillArray(dwellTime, size());
    return result;
  }

  @Override
  public double[] getIntensity() {
    double[] result;
    if (selectedIndices.isEmpty()) {
      result = intensity;
    } else {
      int totalLength = 0;
      for (int[] range : selectedIndices) {
        totalLength += range[1] - range[0] + 1;
      }
      result = new double[totalLength];
      int pos = 0;
      for (int[] range : selectedIndices) {
        for (int i = range[0]; i <= range[1]; i++) {
          result[pos++] = intensity[i];
        }
      }
    }
    return result;
  }

  public double[] getOriginalIntensity() {
    return intensity;
  }

    public int size() {
    int result;
    if (selectedIndices.isEmpty()) {
      result = intensity.length;
    } else {
      result = 0;
      for (int[] range : selectedIndices) {
        result += range[1] - range[0] + 1;
      }
    }
    return result;
  }

  public double getMeanIntensity() {
    return mean;
  }

  public double getMedianIntensity() {
    return median;
  }

  @Override
  public double getSD() {
    return sd;
  }

  @Override
  public double getMadSD() {
    return madSD;
  }

  @Override
  public double getFirstTimeStamp() {
    int result;
    if (selectedIndices.isEmpty()) {
      result = 1;
    } else {
      result = selectedIndices.get(0)[0] + 1;
    }
    return result * dwellTime;
  }

  @Override
  public double getLastTimeStamp() {
    int result;
    if (selectedIndices.isEmpty()) {
      result = intensity.length;
    } else {
      result = selectedIndices.get(selectedIndices.size() - 1)[1] + 1;
    }
    return result * dwellTime;
  }

  @Override
  public double getDuration() {
    return size() * dwellTime;
  }

  @Override
  public boolean isInteger() {
    return isInteger;
  }


  @Override
  public double getDT() {
    return dwellTime;
  }

}
