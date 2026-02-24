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

package prevFeatures;

import core.SpTool3Main;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;
import math.Averager;
import math.SavitzkyGolay;
import math.stat.MeasureOfLocation;
import math.stat.PreFilter;
import math.units.enums.MassUnit;
import math.units.enums.ViewUnits;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.jfree.chart.JFreeChart;
import processing.options.BinWidthEstimator;
import processing.options.HistogramNormalization;
import processing.options.MathMod;
import tasks.BatchTask;
import tasks.Task;
import tasks.batch.SimpleLinearBatch;
import tasks.batch.SimpleParallelBatch;
import tasks.single.ExampleWorkingTask;
import util.ArrUtils;
import visualizer.charts.AxisUtils;
import visualizer.charts.RendererOption;
import visualizer.charts.SpChartFactory;
import visualizer.charts.SpChartFactory.ChartComponent;
import visualizer.charts.SpChartFactory.ChartData;
import visualizer.charts.SpChartFactory.ChartStyle;
import visualizer.styles.LineGraphStyle;
import visualizer.styles.LineLineDashDefaults;
import visualizer.styles.LineWidthDefaults;
import visualizer.styles.MarkerSizeDefaults;
import visualizer.styles.MarkerStyle;
import visualizer.styles.OkabeItoColors;

public class DemoPlot {


  private final BorderPane mainViewerMain;

