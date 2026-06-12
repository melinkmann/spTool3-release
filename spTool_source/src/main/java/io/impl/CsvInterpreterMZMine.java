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

package io.impl;

import analysis.RawProcessingUtils;
import com.opencsv.CSVReader;
import dataModelNew.*;
import dataModelNew.mz.*;
import io.FileInterpreterUtils;
import io.PathUtil;
import io.nu.ShapeEstimator;
import math.stat.Mode;
import math.units.Unit;
import math.units.enums.ViewUnits;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EICNormalisation;
import processing.options.SignalConversionOption;
import processing.options.Source;
import processing.parameterSets.impl.CsvInterpreterParams;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;
import util.NF;
import util.SnF;

import java.nio.file.Path;
import java.util.*;

public class CsvInterpreterMZMine implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterMZMine.class);

  private final CsvInterpreterParams params;

  private Unit unit = ViewUnits.NONE;
  private final List<Sample> samples;

  public CsvInterpreterMZMine(CsvInterpreterParams params) {
    this.samples = new ArrayList<>();
    this.params = params;
  }

  @Override
  public Unit getUnit() {
    return unit;
  }


  @Override
  public List<Sample> getSamples() {
    return samples;
  }

  @Override
  public void parse(Path file) {
    final List<String[]> csv = new ArrayList<>();
    try {
      CSVReader reader = FileInterpreterUtils.buildReader(file, params.getDelimiter(),
          params.getCharSet());

      csv.addAll(reader.readAll());

      // strips empty white space at end as MZmine ends in delimiter
      csv.replaceAll(CsvInterpreterMZMine::stripTrailingEmpty);

      reader.close();
    } catch (Exception e) {
      LOGGER.warn("CSVReader failed to read csv due to wrong separator or character encoding."
          + " Is the file readable? This error has nothing to do with the column order. "
          + "Message: " + e.getMessage()
          + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }

    if (!csv.isEmpty()) {
      List<Trace> traces = new ArrayList<>();
      List<Channel> mzChannels = new ArrayList<>();

      try {
        // find precision
        int digitsPrecision = params.getDigitsPrecision().getValue();
        NF nf;
        switch (digitsPrecision) {
          case 0 -> nf = NF.D1C0;
          case 1 -> nf = NF.D1C1;
          case 2 -> nf = NF.D1C2;
          case 3 -> nf = NF.D1C3;
          case 4 -> nf = NF.D1C4;
          case 5 -> nf = NF.D1C5;
          case 6 -> nf = NF.D1C6;
          default -> nf = NF.D1C3;
        }

        EICNormalisation normalisation = params.getEicNormalisation().getValue();

        // Find Name.
        String sampleName = PathUtil.removeExtension(file.getFileName().toString());
        // files have .mzml.csv signature
        if (sampleName.contains(".mzML")) {
          sampleName = PathUtil.removeExtension(sampleName);
        }
        if (sampleName.isEmpty()) {
          sampleName = "No name available";
        }
        SampleFile sampleFile = new SampleFile(file, sampleName);


        int headerLineIndex = 0;

        // Find MZ.
        int firstMZCol = 1;
        for (int i = firstMZCol; i < csv.get(headerLineIndex).length; i++) {
          if (i < csv.get(headerLineIndex).length) {
            String mzStr = csv.get(headerLineIndex)[i];
            double mz = parseChannel(mzStr);
            mzChannels.add(new MZChannel(new MSIDImpl(new MZImpl(mz, nf))));
          }
        }

        List<Double> time = new ArrayList<>();
        List<List<Double>> allIntensitySeries = new ArrayList<>();


        // assume first line
        for (int lineIdx = headerLineIndex + 1; lineIdx < csv.size(); lineIdx++) {

          // time
          String[] line = csv.get(lineIdx);
          if (line.length > 1) {
            time.add(minToSec(SnF.strToDoubleSilent(line[0])));
            for (int colIdx = 1; colIdx < line.length; colIdx++) {
              if (allIntensitySeries.size() < line.length - 1) {
                List<Double> intensity = new ArrayList<>();
                intensity.add(SnF.strToDoubleSilent(line[colIdx]));
                allIntensitySeries.add(intensity);
              } else {
                allIntensitySeries.get(colIdx - 1).add(SnF.strToDoubleSilent(line[colIdx]));
              }
            }
          }
        }

        // Clarify normalisation
        double globalNonzeroMinimum = 0; // mean value
        List<Double> globalNonzeroMinima = new ArrayList<>();
        for (List<Double> intensity : allIntensitySeries) {
          double min = 0;
          // else, empty list throws no such element exception
          List<Double> nonzeros = ArrUtils.nonzero(intensity);
          if (!nonzeros.isEmpty()) {
            min = Collections.min(nonzeros);
          }
          if (min > 0) {
            globalNonzeroMinima.add(min);
          }
        }
        // Mode should be relatively robust to yield the best value
        globalNonzeroMinimum = Mode.calculateMode(globalNonzeroMinima);

        Sample sample = new SampleImpl(sampleName, sampleFile);
        if (mzChannels.size() == allIntensitySeries.size()) {
          for (int i = 0; i < mzChannels.size(); i++) {
            // Create sample.
            double[] timeArr = ArrUtils.doubleListToArr(time);
            double[] y = ArrUtils.doubleListToArr(allIntensitySeries.get(i));
            // boolean convertToCts = RawProcessingUtils.checkConversion(conversion, unit, y);
            // if (convertToCts) {
            //   LOGGER.trace("Check for unit conversion: applying conversion to counts.");
            //   y = RawProcessingUtils.cpsToCts(timeArr, y);
            // }

            switch (normalisation) {
              case NONE -> {
              }
              case SUBTRACT -> {
                double min = ArrUtils.getMin(ArrUtils.nonzero(y));
                // if this EIC has no values close to zero (too much BG) it will be greater than the mode
                min = Math.min(min, globalNonzeroMinimum);

                // if min = 10, then we want to subtract 9
                min += 1;
                for (int idx = 0; idx < y.length; idx++) {
                  y[idx] = Math.max(0, y[idx] - min);
                }
              }
              // case DIVIDE -> {
              //   double min = ArrUtils.getMin(ArrUtils.nonzero(y));
              //   // if this EIC has no values close to zero (too much BG) it will be greater than the mode
              //   min = Math.min(min, globalNonzeroMinimum);

              //   // if min = 10, then we divide by 10
              //   if (min > 0) {
              //     y = ArrUtils.divide(y, min);
              //   }
              // }
            }

            // Estimate SIA shape
            double sia = ShapeEstimator.computeShape(y, null);
            if (!Double.isFinite(sia)) {
              sia = 0;
            }

            TISeries tiSeries = new TISeriesHDD(timeArr, y);
            Trace trace = new TraceImpl(sample, mzChannels.get(i), tiSeries, sia);
            traces.add(trace);
          }
        } else {
          LOGGER.error("Cannot add samples as there are not as many isotopes as intensity series found.");
        }
        traces.forEach(sample::addTrace);

        this.samples.clear();
        if (!traces.isEmpty()) {
          this.samples.add(sample);
        }

      } catch (Exception e) {
        LOGGER.error("CSVReader parsing was not successful. "
            + "This error is likely due to wrong column order. ", e);
      }
    }
  }


  @Override
  public List<String[]> readPreview(Path file, int lines) {
    List<String[]> previewLines = new ArrayList<>();
    try {
      CSVReader reader = FileInterpreterUtils.buildReader(
          file,
          params.getDelimiter(),
          params.getCharSet());

      for (int i = 0; i < lines; i++) {
        previewLines.add(reader.readNext());
      }
      reader.close();
    } catch (Exception e) {
      LOGGER.warn("CSVReader failed to read csv due to wrong separator or character encoding. "
          + "Check if the file is readable. This error has nothing to do with the column order. "
          + "Message: " + e.getMessage()
          + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }
    return previewLines;
  }

  public static double parseChannel(String input) {
    String parsedStr = "";

    if (input != null && !input.isEmpty()) {
      int bracketIndex = input.indexOf('(');

      if (bracketIndex == -1) {
        parsedStr = input.trim();
      } else {
        parsedStr = input.substring(0, bracketIndex).trim();
      }
    }

    double mz = 0;
    if (SnF.isValidDouble(parsedStr)) {
      mz = SnF.strToDouble(parsedStr);
    }

    return mz;
  }

  public static double minToSec(double min) {
    return min * 60;
  }

  private static String[] stripTrailingEmpty(String[] row) {
    int end = row.length;

    while (end > 0) {
      String lastElement = row[end - 1];
      boolean isEmpty = lastElement == null || lastElement.isEmpty();
      if (!isEmpty) {
        break;
      }
      end--;
    }

    boolean rowWasTrimmed = end != row.length;
    if (rowWasTrimmed) {
      return Arrays.copyOf(row, end);
    }
    return row;
  }

}
