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
import analysis.ResettableStatDataSet.Limit;
import analysis.ResettableStatDataSet.Poisson;
import analysis.ResettableStatDataSet.PoissonNormal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import math.stat.Median;
import math.stat.Rosner;
import processing.options.BaselineDynamic;
import processing.options.CompoundPoissonModel;
import processing.options.DistributionModel;
import processing.options.OutlierModel;
import processing.parameterSets.impl.BaselineParams;
import util.ArrUtils;

public abstract class BaselineGenerator {

  public static Baseline generateBaseline(BaselineParams par, double[] inputData) {

    // Make a copy: We have had a Bug where the original array was destroyed
    double[] data = ArrUtils.copy(inputData);

    /*
    ------------------------ UNPACK PARAMETERS ------------------------
     */
    BaselineDynamic baselineDynamic = par.getBaselineDynamics().getValue();
    int pointsPerSlice = par.getBaselinePointsPerSegment().getValue();
    if (baselineDynamic.equals(BaselineDynamic.CONSTANT)) {
      pointsPerSlice = data.length;
    }

    DistributionModel generalApproach = par.getGeneralDistributionApproach().getValue();
    double poissonGaussianCutoff = par.getPoissonNormalCutoff().getValue();

    DistributionModel poissonCaseChoice = par.getPoissonChoice().getValue();
    CompoundPoissonModel compoundPoissonModelChoice = par.getCompoundPoissonModel().getValue();
    DistributionModel gaussianCaseChoice = par.getGaussianChoice().getValue();

    boolean applyContinuity = par.getApplyPoissonContinuityCorrection().getValue();
    double continuityEpsilon = 0;
    if (applyContinuity) {
      continuityEpsilon = par.getPoissonContinuityCorrection().getValue();
    }

    double siaShape = par.getSiaShape().getValue();

    MeasureOfLocation locGauss = par.getMeasureOfLocationGauss().getValue();
    MeasureOfSpread spreadGauss = par.getMeasureOfSpreadGauss().getValue();

    MeasureOfLocation locPoisson = par.getMeasureOfLocationPoisson().getValue();

    OutlierModel outlierModelGauss = par.getOutlierTestTypeGauss().getValue();
    OutlierModel outlierModelPoisson = par.getOutlierTestTypePoisson().getValue();

    //
    int nonSmartMaxOutlierIncrementGauss = par.getOutlierGaussMaxNonSmartIncrements().getValue();
    int nonSmartMaxOutlierIncrementPoisson = par.getOutlierPoissonMaxNonSmartIncrements()
        .getValue();
    //
    double startFactorGauss = par.getOutlierFactorGaussStart().getValue();
    double startFactorPoisson = par.getOutlierFactorPoissonStart().getValue();

    // special poisson
    MeasureOfLocation locOutlierPoiss = par.getOutlierMeasureOfLocationPoisson().getValue();
    // Model decision happens below as we have to check for each slice in case of THR or PESSIM
    // Do not offset Gaussian model, else SD likely ends up just slightly above the offset,i.e.,
    // SD is mostly given by the offset, which is not ideal.
    boolean offsetBool = par.getOutlierPoissonOffsetBoolean().getValue();
    double poissonOffset;
    if (offsetBool) {
      poissonOffset = par.getOutlierPoissonOffsetValue().getValue();
    } else {
      poissonOffset = 0;
    }

    // smart
    double maxFactor = par.getOutlierFactorMax().getValue();
    double coarseStepSize = 1;
    double fineStepSize = par.getOutlierFineIncrements().getValue();
    fineStepSize = Math.min(fineStepSize, coarseStepSize - 0.1);
    double maxRSD = par.getOutlierStdDevByMeanSmallerThan().getValue();
    double minSD = 0.05;
    double defaultFailMean = par.getOutlierDefaultMeanLowerCap().getValue();
    // special
    double rosnerAlpha = par.getOutlierRosnerAlpha().getValue();
    double rosnerOutlierMaxPct = par.getOutlierRosnerMaxPercent().getValue();



    /*
    ------------------------ TRANSLATE SETTINGS ------------------------
    */

    int poissonDistrID = switch (poissonCaseChoice) {
      case POISSON -> Poisson.getID();
      case POISSON_APPROXIMATION -> PoissonNormal.getID();
      case POISSON_CURRIE -> CurriePoisson.getID();
      case POISSON_COMPOUND -> {
        if (compoundPoissonModelChoice.equals(CompoundPoissonModel.LOG_APPROX)) {
          yield CompoundPoissonApprox.getID();
        } else {
          yield CompoundPoissonTable.getID();
        }
      }

      // essentially not needed as there should not be another option...
      default -> PoissonNormal.getID();
    };

    // Keep switch for structure in case we add options
    int gaussianDistrID = switch (gaussianCaseChoice) {
      case GAUSSIAN -> Gaussian.getID();
      // essentially not needed as there should not be another option...
      default -> Gaussian.getID();
    };

    /*
    ------------------------ PERFORM CALCULATIONS ------------------------
    */

    // Check if we have to split. If so, we treat each piece as a "complete" baseline.
    final List<double[]> dataSlices;
    final int lastSliceLength; // keep for storage
    if (baselineDynamic.equals(BaselineDynamic.SEGMENTED)) {
      // Here, we create new arrays anyway and no copy() is needed (see else statement).
      dataSlices = ArrUtils.splitArray(data, pointsPerSlice, 100);
      if (!dataSlices.isEmpty()) {
        lastSliceLength = dataSlices.get(dataSlices.size() - 1).length;
      } else {
        lastSliceLength = 0;
      }
    } else {
      // Force copy to make sure that no modification trickles back to the original data.
      dataSlices = Collections.singletonList(ArrUtils.copy(data));
      lastSliceLength = 0;
    }

    List<StatDataSet> statDataSets = new ArrayList<>();
    // Now iterate (or whatever is required) for each segment
    for (double[] dataSlice : dataSlices) {

      // prepare distribution
      ResettableStatDataSet poissStatData = new ResettableStatDataSet(
          locOutlierPoiss,
          MeasureOfSpread.SD, // just informative to report SD in the table
          siaShape,
          continuityEpsilon,
          poissonDistrID);

      ResettableStatDataSet gaussStatData = new ResettableStatDataSet(
          locGauss,
          spreadGauss,
          siaShape,
          0,
          gaussianDistrID);

      // Outlier test!
      if (generalApproach.equals(DistributionModel.PESSIMISTIC)) {

        decideAndApplyOutlierTest(outlierModelPoisson, dataSlice, poissStatData,
            nonSmartMaxOutlierIncrementPoisson,
            startFactorPoisson, poissonOffset, coarseStepSize, fineStepSize, maxRSD, maxFactor,
            minSD,
            defaultFailMean, rosnerAlpha, rosnerOutlierMaxPct, locPoisson);

        decideAndApplyOutlierTest(outlierModelGauss, dataSlice, gaussStatData,
            nonSmartMaxOutlierIncrementGauss,
            startFactorGauss, 0, coarseStepSize, fineStepSize, maxRSD, maxFactor, minSD,
            defaultFailMean, rosnerAlpha, rosnerOutlierMaxPct, locPoisson);

        // Which is larger at, e.g., 3 sigma?
        gaussStatData.setZ(3);
        poissStatData.setZ(3);

        if (gaussStatData.calcCriticalLimit() > poissStatData.calcCriticalLimit()) {
          statDataSets.add(gaussStatData);
        } else {
          statDataSets.add(poissStatData);
        }
      } else if (generalApproach.equals(DistributionModel.THRESHOLD)) {
        // I think, we should just use the Raw Data Median.
        // This ensures that any outlier test shenanigans do not play a role in the decision in this case.
        if (MeasureOfLocation.MEDIAN.calc(dataSlice) < poissonGaussianCutoff) {

          decideAndApplyOutlierTest(outlierModelPoisson, dataSlice, poissStatData,
              nonSmartMaxOutlierIncrementPoisson,
              startFactorPoisson, poissonOffset, coarseStepSize, fineStepSize, maxRSD, maxFactor,
              minSD,
              defaultFailMean, rosnerAlpha, rosnerOutlierMaxPct, locPoisson);

          statDataSets.add(poissStatData);

        } else {
          decideAndApplyOutlierTest(outlierModelGauss, dataSlice, gaussStatData,
              nonSmartMaxOutlierIncrementGauss,
              startFactorGauss, 0, coarseStepSize, fineStepSize, maxRSD, maxFactor, minSD,
              defaultFailMean, rosnerAlpha, rosnerOutlierMaxPct, locPoisson);

          statDataSets.add(gaussStatData);
        }

      } else if (generalApproach.equals(DistributionModel.GAUSSIAN_MODEL)) {
        decideAndApplyOutlierTest(outlierModelGauss, dataSlice, gaussStatData,
            nonSmartMaxOutlierIncrementGauss,
            startFactorGauss, 0, coarseStepSize, fineStepSize, maxRSD, maxFactor, minSD,
            defaultFailMean, rosnerAlpha, rosnerOutlierMaxPct, locPoisson);

        statDataSets.add(gaussStatData);

      } else { // case: DistributionModel.POISSON
        decideAndApplyOutlierTest(outlierModelPoisson, dataSlice, poissStatData,
            nonSmartMaxOutlierIncrementPoisson,
            startFactorPoisson, poissonOffset, coarseStepSize, fineStepSize, maxRSD, maxFactor,
            minSD,
            defaultFailMean, rosnerAlpha, rosnerOutlierMaxPct, locPoisson);

        statDataSets.add(poissStatData);
      }
    }

    // prepare a summary
    List<String> summary = new ArrayList<>();
    for (StatDataSet statDataSet : statDataSets) {
      int distrKey = statDataSet.getDistrID();
      String loc = statDataSet.getLocationMeasure();
      String spread = statDataSet.getSpreadMeasure();
      String s = Limit.getName(distrKey) + "(" + loc + "±" + spread + "). ";
      summary.add(s);
    }
    String summaryString = summary.stream().distinct().collect(Collectors.joining());

    StatCollection collection;
    // Note: In the constructor, we extract the outlier factor, i.e., the outlier "significance" as getZ()
    // and store it.
    if (statDataSets.size() > 1) {
      collection = new StatCollectionHDD(statDataSets, pointsPerSlice, lastSliceLength, siaShape,
          continuityEpsilon);
    } else {
      collection = new StatCollectionRAM(statDataSets, pointsPerSlice, lastSliceLength, siaShape,
          continuityEpsilon);
    }

    return new Baseline(collection, summaryString);
  }

