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

import com.google.common.math.Stats;

import java.util.ArrayList;
import java.util.List;

import util.ArrUtils;

public enum MeasureOfLocation implements MeasureOfStat {

  MEAN {
    @Override
    public double calc(List<Double> data) {
      if (!data.isEmpty()) {
        return Stats.meanOf(data);
      } else {
        return 0;
      }
    }

    @Override
    public double calc(double[] data) {
      if (data.length > 0) {
        return Stats.meanOf(data);
      } else {
        return 0;
      }
    }

    @Override
    public String toString() {
      return "Mean";
    }
  },

  MEDIAN {
    @Override
    public double calc(List<Double> data) {
      List<Double> dataList = new ArrayList<>(data);
      if (!data.isEmpty()) {
        return Median.median(dataList);

      } else {
        return 0;
      }
    }

    @Override
    public double calc(double[] data) {
      if (data.length > 0) {
        return Median.median(data);

      } else {
        return 0;
      }
    }

    @Override
    public String toString() {
      return "Median";
    }
  },

  MEDIAN_AVOID_ZERO {
    @Override
    public double calc(List<Double> data) {
      List<Double> dataList = new ArrayList<>(data);
      double median = 0;
      if (!dataList.isEmpty()) {
        double probeMedian = Median.median(dataList);
        if (probeMedian > 0) {
          median = probeMedian;
        } else {
          median = Median.median(ArrUtils.positiveNonzero(dataList));
        }
      }
      return median;
    }

    @Override
    public double calc(double[] data) {
      double median = 0;
      if (data.length > 0) {
        double probeMedian = Median.median(data);
        if (probeMedian > 0) {
          median = probeMedian;
        } else {
          median = Median.median(ArrUtils.positiveNonzero(data));
        }
      }
      return median;
    }

    @Override
    public String toString() {
      return "Checked nonzero median";
    }
  },

  MEDIAN_NONZERO {
    @Override
    public double calc(List<Double> data) {
      if (!data.isEmpty()) {
        double median = Median.median(ArrUtils.positiveNonzero(data));
        return median;
      } else {
        return 0;
      }
    }

    @Override
    public double calc(double[] data) {
      if (data.length > 0) {
        return Median.median(ArrUtils.positiveNonzero(data));

      } else {
        return 0;
      }
    }

    @Override
    public String toString() {
      return "Nonzero median";
    }
  },

  TRUNCATED_POISSON_MEAN {
    @Override
    public double calc(List<Double> data) {
      if (!data.isEmpty()) {
        return TruncatedPoissonEstimator.estimateMean(ArrUtils.doubleListToArr(data));
      } else {
        return 0;
      }
    }

    @Override
    public double calc(double[] data) {
      if (data.length > 0) {
        return TruncatedPoissonEstimator.estimateMean(data);
      } else {
        return 0;
      }
    }

    @Override
    public String toString() {
      return "Truncated Poisson mean";
    }
  },


  CUSTOM {
    @Override
    public double calc(List<Double> data) {
      return 0;
    }

    @Override
    public double calc(double[] data) {
      return 0;
    }

    @Override
    public String toString() {
      return "Custom";
    }
  };

  public static MeasureOfLocation[] baseline() {
    return new MeasureOfLocation[]{MEAN, MEDIAN, MEDIAN_NONZERO, MEDIAN_AVOID_ZERO, TRUNCATED_POISSON_MEAN};
  }

  public static MeasureOfLocation[] window() {
    return new MeasureOfLocation[]{MEAN, MEDIAN};
  }

  public abstract double calc(List<Double> data);

  public abstract double calc(double[] data);

}
