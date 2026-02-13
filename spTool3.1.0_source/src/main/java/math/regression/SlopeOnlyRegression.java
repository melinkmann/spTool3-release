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

/**
 * Idea: Allow Polymorphism to handle "how to quantify". i.e. usually transmission is calculated as
 * ratio of slopes. The calculation of the slope ratio is done by a class of higher level than this
 * one. However, we allow a LinReg implementation to only store a slope that represents a manually
 * entered TE, we found a flexible to solve this.
 */
public class SlopeOnlyRegression implements LinReg, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(SlopeOnlyRegression.class);

  private final boolean isValidInput;
  private final double slope;

  /**
   * Dummy placeholder constructor ensures that @isValidInput = false. Do not use this(0) because
   * that would trigger the warning in the logger.
   */
  public SlopeOnlyRegression() {
    this.slope = 0;
    this.isValidInput = true;
  }

  public SlopeOnlyRegression(double slope) {
    this.slope = slope;
    if (slope > 0) {
      this.isValidInput = true;
    } else {
      this.isValidInput = false;
      LOGGER.info("Slope was zero or negative. Bad input value.");
    }
  }

  @Override
  public void addObservations(double[] x, double[] y, double[] weight) {
    // do nothing
    LOGGER.info("Slope-only regression is not meant to receive observations and only exists "
        + "to achieve compatibility with user input slopes. "
        + "This not a user error but unintended coding.");
  }

  @Override
  public boolean isValidInput() {
    return isValidInput;
  }

  @Override
  public double getSlope() {
    return slope;
  }

  @Override
  public double getIntercept() {
    // as per default if "slope-only" is given.
    return 0;
  }

  @Override
  public double getRSquare() {
    // as per default if "slope-only" is given.
    return 1;
  }

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
      val = 0 + slope * x;
    }
    return val;
  }

  @Override
  public String getString() {
    return "SlopeOnly";
  }



}