  private static void decideAndApplyOutlierTest(
      OutlierModel outlierModel,
      double[] dataSlice,
      ResettableStatDataSet statData,
      int nMax,
      double currentFactor,
      double offset,
      double coarseStepSize,
      double fineStepSize,
      double maxRSD,
      double maxFactor,
      double minSD,
      double defaultFailMean,
      double alphaRosner,
      double maxOutlierPercentRosner,
      MeasureOfLocation poissonLoc) {

    switch (outlierModel) {
      case NOTHING -> {
        statData.updateMuSD(dataSlice);
      }
      case ONE_ITERATION -> {
        double[] modifiableDataArray = ArrUtils.copy(dataSlice);
        iterateLimited(1, modifiableDataArray, statData, currentFactor, offset, poissonLoc);
      }
      case FIXED_NUMBER_ITERATION -> {
        double[] modifiableDataArray = ArrUtils.copy(dataSlice);
        iterateLimited(nMax, modifiableDataArray, statData, currentFactor, offset, poissonLoc);
      }

      case ITERATE_TO_CONVERGENCE -> {
        double[] modifiableDataArray = ArrUtils.copy(dataSlice);
        iterateUnlimited(modifiableDataArray, statData, currentFactor, offset, poissonLoc);
      }

      case SMART_INCREMENT_ITERATION -> {
        iterateSmart(
            dataSlice,
            statData,
            currentFactor,
            coarseStepSize,
            fineStepSize,
            maxRSD,
            maxFactor,
            minSD,
            offset,
            defaultFailMean,
            poissonLoc);
      }
      case ROSNER -> {
        double[] modifiableDataArray = ArrUtils.copy(dataSlice);
        Rosner.find(modifiableDataArray, statData, alphaRosner, maxOutlierPercentRosner);
      }
    }
  }


