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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.io.Serializable;
import org.jfree.chart.HashUtils;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleEdge;

/**
 * An implementation of the {@link XYBarPainter} interface that uses several gradient fills to
 * enrich the appearance of the bars.
 */
public class PatternDotXYBarPainter implements XYBarPainter, Serializable {

  /**
   * The division point between the first and second gradient regions.
   */
  private double g1;

  /**
   * The division point between the second and third gradient regions.
   */
  private double g2;

  /**
   * The division point between the third and fourth gradient regions.
   */
  private double g3;

  /**
   * Creates a new instance.
   */
  public PatternDotXYBarPainter() {
    this(0.10, 0.20, 0.80);
  }

  /**
   * Creates a new instance.
   *
   * @param g1 the division between regions 1 and 2.
   * @param g2 the division between regions 2 and 3.
   * @param g3 the division between regions 3 and 4.
   */
  public PatternDotXYBarPainter(double g1, double g2, double g3) {
    this.g1 = g1;
    this.g2 = g2;
    this.g3 = g3;
  }

  /**
   * Paints a single bar instance.
   *
   * @param g2       the graphics target.
   * @param renderer the renderer.
   * @param row      the row index.
   * @param column   the column index.
   * @param bar      the bar
   * @param base     indicates which side of the rectangle is the base of the bar.
   */


  @Override
  public void paintBar(Graphics2D g2, XYBarRenderer renderer, int row,
      int column, RectangularShape bar, RectangleEdge base) {

    Paint itemPaint = renderer.getItemPaint(row, column);
    Color baseColor;
    if (itemPaint instanceof Color) {
      baseColor = (Color) itemPaint;
    } else {
      baseColor = Color.BLUE;  // fallback color
    }

    // If fully transparent, skip drawing
    if (baseColor.getAlpha() == 0) {
      return;
    }

    // Calculate dot diameter as 10% of bar width
    double barWidth = bar.getWidth();
    double dotDiameter = Math.max(barWidth * 0.01, 2);

    // Save original clip
    Shape oldClip = g2.getClip();

    // Fill bar background with original base color
    g2.setPaint(baseColor);
    g2.fill(bar);

    // Clip to the bar shape so dots don't draw outside
    g2.clip(bar);

    // Get bar bounds for dot grid extents
    Rectangle2D bounds = bar.getBounds2D();

    // Dot grid spacing: equal to dot diameter for tight packing
    double spacing = dotDiameter * 1.5;

    // Draw white dots only
    g2.setPaint(Color.white);

    // Loop over the grid within the bounds and draw dots
    for (double x = bounds.getMinX(); x <= bounds.getMaxX(); x += spacing) {
      for (double y = bounds.getMinY(); y <= bounds.getMaxY(); y += spacing) {
        Ellipse2D.Double dot = new Ellipse2D.Double(
            x - dotDiameter / 2,
            y - (dotDiameter / 4),            // half of half = quarter
            dotDiameter,
            dotDiameter / 2                  // vertical diameter is half
        );
        g2.fill(dot);
      }
    }

    // Restore original clip
    g2.setClip(oldClip);

    // Draw the outline if enabled
    if (renderer.isDrawBarOutline()) {
      Stroke stroke = renderer.getItemOutlineStroke(row, column);
      Paint paint = renderer.getItemOutlinePaint(row, column);
      if (stroke != null && paint != null) {
        g2.setStroke(stroke);
        g2.setPaint(paint);
        g2.draw(bar);
      }
    }
  }


  /**
   * Paints a single bar instance.
   *
   * @param g2        the graphics target.
   * @param renderer  the renderer.
   * @param row       the row index.
   * @param column    the column index.
   * @param bar       the bar
   * @param base      indicates which side of the rectangle is the base of the bar.
   * @param pegShadow peg the shadow to the base of the bar?
   */
  @Override
  public void paintBarShadow(Graphics2D g2, XYBarRenderer renderer, int row,
      int column, RectangularShape bar, RectangleEdge base,
      boolean pegShadow) {

    // handle a special case - if the bar colour has alpha == 0, it is
    // invisible so we shouldn't draw any shadow
    Paint itemPaint = renderer.getItemPaint(row, column);
    if (itemPaint instanceof Color) {
      Color c = (Color) itemPaint;
      if (c.getAlpha() == 0) {
        return;
      }
    }

    RectangularShape shadow = createShadow(bar, renderer.getShadowXOffset(),
        renderer.getShadowYOffset(), base, pegShadow);
    g2.setPaint(Color.GRAY);
    g2.fill(shadow);

  }

