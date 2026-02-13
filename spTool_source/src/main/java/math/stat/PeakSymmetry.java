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

import util.ArrUtils;

public class PeakSymmetry {


  /**
   * Modified from chatGPT:
   * Calculates the Asymmetry Factor (AF) for a sorted peak array. Assumes equally spaced data
   * points.
   *
   * @param peakValues sorted array of peak intensities (assumed to be peak shape)
   * @return asymmetry factor (AF)
   */
  public static double calculateAsymmetryFactor(double[] peakValues) {
    int n = peakValues.length;

    double result = 0;

    if (n > 0) {

      // 1. Find peak apex index and height
      int apexIndex = ArrUtils.getIdxAtMax(peakValues);
      double height = ArrUtils.getMax(peakValues);

      // 2. Calculate 10% peak height threshold
      double threshold = height * 0.1;

      // 3. Find leading edge index at 10% height (search backward from apex)
      int leadingEdgeIndex = apexIndex;
      while (leadingEdgeIndex > 0 && peakValues[leadingEdgeIndex] > threshold) {
        leadingEdgeIndex--;
      }

      // For better accuracy, linear interpolation between points
      double a = interpolateDistance(peakValues, leadingEdgeIndex, leadingEdgeIndex + 1, threshold,
          apexIndex);

      // 4. Find trailing edge index at 10% height (search forward from apex)
      int trailingEdgeIndex = apexIndex;
      while (trailingEdgeIndex < n - 1 && peakValues[trailingEdgeIndex] > threshold) {
        trailingEdgeIndex++;
      }

      double b = interpolateDistance(peakValues, trailingEdgeIndex - 1, trailingEdgeIndex,
          threshold, apexIndex);

      // 5. Calculate distances a and b as number of points between apex and edges
      // Here we use the interpolated fractional distance to increase precision
      // Distance a: apexIndex - leadingEdge (should be positive)
      // Distance b: trailingEdge - apexIndex (should be positive)

      double distanceA = apexIndex - a;
      double distanceB = b - apexIndex;

      if (distanceA != 0) {
        result = distanceB / distanceA;
      }

    }
    return result;
  }

  /**
   * Performs linear interpolation between two points to find fractional index where the peak
   * crosses the threshold.
   *
   * @param peakValues the array of intensities
   * @param index1     first index (below threshold)
   * @param index2     second index (above threshold)
   * @param threshold  the intensity threshold
   * @param apexIndex  the apex index (for reference only)
   * @return fractional index where value crosses threshold
   */
  private static double interpolateDistance(double[] peakValues, int index1, int index2,
                                            double threshold, int apexIndex) {
    double val1 = peakValues[index1];
    double val2 = peakValues[index2];

    if (val2 == val1) {
      return index1; // Avoid division by zero
    }

    // Linear interpolation formula:
    double frac = (threshold - val1) / (val2 - val1);
    return index1 + frac;
  }

}
