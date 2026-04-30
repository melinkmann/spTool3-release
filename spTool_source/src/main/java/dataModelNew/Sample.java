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
import dataModelNew.mz.Element;
import io.export.ExportSimulationEventContainer;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

  List<String> getRemovedIsotopeInfo();

  double getMeanSiaShape();

  /**
   * This method serves to create a list of isotopes present in this sample. In case of merged
   * samples, one isotope may represent several traces. However, each of these traces returns the
   * same isotope (with respect to equals() and hash()).
   */
  List<Isotope> listIsotopes();

  public default List<Element> listElements() {
    return listIsotopes().stream()
        .map(Isotope::getElement)
        .distinct()
        .collect(Collectors.toList());
  }

  List<Trace> getTraces();

  @Nullable
  Trace getTrace(Isotope isotope);

  /**
   * Returns the first trace in the list that correspond to the presented isotopes.
   */
  default List<Trace> getTrace(List<Isotope> isotopes) {
    List<Trace> trace = new ArrayList<>();
    List<Trace> traces = getTraces(isotopes);
    if (!traces.isEmpty()) {
      trace.add(traces.get(0));
    }
    return trace;
  }

  /**
   * Returns the traces that correspond to the presented isotopes.
   */
  List<Trace> getTraces(List<Isotope> isotopes);

  List<ParticlePopulationMatrix> getMatrices();

  List<SpectralArray> getSpectralData(PopulationID popID);

  @org.jetbrains.annotations.Nullable
  HacCrWrapper getHacWrapper(PopulationID popID);

  void putHacWrapper(PopulationID popID, HacCrWrapper wrapper);

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
   * @param isotope Isotope that may or may not be present in this sample.
   * @return A UNIQUE list of the corresponding simulated particle population matrices. Several
   * traces may contain the same matrix, hence the check and guarantee to return unique instances
   * only. List is empty if no match is found or if no simulated Trace is present in this sample.
   */
  List<ParticlePopulationMatrix> getMatrices(Isotope isotope);

  default List<ParticlePopulationMatrix> getMatrices(Trace trace) {
    List<ParticlePopulationMatrix> result = new ArrayList<>();
    if (trace.getMzValue().hasIsotope()) {
      result = getMatrices(trace.getMzValue().getIsotope());
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
  List<PopulationID> listPopulations(List<Isotope> isotopes);

  default boolean hasPopulation(PopulationID id, Isotope isotope) {
    List<PopulationID> availablePops = listPopulations(Arrays.asList(isotope));
    return availablePops.contains(id);
  }

  void removePopulations(List<Isotope> isotopes, PopulationID populationID);

  void removeIsotopes(List<Isotope> isotopes);

  void removeTraces(List<Trace> traces);

  void removeTrace(Trace traceToRemove, String message);

  void addTrace(Trace trace);

  void addMatrices(List<ParticlePopulationMatrix> matrices);

  void setMethod(Method method);

  Color getColor();

  void setColor(Color color);

  List<Isotope> getSampleDefaultIsotopes();

  void setSampleDefaultIsotopes(List<Isotope> sampleDefaultIsotopes);

  Method getMethod();

  ////////////////////////////// Calculations for UI //////////////////////////////////////

  /**
   * @return the "principle" sample. Normally, this means that
   */
  Sample getPrincipleSample();

  List<Sample> getAllSamples();

  double[] getData(@Nullable Isotope isotope, PopulationID populationID, EventType eventType,
                   EventParameter param, Unit unit);

  double getAerosolTEConvention(Isotope isotope);

  double getPncTEConvention(Isotope isotope);


  default double[] getData(@Nullable Isotope isotope, PopulationID populationID, EventType eventType,
                           EventParameter param) {
    return getData(isotope, populationID, eventType, param, IntensityUnit.CTS);
  }

  /**
   * Returns the highest intensity height threshold used to mark a peak height LD.
   */
  double getMaxThr(@Nullable Isotope isotope, PopulationID populationID, boolean netSignal);

  double getMaxThr(@Nullable Isotope isotope, PopulationID populationID, boolean netSignal, Unit unit);

  List<Event> getNPEvents(Isotope isotope, PopulationID popID);

  int getTotalDataPoints(Isotope isotope);

  double getRawMeanCPS(Isotope isotope);

  double getRawMedianCPS(Isotope isotope);

  double getEventRate(@Nullable Isotope isotope, PopulationID populationID);

  /**
   * For the export: get a list of "simulation results".
   *
   * @return
   */

  List<ExportSimulationEventContainer> getSimExport();

  /**
   * Not 100% accurate due to averaging, this is meant as an informative number for the main table.
   * Getting the drift factor for a single sample and a given selection of isotopes and populations.
   * The drift is an average of the mean drift across all traces and populations and the maximum
   * drift observed.
   */
  double getAverageDrift(List<Isotope> isotopes, List<PopulationID> populations);

  /**
   * Not 100% accurate due to averaging, this is meant as an informative number for the main table.
   * For a list of traces, this returns the highest number of events observed across all traces
   * (i.e., isotopes).
   */
  double getAverageNoOfEvents(List<Isotope> isotopes, List<PopulationID> populations);

  /// /////////////// Getters for the results table in the UI /////////////////////////////////

  String tabSampleName();

  String tabNickName();

  String tabSampleFolder();

  String tabFullPath();

  String tabComment();

  String tabHighlight();
  // String tabTraceMZ (Isotope isotope);

  default String tabRemovedIsotopes() {
    return String.join("_", getRemovedIsotopeInfo());
  }

  String tabDwellTime(Isotope isotope); // average

  String tabDuration(Isotope isotope); // sum

  String tabPoints(Isotope isotope); // sum

  String tabTISeriesLimits(Isotope isotope);

  String tabRawMean(Isotope isotope);

  String tabRawMeanCPS(Isotope isotope);

  String tabRawMedian(Isotope isotope);

  String tabRawMedianCPS(Isotope isotope);

  String tabRawSD(Isotope isotope);

  String tabRawMAD(Isotope isotope);

  String tabSIAShape(Isotope isotope);

  String tabMeanSIAShape(Isotope isotope);

  String tabPopName(Isotope isotope, PopulationID populationID);

  String tabPopAdditional(Isotope isotope, PopulationID populationID);

  String tabLodCts(Isotope isotope, PopulationID populationID);

  String tabLodAg(Isotope isotope, PopulationID populationID);

  String tabLodNm(Isotope isotope, PopulationID populationID);

  String tabLodAmol(Isotope isotope, PopulationID populationID);

  String tabPopNpCount(Isotope isotope, PopulationID populationID);

  String tabPNC(Isotope isotope, PopulationID populationID);

  String tabPopNpRate(Isotope isotope, PopulationID populationID);

  String tabPopNpMean(Isotope isotope, PopulationID populationID);

  String tabNpSD(Isotope isotope, PopulationID populationID);

  String tabNpMedian(Isotope isotope, PopulationID populationID);

  String tabMeanHeight(Isotope isotope, PopulationID populationID);

  String tabSdHeight(Isotope isotope, PopulationID populationID);

  String tabMeanDuration(Isotope isotope, PopulationID populationID);

  String tabSdDuration(Isotope isotope, PopulationID populationID);

  String tabMeanSize(Isotope isotope, PopulationID populationID);

  String tabSizeSD(Isotope isotope, PopulationID populationID);

  String tabMedianSize(Isotope isotope, PopulationID populationID);

  String tabMeanMass(Isotope isotope, PopulationID populationID);

  String tabMassSD(Isotope isotope, PopulationID populationID);

  String tabMedianMass(Isotope isotope, PopulationID populationID);

  String tabMeanMol(Isotope isotope, PopulationID populationID);

  String tabMolSD(Isotope isotope, PopulationID populationID);

  String tabMedianMol(Isotope isotope, PopulationID populationID);

  String tabPopNpCustomParamMean(Isotope isotope, PopulationID populationID, EventParameter par,
                                 MathMod math);

  String tabNpCustomParamSD(Isotope isotope, PopulationID populationID, EventParameter par, MathMod math);

  String tabNpCustomParamMedian(Isotope isotope, PopulationID populationID, EventParameter par, MathMod math);

  String tabPopBgMean(Isotope isotope, PopulationID populationID);

  String tabPopBgSD(Isotope isotope, PopulationID populationID);

  String tabPopBgN(Isotope isotope, PopulationID populationID);

  String tabPopDrift(Isotope isotope, PopulationID populationID);

  String tabBlnDistr(Isotope isotope, PopulationID populationID);

  String tabBlnMean(Isotope isotope, PopulationID populationID);

  String tabBlnSD(Isotope isotope, PopulationID populationID);

  String tabBlnOutlierZ(Isotope isotope, PopulationID populationID);

  String tabEquivBGConc(Isotope isotope);

  String tabSearchStart(Isotope isotope, PopulationID populationID);

  String tabSearchStop(Isotope isotope, PopulationID populationID);

  String tabSearchHeight(Isotope isotope, PopulationID populationID);

  List<String> tabGates(Isotope isotope, PopulationID populationID);

  String tabSearchStartMeta(Isotope isotope, PopulationID populationID);

  String tabSearchStopMeta(Isotope isotope, PopulationID populationID);

  String tabSearchHeightMeta(Isotope isotope, PopulationID populationID);

  List<String> tabGatesMeta(Isotope isotope, PopulationID populationID);

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

  default boolean check(Isotope i) {
    return i != null && getTrace(i) != null;
  }

  default boolean check(Isotope isotope, PopulationID popID) {
    boolean isOK = false;
    if (isotope != null) {
      Trace t = getTrace(isotope);
      if (t != null) {
        if (t.hasType(popID) && t.getPopulation(popID) != null) {
          isOK = true;
        }
      }
    }
    return isOK;
  }

}
