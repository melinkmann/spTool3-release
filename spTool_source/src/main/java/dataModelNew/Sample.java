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

package dataModelNew;

import analysis.*;
import analysis.quant.Cal;
import dataModelNew.mz.Channel;
import dataModelNew.mz.Element;
import io.export.ExportSimulationEventContainer;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import math.units.Unit;
import math.units.enums.IntensityUnit;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.MathMod;
import processing.parameterSets.Method;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.ParticlePopulationMatrix;
import util.NF;
import util.SnF;

public interface Sample extends Serializable {

  Sample copy();

  Sample copyWithoutTraces();

  SampleFile getSampleFile();

  abstract String getNickName();

  abstract void setNickName(String nickName);

  boolean isHighlight();

  void setHighlight(boolean highlighted);

  String getComment();

  void setComment(String comment);

  Cal getQuant();

  public void setCalibration(Cal quant);

  List<String> getRemovedChannelInfo();

  double getMeanSiaShape();

  void setTimeLimitIndices(int inclusiveStart, int inclusiveEnd);

  int[] getTimeLimitsIndices();

  /**
   * This method serves to create a list of channels present in this sample. In case of merged
   * samples, one channel may represent several traces. However, each of these traces returns the
   * same channel (with respect to equals() and hash()).
   */
  List<Channel> listChannels();

  List<Channel> getRecordedTofRange();

  public default List<Element> listElements() {
    return listChannels().stream()
        .map(Channel::getIsotope)
        .filter(Objects::nonNull)
        .map(Isotope::getElement)
        .distinct()
        .collect(Collectors.toList());
  }

  List<Trace> getTraces();

  @Nullable
  Trace getTrace(Channel channel);

  /**
   * Returns the first trace in the list that correspond to the presented channels.
   */
  default List<Trace> getTrace(List<Channel> channels) {
    List<Trace> trace = new ArrayList<>();
    List<Trace> traces = getTraces(channels);
    if (!traces.isEmpty()) {
      trace.add(traces.get(0));
    }
    return trace;
  }

  /**
   * Returns the traces that correspond to the presented channels.
   */
  List<Trace> getTraces(List<Channel> channels);

  List<ParticlePopulationMatrix> getMatrices();

  List<SpectralArray> getSpectralData(PopulationID popID);

  @org.jetbrains.annotations.Nullable
  HacInstructionWrapper getHacWrapper(PopulationID popID);

  void putHacWrapper(PopulationID popID, HacInstructionWrapper wrapper);

  public void addSpectralData(PopulationID populationID, List<SpectralArray> spectralData);

  default double[] getSpectralMZ(PopulationID popID) {
    List<SpectralArray> spectralData = getSpectralData(popID);
    double[] spectralMZ = new double[spectralData.size()];
    for (int i = 0; i < spectralData.size(); i++) {
      spectralMZ[i] = spectralData.get(i).getMz();
    }
    return spectralMZ;
  }

  default double[] getSpectralValues(PopulationID popID) {
    List<SpectralArray> spectralData = getSpectralData(popID);
    double[] spectralValues = new double[spectralData.size()];
    for (int i = 0; i < spectralData.size(); i++) {
      spectralValues[i] = spectralData.get(i).getMean();
    }
    return spectralValues;
  }

  default SpectralRegion getSpectralRegions(PopulationID popID) {
    return new SpectralRegion(getSpectralData(popID));
  }

  void clearSpectralData();

  /**
   * @param channel Channel that may or may not be present in this sample.
   * @return A UNIQUE list of the corresponding simulated particle population matrices. Several
   * traces may contain the same matrix, hence the check and guarantee to return unique instances
   * only. List is empty if no match is found or if no simulated Trace is present in this sample.
   */
  List<ParticlePopulationMatrix> getMatrices(Channel channel);

  default List<ParticlePopulationMatrix> getMatrices(Trace trace) {
    List<ParticlePopulationMatrix> result = new ArrayList<>();
    Channel channel = trace.getChannel();
    if (channel!=null) {
      result = getMatrices(trace.getChannel());
    }
    return result;
  }

