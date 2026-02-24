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
import gui.StageFactory;
import gui.util.UiUtil;
import io.export.JFreeExportUtils;

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.Pair;

import javax.annotation.Nullable;

import math.units.Unit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import processing.options.BinWidthEstimator;
import processing.options.HistogramNormalization;
import processing.options.MathMod;
import util.ArrUtils;
import util.NF;
import visualizer.charts.JFreeUtil.ExtendedHistogramDataSet;
import visualizer.styles.Colors;
import visualizer.styles.FontStyles;
import visualizer.styles.LineDash;
import visualizer.styles.LineGraphStyle;
import visualizer.styles.LineLineDashDefaults;
import visualizer.styles.LineWidth;
import visualizer.styles.LineWidthDefaults;
import visualizer.styles.MarkerSize;
import visualizer.styles.MarkerSizeDefaults;
import visualizer.styles.MarkerStyle;
import visualizer.styles.NumberFormats;
import visualizer.styles.SpV2Colors;

/*
TODO:
Negatvive areas althought suppres is checked
Histogram does not show any when a single sel pop has just 1 event...
Pop shows non-existent in listview.. something about refilling is wrong.
 */

public abstract class SpChartFactory {

  private static final Logger LOGGER = LogManager.getLogger(SpChartFactory.class);

  public static JFreeChart createLineChart(List<ChartComponent> components) {

    // Dummy
    JFreeChart chart = ChartFactory.createScatterPlot(null, "", "", new DefaultXYDataset());

    if (!components.isEmpty()) {
      ChartComponent component = components.get(0);

      chart = ChartFactory.createXYLineChart("",
          component.getData().translateXLbl(),
          component.getData().translateYLbl(),
          new DefaultXYDataset(),
          PlotOrientation.VERTICAL,
          false,
          false,
          false);

      int noOfComponents = components.size();
      for (int i = 0; i < noOfComponents; i++) {
        component = components.get(i);
        XYItemRenderer renderer = component.getStyle().getXYRenderer();
        XYDataset dataset = new XYSeriesCollection();
        chart.getXYPlot().setRenderer(i, renderer);
        chart.getXYPlot().setDataset(i, dataset);
      }

      chart.setNotify(false);

      chart.getXYPlot().setNotify(false);
      for (int i = 0; i < noOfComponents; i++) {
        XYDataset dataset = chart.getXYPlot().getDataset(i);
        component = components.get(i);
        JFreeUtil.fillDataset(dataset, component);
      }
    }

    // reactivate notifications
    chart.setNotify(true);
    chart.getXYPlot().setNotify(true);
    for (int i = 0; i < chart.getXYPlot().getSeriesCount(); i++) {
      XYDataset dataset = chart.getXYPlot().getDataset(i);
      if (dataset instanceof XYSeriesCollection) {
        ((XYSeriesCollection) dataset).setNotify(true);
        for (int j = 0; j < dataset.getSeriesCount(); j++) {
          ((XYSeriesCollection) dataset).getSeries(j).setNotify(true);
        }
      }
    }

    return chart;
  }


  public static Pair<JFreeChart, Double> createHistogram(
      List<ChartComponent> components,
      HistogramNormalization normalization,
      BinWidthEstimator binWidthEstimator,
      double customBinWidth) {

    // Dummy instance to not return null.
    JFreeChart chart = ChartFactory.createScatterPlot(null, "", "", new DefaultXYDataset());
    double estimatedBinWidth = 0;

    // Unwrap
    List<double[]> data = components.stream()
        .map(ChartComponent::getData)
        .map(ChartData::getY)
        .collect(Collectors.toList());

    List<String> seriesNames = components.stream()
        .map(ChartComponent::getData)
        .map(ChartData::getSeriesName)
        .collect(Collectors.toList());

    List<ExtendedHistogramDataSet> datasets = JFreeUtil.createHistogramDatasets(data,
        seriesNames, normalization,
        binWidthEstimator, customBinWidth);

    // get bin width
    if (!datasets.isEmpty()) {
      estimatedBinWidth = datasets.get(0).getBinWidth();
    }

    // Get seed
    if (!components.isEmpty()) {
      ChartComponent comp = components.get(0);

      chart = ChartFactory.createHistogram("",
          comp.getData().translateXLbl(),
          comp.getData().translateYLbl(),
          datasets.get(0).getHistogramDataset(), PlotOrientation.VERTICAL, false, false, false);

      // Style
      XYItemRenderer renderer = comp.getStyle().getXYBarRenderer();

      // This is meant to replace, i.e. add at index of current existing index
      chart.getXYPlot().setDataset(0, datasets.get(0).getHistogramDataset());
      chart.getXYPlot().setRenderer(0, renderer);

      // Fill in remaining

      for (int i = 1; i < components.size(); i++) {
        comp = components.get(i);
        renderer = comp.getStyle().getXYBarRenderer();

        // Renderer note: This is meant to add, i.e. put at the index of current size
        // .getDatasetCount() returns the size of the map in which the pairs of index->dataset
        // are stored internally. Since Datasets can only be removed by setDataset(index,null)
        // the .getDatasetCount() does not necessarily return the number of non-null Datasets
        int existingDatasetCount = chart.getXYPlot().getDatasetCount();
        chart.getXYPlot().setDataset(existingDatasetCount, datasets.get(i).getHistogramDataset());
        chart.getXYPlot().setRenderer(existingDatasetCount, renderer);

      }
    }
    return new Pair<>(chart, estimatedBinWidth);
  }

  public static void addData(HistogramDataset dataset, XYBarRenderer renderer, JFreeChart chart) {
    int existingDatasetCount = chart.getXYPlot().getDatasetCount();
    // note: .getDatasetCount() returns the size of the map in which the paris of index->dataset
    // are stored internally. Since Datasets can only be removed by setDataset(index,null)
    // the .getDatasetCount() does not necessarily return the number of non-null Datasets
    // I assume, adding at existingCount means adding at the right index
    // e.g.: count = 1 (with dataset @ index 0) --> add next @ index = 1
    chart.getXYPlot().setDataset(existingDatasetCount, dataset);
    chart.getXYPlot().setRenderer(existingDatasetCount, renderer);
  }