  public DemoPlot() {
    this.mainViewerMain = new BorderPane();

    int[] cts = new int[3000];
    int[] cts2 = new int[3000];
    double[] t = new double[3000];

    final PoissonDistribution events = new PoissonDistribution(500);
    final PoissonDistribution eventsSmall = new PoissonDistribution(75);
    final PoissonDistribution pois = new PoissonDistribution(25);
    final PoissonDistribution pois2 = new PoissonDistribution(45);
    for (int i = 0; i < cts.length; i++) {
      cts[i] = pois.sample();
      cts2[i] = pois2.sample();
      if (i > 2000 && i % 10 == 0) {
        cts[i] = events.sample();
        cts2[i] = events.sample();
      }

      if (1 < i && i < 1500) {
        cts[i] = events.sample() + i / 750;
        cts2[i] = events.sample() + i / 750;
      }

      if (i > 2000 && (i - 1) % 10 == 0) {
        cts[i] = eventsSmall.sample();
        cts2[i] = eventsSmall.sample();
      }
      t[i] = ((double) i) / 20;
    }

    List<ChartComponent> chartComponents = new ArrayList<>();

    chartComponents.add(new ChartComponent(
        new ChartData("A relatively long sample name may lead to issues with readability.",
            t,
            ArrUtils.intArrToDoubleArr(cts),
            "Some value on the X axis", MassUnit.FEMTO_GRAM, MathMod.NONE,
            "Some other value on the y", ViewUnits.NP_PER_SECOND,MathMod.NONE),
        new ChartStyle(OkabeItoColors.VERMILION, 0.1,
            LineWidthDefaults.MEDIUM,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.SMALL,
            MarkerStyle.CROSS,
            false,
            RendererOption.SAMPLING_LINE_AND_SHAPE,
            LineGraphStyle.LINE_AND_MARKER)
    ));

    chartComponents.add(new ChartComponent(
        new ChartData("Another series.",
            t,
            ArrUtils.intArrToDoubleArr(ArrUtils.add(cts2, 50)),
            "", MassUnit.FEMTO_GRAM,MathMod.NONE,
            "", ViewUnits.NP_PER_SECOND,MathMod.NONE),
        new ChartStyle(OkabeItoColors.PINK, 0.3,
            LineWidthDefaults.MEDIUM,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.SMALL,
            MarkerStyle.CROSS, false,
            RendererOption.SAMPLING_LINE_AND_SHAPE,
            LineGraphStyle.LINE_AND_MARKER)
    ));

    chartComponents.add(new ChartComponent(
        new ChartData("Another series.",
            t,
            ArrUtils.add(cts2, 100d),
            "", MassUnit.FEMTO_GRAM,MathMod.NONE,
            "", ViewUnits.NP_PER_SECOND,MathMod.NONE),
        new ChartStyle(OkabeItoColors.GREEN_BLUE, 0.3,
            LineWidthDefaults.MEDIUM,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.SMALL,
            MarkerStyle.CROSS, false,
            RendererOption.SAMPLING_LINE_AND_SHAPE,
            LineGraphStyle.LINE_AND_MARKER)
    ));

    chartComponents.add(new ChartComponent(
        new ChartData("Pre Smooth.",
            t,
            ArrUtils.add(cts2, 15d),
            "", MassUnit.FEMTO_GRAM,MathMod.NONE,
            "", ViewUnits.NP_PER_SECOND,MathMod.NONE),
        new ChartStyle(OkabeItoColors.ORANGE, 0.75,
            LineWidthDefaults.THIN,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.MEDIUM,
            MarkerStyle.CROSS, false,
            RendererOption.LINE_AND_SHAPE,
            LineGraphStyle.LINE)
    ));

    Averager averager = new Averager(0.5, t, ArrUtils.add(cts2, 15d),
        MeasureOfLocation.MEAN, PreFilter.MAD);

    chartComponents.add(new ChartComponent(
        new ChartData("Smooth.",
            averager.getCenterTime(),
            SavitzkyGolay.smoothSG(averager.getDataCTS(), 4, 3),
            "", MassUnit.FEMTO_GRAM,MathMod.NONE,
            "", ViewUnits.NP_PER_SECOND,MathMod.NONE),
        new ChartStyle(OkabeItoColors.BLACK, 0.95,
            LineWidthDefaults.THICKER,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.SMALL,
            MarkerStyle.CROSS, false,
            RendererOption.LINE_AND_SHAPE,
            LineGraphStyle.LINE)
    ));

    averager = new Averager(2, t, ArrUtils.add(cts2, 15d), MeasureOfLocation.MEAN,
        PreFilter.MAD);

    ChartComponent chartComponent = new ChartComponent(
        new ChartData("AV SM.",
            averager.getCenterTime(),
            SavitzkyGolay.smoothSG(averager.getDataCTS(), 4, 3),
            "USA", MassUnit.FEMTO_GRAM,MathMod.NONE,
            "Depending Stuff", ViewUnits.NP_PER_SECOND,MathMod.NONE),
        new ChartStyle(OkabeItoColors.BLACK, 1,
            LineWidthDefaults.MEDIUM,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.SMALL,
            MarkerStyle.CROSS, false,
            RendererOption.SAMPLING_LINE_AND_SHAPE,
            LineGraphStyle.LINE)
    );

    JFreeChart chart = SpChartFactory.createLineChart(chartComponents);
    JFreeChart chartBelow = SpChartFactory.createLineChart(ArrUtils.wrap(chartComponent));

    AxisUtils.constantYaxis(chartBelow.getXYPlot().getRangeAxis(),
        chartBelow.getXYPlot().getRangeAxis());

    Pair<JFreeChart,Double> chart2 = SpChartFactory.createHistogram(chartComponents,
        HistogramNormalization.FREQUENCY,
        BinWidthEstimator.CUSTOM, 2);

//    graphsBorderPane.setCenter(SpChartFactory.bundleChartLegendButtons(chart,
//        SpChartFactory.getHistogramControls(),
//        chartComponents,
//        700, 500));

    /// AxisUtils.linkXAxisWithWindow(chartBelow, chartComponents, mainViewerMain);

//    graphsBorderPane.setBottom(SpChartFactory.bundleChartLegendButtons(chartBelow,
//        SpChartFactory.getHistogramControls(),
//        ArrUtils.wrap(chartComponent), 700, 200));

  }

  public void demoTask() {
    List<Task> parTas = new ArrayList<>();
    for (int k = 0; k < 5; k++) {
      List<Task> tasks = new ArrayList<>();
      for (int j = 0; j < 6; j++) {
        tasks.add(new ExampleWorkingTask("TestTask " + j));
      }
      BatchTask batchTask = new SimpleParallelBatch("Parallel Queue ++++++ " + k, tasks,
          false, () -> {
      });
      parTas.add(batchTask);
    }

    BatchTask linear = new SimpleLinearBatch<>("LINEAR QUEUE ###### ", parTas, false,
        () -> {
        });
    SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(linear);
  }

}
