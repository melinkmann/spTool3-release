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

package analysis;

import analysis.ResettableStatDataSet.CompoundPoissonApprox;
import analysis.ResettableStatDataSet.CompoundPoissonTable;
import analysis.ResettableStatDataSet.CurriePoisson;
import analysis.ResettableStatDataSet.Gaussian;
import analysis.ResettableStatDataSet.Poisson;
import analysis.ResettableStatDataSet.PoissonNormal;

import java.io.Serial;
import java.io.Serializable;

import math.stat.MeasureOfLocation;
import sandbox.montecarlo.Statistics;

import static sandbox.montecarlo.Statistics.MIN_P_VALUE;

public class FixedStatDataSet implements StatDataSet, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final double DEFAULT_Z = 1;
  private static final double DEFAULT_ALPHA = Statistics.zToAlpha(DEFAULT_Z);

  private final int distrID;

  // initialize as standard normal
  private final double location;
  private final double spread;
  private final double siaShape;
  private final double continuityEpsilon;

  // set significance level
  private double z = DEFAULT_Z;
  private double alpha = DEFAULT_ALPHA; // or else we call new ND for each instance:  Statistics.zToAlpha(z)

  public FixedStatDataSet(double location, double spread, double siaShape, double continuityEpsilon,
                          int distrID) {
    this.location = location;
    this.spread = spread;
    this.siaShape = siaShape;
    this.distrID = distrID;
    this.continuityEpsilon = continuityEpsilon;
  }

  // for the outlier test where "z" (f) is incremented
  public void setZ(double z) {
    this.z = z;
    this.alpha = Statistics.zToAlpha(z);
  }

  public void setAlpha(double alpha) {
    this.alpha = alpha;
    this.z = Statistics.alphaToZ(alpha);
  }

  @Override
  public void setSignificance(double z, double alpha) {
    this.z = z;
    this.alpha = alpha;
  }

  public double getAlpha() {
    return alpha;
  }

  public double getZ() {
    return z;
  }

  public void overrideLocation(MeasureOfLocation locationMeasure) {
    // Do nothing
  }

  public double getSiaShape() {
    return siaShape;
  }

  public double getLocation() {
    return location;
  }

  public double getSpread() {
    return spread;
  }

  @Override
  public double getRelativeSpread() {
    double rsd;
    if (location == 0) {
      rsd = Double.MAX_VALUE;
    } else {
      rsd = spread / location;
    }
    return rsd;
  }

  @Override
  public String getLocationMeasure() {
    return "NaN";
  }

  @Override
  public String getSpreadMeasure() {
    return "NaN";
  }

  public int getDistrID() {
    return distrID;
  }


  public double calcCriticalLimit() {
    double lim = 0;
    if (distrID == Poisson.getID()) {
      lim = Poisson.poissonCriticalLimit(alpha, location);
    }
    if (distrID == Gaussian.getID()) {
      lim = Gaussian.gaussianCriticalLimit(z, location, spread);
    }
    if (distrID == PoissonNormal.getID()) {
      lim = PoissonNormal.normalApproximationCriticalLimit(z, location+ continuityEpsilon);
    }
    if (distrID == CurriePoisson.getID()) {
      lim = CurriePoisson.currieCriticalLimit(z, location+ continuityEpsilon);
    }
    if (distrID == CompoundPoissonApprox.getID()) {
      lim = CompoundPoissonApprox.compoundCriticalLimit(alpha, location, siaShape);
    }
    if (distrID == CompoundPoissonTable.getID()) {
      lim = CompoundPoissonTable.compoundCriticalLimit(alpha, location, siaShape);
    }
    return lim;
  }

  public double calcDetectionLimit() {
    double lim = 0;
    if (distrID == Poisson.getID()) {
      lim = Poisson.poissonDetectionLimit(z, location);
    }
    if (distrID == Gaussian.getID()) {
      lim = Gaussian.gaussianDetectionLimit(z, location, spread);
    }
    if (distrID == PoissonNormal.getID()) {
      lim = PoissonNormal.normalApproximationDetectionLimit(z, location+ continuityEpsilon);
    }
    if (distrID == CurriePoisson.getID()) {
      lim = CurriePoisson.currieDetectionLimit(z, location+ continuityEpsilon);
    }
    if (distrID == CompoundPoissonApprox.getID()) {
      lim = CompoundPoissonApprox.compoundDetectionLimit(z, location, siaShape);
    }
    if (distrID == CompoundPoissonTable.getID()) {
      lim = CompoundPoissonTable.compoundDetectionLimit(z, location, siaShape);
    }
    return lim;
  }


  public double calcLowerOutlierLimit() {
    double lim = 0;
    if (distrID == Poisson.getID()) {
      lim = Poisson.poissonLowerOutlierLimit(alpha, location);
    }
    if (distrID == Gaussian.getID()) {
      lim = Gaussian.gaussianLowerOutlierLimit(z, location, spread);
    }
    if (distrID == PoissonNormal.getID()) {
      lim = PoissonNormal.normalApproximationLowerOutlierLimit(z, location+ continuityEpsilon);
    }
    if (distrID == CurriePoisson.getID()) {
      lim = CurriePoisson.currieLowerOutlierLimit(z, location+ continuityEpsilon);
    }
    if (distrID == CompoundPoissonApprox.getID()) {
      lim = CompoundPoissonApprox.compoundLowerOutlierLimit(alpha, location, siaShape);
    }
    if (distrID == CompoundPoissonTable.getID()) {
      lim = CompoundPoissonTable.compoundLowerOutlierLimit(alpha, location, siaShape);
    }
    return lim;
  }

  public double calcPValue(double intensity) {
    double pValue = 1; // this is the "worst" pValue possible, "equivalent" to THR=0.
    if (distrID == Poisson.getID()) {
      // continuity correction happens inside of Poisson.calcSurvival!
      pValue = Poisson.calcSurvival(location, intensity);
    }
    if (distrID == Gaussian.getID()) {
      pValue = Gaussian.calcSurvival(location, spread, intensity);
    }
    if (distrID == PoissonNormal.getID()) {
      pValue = PoissonNormal.calcSurvival(location+ continuityEpsilon, intensity);
    }
    if (distrID == CurriePoisson.getID()) {
      /*
       * For the approximation case, there is no PDF. The approximation defines a critical limit. For the
       * survival,
       * we need to compute z for a given result, which requires the corresponding PDF. The PDF is not
       * included in the
       * approximation. Hence, it is probably best to just use the Poisson distribution.
       */
      // continuity correction happens inside of Poisson.calcSurvival!
      pValue = Poisson.calcSurvival(location, intensity);
    }
    if (distrID == CompoundPoissonApprox.getID()) {
      pValue = CompoundPoissonApprox.calcSurvival(location, siaShape, intensity);
    }
    if (distrID == CompoundPoissonTable.getID()) {
      pValue = CompoundPoissonTable.calcSurvival(location, siaShape, intensity);
    }
    return pValue;
  }


  /////////////////////////////////////////////////////////////////////////////////////////
  /*
    Idea: we do not have to keep/store everything.
    Hence, we do not have to actually store the empirical mean AND whatever the model needs.
    So, we only have to store the location, the spread and outlier factor.
   */


}
