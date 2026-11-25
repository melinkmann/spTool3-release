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

package sandbox.montecarlo;

import analysis.AnalysisUtils;
import analysis.Baseline;
import analysis.PopulationID;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.Trace;
import dataModelNew.mz.Channel;
import dataModelNew.mz.Element;
import gui.dialog.notification.NotificationFactory;
import math.stat.*;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hipparchus.linear.SparseFieldMatrix;
import org.jetbrains.annotations.Nullable;
import processing.options.*;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import processing.parameterSets.ParamBundle;
import processing.parameterSets.ParamSet;
import processing.parameterSets.action.Actions;
import processing.parameterSets.bundle.ElementBundle;
import processing.parameterSets.bundle.SiaBundle;
import processing.parameterSets.impl.MCSimGeneralParams;
import processing.parameterSets.impl.MCSimParticleParams;
import processing.parameterSets.impl.MethodDoeParams;
import processing.parameters.SpawnControlParameter;
import util.ArrUtils;
import util.NF;
import util.SnF;

import java.util.*;

public class AutofillGenerator {

  private static final Logger LOGGER = LogManager.getLogger(AutofillGenerator.class);

  @Nullable
  public static Method bestMatch(Sample sampleA, List<Sample> sampleBs, Channel channel,
                                 PopulationID populationID, @Nullable double[] weights) {

    Method method = null;
    HashMap<Sample, Double> scoreMap = new LinkedHashMap<>();

    if (weights == null || weights.length < 5) {
      weights = new double[]{0.3, 0.05, 0.20, 0.25, 0.20};
    }

    double[] areaA = sampleA.getData(channel, populationID, EventType.NP, EventParameter.NET_AREA);
    double[] heightA = sampleA.getData(channel, populationID, EventType.NP, EventParameter.NET_HEIGHT);
    double[] durationA = sampleA.getData(channel, populationID, EventType.NP, EventParameter.DURATION);
    double[] symA = sampleA.getData(channel, populationID, EventType.NP, EventParameter.ASYMMETRY_FACTOR);

    if ((areaA.length == heightA.length
        && areaA.length == durationA.length
        && areaA.length == symA.length
        && areaA.length > 0)) {

      areaA = MathMod.CUBEROOT.calc(areaA);
      heightA = MathMod.CUBEROOT.calc(heightA);
      // durationA = MathMod.CUBEROOT.calc(durationA);
      // symA = MathMod.CUBEROOT.calc(symA);

      double areaSD = MeasureOfSpread.SD.calc(areaA);
      double heightSD = MeasureOfSpread.SD.calc(heightA);
      double durationSD = MeasureOfSpread.SD.calc(durationA);
      double symSD = MeasureOfSpread.SD.calc(symA);

      if (areaSD == 0) {
        areaSD = 1;
      }
      if (heightSD == 0) {
        heightSD = 1;
      }
      if (durationSD == 0) {
        durationSD = 1;
      }
      if (symSD == 0) {
        symSD = 1;
      }

      for (Sample sampleB : sampleBs) {

        double[] areaB = sampleB.getData(channel, populationID, EventType.NP, EventParameter.NET_AREA);
        double[] heightB = sampleB.getData(channel, populationID, EventType.NP, EventParameter.NET_HEIGHT);
        double[] durationB = sampleB.getData(channel, populationID, EventType.NP, EventParameter.DURATION);
        double[] symB = sampleB.getData(channel, populationID, EventType.NP, EventParameter.ASYMMETRY_FACTOR);

        if ((areaB.length == heightB.length
            && areaB.length == durationB.length
            && areaB.length == symB.length
            && areaB.length > 0)) {

          areaB = MathMod.CUBEROOT.calc(areaB);
          heightB = MathMod.CUBEROOT.calc(heightB);
          // durationB = MathMod.CUBEROOT.calc(durationB);
          // symB = MathMod.CUBEROOT.calc(symB);

          double weightSum = 0;
          double paramSum = 0;
          int counter = 0;

          double scatterHeightArea = Double.NaN;
          try {
            scatterHeightArea = GridSimilarity.referenceScore(areaA, heightA, areaB, heightB);
            if (Double.isFinite(scatterHeightArea) && !Double.isNaN(scatterHeightArea)) {
              paramSum += scatterHeightArea * weights[0];
              weightSum += weights[0];
              counter++;
            }
          } catch (Exception e) {
            LOGGER.error("Cannot compute metric. Message: {} Details: {}",
                ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
          }

          double emdArea = Double.NaN;
          try {
            emdArea = Wasserstein1D.distance(areaA, areaB) / areaSD;
            if (Double.isFinite(emdArea) && !Double.isNaN(emdArea)) {
              paramSum += emdArea * weights[1];
              weightSum += weights[1];
              counter++;
            }
          } catch (Exception e) {
            LOGGER.error("Cannot compute metric. Message: {} Details: {}",
                ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
          }

          double emdHeight = Double.NaN;
          try {
            emdHeight = Wasserstein1D.distance(heightA, heightB) / heightSD;
            if (Double.isFinite(emdHeight) && !Double.isNaN(emdHeight)) {
              paramSum += emdHeight * weights[2];
              weightSum += weights[2];
              counter++;
            }
          } catch (Exception e) {
            LOGGER.error("Cannot compute metric. Message: {} Details: {}",
                ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
          }

          double emdDuration = Double.NaN;
          try {
            emdDuration = Wasserstein1D.distance(durationA, durationB) / durationSD;
            if (Double.isFinite(emdDuration) && !Double.isNaN(emdDuration)) {
              paramSum += emdDuration * weights[3];
              weightSum += weights[3];
              counter++;
            }
          } catch (Exception e) {
            LOGGER.error("Cannot compute metric. Message: {} Details: {}",
                ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
          }

          double emdSym = Double.NaN;
          try {
            emdSym = Wasserstein1D.distance(symA, symB) / symSD;
            if (Double.isFinite(emdSym) && !Double.isNaN(emdSym)) {
              paramSum += emdSym * weights[4];
              weightSum += weights[4];
              counter++;
            }
          } catch (Exception e) {
            LOGGER.error("Cannot compute metric. Message: {} Details: {}",
                ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
          }

          if (weightSum > 0 && counter > 3) {
            scoreMap.put(sampleB, paramSum / weightSum);
          }
        }
      }

      // add score to nickname
      List<Map.Entry<Sample, Double>> entries = new ArrayList<>(scoreMap.entrySet());
      entries.sort(Map.Entry.comparingByValue());

      int position = 0;
      for (Map.Entry<Sample, Double> entry : entries) {
        position++;
        entry.getKey().setDoeMatchScore((double) position);
      }

      if (!entries.isEmpty()) {
        Sample winner = entries.get(0).getKey();
        winner.setHighlight(true);
        method = winner.getMethod().getCopyWithoutFile();
        String comment = method.getCommentParam().getValue();
        method.getCommentParam().setValue(comment + "\nBest match from sample:\n" + winner.getNickName());

        LOGGER.info("Sample {} was best match.", winner.getNickName());

        // This may actually take a while to compute...
        // SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
        SpTool3Main.getRunTime().getMainWindowCtl().triggerSelectionResetOfSampleSetList();

        NotificationFactory.openInfo("The best method was from sample: " +
            "\n" + winner.getNickName() +
            "\n" + winner.getSampleFile().getFileName() +
            "\n" + winner.getSampleFile().getFilePath());
      }
    }

    return method;
  }


  public static void scheduleDOE(Method currentMethod, MethodDoeParams config) {

    int nRep = SpTool3Main.getRunTime().getMainWindowCtl().repetitionSpinner.getValue();

    // List<DoePlanner.Param> params = new ArrayList<>();
    // params.add(new DoePlanner.Param(3, 60)); // D                  0
    // params.add(new DoePlanner.Param(0, 20)); // SD D (max / 3)     1

    // params.add(new DoePlanner.Param(5, 25)); // v                  2
    // params.add(new DoePlanner.Param(0, 8)); // SD v (max / 3)      3

    // params.add(new DoePlanner.Param(1, 30)); // y                  4
    // params.add(new DoePlanner.Param(0, 10)); // SD y (max / 3)     5

    List<DoePlanner.Param> params = new ArrayList<>();
    params.add(new DoePlanner.Param(config.getD_mu_Start(), config.getD_mu_Stop())); // D     0
    params.add(new DoePlanner.Param(config.getD_SD_Start(), config.getD_SD_Stop())); // SD D  1

    params.add(new DoePlanner.Param(config.getV_mu_Start(), config.getV_mu_Stop())); // v     2
    params.add(new DoePlanner.Param(config.getV_SD_Start(), config.getV_SD_Stop())); // SD v  3

    params.add(new DoePlanner.Param(config.getY_mu_Start(), config.getY_mu_Stop())); // y     4
    params.add(new DoePlanner.Param(config.getY_SD_Start(), config.getY_SD_Stop())); // SD y  5

    List<DoePlanner.ParamCondition> inputs;

    if (config.getDoeSamplingModel().equals(DoeModel.LATIN_HYPERCUBE)) {
      inputs = DoePlanner.latinHypercube(params, config.getNConditions());
    } else {
      inputs = DoePlanner.sobol(params, config.getNConditions());
    }

    int counter = 0;
    List<Method> methodCopyList = new ArrayList<>();
    for (DoePlanner.ParamCondition input : inputs) {
      Method methodCopy = currentMethod.getCopyWithoutFile();
      counter++;

      for (ParamSet set : methodCopy.getSets()) {
        if (set instanceof MCSimGeneralParams) {
          MCSimGeneralParams mcSet = ((MCSimGeneralParams) set);
          StringBuilder idStr = new StringBuilder();
          idStr.append("D").append(SnF.doubleToString(input.values().get(0), NF.D1C1)).append("_");
          idStr.append("SDD").append(SnF.doubleToString(input.values().get(1), NF.D1C1)).append("_");
          idStr.append("v").append(SnF.doubleToString(input.values().get(2), NF.D1C1)).append("_");
          idStr.append("SDv").append(SnF.doubleToString(input.values().get(3), NF.D1C1)).append("_");
          idStr.append("y").append(SnF.doubleToString(input.values().get(4), NF.D1C1)).append("_");
          idStr.append("SDy").append(SnF.doubleToString(input.values().get(5), NF.D1C1));
          mcSet.sampleName.setValue(idStr.toString());
        }
        if (set instanceof MCSimParticleParams) {
          MCSimParticleParams mcSet = ((MCSimParticleParams) set);
          mcSet.v_mu.setValue(input.values().get(2));
          mcSet.v_SD.setValue(input.values().get(3));
          mcSet.y_mu.setValue(input.values().get(4));
          mcSet.y_SD.setValue(input.values().get(5));
          for (ElementBundle elementBundle : mcSet.getElementBundles()) {
            elementBundle.d_mu.setValue(input.values().get(0));
            elementBundle.d_SD.setValue(input.values().get(1));
          }
        }
      }

      methodCopy.getLabelParam().setValue("DOE step " + counter);
      methodCopyList.add(methodCopy);
    }
    Actions.executeMethodCreateBtn(methodCopyList, nRep);

  }


  public static void autofill(Method method, Sample sample, List<Channel> selChannels,
                              List<PopulationID> selPopIDs) {

    // remove previous submethods
    List<ParamSet> mcSets = new ArrayList<>();
    for (ParamSet set : method.getSets()) {
      if (set instanceof MCSimGeneralParams
          || set instanceof MCSimParticleParams) {
        mcSets.add(set);
      }
    }

    MCSimGeneralParams generalParams = new MCSimGeneralParams();

    // assume tof if more than 1 channel
    if (selChannels.size() > 1) {
      generalParams.detectorDistribution.setValue(PDF.COMPOUND_POISSON);
    }

    // global SIA stuff
    double meanSIA = sample.getFilteredMeanSiaShape();
    double lowerSIALim = SpTool3Main.getRunTime().getConfParams().getSiaLowerLimit().getValue();
    double upperSIALim = SpTool3Main.getRunTime().getConfParams().getSiaUpperLimit().getValue();
    // set general sia values via sample mean
    if (meanSIA > 0) {
      generalParams.defaultSiaShapeParameter.setValue(meanSIA);
    }
    HashMap<Element, List<Double>> elementSiaMap = new LinkedHashMap<>();
    HashMap<Element, List<Double>> elementBlnMaps = new LinkedHashMap<>();
    HashMap<Element, Double> elementBlnMap = new LinkedHashMap<>();
    Set<Element> backgroundWasApplied = new HashSet<>();

    // reset bundles to zero
    SpawnControlParameter siaSpawn = generalParams.getSiaExceptions();
    siaSpawn.clear();

    double duration = 0;
    double dwellTime = 0;

    // fill in general stuff
    boolean success = false;
    for (Channel selChannel : selChannels) {
      Trace trace = sample.getTrace(selChannel);
      Isotope isotope = selChannel.getIsotope();

      if (trace != null && isotope != null) {
        // fill in tiSeries parameters once
        if (!success) {
          generalParams.macroDT.setValue(trace.getTISeries().getDT() * 1E6);
          duration = trace.getTISeries().getDuration();
          generalParams.duration.setValue(duration);
          dwellTime = trace.getTISeries().getDT();
        }
        success = true;

        Baseline baseline = trace.getBaseline();
        if (baseline != null) {
          double[] muBlnArr = baseline.getBackgroundDistribution().getLocation();
          if (muBlnArr.length > 0) {
            double muBln = MeasureOfLocation.MEAN.calc(muBlnArr);
            if (muBln > 0) {
              elementBlnMaps.computeIfAbsent(isotope.getElement(), k -> new ArrayList<>()).add(muBln);
            }
          }
        }

        double siaShape = trace.getSiaShape();

        if (siaShape >= lowerSIALim && siaShape <= upperSIALim) {
          // OK
        } else if (meanSIA > 0) {
          siaShape = meanSIA;
        } else {
          siaShape = -1;
        }

        if (siaShape > 0) {
          elementSiaMap.computeIfAbsent(isotope.getElement(), k -> new ArrayList<>()).add(siaShape);
        }
      }
    }

    // SIA exception bundles (one per element)
    for (Element element : elementSiaMap.keySet()) {
      List<Double> siaShapes = elementSiaMap.get(element);
      if (siaShapes.isEmpty()) {
        continue;
      }
      double elementMeanSia = MeasureOfLocation.MEAN.calc(siaShapes);
      if (elementMeanSia > 0) {
        ParamBundle bundle = siaSpawn.addBundle();
        if (bundle instanceof SiaBundle) {
          ((SiaBundle) bundle).elementHeaderParameter.setValue(element);
          ((SiaBundle) bundle).siaShapeParameter.setValue(elementMeanSia);
        } else {
          LOGGER.error("AutofillPopulation: unexpected SIA bundle type: {}", bundle == null ? "null" :
              bundle.getClass());
        }
      }
    }
    siaSpawn.sortBundles();

    // BLN means (one per element)
    for (Element element : elementBlnMaps.keySet()) {
      List<Double> means = elementBlnMaps.get(element);
      if (means.isEmpty()) {
        continue;
      }
      double elementMean = MeasureOfLocation.MEAN.calc(means);
      if (elementMean > 0) {
        elementBlnMap.put(element, elementMean);
      }
    }

    // match pop with available channels
    HashMap<PopulationID, List<Channel>> popChannelMap = new LinkedHashMap<>();
    for (PopulationID selPopID : selPopIDs) {
      for (Channel selChannel : selChannels) {
        if (sample.hasPopulation(selPopID, selChannel)) {
          popChannelMap.computeIfAbsent(selPopID, k -> new ArrayList<>()).add(selChannel);
        }
      }
    }

    // only add pops which are present for the sel channels AND could actually be filled
    HashMap<PopulationID, MCSimParticleParams> popMap = new LinkedHashMap<>();

    for (PopulationID popID : popChannelMap.keySet()) {

      if (!AnalysisUtils.isAlignedOrPValOrSim(popID) && selChannels.size() != 1) {
        continue;
      }

      // isotopes are unique per population, so a map keyed by isotope is fine
      HashMap<Isotope, double[]> isotopeData = new LinkedHashMap<>();
      HashMap<Element, List<double[]>> elementDataLists = new LinkedHashMap<>();

      for (Channel popChannel : popChannelMap.get(popID)) {
        Isotope isotope = popChannel.getIsotope();
        if (isotope == null) {
          continue;
        }
        double[] cts = sample.getData(popChannel, popID, EventType.NP, EventParameter.NET_AREA);
        double[] bgArr = sample.getData(popChannel, popID, EventType.BG, EventParameter.HEIGHT);
        if (cts == null || cts.length == 0) {
          continue;
        }

        isotopeData.put(isotope, cts);
        elementDataLists.computeIfAbsent(isotope.getElement(), k -> new ArrayList<>()).add(cts);
      }

      if (isotopeData.isEmpty()) {
        continue;
      }

      // ensure all equal length
      boolean allEqual = isotopeData.values().stream().mapToInt(a -> a.length).distinct().count() == 1;
      if (!allEqual) {
        continue;
      }

      // global sum (no filtering: length stays identical to the per-element sums)
      double[] sum = ArrUtils.sumArrays(isotopeData.values());
      if (sum.length == 0) {
        continue;
      }

      MCSimParticleParams nmpSimPar = new MCSimParticleParams();
      SpawnControlParameter nmpElementBundle = nmpSimPar.getElementBundleParam();
      nmpElementBundle.clear(); // reset bundles to zero

      double meanNpSignal = MeasureOfLocation.MEAN.calc(sum);
      double sdNpSignal = MeasureOfSpread.SD.calc(sum);
      double rate = duration > 0 ? sum.length / duration : 0;

      nmpSimPar.meanEventSignal.setValue(meanNpSignal);
      nmpSimPar.eventSignalSD.setValue(sdNpSignal);
      nmpSimPar.eventRate.setValue(rate);

      // per element
      for (Element element : elementDataLists.keySet()) {
        double[] elementSum = ArrUtils.sumArrays(elementDataLists.get(element));

        // mean mass fraction; events with a zero total are skipped (avoids divide by zero)
        double fracSum = 0;
        int n = 0;
        for (int i = 0; i < sum.length; i++) {
          if (sum[i] > 0) {
            fracSum += elementSum[i] / sum[i];
            n++;
          }
        }
        double meanFrac = n > 0 ? fracSum / n : 0;

        // apply background only once per element across all populations
        double bgSum = 0;

        // Only add element if fraction (i.e., distributed mean signal)
        if (meanFrac > 0) {

          if (backgroundWasApplied.add(element)) {
            bgSum = elementBlnMap.get(element);
          }

          ParamBundle bundle = nmpElementBundle.addBundle();
          if (bundle instanceof ElementBundle) {
            ((ElementBundle) bundle).elementHeaderParameter.setValue(element);
            ((ElementBundle) bundle).massFraction.setValue(meanFrac);
            // cts?
            if (nmpSimPar.backgroundUnit.getValue().equals(BGUnit.COUNTS)) {
              ((ElementBundle) bundle).backgroundSignalIntensity.setValue(bgSum);
            } else if (dwellTime > 0) {
              ((ElementBundle) bundle).backgroundSignalIntensity.setValue(bgSum / dwellTime);
            }

          } else {
            LOGGER.error("AutofillPopulation: unexpected element bundle type: {}", bundle == null ? "null"
                : bundle.getClass());
          }
        }
      }
      nmpElementBundle.sortBundles();

      // register only now that it was successfully filled
      popMap.put(popID, nmpSimPar);
    }

    if (success && !popMap.isEmpty()) {

      // remove old methods
      method.removeSets(mcSets);

      // add new methods
      List<ParamSet> sets = new ArrayList<>();
      sets.add(generalParams);
      sets.addAll(popMap.values());
      sets.addAll(method.getSets());

      method.clearSets();
      method.addSets(sets);
    }
  }

}