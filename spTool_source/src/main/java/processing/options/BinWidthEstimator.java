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

import java.util.Arrays;

import math.Arithmetic;
import math.stat.Shimazaki;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import smile.math.MathEx;

public enum BinWidthEstimator {

  CUSTOM {
    @Override
    public String toString() {
      return "Custom";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      return customValue;
    }

  },

  TERRELL_SCOTT {
    @Override
    public String toString() {
      return "Terrell-Scott rule";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double max = da.getMax();
        double min = da.getMin();

        double k = Math.cbrt(2 * data.length);
        double bw = ((max - min) / k);
        if (bw > 0 && Double.isFinite(bw)) result = bw;
      }
      return result;
    }
  },

  DOANE {
    @Override
    public String toString() {
      return "Doane's formula";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      if (data != null && data.length > 0) {

        int nOfObsv = data.length;
        DescriptiveStatistics da = new DescriptiveStatistics(data);

        double absSkew = Math.abs(da.getSkewness());
        double max = da.getMax();
        double min = da.getMin();

        double upper = 6 * (nOfObsv - 2);
        double lower = (nOfObsv + 1) * (nOfObsv + 3);
        double sg1 = Math.sqrt(upper / lower);

        double k = 1 + Arithmetic.log2(nOfObsv) + Arithmetic.log2(1 + absSkew / sg1);

        double bw = ((max - min) / k);
        if (bw > 0 && Double.isFinite(bw)) result = bw;
      }
      return result;
    }
  },

  RICE {
    @Override
    public String toString() {
      return "Rice rule";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double max = da.getMax();
        double min = da.getMin();

        double k = 2 * (Math.cbrt(data.length));

        double bw = ((max - min) / k);
        if (bw > 0 && Double.isFinite(bw)) result = bw;
      }
      return result;
    }
  },

  SQUARE_ROOT {
    @Override
    public String toString() {
      return "Square root";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      DescriptiveStatistics da = new DescriptiveStatistics(data);
      double max = da.getMax();
      double min = da.getMin();

      double k = Math.sqrt(data.length);

      double bw = ((max - min) / k);
      if (bw > 0 && Double.isFinite(bw)) result = bw;
      return result;
    }
  },

  STURGE {
    @Override
    public String toString() {
      return "Sturge's formula";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double max = da.getMax();
        double min = da.getMin();

        double k = 1 + Arithmetic.log2(data.length);

        double bw = ((max - min) / k);
        if (bw > 0 && Double.isFinite(bw)) result = bw;
      }
      return result;
    }
  },

  FREEDMAN_DIACONIS {
    @Override
    public String toString() {
      return "Freedman-Diaconis' choice";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double iqr = da.getPercentile(75) - da.getPercentile(25);
        double bw = (2 * iqr) / (Math.cbrt(data.length));
        if (bw > 0 && Double.isFinite(bw)) result = bw;
      }
      return result;
    }
  },

  SHIMAZAKI_AND_SHINOMOTO {
    @Override
    public String toString() {
      return "Shimazaki and Shinomoto's choice";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      if (data != null && data.length > 0) {
        double width = Shimazaki.optimalBinWidth(data);
        width = width * 1.25;
        double bw = width;
        if (bw > 0 && Double.isFinite(bw)) result = bw;
      }
      return result;
    }
  },

  SCOTT {
    @Override
    public String toString() {
      return "Scott's normal reference rule";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = Double.NaN;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double sd = da.getStandardDeviation();
        double bw = (3.49 * sd) / (Math.cbrt(data.length));
        if (bw > 0 && Double.isFinite(bw)) result = bw;
      }
      return result;
    }
  },


  POISSON {
    @Override
    public String toString() {
      return "n = 2";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      return 2;
    }
  };


  public static final double DEFAULT = 15;

  /*
   Normal case: ignore the custom value.
   Else, override this method.
   */

  public abstract double getBinWidth(double[] data, double customValue);

  public static BinWidthEstimator[] getAllNonCustom() {
    return new BinWidthEstimator[]{
        DOANE,
        SHIMAZAKI_AND_SHINOMOTO,
        SQUARE_ROOT,
        RICE,
        STURGE,
        SCOTT,
        TERRELL_SCOTT,
        FREEDMAN_DIACONIS,
        POISSON
    };
  }

  public static double silvermanRule(double[] data) {
    double result = Double.NaN;
    if (data != null && data.length > 0) {
      double variance = MathEx.var(data);
      double sd = Math.sqrt(variance);

      Arrays.sort(data);

      int n = data.length;
      double iqr = data[n * 3 / 4] - data[n / 4];
      double h = 1.06 * Math.min(sd, iqr / 1.34) / Math.pow(data.length, 0.2);
      if (h > 0 && Double.isFinite(h)) result = h;
    }
    return result;
  }
}
