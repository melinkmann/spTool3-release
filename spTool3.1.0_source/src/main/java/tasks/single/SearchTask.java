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
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.TISeries;
import dataModelNew.Trace;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import math.stat.MeasureOfLocation;
import math.stat.Median;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.*;
import processing.parameterSets.impl.NormalSearchParams;
import processing.parameterSets.impl.NormalSearchParams.ThresholdBundle;
import processing.parameters.Parameter;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.Statistics;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;
import util.*;

import static sandbox.montecarlo.Statistics.MIN_P_VALUE;

public class SearchTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(SearchTask.class);

  private final PopulationBranch branch;
  private final NormalSearchParams params;
  private final AtomicReference<Sample> sampleRef;

  public SearchTask(String taskName, PopulationBranch branch, NormalSearchParams params,
                    AtomicReference<Sample> sampleRef) {
    super(taskName);
    this.branch = branch;
    this.params = (NormalSearchParams) params.getCopyWithPreviousDateFileAndID();
    this.sampleRef = sampleRef;
  }

  @Override
  public TaskResult call() {

    List<Isotope> selIsotope = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();

    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new EmptyTaskResult();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null) {
        LOGGER.info("Event search starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          Sample sample = sampleRef.get();

          if (sample instanceof SampleImpl) {

            // unpack input parameters
            SearchAlgorithm searchAlgorithmChoice = params.getSearchAlgorithm().getValue();
            ThresholdBundle startBundle = params.getStartCriterium();
            ThresholdBundle stopBundle = params.getStopCriterium();
            ThresholdBundle heightBundle = params.getHeightCriterium();

            ThresholdBundle pAccScreenStartBundle = params.getPrescreenStartStopCriterium();
            ThresholdBundle pAccScreenStopBundle = params.getPrescreenStartStopCriterium();
            ThresholdBundle pAccScreenHeightBundle = params.getPrescreenHeightCriterium();
            ThresholdBundle pAccPValueBundle = params.getpValueThreshold();
            ThresholdBundle pSingleCompPValueBundle = params.getSingleComponentPValueThreshold();
            ThresholdBundle pAccStartStopPValueBundle = params.getpValueStartStop();
            int pAccMinEvents = params.getPrescreenMinEvents().getValue();
            CombinedPStatistics pStatistics = params.getPStatistics().getValue();

            double offset = params.getStartStopOffset().getValue();

            double windowAttenuator = params.getWindowIntensityAttenuator().getValue();
            double windowPercent = params.getWindowPeakPercent().getValue();

            SmoothType smoothType = params.getSmoothType().getValue();
            double smoothWidth = params.getSmoothGaussianWidth().getValue();

            NetCorrectionOption netSignalChoice = params.getNetCorrectionOption().getValue();
            MeasureOfLocation netSignalLocation = params.getNetTimeWindowLocation().getValue();
            boolean suppressNegativeNetValues = params.getSuppressNegativeValues().getValue();
            double netSignalTime = params.getNetTimeWindow().getValue();

            // Iterate
            List<Trace> traces = new ArrayList<>(sample.getTraces());

            if (!searchAlgorithmChoice.equals(SearchAlgorithm.P_VALUE_ACCUMULATION)) {
              for (Trace trace : traces) {

                if (getIsStopped().get()) {
                  break;
                }

                Baseline bln = trace.getBaseline();

                if (bln.hasBaseline()) {
                  // decide thresholding
                  ThresholdSupplierInstructions startInstr = getSupplier("Start threshold",
                      startBundle, offset);
                  ThresholdSupplierInstructions stopInstr = getSupplier("Stop threshold",
                      stopBundle, offset);
                  ThresholdSupplierInstructions heightInstr = getSupplier("Search threshold",
                      heightBundle, 0);

                  // This involves the computation of thresholds, which is heavy for compound Poisson
                  ThresholdSupplier start = startInstr.get(bln.getBackgroundDistribution());
                  ThresholdSupplier stop = stopInstr.get(bln.getBackgroundDistribution());
                  ThresholdSupplier height = heightInstr.get(bln.getBackgroundDistribution());

                  // Dummy
                  EventCollection coll = new MainEventCollection();
                  PopParSummary popParSummary = new PopParSummary();
                  switch (searchAlgorithmChoice) {

                    case SIMPLE -> {
                      coll = SearchUtils.simpleSearch(trace, trace.getTISeries(), height);
                      break;
                    }

                    case SPLIT_CORRECTION -> {
                      coll = SearchUtils.splitCorrectSearch(trace, trace.getTISeries(),
                          start, stop, height);
                      break;
                    }

                    case SPLIT_CORRECTION_WINDOW -> {
                      coll = SearchUtils.windowSplitCorrectSearch(trace, trace.getTISeries(),
                          start, stop, height, windowPercent, windowAttenuator);
                      popParSummary.add("WinPct", params.getWindowPeakPercent(),
                          SnF.doubleToString(windowPercent, NF.D1C2));
                      popParSummary.add("WinAttenuate", params.getWindowIntensityAttenuator(),
                          SnF.doubleToString(windowAttenuator, NF.D1C2));
                      break;
                    }

                    case SPLIT_CORRECTION_SMOOTH -> {
                      coll = SearchUtils.smoothAndSplitCorrectSearch(trace, trace.getTISeries(),
                          start, stop, height, smoothType, smoothWidth);
                      popParSummary.add("SmoothWidth", params.getSmoothGaussianWidth(),
                          SnF.doubleToString(smoothWidth, NF.D1C1));
                      break;
                    }
                  }

                  if (getIsStopped().get()) {
                    break;
                  }

                  PopulationID populationID = branch.startBranchForTrace(trace, params);
                  Population population = new NpPopulation(
                      populationID,
                      coll,
                      populationID.toString(),
                      startInstr,
                      stopInstr,
                      heightInstr,
                      popParSummary);
                  trace.addOverridePopulation(populationID, population);

                  // ##############################################################
                  // Net signal: do this right after the search
                  // ##############################################################
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
                      double bgPerDT = supplier.interpolate(peakIdx, trace.getTISeries().size());

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
                setProgress(counter / traces.size());
              }
            } else {
              // p-value approach

              // ################################################################################
              // find interesting traces
              // ################################################################################
              LOGGER.trace("p-Value accumulation prescreening...");

              // storage map
              HashMap<Trace, EventCollection> preScreenMap = new HashMap<>();
              List<Trace> excludedTraces = new ArrayList<>(); // just for logging
              HashMap<Trace, ThresholdSupplierInstructions> screenStartThrMap = new HashMap<>();
              HashMap<Trace, ThresholdSupplierInstructions> preScreenStopThrMap = new HashMap<>();
              HashMap<Trace, ThresholdSupplierInstructions> preScreenHeightThrMap = new HashMap<>();


              // for constructing a large enough p value array
              int rawDataMaxSize = 0;

              // check if we only want the selected isotopes?
              if (params.getOnlyUseSelectedIsotopesForPValue().getValue()) {
                traces.removeIf(t -> !selIsotope.contains(t.getMzValue().getIsotope()));
              }

              for (Trace trace : traces) {

                if (getIsStopped().get()) {
                  break;
                }

                Baseline bln = trace.getBaseline();

                // dummies
                ThresholdSupplierInstructions screenStartInstr = new ThresholdSupplierInstructions();
                ThresholdSupplierInstructions screenStopInstr = new ThresholdSupplierInstructions();
                ThresholdSupplierInstructions screenHeightInstr = new ThresholdSupplierInstructions();

                if (bln.hasBaseline()) {
                  // decide thresholding for prescreening
                  screenStartInstr = getSupplier("Prescreen start threshold", pAccScreenStartBundle, 0);
                  screenStopInstr = getSupplier("Prescreen stop threshold", pAccScreenStopBundle, 0);
                  screenHeightInstr = getSupplier("Prescreen search threshold", pAccScreenHeightBundle, 0);

                  // This involves the computation of thresholds, which is heavy for compound Poisson
                  ThresholdSupplier screenStart = screenStartInstr.get(bln.getBackgroundDistribution());
                  ThresholdSupplier screenStop = screenStopInstr.get(bln.getBackgroundDistribution());
                  ThresholdSupplier screenHeight = screenHeightInstr.get(bln.getBackgroundDistribution());

                  // just perform a split correction search to find interesting traces
                  EventCollection coll = SearchUtils.splitCorrectSearch(trace, trace.getTISeries(),
                      screenStart, screenStop, screenHeight);

                  // are there enough events to add? note: reference value is given "per minute long sample"
                  if (coll.size() / trace.getTISeries().getDuration() > pAccMinEvents / 60d) {
                    preScreenMap.put(trace, coll);
                    // only update for added traces
                    rawDataMaxSize = Math.max(rawDataMaxSize, trace.getTISeries().size());
                  } else {
                    excludedTraces.add(trace);
                  }

                  if (getIsStopped().get()) {
                    break;
                  }

                  setProgress(0.2 * counter / traces.size());
                }
                screenStartThrMap.put(trace, screenStartInstr);
                preScreenStopThrMap.put(trace, screenStopInstr);
                preScreenHeightThrMap.put(trace, screenHeightInstr);
              }

              // Just sme quick feedback
              LOGGER.trace("Added traces: " + preScreenMap.keySet().stream().map(t -> t.getMzValue().getName()).collect(Collectors.joining(", ")));
              LOGGER.trace("Excluded traces: " + excludedTraces.stream().map(t -> t.getMzValue().getName()).collect(Collectors.joining(", ")));

              // ################################################################################
              // now iterate over all interesting traces and perform the p-value estimation
              // ################################################################################
              LOGGER.trace("p-Value accumulation search begins. Starting p-Value calculation...");

              // store results as p value computation is costly
              HashMap<Trace, HashMap<MultiKey, Double>> pValueMap = new HashMap<>();
              // keep access to bln distr
              HashMap<Trace, StatCollection> statCollectionMap = new HashMap<>();
              preScreenMap.keySet().forEach(t -> {
                Baseline bln = t.getBaseline();
                StatCollection bgDist = new StatCollectionRAM(bln.getBackgroundDistribution());
                statCollectionMap.put(t, bgDist);
                pValueMap.put(t, new HashMap<>());
              });

              // p value container
              double[] lowestLocalPValues = new double[rawDataMaxSize]; // smallest uncombined p

              // prepare Fisher's method
              int k = preScreenMap.keySet().size();

              if (k > 0) {
                // keep H stat: here is container
                double[] hStat = new double[rawDataMaxSize];

                int mod = hStat.length / 10;
                for (int i = 0; i < hStat.length; i++) {

                  if (i > 0 && i % mod == 0) {
                    LOGGER.trace("p-Value computation at data point " + i + " / " + hStat.length);
                  }

                  // check stopping condition every 5000 data points (isStopped.get() likely slower (atomic))
                  if (i % 5000 == 0 && getIsStopped().get()) {
                    break;
                  }

                  // store p values across traces at data point i
                  double[] localPValues = new double[preScreenMap.keySet().size()];

                  // index to keep track of the traces
                  int localTraceIdx = 0;

                  for (Trace trace : preScreenMap.keySet()) {
                    Baseline bln = trace.getBaseline();
                    boolean hasSlices = bln.getBackgroundDistribution().size() > 1;
                    int rawDataLen = trace.getTISeries().size();
                    if (bln.hasBaseline()) {
                      // .getIntensity()[i] should be fine as the access is done via SoftReference.
                      // Else: load array here to combat bottleneck.
                      double intensity = trace.getTISeries().getIntensity()[i];

                      HashMap<MultiKey, Double> pStorage = pValueMap.get(trace);

                    /*
                     Compute key: this is heavy
                     speed up computation by rounding the intensity; matters for TOF data;
                     note that we must consider signal and the baseline as p values depend on both.
                     */
                      StatCollection bgDist = statCollectionMap.get(trace);
                      StatDataSet distribution = bgDist.interpolate(i, rawDataLen);
                      int distKey = distribution.getDistrID();
                      double roundedIntensity;
                      MultiKey key;
                      // heavy rounding for slices; keep intensity as precise as possible for good p
                      roundedIntensity = Math.round(intensity * 1000d) / 1000d;
                      if (!hasSlices) {
                        // roundedMean = 0;  mean is equal everywhere -> not part of key
                        // roundedSpread = 0;  spread is equal everywhere -> not part of key
                        key = new Key1(roundedIntensity);
                      } else {
                        double roundedMean = Math.round(distribution.getLocation() * 100d) / 100d;
                        double roundedSpread;
                        if (ResettableStatDataSet.Limit.isPoisson(distKey)) {
                          roundedSpread = 0; // not needed in Poisson
                        } else {
                          roundedSpread = Math.round(distribution.getSpread() * 100d) / 100d;
                        }
                        key = new Key4(distKey, roundedMean, roundedSpread, roundedIntensity);
                      }

                      double p;
                      if (pStorage.containsKey(key)) {
                        p = pStorage.get(key);
                      } else {
                        p = distribution.calcPValue(roundedIntensity);
                        pStorage.put(key, p);
                      }

                      // Clamp Fisher result but no the intermediate steps (unless the distribution class
                      // gives bad results)
                      // if (p < MIN_P_VALUE) {
                      // p = MIN_P_VALUE;
                      // }
                      localPValues[localTraceIdx] = p;
                    } else {
                      // if no baseline, put 1, i.e., the "no event" indicator
                      localPValues[localTraceIdx] = 1;
                    }
                    localTraceIdx++;
                  }

                  // combine local pValues: Calculate -2 * sum(ln(p_i)) (Fisher's method): only if n>1
                  if (localPValues.length > 1) {
                    double chiSquareStat = 0;
                    for (double p : localPValues) {
                      if (p <= 0 || p > 1.0) {
                        LOGGER.error("p cannot be smaller or equal to zero or larger than one. p=" + p);
                      }
                      // prevent failure here by clamping p.
                      p = Math.max(Math.nextUp(0d), p);
                      p = Math.min(p, 1);
                      chiSquareStat += -2.0 * Math.log(p);
                    }
                    // store H stat
                    hStat[i] = chiSquareStat;
                    double lowestLocalPValue = ArrUtils.getMin(localPValues);
                    // we should not report p=0 here;
                    lowestLocalPValue = Math.max(lowestLocalPValue, MIN_P_VALUE);
                    lowestLocalPValues[i] = lowestLocalPValue;
                  } else {
                    lowestLocalPValues[i] = localPValues[localPValues.length - 1];
                  }
                  // System.out.println(pValues[i]);
                  setProgress(0.2 + 0.8 * (i + 1) / rawDataMaxSize);
                }

                // if only one isotope was selected, do not compute
                double[] pValues; // combined p
                if (preScreenMap.keySet().size() > 1) {
                  if (pStatistics.equals(CombinedPStatistics.GAMMA_EMPIRICAL)) {
                    // ALTERNATIVE: use gamma dist empirically to estimate p
                    pValues = Statistics.estimateGammaP(preScreenMap.size(), hStat);
                  } else {
                    pValues = Statistics.computeChiSquareP(preScreenMap.size(), hStat);
                  }
                } else {
                  // Just one isotope: no need for testing, just use local p value
                  pValues = lowestLocalPValues;
                }


                // ################################################################################
                // now iterate over p values essentially executing a split correction
                // ################################################################################


                boolean considerSingleComponent = params.getEnableSingleComponentPValueThreshold().getValue();
                double singleCompHeightPValue = 0.5; // default ini
                Parameter<Double> singleCompPSignificance = pSingleCompPValueBundle.getAlpha(); // default ini
                switch (pSingleCompPValueBundle.getThresholdOption().getValue()) {
                  case ALPHA -> {
                    singleCompHeightPValue = pSingleCompPValueBundle.getAlpha().getValue();
                    singleCompPSignificance = pSingleCompPValueBundle.getAlpha();
                  }
                  case FACTOR -> {
                    singleCompHeightPValue =
                        Statistics.zToAlpha(pSingleCompPValueBundle.getFactor().getValue());
                    singleCompPSignificance = pSingleCompPValueBundle.getFactor();
                  }
                }

                double heightPValue = 0.5; // default ini
                Parameter<Double> pSignificance = pAccPValueBundle.getAlpha(); // default ini
                switch (pAccPValueBundle.getThresholdOption().getValue()) {
                  case ALPHA -> {
                    heightPValue = pAccPValueBundle.getAlpha().getValue();
                    pSignificance = pAccPValueBundle.getAlpha();
                  }
                  case FACTOR -> {
                    heightPValue = Statistics.zToAlpha(pAccPValueBundle.getFactor().getValue());
                    pSignificance = pAccPValueBundle.getFactor();
                  }
                }

                double startStopPValue = 0.5; // default ini
                Parameter<Double> pStartStop = pAccStartStopPValueBundle.getAlpha(); // default ini
                switch (pAccStartStopPValueBundle.getThresholdOption().getValue()) {
                  case ALPHA -> {
                    startStopPValue = pAccStartStopPValueBundle.getAlpha().getValue();
                    pStartStop = pAccStartStopPValueBundle.getAlpha();
                  }
                  case FACTOR -> {
                    startStopPValue = Statistics.zToAlpha(pAccStartStopPValueBundle.getFactor().getValue());
                    pStartStop = pAccStartStopPValueBundle.getFactor();
                  }
                }

                // chatGPT hint: use Sidak method to account for multi comparison problem
                double combinedStartStopValue = 1.0 - Math.pow(1.0 - startStopPValue,
                    1.0 / preScreenMap.size());

              /*
               Create the EventCollection for each trace that passed pre-screening.
               Note: It would be more efficient to share the indices among
               all traces. However, at the moment, we need a fast proof of principle. Optimization for later.
               This would also require some thoughts: what if user applies gating and removes
               all e.g. Fe events. Should we check for hasEvents() and if not remove Fe from
               aligned collection? Else, going just by index, Fe will always be present.
               Otherwise, this behaviour may be desirable, thinking the spectrum more like a region if NPs
               where our goal is not to find pure NPs but to remove mixed NPs later via cluster analysis?
               Mixing is inevitable anyway and maybe should be considered later and not at this early stage
               in the pipeline.
               */

                for (Trace trace : preScreenMap.keySet()) {
                  if (trace.getBaseline().hasBaseline()) {
                    Baseline bln = trace.getBaseline();
                    MainEventCollection collection = new MainEventCollection(trace);
                    List<Integer> currentEventIndices = new ArrayList<>();

                    TISeries tiSeries = trace.getTISeries();
                    double[] yData = tiSeries.getIntensity();
                    double currentLowestP = 1;
                    double currentLowestLocalP = 1;
                    int lastIndex = tiSeries.size() - 1;

                    if (pValues.length != yData.length) {
                      LOGGER.error("Mismatch between p-value array length and intensity data length. Cannot" +
                          " " +
                          "create event population.");
                    } else {

                      for (int i = 0; i < pValues.length; i++) {
                        double pVal = pValues[i];
                        double localMinPVal = lowestLocalPValues[i]; // smallest local p
                        if (pVal <= startStopPValue ||
                            (considerSingleComponent && localMinPVal <= combinedStartStopValue)) {
                          currentEventIndices.add(i);
                          currentLowestP = Math.min(currentLowestP, pVal);
                          currentLowestLocalP = Math.min(currentLowestLocalP, localMinPVal);
                          // check if event ended due to end of tISeries (i.e. last index reached) -> add it
                          if (i == lastIndex) {
                            if (currentLowestP <= heightPValue ||
                                (considerSingleComponent && currentLowestLocalP <= singleCompHeightPValue)) {
                              collection.add(new NpEvent(collection, currentEventIndices));
                            }
                            // reset the event indices after storing them
                            currentEventIndices.clear();
                            currentLowestP = 1;
                            currentLowestLocalP = 1;
                          }

                          // idx is no event (equals "yVal <= cutoff")
                        } else {
                          // check if event ended (i.e. there are indices stored in the list)
                          if (!currentEventIndices.isEmpty()) {
                            // System.out.println("i\t"+i+"\tthr\t"+heightThr.interpolate(i, yData.length));
                            if (currentLowestP <= heightPValue ||
                                (considerSingleComponent && currentLowestLocalP <= singleCompHeightPValue)) {
                              collection.add(new NpEvent(collection, currentEventIndices));
                            }
                            // reset the event indices after storing them
                            currentEventIndices.clear();
                            currentLowestP = 1;
                            currentLowestLocalP = 1;
                          }
                        }

                      }
                      // Precalculate the event peak index
                      collection.getNpEvents().forEach(e -> e.calcPeakIndex(yData));
                      // ################################################################################
                      // Continue with default procedure: determine BG contribution
                      // (obviously for each trace)
                      // ################################################################################
                      PopParSummary popParSummary = new PopParSummary();
                      popParSummary.add("pHeight", pSignificance, SnF.doubleToString(heightPValue,
                          NF.D1C3Exp));
                      if (considerSingleComponent) {
                        popParSummary.add("singleCompHeight", singleCompPSignificance,
                            SnF.doubleToString(singleCompHeightPValue, NF.D1C3Exp));
                      }
                      popParSummary.add("pStartStop", pStartStop, SnF.doubleToString(startStopPValue,
                          NF.D1C3));
                      popParSummary.add("MZ", preScreenMap.size() + "/" + traces.size());
                      PopulationID populationID = branch.startBranchForTrace(trace, params);
                      Population population = new NpPopulation(
                          populationID,
                          collection,
                          populationID.toString(),
                          screenStartThrMap.get(trace) != null ? screenStartThrMap.get(trace) :
                              new ThresholdSupplierInstructions(),
                          preScreenStopThrMap.get(trace) != null ? preScreenStopThrMap.get(trace) :
                              new ThresholdSupplierInstructions(),
                          preScreenHeightThrMap.get(trace) != null ? preScreenHeightThrMap.get(trace) :
                              new ThresholdSupplierInstructions(),
                          popParSummary);
                      population.setContributingMZs(preScreenMap.keySet().stream()
                          .map(Trace::getMzValue)
                          .collect(Collectors.toList()));
                      trace.addOverridePopulation(populationID, population);

                      // Net signal: do this right after the search

                      if (netSignalChoice.equals(NetCorrectionOption.INDEX_APPROXIMATION)) {
                        double[] intensities = trace.getTISeries().getIntensity();

                        int windowWidth = (int) Math.ceil(netSignalTime / trace.getTISeries().getDT());

                        // only average the background (i.e., what is not an event)
                        List<Integer> bgIndices = collection.getBackgroundIndices_v2();

                        // likely faster to use array than list
                        int[] bgIndicesArr = ArrUtils.integerListToArr(bgIndices);
                        bgIndices = null; // offer for GC

                        for (Event event : collection.getNpEvents()) {

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
                        List<Integer> bgIndices = collection.getBackgroundIndices_v2();
                        double[] bgTimes = new double[bgIndices.size()];
                        for (int i = 0; i < bgIndices.size(); i++) {
                          bgTimes[i] = time[bgIndices.get(i)];
                        }

                        for (Event event : collection.getNpEvents()) {

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

                        for (Event event : collection.getNpEvents()) {

                          if (getIsStopped().get()) {
                            break;
                          }

                          int peakIdx = event.getPeak();
                          double bgPerDT = supplier.interpolate(peakIdx, trace.getTISeries().size());
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

                }
              } else {
                LOGGER.info("Prescreening did find any interesting isotopes. Consider using a lower " +
                    "threshold.");
              }
            }
          }
          setProgress(1);
          break mainLoop;
        }
      }

    } catch (Exception e) {
      LOGGER.error(e.getMessage() + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
      setProgress(1);
    }
    setProgress(1);
    return taskResult;
  }


  public static ThresholdSupplierInstructions getSupplier(String description, ThresholdBundle bundle,
                                                          double offset) {
    ThresholdSupplierInstructions supplier;

    switch (bundle.getThresholdOption().getValue()) {
      // Note: for mean, sig = 0 is not needed internally for mean/location
      case MEAN -> supplier = new ThresholdSupplierInstructions(
          description,
          ThrFormalism.AT_LOCATION, ThrMeasureOfSignificance.FACTOR,
          0,
          offset);
      case ALPHA -> supplier = new ThresholdSupplierInstructions(
          description,
          ThrFormalism.CRITICAL_LIMIT_FORMALISM,
          ThrMeasureOfSignificance.ALPHA,
          bundle.getAlpha().getValue(),
          offset);
      case FACTOR -> supplier = new ThresholdSupplierInstructions(
          description,
          ThrFormalism.CRITICAL_LIMIT_FORMALISM,
          ThrMeasureOfSignificance.FACTOR,
          bundle.getFactor().getValue(),
          offset);
      // Note: for custom, Thr.Sig = factor is not needed internally and is ignored
      case CUSTOM_VALUE -> supplier = new ThresholdSupplierInstructions(
          description,
          ThrFormalism.STATIC_VALUE,
          ThrMeasureOfSignificance.FACTOR,
          bundle.getCustomValue().getValue(),
          offset);
      default -> {
        supplier = new ThresholdSupplierInstructions(
            "FAILED-" + description,
            ThrFormalism.STATIC_VALUE,
            ThrMeasureOfSignificance.FACTOR,
            0,
            offset);
        LOGGER.error("Unexpected option for search threshold: "
            + bundle.getThresholdOption().getValue() + ".");
      }
    }

    return supplier;
  }

}
