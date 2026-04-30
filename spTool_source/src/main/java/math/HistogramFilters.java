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

import math.stat.MeasureOfLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.statistics.HistogramDataset;
import processing.options.BinWidthEstimator;
import smile.stat.distribution.KernelDensity;
import util.ArrUtils;

public class HistogramFilters {

  public static final Logger LOGGER = LogManager.getLogger(HistogramFilters.class);

  // TODO: Delete. I believe, kernel width would be too much of a problem.
  public static double computeValley(double[] data) {
    double value = 0;

    if (data.length > 2) {
      double bandwidth = BinWidthEstimator.silvermanRule(data) * 1.25d;
      KernelDensity kd = new KernelDensity(data, bandwidth);
      double maxVal = ArrUtils.getMax(data);

      // step size
      double minDataStep = Double.MAX_VALUE;
      for (int i = 1; i < data.length; i++) {
        double diff = data[i] - data[i - 1];
        minDataStep = Math.min(diff, minDataStep);
      }

      double stepSize = minDataStep / 10;
      double[] x = ArrUtils.fillArrayExclusive(0, 1.1 * maxVal, stepSize);
      double[] y = new double[x.length];
      for (int i = 0; i < x.length; i++) {
        y[i] = kd.p(x[i]);
      }
    }

    return value;
  }

  public static double findSplitPoint(HistogramDataset histogramDataset, int windowWidth) {
    double val = 0;

    // ensure uneven width
    if (windowWidth % 2 == 0) windowWidth++;

    int seriesIndex = 0;
    int itemCount = histogramDataset.getItemCount(seriesIndex);
    if (itemCount > 0) {
      double[] barMeasure = new double[itemCount];
      double[] barHeights = new double[itemCount];

      boolean useCentre = false;
      for (int i = 0; i < itemCount; i++) {
        double startX = histogramDataset.getStartX(seriesIndex, i).doubleValue();
        if (useCentre) {
          double endX = histogramDataset.getEndX(seriesIndex, i).doubleValue();
          barMeasure[i] = (startX + endX) / 2.0;
        } else {
          barMeasure[i] = startX;
        }
        barHeights[i] = histogramDataset.getY(seriesIndex, i).doubleValue();
      }

      // clamp window to array length, keep it odd
      int clampedWindow = Math.min(windowWidth, barMeasure.length);
      if (clampedWindow % 2 == 0) clampedWindow--;
      int half = clampedWindow / 2;

      if (barMeasure.length > clampedWindow) {
        for (int i = half; i < barMeasure.length - half; i++) {
          double prev = 0, past = 0;
          for (int j = 0; j < half; j++) {
            prev += barHeights[i - j - 1];
            past += barHeights[i + j + 1];
          }
          prev = (barHeights[i] + prev) / (double) (half + 1);
          past = (barHeights[i] + past) / (double) (half + 1);
          if (past > prev) {
            val = barMeasure[i];
            break;
          }
        }
      }
    }
    return val;
  }


  /**
   * ChatGPT: Compute threshold for a HistogramDataset.
   *
   * @param histogramDataset the JFreeChart HistogramDataset
   * @param seriesIndex      index of the series to process
   * @return threshold value (x-coordinate of bin center)
   */
  public static double computeLogDerivativeChangePointThreshold(HistogramDataset histogramDataset,
                                                                int seriesIndex, double z) {
    int itemCount = histogramDataset.getItemCount(seriesIndex);
    double thr = 0;
    if (itemCount > 0) {
      double[] barMeasure = new double[itemCount];
      double[] barHeights = new double[itemCount];

      /*
      Extract bin centers and heights:
      It seems, we should use start as point.
      Else, when looking at size calibrated data later,
      the beginning of the distr is cut even if we return first bar as hit
      (i.e., the lowest 3-4 bars in the size dimension are not accessible at all).
      Maybe consider making this a parameter? However, it destabilises the algorithm
      in that sense, that z=0 and Friedman-Diaconis seem slightly better suited..
       */
      boolean useCentre = false;
      for (int i = 0; i < itemCount; i++) {
        double startX = histogramDataset.getStartX(seriesIndex, i).doubleValue();
        if (useCentre) {
          double endX = histogramDataset.getEndX(seriesIndex, i).doubleValue();
          barMeasure[i] = (startX + endX) / 2.0;
        } else {
          barMeasure[i] = startX;
        }
        barHeights[i] = histogramDataset.getY(seriesIndex, i).doubleValue();
      }

      // Compute Otsu threshold index
      int thresholdIndex = logDerivativeChangePoint(barHeights, z);

      // Map to bin center value
      if (thresholdIndex >= 0) {
        thr = barMeasure[thresholdIndex];
      }
    }
    return thr;
  }

  // ChatGPT plus modifications
  public static int logDerivativeChangePoint(double[] hist, double z) {
    if (hist == null || hist.length < 5)
      return 0;

    int n = hist.length;

    // Normalize histogram
    hist = ArrUtils.normalizeByMaximumTimesFactor(hist, 1);

    // ---- 1. Smooth histogram (3-point moving average)
    double[] smooth = new double[n];
    for (int i = 1; i < n - 1; i++) {
      smooth[i] = (hist[i - 1] + hist[i] + hist[i + 1]) / 3.0;
    }
    smooth[0] = hist[0];
    smooth[n - 1] = hist[n - 1];

    // ---- 2. Log transform
    double eps = 1e-12; // avoid log(0)
    double[] logVals = new double[n];
    for (int i = 0; i < n; i++) {
      logVals[i] = Math.log(smooth[i] + eps);
    }

    // ---- 3. First derivative
    double[] plainDeriv = new double[n - 1];
    double[] logDeriv = new double[n - 1];
    for (int i = 1; i < n; i++) {
      plainDeriv[i - 1] = smooth[i] - smooth[i - 1];
      logDeriv[i - 1] = logVals[i] - logVals[i - 1];
    }

    // ---- 4. Robust threshold using MAD
    //    double median = MeasureOfLocation.MEDIAN_AVOID_ZERO.calc(deriv);
    //    double mad = MeasureOfSpread.AVOID_NONZERO_MAD.calc(deriv);
    double[] firstSection = ArrUtils.getPortion(logDeriv, 0, (int) (logDeriv.length / 3d), LOGGER);
    double mean = MeasureOfLocation.MEAN.calc(firstSection);
    // double sd = MeasureOfSpread.SD.calc(firstSection);
    double sd = Math.sqrt(Math.abs(mean)); // seems to be more stable

    double threshold = mean + z * sd;  // sensitivity parameter

    // ---- 5. Detect first strong upward slope
    int targetIdx = -1; // -1 = fallback: no clear change
    for (int i = 0; i < logDeriv.length; i++) {
      if (logDeriv[i] > threshold) {
        targetIdx = i + 1; // gpt said i+1 but this seems too high (added back in b/c code below!)
        break;
      }
    }

    // Check if we are in monomodal peak (rising slope) - then go back
    int maxSteps = 10;
    int newTargetIdx = Math.max(0, targetIdx - 1);

    for (int i = 0; i < maxSteps && newTargetIdx > 0; i++) {
      if (plainDeriv[newTargetIdx] <= 0) {
        break;
      }
      newTargetIdx--;
    }
    targetIdx = newTargetIdx;

    return targetIdx;
  }
}
