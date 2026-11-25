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
import sandbox.montecarlo.Isotope;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;
import util.ArrUtils;
import util.Util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static analysis.EventAligner.mergeByCoverageWithContributors;

public class AlignTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(AlignTask.class);

  private final PopulationBranch branch;
  private final AlignerParams params;
  private final AtomicReference<Sample> sampleRef;
  private final List<Channel> selChannels;

  public AlignTask(String taskName, PopulationBranch branch, AlignerParams params,
                   AtomicReference<Sample> sampleRef, List<Channel> selChannels) {
    super(taskName);
    this.branch = branch;
    this.params = (AlignerParams) params.getCopyWithPreviousDateFileAndID();
    this.sampleRef = sampleRef;
    this.selChannels = new ArrayList<>(selChannels);
  }

  @Override
  public TaskResult call() {
    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new EmptyTaskResult();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null && params.getEnableBoolean().getValue()) {

        LOGGER.info("Align starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread().getId());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          // TODO: Include option: set all <THR to zero.
          //  set the thr as alpha here in the thingy.
          //  idea: for cluster, we do not want BG to contribute.
          //  so we just set it to zero. But the statistics will be tricky
          //  b/c the "falsely assigned" events will scatter around 0
          //  but with spread widened by having 1 - 15 or so data points.

          NetCorrectionOption netSignalChoice = params.getNetCorrectionOption().getValue();
          AlignAlgorithm alignAlgorithm = params.getAlignAlgorithm().getValue();
          IsotopeSelection isotopeSelection = params.getIsotopeSelection().getValue();
          boolean alignIntoAllChannels = params.getAddAlignedPopulationToAllTraces().getValue();
          List<Isotope> excludedIsotopes = params.listExcludedIsotopes();
          List<Isotope> includedIsotopes = params.listIncludedIsotopes();
          List<Double> includedChannelMZ = params.getIncludedChannels();
          double ppmPrecisionChannels = params.getIncludeChannelsPrecision().getValue();
          MeasureOfLocation netSignalLocation = params.getNetTimeWindowLocation().getValue();
          boolean suppressNegativeNetValues = params.getSuppressNegativeValues().getValue();
          double netSignalTime = params.getNetTimeWindow().getValue();
          boolean unilateralMatchToZero = params.getBorrowedEventsToZero().getValue();
          boolean borrowingViaCriticalLimit = params.getBorrowedEventsViaCriticalLimit().getValue();
          double borrowingZFactor = params.getBorrowedEventsCriticalLimitZFactor().getValue();

          Sample sample = sampleRef.get();
          if (sample instanceof SampleImpl) {

            List<Trace> allTraces = sample.getTraces();
            List<Trace> filteredTraces = new ArrayList<>(allTraces);
            List<Trace> targetTraces;
            if (alignIntoAllChannels) {
              targetTraces = new ArrayList<>(allTraces);
            } else {
              targetTraces = filteredTraces;
            }

            // This map stores the matching between each dummy event and the contributin collections
            Map<Event, List<EventCollection>> unilateralMap = new HashMap<>();

            // check if any trace is TIC
            boolean hasTIC = filteredTraces.stream()
                .map(Trace::getChannel)
                .anyMatch(c -> c.getCategory().equals(StaticChannelCategory.TIC));

            // match traces and requested MZs
            Map<Double, Trace> matchMap = Util.matchDoublesToChannels(
                includedChannelMZ,
                filteredTraces,
                ppmPrecisionChannels);

            // enable exclusion of certain isotopes
            switch (isotopeSelection) {
              case ALL_LOADED -> {
              }
              case SELECTED -> {
                filteredTraces.removeIf(t -> !selChannels.contains(t.getChannel()));
              }
              case POSITIVE_LIST_SELECTION -> {
                filteredTraces.removeIf(t -> !includedIsotopes.contains(t.getChannel().getIsotope()));
              }
              case NEGATIVE_LIST_EXCLUSION -> {
                filteredTraces.removeIf(t -> excludedIsotopes.contains(t.getChannel().getIsotope()));
              }

              case TIC -> {
                if (hasTIC) {
                  filteredTraces.removeIf(Predicate.not(t -> t.getChannel().getCategory().equals(StaticChannelCategory.TIC)));
                  // This would prevent TIC from being part of the align but also disable looking at the
                  // picking and histogram of TIC...
                  // targetTraces.removeIf(t -> t.getChannel().getCategory().equals(StaticChannelCategory
                  // .TIC));
                } else {
                  // All loaded
                }

              }

              case POSITIVE_LIST_CHANNEL_SELECTION -> {
                filteredTraces.removeIf(matchMap::containsValue);
              }
              default -> {
                // keep as is, we should not reach this branch
              }
            }

            List<Channel> contributingChannels = new ArrayList<>();

            // Extract event collections of current leading population
            List<EventCollection> eventCollections = new ArrayList<>();

            for (Trace trace : filteredTraces) {

              // complete list first and iterate over traces
              contributingChannels.add(trace.getChannel());

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
                if (unilateralMatchToZero) {
                  unilateralMap = mergeByCoverageWithContributors(eventCollections, 1);
                  mergedEvents = new ArrayList<>(unilateralMap.keySet());
                } else {
                  mergedEvents = EventAligner.mergeByCoverage(eventCollections, 1);
                }
              } else {
                mergedEvents = EventAligner.mergeByConnection(eventCollections);
                unilateralMap = new HashMap<>();
              }

              if (!mergedEvents.isEmpty()) {

                // put that into each trace as new population
                for (Trace trace : targetTraces) {
                  counter++;

                  // only apply to leading population in the branch
                  PopulationID oldPopID = branch.getID(trace);
                  Population oldPop = trace.getPopulation(oldPopID);

                  Baseline bln = trace.getBaseline();
                  if (bln.hasBaseline() && oldPopID != null && oldPop != null) {

                    MainEventCollection coll = new MainEventCollection(trace);
                    double[] yData = trace.getTISeries().getIntensity();

                    // Aligned events that exist only because other traces had an event here
                    List<Event> zeroNetEvents = new ArrayList<>();
                    // This trace's own events (before alignment), and whether it took part in the alignment.
                    // Traces that only receive the result (alignIntoAllChannels) must not be zeroed.
                    EventCollection oldCollection = oldPop.getEvents();
                    // This one should be guaranteed from logic above but check anyway!
                    boolean oldCollWasMerged = eventCollections.contains(oldCollection);

                    for (Event mergedEvent : mergedEvents) {
                      Event copy = new NpEvent(coll, mergedEvent.getIndicesList());
                      copy.calcPeakIndex(yData);
                      coll.add(copy);

                      // Decide whether this aligned event is "borrowed" from other traces.
                      // - contributors = the collections that had a real event of their own inside this
                      // aligned event. It is null when there is no such info (option off, or Contact mode);
                      // then nothing is zeroed.
                      // - collectionWasMerged = this trace/collection took part in the alignment.
                      // Traces that only receive the result (alignIntoAllChannels) are never zeroed.
                      // If the trace took part but is not in contributors, it had no event here itself:
                      // the event exists only because other traces had one. Remember it, its net area is
                      // set to zero later.
                      List<EventCollection> contributingCollections = unilateralMap.get(mergedEvent);
                      if (contributingCollections != null
                          && oldCollWasMerged
                          && !contributingCollections.contains(oldCollection)) {
                        zeroNetEvents.add(copy);
                      }
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

                    // Events that exist only because other traces had an event here:
                    // background = gross area, so net area = 0. This runs after all net-correction
                    // branches, so it overrides whatever they computed.
                    if (unilateralMatchToZero && borrowingViaCriticalLimit) {
                      List<Event> zeroNetEventsBelowTHR = EventAligner.selectBorrowedEventsToZero(
                          zeroNetEvents,
                          5,
                          borrowingZFactor);

                      for (Event event : zeroNetEventsBelowTHR) {
                        event.setBgPerNP(event.get(EventParameter.AREA));
                      }
                    } else if (unilateralMatchToZero) {
                      for (Event event : zeroNetEvents) {
                        event.setBgPerNP(event.get(EventParameter.AREA));
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
                            contributingChannels),
                        false);
                    // (4) Updating the branch:
                    // Make this new head of branch! Else, OG ID in HashMap is altered, affecting bucketing
                    // in buggy way
                    branch.overrideID(trace, idCopy);
                  }
                }
              }
            }
            setProgress(counter / targetTraces.size());
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
