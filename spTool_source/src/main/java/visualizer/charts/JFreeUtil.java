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

import core.SpTool3Main;
import dataModelNew.TISeries;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import dataModelNew.mz.Element;
import javafx.util.Pair;
import math.units.enums.ViewUnits;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import processing.options.BinWidthEstimator;
import processing.options.HistogramNormalization;
import processing.options.MathMod;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;
import visualizer.charts.SpChartFactory.ChartComponent;
import visualizer.styles.*;

import static visualizer.charts.SpChartFactory.*;

public abstract class JFreeUtil {

  /**
   * Height of a unit-width equilateral triangle.
   */
  public static final double HEIGHT = Math.sqrt(3.0) / 2.0;

  public static final JFreeUtil.Point TRIANGLE_CORNER_A = new JFreeUtil.Point(0.5, HEIGHT);
  public static final JFreeUtil.Point TRIANGLE_CORNER_B = new JFreeUtil.Point(0.0, 0.0);
  public static final JFreeUtil.Point TRIANGLE_CORNER_C = new JFreeUtil.Point(1.0, 0.0);

  public static final JFreeUtil.Point TRIANGLE_CENTROID = new JFreeUtil.Point(
      (TRIANGLE_CORNER_A.x() + TRIANGLE_CORNER_B.x() + TRIANGLE_CORNER_C.x())
          / 3.0, (TRIANGLE_CORNER_A.y() + TRIANGLE_CORNER_B.y() + TRIANGLE_CORNER_C.y()) / 3.0);

  public static String buildTooltip(double minAbundance, double minSignal, double maxYValue,
                                    boolean showAbundance,
                                    List<Isotope> excludedFromLabel,
                                    XYIntervalSeriesCollection ds, int series, int item) {

    double mz = ds.getXValue(series, item);
    int nominalMZ = (int) Math.round(mz);

    List<String> matchingAllowedIsotopes = new ArrayList<>();
    List<String> matchingIsotopes = new ArrayList<>();

    double relSignal = ds.getYValue(series, item) / maxYValue;

    for (Element ele : Element.values()) {
      for (Isotope iso : ele.getIsotopes()) {

        // check if sth was matched - if yes, we know that we do not have to show the generic
        // tip = SnF.doubleToString(mz, NF.D1C3); label
        if (iso.getIsotopicNumber() == nominalMZ) {
          matchingIsotopes.add(iso.getNumberAndElement());
        }

        // Now check if the labels are supposed to be shown
        if (iso.getIsotopicNumber() == nominalMZ
            && iso.getAbundance() > minAbundance
            && relSignal > minSignal
            && !excludedFromLabel.contains(iso)) {
          if (showAbundance) {
            matchingAllowedIsotopes.add(iso.getNumberAndElement() + "(" + SnF.doubleToString(100 * iso.getAbundance(),
                NF.D1C1) + "%)");
          } else {
            matchingAllowedIsotopes.add(iso.getNumberAndElement());
          }
        }
      }
    }

    String tip;
    if (!matchingIsotopes.isEmpty()) {
      tip = String.join("\n", matchingAllowedIsotopes);
    } else {
      // ensure that we at least are confirm with the intensity threshold
      if (relSignal > minSignal) {
        tip = SnF.doubleToString(mz, NF.D1C3);
      } else {
        tip = "";
      }
    }

    return tip;
  }

  /**
   * Makes the plot's domain and range axes (and their labels) invisible,
   * and turns off the default rectangular gridlines/zero-baselines that
   * would otherwise still show through underneath your own ternary grid.
   * That second part isn't optional cleanup -- {@code axis.setVisible(false)}
   * only hides the axis itself; the plot's own background gridlines are a
   * separate setting and stay on unless also disabled here.
   */
  public static void hideAxesAndDefaultGrid(XYPlot plot) {
    ValueAxis domainAxis = plot.getDomainAxis();
    ValueAxis rangeAxis = plot.getRangeAxis();
    domainAxis.setVisible(false);
    rangeAxis.setVisible(false);

    plot.setDomainGridlinesVisible(false);
    plot.setRangeGridlinesVisible(false);
    plot.setDomainMinorGridlinesVisible(false);
    plot.setRangeMinorGridlinesVisible(false);
    plot.setDomainZeroBaselineVisible(false);
    plot.setRangeZeroBaselineVisible(false);
  }

