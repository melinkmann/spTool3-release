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

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OLS implements LinReg, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(OLS.class);

  private SimpleRegression regression = new SimpleRegression();
  private boolean isValidInput;
  private final boolean isSilent;

  public OLS(double[] x, double[] y, boolean isSilent) {
    this.isSilent = isSilent;
    if (x.length != y.length) {
      isValidInput = false;
      LOGGER.info(
          "Cannot regress unequal arrays: {xLen,yLen} = {" + x.length + "," + y.length + "}.");
    } else if (x.length == 0 || y.length == 0) {
      isValidInput = false;
      LOGGER.info(
          "Cannot regress empty arrays: {xLen,yLen} = {" + x.length + "," + y.length + "}.");
    } else {
      // check if single point calibration: add P(0,0) as first point.
      if (x.length == 1) {
        if (!isSilent) {
          LOGGER.info(
              "Regression of single data point was requested. Thus, added the data point P(0,0) to the data" +
                  " " +
                  "set.");
        }
        regression.addData(0, 0);
      }
      for (int i = 0; i < x.length; i++) {
        regression.addData(x[i], y[i]);
      }
      isValidInput = true;
    }
  }

  public OLS(boolean isSilent) {
    this.isSilent = isSilent;
  }

  @Override
  public void addObservations(double[] x, double[] y, double[] weights) {
    // Ignore weights for OLS
    if (x.length != y.length) {
      isValidInput = false;
      LOGGER.info(
          "Cannot regress unequal arrays: {xLen,yLen} = {" + x.length + "," + y.length + "}.");
    } else if (x.length == 0 || y.length == 0) {
      isValidInput = false;
      LOGGER.info(
          "Cannot regress empty arrays: {xLen,yLen} = {" + x.length + "," + y.length + "}.");
    } else {
      // check if single point calibration: add P(0,0) as first point.
      if (x.length == 1) {
        if (!isSilent) {
          LOGGER.info(
              "Regression of single data point was requested. Thus, added the data point P(0,0) to the data" +
                  " set.");
        }
        regression.addData(0, 0);
      }
      for (int i = 0; i < x.length; i++) {
        regression.addData(x[i], y[i]);
      }
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
      val = regression.getSlope();
    }
    return val;
  }

  @Override
  public double getIntercept() {
    double val = 0;
    if (isValidInput) {
      val = regression.getIntercept();
    }
    return val;
  }

  @Override
  public double getRSquare() {
    double val = 0;
    if (isValidInput) {
      val = regression.getRSquare();
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
    if (isValidInput) {
      val = regression.predict(x);
    }
    return val;
  }


  @Override
  public String getString() {
    return "OLS";
  }


}
