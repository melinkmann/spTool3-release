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

import java.util.Locale;
import java.util.function.UnaryOperator;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class TextFieldUtils {

// #############################################################################################

  // https://stackoverflow.com/questions/40472668/numeric-textfield-for-integers-in-javafx-8-with-textformatter-and-or-unaryoperat
  // --> But the conversion only happens when the text is committed (via pressing enter) or when the TextField loses its focus.
  // --> i.e. the issue that 0.0002 becomes 2E-4 and cannot be edited anymore
  // https://qastack.com.de/programming/7555564/what-is-the-recommended-way-to-make-a-numeric-textfield-in-javafx
  // https://stackoverflow.com/questions/14563106/java-regex-on-doubles
  // https://regexr.com/33iga

  /*
  Why \r\n ?
  https://stackoverflow.com/questions/32056725/copypaste-to-javafx-textarea-and-keep-the-line-break
  "I think the issue is that when Excel copies text to the System clipboard,
  it represents a line break as '\r', (ASCII 0xc) instead of the more standard '\n' (ASCII 0xa)."
  --> If \r\ is not replaced, data cannot be copy pasted from excel. Great!
   */

  private static final UnaryOperator<TextFormatter.Change> allPassFilter = change -> change;

  // ############################################

  private static final UnaryOperator<TextFormatter.Change> underscoreNumeralLetterFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("([a-zA-Z0-9\\-\\.\\_\s]*)")) {
      return change;
    }
    return null;
  };

  private static final UnaryOperator<TextFormatter.Change> letterFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("([a-zA-Z]*)")) {
      return change;
    }
    return null;
  };

  private static final UnaryOperator<TextFormatter.Change> numeralLetterFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("([a-zA-Z0-9]*)")) {
      return change;
    }
    return null;
  };

// ############################################

  private static final UnaryOperator<TextFormatter.Change> integerFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("-?([0-9]*)")) {
      return change;
    }
    return null;
  };

  private static final UnaryOperator<TextFormatter.Change> positiveIntegerFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("([0-9]*)")) {
      return change;
    }
    return null;
  };

  // ############################################

  private static final UnaryOperator<TextFormatter.Change> integerExpFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("-?[1-9][0-9]*(E-|E\\+|E|\\d)?\\d*")) {
      return change;
    }
    return null;
  };

  private static final UnaryOperator<TextFormatter.Change> positiveIntegerExpFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("[1-9][0-9]*(E-|E\\+|E|\\d)?\\d*")) {
      return change;
    }
    return null;
  };

  // ############################################

  private static final UnaryOperator<TextFormatter.Change> doubleFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    // "+" version: allows as many digits as possible but at least 1, thus making GUI uncomfortable
//        if (newText.matches("-?[0-9]+(\\.[0-9]*)?")) {
    // idea: use brackets: {1,13} ---> maximum 13 digits before the comma: "-?[0-9]{1,13}(\\.[0-9]*)?"
    // with the workaround: from 0 - 20 digits, or now sth like 30, 100 (up to 326 digits which is the maximum Java doubles could even handle)
    if (newText.matches("-?[0-9]{0,30}(\\.?[0-9]*)")) {
      return change;
    }
    return null;
  };

  private static final UnaryOperator<TextFormatter.Change> positiveDoubleFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("[0-9]{0,30}(\\.?[0-9]*)")) {
      return change;
    }
    return null;
  };


  /*
   Note: a "nonzeroPositiveDoubleFilter" is not really possible here. Whenever you enter "0.",
   you either have to be stopped because 0. == 0, or you should be able to go on to type "0.1" or sth.
   The problem is that at this stage, we check EACH CHANGE, i.e., also the moment of "0.".
   */


  private static final UnaryOperator<TextFormatter.Change> doubleExpFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("(-?\\d*)\\.?\\d*(E-|E\\+|E|\\d*)\\d*")) {
      return change;
    }
    return null;
  };

  private static final UnaryOperator<TextFormatter.Change> positiveDoubleExpFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    if (newText.matches("(\\d*)\\.?\\d*(E-|E\\+|E|\\d*)\\d*")) {
      return change;
    }
    return null;
  };


  private static final UnaryOperator<TextFormatter.Change> doubleExpAndSpaceFilter = change -> {
    String newText = change.getControlNewText();
    newText = newText.replace("\r\n", "");
    // "+" version: allows as many digits as possible but at least 1, thus making GUI uncomfortable
//        if (newText.matches("-?[0-9]+(\\.[0-9]*)?")) {
    // idea: use brackets: {1,13} ---> maximum 13 digits before the comma: "-?[0-9]{1,13}(\\.[0-9]*)?"
    // with the workaround: from 0 - 20 digits
    if (newText.matches("[-0-9.E ]*")) { // "-?[-0-9 ]{0,20}(\\.[-0-9 ]*)?"
      return change;
    }
    return null;
  };

  // ############################################################################################
  // ############################################################################################


  public static final TextFormatter<String> allPass() {
    return allPass("");
  }


  public static final TextFormatter<String> allPass(String defaultValue) {
    return new TextFormatter<String>(new StringConverter<String>() {
      @Override
      public String toString(String s) {
        return s;
      }

      @Override
      public String fromString(String s) {
        return s;
      }
    }, defaultValue, allPassFilter);
  }


  public static final TextFormatter<String> assureLetter(String defaultValue) {
    return new TextFormatter<String>(new StringConverter<String>() {
      @Override
      public String toString(String object) {
        return object;
      }

      @Override
      public String fromString(String string) {
        return string;
      }
    }, defaultValue, letterFilter);
  }


  public static final TextFormatter<String> assureCapitalLetter(String defaultValue) {
    return new TextFormatter<String>(new StringConverter<String>() {
      @Override
      public String toString(String object) {
        return object.toUpperCase(Locale.ROOT);
      }

      @Override
      public String fromString(String string) {
        return string.toUpperCase(Locale.ROOT);
      }
    }, defaultValue, letterFilter);
  }

  public static final TextFormatter<String> assureCharacter(String defaultValue) {
    return new TextFormatter<>(new StringConverter<>() {
      @Override
      public String toString(String object) {
        return String.valueOf(object.charAt(0));
      }

      @Override
      public String fromString(String string) {
        return String.valueOf(string.charAt(0));
      }
    }, defaultValue, allPassFilter);
  }

