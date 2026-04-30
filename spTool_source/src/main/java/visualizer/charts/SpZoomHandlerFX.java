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
import java.awt.geom.Rectangle2D;

import javafx.scene.input.MouseEvent;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.AbstractMouseHandlerFX;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;

/**
 * Handles drag zooming of charts on a {@link ChartCanvas}.  This handler should be configured with
 * the required modifier keys and installed as a live handler (not an auxiliary handler).  This
 * handler only works for a <b>ChartCanvas</b> that is embedded in a {@link ChartViewer}, since it
 * relies on the <b>ChartViewer</b> for drawing the zoom rectangle.
 */
public class SpZoomHandlerFX extends AbstractMouseHandlerFX {


  private final ViewerZoomMemory zoomMemory;

  /**
   * The viewer is used to overlay the zoom rectangle.
   */
  private ChartViewer viewer;

  /**
   * The starting point for the zoom.
   */
  private Point2D startPoint;

  /**
   * Creates a new instance with no modifier keys required.
   *
   * @param id     the handler ID ({@code null} not permitted).
   * @param parent the chart viewer.
   */
  public SpZoomHandlerFX(String id, ChartViewer parent, ViewerZoomMemory zoomMemory) {
    this(id, parent, zoomMemory, false, false, false, false);
  }

  /**
   * Creates a new instance that will be activated using the specified combination of modifier
   * keys.
   *
   * @param id       the handler ID ({@code null} not permitted).
   * @param parent   the chart viewer.
   * @param altKey   require ALT key?
   * @param ctrlKey  require CTRL key?
   * @param metaKey  require META key?
   * @param shiftKey require SHIFT key?
   */
  public SpZoomHandlerFX(String id, ChartViewer parent, ViewerZoomMemory zoomMemory,
                         boolean altKey,
                         boolean ctrlKey, boolean metaKey, boolean shiftKey) {
    super(id, altKey, ctrlKey, metaKey, shiftKey);
    this.viewer = parent;
    this.zoomMemory = zoomMemory;
  }

  /**
   * Handles a mouse pressed event by recording the initial mouse pointer location.
   *
   * @param canvas the JavaFX canvas ({@code null} not permitted).
   * @param e      the mouse event ({@code null} not permitted).
   */
  @Override
  public void handleMousePressed(ChartCanvas canvas, MouseEvent e) {
    if (canvas.getChart() == null) {
      return;
    }
    Point2D pt = new Point2D.Double(e.getX(), e.getY());
    Rectangle2D dataArea = canvas.findDataArea(pt);
    if (dataArea != null) {
      this.startPoint = ShapeUtils.getPointInRectangle(e.getX(),
          e.getY(), dataArea);
    } else {
      this.startPoint = null;
      canvas.clearLiveHandler();
    }
  }

  /**
   * Handles a mouse dragged event by updating the zoom rectangle displayed in the ChartViewer.
   *
   * @param canvas the JavaFX canvas ({@code null} not permitted).
   * @param e      the mouse event ({@code null} not permitted).
   */
  @Override
  public void handleMouseDragged(ChartCanvas canvas, MouseEvent e) {
    if (this.startPoint == null) {
      //no initial zoom rectangle exists but the handler is set
      //as life handler unregister
      canvas.clearLiveHandler();
      return;
    }
    if (canvas.getChart() == null) {
      return;
    }

    boolean hZoom, vZoom;
    Plot p = canvas.getChart().getPlot();
    if (!(p instanceof Zoomable)) {
      return;
    }
    Zoomable z = (Zoomable) p;
    if (z.getOrientation().isHorizontal()) {
      hZoom = z.isRangeZoomable();
      vZoom = z.isDomainZoomable();
    } else {
      hZoom = z.isDomainZoomable();
      vZoom = z.isRangeZoomable();
    }
    Rectangle2D dataArea = canvas.findDataArea(this.startPoint);

    double x = this.startPoint.getX();
    double y = this.startPoint.getY();
    double w = 0;
    double h = 0;
    if (hZoom && vZoom) {
      // selected rectangle shouldn't extend outside the data area...
      double xmax = Math.min(e.getX(), dataArea.getMaxX());
      double ymax = Math.min(e.getY(), dataArea.getMaxY());
      w = xmax - this.startPoint.getX();
      h = ymax - this.startPoint.getY();
    } else if (hZoom) {
      double xmax = Math.min(e.getX(), dataArea.getMaxX());
      y = dataArea.getMinY();
      w = xmax - this.startPoint.getX();
      h = dataArea.getHeight();
    } else if (vZoom) {
      double ymax = Math.min(e.getY(), dataArea.getMaxY());
      x = dataArea.getMinX();
      w = dataArea.getWidth();
      h = ymax - this.startPoint.getY();
    }
    this.viewer.showZoomRectangle(x, y, w, h);
  }

