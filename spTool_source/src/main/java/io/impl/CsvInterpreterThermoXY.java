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
import dataModelNew.mz.SQmz;
import io.FileInterpreterUtils;
import io.PathUtil;

import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.util.Pair;
import math.stat.Mode;
import math.units.Unit;
import math.units.enums.ViewUnits;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.SignalConversionOption;
import processing.options.Source;
import processing.options.TimeStampFormat;
import processing.parameterSets.impl.CsvInterpreterParams;
import util.ArrUtils;
import util.SnF;

public class CsvInterpreterThermoXY implements CsvInterpreter {

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterThermoXY.class);

  private final CsvInterpreterParams params;

  DateTimeFormatter nanoFormatter;
  DateTimeFormatter secondsFormatter;

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

  public CsvInterpreterThermoXY(CsvInterpreterParams params) {
    this.samples = new ArrayList<>();
    this.params = params;
    this.nanoFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSS");
    this.secondsFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
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
      CSVReader reader = FileInterpreterUtils.buildReader(file, params.getDelimiter(),
          params.getCharSet());
      csv.addAll(reader.readAll());
      reader.close();
    } catch (Exception e) {
      LOGGER.error("CSVReader failed to read csv due to wrong separator or character encoding."
          + " Is the file readable? This error has nothing to do with the column order. ", e);
    }

    /*
    Thermo is quite some variety in output format variations...
    Check known cases here:

   a) QTegra live view with "Intensity" keyword
    sep=,
    Number,Time 197Au,Intensity (cps) 197Au
    1,00:00:05.3828050,0
    2,00:00:05.3828100,0
    3,00:00:05.3828150,0
    4,00:00:05.3828200,0

  b) QTegra live view without "Intensity" keyword
    sep=;
    Number;Time 197Au;197Au
    1;0.003;4000.51206554439
    2;0.003;4000.51206554439
    3;0.004;4000.51206554439
    4;0.004;8002.04852442225
    5;0.005;2000.12800819252

  c) QTegra live view without anything
    0.008414;17810.1446583974
    0.01342;16608.8226065686
    0.018426;16008.1961964526
    0.023433;12805.2450283636
    0.028439;11204.0155191621
    0.033446;16809.0365380429
    0.038452;17609.9179057645
    0.043459;19011.559027889
    0.048465;204529.935452285

  d) QTegra with TQ mode: does not contain "|"
    sep=;
    Number;Time 28Si16O;28Si16O
    1;0;10003.2010243278
    2;0;10003.2010243278
    3;0;20012.8081972462
    4;0;10003.2010243278

  e) "Laser reduction format": varying degree of meta data

    Multi_20_ppb_2:12/08/2021 12:28:53 PM;
    Software:Name=Qtegra;Version=2.10.3324.62;File Version=1;
    Configuration:Machine=iCAP TQ;
    U2-TQ-iO2:Additional Gas Flow 1=0;Additional Gas Flow 2=0;Q1 Entry Lens=-145;Angular Deflection=-250;
    Deflection Entry Lens=-30;Extraction Lens 1 Polarity=0;Extraction Lens 1 Negative=0;Extraction Lens 1
    Positive=0;Spray Chamber Temperature=20;Peristaltic Pump Speed=5;Cool Flow=14.5;Sampling Depth=5.5;
    Plasma Power=1550;Auxilliary Flow=0.85;Nebulizer Flow=1.01392857142857;Torch Horizontal Position=-1.64;
    Torch Vertical Position=1.21333333333333;Extraction Lens 2=-192;Q1 Focus Lens=-2.36;CR Bias=-7.76;CR
    Exit Lens=-40;Focus Lens=0.375;D1 Lens=-350;D2 Lens=-159;CR Entry Lens=-115;Quad Entry Lens=-37
    .3333333333333;Pole Bias=-12;Pole Bias Q1=0;CR1 Flow=0.8;CR2 Flow=0.2775;CR3 Flow=0;CR4 Flow=0;CR RF
    High Mass Amplitude Factor=200;CR RF High Mass Amplitude Offset=0;CR RF High Mass Amplitude Exponent=0
    .66;CR RF Low Mass Amplitude Factor=200;CR RF Low Mass Amplitude Offset=-110;CR RF Low Mass Amplitude
    Exponent=0.66;CR Parameter Mass Switch Point=300;Q1 SQ-mode RF Dac Factor=450.001192094032;Q1 SQ-mode
    RF Dac Offset=-340;Q1 SQ-mode parameter b=0.650000238418807;Dry Pump Speed=0;
    RF Generator:RF Plasma Lit Readback=1;RF FET Temperature Ok Readback=1;Plasma Power Readback=1548
    .6077186126;
    Ion Optics:Pole Bias Readback=-11.7106549364614;Torch Horizontal Position Readback=-1.641642228739;
    Torch Vertical Position Readback=1.20879765395894;Sampling Depth Readback=5.5;Extraction Lens 1
    Negative Readback=-198.435972629521;Extraction Lens 1 Positive Readback=-0.0391006842619746;Extraction
    Lens 2 Readback=-187.683284457478;Angular Deflection Readback=-250;Quad Entry Lens Readback=-37
    .4389051808407;Q1 Entry Lens Readback=-146.627565982405;Focus Lens Readback=0.307917888563054;D2 Lens
    Readback=-158.944281524927;Deflection Entry Lens Readback=-30.4203323558162;D1 Lens Readback=-353
    .47018572825;CCT Exit Lens Readback=-40.3225806451613;CCT Bias Readback=-8.04007820136852;CCT Entry
    Lens Readback=-117.546432062561;
    Vacuum:Analyzer Vacuum Ok Readback=1;Interface Pressure Readback=1.72991759314833;Analyzer Pressure
    Readback=6.61645877580996E-07;
    Detector:Detector Voltage (Counting) Readback=2123.16715542522;Detector Voltage (Analog) Readback=-2256
    .10948191593;
    Cooling System:Plasma Cooling Water Flow Readback=0.636862745098039;Interface Temperature Readback=33
    .7570376683741;Exhaust Flow Readback=0.127821114369501;Quad Board Temperature Readback=56
    .0606060606061;Inlet Fan Speed Readback=2517.64705882353;Outlet Fan Speed Readback=2329.41176470588;
    Peltier Temperature Hot Side Readback=26.4220279647803;Spray Chamber Temperature Readback=20;
    Power Supply:Supply Voltage 500 V Readback=-543.132942326491;Supply Voltage 1 kV Readback=-1250;
    Gas Supply:Nebulizer Supply Pressure Readback=0.216515591397849;Nebulizer Flow Readback=1
    .01173020527859;Cool Flow Readback=14.4868035190616;Auxilliary Flow Readback=0.848973607038123;CCT1
    Flow Readback=0.727272727272727;CCT2 Flow Readback=0.269794721407625;CCT3 Flow Readback=0;CCT4 Flow
    Readback=0;Additional Gas Flow 1 Readback=0;
    Pulse Counting:Threshold=2500000;

    Time,24Mg | 24Mg,25Mg | 25Mg,55Mn | 55Mn,56Fe | 56Fe.16O,63Cu | 63Cu,65Cu | 65Cu,66Zn | 66Zn,67Zn |
    67Zn,68Zn | 68Zn,
    ,dwell time=0.1;xcal factor=70632.52789,dwell time=0.1;xcal factor=71761.14834,dwell time=0.1;xcal
    factor=83371.40584,dwell time=0.1;xcal factor=84450.9008,dwell time=0.1;xcal factor=90097.8123,dwell
    time=0.1;xcal factor=89387.22218,dwell time=0.1;xcal factor=89507.29634,dwell time=0.1;xcal
    factor=89627.3705,dwell time=0.1;xcal factor=89747.44466,
    0.11085,264029.317592294,40284.810202654,1168135.08284413,202181.984754494,691375.466915058,347442
    .463333404,357757.384280381,53725.2083367573,243855.645535877,
    1.09243,259189.596285404,39131.1541677333,1113566.01843743,189262.03925817,696794.27352079,323215
    .391977038,315789.153905821,54689.375969867,246170.357926065,
    2.07411,257270.557902798,39983.8462056211,1211429.35747189,178525.819470373,732999.339588305,354424
    .428196879,352254.363021567,51847.3031786585,240970.497142116,
    3.05572,248281.500967918,33976.1123797218,1099618.69501071,183669.538679497,717963.236068682,367689
    .437787061,354249.575062777,53554.4780522846,244773.322131386,
    4.03719,256556.096831616,36392.9007204873,1144129.86897817,182502.633082475,781552.288700585,329188
    .262332579,355257.585076017,52620.5241489224,259322.306527736,
    5.01885,255770.236337436,36663.6903080872,1123269.22892051,190328.042558354,692199.231700474,353632
    .465630616,387277.84608648,52972.0040052687,241408.834089229,
    6.0012,260802.630680195,33996.1667945069,1171816.93128118,181122.779931491,681504.96084495,325934
    .648553018,348069.551858297,53353.621873141,245476.919373217,
    6.98281,246782.247109041,37857.2401471024,1086241.53188427,188023.558257682,646675.80098672,331765
    .060646272,336129.361006602,48995.8358549322,240623.915915203,
    7.96453,250341.990631921,37275.4957580846,1166502.68991648,179976.39931172,686286.194545877,344307
    .486059617,346568.703192492,50993.802985357,253882.329838331,
    8.94618,270801.913302954,34377.2067803508,1166119.27145151,194552.333116324,677738.222648587,309996
    .841312394,309115.411995954,50210.6422112482,233358.095668745,
     */

    // settings
    double userDefinedDwellTime = params.getCustomDwellTime().getValue() / 1E6;
    boolean useUserDefinedDwellTime = params.getDwellTimeSource().getValue().equals(Source.CUSTOM);

    // Sample name
    String internalSampleName = PathUtil.getFileNameWithoutExtension(file);
    int indexCounterColIdx = -1;
    int timeColIdx = -1;
    List<MZValue> mzColAndEntries = new ArrayList<>();
    HashMap<MZValue, Double> mzSpecificDwellTimes = new LinkedHashMap<>();

    boolean hasHeaderLine = false; // Line that contains "Time, mz, and so forth"
    int headerLineIndex = -1; // index within the csv of that line

    // For header indices larger than this we assume the lengthy header export version
    final int headerLineCutoffForLaserReductionExport = 3;

    final String NUMBER = "Number";
    final String TIME = "Time";
    final String DWELL_TIME = "dwell time";
    final String INTENSITY = "Intensity";

    if (!csv.isEmpty()) {
      LOGGER.trace("Parsing iCAP Thermo Fisher csv data: preview.");

      // 200 lines to find out "where we are"
      int previewLineCount = 200;

      previewLoop:
      for (int i = 0; i < Math.min(previewLineCount, csv.size()); i++) {
        String[] line = csv.get(i);

        boolean isPresentNumber;
        boolean isPresentTime;
        boolean isPresentDwellTime;
        boolean isPresentIntensity;
        boolean isPresentMZ;

        for (String cell : line) {
          cell = cell.toLowerCase(Locale.ROOT);
          isPresentNumber = cell.contains(NUMBER.toLowerCase(Locale.ROOT));
          isPresentTime = cell.contains(TIME.toLowerCase(Locale.ROOT));
          isPresentDwellTime = cell.contains(DWELL_TIME.toLowerCase(Locale.ROOT));
          isPresentIntensity = cell.contains(INTENSITY.toLowerCase(Locale.ROOT));
          isPresentMZ = cell.contains(NUMBER.toLowerCase(Locale.ROOT));

          if (isPresentNumber || isPresentTime || isPresentDwellTime || isPresentIntensity
              || isPresentMZ) {
            headerLineIndex = i;
            hasHeaderLine = true;
            break previewLoop;
          }
        }
      }

      // SampleName: predict if laser reduction export
      if (headerLineIndex > headerLineCutoffForLaserReductionExport) {
        // sample name is in first line
        String[] fistLine = csv.get(0);
        if (fistLine.length > 0) {
          internalSampleName = FileInterpreterUtils.parseICapLASampleName(fistLine[0]);
          LOGGER.trace("Found iCAP sample name within the file: " + internalSampleName + ".");
        }
      }

      // extract information from header, which is initialized as -1 --> check >-1 here!
      if (hasHeaderLine && headerLineIndex >= 0) {
        LOGGER.trace("Identified iCAP csv as file with a proper meta data header.");
        String[] header = csv.get(headerLineIndex);

        for (int colIdx = 0; colIdx < header.length; colIdx++) {
          String cell = header[colIdx];
          // prepare check for mz
          Pair<Boolean, MZValue> mz = FileInterpreterUtils.parseIcapTQmz(cell);

          /*
          Else ifs are needed because they sometimes put
          Number,Time 197Au,Intensity (cps) 197Au
          the element also in the Time for some obscure reason.
           */

          if (cell.toLowerCase(Locale.ROOT).contains(NUMBER.toLowerCase(Locale.ROOT))) {
            indexCounterColIdx = colIdx;
          } else if (cell.toLowerCase(Locale.ROOT).contains(TIME.toLowerCase(Locale.ROOT))) {
            timeColIdx = colIdx;
          } else if (mz.getKey()) {
            mzColAndEntries.add(mz.getValue());
            if (csv.size() > headerLineIndex + 1) {
              String[] subHeader = csv.get(headerLineIndex + 1);
              if (subHeader.length > colIdx) {
                if (useUserDefinedDwellTime) {
                  mzSpecificDwellTimes.put(mz.getValue(), userDefinedDwellTime);
                } else {
                  double dt = FileInterpreterUtils.getIcapDeclaredDwellTimeValue(subHeader[colIdx]);
                  if (dt > 0) {
                    mzSpecificDwellTimes.put(mz.getValue(), dt);
                  }
                }
              }
            }
          }
        }
        LOGGER.trace("Found mz: " + mzColAndEntries.stream()
            .map(MZValue::getElementTransition)
            .collect(Collectors.joining(", "))
            + ".");
      } else {
        // No header. We have to assume that time is at index 0 and intensity thereafter
        LOGGER.trace("Identified iCAP csv as file WITHOUT any proper meta data header.");
        if (params.getHasLineIndex().getValue()) {
          indexCounterColIdx = 0;
        }
        timeColIdx = 0;
        mzColAndEntries.add(new SQmz(params.getIsotopeParameter().getValue().unwrap()));
      }

      // How many MZ and how many time?
      int dataLineLength = mzColAndEntries.size();
      int intensityColumn = 0;
      if (indexCounterColIdx >= 0) {
        dataLineLength++;
        intensityColumn++;
      }
      if (timeColIdx >= 0) {
        dataLineLength++;
        intensityColumn++;
      }

      // go through csv
      List<Double> time = new ArrayList<>();
      List<List<Double>> intensities = new ArrayList<>();

      LOGGER.trace("Parsing iCAP csv with"
          + " time column index: " + timeColIdx
          + ", starting intensity column index: " + intensityColumn
          + ", and number of mZ: " + mzColAndEntries.size()
          + ".");

      for (int rowIdx = headerLineIndex + 1; rowIdx < csv.size(); rowIdx++) {
        String[] line = csv.get(rowIdx);
        // check if within data line
        if (line.length >= dataLineLength) {

          // time
          if (timeColIdx >= 0) {
            double timeValue;
            if (params.getTimeStampFormat().getValue().equals(TimeStampFormat.TIME)) {
              LocalTime parsedTime;
              try {
                parsedTime = LocalTime.parse(line[timeColIdx], nanoFormatter);
                timeValue = parsedTime.toSecondOfDay() + parsedTime.getNano() / 1_000_000_000d;
                time.add(timeValue);
              } catch (DateTimeParseException e) {
                try {
                  parsedTime = LocalTime.parse(line[timeColIdx], secondsFormatter);
                  timeValue = parsedTime.toSecondOfDay() + parsedTime.getNano() / 1_000_000_000d;
                  time.add(timeValue);
                } catch (DateTimeParseException e2) {
                  // Fallback: assuming it may actually be a decimal time signature
                  if (SnF.isValidDoubleSilent(line[timeColIdx])) {
                    timeValue = SnF.strToDouble(line[timeColIdx]);
                    time.add(timeValue);
                  }
                }
              }
            } else {
              if (SnF.isValidDoubleSilent(line[timeColIdx])) {
                timeValue = SnF.strToDouble(line[timeColIdx]);
                time.add(timeValue);
              }
            }
          }

          // intensity
          int mzCounter = -1;
          for (int colIdx = intensityColumn; colIdx < line.length; colIdx++) {
            mzCounter++;
            if (SnF.isValidDoubleSilent(line[colIdx])) {
              if (intensities.size() > mzCounter) {
                intensities.get(mzCounter).add(SnF.strToDouble(line[colIdx]));
              } else {
                List<Double> list = new ArrayList<>();
                list.add(SnF.strToDouble(line[colIdx]));
                intensities.add(list);
              }
            }
          }

        }
      }

      // Re-process/adjust read data
      if (intensities.size() > 0 && intensities.size() == mzColAndEntries.size()) {

        Sample sample = new SampleImpl(internalSampleName,
            new SampleFile(file, internalSampleName));

        List<Trace> traces = new ArrayList<>();

        // if we know the dwell time, use it
        if (!mzSpecificDwellTimes.isEmpty()
            && mzColAndEntries.size() == mzSpecificDwellTimes.size()) {

          LOGGER.trace("Reading iCAP sample file using dwell time from meta data.");

          // create new time series for each intensity with the respective dwell time
          for (int i = 0; i < mzColAndEntries.size(); i++) {
            TISeries tiSeries;
            MZValue mzI = mzColAndEntries.get(i);
            double dt;
            if (useUserDefinedDwellTime) {
              LOGGER.trace("Pre-processing iCAP sample with user defined dwell time.");
              dt = userDefinedDwellTime;
            } else {
              LOGGER.trace("Pre-processing iCAP sample with mz-specific dwell time from file.");
              dt = mzSpecificDwellTimes.get(mzI);
            }
            double[] intensity = ArrUtils.doubleListToArr(intensities.get(i));
            double[] timeArr = ArrUtils.incrementArrayInclusive(dt, intensity.length);

            // Check if cps/cst conversion is needed
            SignalConversionOption conversion = params.getSignalConversionOption().getValue();
            boolean convertToCts = RawProcessingUtils.checkConversion(conversion, unit, intensity);
            if (convertToCts) {
              LOGGER.trace("Check for unit conversion: applying conversion to counts.");
              intensity = RawProcessingUtils.cpsToCts(intensity, dt);
            }
            // Create sample.
            tiSeries = new TISeriesHDD(timeArr, intensity);
            Trace trace = new TraceImpl(sample, mzI, tiSeries);
            traces.add(trace);
          }
        } else if (mzColAndEntries.size() == intensities.size()) {

          LOGGER.trace("Reading iCAP sample file using dwell time from time series.");

          // create new time series for each intensity with the respective dwell time
          for (int i = 0; i < mzColAndEntries.size(); i++) {
            TISeries tiSeries;
            MZValue mzI = mzColAndEntries.get(i);
            double[] intensity = ArrUtils.doubleListToArr(intensities.get(i));

            // Prepare time as array
            double[] timeArr;
            if (useUserDefinedDwellTime) {
              timeArr = ArrUtils.incrementArrayInclusive(userDefinedDwellTime, intensity.length);
              LOGGER.trace("Pre-processing iCAP sample with mz-specific dwell time from file.");
            } else {
              // It is possible to get here with an empty time array (if numbers were parsed poorly)
              if (time.size() > 0 && time.size() == intensity.length) {
                // check if we have to divide
                if (params.getIsIonicQmsData().getValue()) {
                  timeArr = ArrUtils.divide(time, intensities.size());
                  LOGGER.trace("1/2 Time stamps were divided by number of mz "
                      + "to account for QMS scan.");
                  timeArr = replaceTimeStamps(timeArr);
                  LOGGER.trace("2/2 Time stamps were replaced by calculating time"
                      + " based on index and dwell time.");
                } else {
                  timeArr = ArrUtils.doubleListToArr(time);
                  timeArr = replaceTimeStamps(timeArr);
                  LOGGER.trace("Time stamps were replaced by calculating time"
                      + " based on index and dwell time.");
                }
              } else {
                // This triggers when time is empty and no user defined DT was given.
                break;
              }
            }

            // Check if cps/cst conversion is needed
            SignalConversionOption conversion = params.getSignalConversionOption().getValue();
            boolean convertToCts = RawProcessingUtils.checkConversion(conversion, unit, intensity);
            if (convertToCts) {
              LOGGER.trace("Check for unit conversion: applying conversion to counts.");
              intensity = RawProcessingUtils.cpsToCts(timeArr, intensity);
            }
            // Create sample.
            tiSeries = new TISeriesHDD(timeArr, intensity);
            Trace trace = new TraceImpl(sample, mzI, tiSeries);
            traces.add(trace);
          }
        }

        traces.forEach(sample::addTrace);
        this.samples.clear();
        if (!traces.isEmpty()) {
          LOGGER.trace("Finished reading Thermo Fisher iCAP sample.");
          this.samples.add(sample);
        } else {
          LOGGER.error("Something went wrong. Could not parse iCAP sample csv file.");
        }
      } else {
        LOGGER.error("Cannot parse iCAP sample csv as there is"
            + " no equal number of mz and intensity entries.");
      }
    }
  }


  public void parseTimeStampSafely(List<Double> time, String cell) {
    // TODO: move code here
  }


  // In thermo exports, first time stamp is usually 'wrong' (way larger than DT)
  public static double[] replaceTimeStamps(double[] time) {
    double[] result;
    if (time.length > 2) {
      result = new double[time.length];

      // --> estimate dwell time
      // check if duplicate time stamps b/c of too few digits
      boolean hasDuplicates = ArrUtils.hasDuplicates(time);

      double dwellTimeGuess;

      // case a: there are no duplicate time stamps:
      if (!hasDuplicates) {
        List<Double> differences = new ArrayList<>();
        for (int i = 1; i < Math.min(time.length, 100); i++) {
          differences.add(time[i] - time[i - 1]);
        }
        // Estimate dwell time as mode of differences
        dwellTimeGuess = Mode.calculateMode(differences);
      } else {
        /*
        we cannot rely on differences, as we do not know how many equal stamps there are in between.
        thus, use duration and number of points to estimate DT:
        idx     ms      I
        0       1.5     0
        1       3       5
        2       4.5     4
        3       6       0
        --> DT=1.5ms, duration=6ms, n=4 --> 6/4=1.5
         */
        dwellTimeGuess = time[time.length - 1] / time.length;
      }

      // create artificial time stamps
      result[0] = dwellTimeGuess;
      for (int i = 1; i < result.length; i++) {
        result[i] = result[i - 1] + dwellTimeGuess;
      }
    } else {
      result = time;
    }
    return result;
  }


}
