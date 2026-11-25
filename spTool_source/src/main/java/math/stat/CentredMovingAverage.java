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

package math.stat;

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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ArrUtils;

/// CLAUDE SONNET 5.5 'High' based on my ShiftingMovingAverage implementation.
public class CentredMovingAverage implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(CentredMovingAverage.class);

  // Window width in samples. The window is centred on each point, so it reaches h = period / 2
  // samples to the left and h to the right. An even period therefore acts like period + 1.
  private final int period;

  public CentredMovingAverage(int period) {
    if (period < 1) {
      LOGGER.info("Period for moving average should be at least 1");
    }
    this.period = Math.max(1, period);
  }


  public List<Double> getMean(List<Double> data) {
    return ArrUtils.arrToList(getMean(ArrUtils.doubleListToArr(data)));
  }

  // Centred mean, result[i] belongs to data[i] (no lag). Fast: O(n), independent of the period.
  // n = number of samples, i = index of the point being smoothed, h = half width of the window,
  // r = half width actually used for point i (h, but smaller near the ends of the array).
  public double[] getMean(double[] data) {
    if (data == null) {
      return new double[0];
    }
    int n = data.length;
    double[] smoothed = new double[n];
    int h = period / 2;

    // prefix[k] = data[0] + ... + data[k-1], prefix[0] = 0.
    // The sum of any window data[a..b] is then prefix[b + 1] - prefix[a], a single subtraction.
    double[] prefix = new double[n + 1];
    for (int k = 0; k < n; k++) {
      prefix[k + 1] = prefix[k] + data[k];
    }

    for (int i = 0; i < n; i++) {
      // Window is i-r .. i+r. r is cut down near the ends so the window stays symmetric around i
      // (= no shift) and does not leave the array. The window holds 2 * r + 1 samples.
      int r = Math.min(h, Math.min(i, n - 1 - i));
      smoothed[i] = (prefix[i + r + 1] - prefix[i - r]) / (2 * r + 1);
    }
    return smoothed;
  }


  public List<Double> get(List<Double> data, MeasureOfStat measure) {
    return ArrUtils.arrToList(get(ArrUtils.doubleListToArr(data), measure));
  }

  // Centred version of any measure (median, ...), result[i] belongs to data[i] (no lag).
  // Slower than getMean: O(n * period), as every window is built and handed to the measure.
  // n, i, h, r as in getMean; j runs over the window of point i.
  public double[] get(double[] data, MeasureOfStat measure) {
    if (data == null) {
      return new double[0];
    }
    int n = data.length;
    double[] smoothed = new double[n];
    int h = period / 2;

    for (int i = 0; i < n; i++) {
      // Same symmetric window i-r .. i+r as in getMean.
      int r = Math.min(h, Math.min(i, n - 1 - i));
      // Window of a single value (first/last sample): measure is trivial/undefined, keep raw value.
      if (r == 0) {
        smoothed[i] = data[i];
        continue;
      }
      List<Double> values = new ArrayList<>(2 * r + 1);
      for (int j = i - r; j <= i + r; j++) {
        values.add(data[j]);
      }
      smoothed[i] = measure.calc(values);
    }
    return smoothed;
  }

}