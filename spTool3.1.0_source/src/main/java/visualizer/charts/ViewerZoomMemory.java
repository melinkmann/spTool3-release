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

package visualizer.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

public class ViewerZoomMemory {

  private static final Logger LOGGER = LogManager.getLogger(ViewerZoomMemory.class);

  private final ChartViewer viewer;
  private final LinkedList<Range[]> zoomList = new LinkedList<>();
  private final Queue<Range[]> zoomQueue = Collections.asLifoQueue(zoomList);

  public ViewerZoomMemory(ChartViewer viewer) {
    this.viewer = viewer;
    storeZoom();
  }

  public void storeZoom() {
    // Dummy
    Range xRange = new Range(0, 0);
    Range yRange = new Range(0, 0);
    if (viewer.getChart().getPlot() instanceof XYPlot) {
      xRange = viewer.getChart().getXYPlot().getDomainAxis().getRange();
      yRange = viewer.getChart().getXYPlot().getRangeAxis().getRange();
    } else if (viewer.getChart().getPlot() instanceof CategoryPlot) {
      // xRange is not set, so we can afford not storing it
      yRange = viewer.getChart().getCategoryPlot().getRangeAxis().getRange();
    }

    // keep from overflowing
    if (zoomQueue.size() > 1E6) {
      // Store the first, and the last 1000 or so.
      final Range[] initialRange = zoomList.get(0);
      List<Range[]> recentRanges = new ArrayList<>(
          zoomList.subList(zoomList.size() - (int) 1E3, zoomList.size()));

      zoomList.clear();
      zoomList.add(initialRange);
      zoomList.addAll(recentRanges);

      LOGGER.debug("FYI: The zoom memory has reached its limit of 1 million items "
          + "and it was reset the initial zoom and the most recent 1000 items.");
    }
    zoomQueue.add(new Range[]{SpChartFactory.clone(xRange), SpChartFactory.clone(yRange)});
  }

  public void resetZoom() {
    Range[] lastRange = getNextZoom();
    if (lastRange != null) {
      JFreeChart chart = viewer.getChart();
      XYPlot plot = chart.getXYPlot();

      ValueAxis xAxis = plot.getDomainAxis();
      ValueAxis yAxis = plot.getRangeAxis();
      xAxis.setRange(lastRange[0]);
      yAxis.setRange(lastRange[1]);
    }
  }

  @Nullable
  private Range[] getNextZoom() {
    if (zoomQueue.size() == 1) {
      return zoomQueue.peek();
    } else if (zoomQueue.size() > 0) {
      return zoomQueue.remove();
    } else {
      return null;
    }
  }

}
