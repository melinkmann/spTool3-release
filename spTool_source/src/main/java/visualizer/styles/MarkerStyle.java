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

package visualizer.styles;

import com.google.common.collect.Iterables;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import org.jfree.chart.util.ShapeUtils;

public enum MarkerStyle {

  CIRCLE {
    @Override
    public Shape getAwt(int size) {
      double dbSize = (double) size;
      return new Ellipse2D.Double(-dbSize / 2, -dbSize / 2, dbSize, dbSize);
    }
  },

  BAR {
    @Override
    public Shape getAwt(int size) {
      int width = (int) (2.5 * size);
      int height = Math.max(size / 3, 1);
      int x = -width / 2;
      int y = Math.max(size - 1, 1); // vertical offset from origin

      return new Rectangle(x, y, width, height);
    }
  },

  RECTANGLE {
    @Override
    public Shape getAwt(int size) {
      return new Rectangle(-size / 2, -size / 2, size, size);
    }
  },

  TRIANGLE_DOWN {
    @Override
    public Shape getAwt(int size) {
      return ShapeUtils.createDownTriangle(size / 2f);
    }
  },

  TRIANGLE_UP {
    @Override
    public Shape getAwt(int size) {
      return ShapeUtils.createUpTriangle(size / 2f);
    }
  },

  DIAMOND {
    @Override
    public Shape getAwt(int size) {
      return ShapeUtils.createDiamond(size / 2f);
    }
  },

  CROSS {
    @Override
    public Shape getAwt(int size) {
      float thickness = 1;
      if (size <= 2) {
        thickness = 0.75f;
      }
      return ShapeUtils.createDiagonalCross(size / 2f, thickness);
    }
  },

  CROSS_UPRIGHT {
    @Override
    public Shape getAwt(int size) {
      float thickness = 1;
      if (size <= 2) {
        thickness = 0.75f;
      }
      return ShapeUtils.createRegularCross(size / 2f, thickness);
    }
  };

  public abstract Shape getAwt(int size);

  public javafx.scene.shape.Shape getFx(int size) {
    return convertAWTShapeToFX(getAwt(size));
  }

  public static Iterable<MarkerStyle> getDefaultAwtIterator() {
    List<MarkerStyle> markers = new ArrayList<>(Arrays.asList(MarkerStyle.values()));
    markers.remove(BAR);
    Iterable<MarkerStyle> iterator = Iterables.cycle(markers);
    return iterator;
  }

  public static Iterable<MarkerStyle> getScatterAwtIterator() {
    List<MarkerStyle> markers = new ArrayList<>();
    markers.add(CIRCLE);
    markers.add(CROSS);
    markers.add(TRIANGLE_UP);
    markers.add(CROSS_UPRIGHT);
    markers.add(TRIANGLE_DOWN);
    markers.add(RECTANGLE);
    markers.add(DIAMOND);
    Iterable<MarkerStyle> iterator = Iterables.cycle(markers);
    return iterator;
  }


  // ChatGPT
  public static Path convertAWTShapeToFX(Shape awtShape) {
    Path fxPath = new Path();
    PathIterator iterator = awtShape.getPathIterator(null);
    double[] coords = new double[6];

    while (!iterator.isDone()) {
      int type = iterator.currentSegment(coords);

      PathElement element;
      switch (type) {
        case PathIterator.SEG_MOVETO:
          element = new MoveTo(coords[0], coords[1]);
          break;
        case PathIterator.SEG_LINETO:
          element = new LineTo(coords[0], coords[1]);
          break;
        case PathIterator.SEG_QUADTO:
          element = new QuadCurveTo(coords[0], coords[1], coords[2], coords[3]);
          break;
        case PathIterator.SEG_CUBICTO:
          element = new CubicCurveTo(coords[0], coords[1], coords[2], coords[3], coords[4],
              coords[5]);
          break;
        case PathIterator.SEG_CLOSE:
          element = new ClosePath();
          break;
        default:
          element = null;
      }
      if (element != null) {
        fxPath.getElements().add(element);
      }
      iterator.next();
    }

    return fxPath;
  }


}
