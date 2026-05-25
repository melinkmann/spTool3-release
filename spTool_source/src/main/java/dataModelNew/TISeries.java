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

import java.io.Serializable;
import java.util.List;
import util.ArrUtils;


public interface TISeries extends XYSeries, Serializable {

  TISeries copy();

  double[] getTime();

  double[] getTimeDifferences();

  double[] getIntensity();

  int size();

  double getFirstTimeStamp();

  double getLastTimeStamp();

  double getDuration();

  double getDT();

  double getMeanIntensity();

  double getMedianIntensity();

  double getSD();

  double getMadSD();

  /**
   * Check whether QMS or TOF-like data.
   */
  boolean isInteger();

  @Override
  default double[] getX() {
    return getTime();
  }

  @Override
  default double[] getY() {
    return getIntensity();
  }

  public static boolean checkInteger(double[] y) {
    // look at 1E4 DP, check if their difference to the rounded value is off by more than 25%
    // Why 25%? Knowing that there are csv-precision and floating point arithmetic issues,
    // better give some margin.
    // only look at nonzero values. else: zeros are never rounded and the data appears to be integer.
    y = ArrUtils.nonzero(y);
    // calc length after non-zeroing
    int length = Math.min(10_000, y.length);
    double totalDifference = 0;
    for (int i = 0; i < length-1; i++) {
      totalDifference += Math.abs(Math.round(y[i]) - y[i]);
    }
    double meanDifference = totalDifference / length;
    return meanDifference < 0.15;
    /*
     For random doubles, expected value is 0.25 (mean of a uniform distribution of [0-0.5]
     */
  }
}
