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

import java.awt.geom.Point2D;
import javafx.scene.input.ScrollEvent;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.interaction.AbstractMouseHandlerFX;
import org.jfree.chart.fx.interaction.MouseHandlerFX;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.Zoomable;

/**
 * Handles scroll events (mouse wheel etc) on a {@link ChartCanvas}.
 */
public class SpScrollHandlerFX extends AbstractMouseHandlerFX
    implements MouseHandlerFX {

  private final ViewerZoomMemory zoomMemory;

  /**
   * The zoom factor.
   */
  private double zoomFactor;

  /**
   * Creates a new instance with the specified ID.
   *
   * @param id the handler ID ({@code null} not permitted).
   */
  public SpScrollHandlerFX(String id, ViewerZoomMemory zoomMemory) {
    super(id, false, false, false, false);
    this.zoomFactor = 0.20;
    this.zoomMemory = zoomMemory;
  }

  ;

  /**
   * Returns the zoom factor.  The default value is 0.10 (ten percent).
   *
   * @return The zoom factor.
   */
  public double getZoomFactor() {
    return this.zoomFactor;
  }

  /**
   * Sets the zoom factor (a percentage amount by which the mouse wheel movement will change the
   * chart size).
   *
   * @param zoomFactor the zoom factor.
   */
  public void setZoomFactor(double zoomFactor) {
    this.zoomFactor = zoomFactor;
  }

  @Override
  public void handleScroll(ChartCanvas canvas, ScrollEvent e) {
    JFreeChart chart = canvas.getChart();
    if (chart == null) {
      return;
    }
    Plot plot = chart.getPlot();
    if (plot instanceof Zoomable) {
      Zoomable zoomable = (Zoomable) plot;
      handleZoomable(canvas, zoomable, e);
    } else if (plot instanceof PiePlot) {
      PiePlot pp = (PiePlot) plot;
      pp.handleMouseWheelRotation((int) e.getDeltaY());
    }
  }

  /**
   * Handle the case where a plot implements the {@link Zoomable} interface.
   *
   * @param canvas   the chart canvas.
   * @param zoomable the zoomable plot.
   * @param e        the mouse wheel event.
   */
  private void handleZoomable(ChartCanvas canvas, Zoomable zoomable,
      ScrollEvent e) {
    if (canvas.getChart() == null) {
      return;
    }
    // don't zoom unless the mouse pointer is in the plot's data area
    ChartRenderingInfo info = canvas.getRenderingInfo();
    PlotRenderingInfo pinfo = info.getPlotInfo();
    Point2D p = new Point2D.Double(e.getX(), e.getY());
    if (pinfo.getDataArea().contains(p)) {
      Plot plot = (Plot) zoomable;
      // do not notify while zooming each axis
      boolean notifyState = plot.isNotify();
      plot.setNotify(false);
      int clicks = (int) e.getDeltaY();
      double zf = 1.0 + this.zoomFactor;
      if (clicks < 0) {
        zf = 1.0 / zf;
      }
      if (canvas.isDomainZoomable()) {
        // shift: only zoom y (range)
        if (!e.isAltDown()) {
          // store before setting the new thing
          zoomMemory.storeZoom();
          // then set
          zoomable.zoomDomainAxes(zf, pinfo, p, true);
        }
      }
      if (canvas.isRangeZoomable()) {
        // control: only zoom x (domain)
        if (!e.isControlDown()) {
          // store before setting the new thing
          zoomMemory.storeZoom();
          // then set
          zoomable.zoomRangeAxes(zf, pinfo, p, true);
        }
      }
      plot.setNotify(notifyState);  // this generates the change event too
    }
  }


}
