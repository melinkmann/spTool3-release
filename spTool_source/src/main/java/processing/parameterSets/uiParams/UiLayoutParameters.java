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

package processing.parameterSets.uiParams;

import gui.dialog.notification.PopupFactory;
import gui.util.TextFormatterOption;
import io.XmlUtil;

import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameters.DoubleParameter;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;
import processing.parameters.StringParameter;
import util.NF;

/**
 * This should be a viewer class. It has 3 main principles:
 * <p>
 * 1) All options are parameters hat can be stored, i.e., this class behaves like a parameter set.
 * <p>
 * 2) There has to be UI instance, that listens to changes in the parameters and refreshes the
 * viewer.
 * <p>
 * 3) The FX class contains a viewer instance which the MainWindowController accesses when the
 * respective pane is shown.
 * <p>
 * <p>
 */


/**
 * Remember to also register a new ParameterSet in the dictionary!
 *
 * @link {@link XmlInstanceDictionary}
 */

public class UiLayoutParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  private static final Logger LOGGER = LogManager.getLogger(UiLayoutParameters.class);

  public static final String XML_ELEMENT_TAG = "UiLayoutParameters";

  private final Parameter<Double> bottomSplitDiv1;
  private final Parameter<Double> bottomSplitDiv2;
  private final Parameter<Double> bottomSplitDiv3;

  private final Parameter<Double> chartLegendSplitDiv;

  private final Parameter<Double> mainMethodEditorSplitDiv;

  private final HashMap<String, Parameter<String>> plotPopupPositions;

  private final Parameter<Integer> defaultGraphWidth;
  private final Parameter<Integer> defaultGraphHeight;

  // try default mz
  // private final List<Parameter<String>> defaultIsotopeList = new ArrayList<>();


  public UiLayoutParameters() {
    super("General UI layout parameters", XML_ELEMENT_TAG);

    this.bottomSplitDiv1 = new DoubleParameter(
        "Slider position",
        "Slider between sample sets and sample list",
        0.15,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "bottomSplitDiv1");

    this.bottomSplitDiv2 = new DoubleParameter(
        "Slider position",
        "Slider between sample list and trace list",
        0.35,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "bottomSplitDiv2");

    this.bottomSplitDiv3 = new DoubleParameter(
        "Slider position",
        "Slider between trace list and population list",
        1d,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "bottomSplitDiv3");

    this.chartLegendSplitDiv = new DoubleParameter(
        "Slider position",
        "Slider between trace list and population list",
        1d,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "chartLegendSplitDiv");

    this.mainMethodEditorSplitDiv = new DoubleParameter(
        "Slider position",
        "Slider to narrow the main method editor view",
        0.6d,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "mainMethodEditorSplitDiv");

    this.plotPopupPositions = new LinkedHashMap<>();

    plotPopupPositions.put("Method viewer", makeStringParameter("Method viewer"));
    plotPopupPositions.put("Raw data viewer", makeStringParameter("Raw data viewer"));
    plotPopupPositions.put("Multi histogram viewer", makeStringParameter("Multi histogram viewer"));
    plotPopupPositions.put("Histogram viewer", makeStringParameter("Histogram viewer"));
    plotPopupPositions.put("Scatter plot viewer", makeStringParameter("Scatter plot viewer"));
    plotPopupPositions.put("Boxplot viewer", makeStringParameter("Boxplot viewer"));
    plotPopupPositions.put("Single event viewer", makeStringParameter("Single event viewer"));
    plotPopupPositions.put("Peak profile viewer", makeStringParameter("Peak profile viewer"));
    plotPopupPositions.put("Average trend viewer", makeStringParameter("Average trend viewer"));
    plotPopupPositions.put("Table viewer", makeStringParameter("Table viewer"));
    plotPopupPositions.put("Logger viewer", makeStringParameter("Logger viewer"));

    defaultGraphWidth = new IntegerParameter(
        "Global default graph width",
        """
            Global default graph width""",
        750,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "defaultGraphWidth");

    defaultGraphHeight = new IntegerParameter(
        "Global default graph height",
        """
            Global default graph height
            """,
        500,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "defaultGraphHeight");


//    for (dataModelNew.mz.Element element : dataModelNew.mz.Element.values()) {
//      defaultIsotopeList.add(new ComboStringParameter(
//          element.getLongName(),
//          "Default isotope of the respective element",
//          element.getMostAbundant().getName(),
//          ArrUtils.stringListToArr(element.getIsotopes()
//              .stream()
//              .map(Isotope::getName)
//              .collect(Collectors.toList())),
//          "defaultIsotopeList_" + element.getSymbol()
//      ));
//    }

    organize();
  }

  public UiLayoutParameters(UiLayoutParameters params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.bottomSplitDiv1 = params.bottomSplitDiv1.copyWithoutChildren();
    this.bottomSplitDiv2 = params.bottomSplitDiv2.copyWithoutChildren();
    this.bottomSplitDiv3 = params.bottomSplitDiv3.copyWithoutChildren();
    this.chartLegendSplitDiv = params.chartLegendSplitDiv.copyWithoutChildren();
    this.mainMethodEditorSplitDiv = params.mainMethodEditorSplitDiv.copyWithoutChildren();

    this.plotPopupPositions = new LinkedHashMap<>();
    for (Parameter<String> plotPopupPosition : params.plotPopupPositions.values()) {
      plotPopupPositions.put(plotPopupPosition.getXmlID(), plotPopupPosition.copyWithoutChildren());
    }

    this.defaultGraphWidth = params.defaultGraphWidth.copyWithoutChildren();
    this.defaultGraphHeight = params.defaultGraphWidth.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new UiLayoutParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new UiLayoutParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new UiLayoutParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    List<Parameter<?>> allParents = new ArrayList<>();
    allParents.add(bottomSplitDiv1);
    allParents.add(bottomSplitDiv2);
    allParents.add(bottomSplitDiv3);
    allParents.add(chartLegendSplitDiv);
    allParents.add(mainMethodEditorSplitDiv);
    allParents.add(defaultGraphWidth);
    allParents.add(defaultGraphHeight);

    allParents.addAll(plotPopupPositions.values());

    Parameter<?>[] allParentsArray = allParents.toArray(new Parameter<?>[0]);
    super.setParentParameters(allParentsArray);
  }


  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = switch (element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE)) {
          case LABEL_PAR_XML_ID -> super.label;
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "bottomSplitDiv1" -> bottomSplitDiv1;
          case "bottomSplitDiv2" -> bottomSplitDiv2;
          case "bottomSplitDiv3" -> bottomSplitDiv3;
          case "mainMethodEditorSplitDiv" -> mainMethodEditorSplitDiv;

          case "chartLegendSplitDiv" -> chartLegendSplitDiv;

          case "Method viewer" -> plotPopupPositions.get("Method viewer");
          case "Raw data viewer" -> plotPopupPositions.get("Raw data viewer");
          case "Multi histogram viewer" -> plotPopupPositions.get("Multi histogram viewer");
          case "Histogram viewer" -> plotPopupPositions.get("Histogram viewer");
          case "Scatter plot viewer" -> plotPopupPositions.get("Scatter plot viewer");
          case "Boxplot viewer" -> plotPopupPositions.get("Boxplot viewer");
          case "Single event viewer" -> plotPopupPositions.get("Single event viewer");
          case "Peak profile viewer" -> plotPopupPositions.get("Peak profile viewer");
          case "Average trend viewer" -> plotPopupPositions.get("Average trend viewer");
          case "Table viewer" -> plotPopupPositions.get("Table viewer");
          case "Logger viewer" -> plotPopupPositions.get("Logger viewer");

          case "defaultGraphWidth" -> defaultGraphWidth;
          case "defaultGraphHeight" -> defaultGraphHeight;

          default -> null;
        };

        if (par != null) {
          par.readFromXmlElement(element);
        } else {
          LOGGER.trace("Could not match xml entry: " + element);
        }
      }
    }
  }

  @Override
  public void executeSaveAs(Path file) {
    super.setAssociatedFileOnDrive(file);
    XmlUtil.writeToXml(this, file);
  }


  // Empty should be fine since they are not used to instantiate.
  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.EMPTY;
  }

  public double[] getBottomSliders() {
    return new double[]{bottomSplitDiv1.getValue(),
        bottomSplitDiv2.getValue(),
        bottomSplitDiv3.getValue()};
  }

  public void setBottomSliders(List<Double> sliders) {
    if (sliders.size() > 0) {
      bottomSplitDiv1.setValue(sliders.get(0));
    }
    if (sliders.size() > 1) {
      bottomSplitDiv2.setValue(sliders.get(1));
    }
    if (sliders.size() > 2) {
      bottomSplitDiv3.setValue(sliders.get(2));
    }
  }

  public double getChartDivider() {
    double val = 0.9;
    if (0 <= chartLegendSplitDiv.getValue() && chartLegendSplitDiv.getValue() <= 1) {
      val = chartLegendSplitDiv.getValue();
    }
    return val;
  }

  public void setChartDivider(double pos) {
    chartLegendSplitDiv.setValue(pos);
  }


  public double getMethodDivider() {
    double val = 0.9;
    if (0 <= mainMethodEditorSplitDiv.getValue() && mainMethodEditorSplitDiv.getValue() <= 1) {
      val = mainMethodEditorSplitDiv.getValue();
    }
    return val;
  }

  public void setMethodDivider(double pos) {
    mainMethodEditorSplitDiv.setValue(pos);
  }

  public double[] getPlotPopupPositionsXYWH(String title) {
    double[] result = new double[0];
    Parameter<String> par = plotPopupPositions.get(title);
    if (par != null) {
      String[] parts = par.getValue().split("_"); // Split by underscore
      if (parts.length == 4) {
        result = new double[4];
        for (int i = 0; i < 4; i++) {
          result[i] = Double.parseDouble(parts[i]);
        }
      }
    }
    return result;
  }

  public void setPlotPopupPositionsXYWH(String title, double[] xywh) {
    Parameter<String> par = plotPopupPositions.get(title);
    if (par != null && xywh.length == 4) {
      String parts = xywh[0] + "_" + xywh[1] + "_" + xywh[2] + "_" + xywh[3];
      par.setValue(parts);
    }
  }


  private Parameter<String> makeStringParameter(String title) {
    return new StringParameter("XYWH parameter",
        "Stores x y width and height of popup window",
        "800_600",
        TextFormatterOption.ALL_PASS,
        true,
        false,
        title
    );
  }

  public Parameter<Integer> getDefaultGraphHeight() {
    return defaultGraphHeight;
  }

  public Parameter<Integer> getDefaultGraphWidth() {
    return defaultGraphWidth;
  }
}
