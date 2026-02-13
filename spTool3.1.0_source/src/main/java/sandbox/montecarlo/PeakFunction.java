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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PeakFunction implements UnivariateFunction {

  public static final Logger LOGGER = LogManager.getLogger(PeakFunction.class.getName());

  // Some default parameter suggestions
  public static final double D_MU_MED = 60;
  public static final double D_SD_MED = 8;
  public static final double V_MU_MED = 24;
  public static final double V_SD_MED = 5.0;
  public static final double YPos_MU_MED = 15;
  public static final double YPos_SD_MED = 15;

  //  Implementation

  // Normalization constant
  private final double c0;
  // Diffusion coefficient of the respective element. Larger value means broader peaks.
  private final double d;
  // Position of (instantaneous) vaporization of the particle. Larger values -> broader peaks.
  // Olesik's model equation considers on-axis vaporization only.
  // It does not consider nozzle effects, change of flow speed towards the orifice, Bernoulli, ...
  // Here, I propose assuming a lognormal distribution to account for the 3D geometry of the point around the axis.
  private final double y_vap;
  // Linear gas velocity. Higher values mean narrower peaks.
  private final double v;
  private final double factor1;
  private final double factor2;

  // For integration
  private final double timeAtMaximum;
  private final double smallestInteg = 1E-9;
  private final double largestInteg;
  private final double fwhm;
  private final double icl;

  public PeakFunction(double c_0, double d, double y_vap, double v) {
    this.c0 = c_0;
    this.d = d;
    this.y_vap = y_vap;
    this.v = v;
    this.factor1 = calc_f1(c_0, d);
    this.factor2 = calc_f2(d);
    // Peak position (via derivative and zero of the latter)
    this.timeAtMaximum =
        (Math.sqrt(16 * Math.pow(v, 2) * Math.pow(y_vap, 2) + 9 * Math.pow(factor2, 2))
            - 3 * factor2) / (4 * Math.pow(v, 2));

    // https://doi.org/10.1021/ac951013b --> unsure if the eq is really correct.
    // this.fwhm = (4 / Math.pow(v, 2)) * Math.sqrt(d * Math.log(2) * (d * Math.log(2) + v * y_vap));
    this.icl = estimateWidthAtHeight(5);
    this.fwhm = estimateWidthAtHeight(50);

    this.largestInteg = timeAtMaximum + 2 * icl;
  }

  public PeakFunction(double d, double y_vap, double v) {
    this(1.0, d, y_vap, v);
  }


  /**
   * Some good values from testing.
   */
  public PeakFunction() {
    this(1, 0.006, 0.015, 24);
  }

  public double getLargestInteg() {
    return largestInteg;
  }

  public double getFwhm() {
    return fwhm;
  }

  public double getIcl() {
    return icl;
  }

  /**
   * Analytical expression may be available but honestly, we do not need better precision.
   */
  public double estimateWidthAtHeight(double percent) {
    double height = value(timeAtMaximum) * percent;
    double heightPct = height / 100;

    double step = 1E-6; // 1 µs

    double timeRight;
    double timeLeft;

    if (percent < 50) {
      timeRight = timeAtMaximum + fwhm / 2;
      timeLeft = timeAtMaximum - fwhm / 2;
    } else {
      timeRight = timeAtMaximum;
      timeLeft = timeAtMaximum;
    }

    double val = height;

    while (val > heightPct) {
      timeRight += step;
      val = value(timeRight);
    }

    val = height;
    while (val > heightPct) {
      timeLeft -= step;
      val = value(timeLeft);
    }

    double width = timeRight - timeLeft;
    return width;
  }

  // Avoid performing the same multiplications for each of the 1E6 data points -> pre-calculate factors
  public static double calc_f1(double c0, double d) {
    return c0 / (8 * Math.pow(Math.PI * d, 1.5));
  }

  public static double calc_f2(double d) {
    return 4 * d;
  }

  /**
   * The actual equation
   */
  public static double diffusionEquation(
      double t,
      double factor1,
      double factor2,
      double y_vap,
      double v) {
    return factor1 / (Math.pow(t, 1.5))
        * Math.exp(-Math.pow(y_vap - v * t, 2) / (factor2 * t));
  }


  @Override
  public double value(double time) {
    return diffusionEquation(time, factor1, factor2, y_vap, v);
  }


  private double integrateSliceOfPeak(BaseAbstractUnivariateIntegrator integrator,
                                      double start, double stop) {

    double area;

    if (start == stop) {
      LOGGER.error("Lower limit was equal to upper limit = " + start +
          ". Peak time index was " + stop + ". Area=0 was returned.");
      area = 0;
    } else if (start > stop) {
      LOGGER.error("Lower limit: " + +start + "  was larger than upper limit: " + stop +
          ". Peak time index was " + stop + ". Area=0 was returned.");
      area = 0;
    } else {
      try {
        area = integrator.integrate((int) 1E6, this, start, stop);
      } catch (Exception e) {
        try {
          area = integrator.integrate((int) 1E8, this, start, stop);
        } catch (Exception ee) {
          area = 0;
          LOGGER.error(
              "Integration failed possibly due to too many iterations. Area=0 was returned. "
                  + "Message: " + e.getMessage()
                  + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
          LOGGER.error("D= " + d + ", \t y= " + y_vap + ", \tv= " + v);
        }
      }
    }
    return area;
  }


  public DataList<Integer> integrateEntirePeak(
      BaseAbstractUnivariateIntegrator integrator,
      double dwellTime,
      double finalTimeStampGlobally,
      int frameSize,
      double peakTime) {

    DataList<Integer> values = new DataList<>();

    // make sure everything is covered for larger DT
    double largestIntegTime = Math.max(largestInteg, 10 * dwellTime);

    /*
     There may be cases, esp. in project exports before version 3.0.14,
     where there are peak time stamps outside of the of time frame.
     Why ? See quote in the MonteCarloGeneratorTask:
     <<<<
              Important: We must limit the particle appearance to the macro frame.
              Otherwise, we may obtain cases where particle peaks occur in the minuscule gap between
              that largest possible macro DT and the final time stamp, and the largest possible micro DT
              and the final time stamp. What do I mean?
              Assume DT = 15 µs and micro = 1 µs, duration = 35 µs.
              In macro frame, we have 15 30.
              In micro frame, we have 1,2, ... 30,31,32,34,35.
              As we start with the micro frame and only later group it to the macro frame, there is a chance
              to allow NP events to occur in the window 30-35 when the lastTimeStamp is taken from micro frame.
              If events occur in this region, which later is cut out during grouping stage,
              we have NP events whose peak time is after the end of the sample.
              Why? The grouping truncates the data!
              When later we try to integrate these peaks for histograms or export (in retrospect),
              we get "array out of bounds" when exporting the event markers,
              and we get "bad integration limits" during integration as start>stop...
     >>>>
     Hence, we will just check here:
     */
    if (peakTime > finalTimeStampGlobally) {
      LOGGER.warn("There was an event with a peak time after the end of the time series. "
          + "This is a rare edge case which is probably fixed in the latest version. "
          + "You are likely seeing this warning because you loaded data created in an earlier version. "
          + "Peak time=" + peakTime + " and final time stamp=" + finalTimeStampGlobally + ". "
          + "The time series was extended during integration to match exactly the event peak time stamp.");
      finalTimeStampGlobally = peakTime;
    }


    /*
     Find where the peak is with respect to the dwell time frame.

     Example: DT = 10 ms, event at 17.2 ms.
     DT time frame has no zero, i.e., it goes 10, 20, 30, ...
     Hence, 17.2 would be represented at DT = 20 ms;
     Thus we have to calc Math.ceil(t/DT)-1,
     which is 17.2/10 = 1.72 --> ceil --> 2 --> "-1" --> 1.
     Look at the DT frame:
      10, 20, 30, ...
      0,  1,  2,
     For idx=1, the event at 17.2 will be correctly shown at the DT = 20 ms data point.

     Which time does this correspond to? Simple: (index+1) · DT.
     E.g., (idx=1 + 1) * 10 = 20 ms.

     Now, we want to put the entire peak on the frame, which also extends before the DT with the Peak.
    */

    int peakDtIdx = (int) Math.ceil(peakTime / dwellTime) - 1;
    double dtStampJustAbovePeakTime = (peakDtIdx + 1) * dwellTime;
    // Make sure that this number does not exceed the final time stamp of the frame.
    dtStampJustAbovePeakTime = Math.min(dtStampJustAbovePeakTime, finalTimeStampGlobally);
    double dtStampJustBelowPeakTime = peakDtIdx * dwellTime;
    // Make sure that this number is not below the smallest possible integration window (and not 0)
    dtStampJustBelowPeakTime = Math.max(dtStampJustBelowPeakTime, smallestInteg);

    /*
    Note: These time points are on the global time frame of the simulation.
    Peak integration, however, always is done from 'zero to the end of the peak'.
    In order to translate those 'relative' positions in the DT time frame
    to the mathematically defined time frame of the peak function,
    we need to know the distance of the peak position relative to the neighboring dwell times.
     */
    double upperDifference = dtStampJustAbovePeakTime - peakTime;
    double lowerDifference = peakTime - dtStampJustBelowPeakTime;

    /*
    Now, integration of the peak needs to be done starting from the
    maximum of the peak and in 2 steps, to the left and to the right (separately).
    Note that for the DT window in which the peak sits, we need to integrate that window "first"
    and then proceed normally.
     */

    // (1) DT with the peak maximum:
    double lowerLimit = Math.max(smallestInteg, timeAtMaximum - lowerDifference);
    double upperLimit = Math.min(largestIntegTime, timeAtMaximum + upperDifference);

    double centerArea = integrateSliceOfPeak(integrator, lowerLimit, upperLimit);
    values.add(peakDtIdx, centerArea);

    // (2) lower end of the peak
    // Get old positions and then later just move each time by one DT
    double left_lowerLimit = lowerLimit;
    double left_upperLimit = upperLimit;
    int left_dtIdx = peakDtIdx;

    // Are we still above the smallest possible point of integration?
    // The index will be decremented... is it still >=0 after incrementing? Else: Peak outside of frame.
    while (left_lowerLimit > smallestInteg && 1 <= left_dtIdx) {
      left_dtIdx -= 1;

      left_lowerLimit -= dwellTime;
      left_upperLimit -= dwellTime;

      // Avoid cases, where the upper limit is negative; this can happen, at DT = 1 ms, i.e., DT larger than the peaks
      if (left_lowerLimit < 0 && left_upperLimit < 0) {
        break;
      }

      // Else: check and correct lower limit (upper must be right implicitly)
      left_lowerLimit = Math.max(left_lowerLimit, smallestInteg);

      centerArea = integrateSliceOfPeak(integrator, left_lowerLimit, left_upperLimit);
      values.add(left_dtIdx, centerArea);
    }

    // (3) upper end of the peak
    /*
     Get old positions  (of the center) and then later just move each time by one DT.
     Note: both start at the upper limit, but the right_upperLimit is directly incremented in the loop.
     */
    double right_lowerLimit = upperLimit;
    double right_upperLimit = upperLimit;
    int right_dtIdx = peakDtIdx;

    // Are we still below the largest possible point of integration? "right_upperLimit < largestInteg"
    // And: Is the integration still within the DT time frame? "right_dtIdx <= frameSize-2"
    //      Why -2? Because "-1" is length->index and we increment directly in the loop, i.e.,
    //      we must be smaller or equal by 2, as the next iteration will then yield idx=size-1.
    while (right_upperLimit < largestIntegTime && right_dtIdx <= frameSize - 2) {
      right_dtIdx += 1;

      right_upperLimit += dwellTime;
      right_upperLimit = Math.min(right_upperLimit, largestIntegTime);

      centerArea = integrateSliceOfPeak(integrator, right_lowerLimit, right_upperLimit);
      values.add(right_dtIdx, centerArea);

      right_lowerLimit += dwellTime;
    }
    // finally, sort
    values.sort();
    return values;
  }

  /**
   * @param totalArea: Target value for the area, i.e., after normalization, the peak will have this
   *                   area.
   */
  public static DataList<Integer> normalizeArea(DataList<Integer> peakData, double totalArea) {
    double integSum = peakData.ySum();
    double factor = totalArea / integSum;
    final DataList<Integer> copy = peakData.copy();
    copy.normalizeOverriding(factor);
    return copy;
  }

  public static DataList<Integer> normalizeHeight(DataList<Integer> peakData, double targetHeight) {
    double max = peakData.yMax();
    double factor = targetHeight / max;
    final DataList<Integer> copy = peakData.copy();
    copy.normalizeOverriding(factor);
    return copy;
  }


  public double getFactor1() {
    return factor1;
  }

  public double getFactor2() {
    return factor2;
  }

  public double getV() {
    return v;
  }

  public double getY_vap() {
    return y_vap;
  }

  /// /////////////////////////////////////////////////////////////////////

//
//ParametricUnivariateFunction
//  @Override
//  public double value(double t, double... p) {
//    double factor1 = p[0]; // factor1
//    double factor2 = p[1]; // factor2
//    double y = p[2]; // y_vap
//    double v = p[3]; // v
//
//    double exponent = -Math.pow(y - v * t, 2) / (factor2 * t);
//    return factor1 / Math.pow(t, 1.5) * Math.exp(exponent);
//  }
//
//  @Override
//  public double[] gradient(double t, double... p) {
//    double factor1 = p[0];// factor1
//    double factor2 = p[1];// factor2
//    double y = p[2];// y_vap
//    double v = p[3];// v
//
//    double diff = y - v * t;
//    double t32 = Math.pow(t, 1.5);
//    double exp = Math.exp(-diff * diff / (factor2 * t));
//
//    double f = factor1 / t32 * exp;
//
//    double dFactor1 = exp / t32;
//    double dFactor2 = f * (diff * diff) / (factor2 * factor2 * t);
//    double dy = f * (-2.0 * diff) / (factor2 * t);
//    double dv = f * (2.0 * diff) / factor2;
//
//    return new double[]{dFactor1, dFactor2, dy, dv};
//  }


}


