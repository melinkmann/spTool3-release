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

package util;

import gui.util.ScientificDoubleStringConverter;
import gui.util.ScientificIntegerStringConverter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.util.Pair;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SnF {

  private static final Logger LOGGER = LogManager.getLogger(SnF.class);

  // ##################################### Conversions ########################################

  /*
   * Instead of parsing, first just check whether the format is valid
   */
  public static boolean isValidDouble(String string) {
    boolean good = false;
    if (string != null) {
      try {
        double parsedValue = Double.parseDouble(string);
        good = true;

        // If we can remove a character and still get the same result, we are out of double precision
        if (string.length() > 15) {
          // String addOne = string + "1"; // Check if we can make the String longer to check if we might
          // me exceeding double precision.
          String workingString = string;
          if (string.contains("E")) {

            String mantissa;
            String exponent;

            String[] split = workingString.split("E");

            if (split.length == 2) {
              mantissa = split[0];
              exponent = split[1];

              mantissa = removeTrailingZeros(mantissa);
              exponent = removeTrailingZeros(exponent);

              mantissa = removeLastChar(mantissa);

              workingString = mantissa + "E" + exponent;
            }

          } else {
            workingString = removeLastChar(string);
          }
          double parsedAfterRemoval = Double.parseDouble(workingString);

          if (parsedValue == parsedAfterRemoval) {
            LOGGER.info("You may be exceeding double precision with too many (decimal) digits."
                + "The given string is '" + string + "' "
                + "which was parsed to a double before and after removing one char at the end. "
                + "The is double is d=" + parsedValue + ".");
          }
        }

      } catch (NumberFormatException e) {
        // If TextField has empty value while typing, the exception does not really help.
        /// Same is true for typing "E-" and getting the error.
        if (!string.isEmpty() && !string.endsWith("E") && !string.endsWith("E-")) {
          LOGGER.info(ExceptionUtils.getStackTrace(e));
        }
        good = false;
      }
    }
    return (good);
  }

  public static boolean isValidDoubleSilent(String string) {
    boolean good = false;
    if (string != null) {
      try {
        double parsedValue = Double.parseDouble(string);
        good = true;
      } catch (NumberFormatException e) {
        good = false;
      }
    }
    return (good);
  }

  public static boolean isValidInt(String string) {
    boolean good = false;
    if (string != null) {
      try {
        int val = Integer.parseInt(string);
        good = true;
      } catch (NumberFormatException e) {
        // If TextField has empty value while typing, the exception does not really help.
        if (!string.isEmpty()) {
          LOGGER.info(ExceptionUtils.getStackTrace(e));
        }
        good = false;
      }
    }
    return (good);
  }

  public static boolean isValidIntSilently(String string) {
    boolean good = false;
    if (string != null) {
      try {
        int val = Integer.parseInt(string);
        good = true;
      } catch (NumberFormatException e) {
        good = false;
      }
    }
    return (good);
  }

  // ###############################################################################################

  /**
   * @param num   the number to round
   * @param digit the precision, i.e., if digit == 10, all numbers will be given as '1950, 1420,
   *              130' ...
   * @return the rounded number
   */
  public static double round(double num, double digit) {
    return Math.round(num / digit) * digit;
  }

  /*
   * parse String to Double with the possibility to pass an alternative Value if the parser
   * returned null; the method without alternative Value passes 0.0
   */
  public static double strToDouble(String string, double alternativeValue) {
    if (isValidDouble(string)) {
      return Double.parseDouble(string);
    } else {
      LOGGER.info("Parser could not parse String to Double. String may be empty or too long."
          + " String value= '" + string + "'. ");
      return alternativeValue;
    }
  }

  public static double strToDoubleSilent(String string, double alternativeValue) {
    if (isValidDoubleSilent(string)) {
      return Double.parseDouble(string);
    } else {
      return alternativeValue;
    }
  }

  // return 0 version
  public static double strToDouble(String string) {
    return strToDouble(string, 0.0);
  }

  public static double strToDoubleSilent(String string) {
    return strToDoubleSilent(string, 0.0);
  }

  /*
   * parse String to Integer with the possibility to pass an alternative Value if the parser
   * returned null; the method without alternative Value passes 0
   */
  public static int strToInt(String string, int alternativeValue) {
    if (isValidInt(string)) {
      return Integer.parseInt(string);
    } else {
      LOGGER.info("Parser could not parse String to Integer. String may be empty or too long."
          + " Given string value= '" + string + "'. "
          + "Note that the longest allows 32Bit String in Java (unfortunately) is "
          + Integer.MAX_VALUE
          + ". This function has returned " + alternativeValue);
      return alternativeValue;
    }
  }

  // return 0 version
  public static int strToInt(String string) {
    return strToInt(string, 0);
  }

  // formatter to show numbers
  public static String intToString(int num, NF format) {
    try {
      DecimalFormatSymbols decimalSymbols = DecimalFormatSymbols.getInstance();
      decimalSymbols.setDecimalSeparator('.');
      return new DecimalFormat(format.pattern(), decimalSymbols).format(num);
    } catch (Exception e) {
      LOGGER.info(ExceptionUtils.getStackTrace(e));
      return String.valueOf(num);
    }
  }

  public static String intToString(int num) {
    return intToString(num, NF.D1C0);
  }


  // return 0 version
  public static List<Integer> strListToInt(List<String> strings) {
    List<Integer> integers = strings.stream()
        .map(str -> strToInt(str, 0))
        .collect(Collectors.toList());
    return integers;
  }

  // Variant that includes an automatic decision which format to use
  public static String intFldToString(int num, NF format) {
    try {
      DecimalFormatSymbols decimalSymbols = DecimalFormatSymbols.getInstance();
      decimalSymbols.setDecimalSeparator('.');
      if (num < ScientificIntegerStringConverter.BIGGEST_STANDARD_NUMERAL) {
        return new DecimalFormat(format.ensurePlain(), decimalSymbols).format(num);
      } else {
        return new DecimalFormat(format.pattern(), decimalSymbols).format(num);
      }
    } catch (Exception e) {
      LOGGER.info(ExceptionUtils.getStackTrace(e));
      return String.valueOf(num);
    }
  }

  public static String doubleToString(double num, NF format) {
    try {
      DecimalFormatSymbols decimalSymbols = DecimalFormatSymbols.getInstance();
      decimalSymbols.setDecimalSeparator('.');
      return new DecimalFormat(format.pattern(), decimalSymbols).format(num);
    } catch (Exception e) {
      LOGGER.info(ExceptionUtils.getStackTrace(e));
      return String.valueOf(num);
    }
  }


  // Variant that includes an automatic decision which format to use
  public static String doubleFldToString(double num, NF format) {
    try {
      DecimalFormatSymbols decimalSymbols = DecimalFormatSymbols.getInstance();
      decimalSymbols.setDecimalSeparator('.');
      if (num < ScientificDoubleStringConverter.SMALLEST_STANDARD_NUMERAL) {
        return new DecimalFormat(format.ensureExp(), decimalSymbols).format(num);
      } else if (num < ScientificDoubleStringConverter.BIGGEST_STANDARD_NUMERAL) {
        return new DecimalFormat(format.ensurePlain(), decimalSymbols).format(num);
      } else {
        return new DecimalFormat(format.ensureExp(), decimalSymbols).format(num);
      }
    } catch (Exception e) {
      LOGGER.info(ExceptionUtils.getStackTrace(e));
      return String.valueOf(num);
    }
  }

  public static String doubleToString(double num, NF small, NF large) {
    try {
      DecimalFormatSymbols decimalSymbols = DecimalFormatSymbols.getInstance();
      decimalSymbols.setDecimalSeparator('.');
      double smallestNumeral = ScientificDoubleStringConverter.SMALLEST_STANDARD_NUMERAL; // 0.01
      if (small.equals(NF.D1C1)) {
        smallestNumeral = 0.1;
      }
      if (num < smallestNumeral) {
        return new DecimalFormat(large.pattern(), decimalSymbols).format(num);
      } else if (num < ScientificDoubleStringConverter.BIGGEST_STANDARD_NUMERAL) {
        return new DecimalFormat(small.pattern(), decimalSymbols).format(num);
      } else {
        return new DecimalFormat(large.pattern(), decimalSymbols).format(num);
      }
    } catch (Exception e) {
      LOGGER.info(ExceptionUtils.getStackTrace(e));
      return String.valueOf(num);
    }
  }

  // ###############################################################################################

  public static Pair<Boolean, double[]> strToDoubleArrChecked(String[] strings) {
    boolean isGood = false;
    double[] doubles = new double[strings.length];
    for (int i = 0; i < strings.length; i++) {
      String str = strings[i];
      if (str != null && !str.isEmpty()) {
        try {
          doubles[i] = Double.parseDouble(str);
          isGood = true;
        } catch (NumberFormatException e) {
          doubles[i] = 0;
          isGood = false;
        }
      }
    }
    return new Pair<>(isGood, doubles);
  }


  public static String[] doubleToStrArr(double[] doubles, NF format) {
    String[] strings = new String[0];
    if (doubles.length > 0) {
      strings = Arrays.stream(doubles)
          .mapToObj(d -> doubleToString(d, format))
          .toArray(String[]::new);
    }
    return strings;
  }


  public static List<String> doubleToStrList(double[] doubles, NF format) {
    List<String> strings = new ArrayList<>();
    if (doubles != null && doubles.length > 0) {
      strings = Arrays.stream(doubles)
          .mapToObj(d -> doubleToString(d, format))
          .collect(Collectors.toList());
    }
    return strings;
  }

  public static List<String> doubleToStrList(List<Double> doubles, NF format) {
    List<String> strings = new ArrayList<>();
    if (!doubles.isEmpty()) {
      strings = doubles.stream()
          .map(d -> doubleToString(d, format))
          .collect(Collectors.toList());
    }
    return strings;
  }

  public static String[] doubleToStrArr(List<Double> doubles, NF format) {
    String[] strings = new String[doubles.size()];
    if (!doubles.isEmpty()) {
      for (int i = 0; i < doubles.size(); i++) {
        strings[i] = doubleToString(doubles.get(i), format);
      }
    }
    return strings;
  }

  public static List<String> intToStrList(int[] integers) {
    List<String> strings = new ArrayList<>();
    if (integers.length > 0) {
      strings = Arrays.stream(integers)
          .mapToObj(Integer::toString)
          .collect(Collectors.toList());
    }
    return strings;
  }

  public static List<String> intToStrList(List<Integer> integers, NF format) {
    List<String> strings = new ArrayList<>();
    if (!integers.isEmpty()) {
      strings = integers.stream()
          .map(d -> intToString(d, format))
          .collect(Collectors.toList());
    }
    return strings;
  }

  // ###################################### General ###############################

  public static String removeSpecialChars(String string) {
    String outString = string.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    return outString;
  }

  public static String removeLastChar(String s) {
    return (s == null || s.length() == 0) ? null : (s.substring(0, s.length() - 1));
  }

  public static String removeTrailingZeros(String string) {
    String lastChar = string.substring(string.length() - 1);
    while (lastChar.equals("0")) {
      string = removeLastChar(string);
      lastChar = string.substring(string.length() - 1);
    }
    return string;
  }

  // ###############################################################################

  // get digit pattern e.g. to extract the mz value
  @Nonnull
  public static Pattern getDigitOnlyPattern() {
    return Pattern.compile("[^0-9]");
  }

  @Nonnull
  public static Pattern getDecimalDigitPattern() {
    return Pattern.compile("[^0-9.]");
  }

  // get alphabet pattern e.g. to extract the element name
  @Nonnull
  public static Pattern getAlphabetOnlyPattern() {
    return Pattern.compile("[^A-Za-z]+");
  }

  public static String getLettersOnly(String str) {
    String justLetters = str.replaceAll("[^a-zA-Z]", "");
    return justLetters;
  }

  public static String getDigitsOnly(String str) {
    String justDigits = str.replaceAll("[^\\d]+", ""); // Keep only digits
    return justDigits;
  }

  public static String getDigitsAndLettersOnly(String str) {
    String cleaned = str.replaceAll("[^A-Za-z0-9]", "");
    return cleaned;
  }

}
