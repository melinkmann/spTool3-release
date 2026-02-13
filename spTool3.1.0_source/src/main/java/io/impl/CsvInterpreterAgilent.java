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
import dataModelNew.TISeriesRAM;
import dataModelNew.Trace;
import dataModelNew.TraceImpl;
import dataModelNew.mz.MZValue;
import io.FileInterpreterUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import math.stat.Mode;
import math.units.Unit;
import math.units.enums.IntensityUnit;
import math.units.enums.ViewUnits;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.SignalConversionOption;
import processing.parameterSets.impl.CsvInterpreterParams;
import util.ArrUtils;
import util.SnF;

/*
  - Should be all implemented [potentially double check multi time series]
      - internal name (batch name?!)
      - read mz (including TQ!!!)
      - read multiple intensity series
      - correct TIME (divide by n if n >1)
      - "gas mode case" --> when there are blocks of zeros and time jumps by >5 s (gas mode)
      -
 */

public class CsvInterpreterAgilent implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterAgilent.class);

  private final CsvInterpreterParams params;

  /*
  Cases:
  A. When using "multiple gas modes",
    you get a csv with dT = DT
    all mz from left to right
    zeros in the "non-active" mz
    a time gap between the elements

  B. single cell mode
    you get a csv with dT = DT
    one file for each mz
    second file starting at the continuous time, i.e, not zero

  C. just several isotopes, e.g., for calibration at longer DT
    you get a csv with dt = n · DT
    all mz from left to right

  Unsure what happens when multiple elements in one gas modes are used as well as multiple gas modes
   */

  private Unit unit = ViewUnits.NONE;
  private final List<Sample> samples;

  public CsvInterpreterAgilent(CsvInterpreterParams params) {
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
  public void parse(Path file) {
    final List<String[]> csv = new ArrayList<>();
    try {
      CSVReader reader = FileInterpreterUtils.buildReader(file,
          params.getDelimiter(),
          params.getCharSet());

      csv.addAll(reader.readAll());
      reader.close();

    } catch (Exception e) {
      LOGGER.warn("CSVReader failed to read csv due to wrong separator or character encoding."
          + " Is the file readable? This error has nothing to do with the column order. "
          + "Message: " + e.getMessage()
          + ". Details: " + ExceptionUtils.getStackTrace(e));
    }

    List<Trace> traces = new ArrayList<>();
    List<MZValue> mzValues = new ArrayList<>();

    /*
    [0] C:\Agilent\ICPMH\1\DATA\AXK\2022-08-04_microFAST-Test_2-Isotope.b\Test_Au_Ag.d
    [1] Intensity Vs Time,CPS
    [2] Acquired      : 8/4/2022 2:05:27 PM using Batch 2022-08-04_microFAST-Test_2-Isotope.b
    [3] Time [Sec],Ag107,Au197
    [4] 0.0210,0.00,0.00
    [5] 0.0211,0.00,0.00
    [6] 0.0212,0.00,0.00
     */

    int nameRow = 0;
    int unitRow = 1;
    int batchRow = 2;
    int mzRow = 3;
    int mzColStart = 1;
    int timeCol = 0;
    int dataRowStart = 4;
    int signalColStart = 1;
    double minSwitchTimeSec = params.getAgilentGasModeSwitchTime().getValue();
    boolean isIonic = params.getIsIonicQmsData().getValue();
    SignalConversionOption conversion = params.getSignalConversionOption().getValue();

    try {

      // Find Name.
      String sampleName = "No name found";
      if (csv.get(nameRow).length > 0) {
        sampleName = parseAgilentSampleName(csv.get(nameRow)[0]);
        LOGGER.trace("Read csv and identified sample name - start interpreting: " + sampleName);
      }

      // Find Unit.
      if (csv.get(unitRow).length > 1) {
        this.unit = parseAgilentUnit(csv.get(unitRow)[1]);
      }

      // Find batch.
      String batchName = "";
      if (csv.get(batchRow).length > 0) {
        batchName = parseAgilentBatchName(csv.get(batchRow)[0]);
      }

      // Find MZ.
      if (mzRow < csv.size()) {
        String[] mzRowStr = csv.get(mzRow);
        if (mzRowStr != null && mzRowStr.length > mzColStart) {
          for (int i = mzColStart; i < mzRowStr.length; i++) {
            String rawMZ = mzRowStr[i];
            MZValue mz = FileInterpreterUtils.parseAgilentTQmz(rawMZ);
            mzValues.add(mz);
          }
          LOGGER.trace("Identified mz " + mzValues.stream()
              .map(MZValue::getElementTransition)
              .collect(Collectors.joining(" ")));
        }
      }

      // resolve time array
      List<Double> time = new ArrayList<>();
      for (int rowIdx = dataRowStart; rowIdx < csv.size(); rowIdx++) {
        String[] row = csv.get(rowIdx);
        if (row.length > timeCol) {
          String timeStr = row[timeCol];
          if (!timeStr.isEmpty() && SnF.isValidDoubleSilent(timeStr)) {
            time.add(SnF.strToDouble(timeStr));
          }
        }

      }

      // resolve intensity arrays
      List<List<Double>> allIntensitySeries = new ArrayList<>();

      // loop top down
      int traceCounter;
      for (int rowIdx = dataRowStart; rowIdx < csv.size(); rowIdx++) {
        traceCounter = -1;
        String[] row = csv.get(rowIdx);

        if (row.length > signalColStart) {
          // loop left to right
          for (int colIdx = signalColStart; colIdx < row.length; colIdx++) {
            // for each column, try to parse the intensity value

            String signalStr = row[colIdx];
            if (!signalStr.isEmpty() && SnF.isValidDoubleSilent(signalStr)) {

              // The intensity is a valid value. Now, increment traceCounter (counting the series) and add value.
              traceCounter++;
              double signalValue = SnF.strToDouble(signalStr);

              // We are in the middle of the data and all series are present
              if (traceCounter < allIntensitySeries.size()) {
                allIntensitySeries.get(traceCounter).add(signalValue);
              } else {
                // First row, we have to instantiate the lists
                List<Double> signalSeries = new ArrayList<>(csv.size());
                signalSeries.add(signalValue);
                allIntensitySeries.add(signalSeries);
              }
            }
          }
        }
      }

      LOGGER.trace("Finished parsing time and intensity data.");

      /*
       Now, we have to translate this to TISeries and Samples.
       Unit conversion and flag recognition can be done later,
       as well as special cases.
       Agilent has constant dwell time, so we can apply this here, by replacing time stamps.
       */

      boolean equalLength = allIntensitySeries.stream()
          .map(List::size)
          .allMatch(intensitySize -> time.size() == intensitySize);

      if (equalLength) {

        List<TISeries> tiSeriesList = new ArrayList<>();

        ///////////////////////////////////////////////////////////////////////
        // Check for gas mode gap
        ///////////////////////////////////////////////////////////////////////
        boolean timeHasGasModeGap = false;
        double dt = 0;
        for (int i = 1; i < time.size(); i++) {
          dt = time.get(i) - time.get(i - 1);
          if (dt > minSwitchTimeSec) {
            timeHasGasModeGap = true;
            LOGGER.trace("Identified presence of different gas mode sections in Agilent csv data.");
            break;
          }
        }

        // Split if there is gap
        if (timeHasGasModeGap) {
          List<Section> sections = splitByTimeGap(time, allIntensitySeries, minSwitchTimeSec);

          // For each intensity series, find the section where it has non-zero sum
          for (int i = 0; i < allIntensitySeries.size(); i++) {
            for (Section section : sections) {
              if (section.allSignalSeriesOfSection.size() == allIntensitySeries.size()) {
                double sum = ArrUtils.doubleSum(section.getAllSignalSeriesOfSection().get(i));
                if (sum > 0) {
                  double[] correctedTime = replaceTimeStamps(section.getTimeOfSection());
                  tiSeriesList.add(new TISeriesRAM(
                      correctedTime,
                      section.getAllSignalSeriesOfSection().get(i)));
                  /*
                   If someone tries to have the same isotope in multiple gas modes,
                   we only use the first occurrence. This break statement enforces that.
                   */
                  break;
                }
              }
            }
          }
        } else {
          for (List<Double> intensitySeries : allIntensitySeries) {
            double[] correctedTime = replaceTimeStamps(time);
            tiSeriesList.add(new TISeriesRAM(correctedTime, intensitySeries));
          }
        }

        Sample sample = new SampleImpl(sampleName, new SampleFile(file, sampleName));
        sample.setComment("Batch: " + batchName);
        if (mzValues.size() == tiSeriesList.size()) {
          for (int i = 0; i < mzValues.size(); i++) {
            // Create sample.
            TISeries tiSeries;
            double[] x = tiSeriesList.get(i).getTime();
            double[] y = tiSeriesList.get(i).getIntensity();
            boolean convertToCts = RawProcessingUtils.checkConversion(conversion, unit, y);
            if (isIonic) {
              int nMZ = mzValues.size();
              x = ArrUtils.divide(x, nMZ);
              LOGGER.trace("Time stamps were divided by number of mz to account for QMS scan.");
            }
            if (convertToCts) {
              LOGGER.trace("Check for unit conversion: applying conversion to counts.");
              y = RawProcessingUtils.cpsToCts(x, y);
            }
            tiSeries = new TISeriesHDD(x, y);
            Trace trace = new TraceImpl(sample, mzValues.get(i), tiSeries);
            traces.add(trace);
          }
        }
        traces.forEach(sample::addTrace);
        this.samples.clear();
        this.samples.add(sample);
        LOGGER.trace("Finished reading Agilent sample.");


      } else {
        LOGGER.error("Time and intensity data did not have equal length.");
      }

    } catch (Exception e) {
      LOGGER.error("CSV reader parsing was not successful."
          + " This error is likely due to wrong column order."
          + " Message: " + e.getMessage()
          + ". Details: " +
          ExceptionUtils.getStackTrace(e));
    }
  }


  /**
   * C:\Agilent\ICPMH\1\DATA\microFAST-SingleCell_Au_50nmNP-2.b\009SMPL.d,
   * <p>
   * read as: C:AgilentICPMH1DATAmicroFAST-SingleCell_Au_50nmNP-2.b009SMPL.d
   * <p>
   * --> return
   */
  private String parseAgilentSampleName(String rawName) {

    String parsedName = "";
    if (rawName != null && !rawName.isEmpty() && rawName.contains(".b") && rawName.contains(".d")) {
      try {
        parsedName = rawName.substring(
            rawName.lastIndexOf(".b") + 2,
            rawName.lastIndexOf(".d"));
      } catch (StringIndexOutOfBoundsException stringIndexOutOfBoundsException) {
        LOGGER.error("Could not parse Agilent sample name."
            + " This indicates that the csv was not read successfully."
            + " Please check the csv settings such as character encoding."
            + " Message: " + stringIndexOutOfBoundsException.getMessage()
            + ". Details: " +
            ExceptionUtils.getStackTrace(stringIndexOutOfBoundsException));
      }
    }
    return parsedName;
  }

  /**
   * Acquired      : 3/10/2022 5:45:46 PM using Batch mFSC_20220308_allSmpls_AuPt.b
   */
  private String parseAgilentBatchName(String rawName) {

    String parsedBatchName = "";
    if (rawName != null && !rawName.isEmpty() && rawName.contains(".b")
        && rawName.contains("Batch")) {
      try {
        // Find the start index of the filename
        int start = rawName.indexOf("Batch ") + "using Batch ".length();

        // Find the end index at the last ".b" (or adjust as needed)
        int end = rawName.lastIndexOf(".b");

        // Extract the substring
        parsedBatchName = rawName.substring(start, end);

      } catch (StringIndexOutOfBoundsException stringIndexOutOfBoundsException) {
        LOGGER.error("Could not parse Agilent sample name."
            + " This indicates that the csv was not read successfully."
            + " Please check the csv settings such as character encoding."
            + " Message: " + stringIndexOutOfBoundsException.getMessage()
            + ". Details: " +
            ExceptionUtils.getStackTrace(stringIndexOutOfBoundsException));
      }
    }
    return parsedBatchName;
  }

  private Unit parseAgilentUnit(String string) {
    Unit unit = ViewUnits.NONE;
    if (string.toLowerCase(Locale.ROOT).contains("counts")) {
      unit = IntensityUnit.CTS;
    } else if (string.toLowerCase(Locale.ROOT).contains("cps")) {
      unit = IntensityUnit.CPS;
    }
    return unit;
  }

  private static class Section {

    private final List<Double> timeOfSection = new ArrayList<>();
    private final List<List<Double>> allSignalSeriesOfSection = new ArrayList<>();

    public Section(int numMZ) {
      for (int i = 0; i < numMZ; i++) {
        allSignalSeriesOfSection.add(new ArrayList<>());
      }
    }

    public List<Double> getTimeOfSection() {
      return timeOfSection;
    }

    public List<List<Double>> getAllSignalSeriesOfSection() {
      return allSignalSeriesOfSection;
    }

  }


  public static double[] normalizeTimeStamps(List<Double> time) {
    double[] result;
    if (time.size() > 2) {

      // Subtract first time stamp to normalize
      result = new double[time.size()];
      for (int i = 0; i < time.size(); i++) {
        result[i] = time.get(i) - time.get(0);
      }

      // In Agilent data, first time stamp always has a delay, so time[0] is not the DT

      // --> estimate dwell time
      List<Double> differences = new ArrayList<>();
      for (int i = 1; i < Math.min(time.size(), 100); i++) {
        differences.add(time.get(i) - time.get(i - 1));
      }

      // override first time stamp
      double dwellTimeGuess = Mode.calculateMode(differences);
      result[0] = result[1] - dwellTimeGuess;

      // just to be safe
      if (result[0] <= 0) {
        result[0] = dwellTimeGuess;
      }


    } else {
      result = ArrUtils.doubleListToArr(time);
    }
    return result;
  }

  // in some cases, we have see bugs that Agilent csv export had the same time stamp twice
  public static double[] replaceTimeStamps(List<Double> time) {
    double[] result;
    if (time.size() > 2) {
      result = new double[time.size()];

      // --> estimate dwell time
      List<Double> differences = new ArrayList<>();
      for (int i = 1; i < Math.min(time.size(), 100); i++) {
        differences.add(time.get(i) - time.get(i - 1));
      }
      double dwellTimeGuess = Mode.calculateMode(differences);

      // create artificial time stamps
      result[0] = dwellTimeGuess;
      for (int i = 1; i < result.length; i++) {
        result[i] = result[i - 1] + dwellTimeGuess;
      }
    } else {
      result = ArrUtils.doubleListToArr(time);
    }
    return result;
  }


  public static List<Section> splitByTimeGap(
      List<Double> time,
      List<List<Double>> allSignalSeries,
      double minSwitchTimeSec) {

    int numMZ = allSignalSeries.size();

    List<Section> sections = new ArrayList<>();

    Section currentSection = new Section(numMZ);

    for (int i = 0; i < time.size(); i++) {
      if (i > 0 && (time.get(i) - time.get(i - 1)) > minSwitchTimeSec) {

        // Gap detected — store current section and start a new one
        sections.add(currentSection);
        currentSection = new Section(numMZ);
      }

      // add time
      currentSection.timeOfSection.add(time.get(i));

      // add all signal at row
      for (int sCol = 0; sCol < numMZ; sCol++) {
        currentSection.allSignalSeriesOfSection.get(sCol).add(allSignalSeries.get(sCol).get(i));
      }
    }

    // Add final section
    if (!currentSection.timeOfSection.isEmpty()) {
      sections.add(currentSection);
    }

    return sections;
  }


}
