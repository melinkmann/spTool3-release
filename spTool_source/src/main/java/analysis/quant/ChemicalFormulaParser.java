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

package analysis.quant;

import java.util.HashMap;
import java.util.Map;

public class ChemicalFormulaParser {

  private final String formula;
  private int pos = 0;

  /**
   * Literal code by chatGPT. Seems to work. Alternative: use cdk.
   */
  public ChemicalFormulaParser(String formula) {
    this.formula = formula;
  }

  public static Map<String, Integer> parse(String formula) {
    ChemicalFormulaParser parser = new ChemicalFormulaParser(formula);
    Map<String, Integer> result = parser.parseFormula();
    if (parser.pos != formula.length()) {
      throw new IllegalArgumentException(
          "Unexpected character at position " + parser.pos);
    }
    return result;
  }

  private Map<String, Integer> parseFormula() {
    Map<String, Integer> counts = new HashMap<>();

    while (pos < formula.length()) {
      char c = formula.charAt(pos);

      if (c == '(') {
        pos++; // skip '('
        Map<String, Integer> inner = parseFormula();

        if (pos >= formula.length() || formula.charAt(pos) != ')') {
          throw new IllegalArgumentException("Missing closing parenthesis at " + pos);
        }
        pos++; // skip ')'

        int multiplier = parseNumber();
        multiplyAndMerge(counts, inner, multiplier);

      } else if (c == ')') {
        // end of this sub-formula
        break;

      } else if (Character.isUpperCase(c)) {
        String element = parseElement();
        int count = parseNumber();
        counts.merge(element, count, Integer::sum);

      } else {
        throw new IllegalArgumentException(
            "Unexpected character '" + c + "' at position " + pos);
      }
    }

    return counts;
  }

  private String parseElement() {
    StringBuilder sb = new StringBuilder();

    // first char must be uppercase
    sb.append(formula.charAt(pos++));

    // optional lowercase
    if (pos < formula.length() && Character.isLowerCase(formula.charAt(pos))) {
      sb.append(formula.charAt(pos++));
    }

    return sb.toString();
  }

  private int parseNumber() {
    int start = pos;

    while (pos < formula.length() && Character.isDigit(formula.charAt(pos))) {
      pos++;
    }

    if (start == pos) {
      return 1; // default multiplier
    }

    return Integer.parseInt(formula.substring(start, pos));
  }

  private void multiplyAndMerge(Map<String, Integer> target,
                                Map<String, Integer> source,
                                int multiplier) {
    for (Map.Entry<String, Integer> e : source.entrySet()) {
      int value = e.getValue() * multiplier;
      target.merge(e.getKey(), value, Integer::sum);
    }
  }
}
