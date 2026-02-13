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

import dataModelNew.TISeries;
import dataModelNew.TISeriesRAM;
import visualizer.styles.Colors;
import visualizer.styles.MarkerStyle;
import visualizer.styles.OkabeItoColors;

public class PlottableEventMarkers {

  private final String seriesLabel;
  private final Colors color;
  private final MarkerStyle marker;
  private final TISeries eventMarkerData;
  // for the export only, which uses the same structure
  private final String populationLabel;
  private final String mzLabel;

  // Dummy
  public PlottableEventMarkers() {
    this.seriesLabel = "Detected peaks";
    this.color = OkabeItoColors.BLACK;
    this.marker = MarkerStyle.CIRCLE;
    this.eventMarkerData = new TISeriesRAM();
    this.populationLabel = "Unknown population";
    this.mzLabel = "Unknown mz";
  }

  public PlottableEventMarkers(Colors color, MarkerStyle marker,
      TISeries eventMarkerData, String populationLabel, String mzLabel) {
    this.seriesLabel = "Detected peaks";
    this.color = color;
    this.marker = marker;
    this.eventMarkerData = eventMarkerData;
    this.populationLabel = populationLabel;
    this.mzLabel = mzLabel;
  }

  public int size() {
    return eventMarkerData.size();
  }

  public MarkerStyle getMarker() {
    return marker;
  }

  public Colors getColor() {
    return color;
  }

  public String getSeriesLabel() {
    return seriesLabel;
  }

  public TISeries getEventMarkerData() {
    return eventMarkerData;
  }

  public String getMzLabel() {
    return mzLabel;
  }

  public String getPopulationLabel() {
    return populationLabel;
  }
}
