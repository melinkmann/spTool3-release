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

import java.util.Arrays;

import mr.go.sgfilter.SGFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SavitzkyGolay {

  private static final Logger LOGGER = LogManager.getLogger(SavitzkyGolay.class);


  // Modified from chatGPT - calls the library suggested by chatGPT.
  public static double[] smoothSG(double[] y, int windowSize, int polyOrder) {
    if (y.length <= 1) {
      return y;
    }
    double[] output;
    try {
      // symmetric window
      int nLeft = (int) Math.round(0.5 * windowSize);
      int nRight = nLeft;
      double[] coefficients = SGFilter.computeSGCoefficients(nLeft, nRight, polyOrder);
      SGFilter sgFilter = new SGFilter(nLeft, nRight);

      // Pad the input
      double[] padded = mirrorPad(y, nLeft, nRight);

      // Apply smoothing
      double[] smoothed = sgFilter.smooth(padded, coefficients);

      // Trim to original length
      output = Arrays.copyOfRange(smoothed, nLeft, nLeft + y.length);

    } catch (Exception e) {
      output = y;
    }
    return output;
  }

  // GPT mirror function.
  public static double[] mirrorPad(double[] y, int nLeft, int nRight) {
    int n = y.length;
    double[] padded = new double[nLeft + n + nRight];

    // Left mirror padding
    for (int i = 0; i < nLeft; i++) {
      padded[i] = y[nLeft - i];
    }

    // Copy original
    System.arraycopy(y, 0, padded, nLeft, n);

    // Right mirror padding
    for (int i = 0; i < nRight; i++) {
      padded[nLeft + n + i] = y[n - 2 - i];
    }

    return padded;
  }


}
