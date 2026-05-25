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

package tasks.single;

import analysis.ResettableStatDataSet;
import com.google.common.util.concurrent.AtomicDouble;
import core.SpTool3Main;
import dataModelNew.*;
import dataModelNew.mz.IsotopeMZ;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;
import dataModelNew.mz.TOFmz;
import gui.dialog.notification.NotificationFactory;
import io.nu.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import math.stat.MeasureOfLocation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.IsotopeConflictReaderOption;
import processing.options.TofIsotopeOption;
import processing.parameterSets.impl.ConfParams;
import processing.parameterSets.impl.NuInterpreterParams;
import sandbox.montecarlo.Isotope;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;
import util.Functional;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NuImportTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(NuImportTask.class);


  private final @Nullable Sample existingSample;
  private final NuInterpreterParams params;
  private final Path directory;
  private final @Nullable Stage parentStage;

  public NuImportTask(NuInterpreterParams params, Path path, @Nullable Stage parentStage) {
    super("Nu data import");
    // pass a copy to avoid changes in UI trickling down into multi thread environment when running in the
    // background
    NuInterpreterParams p = ((NuInterpreterParams) params.getCopyWithPreviousDateFileAndID());
    this.existingSample = null;
    this.params = p;
    this.directory = path;
    this.parentStage = parentStage;
  }

  public NuImportTask(Sample existingSample, NuInterpreterParams params, Path path,
                      @Nullable Stage parentStage) {
    super("Nu data import");
    // pass a copy to avoid changes in UI trickling down into multi thread environment when running in the
    // background
    NuInterpreterParams p = ((NuInterpreterParams) params.getCopyWithPreviousDateFileAndID());
    this.existingSample = existingSample;
    this.params = p;
    this.directory = path;
    this.parentStage = parentStage;
  }

  @Override
  public TaskResult call() {

    List<Sample> result = new ArrayList<>();

    // these keep track of progress in the functions of the NU parser
    AtomicDouble progressValue = new AtomicDouble();
    Functional progressTicker = new Functional() {
      @Override
      public void proceed() {
        setProgress(progressValue.get());
      }
    };

    // Catch any Exception that may occur in the background that would not go through the stack.
    setProgress(0);
    // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
    LOGGER.info("Nu import starts for " + directory
        + " in thread " + Thread.currentThread().getId());

    ParsedNuData nuData = null;
    try {

      TofIsotopeOption tofOption = params.getIsotopeSelectionStrategy().getValue();
      double alpha = params.getPreScreenAlpha().getValue();
      MeasureOfLocation location = params.getPreScreenMeasureOfLocation().getValue();
      double siaShape = params.getPreScreenSiaShape().getValue();
      int nThr = params.getPreScreenDataPoints().getValue();
      IsotopeConflictReaderOption conflictOption = params.getPreScreenConflictResolution().getValue();

      setProgress(0.1);

      // Track which isotopes are available, however, we do not interpret them yet so that the user can
      // decide to import 48Ca or 48Ti.
      List<Double> availableMZ = NuReader_new.listAvailableMZFromCacheOrParse(directory);
      List<Isotope> recordedToFRange = Isotope.getFromNominalMass(availableMZ);

      // Now decide the actual import:
      switch (tofOption) {
        /// USE THE SELECTION BY THE USER
        case DEFAULTS -> {
          if (getIsStopped().get()) {
            break;
          }
          // Bridge: restore previously saved isotope preferences to pre-select
          // matching channels in the dialog. Pass empty list if no prefs stored yet.
          List<Isotope> userSelMZ = params.listDefaultIsotopes();
          HashMap<Double, Isotope> isotopeMap = new HashMap<>();
          List<Double> prevSelMZ = new ArrayList<>();
          for (Isotope isotope : userSelMZ) {
            prevSelMZ.add(isotope.getTheoreticalMass());
            isotopeMap.put(isotope.getTheoreticalMass(), isotope);
          }

          if (!userSelMZ.isEmpty()) {
            nuData = NuReader_new.getFromCacheOrParse(directory, progressTicker, progressValue,
                getIsStopped());

            if (getIsStopped().get()) {
              break;
            }

            if (nuData != null && nuData.isValid()) {
              Sample sample = new SampleImpl(directory.getFileName().toString(),
                  new SampleFile(directory, InstrumentID.NU_VITESSE),recordedToFRange);
              List<Trace> traces = new ArrayList<>();

              List<ParsedNuData.ParsedNuDataResult> data = nuData.getIsotopeData(prevSelMZ);

              for (ParsedNuData.ParsedNuDataResult parsedData : data) {

                TISeries ser = parsedData.tiSeries();
                List<int[]> blanks = parsedData.blanker();

                if (ser == null) {
                  continue;   // channel was not loaded (should not happen, but guard anyway)
                }

                double sia = ShapeEstimator.computeShape(ser.getIntensity(), blanks);
                if (!Double.isFinite(sia)) {
                  sia = 0;
                }

                Isotope isotope = isotopeMap.get(parsedData.requestedMZ());
                if (isotope != null) {
                  traces.add(new TraceImpl(sample, new IsotopeMZ(isotope), ser, sia));
                } else {
                  LOGGER.error("Unexpected mismatch of isotope and m/z.");
                }
              }

              // insert traces in sorted order (backed by linked HashMap in Sample)
              traces.sort(Comparator.comparingDouble(t -> t.getMzValue().getMZ()));
              for (Trace trace : traces) {
                sample.addTrace(trace);
              }

              if (!sample.getTraces().isEmpty()) {
                // Handle case where we reload isotopes into an existing sample
                if (existingSample == null) {
                  // make sure to switch the traces to HDD
                  sample.getTraces().forEach(Trace::toHDD);
                  SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(sample);
                  // make sure the method is stored in the sample (esp. for isotope selection)
                  if (!sample.getMethod().getSets().contains(params)) {
                    sample.getMethod().getSets().add(params);
                  }
                  // instanceof SampleImpl should be guaranteed but check anyway
                } else if (existingSample instanceof SampleImpl) {
                  HashSet<Trace> existingTraces = new HashSet<>(existingSample.getTraces());
                  HashSet<Trace> newTraces = new HashSet<>(sample.getTraces());

                  // existing sample: remove old traces
                  for (Trace existingTrace : existingSample.getTraces()) {
                    if (!newTraces.contains(existingTrace)) {
                      existingSample.removeTraces(List.of(existingTrace));
                    }
                  }

                  for (Trace newTrace : sample.getTraces()) {
                    if (!existingTraces.contains(newTrace)) {
                      Trace newTraceHDD = newTrace.copy(existingSample);
                      newTraceHDD.toHDD();
                      existingSample.addTrace(newTraceHDD);
                    }
                  }
                }
              }
            } else {
              Platform.runLater(() -> {
                NotificationFactory.openInfo("The sample did not contain any of the requested isotopes." +
                    " " +
                    "The sample was not loaded.");
              });
            }
          }

        }

        /// OPEN THE DIALOG
        case DIALOG -> {
          if (getIsStopped().get()) {
            break;
          }

          // This is based on which MZ are there in NU folder
          if (availableMZ != null && !availableMZ.isEmpty()) {

            // <<< ChatGPT to get the dialog essentially from another thread
            // NEVER block FX thread
            if (Platform.isFxApplicationThread()) {
              LOGGER.error("Cannot execute import on FX thread.");
              break;
            }

            // Prepare dialog execution on FX thread
            FutureTask<List<MZValue>> dialogTask = new FutureTask<>(() -> {

              // Bridge: restore previously saved isotope preferences to pre-select
              // matching channels in the dialog. Pass empty list if no prefs stored yet.
              List<Isotope> prevSel = NuInterpreterParams.isotopeFromString(
                  SpTool3Main.getRunTime()
                      .getConfParams()
                      .getNuImportSelectedIsotopes()
                      .getValue()
              );

              IsotopePtoeDialog dlg = new IsotopePtoeDialog(
                  parentStage,
                  availableMZ,
                  prevSel
              );

              return dlg.showAndWait();   // runs on FX thread
            });

            // Schedule dialog on FX thread
            Platform.runLater(dialogTask);

            // Wait for dialog to close
            List<MZValue> resultingMZ;
            try {
              resultingMZ = dialogTask.get();   // blocks THIS thread only
            } catch (InterruptedException | ExecutionException e) {
              LOGGER.error("Prompting for isotope selection failed. Try another isotope selection option " +
                  "form the import.");
              break;
            }

            if (resultingMZ != null && !resultingMZ.isEmpty()) {

              // Persist the user's isotope choices for next time (this is not for the
              // "csv reader" but for the dialog popup window)
              List<Isotope> selectedIsotopes = new ArrayList<>();
              for (MZValue mzValue : resultingMZ) {
                selectedIsotopes.add(mzValue.getIsotope());
              }
              SpTool3Main.getRunTime().getConfParams().getNuImportSelectedIsotopes()
                  .setValue(NuInterpreterParams.isotopesToString(selectedIsotopes));

              // Load the selected channels using recorded mz values (not theoretical)
              // so that channelData map keys match mzVal.getMZ() exactly below.
              List<Double> recordedMzList = new ArrayList<>();
              HashMap<Double, MZValue> mzMap = new HashMap<>();
              for (MZValue mzValue : resultingMZ) {
                recordedMzList.add(mzValue.getMZ());
                mzMap.put(mzValue.getMZ(), mzValue);
              }

              setProgress(0.3);
              nuData = NuReader_new.getFromCacheOrParse(directory, progressTicker, progressValue,
                  getIsStopped());

              if (getIsStopped().get()) {
                break;
              }

              if (nuData != null && nuData.isValid()) {

                Sample sample = new SampleImpl(directory.getFileName().toString(),
                    new SampleFile(directory, InstrumentID.NU_VITESSE),recordedToFRange);
                List<Trace> traces = new ArrayList<>();

                List<ParsedNuData.ParsedNuDataResult> data = nuData.getIsotopeData(recordedMzList);

                for (ParsedNuData.ParsedNuDataResult parsedData : data) {

                  TISeries ser = parsedData.tiSeries();
                  List<int[]> blanks = parsedData.blanker();

                  if (ser == null) {
                    continue;   // channel was not loaded (should not happen, but guard anyway)
                  }

                  double sia = ShapeEstimator.computeShape(ser.getIntensity(), blanks);
                  if (!Double.isFinite(sia)) {
                    sia = 0;
                  }

                  MZValue mzValue = mzMap.get(parsedData.requestedMZ());
                  if (mzValue != null) {
                    traces.add(new TraceImpl(sample, mzValue, ser, sia));
                  } else {
                    LOGGER.error("Unexpected mismatch of m/z values.");
                  }
                }

                // insert traces in sorted order (backed by linked HashMap in Sample)
                traces.sort(Comparator.comparingDouble(t -> t.getMzValue().getMZ()));
                for (Trace trace : traces) {
                  sample.addTrace(trace);
                }

                if (!sample.getTraces().isEmpty()) {

                  if (existingSample == null) {
                    // make sure to switch the traces to HDD
                    sample.getTraces().forEach(Trace::toHDD);
                    SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(sample);
                    // make sure the method is stored in the sample (esp. for isotope selection)
                    if (!sample.getMethod().getSets().contains(params)) {
                      sample.getMethod().getSets().add(params);
                    }
                    // Should be guaranteed but check anyway
                  } else if (existingSample instanceof SampleImpl) {
                    HashSet<Trace> existingTraces = new HashSet<>(existingSample.getTraces());
                    HashSet<Trace> newTraces = new HashSet<>(sample.getTraces());

                    // existing sample: remove old traces
                    for (Trace existingTrace : existingSample.getTraces()) {
                      if (!newTraces.contains(existingTrace)) {
                        existingSample.removeTraces(List.of(existingTrace));
                      }
                    }

                    for (Trace newTrace : sample.getTraces()) {
                      if (!existingTraces.contains(newTrace)) {
                        Trace newTraceHDD = newTrace.copy(existingSample);
                        newTraceHDD.toHDD();
                        existingSample.addTrace(newTraceHDD);
                      }
                    }
                  }
                }

              } else {
                Platform.runLater(() -> {
                  NotificationFactory.openInfo("Could not match any of the requested isotopes to those " +
                      "present in the sample. The sample was not loaded.");
                });
              }
            }
          }
        }


        ///  READ ALL AND APPLY THRESHOLD
        case THRESHOLD -> {
          if (getIsStopped().get()) {
            break;
          }

          List<Isotope> validIsotopes = new ArrayList<>();
          // Just read all mz double values and filter later
          nuData = NuReader_new.getFromCacheOrParse(directory, progressTicker, progressValue,
              getIsStopped());

          if (getIsStopped().get()) {
            break;
          }

          if (nuData != null && nuData.isValid()) {
            Sample sample = new SampleImpl(directory.getFileName().toString(),
                new SampleFile(directory, InstrumentID.NU_VITESSE),recordedToFRange);

            List<ParsedNuData.ParsedNuDataResult> data = nuData.getIsotopeDataRAM();

            List<ParsedNuData.ParsedNuDataResult> sortedData = new ArrayList<>(data);
            sortedData.sort(Comparator.comparingDouble(ParsedNuData.ParsedNuDataResult::requestedMZ));

            for (int i = 0; i < sortedData.size(); i++) {
              progressValue.set(0.8 + 0.19 * ((double) i) / sortedData.size());

              ParsedNuData.ParsedNuDataResult parsedData = sortedData.get(i);

              TISeries ser = parsedData.tiSeries();
              List<int[]> blanks = parsedData.blanker();

              if (ser == null) {
                continue;   // channel was not loaded (should not happen, but guard anyway)
              }

              // prescreen
              double[] y = ser.getY();
              // speed up by lookup what's already available
              double mu;
              if (location.equals(MeasureOfLocation.MEAN)) {
                mu = ser.getMeanIntensity();
              } else if (location.equals(MeasureOfLocation.MEDIAN)) {
                mu = ser.getMedianIntensity();
              } else {
                mu = location.calc(y);
              }
              double cutoff = ResettableStatDataSet.CompoundPoissonTable.compoundCriticalLimit(alpha, mu,
                  siaShape);
              cutoff = Math.max(10, cutoff);
              int counter = 0;

              boolean includeTrace = false;
              for (double v : y) {
                if (v > cutoff && ++counter > nThr) {
                  includeTrace = true;
                  break;
                }
              }

              if (includeTrace) {
                int roundedNominalIsotopicNumber = (int) Math.round(parsedData.requestedMZ());

                //______________ We resolve conflicts with configuration ______________________
                if (conflictOption.equals(IsotopeConflictReaderOption.USE_DEFAULT)) {
                  Isotope guessIsotope = SpTool3Main.getRunTime()
                      .getConfParams().resolveConflictOrGet(roundedNominalIsotopicNumber);

                  double sia = ShapeEstimator.computeShape(ser.getIntensity(), blanks);
                  if (!Double.isFinite(sia)) {
                    sia = 0;
                  }

                  // transfer to HDD
                  if (ser instanceof DTISeriesRAM) {
                    ser = new DTISeriesHDD((DTISeriesRAM) ser);
                  }

                  sample.addTrace(new TraceImpl(sample, new SQmz(parsedData.requestedMZ(),
                      guessIsotope), ser, sia));
                  validIsotopes.add(guessIsotope);

                  //______________ we add all and e.g. 48 is both Ti and Ca ______________________
                } else {
                  // add each
                  List<Isotope> matches = Isotope.getFromNominalMass(roundedNominalIsotopicNumber);
                  for (Isotope isotopeMatch : matches) {

                    double sia = ShapeEstimator.computeShape(ser.getIntensity(), blanks);
                    if (!Double.isFinite(sia)) {
                      sia = 0;
                    }

                    // transfer to HDD
                    if (ser instanceof DTISeriesRAM) {
                      ser = new DTISeriesHDD((DTISeriesRAM) ser);
                    }

                    sample.addTrace(new TraceImpl(sample, new SQmz(parsedData.requestedMZ(),
                        isotopeMatch), ser, sia));
                    validIsotopes.add(isotopeMatch);
                  }
                }
              }
            }


            if (!sample.getTraces().isEmpty()) {
              if (existingSample == null) {
                // make sure to switch the traces to HDD
                sample.getTraces().forEach(Trace::toHDD);
                SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(sample);
                // make sure the method is stored in the sample (esp. for isotope selection)
                if (!sample.getMethod().getSets().contains(params)) {
                  sample.getMethod().getSets().add(params);
                }
                // Should be guaranteed but check anyway
              } else if (existingSample instanceof SampleImpl) {
                HashSet<Trace> existingTraces = new HashSet<>(existingSample.getTraces());
                HashSet<Trace> newTraces = new HashSet<>(sample.getTraces());

                // existing sample: remove old traces
                for (Trace existingTrace : existingSample.getTraces()) {
                  if (!newTraces.contains(existingTrace)) {
                    existingSample.removeTraces(List.of(existingTrace));
                  }
                }

                for (Trace newTrace : sample.getTraces()) {
                  if (!existingTraces.contains(newTrace)) {
                    Trace newTraceHDD = newTrace.copy(existingSample);
                    newTraceHDD.toHDD();
                    existingSample.addTrace(newTraceHDD);
                  }
                }
              }

              // This runs in parallel thread -> the results should only be stored in the local copy
              // or the copy that goes into the sample impl.
              // Else, we don't know which thread sets which value and
              // the outcome is entirely unpredictable/random!
              // Thus, we set the PARAMS to the sample (a bit hacky, but works)
              params.requestSavingIsotopes(validIsotopes);

            } else {
              Platform.runLater(() -> {
                NotificationFactory.openInfo("No isotopes provided enough particle events using " +
                    "thresholds " +
                    "defined in the nu import submethod. The sample was not loaded!");
              });
            }
          }

        }
      }

      LOGGER.info("Finished reading nu data from " + directory + ".");

      setProgress(1);
      // END ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

    } catch (
        Exception e) {
      result.clear();
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    return new FunctionalTaskResult(() -> {
      // I believe this is done abve in each step // (todo check - seems to work like this so leave commented)
      // List<Sample> samples = new ArrayList<>(result);
      // SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(samples);
      if (existingSample != null) {
        // We exchanged traces and must call update on the views
        SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets(); // this should refresh isotope view
      }
    });
  }
}