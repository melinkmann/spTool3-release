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
import dataModelNew.mz.Channel;
import dataModelNew.mz.StaticChannelCategory;
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
import processing.parameterSets.impl.NormalSearchParams;
import sandbox.montecarlo.Isotope;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;
import util.ArrUtils;
import util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class ComputeBgPerNpTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(ComputeBgPerNpTask.class);

  private final PopulationBranch branch;
  private final NormalSearchParams params;
  private final AtomicReference<Sample> sampleRef;

  public ComputeBgPerNpTask(String taskName, PopulationBranch branch, NormalSearchParams params,
                            AtomicReference<Sample> sampleRef) {
    super(taskName);
    this.branch = branch;
    this.params = (NormalSearchParams) params.getCopyWithPreviousDateFileAndID();
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

        LOGGER.info("Background calculation starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread().getId());

        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          NetCorrectionOption netSignalChoice = params.getNetCorrectionOption().getValue();
          MeasureOfLocation netSignalLocation = params.getNetTimeWindowLocation().getValue();
          boolean suppressNegativeNetValues = params.getSuppressNegativeValues().getValue();
          double netSignalTime = params.getNetTimeWindow().getValue();

          Sample sample = sampleRef.get();
          if (sample instanceof SampleImpl) {

            List<Trace> traces = sample.getTraces();
            for (Trace trace : traces) {

              Baseline bln = trace.getBaseline();

              // only apply to leading population in the branch
              PopulationID popID = branch.getID(trace);
              if (popID != null && bln != null && bln.hasBaseline()) {

                if (getIsStopped().get()) {
                  break;
                }

                Population pop = trace.getPopulation(popID);
                if (pop != null) {
                  EventCollection eventCollection = pop.getEvents();
                  List<Event> events = eventCollection.getNpEvents();

                  // COMPUTATION:
                  // Net signal
                  if (netSignalChoice.equals(NetCorrectionOption.INDEX_APPROXIMATION)) {
                    double[] intensities = trace.getTISeries().getIntensity();

                    int windowWidth = (int) Math.ceil(netSignalTime / trace.getTISeries().getDT());

                    // only average the background (i.e., what is not an event)
                    List<Integer> bgIndices = eventCollection.getBackgroundIndices_v2();

                    // likely faster to use array than list
                    int[] bgIndicesArr = ArrUtils.integerListToArr(bgIndices);
                    bgIndices = null; // offer for GC

                    for (Event event : events) {

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

                      // guard against too small windows
                      int count = toIdx - fromIdx + 1;
                      if (count <= 0) {
                        // Window contains no background indices: fallback:
                        event.setBgPerNP(0.0);
                        LOGGER.error("No BG indices available in the window around the event. Event is " +
                            "too wide! Use baseline for net correction instead or increase the window.");
                        continue;
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
                    List<Integer> bgIndices = eventCollection.getBackgroundIndices_v2();
                    double[] bgTimes = new double[bgIndices.size()];
                    for (int i = 0; i < bgIndices.size(); i++) {
                      bgTimes[i] = time[bgIndices.get(i)];
                    }

                    for (Event event : events) {

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

                      // guard against too small windows
                      int count = toIdx - fromIdx + 1;
                      if (count <= 0) {
                        // Window contains no background indices: fallback:
                        event.setBgPerNP(0.0);
                        LOGGER.error("No BG indices available in the window around the event. Event is " +
                            "too wide! Use baseline for net correction instead or increase the window.");
                        continue;
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

                    for (Event event : events) {
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
                }
              }
              counter++;
              setProgress(counter / traces.size());
            }
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
