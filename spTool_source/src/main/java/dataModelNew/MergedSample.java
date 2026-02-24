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

import analysis.Event;
import analysis.NpPopulation;
import analysis.Population;
import analysis.PopulationID;
import analysis.StatCollection;
import analysis.ThresholdSupplier;
import analysis.quant.Calibration;
import analysis.quant.Cal;
import core.SpTool3Main;
import dataModelNew.mz.MZValue;
import io.export.ExportSimulationEventContainer;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.print.Collation;
import math.stat.DriftFactor;
import math.units.Unit;
import math.units.enums.FlowUnit;
import math.units.enums.MassUnit;
import math.units.enums.MolarUnit;
import math.units.enums.SizeUnit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.MathMod;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.ParticlePopulationMatrix;
import util.ArrUtils;
import util.NF;

import static visualizer.ResultsTable.EMPTY_CELL;

public class MergedSample implements Sample, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(MergedSample.class);

  private String nickName;
  private boolean highlighted;
  private String comment;
  private final List<Sample> samples;
  private Sample principleSample;
  private Cal quant;
  private Color color;

  public MergedSample() {
    this.samples = new ArrayList<>();
    this.samples.add(new SampleImpl());
    this.principleSample = samples.get(0);
    this.nickName = "No data";
    this.highlighted = false;
    this.comment = "";
    this.quant = new Calibration();
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
  }

  public MergedSample(String nickName, List<Sample> samples) {
    this.samples = new ArrayList<>(samples);
    // no nesting
    this.samples.removeIf(s -> s instanceof MergedSample);
    if (this.samples.isEmpty()) {
      this.principleSample = new SampleImpl();
    } else {
      this.principleSample = samples.get(0);
    }
    this.nickName = nickName;
    this.highlighted = false;
    StringBuilder commentBuilder = new StringBuilder("Merged (n=" + this.samples.size() + ")");
    this.samples.forEach(s -> commentBuilder.append("\t").append(s.getNickName()));
    this.comment = commentBuilder.toString();
    this.quant = new Calibration(nickName);
    this.color = SpTool3Main.getRunTime().getNextSampleColor().get();
  }

  // Deep copy: note: this constructor does not copy. You have to pass copies as arguments.
  public MergedSample(String nickName, boolean highlighted, String comment, Color color,
                      List<Sample> samples, Sample principleSample, Cal quant) {
    this.nickName = nickName;
    this.samples = new ArrayList<>(samples);
    // no nesting
    this.samples.removeIf(s -> s instanceof MergedSample);

    this.highlighted = highlighted;
    this.comment = comment;
    this.color = color;
    this.quant = quant;
    this.principleSample = principleSample;
  }

  public Sample copy() {
    List<Sample> copySamples = new ArrayList<>();
    this.samples.forEach(s -> copySamples.add(s.copy()));
    Sample principleSampleCopy;
    if (this.samples.isEmpty()) {
      principleSampleCopy = this.principleSample.copy();
    } else {
      principleSampleCopy = samples.get(0);
    }
    Sample newSample = new MergedSample(
        nickName,
        highlighted,
        comment,
        color,
        copySamples,
        principleSampleCopy,
        quant.copy());
    return newSample;
  }

  public Sample copyWithoutTraces() {
    List<Sample> copySamples = new ArrayList<>();
    samples.forEach(s -> this.samples.add(s.copyWithoutTraces()));
    Sample principleSampleCopy;
    if (this.samples.isEmpty()) {
      principleSampleCopy = this.principleSample.copyWithoutTraces();
    } else {
      principleSampleCopy = samples.get(0);
    }
    Sample newSample = new MergedSample(
        nickName,
        highlighted,
        comment,
        color,
        copySamples,
        principleSampleCopy,
        // I assume, here we would have to requantify as after merging q is not defined clearly
        new Calibration());
    return newSample;
  }


  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {
    // Default serialization: Note that ram/hdd conversion is carried out in each sample when called
    out.defaultWriteObject();
    LOGGER.trace("Wrote to object: " + getNickName());
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // Default serialization: Note that ram/hdd conversion is carried out in each sample when called
    in.defaultReadObject();
    if (quant == null) {
      this.quant = new Calibration();
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

  /**
   * Returns a list of isotopes that is unique, i.e., even if two traces contain the same isotopes,
   * we get a unique list of isotopes where no duplicates exist.
   * <p>
   * We need this method to fill the UI (return isotopes here instead of using instance checks to
   * handle different sample implementations elsewhere).
   */
  @Override
  public List<Isotope> listIsotopes() {

    List<Isotope> isotopes = samples.stream()
        .flatMap(sample -> sample.getTraces().stream())
        .map(Trace::getMzValue)
        .filter(MZValue::hasIsotope)
        .map(MZValue::getIsotope)
        .distinct() // ensures uniqueness
        .collect(Collectors.toList());
    return isotopes;
  }

  @Override
  public List<Trace> getTraces() {
    LOGGER.warn("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
    return new ArrayList<>();
  }

  @Override
  public List<Trace> getTraces(List<Isotope> isotopes) {
    LOGGER.warn("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
    return new ArrayList<>();
  }

  @Nullable
  @Override
  public Trace getTrace(Isotope isotope) {
    return null;
  }

  /**
   * Override to return empty list. The "merging" of data has to be handled at a higher level and
   * for each case.
   */
  @Override
  public List<ParticlePopulationMatrix> getMatrices(Isotope isotope) {
    LOGGER.warn("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
    return new ArrayList<>();
  }

  @Override
  public List<ParticlePopulationMatrix> getMatrices() {
    LOGGER.warn("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
    return new ArrayList<>();
  }

  @Override
  public List<PopulationID> listAllPopulations() {
    List<PopulationID> pops = new ArrayList<>();
    for (Sample sample : samples) {
      List<PopulationID> subSamplePops = sample.listAllPopulations();
      for (PopulationID pop : subSamplePops) {
        if (!pops.contains(pop)) {
          pops.add(pop);
        }
      }
    }
    return pops;
  }

  @Override
  public List<PopulationID> listPopulations(List<Isotope> isotopes) {
    List<PopulationID> pops = new ArrayList<>();
    for (Sample sample : samples) {
      List<PopulationID> subSamplePops = sample.listPopulations(isotopes);
      for (PopulationID pop : subSamplePops) {
        if (!pops.contains(pop)) {
          pops.add(pop);
        }
      }
    }
    return pops;
  }


  @Override
  public void removePopulations(List<Isotope> isotopes, PopulationID populationID) {
    for (Sample sample : samples) {
      sample.removePopulations(isotopes, populationID);
    }
  }

  @Override
  public void addTrace(Trace trace) {
    // We don't to this here. This must be managed in the wrapped samples if desired at all.
    LOGGER.warn("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
  }

  @Override
  public void addMatrices(List<ParticlePopulationMatrix> matrices) {
    // We don't to this here. This must be managed in the wrapped samples if desired at all.
    LOGGER.warn("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
  }

  public SampleFile getSampleFile() {
    return new SampleFile("Merged samples");
  }

  public Method getMethod() {
    LOGGER.trace("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
    return new ListMethod();
  }

  public void setMethod(Method method) {
    // We don't to this here. This must be managed in the wrapped samples if desired at all.
    LOGGER.trace("You should not call this method for merged samples! "
        + ExceptionUtils.getStackTrace(new Throwable()));
  }

  @Override
  public Color getColor() {
    return color;
  }


  public void setColor(Color color) {
    this.color = color;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  // non overriding, custom methods
  public List<Sample> getGroupedSamples() {
    return samples;
  }

  @Override
  public Sample getPrincipleSample() {
    return principleSample;
  }

  @Override
  public List<Sample> getAllSamples() {
    List<Sample> samples = new ArrayList<>(this.samples);
    return samples;
  }

  public void setPrincipleSample(Sample principleSample) {
    this.principleSample = principleSample;
  }

  /// /////////////////////////// Calculations for UI //////////////////////////////////////

  @Override
  public double[] getData(Isotope isotope, PopulationID populationID, EventType eventType,
                          EventParameter param, Unit unit) {

    List<double[]> dataList = new ArrayList<>();

    for (Sample sample : samples) {
      dataList.add(sample.getData(isotope, populationID, eventType, param, unit));
    }

    double[] data = ArrUtils.merge(dataList);

    return data;
  }

  @Override
  public double getAerosolTEConvention(Isotope isotope) {
    double te = 0;
    for (Sample sample : samples) {
      te += sample.getAerosolTEConvention(isotope);
    }
    te /= samples.size();
    return te;
  }

  @Override
  public double getPncTEConvention(Isotope isotope) {
    double te = 0;
    for (Sample sample : samples) {
      te += sample.getPncTEConvention(isotope);
    }
    te /= samples.size();
    return te;
  }

  @Override
  public double getMaxThr(@Nullable Isotope isotope, PopulationID populationID, boolean netSignal) {
    double maxThr = 0;
    for (Sample sample : samples) {
      maxThr = Math.max(maxThr, sample.getMaxThr(isotope, populationID, netSignal));
    }
    return maxThr;
  }

  @Override
  public double getMaxThr(@Nullable Isotope isotope, PopulationID populationID, boolean netSignal,
                          Unit unit) {
    double maxThrQ = 0;
    for (Sample sample : samples) {
      if (sample instanceof SampleImpl) {
        double thr = sample.getMaxThr(isotope, populationID, netSignal);
        double[] qThr = ((SampleImpl) sample).applyQuant(new double[]{thr}, isotope,
            EventParameter.NET_AREA, unit);
        if (qThr.length > 0) {
          maxThrQ = Math.max(maxThrQ, qThr[0]);
        }
      }
    }
    return maxThrQ;
  }

  @Override
  public List<Event> getNPEvents(Isotope isotope, PopulationID popID) {
    List<Event> npEvents = new ArrayList<>();
    for (Sample sample : samples) {
      npEvents.addAll(sample.getNPEvents(isotope, popID));
    }
    return npEvents;
  }

  @Override
  public int getTotalDataPoints(Isotope isotope) {
    int dp = 0;
    for (Sample sample : samples) {
      Trace trace = sample.getTrace(isotope);
      if (trace != null) {
        dp += trace.getTISeries().size();
      }
    }

    return dp;
  }

  @Override
  public double getRawMeanCPS(Isotope isotope) {
    // I think, taking all samples, to merge all their data points is unnecessarily cumbersome.
    // For calibration, and if we assume merging is justified, mean of means should be "close enough"
    double val = 0;
    for (Sample sample : samples) {
      // only add if trace is there, else mean is diluted by the zeros that are returned
      if (sample.getTrace(isotope) != null) {
        val += sample.getRawMeanCPS(isotope);
      }
    }
    val = val / samples.size();
    return val;
  }


  @Override
  public double getRawMedianCPS(Isotope isotope) {
    // I think, taking all samples, to merge all their data points is unnecessarily cumbersome.
    // For calibration, and if we assume merging is justified, mean of means should be "close enough"
    double val = 0;
    for (Sample sample : samples) {
      // only add if trace is there, else mean is diluted by the zeros that are returned
      if (sample.getTrace(isotope) != null) {
        val += sample.getRawMedianCPS(isotope);
      }
    }
    val = val / samples.size();
    return val;
  }

  @Override
  public double getEventRate(@Nullable Isotope isotope, PopulationID populationID) {
    double rate = 0;
    int nNP = 0;
    double durationSec = 0;
    for (Sample sample : samples) {
      // call getter (possibly null)
      Trace trace = sample.getTrace(isotope);
      if (trace != null) {
        nNP += trace.getNoOfEvents(populationID);
        durationSec += trace.getTISeries().getDuration();
      }
    }
    // when all N_NP and durations have been added, average
    rate = nNP / durationSec;
    return rate;
  }


  /**
   * Merges all simulated populations into ONE merged population. This loses the fine resolution of
   * the contributing simulated populations.
   *
   * @return
   */
  @Override
  public List<ExportSimulationEventContainer> getSimExport() {
    List<ExportSimulationEventContainer> containers = new ArrayList<>();
    ExportSimulationEventContainer mainContainer = new ExportSimulationEventContainer();
    containers.add(mainContainer);

    for (Sample sample : samples) {
      List<ExportSimulationEventContainer> subContainers = sample.getSimExport();
      for (ExportSimulationEventContainer subContainer : subContainers) {
        if (subContainer.isSimData()) {
          mainContainer.merge(subContainer);
        }
      }
    }

    return containers;
  }

  @Override
  public double getAverageDrift(List<Isotope> isotopes, List<PopulationID> populations) {
    double averageDrift = NpPopulation.DEFAULT_DRIFT;
    if (samples.size() > 0) {
      averageDrift = 0;
      for (Sample sample : samples) {
        averageDrift += sample.getAverageDrift(isotopes, populations);
      }
      averageDrift = averageDrift / samples.size();
    }
    return averageDrift;
  }

  @Override
  public double getAverageNoOfEvents(List<Isotope> isotopes, List<PopulationID> populations) {
    double averageNoOfEvents = 0;
    if (samples.size() > 0) {
      for (Sample sample : samples) {
        averageNoOfEvents += sample.getAverageNoOfEvents(isotopes, populations);
      }
      // JUST RETURN THE SUM OF THE EVENTS :-)
    }
    return averageNoOfEvents;
  }

  /// /////////////// Getters for the results table in the UI /////////////////////////////////

  @Override
  public String tabSampleName() {
    return getSampleFile().getNameWithinFile() + ": "
        + getAllSamples().stream()
        .map(Sample::tabSampleName)
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabNickName() {
    return getNickName() + ": "
        + getAllSamples().stream()
        .map(Sample::tabNickName)
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabSampleFolder() {
    return getAllSamples().stream()
        .map(Sample::tabSampleFolder)
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabFullPath() {
    return getAllSamples().stream()
        .map(Sample::tabFullPath)
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabComment() {
    return getComment() + ": "
        + getAllSamples().stream()
        .map(Sample::tabComment)
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabHighlight() {
    String str = "Marked ";
    str += isHighlight() ? "(X): " : "(0): ";
    str += getAllSamples().stream()
        .map(Sample::isHighlight)
        .map(b -> b ? "X" : "O")
        .collect(Collectors.joining(" --- "));
    return str;
  }

  @Override
  public String tabDwellTime(Isotope isotope) {
    String val = EMPTY_CELL;
    double mean = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .mapToDouble(t -> t.getTISeries().getDT() * 1E6)
        .average()
        .orElse(0);
    val = str(mean, NF.D1C2);
    return val;
  }

  @Override
  public String tabDuration(Isotope isotope) {
    String val = EMPTY_CELL;
    double mean = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .mapToDouble(t -> t.getTISeries().getDuration())
        .sum();
    val = str(mean, NF.D1C2);
    return val;
  }

  @Override
  public String tabPoints(Isotope isotope) {
    String val = EMPTY_CELL;
    double mean = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .mapToDouble(t -> t.getTISeries().size())
        .sum();

    val = str(mean, NF.D1C2);
    return val;
  }

  @Override
  public String tabTISeriesLimits(Isotope isotope) {
    return getAllSamples().stream()
        .map(s -> s.tabTISeriesLimits(isotope))
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabRawMean(Isotope isotope) {
    String val = EMPTY_CELL;
    double mean = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .mapToDouble(t -> t.getTISeries().getMeanIntensity())
        .average()
        .orElse(0);
    val = str(mean, NF.D1C2);
    return val;
  }

  @Override
  public String tabRawMedian(Isotope isotope) {
    String val = EMPTY_CELL;
    double mean = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .mapToDouble(t -> t.getTISeries().getMedianIntensity())
        .average()
        .orElse(0);
    val = str(mean, NF.D1C2);
    return val;
  }

  @Override
  public String tabRawSD(Isotope isotope) {
    String val = EMPTY_CELL;
    double mean = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .mapToDouble(t -> t.getTISeries().getSD())
        .average()
        .orElse(0);
    val = str(mean, NF.D1C2);
    return val;
  }

  @Override
  public String tabRawMAD(Isotope isotope) {
    String val = EMPTY_CELL;
    double mean = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .mapToDouble(t -> t.getTISeries().getMadSD())
        .average()
        .orElse(0);
    val = str(mean, NF.D1C2);
    return val;
  }

  @Override
  public String tabPopName(Isotope isotope, PopulationID populationID) {
    return getAllSamples().stream()
        .map(s -> s.tabPopName(isotope, populationID))
        .distinct()
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabPopAdditional(Isotope isotope, PopulationID populationID) {
    return getAllSamples().stream()
        .map(s -> s.tabPopAdditional(isotope, populationID))
        // dont put distinct as there are input parameters in this string (else, you dont know which sample
        // is which set)
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabPopNpCount(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    double sum = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .filter(t -> t.getPopulation(populationID) != null)
        .mapToDouble(t -> t.getPopulation(populationID).getEvents().size())
        .sum();
    val = str(sum, NF.D1C0);
    return val;
  }

  @Override
  public String tabLodCts(Isotope isotope, PopulationID populationID) {
    String result = EMPTY_CELL;
    List<Double> lodList = new ArrayList<>();
    for (Sample sample : samples) {
      lodList.add(sample.getMaxThr(isotope, populationID, true));
    }
    if (!lodList.isEmpty()) {
      double worstLD = Collections.max(lodList);
      result = str(worstLD, NF.D1C2);
    }
    return result;
  }

  @Override
  public String tabLodAg(Isotope isotope, PopulationID populationID) {
    String result = EMPTY_CELL;
    List<Double> lodList = new ArrayList<>();
    for (Sample sample : samples) {
      if (sample instanceof SampleImpl) {
        double[] cts = new double[]{getMaxThr(isotope, populationID, true)};
        double[] quant = ((SampleImpl) sample).applyQuant(cts, isotope, MassUnit.ATTO_GRAM);
        if (quant.length > 0) {
          lodList.add(quant[0]);
        }
      }
    }
    if (!lodList.isEmpty()) {
      double worstLD = Collections.max(lodList);
      result = str(worstLD, NF.D1C2, NF.D1C2Exp);
    }
    return result;
  }

  @Override
  public String tabLodNm(Isotope isotope, PopulationID populationID) {
    String result = EMPTY_CELL;
    List<Double> lodList = new ArrayList<>();
    for (Sample sample : samples) {
      if (sample instanceof SampleImpl) {
        double[] cts = new double[]{getMaxThr(isotope, populationID, true)};
        double[] quant = ((SampleImpl) sample).applyQuant(cts, isotope, SizeUnit.NANO_METER);
        if (quant.length > 0) {
          lodList.add(quant[0]);
        }
      }
    }
    if (!lodList.isEmpty()) {
      double worstLD = Collections.max(lodList);
      result = str(worstLD, NF.D1C2, NF.D1C2Exp);
    }
    return result;
  }

  @Override
  public String tabLodAmol(Isotope isotope, PopulationID populationID) {
    String result = EMPTY_CELL;
    List<Double> lodList = new ArrayList<>();
    for (Sample sample : samples) {
      if (sample instanceof SampleImpl) {
        double[] cts = new double[]{getMaxThr(isotope, populationID, true)};
        double[] quant = ((SampleImpl) sample).applyQuant(cts, isotope, MolarUnit.ATTO_MOL);
        if (quant.length > 0) {
          lodList.add(quant[0]);
        }
      }
    }
    if (!lodList.isEmpty()) {
      double worstLD = Collections.max(lodList);
      result = str(worstLD, NF.D1C2, NF.D1C2Exp);
    }
    return result;
  }

  @Override
  public String tabPNC(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<Double> concs = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null) {
        Population pop = t.getPopulation(populationID);
        if (pop != null) {
          // Note that the synthetic population is not cut if time roi is applied. Hence this "special
          // getter".
          double duration = pop.getEvents().getCheckedTISeries().getDuration();
          int nNP = t.getPopulation(populationID).getEvents().size();

          double te = sample.getPncTEConvention(isotope) / 100d;
          double npPerMin = 60 * nNP / duration;
          npPerMin = npPerMin / te; // we have more NP in reality
          double mLPerMin = sample.getQuant()
              .getExperimentalConditions().getFlowRate(FlowUnit.MILLILITRE_PER_MINUTE);
          double conc = npPerMin / mLPerMin;
          if (Double.isFinite(conc)) {
            concs.add(conc);
          }
        }
      }
    }
    if (!concs.isEmpty()) {
      val = str(mu(concs), NF.D1C3Exp);
    }
    return val;
  }

  @Override
  public String tabPopNpRate(Isotope isotope, PopulationID popID) {
    String val = EMPTY_CELL;
    double sumNP = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(popID))
        .filter(t -> t.getPopulation(popID) != null)
        .mapToDouble(t -> t.getPopulation(popID).getEvents().size())
        .sum();
    double sumDur = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(popID))
        .filter(t -> t.getPopulation(popID) != null)
        .mapToDouble(t -> t.getPopulation(popID).getEvents().getCheckedTISeries().getDuration())
        .sum();
    val = str(sumNP / sumDur, NF.D1C1);
    return val;
  }

  @Override
  public String tabPopNpMean(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.NP, EventParameter.NET_AREA))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.NP, EventParameter.NET_AREA))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C3);
    }
    return val;
  }


  @Override
  public String tabNpMedian(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.NP, EventParameter.NET_AREA))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(md(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabPopNpCustomParamMean(Isotope isotope, PopulationID populationID, EventParameter par,
                                        MathMod math) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> math.calc(t.get(populationID, EventType.NP, par)))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamMedian(Isotope isotope, PopulationID populationID, EventParameter par,
                                       MathMod math) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> math.calc(t.get(populationID, EventType.NP, par)))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(md(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabNpCustomParamSD(Isotope isotope, PopulationID populationID, EventParameter par,
                                   MathMod math) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> math.calc(t.get(populationID, EventType.NP, par)))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanHeight(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.NP, EventParameter.HEIGHT))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSdHeight(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.NP, EventParameter.HEIGHT))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanDuration(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.NP, EventParameter.DURATION))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSdDuration(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.NP, EventParameter.DURATION))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabMeanSize(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(),
            SizeUnit.NANO_METER));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C1, NF.D1C2Exp);
    }
    return val;
  }

  @Override
  public String tabMedianSize(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(),
            SizeUnit.NANO_METER));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(md(allData), NF.D1C1, NF.D1C2Exp);
    }
    return val;
  }


  @Override
  public String tabSizeSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(),
            SizeUnit.NANO_METER));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C2, NF.D1C3Exp);
    }
    return val;
  }


  @Override
  public String tabMeanMass(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(),
            MassUnit.FEMTO_GRAM));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C3, NF.D1C3Exp);
    }
    return val;
  }


  @Override
  public String tabMedianMass(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(),
            MassUnit.FEMTO_GRAM));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(md(allData), NF.D1C3, NF.D1C3Exp);
    }
    return val;
  }

  @Override
  public String tabMassSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(), MassUnit.FEMTO_GRAM));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C4, NF.D1C3Exp);
    }
    return val;
  }


  @Override
  public String tabMeanMol(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(),
            MolarUnit.ATTO_MOL));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C3, NF.D1C3Exp);
    }
    return val;
  }


  @Override
  public String tabMedianMol(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(),
            MolarUnit.ATTO_MOL));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(md(allData), NF.D1C3, NF.D1C3Exp);
    }
    return val;
  }

  @Override
  public String tabMolSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = new ArrayList<>();
    for (Sample sample : samples) {
      Trace t = sample.getTrace(isotope);
      if (t != null && t.hasType(populationID)) {
        data.add(getData(isotope, populationID, EventType.NP,
            quant.getExperimentalConditions().getEventPar(), MolarUnit.ATTO_MOL));
      }
    }
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C4, NF.D1C3Exp);
    }
    return val;
  }

  @Override
  public String tabPopBgMean(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.BG, EventParameter.AREA))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(mu(allData), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabPopBgSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.BG, EventParameter.AREA))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C4);
    }
    return val;
  }

  @Override
  public String tabPopBgN(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    int totalSize = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.BG, EventParameter.AREA))
        .map(d -> d.length)
        .mapToInt(Integer::intValue)
        .sum();
    val = str(totalSize, NF.D1C0);
    return val;
  }

  @Override
  public String tabPopDrift(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    double[] drifts = new double[samples.size()];
    for (int i = 0; i < samples.size(); i++) {
      Sample sample = samples.get(i);
      Trace t = sample.getTrace(isotope);
      if (t != null) {
        Population pop = t.getPopulation(populationID);
        if (pop != null) {
          double df = pop.getDrift();
          if (df == NpPopulation.DEFAULT_DRIFT) {
            df = DriftFactor.calculateDriftFactor(t, populationID);
            pop.setDrift(df);
          }
          drifts[i] = df;
        }
      }
    }
    if (drifts.length > 0) {
      val = str(mu(drifts), NF.D1C1);
    }
    return val;
  }

  @Override
  public String tabBlnDistr(Isotope isotope, PopulationID populationID) {
    return getAllSamples().stream()
        .map(s -> s.tabBlnDistr(isotope, populationID))
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabBlnMean(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<Double> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.getBaseline() != null)
        .filter(t -> t.getBaseline().hasBaseline())
        .map(t -> mu(t.getBaseline().getBackgroundDistribution().getLocation()))
        .collect(Collectors.toList());
    if (data.size() > 0) {
      val = str(mu(data), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabBlnSD(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<double[]> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.hasType(populationID))
        .map(t -> t.get(populationID, EventType.BG, EventParameter.AREA))
        .collect(Collectors.toList());
    double[] allData = ArrUtils.merge(data);
    if (allData.length > 0) {
      val = str(sd(allData), NF.D1C4);
    }
    return val;
  }

  @Override
  public String tabBlnOutlierZ(Isotope isotope, PopulationID populationID) {
    return getAllSamples().stream()
        .map(s -> s.tabBlnOutlierZ(isotope, populationID))
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabEquivBGConc(Isotope isotope) {
    String val = EMPTY_CELL;
    double sumBG = 0;
    int counter = 0;
    for (Sample sample : samples) {
      if (sample instanceof SampleImpl) {
        double bgConc = ((SampleImpl) sample).calcEquivBGConc(isotope);
        if (bgConc > 0) {
          sumBG += bgConc;
          counter++;
        }
      }
    }

    // calc mean conc
    if (counter > 0 && sumBG > 0) {
      val = str(sumBG / counter, NF.D1C3, NF.D1C3Exp);
    }

    return val;
  }

  @Override
  public String tabSearchStart(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<Double> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.getBaseline() != null)
        .filter(t -> t.getBaseline().hasBaseline())
        .filter(t -> t.getPopulation(populationID) != null)
        .map(t -> {
          StatCollection blnStats = t.getBaseline().getBackgroundDistribution();
          Population population = t.getPopulation(populationID);
          ThresholdSupplier supplier = population.getStartInstructions().get(blnStats);
          return mu(supplier.getThresholdSlices());
        })
        .collect(Collectors.toList());
    if (data.size() > 0) {
      val = str(mu(data), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSearchStop(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<Double> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.getBaseline() != null)
        .filter(t -> t.getBaseline().hasBaseline())
        .filter(t -> t.getPopulation(populationID) != null)
        .map(t -> {
          StatCollection blnStats = t.getBaseline().getBackgroundDistribution();
          Population population = t.getPopulation(populationID);
          ThresholdSupplier supplier = population.getStopInstructions().get(blnStats);
          return mu(supplier.getThresholdSlices());
        })
        .collect(Collectors.toList());
    if (data.size() > 0) {
      val = str(mu(data), NF.D1C3);
    }
    return val;
  }

  @Override
  public String tabSearchHeight(Isotope isotope, PopulationID populationID) {
    String val = EMPTY_CELL;
    List<Double> data = getAllSamples().stream()
        .map(s -> s.getTrace(isotope))
        .filter(Objects::nonNull)
        .filter(t -> t.getBaseline() != null)
        .filter(t -> t.getBaseline().hasBaseline())
        .filter(t -> t.getPopulation(populationID) != null)
        .map(t -> {
          StatCollection blnStats = t.getBaseline().getBackgroundDistribution();
          Population population = t.getPopulation(populationID);
          ThresholdSupplier supplier = population.getHeightInstructions().get(blnStats);
          return mu(supplier.getThresholdSlices());
        })
        .collect(Collectors.toList());
    if (data.size() > 0) {
      val = str(mu(data), NF.D1C3);
    }
    return val;
  }

  @Override
  public List<String> tabGates(Isotope isotope, PopulationID populationID) {
    List<List<String>> values = new ArrayList<>();
    for (Sample sample : samples) {
      List<String> gates = sample.tabGates(isotope, populationID);
      for (int i = 0; i < gates.size(); i++) {
        if (values.size() > i) {
          values.get(i).add(gates.get(i));
        } else {
          List<String> list = new ArrayList<>();
          list.add(gates.get(i));
          values.add(list);
        }
      }
    }
    List<String> result = values.stream()
        .map(inner -> String.join(" --- ", inner))
        .collect(Collectors.toList());
    return result;
  }

  @Override
  public String tabSearchStartMeta(Isotope isotope, PopulationID populationID) {
    return getAllSamples().stream()
        .map(s -> s.tabSearchStartMeta(isotope, populationID))
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabSearchStopMeta(Isotope isotope, PopulationID populationID) {
    return getAllSamples().stream()
        .map(s -> s.tabSearchStopMeta(isotope, populationID))
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public String tabSearchHeightMeta(Isotope isotope, PopulationID populationID) {
    return getAllSamples().stream()
        .map(s -> s.tabSearchHeightMeta(isotope, populationID))
        .collect(Collectors.joining(" --- "));
  }

  @Override
  public List<String> tabGatesMeta(Isotope isotope, PopulationID populationID) {
    List<List<String>> values = new ArrayList<>();
    for (Sample sample : samples) {
      List<String> gates = sample.tabGatesMeta(isotope, populationID);
      for (int i = 0; i < gates.size(); i++) {
        if (values.size() > i) {
          values.get(i).add(gates.get(i));
        } else {
          List<String> list = new ArrayList<>();
          list.add(gates.get(i));
          values.add(list);
        }
      }
    }
    List<String> result = values.stream()
        .map(inner -> String.join(" --- ", inner))
        .collect(Collectors.toList());
    return result;
  }

}
