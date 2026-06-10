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
import dataModelNew.Sample;
import dataModelNew.SampleFile;
import dataModelNew.SampleImpl;
import dataModelNew.TISeries;
import dataModelNew.TISeriesHDD;
import dataModelNew.Trace;
import dataModelNew.TraceImpl;
import dataModelNew.mz.Channel;
import dataModelNew.mz.MZValue;
import io.FileInterpreterUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import math.units.Unit;
import math.units.enums.IntensityUnit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.DataFlag;
import processing.parameterSets.impl.CsvInterpreterParams;
import util.ArrUtils;
import util.SnF;

public class CsvInterpreterAnalytikJena implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterAnalytikJena.class);

    /*
    - older to do item which is mostly resolved for AJ (not for Thermo data!): tidy up for
      1) add unit
      2) add is ionic QMS (in order to know if one has to divide)
      3) add OR recognition, add remediation; store somewhere in the raw data (edit data structure :( )
      4) and create population with OR events (remove after search) - how do we find these ?!
        Problem is, we cant just search for the cps value as we convert to counts... so recognition must be done at import
        -> create Flag enum (e.g., OR)
        -> add HashMap<Flag, List<Integer>>
        -> add this map to the Trace, add it to the readObject in order to make sure it is initiaized even in older ver
   */

  private final CsvInterpreterParams params;

  private static final String HEADER_KEY = "Processed";

  private static final int NAME_COL = 0;
  private static final int MZ_COL = 2;
  private static final int NAME_OFFSET = 1;
  private static final int MZ_OFFSET = 2;
  private static final int TIME_COL = 1;
  private static final int INTENSITY_COL = 2;

  private final List<Sample> samples;
  // For AJ, I think it is always cps.
  private final Unit unit = IntensityUnit.CPS;

  public CsvInterpreterAnalytikJena(CsvInterpreterParams params) {
    samples = new ArrayList<>();
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
      String msg = "CSVReader failed to read csv due to wrong separator or character encoding. "
          + "Check if the file is readable. This error has nothing to do with the column order. "
          + "Message: " + e.getMessage()
          + ". Stack trace: " + ExceptionUtils.getStackTrace(e);
      LOGGER.warn(msg);
      previewLines.add(new String[]{msg});
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

      CSVReader reader = FileInterpreterUtils.buildReader(file, params.getDelimiter(),
          params.getCharSet());

      csv.addAll(reader.readAll());
      reader.close();
    } catch (Exception e) {
      LOGGER.error("CSVReader failed to read csv due to wrong separator or character encoding."
          + " Is the file readable? This error has nothing to do with the column order. ", e);
    }

    /*
     Start at -1 because the incrementation happens right at the beginning.
     Otherwise, it follows index == length!
     */
    AtomicInteger index = new AtomicInteger(-1);
    List<Integer> headerIndices = new ArrayList<>();

    try {
      // Linear Stream and not Parallel to get indices right

      // Find headers
      csv.stream()
          .peek(arr -> index.incrementAndGet())
          .filter(arr -> Arrays.stream(arr).anyMatch(s -> s.contains(HEADER_KEY)))
          .forEach(arr -> headerIndices.add(index.get()));

      List<String> sampleNames = headerIndices.stream()
          .map(i -> csv.get(i + NAME_OFFSET))
          .filter(arr -> arr.length > NAME_COL)
          .map(arr -> arr[NAME_COL])
          .collect(Collectors.toList());

      // Get rows with the MZs
      List<String[]> mzRowString = headerIndices.stream()
          .map(i -> csv.get(i + MZ_OFFSET))
          .filter(arr -> arr.length > MZ_COL)
          .collect(Collectors.toList());

      // Make a list of all MZs across all samples
      List<Channel[]> allMZ = new ArrayList<>();
      for (String[] mzRow : mzRowString) {
        List<Channel> mzInRow = new ArrayList<>();
        for (int i = MZ_COL; i < mzRow.length; i++) {
          String mzCandidate = mzRow[i];
          Channel ch = FileInterpreterUtils.parseAnalytikJenaSQmz(mzCandidate);
          if (ch.getMZ() > 0) {
            mzInRow.add(ch);
          }
        }
        allMZ.add(mzInRow.toArray(new Channel[0]));
      }

      // Create a sublist of the entire csv; one for each sample.
      List<List<String[]>> sampleSubList = new ArrayList<>();
      for (int i = 0; i < headerIndices.size(); i++) {
        int lastIndex;
        if (i < headerIndices.size() - 1) {
          lastIndex = headerIndices.get(i + 1);
        } else {
          lastIndex = csv.size() - 1;
        }
        List<String[]> sample = csv.subList(headerIndices.get(i), lastIndex);
        sampleSubList.add(sample);
        //        // Make new List, else: concurrent exception because only peek
        //        List<String[]> sample = new ArrayList<>(csv.subList(headerIndices.get(i), lastIndex));
        //        sample.removeIf(s -> Arrays.stream(s).anyMatch(String::isEmpty));
      }

      // Build actual Samples
      List<Sample> subSamples = new ArrayList<>();

      if (allMZ.size() == sampleNames.size() && sampleNames.size() == sampleSubList.size()) {

        // Loop over samples.
        for (int sampleIdx = 0; sampleIdx < sampleSubList.size(); sampleIdx++) {
          List<String[]> sampleStrings = sampleSubList.get(sampleIdx);

          // get sample name
          String name = sampleNames.get(sampleIdx);
          Sample sample = new SampleImpl(name, new SampleFile(file, name));
          subSamples.add(sample);

          Channel[] channels = allMZ.get(sampleIdx);
          double nMZ = channels.length;
          if (nMZ > 1) {
            LOGGER.trace("Time stamps will be divided by number of mz to account for QMS scan, n={}.", nMZ);
          }

          // get x data: it is unique
          List<Double> x = new ArrayList<>();
          for (String[] arr : sampleStrings) {
            if (arr.length > 2) {
              if (SnF.isValidDoubleSilent(arr[TIME_COL])) {
                double origTime = SnF.strToDouble(arr[TIME_COL]);
                origTime = origTime / nMZ;
                x.add(origTime);
              }
            }
          }

          // get y data & MZs: loop over the mz, they should accurately represent the data columns.
          for (int traceIdx = 0; traceIdx < allMZ.get(sampleIdx).length; traceIdx++) {

            // get mz
            Channel mzChannel = allMZ.get(sampleIdx)[traceIdx];

            // get y data
            List<Double> y = new ArrayList<>();
            for (String[] arr : sampleStrings) {
              if (arr.length > INTENSITY_COL + traceIdx) {
                if (SnF.isValidDoubleSilent(arr[INTENSITY_COL + traceIdx])) {
                  y.add(SnF.strToDouble(arr[INTENSITY_COL + traceIdx]));
                }
              }
            }

            // over range flagging
            List<Integer> orIndices = new ArrayList<>();
            double orTrigger = params.getORValue();
            for (int i = 0; i < y.size(); i++) {
              if (orTrigger < 0 && y.get(i) <= orTrigger) {
                orIndices.add(i);
              } else if (orTrigger > 0 && y.get(i) >= orTrigger) {
                orIndices.add(i);
              }
            }

            //
            LOGGER.trace("Automatically converting cps to cts for AJ data.");
            y = RawProcessingUtils.cpsToCts(x, y);

            // Create traces.
            if (x.size() == y.size()) {
              TISeries tiSeries = new TISeriesHDD(x, y);
              Trace trace = new TraceImpl(sample, mzChannel, tiSeries);
              trace.setFlags(DataFlag.OVER_RANGE, orIndices);
              // Add trace
              sample.addTrace(trace);
            }
          }
        }
      }
      this.samples.clear();
      this.samples.addAll(subSamples);
    } catch (Exception e) {
      LOGGER.error("CSVReader parsing was not successful. "
          + "This error is likely due to wrong column order. ", e);
    }

  }

}
