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

import analysis.IncompleteEventCollection;
import analysis.NpPopulation;
import analysis.Population;
import analysis.PopulationID;
import com.opencsv.CSVReader;
import dataModelNew.IncompleteParticleMatrix;
import dataModelNew.IncompleteSample;
import dataModelNew.Sample;
import dataModelNew.SampleFile;
import dataModelNew.TISeriesRAM;
import dataModelNew.Trace;
import dataModelNew.TraceImpl;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;
import io.FileInterpreterUtils;
import io.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;
import math.units.Unit;
import math.units.enums.IntensityUnit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.PopulationType;
import processing.options.Source;
import processing.parameterSets.impl.CsvInterpreterParams;
import sandbox.montecarlo.Isotope;
import util.SnF;

public class CsvInterpreterLPC implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterLPC.class);

  // Column indices in the single_particle_info CSV format:
  //   0  particle id
  //   1  largest feret diameters [µm]
  //   2  smallest feret diameters [µm]
  //   3  largest legendre diameters [µm]
  //   4  smallest legendre diameters [µm]
  //   5  circle-equivalent diameters [µm]   → mapped to values[1]
  //   6  circularity [1]
  //   7  convexity [1]
  //   8  solidity [1]
  //   9  particle diameter aspect ratios [1]
  //  10  detection time [sec.]               → mapped to values[0]
  //  11  frame id
  //  12  centroid position row [pix]
  //  13  centroid position column [pix]
  private static final int COL_DETECTION_TIME = 10;
  private static final int COL_CIRCLE_EQ_DIAMETER = 5;
  private static final int COL_ASPECT_RATIO = 9;
  private static final int COL_FERRET_MIN = 1;
  private static final int COL_FERRET_MAX = 2;
  private static final int MIN_REQUIRED_COLUMNS = 11; // need at least up to index 10

  private final CsvInterpreterParams params;

  private Unit unit = IntensityUnit.CTS;
  private final List<Sample> samples;

  public CsvInterpreterLPC(CsvInterpreterParams params) {
    this.samples = new ArrayList<>();
    this.params = params;
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
      CSVReader reader = FileInterpreterUtils.buildReader(
          file,
          params.getDelimiter(),
          params.getCharSet());

      csv.addAll(reader.readAll());
      reader.close();
    } catch (Exception e) {
      LOGGER.warn("CSVReader failed to read csv due to wrong separator or character encoding."
          + " Is the file readable? This error has nothing to do with the column order. "
          + "Message: " + e.getMessage()
          + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }

    // The format is not supposed to provide m/z.
    MZValue mzValue;
    Isotope isotope = params.getIsotopeParameter().getValue().unwrap();
    mzValue = new SQmz(isotope);

    IncompleteParticleMatrix matrix = new IncompleteParticleMatrix(csv.size());
    String sampleName = PathUtil.removeExtension(file.getFileName().toString());
    SampleFile sampleFile = new SampleFile(file, sampleName);
    try {
      Sample sample = new IncompleteSample(sampleName, sampleFile, matrix);
      // sample.getMethod().addSet(params); // should be called in the csvImportTask

      int firstLine = params.getFirstLine().getValue();
      int targetCol = params.getLpcImportParameter().getValue().getCol();
      if (targetCol > MIN_REQUIRED_COLUMNS) {
        targetCol = COL_CIRCLE_EQ_DIAMETER;
      }

      for (int i = firstLine; i < csv.size(); i++) {
        Pair<Boolean, double[]> checkedLine = SnF.strToDoubleArrChecked(csv.get(i));
        if (checkedLine.getKey()) {
          double[] values = checkedLine.getValue();
          if (values.length >= MIN_REQUIRED_COLUMNS) {
            // values[0] → detection time [sec.]
            // values[1] → circle-equivalent diameters [µm]
            // values[2..4] → zero (no area, height, duration equivalent)
            matrix.add(
                values[COL_DETECTION_TIME],   // values[0]: time
                values[targetCol], // values[1]: diameter
                0, // values[COL_CIRCLE_EQ_DIAMETER],                            // values[2]: zeroed
                0, // values[COL_CIRCLE_EQ_DIAMETER],                            // values[3]: zeroed
                0 // values[COL_CIRCLE_EQ_DIAMETER]                             // values[4]: zeroed
            );
          }
        }
      }

      Trace trace = new TraceImpl(sample, mzValue, new TISeriesRAM());

      // add a population to have something to select in the UI
      PopulationID populationID = new PopulationID(PopulationType.EXTERNAL);
      Population population = new NpPopulation(populationID,
          new IncompleteEventCollection(trace, matrix));
      trace.addOverridePopulation(populationID, population, false);

      sample.addTrace(trace);

      this.samples.clear();
      this.samples.add(sample);
    } catch (Exception e) {
      LOGGER.error("CSVReader parsing was not successful. "
          + "This error is likely due to wrong column order. ", e);
    }
  }
}