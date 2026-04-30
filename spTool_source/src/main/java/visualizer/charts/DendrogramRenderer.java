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

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import visualizer.styles.Colors;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;


// 100% Claude Sonnet 4.6
public class DendrogramRenderer extends AbstractXYItemRenderer {

  private final SpChartFactory.DendrogramChartStyle chartStyle;
  private final float strokeWidth;
  private final int[][] mergeTree;
  private final double[] mergeHeights;
  private final double[] nodeX;
  private final double[] nodeHeight;
  private final int[] nodeCluster;
  private final int nParticles;
  private final int nMerges;

  /**
   * @param mergeTree    cr.mergeTree()   — shape [nMerges][2]
   * @param mergeHeights cr.mergeHeights() — length nMerges
   * @param nodeX        x-centre of every node (leaves + internal), length nParticles + nMerges
   * @param nodeHeight   y-height of every node,                      length nParticles + nMerges
   * @param nodeCluster  cluster id per node (-1 = above threshold),  length nParticles + nMerges
   * @param nParticles   number of leaf nodes
   * @param nMerges      number of merge steps  (= nParticles - 1)
   */
  public DendrogramRenderer(SpChartFactory.DendrogramChartStyle chartStyle,
                            int[][] mergeTree,
                            double[] mergeHeights,
                            double[] nodeX,
                            double[] nodeHeight,
                            int[] nodeCluster,
                            int nParticles,
                            int nMerges,
                            boolean logScaleDendrogram) {
    this.chartStyle = chartStyle;
    this.strokeWidth = chartStyle.getBranchStrokeWidth().get();
    this.mergeTree = mergeTree;
    this.mergeHeights = logScaleDendrogram ? logTransform(mergeHeights) : mergeHeights;
    this.nodeX = nodeX;
    this.nodeHeight = logScaleDendrogram ? logTransform(nodeHeight) : nodeHeight;
    this.nodeCluster = nodeCluster;
    this.nParticles = nParticles;
    this.nMerges = nMerges;
  }


  /*
  log1p instead of log : leaf nodes have a height of 0.0, and log(0) is -Infinity. log1p(x) computes log(1
  + x), so 0 stays 0 and all other values shift up gracefully.
   */
  private static double[] logTransform(double[] values) {
    double[] result = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = Math.log1p(values[i]);
    }
    return result;
  }

  /**
   * Nothing to draw per item — the whole tree is painted in {@link #drawAnnotations}.
   */
  @Override
  public void drawItem(Graphics2D g2,
                       XYItemRendererState state,
                       Rectangle2D dataArea,
                       PlotRenderingInfo info,
                       XYPlot plot,
                       ValueAxis domainAxis,
                       ValueAxis rangeAxis,
                       XYDataset dataset,
                       int series,
                       int item,
                       CrosshairState crosshair,
                       int pass) {
    // Intentionally empty — tree is painted in drawAnnotations()
  }

  @Override
  public void drawAnnotations(Graphics2D g2,
                              Rectangle2D dataArea,
                              ValueAxis domainAxis,
                              ValueAxis rangeAxis,
                              Layer layer,
                              PlotRenderingInfo info) {

    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND));

    RectangleEdge xEdge = getPlot().getDomainAxisEdge();
    RectangleEdge yEdge = getPlot().getRangeAxisEdge();

    for (int m = 0; m < nMerges; m++) {

      int left = mergeTree[m][0];
      int right = mergeTree[m][1];
      double parentH = mergeHeights[m];

      // ── Pixel coordinates ──────────────────────────────────────────

      double pxLeft = domainAxis.valueToJava2D(nodeX[left], dataArea, xEdge);
      double pxRight = domainAxis.valueToJava2D(nodeX[right], dataArea, xEdge);
      double pyLeft = rangeAxis.valueToJava2D(nodeHeight[left], dataArea, yEdge);
      double pyRight = rangeAxis.valueToJava2D(nodeHeight[right], dataArea, yEdge);
      double pyParent = rangeAxis.valueToJava2D(parentH, dataArea, yEdge);

      // ── Segment colours ────────────────────────────────────────────

      Color aboveThrColor = chartStyle.getBranchesAboveThresholdColor();
      Color leftColor = nodeCluster[left] >= 0 ? Colors.paletteColor(nodeCluster[left]) : aboveThrColor;
      Color rightColor = nodeCluster[right] >= 0 ? Colors.paletteColor(nodeCluster[right]) : aboveThrColor;
      Color horizColor = nodeCluster[nParticles + m] >= 0 ?
          Colors.paletteColor(nodeCluster[nParticles + m]) : aboveThrColor;

      // Left vertical
      g2.setColor(leftColor);
      g2.draw(new Line2D.Double(pxLeft, pyLeft, pxLeft, pyParent));

      // Right vertical
      g2.setColor(rightColor);
      g2.draw(new Line2D.Double(pxRight, pyRight, pxRight, pyParent));

      // Horizontal connector
      g2.setColor(horizColor);
      g2.draw(new Line2D.Double(pxLeft, pyParent, pxRight, pyParent));
    }
  }
}