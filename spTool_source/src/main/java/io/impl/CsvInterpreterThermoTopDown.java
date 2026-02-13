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

import com.opencsv.CSVReader;
import dataModelNew.Sample;
import dataModelNew.Trace;
import dataModelNew.mz.MZValue;
import io.FileInterpreterUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import math.units.Unit;
import math.units.enums.ViewUnits;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.impl.CsvInterpreterParams;
import util.SnF;

public class CsvInterpreterThermoTopDown implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterThermoTopDown.class);

  private final CsvInterpreterParams params;

  public static final String icapTimeKey = "Time";
  public static final String icapIntKeyCross = "Y";
  // Counter is strictly digital counting. Usually the export contains "Y" which is either
  // the counting data or cross-calibrated analog signal. If properly exported,
  // only y data should be found
  public static final String icapIntKeyCounter = "Counter";

  // MZ in column that comes 2 after the "MainRuns"
  private static final String mainKey = "MainRuns";
  private static final int mzLineOffset = 2;
  private static final int mzColOffset = 2;
  // "Time", "Y", .. comes 1 after mz, i.e., 3 after "MainRuns"
  private static final int dataTypeColOffset = 3;

  private static final int nameLineOffset = 1;
  private static final int nameColOffset = 4;

  // TODO
  // Note: -33 flags over ranges which should be noted down.
  /*
  COUNTER -33
  Y
  ANALOG
  (note that analog needs cross calibration and is not useful on itself).
   */

  private Unit unit = ViewUnits.NONE;
  private final List<Sample> samples;

  public CsvInterpreterThermoTopDown(CsvInterpreterParams params) {
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
      reader.close();
    } catch (Exception e) {
      LOGGER.error("CSVReader failed to read csv due to wrong separator or character encoding."
          + " Is the file readable? This error has nothing to do with the column order. ", e);
    }

    List<Trace> traces = new ArrayList<>();
    List<String> sampleNames = new ArrayList<>();
    List<MZValue> mzValues = new ArrayList<>();
    // TODO: Parse the unit and set it.

    if (csv.size() >= nameLineOffset) {
      String[] lineWithNames = csv.get(nameLineOffset);
      if (lineWithNames.length >= nameColOffset) {
        for (int col = nameColOffset; col < lineWithNames.length; col++) {
          sampleNames.add(lineWithNames[col]);
        }
      }
    }

    if (csv.size() >= mzLineOffset) {
      String[] lineWithMZ = csv.get(mzLineOffset);
      if (lineWithMZ.length >= mzColOffset) {
        // mzValues.add(FileInterpreterUtils.parseIcapTQmz(lineWithMZ[mzColOffset]));
      }
    }

    List<List<Double>> times = new ArrayList<>();
    List<List<Double>> yCounter = new ArrayList<>();
    List<List<Double>> yCrosses = new ArrayList<>();
    List<List<Integer>> counterOverRange = new ArrayList<>();

    for (int l = 0; l < csv.size(); l++) {
      String[] line = csv.get(l);
      if (line.length >= dataTypeColOffset) {
        for (int col = dataTypeColOffset + 1; col < line.length; col++) {
          String field = line[col];
          if (line[dataTypeColOffset].equals(icapTimeKey)) {
            if (times.size() < col) {
              times.add(new ArrayList<>());
            }
            if (SnF.isValidDouble(field)) {
              times.get(col).add(SnF.strToDouble(field));
            }
          } else if (line[dataTypeColOffset].equals(icapIntKeyCounter)) {
            if (yCounter.size() < col) {
              yCounter.add(new ArrayList<>());
            }
            if (counterOverRange.size() < col) {
              counterOverRange.add(new ArrayList<>());
            }
            if (SnF.isValidDouble(field)) {
              double val = SnF.strToDouble(field);
              yCounter.get(col).add(val);
              // Field is flagged as counter of range if "-33"
              if (val < 0) {
                counterOverRange.get(col).add(l);
              }
            }
          } else if (line[dataTypeColOffset].equals(icapIntKeyCross)) {
            if (yCrosses.size() < col) {
              yCrosses.add(new ArrayList<>());
            }
            if (SnF.isValidDouble(field)) {
              yCrosses.get(col).add(SnF.strToDouble(field));
            }
          }
        }
      }
    }

    // Get lines with "Time"

    // Get lines with "Y"

    // Get lines with "Count"

  }


}
