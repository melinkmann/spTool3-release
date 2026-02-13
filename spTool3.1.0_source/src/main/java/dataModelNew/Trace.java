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
import analysis.Population;
import analysis.PopulationID;
import core.SpTool3Main;
import dataModelNew.mz.MZValue;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

import org.jfree.data.xy.XYSeries;
import processing.options.DataFlag;
import processing.options.EventParameter;
import processing.options.EventType;
import sandbox.montecarlo.Isotope;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;

import javax.annotation.Nullable;

public interface Trace extends Serializable {

  Trace copy(Sample newSample);

  Sample getSample();

  MZValue getMzValue();

  TISeries getTISeries();

  TISeries getOriginalTISeries();

  void setFlags(DataFlag flag, List<Integer> indices);

  HashSet<Integer> getFlags(DataFlag flag);

  public void setBaseline(Baseline baseline);

  public Baseline getBaseline();

  boolean hasType(PopulationID id);

  List<PopulationID> getAllPopulationsTypes();

  List<Population> getAllPopulations();

  void addOverridePopulation(PopulationID id, Population population);

  void removePopulation(PopulationID id);

  // Returns empty Population of not present instead of null
  Population getPopulation(PopulationID id);

  public int getNoOfEvents(PopulationID id);

  public double[] get(PopulationID id, EventType eventType, EventParameter parameter);

  public void setTISeriesLimits(double lower, double upper);

  public void resetTISeriesLimits();

  boolean hasLimits();

  public void voidClearEvaluation();

  XYSeries getXYSeries();

  void clearXYSeriesCache();

  // void setXYSeries(XYSeries xySeries);

  default Colors getColor(Sample sample) {
    Colors color;
    if (getMzValue().hasIsotope()) {
      Isotope isotope = getMzValue().getIsotope();
      color = SpTool3Main.getRunTime().getConfParams().getColor(isotope);
    } else {
      color = new SpColor(sample.getColor());
    }
    return color;
  }

}
