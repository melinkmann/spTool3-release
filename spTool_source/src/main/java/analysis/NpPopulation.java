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
import dataModelNew.TISeries;
import dataModelNew.TISeriesRAM;
import dataModelNew.Trace;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import dataModelNew.mz.MZValue;
import processing.options.SearchAlgorithm;
import util.ArrUtils;
import visualizer.styles.MarkerStyle;

public class NpPopulation implements Population, Serializable {

  static final long serialVersionUID = 1L; //assign a long value
  public static final int DEFAULT_DRIFT = -2;

  private final PopulationID id;
  private final EventCollection mainEventCollection;
  private String name;
  private final PopParSummary inputSummary;
  private List<MZValue> contributingMZs;

  private double drift;

  // keep track of thresholding
  private final ThresholdSupplierInstructions startInstructions;
  private final ThresholdSupplierInstructions stopInstructions;
  private final ThresholdSupplierInstructions heightInstructions;
  private final List<ThresholdSupplierInstructions> gatingInstr;

  /*
  TODO: Store the spectral data in the sample.
   - It does not make any sense to have a copy in each population in each trace.
   - Keep a reference to the SAME SpectralArray to avoid losing object identity
     when serialization is involved.
   - Restructure the NpPop class to essentially remove all references/redirect to
     the HasMap in the sample.
   - Adjust export as well as creation of the spectra...
   */

  // Dummy
  public NpPopulation() {
    this.id = new PopulationID();
    this.mainEventCollection = new MainEventCollection();
    this.name = "Empty population";
    this.drift = DEFAULT_DRIFT;
    this.startInstructions = new ThresholdSupplierInstructions();
    this.stopInstructions = new ThresholdSupplierInstructions();
    this.heightInstructions = new ThresholdSupplierInstructions();
    this.gatingInstr = new ArrayList<>();
    this.inputSummary = new PopParSummary();
    this.contributingMZs = new ArrayList<>();
  }

  // Create an "Incomplete population"
  public NpPopulation(PopulationID id, EventCollection mainEventCollection) {
    this.id = id;
    this.mainEventCollection = mainEventCollection;
    this.name = id.toString();
    this.drift = DEFAULT_DRIFT;
    this.startInstructions = new ThresholdSupplierInstructions();
    this.stopInstructions = new ThresholdSupplierInstructions();
    this.heightInstructions = new ThresholdSupplierInstructions();
    this.gatingInstr = new ArrayList<>();
    this.inputSummary = new PopParSummary();
    this.contributingMZs = new ArrayList<>();
  }

  // Create an "Incomplete population" ROI
  public NpPopulation(PopulationID id, EventCollection mainEventCollection, PopParSummary popParSummary) {
    this.id = id;
    this.mainEventCollection = mainEventCollection;
    this.name = id.toString();
    this.drift = DEFAULT_DRIFT;
    this.startInstructions = new ThresholdSupplierInstructions();
    this.stopInstructions = new ThresholdSupplierInstructions();
    this.heightInstructions = new ThresholdSupplierInstructions();
    this.gatingInstr = new ArrayList<>();
    this.inputSummary = popParSummary.copy();
    this.contributingMZs = new ArrayList<>();
  }

  // Gating
  public NpPopulation(PopulationID id, Population npPopulation,
                      EventCollection mainEventCollection,
                      String newName,
                      ThresholdSupplierInstructions gatingInstr) {
    this.id = id;
    this.mainEventCollection = mainEventCollection;
    this.name = newName;
    this.drift = DEFAULT_DRIFT;
    this.startInstructions = npPopulation.getStartInstructions();
    this.stopInstructions = npPopulation.getStopInstructions();
    this.heightInstructions = npPopulation.getHeightInstructions();
    this.gatingInstr = new ArrayList<>(npPopulation.getGatingInstr());
    this.gatingInstr.add(gatingInstr);
    this.inputSummary = npPopulation.getInputSummary(); // keep summary from search
    this.contributingMZs = new ArrayList<>(npPopulation.getContributingMZs());
  }

