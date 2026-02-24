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

import analysis.Baseline;
import analysis.MCEventCollection;
import analysis.MCPopulation;
import analysis.Population;
import analysis.PopulationID;
import dataModelNew.mz.MZValue;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.DataFlag;
import processing.options.PopulationType;


public class TraceMC extends TraceImpl implements Trace, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(TraceMC.class.getName());

  private final double macroDTSeconds;
  private double empiricalMeanBG = 0;
  private double expectedMeanBG = 0;

  // Dummy
  public TraceMC() {
    super();
    this.macroDTSeconds = 0;
  }

  public TraceMC(Sample sample, MZValue mzValue, TISeries tiSeries, double macroDTSeconds) {
    super(sample, mzValue, tiSeries);
    this.macroDTSeconds = macroDTSeconds;
    // always add simulation as population
    PopulationID id = new PopulationID(PopulationType.SIMULATION);
    super.addOverridePopulation(id,
        new MCPopulation(id, id.toString(), new MCEventCollection(this)));
  }

  // Deep copy: note that the populations are cloned in the super class, we may pass direct pointers
  public TraceMC(Sample parentSample,
                 MZValue mzValue,
                 TISeries tiSeries,
                 TISeries tiSeriesCopy,
                 HashMap<DataFlag, List<Integer>> rawDataFlags,
                 double macroDTSeconds,
                 double empiricalMeanBG,
                 double expectedMeanBG,
                 Baseline baseline,
                 HashMap<PopulationID, Population> populations) {

    super(parentSample, mzValue, tiSeries,
        tiSeriesCopy, rawDataFlags, baseline,
        populations);
    this.macroDTSeconds = macroDTSeconds;
    this.empiricalMeanBG = empiricalMeanBG;
    this.expectedMeanBG = expectedMeanBG;
  }

  @Override
  public Trace copy(Sample newSample) {
    return new TraceMC(
        newSample,
        super.mzValue.copy(),
        super.tiSeries.copy(),
        super.tiSeriesCopy.copy(),
        super.rawDataFlags,
        macroDTSeconds,
        empiricalMeanBG,
        expectedMeanBG,
        baseline.copy(),
        super.populations);
  }

  @Override
  public void clearEvaluation() {
    // make sure to keep the simulation populations as these are not generated during reprocessing
    HashMap<PopulationID, Population> mcPops = new LinkedHashMap<>();
    for (PopulationID id : populations.keySet()) {
      if (id.getType().equals(PopulationType.SIMULATION)) {
        mcPops.put(id, populations.get(id));
      }
    }
    super.clearEvaluation();
    super.populations.putAll(mcPops);
  }

  //----------------------

  public double getMacroDTSeconds() {
    return macroDTSeconds;
  }

  public void setEmpiricalMeanBG(double empiricalMeanBG) {
    this.empiricalMeanBG = empiricalMeanBG;
  }

  public double getEmpiricalMeanBG() {
    return empiricalMeanBG;
  }

  public void setExpectedMeanBG(double expectedMeanBG) {
    this.expectedMeanBG = expectedMeanBG;
  }

  public double getExpectedMeanBG() {
    return expectedMeanBG;
  }
}
