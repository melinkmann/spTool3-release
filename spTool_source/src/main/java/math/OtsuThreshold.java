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

import org.jfree.data.statistics.HistogramDataset;

public abstract class OtsuThreshold {


  /**
   * ChatGPT: Compute Otsu threshold for a HistogramDataset.
   *
   * @param histogramDataset the JFreeChart HistogramDataset
   * @param seriesIndex      index of the series to process
   * @return threshold value (x-coordinate of bin center)
   */
  public static double computeThreshold(HistogramDataset histogramDataset, int seriesIndex) {
    int itemCount = histogramDataset.getItemCount(seriesIndex);
    double thr = 0;
    if (itemCount > 0) {
      double[] barCenters = new double[itemCount];
      double[] barHeights = new double[itemCount];

      // Extract bin centers and heights
      for (int i = 0; i < itemCount; i++) {
        double startX = histogramDataset.getStartX(seriesIndex, i).doubleValue();
        double endX = histogramDataset.getEndX(seriesIndex, i).doubleValue();
        barCenters[i] = (startX + endX) / 2.0;
        barHeights[i] = histogramDataset.getY(seriesIndex, i).doubleValue();
      }

      // Compute Otsu threshold index
      int thresholdIndex = otsuThresholdIndex(barHeights);

      // Map to bin center value
      if (thresholdIndex >= 0) {
        thr = barCenters[thresholdIndex];
      }
    }
    return thr;
  }

  /**
   * ChatGPT: Compute Otsu threshold index for a histogram.
   *
   * @param barHeights histogram bin heights (double[], non-negative)
   * @return index of the threshold bin
   * @throws IllegalArgumentException if barHeights is null, empty, or all zeros
   */
  public static int otsuThresholdIndex(double[] barHeights) {
    int index = -1;
    if (barHeights != null && barHeights.length > 0) {

      int n = barHeights.length;
      double totalWeight = 0.0;
      double totalMean = 0.0;

      // ensure positive bar heights
      for (int i = 0; i < barHeights.length; i++) {
        barHeights[i] = Math.max(0, barHeights[i]);
      }

      for (int i = 0; i < n; i++) {
        double h = barHeights[i];
        totalWeight += h;
        totalMean += i * h;
      }

      if (totalWeight > 0) {

        double weightB = 0.0;
        double meanB = 0.0;
        double maxBetweenVar = -1.0;
        int thresholdIndex = 0;

        for (int t = 0; t < n; t++) {
          double h = barHeights[t];
          weightB += h;
          if (weightB == 0) continue;

          double weightF = totalWeight - weightB;
          if (weightF == 0) break;

          meanB += t * h;

          double meanBackground = meanB / weightB;
          double meanForeground = (totalMean - meanB) / weightF;

          double betweenVar =
              weightB * weightF * (meanBackground - meanForeground) * (meanBackground - meanForeground);

          if (betweenVar > maxBetweenVar) {
            maxBetweenVar = betweenVar;
            thresholdIndex = t;
          }
        }
        index = thresholdIndex;
      }
    }
    return index;
  }

}