  @Override
  public void handleMouseReleased(ChartCanvas canvas, MouseEvent e) {
    if (canvas.getChart() == null) {
      return;
    }
    Plot p = canvas.getChart().getPlot();
    if (!(p instanceof Zoomable)) {
      return;
    }
    boolean hZoom, vZoom;
    Zoomable z = (Zoomable) p;
    if (z.getOrientation().isHorizontal()) {
      hZoom = z.isRangeZoomable();
      vZoom = z.isDomainZoomable();
    } else {
      hZoom = z.isDomainZoomable();
      vZoom = z.isRangeZoomable();
    }

    boolean zoomTrigger1 = hZoom && Math.abs(e.getX() - this.startPoint.getX()) >= 10;
    boolean zoomTrigger2 = vZoom && Math.abs(e.getY() - this.startPoint.getY()) >= 10;
    if (zoomTrigger1 || zoomTrigger2) {
      Point2D endPoint = new Point2D.Double(e.getX(), e.getY());
      PlotRenderingInfo pri = canvas.getRenderingInfo().getPlotInfo();
      if ((hZoom && (e.getX() < this.startPoint.getX()))
          || (vZoom && (e.getY() < this.startPoint.getY()))) {
        boolean saved = p.isNotify();
        p.setNotify(false);
        // store before setting the new thing
        zoomMemory.storeZoom();
        // then set
        z.zoomDomainAxes(0, pri, endPoint);
        z.zoomRangeAxes(0, pri, endPoint);
        p.setNotify(saved);

        // Fix log axis bounds after reset. Added later in 3.1.2 for MS spectrum
        if (p instanceof XYPlot plot && plot.getRangeAxis() instanceof LogAxis logAxis) {
          double[] minMax = getMinMaxYFromDataset(canvas);
          double yLimLow = Math.max(Math.nextUp(0d), 0.5 * minMax[0]);
          double yLimUp = minMax[1];
          if (!(yLimLow < yLimUp)) yLimUp = Math.nextUp(yLimLow);
          logAxis.setAutoRange(false);
          logAxis.setRange(new Range(yLimLow, 2 * yLimUp));
        }

      } else {
        double x = this.startPoint.getX();
        double y = this.startPoint.getY();
        double w = e.getX() - x;
        double h = e.getY() - y;
        Rectangle2D dataArea = canvas.findDataArea(this.startPoint);
        double maxX = dataArea.getMaxX();
        double maxY = dataArea.getMaxY();
        // for mouseReleased event, (horizontalZoom || verticalZoom)
        // will be true, so we can just test for either being false;
        // otherwise both are true
        if (!vZoom) {
          y = dataArea.getMinY();
          w = Math.min(w, maxX - this.startPoint.getX());
          h = dataArea.getHeight();
        } else if (!hZoom) {
          x = dataArea.getMinX();
          w = dataArea.getWidth();
          h = Math.min(h, maxY - this.startPoint.getY());
        } else {
          w = Math.min(w, maxX - this.startPoint.getX());
          h = Math.min(h, maxY - this.startPoint.getY());
        }
        Rectangle2D zoomArea = new Rectangle2D.Double(x, y, w, h);
        if (!zoomArea.isEmpty()) {
          boolean saved = p.isNotify();
          p.setNotify(false);
          double pw0 = percentW(x, dataArea);
          double pw1 = percentW(x + w, dataArea);
          double ph0 = percentH(y, dataArea);
          double ph1 = percentH(y + h, dataArea);
          PlotRenderingInfo info
              = this.viewer.getRenderingInfo().getPlotInfo();
          if (z.getOrientation().isVertical()) {
            // store before setting the new thing
            zoomMemory.storeZoom();
            // then set
            z.zoomDomainAxes(pw0, pw1, info, endPoint);
            z.zoomRangeAxes(1 - ph1, 1 - ph0, info, endPoint);
          } else {
            // store before setting the new thing
            zoomMemory.storeZoom();
            // then set
            z.zoomRangeAxes(pw0, pw1, info, endPoint);
            z.zoomDomainAxes(1 - ph1, 1 - ph0, info, endPoint);
          }
          p.setNotify(saved);
        }
      }
    }
    this.viewer.hideZoomRectangle();
    this.startPoint = null;
    canvas.clearLiveHandler();
  }

  private double percentW(double x, Rectangle2D r) {
    return (x - r.getMinX()) / r.getWidth();
  }

  private double percentH(double y, Rectangle2D r) {
    return (y - r.getMinY()) / r.getHeight();
  }

  private double[] getMinMaxYFromDataset(ChartCanvas canvas) {
    if (!(canvas.getChart().getPlot() instanceof XYPlot plot)) {
      return new double[]{Math.nextUp(0d), Double.MAX_VALUE};
    }
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    for (int ds = 0; ds < plot.getDatasetCount(); ds++) {
      XYDataset dataset = plot.getDataset(ds);
      if (dataset == null) continue;
      for (int s = 0; s < dataset.getSeriesCount(); s++) {
        for (int i = 0; i < dataset.getItemCount(s); i++) {
          double y = dataset.getYValue(s, i);
          if (y > 0) {
            if (y < min) min = y;
            if (y > max) max = y;
          }
        }
      }
    }
    return new double[]{
        min == Double.MAX_VALUE ? Math.nextUp(0d) : min,
        max == -Double.MAX_VALUE ? Math.nextUp(min) : max
    };
  }

}
