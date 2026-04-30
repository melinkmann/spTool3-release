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

/*
 *  Java port of spcal/io/nu.py  (https://github.com/djdt/spcal)
 *  Original Python by djdt – ported to Java 15 for spTool3.
 */

package io.nu;

import java.util.List;

/**
 * Written by claude sonnet 4.6 to create Java port of spcal nu parser  (https://github.com/djdt/spcal)
 * Original Python by djdt – ported to Java 15 for spTool.
 * Immutable container for all parameters read from the {@code run.info} JSON file
 * in a Nu Instruments ICP-ToF data directory.
 *
 * <p>Instances are created exclusively by {@link NuReader_v1}.
 */

public final class NuRunInfo {

  // -------------------------------------------------------------------------
  // Mass-calibration
  // -------------------------------------------------------------------------

  /**
   * Two-element [a, b] such that sqrt(m/q) = a + t * b.
   * <p>
   * Explanation by claude sonnet 4.6:
   * massCalCoefficients [a, b] are the core time-to-mass calibration.
   * The relationship is √(m/q) = a + t·b, so m/q = (a + t·b)².
   * Every peak _centre time_ coming out of the .integ files goes through this to become
   * a Da value.
   */
  public final double[] massCalCoefficients;

  /**
   * Explanation by claude sonnet 4.6:
   * Two-element [a, b] blanker-open mass-calibration coefficients.
   * <p> blMassCalStartCoef [a, b] and blMassCalEndCoef [a, b] —
   * the same style of calibration as the main mass cal but specifically for the blanker hardware.
   * The blanker physically blocks the detector for certain mass windows
   * when a signal is too intense (overrange). These two coefficient pairs define where the blanker opened
   * and closed in mass space respectively, so we can record which regions were suppressed.
   *
   */
  public final double[] blMassCalStartCoef;

  /**
   * Two-element [a, b] blanker-close mass-calibration coefficients.
   */
  public final double[] blMassCalEndCoef;

  // -------------------------------------------------------------------------
  // Accumulations
  // -------------------------------------------------------------------------

  /*
  numAccumulations1 and numAccumulations2 — the Nu instrument fires the ToF pulser many times per
  "acquisition" to build up signal. These two numbers multiply together to give the total number of
  individual ToF shots averaged into one data point. Their product (totalAccumulations()) is the divisor
  used to convert raw acquisition numbers into time-point indices, and it feeds into the dwell time
  calculation.
   */
  public final int numAccumulations1;
  public final int numAccumulations2;

  /**
   * Mean single-ion area used to convert raw ADC counts → ion counts.
   */
  public final double averageSingleIonArea;

  // -------------------------------------------------------------------------
  // Segment descriptors
  // -------------------------------------------------------------------------

  public final List<SegmentInfo> segmentInfoList;

  // -------------------------------------------------------------------------
  // Package-private constructor – only NuReader creates instances
  // -------------------------------------------------------------------------

  NuRunInfo(
      double[] massCalCoefficients,
      double[] blMassCalStartCoef,
      double[] blMassCalEndCoef,
      int numAccumulations1,
      int numAccumulations2,
      double averageSingleIonArea,
      List<SegmentInfo> segmentInfoList) {

    this.massCalCoefficients = massCalCoefficients;
    this.blMassCalStartCoef = blMassCalStartCoef;
    this.blMassCalEndCoef = blMassCalEndCoef;
    this.numAccumulations1 = numAccumulations1;
    this.numAccumulations2 = numAccumulations2;
    this.averageSingleIonArea = averageSingleIonArea;
    this.segmentInfoList = List.copyOf(segmentInfoList);
  }

  // -------------------------------------------------------------------------
  // Derived helpers
  // -------------------------------------------------------------------------

  /**
   * Product of both accumulation counts — total accumulations per acquisition.
   */
  public int totalAccumulations() {
    return numAccumulations1 * numAccumulations2;
  }

