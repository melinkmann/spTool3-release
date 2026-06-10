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
import analysis.quant.Calibration;
import analysis.quant.Cal;
import core.SpTool3Main;
import dataModelNew.mz.*;
import io.export.ExportSimulationEventContainer;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import math.units.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.MathMod;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import sandbox.montecarlo.ParticlePopulationMatrix;
import util.NF;

import static visualizer.ResultsTable.EMPTY_CELL;

public class IncompleteSample implements Sample, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(IncompleteSample.class);

  private String nickName;
  private boolean highlighted;
  private String comment;
  private final SampleFile sampleFile;
  private final HashMap<Channel, Trace> traces;
  private Method method;
  private Cal quant;
  private Color color;
  private List<Channel> sampleDefaultChannels;
  private List<String> removedIsotopeInfo;

  private final IncompleteParticleMatrix matrix;


  // Dummy
  public IncompleteSample() {
    this.matrix = new IncompleteParticleMatrix();
    this.traces = new LinkedHashMap<>();
    this.method = new ListMethod();
    this.nickName = "No data";
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = new SampleFile();
    this.quant = new Calibration();
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
    this.sampleDefaultChannels = new ArrayList<>();
    this.removedIsotopeInfo = new ArrayList<>();
  }

  public IncompleteSample(String nickName, SampleFile sampleFile, IncompleteParticleMatrix matrix) {
    this.matrix = matrix;
    this.traces = new LinkedHashMap<>();
    this.method = new ListMethod();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = sampleFile;
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
    this.sampleDefaultChannels = new ArrayList<>();
    this.removedIsotopeInfo = new ArrayList<>();
  }

  // Deep copy
  public IncompleteSample(String nickName, boolean highlighted, String comment,
                          SampleFile sampleFile, HashMap<Channel, Trace> traces,
                          Method method,
                          Cal quant,
                          Color color,
                          IncompleteParticleMatrix matrix,
                          List<Channel> sampleDefaultChannels,
                          List<String> removedIsotopeInfo) {
    this.nickName = nickName;
    this.highlighted = highlighted;
    this.comment = comment;
    this.sampleFile = new SampleFile(sampleFile);
    this.traces = traces;
    this.method = method;
    this.quant = quant;
    this.color = color;
    this.matrix = matrix.copy();
    this.sampleDefaultChannels = new ArrayList<>(sampleDefaultChannels);
    this.removedIsotopeInfo = new ArrayList<>(removedIsotopeInfo);
  }

  @Override
  public Sample copy() {
    return copyWithoutTraces();
  }

  @Override
  public Sample copyWithoutTraces() {
    return new IncompleteSample(nickName, highlighted, comment, sampleFile,
        traces, method.getCopyWithoutFile(), quant, color, matrix.copy(), sampleDefaultChannels,
        removedIsotopeInfo);
  }


  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {
    // do nothing, there is not enough data to justify conversion and this is always in RAM
    // Default serialization
    out.defaultWriteObject();
    LOGGER.trace("Wrote to object: " + getNickName());
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // do nothing, there is not enough data to justify conversion and this is always in RAM
    // Default deserialization
    in.defaultReadObject();
    if (quant == null) {
      this.quant = new Calibration();
    }
    if (sampleDefaultChannels == null) {
      this.sampleDefaultChannels = new ArrayList<>();
    }
    if (removedIsotopeInfo == null) {
      this.removedIsotopeInfo = new ArrayList<>();
    }
    if (method == null) {
      this.method = new ListMethod();
    }
    LOGGER.trace("Read from object: " + getNickName());
  }

  @Override
  public String getNickName() {
    return nickName;
  }

  @Override
  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  @Override
  public void setHighlight(boolean highlighted) {
    this.highlighted = highlighted;
  }

  @Override
  public boolean isHighlight() {
    return highlighted;
  }

  @Override
  public String getComment() {
    return comment;
  }

  @Override
  public void setComment(String comment) {
    this.comment = comment;
  }

  public Cal getQuant() {
    return quant;
  }

  public void setCalibration(Cal quant) {
    this.quant = quant;
  }

  @Override
  public List<String> getRemovedChannelInfo() {
    return new ArrayList<>(removedIsotopeInfo);
  }

  @Override
  public double getMeanSiaShape() {
    double lowerLimit = SpTool3Main.getRunTime().getConfParams().getSiaLowerLimit().getValue();
    double upperLimit = SpTool3Main.getRunTime().getConfParams().getSiaUpperLimit().getValue();
    double sia = traces.values().stream()
        .map(Trace::getSiaShape)
        .filter(d -> d > lowerLimit)
        .filter(d -> d < upperLimit)
        .mapToDouble(Double::doubleValue)
        .average().orElse(0);
    return sia;
  }

  @Override
  public List<Trace> getTraces() {
    return Collections.unmodifiableList(new ArrayList<>(traces.values()));
  }

  @Nullable
  @Override
  public Trace getTrace(Channel channel) {
    return traces.get(channel);
  }

  /*
   * @param channels Channels that may or may not be present in this sample.
   * @return A list of the corresponding traces. List is empty if no match is found.
   */
  @Override
  public List<Trace> getTraces(List<Channel> channels) {
    List<Trace> result = new ArrayList<>();
    for (Channel channel : channels) {
      if (traces.containsKey(channel)) {
        result.add(traces.get(channel));
      }
    }
    return result;
  }

  @Override
  public List<Channel> listChannels() {
    return Collections.unmodifiableList(new ArrayList<>(traces.keySet()));
  }


  @Override
  public List<Channel> getRecordedTofRange() {
    return new ArrayList<>();
  }

  @Override
  public List<ParticlePopulationMatrix> getMatrices() {
    return new ArrayList<>();
  }

  @Override
  public @Nullable List<SpectralArray> getSpectralData(PopulationID popID) {
    return new ArrayList<>();
  }

  @Override
  @Nullable
  public HacInstructionWrapper getHacWrapper(PopulationID popID) {
    return null;
  }

  @Override
  public void putHacWrapper(PopulationID popID, HacInstructionWrapper wrapper) {
    // nada
  }

  @Override
  public void addSpectralData(PopulationID populationID, List<SpectralArray> spectralData) {
    // do nothing
  }


  @Override
  public void clearSpectralData() {
    // do nothing
  }

  @Override
  public List<ParticlePopulationMatrix> getMatrices(Channel channel) {
    return new ArrayList<>();
  }


  @Override
  public List<PopulationID> listAllPopulations() {
    List<PopulationID> pops = new ArrayList<>();
    List<Trace> traces = getTraces();
    for (Trace trace : traces) {
      for (PopulationID popID : trace.getAllPopulationsTypes()) {
        if (!pops.contains(popID)) {
          pops.add(popID);
        }
      }
    }
    return pops;
  }

  @Override
  public List<PopulationID> listPopulations(List<Channel> channels) {
    List<PopulationID> pops = new ArrayList<>();
    List<Trace> traces = getTraces(channels);
    for (Trace trace : traces) {
      for (PopulationID popID : trace.getAllPopulationsTypes()) {
        if (!pops.contains(popID)) {
          pops.add(popID);
        }
      }
    }
    return pops;
  }

  @Override
  public void removePopulations(List<Channel> channels, PopulationID populationID) {
    List<Trace> traces = getTraces(channels);
    for (Trace trace : traces) {
      trace.removePopulation(populationID);
    }
  }

  @Override
  public void removeChannels(List<Channel> channels) {
    this.traces.keySet().removeAll(channels);
  }

  @Override
  public void removeTraces(List<Trace> tracesToRemove) {
    this.traces.values().removeAll(tracesToRemove);
  }

  @Override
  public void removeTrace(Trace traceToRemove, String message) {
    this.traces.values().remove(traceToRemove);
    removedIsotopeInfo.add(message);
  }

  @Override
  public void addTrace(Trace trace) {
    traces.put(trace.getChannel(), trace);
  }

  @Override
  public void addMatrices(List<ParticlePopulationMatrix> matrices) {
    // no function in this class
  }


  public Method getMethod() {
    // essentially a "do nothing" but without risking null pointer
    return method;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  public SampleFile getSampleFile() {
    return sampleFile;
  }


  @Override
  public Color getColor() {
    return color;
  }

  @Override
  public List<Channel> getSampleDefaultChannels() {
    return new ArrayList<>(sampleDefaultChannels);
  }

  @Override
  public void setSampleDefaultChannels(List<Channel> sampleDefaultChannels) {
    this.sampleDefaultChannels.clear();
    this.sampleDefaultChannels.addAll(sampleDefaultChannels);
  }


  public void setColor(Color color) {
    this.color = color;
  }

  public IncompleteParticleMatrix getMatrix() {
    return matrix;
  }

  // only has one MZ
  public Channel getChannel() {
    if (traces.isEmpty()) {
      // dummy
      return new MZChannel();
    } else {
      List<Channel> keys = new ArrayList<>(traces.keySet());
      return traces.get(keys.get(0)).getChannel();
    }
  }

  @Override
  public Sample getPrincipleSample() {
    return this;
  }

  @Override
  public List<Sample> getAllSamples() {
    List<Sample> samples = new ArrayList<>();
    samples.add(this);
    return samples;
  }

  /// /////////////////////////// Calculations for UI //////////////////////////////////////

  @Override
  public double[] getData(Channel channel, PopulationID populationID, EventType eventType,
                          EventParameter param, Unit unit) {

    double[] data = new double[0];
    // Just return, whatever has been stored (user may load quant or external data)
    //if (IntensityUnit.CTS.equals(unit)) {
    // call getter (possibly null)
    Trace trace = getTrace(channel);
    if (trace != null) {
      data = trace.get(populationID, eventType, param);
    }
    //}
    return data;
  }

  @Override
  public double getAerosolTEConvention(Channel channel) {
    return 0;
  }

  @Override
  public double getPncTEConvention(Channel channel) {
    return 0;
  }

  @Override
  public double getMaxThr(@Nullable Channel channel, PopulationID populationID, boolean netSignal) {
    return 0;
  }

  @Override
  public double getMaxThr(@Nullable Channel channel, PopulationID populationID, boolean netSignal,
                          Unit unit) {
    return 0;
  }

  @Override
  public List<Event> getNPEvents(Channel channel, PopulationID popID) {
    return new ArrayList<>();
  }

  @Override
  public int getTotalDataPoints(Channel channel) {
    int dp = 0;
    return dp;
  }

  @Override
  public double getRawMeanCPS(Channel channel) {
    double val = 0;
    if (traces.containsKey(channel)) {
      Trace trace = traces.get(channel);
      val = trace.getTISeries().getMeanIntensity();
      val = val / trace.getTISeries().getDT();
    }
    return val;
  }


  @Override
  public double getRawMedianCPS(Channel channel) {
    double val = 0;
    if (traces.containsKey(channel)) {
      Trace trace = traces.get(channel);
      val = trace.getTISeries().getMedianIntensity();
      val = val / trace.getTISeries().getDT();
    }
    return val;
  }

  @Override
  public double getEventRate(Channel channel, PopulationID populationID) {
    double rate = 0;
    // call getter (possibly null)
    Trace trace = getTrace(channel);
    if (trace != null) {
      int nNP = trace.getNoOfEvents(populationID);
      double durationSec = trace.getTISeries().getDuration();
      rate = nNP / durationSec;
    }
    return rate;
  }

  @Override
  public List<ExportSimulationEventContainer> getSimExport() {
    return new ArrayList<>();
  }

  @Override
  public double getAverageDrift(List<Channel> channels, List<PopulationID> populations) {
    return NpPopulation.DEFAULT_DRIFT;
  }

  @Override
  public double getAverageNoOfEvents(List<Channel> channels, List<PopulationID> populations) {
    int sumEvents = 0;
    double counter = 0;

    List<Trace> tracesWithChannels = getTraces(channels);

    for (Trace trace : tracesWithChannels) {
      for (PopulationID popID : populations) {
        sumEvents += trace.getNoOfEvents(popID);
        counter++;
      }
    }
    return (int) Math.ceil(sumEvents / Math.max(1d, counter));
  }

  /// /////////////// Getters for the results table in the UI /////////////////////////////////

  @Override
  public String tabSampleName() {
    return getSampleFile().getNameWithinFile();
  }

  @Override
  public String tabNickName() {
    return getNickName();
  }

  @Override
  public String tabSampleFolder() {
    return getSampleFile().getFolder();
  }

  @Override
  public String tabFullPath() {
    return getSampleFile().getPath();
  }

  @Override
  public String tabComment() {
    return getComment();
  }

  @Override
  public String tabHighlight() {
    return isHighlight() ? "Marked" : "";
  }

  @Override
  public String tabDwellTime(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabDuration(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabPoints(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabTISeriesLimits(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabRawMean(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabRawMeanCPS(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabRawMedian(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabRawMedianCPS(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabRawSD(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabRawMAD(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabSIAShape(Channel channel) {
    String val = EMPTY_CELL;
    Trace trace = getTrace(channel);
    if (trace != null) {
      double siaShape = trace.getSiaShape();
      if (siaShape > 0) {
        val = str(siaShape, NF.D1C4);
      }
    }
    return val;
  }

  @Override
  public String tabMeanSIAShape(Channel channel) {
    return str(getMeanSiaShape(), NF.D1C4);
  }

  @Override
  public String tabPopName(Channel channel, PopulationID populationID) {
    return check(channel, populationID) ?
        getTrace(channel).getPopulation(populationID).getName() : EMPTY_CELL;
  }

  @Override
  public String tabPopAdditional(Channel channel, PopulationID populationID) {
    return check(channel, populationID) ?
        getTrace(channel).getPopulation(populationID).translateParams() : EMPTY_CELL;
  }

  @Override
  public String tabPopNpCount(Channel channel, PopulationID populationID) {
    return check(channel, populationID) ?
        str(getTrace(channel).getPopulation(populationID).getEvents().size(), NF.D1C0) : EMPTY_CELL;
  }

  @Override
  public String tabLodCts(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabLodAg(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabLodNm(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabLodAmol(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabPNC(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabPopNpRate(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabPopNpMean(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.NP, EventParameter.NET_AREA)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpSD(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.NP, EventParameter.NET_AREA)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpMedian(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(md(t.get(populationID, EventType.NP, EventParameter.NET_AREA)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabPopNpCustomParamMean(Channel channel, PopulationID populationID, EventParameter par,
                                        MathMod math) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(math.calc(t.get(populationID, EventType.NP, par))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamMedian(Channel channel, PopulationID populationID, EventParameter par,
                                       MathMod math) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(md(math.calc(t.get(populationID, EventType.NP, par))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamSD(Channel channel, PopulationID populationID, EventParameter par,
                                   MathMod math) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(math.calc(t.get(populationID, EventType.NP, par))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanHeight(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.NP, EventParameter.HEIGHT)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSdHeight(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.NP, EventParameter.HEIGHT)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanDuration(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.NP, EventParameter.DURATION)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSdDuration(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.NP, EventParameter.DURATION)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanSize(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabSizeSD(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabMedianSize(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabMeanMass(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabMassSD(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabMedianMass(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabMeanMol(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabMolSD(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabMedianMol(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabPopBgMean(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.BG, EventParameter.AREA)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabPopBgSD(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.BG, EventParameter.AREA)), NF.D1C4);
    }
    return val;
  }

  @Override
  public String tabPopBgN(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(t.get(populationID, EventType.BG, EventParameter.AREA).length, NF.D1C0);
    }
    return val;
  }

  @Override
  public String tabPopDrift(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabBlnDistr(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabBlnMean(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabBlnSD(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabBlnOutlierZ(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabEquivBGConc(Channel channel) {
    return EMPTY_CELL;
  }

  @Override
  public String tabSearchStart(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabSearchStop(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabSearchHeight(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public List<String> tabGates(Channel channel, PopulationID populationID) {
    return List.of(EMPTY_CELL);
  }

  @Override
  public String tabSearchStartMeta(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabSearchStopMeta(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public String tabSearchHeightMeta(Channel channel, PopulationID populationID) {
    return EMPTY_CELL;
  }

  @Override
  public List<String> tabGatesMeta(Channel channel, PopulationID populationID) {
    return List.of(EMPTY_CELL);
  }


}
