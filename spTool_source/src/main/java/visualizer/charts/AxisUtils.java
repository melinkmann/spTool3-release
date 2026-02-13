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

import com.google.common.util.concurrent.AtomicDouble;
import gui.util.UiUtil;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.data.Range;
import processing.options.HistogramNormalization;
import util.ArrUtils;
import visualizer.charts.SpChartFactory.ChartComponent;

public abstract class AxisUtils {

  private static final double MAX_WINDOW_SECONDS = 80;


  public static void linkAxes(ValueAxis... valueAxes) {
    for (ValueAxis firingAxis : valueAxes) {
      firingAxis.addChangeListener(new AxisChangeListener() {
        @Override
        public void axisChanged(AxisChangeEvent event) {

          for (ValueAxis affectedAxis : valueAxes) {
            if (affectedAxis != firingAxis) {
              Range firingRange = ((ValueAxis) event.getAxis()).getRange();
              refreshAxis(affectedAxis, firingRange);
            }
          }
        }
      });
    }
  }

  public static void linkAxes(List<ValueAxis> xAxes ) {
    for (ValueAxis firingAxis : xAxes) {
      firingAxis.addChangeListener(new AxisChangeListener() {
        @Override
        public void axisChanged(AxisChangeEvent event) {

          for (ValueAxis affectedAxis : xAxes) {
            if (affectedAxis != firingAxis) {
              Range firingRange = ((ValueAxis) event.getAxis()).getRange();
              refreshAxis(affectedAxis, firingRange);
            }
          }
        }
      });
    }
  }

  public static void constantYaxis(ValueAxis firingAxis, ValueAxis constantAxis) {
    Range initialRange = new Range(0, 1.1 * constantAxis.getRange().getUpperBound());

    firingAxis.addChangeListener(new AxisChangeListener() {
      @Override
      public void axisChanged(AxisChangeEvent event) {
        refreshAxis(constantAxis, initialRange);
      }
    });
  }

  private static void refreshAxis(ValueAxis axis, Range range) {
    double lowerBoundBound = range.getLowerBound();
    double upperBound = range.getUpperBound();
    if (axis.getRange().getLowerBound() != lowerBoundBound ||
        axis.getRange().getUpperBound() != upperBound) {
      if (Double.isFinite(upperBound) && Double.isFinite(lowerBoundBound)
          && upperBound != lowerBoundBound) {
        axis.setRange(range);
      }
    }
  }


}
