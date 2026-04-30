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


import com.google.common.primitives.Doubles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.event.OverlayChangeEvent;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.overlay.OverlayFX;
import org.jfree.chart.plot.XYPlot;

public class PolygonOverlay implements OverlayFX {

  private boolean useConvexHull = false;
  private final List<Point2D> points = new ArrayList<>();
  private final List<OverlayChangeListener> listeners = new ArrayList<>();

  public void addPoint(Point2D p) {
    points.add(p);
    fireChangeEvent();
  }

  public void clear() {
    points.clear();
    fireChangeEvent();
  }

  public List<Point2D> getPoints() {
    return Collections.unmodifiableList(points);
  }

  @Override
  public void paintOverlay(Graphics2D g2, ChartCanvas canvas) {
    if (canvas.getRenderingInfo() != null && points.size() > 0) {
      XYPlot plot = (XYPlot) canvas.getChart().getPlot();
      Rectangle2D dataArea = canvas.getRenderingInfo().getPlotInfo().getDataArea();

      if (useConvexHull && points.size() > 1) {
        List<Point2D> hull = graham(points); // compute convex hull
        points.clear();
        points.addAll(hull); // update the list without reassigning
      }


      int[] xs = new int[points.size()];
      int[] ys = new int[points.size()];

      for (int i = 0; i < points.size(); i++) {
        double xx = plot.getDomainAxis().valueToJava2D(
            points.get(i).getX(), dataArea, plot.getDomainAxisEdge());
        double yy = plot.getRangeAxis().valueToJava2D(
            points.get(i).getY(), dataArea, plot.getRangeAxisEdge());

        xs[i] = (int) xx;
        ys[i] = (int) yy;
      }

      // prepare first vertex
      int vertexSize = 6;
      g2.setColor(new Color(0, 0, 151));

      if (points.size() == 1) {
        g2.fillOval(xs[0] - vertexSize / 2, ys[0] - vertexSize / 2, vertexSize, vertexSize);
        return;
      }

      // Draw filled polygon
      g2.setColor(new Color(0, 0, 255, 64));
      g2.fillPolygon(xs, ys, points.size());

      // Draw polygon outline
      g2.setColor(Color.BLUE);
      g2.drawPolygon(xs, ys, points.size());

      // Draw vertices
      g2.setColor(new Color(0, 0, 151));
      for (int i = 0; i < points.size(); i++) {
        g2.fillOval(xs[i] - vertexSize / 2, ys[i] - vertexSize / 2, vertexSize, vertexSize);
      }
    }
  }


