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

package io;

import com.google.common.math.DoubleMath;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import dataModelNew.mz.Element;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;
import dataModelNew.mz.TOFmz;
import dataModelNew.mz.TQmz;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Locale;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.CharSet;
import processing.options.Delimiter;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;

public class FileInterpreterUtils {

  private static final Logger LOGGER = LogManager.getLogger(FileInterpreterUtils.class);

  public static CSVReader buildReader(Path file, Delimiter delimiter, CharSet charSet)
      throws IOException, CsvException {
    return buildReader(file, delimiter.getDelimiter(), charSet);
  }

  public static CSVReader buildReader(Path file, Character delimiter, CharSet charSet)
      throws IOException, CsvException {

    // CSV reader: does not throw exceptions
    CSVParser csvParser = new CSVParserBuilder().withSeparator(delimiter).build();
    // CSV reader: throw exceptions
    FileInputStream fileInputStream = new FileInputStream(file.toFile());
    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
    InputStreamReader inputStreamReader = new InputStreamReader(bufferedInputStream,
        charSet.getCharSet());

    // CSV reader: does not throw exceptions
    Reader reader = new BufferedReader(inputStreamReader);
    CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(0).withCSVParser(csvParser)
        .build();

    return csvReader;
  }

  public static boolean arrHasKeyword(String[] line, String keyword) {
    boolean hasKey = false;
    loopArray:
    for (int i = 0; i < line.length; i++) {
      hasKey = arrHasKeyword(line, keyword, i);
      if (hasKey) {
        break loopArray;
      }
    }
    return hasKey;
  }

  public static boolean arrHasKeyword(String[] line, String keyword, int atIndex) {
    if (line != null) {
      return (!SnF.isValidDouble(line[atIndex]) && line[atIndex].contains(keyword));
    } else {
      return false;
    }
  }


  // iCAP in TQ mode writes mz as "56Fe | 56Fe.16O" OR "24Mg | 24Mg"--> parse this!
  public static Pair<Boolean, MZValue> parseIcapTQmz(String rawMzField) {
    // in some exports, e.g., form QTegra, there seem to be cases where it says "Intensity (cps) 197Au"

    if (rawMzField.toLowerCase(Locale.ROOT).contains("intensity")) {
      rawMzField = rawMzField.substring(rawMzField.indexOf(')') + 1).trim();
    }

    // "|" indicates TQ data
    Pair<Boolean, MZValue> result;
    if (rawMzField.contains("|")) {
      // cut out first mz (Q1)
      String q3Mass = rawMzField.substring(rawMzField.indexOf("|"));

      // TQ has mass shift
      if (rawMzField.contains(".")) {
        String firstMass = q3Mass.substring(0, q3Mass.indexOf("."));
        String massShift = q3Mass.substring(q3Mass.indexOf("."));

        String firstExtractedMZ = SnF.getDigitsOnly(firstMass);
        String massShiftMZ = SnF.getDigitsOnly(massShift);

        double mz = SnF.strToDouble(firstExtractedMZ);
        double shift = SnF.strToDouble(massShiftMZ);
        String elementLetter = SnF.getLettersOnly(firstMass);

        Pair<Boolean, MZValue> q1 = safelyGetMZ1(elementLetter, mz);
        result = new Pair<>(q1.getKey(), new TQmz(mz, mz + shift, elementLetter));
        // TQ is on mass
      } else {
        String q3ExtractedMZ = q3Mass.trim();
        result = safelyGetMZ1(q3ExtractedMZ);
      }
    } else {
      result = safelyGetMZ1(rawMzField.trim());
    }
    return result;
  }

  public static double getIcapDeclaredDwellTimeValue(String input) {
    double dt = -1;

    if (input != null && input.toLowerCase(Locale.ROOT).contains("dwell time")) {
      int index = input.indexOf("time=");
      if (index != -1) {
        int start = index + "time=".length();
        int end = input.indexOf(';', start);
        if (end == -1) {
          end = input.length();  // in case there's no semicolon
        }
        String dtCandidate = input.substring(start, end).trim();
        if (SnF.isValidDoubleSilent(dtCandidate)) {
          dt = SnF.strToDouble(dtCandidate);
        }
      }
    }

    return dt;
  }


