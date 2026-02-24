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
import analysis.PopulationStep.MultiEventSubtype;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.TISeries;
import dataModelNew.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import math.stat.Median;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.*;
import processing.parameterSets.bundle.RoiSigFactorBundle;
import processing.parameterSets.bundle.RoiStartStopBundle;
import processing.parameterSets.impl.FilterParams;
import sandbox.montecarlo.Isotope;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;

public class FilterTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(FilterTask.class);

  private final PopulationBranch branch;
  private final FilterParams params;
  private final AtomicReference<Sample> sampleRef;
  private final PopParSummary inputPopSummary;

  public FilterTask(String taskName, PopulationBranch branch, FilterParams params,
                    AtomicReference<Sample> sampleRef) {
    super(taskName);
    this.branch = branch;
    this.params = (FilterParams) params.getCopyWithPreviousDateFileAndID();
    this.sampleRef = sampleRef;
    //
    this.inputPopSummary = new PopParSummary(params);
  }

  @Override
  public TaskResult call() {
    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new EmptyTaskResult();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null && params.getEnableBoolean().getValue()) {
        LOGGER.info("Filtering starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          Sample sample = sampleRef.get();
          if (sample instanceof SampleImpl) {

            List<Trace> traces = sample.getTraces();
            for (Trace trace : traces) {

              PopulationID popID = branch.getID(trace);
              if (popID != null && trace.hasType(popID) && trace.getPopulation(popID) != null
                  && trace.getPopulation(popID).getEvents().size() > 0) {

                if (getIsStopped().get()) {
                  break;
                }

                FilterOptions filterOption = params.getFilterOption().getValue();

                ////////////////////////////////////////////////////////////////////////////////
                //////////////////// OVERLAP ///////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////

                if (filterOption.equals(FilterOptions.OVERLAP)) {
                  double zIntensity = params.getzIntensity().getValue();
                  double zSegment = params.getzSegment().getValue();
                  int moavPeriod = params.getMoavPeriod().getValue();
                  OverlapSmoothOption smoothTarget = params.getSmoothTargetOption().getValue();

                  TISeries tiSeries = trace.getTISeries();
                  double[] intensity = tiSeries.getIntensity();

                  Population oldPop = trace.getPopulation(popID);

                  if (oldPop != null) {
                    HashMap<Integer, List<Event>> multiEventsMap = new HashMap<>();

                    for (Event event : oldPop.getEvents().getNpEvents()) {

                      if (getIsStopped().get()) {
                        break;
                      }

                      int nEvents;
                      if (smoothTarget.equals(OverlapSmoothOption.RAW_DATA)) {
                        nEvents = EventFilterUtils.checkForMultiParticleEvent_rawSmooth(
                            intensity,
                            event,
                            zIntensity,
                            zSegment,
                            moavPeriod);
                      } else {
                        nEvents = EventFilterUtils.checkForMultiParticleEvent_derivativeSmooth(
                            intensity,
                            event,
                            zIntensity,
                            zSegment,
                            moavPeriod);
                      }

                      // If it thinks its 0, likely should be overwritten with 1, i.e., normal peak.
                      nEvents = Math.max(nEvents, 1);

                      if (multiEventsMap.containsKey(nEvents)) {
                        multiEventsMap.get(nEvents).add(event);
                      } else {
                        List<Event> eventsWithN = new ArrayList<>();
                        eventsWithN.add(event);
                        multiEventsMap.put(nEvents, eventsWithN);
                      }
                    }

                    if (multiEventsMap.containsKey(1)) {
                      // (1) Keep  existing population to compare filter result with idle state
                      // trace.removePopulation(popID);
                      // (2) Update as new ID with the latest step; prepare copy here before altering popID
                      PopulationID idCopy = new PopulationID(popID);
                      // (2) append existing ID as new main of branch
                      idCopy.append(new MultiEventSubtype(
                          params.getFilterOption().getValue().toString(),
                          "1"));
                      // (3) Add the new Population to the trace: SubEventColl keeps BG from MainEventColl.
                      trace.addOverridePopulation(idCopy,
                          new NpPopulation(
                              idCopy,
                              oldPop,
                              new SubEventCollection(trace, multiEventsMap.get(1), oldPop),
                              idCopy.toString(),
                              inputPopSummary));

                      // Make this new head of branch! Else, OG ID in HashMap is altered, affecting
                      // bucketing in buggy way
                      branch.overrideID(trace, idCopy);
                    }

                    List<Integer> multiList = new ArrayList<>(multiEventsMap.keySet());
                    multiList.sort(Integer::compareTo);

                    if (params.getListNumberOfEvents().getValue()) {
                      for (Integer nEvents : multiList) {
                        if (nEvents > 1) {
                          // (2) Update as new ID with the latest step
                          PopulationID idCopyTemp = new PopulationID(popID);
                          idCopyTemp.append(new MultiEventSubtype(
                              params.getFilterOption().getValue().toString(),
                              nEvents.toString()));
                          // (3) Add the new Population to the trace: SubEventColl keeps BG from
                          // MainEventColl.
                          trace.addOverridePopulation(idCopyTemp,
                              new NpPopulation(
                                  idCopyTemp,
                                  oldPop,
                                  new SubEventCollection(trace, multiEventsMap.get(nEvents), oldPop),
                                  idCopyTemp.toString(),
                                  inputPopSummary));
                          // (4) Updating the branch: when ID was not just "appended"
                          // branch.overrideID(trace, );
                        }
                      }
                    } else {
                      List<Event> allGreaterThanOne = new ArrayList<>();
                      for (Integer nEvents : multiList) {
                        if (nEvents > 1) {
                          allGreaterThanOne.addAll(multiEventsMap.get(nEvents));
                        }
                      }
                      // (2) Update as new ID with the latest step (copy is prepared above)
                      PopulationID idCopy = new PopulationID(popID);
                      idCopy.append(new MultiEventSubtype(
                          params.getFilterOption().getValue().toString(),
                          ">1"));
                      // (3) Add the new Population to the trace: SubEventColl keeps BG from MainEventColl.
                      trace.addOverridePopulation(idCopy,
                          new NpPopulation(
                              idCopy,
                              oldPop,
                              new SubEventCollection(trace, allGreaterThanOne, oldPop),
                              idCopy.toString(),
                              inputPopSummary));
                      // (4) Updating the branch: when ID was not just "appended"
                      // branch.overrideID(trace, );
                    }
                  }

                  ////////////////////////////////////////////////////////////////////////////////
                  //////////////////// OVER RANGE/////////////////////////////////////////////////
                  ////////////////////////////////////////////////////////////////////////////////
                } else if (filterOption.equals(FilterOptions.OVER_RANGE)) {

                  Population oldPop = trace.getPopulation(popID);

                  if (oldPop != null) {

                    List<Event> eventsToKeep = new ArrayList<>();
                    List<Event> orEventToRemove = new ArrayList<>();
                    HashSet<Integer> overRangeIndices = trace.getFlags(DataFlag.OVER_RANGE);

                    if (overRangeIndices != null && overRangeIndices.isEmpty()) {
                      // just add all to the keep list
                      eventsToKeep.addAll(oldPop.getEvents().getNpEvents());
                    } else {
                      // check which are over-range
                      for (Event event : oldPop.getEvents().getNpEvents()) {

                        // check breaking condition
                        if (getIsStopped().get()) {
                          break;
                        }

                        boolean isOverRange = false;
                        int[] eventIndices = event.getIndices();
                        for (int eventIndex : eventIndices) {
                          if (overRangeIndices.contains(eventIndex)) {
                            isOverRange = true;
                            break;
                          }
                        }
                        // indices have been checked: assign events
                        if (isOverRange) {
                          orEventToRemove.add(event);
                        } else {
                          eventsToKeep.add(event);
                        }
                      }
                    }

                    // (1) Remove existing population from the Trace to avoid pile up of Populations)
                    trace.removePopulation(popID);
                    // (2) Update as new ID with the latest step; prepare copy here before altering popID
                    PopulationID idCopy = new PopulationID(popID);
                    // (2) append existing ID as new main of branch
                    idCopy.append(new PopulationStep.OverRangeSubtype(
                        params.getFilterOption().getValue().toString(), "InRng"));
                    // (3) Add the new Population to the trace
                    trace.addOverridePopulation(idCopy,
                        new NpPopulation(
                            idCopy,
                            oldPop,
                            new SubEventCollection(trace, eventsToKeep, oldPop),
                            idCopy.toString(),
                            inputPopSummary));
                    // (4) Make this new head of branch!
                    branch.overrideID(trace, idCopy);


                    if (!orEventToRemove.isEmpty()) {
                      // (1) Existing population was already removed.
                      // trace.removePopulation(popID);
                      // (2) Update as new ID with the latest step
                      idCopy = new PopulationID(popID);
                      idCopy.append(new PopulationStep.OverRangeSubtype(
                          params.getFilterOption().getValue().toString(), "OvRng"));
                      // (3) Add the new Population to the trace
                      trace.addOverridePopulation(idCopy,
                          new NpPopulation(
                              idCopy,
                              oldPop,
                              new SubEventCollection(trace, orEventToRemove, oldPop),
                              idCopy.toString(),
                              inputPopSummary));

                    }
                  }
                  ////////////////////////////////////////////////////////////////////////////////
                  //////////////////// ROI ///////////////////////////////////////////////////////
                  ////////////////////////////////////////////////////////////////////////////////
                } else if (filterOption.equals(FilterOptions.ROI_REGION)) {
                  Population oldPop = trace.getPopulation(popID);
                  if (oldPop != null) {

                    // check for isotope-specific limits
                    Isotope isotope = trace.getMzValue().getIsotope();

                    // extract params
                    RoiCategory roiCategory = params.roiCategory.getValue();
                    EventParameter eventParameter = params.eventParameter.getValue();
                    MathMod mathConversion = params.mathConversion.getValue();
                    RoiType roiType = params.roiType.getValue();
                    Double startPar = params.start.getValue();
                    Double endPar = params.end.getValue();
                    Double sigFactor = params.sigFactor.getValue();

                    // Check for exceptions
                    List<RoiStartStopBundle> startStopExceptions = params.getRoiStartStopBundles();
                    List<RoiSigFactorBundle> sigFactorExceptions = params.getRoiSigFactorBundles();

                    for (RoiStartStopBundle startStopException : startStopExceptions) {
                      if (startStopException.isotopeHeaderParameter.getValue().unwrap().equals(isotope)) {
                        startPar = startStopException.start.getValue();
                        endPar = startStopException.end.getValue();
                        break; // break at first match
                      }
                    }

                    for (RoiSigFactorBundle sigException : sigFactorExceptions) {
                      if (sigException.isotopeHeaderParameter.getValue().unwrap().equals(isotope)) {
                        sigFactor = sigException.sigFactor.getValue();
                        break; // break at first match
                      }
                    }

                    List<Event> eventsToKeep = new ArrayList<>();

                    if (roiCategory.equals(RoiCategory.ABSOLUTE_VALUES)) {
                      startPar = mathConversion.invert(startPar);
                      endPar = mathConversion.invert(endPar);
                      if (startPar < endPar) {
                        // check which are in ROI
                        for (Event event : oldPop.getEvents().getNpEvents()) {
                          // check breaking condition
                          if (getIsStopped().get()) {
                            break;
                          }
                          double eventVal = event.get(eventParameter);
                          if (roiType.valid(startPar, endPar, eventVal)) {
                            eventsToKeep.add(event);
                          }
                        }
                      } else {
                        LOGGER.info("Cannot create ROI. start > end: " + startPar + " > " + endPar);
                      }
                    } else if (roiCategory.equals(RoiCategory.PERCENTILES)) {
                      // now start and stop should be percentiles.
                      startPar = Math.max(startPar, Double.MIN_VALUE);
                      startPar = Math.min(100, startPar);
                      endPar = Math.max(endPar, Double.MIN_VALUE);
                      endPar = Math.min(100, endPar);
                      if (startPar < endPar) {
                        Percentile percentile = new Percentile();
                        // percentile only uses order, which does not changer with math mod!
                        double[] data = oldPop.getEvents().get(EventType.NP, eventParameter);
                        double lowerLim = percentile.evaluate(data, startPar);
                        double upperLim = percentile.evaluate(data, endPar);
                        if (lowerLim < upperLim) {
                          // check which are in ROI
                          for (Event event : oldPop.getEvents().getNpEvents()) {
                            // check breaking condition
                            if (getIsStopped().get()) {
                              break;
                            }
                            double eventVal = event.get(eventParameter);
                            if (roiType.valid(lowerLim, upperLim, eventVal)) {
                              eventsToKeep.add(event);
                            }
                          }
                        } else {
                          LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerLim + " " +
                              "> " + upperLim);
                        }
                      } else {
                        LOGGER.info("Cannot create ROI. start > end: " + startPar + " > " + endPar
                            + ". Start and stop are given as percentiles and have been clamped to 0-100%.");
                      }
                    } else if (roiCategory.equals(RoiCategory.IQR)) {
                      // sig factor should be nonzero
                      if (sigFactor < 0) {
                        LOGGER.info("Factor must be greater than zero! Current factor: " + sigFactor + ".");
                      }
                      sigFactor = Math.max(sigFactor, Double.MIN_VALUE);
                      Percentile percentile = new Percentile();
                      // percentile only uses order, which does not changer with math mod, but distance +1
                      // .5IQR scales differently on log scale!
                      double[] data = oldPop.getEvents().get(EventType.NP, eventParameter);
                      data = mathConversion.calc(data);
                      double q1 = percentile.evaluate(data, 25); // Q1
                      double q3 = percentile.evaluate(data, 75); // Q3
                      double iqr = q3 - q1;
                      double lowerLim = q1 - sigFactor * iqr;
                      double upperLim = q3 + sigFactor * iqr;

                      if (iqr > 0 && lowerLim < upperLim) {
                        // check which are in ROI
                        for (Event event : oldPop.getEvents().getNpEvents()) {
                          // check breaking condition
                          if (getIsStopped().get()) {
                            break;
                          }
                          double eventVal = event.get(eventParameter);
                          eventVal = mathConversion.calc(eventVal);
                          if (roiType.valid(lowerLim, upperLim, eventVal)) {
                            eventsToKeep.add(event);
                          }
                        }
                      } else {
                        LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerLim + " > "
                            + upperLim);
                      }
                    } else if (roiCategory.equals(RoiCategory.MAD)) {
                      // sig factor should be nonzero
                      if (sigFactor < 0) {
                        LOGGER.info("Factor must be greater than zero! Current factor: " + sigFactor + ".");
                      }
                      sigFactor = Math.max(sigFactor, Double.MIN_VALUE);
                      // percentile only uses order, which does not changer with math mod, but distance +1
                      // .5MAD scales differently on log scale!
                      double[] data = oldPop.getEvents().get(EventType.NP, eventParameter);
                      data = mathConversion.calc(data);
                      double median = Median.median(data);
                      double mad = Median.mad(data);
                      double lowerLim = median - sigFactor / 2 * mad;
                      double upperLim = median + sigFactor / 2 * mad;

                      if (lowerLim < upperLim) {
                        // check which are in ROI
                        for (Event event : oldPop.getEvents().getNpEvents()) {
                          // check breaking condition
                          if (getIsStopped().get()) {
                            break;
                          }
                          double eventVal = event.get(eventParameter);
                          eventVal = mathConversion.calc(eventVal);
                          if (roiType.valid(lowerLim, upperLim, eventVal)) {
                            eventsToKeep.add(event);
                          }
                        }
                      } else {
                        LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerLim + " > "
                            + upperLim);
                      }
                    }


                    // (1) Remove existing population from the Trace to avoid pile up of Populations)
                    // trace.removePopulation(popID);
                    // (2) Update as new ID with the ROI;
                    PopulationID idCopy = new PopulationID(popID);
                    // (2) append existing ID as new main of branch
                    idCopy.append(new PopulationStep.RoiSubtype(params.getRoiID().getValue()));
                    // (3) Add the new Population to the trace
                    trace.addOverridePopulation(idCopy,
                        new NpPopulation(
                            idCopy,
                            oldPop,
                            new SubEventCollection(trace, eventsToKeep, oldPop),
                            idCopy.toString(),
                            inputPopSummary));
                    // (4) Updating the branch: not necessary as it still has the pointer and ID was only
                    // "appended"


                  }

                  ////////////////////////////////////////////////////////////////////////////////
                  //////////////////// MATCH ///////////////////////////////////////////////////////
                  ////////////////////////////////////////////////////////////////////////////////
                  // do not match syn with syn -> check here and skip otherwise
                } else if (filterOption.equals(FilterOptions.MATCH_SIM)
                    && !popID.getType().equals(PopulationType.SIMULATION)) {

                  boolean setToZero = params.getSuppressNegativeValues().getValue();
                  boolean removeZeros = params.getRemoveNegativeValues().getValue();

                  boolean listMatches = params.getListMatches().getValue();
                  boolean listFalsePositives = params.getListFalsePositives().getValue();
                  boolean listFalseNegatives = params.getListFalseNegatives().getValue();

                  // find populations
                  Population oldPop = trace.getPopulation(popID);
                  List<Population> synPops = trace.getAllPopulations().stream()
                      .filter(p -> p.getId().getType().equals(PopulationType.SIMULATION))
                      .collect(Collectors.toList());

                  if (oldPop != null && !synPops.isEmpty()) {

                    // extract the simulated population
                    Population synPop = synPops.get(0);

                    List<Event> evalEvts = oldPop.getEvents().getNpEvents();
                    List<Event> simEvts = synPop.getEvents().getNpEvents();

                    List<Event> matchFromSim = new ArrayList<>();
                    List<Event> matchFromEval = new ArrayList<>();
                    List<Event> noMatchSim = new ArrayList<>();
                    List<Event> noMatchEval = new ArrayList<>();

                    AlphaBetaEvaluation.checkEvents(simEvts, evalEvts, matchFromSim, matchFromEval,
                        noMatchSim,
                        noMatchEval);

                    if (listMatches) {
                      // (2) Update as new ID with the latest step
                      PopulationID idCopyTemp = new PopulationID(popID);
                      idCopyTemp.append(new PopulationStep.SimMatchSubtype(
                          params.getFilterOption().getValue().toString(),
                          "Matched"));
                      // (3) Add the new Population to the trace: SubEventColl keeps BG from
                      // MainEventColl.
                      trace.addOverridePopulation(idCopyTemp,
                          new NpPopulation(
                              idCopyTemp,
                              oldPop,
                              new SubEventCollection(trace, matchFromEval, oldPop),
                              idCopyTemp.toString(),
                              inputPopSummary));
                      // (4) Updating the branch: when ID was not just "appended"
                      // branch.overrideID(trace, );
                    }


                    if (listFalsePositives) {
                      // (2) Update as new ID with the latest step
                      PopulationID idCopyTemp = new PopulationID(popID);
                      idCopyTemp.append(new PopulationStep.SimMatchSubtype(
                          params.getFilterOption().getValue().toString(),
                          "FPos"));
                      // (3) Add the new Population to the trace: SubEventColl keeps BG from
                      // MainEventColl.
                      trace.addOverridePopulation(idCopyTemp,
                          new NpPopulation(
                              idCopyTemp,
                              oldPop,
                              new SubEventCollection(trace, noMatchEval, oldPop),
                              idCopyTemp.toString(),
                              inputPopSummary));
                      // (4) Updating the branch: when ID was not just "appended"
                      // branch.overrideID(trace, );
                    }

                    if (listFalseNegatives) {
                      // (2) Update as new ID with the latest step
                      PopulationID idCopyTemp = new PopulationID(popID);
                      idCopyTemp.append(new PopulationStep.SimMatchSubtype(
                          params.getFilterOption().getValue().toString(),
                          "Fneg"));
                      // (3) Add the new Population to the trace: SubEventColl keeps BG from
                      // MainEventColl.
                      Population falseNegatives = new NpPopulation(
                          idCopyTemp,
                          oldPop,
                          new SubEventCollection(trace, noMatchSim, oldPop),
                          idCopyTemp.toString(),
                          inputPopSummary);

                      // After adding to new population to its (sub)collection:
                      // these events are defined from theory point of view with >1cts, hence adjust area
                      // correction; do this after creating the population
                      // as creating the pop sets the Collection and Trace
                      // Note: BG is estimated as <event.setBgPerNP(trace.getEmpiricalMeanBG() * event
                      // .getNoOfPoints());>
                      if (setToZero) {
                        List<Event> falseNegCopy = new ArrayList<>(falseNegatives.getEvents().getNpEvents());
                        for (Event falseNegEvt : falseNegCopy) {
                          double assumedBG = falseNegEvt.getBgPerNP();
                          double area = falseNegEvt.get(EventParameter.AREA);
                          // make sure, lowest possible is zero
                          if (assumedBG > area) {
                            falseNegEvt.setBgPerNP(area);
                          }
                        }

                        if (removeZeros) {
                          falseNegCopy.removeIf(e -> e.get(EventParameter.NET_AREA) <= 0);
                        }

                        falseNegatives = new NpPopulation(
                            idCopyTemp,
                            oldPop,
                            new SubEventCollection(trace, falseNegCopy, oldPop),
                            idCopyTemp.toString(),
                            inputPopSummary);
                      }

                      trace.addOverridePopulation(idCopyTemp, falseNegatives);
                      // (4) Updating the branch: when ID was not just "appended"
                      // branch.overrideID(trace, );
                    }
                  } // end
                }

              }

              setProgress(counter / traces.size());
            }
          }
          setProgress(1);
          break mainLoop;
        }
      }


    } catch (
        Exception e) {
      LOGGER.error(e.getMessage() + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }

    return taskResult;
  }


}