// ######################################################################################

  public static final TextFormatter<Integer> assureInteger(int defaultValue) {
    return new TextFormatter<>(new IntegerStringConverter(), defaultValue, integerFilter);
  }

  public static final TextFormatter<Integer> assurePositiveInteger(int defaultValue) {
    return new TextFormatter<>(new IntegerStringConverter(), defaultValue,
        positiveIntegerFilter);
  }

  public static final TextFormatter<Integer> assureNonzeroPositiveInteger(int defaultValue) {
    return new TextFormatter<>(new NonzeroIntegerStringConverter(), defaultValue,
        positiveIntegerFilter);
  }

  //###########################################

  public static final TextFormatter<Integer> assureExpInteger(int defaultValue) {
    return new TextFormatter<>(new ScientificIntegerStringConverter(), defaultValue,
        integerExpFilter);
  }


  public static final TextFormatter<Integer> assurePosExpInteger(int defaultValue) {
    return new TextFormatter<>(new NonzeroScientificIntegerStringConverter(), defaultValue,
        positiveIntegerExpFilter);
  }

  public static final TextFormatter<Integer> assureNonzeroPosExpInteger(int defaultValue) {
    return new TextFormatter<>(new NonzeroScientificIntegerStringConverter(), defaultValue,
        positiveIntegerExpFilter);
  }

  //####################################################################################

  public static final TextFormatter<Double> assureDouble(double defaultValue) {
    return new TextFormatter<>(new PlainDoubleStringConverter(), defaultValue, doubleFilter);
  }


  public static final TextFormatter<Double> assurePositiveDouble(double defaultValue) {
    return new TextFormatter<>(new PlainDoubleStringConverter(), defaultValue,
        positiveDoubleFilter);
  }


  public static final TextFormatter<Double> assureNonzeroPositiveDouble(double defaultValue) {
    return new TextFormatter<>(new NonzeroDoubleStringConverter(), defaultValue,
        positiveDoubleFilter);
  }

  // ############################################

  public static final TextFormatter<Double> assureExpDouble(double defaultValue) {
    return new TextFormatter<>(new ScientificDoubleStringConverter(), defaultValue,
        doubleExpFilter);
  }


  public static final TextFormatter<Double> assurePosExpDouble(double defaultValue) {
    return new TextFormatter<>(new ScientificDoubleStringConverter(), defaultValue,
        positiveDoubleExpFilter);
  }

  public static final TextFormatter<Double> assureNonzeroPosExpDouble(double defaultValue) {
    return new TextFormatter<>(new NonzeroScientificDoubleStringConverter(), defaultValue,
        positiveDoubleExpFilter);
  }

  // ########################################################

  public static final TextFormatter<String> assureDoubleToleratingSpace(String defaultValue) {
    return new TextFormatter<>(new DefaultStringConverter(), defaultValue,
        doubleExpAndSpaceFilter);
  }

  public static final TextFormatter<String> assureNumeralOrLetter(String defaultValue) {
    return new TextFormatter<>(new StringConverter<>() {
      @Override
      public String toString(String object) {
        return object;
      }

      @Override
      public String fromString(String string) {
        return string;
      }
    }, defaultValue, numeralLetterFilter);
  }

  public static final TextFormatter<String> underscoreNumeralOrLetter(String defaultValue) {
    return new TextFormatter<>(new StringConverter<>() {
      @Override
      public String toString(String object) {
        return object;
      }

      @Override
      public String fromString(String string) {
        return string;
      }
    }, defaultValue, underscoreNumeralLetterFilter);
  }

  // WARNING: Do not waste your time here, especially since we do not need this right now.
  /// What one could try. An additional if(str.contains(".") {return null;} to reject all doubles
  // Note that an integer sci number is only possible if there is no comma or you would have to check
  // if there are enough digits hidden in the exponent to compensate for the comma..
  // In which scenario is this better than just rounding?...
  // except for that the user does not really understand whats going on when rounding.
  // "Did not work because the StringFormatter for this field would have to know that only 1E6 like
  // formats are valid and I currently do not know how to implement this especially bc of the
  // weirdness in the exponential format where EXXX seems to count as decimal places, too."
  //  public static final TextFormatter<Double> assureExpInt(double defaultValue) {
  //    return new TextFormatter<Double>(new ScientificDoubleStringConverter(), defaultValue,
  //        intExpFilter);
  //  }
}
