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

import java.io.Serial;
import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ArrUtils;

public class WLS implements LinReg, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(OLS.class);

  private final WeightedLinearRegressionSpTool regression = new WeightedLinearRegressionSpTool();
  private boolean isValidInput;
  private final boolean isSilent;

  /**
   * @param x
   * @param y
   * @param weight The weight is used linearly and w/o further math. What does that mean? Usually
   *               one would like to weigh the observation with 1/variance = 1/sigma^2. However,
   *               other estimators are also possible. This class simply multiplies an observed
   *               value y with the weight w. If 1/variance is desired, the weight array must be
   *               prepared as weight[] = {1/var_1, 1/var_2, ... 1/var_n}.
   */
  public WLS(double[] x, double[] y, double[] weight, boolean isSilent) {
    this.isSilent = isSilent;
    if (x.length != y.length || x.length != weight.length) {
      isValidInput = false;
      LOGGER.info(
          "Cannot regress unequal arrays: {xLen,yLen,wLen} "
              + "= {" + x.length + "," + y.length + "," + weight.length + "}.");
    } else if (x.length < 2 || y.length < 2 || weight.length < 2) {
      isValidInput = false;
      if (!isSilent) {
        LOGGER.info(
            "Cannot do weighted regression of only 1 or less data points: {xLen,yLen,wLen} "
                + "= {" + x.length + "," + y.length + "," + weight.length + "}.");
      }
    } else {
      // check if single point calibration: add P(0,0) with weight w=1 as first point.
      if (x.length == 2) {
        if (!isSilent) {
          LOGGER.info(
              "Weighted regression of 2 data points was requested (algorithm needs 3). To make it 3 points," +
                  " " +
                  "added the data point P(0,0) with weight w=1 to the data set.");
        }
        x = ArrUtils.concatWithCopy(new double[1], x);
        y = ArrUtils.concatWithCopy(new double[1], y);
        weight = ArrUtils.concatWithCopy(new double[]{1}, weight);
      }
      regression.regress1d(y, x, weight);
      isValidInput = true;
    }
  }

  public WLS(boolean isSilent) {
    this.isSilent = isSilent;
  }

  @Override
  public void addObservations(double[] x, double[] y, double[] weight) {
    if (x.length != y.length || x.length != weight.length) {
      isValidInput = false;
      LOGGER.info(
          "Cannot regress unequal arrays: {xLen,yLen,wLen} "
              + "= {" + x.length + "," + y.length + "," + weight.length + "}.");
    } else if (x.length < 2 || y.length < 2 || weight.length < 2) {
      isValidInput = false;
      LOGGER.info(
          "Cannot do weighted regression of only 1 or less data points: {xLen,yLen,wLen} "
              + "= {" + x.length + "," + y.length + "," + weight.length + "}.");
    } else {
      // check if single point calibration: add P(0,0) with weight w=1 as first point.
      if (x.length == 2) {
//        LOGGER.info(
//            "Weighted regression of 2 data points was requested (algorithm needs 3). To make it 3 points,
//            added the data point P(0,0) with weight w=1 to the data set.");
        x = ArrUtils.concatWithCopy(new double[1], x);
        y = ArrUtils.concatWithCopy(new double[1], y);
        weight = ArrUtils.concatWithCopy(new double[]{1}, weight);
      }
      regression.regress1d(y, x, weight);
      isValidInput = true;
    }
  }

  @Override
  public boolean isValidInput() {
    return isValidInput;
  }

  @Override
  public double getSlope() {
    double val = 0;
    if (isValidInput) {
      val = regression.getCoefficients()[1];
    }
    return val;
  }

  @Override
  public double getIntercept() {
    double val = 0;
    if (isValidInput) {
      val = regression.getCoefficients()[0];
    }
    return val;
  }

  @Override
  public double getRSquare() {
    double val = 0;
    if (isValidInput) {
      val = regression.getRSqare();
    }
    return val;
  }

  @Override
  public double predictX(double y) {
    double val = 0;
    double slope = getSlope();
    if (isValidInput && slope != 0) {
      val = (y - getIntercept()) / slope;
    }
    return val;
  }

  @Override
  public double predictY(double x) {
    double val = 0;
    double slope = getSlope();
    if (isValidInput && slope != 0) {
      val = getIntercept() + slope * x;
    }
    return val;
  }

  @Override
  public String getString() {
    return "WLS";
  }

  /**
   * For the 1/variance model or 1/x, 1/y.
   */
  public static double[] inverse(double[] arr) {
    double[] inverse = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      inverse[i] = 1 / arr[i];
    }
    return inverse;
  }

  /**
   * For 1/StdDev model or 1/y^2 or 1/x^2
   */
  public static double[] inverseSquare(double[] arr) {
    double[] inverse = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      inverse[i] = 1 / Math.pow(arr[i], 2);
    }
    return inverse;
  }

  /**
   * For Poisson-like model assuming StdDev=sqRoot(mean).
   */
  public static double[] inverseSquareRoot(double[] arr) {
    double[] inverse = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      inverse[i] = 1 / Math.pow(arr[i], 0.5);
    }
    return inverse;
  }


}
