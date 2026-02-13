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
import dataModelNew.TISeries;
import dataModelNew.Trace;
import sandbox.montecarlo.Isotope;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;
import visualizer.styles.MarkerStyle;
import visualizer.styles.OkabeItoColors;

public interface PlottablePeakTrace {

  String getPopLabelForPlot();

  String getPopulationLabel();

  String getMZLabel();

  TISeries getMarkerData();

  MarkerStyle getMarker();

  Colors getColor();

  public static class MatrixPeakTrace implements PlottablePeakTrace {

    private final TISeries markerData;
    private final String mzLabel;
    private final String populationLabel;

    public MatrixPeakTrace(Trace trace, PopulationID popID, TISeries data) {
      this.markerData = data;
      this.populationLabel = popID.toString();
      if (trace.getMzValue().hasIsotope()) {
        this.mzLabel = trace.getMzValue().getIsotope().getName();
      } else {
        this.mzLabel = trace.getMzValue().getElementTransition();
      }
    }

    @Override
    public String getPopLabelForPlot() {
      return "Simulated peaks";
    }

    @Override
    public String getPopulationLabel() {
      return populationLabel;
    }

    @Override
    public String getMZLabel() {
      return mzLabel;
    }

    @Override
    public TISeries getMarkerData() {
      return markerData;
    }

    @Override
    public MarkerStyle getMarker() {
      return MarkerStyle.TRIANGLE_DOWN;
    }

    @Override
    public Colors getColor() {
      return OkabeItoColors.BLACK_DARK;
    }


  }


  public static class CollectionTrace implements PlottablePeakTrace {

    private final Population population;
    private final TISeries markerData;

    public CollectionTrace(Population population, TISeries markerData) {
      this.population = population;
      this.markerData = markerData;
    }

    @Override
    public String getPopLabelForPlot() {
      return "Detected peaks";
    }

    @Override
    public String getPopulationLabel() {
      return population.getName();
    }

    @Override
    public String getMZLabel() {
      Trace trace = population.getEvents().getTrace();
      if (trace.getMzValue().hasIsotope()) {
        return trace.getMzValue().getIsotope().getName();
      } else {
        return trace.getMzValue().getElementTransition();
      }
    }

    @Override
    public TISeries getMarkerData() {
      return markerData;
    }

    public Population getPopulation() {
      return population;
    }

    public double getDwellTime() {
      return population.getEvents().getCheckedTISeries().getDT();
    }

    public int getRawDataSize() {
      return population.getEvents().getCheckedTISeries().size();
    }

    @Override
    public MarkerStyle getMarker() {
      return MarkerStyle.BAR;
    }

    @Override
    public Colors getColor() {
      Colors color;
      Trace trace = population.getEvents().getTrace();
      if (trace.getMzValue().hasIsotope()) {
        Isotope isotope = trace.getMzValue().getIsotope();
        color = SpTool3Main.getRunTime().getConfParams().getColor(isotope);
      } else {
        color = new SpColor(trace.getSample().getColor());
      }
      return color;
    }
  }
}
