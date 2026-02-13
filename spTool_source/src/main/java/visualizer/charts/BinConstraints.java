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

package visualizer.charts;

import com.google.common.math.DoubleMath;
import java.util.ArrayList;
import java.util.List;
import processing.options.BinWidthEstimator;
import util.ArrUtils;

public class BinConstraints {

  // If bin width is too small for data set, JFree's bin counting algorithm fails.
  public static final int MAX_NO_OF_BINS = 10_000;
  public static final double MIN_BIN_WIDTH = 1E-12; // if m = 1 ag, and [y] = µg --> 18-6 = 1E-12

  private final double min;
  private final double max;
  private final int noOfBins;

  private double width;

  public BinConstraints(List<double[]> data, BinWidthEstimator estimator, double customWidth) {

    // Make a copy, else we cannot removeIf safely as the list is needed elsewhere.
    List<double[]> dataCopy = new ArrayList<>(data);

    // Remove all empty data and n=1 as computing on these may lead to unwanted behavior (e.g., max-min=0 for n=1).
    dataCopy.removeIf(d -> d.length == 0);

    // When simulations are loaded we may face arrays with zeros only!
    dataCopy.removeIf(ArrUtils::isZero);

    // Avoid zero b/c division.
    double bwCandidate = Double.MAX_VALUE;
    for (double[] arr : dataCopy) {
      double bw;

      // custom case? we can skip most calculations!
      if (estimator.equals(BinWidthEstimator.CUSTOM)) {
        bw = customWidth;
      } else {
        // arrays with n=1 are difficult to estimate
        if (arr.length > 1) {
          bw = estimator.getBinWidth(arr, customWidth);
          // avoid zero bin width result from ill-configured data set
          if (DoubleMath.fuzzyEquals(bw, 0, 1E-13)) {
            double min = ArrUtils.getMin(arr);
            double max = ArrUtils.getMax(arr);
            double diff = max - min;
            if (DoubleMath.fuzzyEquals(diff, 0, 1E-13)) {
              // min=max:
              // "difference" from zero to min (zero is only other remaining anchor point)
              bw = Math.sqrt(min);
            } else {
              // min!=max
              bw = diff / Math.sqrt(arr.length);
            }
          }
        } else {
          bw = Math.sqrt(arr[0]);
        }
      }
      // strive for minimum bin width
      bwCandidate = Math.min(bwCandidate, bw);
    }

    // clean up: this only triggers if ALL selected data sets are ill-configured (or n=1)
    width = bwCandidate;
    if (width < MIN_BIN_WIDTH) {
      width = MIN_BIN_WIDTH;
    }

    if (width >= Double.MAX_VALUE - 1) {
      width = MIN_BIN_WIDTH;
    }

    this.min = dataCopy.stream()
        .map(ArrUtils::getMin)
        .mapToDouble(Double::doubleValue)
        .min()
        .orElse(0);

    double preciseMax = dataCopy.stream()
        .map(ArrUtils::getMax)
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(min + 1);

    // if min = max (i.e., likely only one value), avoid having a zero at max-min
    if (preciseMax == min){
      preciseMax++;
    }

    this.noOfBins = Math.min(MAX_NO_OF_BINS, (int) Math.ceil((preciseMax - min) / width));

    this.max = min + noOfBins * width;
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public int getNoOfBins() {
    return noOfBins;
  }

  public double getWidth() {
    return width;
  }
}

/* [DONE]
 * Implement percentile based overflow bin; For that: calc percentiles!
 * <p>
 * UPDATE: We only need this for events, i.e., rare things compared to all data points, --> Guava is
 * fine...
 * <p>
 * For that: find a faster method than the Apache.. e.g. quickselect
 * https://github.com/mpollmeier/Selection-Algorithms/blob/master/src/main/java/com/michaelpollmeier/selection/QuickSelect.
 * or https://guava.dev/releases/21.0/api/docs/com/google/common/math/Quantiles.html
 * https://github.com/jalkanen/speed4j/blob/master/src/main/java/com/ecyrd/speed4j/util/Percentile.java
 * <p>
 * CONSIDER: Creating own HistogramDataset, e.g. backup on the  drive in a Storage
 * https://www.jfree.org/jfreechart/api/javadoc/org/jfree/data/statistics/HistogramDataset.html#HistogramDataset--
 */