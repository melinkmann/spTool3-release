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

import dataModelNew.mz.Element;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;


import java.util.Map;


public class ChemistryUtils {

  private static final Logger LOGGER = LogManager.getLogger(ChemistryUtils.class);

  /**
   * Literal code by chatGPT. Seems to work. Alternative below using cdk.
   */
  public static double massFraction(String formula, String elementSymbol) {
    Map<String, Integer> composition = ChemicalFormulaParser.parse(formula);

    double totalMass = 0.0;
    double elementMass = 0.0;

    for (Map.Entry<String, Integer> entry : composition.entrySet()) {
      String symbol = entry.getKey();
      int count = entry.getValue();

      Element element = Element.valueOf(symbol);
      double molarMass = element.calcMolarMass();

      double massContribution = count * molarMass;
      totalMass += massContribution;

      if (symbol.equals(elementSymbol)) {
        elementMass += massContribution;
      }
    }

    if (totalMass == 0) {
      throw new IllegalArgumentException("Total mass is zero for formula: " + formula);
    }

    return elementMass / totalMass;
  }

  public static double massFractionCDK(String formulaString, String elementSymbol) {

    double frac = 1;

    try {
      IMolecularFormula formula =
          MolecularFormulaManipulator.getMolecularFormula(
              formulaString,
              DefaultChemObjectBuilder.getInstance()
          );

      StringBuilder builder = new StringBuilder("Parse formula contains: ");

      for (IIsotope isotope : formula.isotopes()) {
        int count = formula.getIsotopeCount(isotope); // get the number of atoms
        builder.append(isotope.getSymbol());
        if (count > 1) { // only show number if >1
          builder.append(count);
        }
        builder.append(" ");
      }
      LOGGER.trace(builder.toString());


      IMolecularFormula elementAsFormula =
          MolecularFormulaManipulator.getMolecularFormula(
              elementSymbol,
              DefaultChemObjectBuilder.getInstance()
          );

      double totalMass = MolecularFormulaManipulator.getMass(
          formula, MolecularFormulaManipulator.MolWeight);

      int count = MolecularFormulaManipulator.getElementCount(formula, elementSymbol);

      double elementMass = count * MolecularFormulaManipulator.getMass(
          elementAsFormula, MolecularFormulaManipulator.MolWeight);

      if (totalMass <= 0) {
        LOGGER.error("Cannot compute mass fraction from sum formula. " +
            "Attempted to calculate molecular mass but it was zero. Returned frac=1.");
      } else {
        frac = elementMass / totalMass;
      }


    } catch (Exception e) {
      LOGGER.error("Cannot compute mass fraction from sum formula! Returned frac=1." +
          " Message: " + ExceptionUtils.getMessage(e) +
          " Stack trace: " + ExceptionUtils.getStackTrace(e));
    }
    return frac;
  }
}