  // Align
  public NpPopulation(PopulationID id,
                      Population npPopulation,
                      EventCollection mainEventCollection,
                      String newName,
                      List<MZValue> contributingMZs) {
    this.id = id;
    this.mainEventCollection = mainEventCollection;
    this.name = newName;
    this.drift = DEFAULT_DRIFT;
    this.startInstructions = npPopulation.getStartInstructions();
    this.stopInstructions = npPopulation.getStopInstructions();
    this.heightInstructions = npPopulation.getHeightInstructions();
    this.gatingInstr = new ArrayList<>(npPopulation.getGatingInstr());
    this.inputSummary = npPopulation.getInputSummary(); // keep summary from search
    this.contributingMZs = new ArrayList<>(npPopulation.getContributingMZs());
    for (MZValue mz : contributingMZs) {
      if (!this.contributingMZs.contains(mz)) {
        this.contributingMZs.add(mz);
      }
    }
  }

  // Filtering
  public NpPopulation(PopulationID id, Population npPopulation,
                      EventCollection mainEventCollection,
                      String newName,
                      PopParSummary popParSummary) {
    this.id = id;
    this.mainEventCollection = mainEventCollection;
    this.name = newName;
    this.drift = DEFAULT_DRIFT;
    this.startInstructions = npPopulation.getStartInstructions();
    this.stopInstructions = npPopulation.getStopInstructions();
    this.heightInstructions = npPopulation.getHeightInstructions();
    this.gatingInstr = new ArrayList<>(npPopulation.getGatingInstr());
    // copy: else, we would write/merge the filter instructions into the gating instructions
    this.inputSummary = npPopulation.getInputSummary().copy();
    this.inputSummary.mergeInto(popParSummary);
    this.contributingMZs = new ArrayList<>(npPopulation.getContributingMZs());
  }

  // Search, i.e., no gating
  public NpPopulation(PopulationID id, EventCollection mainEventCollection, String name,
                      ThresholdSupplierInstructions startInstructions,
                      ThresholdSupplierInstructions stopInstructions,
                      ThresholdSupplierInstructions heightInstructions,
                      PopParSummary popParSummary) {
    this.id = id;
    this.mainEventCollection = mainEventCollection;
    this.name = name;
    this.drift = DEFAULT_DRIFT;
    this.startInstructions = startInstructions;
    this.stopInstructions = stopInstructions;
    this.heightInstructions = heightInstructions;
    this.gatingInstr = new ArrayList<>();
    this.inputSummary = popParSummary; // store input summary from search
    this.contributingMZs = new ArrayList<>();
  }

  // Deep copy
  public NpPopulation(PopulationID id, EventCollection mainEventCollection, String name,
                      double drift,
                      ThresholdSupplierInstructions startInstructions,
                      ThresholdSupplierInstructions stopInstructions,
                      ThresholdSupplierInstructions heightInstructions,
                      List<ThresholdSupplierInstructions> gatingInstr,
                      PopParSummary inputSummary,
                      List<MZValue> contributingMZs
  ) {
    this.id = id;
    this.mainEventCollection = mainEventCollection;
    this.name = name;
    this.drift = drift;
    this.startInstructions = startInstructions;
    this.stopInstructions = stopInstructions;
    this.heightInstructions = heightInstructions;
    this.gatingInstr = gatingInstr;
    this.inputSummary = inputSummary;
    this.contributingMZs = contributingMZs;
  }

  @Override
  public Population copy(Trace newTrace) {

    List<ThresholdSupplierInstructions> newGatingInstr = new ArrayList<>();
    for (ThresholdSupplierInstructions gateInstr : gatingInstr) {
      newGatingInstr.add(gateInstr.copy());
    }

    List<MZValue> contributingMZs = this.contributingMZs.stream()
        .map(MZValue::copy)
        .toList();

    return new NpPopulation(id,
        mainEventCollection.copy(newTrace),
        name,
        drift,
        startInstructions.copy(),
        stopInstructions.copy(),
        heightInstructions.copy(),
        newGatingInstr,
        inputSummary.copy(),
        new ArrayList<>(contributingMZs)
    );
  }

  public PopulationID getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public PopParSummary getInputSummary() {
    return inputSummary;
  }

