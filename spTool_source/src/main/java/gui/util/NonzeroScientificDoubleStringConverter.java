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

package gui.util;

import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.NF;
import util.SnF;

public class NonzeroScientificDoubleStringConverter extends StringConverter<Double> {

  private static final Logger LOGGER = LogManager
      .getLogger(NonzeroScientificDoubleStringConverter.class.getName());

  /*

  Note, this viewer is meant to be scientific.
  Only in the "human readable range between 0.001 and 9999", we want to keep standard notation.

  The UI is mostly based on a guaranteed 3 digit visualization,
  which I personally like as it gives a good grasp on magnitudes but does not overwhelm.
  Now, if we use this converter, we want to switch to scientific notation, as soon as the value
  goes below 0.01, i.e., the smallest standard format. Why not 0.001? Assume you allow 0.001 as
  the smallest value, that means that you only get ONE significant digit, assuming a default of
  showing 3 digits.

  Keep in mind, the idea here is, using scientific notation whenever it makes sense, but
  not in the "human readable range between 0.001 and 9999".
   */
  public static final double SMALLEST_STANDARD_NUMERAL = 0.01;
  public static final double BIGGEST_STANDARD_NUMERAL = 10_000;

  private final NF nf_SCIENCE;
  private final NF nf_PLAIN;

  public NonzeroScientificDoubleStringConverter() {

    /*
    These should be large as otherwise, we cannot show more digits when going into "editing mode"
    of TextFields.
     */
    this.nf_PLAIN = NF.D1C30TxtFormatter;
    this.nf_SCIENCE = NF.D1C30ExpTxtFormatter;
  }

  public NonzeroScientificDoubleStringConverter(NF nfPLAIN, NF nfSCIENCE) {
    this.nf_PLAIN = nfPLAIN;
    this.nf_SCIENCE = nfSCIENCE;
  }

  public Double fromString(String var1) {
    Double result;
    boolean wasZero = false;
    if (var1 == null) {
      result = null;
    } else {
      var1 = var1.trim();
      if (var1.length() < 1) {
        result = null;
      } else {
        if (Double.parseDouble(var1) == 0) {
          wasZero = true;
          result = 1d;
        } else {
          result = Double.valueOf(var1);
        }

      }

    }
    if (wasZero) {
      LOGGER.debug("Value cannot be zero. Continued with value =" + result + " instead.");
      // This gets stuck in inf loop as the UI never gets updated with the popup open
      // DialogFactory.openOK("Value cannot be zero.");
    }
    return result;
  }

  @Override
  public String toString(Double number) {
    if (number == null) {
      return "";
    } else {
      String returnVal;
      if (number < SMALLEST_STANDARD_NUMERAL) {
        returnVal = SnF.doubleToString(number, nf_SCIENCE); // 0.000258 e.g.
      } else if (number < BIGGEST_STANDARD_NUMERAL) {
        returnVal = SnF.doubleToString(number, nf_PLAIN); // 3500 e.g. or 0.253
      } else {
        returnVal = SnF.doubleToString(number, nf_SCIENCE); // 142684684268 e.g.
      }
      return returnVal;
    }
  }


}
