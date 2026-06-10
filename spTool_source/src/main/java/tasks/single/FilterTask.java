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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import dataModelNew.mz.Channel;
import dataModelNew.mz.IsotopeMZ;
import dataModelNew.mz.MZValue;
import math.HistogramFilters;
import math.OtsuThreshold;
import math.stat.Median;
import math.units.Unit;
import math.units.enums.IntensityUnit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.statistics.HistogramDataset;
import processing.options.*;
import processing.parameterSets.bundle.AlignFilterStartStopBundle;
import processing.parameterSets.bundle.RoiSigFactorBundle;
import processing.parameterSets.bundle.RoiStartStopBundle;
import processing.parameterSets.impl.FilterParams;
import sandbox.montecarlo.Isotope;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;
import util.ArrUtils;
import util.NF;
import util.SnF;
import visualizer.charts.JFreeUtil;

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

    FilterOptions filterOption = params.getFilterOption().getValue();
    boolean setAsMainBranch = params.getSetAsMainBranch().getValue();


    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null && params.getEnableBoolean().getValue()) {
        LOGGER.info("Filtering starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread().getId());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          Sample sample = sampleRef.get();
          if (sample instanceof SampleImpl) {

            ////////////////////////////////////////////////////////////////////////////////
            //////////////////// ALIGN ///////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////
            if (filterOption.equals(FilterOptions.ALIGNED_FILTER)) {
              List<AlignFilterStartStopBundle> alignBundles = params.getAlignFilterStartStopBundles();

              String label = params.getAlignFilterID().getValue();

              // 1. Identify aligned isotopes and their popIDs
              // A sample could have two branches, so we collect all aligned population IDs
              // and the isotopes that belong to each.
              HashMap<PopulationID, List<Channel>> alignedIDs = new HashMap<>();

              for (Trace trace : sample.getTraces()) {
                PopulationID popID = branch.getID(trace);
                if (AnalysisUtils.isAlignedOrPVal(popID)) {
                  alignedIDs.computeIfAbsent(popID, k -> new ArrayList<>())
                      .add(trace.getChannel());
                }
              }

              // 2. For each aligned population, apply the intensity filter
              for (PopulationID popIDKey : alignedIDs.keySet()) {
                List<Channel> alignedChannels = alignedIDs.get(popIDKey);

                // Find which of the user-specified limits apply to this population
                // (only restrictions whose isotope is actually present in this population)
                HashMap<Channel, AlignFilterStartStopBundle> restrictedChannels = new HashMap<>();
                for (AlignFilterStartStopBundle alignBundle : alignBundles) {
                  Isotope restrictedIsotope = alignBundle.isotopeHeaderParameter.getValue().unwrap();
                  Channel restrictedChannel = AnalysisUtils.getChannel(alignedChannels, restrictedIsotope);
                  if (restrictedChannel != null) {
                    restrictedChannels.put(restrictedChannel, alignBundle);
                  }
                }

                // Are there any restrictions to filter?
                if (!restrictedChannels.isEmpty()) {

                  // Double check that all of these populations have the same size!
                  long distinctSizes = restrictedChannels.keySet().stream()
                      .map(sample::getTrace)
                      .filter(Objects::nonNull)
                      .map(trace -> trace.getPopulation(popIDKey).getEvents().size())
                      .distinct()
                      .count();
                  boolean allEqualSize = distinctSizes <= 1;

                  if (allEqualSize) {

                    // 3. Build a boolean mask of surviving particles
                    // Start with all particles passing — AND each restriction in turn.
                    // validIndexPositions[p] = true means particle p survives all restrictions.
                    boolean[] validIndexPositions = null;

                    for (Channel channel : restrictedChannels.keySet()) {
                      AlignFilterStartStopBundle alignBundle = restrictedChannels.get(channel);
                      double lower = alignBundle.start.getValue();
                      double upper = alignBundle.end.getValue();
                      EventParameter eventParameter = params.getEventParameter().getValue();
                      Unit unit = params.getUnitConversion();
                      MathMod math = params.getMathConversion().getValue();

                      double[] data = sample.getData(channel, popIDKey, EventType.NP, eventParameter, unit);
                      data = math.calc(data);

                      // ensure data exists
                      if (data != null && data.length != 0) {

                        // Initialise mask on first isotope as TRUE
                        if (validIndexPositions == null) {
                          validIndexPositions = new boolean[data.length];
                          Arrays.fill(validIndexPositions, true);
                        }

                        // Safety check: all isotopes must have the same particle count
                        // (we checked above, so the loop should never trigger if unequal)
                        if (data.length == validIndexPositions.length) {

                          // AND condition: particle must be within [lower, upper] for this isotope
                          for (int p = 0; p < data.length; p++) {
                            if (data[p] < lower || data[p] > upper) {
                              validIndexPositions[p] = false;
                            }
                          }

                        } else {
                          LOGGER.warn("Isotope {} has {} particles but expected {}! Skipping filtering.",
                              channel, data.length, validIndexPositions.length);
                        }
                      } else {
                        LOGGER.warn("No data for isotope {} in population {}! Skipping filtering.",
                            channel, popIDKey);
                      }
                    }

                    // Ensure there is data to filter, i.e., all data was not null or empty
                    if (validIndexPositions != null) {

                      // Collect surviving particle indices
                      List<Integer> survivingIndices = new ArrayList<>();
                      for (int p = 0; p < validIndexPositions.length; p++) {
                        if (validIndexPositions[p]) {
                          survivingIndices.add(p);
                        }
                      }

                      LOGGER.info("Aligned filter: {} of {} particles survive in population {}.",
                          survivingIndices.size(), validIndexPositions.length, popIDKey);

                      // Ensure there is data within ROI
                      if (!survivingIndices.isEmpty()) {

                        // 4. Build new PopulationID for the filtered population
                        PopulationID filteredPopID = new PopulationID(popIDKey);
                        filteredPopID.append(
                            new PopulationStep.ManualAlignFilterSubtype(label,
                                new ArrayList<>(restrictedChannels.keySet())
                            ));

                        // 5. Slice events for each isotope:
                        // For each isotope in the aligned population, pick only the surviving
                        // particle indices from the event list and register a new subpopulation.
                        for (Channel channel : alignedChannels) {
                          Trace trace = sample.getTrace(channel);
                          if (trace != null) {

                            Population pop = trace.getPopulation(popIDKey);
                            if (pop != null) {

                              EventCollection collection = pop.getEvents();
                              List<Event> events = collection.getNpEvents();

                              List<Event> survivingEvents = new ArrayList<>();

                              for (int idx : survivingIndices) {
                                if (idx < events.size()) {
                                  survivingEvents.add(events.get(idx));
                                } else {
                                  LOGGER.warn("Surviving index {} out of bounds for isotope {} (events={}) " +
                                          "— " +
                                          "skipping.",
                                      idx, channel, events.size());
                                }
                              }

                              // Note: SubEventCollection means that BG is defined by old pop which is correct
                              MainEventCollection filteredCollection = new SubEventCollection(trace,
                                  collection);
                              filteredCollection.add(survivingEvents);

                              List<Channel> channels = alignedChannels.stream()
                                  .map(Channel::copy)
                                  .toList();

                              trace.addOverridePopulation(filteredPopID,
                                  new NpPopulation(
                                      filteredPopID,
                                      pop,
                                      filteredCollection,
                                      filteredPopID.toString(),
                                      channels
                                  ),
                                  false);
                            }
                          }
                        }

                        // ── 6. Slice spectral data for the filtered population ────────
                        // Same pattern as the HAC save button — slice SpectralArray intensities
                        // and additional features to surviving particle indices only.
                        List<SpectralArray> allSpectra = sample.getSpectralData(popIDKey);
                        if (allSpectra != null && !allSpectra.isEmpty()) {

                          List<SpectralArray> filteredSpectra = new ArrayList<>();

                          // Each spectral array holds all NP data for one isotope
                          // (essentially the double[] getData() on an eventCollection)
                          for (SpectralArray sarr : allSpectra) {
                            double[] allIntensities = sarr.getIntensity();
                            double[] filteredIntensities = new double[survivingIndices.size()];

                            for (int j = 0; j < survivingIndices.size(); j++) {
                              int particleIdx = survivingIndices.get(j);
                              if (particleIdx < allIntensities.length) {
                                filteredIntensities[j] = allIntensities[particleIdx];
                              } else {
                                LOGGER.warn("Particle index {} out of bounds for spectral array "
                                        + "isotope {} (length {}) — filling 0.",
                                    particleIdx, sarr.getChannel().getUIString(), allIntensities.length);
                                filteredIntensities[j] = 0.0;
                              }
                            }

                            // Slice additional features by the same surviving indices
                            HashMap<String, double[]> filteredFeatures = new HashMap<>();
                            for (String key : sarr.listAdditionalFeatures()) {
                              double[] allFeatureValues = sarr.getAdditionalFeature(key);
                              double[] filteredFeatureValues = new double[survivingIndices.size()];

                              for (int j = 0; j < survivingIndices.size(); j++) {
                                int particleIdx = survivingIndices.get(j);
                                if (allFeatureValues != null && particleIdx < allFeatureValues.length) {
                                  filteredFeatureValues[j] = allFeatureValues[particleIdx];
                                } else {
                                  filteredFeatureValues[j] = 0.0;
                                }
                              }
                              filteredFeatures.put(key, filteredFeatureValues);
                            }

                            filteredSpectra.add(new SpectralArray(
                                sarr.getChannel(),
                                filteredIntensities,
                                filteredFeatures));
                          }

                          sample.addSpectralData(filteredPopID, filteredSpectra);
                        }
                      } else {
                        LOGGER.warn("Aligned filter removed all particles from population {} — skipping.",
                            popIDKey);
                      }
                    }
                  }
                }
              }

              ////////////////////////////////////////////////////////////////////////////////
              //////////////////// ISOTOPE BASED FILERS //////////////////////////////////////
              ////////////////////////////////////////////////////////////////////////////////

            } else {


              List<Trace> traces = sample.getTraces();
              traceLoop:
              for (Trace trace : traces) {

                PopulationID popID = branch.getID(trace);
                if (popID != null && trace.hasType(popID) && trace.getPopulation(popID) != null
                    && trace.getPopulation(popID).getEvents().size() > 0) {

                  boolean isAlignedOrPValue = AnalysisUtils.isAlignedOrPVal(popID);

                  if (getIsStopped().get()) {
                    break;
                  }

                  ////////////////////////////////////////////////////////////////////////////////
                  //////////////////// OVERLAP ///////////////////////////////////////////////////
                  ////////////////////////////////////////////////////////////////////////////////

                  if (filterOption.equals(FilterOptions.OVERLAP) && !isAlignedOrPValue) {
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
                                inputPopSummary),
                            false);

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
                                    inputPopSummary),
                                false);
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
                                inputPopSummary),
                            false);
                        // (4) Updating the branch: when ID was not just "appended"
                        // branch.overrideID(trace, );
                      }
                    }

                    ////////////////////////////////////////////////////////////////////////////////
                    //////////////////// OVER RANGE/////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////////
                  } else if (filterOption.equals(FilterOptions.OVER_RANGE) && !isAlignedOrPValue) {

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
                              inputPopSummary),
                          false);
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
                                inputPopSummary),
                            false);

                      }
                    }
                    ////////////////////////////////////////////////////////////////////////////////
                    //////////////////// ROI ///////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////////
                  } else if (filterOption.equals(FilterOptions.ROI_REGION) && !isAlignedOrPValue) {

                    // During processing, merged samples are analysed based on the subsamples so we do not
                    // expect merged samples here. Check is done above already. Repeat here as we cast
                    // and want to keep it safe even if logic above changes.

                    if (sample instanceof SampleImpl) {
                      SampleImpl sampleImpl = ((SampleImpl) sample);

                      Population oldPop = trace.getPopulation(popID);
                      if (oldPop != null) {

                        // check for isotope-specific limits
                        Channel channel = trace.getChannel();

                        // extract params
                        RoiCategory roiCategory = params.roiCategory.getValue();
                        EventParameter eventParameter = params.eventParameter.getValue();
                        MathMod mathConversion = params.mathConversion.getValue();
                        Unit unit = params.getUnitConversion();
                        RoiType roiType = params.roiType.getValue();
                        Double startPar = params.start.getValue();
                        Double endPar = params.end.getValue();
                        Double sigFactor = params.sigFactor.getValue();


                        // Check for exceptions
                        List<RoiStartStopBundle> startStopExceptions = params.getRoiStartStopBundles();
                        List<RoiSigFactorBundle> sigFactorExceptions = params.getRoiSigFactorBundles();

                        boolean hasExceptionData = false;
                        for (RoiStartStopBundle startStopException : startStopExceptions) {
                          if (startStopException.isotopeHeaderParameter.getValue().unwrap().equals(channel.getIsotope())) {
                            startPar = startStopException.start.getValue();
                            endPar = startStopException.end.getValue();
                            hasExceptionData = true;
                            break; // break at first match
                          }
                        }

                        for (RoiSigFactorBundle sigException : sigFactorExceptions) {
                          if (sigException.isotopeHeaderParameter.getValue().unwrap().equals(channel.getIsotope())) {
                            sigFactor = sigException.sigFactor.getValue();
                            hasExceptionData = true;
                            break; // break at first match
                          }
                        }
                        boolean applyToExceptionOnly = params.getRoiExceptionExclusive().getValue();

                        List<Event> eventsToKeep = new ArrayList<>();

                        /// CATEGORY STARTS
                        if (roiCategory.equals(RoiCategory.ABSOLUTE_VALUES)) {

                        /*
                        When you want a ROI for 50 - 100 nm but eventParameter is GROSS_AREA,
                        sampleImpl.revertQuant() will just return 50 - 100 because this is
                        the desired and expected literal return value for the applyQuant()
                        function in opposite direction.
                        We should not interfere with consistency there but HERE in the ROI
                        function we have to make sure that ill configurations are not evaluated
                        by returning (in the example given) all GROSS_AREA events between 50 - 100 COUNTS.

                        case a) Parameter is duration or sth that cannot quantify -> we are OK apply ROI
                        case b)
                         */

                          // special case: limiting to exceptions only
                          if (applyToExceptionOnly) {
                            if (!hasExceptionData) {
                              continue traceLoop;
                            }
                          }

                          boolean applyRoi;
                          if (!EventParameter.isAreaOrHeight(eventParameter)) {
                            // just go ahead: quantification will not be applied anyway
                            applyRoi = true;
                          } else {
                            // we can quantify BUT is quant unit requested at all? If not, just go ahead!
                            if (IntensityUnit.CTS.equals(unit)) {
                              applyRoi = true;
                            } else {
                              // unit allows quant (i.e, net height or net are) and user wants quant unit (i
                              // .e., not cts/DT)
                              // Check: does parameter match event parameter in quant?
                              applyRoi = sampleImpl.getQuant()
                                  .getExperimentalConditions().getEventPar().equals(eventParameter);
                            }
                          }

                          if (applyRoi) {

                            // try to revert quantification
                            double[] startParArr = sampleImpl.revertQuant(new double[]{startPar}, channel,
                                eventParameter, unit);
                            double[] endParArr = sampleImpl.revertQuant(new double[]{endPar}, channel,
                                eventParameter, unit);

                            // in some edge cases where quant is not done correctly, empty arrays return
                            if (startParArr.length > 0 || endParArr.length > 0) {
                              startPar = startParArr[0];
                              endPar = endParArr[0];

                              // then apply math
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
                                continue; // skip to next trace and do not add ROI
                              }
                            } else {
                              LOGGER.info("A ROI was requested but quantification could not be reversed. " +
                                  "Please" +
                                  " change the uni in the ROI submethod.");
                              continue; // skip to next trace and do not add ROI
                            }
                          } else {
                            LOGGER.info("Cannot apply ROI because requested quantified unit is not " +
                                "available " +
                                "with calibration of sample." +
                                "Example: You may have requested a ROI based on size (nm) for the event " +
                                "parameter 'gross area' which is not possible." +
                                "Or you quantified using 'net area' and try to construct a size-based ROI" +
                                "using 'net height' now.");
                            continue; // skip to next trace and do not add ROI
                          }

                        } else if (roiCategory.equals(RoiCategory.PERCENTILES)) {

                          // special case: limiting to exceptions only
                          if (applyToExceptionOnly) {
                            if (!hasExceptionData) {
                              continue traceLoop;
                            }
                          }

                          // now start and stop should be percentiles.
                          startPar = Math.max(startPar, Double.MIN_VALUE);
                          startPar = Math.min(100, startPar);
                          endPar = Math.max(endPar, Double.MIN_VALUE);
                          endPar = Math.min(100, endPar);

                          // include computed threshold bounds
                          inputPopSummary.add("start", null, SnF.doubleToString(startPar, NF.D1C3));
                          inputPopSummary.add("end", null, SnF.doubleToString(endPar, NF.D1C3));

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
                              LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerLim +
                                  " " + "> " + upperLim);
                              continue; // skip to next trace and do not add ROI
                            }
                          } else {
                            LOGGER.info("Cannot create ROI. start > end: " + startPar + " > " + endPar
                                + ". Start and stop are given as percentiles and have been clamped to " +
                                "0-100%.");
                            continue; // skip to next trace and do not add ROI
                          }
                        } else if (roiCategory.equals(RoiCategory.IQR)) {
                          // sig factor should be nonzero
                          if (sigFactor < 0) {
                            LOGGER.info("Factor must be greater than zero! Current factor: " + sigFactor +
                                ".");
                            continue; // skip to next trace and do not add ROI
                          }
                          sigFactor = Math.max(sigFactor, Double.MIN_VALUE);
                          Percentile percentile = new Percentile();
                          // percentile only uses order, which does not changer with math mod, but distance
                          // +1.5IQR scales differently on log scale!
                          double[] data = sample.getData(channel, popID, EventType.NP, eventParameter, unit);
                          // This does not support QUANT:
                          // double[] data = oldPop.getEvents().get(EventType.NP, eventParameter);

                          data = mathConversion.calc(data);
                          double q1 = percentile.evaluate(data, 25); // Q1
                          double q3 = percentile.evaluate(data, 75); // Q3
                          double iqr = q3 - q1;
                          double lowerLim = q1 - sigFactor * iqr;
                          double upperLim = q3 + sigFactor * iqr;

                          // include computed threshold bounds
                          inputPopSummary.add("start", null, SnF.doubleToString(lowerLim, NF.D1C3));
                          inputPopSummary.add("end", null, SnF.doubleToString(upperLim, NF.D1C3));

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
                            LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerLim +
                                " > " + upperLim);
                            continue; // skip to next trace and do not add ROI
                          }
                        } else if (roiCategory.equals(RoiCategory.MAD)) {
                          // sig factor should be nonzero
                          if (sigFactor < 0) {
                            LOGGER.info("Factor must be greater than zero! Current factor: " + sigFactor +
                                ".");
                            continue; // skip to next trace and do not add ROI
                          }
                          sigFactor = Math.max(sigFactor, Double.MIN_VALUE);
                          // percentile only uses order, which does not changer with math mod,
                          // but distance +1.5MAD scales differently on log scale!
                          double[] data = sample.getData(channel, popID, EventType.NP, eventParameter, unit);
                          // This does not support QUANT:
                          // double[] data = oldPop.getEvents().get(EventType.NP, eventParameter);

                          data = mathConversion.calc(data);
                          double median = Median.median(data);
                          double mad = Median.mad(data);
                          double lowerLim = median - sigFactor / 2 * mad;
                          double upperLim = median + sigFactor / 2 * mad;

                          // include computed threshold bounds
                          inputPopSummary.add("start", null, SnF.doubleToString(lowerLim, NF.D1C3));
                          inputPopSummary.add("end", null, SnF.doubleToString(upperLim, NF.D1C3));

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
                            LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerLim +
                                " > " + upperLim);
                            continue; // skip to next trace and do not add ROI
                          }
                        } else if (roiCategory.equals(RoiCategory.OTSU)) {
                          double[] data = sample.getData(channel, popID, EventType.NP, eventParameter, unit);
                          data = mathConversion.calc(data);

                          BinWidthEstimator estimator = params.getBinWidthEstimator().getValue();
                          double customWidth = params.getCustomBinWidth().getValue();

                          List<double[]> dataList = new ArrayList<>();
                          dataList.add(data);

                          List<JFreeUtil.ExtendedHistogramDataSet> histoDataSets =
                              JFreeUtil.createHistogramDatasets(
                                  dataList, List.of("Name"),
                                  HistogramNormalization.FREQUENCY,
                                  estimator,
                                  customWidth
                              );

                          if (!histoDataSets.isEmpty()) {
                            HistogramDataset histogramDataset = histoDataSets.get(0).getHistogramDataset();
                            OtsuRegion otsuRegion = params.getOtsuRegion().getValue();
                            double lowerThr;
                            double upperThr;
                            if (otsuRegion.equals(OtsuRegion.UPPER)) {
                              lowerThr = OtsuThreshold.computeThreshold(histogramDataset, 0);
                              upperThr = ArrUtils.getMax(data) + 1; // make sure we are larger than max
                            } else {
                              lowerThr = ArrUtils.getMin(data) - 1; // make sure we are smaller than min
                              upperThr = OtsuThreshold.computeThreshold(histogramDataset, 0);
                            }

                            // include computed threshold bounds
                            inputPopSummary.add("start", null, SnF.doubleToString(lowerThr, NF.D1C3));
                            inputPopSummary.add("end", null, SnF.doubleToString(upperThr, NF.D1C3));

                            if (lowerThr < upperThr) {
                              // Problem: histogram and events are not from the same list so we have to
                              // rely on indices being in correct order...
                              List<Event> events = oldPop.getEvents().getNpEvents();
                              if (data.length == events.size()) {
                                // check which are in ROI
                                for (int i = 0; i < events.size(); i++) {
                                  Event event = events.get(i);
                                  // check breaking condition
                                  if (getIsStopped().get()) {
                                    break;
                                  }
                                  double eventVal = data[i];
                                  if (roiType.valid(lowerThr, upperThr, eventVal)) {
                                    eventsToKeep.add(event);
                                  }
                                }
                              } else {
                                LOGGER.info("Cannot create ROI. Data and events do not have same length.");
                                continue;
                              }
                            } else {
                              LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerThr +
                                  " > " + upperThr);
                              continue;
                            }
                          } else {
                            LOGGER.info("Cannot estimate Otsu for empty histogram data.");
                            continue;
                          }
                        } else if (roiCategory.equals(RoiCategory.CHANGE_POINT)) {
                          double[] data = sample.getData(channel, popID, EventType.NP, eventParameter, unit);

                          data = mathConversion.calc(data);

                          BinWidthEstimator estimator = params.getBinWidthEstimator().getValue();
                          double customWidth = params.getCustomBinWidth().getValue();

                          List<double[]> dataList = new ArrayList<>();
                          dataList.add(data);

                          List<JFreeUtil.ExtendedHistogramDataSet> histoDataSets =
                              JFreeUtil.createHistogramDatasets(
                                  dataList, List.of("Name"),
                                  HistogramNormalization.FREQUENCY,
                                  estimator,
                                  customWidth
                              );

                          if (!histoDataSets.isEmpty()) {
                            HistogramDataset histogramDataset = histoDataSets.get(0).getHistogramDataset();
                            OtsuRegion otsuRegion = params.getOtsuRegion().getValue();
                            int width = params.getSmoothWidth().getValue();
                            double lowerThr;
                            double upperThr;
                            if (otsuRegion.equals(OtsuRegion.UPPER)) {
                              lowerThr = HistogramFilters.findSplitPoint(histogramDataset, width);
                              upperThr = ArrUtils.getMax(data) + 1; // make sure we are larger than max
                            } else {
                              lowerThr = ArrUtils.getMin(data) - 1; // make sure we are smaller than min
                              upperThr = HistogramFilters.findSplitPoint(histogramDataset, width);
                            }

                            // include computed threshold bounds
                            inputPopSummary.add("start", null, SnF.doubleToString(lowerThr, NF.D1C3));
                            inputPopSummary.add("end", null, SnF.doubleToString(upperThr, NF.D1C3));

                            if (lowerThr < upperThr) {
                              // Problem: histogram and events are not from the same list so we have to
                              // rely on indices being in correct order...
                              List<Event> events = oldPop.getEvents().getNpEvents();
                              if (data.length == events.size()) {
                                // check which are in ROI
                                for (int i = 0; i < events.size(); i++) {
                                  Event event = events.get(i);
                                  // check breaking condition
                                  if (getIsStopped().get()) {
                                    break;
                                  }
                                  double eventVal = data[i];
                                  if (roiType.valid(lowerThr, upperThr, eventVal)) {
                                    eventsToKeep.add(event);
                                  }
                                }
                              } else {
                                LOGGER.info("Cannot create ROI. Data and events do not have same length.");
                                continue;
                              }
                            } else {
                              LOGGER.info("Cannot create ROI. Calculated lowerLim > upperLim: " + lowerThr +
                                  " > " + upperThr);
                              continue;
                            }
                          } else {
                            LOGGER.info("Cannot estimate Otsu for empty histogram data.");
                            continue;
                          }
                        }


                        if (setAsMainBranch) {
                          // (1) Remove existing population from the Trace to avoid pile up of Populations)
                          trace.removePopulation(popID);
                          // (2) Update as new ID with the latest step; prepare copy here before altering
                          // popID
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
                                  inputPopSummary),
                              false);
                          // (4) Make this new head of branch!
                          branch.overrideID(trace, idCopy);

                        } else {
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
                                  inputPopSummary),
                              false);
                          // (4) Updating the branch: not necessary as it still has the pointer and ID was
                          // only
                          // "appended"
                        }


                      }
                    }
                    ////////////////////////////////////////////////////////////////////////////////
                    //////////////////// MATCH ///////////////////////////////////////////////////////
                    ////////////////////////////////////////////////////////////////////////////////
                    // do not match syn with syn -> check here and skip otherwise
                    // ALso: no "&& !isAlignedOrPValue" here b/c this option does not remove any events
                    // from individual isotopes and is intended to evaluate different align algorithms
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
                                inputPopSummary),
                            false);
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
                                inputPopSummary),
                            false);
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
                          List<Event> falseNegCopy =
                              new ArrayList<>(falseNegatives.getEvents().getNpEvents());
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

                        trace.addOverridePopulation(idCopyTemp, falseNegatives, false);
                        // (4) Updating the branch: when ID was not just "appended"
                        // branch.overrideID(trace, );
                      }
                    } // end
                  } else {
                    LOGGER.warn("Failed to apply filter {}! Note that you cannot apply filters to aligned " +
                        "populations. You must apply filters before aligning.", filterOption);
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


    } catch (
        Exception e) {
      LOGGER.error(e.getMessage() + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }

    return taskResult;
  }


}