  /**
   * Adds the "10%", "20%", ... tick-label annotations along all three
   * edges directly to {@code plot}. {@code numSubdivisions} is the number
   * of interior tick marks per edge -- e.g. 9 gives labels at 10%, 20%,
   * ..., 90% (10 equal segments per edge). Uses {@code XYTextAnnotation},
   * so each label is placed in data-space and stays correctly positioned
   * under zoom/pan without any further work on your end.
   *
   * @param numSubdivisions number of interior ticks per edge (must be >= 1);
   *                        the resulting interval is 1.0 / (numSubdivisions + 1)
   * @param offset          how far outside the edge the label sits, in the same
   *                        data units as the triangle itself (not pixels) -- try
   *                        something around 0.03-0.04 and adjust to taste.
   */
  public static void addEdgeTickLabelAnnotations(XYPlot plot, int numSubdivisions, double offset) {
    if (numSubdivisions >= 1) {

      //      Paint paint = Colors.PLOT_GRIDLINES;
      Paint paint = OkabeItoColors.BLACK.get();
      java.awt.Font font = new java.awt.Font("Tahoma", Font.PLAIN,
          SpTool3Main.getRunTime().getConfParams().getAxisFontSize());

      double gridInterval = 1.0 / (numSubdivisions + 1);
      for (double v = gridInterval; v < 1.0 - 1.0e-9; v += gridInterval) {
        JFreeUtil.Point pAonAB = toXY(v, 1 - v, 0);
        JFreeUtil.Point pBonBC = toXY(0, v, 1 - v);
        JFreeUtil.Point pConAC = toXY(1 - v, 0, v);

        String text = Math.round(v *
            100) + "%";
        addOutwardTextAnnotation(plot, pAonAB, TRIANGLE_CORNER_A, TRIANGLE_CORNER_B, text, font, paint,
            offset);
        addOutwardTextAnnotation(plot, pBonBC, TRIANGLE_CORNER_B, TRIANGLE_CORNER_C, text, font, paint,
            offset);
        addOutwardTextAnnotation(plot, pConAC, TRIANGLE_CORNER_C, TRIANGLE_CORNER_A, text, font, paint,
            offset);
      }
    }
  }

  private static void addOutwardTextAnnotation(XYPlot plot, JFreeUtil.Point point,
                                               JFreeUtil.Point edgeStart, JFreeUtil.Point edgeEnd,
                                               String text, java.awt.Font font, Paint paint, double offset) {
    double dx = edgeEnd.x() - edgeStart.x();
    double dy = edgeEnd.y() - edgeStart.y();
    double len = Math.hypot(dx, dy);
    dx /= len;
    dy /= len;
    double nx = -dy;
    double ny = dx;
    double midX = (edgeStart.x() + edgeEnd.x()) / 2.0;
    double midY = (edgeStart.y() + edgeEnd.y()) / 2.0;
    if (nx * (midX - TRIANGLE_CENTROID.x()) + ny * (midY - TRIANGLE_CENTROID.y()) < 0) {
      nx = -nx;
      ny = -ny;
    }

    XYTextAnnotation ann = new XYTextAnnotation(text, point.x() + nx * offset, point.y() + ny * offset);
    ann.setFont(font);
    ann.setPaint(paint);
    ann.setTextAnchor(TextAnchor.CENTER);
    plot.addAnnotation(ann);
  }

