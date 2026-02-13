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

import analysis.Baseline.ThrFormalism;
import analysis.Baseline.ThrMeasureOfSignificance;
import dataModelNew.TISeries;
import dataModelNew.TISeriesRAM;
import dataModelNew.Trace;
import dataModelNew.TraceMC;
import dataModelNew.mz.IsotopeMZ;
import dataModelNew.mz.MZValue;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.PopulationType;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.ParticlePopulationMatrix;
import sandbox.montecarlo.ParticlePopulationMatrixRAM;
import util.ArrUtils;
import visualizer.styles.MarkerStyle;

public class MCPopulation implements Population, Serializable {

  static final long serialVersionUID = 1L; //assign a long value

  private static final Logger LOGGER = LogManager.getLogger(MCPopulation.class);

  private final PopulationID id;
  private String name;
  private final EventCollection mainEventCollection;
  private List<MZValue> contributingMZs; // always empty as this type of Pop is not "aligned"

  // Dummy
  public MCPopulation() {
    this.id = new PopulationID(PopulationType.SIMULATION);
    this.name = "Empty population";
    this.mainEventCollection = new MCEventCollection();
    this.contributingMZs = new ArrayList<>();
  }

  // Main constructor
  public MCPopulation(PopulationID id, String name, MCEventCollection mainEventCollection) {
    this.id = id;
    this.name = name;
    this.mainEventCollection = mainEventCollection;
    this.contributingMZs = new ArrayList<>();
  }

  // Deep copy
  private MCPopulation(PopulationID id, String name, EventCollection mainEventCollection) {
    this.id = id;
    this.name = name;
    this.mainEventCollection = mainEventCollection;
    this.contributingMZs = new ArrayList<>();
  }

  @Override
  public Population copy(Trace newTrace) {
    return new MCPopulation(id, name, mainEventCollection.copy(newTrace));
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
    return new PopParSummary();
  }

