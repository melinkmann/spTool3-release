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
import dataModelNew.*;
import dataModelNew.mz.Channel;
import dataModelNew.mz.IsotopeChannel;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;
import io.FileInterpreterUtils;
import io.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

  // RAFA: Column indices in the single_particle_info CSV format:
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


  /*
  TOM:

  0   id
  1   frame
  2   frame_count
  3   area
  4   aspect
  5   circular_equivalent_diameter
  6   circularity
  7   convexity
  8   intensity
  9   maximum_width
  10  minimum_width
  11  perimeter
  12  radius
  13  sharpness
  14  x
  15  y
   */


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
    Channel mzValue;
    Isotope isotope = params.getIsotopeParameter().getValue().unwrap();
    // Isotope channel is correct since the user assigned this isotope via parameter!
    mzValue = new IsotopeChannel(isotope);

    LPCParticleMatrix matrix = new LPCParticleMatrix();
    String sampleName = PathUtil.removeExtension(file.getFileName().toString());
    SampleFile sampleFile = new SampleFile(file, sampleName);
    try {
      Sample sample = new IncompleteSample(sampleName, sampleFile, matrix);
      // sample.getMethod().addSet(params); // should be called in the csvImportTask

      int firstLine = params.getFirstLine().getValue();

      // Check format
      boolean isRafa = false;
      boolean isTom = false;

      if (!csv.isEmpty()) {
        String[] line = csv.get(0);
        if (line.length > 0) {
          if (Objects.equals(line[0], "id")) {
            isTom = true;
            LOGGER.info("Found data format: Tom.");
          } else if (Objects.equals(line[0], "particle id")) {
            isRafa = true;
            LOGGER.info("Found data format: Rafa.");
          }
        }
      }

      int targetColIdx = params.getLpcImportParameter().getValue().getCol(isRafa, isTom);
      if (targetColIdx < 0) {
        targetColIdx = Integer.MAX_VALUE; // gets rectified below
      }

      if (isRafa) {

        if (targetColIdx > 14) {
          targetColIdx = 5;
        }

        for (int i = firstLine; i < csv.size(); i++) {
          Pair<Boolean, double[]> checkedLine = SnF.strToDoubleArrChecked(csv.get(i));
          if (checkedLine.getKey()) {
            double[] values = checkedLine.getValue();
            if (values.length >= 14) {

              matrix.add("TARGET", values[targetColIdx]);

              matrix.add("particle_id", values[0]);

              matrix.add("largest_feret_diameter", values[1]);
              matrix.add("smallest_feret_diameter", values[2]);

              matrix.add("largest_legendre_diameter", values[3]);
              matrix.add("smallest_legendre_diameter", values[4]);

              matrix.add("circle_equivalent_diameter", values[5]);

              matrix.add("circularity", values[6]);
              matrix.add("convexity", values[7]);
              matrix.add("solidity", values[8]);

              matrix.add("aspect_ratio", values[9]);

              matrix.add("detection_time", values[10]);

              matrix.add("frame_id", values[11]);

              matrix.add("centroid_row", values[12]);
              matrix.add("centroid_col", values[13]);
            }
          }
        }
      } else if (isTom) {

        if (targetColIdx > 16) {
          targetColIdx = 5;
        }

        for (int i = firstLine; i < csv.size(); i++) {
          Pair<Boolean, double[]> checkedLine = SnF.strToDoubleArrChecked(csv.get(i));
          if (checkedLine.getKey()) {
            double[] values = checkedLine.getValue();
            if (values.length >= 16) {

              matrix.add("TARGET", values[targetColIdx]);

              matrix.add("id", values[0]);

              matrix.add("frame", values[1]);
              matrix.add("frame_count", values[2]);

              matrix.add("area", values[3]);

              matrix.add("aspect", values[4]);

              matrix.add("circular_equivalent_diameter", values[5]);

              matrix.add("circularity", values[6]);
              matrix.add("convexity", values[7]);

              matrix.add("intensity", values[8]);

              matrix.add("maximum_width", values[9]);
              matrix.add("minimum_width", values[10]);

              matrix.add("perimeter", values[11]);

              matrix.add("radius", values[12]);

              matrix.add("sharpness", values[13]);

              matrix.add("x", values[14]);
              matrix.add("y", values[15]);
            }
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