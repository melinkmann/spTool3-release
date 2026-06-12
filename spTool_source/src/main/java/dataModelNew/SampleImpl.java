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
import analysis.quant.Quantity;
import core.SpTool3Main;
import dataModelNew.mz.*;
import io.export.ExportSimulationEventContainer;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import io.nu.NuReader_new;
import math.stat.DriftFactor;
import math.units.Unit;
import math.units.enums.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import processing.options.*;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import processing.parameterSets.impl.ExperimentalSubConditions;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.ParticlePopulationMatrix;
import sandbox.montecarlo.ParticlePopulationMatrixHDD;
import sandbox.montecarlo.ParticlePopulationMatrixRAM;
import sandbox.montecarlo.PeakFunction;
import util.ArrUtils;
import util.NF;
import util.SnF;

import static visualizer.ResultsTable.EMPTY_CELL;

public class SampleImpl implements Sample, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(SampleImpl.class);

  private String nickName;
  private boolean highlighted;
  private String comment;
  private final SampleFile sampleFile;
  private final HashMap<Channel, Trace> traces;
  private final List<ParticlePopulationMatrix> matrices;
  private HashMap<PopulationID, List<SpectralArray>> spectra;
  // transient version to store HAC results for faster access
  private transient HashMap<PopulationID, SoftReference<HacInstructionWrapper>> hacWrapper;
  private Method method;
  private Cal quant;
  private Color color;
  private List<Channel> sampleDefaultIsotopes;
  private List<String> removedIsotopeInfo;
  private List<Channel> recordedTofRange;
  // remember time limits for TOF data
  private int inclusiveStartIndex = -1;
  private int inclusiveEndIndex = -1;

  public SampleImpl() {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.spectra = new HashMap<>();
    this.hacWrapper = new HashMap<>();
    this.nickName = "No data";
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = new SampleFile();
    this.method = new ListMethod();
    this.quant = new Calibration();
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
    this.sampleDefaultIsotopes = new ArrayList<>();
    this.removedIsotopeInfo = new ArrayList<>();
    this.recordedTofRange = new ArrayList<>();
  }

  public SampleImpl(String nickName) {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.spectra = new HashMap<>();
    this.hacWrapper = new HashMap<>();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = new SampleFile();
    this.method = new ListMethod();
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
    this.sampleDefaultIsotopes = new ArrayList<>();
    this.removedIsotopeInfo = new ArrayList<>();
    this.recordedTofRange = new ArrayList<>();
  }

  // CSV
  public SampleImpl(String nickName, SampleFile sampleFile) {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.spectra = new HashMap<>();
    this.hacWrapper = new HashMap<>();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = sampleFile;
    this.method = new ListMethod();
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
    this.sampleDefaultIsotopes = new ArrayList<>();
    this.removedIsotopeInfo = new ArrayList<>();
    this.recordedTofRange = new ArrayList<>();
  }

  // NU
  public SampleImpl(String nickName, SampleFile sampleFile, List<Channel> recordedTofRange) {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.spectra = new HashMap<>();
    this.hacWrapper = new HashMap<>();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = sampleFile;
    this.method = new ListMethod();
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
    this.sampleDefaultIsotopes = new ArrayList<>();
    this.removedIsotopeInfo = new ArrayList<>();
    this.recordedTofRange = new ArrayList<>(recordedTofRange);
  }

  public SampleImpl(String nickName, SampleFile sampleFile, Method method) {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.spectra = new HashMap<>();
    this.hacWrapper = new HashMap<>();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = sampleFile;
    this.method = method;
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
    this.sampleDefaultIsotopes = new ArrayList<>();
    this.removedIsotopeInfo = new ArrayList<>();
    this.recordedTofRange = new ArrayList<>();
  }

  // Deep copy
  public SampleImpl(String nickName, boolean highlighted, String comment,
                    SampleFile sampleFile, HashMap<Channel, Trace> traces,
                    List<ParticlePopulationMatrix> matrices,
                    HashMap<PopulationID, List<SpectralArray>> spectra,
                    Method method,
                    Cal quant,
                    Color color,
                    List<Channel> sampleDefaultIsotopes,
                    List<String> removedIsotopeInfo,
                    List<Channel> recordedTofRange,
                    int inclusiveStartIndex,
                    int inclusiveEndIndex) {
    this.nickName = nickName;
    this.matrices = new ArrayList<>();
    for (ParticlePopulationMatrix matrix : matrices) {
      this.matrices.add(matrix.copy());
    }
    this.spectra = new HashMap<>();
    for (PopulationID popID : spectra.keySet()) {
      List<SpectralArray> allArr = spectra.get(popID);

      List<SpectralArray> copies = new ArrayList<>();
      for (SpectralArray spectralArray : allArr) {
        SpectralArray copyArr = spectralArray.copy();
        copies.add(copyArr);
      }

      this.spectra.put(new PopulationID(popID), copies);
    }
    this.hacWrapper = new HashMap<>();
    this.highlighted = highlighted;
    this.comment = comment;
    this.sampleFile = new SampleFile(sampleFile);
    this.traces = new LinkedHashMap<>();
    traces.values().forEach(t -> this.traces.put(t.getChannel(), t.copy(this)));
    this.method = method.getCopyWithoutFile();
    this.quant = quant.copy();
    this.color = color;
    this.sampleDefaultIsotopes = new ArrayList<>(sampleDefaultIsotopes);
    this.removedIsotopeInfo = new ArrayList<>(removedIsotopeInfo);
    this.recordedTofRange = new ArrayList<>(recordedTofRange);
    this.inclusiveStartIndex = inclusiveStartIndex;
    this.inclusiveEndIndex = inclusiveEndIndex;
  }

  public Sample copy() {
    Sample newSample = new SampleImpl(
        nickName,
        highlighted,
        comment,
        new SampleFile(sampleFile),
        traces,
        matrices,
        spectra,
        method,
        quant,
        color,
        sampleDefaultIsotopes,
        removedIsotopeInfo,
        recordedTofRange,
        inclusiveStartIndex,
        inclusiveEndIndex);
    return newSample;
  }

  public Sample copyWithoutTraces() {
    Sample newSample = new SampleImpl(
        nickName,
        highlighted,
        comment,
        new SampleFile(sampleFile),
        new LinkedHashMap<>(),
        new ArrayList<>(),
        new HashMap<>(),
        method,
        // I assume, here we would have to requantify as after merging q is not defined clearly
        new Calibration(),
        color,
        sampleDefaultIsotopes,
        removedIsotopeInfo,
        recordedTofRange,
        inclusiveStartIndex,
        inclusiveEndIndex);
    return newSample;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////

  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {
    System.gc(); // Suggest JVM to clean up
    try {
      Thread.sleep(100); // Give GC a little time (optional)
    } catch (InterruptedException e) {
      LOGGER.trace("Cannot sleep: " + ExceptionUtils.getStackTrace(e));
    }

    // Load data into RAM
    matricesToRAM();

    // Default serialization
    out.defaultWriteObject();

    // Offload data back to HDD
    matricesToHDD();
    LOGGER.trace("Wrote to object: " + getNickName());
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // Default deserialization: issue is that sometimes there is not enough memory for deserialization
    System.gc(); // Suggest JVM to clean up
    try {
      Thread.sleep(100); // Give GC a little time (optional)
    } catch (InterruptedException e) {
      LOGGER.trace("Cannot sleep: " + ExceptionUtils.getStackTrace(e));
    }

    in.defaultReadObject();

    // backwards compatibility
    if (quant == null) {
      this.quant = new Calibration();
    }

    if (sampleDefaultIsotopes == null) {
      this.sampleDefaultIsotopes = new ArrayList<>();
    }

    if (removedIsotopeInfo == null) {
      this.removedIsotopeInfo = new ArrayList<>();
    }

    if (spectra == null) {
      this.spectra = new HashMap<>();
    }

    if (hacWrapper == null) {
      this.hacWrapper = new HashMap<>();
    }

    if (recordedTofRange == null) {
      this.recordedTofRange = new ArrayList<>();
    }

    if (inclusiveStartIndex == 0 && inclusiveEndIndex == 0) {
      // indicate that both never have been set
      inclusiveStartIndex = -1;
      inclusiveEndIndex = -1;
    }


    // Offload to HDD immediately after reading
    matricesToHDD();
    LOGGER.trace("Read from object: " + getNickName());
  }

  // Traces are converted within Trace implementations
  public void matricesToRAM() {
    for (int i = 0; i < matrices.size(); i++) {
      ParticlePopulationMatrix matrix = matrices.get(i);
      if (matrix instanceof ParticlePopulationMatrixHDD) {
        matrices.set(i, new ParticlePopulationMatrixRAM(matrices.get(i)));
      }
    }
  }

  // Traces are converted within Trace implementations
  public void matricesToHDD() {
    for (int i = 0; i < matrices.size(); i++) {
      ParticlePopulationMatrix matrix = matrices.get(i);
      if (matrix instanceof ParticlePopulationMatrixRAM) {
        matrices.set(i, new ParticlePopulationMatrixHDD(matrices.get(i)));
      }
    }
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////


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
  public void setTimeLimitIndices(int inclusiveStart, int inclusiveEnd) {
    this.inclusiveStartIndex = inclusiveStart;
    this.inclusiveEndIndex = inclusiveEnd;
  }

  @Override
  public int[] getTimeLimitsIndices() {
    return new int[]{inclusiveStartIndex, inclusiveEndIndex};
  }

  @Override
  public List<Trace> getTraces() {
    List<Trace> tcs = new ArrayList<>(this.traces.values());
    tcs.sort(Comparator.comparingDouble(o -> o.getChannel().getMZ()));
    return tcs;
  }

  @Nullable
  @Override
  public Trace getTrace(Channel channel) {
    if (channel == null) {
      return null;
    }
    return traces.get(channel);
  }

  /**
   * @param channels Channels/Isotopes that may or may not be present in this sample.
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
    // unmodifiable
    return List.copyOf(traces.keySet());
  }

  public List<Channel> getRecordedTofRange() {
    if (recordedTofRange.isEmpty()) {
      if (sampleFile.getInstrumentID().equals(InstrumentID.NU_VITESSE)) {
        LOGGER.trace("Did not find any stored information on available isotopes in sample." +
            " Try read from file...");
        Path directory = sampleFile.getFilePath();
        List<Double> availableMZ = NuReader_new.listAvailableMZFromCacheOrParse(directory);
        if (!availableMZ.isEmpty()) {
          for (Double empirMZ : availableMZ) {
            MSID msID = new MSIDImpl(new MZImpl(empirMZ));
            int roundedNominalIsotopicNumber = (int) Math.round(empirMZ);
            List<Isotope> matches = Isotope.getFromNominalMass(roundedNominalIsotopicNumber);
            for (Isotope match : matches) {
              recordedTofRange.add(new MZChannel(msID, match));
            }
          }
          LOGGER.trace("Read available isotopes in sample from file.");

        } else {
          LOGGER.info("Unable to read isotopes from file: " + directory);
        }
      }
    }
    return recordedTofRange;
  }

  /**
   * @param channel Isotope that may or may not be present in this sample.
   * @return A UNIQUE list of the corresponding simulated particle population matrices. Several
   * traces may contain the same matrix, hence the check and guarantee to return unique instances
   * only. List is empty if no match is found or if no simulated Trace is present in this sample.
   */
  @Override
  public List<ParticlePopulationMatrix> getMatrices(Channel channel) {
    List<ParticlePopulationMatrix> result = new ArrayList<>();

    Isotope isotope = channel.getIsotope();
    if (isotope != null) {
      for (ParticlePopulationMatrix particleMatrix : this.matrices) {
        // add if present but ensure uniqueness!
        if (particleMatrix.hasIsotope(isotope) && !result.contains(particleMatrix)) {
          result.add(particleMatrix);
        }
      }
    }
    return result;
  }

  @Override
  public List<ParticlePopulationMatrix> getMatrices() {
    return matrices;
  }


  @Override
  public List<SpectralArray> getSpectralData(PopulationID popID) {
    List<SpectralArray> result = spectra.get(popID);
    if (result == null) {
      result = new ArrayList<>();
    } else {
      result = new ArrayList<>(result);
    }
    return result;
  }


  @Override
  @Nullable
  public HacInstructionWrapper getHacWrapper(PopulationID popID) {
    HacInstructionWrapper wrapper = null;
    SoftReference<HacInstructionWrapper> wrapperRef = hacWrapper.get(popID);
    if (wrapperRef != null) {
      wrapper = wrapperRef.get();
    }
    return wrapper;
  }

  @Override
  public void putHacWrapper(PopulationID popID, HacInstructionWrapper wrapper) {
    this.hacWrapper.put(popID, new SoftReference<>(wrapper));
  }


  @Override
  public void addSpectralData(PopulationID populationID, List<SpectralArray> spectralData) {
    spectra.put(populationID, spectralData);
  }

  @Override
  public void clearSpectralData() {
    spectra.clear();
    hacWrapper.clear();
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
    spectra.remove(populationID);
    hacWrapper.remove(populationID);
  }

  @Override
  public void removeChannels(List<Channel> channels) {
    // remove trace itself
    this.traces.keySet().removeAll(channels);
    //    // remove from spectra too [feels unexpected as we do not see these in the UI]
    //    for (PopulationID popID : spectra.keySet()) {
    //      List<SpectralArray> spec = spectra.get(popID);
    //      spec.removeIf(sarr -> isotopes.contains(sarr.getIsotope()));
    //    }
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
    this.matrices.addAll(matrices);
  }

  public SampleFile getSampleFile() {
    return sampleFile;
  }

  public Method getMethod() {
    return method;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  @Override
  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  @Override
  public List<Channel> getSampleDefaultChannels() {
    return new ArrayList<>(sampleDefaultIsotopes);
  }

  @Override
  public void setSampleDefaultChannels(List<Channel> sampleDefaultChannels) {
    this.sampleDefaultIsotopes.clear();
    this.sampleDefaultIsotopes.addAll(sampleDefaultChannels);
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
    // call getter (possibly null)
    Trace trace = getTrace(channel);
    if (trace != null) {
      // cts (or duration, ...) data
      data = trace.get(populationID, eventType, param);
      // conversion
      double[] quantData = applyQuant(data, channel, param, unit);

      // quant data is empty if unable to quantify
      if (quantData.length < data.length) {
        if (IntensityUnit.CTS.equals(unit)) {
          data = data; // indicates that we request cts and return these
        } else {
          // indicate that data is not quantified by passing empty list
          data = new double[0];
        }
      } else {
        data = quantData;
      }
    }
    return data;
  }

  /**
   * Uses the event parameter stored in the Experiment. This getter works, e.g., for the LOD calculation in
   * the table where no event parameter input is needed. We need to pass the parameter as an argument to
   * check if quantification can be calculated or not.
   * ### Returns empty array if cannot quantify!
   */
  public double[] applyQuant(double[] dataInput, Channel channel, Unit unit) {
    return applyQuant(
        dataInput,
        channel,
        quant.getExperimentalConditions().getEventPar(),
        unit);
  }


  /// ### Returns empty array if cannot quantify!
  public double[] applyQuant(double[] dataInput, Channel channel, EventParameter param, Unit unit) {

    double[] result = new double[0];

    // isotope is null if none present
    CalChannel calChannel = AnalysisUtils.getCalChannel(channel);

    // Check: request = cts || EventParameter is duration,...  || EventParameter is not what was used to
    // quantify?  --> do nothing and return cts
    if (calChannel == null
        || IntensityUnit.CTS.equals(unit)
        || !EventParameter.canQuantify(param)
        || !quant.getExperimentalConditions().getEventPar().equals(param)) {
      //------ do nothing: return the copy of the data-------
      // result = ArrUtils.copy(dataInput);

      // return empty array
      result = new double[0];

      // Else: check if sample instance is set as sample
    } else if (quant.getExperimentalConditions().getCalibratorRole().getValue().equals(CalibratorRole.SAMPLE)) {

      // Copy o.g. array
      result = ArrUtils.copy(dataInput);

      // find the response
      double ctsPerFg = 0;

      // ########################## APPLY PACE-style QUANT ###########################################
      switch (quant.getCalibrationStrategy()) {
        // ########################## REFERENCE PARTICLE with known mass ###############################
        case MASS -> {
          Quantity npResp = quant.getResponses().getOrCreateNpResponse(calChannel);

          if (npResp != null && npResp.getValue() > 0) {
            ctsPerFg = npResp.getUnit().convert(npResp.getValue(), SensitivityUnit.CTS_PER_FEMTOGRAM);
          } else {
            // no valid quantification data --> invalidate the result array
            result = new double[0];
          }
          break;
        }

        // ########################## FREQUENCY or SIZE method ###########################################
        case SIZE_METHOD, FREQUENCY_METHOD -> {
          Quantity ionResp = quant.getResponses().getOrCreateIonicResponse(calChannel);

          // check if TE was determined elsewhere
          double tePct = getAerosolTEConvention(channel);

          if (tePct > 0 && ionResp != null && ionResp.getValue() > 0) {
            ctsPerFg = ionResp.getUnit().convert(ionResp.getValue(), SensitivityUnit.CTS_PER_FEMTOGRAM);
            // we get more cts for NP as no losses (divide by TE)
            ctsPerFg = ctsPerFg / (tePct / 100d);
          } else {
            // no valid quantification data --> invalidate the result array
            result = new double[0];
          }
          break;
        }
      }

      // ########################## FIND TARGET UNIT (above we calculated cts/fg)############################
      ExperimentalSubConditions subPar =
          quant.getExperimentalConditions().getElementSpecificQuantParams().get(calChannel.element());

      if (subPar != null && ctsPerFg > 0) {

        // ########################## TARGET: DIAMETER ###########################################
        if ((unit.equals(SizeUnit.NANO_METER) || unit.equals(SizeUnit.MICRO_METER))) {

          if (subPar.getNpQuantificationApproach().getValue().equals(ParticleQuantApproach.ESD)) {
            double density = subPar.getNpDensity().getValue();
            density = subPar.getNpDensityUnit().convert(density, DensityUnit.GRAM_PER_CM3);
            double massFrac = subPar.getNpMassFraction().getValue();

            if (density > 0 && massFrac > 0) {
              for (int i = 0; i < result.length; i++) {
                double counts = result[i]; // result is a copy of the input data and carries cts intensity

                // counts -> mass (fg)
                double massFg = counts / ctsPerFg;

                /*
                 adjust mass: Why? Example: Polystyrene.
                 Density is based on the material PS. Mass from ICP is for C only.
                 Hence, the whole NP should be more massive as we need to add mass of H to get mass of PS.
                 */
                massFg = massFg / massFrac;

                // µg -> g
                double massGrams = massFg * 1e-15;

                // mass -> volume (cm^3)
                double volumeCm3 = massGrams / density;

                // volume -> diameter (cm)
                double diameterCm = Math.cbrt((6.0 * volumeCm3) / Math.PI);

                if (unit.equals(SizeUnit.NANO_METER)) {
                  // cm -> nm
                  result[i] = diameterCm * 1e7;
                } else {
                  // cm -> µm
                  result[i] = diameterCm * 1e4;
                }
              }
              // density or mass frac are <= 0
            } else {
              result = new double[0];
            }
            // subparameter set is not ESD
          } else {
            result = new double[0];
          }

          // ########################## TARGET: MASS or MOL ###########################################
        } else {
          double ctsPerTargetUnit;
          if (unit.equals(MassUnit.FEMTO_GRAM)) {
            ctsPerTargetUnit = ctsPerFg;
          } else if (unit.equals(MassUnit.ATTO_GRAM)) {
            ctsPerTargetUnit = ctsPerFg / 1E3; // fewer counts per attogram --> smaller number
          } else if (unit.equals(MassUnit.PICO_GRAM)) {
            ctsPerTargetUnit = ctsPerFg * 1E3; // more counts per pg --> larger number
          } else if (unit.equals(MolarUnit.FEMTO_MOL)) {
            // molar mass g/mol == fg/fmol
            double gPerMol = calChannel.element().calcMolarMass();
            ctsPerTargetUnit = ctsPerFg * gPerMol; // cts/fg * fg/fmol --> cts/fmol
          } else {
            // attogram
            // molar mass g/mol == fg/fmol
            double gPerMol = calChannel.element().calcMolarMass();
            ctsPerTargetUnit = ctsPerFg * gPerMol / 1E3; // cts/fg * fg/fmol / 1E3 --> cts/amol
          }

          // divide cts array by "cts/target" --> target array
          result = ArrUtils.divide(result, ctsPerTargetUnit);
        }

        // subparameter was null or sensitivity was zero
      } else {
        result = new double[0];
      }

      // sample instance is not a "sample" role
    } else {
      result = new double[0];
    }

    return result;
  }

  public double[] revertQuant(double[] quantifiedInput, Channel channel, EventParameter param,
                              Unit unit) {

    // double[] result = ArrUtils.copy(quantifiedInput);
    double[] result = new double[0];

    // isotope is null if none present
    CalChannel calChannel = AnalysisUtils.getCalChannel(channel);

    // Same early exit logic as applyQuant
    if (calChannel == null
        || IntensityUnit.CTS.equals(unit)
        || !EventParameter.canQuantify(param)
        || !quant.getExperimentalConditions().getEventPar().equals(param)) {
      return result;
    }

    if (!quant.getExperimentalConditions().getCalibratorRole().getValue().equals(CalibratorRole.SAMPLE)) {
      return new double[0];
    }

    double ctsPerFg = 0;

    // --- Recompute sensitivity exactly as in applyQuant ---
    switch (quant.getCalibrationStrategy()) {

      case MASS -> {
        Quantity npResp = quant.getResponses().getOrCreateNpResponse(calChannel);
        if (npResp != null && npResp.getValue() > 0) {
          ctsPerFg = npResp.getUnit().convert(npResp.getValue(), SensitivityUnit.CTS_PER_FEMTOGRAM);
        } else {
          return new double[0];
        }
      }

      case SIZE_METHOD, FREQUENCY_METHOD -> {
        Quantity ionResp = quant.getResponses().getOrCreateIonicResponse(calChannel);
        double tePct = getAerosolTEConvention(channel);

        if (tePct > 0 && ionResp != null && ionResp.getValue() > 0) {
          double ionicCtsPerFg = ionResp.getUnit().convert(ionResp.getValue(),
              SensitivityUnit.CTS_PER_FEMTOGRAM);
          ctsPerFg = ionicCtsPerFg / (tePct / 100d);
        } else {
          return new double[0];
        }
      }
    }

    ExperimentalSubConditions subPar = quant.getExperimentalConditions()
        .getElementSpecificQuantParams()
        .get(calChannel.element());

    if (subPar == null || ctsPerFg <= 0) {
      return new double[0];
    }

    // ===================== SOURCE: DIAMETER =====================
    if (unit.equals(SizeUnit.NANO_METER) || unit.equals(SizeUnit.MICRO_METER)) {

      if (!subPar.getNpQuantificationApproach()
          .getValue()
          .equals(ParticleQuantApproach.ESD)) {
        return new double[0];
      }

      double density = subPar.getNpDensityUnit().convert(subPar.getNpDensity().getValue(),
          DensityUnit.GRAM_PER_CM3);
      double massFrac = subPar.getNpMassFraction().getValue();

      if (density <= 0 || massFrac <= 0) {
        return new double[0];
      }

      for (int i = 0; i < result.length; i++) {

        double diameter = result[i];

        // convert diameter to cm
        double diameterCm;
        if (unit.equals(SizeUnit.NANO_METER)) {
          diameterCm = diameter / 1e7;
        } else {
          diameterCm = diameter / 1e4;
        }
        // diameter → volume (cm³)
        double volumeCm3 = (Math.PI / 6.0) * Math.pow(diameterCm, 3);

        // volume → mass (g)
        double massGrams = volumeCm3 * density;

        // g → fg
        double massFg = massGrams / 1e-15;

        // apply mass fraction correction
        massFg = massFg * massFrac;

        // mass → counts
        result[i] = massFg * ctsPerFg;
      }

    } else {
      // ########################## SOURCE: MASS or MOL ###########################################
      double ctsPerTargetUnit;

      if (unit.equals(MassUnit.FEMTO_GRAM)) {
        ctsPerTargetUnit = ctsPerFg;

      } else if (unit.equals(MassUnit.ATTO_GRAM)) {
        ctsPerTargetUnit = ctsPerFg / 1E3; // fewer cts per ag

      } else if (unit.equals(MassUnit.PICO_GRAM)) {
        ctsPerTargetUnit = ctsPerFg * 1E3; // more cts per fg

      } else if (unit.equals(MolarUnit.FEMTO_MOL)) {
        double gPerMol = calChannel.element().calcMolarMass();
        ctsPerTargetUnit = ctsPerFg * gPerMol; // g/mol = fg/fmol

      } else { // amol
        double gPerMol = calChannel.element().calcMolarMass();
        ctsPerTargetUnit = ctsPerFg * gPerMol / 1E3; // fewer cts per ag --> convert to fg as g/mol=fg/fmol
      }

      // multiply to get cts = unitValue * cts/unit
      result = ArrUtils.multiply(result, ctsPerTargetUnit);
    }

    return result;
  }

  @Override
  public double getAerosolTEConvention(Channel channel) {
    double tePct = 0;
    // isotope is null if none present
    CalChannel calChannel = AnalysisUtils.getCalChannel(channel);
    if (calChannel != null) {

      Quantity te = quant.getResponses().getOrCreateAerosolTE(calChannel);
      // no TE data for the isotope
      if (te != null && te.getValue() <= 0) {
        List<CalChannel> calChannels = quant.listChannels();
        for (CalChannel calCh : calChannels) {
          Quantity testTE = quant.getResponses().getOrCreateAerosolTE(calCh);
          if (testTE != null && testTE.getValue() > 0) {
            tePct = testTE.getValue();
            break;
          }
        }

        // isotope has specific TE data
      } else if (te != null) {
        tePct = te.getValue();
      }
    }
    return tePct;
  }

  @Override
  public double getPncTEConvention(Channel channel) {
    double tePct = 0;

    // isotope is null if none present
    CalChannel calChannel = AnalysisUtils.getCalChannel(channel);
    if (calChannel != null) {

      Quantity te = quant.getResponses().getOrCreateParticleNumberTE(calChannel);
      // no TE data for the isotope
      if (te != null && te.getValue() <= 0) {
        List<CalChannel> calChannels = quant.listChannels();
        for (CalChannel calCh : calChannels) {
          Quantity testTE = quant.getResponses().getOrCreateParticleNumberTE(calCh);
          if (testTE != null && testTE.getValue() > 0) {
            tePct = testTE.getValue();
            break;
          }
        }
        // isotope has specific TE data
      } else if (te != null) {
        tePct = te.getValue();
      }

      // final check
      if (tePct <= 0) {
        tePct = getAerosolTEConvention(channel);
      }
    }
    return tePct;
  }

  @Override
  public double getMaxThr(@Nullable Channel channel, PopulationID populationID, boolean netSignal) {
    double maxThr = 0;
    // call getter (possibly null)
    Trace trace = getTrace(channel);
    if (trace != null && trace.getBaseline() != null && trace.getBaseline().hasBaseline()) {
      // Search height
      StatCollection blnStats = trace.getBaseline().getBackgroundDistribution();
      Population population = trace.getPopulation(populationID);
      if (population != null) {
        ThresholdSupplier supplier = population.getHeightInstructions().get(blnStats);
        maxThr = Math.max(maxThr, mu(supplier.getThresholdSlices()));
        // Gate height
        List<ThresholdSupplier> gateSuppliers = population.getGatingInstr().stream()
            /*
            Changed this to "any intensity". Why? In the limit, at LOD, convention is that
            peak has just one data point, i.e., height=area, anyway.
            In addition, for LODs, it makes no sense to set threshold but not use it.
            Note that this justifies distinction between ROI and Gate.
             */
            .filter(ThresholdSupplierInstructions::isIntensity)
            .map(gInstr -> gInstr.get(blnStats))
            .toList();
        for (ThresholdSupplier suppl : gateSuppliers) {
          maxThr = Math.max(maxThr, mu(suppl.getThresholdSlices()));
        }
        if (netSignal) {
          maxThr = maxThr - mu(blnStats.getLocation());
        }
      }
    }
    return maxThr;
  }

  @Override
  public double getMaxThr(@Nullable Channel channel, PopulationID populationID, boolean netSignal,
                          Unit unit) {
    double thr = getMaxThr(channel, populationID, netSignal);
    double[] qThr = applyQuant(new double[]{thr}, channel, EventParameter.NET_AREA, unit);
    return qThr.length > 0 ? qThr[0] : 0;
  }

  @Override
  public List<Event> getNPEvents(Channel channel, PopulationID popID) {
    List<Event> npEvents = new ArrayList<>();
    Trace trace = getTrace(channel);
    if (trace != null) {
      Population pop = trace.getPopulation(popID);
      if (pop != null) {
        npEvents.addAll(pop.getEvents().getNpEvents());
      }
    }
    return npEvents;
  }

  @Override
  public int getTotalDataPoints(Channel channel) {
    int dp = 0;
    Trace trace = getTrace(channel);
    if (trace != null) {
      dp = trace.getTISeries().size();
    }
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
    List<ExportSimulationEventContainer> containers = new ArrayList<>();

    for (ParticlePopulationMatrix matrix : matrices) {
      ExportSimulationEventContainer container = new ExportSimulationEventContainer();
      if (!matrices.isEmpty()) {
        containers.add(container);
        container.setHasSimData(true);

        ParticlePopulationMatrixRAM p = matrix.getNewRamInstance();

        // Transfer data from the population to the container

        // for each event
        container.getnEvents().add(p.getNumberOfEvents());
        container.getLabels().add(p.getLabel());

        double[] v = p.getPlasmaVelocities();
        double[] yPosArr = p.getYPositions();

        container.getVelocity().addAll(ArrUtils.arrToList(v));
        container.getYPos().addAll(ArrUtils.arrToList(yPosArr));

        // now parse by element
        List<Element> elements = p.listElements();

        for (Element element : elements) {
          container.getPeakTime().put(element, p.getArrivalTimeMap().get(element));

          double[] d = p.getPlasmaDiffusionDMap().get(element);
          container.getDiffCoeff().put(element, d);

          if (v.length == yPosArr.length && v.length == d.length) {
            // compute ICL and FWHM
            double[] icl = new double[d.length];
            double[] fwhm = new double[d.length];

            for (int i = 0; i < v.length; i++) {
              PeakFunction pf = new PeakFunction(1, d[i], yPosArr[i], v[i]);
              icl[i] = pf.getIcl();
              fwhm[i] = pf.getFwhm();
            }

            container.getIcl().put(element, icl);
            container.getFwhm().put(element, fwhm);
          }

          // Isotope specifics
          for (Isotope isotope : element.getIsotopes()) {
            container.getRandNetSignal().put(isotope, p.getIntensityMap().get(isotope));

            HashMap<EventParameter, double[]> dataMap
                = AnalysisUtils.getFromSimulation(this, isotope, matrix);
            container.getDataMap().put(isotope, dataMap);
          }
        }
      }
    }
    return containers;
  }

  @Override
  public double getAverageDrift(List<Channel> channels, List<PopulationID> populations) {
    double sumDrift = 0;
    double maxDrift = 0;
    int counter = 0;

    List<Trace> tracesWithIsotopes = getTraces(channels);

    for (Trace trace : tracesWithIsotopes) {
      for (PopulationID popID : populations) {
        // Note: hasType() excludes the simulated populations, which do not have BG data anyway.
        if (trace.hasType(popID) && trace.getPopulation(popID) != null) {

          //-2 indicates that the drift has never been calculated for the trace
          double df = trace.getPopulation(popID).getDrift();
          if (df == NpPopulation.DEFAULT_DRIFT) {
            df = DriftFactor.calculateDriftFactor(trace, popID);
            trace.getPopulation(popID).setDrift(df);
          }

          // Only add positive drift, or else "NpPopulation.DEFAULT_DRIFT" indicators for "sth went wrong)
          // feigns smaller, i.e., better drift
          if (df > 0) {
            sumDrift += df;
            maxDrift = Math.max(maxDrift, df);
            counter++;
          }
        }
      }
    }
    double meanDrift = sumDrift / Math.max(1, counter);
    double combinedDrift = 0.5 * (maxDrift + meanDrift);
    return combinedDrift;
  }

  @Override
  public double getAverageNoOfEvents(List<Channel> channels, List<PopulationID> populations) {
    int sumEvents = 0;
    double counter = 0;

    List<Trace> tracesWithIsotopes = getTraces(channels);

    for (Trace trace : tracesWithIsotopes) {
      for (PopulationID popID : populations) {
        // if check is necessary. Else, pops that are simply not present reduce average no of events
        if (trace.hasType(popID)) {
          sumEvents += trace.getNoOfEvents(popID);
          counter++;
        }
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
    Trace t = getTrace(channel);
    return check(t) ? str(t.getTISeries().getDT() * 1E6, NF.D1C2) : EMPTY_CELL;
  }

  @Override
  public String tabDuration(Channel channel) {
    Trace t = getTrace(channel);
    return check(t) ? str(t.getTISeries().getDuration(), NF.D1C2) : EMPTY_CELL;
  }

  @Override
  public String tabPoints(Channel channel) {
    Trace t = getTrace(channel);
    return check(t) ? str(t.getTISeries().size(), NF.D1C0) : EMPTY_CELL;
  }

  @Override
  public String tabTISeriesLimits(Channel channel) {
    String val = "";
    if (check(channel)) {
      Trace t = getTrace(channel);
      val = t.hasLimits() ?
          "Time: " + str(t.getTISeries().getFirstTimeStamp(), NF.D1C1)
              + "-" + str(t.getTISeries().getLastTimeStamp(), NF.D1C1) :
          str(t.getTISeries().getFirstTimeStamp(), NF.D1C1)
              + "-" + str(t.getTISeries().getLastTimeStamp(), NF.D1C1);
    }
    return val;
  }

  @Override
  public String tabRawMean(Channel channel) {
    Trace trace = getTrace(channel);
    String val = trace != null ? str(trace.getTISeries().getMeanIntensity(), NF.D1C2) : EMPTY_CELL;
    return val;
  }

  @Override
  public String tabRawMeanCPS(Channel channel) {
    String val = EMPTY_CELL;
    Trace trace = getTrace(channel);
    if (trace != null) {
      double dtSec = trace.getTISeries().getDT();
      double ctsPerDT = trace.getTISeries().getMeanIntensity();
      double cps = ctsPerDT / dtSec;
      val = SnF.doubleToString(cps, NF.D1C1, NF.D1C1Exp);
    }
    return val;
  }

  @Override
  public String tabRawMedian(Channel channel) {
    Trace trace = getTrace(channel);
    String val = trace != null ? str(trace.getTISeries().getMedianIntensity(), NF.D1C2) : EMPTY_CELL;
    return val;
  }

  @Override
  public String tabRawMedianCPS(Channel channel) {
    String val = EMPTY_CELL;
    Trace trace = getTrace(channel);
    if (trace != null) {
      double dtSec = trace.getTISeries().getDT();
      double ctsPerDT = trace.getTISeries().getMedianIntensity();
      double cps = ctsPerDT / dtSec;
      val = SnF.doubleToString(cps, NF.D1C1, NF.D1C1Exp);
    }
    return val;
  }

  @Override
  public String tabRawSD(Channel channel) {
    Trace trace = getTrace(channel);
    String val = trace != null ? str(trace.getTISeries().getSD(), NF.D1C2) : EMPTY_CELL;
    return val;
  }

  @Override
  public String tabRawMAD(Channel channel) {
    Trace trace = getTrace(channel);
    String val = trace != null ? str(trace.getTISeries().getMadSD(), NF.D1C2) : EMPTY_CELL;
    return val;
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
  public String tabLodCts(Channel channel, PopulationID populationID) {
    return str(getMaxThr(channel, populationID, true), NF.D1C2);
  }

  @Override
  public String tabLodAg(Channel channel, PopulationID populationID) {
    double[] cts = new double[]{getMaxThr(channel, populationID, true)};

    double[] quant = applyQuant(cts, channel, MassUnit.ATTO_GRAM);
    return quant.length > 0 ? str(quant[0], NF.D1C2, NF.D1C2Exp) : EMPTY_CELL;

  }

  @Override
  public String tabLodNm(Channel channel, PopulationID populationID) {
    double[] cts = new double[]{getMaxThr(channel, populationID, true)};
    double[] quant = applyQuant(cts, channel, SizeUnit.NANO_METER);
    return quant.length > 0 ? str(quant[0], NF.D1C2, NF.D1C2Exp) : EMPTY_CELL;
  }

  @Override
  public String tabLodAmol(Channel channel, PopulationID populationID) {
    double[] cts = new double[]{getMaxThr(channel, populationID, true)};
    double[] quant = applyQuant(cts, channel, MolarUnit.ATTO_MOL);
    return quant.length > 0 ? str(quant[0], NF.D1C2, NF.D1C2Exp) : EMPTY_CELL;
  }

  @Override
  public String tabPopNpCount(Channel channel, PopulationID populationID) {
    return check(channel, populationID) ?
        str(getTrace(channel).getPopulation(populationID).getEvents().size(), NF.D1C0) : EMPTY_CELL;
  }

  @Override
  public String tabPNC(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null) {
      Population pop = t.getPopulation(populationID);
      if (pop != null) {
        // Note that the synthetic population is not cut if time roi is applied. Hence this "special getter".
        double duration = pop.getEvents().getCheckedTISeries().getDuration();
        int nNP = t.getPopulation(populationID).getEvents().size();

        double te = getPncTEConvention(channel) / 100d;
        double npPerMin = 60 * nNP / duration;
        npPerMin = npPerMin / te; // we have more NP in reality
        double mLPerMin = quant.getExperimentalConditions().getFlowRate(FlowUnit.MILLILITRE_PER_MINUTE);
        double conc = npPerMin / mLPerMin;
        if (Double.isFinite(conc)) {
          val = str(conc, NF.D1C3Exp);
        }
      }
    }
    return val;
  }

  @Override
  public String tabPopNpRate(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null) {
      Population pop = t.getPopulation(populationID);
      if (pop != null) {
        // Note that the synthetic population is not cut if time roi is applied. Hence this "special getter".
        double duration = pop.getEvents().getCheckedTISeries().getDuration();
        int nNP = t.getPopulation(populationID).getEvents().size();
        val = str(nNP / duration, NF.D1C1);
      }
    }
    return val;
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
  public String tabPopNpCustomParamMean(Channel channel, PopulationID populationID, EventParameter par,
                                        MathMod math, Unit unit) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      // val = str(mu(math.calc(t.get(populationID, EventType.NP, par))), NF.D1C3);
      val = str(mu(math.calc(getData(channel, populationID, EventType.NP, par, unit))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamMedian(Channel channel, PopulationID populationID, EventParameter par,
                                       MathMod math, Unit unit) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(md(math.calc(getData(channel, populationID, EventType.NP, par, unit))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamSD(Channel channel, PopulationID populationID, EventParameter par,
                                   MathMod math, Unit unit) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(math.calc(getData(channel, populationID, EventType.NP, par, unit))), NF.D1C3);
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
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          SizeUnit.NANO_METER);
      if (data.length > 0) {
        val = str(mu(data), NF.D1C1);
      }
    }
    return val;
  }

  @Override
  public String tabMedianSize(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          SizeUnit.NANO_METER);
      if (data.length > 0) {
        val = str(md(data), NF.D1C1);
      }
    }
    return val;
  }


  @Override
  public String tabSizeSD(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          SizeUnit.NANO_METER);
      if (data.length > 0) {
        val = str(sd(data), NF.D1C2);
      }
    }
    return val;
  }


  @Override
  public String tabMeanMass(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          MassUnit.FEMTO_GRAM);

      if (data.length > 0) {
        val = str(mu(data), NF.D1C3Exp);
      }
    }
    return val;
  }


  @Override
  public String tabMedianMass(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          MassUnit.FEMTO_GRAM);

      if (data.length > 0) {
        val = str(md(data), NF.D1C3Exp);
      }
    }
    return val;
  }

  @Override
  public String tabMassSD(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          MassUnit.FEMTO_GRAM);
      if (data.length > 0) {
        val = str(sd(data), NF.D1C3Exp);
      }
    }
    return val;
  }

  @Override
  public String tabMeanMol(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          MolarUnit.ATTO_MOL);

      if (data.length > 0) {
        val = str(mu(data), NF.D1C3Exp);
      }
    }
    return val;
  }


  @Override
  public String tabMedianMol(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          MolarUnit.ATTO_MOL);

      if (data.length > 0) {
        val = str(md(data), NF.D1C3Exp);
      }
    }
    return val;
  }

  @Override
  public String tabMolSD(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.hasType(populationID)) {
      double[] data = getData(channel, populationID, EventType.NP,
          quant.getExperimentalConditions().getEventPar(),
          MolarUnit.ATTO_MOL);

      if (data.length > 0) {
        val = str(sd(data), NF.D1C3Exp);
      }
    }
    return val;
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
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null) {
      Population pop = t.getPopulation(populationID);
      if (pop != null) {
        double df = pop.getDrift();
        if (df == NpPopulation.DEFAULT_DRIFT) {
          df = DriftFactor.calculateDriftFactor(t, populationID);
          pop.setDrift(df);
        }
        val = str(df, NF.D1C1);
      }
    }
    return val;
  }

  @Override
  public String tabBlnDistr(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = t.getBaseline().getSummary();
    }
    return val;
  }

  @Override
  public String tabBlnMean(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = str(mu(t.getBaseline().getBackgroundDistribution().getLocation()), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabBlnSD(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = str(mu(t.getBaseline().getBackgroundDistribution().getSpread()), NF.D1C4);
    }
    return val;
  }

  @Override
  public String tabBlnOutlierZ(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = str(mu(t.getBaseline().getBackgroundDistribution().getOutlierFactor()), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabEquivBGConc(Channel channel) {
    String val = EMPTY_CELL;
    double bgConc = calcEquivBGConc(channel);
    if (bgConc > 0) {
      val = str(bgConc, NF.D1C3, NF.D1C3Exp);
    }
    return val;
  }

  public double calcEquivBGConc(Channel channel) {
    double val = 0d;
    Trace t = getTrace(channel);
    CalChannel calChannel = AnalysisUtils.getCalChannel(channel);
    if (t != null && calChannel != null) {
      double signalCtsPerDT = t.getTISeries().getMeanIntensity();
      if (t.getBaseline() != null && t.getBaseline().hasBaseline()) {
        signalCtsPerDT = mu(t.getBaseline().getBackgroundDistribution().getLocation());
      }
      double flowRateUllPerMin =
          quant.getExperimentalConditions().getFlowRate(FlowUnit.MICROLITRE_PER_MINUTE);
      double dwellTimeSec = t.getTISeries().getDT();
      Quantity sensitivity = quant.getResponses().getOrCreateIonicResponse(calChannel);
      if (sensitivity.getValue() > 0) {
        double sensitivityCtsPerFg = sensitivity.getUnit().convert(sensitivity.getValue(),
            SensitivityUnit.CTS_PER_FEMTOGRAM);

        // Step 1: convert signal to mass flow rate (fg/sec)
        double massRateFgPerSec = signalCtsPerDT / (sensitivityCtsPerFg * dwellTimeSec);

        // Step 2: convert flow rate to µL/sec
        double flowRateUllPerSec = flowRateUllPerMin / 60.0;

        // Step 3: compute concentration in fg/µL
        double concFgPerUll = massRateFgPerSec / flowRateUllPerSec;

        // Step 4: convert fg/µL to µg/L (1 fg/µL = 1e-3 µg/L)
        double concUgPerL = concFgPerUll * 1e-3;

        val = concUgPerL;
      }
    }
    return val;
  }

  @Override
  public String tabSearchStart(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      StatCollection blnStats = t.getBaseline().getBackgroundDistribution();
      Population population = t.getPopulation(populationID);
      if (population != null) {
        ThresholdSupplier supplier = population.getStartInstructions().get(blnStats);
        val = str(mu(supplier.getThresholdSlices()), NF.D1C2);
      }
    }
    return val;
  }

  @Override
  public String tabSearchStop(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      StatCollection blnStats = t.getBaseline().getBackgroundDistribution();
      Population population = t.getPopulation(populationID);
      if (population != null) {
        ThresholdSupplier supplier = population.getStopInstructions().get(blnStats);
        val = str(mu(supplier.getThresholdSlices()), NF.D1C2);
      }
    }
    return val;
  }

  @Override
  public String tabSearchHeight(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      StatCollection blnStats = t.getBaseline().getBackgroundDistribution();
      Population population = t.getPopulation(populationID);
      if (population != null) {
        ThresholdSupplier supplier = population.getHeightInstructions().get(blnStats);
        val = str(mu(supplier.getThresholdSlices()), NF.D1C2);
      }
    }
    return val;
  }

  @Override
  public List<String> tabGates(Channel channel, PopulationID populationID) {
    List<String> values = new ArrayList<>();
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      StatCollection blnStats = t.getBaseline().getBackgroundDistribution();
      Population population = t.getPopulation(populationID);
      if (population != null) {
        List<ThresholdSupplier> gateSuppliers = new ArrayList<>();
        for (ThresholdSupplierInstructions gateInstr : population.getGatingInstr()) {
          gateSuppliers.add(gateInstr.get(blnStats));
        }
        for (ThresholdSupplier supplier : gateSuppliers) {
          String val = EMPTY_CELL;
          val = str(mu(supplier.getThresholdSlices()), NF.D1C2);
          values.add(val);
        }
      }
    }
    return values;
  }

  @Override
  public String tabSearchStartMeta(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      Population population = t.getPopulation(populationID);
      if (population != null) {
        val = population.getStartInstructions().translate();
      }
    }
    return val;
  }

  @Override
  public String tabSearchStopMeta(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      Population population = t.getPopulation(populationID);
      if (population != null) {
        val = population.getStopInstructions().translate();
      }
    }
    return val;
  }

  @Override
  public String tabSearchHeightMeta(Channel channel, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      Population population = t.getPopulation(populationID);
      if (population != null) {
        val = population.getHeightInstructions().translate();
      }
    }
    return val;
  }

  @Override
  public List<String> tabGatesMeta(Channel channel, PopulationID populationID) {
    List<String> values = new ArrayList<>();
    Trace t = getTrace(channel);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      Population population = t.getPopulation(populationID);
      if (population != null) {
        for (ThresholdSupplierInstructions gateInstr : population.getGatingInstr()) {
          String val = EMPTY_CELL;
          val = gateInstr.translate();
          values.add(val);
        }
      }
    }
    return values;
  }
}
