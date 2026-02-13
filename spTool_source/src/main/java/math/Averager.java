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

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import math.stat.MeasureOfStat;
import math.stat.PreFilter;
import util.ArrUtils;

public class Averager {

  private final double widthSec;
  private final double finalTimeStamp;
  double[] newTime;
  double[] newDataCTS;

  /**
   * @param data UNIT: counts per second are returned!
   */


  /**
   * @param data UNIT: counts per second are returned!
   */
  public Averager(double widthSec, double[] time, double[] data,
      MeasureOfStat measure, PreFilter preFilter) {
    this.widthSec = widthSec;

    this.finalTimeStamp = time[time.length - 1];

    // Create new time frame
    this.newTime = getTime(widthSec);
    this.newDataCTS = new double[newTime.length];

    // Build map to group into new DTs
    NavigableMap<Double, Double> map = new TreeMap<>();
    for (int i = 0; i < time.length; i++) {
      map.put(time[i], data[i]);
    }

    double start = 0;
    for (int i = 0; i < newTime.length; i++) {
      double end = newTime[i];
      // no zero time stamp (start not inclusive)
      NavigableMap<Double, Double> subMap = map.subMap(start, false, end, true);
      if (!subMap.isEmpty()) {

        /*
         1. Outlier test only within the subunits or else SD -> INF
         2. Outlier testing BEFORE averaging!
         3. Note: No need to keep track of the corresponding time stamps as these are "included"
            in the subMap. If there are no data points left after filtering, the default value (0)
            is actually correct.
         */
        List<Double> filtered = preFilter.filter(new ArrayList<>(subMap.values()));

        // returns value in units of cts per original DT
        double val = measure.calc(filtered);

        boolean calcCPS = false;

        if (calcCPS) {
          double[] timeInMap = ArrUtils.doubleListToArr(subMap.keySet());
          List<Double> differences = new ArrayList<>();
          for (int o = 1; o < timeInMap.length; o++) {
            differences.add(timeInMap[o] - timeInMap[o - 1]);
          }
          double actualDT = differences.stream()
              .mapToDouble(Double::doubleValue)
              .average()
              .orElse(1);

          //convert to cts per new DT
          newDataCTS[i] = val / actualDT;
        }

        newDataCTS[i] = val;
      }
      start = end;
    }
  }

  public double[] getCenterTime() {
    double[] centers = new double[newTime.length];
    for (int i = 0; i < newTime.length; i++) {
      centers[i] = newTime[i] - widthSec / 2;
    }
    return centers;
  }

  public double[] getDataCTS() {
    return newDataCTS;
  }

  private double[] getTime(double widthSec) {
    final List<Double> newTime = new ArrayList<>();
    double currentTime = 0;
    // Calc and add while still smaller:
    while (currentTime < finalTimeStamp) {
      currentTime += widthSec;
      newTime.add(currentTime);
    }
    return ArrUtils.doubleListToArr(newTime);
  }

}
