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

package gui;

import core.SpTool3Main;
import dataModelNew.mz.Element;
import javafx.util.Pair;
import math.HAC;
import math.HAC.ClusterResult;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import sandbox.montecarlo.Isotope;
import visualizer.charts.SpChartFactory;
import visualizer.styles.Colors;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

import static visualizer.styles.Colors.OTHER_PIE_SLICE;
import static visualizer.styles.Colors.paletteColor;

public class HACCharts {

  // ============================================================================
  // 2. Knee plot
  // ============================================================================

  /**
   * Builds a knee plot data set: Ward merge distances (descending) vs. merge step index.
   *
   * <p>The top {@code nSteps} merges are shown (default 50). A dashed vertical
   * red line marks {@code k}, the number of clusters produced by the current
   * threshold.
   *
   * @param cr       cluster result from {@link HAC#cluster}
   * @param maxSteps number of merge steps to display (suggested: 50)
   * @return chart xy data
   */

  public static Pair<double[], double[]> getKneePlotXY(ClusterResult cr, int maxSteps) {
    Pair<double[], double[]> result = new Pair<>(new double[0], new double[0]);

    double[] wardDistances = cr.mergeHeights();
    int nMerges = wardDistances.length;

    if (nMerges > 0) {

      // Clamp to available merges so indices into wardDistances stay in bounds
      int nPts = Math.min(maxSteps, nMerges);

      double[] xData = new double[nPts];
      double[] yData = new double[nPts];

      // Heights are ascending in SMILE; iterate from the end to present the most
      // significant splits (highest distances) first, left-to-right on the x-axis
      for (int i = 0; i < nPts; i++) {
        int srcIdx = nMerges - 1 - i;
        xData[i] = i + 1;
        yData[i] = wardDistances[srcIdx];
      }

      result = new Pair<>(xData, yData);
    }
    return result;
  }


  // ============================================================================
  // 3. Pie charts
  // ============================================================================

  /**
   * Builds one pie chart per cluster (only clusters with
   * {@code size >= minDisplay} are included).
   *
   * <p>Each slice represents one <em>element</em>. Isotopes belonging to the
   * same element are summed together. The slice value is the mean <em>raw
   * intensity</em> of that element across all member particles, then
   * re-normalised so all slices sum to 100 %. Slices below 2 % are merged
   * into an "Other" slice.
   *
   * <p>Data orientation of {@code raw} must match {@link HAC#preprocess}
   * input: {@code raw.get(iso)[particle]}.
   *
   * @param raw            raw (unprocessed) spectral data —
   *                       same {@code List<double[]>} orientation as the input
   *                       to {@link HAC#preprocess}. Size: nIsotopes × nParticles.
   * @param cr             cluster result from {@link HAC#cluster}
   * @param elementNames   element symbol for each isotope channel, e.g. "Si"
   *                       for both "28Si" and "29Si". Length must equal
   *                       {@code raw.size()}.
   * @param minClusterSize minimum cluster size to include in output
   * @return one {@link JFreeChart} per shown cluster, in ascending cluster-id order
   */

