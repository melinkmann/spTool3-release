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

import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
import util.ArrUtils;

public enum PreFilter {

  NONE {
    @Override
    public double[] filter(double[] values) {
      return values;
    }

    @Override
    public List<Double> filter(List<Double> values) {
      return values;
    }

    @Override
    public Pair<double[], double[]> filter(double[] x, double[] y) {
      return new Pair<>(x, y);
    }
  },


  MAD {
    @Override
    public double[] filter(double[] values) {
      if (values.length < 2) {
        return values;
      } else {
        List<Double> within = new ArrayList<>();

        double median = MeasureOfLocation.MEDIAN_AVOID_ZERO.calc(values);
        double mad = MeasureOfSpread.AVOID_NONZERO_MAD.calc(values);

        for (double d : values) {
          if (median - F * mad < d && d < median + F * mad) {
            within.add(d);
          }
        }

        return ArrUtils.doubleListToArr(within);
      }
    }

    @Override
    public Pair<double[], double[]> filter(double[] x, double[] y) {
      if (x.length < 2 && y.length < 2) {
        return new Pair<>(x, y);
      } else {

        List<Double> xF = new ArrayList<>();
        List<Double> yF = new ArrayList<>();

        double median = MeasureOfLocation.MEDIAN_AVOID_ZERO.calc(y);
        double mad = MeasureOfSpread.AVOID_NONZERO_MAD.calc(y);

        for (int i = 0; i < y.length; i++) {
          if (median - F * mad < y[i] && y[i] < median + F * mad) {
            xF.add(x[i]);
            yF.add(y[i]);
          }
        }

        return new Pair<>(ArrUtils.doubleListToArr(xF), ArrUtils.doubleListToArr(yF));
      }
    }

    @Override
    public List<Double> filter(List<Double> values) {
      if (values.size() < 2) {
        return values;
      } else {
        List<Double> within = new ArrayList<>();

        double median = MeasureOfLocation.MEDIAN_AVOID_ZERO.calc(values);
        double mad = MeasureOfSpread.AVOID_NONZERO_MAD.calc(values);

        for (double d : values) {
          if (median - F * mad < d && d < median + F * mad) {
            within.add(d);
          }
        }
        return within;
      }
    }
  };

  private static final double F = 1; // or F=1-3 ???

  public abstract double[] filter(double[] values);

  public abstract List<Double> filter(List<Double> values);

  public abstract Pair<double[], double[]> filter(double[] x, double[] y);


}
