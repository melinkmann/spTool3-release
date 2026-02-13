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

import java.awt.Paint;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import visualizer.charts.SpChartFactory.ChartStyle;

public class SingleColorBarRenderer extends XYBarRenderer {


  private final Paint paint;

  public SingleColorBarRenderer(ChartStyle style) {
    this.paint = style.getPaint();
  }

  @Override
  public Paint lookupSeriesPaint(int series) {
    return paint;
  }

}
