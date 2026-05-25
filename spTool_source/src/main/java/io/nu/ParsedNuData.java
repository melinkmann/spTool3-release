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

import dataModelNew.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import util.ArrUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class ParsedNuData implements Serializable {

  @Serial
  long serialVersionUID = 1_000_000L;

  public static final Logger LOGGER = LogManager.getLogger(ParsedNuData.class);
  /**
   * Default maximum allowed distance (Da) between a requested m/z and the nearest
   * recorded channel when no explicit tolerance is supplied.
   */
  private static final double DEFAULT_MAX_MASS_DIFF_DA = 0.1;

  private final boolean isValid;
  private final double[] massToChargeRatios;
  private final double[][] signals;
  private final double dwellTime;
  private final Map<Integer, List<int[]>> autoblankedRegions;

  // Dummy constructor
  public ParsedNuData() {
    this.isValid = false;
    this.massToChargeRatios = new double[0];
    this.signals = new double[0][0];
    this.dwellTime = 0;
    this.autoblankedRegions = new HashMap<>();
  }

  public ParsedNuData(double[] massToChargeRatios, double[][] signals, double dwellTime,
                      Map<Integer, List<int[]>> autoblankedRegions) {
    this.massToChargeRatios = massToChargeRatios;
    this.isValid = true;
    this.signals = signals;
    this.dwellTime = dwellTime;
    this.autoblankedRegions = autoblankedRegions;
  }

  /// When retrieving all series, e.g., to compute spectra, we do not want to create
  /// memory-mapped storage files for each. Returns all channels in original column order.
  public List<ParsedNuDataArray> getIsotopeArrayRAM() {
    List<ParsedNuDataArray> serList = new ArrayList<>();

    if (isValid) {
      for (int c = 0; c < massToChargeRatios.length; c++) {
        double mz = massToChargeRatios[c];

        // Deep copy: the column slice is independent of the cached signal matrix.
        double[] channelIntensity = extractColumnFrom2DArray(signals, c);
        // TISeries computes mean, median, ... internally which we do not need for spectra.
        List<int[]> colRanges = autoblankedRegions.getOrDefault(c, new ArrayList<>());
        serList.add(new ParsedNuDataArray(mz, mz, channelIntensity, colRanges, dwellTime));
      }
    }

    return serList;
  }

  /// Lazily yields one channel at a time — avoids holding all column slices in memory.
  public Iterator<ParsedNuDataArray> getIsotopeArrayIterator() {
    if (!isValid) {
      return Collections.emptyIterator();
    }

    return new Iterator<>() {
      private int c = 0;

      @Override
      public boolean hasNext() {
        return c < massToChargeRatios.length;
      }

      @Override
      public ParsedNuDataArray next() {
        if (!hasNext()) throw new NoSuchElementException();
        double mz = massToChargeRatios[c];
        double[] channelIntensity = extractColumnFrom2DArray(signals, c);
        List<int[]> colRanges = autoblankedRegions.getOrDefault(c, new ArrayList<>());
        c++;
        return new ParsedNuDataArray(mz, mz, channelIntensity, colRanges, dwellTime);
      }
    };
  }

  /// When retrieving all series, e.g., to compute spectra, we do not want to create
  /// memory-mapped storage files for each. Returns all channels in original column order.
  public List<ParsedNuDataResult> getIsotopeDataRAM() {
    List<ParsedNuDataResult> serList = new ArrayList<>();

    if (isValid) {
      for (int c = 0; c < massToChargeRatios.length; c++) {
        double mz = massToChargeRatios[c];

        // Deep copy: the column slice is independent of the cached signal matrix.
        double[] channelIntensity = extractColumnFrom2DArray(signals, c);
         TISeries tiSeries = new DTISeriesRAM(dwellTime, channelIntensity);
        List<int[]> colRanges = autoblankedRegions.getOrDefault(c, new ArrayList<>());
        serList.add(new ParsedNuDataResult(mz, mz, tiSeries, colRanges));
      }
    }

    return serList;
  }



  /// When loading data, we want them as HDD series to store them in a sample.
  public List<ParsedNuDataResult> getIsotopeData(List<Double> mzValues) {
    List<ParsedNuDataResult> serList = new ArrayList<>();

    if (isValid) {
      // iterate over all mz
      double[] allMasses = ArrUtils.copy(massToChargeRatios);
      List<Double> requestedMz = new ArrayList<>(mzValues);

      // Match requested m/z to column indices
      List<MzMatch> matches = matchMzToColumns(requestedMz, allMasses, DEFAULT_MAX_MASS_DIFF_DA);

      // Assemble per-channel TISeries and blanking metadata
      // extractColumnFrom2DArray produces a fresh copy each time, so the caller
      // cannot corrupt the cached signal matrix.
      for (int r = 0; r < requestedMz.size(); r++) {
        MzMatch m = matches.get(r);
        if (!m.found()) {
          LOGGER.warn("readSelectedChannels: no match for requested m/z "
              + requestedMz.get(r) + " Da"
              + "; nearest channel is " + m.nearestMz + " Da"
              + " (gap " + String.format("%.4f", m.gap) + " Da,"
              + " tolerance " + DEFAULT_MAX_MASS_DIFF_DA + " Da); skipping");
          continue;
        }

        double matchedMz = allMasses[m.colIdx];
        // Deep copy: the column slice is independent of the cached matrix.
        double[] channelIntensity = extractColumnFrom2DArray(signals, m.colIdx);

        TISeries tiSeries = new DTISeriesHDD(dwellTime, channelIntensity);
        List<int[]> colRanges = autoblankedRegions.getOrDefault(m.colIdx, new ArrayList<>());
        serList.add(new ParsedNuDataResult(m.requestedMz, matchedMz, tiSeries, colRanges));
      }
    }
    return serList;
  }

  public Map<Integer, List<int[]>> getAutoblankedRegions() {
    return autoblankedRegions;
  }

  public double[] getMassToChargeRatios() {
    return massToChargeRatios;
  }

  public double[][] getSignals() {
    return signals;
  }

  public boolean isValid() {
    return isValid;
  }

  /// /////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////// HELPER METHODS //////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////


  private record MzMatch(int colIdx, double requestedMz, double matchedMz, double nearestMz, double gap) {
    boolean found() {
      return colIdx >= 0;
    }
  }

  /**
   * Matches each requested m/z to the nearest column in allMasses, capturing
   * enough information for both channel selection and diagnostic logging in a
   * single pass. Returns one MzMatch per requested m/z, in the same order.
   * Unmatched entries (gap exceeds maxMassDiffDa) have colIdx == -1.
   */
  private static List<MzMatch> matchMzToColumns(
      List<Double> requestedMz,
      double[] allMasses,
      double maxMassDiffDa) {

    List<MzMatch> matches = new ArrayList<>(requestedMz.size());

    for (double target : requestedMz) {

      // Find the nearest column by linear scan over allMasses.
      int bestIdx = -1;
      double bestDiff = Double.MAX_VALUE;

      for (int c = 0; c < allMasses.length; c++) {
        double diff = Math.abs(allMasses[c] - target);
        if (diff < bestDiff) {
          bestDiff = diff;
          bestIdx = c;
        }
      }

      // Accept the match only if it falls within the allowed tolerance.
      if (bestIdx >= 0 && bestDiff <= maxMassDiffDa) {
        matches.add(new MzMatch(bestIdx, target, allMasses[bestIdx], allMasses[bestIdx], bestDiff));
      } else {
        // Record a miss: colIdx == -1 signals no usable match to the caller.
        // nearestMz is still populated (when any mass exists) so the caller
        // can report the closest candidate in its warning message.
        double nearestMz = (bestIdx >= 0) ? allMasses[bestIdx] : Double.NaN;
        matches.add(new MzMatch(-1, target, Double.NaN, nearestMz, bestDiff));
      }
    }

    return matches;
  }

  /**
   * Extracts one column from a row-major 2-D array as a fresh copy.
   *
   * @param matrix [row][col]
   * @param col    column index to extract
   * @return independent 1-D array of length {@code matrix.length}
   */
//  private static double[] extractColumnFrom2DArray(double[][] matrix, int col) {
//    double[] column = new double[matrix.length];
//    for (int i = 0; i < matrix.length; i++) {
//      column[i] = matrix[i][col];
//    }
//    return column;
//  }
  private static double[] extractColumnFrom2DArray(double[][] matrix, int col) {
    return Arrays.copyOf(matrix[col], matrix[col].length);
  }

  /// Container to return both TISeries and blanked regions.
  public record ParsedNuDataResult(double requestedMZ, double tofMZ, TISeries tiSeries, List<int[]> blanker) {
  }

  /// Container to return both TISeries and blanked regions.
  public record ParsedNuDataArray(double requestedMZ, double tofMZ, double[] intensity, List<int[]> blanker, double dwellTime) {
  }

}