  @Override
  public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
    if (chartPanel.getScreenDataArea() != null && points.size() > 0) {
      XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
      Rectangle2D dataArea = chartPanel.getScreenDataArea();

      if (useConvexHull && points.size() > 1) {
        List<Point2D> hull = graham(points); // compute convex hull
        points.clear();
        points.addAll(hull); // update the list without reassigning
      }

      int[] xs = new int[points.size()];
      int[] ys = new int[points.size()];

      for (int i = 0; i < points.size(); i++) {
        double x = plot.getDomainAxis().valueToJava2D(points.get(i).getX(),
            dataArea, plot.getDomainAxisEdge());
        double y = plot.getRangeAxis().valueToJava2D(points.get(i).getY(),
            dataArea, plot.getRangeAxisEdge());
        xs[i] = (int) x;
        ys[i] = (int) y;
      }

      // prepare first vertex
      int vertexSize = 6;
      g2.setColor(new Color(0, 0, 151));

      if (points.size() == 1) {
        g2.fillOval(xs[0] - vertexSize / 2, ys[0] - vertexSize / 2, vertexSize, vertexSize);
        return;
      }

      // Draw filled polygon
      g2.setColor(new Color(0, 0, 255, 128)); //64
      g2.fillPolygon(xs, ys, points.size());

      // Draw polygon outline
      g2.setColor(Color.BLUE);
      g2.drawPolygon(xs, ys, points.size());

      // Draw vertices
      g2.setColor(new Color(0, 0, 151));
      for (int i = 0; i < points.size(); i++) {
        g2.fillOval(xs[i] - vertexSize / 2, ys[i] - vertexSize / 2, vertexSize, vertexSize);
      }
    }
  }


  @Override
  public void addChangeListener(OverlayChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeChangeListener(OverlayChangeListener listener) {
    listeners.remove(listener);
  }

  private void fireChangeEvent() {
    OverlayChangeEvent evt = new OverlayChangeEvent(this);
    for (OverlayChangeListener l : listeners) {
      l.overlayChanged(evt);
    }
  }

  public List<Point2D> graham(List<Point2D> points) {
    if (points.size() <= 1) return new ArrayList<>(points);

    // Step 1: Find pivot (lowest Y, then leftmost X)
    Point2D pivot = Collections.min(points, Comparator
        .comparingDouble(Point2D::getY)
        .thenComparingDouble(Point2D::getX));

    // Step 2: Sort points by polar angle relative to pivot
    List<Point2D> sorted = new ArrayList<>(points);
    sorted.sort((a, b) -> {
      if (a.equals(pivot)) return -1;
      if (b.equals(pivot)) return 1;
      double angleA = Math.atan2(a.getY() - pivot.getY(), a.getX() - pivot.getX());
      double angleB = Math.atan2(b.getY() - pivot.getY(), b.getX() - pivot.getX());
      return Doubles.compare(angleA, angleB);
    });

    // Step 3: Build hull with stack
    Stack<Point2D> stack = new Stack<>();
    stack.push(sorted.get(0));
    stack.push(sorted.get(1));

    for (int i = 2; i < sorted.size(); i++) {
      Point2D top = stack.pop();
      while (!stack.isEmpty() && ccw(stack.peek(), top, sorted.get(i)) <= 0) {
        top = stack.pop();
      }
      stack.push(top);
      stack.push(sorted.get(i));
    }

    return new ArrayList<>(stack); // return new list, original points untouched
  }


  // Cross product for orientation
  private double ccw(Point2D a, Point2D b, Point2D c) {
    return (b.getX() - a.getX()) * (c.getY() - a.getY()) -
        (b.getY() - a.getY()) * (c.getX() - a.getX());
  }


  public static void enablePolygon(ChartViewer viewer) {
    enablePolygon(viewer, new PolygonOverlay(), new AtomicBoolean(true));
  }

  public static void enablePolygon(ChartViewer viewer, PolygonOverlay overlay, AtomicBoolean listen) {
    if (viewer != null) {
      ChartCanvas chartPanel = (ChartCanvas) viewer.getCanvas();
      chartPanel.addOverlay(overlay);

      // Mouse click: add polygon vertices
      final List<Point2D> polygonPoints = new ArrayList<>();

      viewer.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
        if (listen.get() && e.getButton().name().equals("PRIMARY")) {

          // clear old stuff
          overlay.clear();

          double xx = e.getX();
          double yy = e.getY();

          ChartRenderingInfo info = viewer.getRenderingInfo();
          XYPlot plot = (XYPlot) viewer.getChart().getPlot();
          Rectangle2D dataArea = info.getPlotInfo().getDataArea();

          double chartX = plot.getDomainAxis().java2DToValue(xx, dataArea, plot.getDomainAxisEdge());
          double chartY = plot.getRangeAxis().java2DToValue(yy, dataArea, plot.getRangeAxisEdge());

          // add last point
          polygonPoints.add(new Point2D.Double(chartX, chartY));
          // Double-click closes polygon

          for (Point2D p : polygonPoints) {
            overlay.addPoint(new Point2D.Double(p.getX(), p.getY()));
          }

          if (e.getClickCount() == 2) {
            polygonPoints.clear();
            overlay.clear();
          }
        }
      });
    }
  }


}


