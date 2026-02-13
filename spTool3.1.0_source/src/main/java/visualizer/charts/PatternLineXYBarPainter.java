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
import java.awt.geom.AffineTransform;
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
public class PatternLineXYBarPainter implements XYBarPainter, Serializable {

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
  public PatternLineXYBarPainter() {
    this(0.10, 0.20, 0.80);
  }

  /**
   * Creates a new instance.
   *
   * @param g1 the division between regions 1 and 2.
   * @param g2 the division between regions 2 and 3.
   * @param g3 the division between regions 3 and 4.
   */
  public PatternLineXYBarPainter(double g1, double g2, double g3) {
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
//  @Override
//  public void paintBar(Graphics2D g2, XYBarRenderer renderer, int row,
//      int column, RectangularShape bar, RectangleEdge base) {
//
//    Paint itemPaint = renderer.getItemPaint(row, column);
//    Color baseColor;
//    if (itemPaint instanceof Color) {
//      baseColor = (Color) itemPaint;
//    } else {
//      baseColor = Color.BLUE;  // fallback color
//    }
//
//    // If fully transparent, skip drawing
//    if (baseColor.getAlpha() == 0) {
//      return;
//    }
//
//    // Fixed stripe widths in pixels
//    final double whiteWidth = 2.0;
//    final double colorWidth = 10.0;
//    final double patternWidth = whiteWidth + colorWidth;
//
//    // Get bar bounds (in device space)
//    Rectangle2D bounds = bar.getBounds2D();
//    double minX = bounds.getMinX();
//    double maxX = bounds.getMaxX();
//    double minY = bounds.getMinY();
//    double maxY = bounds.getMaxY();
//
//    // Vectors for stripe geometry
//    // Direction along stripes (45 deg): (1,1)
//    // Perpendicular to stripes: (-1,1)
//    final double sqrt2 = Math.sqrt(2.0);
//
//    // Perpendicular vector (unit length)
//    double px = -1.0 / sqrt2;
//    double py = 1.0 / sqrt2;
//
//    // Pattern spacing along the perpendicular vector
//    double spacing = patternWidth * sqrt2;
//
//    // Save original clip and set clip to bar shape
//    Shape oldClip = g2.getClip();
//    g2.setClip(bar);
//
//    // Fill the bar first with base color
//    g2.setPaint(baseColor);
//    g2.fill(bar);
//
//    // We'll draw white stripes now
//
//    // Compute bounding box corners
//    // We want to cover the entire bar bounding box plus some margin to ensure full coverage
//
//    // Project bounding box corners onto the perpendicular axis to find iteration range
//    double[] projections = new double[4];
//    projections[0] = px * minX + py * minY;
//    projections[1] = px * maxX + py * minY;
//    projections[2] = px * minX + py * maxY;
//    projections[3] = px * maxX + py * maxY;
//
//    double projMin = projections[0];
//    double projMax = projections[0];
//    for (int i = 1; i < 4; i++) {
//      if (projections[i] < projMin) projMin = projections[i];
//      if (projections[i] > projMax) projMax = projections[i];
//    }
//
//    // Define a global origin offset for pattern alignment (you can tweak this to align)
//    double originOffset = 0.0;
//
//    double margin = spacing * 2;
//    double start = Math.floor((projMin - margin - originOffset) / spacing) * spacing + originOffset;
//    double end = projMax + margin;
//
//    // For each stripe, calculate its parallelogram and fill white
//    for (double pos = start; pos <= end; pos += spacing) {
//      // This pos is along the perpendicular vector where the stripe starts
//
//      // To draw stripe parallelogram, we find 4 points:
//
//      // 1 & 2: start and end along stripe direction at position 'pos' on perpendicular axis
//      // 3 & 4: offset by whiteWidth along perpendicular vector
//
//      // Calculate corner points of the stripe parallelogram
//
//      // Point A (bottom-left)
//      double ax = px * pos;
//      double ay = py * pos;
//
//      // Point B (top-left) = A + stripe length along stripe direction
//      // Stripe length needs to cover the bar diagonal fully
//      double barDiagonalLength = Math.hypot(maxX - minX, maxY - minY) * 3.0;
//
//      double bx = ax + (1.0 / sqrt2) * barDiagonalLength;
//      double by = ay + (1.0 / sqrt2) * barDiagonalLength;
//
//      // Offset vector for stripe width along perpendicular vector
//      double ox = px * whiteWidth;
//      double oy = py * whiteWidth;
//
//      // Now define the four corners of the stripe parallelogram
//      Path2D stripe = new Path2D.Double();
//      stripe.moveTo(ax, ay);
//      stripe.lineTo(bx, by);
//      stripe.lineTo(bx + ox, by + oy);
//      stripe.lineTo(ax + ox, ay + oy);
//      stripe.closePath();
//
//      // Fill the stripe with white
//      g2.setPaint(Color.WHITE);
//      g2.fill(stripe);
//    }
//
//    // Restore original clip
//    g2.setClip(oldClip);
//
//    // Draw the outline if enabled
//    if (renderer.isDrawBarOutline()) {
//      Stroke stroke = renderer.getItemOutlineStroke(row, column);
//      Paint paint = renderer.getItemOutlinePaint(row, column);
//      if (stroke != null && paint != null) {
//        g2.setStroke(stroke);
//        g2.setPaint(paint);
//        g2.draw(bar);
//      }
//    }
//  }

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

    // Define stripe pattern
    final double whiteHeight = 1.5;
    final double colorHeight = 5.0;
    final double patternHeight = whiteHeight + colorHeight;

    // Save original transform and clip
    AffineTransform oldTransform = g2.getTransform();
    Shape oldClip = g2.getClip();

    // Clip to the bar shape so stripes don't draw outside
    g2.clip(bar);

    // Rotate coordinate system by 45 degrees clockwise
    AffineTransform rotate = AffineTransform.getRotateInstance(
        Math.toRadians(15), //45
        bar.getCenterX(),
        bar.getCenterY()
    );
    g2.transform(rotate);

    // Determine bounding box for stripe drawing
    Rectangle2D bounds = rotate.createTransformedShape(bar).getBounds2D();
    double startY = bounds.getMinY();
    double endY = bounds.getMaxY();

    // Align to a global grid
    double globalOrigin = 0.0;
    double y = startY - ((startY - globalOrigin) % patternHeight);

    while (y < endY) {
      double mod = (y - globalOrigin) % patternHeight;
      boolean isBase = mod < colorHeight;
      double height = isBase ? (colorHeight - mod) : (patternHeight - mod);
      height = Math.min(height, endY - y);

      g2.setPaint(isBase ? baseColor : Color.WHITE);
      g2.fill(new Rectangle2D.Double(bounds.getMinX(), y, bounds.getWidth(), height));

      y += height;
    }

    // Restore original transform and clip
    g2.setTransform(oldTransform);
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
    if (!(obj instanceof PatternLineXYBarPainter)) {
      return false;
    }
    PatternLineXYBarPainter that = (PatternLineXYBarPainter) obj;
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