  @Override
  public String translateParams() {
    return inputSummary.translate();
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public double getDrift() {
    return drift;
  }

  @Override
  public void setDrift(double drift) {
    this.drift = drift;
  }


  @Override
  public EventCollection getEvents() {
    return mainEventCollection;
  }

  @Override
  public List<MZValue> getContributingMZs() {
    return contributingMZs;
  }

  @Override
  public void setContributingMZs(List<MZValue> contributingMZs) {
    contributingMZs.sort(Comparator.comparingInt(o -> o.getIsotope().getIsotopicNumber()));
    this.contributingMZs = contributingMZs;
  }

  /**
   * We define background as "anything that is not event". Here, the problem is that ROIs or
   * filtered populations (e.g. MultiEvents with n=3 or  over range events) have many DP that are in
   * fact events but not events of that population. Hence, we need a reference to the original
   * population where the true decision "event or not" is stored.
   */
  @Override
  public EventCollection getBgDefiningCollection() {
    EventCollection bgCollection = mainEventCollection.getBackgroundDefiningCollection();
    return bgCollection;
  }

  @Override
  public ThresholdSupplierInstructions getStartInstructions() {
    return startInstructions;
  }

  @Override
  public ThresholdSupplierInstructions getStopInstructions() {
    return stopInstructions;
  }

  @Override
  public ThresholdSupplierInstructions getHeightInstructions() {
    return heightInstructions;
  }

  @Override
  public List<ThresholdSupplierInstructions> getGatingInstr() {
    return gatingInstr;
  }

  @Override
  public boolean isEquivalent(Population population) {
    return this.equals(population);
  }

  @Override
  public PlottableEventMarkers getPeakMarkers() {
    TISeries markers = new TISeriesRAM();
    String mzLabel = "";
    if (mainEventCollection.size() > 0) {
      Trace trace = mainEventCollection.getTrace();
      mzLabel = trace.getMzValue().getName();
      TISeries rawSeries = trace.getTISeries();
      double[] time = rawSeries.getTime();
      double[] intensity = rawSeries.getIntensity();

      List<Double> timeMarkers = new ArrayList<>();
      List<Double> intensityMarkers = new ArrayList<>();

      for (Event event : mainEventCollection.getNpEvents()) {
        int peakIdx = event.getPeak();
        timeMarkers.add(time[peakIdx]);
        intensityMarkers.add(intensity[peakIdx]);
      }

      markers = new TISeriesRAM(timeMarkers, intensityMarkers);
    }
    return new PlottableEventMarkers(getEventMarkerColor(), getEventMarkerStyle(), markers,
        getName(), mzLabel);
  }

  /**
   * Without alignment, this is the same as the peak markers time stamp. // TODO: For aligned
   * population, this will make sense!
   */
  @Override
  public List<PlottableSubPopulation> getPopulationMarkers() {
    List<PlottableSubPopulation> results = new ArrayList<>();

    TISeries rawSeries = mainEventCollection.getCheckedTISeries();
    double[] time = rawSeries.getTime();

    List<Double> timeMarkers = new ArrayList<>();

    for (Event event : mainEventCollection.getNpEvents()) {
      int peakIdx = event.getPeak();
      timeMarkers.add(time[peakIdx]);
    }

    double[] markers = ArrUtils.doubleListToArr(timeMarkers);
    Trace trace = mainEventCollection.getTrace();
    Sample sample = trace.getSample();

    // some visual distinction
    MarkerStyle markerStyle = MarkerStyle.CROSS_UPRIGHT;
    if (AnalysisUtils.isAligned(id)) {
      markerStyle = MarkerStyle.DIAMOND;
    }

    if (AnalysisUtils.isPVal(id)) {
      markerStyle = MarkerStyle.CIRCLE;
    }

    results.add(new PlottableSubPopulation(
        getName(),
        mainEventCollection.getTrace().getColor(sample),
        markerStyle,
        markers,
        Collections.singletonList(trace.getMzValue().getIsotope()),
        trace.getTISeries().size(),
        trace.getTISeries().getDT()
    ));

    return results;
  }

  @Override
  public MarkerStyle getEventMarkerStyle() {
    return MarkerStyle.BAR;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    if (contributingMZs == null) {
      this.contributingMZs = new ArrayList<>();
    }

  }
}
