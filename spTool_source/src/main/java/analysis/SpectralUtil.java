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

package analysis;

import dataModelNew.Sample;
import dataModelNew.SampleFile;
import dataModelNew.TISeries;
import dataModelNew.Trace;
import io.nu.NuReader;
import io.nu.NuReaderResult;
import javafx.util.Pair;
import math.stat.MeasureOfLocation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import processing.options.SearchAlgorithm;
import sandbox.montecarlo.Isotope;
import util.Util;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class SpectralUtil {

  private static final Logger LOGGER = LogManager.getLogger(SpectralUtil.class);

  public static void computeSpectra(Sample mainSample, PopulationID popID) {

    // Check if there are spectral data already
    if (!mainSample.getSpectralData(popID).isEmpty()) {
      return;
    } else {

      // check if we can compute spectra
      boolean isAligned = popID.getSteps().stream()
          .anyMatch(step -> step instanceof PopulationStep.AlignSubtype);

      boolean isPValSearch = false;
      for (PopulationStep step : popID.getSteps()) {
        if (step instanceof PopulationStep.SearchSubtype) {
          isPValSearch = ((PopulationStep.SearchSubtype) step).getSearchAlgorithm()
              .equals(SearchAlgorithm.P_VALUE_ACCUMULATION);
          if (isPValSearch) {
            // one step is enough
            break;
          }
        }
      }

      if (isAligned || isPValSearch) {
        for (Sample sample : mainSample.getAllSamples()) {

          List<Isotope> isotopes = sample.listIsotopes();

          // Just find any isotope that matches
          for (Isotope isotope : isotopes) {
            Trace trace = sample.getTrace(isotope);
            List<Event> npEvents = sample.getNPEvents(isotope, popID);
            if (!npEvents.isEmpty() && trace != null) {

              // for the background: identify bg indices once
              int totalDP = trace.getTISeries().size();
              List<Integer> npIndices = new ArrayList<>();
              for (Event npEvent : npEvents) {
                List<Integer> indices = npEvent.getIndicesList();
                npIndices.addAll(indices);
              }
              List<Integer> bgIndices = MainEventCollection.getBGIndicesBitSet(totalDP, npIndices, (int) 1E5);

              // Case A: we can reload the whole data set
              SampleFile sampleFile = sample.getSampleFile();
              Path sampleDir = sampleFile.getFilePath();

              List<SpectralArray> result;
              if (Util.isNuPath(sampleDir)) {
                result = getSpectralDataFromNUFile(sample, isotopes, sampleDir, bgIndices, npEvents);
              } else {
                result = getSpectralData(sample, isotopes, bgIndices, npEvents);
              }
              // store result to sample
              sample.addSpectralData(popID, result);
              // We only need to find ONE non-null isotope b/c they are all aligned
              break;
            }
          }
        }
      }
    }
  }

  public static List<SpectralArray> getSpectralData(Sample subSample,
                                                    List<Isotope> isotopes,
                                                    List<Integer> bgIndices,
                                                    List<Event> npEvents) {

    List<SpectralArray> result = new ArrayList<>();

    // maintains order of isotopes
    List<Isotope> validIsotopes = new ArrayList<>();
    HashMap<Isotope, Double> meanBGData = new HashMap<>();

    // first find mean BG
    for (Isotope isotope : isotopes) {
      Trace trace = subSample.getTrace(isotope);
      if (trace == null) {
        continue;   // trace is not present (should not happen, but guard anyway)
      }
      List<Double> bgData = new ArrayList<>(bgIndices.size());
      double[] intensity = trace.getTISeries().getIntensity();
      for (Integer bgIndex : bgIndices) {
        if (intensity.length > bgIndex) {
          bgData.add(intensity[bgIndex]);
        }
      }
      double meanBG = MeasureOfLocation.MEAN.calc(bgData);
      meanBGData.put(isotope, meanBG);
      validIsotopes.add(isotope);
    }

    // Then assign NP data
    for (Isotope iso : validIsotopes) {
      double[] specArr = new double[npEvents.size()];
      double[] symmetryArr = new double[npEvents.size()];
      double[] heightToBGArr = new double[npEvents.size()];
      double[] nPoints = new double[npEvents.size()];
      HashMap<String, double[]> extraFeatures = new HashMap<>();
      extraFeatures.put("Symmetry", symmetryArr);
      extraFeatures.put("heightToBackground", heightToBGArr);
      extraFeatures.put("numberOfPoints", nPoints);


      for (int i = 0; i < specArr.length; i++) {
        Event npEvent = npEvents.get(i);
        int[] indices = npEvent.getIndices();
        double[] intensity = new double[indices.length];

        Trace trace = subSample.getTrace(iso);
        if (trace != null) {
          TISeries ser = trace.getTISeries();
          double totalNpSum = 0;
          double meanBgPerDt = meanBGData.get(iso);
          for (int j = 0; j < indices.length; j++) {
            int index = indices[j];
            // subtract mean bg per DT for each data point
            double intensityValue = ser.getIntensity()[index];
            totalNpSum += intensityValue - meanBgPerDt;
            intensity[j] = intensityValue;
          }
          specArr[i] = totalNpSum;
          ///
          int peakIdx = getPeakIdx(intensity);
          symmetryArr[i] = strictlyPositiveAsymmetryFactor(intensity, peakIdx);
          ///
          // avoid division by <1 (which inflates ratio to larger value than net height)
          double bg = Math.max(1, npEvent.getBgPerNP());
          double grossHeight = 0;
          if (peakIdx > 0) {
            grossHeight = intensity[peakIdx];
          }
          heightToBGArr[i] = grossHeight/bg;
          ///
          nPoints[i] = intensity.length;

        }
      }
      SpectralArray spec = new SpectralArray(iso, iso.getIsotopicNumber(), specArr, extraFeatures);
      result.add(spec);
    }

    return result;
  }


  public static List<SpectralArray> getSpectralDataFromNUFile(Sample subSample,
                                                              List<Isotope> isotopes, Path sampleDir,
                                                              List<Integer> bgIndices,
                                                              List<Event> npEvents) {

    List<SpectralArray> result = new ArrayList<>();


    Path nuDir = Util.getCheckedNuPathDir(sampleDir);

    // Check which MZ are there in NU folder
    try {
      NuReaderResult scan = NuReader.readAvailableMZ(nuDir);
      LOGGER.trace("Read available mz values from NU data.");
      NuReaderResult nuData = NuReader.readSelectedChannels(nuDir, scan.mzValues);
      LOGGER.trace("Read time and intensity from NU data.");

      // maintains order of isotopes
      List<Double> validMZValues = new ArrayList<>();
      HashMap<Double, Double> meanBGData = new HashMap<>();

      // first find mean BG
      for (Double mzDoubleVal : scan.mzValues) {
        TISeries ser = nuData.channelData.get(mzDoubleVal);
        if (ser == null) {
          continue;   // channel was not loaded (should not happen, but guard anyway)
        }
        List<Double> bgData = new ArrayList<>(bgIndices.size());
        for (Integer bgIndex : bgIndices) {
          if (ser.size() > bgIndex) {
            bgData.add(ser.getIntensity()[bgIndex]);
          }
        }
        double meanBG = MeasureOfLocation.MEAN.calc(bgData);
        meanBGData.put(mzDoubleVal, meanBG);
        validMZValues.add(mzDoubleVal);
      }

      LOGGER.trace("Finished parsing NU background data.");

      /*
       TODO: We should offer an option to exclude certain isotopes here!
        In case we know that there are isobaric interferences,
        e.g., Ca/Ti or in the REE region,
        I assume, the best way to go ahead is
        a) ignore all isobars
        b) define "preferred isotopes"
        One idea could be to have two versions of this function (or a boolean):
        a) show all MZ for spectrum regardless interferences (we want to see them)
        b) restricted list of MZ for cluster analysis (we could even filter downstream tbh)
       */

      // Then assign NP data
      for (Double mz : validMZValues) {
        double[] specArr = new double[npEvents.size()];
        double[] symmetryArr = new double[npEvents.size()];
        double[] heightToBGArr = new double[npEvents.size()];
        double[] nPoints = new double[npEvents.size()];

        HashMap<String, double[]> extraFeatures = new HashMap<>();
        extraFeatures.put("Symmetry", symmetryArr);
        extraFeatures.put("heightToBackground", heightToBGArr);
        extraFeatures.put("numberOfPoints", nPoints);

        for (int i = 0; i < specArr.length; i++) {
          Event npEvent = npEvents.get(i);
          int[] indices = npEvent.getIndices();
          double[] intensity = new double[indices.length];

          TISeries ser = nuData.channelData.get(mz);
          double meanBgPerDt = meanBGData.get(mz);
          double totalNpSum = 0;
          for (int j = 0; j < indices.length; j++) {
            int index = indices[j];
            // subtract mean bg per DT for each data point
            double intensityValue = ser.getIntensity()[index];
            totalNpSum += intensityValue - meanBgPerDt;
            intensity[j] = intensityValue;
          }
          specArr[i] = totalNpSum;
          ///
          int peakIdx = getPeakIdx(intensity);
          symmetryArr[i] = strictlyPositiveAsymmetryFactor(intensity, peakIdx);
          ///
          double bg = Math.max(1, npEvent.getBgPerNP());
          // avoid division by <1 (which inflates ratio to larger value than net height)
          double grossHeight = 0;
          if (peakIdx > 0) {
            grossHeight = intensity[peakIdx];
          }
          heightToBGArr[i] = grossHeight/bg;
          ///
          nPoints[i] = intensity.length;
        }
        SpectralArray spec = new SpectralArray(mz, specArr, extraFeatures);
        result.add(spec);
      }

    } catch (IOException ex) {
      LOGGER.error("Error reading nu data." +
          " Message: " + ExceptionUtils.getMessage(ex)
          + " Stack trace: " + ExceptionUtils.getStackTrace(ex));
      result = getSpectralData(subSample, isotopes, bgIndices, npEvents);
    }
    return result;
  }


  private static int getPeakIdx(double[] values) {
    int idx = -1;
    int peakIndex = 0;
    if (values.length > 0) {
      double peakValue = values[0];
      for (int i = 1; i < values.length; i++) {
        if (values[i] > peakValue) {
          peakValue = values[i];
          peakIndex = i;
        }
      }
      idx = peakIndex;
    }
    return idx;
  }

  public static double strictlyPositiveAsymmetryFactor(double[] values) {
    return strictlyPositiveAsymmetryFactor(values, null);
  }

  public static double strictlyPositiveAsymmetryFactor(double[] values, @Nullable Integer peakIdx) {

    double result = 1;

    if (values != null && values.length > 2) {

      int peakIndex;
      if (peakIdx == null) {
        peakIndex = getPeakIdx(values);
      } else {
        peakIndex = peakIdx;
      }

      if (peakIndex > 0) {
        double a = peakIndex;                          // distance from start to apex
        double b = (values.length - 1) - peakIndex;   // distance from apex to end
        result = 2d * b / (a + b);
      }

    }

    return result;
  }
}
