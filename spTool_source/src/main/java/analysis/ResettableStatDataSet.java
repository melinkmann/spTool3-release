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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;

import core.SpTool3Main;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.statistics.distribution.NormalDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.CompoundPoissonModel;
import sandbox.montecarlo.Statistics;

public class ResettableStatDataSet implements StatDataSet, Serializable {


  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(ResettableStatDataSet.class);

  // a distribution as a part of a baseline
  private MeasureOfLocation locationMeasure;
  @Nullable
  private final MeasureOfSpread spreadMeasure;
  private final int distrID;

  // initialize as standard normal
  private double location = 0;
  private double spread = 1;
  private double siaShape = 0.47;

  // set significance level... maybe initialize as @mean?
  private double z = 0;
  private double alpha = 0.5;

  private double continuityEpsilon = 0;


  public ResettableStatDataSet(MeasureOfLocation positionMeasure,
                               @Nullable MeasureOfSpread spreadMeasure,
                               double siaShape,
                               double continuityEpsilon,
                               int distrID) {
    this.locationMeasure = positionMeasure;
    this.spreadMeasure = spreadMeasure;
    this.siaShape = siaShape;
    this.continuityEpsilon = continuityEpsilon;
    this.distrID = distrID;
  }

  public ResettableStatDataSet(ResettableStatDataSet stat) {
    this.locationMeasure = stat.locationMeasure;
    this.spreadMeasure = stat.spreadMeasure;
    this.distrID = stat.distrID;
    //
    this.location = stat.location;
    this.spread = stat.spread;
    this.siaShape = stat.siaShape;
    this.continuityEpsilon = stat.continuityEpsilon;
    //
    this.z = stat.z;
    this.alpha = stat.alpha;
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
    this.locationMeasure = locationMeasure;
  }

  // specify location, ...
  public void updateMuSD(double[] data) {
    this.location = locationMeasure.calc(data);
    if (spreadMeasure != null) {
      this.spread = spreadMeasure.calc(data);
    }
  }

  public void updateMuSD(List<Double> data) {
    this.location = locationMeasure.calc(data);
    if (spreadMeasure != null) {
      this.spread = spreadMeasure.calc(data);
    }
  }

  public void updateMuSD(double location, double spread) {
    this.location = location;
    this.spread = spread;
  }

  public void updateMuSD(StatDataSet backupSet) {
    this.location = backupSet.getLocation();
    this.spread = backupSet.getSpread();
  }

  public ResettableStatDataSet copy() {
    return new ResettableStatDataSet(this);
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
    return locationMeasure.toString();
  }

  @Override
  public String getSpreadMeasure() {
    return spreadMeasure != null ? spreadMeasure.toString() : "NaN";
  }

  public int getDistrID() {
    return distrID;
  }

