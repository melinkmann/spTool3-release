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

import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.SampleFile;
import dataModelNew.SampleImpl;
import dataModelNew.TISeries;
import dataModelNew.TISeriesHDD;
import dataModelNew.TraceMC;
import dataModelNew.mz.Element;
import dataModelNew.mz.IsotopeChannel;
import dataModelNew.mz.TOFmz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.nu.ShapeEstimator;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.DeadTimeModel;
import processing.options.IsotopeConflictOption;
import processing.options.MCSimIclShapeParameters;
import processing.options.MonteCarloOscillation;
import processing.options.PDF;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameterSets.bundle.ElementBundle;
import processing.parameterSets.bundle.SiaBundle;
import processing.parameterSets.impl.ConfParams;
import processing.parameterSets.impl.MCSimGeneralParams;
import processing.parameterSets.impl.MCSimParticleParams;
import sandbox.montecarlo.Background;
import sandbox.montecarlo.DeadTimeUtil;
import sandbox.montecarlo.GettablePDF;
import sandbox.montecarlo.IndexBufferCollection;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.MacroTimeFrameUtil;
import sandbox.montecarlo.ParticleInstructions;
import sandbox.montecarlo.ParticlePopulationMatrix;
import sandbox.montecarlo.ParticlePopulationMatrixRAM;
import sandbox.montecarlo.Statistics;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;
import tasks.results.FunctionalTaskResult;
import util.ArrUtils;
import util.Util;
import visualizer.styles.Colors;
import visualizer.styles.MarkerStyle;

