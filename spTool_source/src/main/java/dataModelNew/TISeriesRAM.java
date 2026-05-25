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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import util.ArrUtils;

public class TISeriesRAM implements Serializable, TISeries {


  @Serial
  private static final long serialVersionUID = 1_000_000L;


  private final double dwellTime; // dwell time
  private final boolean isInteger;
  private final double[] time;
  private final double[] intensity;

  private final double mean;
  private final double median;
  private final double sd;
  private final double madSD;

  // Dummy
  public TISeriesRAM() {
    this.time = new double[0];
    this.intensity = new double[0];
    this.dwellTime = 0;
    this.isInteger = true;
    this.mean = 0;
    this.median = 0;
    this.sd = 0;
    this.madSD = 0;
  }

  // For conversion: Not a copy constructor! Points to the same arrays.
  public TISeriesRAM(TISeries tiSeries) {
    this.time = tiSeries.getTime();
    this.intensity = tiSeries.getIntensity();
    this.dwellTime = calcDwellTime();
    this.isInteger = TISeries.checkInteger(this.intensity);
    //
    this.mean = tiSeries.getMeanIntensity();
    this.median = tiSeries.getMedianIntensity();
    this.sd = tiSeries.getSD();
    this.madSD = tiSeries.getMadSD();
  }

  public TISeriesRAM(double[] time, double[] intensity) {
    this.time = time;
    this.intensity = intensity;
    this.dwellTime = calcDwellTime();
    this.isInteger = TISeries.checkInteger(intensity);
    //
    this.mean = MeasureOfLocation.MEAN.calc(intensity);
    this.median = MeasureOfLocation.MEDIAN.calc(intensity);
    this.sd = MeasureOfSpread.SD.calc(intensity);
    this.madSD = MeasureOfSpread.MAD.calc(intensity);
  }

  public TISeriesRAM(double[] time, List<Double> intensity) {
    this.time = time;
    this.intensity = ArrUtils.doubleListToArr(intensity);
    this.dwellTime = calcDwellTime();
    this.isInteger = TISeries.checkInteger(this.intensity);
    //
    this.mean = MeasureOfLocation.MEAN.calc(intensity);
    this.median = MeasureOfLocation.MEDIAN.calc(intensity);
    this.sd = MeasureOfSpread.SD.calc(intensity);
    this.madSD = MeasureOfSpread.MAD.calc(intensity);
  }


  public TISeriesRAM(List<Double> time, List<Double> intensity) {
    this.time = ArrUtils.doubleListToArr(time);
    this.intensity = ArrUtils.doubleListToArr(intensity);
    this.dwellTime = calcDwellTime();
    this.isInteger = TISeries.checkInteger(this.intensity);
    //
    this.mean = MeasureOfLocation.MEAN.calc(intensity);
    this.median = MeasureOfLocation.MEDIAN.calc(intensity);
    this.sd = MeasureOfSpread.SD.calc(intensity);
    this.madSD = MeasureOfSpread.MAD.calc(intensity);
  }

  // Copy
  public TISeriesRAM(double dwellTime, boolean isInteger, double[] time, double[] intensity,
      double mean, double median,double sd,double madSD) {
    this.dwellTime = dwellTime;
    this.isInteger = isInteger;
    this.time = ArrUtils.copy(time);
    this.intensity = ArrUtils.copy(intensity);
    this.mean = mean;
    this.median = median;
    this.sd = sd;
    this.madSD = madSD;
  }

  @Override
  public TISeries copy() {
    return new TISeriesRAM(dwellTime, isInteger, time, intensity, mean, median, sd, madSD);
  }

  public double[] getTime() {
    return time;
  }

  @Override
  public double[] getTimeDifferences() {
    double[] diff = new double[1];
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
  }

  public double[] getIntensity() {
    return intensity;
  }

  public int size() {
    return Math.min(time.length, intensity.length);
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
    double last = 0;
    if (time.length > 0) {
      last = time[time.length - 1];
    }
    return last;
  }

  @Override
  public double getFirstTimeStamp() {
    double first = 0;
    if (time.length > 0) {
      first = time[0];
    }
    return first;
  }

  @Override
  public double getDuration() {
    return getLastTimeStamp() - getFirstTimeStamp() + getDT();
  }

  @Override
  public boolean isInteger() {
    return isInteger;
  }


  @Override
  public double getDT() {
    return dwellTime;
  }

  private double calcDwellTime() {
    double meanTimeDifference = 0;
    double[] differences = getTimeDifferences();
    meanTimeDifference = MeasureOfLocation.MEAN.calc(differences);
    return meanTimeDifference;
  }
}
