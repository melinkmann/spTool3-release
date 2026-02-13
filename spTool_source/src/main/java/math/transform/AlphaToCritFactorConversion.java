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

package math.transform;

import gui.dialog.notification.NotificationFactory;
import java.io.Serializable;
import java.util.Optional;
import math.stat.BeasleySpringerMoroInvNormCdf;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AlphaToCritFactorConversion implements Conversion<Double>, Serializable {

  private static final Logger LOGGER = LogManager.getLogger(
      AlphaToCritFactorConversion.class.getName());

  private final NormalDistribution nd = new NormalDistribution(0, 1);

  @Override
  public String defineCalculationResult() {
    return "z-Score (critical factor)";
  }

  @Override
  public String defineInversionResult() {
    return "Alpha (false positive ratio)";
  }


    /*
  NOTE: Although this topic hurts, we need to pay some attention to how small or precise we can go
  with doubles. The problem is that critical values corresponding to e.g., f=8.27 already start
  to exceed what we can represent as "1-alpha". This might be due to conversion between Binary and
  Decimal system (?). Anyway, "1-alpha" fails way earlier than just "alpha".

  This does not even include issues with either the approximation to the erf() used by Apache
  or intermediate steps in their calculations that may include "1-alpha". I only checked what can
  be calculated from "outside" of the function to check where the results OBVIOUSLY do not match
  any sensible expectations to numbers. The mathematical accuracy may very well fail earlier.

    System.out.println(Double.MIN_VALUE);                                   --> 4.9E-324
    System.out.println(Math.nextDown(1.0d));                                --> 0.9999999999999999
    System.out.println(1.0-Math.nextDown(1.0d));                            --> 1.1102230246251565E-16

    --> We can show way smaller values than we can approach one.

     System.out.println(Math.nextUp(0.0d));                                 --> 4.9E-324

    System.out.println(Math.nextUp(Math.nextUp(0.0d)));                     --> 1.0E-323
    System.out.println(Math.nextUp(Math.nextUp(0.0d)) - Math.nextUp(0.0d)); --> 4.9E-324
   */

  @Override
  public Double calc(Double alpha) {
    /*
    The Beasley-... approximation seems to work best for SMALL alphas, i.e., not for 1-alpha.
    I am not sure whether this is part of the approximation (numerically) or if it is due to the
    fact that we cann store smaller alphas as a 64bit double than "1-alpha" can approach 1.
    Hence, here we prefer the -invCdf(alpha) version to the invCdf(1-alpha).
     */

    // Assume ApacheCommons as the default.
    double f_crit = -nd.inverseCumulativeProbability(alpha);
    // Check value

    // This should not happen as we cannot pass such a number as an argument,...
    if (alpha < Math.nextUp(0.0d)) {
      f_crit = 37; // This is the largest z we tolerate when reversing the operation.
      String msg = "Your alpha it too low for floating point arithmetic."
          + " The z-Value result was set z=" + f_crit + ".";
      LOGGER.error(msg);
    }

    if (alpha >= 0.5) {
      String msg = "By internal convention, alpha values must be strictly smaller than 0.5 "
          + "(i.e., a<50%). You gave alpha=" + alpha + ". SpTool will proceed with z-Value = 0.";
      LOGGER.info(msg);
      f_crit = 0;
    }

    if (alpha < 1E-94) {
      f_crit = 37; // This is the largest z we tolerate when reversing the operation.
      String msg = "Your alpha-value alpha=" + alpha + " is extremely low. "
          + "You should consider just entering a z-Value instead of the alpha error approach."
          + " z-Value result was set z=" + f_crit + ".";
      LOGGER.info(msg);
      NotificationFactory.openError(msg);
    } else if (alpha < 1E-46) {
      String msg = "Your alpha value alpha=" + alpha + " is really low. "
          + "SpTool uses the Beasley-Springer-Moro approximation for a<1E-13. "
          + "At your alpha, be aware that the approximation given as your z-Value is more and more inaccurate. "
          + "You should consider just entering a z-Value instead of the alpha error approach.";
      f_crit = -BeasleySpringerMoroInvNormCdf.invNormCdfApprox(alpha);
      LOGGER.info(msg);
      NotificationFactory.openError(msg);
    } else if (alpha < 1E-13) {
      String msg = "Your alpha value alpha=" + alpha + " is rather low."
          + " Things are still alright but SpTool will have to use an approximation"
          + " called Beasley-Springer-Moro instead of slightly more precise calculations based on erf().";
      LOGGER.info(msg);
      f_crit = -BeasleySpringerMoroInvNormCdf.invNormCdfApprox(alpha);
    }

    return f_crit;
  }

  @Override
  public Double invert(Double f_crit) {
    /*
     The erf() used by nd.cdf() seems more stable than the reverse. Thus, we can tolerate
     higher z-values.
     However, note that "1-alpha" easily exceeds double precision. Hence, use dcf(-f)
     instead of -cdf(f)
     */
    double alpha;
    // Check value
    String msg;
    if (f_crit < 0) {
      msg = "By internal convention, z-value must be greater than zero. "
          + "SpTool will use the respective positive number instead. "
          + "Your input was z=" + f_crit + " which was changed to " + Math.abs(f_crit) + ".";
      LOGGER.info(msg);
      f_crit = Math.abs(f_crit);
    }

    alpha = nd.cumulativeProbability(-f_crit);

    if (f_crit > 20) {
      msg = "For z > 20, the result may be numerically unstable and inaccurate. "
          + "Consider using an alpha value directly. You gave z=" + f_crit + ".";
      LOGGER.info(msg);
    } else if (f_crit > 37) {
      msg = "For z > 37, the corresponding alpha value will reach close to the smallest number "
          + "that can be stored as a 64Bit double. This operation yields alpha = 0. "
          + "Consider using an alpha value directly. You gave z=" + f_crit + ".";
      LOGGER.info(msg);
      NotificationFactory.openError(msg);
      alpha = 0;
    }

    return alpha;
  }

  /////////////////////////////////////////////////////////

  /**
   * A couple of remarks: in spICP-MS, we are aiming for somewhat ridiculous alpha values. Why?
   * There are to problems: (1) Our detection capacity maxes out at 50-100 events/s. In a minute, the
   * amounts to 50 * 60 = 3000 particles. Often, however, we are lower at e.g., 500 particles per
   * minute (around 9 NP/s). For a Poisson process that gives 500+-22 (sqrt!) particles. (2) At the
   * same time, our detector runs at approx 50 µs, i.e., 20k data points per second, i.e., 1.2e6
   * data points per minute. At around 3.3 sigma we are at a false positive rate that give
   * approximately 500 false positives (a=4E-4). In order to get down to only one false positive,
   * we'd have to go down to at least 5E-7 at 4.9 sigma. However, we know that the distribution is
   * not perfectly normal but has fat-tails, i.e., as a user one is tempted / forced to use much
   * larger sigma values. Hence, this routine to make sure, that the ApacheCommons library does not
   * fail at these low alpha values. Note 2 things: (1) For the Beasley algorithm, we need alpha
   * (not 1-a) as the Beasley Approximation is better at SMALL p values (whereas 1-alpha becomes
   * LARGE!!). Due to the Symmetry of the Gaussian however, cdf(1-p) = - cdf(p), which becomes quite
   * evident when the distribution is draw with the respective percentiles. (2) Apache's
   * inverseErrorFunction seems to fail earlier than the erfc() approximation. Hence, it still
   * delivers plausible cdf() while the inverseCdf() has already failed.
   * <p>
   * Check out some test data: in the Class of BeasleyAlgorithm.
   */

  //
  public Optional<Double> openDialog(Double inputDouble) {
//    // Note: INPUT and OUTPUT in declared direction of the Transformation
//    Parameter<Double> inputParam = new DoubleParameter(
//        revertLabel(),
//        "Alpha value, i.e., false positive ratio",
//        invert(inputDouble),
//        NF.D1C3E3,
//        TextFormatterOption.ASSURE_POS_EXP_DOUBLE,
//        "dummy");
//
//    Parameter<Double> outputParam = new DoubleParameter(
//        "z-Score",
//        forwardLabel(),
//        calc(inputDouble),
//        NF.D1C3E3,
//        TextFormatterOption.ASSURE_POS_EXP_DOUBLE,
//        "dummy");
//
//    FxParameter<Double> inputFxParameter;
//    FxParameter<Double> outputFxParameter;
//
//    if (direction.equals(Direction.FORWARD)) {
//      // Normal order of output and input
//      inputFxParameter = inputParam.getObservableInstance();
//      outputFxParameter = outputParam.getObservableInstance();
//    } else {
//      // Invert output and input!
//      inputFxParameter = outputParam.getObservableInstance();
//      outputFxParameter = inputParam.getObservableInstance();
//    }
//
//    outputFxParameter.setUneditable();
//
//    final PauseTransition slowPause = new PauseTransition(Duration.seconds(0.15));
//
//    // TODO.. automated this?! Register all params in a "eq" and then just call that and put the result in the output?
//
//    ((TextField) inputFxParameter.getValueNode()).textProperty().addListener(
//        (observable, oldValue, newValue) -> {
//          slowPause.setOnFinished(event -> {
//            if (SnF.isValidDouble(newValue)) {
//              outputFxParameter.getPlainParameter().setValueWithFormat(
//                  calc(SnF.strToDouble(newValue)));
//              System.out.println(calc(SnF.strToDouble(newValue)));
//              outputFxParameter.externalUpdate();
//            }
//          });
//          slowPause.playFromStart();
//        }
//    );
//
//    // Here we know exactly what kind of type they are and how to convert, hence do the UI stuff here.
//
//    Dialog<Double> dialog = new ConversionDialog<>(inputFxParameter, outputFxParameter, this);
//
//    Optional<Double> result = dialog.showAndWait();
//    return result;

    return null;
  }
}
