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

package io.nu;

import analysis.Baseline;
import analysis.BaselineGenerator;
import analysis.MainEventCollection;
import analysis.SearchUtils;
import core.SpTool3Main;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import math.stat.Median;
import math.stat.Mode;
import org.jetbrains.annotations.Nullable;
import processing.options.CompoundPoissonModel;
import processing.options.MathMod;
import sandbox.montecarlo.Statistics;
import util.ArrUtils;


import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

public class SiaShapeEstimation {

  public static double computeShape(double[] intensity, @Nullable List<int[]> blankedRanges) {
    double shape = Double.NaN;
    if (intensity.length > 0) {

      // parameters
      int requiredZeros = 15;

      // from the config
      double alpha = SpTool3Main.getRunTime().getConfParams().getSiaParticleFilterAlpha().getValue();
      double increasedAlpha = SpTool3Main.getRunTime().getConfParams().getSiaParticleFilterAlpha().getValue();

      boolean[] blankerMask = buildBlankerMask(intensity.length, blankedRanges);

      double[] blankedData = getBlankedData(intensity, blankerMask);
      double[] nonzeroBlankedData = ArrUtils.nonzero(blankedData);
      int zeroInBlanked = blankedData.length - nonzeroBlankedData.length;

      double[] unblankedData = getNonBankedData(intensity, blankerMask);
      double[] nonzeroUnblankedData = ArrUtils.nonzero(unblankedData);
      int zeroInUnBlanked = unblankedData.length - nonzeroUnblankedData.length;

      double[] targetData;
      if (zeroInUnBlanked >= requiredZeros || zeroInBlanked >= requiredZeros) {

        // BLANKER
        if (zeroInBlanked >= requiredZeros) {
          // In the blanked data, there are likely no outliers from NPs.
          // Actually, we may not get the perfect isotope-specific shape, as we also weigh in spurious ions
          // and
          // electronic noise.
          targetData = blankedData;
          // UNBLANKED DATA
        } else {
          targetData = unblankedData;
        }

        // START EVAL BY COMPUTING THRESHOLD TO REMOVE NPs
        double[] filteredData = filterDataIteratively(targetData, alpha, increasedAlpha, 0); //0.2
        double[] nonzeroFilteredData = ArrUtils.nonzero(filteredData);

        // We do not know the underlying Poisson lambda.
        // --> estimate it from the number of zeros for the Poisson component:
        // P(k=0) = exp(-lambda)  →  lambda = -ln(nZeros / nTotal)
        // This is essentially a MLE of lambda using only the zero-count observations.
        int nZeros = filteredData.length - nonzeroFilteredData.length;
        double pZero = (double) nZeros / (double) filteredData.length;  // P(k=0)
        double lambda = -Math.log(pZero);

        double momentShape = Double.NaN;
        /*
        Math:

        Shape parameter sigma:
        sigma = sqrt(ln(Var(Y) × lambda / E[Y]²))

        Thus, we compute:
        ratio = Var(Y) * lambda / E[Y]^2 = exp(sigma^2)

        Note: ratio must be > 1 for sigma^2 > 0. If ratio <= 1 the data are under-dispersed
        relative to a compound Poisson, indicating numerical issue or too few spikes removed.
        */

        double dataMean = MeasureOfLocation.MEAN.calc(filteredData);
        double dataVar = Math.pow(MeasureOfSpread.SD.calc(filteredData), 2);

        double ratio = dataVar * lambda / (dataMean * dataMean);
        if (ratio > 1.0) {
          momentShape = Math.sqrt(Math.log(ratio));
          shape = momentShape;
        }

        // NOT ENOUGH ZEROS!
      } else {
        /*
         * This path only works accurately, if the cts data has been normalised to mean = 1 properly.
         * If instead, the SIA is not captured accurately for each isotope, this will likely not work
         * totally accurate...
         */
        // there are not enough blanker, thus, work with normal data
        targetData = unblankedData;

        // START EVAL BY COMPUTING THRESHOLD TO REMOVE NPs
        double[] filteredData = filterDataIteratively(targetData, alpha, increasedAlpha, 0); //0.2

        double momentShape = Double.NaN;
        double dataMean = MeasureOfLocation.MEAN.calc(filteredData);
        double dataVar = Math.pow(MeasureOfSpread.SD.calc(filteredData), 2);

        double lambda = dataMean;

        double ratio = dataVar * lambda / (dataMean * dataMean);
        if (ratio > 1.0) {
          momentShape = Math.sqrt(Math.log(ratio));
          shape = momentShape;
        }

      }

    }
    return shape;
  }

  public static double[] filterDataIteratively(double[] targetData, double alpha, double increasedAlpha,
                                               double meanCriterion) {
    boolean iterate = true;

    // Fine-tune alpha: start with something around 1E-6 (works well for blanks)
    // but cut if particles are present, e.g., dividing by 10

    int originalSize = targetData.length;
    double[] data = ArrUtils.copy(targetData);
    data = filterData(data, alpha, meanCriterion);
    int currentSize = data.length;
    double loss = originalSize - currentSize;
    // rough estimate: if we have more than 0.001 * length, then likely NPs are present
    if (loss / originalSize > 0.001) {
      // e.g., put it in the middle between 0.001 and user input value
      // alpha = (alpha + increasedAlpha) /2;
      alpha = increasedAlpha;
    }


    // reset, and go into actual iteration
    int prevSize;
    data = ArrUtils.copy(targetData);

    while (iterate) {
      prevSize = data.length;
      data = filterData(data, alpha, meanCriterion);
      if (prevSize == data.length) {
        iterate = false;
      }
    }

    return data;
  }

