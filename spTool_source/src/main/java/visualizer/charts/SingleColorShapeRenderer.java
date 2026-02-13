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


import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import visualizer.charts.SpChartFactory.ChartStyle;

import java.awt.*;

/**
 * Hard override the DrawingSupplier and the issues with it. Create a Renderer that only has ONE
 * paint, stroke and shape. Rest of the settings should be as in the XYLineAndShapeRenderer default
 * constructor.
 */

public class SingleColorShapeRenderer extends XYShapeRenderer {

  private final Paint paint;
  private final BasicStroke stroke;
  private final Shape shape;

  public SingleColorShapeRenderer(Paint paint, BasicStroke stroke, Shape shape) {
    this.paint = paint;
    this.stroke = stroke;
    this.shape = shape;

    // Likely performance improvement from these
    setDrawOutlines(false);
    setUseOutlinePaint(false);
  }


  public SingleColorShapeRenderer(ChartStyle style) {
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
}