  public static List<SpChartFactory.PieChartData> getPieChartData(ClusterResult cr,
                                                                  String[] elementNames,
                                                                  List<double[]> raw,
                                                                  int minClusterSize,
                                                                  double minFractionPct) {

    List<SpChartFactory.PieChartData> result = new ArrayList<>();

    int nIsotopes = raw.size();

    if (nIsotopes > 0) {
      // raw is laid out as [element/isotope][particle], i.e. raw.get(iso) gives you
      // a double[] of length nParticles — one intensity value per particle for that isotope
      int nParticles = raw.get(0).length;

      // Build a map of unique element names → index.
      // Multiple isotopes can belong to the same element (e.g. "28Si" and "29Si" both → "Si").
      // LinkedHashMap preserves insertion order, which matters for consistent slice ordering.
      LinkedHashMap<String, Integer> elementIndex = new LinkedHashMap<>();
      for (String name : elementNames) {
        if (!elementIndex.containsKey(name))
          elementIndex.put(name, elementIndex.size());
      }

      int nElements = elementIndex.size();

      // meanRaw[cluster][element] — will hold the mean raw intensity of each element
      // across all particles that belong to that cluster
      double[][] meanRaw = new double[cr.k()][nElements];

      // counts[cluster] — how many particles were assigned to each cluster
      // cr.labels() is an int[] of length nParticles where labels()[p] = cluster id of particle p
      int[] counts = new int[cr.k()];

      for (int p = 0; p < nParticles; p++)
        counts[cr.labels()[p]]++;

      // Accumulate raw intensities per cluster and element.
      // For each isotope channel, find which element it belongs to,
      // then add its intensity for each particle into the correct cluster bucket.
      for (int iso = 0; iso < nIsotopes; iso++) {
        double[] channel = raw.get(iso);
        int eIdx = elementIndex.get(elementNames[iso]);
        for (int p = 0; p < nParticles; p++) {
          int c = cr.labels()[p];
          meanRaw[c][eIdx] += channel[p];
        }
      }

      // Divide accumulated intensities by particle count to get the mean per cluster
      for (int c = 0; c < cr.k(); c++) {
        if (counts[c] == 0) continue;
        for (int e = 0; e < nElements; e++)
          meanRaw[c][e] /= counts[c];
      }

      String[] elementList = elementIndex.keySet().toArray(new String[0]);

      // Build one PieChartData per cluster.
      // cr.k() = total number of clusters found by HAC.
      // cr.sizes()[c] = number of particles in cluster c.
      for (int c = 0; c < cr.k(); c++) {

        // Skip clusters that are too small to be worth displaying
        if (cr.sizes()[c] < minClusterSize)
          continue;

        // Sum all element means for this cluster — used to normalise slices to fractions
        double total = 0.0;
        for (int e = 0; e < nElements; e++)
          total += meanRaw[c][e];

        List<String> keys = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        double other = 0.0;

        // Convert each element's mean intensity to a fraction of the total.
        // Slices below 0.2% are too small to see — merge them into a single "Other" slice.
        for (int e = 0; e < nElements; e++) {
          double fraction = (total > 0.0) ? meanRaw[c][e] / total : 0.0;
          if (fraction < minFractionPct / 100) {
            other += fraction;
          } else {
            keys.add(elementList[e]);
            values.add(fraction);
          }
        }

        if (other > 0.0) {
          keys.add("Other");
          values.add(other);
        }

        // seriesName doubles as the chart title — "Cluster 1  (n = 42)" etc.
        String title = cr.clusterNames()[c] + "  (n = " + cr.sizes()[c] + ")";
        result.add(new SpChartFactory.PieChartData(
            title,
            keys.toArray(new String[0]),
            values.stream().mapToDouble(Double::doubleValue).toArray()
        ));
      }
    }

    return result;
  }

  public static Map<String, Colors> getPieChartColors(List<Element> elements) {
    Map<String, Colors> colorMap = new LinkedHashMap<>();
    Random random = new Random();

    for (Element element : elements) {
      // Copy and sort descending by abundance to get the most abundant isotope first
      List<Isotope> isotopes = new ArrayList<>(element.getIsotopes());
      isotopes.sort((a, b) -> Double.compare(b.getAbundance(), a.getAbundance()));

      if (isotopes.isEmpty()) continue;

      // Use the most abundant isotope's color as the element color
      Colors color = SpTool3Main.getRunTime().getConfParams().getColor(isotopes.get(0));

      // Check for color collision with already assigned element colors
      boolean isDuplicate = colorMap.values().stream()
          .anyMatch(c -> color.equals(new Colors.SpColor(c.get())));

      if (!isDuplicate) {
        colorMap.put(element.getSymbol(), color);
      } else {
        // Collision: assign a random color instead
        colorMap.put(element.getSymbol(), new Colors.SpColor(
            new java.awt.Color(random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256))
        ));
      }
    }
    return colorMap;
  }


}