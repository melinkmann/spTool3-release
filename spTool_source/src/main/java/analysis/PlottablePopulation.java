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

import analysis.PlottablePeakTrace.CollectionTrace;
import javax.annotation.Nullable;
import visualizer.styles.Colors;
import visualizer.styles.MarkerStyle;

public interface PlottablePopulation {

  String getPopulationLabel();

  @Nullable
  public Colors getColor();

  @Nullable
  public MarkerStyle getMarker();


  double[] getTimerMarker();

  int size();

  double getDwellTime();

  int getRawDataSize();

  public static class PlottableMatrix implements PlottablePopulation {

    private final String popLabel;
    private final Colors color;
    private final MarkerStyle marker;
    private final double[] timerMarker;
    // dwell time & size are needed for some calculations
    private final double dwellTime;
    private final int rawDataSize;


    public PlottableMatrix(String popLabel, Colors color, MarkerStyle marker,
        double[] timeMarker,
        double dwellTime,
        int rawDataSize) {
      this.popLabel = popLabel;
      this.color = color;
      this.marker = marker;
      this.timerMarker = timeMarker;
      this.dwellTime = dwellTime;
      this.rawDataSize = rawDataSize;
    }

    @Override
    public String getPopulationLabel() {
      return popLabel;
    }

    @Override
    public Colors getColor() {
      return color;
    }

    @Override
    public MarkerStyle getMarker() {
      return marker;
    }

    @Override
    public double[] getTimerMarker() {
      return timerMarker;
    }

    @Override
    public int size() {
      return timerMarker.length;
    }

    @Override
    public double getDwellTime() {
      return dwellTime;
    }

    public int getRawDataSize() {
      return rawDataSize;
    }
  }

  public static class PlottableEventCollection implements PlottablePopulation {

    private final String popLabel;
    private final double[] timeMarkers;
    private final double dwellTime;
    private final int rawDataSize;

    public PlottableEventCollection(CollectionTrace plottablePeakTrace) {
      this.popLabel = plottablePeakTrace.getPopulation().getName();
      this.timeMarkers = plottablePeakTrace.getMarkerData().getTime();
      this.dwellTime = plottablePeakTrace.getDwellTime();
      this.rawDataSize = plottablePeakTrace.getRawDataSize();
    }

    @Override
    public String getPopulationLabel() {
      return popLabel;
    }

    // Returns null so that calling function can null check and else use iterator
    @Override
    public Colors getColor() {
      return null;
    }

    // Returns null so that calling function can null check and else use iterator
    @Override
    public MarkerStyle getMarker() {
      return null;
    }

    @Override
    public double[] getTimerMarker() {
      return timeMarkers;
    }

    @Override
    public int size() {
      return timeMarkers.length;
    }

    @Override
    public double getDwellTime() {
      return dwellTime;
    }

    @Override
    public int getRawDataSize() {
      return rawDataSize;
    }
  }


}