  @Override
  public String translateParams() {
    return "";
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public double getDrift() {
    return NpPopulation.DEFAULT_DRIFT;
  }

  @Override
  public void setDrift(double drift) {
    // do nothing
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
    this.contributingMZs = contributingMZs;
  }

  @Override
  public EventCollection getBgDefiningCollection() {
    return mainEventCollection.getBackgroundDefiningCollection();
  }

  @Override
  public ThresholdSupplierInstructions getStartInstructions() {
    return new ThresholdSupplierInstructions(
        "Default in-silico THR",
        ThrFormalism.STATIC_VALUE,
        ThrMeasureOfSignificance.FACTOR,
        ParticlePopulationMatrixRAM.THR,
        0d);
  }

  @Override
  public ThresholdSupplierInstructions getStopInstructions() {
    return new ThresholdSupplierInstructions(
        "Default in-silico THR",
        ThrFormalism.STATIC_VALUE,
        ThrMeasureOfSignificance.FACTOR,
        ParticlePopulationMatrixRAM.THR,
        0d);
  }

  @Override
  public ThresholdSupplierInstructions getHeightInstructions() {
    return new ThresholdSupplierInstructions(
        "Default in-silico THR",
        ThrFormalism.STATIC_VALUE,
        ThrMeasureOfSignificance.FACTOR,
        ParticlePopulationMatrixRAM.THR,
        0d);
  }

  @Override
  public List<ThresholdSupplierInstructions> getGatingInstr() {
    return new ArrayList<>();
  }

  @Override
  public boolean isEquivalent(Population population) {
    return this.equals(population);
  }

  @Override
  public PlottableEventMarkers getPeakMarkers() {
    TISeries markers = new TISeriesRAM();
    int size = mainEventCollection.size();
    String mzLabel = "";
    if (size > 0) {
      List<Double> timeMarkers = new ArrayList<>(size);
      List<Double> intensityMarkers = new ArrayList<>(size);

      // it should be MC but check to avoid cast issues
      if (mainEventCollection.getTrace() instanceof TraceMC) {
        TraceMC mcTrace = ((TraceMC) mainEventCollection.getTrace());
        mzLabel = mcTrace.getMzValue().getName();

        // use this DT not the macro!!: there might be aliasing from e.g. DT=9.999999999 ms and a grid time of e.g. 5 µs would cause DT=9.999995 ms
        double macroDT = mcTrace.getTISeries().getDT();
        double firstTimeStamp = mcTrace.getTISeries().getFirstTimeStamp();
        double lastTimeStamp = mcTrace.getTISeries().getLastTimeStamp();
        boolean isRestricted = mcTrace.hasLimits();

        List<ParticlePopulationMatrix> particleMatrices = mcTrace.getSample().getMatrices(mcTrace);

        ////////////////////////////////////////////////////////////////////////
        // For each trace, we iterate over the populations/matrices to find peaks with it
        for (ParticlePopulationMatrix population : particleMatrices) {
          ParticlePopulationMatrixRAM populationRAM = population.getNewRamInstance();

          // get the indices: calculates based on the TRUE POINT of occurrence.
          HashMap<Isotope, int[]> popMarkers = populationRAM.getPeakMarkerMacroIndices(macroDT);

          // Check to prevent null pointer (in principle Trace should only have Matrices with
          // their isotope, but better save than run time exception).
          Isotope isotope = mcTrace.getMzValue().getIsotope();
          if (popMarkers.containsKey(isotope)) {

            // calc intensity at that index
            int[] macroIndices = popMarkers.get(isotope);
            // the index calculation does not account for cutting a time roi.
            // Hence, base calculations on original series
            double[] macroSignalArray = mcTrace.getOriginalTISeries().getIntensity();

            if (macroSignalArray.length > 0) {
              for (int macroFrameIdx : macroIndices) {
                // Somehow we get index out if bounds exceptions here...
                macroFrameIdx = Math.min(macroFrameIdx, macroSignalArray.length - 1);
                // +1: At index=0, we have the first time stamp, i.e., "1*DT ms"
                double macroFrameStamp = macroDT * (macroFrameIdx + 1);
                // Here, no +1 as we access the index in an array! (+1 only when converting index to time stamp)4
                double macroFrameSignal = macroSignalArray[macroFrameIdx];
                // Try to make plot nicer: only show what is within time limits
                if (!isRestricted ||
                    (firstTimeStamp <= macroFrameStamp && macroFrameStamp <= lastTimeStamp)) {
                  timeMarkers.add(macroFrameStamp);
                  intensityMarkers.add(macroFrameSignal);
                }
              }
            } else {
              // This happened in the past due to issues in the data structure.
              // This helps understanding/debugging.
              LOGGER.error("Expected valid array of signal intensity but array was empty.");
            }
          }
        }
        ////////////////////////////////////////////////////////////////////////

      } else {
        LOGGER.error("Expected simulated Trace but obtained another instance. "
            + "If you are reading this message, an unexpected operation or coding error has occurred.");
      }
      markers = new TISeriesRAM(timeMarkers, intensityMarkers);
    }
    return new PlottableEventMarkers(getEventMarkerColor(), getEventMarkerStyle(), markers,
        getName(), mzLabel);
  }

  @Override
  public List<PlottableSubPopulation> getPopulationMarkers() {
    List<PlottableSubPopulation> result = new ArrayList<>();

    // it should be MC but check to avoid cast issues
    if (mainEventCollection.getTrace() instanceof TraceMC) {
      TraceMC mcTrace = ((TraceMC) mainEventCollection.getTrace());

      /*
      Here we have the issue, that this list of matrices also contains data on other traces.
      In the UI, we cannot just show all particle population matrices of all traces,
      as there are not unique. Hence, we store the contributing isotopes and later,
      at the plotting stage, have to ensure that the list of PlottableSubPopulations
      it unique, for each respective PopulationID.
      It is important to only reduce for each ID, as we do want to show data on that same
      isotope for DIFFERENT IDs.
       */
      List<ParticlePopulationMatrix> particleMatrices = mcTrace.getSample().getMatrices(mcTrace);
      List<Isotope> contributingIsotopes = particleMatrices.stream()
          .map(ParticlePopulationMatrix::listIsotopes)
          .flatMap(Collection::stream)
          .distinct()
          .collect(Collectors.toList());

      // iterate over the matrices (each matrix is one population)
      for (ParticlePopulationMatrix matrix : particleMatrices) {
        ParticlePopulationMatrixRAM matrixRAM = matrix.getNewRamInstance();

        // check if time was cut
        double firstTimeStamp = mcTrace.getTISeries().getFirstTimeStamp();
        double lastTimeStamp = mcTrace.getTISeries().getLastTimeStamp();

        double[] meanArrivalTimesOfPopulation = matrixRAM.getMeanArrivalTimes();
        if (mcTrace.hasLimits()) {
          meanArrivalTimesOfPopulation = ArrUtils.filterInclusively(meanArrivalTimesOfPopulation,
              firstTimeStamp, lastTimeStamp);
        }

        result.add(new PlottableSubPopulation(matrix.getLabel(),
            matrix.getColor(),
            matrix.getMarker(),
            meanArrivalTimesOfPopulation,
            contributingIsotopes,
            matrix,
            mcTrace.getTISeries().size(),
            mcTrace.getTISeries().getDT()
        ));
      }

    }
    return result;
  }

  @Override
  public MarkerStyle getEventMarkerStyle() {
    return MarkerStyle.TRIANGLE_DOWN;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    if (contributingMZs == null) {
      this.contributingMZs = new ArrayList<>();
    }
  }

}
