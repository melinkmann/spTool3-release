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
import java.util.List;
import org.apache.commons.math3.stat.StatUtils;
import util.ArrUtils;

public enum MeasureOfSpread implements MeasureOfStat {


  SD {
    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(List<Double> data, MeasureOfLocation pos) {
      double val = 0;
      if (data.size() > 1) {
        val = Stats.of(data).sampleStandardDeviation();
      }
      return val;
    }

    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(double[] data, MeasureOfLocation pos) {
      double val = 0;
      if (data.length > 1) {
        //val = Stats.of(data).sampleStandardDeviation();
        val = Math.sqrt(StatUtils.variance(data));
      }
      return val;
    }


    @Override
    public String toString() {
      return "SD";
    }
  },

  SQUARE_ROOT {
    @Override
    public double calc(List<Double> data, MeasureOfLocation pos) {
      double val = 0;
      if (!data.isEmpty()) {
        double posVal = pos.calc(data);
        val = Math.sqrt(posVal);
      }
      return val;
    }

    @Override
    public double calc(double[] data, MeasureOfLocation pos) {
      double val = 0;
      if (data.length > 0) {
        double posVal = pos.calc(data);
        val = Math.sqrt(posVal);
      }
      return val;
    }

    @Override
    public String toString() {
      return "Square root";
    }

    @Override
    public boolean usesPosition() {
      return true;
    }
  },

  MAD {
    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(List<Double> data, MeasureOfLocation pos) {
      double val = 0;
      if (!data.isEmpty()) {
        val = Median.mad(data);
        val = val * 1.4826; // for the estimation. c.f. wikipedia
      }
      return val;
    }

    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(double[] data, MeasureOfLocation pos) {
      double val = 0;
      if (data.length > 0) {
        val = Median.mad(data);
        val = val * 1.4826; // for the estimation. c.f. wikipedia
      }
      return val;
    }

    @Override
    public String toString() {
      return "MAD";
    }
  },


  AVOID_NONZERO_MAD {
    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(List<Double> data, MeasureOfLocation pos) {

      double val = 0;

      if (!data.isEmpty()) {

        double median = Median.median(data);
        if (median > 0) {
          val = Median.mad(data, median);
        } else {
          List<Double> nonzeroData = ArrUtils.positiveNonzero(data);
          if (!nonzeroData.isEmpty()) {
            val = Median.mad(ArrUtils.doubleListToArr(nonzeroData));
          }
        }
      }

      val = val * 1.4826; // for the estimation. c.f. wikipedia
      return val;
    }

    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(double[] data, MeasureOfLocation pos) {

      double val = 0;

      if (data.length > 0) {

        double median = Median.median(data);
        if (median > 0) {
          val = Median.mad(data, median);
        } else {
          double[] nonzeroData = ArrUtils.positiveNonzero(data);
          if (nonzeroData.length > 0) {
            val = Median.mad(nonzeroData);
          }
        }
      }

      val = val * 1.4826; // for the estimation. c.f. wikipedia
      return val;
    }

    @Override
    public String toString() {
      return "Checked nonzero MAD";
    }
  },

  NONZERO_MAD {
    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(List<Double> data, MeasureOfLocation pos) {
      List<Double> nonzeroData = ArrUtils.positiveNonzero(data);
      double val = 0;
      if (!nonzeroData.isEmpty()) {
        val = Median.mad(ArrUtils.doubleListToArr(nonzeroData));
        val = val * 1.4826; // for the estimation. c.f. wikipedia
      }
      return val;
    }

    /**
     * @param pos will be ignored and the Median is used instead.
     */
    @Override
    public double calc(double[] data, MeasureOfLocation pos) {
      double[] nonzeroData = ArrUtils.positiveNonzero(data);
      double val = 0;
      if (nonzeroData.length > 0) {
        val = Median.mad(nonzeroData);
        val = val * 1.4826; // for the estimation. c.f. wikipedia
      }
      return val;
    }

    @Override
    public String toString() {
      return "Nonzero MAD";
    }
  };

  /**
   * @param measureOfLocation will be ignored unless square root option is chosen!
   */
  public abstract double calc(List<Double> data, MeasureOfLocation measureOfLocation);

  /**
   * @param measureOfLocation will be ignored unless square root option is chosen!
   */
  public abstract double calc(double[] data, MeasureOfLocation measureOfLocation);


  public double calc(List<Double> data) {
    return calc(data, MeasureOfLocation.MEAN);
  }

  public double calc(double[] data) {
    return calc(data, MeasureOfLocation.MEAN);
  }


  public static MeasureOfSpread[] listOutlierTestOptions() {
    return new MeasureOfSpread[]{SD, MAD};
  }

  public boolean usesPosition() {
    return false;
  }

}