  public static JFreeChart createBoxplot(List<ChartComponent> components) {

    // Dummy instance to not return null.
    JFreeChart chart = ChartFactory.createScatterPlot(null, "", "", new DefaultXYDataset());

    // Unwrap
    List<double[]> data = components.stream()
        .map(ChartComponent::getData)
        .map(ChartData::getY)
        .collect(Collectors.toList());

    List<String> seriesNames = components.stream()
        .map(ChartComponent::getData)
        .map(ChartData::getSeriesName)
        .collect(Collectors.toList());

    List<String> seriesShortNames = components.stream()
        .map(ChartComponent::getData)
        .map(ChartData::getSeriesShortname)
        .collect(Collectors.toList());

    List<Paint> paints = components.stream()
        .map(ChartComponent::getStyle)
        .map(ChartStyle::getPaint)
        .collect(Collectors.toList());

    DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

    if (data.size() == seriesNames.size() && seriesNames.size() == components.size()
        && seriesShortNames.size() == seriesNames.size()) {

      String xLbl = "";
      String yLbl = "";

      for (int i = 0; i < data.size(); i++) {
        // names must be unique or else the internal hashmap for the series/column keys fails.
        // Note: when using column keys with the same rowKey, we get the same color for all within a category.
        dataset.add(ArrUtils.arrToList(data.get(i)),
            seriesNames.get(i) + "_" + i,
            "");
        if (i == 0) {
          xLbl = components.get(i).getData().translateXLbl();
          yLbl = components.get(i).getData().translateYLbl();
        }
      }
      CategoryAxis categoryAxis = new CategoryAxis(xLbl);
      NumberAxis valueAxis = new NumberAxis(yLbl);
      valueAxis.setAutoRangeIncludesZero(false);

      CustomBoxAndWhiskerRenderer renderer = new CustomBoxAndWhiskerRenderer(paints, seriesShortNames);
      renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
      renderer.setMaximumBarWidth(0.10);
      renderer.setItemMargin(0.1); // reduces gap between series boxes within a category
      renderer.setFillBox(true);
      renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());

      CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis,
          renderer);

      chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT,
          plot, false);
    }
    return chart;
  }

  /**
   * @param chart    Existing chart.
   * @param dataset  Data set to be added
   * @param renderer Renderer to be used (e.g., a histogram is overlaid with a fit)
   */
  public static void addLineData(JFreeChart chart, XYSeriesCollection dataset,
                                 XYItemRenderer renderer) {
    // Note: .getDatasetCount() returns the size of the map in which the pairs of index->dataset
    // are stored internally. Since Datasets can only be removed by setDataset(index,null)
    // the .getDatasetCount() does not necessarily return the number of non-null Datasets
    int existingDatasetCount = chart.getXYPlot().getDatasetCount();
    chart.getXYPlot().setDataset(existingDatasetCount, dataset);
    chart.getXYPlot().setRenderer(existingDatasetCount, renderer);
  }

  public static void addLineData(JFreeChart chart, List<ChartComponent> components) {
    for (ChartComponent component : components) {
      // There are no XY renderer in histograms
      if (!(component.getStyle() instanceof HistoChartStyle)) {

      }
      SpChartFactory.addLineData(chart, JFreeUtil.createDataset(
              component.getData().getX(),
              component.getData().getY(),
              component.getData().getSeriesName()),
          component.getStyle().getXYRenderer());
    }
  }

  public static void makeNice(JFreeChart chart) {
    /*
    BASIC THEME
     */
    chart.setAntiAlias(true);
    chart.setTextAntiAlias(true);

    ValueMarker marker = new ValueMarker(0);  // position is the value on the axis
    marker.setPaint(Colors.PLOT_ZERO_MARKER);
    marker.setStroke(LineWidthDefaults.THINNER.getStroke());

    // This is supposed to speed up symbol plots
    chart.getRenderingHints().put(
        JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);

    // XY
    if (chart.getPlot() instanceof XYPlot) {
      chart.getXYPlot().addDomainMarker(marker);
      chart.getXYPlot().addRangeMarker(marker);

      // important for DrawingSupplier
      chart.getXYPlot().setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
      chart.getXYPlot().setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

      // axes pan
      chart.getXYPlot().setRangePannable(true);
      chart.getXYPlot().setDomainPannable(true);

      // Category
    } else if (chart.getPlot() instanceof CategoryPlot) {
      CategoryMarker catMarker = new CategoryMarker(0);  // position is the value on the axis
      marker.setPaint(Colors.PLOT_ZERO_MARKER);
      marker.setStroke(LineWidthDefaults.THINNER.getStroke());
      chart.getCategoryPlot().addDomainMarker(catMarker);
      chart.getCategoryPlot().addRangeMarker(marker);

      // important for DrawingSupplier
      chart.getCategoryPlot().setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

      // axes pan
      chart.getCategoryPlot().setRangePannable(true);
    }

    /*
    BACKGROUND
     */
    chart.setBackgroundPaint(Colors.CHART_BACKGROUND);

    if (chart.getPlot() instanceof XYPlot) {
      chart.getXYPlot().setBackgroundPaint(Colors.PLOT_BACKGROUND);

      chart.getXYPlot().setRangeGridlinesVisible(true);
      chart.getXYPlot().setRangeMinorGridlinesVisible(false);
      chart.getXYPlot().setRangeGridlinePaint(Colors.PLOT_GRIDLINES);
      chart.getXYPlot().setRangeGridlineStroke(LineWidthDefaults.GRID.getStroke());

      chart.getXYPlot().setDomainGridlinesVisible(true);
      chart.getXYPlot().setDomainMinorGridlinesVisible(false);
      chart.getXYPlot().setDomainGridlinePaint(Colors.PLOT_GRIDLINES);
      chart.getXYPlot().setDomainGridlineStroke(LineWidthDefaults.GRID.getStroke());

    } else if (chart.getPlot() instanceof CategoryPlot) {
      chart.getCategoryPlot().setBackgroundPaint(Colors.BOXPLOT_BACKGROUND);

      chart.getCategoryPlot().setRangeGridlinesVisible(true);
      chart.getCategoryPlot().setRangeGridlinePaint(Colors.PLOT_GRIDLINES);
      chart.getCategoryPlot().setRangeGridlineStroke(LineWidthDefaults.GRID.getStroke());

      chart.getCategoryPlot().setDomainGridlinesVisible(true);
      chart.getCategoryPlot().setDomainGridlinePaint(Colors.PLOT_GRIDLINES);
      chart.getCategoryPlot().setDomainGridlineStroke(LineWidthDefaults.GRID.getStroke());
    }

    /*
    AXES
    */
    if (chart.getPlot() instanceof XYPlot) {

      // x axis
      if (chart.getXYPlot().getDomainAxis() != null) {
        NumberAxis domainAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
        domainAxis.setLabelFont(FontStyles.getBold());
        domainAxis.setTickLabelFont(FontStyles.getPlain());
        domainAxis.setNumberFormatOverride(NumberFormats.automatic(domainAxis.getRange()));
        // Forces JFree to use larger spacing between ticks,i.e., show fewer ticks
        // Note: Bottom = dist from num to ax.label, top = dist from num to axis.line
        domainAxis.setTickLabelInsets(new RectangleInsets(5, 15, 5, 15));
      }

      // y-axis
      if (chart.getXYPlot().getRangeAxis() != null) {
        ValueAxis valueAxis = chart.getXYPlot().getRangeAxis();
        valueAxis.setLabelFont(FontStyles.getBold());
        valueAxis.setTickLabelFont(FontStyles.getPlain());
        NumberAxis numberAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        numberAxis.setNumberFormatOverride(NumberFormats.automatic(valueAxis.getRange()));
        // Forces JFree to use larger spacing between ticks,i.e., show fewer ticks.
        // Note: left = dist from num to ax, right = dist from num to ax.label
        valueAxis.setTickLabelInsets(new RectangleInsets(15, 5, 15, 15));

      }
    } else if (chart.getPlot() instanceof CategoryPlot) {
      // x axis
      if (chart.getCategoryPlot().getDomainAxis() != null) {
        CategoryAxis domainAxis = chart.getCategoryPlot().getDomainAxis();
        domainAxis.setLabelFont(FontStyles.getBold());
        domainAxis.setTickLabelFont(FontStyles.getPlain());
      }

      // y-axis
      if (chart.getCategoryPlot().getRangeAxis() != null) {
        ValueAxis valueAxis = chart.getCategoryPlot().getRangeAxis();
        valueAxis.setLabelFont(FontStyles.getBold());
        valueAxis.setTickLabelFont(FontStyles.getPlain());
        NumberAxis numberAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        numberAxis.setNumberFormatOverride(NumberFormats.automatic(valueAxis.getRange()));
        // Forces JFree to use larger spacing between ticks,i.e., show fewer ticks
        valueAxis.setTickLabelInsets(new RectangleInsets(15, 5, 15, 15));
      }
    }

    /*
    RENDERER: MAKE THE LEGEND HAVE THE SAME COLORS
     */
    if (chart.getPlot() instanceof XYPlot) {
      for (int i = 0; i < chart.getXYPlot().getRendererCount(); i++) {
        try {

          XYItemRenderer renderer = chart.getXYPlot().getRenderer(i);
          if (renderer instanceof SamplingXYLineRenderer) {
            SamplingXYLineRenderer ren = ((SamplingXYLineRenderer) renderer);
            // legend labels
            for (int n = 0; n < ren.getLegendItems().getItemCount(); n++) {
              ren.setLegendTextPaint(n, ren.lookupSeriesPaint(n));
              ren.setLegendTextFont(n, FontStyles.SMALL.get());
              ren.getLegendItems().get(n).setShape(new Line2D.Double(50, 50, 50, 50));
              ren.getLegendItems().get(n).setShapeVisible(true);
              ren.getLegendItems().get(n).setFillPaint(ren.lookupSeriesPaint(n));
            }
          } else if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer ren = ((XYLineAndShapeRenderer) renderer);
            // legend labels
            for (int n = 0; n < ren.getLegendItems().getItemCount(); n++) {
              ren.setLegendTextPaint(n, ren.getSeriesPaint(n));
              ren.setLegendTextFont(n, FontStyles.getBold());
              ren.setSeriesShape(n, new Rectangle2D.Double(-100, -100, 20, 20));
            }
          }

        } catch (ClassCastException e) {
          LOGGER.error(ExceptionUtils.getStackTrace(e));
        }
      }

    } else if (chart.getPlot() instanceof CategoryPlot) {
      for (int i = 0; i < chart.getCategoryPlot().getRendererCount(); i++) {
        try {
          CategoryItemRenderer renderer = (CategoryItemRenderer) chart.getCategoryPlot()
              .getRenderer(i);
          if (renderer != null) {
            // legend labels
            for (int n = 0; n < renderer.getLegendItems().getItemCount(); n++) {
              // ... consider fixing this (not urgent)
              // renderer.setLegendTextPaint(n, renderer.getSeriesPaint(n));
              // renderer.setLegendTextFont(n, FontStyles.SMALLER.get());
            }
          }
        } catch (ClassCastException e) {
          LOGGER.error(ExceptionUtils.getStackTrace(e));
        }
      }
    }

    /*
     ////////////////////
     Put previous axis limits
     //////////////
     */
    if (chart.getPlot() instanceof XYPlot) {
      XYPlot plot = chart.getXYPlot();
      ValueAxis xAxis = plot.getDomainAxis();
      ValueAxis yAxis = plot.getRangeAxis();
      AxisLimits limits = SpTool3Main.getRunTime().getGuiParameterManager().getZoom();
      if (limits != null && SpTool3Main.getRunTime().getConfParams().isLockZoomInGraphs()) {
        xAxis.setRange(new Range(limits.getxLow(), limits.getxHigh()));
        yAxis.setRange(new Range(limits.getyLow(), limits.getyHigh()));
      }
    } else {
      CategoryPlot plot = chart.getCategoryPlot();
      ValueAxis yAxis = plot.getRangeAxis();
      AxisLimits limits = SpTool3Main.getRunTime().getGuiParameterManager().getZoom();
      if (limits != null && SpTool3Main.getRunTime().getConfParams().isLockZoomInGraphs()) {
        yAxis.setRange(new Range(limits.getyLow(), limits.getyHigh()));
      }
    }

  }


  public static void installSpUserComfort(ChartViewer viewer) {
    ViewerZoomMemory zoomMemory = new ViewerZoomMemory(viewer);

    /**
     * Custom handler with properties:
     * 1) Zoom memory.
     * 2) Pan with double click.
     * 3) Do not zoom while dragging to pan.
     * 4) Keep y fixed if ctl+scroll, Keep x fixed if alt+zoom
     */
    //https://stackoverflow.com/questions/64480910/how-to-disable-zoom-in-a-chartviewer/68731532
    final ChartCanvas canvas = viewer.getCanvas();
    canvas.removeAuxiliaryMouseHandler(canvas.getMouseHandler("scroll"));
    canvas.addAuxiliaryMouseHandler(new SpScrollHandlerFX("scroll", zoomMemory));

    // This has to be set BEFORE the PAN HANDLER!!!
    canvas.removeMouseHandler(canvas.getMouseHandler("zoom"));
    SpZoomHandlerFX zoomHandler = new SpZoomHandlerFX("zoom", viewer, zoomMemory);
    canvas.addMouseHandler(zoomHandler);

    // This has to be set AFTER the ZOOM HANDLER!!!
    canvas.removeMouseHandler(canvas.getMouseHandler("pan"));
    canvas.addMouseHandler(new SpPanHandlerFX("pan", zoomMemory));

    canvas.addMouseHandler(new SpDragZoomHandlerFX("dragZoom", viewer, zoomMemory));

    /**
     * Install hotkeys:
     * 1) Control Z to revert zoom.
     * 2) Control + Arrows to zoom.
     * 3) Arrows to pan.
     * 4) On clicked: request focus, in order to register the key strokes.
     */
    if (viewer.getChart().getPlot() instanceof XYPlot) {
      XYPlot plot = viewer.getChart().getXYPlot();
      ValueAxis xAxis = plot.getDomainAxis();
      ValueAxis yAxis = plot.getRangeAxis();

      // Remember axis limits in to keep them when sample change puts new plot
      xAxis.addChangeListener(new AxisChangeListener() {
        @Override
        public void axisChanged(AxisChangeEvent event) {
          AxisLimits limits = new AxisLimits(xAxis.getLowerBound(), xAxis.getUpperBound(),
              yAxis.getLowerBound(), yAxis.getUpperBound());
          SpTool3Main.getRunTime().getGuiParameterManager().updateZoom(limits);
        }
      });

      yAxis.addChangeListener(new AxisChangeListener() {
        @Override
        public void axisChanged(AxisChangeEvent event) {
          AxisLimits limits = new AxisLimits(xAxis.getLowerBound(), xAxis.getUpperBound(),
              yAxis.getLowerBound(), yAxis.getUpperBound());
          SpTool3Main.getRunTime().getGuiParameterManager().updateZoom(limits);
        }
      });

      // Add mouse handler to focus the chart, else we cannot register hotkeys.
      // Use a PauseTransition to show short change of cursor
      PauseTransition mouseShapePause = new PauseTransition(Duration.millis(500));
      mouseShapePause.setOnFinished(e -> canvas.setCursor(Cursor.DEFAULT));

      viewer.setOnMousePressed(event -> {
        viewer.requestFocus();
        if (event.getClickCount() > 1) {
          canvas.setCursor(Cursor.HAND);
          Point2D pnt = viewer.getCanvas().getLocalToParentTransform()
              .transform(event.getX(), event.getY());
          Rectangle2D plotArea = viewer.getRenderingInfo().getPlotInfo().getDataArea();
          double chartX = plot.getDomainAxis()
              .java2DToValue(pnt.getX(), plotArea, plot.getDomainAxisEdge());
          double chartY = plot.getRangeAxis()
              .java2DToValue(pnt.getY(), plotArea, plot.getRangeAxisEdge());
          SpTool3Main.getRunTime().getXPosition().set(chartX);
          SpTool3Main.getRunTime().getYPosition().set(chartY);
          LOGGER.trace("Copied xy position in the chart: x=" + chartX + " y=" + chartY + ".");
          //
          mouseShapePause.playFromStart();
        }
      });

      viewer.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
        @Override
        public void handle(KeyEvent event) {
          Range xRng = xAxis.getRange();
          Range yRng = yAxis.getRange();

          if (StageFactory.KEY_CTL_Z.match(event)) {
            zoomMemory.resetZoom();
          } else if (event.getCode().isArrowKey()) {

            boolean wasCtl = false;
            double reducedXRangeLength = xAxis.getRange().getLength() * 0.10;
            double reducedYRangeLength = yAxis.getRange().getLength() * 0.10;

            // If not ctl+z --> Check if Ctl+Arrow is present, i.e., if we need Zooming
            if (StageFactory.KEY_CTL_DOWN.match(event)) {
              wasCtl = true;
              zoomMemory.storeZoom();
              if (yRng.getUpperBound() > yRng.getLowerBound()) {
                yAxis.setRange(yRng.getLowerBound() - reducedYRangeLength,
                    yRng.getUpperBound() + reducedYRangeLength);
              }
            }

            if (StageFactory.KEY_CTL_UP.match(event)) {
              wasCtl = true;
              zoomMemory.storeZoom();
              if (yRng.getUpperBound() > yRng.getLowerBound()) {
                yAxis.setRange(yRng.getLowerBound() + reducedYRangeLength,
                    yRng.getUpperBound() - reducedYRangeLength);
              }
            }

            if (StageFactory.KEY_CTL_RIGHT.match(event)) {
              wasCtl = true;
              zoomMemory.storeZoom();
              if (yRng.getUpperBound() > yRng.getLowerBound()) {
                xAxis.setRange(xRng.getLowerBound() + reducedXRangeLength,
                    xRng.getUpperBound() - reducedXRangeLength);
              }
            }

            if (StageFactory.KEY_CTL_LEFT.match(event)) {
              wasCtl = true;
              zoomMemory.storeZoom();
              if (yRng.getUpperBound() > yRng.getLowerBound()) {
                xAxis.setRange(xRng.getLowerBound() - reducedXRangeLength,
                    xRng.getUpperBound() + reducedXRangeLength);
              }
            }

            // If not control --> Panning
            if (!wasCtl) {
              if (event.getCode() == KeyCode.KP_RIGHT || event.getCode() == KeyCode.RIGHT) {
                if (xRng.getUpperBound() > xRng.getLowerBound()) {
                  zoomMemory.storeZoom();
                  xAxis.setRange(xRng.getLowerBound() + reducedXRangeLength,
                      xRng.getUpperBound() + reducedXRangeLength);
                }
              }
              if (event.getCode() == KeyCode.KP_LEFT || event.getCode() == KeyCode.LEFT) {
                if (xRng.getUpperBound() > xRng.getLowerBound()) {
                  zoomMemory.storeZoom();
                  xAxis.setRange(xRng.getLowerBound() - reducedXRangeLength,
                      xRng.getUpperBound() - reducedXRangeLength);
                }
              }

              if (event.getCode() == KeyCode.KP_DOWN || event.getCode() == KeyCode.DOWN) {
                if (yRng.getUpperBound() > yRng.getLowerBound()) {
                  zoomMemory.storeZoom();
                  yAxis.setRange(yRng.getLowerBound() - reducedYRangeLength,
                      yRng.getUpperBound() - reducedYRangeLength);
                }
              }

              if (event.getCode() == KeyCode.KP_UP || event.getCode() == KeyCode.UP) {
                if (yRng.getUpperBound() > yRng.getLowerBound()) {
                  zoomMemory.storeZoom();
                  yAxis.setRange(yRng.getLowerBound() + reducedYRangeLength,
                      yRng.getUpperBound() + reducedYRangeLength);
                }
              }
            }
          }
          event.consume(); // else, other stuff on the UI gets selected via focus traversing
          viewer.requestFocus(); // continue try to keep focus
        }
      });
    } else {
      CategoryPlot plot = viewer.getChart().getCategoryPlot();
      CategoryAxis xAxis = plot.getDomainAxis();
      ValueAxis yAxis = plot.getRangeAxis();

      // Remember axis limits in to keep them when sample change puts new plot
      xAxis.addChangeListener(new AxisChangeListener() {
        @Override
        public void axisChanged(AxisChangeEvent event) {
          AxisLimits limits = new AxisLimits(xAxis.getLowerMargin(), xAxis.getUpperMargin(),
              yAxis.getLowerBound(), yAxis.getUpperBound());
          SpTool3Main.getRunTime().getGuiParameterManager().updateZoom(limits);
        }
      });

      yAxis.addChangeListener(new AxisChangeListener() {
        @Override
        public void axisChanged(AxisChangeEvent event) {
          AxisLimits limits = new AxisLimits(xAxis.getLowerMargin(), xAxis.getUpperMargin(),
              yAxis.getLowerBound(), yAxis.getUpperBound());
          SpTool3Main.getRunTime().getGuiParameterManager().updateZoom(limits);
        }
      });

      // BoxPlot category x axis cannot be zoomed, so there is no need to implement the functions.
    }
  }

  public static Pair<ScrollPane, Pane> getLegend(List<ChartComponent> components, boolean forceSingleColumn) {
    ScrollPane scroll = new ScrollPane();

    Pane pane;
    if (!forceSingleColumn) {
      FlowPane flow = new FlowPane();
      flow.setVgap(6); //8
      flow.setHgap(12); // 10
      flow.setAlignment(Pos.TOP_LEFT);
      pane = flow;
    } else {
      VBox box = new VBox(6);
      box.setAlignment(Pos.CENTER_LEFT);
      pane = new VBox(6);
    }

    scroll.setContent(UiUtil.putOnAnchorWithoutInsets(pane));
    scroll.setFitToWidth(true); // puts things in the middle on the scroll pane apparently...
    scroll.setMinHeight(5);

    // Make FlowPane fill the available width
    pane.prefWidthProperty().bind(Bindings.createDoubleBinding(
        () -> scroll.getViewportBounds().getWidth(),
        scroll.viewportBoundsProperty()
    ));

    // DO NOT bind prefHeight — let it grow naturally
  /*
   DO NOT call setFitToHeight(true)
   prefHeightProperty().bind(...) is the culprit. It makes the FlowPane "lie" about its size:
   it says it's never taller than the viewport, so ScrollPane thinks there is nothing to scroll!
   My idea was:     // scroll.setFitToHeight(true); // when this is false, the flow pane does not fill the
   whole pane
   */
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(false); // important!

    // Optional: Trigger layout if width changes
    scroll.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
      pane.requestLayout();
    });

    UiUtil.formatLegendScrollPane(scroll);
    UiUtil.makePaneBrightAndRound(scroll);

    UiUtil.makePaneBright(pane);

    for (ChartComponent component : components) {
      String series = component.getData().getSeriesName();
      Label label = new Label(series);
      label.setFont(FontStyles.SMALL.getFX());
      //
      Color color = component.getStyle().getColorFX();
      Color txtColor = Colors.removeAlpha(color);
      txtColor = txtColor.darker().darker();
      label.setTextFill(txtColor);
      //
      if (component.getStyle().isOverrideLegendBoxSymbol()) {
        label.setGraphic(component.getStyle().getColoredFxMarker());
      } else {
        label.setGraphic(component.getStyle().getLegendSymbol());
      }

      pane.getChildren().add(label);
    }

    return new Pair<>(scroll, pane);
  }

  public static ChartContainer bundleChartLegend(JFreeChart chart,
                                                 List<ChartComponent> components,
                                                 double width, double height) {
    return bundleChartLegend(chart,
        components,
        width, height,
        false,
        Orientation.VERTICAL,
        false);
  }

  public static ChartContainer bundleChartLegend(JFreeChart chart,
                                                 List<ChartComponent> components,
                                                 double width, double height,
                                                 Orientation splitOrientation) {
    return bundleChartLegend(chart,
        components,
        width, height,
        false,
        splitOrientation,
        false);
  }

  public static ChartContainer bundleChartLegend(JFreeChart chart,
                                                 List<ChartComponent> components,
                                                 double width, double height,
                                                 boolean disableSplitDivider,
                                                 boolean singleCol) {

    return bundleChartLegend(chart,
        components,
        width, height,
        disableSplitDivider,
        Orientation.VERTICAL,
        singleCol);
  }

  public static ChartContainer bundleChartLegend(JFreeChart chart,
                                                 List<ChartComponent> components,
                                                 double width, double height,
                                                 boolean disableSplitDivider,
                                                 Orientation splitOrientation,
                                                 boolean singleCol) {

    // Colors, ...
    makeNice(chart);

    // The viewer
    ChartViewer viewer = new ChartViewer(chart);
    viewer.setPrefSize(width, height);

    // Zoom, ...
    installSpUserComfort(viewer);

    // The container: Split Pane
    BorderPane combinedPane = new BorderPane();
    SplitPane splitPane = new SplitPane();
    splitPane.setOrientation(splitOrientation);

    Pair<ScrollPane, Pane> legend = getLegend(components, singleCol);
    splitPane.getItems().addAll(viewer, UiUtil.putOnAnchorWithoutInsets(legend.getKey()));

    if (!disableSplitDivider) {
      splitPane.setDividerPositions(SpTool3Main.getRunTime().getGuiParameterManager()
          .getLayoutParameters().getChartDivider());

      splitPane.getDividers().get(0).positionProperty().addListener(
          (observable, oldValue, newValue) -> {
            SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters()
                .setChartDivider(newValue.doubleValue());
          });
    } else {
      splitPane.setDividerPositions(0.99);
      splitPane.getDividers().get(0).positionProperty().addListener(
          (observable, oldValue, newValue) -> {
            splitPane.setDividerPositions(0.99);
          });
    }

    combinedPane.setCenter(splitPane);

    // Save context menu...
    JFreeExportUtils.makeExportable(viewer, legend.getValue());

    AnchorPane anchorPane = UiUtil.putOnAnchorWithoutInsets(combinedPane);
    anchorPane.setPrefSize(width, height);

    viewer.requestFocus();
    return new ChartContainer(anchorPane, viewer, legend.getKey());

  }

  public static class ChartContainer {

    public final AnchorPane combinedPane;
    public final ChartViewer viewer;
    public final ScrollPane legend;

    public ChartContainer(AnchorPane combinedPane, ChartViewer viewer,
                          ScrollPane legend) {
      this.combinedPane = combinedPane;
      this.viewer = viewer;
      this.legend = legend;
    }
  }


  private static void setTickStepsToFive(ValueAxis axis) {
    // JFree Chart chooses the step size between ticks from this list to maximize the number
    // of ticks shown without overlapping of the labels.
    TickUnits units = new TickUnits();
    NumberFormat nf = new DecimalFormat(NF.D1C6.pattern());
    double low = Math.abs(axis.getRange().getLowerBound());
    double up = Math.abs(axis.getRange().getUpperBound());
    int lowI = (int) Math.floor(Math.log10(low)) - 1;
    if (low <= 0) {
      lowI = -1;
    }
    int upI = (int) Math.ceil(Math.log10(up)) + 1;
    if (up <= 0) {
      upI = -1;
    }

    for (int i = Math.min(lowI, upI); i < Math.max(upI, lowI); i++) {
      units.add(new NumberTickUnit(5 * Math.pow(10, i), nf));
    }
    axis.setStandardTickUnits(units);
  }

  //
  public static Range clone(Range range) {
    return new Range(range.getLowerBound(), range.getUpperBound());
  }

  public static boolean equals(Range rangeA, Range rangeB) {
    return rangeA.getLowerBound() == rangeB.getLowerBound() &&
        rangeA.getUpperBound() == rangeB.getUpperBound();
  }


  public static class ChartComponent {

    private final ChartData data;
    private final ChartStyle style;

    public ChartComponent(ChartData data, ChartStyle style) {
      this.data = data;
      this.style = style;
    }

    public ChartComponent(ChartComponent component, double[] x, double[] y) {
      this.data = new ChartData(component.getData(), x, y);
      this.style = component.getStyle();
    }

    public ChartComponent(ChartData data) {
      this.data = data;
      this.style = new ChartStyle();
    }

    public ChartData getData() {
      return data;
    }

    public ChartStyle getStyle() {
      return style;
    }

  }

  // Container for the chart data. Idea: create charts more iteratively
  // instead of having to create a chart and then add additional data in case of multiple per plot.
  public static class ChartData {

    private final String seriesName;
    private final String seriesShortname;
    private final double[] x;
    private final double[] y;
    private final TISeries tiSeries;
    private final XYSeries xySeries;
    private final String xLbl;
    private final String yLbl;
    private final Unit xUnit;
    private final Unit yUnit;
    private final MathMod xMath;
    private final MathMod yMath;

    public ChartData(String seriesName, double[] x, double[] y,
                     String xLbl, Unit xUnit, MathMod xMath, String yLbl, Unit yUnit, MathMod yMath) {
      this(seriesName, seriesName, x, y, xLbl, xUnit, xMath, yLbl, yUnit, yMath);
    }

    public ChartData(String seriesName, String seriesShortname, double[] x, double[] y,
                     String xLbl, Unit xUnit, MathMod xMath, String yLbl, Unit yUnit, MathMod yMath) {
      this.seriesName = seriesName;
      this.seriesShortname = seriesShortname;
      this.x = x;
      this.y = y;
      this.xLbl = xLbl;
      this.yLbl = yLbl;
      this.xUnit = xUnit;
      this.yUnit = yUnit;
      this.xMath = xMath;
      this.yMath = yMath;
      this.tiSeries = null;
      this.xySeries = null;
    }

    public ChartData(String seriesName, List<Double> x, List<Double> y,
                     String xLbl, Unit xUnit, MathMod xMath, String yLbl, Unit yUnit, MathMod yMath) {
      this.seriesName = seriesName;
      this.seriesShortname = seriesName;
      this.x = ArrUtils.doubleListToArr(x);
      this.y = ArrUtils.doubleListToArr(y);
      this.xLbl = xLbl;
      this.yLbl = yLbl;
      this.xUnit = xUnit;
      this.yUnit = yUnit;
      this.xMath = xMath;
      this.yMath = yMath;
      this.tiSeries = null;
      this.xySeries = null;
    }

    public ChartData(String seriesName, TISeries tiSeries,
                     String xLbl, Unit xUnit, MathMod xMath, String yLbl, Unit yUnit, MathMod yMath) {
      this.seriesName = seriesName;
      this.seriesShortname = seriesName;
      this.x = tiSeries.getX();
      this.y = tiSeries.getY();
      this.xLbl = xLbl;
      this.yLbl = yLbl;
      this.xUnit = xUnit;
      this.yUnit = yUnit;
      this.xMath = xMath;
      this.yMath = yMath;
      this.tiSeries = tiSeries;
      this.xySeries = null;
    }

    public ChartData(XYSeries xySeries,
                     String xLbl, Unit xUnit, MathMod xMath, String yLbl, Unit yUnit, MathMod yMath) {
      this.seriesName = xySeries.getKey().toString();
      this.seriesShortname = seriesName;
      this.x = new double[0];
      this.y = new double[0];
      this.xLbl = xLbl;
      this.yLbl = yLbl;
      this.xUnit = xUnit;
      this.yUnit = yUnit;
      this.xMath = xMath;
      this.yMath = yMath;
      this.tiSeries = null;
      this.xySeries = xySeries;
    }

    // New Instance with different data
    public ChartData(ChartData data, double[] x, double[] y) {
      this.seriesName = data.getSeriesName();
      this.seriesShortname = seriesName;
      this.x = x;
      this.y = y;
      this.xLbl = data.xLbl;
      this.yLbl = data.yLbl;
      this.xUnit = data.xUnit;
      this.yUnit = data.yUnit;
      this.xMath = data.xMath;
      this.yMath = data.yMath;
      this.tiSeries = null;
      this.xySeries = null;
    }

    public String getSeriesName() {
      return seriesName;
    }

    public String getSeriesShortname() {
      return seriesShortname;
    }

    public double[] getX() {
      return x;
    }

    public double[] getY() {
      return y;
    }

    public String translateXLbl() {
      if (xMath.equals(MathMod.NONE)) {
        return xLbl + " /" + xUnit.getAxisString();
      } else {
        return xMath.getUiString() + "(" + xLbl + " /" + xUnit.getAxisString() + ")";
      }
    }

    public String getxLbl() {
      return xLbl;
    }

    public Unit getxUnit() {
      return xUnit;
    }

    public MathMod getxMath() {
      return xMath;
    }

    public String translateYLbl() {
      if (yMath.equals(MathMod.NONE)) {
        return yLbl + " /" + yUnit.getAxisString();
      } else {
        return yMath.getUiString() + "(" + yLbl + " /" + yUnit.getAxisString() + ")";
      }
    }

    public String getyLbl() {
      return yLbl;
    }

    public Unit getyUnit() {
      return yUnit;
    }

    public MathMod getyMath() {
      return yMath;
    }

    @Nullable
    public TISeries getTiSeries() {
      return tiSeries;
    }

    @Nullable
    public XYSeries getXySeries() {
      return xySeries;
    }
  }

  public static class HistogramChartData extends ChartData {

    private final double thrLOD;

    public HistogramChartData(String seriesName, double[] y,
                              String xLbl, Unit xUnit, MathMod xMath, String yLbl, Unit yUnit, MathMod yMath,
                              double thrLOD) {
      super(seriesName, seriesName, y, y, xLbl, xUnit, xMath, yLbl, yUnit, yMath);
      this.thrLOD = thrLOD;
    }

    public HistogramChartData(String seriesName, String seriesShortname, double[] y,
                              String xLbl, Unit xUnit, MathMod xMath, String yLbl, Unit yUnit, MathMod yMath,
                              double thrLOD) {
      super(seriesName, seriesShortname, y, y, xLbl, xUnit, xMath, yLbl, yUnit, yMath);
      this.thrLOD = thrLOD;
    }

    public double getThrLOD() {
      return thrLOD;
    }
  }

  // Container with information on Chart Style.
  public static class ChartStyle {

    private final Colors paint;
    private final LineWidth width;
    private final LineDash dashPattern;
    private final MarkerSize markerSize;
    private final MarkerStyle markerStyle;
    private final boolean overrideLegendBoxSymbol;
    private final RendererOption rendererOption;
    private final LineGraphStyle lineMarker;
    private final BarFillPattern fillPattern;
    private final double alpha;
    private final boolean active;

    public ChartStyle() {
      // Defaults
      this.paint = SpV2Colors.BLACK;
      this.width = LineWidthDefaults.MEDIUM;
      this.dashPattern = LineLineDashDefaults.STRAIGHT;
      this.markerSize = MarkerSizeDefaults.MEDIUM;
      this.markerStyle = MarkerStyle.CROSS;
      this.overrideLegendBoxSymbol = false;
      this.rendererOption = RendererOption.LINE_AND_SHAPE;
      this.lineMarker = LineGraphStyle.LINE_AND_MARKER;
      this.fillPattern = BarFillPattern.NONE;
      this.alpha = 1; // i.e. no alpha
      this.active = false;
    }

    public ChartStyle(Colors paint, double alpha, LineWidthDefaults width, LineDash dashPattern,
                      MarkerSize markerSize, MarkerStyle markerStyle,
                      boolean overrideLegendBoxSymbol,
                      RendererOption rendererOption, LineGraphStyle lineGraphStyle,
                      BarFillPattern fillPattern) {
      this.paint = paint;
      this.width = width;
      this.dashPattern = dashPattern;
      this.markerSize = markerSize;
      this.markerStyle = markerStyle;
      this.overrideLegendBoxSymbol = overrideLegendBoxSymbol;
      this.rendererOption = rendererOption;
      this.lineMarker = lineGraphStyle;
      this.fillPattern = fillPattern;
      this.alpha = Math.min(alpha, 1);
      this.active = true;
    }

    public ChartStyle(Colors paint, double alpha, LineWidthDefaults width, LineDash dashPattern,
                      MarkerSize markerSize, MarkerStyle markerStyle,
                      boolean overrideLegendBoxSymbol,
                      RendererOption rendererOption, LineGraphStyle lineGraphStyle) {
      this.paint = paint;
      this.width = width;
      this.dashPattern = dashPattern;
      this.markerSize = markerSize;
      this.markerStyle = markerStyle;
      this.overrideLegendBoxSymbol = overrideLegendBoxSymbol;
      this.rendererOption = rendererOption;
      this.lineMarker = lineGraphStyle;
      this.fillPattern = BarFillPattern.NONE;
      this.alpha = Math.min(alpha, 1);
      this.active = true;
    }


    public Paint getPaint() {
      return paint.get(alpha);
    }

    public Color getColorFX() {
      return paint.getFX(alpha);
    }

    public BasicStroke getStroke() {
      if (dashPattern == null || dashPattern.get() == null) {
        return width.getStroke();
      } else {
        return new BasicStroke(
            width.get(),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            1.0f,
            dashPattern.get(),
            0.0f
        );
      }
    }

    public Shape getAwtMarker() {
      return markerStyle.getAwt(markerSize.get());
    }

    public MarkerStyle getMarkerStyle() {
      return markerStyle;
    }

    public javafx.scene.shape.Shape getColoredFxMarker() {
      javafx.scene.shape.Shape shape = markerStyle.getFx(markerSize.get());
      shape.setFill(paint.getFX());
      shape.setStroke(paint.getFX());
      return shape;
    }

    public Node getLegendSymbol() {
      int width = 15;
      int height = 10;

      Rectangle rectangle = new Rectangle(width, height, paint.getFX());
      Node result = rectangle;

      if (fillPattern != null && !fillPattern.equals(BarFillPattern.NONE)) {
        Line stripe1 = new Line(0, 0, width, height);
        stripe1.setStroke(Color.WHITE);
        stripe1.setStrokeWidth(1); // adjust width as needed

        Line stripe2 = new Line(0, width, height, 0);
        stripe2.setStroke(Color.WHITE);
        stripe1.setStrokeWidth(1); // adjust width as needed

        // Group the base shape and the stripes
        Group group = new Group(rectangle, stripe1, stripe2);
        result = group;
      }
      return result;
    }

    public boolean isActive() {
      return active;
    }

    public XYItemRenderer getXYRenderer() {

      return switch (rendererOption) {
        case LINE_AND_SHAPE -> {
          XYItemRenderer renderer;
          if (lineMarker.drawLine()) {
            renderer = new SingleColorLineShapeRenderer(this); // default
            ((XYLineAndShapeRenderer) renderer).setSeriesShapesVisible(0, lineMarker.drawMarkers());
            ((XYLineAndShapeRenderer) renderer).setSeriesLinesVisible(0, lineMarker.drawLine());
          } else {
            renderer = new SingleColorShapeRenderer(this); // default
          }
          yield renderer;
        }
        case SAMPLING_LINE_AND_SHAPE -> {
          yield new SingleColorSamplingLineShapeRenderer(this);
        }
        case AREA -> {
          yield new SingleColorAreaLineRenderer(this);
        }
      };
    }

    public XYItemRenderer getXYBarRenderer() {
      XYBarRenderer renderer = new SingleColorBarRenderer(this); // default
      renderer.setBarPainter(fillPattern.get());

      renderer.setShadowVisible(false);
      renderer.setDrawBarOutline(false); // else: at low bin width only outline is left!
      return renderer;
    }

    public boolean isOverrideLegendBoxSymbol() {
      return overrideLegendBoxSymbol;
    }


  }

  public static class HistoChartStyle extends ChartStyle {

    public HistoChartStyle(Colors paint, double alpha, BarFillPattern barFillPattern) {

      super(paint, alpha,
          null, null, null, null, false, null, null, barFillPattern);
    }
  }


}