  public static void iterateSmart(
      double[] modifiableDataArray,
      ResettableStatDataSet statData,
      double factor,
      double coarseStepSize,
      double fineStepSize,
      double maxRSD,
      double maxFactor,
      double minSD,
      double offset, // not needed here but submethods have it
      double defaultFailMean,
      MeasureOfLocation poissonLoc // // not needed here but submethods have it
  ) {

    // Copy to keep originals for repeated iteration.
    // For the distribution encoded in statData, this safes calculating median,SD, ... again and again.
    double[] backupDataArray = ArrUtils.copy(modifiableDataArray);
    ResettableStatDataSet backupStatData = statData.copy();

     /*
     +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     (1) BACKGROUND IS TOO SMALL AND YIELDS mean = approx 0.
     +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     First check: if mean already super small, create poisson-normal BLN w/o Outlier test
     Use "default mean" as threshold to recognize "approx. ZERO".
     Why? It looks weird, if a piece of mean=0 baseline yields a higher background/threshold
     than a slice with a single count which could be the case if default mean = 1
     or if a switch to a poisson model was performed.
     The latter can be achieved by using the pessimistic mode or poisson settings anyway.
     */
    statData.updateMuSD(modifiableDataArray);
    if (statData.getLocation() < defaultFailMean) {
      statData.updateMuSD(defaultFailMean, Math.sqrt(defaultFailMean));
      statData.setSignificance(0, 0.5); // indicate that we have failed

    } else {
      /*
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      (2) ITERATE
      Note: To calculate the outlier test limits, "statData.update(dataArray);"
      is carried out in the unlimited iteration function.
      Here, in this section, we make sure that the SD is not zero, i.e.,
      we avoid swamping (all DP seem to be outliers).
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      */

      // Writes new values into statData (distr).
      iterateUnlimited(modifiableDataArray, statData, factor, offset, poissonLoc);

      /*
       What can go wrong here? A) SD < 0.05. B) RSD > 1.5 @ µ > 1.
       Note: the "floor() >= " logic allows, e.g., 9.29 as search factor.
       Else it stops at 9.29 because 10.29 would be larger than 10 (the default max value).
       Result: 9.29->9 OK, 10->10 or 10.29->10 OK, but 11.X->11 NOT.

       This while loop triggers if poor BLN was obtained to find better, i.e., a larger SD window.
       Note: Loop stops firing if condition is not the case but KEEP IN MIND:
       We learned that one additional increment is beneficial and should be added after FIRST success.
       */

      while (statData.getSpread() < minSD && Math.floor(factor) <= maxFactor) {
        // reset
        statData.updateMuSD(backupStatData); // do not (loses pointer!): backupStatData.copy();
        modifiableDataArray = ArrUtils.copy(backupDataArray);
        factor += coarseStepSize;
        // Writes new values into distr.
        iterateUnlimited(modifiableDataArray, statData, factor, offset, poissonLoc);
        // System.out.println("\n\n1 . smart . µ = " + statData.getLocation());
      }

      // Additional execution
      if (Math.floor(factor) <= maxFactor) {
        // reset
        statData.updateMuSD(backupStatData); // do not (loses pointer!): backupStatData.copy();
        modifiableDataArray = ArrUtils.copy(backupDataArray);
        factor += coarseStepSize;
        // Writes new values into distr.
        iterateUnlimited(modifiableDataArray, statData, factor, offset, poissonLoc);
      }

      /*
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      (4) CHECK if it actually worked. Else, replace with sensible PoissonNormal approximation.
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      */
      if (statData.getSpread() < minSD) {
        // At zero SD, the mean in the statData is likely at the lower end, hence max() is fine.

        // MAD and median provide good alternatives
        double median = Median.median(backupDataArray);
        double mad = Median.mad(backupDataArray, median);
        // whatever is larger: default fail mean or empirical mean
        double mean = Math.max(statData.getLocation(), defaultFailMean);

        // Case: median is largest estimator and the MAD was not zero
        if (median > mean && mad > 0) {
          statData.updateMuSD(median, mad);
        } else {
          // Either the mean estimators were larger or the MAD was zero: Assume Poisson-normal.
          mean = Math.max(median, mean); // only triggers if MAD was zero and that sent us here.
          statData.updateMuSD(mean, Math.sqrt(mean));
        }
        statData.setSignificance(0, 0.5); // indicate that we have failed
      } else {
        /*
        +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        (4) IT LOOKS GOOD - YET, CHECK!
        Here, in this section, we make sure that the SD is not too large, i.e.,
        we avoid masking (outliers are missed and inflate SD).
        In other words: This may be a good result! Only check if RSD is good.
        Note that this comes after the else statement. Why?
        SD cannot be too small and to large at the same time!
        +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        */
        if (statData.getRelativeSpread() > maxRSD && statData.getLocation() > 1) {
          // We failed! We need to iterate with a smaller step size.

          // remember last factor used above
          double lastPreviousFactor = factor;

          /*
          Considerations for changing the factor:
          Make sure start value for factor is smaller but still > 0 (or better > fineStepSize i.e. 0.1).
          E.g., before: sf=2 (or greater) @ coarseStepSize=1.
          Then reduces --> sf=2-1=1 @ fineStepSize=0.1.
          Or only other case: If sf=1 @fineStepSize=1 then --> sf=1-1=0. Thus, correct to--> 0.1
          @fineStepSize=0.1
          Since we include an additional increment iteration above in the incremented outlier test,
          we try to move back 2 iteration steps.
          As the jump to outlier masking occurs so localized,
          I assume it may be smarter so simply go backwards with the reduced step size.
          Why? If one goes -1 and then +0.1, e.g. 4 - 1 + 0.1 = 3.1, then 3.1 likely yields SD > 0 and stop
           right there.
          This would reduce all the effect of giving the additional increment.
          preliminary solution: reductionFactor + while(). It does not go back the 2 steps
          (one step because f is too large and another b/c f was increased by an additional step)
          all at once but -1 -> check -> if bad, again -1 -> check, break.
           */

          int reductionFactor = 1;
          reductionLoop:
          while (reductionFactor <= 2) {
            factor = Math.max(fineStepSize, lastPreviousFactor - reductionFactor * coarseStepSize);
            // do not iterate to the same value that already failed, again.
            double maxSearchFactor = Math.max(lastPreviousFactor - fineStepSize, coarseStepSize);

            // Enforce the loop at least once to get updated statData.getSpread()
            boolean forceFire = true;
            while (forceFire || (statData.getSpread() < minSD && factor < maxSearchFactor)) {
              forceFire = false;
              // reset
              statData.updateMuSD(backupStatData); // NO: backupStatData.copy(); (loses pointer!)
              modifiableDataArray = ArrUtils.copy(backupDataArray);
              factor += fineStepSize;
              // Writes new values into distr.
              iterateUnlimited(modifiableDataArray, statData, factor, offset, poissonLoc);
              // System.out.println("\n2 . smart . µ = " + statData.getLocation());
            }

            if (!(statData.getRelativeSpread() > maxRSD && statData.getLocation() > 1)) {
              break reductionLoop;
            } else {
              reductionFactor++; // increments 1->2 and then the while loop breaks after trying again.
            }
          }

          /*
          +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
          (5) SUCCESS?
          If --SD is zero-- OR  --RSD is too large for a µ>1--
          Else use default fail mean
          +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
          */
          if (statData.getSpread() < minSD
              || (statData.getRelativeSpread() > maxRSD && statData.getLocation() > 1)) {
            // MAD and median provide good alternatives
            double median = Median.median(backupDataArray);
            double mad = Median.mad(backupDataArray, median);
            // whatever is larger: default fail mean or empirical mean
            double mean = Math.max(statData.getLocation(), defaultFailMean);

            // Case: median is largest estimator and the MAD was not zero
            if (median > mean && mad > 0) {
              statData.updateMuSD(median, mad);
            } else {
              // Either the mean estimators were larger or the MAD was zero: Assume Poisson-normal.
              mean = Math.max(median, mean); // only triggers if MAD was zero and that sent us here.
              statData.updateMuSD(mean, Math.sqrt(mean));
            }
            statData.setSignificance(0, 0.5); // indicate that we have failed
          }
        }
      }

    } // closing bracket of the "else condition" of the "µ is too small" if statement!
  }


