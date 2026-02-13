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

import analysis.AnalysisUtils;
import analysis.Event;
import analysis.NpPopulation;
import analysis.Population;
import analysis.PopulationID;
import analysis.StatCollection;
import analysis.ThresholdSupplier;
import analysis.ThresholdSupplierInstructions;
import analysis.quant.Calibration;
import analysis.quant.Cal;
import analysis.quant.Quantity;
import core.SpTool3Main;
import dataModelNew.mz.Element;
import io.export.ExportSimulationEventContainer;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

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

import static visualizer.ResultsTable.EMPTY_CELL;

public class SampleImpl implements Sample, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(SampleImpl.class);

  private String nickName;
  private boolean highlighted;
  private String comment;
  private final SampleFile sampleFile;
  private final HashMap<Isotope, Trace> traces;
  private final List<ParticlePopulationMatrix> matrices;
  private Method method;
  private Cal quant;
  private Color color;

  public SampleImpl() {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.nickName = "No data";
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = new SampleFile();
    this.method = new ListMethod();
    this.quant = new Calibration();
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
  }

  public SampleImpl(String nickName) {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = new SampleFile();
    this.method = new ListMethod();
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
  }

  public SampleImpl(String nickName, SampleFile sampleFile) {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = sampleFile;
    this.method = new ListMethod();
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
  }

  public SampleImpl(String nickName, SampleFile sampleFile, Method method) {
    this.traces = new LinkedHashMap<>();
    this.matrices = new ArrayList<>();
    this.nickName = nickName;
    this.highlighted = false;
    this.comment = "";
    this.sampleFile = sampleFile;
    this.method = method;
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
  }

  // Deep copy
  public SampleImpl(String nickName, boolean highlighted, String comment,
                    SampleFile sampleFile, HashMap<Isotope, Trace> traces,
                    List<ParticlePopulationMatrix> matrices,
                    Method method,
                    Cal quant,
                    Color color) {
    this.nickName = nickName;
    this.matrices = new ArrayList<>();
    for (ParticlePopulationMatrix matrix : matrices) {
      this.matrices.add(matrix.copy());
    }
    this.highlighted = highlighted;
    this.comment = comment;
    this.sampleFile = new SampleFile(sampleFile);
    this.traces = new LinkedHashMap<>();
    traces.values().forEach(t -> this.traces.put(t.getMzValue().getIsotope(), t.copy(this)));
    this.method = method.getCopyWithoutFile();
    this.quant = quant.copy();
    this.color = color;
  }

  public Sample copy() {
    Sample newSample = new SampleImpl(
        nickName,
        highlighted,
        comment,
        new SampleFile(sampleFile),
        traces,
        matrices,
        method,
        quant,
        color);
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
        method,
        // I assume, here we would have to requantify as after merging q is not defined clearly
        new Calibration(),
        color);
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
  public List<Trace> getTraces() {
    List<Trace> tcs = new ArrayList<>(this.traces.values());
    tcs.sort(Comparator.comparingDouble(o -> o.getMzValue().getMZ()));
    return tcs;
  }

  @Nullable
  @Override
  public Trace getTrace(Isotope isotope) {
    if (isotope == null) {
      return null;
    }
    return traces.get(isotope);
  }

  /**
   * @param isotopes Isotopes that may or may not be present in this sample.
   * @return A list of the corresponding traces. List is empty if no match is found.
   */
  @Override
  public List<Trace> getTraces(List<Isotope> isotopes) {
    List<Trace> result = new ArrayList<>();
    for (Isotope isotope : isotopes) {
      if (traces.containsKey(isotope)) {
        result.add(traces.get(isotope));
      }
    }
    return result;
  }

  @Override
  public List<Isotope> listIsotopes() {
    return Collections.unmodifiableList(new ArrayList<>(traces.keySet()));
  }

  /**
   * @param isotope Isotope that may or may not be present in this sample.
   * @return A UNIQUE list of the corresponding simulated particle population matrices. Several
   * traces may contain the same matrix, hence the check and guarantee to return unique instances
   * only. List is empty if no match is found or if no simulated Trace is present in this sample.
   */
  @Override
  public List<ParticlePopulationMatrix> getMatrices(Isotope isotope) {
    List<ParticlePopulationMatrix> result = new ArrayList<>();

    for (ParticlePopulationMatrix particleMatrix : this.matrices) {
      // add if present but ensure uniqueness!
      if (particleMatrix.hasIsotope(isotope) && !result.contains(particleMatrix)) {
        result.add(particleMatrix);
      }
    }
    return result;
  }

  @Override
  public List<ParticlePopulationMatrix> getMatrices() {
    return matrices;
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
  public List<PopulationID> listPopulations(List<Isotope> isotopes) {
    List<PopulationID> pops = new ArrayList<>();
    List<Trace> traces = getTraces(isotopes);
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
  public void removePopulations(List<Isotope> isotopes, PopulationID populationID) {
    List<Trace> traces = getTraces(isotopes);
    for (Trace trace : traces) {
      trace.removePopulation(populationID);
    }
  }

  @Override
  public void addTrace(Trace trace) {
    traces.put(trace.getMzValue().getIsotope(), trace);
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
  public double[] getData(Isotope isotope, PopulationID populationID, EventType eventType,
                          EventParameter param, Unit unit) {

    double[] data = new double[0];
    // call getter (possibly null)
    Trace trace = getTrace(isotope);
    if (trace != null) {
      data = trace.get(populationID, eventType, param);

      //  check conversion
      if (IntensityUnit.CTS.equals(unit) || !EventParameter.canQuantify(param)) {
        // do nothing
      } else if (quant.getExperimentalConditions().getCalibratorRole().getValue().equals(CalibratorRole.SAMPLE)) {

        // find the quant
        double ctsPerFg = 0;

        switch (quant.getCalibrationStrategy()) {
          case MASS -> {
            Quantity npResp = quant.getResponses().getNpResponse(isotope);
            if (npResp != null && npResp.getValue() > 0) {
              ctsPerFg = npResp.getUnit().convert(npResp.getValue(), SensitivityUnit.CTS_PER_FEMTOGRAM);
            } else {
              data = new double[0];
            }
            break;
          }

          case SIZE_METHOD, FREQUENCY_METHOD -> {

            Quantity ionResp = quant.getResponses().getIonicResponse(isotope);

            // check if TE was determined elsewhere
            double tePct = getAerosolTEConvention(isotope);

            if (tePct > 0 && ionResp != null && ionResp.getValue() > 0) {
              ctsPerFg = ionResp.getUnit().convert(ionResp.getValue(), SensitivityUnit.CTS_PER_FEMTOGRAM);
              // we get more cts for NP as no losses (divide by TE)
              ctsPerFg = ctsPerFg / (tePct / 100d);
            } else {
              data = new double[0];
            }
            break;
          }
        }

        // check conversion
        ExperimentalSubConditions subPar =
            quant.getExperimentalConditions().getElementSpecificQuantParams().get(isotope.getElement());

        if (subPar != null && ctsPerFg > 0) {
          // diameter?
          if ((unit.equals(SizeUnit.NANO_METER) || unit.equals(SizeUnit.MICRO_METER))) {

            if (subPar.getNpQuantificationApproach().getValue().equals(ParticleQuantApproach.ESD)) {
              double density = subPar.getNpDensity().getValue();
              density = subPar.getNpDensityUnit().convert(density, DensityUnit.GRAM_PER_CM3);
              double massFrac = subPar.getNpMassFraction().getValue();

              if (density > 0 && massFrac > 0) {
                for (int i = 0; i < data.length; i++) {
                  double counts = data[i];

                  // counts → mass (fg)
                  double massFg = counts / ctsPerFg;

                  /*
                   adjust mass:
                   - this is for the case, where e.g., Au@Ag is calibrated for and based on
                   total particle size
                   - for all other, mass fraction should just be left at 1
                   */
                  massFg = massFg / massFrac;

                  // µg → g
                  double massGrams = massFg * 1e-15;

                  // mass → volume (cm^3)
                  double volumeCm3 = massGrams / density;

                  // volume → diameter (cm)
                  double diameterCm = Math.cbrt((6.0 * volumeCm3) / Math.PI);

                  if (unit.equals(SizeUnit.NANO_METER)) {
                    // cm → nm
                    data[i] = diameterCm * 1e7;
                  } else {
                    // cm -> µm
                    data[i] = diameterCm * 1e4;
                  }
                }
              } else {
                data = new double[0];
              }


            } else {
              // return empty array: we don't' have a sample that qualifies for sphere
              data = new double[0];
            }

          } else {
            double factor;
            if (unit.equals(MassUnit.FEMTO_GRAM)) {
              factor = ctsPerFg;
            } else if (unit.equals(MassUnit.ATTO_GRAM)) {
              factor = ctsPerFg / 1E3; // fewer counts per attogram
            } else {
              factor = ctsPerFg * 1E3; // more counts per pg
            }

            data = ArrUtils.divide(data, factor);
          }
        } else {
          data = new double[0];
        }

      } else {
        data = new double[0];
      }
    }
    return data;
  }

  @Override
  public double getAerosolTEConvention(Isotope isotope) {
    double tePct = 0;
    Quantity te = quant.getResponses().getAerosolTE(isotope);
    if (te != null && te.getValue() <= 0) {
      List<Isotope> isotopes = quant.listIsotopes();
      for (Isotope iso : isotopes) {
        Quantity testTE = quant.getResponses().getAerosolTE(iso);
        if (testTE != null && testTE.getValue() > 0) {
          tePct = testTE.getValue();
          break;
        }
      }
    } else if (te != null) {
      tePct = te.getValue();
    }
    return tePct;
  }

  @Override
  public double getPncTEConvention(Isotope isotope) {
    double tePct = 0;
    Quantity te = quant.getResponses().getParticleNumberTE(isotope);
    if (te != null && te.getValue() <= 0) {
      List<Isotope> isotopes = quant.listIsotopes();
      for (Isotope iso : isotopes) {
        Quantity testTE = quant.getResponses().getParticleNumberTE(iso);
        if (testTE != null && testTE.getValue() > 0) {
          tePct = testTE.getValue();
          break;
        }
      }
    } else if (te != null) {
      tePct = te.getValue();
    }

    // final check
    if (tePct <= 0) {
      tePct = getAerosolTEConvention(isotope);
    }
    return tePct;
  }

  @Override
  public double getMaxThr(@Nullable Isotope isotope, PopulationID populationID) {
    double maxThr = 0;
    // call getter (possibly null)
    Trace trace = getTrace(isotope);
    if (trace != null) {
      if (trace != null && trace.getBaseline() != null && trace.getBaseline().hasBaseline()) {
        // Search height
        StatCollection blnStats = trace.getBaseline().getBackgroundDistribution();
        Population population = trace.getPopulation(populationID);
        if (population != null) {
          ThresholdSupplier supplier = population.getHeightInstructions().get(blnStats);
          maxThr = Math.max(maxThr, mu(supplier.getThresholdSlices()));
          // Gate height
          List<ThresholdSupplier> gateSuppliers = population.getGatingInstr().stream()
              .filter(ThresholdSupplierInstructions::isHeight)
              .map(gInstr -> gInstr.get(blnStats))
              .collect(Collectors.toList());
          for (ThresholdSupplier suppl : gateSuppliers) {
            maxThr = Math.max(maxThr, mu(suppl.getThresholdSlices()));
          }
        }
      }
    }
    return maxThr;
  }


  @Override
  public List<Event> getNPEvents(Isotope isotope, PopulationID popID) {
    List<Event> npEvents = new ArrayList<>();
    Trace trace = getTrace(isotope);
    if (trace != null) {
      Population pop = trace.getPopulation(popID);
      if (pop != null) {
        npEvents.addAll(pop.getEvents().getNpEvents());
      }
    }
    return npEvents;
  }

  @Override
  public int getTotalDataPoints(Isotope isotope) {
    int dp = 0;
    Trace trace = getTrace(isotope);
    if (trace != null) {
      dp = trace.getTISeries().size();
    }
    return dp;
  }

  @Override
  public double getRawMeanCPS(Isotope isotope) {
    double val = 0;
    if (traces.containsKey(isotope)) {
      Trace trace = traces.get(isotope);
      val = trace.getTISeries().getMeanIntensity();
      val = val / trace.getTISeries().getDT();
    }
    return val;
  }


  @Override
  public double getRawMedianCPS(Isotope isotope) {
    double val = 0;
    if (traces.containsKey(isotope)) {
      Trace trace = traces.get(isotope);
      val = trace.getTISeries().getMedianIntensity();
      val = val / trace.getTISeries().getDT();
    }
    return val;
  }

  @Override
  public double getEventRate(Isotope isotope, PopulationID populationID) {

    double rate = 0;
    // call getter (possibly null)
    Trace trace = getTrace(isotope);
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

            HashMap<EventParameter, double[]> dataMap = AnalysisUtils
                .getFromSimulation(this, isotope, matrix);
            container.getDataMap().put(isotope, dataMap);
          }
        }
      }
    }
    return containers;
  }

  @Override
  public double getAverageDrift(List<Isotope> isotopes, List<PopulationID> populations) {
    double sumDrift = 0;
    double maxDrift = 0;
    int counter = 0;

    List<Trace> tracesWithIsotopes = getTraces(isotopes);

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
  public double getAverageNoOfEvents(List<Isotope> isotopes, List<PopulationID> populations) {
    int sumEvents = 0;
    double counter = 0;

    List<Trace> tracesWithIsotopes = getTraces(isotopes);

    for (Trace trace : tracesWithIsotopes) {
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
  public String tabDwellTime(Isotope isotope) {
    Trace t = getTrace(isotope);
    return check(t) ? str(t.getTISeries().getDT() * 1E6, NF.D1C2) : EMPTY_CELL;
  }

  @Override
  public String tabDuration(Isotope isotope) {
    Trace t = getTrace(isotope);
    return check(t) ? str(t.getTISeries().getDuration(), NF.D1C2) : EMPTY_CELL;
  }

  @Override
  public String tabPoints(Isotope isotope) {
    Trace t = getTrace(isotope);
    return check(t) ? str(t.getTISeries().size(), NF.D1C0) : EMPTY_CELL;
  }

  @Override
  public String tabTISeriesLimits(Isotope isotope) {
    String val = "";
    if (check(isotope)) {
      Trace t = getTrace(isotope);
      val = t.hasLimits() ?
          "Time: " + str(t.getTISeries().getFirstTimeStamp(), NF.D1C1)
              + "-" + str(t.getTISeries().getLastTimeStamp(), NF.D1C1) :
          str(t.getTISeries().getFirstTimeStamp(), NF.D1C1)
              + "-" + str(t.getTISeries().getLastTimeStamp(), NF.D1C1);
    }
    return val;
  }

  @Override
  public String tabRawMean(Isotope isotope) {
    Trace trace = getTrace(isotope);
    String val = trace != null ? str(trace.getTISeries().getMeanIntensity(), NF.D1C2) : EMPTY_CELL;
    return val;
  }

  @Override
  public String tabRawMedian(Isotope isotope) {
    Trace trace = getTrace(isotope);
    String val = trace != null ? str(trace.getTISeries().getMedianIntensity(), NF.D1C2) : EMPTY_CELL;
    return val;
  }

  @Override
  public String tabRawSD(Isotope isotope) {
    Trace trace = getTrace(isotope);
    String val = trace != null ? str(trace.getTISeries().getSD(), NF.D1C2) : EMPTY_CELL;
    return val;
  }

  @Override
  public String tabRawMAD(Isotope isotope) {
    Trace trace = getTrace(isotope);
    String val = trace != null ? str(trace.getTISeries().getMadSD(), NF.D1C2) : EMPTY_CELL;
    return val;
  }

  @Override
  public String tabPopName(Isotope isotope, PopulationID populationID) {
    return check(isotope, populationID) ?
        getTrace(isotope).getPopulation(populationID).getName() : EMPTY_CELL;
  }

  @Override
  public String tabPopAdditional(Isotope isotope, PopulationID populationID) {
    return check(isotope, populationID) ?
        getTrace(isotope).getPopulation(populationID).translateParams() : EMPTY_CELL;
  }

  @Override
  public String tabPopNpCount(Isotope isotope, PopulationID populationID) {
    return check(isotope, populationID) ?
        str(getTrace(isotope).getPopulation(populationID).getEvents().size(), NF.D1C0) : EMPTY_CELL;
  }

  @Override
  public String tabPNC(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null) {
      Population pop = t.getPopulation(populationID);
      if (pop != null) {
        // Note that the synthetic population is not cut if time roi is applied. Hence this "special getter".
        double duration = pop.getEvents().getCheckedTISeries().getDuration();
        int nNP = t.getPopulation(populationID).getEvents().size();

        double te = getPncTEConvention(isotope) / 100d;
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
  public String tabPopNpRate(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
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
  public String tabPopNpMean(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.NP, EventParameter.NET_AREA)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.NP, EventParameter.NET_AREA)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabPopNpCustomParamMean(Isotope isotope, PopulationID populationID, EventParameter par,
                                        MathMod math) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(math.calc(t.get(populationID, EventType.NP, par))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamMedian(Isotope isotope, PopulationID populationID, EventParameter par,
                                       MathMod math) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(md(math.calc(t.get(populationID, EventType.NP, par))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamSD(Isotope isotope, PopulationID populationID, EventParameter par,
                                   MathMod math) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(math.calc(t.get(populationID, EventType.NP, par))), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpMedian(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(md(t.get(populationID, EventType.NP, EventParameter.NET_AREA)), NF.D1C3);
    }
    return val;
  }


  @Override
  public String tabMeanHeight(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.NP, EventParameter.HEIGHT)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSdHeight(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.NP, EventParameter.HEIGHT)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanDuration(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.NP, EventParameter.DURATION)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSdDuration(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.NP, EventParameter.DURATION)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanSize(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(getData(isotope, populationID, EventType.NP,
              quant.getExperimentalConditions().getEventPar(),
              SizeUnit.NANO_METER)),
          NF.D1C1);
    }
    return val;
  }

  @Override
  public String tabMedianSize(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(md(getData(isotope, populationID, EventType.NP,
              quant.getExperimentalConditions().getEventPar(),
              SizeUnit.NANO_METER)),
          NF.D1C1);
    }
    return val;
  }


  @Override
  public String tabSizeSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(getData(isotope, populationID, EventType.NP,
              quant.getExperimentalConditions().getEventPar(),
              SizeUnit.NANO_METER)),
          NF.D1C2);
    }
    return val;
  }


  @Override
  public String tabMeanMass(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(getData(isotope, populationID, EventType.NP,
              quant.getExperimentalConditions().getEventPar(),
              MassUnit.FEMTO_GRAM)),
          NF.D1C3Exp);
    }
    return val;
  }


  @Override
  public String tabMedianMass(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(md(getData(isotope, populationID, EventType.NP,
              quant.getExperimentalConditions().getEventPar(),
              MassUnit.FEMTO_GRAM)),
          NF.D1C3Exp);
    }
    return val;
  }

  @Override
  public String tabMassSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(getData(isotope, populationID, EventType.NP,
              quant.getExperimentalConditions().getEventPar(),
              MassUnit.FEMTO_GRAM)),
          NF.D1C3Exp);
    }
    return val;
  }

  @Override
  public String tabPopBgMean(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(mu(t.get(populationID, EventType.BG, EventParameter.AREA)), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabPopBgSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(sd(t.get(populationID, EventType.BG, EventParameter.AREA)), NF.D1C4);
    }
    return val;
  }

  @Override
  public String tabPopBgN(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.hasType(populationID)) {
      val = str(t.get(populationID, EventType.BG, EventParameter.AREA).length, NF.D1C0);
    }
    return val;
  }


  @Override
  public String tabPopDrift(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
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
  public String tabBlnDistr(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = t.getBaseline().getSummary();
    }
    return val;
  }

  @Override
  public String tabBlnMean(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = str(mu(t.getBaseline().getBackgroundDistribution().getLocation()), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabBlnSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = str(mu(t.getBaseline().getBackgroundDistribution().getSpread()), NF.D1C4);
    }
    return val;
  }

  @Override
  public String tabBlnOutlierZ(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      val = str(mu(t.getBaseline().getBackgroundDistribution().getOutlierFactor()), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSearchStart(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
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
  public String tabSearchStop(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
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
  public String tabSearchHeight(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
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
  public List<String> tabGates(Isotope isotope, PopulationID populationID) {
    List<String> values = new ArrayList<>();
    Trace t = getTrace(isotope);
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
  public String tabSearchStartMeta(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      Population population = t.getPopulation(populationID);
      if (population != null) {
        val = population.getStartInstructions().translate();
      }
    }
    return val;
  }

  @Override
  public String tabSearchStopMeta(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      Population population = t.getPopulation(populationID);
      if (population != null) {
        val = population.getStopInstructions().translate();
      }
    }
    return val;
  }

  @Override
  public String tabSearchHeightMeta(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    Trace t = getTrace(isotope);
    if (t != null && t.getBaseline() != null && t.getBaseline().hasBaseline()) {
      Population population = t.getPopulation(populationID);
      if (population != null) {
        val = population.getHeightInstructions().translate();
      }
    }
    return val;
  }

  @Override
  public List<String> tabGatesMeta(Isotope isotope, PopulationID populationID) {
    List<String> values = new ArrayList<>();
    Trace t = getTrace(isotope);
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
