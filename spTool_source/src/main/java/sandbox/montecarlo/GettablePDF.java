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

package sandbox.montecarlo;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

public enum GettablePDF {
  GAUSSIAN {
    @Override
    public CheckedDistribution get(double... params) {
      double mean = params[0];
      double sd = params[1];
      AbstractRealDistribution distr;
      if (sd > 0) {
        distr = new NormalDistribution(mean, sd);
      } else {
        distr = null;
      }
      return new CheckedDistribution(distr, mean);
    }

    @Override
    public String toString() {
      return "Normal";
    }
  },

  LOGNORMAL {
    @Override
    public CheckedDistribution get(double... params) {
      double mean = params[0];
      double sd = params[1];
      AbstractRealDistribution distr;
      if (sd > 0) {
        double mu_Log = Statistics.lognormalMu(mean, sd);
        double sd_Log = Statistics.lognormalSD(mean, sd);
        distr = new LogNormalDistribution(mu_Log, sd_Log);
      } else {
        distr = null;
      }
      return new CheckedDistribution(distr, mean);

    }

    @Override
    public String toString() {
      return "Lognormal";
    }
  },

  EXPONENTIAL {
    @Override
    public CheckedDistribution get(double... params) {
      double mean = params[0];
      AbstractRealDistribution distr;
      if (mean > 0) {
        distr = new ExponentialDistribution(mean);
      } else {
        distr = null;
      }
      return new CheckedDistribution(distr, mean, true);
    }

    @Override
    public String toString() {
      return "Exponential";
    }
  },

  PARETO {
    @Override
    public CheckedDistribution get(double... params) {
      double mean = params[0];
      double scale = params[2];
      double shape = params[3];
      AbstractRealDistribution distr;
      if (scale > 0 && shape > 0) {
        distr = new ParetoDistribution2(scale, shape);
      } else {
        distr = null;
      }
      return new CheckedDistribution(distr, mean);
    }

    @Override
    public String toString() {
      return "Pareto distribution (Power law): shape";
    }
  },

  PARETO_MU {
    @Override
    public CheckedDistribution get(double... params) {
      double mean = params[0];
      double scale = params[2];

      double shape = mean / (mean - scale);

      AbstractRealDistribution distr;
      if (scale > 0 && shape > 0) {
        distr = new ParetoDistribution2(scale, shape);
      } else {
        distr = null;
      }
      return new CheckedDistribution(distr, mean);
    }

    @Override
    public String toString() {
      return "Pareto distribution (Power law): mean";
    }
  };


  // MEAN     SD    paretoScale     paretoShape
  public abstract CheckedDistribution get(double... params);

  public static GettablePDF[] getGaussianFamily(){
    return new GettablePDF[]{GAUSSIAN, LOGNORMAL};
  }
}