  public static void iterateUnlimited(
      double[] modifiableDataArray,
      ResettableStatDataSet statData,
      double currentFactor,
      double offset,
      MeasureOfLocation poissonLoc) {

    // checker boolean
    boolean needsIteration = true;
    statData.updateMuSD(modifiableDataArray);

    while (needsIteration) {
      // System.out.println("\n3 . unlimited . µ = " + statData.getLocation());

      // get limits
      statData.setZ(currentFactor);
      // I guess offset should be subtracted or else zero is never allowed (biasing!)
      double lower = statData.calcLowerOutlierLimit() - offset;
      double upper = statData.calcCriticalLimit() + offset;

      // remove outlier
      int sizeBefore = modifiableDataArray.length;
      modifiableDataArray = removeOutliers(modifiableDataArray, lower, upper);
      int sizeAfter = modifiableDataArray.length;
      needsIteration = !(sizeAfter == sizeBefore);

      // set the new values into the distribution
      statData.updateMuSD(modifiableDataArray);
    }

    // check if we must replace the outlier location of Poisson with actual location?
    if (Limit.isPoisson(statData.getDistrID())) {
      statData.overrideLocation(poissonLoc);
      statData.updateMuSD(modifiableDataArray);
    }
  }

  public static void iterateLimited(
      int nMax,
      double[] modifiableDataArray,
      ResettableStatDataSet statData,
      double currentFactor,
      double offset,
      MeasureOfLocation poissonLoc) {

    // checker boolean
    boolean needsIteration = true;
    int iterCount = 0;
    statData.updateMuSD(modifiableDataArray);

    while (iterCount < nMax && needsIteration) {
      // increment
      iterCount++;

      // get limits
      statData.setZ(currentFactor);
      // I guess offset should be subtracted or else zero is never allowed (biasing!)
      double lower = statData.calcLowerOutlierLimit() - offset;
      double upper = statData.calcCriticalLimit() + offset;

      // remove outlier
      int sizeBefore = modifiableDataArray.length;
      modifiableDataArray = removeOutliers(modifiableDataArray, lower, upper);
      int sizeAfter = modifiableDataArray.length;
      needsIteration = !(sizeAfter == sizeBefore);

      // set the new values into the distribution
      statData.updateMuSD(modifiableDataArray);
    }
    // check if we must replace the outlier location of Poisson with actual location?
    if (Limit.isPoisson(statData.getDistrID())) {
      statData.overrideLocation(poissonLoc);
      statData.updateMuSD(modifiableDataArray);
    }
  }


