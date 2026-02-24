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

package math.regression;

import math.stat.MeasureOfLocation;
import math.units.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.LinRegType;
import processing.options.MathMod;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;
import util.NF;
import util.SnF;
import visualizer.charts.RendererOption;
import visualizer.charts.SpChartFactory;
import visualizer.charts.SpChartFactory.ChartComponent;
import visualizer.styles.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class RegressionUtils {

  private static final Logger LOGGER = LogManager.getLogger(RegressionUtils.class);

  public static class LSResult {

    public final double[] x;
    public final double[] y;
    public final double slope;
    public final double intercept;
    public final double rSquare;

    public LSResult(double[] x, double[] y, double slope, double intercept, double rSquare) {
      this.x = x;
      this.y = y;
      this.slope = slope;
      this.intercept = intercept;
      this.rSquare = rSquare;
    }
  }

  public static LSResult getOLS(double[] x, double[] y, double ratio) {
    OLS ols = new OLS(true);
    if (x.length > 0 && y.length > 0) {
      ols.addObservations(x, y, null);
      double[] xValuesReg = ArrUtils.extendForRegression(x, ratio);
      double[] yValuesReg = new double[xValuesReg.length];
      for (int i = 0; i < xValuesReg.length; i++) {
        yValuesReg[i] = ols.predictY(xValuesReg[i]);
      }
      return new LSResult(xValuesReg, yValuesReg, ols.getSlope(), ols.getIntercept(),
          ols.getRSquare());
    } else {
      return new LSResult(new double[]{0}, new double[]{0}, 0, 0, 0);
    }
  }


  public static LSResult getWLS(double[] x, double[] y, double[] w, double ratio) {
    WLS wls = new WLS(true);
    if (x.length > 1 && y.length > 1 && w.length > 1) {
      wls.addObservations(x, y, w);
      double[] xValuesReg = ArrUtils.extendForRegression(x, ratio);
      double[] yValuesReg = new double[xValuesReg.length];
      for (int i = 0; i < xValuesReg.length; i++) {
        yValuesReg[i] = wls.predictY(xValuesReg[i]);
      }
      return new LSResult(xValuesReg, yValuesReg, wls.getSlope(), wls.getIntercept(),
          wls.getRSquare());
    } else {
      return getOLS(x, y, ratio);
    }
  }

  public static class ReducedData {
    public final double[] x;
    public final double[] y;
    public final double[] w;

    public ReducedData(double[] x, double[] y, double[] w) {
      this.x = x;
      this.y = y;
      this.w = w;
    }
  }


  public static void addPlotsToListAndRegression(
      List<ChartComponent> graphComponents,
      List<ChartComponent> legendComponents,
      String isotopeLbl,
      double[] x,
      double[] y,
      String xLbl, Unit xUnit,
      String yLbl, Unit yUnit,
      Colors color,
      MarkerStyle markerStyle,
      Isotope isotope,
      Map<Isotope, Double> slopes,
      LinRegType linRegType) {

    // Make sure there is data!

    SpChartFactory.ChartComponent graphComponent = new SpChartFactory.ChartComponent(
        new SpChartFactory.ChartData(
            isotopeLbl,
            isotopeLbl,
            x,
            y,
            xLbl, xUnit, MathMod.NONE,
            yLbl, yUnit, MathMod.NONE),
        new SpChartFactory.ChartStyle(color, 1,
            LineWidthDefaults.MEDIUM_THICK,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.LARGE,
            markerStyle,
            false,
            RendererOption.LINE_AND_SHAPE,
            LineGraphStyle.MARKER)
    );
    graphComponents.add(graphComponent);

    ///

    ReducedData reduced = RegressionUtils.collapseDuplicateX(x, y, null);

    LSResult linReg;
    if (linRegType.equals(LinRegType.OLS) && y.length > 0) {
      linReg = getOLS(reduced.x, reduced.y, 1.2);
    } else {
      linReg = getWLS(reduced.x, reduced.y, makeWeights(reduced.y), 1.2);
    }

    slopes.put(isotope, linReg.slope);

    SpChartFactory.ChartComponent regressionComponent = new SpChartFactory.ChartComponent(
        new SpChartFactory.ChartData(
            isotopeLbl + ": s=" + SnF.doubleToString(linReg.slope, NF.D1C1, NF.D1C1Exp) + " "
                + "i=" + SnF.doubleToString(linReg.intercept, NF.D1C1) + " "
                + "R2=" + SnF.doubleToString(linReg.rSquare, NF.D1C3),
            linReg.x,
            linReg.y,
            xLbl, xUnit, MathMod.NONE,
            yLbl, yUnit, MathMod.NONE),
        new SpChartFactory.ChartStyle(color, 0.75,
            LineWidthDefaults.THICK,
            LineLineDashDefaults.STRAIGHT,
            MarkerSizeDefaults.SMALL,
            MarkerStyle.CIRCLE,
            false,
            RendererOption.LINE_AND_SHAPE,
            LineGraphStyle.LINE)
    );
    graphComponents.add(regressionComponent);
    legendComponents.add(regressionComponent);
  }

  public static double[] makeWeights(double[] y) {
    // check for zeros
    boolean anyZero = false;
    for (double v : y) {
      if (v == 0) {
        anyZero = true;
        break;
      }
    }

    if (anyZero) {
      return y;
    } else {
      double[] invY = new double[y.length];
      for (int i = 0; i < invY.length; i++) {
        invY[i] = 1d / y[i];
      }
      return invY;
    }
  }

  // chatgpt method to bucket/collapse duplicate x values while averaging y [checked]
  public static ReducedData collapseDuplicateX(
      double[] x, double[] y, double[] w) {

    // avoid empty array and checking at each sub step.
    if (w == null || w.length == 0) {
      w = new double[x.length];
    }

    if (x.length != y.length || x.length != w.length) {
      LOGGER.error("x, y, w must have same length. Returning original data.");
      return new ReducedData(x, y, w);
    }

    if (x.length == 0) {
      return new ReducedData(new double[0], new double[0], new double[0]);
    }

    // ---- compute epsilon (absolute + ULP based) ----
    double minAbsX = Double.POSITIVE_INFINITY; // initialize high, find min
    for (double xi : x) {
      minAbsX = Math.min(minAbsX, Math.abs(xi));
    }

    // ulp: difference to next possible floating point number
    // key issue: we may find very small values as well as fluctuations e.g. when empirical DT is not
    // perfectly at value
    double epsilon = Math.max(1e-8, 10 * Math.ulp(minAbsX));

    // ---- group values ----
    Map<Double, Bucket> buckets = new LinkedHashMap<>();

    outer:
    for (int i = 0; i < x.length; i++) {
      double xi = x[i];

      for (Bucket bucket : buckets.values()) {
        if (Math.abs(xi - bucket.x) <= epsilon) {
          bucket.ys.add(y[i]);
          bucket.ws.add(w[i]);
          continue outer;
        }
      }

      // new bucket
      Bucket b = new Bucket(xi);
      b.ys.add(y[i]);
      b.ws.add(w[i]);
      buckets.put(xi, b);
    }

    // ---- compute means ----
    int n = buckets.size();
    double[] xr = new double[n];
    double[] yr = new double[n];
    double[] wr = new double[n];

    int k = 0;
    for (Bucket b : buckets.values()) {
      xr[k] = b.x;
      yr[k] = MeasureOfLocation.MEAN.calc(b.ys);
      wr[k] = MeasureOfLocation.MEAN.calc(b.ws);
      k++;
    }

    return new ReducedData(xr, yr, wr);
  }

  private static class Bucket {
    final double x;                 // representative x
    final List<Double> ys = new ArrayList<>();
    final List<Double> ws = new ArrayList<>();

    Bucket(double x) {
      this.x = x;
    }
  }

}