  /**
   * Dwell time in seconds: first-segment acquisition period × total accumulations,
   * rounded to the nearest nanosecond (mirrors Python {@code np.around(..., 9)}).
   */
  public double dwellTimeSeconds() {
    double result = 0.0;
    if (!segmentInfoList.isEmpty()) {
      // MElin: NU uses nanoseconds here.
      double acqPeriodS = segmentInfoList.get(0).acquisitionPeriodNs * 1e-9;
      double raw = acqPeriodS * totalAccumulations();
      result = Math.round(raw * 1e9) / 1e9;
    }
    return result;
  }

  public int acquisitionCountForSegment(int segNum) {
    for (SegmentInfo s : segmentInfoList) {
      if (s.num == segNum) return s.acquisitionCount;
    }
    throw new IllegalArgumentException("NuRunInfo: unknown segment number: " + segNum);
  }

  // -------------------------------------------------------------------------
  // Nested segment descriptor
  // -------------------------------------------------------------------------

  /**
   * Mirrors one entry in the {@code SegmentInfo} JSON array of {@code run.info}.
   */

  /*
  Explanation by claude sonnet 4.6:
  segmentInfoList — a Nu run can be divided into segments, each covering a different mass range or timing
  configuration. Each SegmentInfo has:
  ->num — the segment index (1-based), used to look up the right delay when converting peak times to masses
  -> acquisitionPeriodNs — how long one single accumulation lasts in nanoseconds. Multiplied by
  -> totalAccumulations() this gives the dwell time — the real-world time represented by one row in your
  signal matrix
  -> acquisitionTriggerDelayNs — a fixed time offset added to every peak centre time before the mass
  calibration is applied. Each segment has its own delay because different mass windows are reached at
  different flight times; without this correction, all masses in non-primary segments would be wrong.

  I asked for a more detailed explanation and here it is:
  >>> The ToF pulser fires, ions fly down the tube, and the detector starts a clock. But the detector doesn't
  necessarily start counting from the moment the pulse fires — there's a configurable trigger delay: a dead
  time at the start of the flight window where the detector is simply not listening. This shifts the
  entire time axis of that segment.
  So when you read a raw peak centre time t from the .integ file, what you actually have is:
  t_raw = t_true_flight_time - trigger_delay
  The conversion back is what the code does:
  javadouble adjustedTime = centre * 0.5 + delay;
  Without adding delay back in, every mass in that segment would be calculated as if the ions flew for less
  time than they actually did, and all masses would come out too low.
  The reason different segments have different delays is that each segment is configured to listen to a
  different slice of the flight-time spectrum — an early window for light elements, a later window for heavy
  elements. Each window has its own trigger delay set by the instrument operator.

  I asked why 0.5.
  >>> Python actually says:
  pythonmasses = (integ["result"]["center"] * 0.5) + delays[:, None]
  The * 0.5 is there in the original Python too, but the spcal source doesn't explain it. My honest answer
  is: I don't know for certain why it's 0.5 specifically.
  The most plausible explanation is a hardware clock unit conversion — the raw centre value stored in the
  .integ file is in some internal ToF clock unit where one tick = 2 ns (or similar), so multiplying by 0.5
  converts to the same nanosecond units that acquisitionTriggerDelayNs is expressed in. That would make the
  addition of the delay dimensionally consistent.


   */

  public static final class SegmentInfo {

    public final int num;
    public final double acquisitionPeriodNs;
    public final double acquisitionTriggerDelayNs;
    public final int acquisitionCount;

    SegmentInfo(int num, double acquisitionPeriodNs, double acquisitionTriggerDelayNs, int acquisitionCount) {
      this.num = num;
      this.acquisitionPeriodNs = acquisitionPeriodNs;
      this.acquisitionTriggerDelayNs = acquisitionTriggerDelayNs;
      this.acquisitionCount = acquisitionCount;
    }
  }
}