  /**
   * Creates a shadow for the bar.
   *
   * @param bar       the bar shape.
   * @param xOffset   the x-offset for the shadow.
   * @param yOffset   the y-offset for the shadow.
   * @param base      the edge that is the base of the bar.
   * @param pegShadow peg the shadow to the base?
   * @return A rectangle for the shadow.
   */
  private Rectangle2D createShadow(RectangularShape bar, double xOffset,
      double yOffset, RectangleEdge base, boolean pegShadow) {
    double x0 = bar.getMinX();
    double x1 = bar.getMaxX();
    double y0 = bar.getMinY();
    double y1 = bar.getMaxY();
    if (base == RectangleEdge.TOP) {
      x0 += xOffset;
      x1 += xOffset;
      if (!pegShadow) {
        y0 += yOffset;
      }
      y1 += yOffset;
    } else if (base == RectangleEdge.BOTTOM) {
      x0 += xOffset;
      x1 += xOffset;
      y0 += yOffset;
      if (!pegShadow) {
        y1 += yOffset;
      }
    } else if (base == RectangleEdge.LEFT) {
      if (!pegShadow) {
        x0 += xOffset;
      }
      x1 += xOffset;
      y0 += yOffset;
      y1 += yOffset;
    } else if (base == RectangleEdge.RIGHT) {
      x0 += xOffset;
      if (!pegShadow) {
        x1 += xOffset;
      }
      y0 += yOffset;
      y1 += yOffset;
    }
    return new Rectangle2D.Double(x0, y0, (x1 - x0), (y1 - y0));
  }

  /**
   * Splits a bar into subregions (elsewhere, these subregions will have different gradients applied
   * to them).
   *
   * @param bar the bar shape.
   * @param a   the first division.
   * @param b   the second division.
   * @param c   the third division.
   * @return An array containing four subregions.
   */
  private Rectangle2D[] splitVerticalBar(RectangularShape bar, double a,
      double b, double c) {
    Rectangle2D[] result = new Rectangle2D[4];
    double x0 = bar.getMinX();
    double x1 = Math.rint(x0 + (bar.getWidth() * a));
    double x2 = Math.rint(x0 + (bar.getWidth() * b));
    double x3 = Math.rint(x0 + (bar.getWidth() * c));
    result[0] = new Rectangle2D.Double(bar.getMinX(), bar.getMinY(),
        x1 - x0, bar.getHeight());
    result[1] = new Rectangle2D.Double(x1, bar.getMinY(), x2 - x1,
        bar.getHeight());
    result[2] = new Rectangle2D.Double(x2, bar.getMinY(), x3 - x2,
        bar.getHeight());
    result[3] = new Rectangle2D.Double(x3, bar.getMinY(),
        bar.getMaxX() - x3, bar.getHeight());
    return result;
  }

  /**
   * Splits a bar into subregions (elsewhere, these subregions will have different gradients applied
   * to them).
   *
   * @param bar the bar shape.
   * @param a   the first division.
   * @param b   the second division.
   * @param c   the third division.
   * @return An array containing four subregions.
   */
  private Rectangle2D[] splitHorizontalBar(RectangularShape bar, double a,
      double b, double c) {
    Rectangle2D[] result = new Rectangle2D[4];
    double y0 = bar.getMinY();
    double y1 = Math.rint(y0 + (bar.getHeight() * a));
    double y2 = Math.rint(y0 + (bar.getHeight() * b));
    double y3 = Math.rint(y0 + (bar.getHeight() * c));
    result[0] = new Rectangle2D.Double(bar.getMinX(), bar.getMinY(),
        bar.getWidth(), y1 - y0);
    result[1] = new Rectangle2D.Double(bar.getMinX(), y1, bar.getWidth(),
        y2 - y1);
    result[2] = new Rectangle2D.Double(bar.getMinX(), y2, bar.getWidth(),
        y3 - y2);
    result[3] = new Rectangle2D.Double(bar.getMinX(), y3, bar.getWidth(),
        bar.getMaxY() - y3);
    return result;
  }

  /**
   * Tests this instance for equality with an arbitrary object.
   *
   * @param obj the obj ({@code null} permitted).
   * @return A boolean.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof PatternDotXYBarPainter)) {
      return false;
    }
    PatternDotXYBarPainter that = (PatternDotXYBarPainter) obj;
    if (this.g1 != that.g1) {
      return false;
    }
    if (this.g2 != that.g2) {
      return false;
    }
    if (this.g3 != that.g3) {
      return false;
    }
    return true;
  }

  /**
   * Returns a hash code for this instance.
   *
   * @return A hash code.
   */
  @Override
  public int hashCode() {
    int hash = 37;
    hash = HashUtils.hashCode(hash, this.g1);
    hash = HashUtils.hashCode(hash, this.g2);
    hash = HashUtils.hashCode(hash, this.g3);
    return hash;
  }

}

