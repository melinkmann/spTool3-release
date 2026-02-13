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

import dataModelNew.mz.IsotopeMZ;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.ParticlePopulationMatrix;
import visualizer.styles.Colors;
import visualizer.styles.MarkerStyle;
import visualizer.styles.OkabeItoColors;

public class PlottableSubPopulation {

  private final String popLabel;
  private final Colors color;
  private final MarkerStyle marker;
  private final double[] timerMarkers;
  private final List<Isotope> coveredIsotopes;
  @Nullable
  private final ParticlePopulationMatrix matrix;
  // for the export only, which uses the same structure
  private final int rawDataSize;
  private final double rawDwellTime;

  // Dummy
  public PlottableSubPopulation() {
    this.popLabel = "Empty population";
    this.color = OkabeItoColors.BLACK;
    this.marker = MarkerStyle.CIRCLE;
    this.timerMarkers = new double[0];
    this.coveredIsotopes = new ArrayList<>();
    this.matrix = null;
    this.rawDataSize = 0;
    this.rawDwellTime = 0;
  }

  public PlottableSubPopulation(String popLabel, Colors color, MarkerStyle marker,
      double[] timerMarker, List<Isotope> coveredIsotopes, ParticlePopulationMatrix matrix,
      int rawDataSize, double rawDwellTime) {
    this.popLabel = popLabel;
    this.color = color;
    this.marker = marker;
    this.timerMarkers = timerMarker;
    this.coveredIsotopes = new ArrayList<>(coveredIsotopes);
    this.matrix = matrix;
    this.rawDataSize = rawDataSize;
    this.rawDwellTime = rawDwellTime;
  }

  public PlottableSubPopulation(String popLabel, Colors color, MarkerStyle marker,
      double[] timerMarker, List<Isotope> coveredIsotopes, int rawDataSize, double rawDwellTime) {
    this(popLabel, color, marker, timerMarker, coveredIsotopes, null, rawDataSize, rawDwellTime);
  }

  public int size() {
    return timerMarkers.length;
  }

  public MarkerStyle getMarker() {
    return marker;
  }

  public Colors getColor() {
    return color;
  }

  public String getPopLabel() {
    return popLabel;
  }

  public double[] getTimerMarkers() {
    return timerMarkers;
  }

  public List<Isotope> getCoveredIsotopes() {
    return coveredIsotopes;
  }

  public int getRawDataSize() {
    return rawDataSize;
  }

  public double getRawDwellTime() {
    return rawDwellTime;
  }

  public String getMzID() {
    return coveredIsotopes.stream()
        .map(Isotope::getName)
        .collect(Collectors.joining("_"));
  }

  public boolean isSameMatrix(PlottableSubPopulation pop) {
    return (matrix != null && pop.matrix != null) && (matrix == pop.matrix);
  }
}