  /**
   * This method serves to create a list of populationIDs present in this sample. In case of TOF
   * data with several traces or merged samples, one ID may occur in several cases. However, each of
   * these traces returns the same ID (with respect to equals() and hash()).
   */
  List<PopulationID> listAllPopulations();

  /**
   * This method serves to create a list of populationIDs for a given Trace present in this sample.
   * In case of TOF data with several traces or merged samples, one ID may occur in several cases.
   * However, each of these traces returns the same ID (with respect to equals() and hash()).
   */
  List<PopulationID> listPopulations(List<Channel> channels);

  default boolean hasPopulation(PopulationID id, Channel channel) {
    List<PopulationID> availablePops = listPopulations(Arrays.asList(channel));
    return availablePops.contains(id);
  }

  void removePopulations(List<Channel> channels, PopulationID populationID);

  void removeChannels(List<Channel> channels);

  void removeTraces(List<Trace> traces);

  void removeTrace(Trace traceToRemove, String message);

  void addTrace(Trace trace);

  void addMatrices(List<ParticlePopulationMatrix> matrices);

  void setMethod(Method method);

  Color getColor();

  void setColor(Color color);

  List<Channel> getSampleDefaultChannels();

  void setSampleDefaultChannels(List<Channel> sampleDefaultChannels);

  Method getMethod();

  ////////////////////////////// Calculations for UI //////////////////////////////////////

  /**
   * @return the "principle" sample. Normally, this means that
   */
  Sample getPrincipleSample();

  List<Sample> getAllSamples();

  double[] getData(@Nullable Channel channel, PopulationID populationID, EventType eventType,
                   EventParameter param, Unit unit);

  double getAerosolTEConvention(Channel channel);

  double getPncTEConvention(Channel channel);

  default double[] getData(@Nullable Channel channel, PopulationID populationID, EventType eventType,
                           EventParameter param) {
    return getData(channel, populationID, eventType, param, IntensityUnit.JUST_CTS);
  }

  /**
   * Returns the highest intensity height threshold used to mark a peak height LD.
   */
  double getMaxThr(@Nullable Channel channel, PopulationID populationID, boolean netSignal);

  double getMaxThr(@Nullable Channel channel, PopulationID populationID, boolean netSignal, Unit unit);

  List<Event> getNPEvents(Channel channel, PopulationID popID);

  int getTotalDataPoints(Channel channel);

  double getRawMeanCPS(Channel channel);

  double getRawMedianCPS(Channel channel);

  double getEventRate(@Nullable Channel channel, PopulationID populationID);

  /**
   * For the export: get a list of "simulation results".
   *
   * @return
   */

  List<ExportSimulationEventContainer> getSimExport();

  /**
   * Not 100% accurate due to averaging, this is meant as an informative number for the main table.
   * Getting the drift factor for a single sample and a given selection of channels and populations.
   * The drift is an average of the mean drift across all traces and populations and the maximum
   * drift observed.
   */
  double getAverageDrift(List<Channel> channels, List<PopulationID> populations);

  /**
   * Not 100% accurate due to averaging, this is meant as an informative number for the main table.
   * For a list of traces, this returns the highest number of events observed across all traces
   * (i.e., channels).
   */
  double getAverageNoOfEvents(List<Channel> channels, List<PopulationID> populations);

  /// /////////////// Getters for the results table in the UI /////////////////////////////////

  String tabSampleName();

  String tabNickName();

  String tabSampleFolder();

  String tabFullPath();

  String tabComment();

  String tabHighlight();
  // String tabTraceMZ (Channel channel);

  default String tabRemovedChannels() {
    return String.join("_", getRemovedChannelInfo());
  }

  String tabDwellTime(Channel channel); // average

  String tabDuration(Channel channel); // sum

  String tabPoints(Channel channel); // sum

  String tabTISeriesLimits(Channel channel);

  String tabRawMean(Channel channel);

  String tabRawMeanCPS(Channel channel);

  String tabRawMedian(Channel channel);

  String tabRawMedianCPS(Channel channel);

  String tabRawSD(Channel channel);

  String tabRawMAD(Channel channel);

  String tabSIAShape(Channel channel);

  String tabMeanSIAShape(Channel channel);

  String tabPopName(Channel channel, PopulationID populationID);