  public double calcCriticalLimit() {
    double lim = 0;
    if (distrID == Poisson.getID()) {
      lim = Poisson.poissonCriticalLimit(alpha, location+ continuityEpsilon);
    }
    if (distrID == Gaussian.getID()) {
      lim = Gaussian.gaussianCriticalLimit(z, location, spread);
    }
    if (distrID == PoissonNormal.getID()) {
      lim = PoissonNormal.normalApproximationCriticalLimit(z, location + continuityEpsilon);
    }
    if (distrID == CurriePoisson.getID()) {
      lim = CurriePoisson.currieCriticalLimit(z, location + continuityEpsilon);
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
      lim = Poisson.poissonDetectionLimit(alpha, location+ continuityEpsilon);
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
      lim = Poisson.poissonLowerOutlierLimit(alpha, location+ continuityEpsilon);
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

  /*
    Switching between z and alpha value for each data point is a bad idea.
    Hence, we call this on a higher level, and pass the easier value down to the distribution.
  */



  /*
    Essentially, the functions that we need for statistics can be put into static functions.
   */

  /**
   * Pure Poisson.
   */

  public interface Limit {

    static int getID() {
      return 0;
    }

    static boolean isID(int id) {
      return false;
    }

    static MeasureOfLocation defPos() {
      return MeasureOfLocation.MEAN;
    }

    static MeasureOfSpread defSpread() {
      return MeasureOfSpread.SD;
    }

    static String getName() {
      return "Unknown";
    }

    static String getName(int distrID) {
      String name = "Unknown";
      if (distrID == Poisson.getID()) {
        name = Poisson.getName();
      }
      if (distrID == Gaussian.getID()) {
        name = Gaussian.getName();
      }
      if (distrID == PoissonNormal.getID()) {
        name = PoissonNormal.getName();
      }
      if (distrID == CurriePoisson.getID()) {
        name = CurriePoisson.getName();
      }
      if (distrID == CompoundPoissonApprox.getID()) {
        name = CompoundPoissonApprox.getName();
      }
      if (distrID == CompoundPoissonTable.getID()) {
        name = CompoundPoissonTable.getName();
      }
      return name;
    }

    static boolean isPoisson(int distrID) {
      boolean isPoiss = false;
      if (distrID == Poisson.getID()) {
        isPoiss = true;
      }
      if (distrID == Gaussian.getID()) {
        isPoiss = false;
      }
      if (distrID == PoissonNormal.getID()) {
        isPoiss = true;
      }
      if (distrID == CurriePoisson.getID()) {
        isPoiss = true;
      }
      if (distrID == CompoundPoissonApprox.getID()) {
        isPoiss = true;
      }
      if (distrID == CompoundPoissonTable.getID()) {
        isPoiss = true;
      }
      return isPoiss;
    }
  }

  public static class Poisson implements Limit {

    public static double poissonCriticalLimit(double alpha, double mean) {
      if (mean > 0) {
        PoissonDistribution pd = new PoissonDistribution(mean);
        return pd.inverseCumulativeProbability(1 - alpha);
      } else {
        return 0;
      }
    }

    public static double poissonLowerOutlierLimit(double alpha, double mean) {
      if (mean > 0) {
        PoissonDistribution pd = new PoissonDistribution(mean);
        // LOWER limit: Not 1-alpha BUT alpha!
        double val = pd.inverseCumulativeProbability(alpha);
        return val;
      } else {
        return 0;
      }
    }

    // The currie formalism is based on z, hence we have to use it here.
    public static double poissonDetectionLimit(double z, double mean) {
      double alpha = Statistics.zToAlpha(2 * z);
      return Math.pow(z, 2) + poissonCriticalLimit(1 - alpha, mean);
    }

    public static double calcSurvival(double mean, double intensity) {
      if (mean > 0) {
        //        org.apache.commons.statistics.distribution.PoissonDistribution pd =
        //            org.apache.commons.statistics.distribution.PoissonDistribution.of(mean);
        //        return pd.survivalProbability((int) Math.floor(intensity));
        // Switch to miP for more uniformity in discrete distributions

        org.apache.commons.statistics.distribution.PoissonDistribution pd =
            org.apache.commons.statistics.distribution.PoissonDistribution.of(mean);

        int k = (int) Math.floor(intensity);
        double pGreater = pd.survivalProbability(k + 1); // P(X > k)
        double pEqual = pd.probability(k);             // P(X = k)
        double pVal = pGreater + 0.5 * pEqual;

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);

        return pVal;
      } else {
        // Poisson is not defined for mu=0!
        //... we kind of are at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }

    public static int getID() {
      return 1;
    }

    public static boolean isID(int id) {
      return id == getID();
    }

    static String getName() {
      return "Poisson exact";
    }

    public static MeasureOfLocation defPos() {
      return MeasureOfLocation.MEAN;
    }

    public static MeasureOfSpread defSpread() {
      return MeasureOfSpread.SQUARE_ROOT;
    }
  }


  public static class Gaussian implements Limit {

    public static double gaussianCriticalLimit(double z, double mean, double sigma) {
      return mean + z * sigma;
    }

    public static double gaussianLowerOutlierLimit(double z, double mean, double sigma) {
      return mean - z * sigma;
    }

    // The currie formalism is based on z, hence we have to use it here.
    public static double gaussianDetectionLimit(double z, double mean, double sigma) {
      return gaussianCriticalLimit(2 * z, mean, sigma);
    }

    public static double calcSurvival(double mean, double spread, double intensity) {
      if (mean > 0 && spread > 0) {
        NormalDistribution nd = NormalDistribution.of(mean, spread);
        double pVal = nd.survivalProbability(intensity);

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);
        return pVal;
      } else {
        // We are likely at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }

    public static int getID() {
      return 2;
    }

    public static boolean isID(int id) {
      return id == getID();
    }

    static String getName() {
      return "Gaussian";
    }


    public static MeasureOfLocation defPos() {
      return MeasureOfLocation.MEAN;
    }

    public static MeasureOfSpread defSpread() {
      return MeasureOfSpread.SD;
    }
  }


  public static class PoissonNormal implements Limit {

    public static double normalApproximationCriticalLimit(double z, double mean) {
      return mean + z * Math.sqrt(mean);
    }

    public static double normalApproximationLowerOutlierLimit(double z, double mean) {
      return mean - z * Math.sqrt(mean);
    }

    // The currie formalism is based on z, hence we have to use it here.
    public static double normalApproximationDetectionLimit(double z, double mean) {
      return normalApproximationCriticalLimit(2 * z, mean);
    }

    public static double calcSurvival(double mean, double intensity) {
      if (mean > 0) {
        NormalDistribution nd = NormalDistribution.of(mean, Math.sqrt(mean));
        double pVal = nd.survivalProbability(intensity);

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);
        return pVal;
      } else {
        // We are at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }

    public static int getID() {
      return 3;
    }

    public static boolean isID(int id) {
      return id == getID();
    }

    static String getName() {
      return "Poisson normal approximation";
    }

    public static MeasureOfLocation getDefaultPosition() {
      return MeasureOfLocation.MEAN;
    }

    public static MeasureOfSpread getDefaultSpread() {
      return MeasureOfSpread.SQUARE_ROOT;
    }
  }


  public static class CurriePoisson implements Limit {

    public static double currieCriticalLimit(double z, double mean) {
      return mean + z * Math.sqrt(mean);
    }

    public static double currieLowerOutlierLimit(double z, double mean) {
      return mean - z * Math.sqrt(mean);
    }

    // The currie formalism is based on z, hence we have to use it here.
    public static double currieDetectionLimit(double z, double mean) {
      return Math.pow(2 * z, 2) + currieCriticalLimit(2 * z, mean);
    }

    public static int getID() {
      return 4;
    }

    public static boolean isID(int id) {
      return id == getID();
    }

    static String getName() {
      return "Currie Poisson approximation";
    }

    public static MeasureOfLocation getDefaultPosition() {
      return MeasureOfLocation.MEAN;
    }

    public static MeasureOfSpread getDefaultSpread() {
      return MeasureOfSpread.SQUARE_ROOT;
    }

    // Intrinsically, Currie uses a Poisson-Normal approximation, so I guess this is our best guess.
    public static double calcSurvival(double mean, double intensity) {
      if (mean > 0) {
        NormalDistribution nd = NormalDistribution.of(mean, Math.sqrt(mean));
        double pVal = nd.survivalProbability(intensity);

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);
        return pVal;
      } else {
        // We are at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }
  }

  public static class CompoundPoissonApprox implements Limit {

    public static double compoundCriticalLimit(double alpha, double mean, double siaShape) {
      return Statistics.getCompoundThreshold(CompoundPoissonModel.LOG_APPROX,
          mean,
          siaShape,
          alpha
      );
    }

    public static double compoundLowerOutlierLimit(double alpha, double mean, double siaShape) {
      return Statistics.getCompoundThreshold(CompoundPoissonModel.LOG_APPROX,
          mean,
          siaShape,
          1 - alpha
      );
    }

    // The currie formalism is based on z, hence we have to use it here.
    public static double compoundDetectionLimit(double z, double mean, double siaShape) {
      z = 2 * z;
      double alpha = Statistics.zToAlpha(z);
      return Statistics.getCompoundThreshold(CompoundPoissonModel.LOG_APPROX,
          mean,
          siaShape,
          alpha
      );
    }

    public static double calcSurvival(double mean, double sigmaLog, double intensity) {
      if (mean > 0) {
        double muLog = Statistics.getLognormalMuNormalized(sigmaLog);

        // check for discreteness, then use midP
        double pVal;
        CompoundPoissonModel model = CompoundPoissonModel.LOG_APPROX;
        if (mean < 2) {

          // P(X > k)
          double pGreater =
              Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
                  intensity + 1,
                  mean,
                  muLog,
                  sigmaLog);

          // Here we have to estimate the p(x) via CDF(x+e)-CDF(x)
          double pX = Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);

          // get next larger value from table
          double pX1 = pX;
          int maxCounter = 10_000;
          for (int i = 0; i < maxCounter; i++) {
            pX1 = Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
                intensity + (i + 1) * 0.1,
                mean,
                muLog,
                sigmaLog);
            if (pX != pX1) {
              break;
            }
          }

          double approxPMF = pX - pX1;
          if (approxPMF > 0) {
            // just make sure that this is positive+
            double pEqual = Math.abs(approxPMF); // P(X = k)
            pVal = pGreater + 0.5 * pEqual;
          } else {
            // midP failed, use the normal p (comes from code "kind of anyway if suv=0" but enforce strictly

            // Only ensures that p>0. No further checking of caps internally in this function:
            pVal = Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
                intensity,
                mean,
                muLog,
                sigmaLog);
          }

          return pVal;
        } else {
          return Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);
        }

      } else {
        // We are at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }


    public static int getID() {
      return 5;
    }

    public static boolean isID(int id) {
      return id == getID();
    }

    static String getName() {
      return "Compound Poisson approximation";
    }


    public static MeasureOfLocation getDefaultPosition() {
      return MeasureOfLocation.MEAN;
    }

    public static MeasureOfSpread getDefaultSpread() {
      return MeasureOfSpread.SQUARE_ROOT;
    }
  }

  public static class CompoundPoissonTable implements Limit {

    public static double compoundCriticalLimit(double alpha, double mean, double siaShape) {
      return Statistics.getCompoundThreshold(CompoundPoissonModel.LOOKUP_TABLE,
          mean,
          siaShape,
          alpha
      );
    }

    public static double compoundLowerOutlierLimit(double alpha, double mean, double siaShape) {
      // Problem: at low means, the zero quantile does not yield zero (I assume b/c interpolation in the
      // table falls short)
      double val;
      if (mean < 10) {

        /*
        Assume alpha set in the statDataSet is = 0.05;
        Now, to get that significance at left tail, we need 1-alpha = 0.95;
        If 0.95 does not yield a lower threshold than e.g., 0.95 * 1.1,
        we know that the lookup table should deliver something smaller, likely zero.
        For the outlier test, it is important to include zeros, thus this non.ideal case distinction.
         */
        val = Statistics.getCompoundThreshold(CompoundPoissonModel.LOOKUP_TABLE,
            mean,
            siaShape,
            1 - alpha);

        double valHigh = Statistics.getCompoundThreshold(CompoundPoissonModel.LOOKUP_TABLE,
            mean,
            siaShape,
            (1 - alpha) * 1.1);

        // Hard cap
        if (val >= valHigh) {
          val = 0;
        }
      } else {
        val = Statistics.getCompoundThreshold(CompoundPoissonModel.LOOKUP_TABLE,
            mean,
            siaShape,
            1 - alpha);
      }

      return val;
    }

    // The currie formalism is based on z, hence we have to use it here.
    public static double compoundDetectionLimit(double z, double mean, double siaShape) {
      z = 2 * z;
      double alpha = Statistics.zToAlpha(z);
      return Statistics.getCompoundThreshold(CompoundPoissonModel.LOOKUP_TABLE,
          mean,
          siaShape,
          alpha
      );
    }

    public static double calcSurvival(double mean, double sigmaLog, double intensity) {
      if (mean > 0) {
        double muLog = Statistics.getLognormalMuNormalized(sigmaLog);

        // check for discreteness, then use midP
        double pVal;
        CompoundPoissonModel model = CompoundPoissonModel.LOOKUP_TABLE;
        if (mean < 2) {

          // P(X > k)
          double pGreater =
              Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
                  intensity + 1,
                  mean,
                  muLog,
                  sigmaLog);

          // Here we have to estimate the p(x) via CDF(x+e)-CDF(x)
          double pX = Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);

          // get next larger value from table
          double pX1 = pX;
          int maxCounter = 10_000;
          for (int i = 0; i < maxCounter; i++) {
            pX1 = Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
                intensity + (i + 1) * 0.1,
                mean,
                muLog,
                sigmaLog);
            if (pX != pX1) {
              break;
            }
          }

          double approxPMF = pX - pX1;
          if (approxPMF > 0) {
            // just make sure that this is positive+
            double pEqual = Math.abs(approxPMF); // P(X = k)
            pVal = pGreater + 0.5 * pEqual;
          } else {
            // midP failed, use the normal p (comes from code "kind of anyway if suv=0" but enforce strictly

            // Only ensures that p>0. No further checking of caps internally in this function:
            pVal = Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
                intensity,
                mean,
                muLog,
                sigmaLog);
          }

          return pVal;
        } else {
          return Statistics.getCompoundPoissonLnApproxSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);
        }

      } else {
        // We are at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }

    public static int getID() {
      return 6;
    }

    public static boolean isID(int id) {
      return id == getID();
    }

    static String getName() {
      return "Compound Poisson lookup table";
    }


    public static MeasureOfLocation getDefaultPosition() {
      return MeasureOfLocation.MEAN;
    }

    public static MeasureOfSpread getDefaultSpread() {
      return MeasureOfSpread.SQUARE_ROOT;
    }
  }


}
