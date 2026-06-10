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
import dataModelNew.mz.Channel;
import dataModelNew.mz.IsotopeChannel;
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

public class CsvInterpreterCustomPeaks implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterCustomPeaks.class);

  private final CsvInterpreterParams params;

  private Unit unit = IntensityUnit.CTS;
  private final List<Sample> samples;

  public CsvInterpreterCustomPeaks(CsvInterpreterParams params) {
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

    IncompleteParticleMatrix matrix = new IncompleteParticleMatrix(csv.size());
    String sampleName = PathUtil.removeExtension(file.getFileName().toString());
    SampleFile sampleFile = new SampleFile(file, sampleName);
    try {
      Sample sample = new IncompleteSample(sampleName, sampleFile, matrix);

      int firstLine = params.getFirstLine().getValue();

      for (int i = firstLine; i < csv.size(); i++) {
        Pair<Boolean, double[]> checkedLine = SnF.strToDoubleArrChecked(csv.get(i));
        if (checkedLine.getKey()) {
          double[] values = checkedLine.getValue();
          if (values.length > 4) {
            matrix.add(values[0],
                values[1],
                values[2],
                values[3],
                values[4]
            );
          }
        }
      }

      Trace trace = new TraceImpl(sample, mzValue, new TISeriesRAM());

      // add a population to have something to select in the UI
      PopulationID populationID = new PopulationID(PopulationType.EXTERNAL);
      Population population = new NpPopulation(populationID,
          new IncompleteEventCollection(trace, matrix));
      trace.addOverridePopulation(populationID, population,false);

      sample.addTrace(trace);

      this.samples.clear();
      this.samples.add(sample);
    } catch (Exception e) {
      LOGGER.error("CSVReader parsing was not successful. "
          + "This error is likely due to wrong column order. ", e);
    }
  }


}
