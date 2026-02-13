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

import java.io.Serializable;

public enum DistributionModel implements Serializable {

  PESSIMISTIC {
    @Override
    public String toString() {
      return "Use model with higher threshold";
    }
  },

  THRESHOLD {
    @Override
    public String toString() {
      return "Use Poisson if µ < cutoff";
    }
  },

  GAUSSIAN_MODEL {
    @Override
    public String toString() {
      return "Use the Gaussian model";
    }
  },

  POISSON_MODEL {
    @Override
    public String toString() {
      return "Use the Poisson model";
    }
  },

  GAUSSIAN {
    @Override
    public String toString() {
      return "Gaussian";
    }
  },

  POISSON {
    @Override
    public String toString() {
      return "Poisson";
    }
  },

  POISSON_APPROXIMATION {
    @Override
    public String toString() {
      return "Poisson-normal approximation";
    }
  },

  POISSON_CURRIE {
    @Override
    public String toString() {
      return "Poisson Currie-1968";
    }
  },

  POISSON_COMPOUND {
    @Override
    public String toString() {
      return "Compound Poisson";
    }
  };

  public static DistributionModel[]
  listGeneralOptions() {
    return new DistributionModel[]{THRESHOLD, PESSIMISTIC, GAUSSIAN_MODEL, POISSON_MODEL};
  }

  public static DistributionModel[] listOptionsRequiringPoissonOption() {
    return new DistributionModel[]{THRESHOLD, PESSIMISTIC, POISSON_MODEL};
  }

  public static DistributionModel[] listOptionsRequiringGaussianOption() {
    return new DistributionModel[]{THRESHOLD, PESSIMISTIC, GAUSSIAN_MODEL};
  }

  public static DistributionModel[] listOptionsForGaussian() {
    return new DistributionModel[]{GAUSSIAN};
  }

  public static DistributionModel[] listOptionsForPoisson() {
    return new DistributionModel[]{POISSON, POISSON_APPROXIMATION, POISSON_CURRIE,
        POISSON_COMPOUND};
  }

}
