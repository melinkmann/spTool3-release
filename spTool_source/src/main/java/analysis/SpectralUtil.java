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

import dataModelNew.*;
import dataModelNew.mz.Channel;
import gui.dialog.notification.NotificationFactory;
import io.nu.NuReader_new;
import io.nu.ParsedNuData;
import javafx.application.Platform;
import math.stat.MeasureOfLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ArrUtils;
import util.Util;

import java.nio.file.Path;
import java.util.*;

public abstract class SpectralUtil {

  private static final Logger LOGGER = LogManager.getLogger(SpectralUtil.class);

  public static void computeSpectra(Sample mainSample, PopulationID popID) {

    // Check if there are spectral data already
    if (!mainSample.getSpectralData(popID).isEmpty()) {
      return;
    } else {

      // check if we can compute spectra
      boolean isAligned = AnalysisUtils.isAlignedOrPValOrSim(popID);

      if (isAligned) {
        for (Sample sample : mainSample.getAllSamples()) {

          List<Channel> channels = sample.listChannels();

          // Just find any isotope that matches
          for (Channel channel : channels) {
            Trace trace = sample.getTrace(channel);
            List<Event> npEvents = sample.getNPEvents(channel, popID);
            if (!npEvents.isEmpty() && trace != null) {

              // for the background: identify bg indices once
              int totalDP = trace.getTISeries().size();
              List<Integer> npIndices = new ArrayList<>();
              for (Event npEvent : npEvents) {
                List<Integer> indices = npEvent.getIndicesList();
                npIndices.addAll(indices);
              }

              int skip;
              if (totalDP > 1E6) {
                skip = 10;
              } else {
                skip = 5;
              }
              List<Integer> bgIndices = MainEventCollection.getBGIndicesBitSet(totalDP, npIndices, skip);

              // Case A: we can reload the whole data set
              SampleFile sampleFile = sample.getSampleFile();
              Path sampleDir = sampleFile.getFilePath();

              List<SpectralArray> result;
              if (Util.isNuPath(sampleDir)) {
                result = getSpectralDataFromNUFile(sample, channels, sampleDir, bgIndices, npEvents);
              } else {
                if (sampleFile.getInstrumentID().equals(InstrumentID.NU_VITESSE)) {
                  Platform.runLater(() -> NotificationFactory.openInfo("""
                      Cannot find the corresponding NU Vitesse data on you drive!
                      Please update the path of your samples!
                      The spectrum will be computed based on the imported isotopes only
                      and will thus be incomplete!"""));
                }
                result = getSpectralData(sample, channels, bgIndices, npEvents);
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
                                                    List<Channel> channels,
                                                    List<Integer> bgIndices,
                                                    List<Event> npEvents) {

    List<SpectralArray> result = new ArrayList<>();

    // maintains order of isotopes
    List<Channel> validChannels = new ArrayList<>();
    HashMap<Channel, Double> meanBGData = new HashMap<>();

    // first find mean BG
    for (Channel channel : channels) {
      Trace trace = subSample.getTrace(channel);
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
      meanBGData.put(channel, meanBG);
      validChannels.add(channel);
    }

    // Then assign NP data
    for (Channel channel : validChannels) {
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

        Trace trace = subSample.getTrace(channel);
        if (trace != null) {
          TISeries ser = trace.getTISeries();
          double totalNpSum = 0;
          double meanBgPerDt = meanBGData.get(channel);
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
          heightToBGArr[i] = grossHeight / bg;
          ///
          nPoints[i] = intensity.length;

        }
      }
      SpectralArray spec = new SpectralArray(channel, specArr, extraFeatures);
      result.add(spec);
    }

    return result;
  }


  public static List<SpectralArray> getSpectralDataFromNUFile(Sample subSample,
                                                              List<Channel> channels, Path sampleDir,
                                                              List<Integer> bgIndices,
                                                              List<Event> npEvents) {

    List<SpectralArray> result = new ArrayList<>();


    Path nuDir = Util.getCheckedNuPathDir(sampleDir);

    // Check which MZ are there in NU folder
    ParsedNuData nuData = NuReader_new.getFromCacheOrParse(nuDir, null, null, null);
    if (nuData != null && nuData.isValid()) {

      // iterator not to load all into RAM at the same time
      Iterator<ParsedNuData.ParsedNuDataArray> it = nuData.getIsotopeArrayIterator();

      // store the event indices (else we create the same array all the time)
      HashMap<Event, int[]> eventIndexBuffer = new HashMap<>();

      while (it.hasNext()) {
        ParsedNuData.ParsedNuDataArray parsedData = it.next();
        if (parsedData != null && parsedData.intensity().length > 0) {
          double[] intensityArr = parsedData.intensity();

          // If time has been cut, we need to cut here, too.
          int[] limitIndices = subSample.getTimeLimitsIndices();
          if (limitIndices.length > 1
              && limitIndices[0] != -1 && limitIndices[1] != -1
              && limitIndices[1] > limitIndices[0]) {

            int clampedEnd = Math.min(limitIndices[1], intensityArr.length - 1);
            intensityArr = ArrUtils.getPortion(intensityArr, limitIndices[0], clampedEnd, LOGGER);
          }

          // Find Mean BG
          int serSize = intensityArr.length;
          double sum = 0;
          int count = 0;
          for (int bgIndex : bgIndices) {
            if (serSize > bgIndex) {
              sum += intensityArr[bgIndex];
              count++;
            }
          }
          double meanBgPerDt = (count > 0) ? sum / count : 0.0;

          // loop over NP
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
            int[] indices = eventIndexBuffer.computeIfAbsent(npEvent, Event::getIndices);

            double[] intensity = new double[indices.length];

            double totalNpSum = 0;
            for (int j = 0; j < indices.length; j++) {
              int index = indices[j];
              // subtract mean bg per DT for each data point
              double intensityValue = intensityArr[index];
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
            heightToBGArr[i] = grossHeight / bg;
            ///
            nPoints[i] = intensity.length;
          }
          SpectralArray spec = new SpectralArray(parsedData.requestedMZ(), specArr, extraFeatures);
          result.add(spec);
        }
      }
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


  public static double strictlyPositiveAsymmetryFactor(double[] values, Integer peakIdx) {
    double result = 1;

    if (values != null && values.length > 2 && peakIdx > 0) {
      double a = peakIdx;                          // distance from start to apex
      double b = (values.length - 1) - peakIdx;   // distance from apex to end
      result = 2d * b / (a + b);
    }

    return result;
  }
}
