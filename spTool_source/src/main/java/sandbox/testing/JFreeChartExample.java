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

package sandbox.testing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import visualizer.styles.Colors;
import visualizer.styles.OkabeItoColors;

public class JFreeChartExample {

  private XYSeriesCollection dataset;

  // Method to create and show the chart in a JFrame
  public void createAndShowChart(double[] xData, double[] yData, String seriesName) {
    // Create a dataset from the x and y data
    XYSeries series = new XYSeries(seriesName);
    for (int i = 0; i < xData.length; i++) {
      series.add(xData[i], yData[i]);
    }

    this.dataset = new XYSeriesCollection(series);

    // Create a chart using the dataset
    JFreeChart chart = ChartFactory.createXYLineChart(
        "",      // Chart title
        "Time /s",                  // X-Axis label
        "Signal /cts·DT-1",                  // Y-Axis label
        dataset,                   // Dataset
        PlotOrientation.VERTICAL,  // Plot orientation
        true,                      // Show legend
        true,                      // Use tooltips
        false                      // Configure chart to generate URLs?
    );

    chart.setAntiAlias(true);
    chart.getRenderingHints()
        .put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    XYItemRenderer renderer = new SamplingXYLineRenderer();
    renderer.setDefaultStroke(new BasicStroke(10));
    renderer.setSeriesPaint(0, Colors.PLOT_BACKGROUND); // First data are a dummy initialization!
    for (int i = 1; i < OkabeItoColors.getColors().length; i++) {
      Color c = OkabeItoColors.getColors()[i];
      renderer.setSeriesPaint(i, c);
    }

    chart.getXYPlot().setRenderer(renderer);

    // Customize the chart (optional)
    chart.setBackgroundPaint(Color.white);
    chart.getXYPlot().setBackgroundPaint(Color.white);

    // Create and set up a panel to display the chart
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new Dimension(560, 370));

    // Create a new JFrame to display the chart
    JFrame frame = new JFrame("Simulated data");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(chartPanel);
    frame.pack();
    frame.setVisible(true);
  }

  // Method to add another series to the dataset
  public void addSeries(double[] xData, double[] yData, String seriesName) {
    XYSeries series = new XYSeries(seriesName);
    for (int i = 0; i < xData.length; i++) {
      series.add(xData[i], yData[i]);
    }
    dataset.addSeries(series);
  }


}
