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

import math.stat.MovingAverage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Smoothing {

  private static final Logger LOGGER = LogManager.getLogger(Smoothing.class);

  public static double[] moavSmooth(double[] data, int moavPeriod) {
    double[] smoothedData;
    if (data != null) {
      MovingAverage moav = new MovingAverage(moavPeriod);

      smoothedData = new double[data.length];

      for (int i = 0; i < data.length; i++) {
        moav.add(data[i]);
        smoothedData[i] = moav.getMean();
      }
    } else {
      smoothedData = new double[0];
    }
    return smoothedData;
  }


  // Modified from chatGPT. Gaussian kernel by chatGPT.
  public static double[] gaussianSmooth(double[] input, double sigma) {
    if (input.length <= 3) {
      return input;
    }
    try {
      int radius = (int) FastMath.ceil(3 * sigma);
      double[] kernel = new double[2 * radius + 1];
      double sum = 0.0;

      // Create Gaussian kernel
      for (int i = -radius; i <= radius; i++) {
        kernel[i + radius] = FastMath.exp(-0.5 * i * i / (sigma * sigma));
        sum += kernel[i + radius];
      }

      // Normalize kernel
      for (int i = 0; i < kernel.length; i++) {
        kernel[i] /= sum;
      }

      // Convolve input with kernel
      double[] output = new double[input.length];
      for (int i = 0; i < input.length; i++) {
        double val = 0.0;

        // Add mirror padding
        for (int j = -radius; j <= radius; j++) {
          int index = i + j;

          // Mirror the index at the boundaries
          if (index < 0) {
            index = -index;
          } else if (index >= input.length) {
            index = 2 * input.length - index - 2;
          }

          val += input[index] * kernel[j + radius];
        }

        output[i] = val;
      }

      return output;
    } catch (Exception e) {
      LOGGER.error("Cannot apply Gaussian kernel smooth. Continuing with input data. "
          + "Your input data has length: " + input.length
          + " and the sigma window is " + sigma
          + ". Message: "
          + ExceptionUtils.getMessage(e));
      return input;
    }
  }


  // Modified from chatGPT. Sinc kernel by chatGPT.
  public static double[] sincSmooth(double[] data, int degree, int m) {
    if (data.length <= 1) {
      return data;
    }

    try {
      int size = 2 * m + 1;
      double[] kernel = new double[size];
      double sum = 0;

      // Create sinc × Gaussian kernel
      for (int i = -m; i <= m; i++) {
        double x = (double) i / (m + 1);
        double sincArg = Math.PI * 0.5 * (degree + 4) * Math.abs(x);
        double sinc = (sincArg == 0) ? 1.0 : Math.sin(sincArg) / sincArg;
        double gauss = Math.exp(-x * x * 4);
        double value = sinc * gauss;
        kernel[i + m] = value;
        sum += value;
      }

      // Normalize kernel
      for (int i = 0; i < size; i++) {
        kernel[i] /= sum;
      }

      // Apply smoothing with mirror padding
      double[] result = new double[data.length];
      for (int i = 0; i < data.length; i++) {
        double acc = 0;
        for (int j = -m; j <= m; j++) {
          int idx = i + j;

          // Mirror the index at the boundaries
          if (idx < 0) {
            idx = -idx;
          } else if (idx >= data.length) {
            idx = 2 * data.length - idx - 2;
          }

          acc += data[idx] * kernel[j + m];
        }
        result[i] = acc;
      }

      return result;
    } catch (Exception e) {
      LOGGER.error("Cannot apply Sinc kernel smooth. Continue with input data. "
          + "Your input data has length: " + data.length
          + " ,the degree is " + degree
          + " and the kernel size is " + 2 * m + 1
          + ". Message: "
          + ExceptionUtils.getMessage(e));
      return data;
    }
  }

  /**
   * Modified from chatGPT. Uses Apache's LOESS interpolator.
   * Apply LOESS smoothing to a 1D signal.
   *
   * @param input     the raw y-values to be smoothed
   * @param bandwidth smoothing span (0–1, fraction of points)
   * @return smoothed y-values, same length as input
   */
  public static double[] loessSmoothWithPadding(double[] input, double bandwidth) {
    if (input.length <= 1) {
      return input;
    }
    try {
      int n = input.length;
      bandwidth = Math.min(Math.nextDown(1.0), bandwidth);
      bandwidth = Math.max(3.0 / n, bandwidth);
      bandwidth = Math.max(1E-4, bandwidth);

      int padding = (int) Math.round(bandwidth * n);
      padding = Math.min(n - 2, padding);
      int paddedLength = n + 2 * padding;

      // Create padded x and y
      double[] x = new double[paddedLength];
      double[] y = new double[paddedLength];

      // Mirror padding (left)
      for (int i = 0; i < padding; i++) {
        int mirrorIndex = padding - i;
        if (mirrorIndex >= n) {
          mirrorIndex = n - 1; // clamp to last valid index
        }
        y[i] = input[mirrorIndex];
        x[i] = i;
      }

      // Original data
      for (int i = 0; i < n; i++) {
        x[i + padding] = i + padding;
        y[i + padding] = input[i];
      }

      // Mirror padding (right)
      for (int i = 0; i < padding; i++) {
        int mirrorIndex = n - 2 - i;
        if (mirrorIndex < 0) {
          mirrorIndex = 0;  // clamp
        }
        y[n + padding + i] = input[mirrorIndex];
        x[n + padding + i] = n + padding + i;
      }

      // Apply LOESS
      LoessInterpolator loess = new LoessInterpolator(bandwidth, 2);
      double[] smoothed = loess.smooth(x, y);

      // Extract central part
      double[] result = new double[n];
      System.arraycopy(smoothed, padding, result, 0, n);

      return result;

    } catch (Exception e) {
      LOGGER.error("Cannot apply LOESS smoothing. Continue with input data. "
          + "Your input data has length: " + input.length
          + " and the bandwidth is " + bandwidth
          + ". Message: "
          + ExceptionUtils.getMessage(e));
      return input;
    }
  }

}