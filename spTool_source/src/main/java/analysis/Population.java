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

import core.SpTool3Main;
import dataModelNew.Trace;
import java.io.Serializable;
import java.util.List;

import dataModelNew.mz.MZValue;
import sandbox.montecarlo.Isotope;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;
import visualizer.styles.MarkerStyle;

public interface Population extends Serializable {

  Population copy(Trace newTrace);

  PopulationID getId();

  String getName();

  PopParSummary getInputSummary();

  // For anything beyond search and gating (which are stored anyway): e.g., filters
  String translateParams();

  void setName(String name);

  double getDrift();

  public void setDrift(double drift);

  EventCollection getEvents();

  List<MZValue> getContributingMZs();

  void setContributingMZs(List<MZValue> mZs);

  // for the subEventCollection
  EventCollection getBgDefiningCollection();

  ThresholdSupplierInstructions getStartInstructions();

  ThresholdSupplierInstructions getStopInstructions();

  ThresholdSupplierInstructions getHeightInstructions();

  List<ThresholdSupplierInstructions> getGatingInstr();

  boolean isEquivalent(Population population);

  PlottableEventMarkers getPeakMarkers();

  List<PlottableSubPopulation>  getPopulationMarkers();

  MarkerStyle getEventMarkerStyle();

  default Colors getEventMarkerColor() {
    Colors color;
    Trace trace = getEvents().getTrace();
    if (trace.getMzValue().hasIsotope()) {
      Isotope isotope = trace.getMzValue().getIsotope();
      color = SpTool3Main.getRunTime().getConfParams().getColor(isotope);
    } else {
      color = new SpColor(trace.getSample().getColor());
    }
    return color;
  }

}
