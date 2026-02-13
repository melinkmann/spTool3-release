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

import analysis.*;
import analysis.Baseline.ThrFormalism;
import analysis.Baseline.ThrMeasureOfSignificance;
import analysis.PopulationStep.GateSubtype;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.TISeries;
import dataModelNew.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import processing.options.GatingMeasureOfSignificance;
import processing.options.GatingOption;
import processing.parameterSets.impl.GatingParams;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;

public class GatingTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(GatingTask.class);

  private final PopulationBranch branch;
  private final GatingParams params;
  private final AtomicReference<Sample> sampleRef;

  public GatingTask(String taskName, PopulationBranch branch, GatingParams params,
                    AtomicReference<Sample> sampleRef) {
    super(taskName);
    this.branch = branch;
    this.params = (GatingParams) params.getCopyWithPreviousDateFileAndID();
    this.sampleRef = sampleRef;
  }

  @Override
  public TaskResult call() {
    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new EmptyTaskResult();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null && params.getEnableBoolean().getValue()) {

        LOGGER.info("Gating starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          Sample sample = sampleRef.get();
          if (sample instanceof SampleImpl) {

            List<Trace> traces = sample.getTraces();
            for (Trace trace : traces) {

              PopulationID popID = branch.getID(trace);
              if (popID != null) {

                if (getIsStopped().get()) {
                  break;
                }

                Baseline bln = trace.getBaseline();

                if (bln.hasBaseline()) {
                  GatingOption gatingOption = params.getGatingOption().getValue();

                  ThresholdSupplierInstructions instr = getSupplier(params);
                  ThresholdSupplier supplier = instr.get(bln.getBackgroundDistribution());

                  boolean forceNewBGDefinition = params.getForceNewBGDefinition().getValue();

                  TISeries tiSeries = trace.getTISeries();
                  double[] time = tiSeries.getTime();
                  double[] intensity = tiSeries.getIntensity();

                  List<Event> chosenEvents = new ArrayList<>();
                  Population oldPop = trace.getPopulation(popID);

                  if (oldPop != null) {
                    for (Event event : oldPop.getEvents().getNpEvents()) {

                      if (getIsStopped().get()) {
                        break;
                      }

                      double target = supplier.interpolate(event.getPeak(), time.length);
                      switch (gatingOption) {

                        case HEIGHT -> {
                          double h = event.get(EventParameter.HEIGHT, time, intensity);
                          if (h > target) {
                            chosenEvents.add(event);
                          }
                        }

                        case AREA -> {
                          double a = event.get(EventParameter.AREA, time, intensity);
                          if (a > target) {
                            chosenEvents.add(event);
                          }
                        }

                        case NET_AREA -> {
                          double a = event.get(EventParameter.NET_AREA, time, intensity);
                          if (a > target) {
                            chosenEvents.add(event);
                          }
                        }

                        case MEAN_SIGNAL -> {
                          double a = event.getProfile().getMeanIntensity();
                          if (a > target) {
                            chosenEvents.add(event);
                          }
                        }

                        case PEAK_DOMINANCE -> {
                          double area = event.get(EventParameter.NET_AREA, time, intensity);
                          double height = event.get(EventParameter.NET_HEIGHT, time, intensity);
                          double pct = target / 100d;
                          if (height < area * pct) {
                            chosenEvents.add(event);
                          }
                        }

                        case MORE_POINTS_THAN -> {
                          double p = event.get(EventParameter.NO_OF_POINTS, time, intensity);
                          if (p > target) {
                            chosenEvents.add(event);
                          }
                        }

                        case FEWER_POINTS_THAN -> {
                          double p = event.get(EventParameter.NO_OF_POINTS, time, intensity);
                          if (p < target) {
                            chosenEvents.add(event);
                          }
                        }

                        case ACCUMULATED_P -> {
                          int[] evtIdcs = event.getIndices();
                          // Chi-square distribution with 2k degrees of freedom
                          ChiSquaredDistribution chiSquared = new ChiSquaredDistribution(2 * evtIdcs.length);
                          double chiSquareStat = 0;
                          double lowestP = 1;
                          StatCollection bgDist = bln.getBackgroundDistribution();
                          for (int evtIdc : evtIdcs) {
                            StatDataSet distribution = bgDist.interpolate(evtIdc, tiSeries.size());
                            double value = tiSeries.getIntensity()[evtIdc];
                            double p = distribution.calcPValue(value);
                            // prevent failure here by clamping p.
                            p = Math.max(Math.nextUp(0d), p);
                            p = Math.min(p, 1);
                            chiSquareStat += -2.0 * Math.log(p);
                            lowestP = Math.min(lowestP, p);
                          }
                          double combinedPValue;
                          if (evtIdcs.length > 1) {
                            combinedPValue = 1.0 - chiSquared.cumulativeProbability(chiSquareStat);
                          } else {
                            combinedPValue = lowestP;
                          }

                          ///
                          // double area = event.get(EventParameter.NET_AREA);
//                          System.out.println(
//                              "mz: " +  event.getCollection().getTrace().getMzValue().getName()+
//                              "\tcombinedPValue: " + combinedPValue +
//                              "\tarea: " + area +
//                              "\tpoints: " + event.getNoOfPoints());
                          if (combinedPValue < target) {
                            chosenEvents.add(event);
                          }
                        }
                      }
                    }

                    // (1) Remove existing population from the Trace to avoid pile up of Populations)
                    trace.removePopulation(popID);
                    // (2) Update the ID with the latest step
                    PopulationID idCopy = new PopulationID(popID);
                    idCopy.append(new GateSubtype(params.getGatingOption().getValue()));
                    // (3) Add the new Population to the trace

                    if (!forceNewBGDefinition) {
                      trace.addOverridePopulation(idCopy,
                          new NpPopulation(
                              idCopy,
                              oldPop,
                              new SubEventCollection(trace, chosenEvents, oldPop),
                              idCopy.toString(),
                              instr));
                    } else {
                      // we want the new population to also define the BG
                      trace.addOverridePopulation(idCopy,
                          new NpPopulation(
                              idCopy,
                              oldPop,
                              new MainEventCollection(trace, chosenEvents),
                              idCopy.toString(),
                              instr));
                    }
                    // (4) Updating the branch:
                    // Make this new head of branch! Else, OG ID in HashMap is altered, affecting bucketing
                    // in buggy way
                    branch.overrideID(trace, idCopy);
                  }
                }

                setProgress(counter / traces.size());
              }
            }
          }
          setProgress(1);
          break mainLoop;
        }
      }


    } catch (Exception e) {
      LOGGER.error("{}. Stack trace: {}", e.getMessage(), ExceptionUtils.getStackTrace(e));
    }
    setProgress(1);
    return taskResult;
  }


  private ThresholdSupplierInstructions getSupplier(GatingParams params) {
    ThresholdSupplierInstructions instructions;
/*
String description,
      ThrType thrType,
      ThrSignificance thrSignificance,
      double significance,
      double factor
 */

    GatingOption gatingOption = params.getGatingOption().getValue();
    String description = params.getLabelParameter().getValue() +
        " (" + gatingOption.shortString() + ")";
    GatingMeasureOfSignificance measureOfSignificance =
        params.getMeasureOfSignificance().getValue();
    boolean useFactor = params.getFactorBoolean().getValue();
    double factor = params.getFactor().getValue();
    double alpha = params.getAlphaValue().getValue();
    double zFactor = params.getzValue().getValue();
    double absValueCutoff = params.getAbsoluteCutoff().getValue();
    double peakDominancePct = params.getPeakDominancePct().getValue();
    double accPVal = params.getpValueAccumulationAlpha().getValue();
    double factorForGate = 1; // 1 is neutral

    // define threshold
    double significance = 0;

    ThrFormalism thrFormalism = null;
    ThrMeasureOfSignificance thrMeasureOfSignificance = null;
    boolean isHeight = (gatingOption == GatingOption.HEIGHT || gatingOption == GatingOption.MEAN_SIGNAL);
    boolean isIntensity = true;

    // check if we have to override
    switch (gatingOption) {

      case HEIGHT, AREA, NET_AREA, MEAN_SIGNAL -> {
        switch (measureOfSignificance) {
          case ALPHA -> {
            thrFormalism = ThrFormalism.CRITICAL_LIMIT_FORMALISM;
            significance = alpha;
            thrMeasureOfSignificance = ThrMeasureOfSignificance.ALPHA;
          }
          case Z_VALUE -> {
            thrFormalism = ThrFormalism.CRITICAL_LIMIT_FORMALISM;
            significance = zFactor;
            thrMeasureOfSignificance = ThrMeasureOfSignificance.FACTOR;
          }
          case CUSTOM_VALUE -> {
            thrFormalism = ThrFormalism.STATIC_VALUE;
            thrMeasureOfSignificance = null;
          }
        }

        if (useFactor) {
          factorForGate = factor;
          thrFormalism = ThrFormalism.DETECTION_LIMIT_FORMALISM;
        }

        isIntensity = true;
      }

      case MORE_POINTS_THAN, FEWER_POINTS_THAN -> {
        thrFormalism = ThrFormalism.STATIC_VALUE;
        significance = absValueCutoff;
        thrMeasureOfSignificance = null;
        isIntensity = false;
      }

      case PEAK_DOMINANCE -> {
        thrFormalism = ThrFormalism.STATIC_VALUE;
        significance = peakDominancePct;
        thrMeasureOfSignificance = null;
        isIntensity = false;
      }

      case ACCUMULATED_P -> {
        thrFormalism = ThrFormalism.STATIC_VALUE;
        significance = accPVal;
        thrMeasureOfSignificance = null;
        isIntensity = false;
      }
    }

    instructions = new ThresholdSupplierInstructions(
        description,
        thrFormalism,
        thrMeasureOfSignificance,
        significance,
        factorForGate,
        isHeight,
        isIntensity
    );

    return instructions;
  }


}
