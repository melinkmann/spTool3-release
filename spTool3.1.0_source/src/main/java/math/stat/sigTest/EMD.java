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

package math.stat.sigTest;

import com.google.common.math.Stats;
import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.util.Pair;
import math.Arithmetic;
import math.stat.Median;
import math.units.enums.ViewUnits;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import processing.options.BinWidthEstimator;
import processing.options.HistogramNormalization;
import util.ArrUtils;
import visualizer.charts.BarFillPattern;
import visualizer.charts.SpChartFactory;
import visualizer.charts.SpChartFactory.ChartComponent;
import visualizer.charts.SpChartFactory.HistoChartStyle;
import visualizer.charts.SpChartFactory.HistogramChartData;
import visualizer.styles.OkabeItoColors;

public abstract class EMD {

  private static final Logger LOGGER = LogManager.getLogger(EMD.class);

  /**
   * Basic idea: MEDIAN: H0 assumes that there is not difference in x and y, so we pool them and
   * draw x.length and y.length (or a number on that order) random observations from the pool
   * including repetitions. Then we calculate the difference and put it in a list. We do this n
   * times (n >= 1000). Now, the differences should follow a normal distribution. We then compare
   * this distribution with the observed difference in the data set an calculate a p-value. When in
   * fact, x and y are different distributions, the observed difference should be way larger than
   * the pooled difference.
   * <p>
   * Basic idea: Earth Movers Distance: We do not know what kind of EMD represents "little
   * difference". But we can bootstrap EMDs of the pooled x and y data, which generates random
   * histograms assuming equal source distributions of x and y. The log10 of the EMDs appears to
   * follow a normal distribution. We can then calculate the p-value of the observed EMD. If it is
   * far out, it is much more likely that x and y are in fact not from the same pool but from
   * different distributions.
   */
  public static double calcPermutationDifferences(double[] x, double[] y, int n) {
    double p = 1;
    XoRoShiRo128PlusRandom xoroRandom = new XoRoShiRo128PlusRandom();
    try {
      double xmean = Median.median(x);
      double ymean = Median.median(y);
      double originalMeanDifference = xmean - ymean;

      double originalEMD = Math.log10(emd1D(x, y));

      double[] xy = ArrUtils.concat(x, y);
      double[] diff = new double[n];
      double[] emdArr = new double[n];

      int rep = 0;
      while (rep < n) {

        //double randMeanX = 0;
        List<Double> randX = new ArrayList<>();
        for (int i = 0; i < x.length; i++) {
          randX.add(xy[Arithmetic.getFastRandomInteger(xoroRandom, 0, xy.length)]);
        }
        //randMeanX /= x.length;
        double randMeanX = Median.median(randX);

        //double randMeanY = 0;
        List<Double> randY = new ArrayList<>();
        for (int i = 0; i < y.length; i++) {
          randY.add(xy[Arithmetic.getFastRandomInteger(xoroRandom, 0, xy.length)]);
        }
        // randMeanY /= y.length;
        double randMeanY = Median.median(randY);

        diff[rep] = randMeanX - randMeanY;
        emdArr[rep] = Math.log10(emd1D(ArrUtils.doubleListToArr(randX),
            ArrUtils.doubleListToArr(randY)));
        rep++;
      }

      double repMedianMean = Stats.meanOf(diff);
      double repMedianSD = Stats.of(diff).sampleStandardDeviation();
      double zMedian = (originalMeanDifference - repMedianMean) / repMedianSD;
      zMedian = Math.abs(zMedian);

      double repEMDMean = Stats.meanOf(emdArr);
      double repEMDSD = Stats.of(emdArr).sampleStandardDeviation();
      double zEMD = (originalEMD - repEMDMean) / repEMDSD;
      zEMD = Math.abs(zEMD);

      PermutationResults medianPerm = new PermutationResults("Permutation of Median", zMedian,
          x.length, y.length, diff);
      PermutationResults emdPerm = new PermutationResults("Permutation of log10(EDF)", zEMD,
          x.length, y.length, emdArr);

      // Here, so far only use the EMD.
      p = emdPerm.pValue;

    } catch (Exception e) {
      LOGGER.warn("Cannot calculate permutation. Error message: " + e.getMessage()
          + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }
    return p;
  }


  // Proposed measure. May not be ideal. Developed as a tool w chatGPT and some tinkering with plotting.
  public static double similarity(double[] arr1, double[] arr2) {
    // return 1.0 / (1.0 + emd1D(arr1, arr2));
    // Compute exponential similarity: y = (e^{-b x} - e^{-b}) / (1 - e^{-b})
    double decay = Math.E; // other values possible, larger values scaler decay stronger
    double expMinusB = Math.exp(-decay);
    double similarity = (Math.exp(-decay * emd1D(arr1, arr2) ) - expMinusB) / (1 - expMinusB);
    return similarity;
  }

  /**
   * ChatGPT (double check!): 1D version does not need histogram but CDF is fine.
   * Computes 1D Earth Mover's Distance / Wasserstein distance between two arrays.
   * Arrays do not need to be normalized or equal length.
   */
  public static double emd1D(double[] a, double[] b) {

    if (a.length == 0) return b.length == 0 ? 0.0 : 1.0;
    if (b.length == 0) return 1.0;

    // Sort the arrays
    double[] sortedA = Arrays.copyOf(a, a.length);
    double[] sortedB = Arrays.copyOf(b, b.length);
    Arrays.sort(sortedA);
    Arrays.sort(sortedB);

    double maxA = sortedA[sortedA.length - 1];
    double maxB = sortedB[sortedB.length - 1];
    double minA = sortedA[0];
    double minB = sortedB[0];

    int n = sortedA.length;
    int m = sortedB.length;

    int i = 0, j = 0;
    double prevX = Math.min(sortedA[0], sortedB[0]);
    double cdfA = 0.0;
    double cdfB = 0.0;
    double emd = 0.0;

    // Walk through both arrays
    while (i < n || j < m) {
      double nextA = i < n ? sortedA[i] : Double.POSITIVE_INFINITY;
      double nextB = j < m ? sortedB[j] : Double.POSITIVE_INFINITY;

      double x;
      if (nextA <= nextB) {
        x = nextA;
        cdfA = (double) (i + 1) / n;
        i++;
      } else {
        x = nextB;
        cdfB = (double) (j + 1) / m;
        j++;
      }

      // Area contribution: |CDF difference| * dx
      double dx = x - prevX;
      emd += Math.abs(cdfA - cdfB) * dx;

      prevX = x;
    }

    // normalize emd to max diff
    double maxEMD = Math.max(
        Math.abs(maxA - minB),
        Math.abs(maxB - minA)
    );

    double normalizedEMD = emd / maxEMD;

    return normalizedEMD;
  }

  /**
   * TODO: Double check!
   *  1) check normalization after EMD
   *  2) make sure that apache commons i used correctly (input should be histogram y values)
   *  3) make sure that bin width is included: apache does not do that!
   */
  private static double calcEMD(double[] xArr, double[] yArr) {
    JFreeChart histChart = getHistogram(xArr, yArr).getChart();
    XYPlot plot = histChart.getXYPlot();
    double emdValue = Double.MAX_VALUE;
    if (plot != null) {
      List<Double> x = new ArrayList<>();
      List<Double> y = new ArrayList<>();
      // if histograms are overlays: 1 dataset each
      int datSetCount = plot.getDatasetCount();
      for (int setIdx = 0; setIdx < datSetCount; setIdx++) {
        XYDataset dataset = plot.getDataset(setIdx);
        if (dataset instanceof HistogramDataset) {
          HistogramDataset histogramDataset = (HistogramDataset) dataset;
          // here, histograms have only one series and the overlay is done by adding datasets to the
          // XYPlot.
          int seriesCount = histogramDataset.getSeriesCount();
          if (seriesCount > 0) {
            int barCount = histogramDataset.getItemCount(0);
            for (int barIdx = 0; barIdx < barCount; barIdx++) {
              if (setIdx == 0) {
                x.add(histogramDataset.getY(0, barIdx).doubleValue());
              } else if (setIdx == 1) {
                y.add(histogramDataset.getY(0, barIdx).doubleValue());
              }
            }
          }
        }
      }
      if (x.size() == y.size()) {
        // https://math.stackexchange.com/questions/714476/how-do-you-compute-numerically-the-earth-movers-distance-emd
        EarthMoversDistance emd = new EarthMoversDistance();
        emdValue = emd.compute(ArrUtils.doubleListToArr(x),
            ArrUtils.doubleListToArr(y));
      }
    }
    return emdValue;
  }


  private static ChartViewer getHistogram(double[] x, double[] y) {

    List<ChartComponent> histoComponents = new ArrayList<>();
    histoComponents.add(new ChartComponent(
        new HistogramChartData(
            "Dummy",
            x,
            "X", ViewUnits.NONE,
            "Y", ViewUnits.NONE, 0d),
        new HistoChartStyle(OkabeItoColors.BLACK,
            1, BarFillPattern.NONE)
    ));

    histoComponents.add(new ChartComponent(
        new HistogramChartData(
            "Dummy",
            y,
            "X", ViewUnits.NONE,
            "Y", ViewUnits.NONE, 0d),
        new HistoChartStyle(OkabeItoColors.BLACK,
            1, BarFillPattern.NONE)
    ));

    Pair<JFreeChart, Double> chartPair = SpChartFactory.createHistogram(
        histoComponents,
        HistogramNormalization.RELATIVE_FREQUENCY, // normalize bars or else sample size matters greatly!
        BinWidthEstimator.TERRELL_SCOTT,
        1
    );

    ChartViewer view = new ChartViewer(chartPair.getKey());
    // w/o setPrefSize nothing is shown
    view.setPrefSize(500, 300);
    view.setMaxSize(450, 300);
    return view;
  }


  private static class PermutationResults {

    public final double pValue;
    public final double[] differences;
    public final double ksNormalPValue;

    public PermutationResults(String test, double z, int xlen, int ylen, double[] differences) {
      this.differences = differences;
      //
      NormalDistribution standardNormal = new NormalDistribution(0.0D, 1.0D);
      double p = 2.0D * Math.min(
          standardNormal.cumulativeProbability(z),
          (1 - standardNormal.cumulativeProbability(z)));
      double effect = z / Math.sqrt(xlen + ylen);
      // this.statResults = new StatResults(test, z, p, effect);
      this.pValue = p; // I think this is correct (porting from spTool2 to spTool3)

      // I think, this is just for checking if the assumption that EMD result is N-distributed
      KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
      double ksMean = Stats.meanOf(differences);
      double ksSD = Stats.of(differences).sampleStandardDeviation();
      RealDistribution ksNorm = new NormalDistribution(ksMean, ksSD);
      ksNormalPValue = ksTest.kolmogorovSmirnovTest(ksNorm, differences);
    }
  }

}
