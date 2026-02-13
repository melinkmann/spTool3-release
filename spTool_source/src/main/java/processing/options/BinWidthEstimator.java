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
      double result = DEFAULT;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double max = da.getMax();
        double min = da.getMin();

        double k = Math.cbrt(2 * data.length);
        result = ((max - min) / k);
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
      double result = DEFAULT;
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

        result = ((max - min) / k);
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
      double result = DEFAULT;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double max = da.getMax();
        double min = da.getMin();

        double k = 2 * (Math.cbrt(data.length));
        result = ((max - min) / k);
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
      DescriptiveStatistics da = new DescriptiveStatistics(data);
      double max = da.getMax();
      double min = da.getMin();

      double k = Math.sqrt(data.length);

      return ((max - min) / k);
    }
  },

  STURGE {
    @Override
    public String toString() {
      return "Sturge's formula";
    }

    @Override
    public double getBinWidth(double[] data, double customValue) {
      double result = DEFAULT;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double max = da.getMax();
        double min = da.getMin();

        double k = 1 + Arithmetic.log2(data.length);

        result = ((max - min) / k);
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
      double result = DEFAULT;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double iqr = da.getPercentile(75) - da.getPercentile(25);
        double width = (2 * iqr) / (Math.cbrt(data.length));
        result = width;
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
      double result = DEFAULT;
      if (data != null && data.length > 0) {
        double width = Shimazaki.optimalBinWidth(data);
        width = width * 1.25;
        result = width;
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
      double result = DEFAULT;
      if (data != null && data.length > 0) {
        DescriptiveStatistics da = new DescriptiveStatistics(data);
        double sd = da.getStandardDeviation();
        double width = (3.49 * sd) / (Math.cbrt(data.length));
        result = width;
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


  private static final double DEFAULT = 15;

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
    double result = DEFAULT;
    if (data != null && data.length > 0) {
      double variance = MathEx.var(data);
      double sd = Math.sqrt(variance);

      Arrays.sort(data);

      int n = data.length;
      double iqr = data[n * 3 / 4] - data[n / 4];
      double h = 1.06 * Math.min(sd, iqr / 1.34) / Math.pow(data.length, 0.2);
      result = h > 0 ? h : 1;
    }
    return result;
  }
}
