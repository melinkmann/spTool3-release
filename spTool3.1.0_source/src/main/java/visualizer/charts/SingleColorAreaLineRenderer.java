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

import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import visualizer.charts.SpChartFactory.ChartStyle;

public class SingleColorAreaLineRenderer extends XYAreaRenderer {

  private final Paint paint;
  private final BasicStroke stroke;
  private final Shape shape;

  public SingleColorAreaLineRenderer(Paint paint, BasicStroke stroke,
      Shape shape) {
    super(AreaPlotType.AREA.get());
    this.paint = paint;
    this.stroke = stroke;
    this.shape = shape;
  }

  public SingleColorAreaLineRenderer(ChartStyle style) {
    this(style.getPaint(), style.getStroke(), style.getAwtMarker());
  }

  @Override
  public Stroke lookupSeriesStroke(int series) {
    return stroke;
  }

  @Override
  public Paint lookupSeriesPaint(int series) {
    return paint;
  }

  @Override
  public Shape lookupSeriesShape(int series) {
    return shape;
  }

  @Override
  public Paint lookupSeriesFillPaint(int series) {
    return paint;
  }

  public static enum AreaPlotType {

    SHAPES {
      @Override
      public int get() {
        return 1;
      }
    },

    LINES {
      @Override
      public int get() {
        return 2;
      }
    },

    SHAPES_AND_LINES {
      @Override
      public int get() {
        return 3;
      }
    },

    AREA {
      @Override
      public int get() {
        return 4;
      }
    },

    AREA_AND_SHAPES {
      @Override
      public int get() {
        return 5;
      }
    };

    public abstract int get();
  }

}