  String tabPopAdditional(Channel channel, PopulationID populationID);

  String tabLodCts(Channel channel, PopulationID populationID);

  String tabLodAg(Channel channel, PopulationID populationID);

  String tabLodNm(Channel channel, PopulationID populationID);

  String tabLodAmol(Channel channel, PopulationID populationID);

  String tabPopNpCount(Channel channel, PopulationID populationID);

  String tabPNC(Channel channel, PopulationID populationID);

  String tabPopNpRate(Channel channel, PopulationID populationID);

  String tabPopNpMean(Channel channel, PopulationID populationID);

  String tabNpSD(Channel channel, PopulationID populationID);

  String tabNpMedian(Channel channel, PopulationID populationID);

  String tabMeanHeight(Channel channel, PopulationID populationID);

  String tabSdHeight(Channel channel, PopulationID populationID);

  String tabMeanDuration(Channel channel, PopulationID populationID);

  String tabSdDuration(Channel channel, PopulationID populationID);

  String tabMeanSize(Channel channel, PopulationID populationID);

  String tabSizeSD(Channel channel, PopulationID populationID);

  String tabMedianSize(Channel channel, PopulationID populationID);

  String tabMeanMass(Channel channel, PopulationID populationID);

  String tabMassSD(Channel channel, PopulationID populationID);

  String tabMedianMass(Channel channel, PopulationID populationID);

  String tabMeanMol(Channel channel, PopulationID populationID);

  String tabMolSD(Channel channel, PopulationID populationID);

  String tabMedianMol(Channel channel, PopulationID populationID);

  String tabPopNpCustomParamMean(Channel channel, PopulationID populationID, EventParameter par,
                                 MathMod math, Unit unit);

  String tabNpCustomParamSD(Channel channel, PopulationID populationID, EventParameter par, MathMod math, Unit unit);

  String tabNpCustomParamMedian(Channel channel, PopulationID populationID, EventParameter par, MathMod math, Unit unit);

  String tabPopBgMean(Channel channel, PopulationID populationID);

  String tabPopBgSD(Channel channel, PopulationID populationID);

  String tabPopBgN(Channel channel, PopulationID populationID);

  String tabPopDrift(Channel channel, PopulationID populationID);

  String tabBlnDistr(Channel channel, PopulationID populationID);

  String tabBlnMean(Channel channel, PopulationID populationID);

  String tabBlnSD(Channel channel, PopulationID populationID);

  String tabBlnOutlierZ(Channel channel, PopulationID populationID);

  String tabEquivBGConc(Channel channel);

  String tabSearchStart(Channel channel, PopulationID populationID);

  String tabSearchStop(Channel channel, PopulationID populationID);

  String tabSearchHeight(Channel channel, PopulationID populationID);

  List<String> tabGates(Channel channel, PopulationID populationID);

  String tabSearchStartMeta(Channel channel, PopulationID populationID);

  String tabSearchStopMeta(Channel channel, PopulationID populationID);

  String tabSearchHeightMeta(Channel channel, PopulationID populationID);

  List<String> tabGatesMeta(Channel channel, PopulationID populationID);

  default String str(double d, NF nf) {
    return SnF.doubleToString(d, nf);
  }

  default String str(double d, NF nf, NF nf2) {
    return SnF.doubleToString(d, nf, nf2);
  }

  default double mu(double[] data) {
    return MeasureOfLocation.MEAN.calc(data);
  }

  default double mu(List<Double> data) {
    return MeasureOfLocation.MEAN.calc(data);
  }

  default double sd(double[] data) {
    return MeasureOfSpread.SD.calc(data);
  }

  default double md(double[] data) {
    return MeasureOfLocation.MEDIAN.calc(data);
  }

  default boolean check(Trace t) {
    return t != null;
  }

  default boolean check(Channel i) {
    return i != null && getTrace(i) != null;
  }

  default boolean check(Channel channel, PopulationID popID) {
    boolean isOK = false;
    if (channel != null) {
      Trace t = getTrace(channel);
      if (t != null) {
        if (t.hasType(popID) && t.getPopulation(popID) != null) {
          isOK = true;
        }
      }
    }
    return isOK;
  }

}
