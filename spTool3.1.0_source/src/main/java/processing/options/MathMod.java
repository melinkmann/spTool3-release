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

package processing.options;

import com.google.common.math.DoubleMath;

public enum MathMod {


  NONE {
    @Override
    public String toString() {
      return "None";
    }

    @Override
    public double calc(double val) {
      return val;
    }

    @Override
    public double invert(double val) {
      return val;
    }

    @Override
    public double[] calc(double[] values) {
      return values;
    }


  },

  CUBEROOT {
    @Override
    public String toString() {
      return "Cuberoot";
    }

    @Override
    public double calc(double val) {
      return Math.cbrt(val);
    }

    @Override
    public double invert(double val) {
      return Math.pow(val, 3);
    }

    @Override
    public double[] calc(double[] values) {
      double[] conv = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        conv[i] = Math.cbrt(values[i]);
      }
      return conv;
    }
  },

  LOG10 {
    @Override
    public String toString() {
      return "Log10";
    }


    /**
     * Forward log10 transform.
     *
     * @param val Input value
     * @return log10(val) if val > 0; otherwise returns LOG_ZERO,
     * a finite substitute for log10(0) to avoid -Infinity
     * <p>
     * Notes:
     * - Math.nextUp(0.0) gives Double.MIN_VALUE (~4.94e-324), the smallest positive double.
     * - log10(Double.MIN_VALUE) ≈ -323.3, which is finite and safe for charting libraries like JFreeChart.
     * - This ensures that zero or negative inputs are mapped to a consistent "far-left" value
     * in histograms without introducing Infinity.
     */
    @Override
    public double calc(double val) {
      return val > 0 ? Math.log10(val) : LOG_ZERO;
    }

    /**
     * Inverse of the log10 transform.
     *
     * @param input Transformed value
     * @return 0 if input == LOG_ZERO (the sentinel for log10(0)),
     * otherwise 10^input
     * <p>
     * Notes:
     * - Exact equality comparison is safe here because LOG_ZERO is a constant sentinel
     * that cannot be produced by log10 of any positive value.
     * - This ensures that invert(calc(0)) == 0, maintaining symmetry.
     * - Positive inputs are inverted normally with Math.pow(10, input).
     */
    public double invert(double input) {
      return input == LOG_ZERO ? 0 : Math.pow(10, input);
    }

    @Override
    public double[] calc(double[] values) {
      double[] conv = new double[values.length];

      for (int i = 0; i < values.length; i++) {
        if (values[i] <= 0) {
          // Input is zero or negative
          conv[i] =LOG_ZERO;
        } else {
          // Normal positive input: calc log10
          conv[i] = Math.log10(values[i]);
        }
      }
      return conv;
    }
  };

  public abstract double calc(double val);

  public abstract double invert(double val);

  public abstract double[] calc(double[] values);

  // Smallest positive double to use as a finite substitute for log10(0)
  private static final double LOG_ZERO = Math.log10(Math.nextUp(0.0));
}
