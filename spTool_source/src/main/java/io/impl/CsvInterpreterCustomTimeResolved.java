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
import dataModelNew.SampleFile;
import dataModelNew.SampleImpl;
import dataModelNew.TISeries;
import dataModelNew.TISeriesHDD;
import dataModelNew.Trace;
import dataModelNew.TraceImpl;
import dataModelNew.mz.*;
import io.FileInterpreterUtils;
import io.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import math.units.Unit;
import math.units.enums.ViewUnits;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.Source;
import processing.parameterSets.impl.CsvInterpreterParams;
import sandbox.montecarlo.Isotope;
import util.SnF;

public class CsvInterpreterCustomTimeResolved implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterCustomTimeResolved.class);

  private final CsvInterpreterParams params;

  private Unit unit = ViewUnits.NONE;
  private final List<Sample> samples;

  public CsvInterpreterCustomTimeResolved(CsvInterpreterParams params) {
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
      LOGGER.warn("CSVReader failed to read csv due to wrong separator or character encoding."
          + " Is the file readable? This error has nothing to do with the column order. "
          + "Message: " + e.getMessage()
          + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }

    if (!csv.isEmpty()) {
      List<Trace> traces = new ArrayList<>();
      List<Channel> mzChannels = new ArrayList<>();

      try {
        // Find Name.
        String sampleName = PathUtil.removeExtension(file.getFileName().toString());
        if (sampleName.isEmpty()) {
          sampleName = "No name available";
        }
        SampleFile sampleFile = new SampleFile(file, sampleName);


        int headerLineIndex = params.getFirstLine().getValue() - 1;

        // Find MZ.
        // TODO: This is subpar. We should offer option to set first col.
        //  else, you cannot have data with time stamp and override the DT as this would shift import by 1
        //  col.
        int firstMZCol = params.getDwellTimeSource().getValue().equals(Source.CUSTOM) ? 0 : 1;
        if (params.getMzSource().getValue().equals(Source.CUSTOM)) {
          Isotope isotope = params.getIsotopeParameter().getValue().unwrap();
          // Isotope channel is correct since the user assigned this isotope via parameter!
          mzChannels.add(new IsotopeChannel(isotope));
        } else if (headerLineIndex < csv.size()) {
          for (int i = firstMZCol; i < csv.get(headerLineIndex).length; i++) {
            if (i < csv.get(headerLineIndex).length) {
              String mzStr = csv.get(headerLineIndex)[i];
              Isotope isotope = Isotope.guessFromString(mzStr);
              mzChannels.add(new MZChannel(new MSIDImpl(new MZImpl(isotope.getIsotopicNumber())), isotope));
            }
          }
        }

        List<Double> time = new ArrayList<>();
        List<List<Double>> allIntensitySeries = new ArrayList<>();

        double dwellTime = params.getCustomDwellTime().getValue() * 1E-6;

        // assume first line
        for (int lineIdx = headerLineIndex + 1; lineIdx < csv.size(); lineIdx++) {

          // time
          if (params.getHasXData().getValue()) {

          }
          if (params.getDwellTimeSource().getValue().equals(Source.CUSTOM)) {
            if (time.isEmpty()) {
              time.add(dwellTime);
            } else {
              time.add(dwellTime + time.get(time.size() - 1));
            }

            // intensity: if custom time, all columns are intensity
            String[] line = csv.get(lineIdx);
            for (int colIdx = 0; colIdx < line.length; colIdx++) {
              if (allIntensitySeries.size() < line.length) {
                List<Double> intensity = new ArrayList<>();
                intensity.add(SnF.strToDoubleSilent(line[colIdx]));
                allIntensitySeries.add(intensity);
              } else {
                allIntensitySeries.get(colIdx).add(SnF.strToDoubleSilent(line[colIdx]));
              }
            }

          } else {

            String[] line = csv.get(lineIdx);
            if (line.length > 1) {
              time.add(SnF.strToDoubleSilent(line[0]));
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
        }

        Sample sample = new SampleImpl(sampleName, sampleFile);
        if (mzChannels.size() == allIntensitySeries.size()) {
          for (int i = 0; i < mzChannels.size(); i++) {
            // Create sample.
            TISeries tiSeries = new TISeriesHDD(time, allIntensitySeries.get(i));
            Trace trace = new TraceImpl(sample, mzChannels.get(i), tiSeries);
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

}
