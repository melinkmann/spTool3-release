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
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.Trace;
import dataModelNew.mz.MZValue;
import math.stat.MeasureOfLocation;
import math.stat.Median;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.AlignAlgorithm;
import processing.options.EventParameter;
import processing.options.IsotopeSelection;
import processing.options.NetCorrectionOption;
import processing.parameterSets.impl.AlignerParams;
import sandbox.montecarlo.Isotope;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;
import util.ArrUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AlignTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(AlignTask.class);

  private final PopulationBranch branch;
  private final AlignerParams params;
  private final AtomicReference<Sample> sampleRef;
  private final List<Isotope> selectedIsotopes;

  public AlignTask(String taskName, PopulationBranch branch, AlignerParams params,
                   AtomicReference<Sample> sampleRef, List<Isotope> selectedIsotopes) {
    super(taskName);
    this.branch = branch;
    this.params = (AlignerParams) params.getCopyWithPreviousDateFileAndID();
    this.sampleRef = sampleRef;
    this.selectedIsotopes=new ArrayList<>(selectedIsotopes);
  }

  @Override
  public TaskResult call() {
    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new EmptyTaskResult();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null && params.getEnableBoolean().getValue()) {

        LOGGER.info("Merging starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread().getId());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          NetCorrectionOption netSignalChoice = params.getNetCorrectionOption().getValue();
          AlignAlgorithm alignAlgorithm = params.getAlignAlgorithm().getValue();
          IsotopeSelection isotopeSelection = params.getIsotopeSelection().getValue();
          List<Isotope> excludedIsotopes = params.listExcludedIsotopes();
          List<Isotope> includedIsotopes = params.listIncludedIsotopes();
          MeasureOfLocation netSignalLocation = params.getNetTimeWindowLocation().getValue();
          boolean suppressNegativeNetValues = params.getSuppressNegativeValues().getValue();
          double netSignalTime = params.getNetTimeWindow().getValue();

          Sample sample = sampleRef.get();
          if (sample instanceof SampleImpl) {

            List<Trace> traces = sample.getTraces();

            // enable exclusion of certain isotopes
            switch (isotopeSelection) {
              case ALL_LOADED -> {
              }
              case SELECTED -> {
                traces.removeIf(t -> !selectedIsotopes.contains(t.getMzValue().getIsotope()));
              }
              case POSITIVE_LIST_SELECTION -> {
                traces.removeIf(t -> !includedIsotopes.contains(t.getMzValue().getIsotope()));
              }
              case NEGATIVE_LIST_EXCLUSION -> {
                traces.removeIf(t -> excludedIsotopes.contains(t.getMzValue().getIsotope()));
              }
              default -> {
                // keep as is, we should not reach this branch
              }
            }

            List<MZValue> contributingMZs = new ArrayList<>();

            // Extract event collections of current leading population
            List<EventCollection> eventCollections = new ArrayList<>();
            for (Trace trace : traces) {

              // only apply to leading population in the branch
              PopulationID popID = branch.getID(trace);
              if (popID != null) {

                if (getIsStopped().get()) {
                  break;
                }
                Population pop = trace.getPopulation(popID);

                if (pop != null) {
                  EventCollection eventCollection = pop.getEvents();
                  eventCollections.add(eventCollection);
                }
              } else {
                break;
              }
            }

            // We extracted the EventCollections from the Traces

            if (!eventCollections.isEmpty()) {

              List<Event> mergedEvents;

              if (alignAlgorithm.equals(AlignAlgorithm.REGION_COVERAGE)) {
                mergedEvents = EventAligner.mergeByCoverage(eventCollections, 1);
              } else {
                mergedEvents = EventAligner.mergeByConnection(eventCollections);
              }

              if (!mergedEvents.isEmpty()) {

                // put that into each trace as new population
                for (Trace trace : traces) {
                  counter++;

                  contributingMZs.add(trace.getMzValue());

                  // only apply to leading population in the branch
                  PopulationID oldPopID = branch.getID(trace);
                  Population oldPop = trace.getPopulation(oldPopID);

                  Baseline bln = trace.getBaseline();
                  if (bln.hasBaseline() && oldPopID != null && oldPop != null) {

                    MainEventCollection coll = new MainEventCollection(trace);
                    double[] yData = trace.getTISeries().getIntensity();

                    for (Event mergedEvent : mergedEvents) {
                      Event copy = new NpEvent(coll, mergedEvent.getIndicesList());
                      copy.calcPeakIndex(yData);
                      coll.add(copy);
                    }
                    if (getIsStopped().get()) {
                      break;
                    }


                    // Net signal
                    if (netSignalChoice.equals(NetCorrectionOption.INDEX_APPROXIMATION)) {
                      double[] intensities = trace.getTISeries().getIntensity();

                      int windowWidth = (int) Math.ceil(netSignalTime / trace.getTISeries().getDT());

                      // only average the background (i.e., what is not an event)
                      List<Integer> bgIndices = coll.getBackgroundIndices_v2();

                      // likely faster to use array than list
                      int[] bgIndicesArr = ArrUtils.integerListToArr(bgIndices);
                      bgIndices = null; // offer for GC

                      for (Event event : coll.getNpEvents()) {

                        if (getIsStopped().get()) {
                          break;
                        }

                        int peakIdx = event.getPeak();
                        int startIdx = Math.max(0, peakIdx - windowWidth);
                        int endIdx = Math.min(peakIdx + windowWidth, trace.getTISeries().size() - 1);

                        // index handling checked with chatGPT
                        // Find the first index > start
                        int fromIdx = Arrays.binarySearch(bgIndicesArr, startIdx + 1);
                        if (fromIdx < 0) {
                          fromIdx = -(fromIdx + 1);
                        }

                        // Find the last index <= stop
                        int toIdx = Arrays.binarySearch(bgIndicesArr, endIdx);
                        if (toIdx < 0) {
                          toIdx = -(toIdx + 1) - 1;
                        }

                        if (netSignalLocation.equals(MeasureOfLocation.MEDIAN)) {
                          double[] region = new double[toIdx - fromIdx + 1];
                          // chatGPT revealed this double increment which is awesome!
                          for (int i = fromIdx, j = 0; i <= toIdx; i++, j++) {
                            int bgIdx = bgIndicesArr[i];
                            region[j] = intensities[bgIdx];
                          }
                          double medianBG = Median.median(region);
                          double bgPerNP = medianBG * event.getNoOfPoints();
                          if (suppressNegativeNetValues) {
                            double grossArea = event.get(EventParameter.AREA);
                            // largest BG per NP is the grossArea --> then netArea = 0.
                            bgPerNP = Math.min(grossArea, bgPerNP);
                          }
                          event.setBgPerNP(bgPerNP);
                        } else {
                          // mean is the default case
                          double bgSum = 0;
                          for (int i = fromIdx; i <= toIdx; i++) {
                            int bgIdx = bgIndicesArr[i];
                            bgSum += intensities[bgIdx];
                          }
                          double bgPerNP = bgSum / (toIdx - fromIdx + 1) * event.getNoOfPoints();
                          if (suppressNegativeNetValues) {
                            double grossArea = event.get(EventParameter.AREA);
                            // largest BG per NP is the grossArea --> then netArea = 0.
                            bgPerNP = Math.min(grossArea, bgPerNP);
                          }
                          event.setBgPerNP(bgPerNP);
                        }

                      }
                    } else if (netSignalChoice.equals(NetCorrectionOption.TIME_EXACT)) {

                      double[] intensities = trace.getTISeries().getIntensity();
                      double[] time = trace.getTISeries().getTime();

                      // only average the background (i.e., what is not an event)
                      List<Integer> bgIndices = coll.getBackgroundIndices_v2();
                      double[] bgTimes = new double[bgIndices.size()];
                      for (int i = 0; i < bgIndices.size(); i++) {
                        bgTimes[i] = time[bgIndices.get(i)];
                      }

                      for (Event event : coll.getNpEvents()) {

                        if (getIsStopped().get()) {
                          break;
                        }

                        int peakIdx = event.getPeak();
                        double peakTime = time[peakIdx];
                        double startTime = Math.max(time[0], peakTime - netSignalTime);
                        double endTime = Math.min(peakTime + netSignalTime, time[time.length - 1]);

                        // index handling checked with chatGPT
                        // Find the index of the first value > start
                        int fromIdx = Arrays.binarySearch(bgTimes, startTime);
                        if (fromIdx >= 0) {
                          fromIdx++; // exact match found; move to next greater
                        } else {
                          fromIdx = -(fromIdx + 1); // insertion point of next greater
                        }

                        // Find the index of the last value <= stop
                        int toIdx = Arrays.binarySearch(bgTimes, endTime);
                        if (toIdx < 0) {
                          toIdx = -(toIdx + 1) - 1;
                        }

                        if (netSignalLocation.equals(MeasureOfLocation.MEDIAN)) {
                          double[] region = new double[toIdx - fromIdx + 1];
                          // chatGPT revealed this double increment which is awesome!
                          for (int i = fromIdx, j = 0; i <= toIdx; i++, j++) {
                            int bgIdx = bgIndices.get(i);
                            region[j] = intensities[bgIdx];
                          }
                          double medianBG = Median.median(region);
                          double bgPerNP = medianBG * event.getNoOfPoints();
                          if (suppressNegativeNetValues) {
                            double grossArea = event.get(EventParameter.AREA);
                            // largest BG per NP is the grossArea --> then netArea = 0.
                            bgPerNP = Math.min(grossArea, bgPerNP);
                          }
                          event.setBgPerNP(bgPerNP);
                        } else {
                          // mean is the default case
                          double bgSum = 0;
                          for (int i = fromIdx; i <= toIdx; i++) {
                            int bgIdx = bgIndices.get(i);
                            bgSum += intensities[bgIdx];
                          }
                          double bgPerNP = bgSum / (toIdx - fromIdx + 1) * event.getNoOfPoints();
                          if (suppressNegativeNetValues) {
                            double grossArea = event.get(EventParameter.AREA);
                            // largest BG per NP is the grossArea --> then netArea = 0.
                            bgPerNP = Math.min(grossArea, bgPerNP);
                          }
                          event.setBgPerNP(bgPerNP);
                        }
                      }
                    } else {

                      // Get a threshold supplier set to "Mean" which retrieves the bln mean
                      ThresholdSupplierInstructions instr = new ThresholdSupplierInstructions(
                          "Location for background subtraction",
                          ThrFormalism.AT_LOCATION,
                          ThrMeasureOfSignificance.FACTOR,
                          // sig is ignored internally for mean/location
                          0,
                          0
                      );

                      ThresholdSupplier supplier = instr.get(bln.getBackgroundDistribution());

                      for (Event event : coll.getNpEvents()) {
                        int peakIdx = event.getPeak();
                        double bgPerDT = supplier.interpolateProtected(peakIdx, trace.getTISeries().size());

                        double bgPerNP = bgPerDT * event.getNoOfPoints();
                        if (suppressNegativeNetValues) {
                          double grossArea = event.get(EventParameter.AREA);
                          // largest BG per NP is the grossArea --> then netArea = 0.
                          bgPerNP = Math.min(grossArea, bgPerNP);
                        }
                        event.setBgPerNP(bgPerNP);
                      }
                    }


                    // add the resulting collection as new leading pop
                    // (1) Remove existing population from the Trace to avoid pile up of Populations)
                    trace.removePopulation(oldPopID);
                    // (2) Update the ID with the latest step
                    PopulationID idCopy = new PopulationID(oldPopID);
                    idCopy.append(new PopulationStep.AlignSubtype());
                    // (3) Add the new Population to the trace

                    // we want the new population to also define the BG
                    trace.addOverridePopulation(idCopy,
                        new NpPopulation(
                            idCopy,
                            oldPop,
                            coll,
                            idCopy.toString(),
                            contributingMZs),
                        false);
                    // (4) Updating the branch:
                    // Make this new head of branch! Else, OG ID in HashMap is altered, affecting bucketing
                    // in buggy way
                    branch.overrideID(trace, idCopy);
                  }
                }
              }
            }
            setProgress(counter / traces.size());
          }
          setProgress(1);
          break mainLoop;
        }
      }


    } catch (
        Exception e) {
      LOGGER.error("{}. Stack trace: {}", e.getMessage(), ExceptionUtils.getStackTrace(e));
    }

    setProgress(1);
    return taskResult;
  }


}
