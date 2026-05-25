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

import core.SpTool3Main;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import util.ArrUtils;
import util.storage.MemoryMapStorage;
import util.storage.StorageUtils;

import java.io.Serial;
import java.lang.ref.SoftReference;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

public class DTISeriesHDD implements TISeries {


  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final double dwellTime; // dwell time
  private final DoubleBuffer intensity;
  private final List<int[]> selectedIndices;
  private final boolean isInteger;
  // caches to keep stuff in RAM if loaded once. Likely, during processing, this helps for speed.
  private transient SoftReference<double[]> timeCache;
  private transient SoftReference<double[]> intensityCache;

  private double mean;
  private double median;
  private double sd;
  private double madSD;

  // Dummy
  public DTISeriesHDD() {
    this.intensity = DoubleBuffer.wrap(new double[0]);
    this.dwellTime = 0;
    this.selectedIndices = new ArrayList<>();
    this.isInteger = true;
    this.timeCache = new SoftReference<>(null);
    this.intensityCache = new SoftReference<>(null);
    this.mean = 0;
    this.median = 0;
    this.sd = 0;
    this.madSD = 0;
  }

  // For conversion: Not a copy constructor, points to same arrays in weak reference!
  public DTISeriesHDD(DTISeriesRAM tiSeries) {
    double[] intensityArr = tiSeries.getOriginalIntensity();
    this.dwellTime = tiSeries.getDT();
    this.intensity = StorageUtils.storeToDoubleBuffer(getStorage(), intensityArr);
    this.selectedIndices = tiSeries.getSelectedIndices();
    this.isInteger = TISeries.checkInteger(intensityArr);
    this.timeCache = new SoftReference<>(null);
    this.intensityCache = new SoftReference<>(null);
    //
    this.mean = tiSeries.getMeanIntensity();
    this.median = tiSeries.getMedianIntensity();
    this.sd = tiSeries.getSD();
    this.madSD = tiSeries.getMadSD();
  }

  public DTISeriesHDD(double dwellTime, double[] intensityArr) {
    this.intensity = StorageUtils.storeToDoubleBuffer(getStorage(), intensityArr);
    this.dwellTime = dwellTime;
    this.selectedIndices = new ArrayList<>();
    this.isInteger = TISeries.checkInteger(intensityArr);
    this.timeCache = new SoftReference<>(getTime());
    this.intensityCache = new SoftReference<>(ArrUtils.copy(intensityArr));
    //
    updateStatistics(getIntensity());
  }

  // Copy: Buffer "copy" by making a new one...
  public DTISeriesHDD( double dwellTime, double[] intensityArr,List<int[]> selectedIndices, boolean isInteger,
                      double mean, double median, double sd, double madSD) {
    this.dwellTime = dwellTime;
    this.intensity = StorageUtils.storeToDoubleBuffer(getStorage(), intensityArr);
    this.selectedIndices = new ArrayList<>();
    for (int[] idxRange : selectedIndices) {
      this.selectedIndices.add(ArrUtils.copy(idxRange));
    }
    this.isInteger = isInteger;
    this.timeCache = new SoftReference<>(ArrUtils.copy(getTime()));
    this.intensityCache = new SoftReference<>(ArrUtils.copy(intensityArr));
    //
    this.mean = mean;
    this.median = median;
    this.sd = sd;
    this.madSD = madSD;
  }

  @Override
  public TISeries copy() {
    return new DTISeriesHDD(dwellTime,getIntensity(),selectedIndices, isInteger,
        mean, median, sd, madSD);
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
    this.intensityCache = new SoftReference<>(null);
    this.timeCache = new SoftReference<>(null);
    updateStatistics(getIntensity());
  }

  public List<int[]> getSelectedIndices() {
    return selectedIndices;
  }

  public double[] getTime() {
    double[] timeArr;
    if (timeCache != null && timeCache.get() != null) {
      timeArr = timeCache.get();
    } else {
      if (selectedIndices.isEmpty()) {
        timeArr = new double[size()];
        for (int i = 0; i < size(); i++) {
          timeArr[i] = (i + 1) * dwellTime;
        }
      } else {
        timeArr = new double[size()];
        int pos = 0;
        for (int[] range : selectedIndices) {
          for (int i = range[0]; i <= range[1]; i++) {
            timeArr[pos++] = (i + 1) * dwellTime;
          }
        }
      }
      timeCache = new SoftReference<>(timeArr);
    }
    return timeArr;
  }

  @Override
  public double[] getTimeDifferences() {
    double[] diff = ArrUtils.fillArray(dwellTime, size());
    return diff;
  }

  public double[] getIntensity() {
    double[] intensityArr;
    if (intensityCache != null && intensityCache.get() != null) {
      intensityArr = intensityCache.get();
    } else {
      if (selectedIndices.isEmpty()) {
        intensityArr = StorageUtils.getArray(intensity);
      } else {
        int totalLength = 0;
        for (int[] range : selectedIndices) {
          totalLength += range[1] - range[0] + 1;
        }
        double[] fullArr = StorageUtils.getArray(intensity);
        intensityArr = new double[totalLength];
        int pos = 0;
        for (int[] range : selectedIndices) {
          for (int i = range[0]; i <= range[1]; i++) {
            intensityArr[pos++] = fullArr[i];
          }
        }
      }
      intensityCache = new SoftReference<>(intensityArr);
    }
    return intensityArr;
  }

  public double[] getOriginalIntensity() {
    return StorageUtils.getArray(intensity);
  }


  public int size() {
    int result;
    if (selectedIndices.isEmpty()) {
      result = intensity.capacity();
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
      result = intensity.capacity();
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
  public double getDT() {
    return dwellTime;
  }

  @Override
  public boolean isInteger() {
    return isInteger;
  }

  private MemoryMapStorage getStorage() {
    return SpTool3Main.getRunTime().getRawBufferStorage();
  }
}
