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

import analysis.BaselineGenerator;
import java.util.Arrays;
import util.ArrUtils;

public class LocalPeakLikelihood {

  /*
  Let us refine everything a bit: What I want to do is:
  I have time resolved data for 250 isotopes (ICP-MS mass spectrometry).
  Each isotope has an evenly-spaced, time-resolved intensity data reading.
  I want to so some clustering using UMAP on the data.
  Now, in addition to the existing raw data, I want to feed in some scores
  for reach data point and isotope ( Note: Only within each isotope time series not across!).
  These scores shall reflect whether the data point likely is part of a peak with an expected
  number of points per peak of 3-10. One important detail: The peak shape may look very, very poor,
  which is why I cannot run a typical peak search. It is more of a statistical argument
  that is unlikely to find a high density of high intensity data points close to each other.
  May data is a mix of gaussian and poisson statistics.
   */

  /**
   * Computes a peak likelihood score using large local threshold estimation with MAD-based
   * filtering.
   *
   * @param data        Time series data
   * @param shortWindow Half-size for local peak group check (e.g., 3 → 7-point window)
   * @param longWindow  Half-size for threshold estimation window (e.g., 100 → 201-point window)
   * @param k           Threshold offset: median + k * MAD (e.g. 0.1)
   * @return Scores per point [0.0 – 1.0]
   */
  public static double[] localPeakLikelihoodConservative(double[] data, int shortWindow,
      int longWindow, double k) {
    int n = data.length;
    double[] scores = new double[n];

    for (int i = 0; i < n; i++) {
      // === Long window for threshold estimation
      int longStart = Math.max(0, i - longWindow);
      int longEnd = Math.min(n, i + longWindow + 1);
      double[] longWindowData = Arrays.copyOfRange(data, longStart, longEnd);

      double median = MeasureOfLocation.MEDIAN.calc(longWindowData);
      double mad = MeasureOfSpread.MAD.calc(longWindowData);

      // Filter out outliers (|x - median| > 3 * MAD)
      double[] cleaned = BaselineGenerator.removeOutliers(longWindowData, median - mad,
          median + mad);

      if (cleaned.length == 0) {
        scores[i] = 0.0;
        continue;
      }

      double cleanMedian = MeasureOfLocation.MEDIAN.calc(cleaned);
      double cleanMAD = MeasureOfSpread.MAD.calc(cleaned);

      double threshold = cleanMedian + k * cleanMAD;

      // === Short window for scoring
      int shortStart = Math.max(0, i - shortWindow);
      int shortEnd = Math.min(n, i + shortWindow + 1);
      double[] shortWindowData = Arrays.copyOfRange(data, shortStart, shortEnd);

      int countAbove = 0;
      for (double val : shortWindowData) {
        if (val > threshold) {
          countAbove++;
        }
      }

      scores[i] = (double) countAbove / shortWindowData.length;
    }

    scores=ArrUtils.normalizeByMaximumTimesFactor(scores, 1);
    return scores;
  }

}
