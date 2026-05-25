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
import java.io.Serial;
import java.lang.ref.SoftReference;
import java.nio.DoubleBuffer;
import java.util.List;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import util.ArrUtils;
import util.storage.MemoryMapStorage;
import util.storage.StorageUtils;

public class TISeriesHDD implements TISeries {


  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final DoubleBuffer time;
  private final DoubleBuffer intensity;
  private final double dwellTime; // dwell time
  private final boolean isInteger;
  private final double lastTimeStamp;
  private final double firstTimeStamp;
  // caches to keep stuff in RAM if loaded once. Likely, during processing, this helps for speed.
  private transient SoftReference<double[]> timeCache;
  private transient SoftReference<double[]> intensityCache;

  private final double mean;
  private final double median;
  private final double sd;
  private final double madSD;

  // Dummy
  public TISeriesHDD() {
    this.time = DoubleBuffer.wrap(new double[0]);
    this.intensity = DoubleBuffer.wrap(new double[0]);
    this.dwellTime = 0;
    this.lastTimeStamp = 0;
    this.firstTimeStamp = 0;
    this.isInteger = true;
    this.timeCache = new SoftReference<>(null);
    this.intensityCache = new SoftReference<>(null);
    this.mean = 0;
    this.median = 0;
    this.sd = 0;
    this.madSD = 0;
  }

  // For conversion: Not a copy constructor, points to same arrays in weak reference!
  public TISeriesHDD(TISeries tiSeries) {
    double[] timeArr = tiSeries.getTime();
    double[] intensityArr = tiSeries.getIntensity();
    this.time = StorageUtils.storeToDoubleBuffer(getStorage(), timeArr);
    this.intensity = StorageUtils.storeToDoubleBuffer(getStorage(), intensityArr);
    this.dwellTime = calcDwellTime(timeArr);
    this.lastTimeStamp = timeArr.length > 0 ? timeArr[timeArr.length - 1] : 0d;
    this.firstTimeStamp = timeArr.length > 0 ? timeArr[0] : 0d;
    this.isInteger = TISeries.checkInteger(intensityArr);
    this.timeCache = new SoftReference<>(ArrUtils.copy(timeArr));
    this.intensityCache = new SoftReference<>(ArrUtils.copy(intensityArr));
    //
    this.mean = tiSeries.getMeanIntensity();
    this.median = tiSeries.getMedianIntensity();
    this.sd = tiSeries.getSD();
    this.madSD = tiSeries.getMadSD();
  }

  public TISeriesHDD(double[] timeArr, double[] intensityArr) {
    this.time = StorageUtils.storeToDoubleBuffer(getStorage(), timeArr);
    this.intensity = StorageUtils.storeToDoubleBuffer(getStorage(), intensityArr);
    this.dwellTime = calcDwellTime(timeArr);
    this.lastTimeStamp = timeArr.length > 0 ? timeArr[timeArr.length - 1] : 0d;
    this.firstTimeStamp = timeArr.length > 0 ? timeArr[0] : 0d;
    this.isInteger = TISeries.checkInteger(intensityArr);
    this.timeCache = new SoftReference<>(ArrUtils.copy(timeArr));
    this.intensityCache = new SoftReference<>(ArrUtils.copy(intensityArr));
    //
    this.mean = MeasureOfLocation.MEAN.calc(intensityArr);
    this.median = MeasureOfLocation.MEDIAN.calc(intensityArr);
    this.sd = MeasureOfSpread.SD.calc(intensityArr);
    this.madSD = MeasureOfSpread.MAD.calc(intensityArr);
  }

  public TISeriesHDD(List<Double> timeList, List<Double> intensityList) {
    this(ArrUtils.doubleListToArr(timeList), ArrUtils.doubleListToArr(intensityList));
  }

  // Copy: Buffer "copy" by making a new one...
  public TISeriesHDD(double[] timeArr, double[] intensityArr, double dwellTime, boolean isInteger,
      double lastTimeStamp, double firstTimeStamp,
      double mean, double median,double sd,double madSD) {
    this.time = StorageUtils.storeToDoubleBuffer(getStorage(), timeArr);
    this.intensity = StorageUtils.storeToDoubleBuffer(getStorage(), intensityArr);
    this.dwellTime = dwellTime;
    this.isInteger = isInteger;
    this.lastTimeStamp = lastTimeStamp;
    this.firstTimeStamp = firstTimeStamp;
    this.timeCache = new SoftReference<>(ArrUtils.copy(timeArr));
    this.intensityCache = new SoftReference<>(ArrUtils.copy(intensityArr));
    //
    this.mean = mean;
    this.median = median;
    this.sd = sd;
    this.madSD = madSD;
  }

  @Override
  public TISeries copy() {
    return new TISeriesHDD(getTime(), getIntensity(), dwellTime, isInteger,
        lastTimeStamp, firstTimeStamp, mean, median,sd, madSD);
  }

  public double[] getTime() {
    double[] timeArr;
    if (timeCache != null && timeCache.get() != null) {
      timeArr = timeCache.get();
      //System.out.println("cached time");
    } else {
      timeArr = StorageUtils.getArray(time);
      timeCache = new SoftReference<>(timeArr);
    }
    return timeArr;
  }

  @Override
  public double[] getTimeDifferences() {
    double[] timeArr = getTime();
    double[] diff = new double[timeArr.length - 1];
    if (timeArr.length > 2) {
      diff = new double[timeArr.length - 1];
      // first time stamp may be unreliable in real data
      diff[0] = timeArr[2] - timeArr[1];
      diff[1] = timeArr[2] - timeArr[1];
      for (int i = 2; i < timeArr.length; i++) {
        diff[i - 1] = timeArr[i] - timeArr[i - 1];
      }
    }
    return diff;
  }

  private double[] getTimeDifferences(double[] time) {
    if (time.length > 0) {
      double[] diff = new double[time.length - 1];
      if (time.length > 2) {
        diff = new double[time.length - 1];
        // first time stamp may be unreliable in real data
        diff[0] = time[2] - time[1];
        diff[1] = time[2] - time[1];
        for (int i = 2; i < time.length; i++) {
          diff[i - 1] = time[i] - time[i - 1];
        }
      }
      return diff;
    } else {
      return new double[0];
    }
  }

  public double[] getIntensity() {
    double[] intensityArr;
    if (intensityCache != null && intensityCache.get() != null) {
      intensityArr = intensityCache.get();
      //System.out.println("cached intensity");
    } else {
      intensityArr = StorageUtils.getArray(intensity);
      intensityCache = new SoftReference<>(intensityArr);
    }
    return intensityArr;
  }

  public int size() {
    return Math.min(time.capacity(), intensity.capacity());
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
  public double getLastTimeStamp() {
    return lastTimeStamp;
  }

  @Override
  public double getFirstTimeStamp() {
    return firstTimeStamp;
  }

  @Override
  public double getDuration() {
    return getLastTimeStamp() - getFirstTimeStamp() + getDT();
  }

  @Override
  public double getDT() {
    return dwellTime;
  }

  private double calcDwellTime(double[] time) {
    double meanTimeDifference = 0;
    double[] differences = getTimeDifferences(time);
    meanTimeDifference = MeasureOfLocation.MEAN.calc(differences);
    return meanTimeDifference;
  }

  @Override
  public boolean isInteger() {
    return isInteger;
  }

  private MemoryMapStorage getStorage() {
    return SpTool3Main.getRunTime().getRawBufferStorage();
  }
}
