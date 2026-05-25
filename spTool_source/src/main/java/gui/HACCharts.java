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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import util.NF;
import util.SnF;
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

  private static final Logger LOGGER = LogManager.getLogger(HACCharts.class);

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
   * @param cr       cluster result from
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
   * Builds one PieChartData per cluster, showing the mean elemental composition
   * of the particles in that cluster as pie slices.
   *
   * Data flow:
   *   - raw is element-major (raw.get(e)[p] = intensity of element e in particle p),
   *     already collapsed from isotopes to elements by SpectralRegionElement upstream.
   *     No further isotope merging is done here.
   *   - For each cluster, the mean intensity per element is computed across all
   *     member particles, then normalised to fractions summing to 1.0.
   *   - Elements whose fraction is below minFractionPct are grouped into a single
   *     "Other" slice. Other is always shown if non-zero, even if its combined
   *     fraction is below the threshold — hiding it would make the pie not sum to 100%.
   *   - Negative means (from background subtraction) are clamped to zero before
   *     normalisation. Large negatives (> 5% of the cluster's max element mean)
   *     are logged as warnings.
   *
   * @param cr              cluster result from HAC — provides labels, cluster count,
   *                        sizes, and display names (e.g. "C1" or "C1+3" after cosine merging).
   * @param elementNames    element symbols in the same order as raw
   *                        (e.g. ["Si", "Pb", "Zn"]) — one entry per element, no duplicates.
   * @param raw             element-major intensity data: raw.get(e) is a double[] of length
   *                        nParticles with the summed intensity of element e for each particle.
   * @param minClusterSize  clusters smaller than this are skipped entirely.
   * @param minFractionPct  elements whose mean fraction is below this percentage
   *                        (e.g. 2.0 for 2%) are collapsed into the "Other" slice.
   * @return one PieChartData per qualifying cluster, in cluster order.
   */

  public static List<SpChartFactory.PieChartData> getPieChartData(ClusterResult cr,
                                                                  String[] elementNames,
                                                                  List<double[]> raw,
                                                                  int minClusterSize,
                                                                  double minFractionPct) {

    List<SpChartFactory.PieChartData> result = new ArrayList<>();

    // raw is laid out as [element][particle] — already collapsed from isotopes to elements
    // by SpectralRegionElement upstream. raw.get(e) = double[] of length nParticles,
    // one summed intensity per particle for that element.
    int nElements = raw.size();
    if (nElements == 0) {
      return result;
    }

    int nParticles = raw.get(0).length;

    // ── 1. Accumulate raw intensities per cluster and element ─────────────
    // meanRaw[cluster][element] — starts as a sum, divided by counts below.
    // cr.k() = total number of clusters produced by HAC (after any cosine merging).
    double[][] meanRaw = new double[cr.k()][nElements];

    // counts[c] = number of particles assigned to cluster c.
    // cr.labels() = int[] of length nParticles where labels()[p] = cluster index of particle p.
    int[] counts = new int[cr.k()];

    for (int p = 0; p < nParticles; p++) {
      int clusterIndex = cr.labels()[p];
      counts[clusterIndex] = counts[clusterIndex] + 1;
    }

    for (int e = 0; e < nElements; e++) {
      double[] channel = raw.get(e);
      for (int p = 0; p < nParticles; p++) {
        int clusterIndex = cr.labels()[p];
        meanRaw[clusterIndex][e] = meanRaw[clusterIndex][e] + channel[p];
      }
    }

    // ── 2. Divide to get means, then clamp negatives ──────────────────────
    // Negative means can occur because data are background-subtracted:
    //   - Small negatives (< 5% of the max element mean for this cluster) are noise
    //     around zero → clamp silently to 0.
    //   - Large negatives suggest the background subtraction overshot badly → log a warning.
    for (int c = 0; c < cr.k(); c++) {
      if (counts[c] == 0) {
        continue;
      }

      // First pass: divide accumulated sum by particle count to get the mean
      for (int e = 0; e < nElements; e++) {
        meanRaw[c][e] = meanRaw[c][e] / counts[c];
      }

      // Second pass: find the maximum mean for this cluster — used as reference
      // for deciding whether a negative is noise or a real problem
      double maxMean = 0.0;
      for (int e = 0; e < nElements; e++) {
        if (meanRaw[c][e] > maxMean) {
          maxMean = meanRaw[c][e];
        }
      }

      // Third pass: clamp negatives to zero
      for (int e = 0; e < nElements; e++) {
        if (meanRaw[c][e] < 0.0) {
          boolean isLargeNegative = Math.abs(meanRaw[c][e]) > 0.25 * maxMean;
          if (isLargeNegative) {
            LOGGER.warn(
                "Cluster {} '{}': element '{}' has a strongly negative mean ({}) "
                    + "— background subtraction may have overshot. Clamping to 0.",
                c, cr.clusterNames()[c], elementNames[e], meanRaw[c][e]);
          }
          meanRaw[c][e] = 0.0;
        }
      }
    }

    // ── 3. Build one PieChartData per cluster ─────────────────────────────
    for (int c = 0; c < cr.k(); c++) {

      // cr.sizes()[c] = number of particles in cluster c (from the ClusterResult).
      // Skip clusters that are too small to be worth displaying.
      if (cr.sizes()[c] < minClusterSize) {
        LOGGER.debug("Skipping cluster {} '{}':  size {} below minimum {}.",
            c, cr.clusterNames()[c], cr.sizes()[c], minClusterSize);
        continue;
      }

      // Sum all element means to get the total signal for this cluster.
      // Used to normalise each element mean into a fraction of the whole.
      double total = 0.0;
      for (int e = 0; e < nElements; e++) {
        total = total + meanRaw[c][e];
      }

      // If total is zero after clamping, all elements had zero or negative signal.
      // Nothing meaningful to show — skip this cluster.
      if (total <= 0.0) {
        LOGGER.warn("Cluster {} '{}' has zero total intensity after clamping — skipping pie chart.",
            c, cr.clusterNames()[c]);
        continue;
      }

      // Convert each element mean to a fraction of the total.
      // Elements below minFractionPct are too small to show as individual slices
      // and are collected into a single "Other" bucket instead.
      double effectiveThreshold = minFractionPct / 100.0;
      List<String> keys = new ArrayList<>();
      List<Double> values = new ArrayList<>();
      double keptSum = 0.0;

      for (int e = 0; e < nElements; e++) {
        double fraction = meanRaw[c][e] / total;
        if (fraction >= effectiveThreshold) {
          keys.add(elementNames[e]);
          values.add(fraction);
          keptSum = keptSum + fraction;
        }
      }

      // Compute Other as the remainder rather than accumulating tiny fractions —
      // individual fractions below threshold may round to 0.0 before accumulation,
      // but 1.0 - keptSum is always reliable.
      // Always show Other if anything was bucketed — even if the combined fraction
      // is below minFractionPct, hiding it would make the pie not sum to 100%.
      double other = 1.0 - keptSum;
      if (other > 1e-9) {
        keys.add("Other");
        values.add(other);
      }

      // cr.clusterNames()[c] = display name assigned during HAC cut (and cosine merging),
      //                        e.g. "C1" or "C1+3" if cosine-merged.
      // cr.sizes()[c]        = particle count for this cluster.
      String title = cr.clusterNames()[c]
          + "  (n=" + cr.sizes()[c]
          + " µ="+ SnF.doubleToString(total, NF.D1C1) +")";

      result.add(new SpChartFactory.PieChartData(
          title,
          keys.toArray(new String[0]),
          values.stream().mapToDouble(Double::doubleValue).toArray()
      ));
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