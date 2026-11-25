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

import com.google.common.math.DoubleMath;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.numbers.gamma.RegularizedBeta;
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
  private final boolean lowerPoissonLimitZero;


  public ResettableStatDataSet(MeasureOfLocation positionMeasure,
                               @Nullable MeasureOfSpread spreadMeasure,
                               double siaShape,
                               double continuityEpsilon,
                               boolean lowerPoissonLimitZero,
                               int distrID) {
    this.locationMeasure = positionMeasure;
    this.spreadMeasure = spreadMeasure;
    this.siaShape = siaShape;
    this.continuityEpsilon = continuityEpsilon;
    this.distrID = distrID;
    this.lowerPoissonLimitZero = lowerPoissonLimitZero;
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
    this.lowerPoissonLimitZero = stat.lowerPoissonLimitZero;
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
      lim = Poisson.poissonCriticalLimit(alpha, location + continuityEpsilon);
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
    if (distrID == NegativeBinomial.getID()) {
      lim = NegativeBinomial.negativeBinomialCriticalLimit(alpha, location + continuityEpsilon, spread);
    }

    return lim;
  }

  public double calcDetectionLimit() {
    double lim = 0;
    if (distrID == Poisson.getID()) {
      lim = Poisson.poissonDetectionLimit(alpha, location + continuityEpsilon);
    }
    if (distrID == Gaussian.getID()) {
      lim = Gaussian.gaussianDetectionLimit(z, location, spread);
    }
    if (distrID == PoissonNormal.getID()) {
      lim = PoissonNormal.normalApproximationDetectionLimit(z, location + continuityEpsilon);
    }
    if (distrID == CurriePoisson.getID()) {
      lim = CurriePoisson.currieDetectionLimit(z, location + continuityEpsilon);
    }
    if (distrID == CompoundPoissonApprox.getID()) {
      lim = CompoundPoissonApprox.compoundDetectionLimit(z, location, siaShape);
    }
    if (distrID == CompoundPoissonTable.getID()) {
      lim = CompoundPoissonTable.compoundDetectionLimit(z, location, siaShape);
    }
    if (distrID == NegativeBinomial.getID()) {
      lim = NegativeBinomial.negativeBinomialDetectionLimit(z, location + continuityEpsilon, spread);
    }
    return lim;
  }


  public double calcLowerOutlierLimit() {
    double lim = 0;
    if (distrID == Poisson.getID()) {
      if (lowerPoissonLimitZero) {
        lim = 0;
      } else {
        lim = Poisson.poissonLowerOutlierLimit(alpha, location + continuityEpsilon);
      }
    }
    if (distrID == Gaussian.getID()) {
      lim = Gaussian.gaussianLowerOutlierLimit(z, location, spread);
    }
    if (distrID == PoissonNormal.getID()) {
      if (lowerPoissonLimitZero) {
        lim = 0;
      } else {
        lim = PoissonNormal.normalApproximationLowerOutlierLimit(z, location + continuityEpsilon);
      }
    }
    if (distrID == CurriePoisson.getID()) {
      if (lowerPoissonLimitZero) {
        lim = 0;
      } else {
        lim = CurriePoisson.currieLowerOutlierLimit(z, location + continuityEpsilon);
      }
    }
    if (distrID == CompoundPoissonApprox.getID()) {
      lim = CompoundPoissonApprox.compoundLowerOutlierLimit(alpha, location, siaShape);
    }
    if (distrID == CompoundPoissonTable.getID()) {
      lim = CompoundPoissonTable.compoundLowerOutlierLimit(alpha, location, siaShape);
    }

    if (distrID == NegativeBinomial.getID()) {
      if (lowerPoissonLimitZero) {
        lim = 0;
      } else {
        lim = NegativeBinomial.negativeBinomialLowerOutlierLimit(alpha, location + continuityEpsilon, spread);
      }
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
      pValue = PoissonNormal.calcSurvival(location + continuityEpsilon, intensity);
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
    if (distrID == NegativeBinomial.getID()) {
      pValue = NegativeBinomial.calcSurvival(location + continuityEpsilon, spread, intensity);
    }
    return pValue;
  }

  private double calcTruncatedPValue(double threshold) {
    double pSel = 1; // same neutral default as calcPValue
    if (distrID == Poisson.getID()) {
      pSel = Poisson.calcTruncatedPValue(location, threshold);
    }
    if (distrID == Gaussian.getID()) {
      pSel = Gaussian.calcTruncatedPValue(location, spread, threshold);
    }
    if (distrID == PoissonNormal.getID()) {
      pSel = PoissonNormal.calcTruncatedPValue(location + continuityEpsilon, threshold);
    }
    if (distrID == CurriePoisson.getID()) {
      // calcPValue uses the exact Poisson for Currie, so the selection probability must too.
      pSel = Poisson.calcTruncatedPValue(location, threshold);
    }
    if (distrID == CompoundPoissonApprox.getID()) {
      pSel = CompoundPoissonApprox.calcTruncatedPValue(location, siaShape, threshold);
    }
    if (distrID == CompoundPoissonTable.getID()) {
      pSel = CompoundPoissonTable.calcTruncatedPValue(location, siaShape, threshold);
    }
    if (distrID == NegativeBinomial.getID()) {
      pSel = NegativeBinomial.calcTruncatedPValue(location + continuityEpsilon, spread, threshold);
    }
    return pSel;
  }

  @Override
  public double calcTruncatedPValue(double intensity, double threshold) {
    double pValue;
    if (intensity > threshold) {
      pValue = calcPValue(intensity) / calcTruncatedPValue(threshold);

      // cap to (0,1]
      pValue = Math.min(1d, Math.max(Math.nextUp(0d), pValue));
    } else {
      pValue = calcPValue(intensity);
      pValue = Math.min(1d, Math.max(Math.nextUp(0d), pValue));
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
        double val = pd.inverseCumulativeProbability(1 - alpha);
        return val;
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
        double pGreater = pd.survivalProbability(k); // per Commons Statistics source
        double pEqual = pd.probability(k);             // P(X = k)
        double pVal = pGreater + 0.5 * pEqual;

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);

        return pVal;
      } else {
        // Poisson is not defined for mu=0!
        //... we kind of are at the leftmost tail of the defined intensity space.
        if (DoubleMath.fuzzyEquals(intensity, 0d, 1E-6)) {
          return 1;
        } else {
          return Math.nextUp(0d);
        }
      }
    }

    public static double calcTruncatedPValue(double mean, double threshold) {
      if (mean > 0) {
        // Full probability mass of the selected region, NOT mid-p:
        // this is the probability of the selection event itself, so there are no ties to split.
        org.apache.commons.statistics.distribution.PoissonDistribution pd =
            org.apache.commons.statistics.distribution.PoissonDistribution.of(mean);

        // integer counts: x > threshold <=> x > floor(threshold)
        int k = (int) Math.floor(threshold);
        double pVal = pd.survivalProbability(k); // P(X > k)

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);

        return pVal;
      } else {
        // We are at the leftmost tail of the defined intensity space.
        if (DoubleMath.fuzzyEquals(threshold, 0d, 1E-6)) {
          return 1;
        } else {
          return Math.nextUp(0d);
        }
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
        // ND only defined for strictly positive SD
        NormalDistribution nd = NormalDistribution.of(mean, spread);
        double pVal = nd.survivalProbability(intensity);

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);
        return pVal;
      } else {
        // We are likely at the leftmost tail of the defined intensity space, i.e.,
        // if blank is truly just zeros, we can return a very small probability for any nonzero intensity.
        if (DoubleMath.fuzzyEquals(intensity, 0d, 1E-6)) {
          return 1;
        } else {
          return Math.nextUp(0d);
        }
      }
    }

    public static double calcTruncatedPValue(double mean, double spread, double threshold) {
      // continuous: there is no mid-p, so calcSurvival already is P(X > threshold)
      return calcSurvival(mean, spread, threshold);
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
        if (DoubleMath.fuzzyEquals(intensity, 0d, 1E-6)) {
          return 1;
        } else {
          return Math.nextUp(0d);
        }
      }
    }

    public static double calcTruncatedPValue(double mean, double threshold) {
      // counts are integers: x > threshold <=> x >= t, with t = floor(threshold) + 1.
      // The normal approximation of P(X >= t) is S(t - 0.5) = S(floor(threshold) + 0.5).
      // calcSurvival has no mid-p here, so it can be reused as is.
      return calcSurvival(mean, Math.floor(threshold) + 0.5);
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
      // I think this was wrong, as there should not be 2 * z but only z.
      // Currie only needs to multiply 2 * z to replace the critical limit formula z * sqrt(mean)
      // with 2 * z * sqrt(mean), as LD is given as LD = z^2 + 2 * LC.
      // Note that this refers to the net LD (not gross YD), i.e., the mean is not taken into account,
      // and one should not multiply 2 *  YC, as this gives 2 * [mean + z * sqrt(mean)].

      // Old formula: return Math.pow(2 * z, 2) + currieCriticalLimit(2 * z, mean);
      double k = Math.pow(z, 2);
      return k + currieCriticalLimit(2 * z, mean);
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
        if (DoubleMath.fuzzyEquals(intensity, 0d, 1E-6)) {
          return 1;
        } else {
          return Math.nextUp(0d);
        }
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
              Statistics.getCompoundPoissonSurvivalPValue(model,
                  intensity + 1,
                  mean,
                  muLog,
                  sigmaLog);

          // Here we have to estimate the p(x) via CDF(x+e)-CDF(x)
          double pX = Statistics.getCompoundPoissonSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);

          // get next larger value from approx; here increment by 1 and not 0.1
          // Why? aprrox. ist continuous and would trigger immediately;
          // for mean below 2, adding +1 sounds reasonable
          double pX1 = pX;
          int maxCounter = 100;
          for (int i = 0; i < maxCounter; i++) {
            pX1 = Statistics.getCompoundPoissonSurvivalPValue(model,
                intensity + (i + 1d),
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
            pVal = Statistics.getCompoundPoissonSurvivalPValue(model,
                intensity,
                mean,
                muLog,
                sigmaLog);
          }

          pVal = Math.max(pVal, Math.nextUp(0d));
          return pVal;
        } else {
          double p = Statistics.getCompoundPoissonSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);
          return Math.max(p, Math.nextUp(0d));
        }

      } else {
        // We are at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }

    public static double calcTruncatedPValue(double mean, double sigmaLog, double threshold) {
      if (mean > 0) {
        double muLog = Statistics.getLognormalMuNormalized(sigmaLog);

        // continuous: P(X > threshold) is the plain survival, not the mid-p branch of calcSurvival
        double pSel = Statistics.getCompoundPoissonSurvivalPValue(CompoundPoissonModel.LOG_APPROX,
            threshold,
            mean,
            muLog,
            sigmaLog);
        return Math.max(pSel, Math.nextUp(0d));
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
              Statistics.getCompoundPoissonSurvivalPValue(model,
                  intensity + 1,
                  mean,
                  muLog,
                  sigmaLog);

          // Here we have to estimate the p(x) via CDF(x+e)-CDF(x)
          double pX = Statistics.getCompoundPoissonSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);

          // get next larger value from table
          double pX1 = pX;
          int maxCounter = 10_000;
          for (int i = 0; i < maxCounter; i++) {
            pX1 = Statistics.getCompoundPoissonSurvivalPValue(model,
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
            // just make sure that this is positive
            double pEqual = Math.abs(approxPMF); // P(X = k)
            pVal = pGreater + 0.5 * pEqual;
          } else {
            // midP failed, use the normal p (comes from code "kind of anyway if suv=0" but enforce strictly

            // Only ensures that p>0. No further checking of caps internally in this function:
            pVal = Statistics.getCompoundPoissonSurvivalPValue(model,
                intensity,
                mean,
                muLog,
                sigmaLog);
          }

          pVal = Math.max(pVal, Math.nextUp(0d));
          return pVal;
        } else {
          double p = Statistics.getCompoundPoissonSurvivalPValue(model,
              intensity,
              mean,
              muLog,
              sigmaLog);
          return Math.max(p, Math.nextUp(0d));
        }

      } else {
        // We are at the leftmost tail of the defined intensity space.
        return Math.nextUp(0d);
      }
    }

    public static double calcTruncatedPValue(double mean, double sigmaLog, double threshold) {
      if (mean > 0) {
        double muLog = Statistics.getLognormalMuNormalized(sigmaLog);

        // continuous: P(X > threshold) is the plain survival, not the mid-p branch of calcSurvival
        double pSel = Statistics.getCompoundPoissonSurvivalPValue(CompoundPoissonModel.LOOKUP_TABLE,
            threshold,
            mean,
            muLog,
            sigmaLog);
        return Math.max(pSel, Math.nextUp(0d));
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

  // Claude Sonnet 5.5, unchecked (TODO)
  public static class NegativeBinomial implements Limit {

    // The NB needs var > mean. If the estimated variance is not above the mean (by chance, or because the
    // blank is not overdispersed), it is raised to mean * (1 + MIN_OVERDISPERSION). The NB then is
    // practically a Poisson, and r and p stay valid, so nothing breaks.
    private static final double MIN_OVERDISPERSION = 1E-3;

    private static double calcVariance(double mean, double spread) {
      return Math.max(spread * spread, mean * (1 + MIN_OVERDISPERSION));
    }

    // Method of moments: var = mean + mean^2 / r. Real-valued, NOT rounded to an int.
    private static double calcR(double mean, double spread) {
      return mean * mean / (calcVariance(mean, spread) - mean);
    }

    // success probability: mean = r * (1 - p) / p and var = mean / p
    private static double calcP(double mean, double spread) {
      return mean / calcVariance(mean, spread);
    }

    // P(X > k), full mass. Complement of the regularized beta, so small tails stay accurate.
    private static double survivalProbability(double r, double p, int k) {
      if (k < 0) {
        return 1;
      } else {
        return RegularizedBeta.complement(p, r, k + 1.0);
      }
    }

    // smallest integer k with P(X > k) <= tail
    private static double upperTailQuantile(double r, double p, double tail, double mean, double spread) {
      // bracket from above
      int hi = Math.max(1, (int) Math.ceil(mean + spread));
      while (survivalProbability(r, p, hi) > tail) {
        if (hi > Integer.MAX_VALUE / 2) {
          return Integer.MAX_VALUE;
        }
        hi *= 2;
      }
      // bisection, with P(X > lo) > tail >= P(X > hi); lo = -1 stands for P(X > -1) = 1
      int lo = -1;
      while (hi - lo > 1) {
        int mid = lo + (hi - lo) / 2;
        if (survivalProbability(r, p, mid) > tail) {
          lo = mid;
        } else {
          hi = mid;
        }
      }
      return hi;
    }

    public static double negativeBinomialCriticalLimit(double alpha, double mean, double spread) {
      if (mean > 0) {
        return upperTailQuantile(calcR(mean, spread), calcP(mean, spread), alpha, mean, spread);
      } else {
        return 0;
      }
    }

    public static double negativeBinomialLowerOutlierLimit(double alpha, double mean, double spread) {
      if (mean > 0) {
        // LOWER limit: Not P(X > k) <= alpha BUT P(X > k) <= 1 - alpha
        return upperTailQuantile(calcR(mean, spread), calcP(mean, spread), 1 - alpha, mean, spread);
      } else {
        return 0;
      }
    }

    // The currie formalism is based on z, hence we have to use it here. Analogous to the exact Poisson.
    public static double negativeBinomialDetectionLimit(double z, double mean, double spread) {
      double alpha = Statistics.zToAlpha(2 * z);
      return Math.pow(z, 2) + negativeBinomialCriticalLimit(1 - alpha, mean, spread);
    }

    public static double calcSurvival(double mean, double spread, double intensity) {
      if (mean > 0) {
        double r = calcR(mean, spread);
        double p = calcP(mean, spread);

        // midP for more uniformity in discrete distributions
        int k = (int) Math.floor(intensity);
        double pGreater = survivalProbability(r, p, k);                 // P(X > k)
        double pEqual = survivalProbability(r, p, k - 1) - pGreater;    // P(X = k) = P(X >= k) - P(X > k)
        double pVal = pGreater + 0.5 * pEqual;

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);

        return pVal;
      } else {
        // NB is not defined for mean = 0: same handling as the Poisson
        if (DoubleMath.fuzzyEquals(intensity, 0d, 1E-6)) {
          return 1;
        } else {
          return Math.nextUp(0d);
        }
      }
    }

    public static double calcTruncatedPValue(double mean, double spread, double threshold) {
      if (mean > 0) {
        double r = calcR(mean, spread);
        double p = calcP(mean, spread);

        // Full probability mass, NOT mid-p. Integer counts: x > threshold <=> x > floor(threshold)
        int k = (int) Math.floor(threshold);
        double pVal = survivalProbability(r, p, k); // P(X > k)

        // cap to >0
        pVal = Math.max(Math.nextUp(0d), pVal);

        return pVal;
      } else {
        return Poisson.calcTruncatedPValue(mean, threshold);
      }
    }

    public static int getID() {
      return 7;
    }

    public static boolean isID(int id) {
      return id == getID();
    }

    static String getName() {
      return "Negative binomial";
    }

    public static MeasureOfLocation getDefaultLocation() {
      return MeasureOfLocation.MEAN;
    }

    public static MeasureOfSpread getDefaultSpread() {
      return MeasureOfSpread.SD;
    }
  }


}
