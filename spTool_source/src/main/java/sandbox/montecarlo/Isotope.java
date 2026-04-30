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

package sandbox.montecarlo;

import core.SpTool3Main;
import dataModelNew.mz.Element;
import gui.dialog.FillCollection;
import gui.dialog.Fillable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import util.SnF;

// Class representing an Isotope
public class Isotope implements Fillable<Isotope>, FillCollection<Isotope>, Comparable<Isotope>,
    Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(Isotope.class.getName());

  private final Element element;
  private final int isotopicNumber;
  private final double abundance;
  private final double exactMass;

  public Isotope(Element element, int isotopicNumber, double exactMass, double abundance) {
    this.element = element;
    this.isotopicNumber = isotopicNumber;
    this.exactMass = exactMass;
    this.abundance = abundance;
  }

  public int getIsotopicNumber() {
    return isotopicNumber;
  }

  public Element getElement() {
    return element;
  }

  public double getTheoreticalMass() {
    return exactMass;
  }

  public double getAbundance() {
    return abundance;
  }

  public String getName() {
    return isotopicNumber + element.getSymbol();
  }

  // Do not change the labelling here!!! These are constants to find the isotopes in xml files!
  public String getXMLCode() {
    return getElement().getLongName() + "("
        + getIsotopicNumber() + getElement().getSymbol() + ")";
  }

  // "Iron: 56Fe" --> If you change this, also change the reversion function!!!
  public String getFullUIName() {
    return getElement().getLongName() + ": "
        + getIsotopicNumber() + getElement().getSymbol();
  }

  // parses "Iron: 56Fe"
  public static Isotope getFromFullUIName(String symbol) {
    Isotope isotope = Element.UNKNOWN.getMostAbundant();
    if (symbol != null) {

      //split(":\\s*") splits on colon : followed by any amount of whitespace.

      String[] parts = symbol.split(":\\s*");

      if (parts.length == 2) {
        String elementStr = parts[0];      // "Iron"
        String isotopeStr = parts[1];      // "56Fe"

        Element element = null;
        if (!elementStr.isEmpty()) {
          for (Element value : Element.values()) {
            if (value.getLongName().equals(elementStr)) {
              element = value;
              break;
            }
          }
        }

        String justDigits = SnF.getDigitsOnly(isotopeStr);
        if (element != null) {
          for (Isotope iso : element.getIsotopes()) {
            String isoNumber = Integer.toString(iso.isotopicNumber);
            if (isoNumber.equals(justDigits)) {
              isotope = iso;
            }
          }
        }
      }
    }
    return isotope;
  }

  // TODO: at some point we may want to allow identification by precision comparison!
  //  @Nullable
  //  public static Isotope getFromDouble(double exactMass) {
  //  }

  public static List<Isotope> getFromNominalMass(int nominal) {
    List<Isotope> matches = new ArrayList<>();
    for (Isotope isotope : Element.getAllIsotopes()) {
      if (isotope.getIsotopicNumber() == nominal) {
        matches.add(isotope);
      }
    }
    return matches;
  }

  public static Isotope guessFromString(String symbol) {
    Isotope isotope = Element.UNKNOWN.getMostAbundant();
    if (symbol != null) {

      symbol = SnF.getDigitsAndLettersOnly(symbol);

      // (1) match with element
      // Remove potential numbers and special characters, convert to lower case
      String justLetters = SnF.getLettersOnly(symbol);
      justLetters = justLetters.toLowerCase(Locale.ROOT);

      Element element = null;

      if (!justLetters.isEmpty()) {
        for (Element value : Element.values()) {
          // Au
          if (value.getShortName().toLowerCase(Locale.ROOT).equals(justLetters)) {
            element = value;
            break;
            // Gold
          } else if (value.getLongName().toLowerCase(Locale.ROOT).equals(justLetters)) {
            element = value;
            break;
          }
        }
      }

      // (2) identify isotope
      String justDigits = SnF.getDigitsOnly(symbol);
      if (!justDigits.isEmpty()) {
        if (element != null) {
          for (Isotope iso : element.getIsotopes()) {
            String isoNumber = Integer.toString(iso.isotopicNumber);
            if (isoNumber.equals(justDigits)) {
              isotope = iso;
            }
          }
        } else {
          // we just have an isotopic number and not an element
          if (SnF.isValidInt(symbol)) {
            int isoNumber = SnF.strToInt(symbol);
            isotope = SpTool3Main.getRunTime().getConfParams().resolveConflictOrGet(isoNumber);
            LOGGER.trace("Had to read/guess ELEMENT from isotope conflict list"
                + "list as just an isotopic number was passed: " + symbol);
          } else {
            int isoNumber = SnF.strToInt(justDigits);
            isotope = SpTool3Main.getRunTime().getConfParams().resolveConflictOrGet(isoNumber);
            LOGGER.trace("Had to read/guess ELEMENT from isotope conflict list"
                + "list as just an isotopic number was passed: " + isoNumber);
          }
        }
      } else if (element != null) {
        isotope = SpTool3Main.getRunTime().getConfParams().getDefaultIsotope(element);
        LOGGER.trace("Had to read/guess ISOTOPE from element using the default isotope "
            + "list as just an element was passed: " + element.getSymbol());
      }
    }

    return isotope;
  }

  public boolean isValid() {
    return getElement() != Element.UNKNOWN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Isotope isotope = (Isotope) o;
    return isotopicNumber == isotope.isotopicNumber
        && Double.compare(isotope.abundance, abundance) == 0
        && Double.compare(isotope.exactMass, exactMass) == 0
        && element == isotope.element;
  }

  @Override
  public int hashCode() {
    return Objects.hash(element, isotopicNumber, exactMass, abundance);
  }

//


  @Override
  public List<Fillable<Isotope>> getItems() {
    return new ArrayList<>(Element.getAllIsotopes());
  }

  @org.jetbrains.annotations.Nullable
  @Override
  public Fillable<Isotope> getMatch(String string, boolean muteError) {
    Isotope match = null;
    boolean parserFailed = true;
    for (Fillable<Isotope> item : getItems()) {
      if (string.equals(item.getStringValue())) {
        match = (Isotope) item; // Element implements Fillable<Element> :-
        parserFailed = false;
        break;
      }
    }
    if (!muteError && parserFailed) {
      LOGGER.debug("Unable to parse AutoFillable instance for class Element. "
          + "Input string was: '" + string + "'.");
    }
    return match;
  }

  @Override
  public String getStringValue() {
    return isotopicNumber + element.getShortName()
        + " (" + element.getLongName() + ")";
  }

  @Override
  public boolean isEqual(Fillable<?> thatFillable) {
    return this.getStringValue().equals(thatFillable.getStringValue());
  }

  @Override
  public Isotope unwrap() {
    return this;
  }

  // Needed for the fillable
  @Override
  public int compareTo(@NotNull Isotope o) {
    return Integer.compare(isotopicNumber, o.isotopicNumber);
  }
}