  public static double[] filterData(double[] targetData, double alpha, double meanCriterion) {
    double[] nonzeroTargetData = ArrUtils.positiveNonzero(targetData);

    // percent of zeros
    double pNonzero = (double) nonzeroTargetData.length / targetData.length;

    // estimate mean via mode (will undershoot but is stable)
    double mode = Mode.calcNonzero(ArrUtils.round(targetData, 2));
    // corrects mode for being drawn from nonzero
    double corrMode = pNonzero * mode;

    // mean
    double mean = MeasureOfLocation.MEAN.calc(targetData);

    // if mean and mode are close, use the mean (it is likely the more accurate of the two),
    // else: use mode, because the mean is the one to deviate and then mode is better.
    double meanEstimator;
    if (Math.abs(mean - corrMode) / corrMode > meanCriterion) {
      meanEstimator = corrMode;
    } else {
      meanEstimator = mean;
    }

    double thr = Statistics.getCompoundThreshold(CompoundPoissonModel.LOOKUP_TABLE,
        meanEstimator,
        0.6, // default value that roughly seems to agree well with experimental data
        alpha);

    // This uses that events occur in peaks, and connected points are likely an event
    List<Integer> eventIndices = SearchUtils.splitEventSearch(targetData, meanEstimator, meanEstimator, thr);
    HashSet<Integer> eventIndicesSet = new HashSet<>(eventIndices);
    List<Double> backgroundData = new ArrayList<>(targetData.length);
    for (int i = 0; i < targetData.length; i++) {
      if (!eventIndicesSet.contains(i)) {
        backgroundData.add(targetData[i]);
      }
    }
    // double[] filteredData = ArrUtils.strictlySmallerThan(targetData, thr);

    double[] filteredData = ArrUtils.doubleListToArr(backgroundData);

    return filteredData;
  }

  public static class Stats {


    public final double mean;
    public final double median;
    public final double nonzeroMedian;
    public final double mode;
    public final double modeNonzero;
    public final double roundingMode;
    public final double roundingModeNonzero;
    public final double sD;
    public final double mAD;
    public final double nonzeroMAD;


    public Stats(double[] intensity) {
      this.mean = MeasureOfLocation.MEAN.calc(intensity);
      this.median = MeasureOfLocation.MEDIAN.calc(intensity);
      this.nonzeroMedian = MeasureOfLocation.MEDIAN_NONZERO.calc(intensity);
      this.mode = Mode.calc(intensity);
      this.modeNonzero = Mode.calcNonzero(intensity);
      double[] roundedIntensity = ArrUtils.round(intensity, 2);
      this.sD = MeasureOfSpread.SD.calc(intensity);
      this.mAD = MeasureOfSpread.MAD.calc(intensity);
      this.nonzeroMAD = MeasureOfSpread.NONZERO_MAD.calc(intensity);
      this.roundingMode = Mode.calc(roundedIntensity);
      this.roundingModeNonzero = Mode.calcNonzero(roundedIntensity);
    }
  }

  /**
   * True at every index that falls inside at least one blanker range.
   */
  private static boolean[] buildBlankerMask(int n, @Nullable List<int[]> blankedRanges) {
    boolean[] mask = new boolean[n];
    if (blankedRanges != null && !blankedRanges.isEmpty()) {
      for (int[] range : blankedRanges) {
        int lo = Math.max(0, range[0]);
        int hi = Math.min(n - 1, range[1]);
        for (int i = lo; i <= hi; i++) {
          mask[i] = true;
        }
      }
    }
    return mask;
  }

  /**
   * Returns elements of intensity where inBlanker[i] == true.
   */
  private static double[] getBlankedData(double[] intensity, boolean[] blankerMask) {
    double[] result = new double[0];
    int count = 0;
    if (intensity.length == blankerMask.length) {
      for (boolean bm : blankerMask) {
        if (bm) {
          count++;
        }
      }
      result = new double[count];
      int idx = 0;
      for (int i = 0; i < intensity.length; i++) {
        if (blankerMask[i]) {
          result[idx] = intensity[i];
          idx++;
        }
      }
    }
    return result;
  }

  /**
   * Returns elements of intensity where inBlanker[i] == false.
   */
  private static double[] getNonBankedData(double[] intensity, boolean[] blankerMask) {
    double[] result = new double[0];
    int count = 0;
    if (intensity.length == blankerMask.length) {
      for (boolean bm : blankerMask) {
        if (!bm) {
          count++;
        }
      }
      result = new double[count];
      int idx = 0;
      for (int i = 0; i < intensity.length; i++) {
        if (!blankerMask[i]) {
          result[idx] = intensity[i];
          idx++;
        }
      }
    }
    return result;
  }

  public static void copyToClipboard(double[] values) {
    StringBuilder sb = new StringBuilder();

    for (double value : values) {
      sb.append(value).append(System.lineSeparator());
    }

    Toolkit.getDefaultToolkit()
        .getSystemClipboard()
        .setContents(new StringSelection(sb.toString()), null);
  }

}
