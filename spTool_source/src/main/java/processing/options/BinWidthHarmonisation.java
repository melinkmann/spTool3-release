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

import math.stat.MeasureOfLocation;

import java.util.Collections;
import java.util.List;

public enum BinWidthHarmonisation {

  MEAN {
    @Override
    public String toString() {
      return "Mean";
    }

    @Override
    public double get(List<Double> candidates) {
      if (candidates.isEmpty()) {
        return BinWidthEstimator.DEFAULT;
      } else {
        return MeasureOfLocation.MEAN.calc(candidates);
      }
    }
  },

  MEDIAN {
    @Override
    public String toString() {
      return "Median";
    }

    @Override
    public double get(List<Double> candidates) {
      if (candidates.isEmpty()) {
        return BinWidthEstimator.DEFAULT;
      } else {
        return MeasureOfLocation.MEDIAN_AVOID_ZERO.calc(candidates);
      }
    }
  },

  MIN {
    @Override
    public String toString() {
      return "Smallest";
    }

    @Override
    public double get(List<Double> candidates) {
      if (candidates.isEmpty()) {
        return BinWidthEstimator.DEFAULT;
      } else {
        return Collections.min(candidates);
      }
    }
  },

  MAX {
    @Override
    public String toString() {
      return "Largest";
    }

    @Override
    public double get(List<Double> candidates) {
      if (candidates.isEmpty()) {
        return BinWidthEstimator.DEFAULT;
      } else {
        return Collections.max(candidates);
      }
    }
  };

  public abstract double get(List<Double> candidates);
}
