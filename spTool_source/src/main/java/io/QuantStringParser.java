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

import dataModelNew.Sample;
import math.units.enums.ConcentrationUnit;
import math.units.enums.FlowUnit;
import math.units.enums.SizeUnit;
import processing.options.CalibratorRole;
import processing.options.SampleType;
import processing.parameterSets.impl.ExperimentalSubConditions;

import java.util.Locale;
import java.util.Objects;

public class QuantStringParser {

  public static void parseIntoSample(String input, Sample sample) {
    if (input != null && sample != null) {

      String[] parts = input.split("_");

      for (String part : parts) {
        if (part.isEmpty()) continue;

        // continue as we may have several matches per sample
        if (tryParseBlock(part, sample)) {
          //          return; // stop after first valid block
        }
      }
    }
  }

  private static boolean tryParseBlock(String block, Sample sample) {

    boolean valid = true;

    int len = block.length();
    int index = 0;

    String prefix = "";
    String unit = "";
    double value = 0.0;

    StringBuilder prefixBuilder = new StringBuilder();
    StringBuilder numberBuilder = new StringBuilder();

    // 1. Parse prefix (letters before number)
    while (index < len && Character.isLetter(block.charAt(index))) {
      prefixBuilder.append(block.charAt(index));
      index++;
    }

    if (index >= len || !Character.isDigit(block.charAt(index))) {
      valid = false;
    }

    // 2. Integer part
    if (valid) {
      while (index < len && Character.isDigit(block.charAt(index))) {
        numberBuilder.append(block.charAt(index));
        index++;
      }
    }

    // 3. Optional decimal via 'p' (only if followed by digit)
    if (valid && index + 1 < len
        && block.charAt(index) == 'p'
        && Character.isDigit(block.charAt(index + 1))) {

      numberBuilder.append('.');
      index++;

      while (index < len && Character.isDigit(block.charAt(index))) {
        numberBuilder.append(block.charAt(index));
        index++;
      }
    }

    // 4. Optional scientific exponent
    if (valid && index < len
        && (block.charAt(index) == 'E' || block.charAt(index) == 'e')) {

      numberBuilder.append('E');
      index++;

      // optional sign
      if (index < len &&
          (block.charAt(index) == '+' || block.charAt(index) == '-')) {
        numberBuilder.append(block.charAt(index));
        index++;
      }

      // must have at least one digit
      if (index >= len || !Character.isDigit(block.charAt(index))) {
        valid = false;
      } else {
        while (index < len && Character.isDigit(block.charAt(index))) {
          numberBuilder.append(block.charAt(index));
          index++;
        }
      }
    }

    // 5. Parse numeric value
    if (valid) {
      try {
        value = Double.parseDouble(numberBuilder.toString());
      } catch (NumberFormatException e) {
        valid = false;
      }
    }

    // 6. Unit = remainder
    if (valid) {
      if (index < len) {
        unit = block.substring(index);
      } else {
        valid = false;
      }
    }

    // 7. Apply rules
    if (valid) {

      prefix = prefixBuilder.toString();
      String prefixLower = prefix.toLowerCase(Locale.ROOT);
      String unitLower = unit.toLowerCase(Locale.ROOT);

      var quant = sample.getQuant();
      var conditions = quant.getExperimentalConditions();
      var subConditionsMap = conditions.getElementSpecificQuantParams();

      // ---- PREFIX RULES ----
      switch (prefixLower) {

        case "d":
        case "n":
          conditions.getCalibratorRole().setValue(CalibratorRole.CALIBRATOR);
          conditions.getSampleType().setValue(SampleType.PARTICLE);
          break;

        case "c":
          conditions.getCalibratorRole().setValue(CalibratorRole.CALIBRATOR);
          conditions.getSampleType().setValue(SampleType.IONIC);
          break;

        case "q":
          conditions.getCalibratorRole().setValue(CalibratorRole.CALIBRATOR);
          conditions.getSampleIntroductionFlowRate().setValue(value);
          break;
      }

      // ---- FLOW UNITS ----
      switch (unitLower) {
        case "ulmin":
        case "µlmin":
          conditions.setSampleIntroductionFlowRate(FlowUnit.MICROLITRE_PER_MINUTE);
          break;

        case "mlmin":
          conditions.setSampleIntroductionFlowRate(FlowUnit.MILLILITRE_PER_MINUTE);
          break;
      }

      // ---- SUB-CONDITION UNITS ----
      if (!subConditionsMap.isEmpty()) {

        switch (unitLower) {

          case "um":
          case "µm":
            for (ExperimentalSubConditions esc : subConditionsMap.values()) {
              esc.setNpSphericalDiameterUnit(SizeUnit.MICRO_METER);
              esc.getNpSphericalDiameter().setValue(value);
            }
            break;

          case "nm":
            for (ExperimentalSubConditions esc : subConditionsMap.values()) {
              esc.setNpSphericalDiameterUnit(SizeUnit.NANO_METER);
              esc.getNpSphericalDiameter().setValue(value);
            }
            break;

          case "ppb":
            for (ExperimentalSubConditions esc : subConditionsMap.values()) {
              esc.setIonicConcentration(ConcentrationUnit.MICROGRAM_PER_LITRE);
              esc.getIonicConcentration().setValue(value);
            }
            break;

          case "ppm":
            for (ExperimentalSubConditions esc : subConditionsMap.values()) {
              esc.setIonicConcentration(ConcentrationUnit.MILLIGRAM_PER_LITRE);
              esc.getIonicConcentration().setValue(value);
            }
            break;

          case "npml":
            for (ExperimentalSubConditions esc : subConditionsMap.values()) {
              esc.setNpConcentrationUnit(ConcentrationUnit.NP_PER_MILLILITRE);
              esc.getNpConcentration().setValue(value);
            }
            break;
        }
      }
    }
    return valid;
  }

}