  /**
   * Builds the internal "lines of constant %" ternary grid as
   * {@link ChartComponent}s, ready to add to your {@code components} list
   * alongside the triangle edges. {@code numSubdivisions} follows the same
   * convention as {@link #addEdgeTickLabelAnnotations} (e.g. 9 -> a line
   * every 10%).
   */
  public static List<ChartComponent> buildTernaryGridLineComponents(int numSubdivisions) {
    List<ChartComponent> gridLines = new ArrayList<>();
    if (numSubdivisions < 1) {
      return gridLines;
    }

    double gridInterval = 1.0 / (numSubdivisions + 1);

    int step = 0;
    for (double v = gridInterval; v < 1.0 - 1.0e-9; v += gridInterval) {
      step++;

      JFreeUtil.Point pAonAB = toXY(v, 1 - v, 0);
      JFreeUtil.Point pAonAC = toXY(v, 0, 1 - v);
      gridLines.add(newGridLineComponent("Grid-A-" + step, pAonAB, pAonAC));

      JFreeUtil.Point pBonAB = toXY(1 - v, v, 0);
      JFreeUtil.Point pBonBC = toXY(0, v, 1 - v);
      gridLines.add(newGridLineComponent("Grid-B-" + step, pBonAB, pBonBC));

      JFreeUtil.Point pConAC = toXY(1 - v, 0, v);
      JFreeUtil.Point pConBC = toXY(0, 1 - v, v);
      gridLines.add(newGridLineComponent("Grid-C-" + step, pConAC, pConBC));
    }

    return gridLines;
  }

  private static ChartComponent newGridLineComponent(String name, JFreeUtil.Point start,
                                                     JFreeUtil.Point end) {
    return new ChartComponent(
        new ChartData(
            name,
            new double[]{start.x(), end.x()},
            new double[]{start.y(), end.y()},
            "X", ViewUnits.NONE, MathMod.NONE,
            "Y", ViewUnits.NONE, MathMod.NONE),
        new ChartStyle(new Colors.SpColor(Colors.PLOT_GRIDLINES), 1,
            LineWidthDefaults.THINNEST,
            LineDashDefaults.STRAIGHT, 0f,
            MarkerSizeDefaults.SMALL,
            MarkerStyle.CROSS,
            false,
            RendererOption.LINE_AND_SHAPE,
            LineGraphStyle.LINE)
    );
  }

  /**
   * Adds the three component-name labels (e.g. "SiO2"/"Al2O3"/"CaO") at
   * the corners, pushed outward along the centroid-to-vertex direction,
   * directly to {@code plot} as {@code XYTextAnnotation}s. Builds its own
   * font/paint the same way {@link #addEdgeTickLabelAnnotations} does, so
   * callers don't need to hand in AWT objects.
   */
  public static void addComponentLabelAnnotations(XYPlot plot, String labelA, String labelB, String labelC,
                                                  double offset) {
    Paint paint = OkabeItoColors.BLACK.get();
    java.awt.Font font = new java.awt.Font("Tahoma", java.awt.Font.BOLD,
        SpTool3Main.getRunTime().getConfParams().getAxisFontSize());

    addCornerTextAnnotation(plot, TRIANGLE_CORNER_A, labelA, font, paint, offset);
    addCornerTextAnnotation(plot, TRIANGLE_CORNER_B, labelB, font, paint, offset);
    addCornerTextAnnotation(plot, TRIANGLE_CORNER_C, labelC, font, paint, offset);
  }

  private static void addCornerTextAnnotation(XYPlot plot, JFreeUtil.Point vertex, String text,
                                              java.awt.Font font, Paint paint, double offset) {
    double dx = vertex.x() - TRIANGLE_CENTROID.x();
    double dy = vertex.y() - TRIANGLE_CENTROID.y();
    double len = Math.hypot(dx, dy);
    dx /= len;
    dy /= len;

    XYTextAnnotation ann = new XYTextAnnotation(text, vertex.x() + dx * offset, vertex.y() + dy * offset);
    ann.setFont(font);
    ann.setPaint(paint);
    ann.setTextAnchor(TextAnchor.CENTER);
    plot.addAnnotation(ann);
  }