  public static String parseICapLASampleName(String rawName) {
    // AuNP60_0p05TX114_2p1e5NPmL_01:02/02/2021 01:48:07 AM;
    // read as: AuNP60_0p05TX114_2p1e5NPmL_01:02/02/2021 01:48:07 AM;
    // --> return AuNP60_0p05TX114_2p1e5NPmL_01

    String parsedName = "";
    try {
      parsedName = rawName.substring(0, rawName.indexOf(":"));
    } catch (StringIndexOutOfBoundsException stringIndexOutOfBoundsException) {
      LOGGER.info("Could not parse iCAP sample name assuming 'laser reduction export' format."
          + " This indicates that the csv was not read successfully."
          + " Please consider checking the character encoding.");
    }
    return parsedName;
  }


  public static MZValue parseAnalytikJenaSQmz(String rawMzField) {
    String extractedMZ = SnF.getDigitsOnly(rawMzField);
    return new SQmz(extractedMZ);
  }

  public static MZValue parseAgilentTQmz(String rawMzField) throws IndexOutOfBoundsException {
    // "->" indicates TQ data, cf: "Fe56 -> 72" or "Fe55 -> 55"
    MZValue mzValue;
    if (rawMzField.contains("->")) {
      String firstMass = rawMzField.substring(0, rawMzField.indexOf("-"));
      String secondMass = rawMzField.substring(rawMzField.indexOf(">"));

      String firstExtractedMZ = SnF.getDigitOnlyPattern().matcher(firstMass).replaceAll("");
      String secondExtractedMZ = SnF.getDigitOnlyPattern().matcher(secondMass).replaceAll("");
      double fistMassValue = SnF.strToDouble(firstExtractedMZ);
      double secondMassValue = SnF.strToDouble(secondExtractedMZ);
      // TQ has mass shift?
      if (!DoubleMath.fuzzyEquals(fistMassValue, secondMassValue, MZValue.EPSILON)) {
        mzValue = new TQmz(firstExtractedMZ, secondMassValue);
        // TQ is on mass
      } else {
        mzValue = new SQmz(firstExtractedMZ);
      }
    } else {
      String cleanStr = SnF.getDigitsAndLettersOnly(rawMzField);
      mzValue = new SQmz(cleanStr);
    }
    return mzValue;
  }

  public static Pair<Boolean, MZValue> safelyGetMZ1(String elementAndNumber) {
    boolean isValid = true;
    Isotope candidate = Isotope.getFromString(elementAndNumber);
    if (candidate == null) {
      candidate = Element.UNKNOWN.getMostAbundant();
      isValid = false;
    }
    return new Pair<>(isValid, new SQmz(candidate));
  }


  public static Pair<Boolean, MZValue> safelyGetMZ1(String element, double mz) {
    return safelyGetMZ1(SnF.doubleToString(mz, NF.D1C0) + element);
  }


  // Tofwerk writes mz as "[195Pt]+ mass 194.964" --> parse this!
  public static MZValue parseTOFmz(String rawMZfield) throws IndexOutOfBoundsException {
    int idxBracketStart = rawMZfield.indexOf("[");
    int idxBracketEnd = rawMZfield.indexOf("]");
    String elementBracket = rawMZfield.substring(idxBracketStart + 1, idxBracketEnd);
    String element = SnF.getAlphabetOnlyPattern().matcher(elementBracket).replaceAll("");
    String massInformation = rawMZfield.substring(rawMZfield.indexOf("+"));

    String nominalMass = SnF.getDigitOnlyPattern().matcher(elementBracket).replaceAll("");
    String theoreticalMass = SnF.getDecimalDigitPattern().matcher(massInformation).replaceAll("");

    double nominal = SnF.strToDouble(nominalMass);
    double theoretical = SnF.strToDouble(theoreticalMass);
    return new TOFmz(nominal, theoretical, element);
  }


}
