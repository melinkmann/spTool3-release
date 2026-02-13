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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.*;
import util.SnF;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpCalDensity {

  private static final Logger LOGGER = LogManager.getLogger(SpCalDensity.class);

  private final List<ElementContainer> elementData = new ArrayList<>();
  private final List<IsotopeContainer> isotopeData = new ArrayList<>();
  private final List<InorganicContainer> inorganicData = new ArrayList<>();
  private final List<PolymerContainer> polymerData = new ArrayList<>();

  public SpCalDensity() {
    load();
  }

  public List<Container> lookup() {
    List<Container> lookups = new ArrayList<>();
    lookups.addAll(elementData);
    lookups.addAll(inorganicData);
    lookups.addAll(polymerData);
    return lookups;
  }

  public List<Container> lookup(String lookupStr) {
    lookupStr = lookupStr.trim();
    List<Container> lookups = new ArrayList<>();

    for (Container dat : elementData) {
      for (String str : dat.getStrs()) {
        if (str.contains(lookupStr)) {
          lookups.add(dat);
        }
      }
    }

    for (Container dat : inorganicData) {
      for (String str : dat.getStrs()) {
        if (str.contains(lookupStr)) {
          lookups.add(dat);
        }
      }
    }

    for (Container dat : polymerData) {
      for (String str : dat.getStrs()) {
        if (str.contains(lookupStr)) {
          lookups.add(dat);
        }
      }
    }

    return lookups;
  }


  public static interface Container {
    List<String> getStrs();

    default String getStr() {
      StringBuilder builder = new StringBuilder();
      int columnWidth = 50; // adjust based on expected content length
      for (String str : getStrs()) {
        builder.append(str);
        int chars = Math.max(5, columnWidth - str.length());
        for (int i = 0; i < chars; i++) {
          builder.append(" ");
        }
      }
      return builder.toString();
    }

    double getDensity();
  }

  public static class ElementContainer implements Container {
    private final double atomicNumber;
    private final String abbreviation;
    private final String fullName;
    private final double molecularWeight;
    private final double density;

    public ElementContainer(double atomicNumber, String abbreviation, String fullName,
                            double molecularWeight, double density) {
      this.atomicNumber = atomicNumber;
      this.abbreviation = abbreviation;
      this.fullName = fullName;
      this.molecularWeight = molecularWeight;
      this.density = density;
    }

    @Override
    public List<String> getStrs() {
      return List.of(abbreviation, fullName);
    }

    @Override
    public double getDensity() {
      return density;
    }
  }

  public static class IsotopeContainer implements Container {
    private final double atomicNumber;
    private final String abbreviation;
    private final double isotopicNumber;
    private final double exactMass;
    private final double abundance;
    private final boolean isDefault;

    public IsotopeContainer(double atomicNumber, String abbreviation, double isotopicNumber,
                            double exactMass, double abundance,
                            boolean isDefault) {
      this.atomicNumber = atomicNumber;
      this.abbreviation = abbreviation;
      this.isotopicNumber = isotopicNumber;
      this.exactMass = exactMass;
      this.abundance = abundance;
      this.isDefault = isDefault;
    }

    @Override
    public List<String> getStrs() {
      return List.of(abbreviation);
    }

    @Override
    public double getDensity() {
      return 0d;
    }
  }


  public static class InorganicContainer implements Container {
    private final String sumFormula;
    private final String fullName;
    private final String casNumber;
    private final double density;

    public InorganicContainer(String sumFormula, String fullName, String casNumber, double density) {
      this.sumFormula = sumFormula;
      this.fullName = fullName;
      this.casNumber = casNumber;
      this.density = density;
    }

    @Override
    public List<String> getStrs() {
      return List.of(sumFormula, fullName, casNumber);
    }

    @Override
    public double getDensity() {
      return density;
    }
  }

  public static class PolymerContainer implements Container {
    private final String sumFormula;
    private final String fullName;
    private final String casNumber;
    private final double density;

    public PolymerContainer(String sumFormula, String fullName, String casNumber, double density) {
      this.sumFormula = sumFormula;
      this.fullName = fullName;
      this.casNumber = casNumber;
      this.density = density;
    }

    @Override
    public List<String> getStrs() {
      return List.of(sumFormula, fullName, casNumber);
    }

    @Override
    public double getDensity() {
      return density;
    }
  }


  public void load() {
    try {
      // Load XML from classpath
      InputStream is = SpCalDensity.class.getResourceAsStream("/spcal_database.xml");
      if (is == null) {
        throw new RuntimeException("Could not database.xml in classpath");
      }

      // Parse XML from InputStream
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(is);

      doc.getDocumentElement().normalize();
      // System.out.println("Root element: " + doc.getDocumentElement().getNodeName());

      // Read elements
      NodeList elementList = doc.getElementsByTagName("elements").item(0).getChildNodes();
      // System.out.println("=== Elements ===");
      for (int i = 0; i < elementList.getLength(); i++) {
        Node node = elementList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element e = (Element) node;
          // System.out.println(
          //     "Atomic #: " + e.getElementsByTagName("atomicNumber").item(0).getTextContent() +
          //         ", Abbr: " + e.getElementsByTagName("abbreviation").item(0).getTextContent() +
          //         ", Name: " + e.getElementsByTagName("fullName").item(0).getTextContent() +
          //         ", MW: " + e.getElementsByTagName("MW").item(0).getTextContent() +
          //         ", Density: " + e.getElementsByTagName("density").item(0).getTextContent()
          // );
          elementData.add(new ElementContainer(
              SnF.strToDoubleSilent(e.getElementsByTagName("atomicNumber").item(0).getTextContent()),
              e.getElementsByTagName("abbreviation").item(0).getTextContent(),
              e.getElementsByTagName("fullName").item(0).getTextContent(),
              SnF.strToDoubleSilent(e.getElementsByTagName("MW").item(0).getTextContent()),
              SnF.strToDoubleSilent(e.getElementsByTagName("density").item(0).getTextContent())
          ));
        }
      }

      // Read isotopes
      NodeList isotopeList = doc.getElementsByTagName("isotopes").item(0).getChildNodes();
      // System.out.println("\n=== Isotopes ===");
      for (int i = 0; i < isotopeList.getLength(); i++) {
        Node node = isotopeList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element e = (Element) node;
          // System.out.println(
          //     "Atomic #: " + e.getElementsByTagName("atomicNumber").item(0).getTextContent() +
          //         ", Abbr: " + e.getElementsByTagName("abbreviation").item(0).getTextContent() +
          //         ", Isotopic #: " + e.getElementsByTagName("isotopicNumber").item(0).getTextContent() +
          //         ", Exact Mass: " + e.getElementsByTagName("exactMass").item(0).getTextContent() +
          //         ", Abundance: " + e.getElementsByTagName("abundance").item(0).getTextContent() +
          //         ", Default: " + e.getElementsByTagName("default").item(0).getTextContent()
          // );
          isotopeData.add(new IsotopeContainer(
              SnF.strToDoubleSilent(e.getElementsByTagName("atomicNumber").item(0).getTextContent()),
              e.getElementsByTagName("abbreviation").item(0).getTextContent(),
              SnF.strToDoubleSilent(e.getElementsByTagName("isotopicNumber").item(0).getTextContent()),
              SnF.strToDoubleSilent(e.getElementsByTagName("exactMass").item(0).getTextContent()),
              SnF.strToDoubleSilent(e.getElementsByTagName("abundance").item(0).getTextContent()),
              Objects.equals(e.getElementsByTagName("default").item(0).getTextContent(), "1")
          ));
        }
      }

      // Read inorganic
      NodeList inorganicList = doc.getElementsByTagName("inorganic").item(0).getChildNodes();
      // System.out.println("\n=== Inorganic ===");
      for (int i = 0; i < inorganicList.getLength(); i++) {
        Node node = inorganicList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element e = (Element) node;
          // System.out.println(
          //     "Formula: " + e.getElementsByTagName("sumFormula").item(0).getTextContent() +
          //         ", Name: " + e.getElementsByTagName("fullName").item(0).getTextContent() +
          //         ", CAS: " + e.getElementsByTagName("CAS").item(0).getTextContent() +
          //         ", Density: " + e.getElementsByTagName("density").item(0).getTextContent()
          // );
          inorganicData.add(new InorganicContainer(
              e.getElementsByTagName("sumFormula").item(0).getTextContent(),
              e.getElementsByTagName("fullName").item(0).getTextContent(),
              e.getElementsByTagName("CAS").item(0).getTextContent(),
              SnF.strToDoubleSilent(e.getElementsByTagName("density").item(0).getTextContent())
          ));
        }
      }

      // Read polymer
      NodeList polymerList = doc.getElementsByTagName("polymer").item(0).getChildNodes();
      // System.out.println("\n=== Polymer ===");
      for (int i = 0; i < polymerList.getLength(); i++) {
        Node node = polymerList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element e = (Element) node;
          // System.out.println(
          //     "Formula: " + e.getElementsByTagName("sumFormula").item(0).getTextContent() +
          //         ", Name: " + e.getElementsByTagName("fullName").item(0).getTextContent() +
          //         ", CAS: " + e.getElementsByTagName("CAS").item(0).getTextContent() +
          //         ", Density: " + e.getElementsByTagName("density").item(0).getTextContent()
          // );
          polymerData.add(new PolymerContainer(
              e.getElementsByTagName("sumFormula").item(0).getTextContent(),
              e.getElementsByTagName("fullName").item(0).getTextContent(),
              e.getElementsByTagName("CAS").item(0).getTextContent(),
              SnF.strToDoubleSilent(e.getElementsByTagName("density").item(0).getTextContent())
          ));
        }
      }

    } catch (Exception e) {
      LOGGER.error("Cannot load density database: {}", ExceptionUtils.getStackTrace(e));
    }
  }
}