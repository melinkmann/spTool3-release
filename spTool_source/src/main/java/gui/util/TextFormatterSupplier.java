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

import javafx.scene.control.TextFormatter;

public class TextFormatterSupplier {

  public static TextFormatter<Integer> get(TextFormatterOption option, int defaultValue) {
    TextFormatter<Integer> formatter;
    switch (option) {
      case ASSURE_INTEGER -> formatter = TextFieldUtils
          .assureInteger(defaultValue);

      case ASSURE_POSITIVE_INTEGER -> formatter = TextFieldUtils
          .assurePositiveInteger(defaultValue);

      case ASSURE_NONZERO_POSITIVE_INTEGER -> formatter = TextFieldUtils
          .assureNonzeroPositiveInteger(defaultValue);

      case ASSURE_EXP_INTEGER -> formatter = TextFieldUtils
          .assureExpInteger(defaultValue);

      case ASSURE_POSITIVE_EXP_INTEGER -> formatter = TextFieldUtils
          .assurePosExpInteger(defaultValue);

      case ASSURE_NONZERO_POSITIVE_EXP_INTEGER -> formatter = TextFieldUtils
          .assureNonzeroPosExpInteger(defaultValue);

      default -> formatter = TextFieldUtils.assureInteger(defaultValue);
    }
    return formatter;
  }

  public static TextFormatter<Double> get(TextFormatterOption option, double defaultValue) {
    TextFormatter<Double> formatter;
    switch (option) {

      case ASSURE_DOUBLE -> formatter = TextFieldUtils.assureDouble(defaultValue);

      case ASSURE_POSITIVE_DOUBLE -> formatter = TextFieldUtils
          .assurePositiveDouble(defaultValue);

      case ASSURE_NONZERO_POSITIVE_DOUBLE -> formatter = TextFieldUtils
          .assureNonzeroPositiveDouble(defaultValue);

      case ASSURE_EXP_DOUBLE -> formatter = TextFieldUtils
          .assureExpDouble(defaultValue);

      case ASSURE_POS_EXP_DOUBLE -> formatter = TextFieldUtils
          .assurePosExpDouble(defaultValue);

      case ASSURE_NONZERO_POS_EXP_DOUBLE -> formatter = TextFieldUtils
          .assureNonzeroPosExpDouble(defaultValue);

      default -> formatter = TextFieldUtils.assureDouble(defaultValue);
    }
    return formatter;
  }

  public static TextFormatter<String> get(TextFormatterOption option, String defaultValue) {
    TextFormatter<String> formatter;
    switch (option) {
      case ASSURE_CAPITAL_LETTER -> formatter = TextFieldUtils
          .assureCapitalLetter(defaultValue);

      case ASSURE_ANY_DOUBLE_TOLERATING_SPACE -> formatter = TextFieldUtils
          .assureDoubleToleratingSpace(defaultValue);

      case ASSURE_EXTD_NUMERAL_OR_LETTER -> formatter = TextFieldUtils
          .underscoreNumeralOrLetter(defaultValue);

      case ASSURE_NUMERAL_OR_LETTER -> formatter = TextFieldUtils
          .assureNumeralOrLetter(defaultValue);

      case ASSURE_CHARACTER -> formatter = TextFieldUtils
          .assureCharacter(defaultValue);

      case ALL_PASS -> formatter = TextFieldUtils.allPass(defaultValue);

      default -> formatter = TextFieldUtils.assureLetter(defaultValue);
    }
    return formatter;
  }


}
