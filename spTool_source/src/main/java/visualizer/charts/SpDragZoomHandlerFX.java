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
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.AbstractMouseHandlerFX;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.util.ShapeUtils;

public class SpDragZoomHandlerFX extends AbstractMouseHandlerFX {

  private final ViewerZoomMemory zoomMemory;

  /**
   * The viewer is used to overlay the zoom rectangle.
   */
  private ChartViewer viewer;


  /**
   * The starting point for the zoom.
   */
  private Point2D startPoint;

  private double zoomW;
  private double zoomH;

  /**
   * Creates a new instance that requires no modifier keys.
   *
   * @param id the id ({@code null} not permitted).
   */
  public SpDragZoomHandlerFX(String id, ChartViewer viewer, ViewerZoomMemory zoomMemory) {
    this(id, viewer, zoomMemory, false, false, false, false);
  }

  /**
   * Creates a new instance that will be activated using the specified combination of modifier
   * keys.
   *
   * @param id       the id ({@code null} not permitted).
   * @param altKey   require ALT key?
   * @param ctrlKey  require CTRL key?
   * @param metaKey  require META key?
   * @param shiftKey require SHIFT key?
   */
  public SpDragZoomHandlerFX(String id, ChartViewer viewer, ViewerZoomMemory zoomMemory,
      boolean altKey, boolean ctrlKey,
      boolean metaKey, boolean shiftKey) {
    super(id, altKey, ctrlKey, metaKey, shiftKey);
    this.viewer = viewer;
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
      this.startPoint = ShapeUtils.getPointInRectangle(e.getX(), e.getY(), dataArea);

      this.zoomW = dataArea.getWidth();
      this.zoomH = dataArea.getHeight();
      if (e.isShiftDown()){
        canvas.setCursor(Cursor.N_RESIZE);
      }
      if (e.isControlDown()) {
        canvas.setCursor(Cursor.E_RESIZE);
      }

      if (e.isControlDown()&&e.isShiftDown()) {
        canvas.setCursor(Cursor.NE_RESIZE);
      }


    } else {
      this.startPoint = null;
      canvas.clearLiveHandler();
    }

  }

  /**
   * Handles a mouse dragged event by calculating the distance panned and updating the axes
   * accordingly.
   *
   * @param canvas the JavaFX canvas ({@code null} not permitted).
   * @param e      the mouse event ({@code null} not permitted).
   */
  @Override
  public void handleMouseDragged(ChartCanvas canvas, MouseEvent e) {
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

    double x = this.startPoint.getX();
    double y = this.startPoint.getY();

    Rectangle2D dataArea = canvas.findDataArea(this.startPoint);

    double pct = 1.0 / 10000;

    // Accelerates, when further away from start... Also, does not allow to update start,
    // b/c then the anchor changes while moving (unless we copy the anchor point...)
    //    double xChange = 1 - (e.getX() - x) / dataArea.getWidth();
    //    double yChange = 1 + (e.getY() - y) / dataArea.getHeight();

    double xChange = 1 - Math.signum(e.getX() - x) * pct * dataArea.getWidth();
    double yChange = 1 + Math.signum(e.getY() - y) * pct * dataArea.getHeight();

    boolean saved = p.isNotify();
    p.setNotify(false);

    PlotRenderingInfo info = this.viewer.getRenderingInfo().getPlotInfo();

    if (z.getOrientation().isVertical()) {
      // store before setting the new thing
      zoomMemory.storeZoom();
      // then set
      if (hZoom && e.isControlDown()) {
        z.zoomDomainAxes(xChange, info, startPoint, true);
      }
      if (vZoom && e.isShiftDown()) {
        z.zoomRangeAxes(yChange, info, startPoint, true);
      }


    } else {
      // store before setting the new thing
      zoomMemory.storeZoom();
      // then set
      if (hZoom && e.isControlDown()) {
        z.zoomRangeAxes(xChange, info, startPoint, true);
      }
      if (vZoom && e.isShiftDown()) {
        z.zoomDomainAxes(yChange, info, startPoint, true);
      }
    }
    // reset direction
    this.startPoint = ShapeUtils.getPointInRectangle(e.getX(), e.getY(), dataArea);
    // from the JFreeStuff itself
    p.setNotify(saved);
  }

  @Override
  public void handleMouseReleased(ChartCanvas canvas, MouseEvent e) {
    //if we have been panning reset the cursor
    //unregister in any case
    canvas.setCursor(javafx.scene.Cursor.DEFAULT);
    this.startPoint = null;
    canvas.clearLiveHandler();
  }

  /**
   * Returns {@code true} if the specified mouse event has modifier keys that match this handler.
   *
   * @param e the mouse event ({@code null} not permitted).
   * @return A boolean.
   */
  @Override
  public boolean hasMatchingModifiers(MouseEvent e) {
    boolean ctl = e.isControlDown();
    boolean shift = e.isShiftDown();
    return (ctl || shift);
  }


}