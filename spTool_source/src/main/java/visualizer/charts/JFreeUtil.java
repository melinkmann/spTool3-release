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

import dataModelNew.TISeries;

import java.util.ArrayList;
import java.util.List;

import dataModelNew.mz.Element;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import processing.options.BinWidthEstimator;
import processing.options.HistogramNormalization;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;
import visualizer.charts.SpChartFactory.ChartComponent;

public abstract class JFreeUtil {

  public static String buildTooltip(double minAbundance, double minSignal, double maxYValue,
                                    boolean showAbundance,
                                    List<Isotope> excludedFromLabel,
                                    XYIntervalSeriesCollection ds, int series, int item) {

    double mz = ds.getXValue(series, item);
    int nominalMZ = (int) Math.round(mz);

    List<String> matchingAllowedIsotopes = new ArrayList<>();
    List<String> matchingIsotopes = new ArrayList<>();


    for (Element ele : Element.values()) {
      for (Isotope iso : ele.getIsotopes()) {

        // check if sth was matched - if yes, we know that we do not have to show the generic
        // tip = SnF.doubleToString(mz, NF.D1C3); label
        if (iso.getIsotopicNumber() == nominalMZ) {
          matchingIsotopes.add(iso.getNumberAndElement());
        }

        // Now check if the labels are supposed to be shown
        double relSignal = ds.getYValue(series, item) / maxYValue;
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
      tip = SnF.doubleToString(mz, NF.D1C3);
    }

    return tip;
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