  /**
   * Converts ternary (a, b, c) into Cartesian (x, y) via barycentric interpolation between the triangle
   * corners.
   */
  public static JFreeUtil.Point toXY(double a, double b, double c) {
    double sum = a + b + c;
    if (sum <= 0.0) {
      throw new IllegalArgumentException("a + b + c must be > 0");
    }
    double fa = a / sum;
    double fb = b / sum;
    double fc = c / sum;
    double x = fa * TRIANGLE_CORNER_A.x() + fb * TRIANGLE_CORNER_B.x() + fc * TRIANGLE_CORNER_C.x();
    double y = fa * TRIANGLE_CORNER_A.y() + fb * TRIANGLE_CORNER_B.y() + fc * TRIANGLE_CORNER_C.y();
    return new JFreeUtil.Point(x, y);
  }

  public record Point(double x, double y) {
  }

  /**
   * Clamps negative values to zero (background-subtraction artifacts),
   * normalizes each (a, b, c) triple onto the ternary 0-1 scale, and
   * converts straight to Cartesian (x, y) via {@link #toXY}. Takes three
   * parallel arrays of guaranteed-equal length -- one point per index --
   * and returns a {@code Pair} of parallel {@code x[]}/{@code y[]} arrays,
   * same order, ready to feed directly into a {@code ChartData}.
   * <p>
   * If a point's three raw values are all &lt;= 0 (so plain clamping would
   * collapse it to {@code (0,0,0)}, with no information left to place it),
   * the point is instead shifted by its most negative raw value before
   * normalizing -- preserving the relative differences between the three
   * components and landing the point on whichever edge/corner it was
   * actually closest to, rather than an arbitrary fallback.
   */
  public static Pair<double[], double[]> clampAndNormalizeToUnitTernary(double[] aValues, double[] bValues,
                                                                        double[] cValues) {
    double[] xs = new double[0];
    double[] ys = new double[0];

    if (aValues.length == bValues.length && bValues.length == cValues.length) {
      xs = new double[aValues.length];
      ys = new double[aValues.length];

      for (int i = 0; i < aValues.length; i++) {
        double rawA = aValues[i];
        double rawB = bValues[i];
        double rawC = cValues[i];

        double a = Math.max(0.0, rawA);
        double b = Math.max(0.0, rawB);
        double c = Math.max(0.0, rawC);

        double sum = a + b + c;
        JFreeUtil.Point p;
        if (sum <= 0.0) {
          double min = Math.min(rawA, Math.min(rawB, rawC));
          double shiftedA = rawA - min;
          double shiftedB = rawB - min;
          double shiftedC = rawC - min;
          double shiftedSum = shiftedA + shiftedB + shiftedC;

          if (shiftedSum <= 0.0) {
            // rawA == rawB == rawC: genuinely no directional information at all.
            p = TRIANGLE_CENTROID;
          } else {
            p = toXY(shiftedA / shiftedSum, shiftedB / shiftedSum, shiftedC / shiftedSum);
          }
        } else {
          p = toXY(a / sum, b / sum, c / sum);
        }

        xs[i] = p.x();
        ys[i] = p.y();
      }
    }

    return new Pair<>(xs, ys);
  }


  /**
   * Filling is faster than adding new XYDataSets.
   */
  public static void fillDataset(XYDataset dataset, ChartComponent component) {
    XYSeries xySeries = component.getData().getXySeries();

    if (xySeries != null) {
    } else {
      xySeries = wrapDataAsJFreeXYSeries(component.getData().getX(),
          component.getData().getY(),
          component.getData().getSeriesName());
    }
    if (dataset instanceof XYSeriesCollection) {
      XYSeriesCollection collection = (XYSeriesCollection) dataset;
      collection.setNotify(false);
      xySeries.setNotify(false);
      collection.addSeries(xySeries);
    }

  }