public class MonteCarloGeneratorTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(MonteCarloGeneratorTask.class);

  private final Method method;
  private final AtomicReference<Sample> sampleRef;

  public MonteCarloGeneratorTask(String taskName, Method method,
                                 AtomicReference<Sample> sampleRef) {
    super(taskName);
    // probably safer for multithreading environment. Also makes sure that sample gets a copy.
    this.method = method.getCopyWithoutFile();
    this.sampleRef = sampleRef;
    //
    // Make sure that the RNG is randomly seeded.
    Statistics.resetXoroSeed();
  }

  @Override
  public TaskResult call() {

    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new EmptyTaskResult();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      LOGGER.info("Data generator starts."
          + " in thread " + Thread.currentThread().getId());

      mainLoop:
      while (!getIsStopped().get()) {

        ConfParams conf = SpTool3Main.getRunTime().getConfParams();

        List<ParamSet> subMethods = method.getSets();

        // Style for plotting
        Iterator<MarkerStyle> markerIterator = MarkerStyle.getDefaultAwtIterator().iterator();
        Iterator<Colors> colorIterator = Colors.getDefaultIterator().iterator();

        // Check Monte Carlo Case
        List<ParamSet> simGeneral = subMethods.stream()
            .filter(s -> s.getEnum().equals(AvailableParameterSets.SPICPMS_MC_SIMUL))
            .collect(Collectors.toList());

        List<ParamSet> simNpPop = subMethods.stream()
            .filter(s -> s.getEnum().equals(AvailableParameterSets.SPICPMS_MC_SIMUL_NP_POPULATION))
            .collect(Collectors.toList());

        if (!simGeneral.isEmpty() && !simNpPop.isEmpty()) {

          // Cast to the actual instances
          ParamSet paramSet = simGeneral.get(0);
          if (paramSet instanceof MCSimGeneralParams) {
            MCSimGeneralParams generalParams = (MCSimGeneralParams) paramSet;

            List<MCSimParticleParams> simParticlePopulations = simNpPop.stream()
                .filter(p -> p instanceof MCSimParticleParams)
                .map(p -> (MCSimParticleParams) p)
                .collect(Collectors.toList());

            // Clarify sample name
            String sampleName;
            String sampleNamePar = generalParams.sampleName.getValue();
            if (sampleNamePar.replace(" ", "").isEmpty()) {
              sampleName = method.getLabelParam().getValue();
            } else {
              sampleName = sampleNamePar;
            }
            String innerSampleName = Util.dateToString() + " " + sampleName;

            final Sample sample = new SampleImpl(sampleName, new SampleFile(innerSampleName),
                method);

            // Extract the instructions for the simulation
            final double microDTSec = generalParams.microDT.getValue() * 1E-6;
            double macroDTSec = generalParams.macroDT.getValue() * 1E-6;
            double durationSec = generalParams.duration.getValue();

            // evenly spaced peaks?
            boolean fixPeakDistance = generalParams.evenPeakSpacing.getValue();
            double estimPeakWidth = generalParams.estimatedPeakWidth.getValue() / 1000;

            // If we allow e.g. 90 µs and 20 µs, we will get an index mismatch at the end when
            // combining data to the macro frame.
            if (macroDTSec % microDTSec != 0) {
              double oldMacroDT = macroDTSec;
              int intDiv = (int) (macroDTSec / microDTSec);
              macroDTSec = (intDiv) * microDTSec;
              if (oldMacroDT < macroDTSec) {
                LOGGER.trace("Extended dwell time by one grid unit.");
                macroDTSec = (intDiv + 1) * microDTSec;
              }
              LOGGER.warn("Dwell time was not fully divisible by grid time."
                  + " Grid time = " + microDTSec
                  + " and macro dwell time = " + oldMacroDT
                  + ". Instead, a new dwell time was calculated: " + macroDTSec
                  + ". It is possible, that the 'inequality'"
                  + " is only an artifact due to binary to decimal number conversion."
                  + " In that case, this message can be ignored.");
            }

              /*
              Important: We must limit the particle appearance to the macro frame.
              Otherwise, we may obtain cases where particle peaks occur in the minuscule gap between
              that largest possible macro DT and the final time stamp, and the largest possible micro DT
              and the final time stamp. What do I mean?
              Assume DT = 15 µs and micro = 1 µs, duration = 35 µs.
              In macro frame, we have 15 30.
              In micro frame, we have 1,2, ... 30,31,32,34,35.
              As we start with the micro frame and only later group it to the macro frame, there is a chance
              to allow NP events to occur in the window 30-35 when the lastTimeStamp is taken from micro
              frame.
              If events occur in this region, which later is cut out during grouping stage,
              we have NP events whose peak time is after the end of the sample.
              Why? The grouping truncates the data!
              When later we try to integrate these peaks for histograms or export (in retrospect),
              we get "array out of bounds" when exporting the event markers,
              and we get "bad integration limits" during integration as start>stop...
               */
            int intDiv = (int) (durationSec / macroDTSec);
            durationSec = macroDTSec * intDiv;

            // TIME FRAME
            double[] time = ArrUtils.fillArrayExclusive(0, durationSec, microDTSec);
            double lastTimeStamp = time[time.length - 1];
            int totalTimeFrameLength = time.length;

            // DILUTION
            double dilutionFactor = generalParams.dilutionFactor.getValue();
            boolean applyDilution = generalParams.applyDilution.getValue();
            if (dilutionFactor <= 0) {
              LOGGER.error("Cannot process dilution factor <= 0! Factor=" + dilutionFactor);
              dilutionFactor = 1;
              applyDilution = false;
            }

            // DETECTOR
            final double deadTimeSec = generalParams.deadTime.getValue() * 1E-9;
            final DeadTimeModel deadTimeModel = generalParams.deadTimeModel.getValue();

            PDF signalPdfModel = generalParams.detectorDistribution.getValue();

            // sia settings
            double siaShape = generalParams.defaultSiaShapeParameter.getValue();
            HashMap<Element, Double> siaShapeParameters = new HashMap<>();
            List<SiaBundle> siaExceptions = generalParams.getSiaBundles();
            for (SiaBundle siaBundle : siaExceptions) {
              Element siaCaseElement = siaBundle.elementHeaderParameter.getValue().unwrap();
              double siaCaseShapePar = siaBundle.siaShapeParameter.getValue();
              siaShapeParameters.put(siaCaseElement, siaCaseShapePar);
            }

            /*
             Isotope conflicts: Statistically, I assume it will not be the same effect to
             just "add" the isotopes after resampling. This likely changes the spread.
             In the instrument, it is probably more similar to sampling the same ion beam.
             */
            IsotopeConflictOption isoConflictOpt = generalParams.isotopeConflictOption.getValue();
            List<Isotope> isotopes = new ArrayList<>();
            for (MCSimParticleParams simParticlePopulation : simParticlePopulations) {
              for (ElementBundle elementBundle : simParticlePopulation.getElementBundles()) {
                Element element = elementBundle.elementHeaderParameter.getValue().unwrap();
                isotopes.addAll(element.getIsotopes());
              }
            }

            HashMap<Integer, List<Isotope>> isoConflictMap = new LinkedHashMap<>();
            // Only search for conflicts if we actually need it
            if (isoConflictOpt.equals(IsotopeConflictOption.COMBINE)) {
              for (Isotope isotope : isotopes) {
                int nominalMass = isotope.getIsotopicNumber();
                if (isoConflictMap.containsKey(nominalMass)) {
                  List<Isotope> conflictingIsotopes = isoConflictMap.get(nominalMass);
                  // do not add the same isotope twice or else you will double its signal and resample twice!
                  if (!conflictingIsotopes.contains(isotope)) {
                    conflictingIsotopes.add(isotope);
                  }
                } else {
                  List<Isotope> confIso = new ArrayList<>();
                  confIso.add(isotope);
                  isoConflictMap.put(nominalMass, confIso);
                }
              }
              isoConflictMap.keySet().removeIf(isoNum -> isoConflictMap.get(isoNum).size() < 2);
            }

            // BACKGROUND
            final MonteCarloOscillation oscillation = generalParams.oscillation.getValue();
            double bgPeriod = generalParams.oscillationPeriod.getValue();
            double bgAmplitude = generalParams.oscillationAmplitude.getValue();
            double bgExpRampFactor = generalParams.expRampFactor.getValue();

            double flickerCoefficient = generalParams.flickerNoiseCoefficient.getValue();
            double shotCoefficient = generalParams.shotNoiseCoefficient.getValue();

            //////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////

            // Now go through the available particle populations and retrieve the data
            HashMap<Element, Double> bgIntensities = new HashMap<>();
            List<ParticleInstructions> npPopInstructionList = new ArrayList<>();

            for (MCSimParticleParams npPopParams : simParticlePopulations) {

              // General parameters
              String label = npPopParams.getLabelParameter().getValue();
              double eventRate = npPopParams.eventRate.getValue();
              if (applyDilution) {
                eventRate = eventRate / dilutionFactor;
              }
              double meanEventSignal = npPopParams.meanEventSignal.getValue();
              double eventSignalSD = npPopParams.eventSignalSD.getValue();
              double paretoScale = npPopParams.paretoScale.getValue();
              double paretoShape = npPopParams.paretoShape.getValue();

              GettablePDF massDist = npPopParams.particleMassDistribution.getValue();
              double minimumPeakHeightExpDistr = npPopParams.minimumEventSignal.getValue();

              double elementFractionUncertainty = npPopParams.elementFractionUncertainty.getValue();
              double peakDelayUncertainty = npPopParams.peakDelayUncertainty.getValue();
              double isotopicFracUncertainty = npPopParams.isotopicFractionUncertainty.getValue();

              GettablePDF chosen_pdf = npPopParams.y_Distribution.getValue();
              double chosen_v_mu;
              double chosen_v_SD;
              double chosen_y_mu;
              double chosen_y_SD;

              // Peak properties that are non-element specific but global
              MCSimIclShapeParameters iclShapeParameterSet = npPopParams.peakShape.getValue();
              if (iclShapeParameterSet.equals(MCSimIclShapeParameters.DEFAULT)) {
                double default_v_mu = conf.getDefault_v_mu().getValue();
                double default_v_SD = conf.getDefault_v_SD().getValue();
                double default_y_mu = conf.getDefault_y_mu().getValue();
                double default_y_SD = conf.getDefault_y_SD().getValue();

                chosen_v_mu = default_v_mu;
                chosen_v_SD = default_v_SD;
                chosen_y_mu = default_y_mu / 1E3;
                chosen_y_SD = default_y_SD / 1E3;
              } else {
                chosen_v_mu = npPopParams.v_mu.getValue();
                chosen_v_SD = npPopParams.v_SD.getValue();
                chosen_y_mu = npPopParams.y_mu.getValue() / 1E3;
                chosen_y_SD = npPopParams.y_SD.getValue() / 1E3;
              }

              // Find the most abundant isotope and use this as the reference for the least signal
              List<ElementBundle> elementBundles = npPopParams.getElementBundles();
              // The number "minimumPeakHeightExp" will be compared against the area for the whole NP,
              // i.e., we have to make it bigger by the number of elements and isotopes.
              double mostAbuIsoFrac = 0;
              for (ElementBundle elementBundle : elementBundles) {
                Element element = elementBundle.elementHeaderParameter.getValue().unwrap();
                for (Isotope isotope : element.getIsotopes()) {
                  if (isotope.getAbundance() > mostAbuIsoFrac) {
                    mostAbuIsoFrac = isotope.getAbundance();
                  }
                }
              }
              minimumPeakHeightExpDistr =
                  minimumPeakHeightExpDistr * elementBundles.size() / mostAbuIsoFrac;

              // new instructions for each population
              ParticleInstructions particlePopulationInstruction = new ParticleInstructions(
                  label,
                  colorIterator.next(),
                  markerIterator.next(),
                  eventRate,
                  fixPeakDistance,
                  estimPeakWidth,
                  lastTimeStamp,
                  microDTSec,
                  meanEventSignal,
                  eventSignalSD,
                  paretoScale,
                  paretoShape,
                  massDist,
                  minimumPeakHeightExpDistr,
                  chosen_v_mu,
                  chosen_v_SD,
                  chosen_y_mu,
                  chosen_y_SD,
                  chosen_pdf,
                  peakDelayUncertainty,
                  elementFractionUncertainty,
                  isotopicFracUncertainty);

              // Element-specific parameters
              elementBundles = npPopParams.getElementBundles();
              for (ElementBundle elementBundle : elementBundles) {
                // Parse the element
                Element element = elementBundle.elementHeaderParameter.getValue().unwrap();

                // BG
                double muBG = elementBundle.backgroundSignalIntensity.getValue();
                if (applyDilution) {
                  muBG = muBG / dilutionFactor;
                }
                if (bgIntensities.containsKey(element)) {
                  bgIntensities.put(element, bgIntensities.get(element) + muBG);
                } else {
                  bgIntensities.put(element, muBG);
                }

                // Particle properties
                double signalFraction = elementBundle.massFraction.getValue();
                double peakDelay = elementBundle.peakDelay.getValue();

                // Peak properties
                double chosen_d_mu;
                double chosen_d_SD;

                // Peak properties that are element-specific
                MCSimIclShapeParameters peakShapeDiffusion = elementBundle.peakShape.getValue();
                if (peakShapeDiffusion.equals(MCSimIclShapeParameters.DEFAULT)) {
                  double default_D_mu = conf.getDefault_D_mu().getValue();
                  double default_D_SD = conf.getDefault_D_SD().getValue();

                  chosen_d_mu = default_D_mu / 1E4;
                  chosen_d_SD = default_D_SD / 1E4;

                } else {
                  chosen_d_mu = elementBundle.d_mu.getValue() / 1E4;
                  chosen_d_SD = elementBundle.d_SD.getValue() / 1E4;
                }

                particlePopulationInstruction.addElement(
                    element,
                    signalFraction,
                    chosen_d_mu,
                    chosen_d_SD,
                    peakDelay);
              }
              // register the population
              npPopInstructionList.add(particlePopulationInstruction);
            }

            // ********************************************************************************
            // ********************************************************************************
            // **************************** Execute! ******************************************
            // ********************************************************************************
            // ********************************************************************************

            List<ParticlePopulationMatrix> particleMatrices = new ArrayList<>();

            // Time is not needed as micro frame anymore and can thus free some RAM.
            time = MacroTimeFrameUtil.groupTime(microDTSec, macroDTSec, time);

            // Data from all particles
            HashMap<Isotope, IndexBufferCollection> globalPeakData = new LinkedHashMap<>();

            // Data from all backgrounds
            HashMap<Isotope, Double> bgLevels = new LinkedHashMap<>();
            for (Element element : bgIntensities.keySet()) {
              bgLevels.putAll(element.calcIsotopeSignalLvl(bgIntensities.get(element)));
            }

            // Create random particle data for each particle population
            int progressCounter = 0;
            for (ParticleInstructions population : npPopInstructionList) {

              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              // Check stopping condition.
              if (getIsStopped().get()) {
                break mainLoop;
              }
              // Estimate progress.
              progressCounter++;
              setProgress(0.20 * progressCounter / (double) npPopInstructionList.size());
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////

              // Make the matrix for each population. Note that in this process,
              // we draw the random numbers for the events.
              // The matrix is stored on the HDD in a buffer directly after creation to keep the
              // RAM free if several populations with large number of NP are simulated.
              ParticlePopulationMatrix matrix = population.simulateParticleMatrix();
              particleMatrices.add(matrix);
            }

            // INTEGRATE each particle population
            progressCounter = 0;
            for (ParticlePopulationMatrix populationMatrix : particleMatrices) {

              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              // Check stopping condition.
              if (getIsStopped().get()) {
                break mainLoop;
              }
              // Estimate progress.
              progressCounter++;
              setProgress(0.20 + 0.6 * progressCounter / (double) npPopInstructionList.size());
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////

              // Get a RAM instance from the drive
              ParticlePopulationMatrixRAM ramMatrix = populationMatrix.getNewRamInstance();
              // Integrate all peaks in the ramMatrix, for all isotopes
              HashMap<Isotope, IndexBufferCollection> peakData = ramMatrix.integratePeaks(
                  microDTSec,
                  lastTimeStamp,
                  totalTimeFrameLength,
                  getIsStopped());

              // Check if another population had the same isotope, combine the index-based data in one buffer.
              for (Isotope isotope : peakData.keySet()) {
                LOGGER.trace("Adding to indexed buffer " + isotope.getNumberAndElement() + ".");
                if (globalPeakData.containsKey(isotope)) {
                  IndexBufferCollection bufferedIndexData = peakData.get(isotope);
                  globalPeakData.get(isotope).addData(bufferedIndexData);
                } else {
                  globalPeakData.put(isotope, peakData.get(isotope));
                }
              }
              ramMatrix = null; // does this help to enable gc on this instance?
            }

            // Evaluate for global data, i.e., the mix from all populations

            // First, we find and store all cases where isotopes have been merged
            HashMap<Isotope, double[]> resolvedSignals = new HashMap<>();
            if (isoConflictOpt.equals(IsotopeConflictOption.COMBINE)) {
              // Iterate over the conflicts that have been found
              for (Integer isoNumber : isoConflictMap.keySet()) {
                LOGGER.trace("Resolving isobaric conflict for nominal mass " + isoNumber + ".");

                // Initialize as null.
                double[] mergedSignal = null;

                // For each isotope of equal number, merge.
                for (Isotope isotope : isoConflictMap.get(isoNumber)) {
                  /////////////////////////////////////////////////////////////////////////
                  /////////////////////////////////////////////////////////////////////////
                  /////////////////////////////////////////////////////////////////////////
                  // Check stopping condition.
                  if (getIsStopped().get()) {
                    break mainLoop;
                  }
                  /////////////////////////////////////////////////////////////////////////
                  /////////////////////////////////////////////////////////////////////////
                  /////////////////////////////////////////////////////////////////////////

                  IndexBufferCollection values = globalPeakData.get(isotope);
                  double macroBgLevel = bgLevels.get(isotope);

                  double[] signal = preRandomIsotopeData(
                      isotope,
                      totalTimeFrameLength,
                      macroBgLevel,
                      values,
                      macroDTSec, microDTSec,
                      oscillation, bgPeriod, bgAmplitude, bgExpRampFactor);

                  // If merger already contains data, add more. Else, initialize merger.
                  if (mergedSignal == null) {
                    mergedSignal = signal;
                  } else {
                    ArrUtils.addOverriding(mergedSignal, signal);
                  }
                }

                // Now, pretend each of these isotopes has the same data
                for (Isotope isotope : isoConflictMap.get(isoNumber)) {
                  resolvedSignals.put(isotope, mergedSignal);
                }
              }
            }

            progressCounter = 0;
            // We must store the same randomization for each conflict, else it looks weird.
            HashMap<Integer, double[]> randomizedResolvedSignals = new HashMap<>();
            for (Isotope isotope : globalPeakData.keySet()) {

              LOGGER.trace("Building global time frame for " + isotope.getNumberAndElement() + ".");
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              // Check stopping condition.
              if (getIsStopped().get()) {
                break mainLoop;
              }
              // Estimate progress.
              progressCounter++;
              setProgress(0.80 + 0.2 * progressCounter / (double) globalPeakData.keySet().size());
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////
              /////////////////////////////////////////////////////////////////////////

              IndexBufferCollection values = globalPeakData.get(isotope);
              double macroBgLevel = bgLevels.get(isotope);

              /*
              Two cases:
              A. the signal is the sum of conflicting isotopes.
              B. the signal has not been summed, hence no entry in the map, hence: calc freshly
               */
              double[] signal;
              if (resolvedSignals.containsKey(isotope)) {
                signal = resolvedSignals.get(isotope);
                resolvedSignals.remove(isotope); // offer to GC. isotope should only be there once.
              } else {
                signal = preRandomIsotopeData(
                    isotope,
                    totalTimeFrameLength,
                    macroBgLevel,
                    values,
                    macroDTSec, microDTSec,
                    oscillation, bgPeriod, bgAmplitude, bgExpRampFactor);
              }

              if (randomizedResolvedSignals.containsKey(isotope.getIsotopicNumber())) {
                signal = randomizedResolvedSignals.get(isotope.getIsotopicNumber());
              } else {

                // Before transforming to macro array: Apply Dead Time.
                DeadTimeUtil.applyDeadTime(
                    signal,
                    deadTimeSec,
                    deadTimeModel,
                    microDTSec);

                LOGGER.trace(" ... global time frame for " + isotope.getNumberAndElement()
                    + " applied dead time.");

                // After dead time -> now we are in the regime of measurable ICP-MS (DT > 1 µs)
                signal = MacroTimeFrameUtil.groupSignal(microDTSec, macroDTSec, signal);

                LOGGER.trace(" ... global time frame for " + isotope.getNumberAndElement()
                    + " grouped the signal.");

                // Resample: Assume the data represent true expected values of ion flow, resampling
                // transforms them into a random Poisson process.

              /*
              Idea: Resample the Peaks now in order to guarantee that the flickering in the tail is
              present. Why? I am not sure how "adding" a BLN of ,e.g., 500 would affect the resampling.
              The math-function used here generates continuous data but the particles in fact
              release countable ions, i.e., resampling here to make a quantized signal out of
              the constant signal seems perfect.
              */

                // Switch the first step of distribution modelling
                switch (signalPdfModel) {
                  case POISSON, COMPOUND_POISSON -> {
                    Statistics.resamplePoissonDynamically(signal);
                  }
                  case POISSON_OVERDISPERSED, COMPOUND_POISSON_OVERDISPERSED -> {
                    Statistics.resampleOverdispersedPoisson(
                        signal,
                        shotCoefficient,
                        flickerCoefficient);
                  }
                  default -> {
                    // keep as is
                  }
                }
                LOGGER.trace(" ... global frame for " + isotope.getNumberAndElement() + ": resampled " +
                    "Poisson.");

                // Switch 2: Check for Compound Poisson
                switch (signalPdfModel) {
                  case COMPOUND_POISSON, COMPOUND_POISSON_OVERDISPERSED -> {
                    if (siaShapeParameters.containsKey(isotope.getElement())) {
                      siaShape = siaShapeParameters.get(isotope.getElement());
                    }

                    // estimate which algorithm to use via the mean background
                    double cutoffMean = 500;
                    if (macroBgLevel > cutoffMean) {
                      Statistics.resampleCompoundPoissonFastest(signal, siaShape);
                    } else {
                      Statistics.resampleCompoundPoissonFast(signal, siaShape);
                    }
                  }
                  default -> {
                    // keep as is
                  }
                }
                LOGGER.trace(" ... global frame for " + isotope.getNumberAndElement() + ": Compound Poisson" +
                    ".");

                // if this is a conflicting isotope, we must store its result to keep same randomization
                // for its instances
                if (isoConflictMap.containsKey(isotope.getIsotopicNumber())) {
                  randomizedResolvedSignals.put(isotope.getIsotopicNumber(), signal);
                }
              }

              /*
               Check for the resulting apparent BG signal: thr -> if NP signal is below 0.1, we consider
               the peak is over.
               Note that a thr=0.5 will bias the mean to be rather approx. +0.5 than the true value.
               Note that we perform this here for the BG since for a sine BG, we do not know the outcome.
               I think, for the particles, we do not need this.
               */
              int blockSize = (int) (macroDTSec / microDTSec);
              double empiricalBG = values.getEmpiricalBG(signal, blockSize, 0.01);

              LOGGER.trace(" ... global time frame for " + isotope.getNumberAndElement()
                  + " estimated the background signal without NP.");

              // Create the actual " trace in the sample" for the UI.
              TISeries tiSeries = new TISeriesHDD(time, signal);
              // Estimate SIA for the giggles
              double empiricalShape = ShapeEstimator.computeShape(signal);

              TraceMC trace = new TraceMC(
                  sample,
                  new IsotopeChannel(isotope),
                  tiSeries,
                  macroDTSec,
                  empiricalShape);

              trace.setEmpiricalMeanBG(empiricalBG);
              trace.setExpectedMeanBG(macroBgLevel);
              sample.addTrace(trace);
            }

            // store the matrices in the sample
            sample.addMatrices(particleMatrices);

            // set the result for the following processing steps
            sampleRef.set(sample);

            // Replace the dummy (empty) result with a result that creates a sample:
            taskResult = new FunctionalTaskResult(() -> {
              SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(sample);
            });
          }
        }
        break;
      }
      // END ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
    } catch (Exception e) {
      LOGGER.error(e.getMessage() + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }
    return taskResult;
  }

  public double[] preRandomIsotopeData(Isotope isotope, int totalTimeFrameLength,
                                       double macroBgLevel,
                                       IndexBufferCollection values,
                                       double macroDTSec, double microDTSec,
                                       MonteCarloOscillation oscillation, double bgPeriod,
                                       double bgAmplitude,
                                       double bgRise) {

    double[] signal = new double[totalTimeFrameLength];

    // bg level is given for macro scale --> calc what it is at micro scale
    double microBgLevel = macroBgLevel * microDTSec / macroDTSec;

    // Put particle data on the time frame
    values.transferToSignalArray(signal);

    // Before transforming to macro array or applying dead time: Add Background.
    if (oscillation.equals(MonteCarloOscillation.SINE)) {
      Background.addSineBackgroundFast(
          microBgLevel,
          signal,
          bgPeriod,
          bgAmplitude,
          microDTSec);
    } else if (oscillation.equals(MonteCarloOscillation.EXP)) {
      Background.addExponentialBackground(signal, microBgLevel, bgRise);
    } else if (oscillation.equals(MonteCarloOscillation.LINE)) {
      Background.addLinearBackground(signal, microBgLevel, bgRise);
    } else {
      Background.addSteadyBackground(microBgLevel, signal);
    }

    LOGGER.trace(" ... global time frame for " + isotope.getNumberAndElement()
        + " processed background modulation.");

    return signal;
  }


}