  public static double[] removeOutliers(double[] data, double lowerLim, double upperLim) {
    return removeOutliersArrCopy(data, lowerLim, upperLim);
  }

  /*
  Idea modified from ChatGPT ... seems to check out.
  I think use EQUAL sign (>=), else zeros would never be able to be included for Poiss distr...
   */
  public static double[] removeOutliersArrCopy(double[] data, double lowerLim, double upperLim) {

    int j = 0;
    for (int i = 0; i < data.length; i++) {
      if (data[i] >= lowerLim && data[i] <= upperLim) {
        data[j] = data[i]; // Move valid elements forward
        j++;
      }
      // System.out.println(i + ": " + Arrays.toString(data)); // Expected: [5.6, 3.4, 7.8]
    }
    double[] filteredData = Arrays.copyOfRange(data, 0, j); // Optional truncation
    return filteredData;
  }

  public static double[] removeOutliersStream(double[] data, double lowerLim, double upperLim) {
    double[] filteredData = Arrays.stream(data)
        .filter(d -> d >= lowerLim && d <= upperLim)
        .toArray();
    return filteredData;
  }

  public static double[] removeOutliersRemIf(double[] data, double lowerLim,
                                             double upperLim) {
    List<Double> dataList = ArrUtils.arrToList(data);
    dataList.removeIf(d -> d <= lowerLim && d >= upperLim);
    data = ArrUtils.doubleListToArr(dataList);
    return data;
  }

  public static void removeOutliersRemIf(List<Double> data, double lowerLim,
                                         double upperLim) {
    data.removeIf(d -> d <= lowerLim && d >= upperLim);
  }


}
