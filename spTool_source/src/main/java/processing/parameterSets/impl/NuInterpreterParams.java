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

package processing.parameterSets.impl;

import core.SpTool3Main;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import io.nu.IsotopePtoeDialog;
import javafx.stage.Window;
import math.stat.MeasureOfLocation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.*;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.*;
import sandbox.montecarlo.Isotope;
import util.Functional;
import util.NF;

import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NuInterpreterParams extends AbstractParamSet implements Serializable, ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "NuInterpreterParams";

  // Parameter

  private final Parameter<TofIsotopeOption> isotopeSelectionStrategy;
  private final Parameter<String> selectedIsotopes;

  private final Parameter<MeasureOfLocation> preScreenMeasureOfLocation;
  private final Parameter<IsotopeConflictReaderOption> preScreenConflictResolution;
  private final Parameter<Double> preScreenAlpha;
  private final Parameter<Double> preScreenSiaShape;
  private final Parameter<Integer> preScreenDataPoints;
  private final Parameter<Boolean> savePreScreenResultToMethod;


  public NuInterpreterParams() {
    this("Nu import parameters");
  }

  public NuInterpreterParams(String label) {
    super(label, XML_ELEMENT_TAG);

    this.isotopeSelectionStrategy = new ComboEnumParameter<>("Isotope selection",
        """
            Decide how to select isotopes
            """,
        TofIsotopeOption.THRESHOLD,
        TofIsotopeOption.values(),
        TofIsotopeOption.class,
        false,
        "isotopeSelectionStrategy"
    );

    this.selectedIsotopes = new FeedbackStringParameter(
        "Default isotopes",
        """
            Use the button on the right to define default list of isotopes.
            The text field is just a dummy to show feedback and it is uneditable on purpose.
            Use the button on the right to change selection.
            """,
        "",
        TextFormatterOption.ALL_PASS,
        false,
        "selectedIsotopes"
    );

    this.preScreenMeasureOfLocation = new ComboEnumParameter<>(
        "Measure of location",
        """
            Measure of location for thresholding""",
        MeasureOfLocation.MEAN,
        MeasureOfLocation.baseline(),
        MeasureOfLocation.class,
        false,
        "preScreenMeasureOfLocation"
    );

    this.preScreenConflictResolution = new ComboEnumParameter<>(
        "Isobaric",
        """
            When nominal masses of isotopes overlap:
            Either load the default option defined in the configuration
            or load all potential candidates""",
        IsotopeConflictReaderOption.ALL,
        IsotopeConflictReaderOption.values(),
        IsotopeConflictReaderOption.class,
        false,
        "preScreenConflictResolution"
    );

    preScreenAlpha = new DoubleParameter(
        "Alpha",
        """
            Significance threshold for the isotope pre-screening
            """,
        1E-6,
        NF.D1C3Exp,
        TextFormatterOption.ASSURE_NONZERO_POS_EXP_DOUBLE,
        false,
        "preScreenAlpha");

    preScreenSiaShape = new DoubleParameter(
        "SIA",
        """
            SIA shape parameter for the isotope pre-screening
            """,
        0.47,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "preScreenSiaShape");

    preScreenDataPoints = new IntegerParameter(
        "Min points",
        """
            There must be at least this number of data points above the threshold 
            to import the isotope""",
        50,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "preScreenDataPoints");

    this.savePreScreenResultToMethod = new BooleanParameter(
        "Result",
        "Save in method",
        """
            Store the result of the threshold-based prescreening
            in the list of selected isotopes 'Select' option""",
        true,
        false,
        "savePreScreenResultToMethod"
    );

    organize();
  }

  // Copy
  public NuInterpreterParams(NuInterpreterParams nuInterpreterParams) {
    super(nuInterpreterParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(nuInterpreterParams.getCommentParameter());

    this.isotopeSelectionStrategy = nuInterpreterParams.isotopeSelectionStrategy.copyWithoutChildren();
    this.selectedIsotopes = nuInterpreterParams.selectedIsotopes.copyWithoutChildren();
    this.preScreenConflictResolution = nuInterpreterParams.preScreenConflictResolution.copyWithoutChildren();
    this.preScreenMeasureOfLocation = nuInterpreterParams.preScreenMeasureOfLocation.copyWithoutChildren();
    this.preScreenAlpha = nuInterpreterParams.preScreenAlpha.copyWithoutChildren();
    this.preScreenSiaShape = nuInterpreterParams.preScreenSiaShape.copyWithoutChildren();
    this.preScreenDataPoints = nuInterpreterParams.preScreenDataPoints.copyWithoutChildren();
    this.savePreScreenResultToMethod = nuInterpreterParams.savePreScreenResultToMethod.copyWithoutChildren();


    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new NuInterpreterParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new NuInterpreterParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new NuInterpreterParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Add all PARENT (not the depending) parameters!
    super.setParentParameters(
        isotopeSelectionStrategy
    );

    isotopeSelectionStrategy.addConditionalChild(TofIsotopeOption.DEFAULTS, selectedIsotopes);

    isotopeSelectionStrategy.addConditionalChild(TofIsotopeOption.THRESHOLD,
        preScreenDataPoints,
        preScreenMeasureOfLocation,
        preScreenAlpha,
        preScreenSiaShape,
        preScreenConflictResolution,
        savePreScreenResultToMethod);

    selectedIsotopes.setDecoration(new ParamSetterButtonDecoration<>("Select isotopes", "/img/tableTrace.png",
        new Functional() {

          @Override
          public void proceed() {
            proceed(null);
          }

          @Override
          public void proceed(Window window) {
            List<Isotope> prevSel = isotopeFromString(selectedIsotopes.getValue());

            IsotopePtoeDialog dlg = IsotopePtoeDialog.forIsotopeSelection(
                window,
                dataModelNew.mz.Element.getAllIsotopes(),   // all isotopes available
                prevSel);                  // null or empty = open blank

            List<MZValue> resultingMZ = dlg.showAndWait();
            if (resultingMZ != null) {
              List<Isotope> resultingIsotopes = new ArrayList<>();
              for (MZValue mzValue : resultingMZ) {
                resultingIsotopes.add(mzValue.getIsotope());
              }
              selectedIsotopes.setValue(isotopesToString(resultingIsotopes));
            }
          }
        }));
  }


  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.NU_READER;
  }

  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int temp = 0; temp < nodeList.getLength(); temp++) {
      Node node = nodeList.item(temp);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = switch (element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE)) {
          case LABEL_PAR_XML_ID -> super.label;
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "selectedIsotopes" -> selectedIsotopes;
          case "isotopeSelectionStrategy" -> isotopeSelectionStrategy;
          case "preScreenMeasureOfLocation" -> preScreenMeasureOfLocation;
          case "preScreenAlpha" -> preScreenAlpha;
          case "preScreenConflictResolution" -> preScreenConflictResolution;
          case "preScreenSiaShape" -> preScreenSiaShape;
          case "preScreenDataPoints" -> preScreenDataPoints;
          case "savePreScreenResultToMethod" -> savePreScreenResultToMethod;

          default -> null;
        };

        if (par != null) {
          par.readFromXmlElement(element);
        }
      }
    }
  }

  @Override
  public void executeSaveAs(Path file) {
    super.setAssociatedFileOnDrive(file);
    XmlUtil.writeToXml(this, file);
  }


  /// ///////////////////////////////////////////////////////////////

  public Parameter<TofIsotopeOption> getIsotopeSelectionStrategy() {
    return isotopeSelectionStrategy;
  }

  public Parameter<MeasureOfLocation> getPreScreenMeasureOfLocation() {
    return preScreenMeasureOfLocation;
  }

  public Parameter<Double> getPreScreenAlpha() {
    return preScreenAlpha;
  }

  public Parameter<Double> getPreScreenSiaShape() {
    return preScreenSiaShape;
  }

  public Parameter<Integer> getPreScreenDataPoints() {
    return preScreenDataPoints;
  }

  public Parameter<IsotopeConflictReaderOption> getPreScreenConflictResolution() {
    return preScreenConflictResolution;
  }

  public List<Isotope> listDefaultIsotopes() {
    return isotopeFromString(selectedIsotopes.getValue());
  }

  public Parameter<Boolean> getSavePreScreenResultToMethod() {
    return savePreScreenResultToMethod;
  }

  public void requestSavingIsotopes(List<Isotope> isotopes) {
    if (savePreScreenResultToMethod.getValue()) {
      if (!isotopes.isEmpty()) {
        String isoStr = isotopesToString(isotopes);
        selectedIsotopes.setValue(isoStr);
      }
    }
  }

  /// ///////////////////////////////////////////////////////////////


  /**
   * Claude Sonnet 4.6: Quick and dirty read/write isotopes to/from string.
   * Parses a string produced by {@link #isotopesToString} back into isotopes.
   * Silently skips malformed or unresolvable tokens.
   */
  public static String isotopesToString(List<Isotope> isotopes) {
    if (isotopes == null || isotopes.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < isotopes.size(); i++) {
      Isotope iso = isotopes.get(i);
      sb.append("ISOTOPE[m=")
          .append(iso.getIsotopicNumber())
          .append(",e=")
          .append(iso.getElement().getShortName())
          .append("]");
      if (i < isotopes.size() - 1) {
        sb.append(";");
      }
    }
    return sb.toString();
  }

  /**
   * Claude Sonnet 4.6: Quick and dirty read/write isotopes to/from string.
   * Parses a string produced by {@link #isotopesToString} back into isotopes.
   * Silently skips malformed or unresolvable tokens.
   */
  public static List<Isotope> isotopeFromString(String stored) {
    List<Isotope> result = new ArrayList<>();
    if (stored == null || stored.isBlank()) {
      return result;
    }
    for (String token : stored.split(";")) {
      token = token.trim();
      // Expect: ISOTOPE[m=56,e=Fe]
      if (!token.startsWith("ISOTOPE[") || !token.endsWith("]")) {
        continue;
      }
      String inner = token.substring("ISOTOPE[".length(), token.length() - 1);
      // inner = "m=56,e=Fe"
      int massNumber = -1;
      String shortName = null;
      for (String field : inner.split(",")) {
        if (field.startsWith("m=")) {
          try {
            massNumber = Integer.parseInt(field.substring(2));
          } catch (NumberFormatException ignored) {
          }
        } else if (field.startsWith("e=")) {
          shortName = field.substring(2);
        }
      }
      if (massNumber < 0 || shortName == null) {
        continue;
      }
      for (Isotope iso : dataModelNew.mz.Element.getAllIsotopes()) {
        if (iso.getIsotopicNumber() == massNumber
            && iso.getElement().getShortName().equals(shortName)) {
          result.add(iso);
          break;
        }
      }
    }
    return result;
  }
}