  public static XYSeriesCollection createDataset(ChartComponent component) {
    XYSeriesCollection collection;
    XYSeries xySeries = component.getData().getXySeries();

    if (xySeries != null) {
      collection = new XYSeriesCollection();
      collection.addSeries(xySeries);
    } else {
      collection = createDataset(component.getData().getX(),
          component.getData().getY(),
          component.getData().getSeriesName());
    }
    return collection;
  }

  public static XYSeriesCollection createDataset(double[] xData, double[] yData,
                                                 String seriesName) {
    XYSeriesCollection seriesCollection = new XYSeriesCollection();
    XYSeries series = wrapDataAsJFreeXYSeries(xData, yData, seriesName);
    seriesCollection.addSeries(series);
    return seriesCollection;
  }

  public static XYSeriesCollection createBufferedDataset(ChartComponent component) {
    XYSeriesCollection collection = createBufferedDataset(component.getData().getTiSeries(),
        component.getData().getSeriesName());
    return collection;
  }

  public static XYSeriesCollection createBufferedDataset(TISeries tiSeries, String seriesName) {
    XYSeriesCollection seriesCollection = new XYSeriesCollection();
    XYSeries series = new BufferedXYSeries(seriesName, tiSeries);
    seriesCollection.addSeries(series);
    return seriesCollection;
  }

  public static XYSeriesCollection createDataset(List<double[]> xData, List<double[]> yData,
                                                 List<String> seriesName) {
    XYSeriesCollection seriesCollection = new XYSeriesCollection();
    if (xData.size() == yData.size() && xData.size() == seriesName.size()) {
      for (int i = 0; i < xData.size(); i++) {
        XYSeries series = wrapDataAsJFreeXYSeries(xData.get(i), yData.get(i), seriesName.get(i));
        seriesCollection.addSeries(series);
      }
    }
    return seriesCollection;
  }

  private static XYSeries wrapDataAsJFreeXYSeries(double[] xData, double[] yData,
                                                  String seriesName) {
    final XYSeries xySeries = new XYSeries(seriesName, true, true);
    if (xData.length == yData.length) {
      for (int i = 0; i < xData.length; i++) {
        xySeries.add(xData[i], yData[i]);
      }
    }
    return xySeries;
  }


  /*
  HISTOGRAMS
   */

  // Use List instead of tedious adding (where once has to check and update each time sth is added):
  public static List<ExtendedHistogramDataSet> createHistogramDatasets(List<double[]> dataList,
                                                                       List<String> seriesNames,
                                                                       HistogramNormalization type,
                                                                       BinWidthEstimator binWidthEstimator,
                                                                       double customBinWidth) {

    List<ExtendedHistogramDataSet> sets = new ArrayList<>();

    // Estimate limits
    BinConstraints constraints = new BinConstraints(dataList, binWidthEstimator, customBinWidth);

    for (int i = 0; i < dataList.size(); i++) {

      HistogramDataset dataset = new HistogramDataset();
      dataset.setType(type.get());

      if (constraints.getNoOfBins() > 0) {
        dataset.addSeries(seriesNames.get(i), dataList.get(i),
            constraints.getNoOfBins(), constraints.getMin(), constraints.getMax());
      }

      sets.add(new ExtendedHistogramDataSet(dataset, constraints.getWidth()));
    }

    return sets;
  }

  public static class ExtendedHistogramDataSet {

    private final HistogramDataset histogramDataset;
    private final double binWidth;

    public ExtendedHistogramDataSet(HistogramDataset histogramDataset, double binWidth) {
      this.histogramDataset = histogramDataset;
      this.binWidth = binWidth;
    }

    public HistogramDataset getHistogramDataset() {
      return histogramDataset;
    }

    public double getBinWidth() {
      return binWidth;
    }
  }


}
