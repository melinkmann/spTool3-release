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

import core.SpTool3Main;
import gui.util.TextFormatterOption;
import io.XmlUtil;

import java.io.Serial;
import java.nio.file.Path;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameterSets.uiParams.Viewers.MonteCarloRawDataViewer;
import processing.parameters.*;
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
 * --> Do the same also for WHICH viewers are shown in the UI :)
 */


/**
 * Remember to also register a new ParameterSet in the dictionary!
 *
 * @link {@link XmlInstanceDictionary}
 */

public class MonteCarloRawDataParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "MonteCarloRawDataViewer";

  private final Parameter<Boolean> showEventMarkers;
  private final Parameter<Boolean> showPopulationMarkers;
  private final Parameter<Integer> upperPointCountCutoff;
  private final Parameter<Boolean> showThresholdMarkers;
  private final Parameter<Boolean> limitAxes;
  private final Parameter<Double> upperYLimit;

  public MonteCarloRawDataParameters() {
    super("Monte carlo raw data viewer parameters", XML_ELEMENT_TAG);

    this.showEventMarkers = new BooleanParameter(
        "Events",
        "Marker",
        "Show symbols to indicate the peak position of each event "
            + "\nwith a separate marker for each isotope",
        false,
        false,
        "showEventMarkers");

    this.showPopulationMarkers = new BooleanParameter(
        "Population",
        "Marker",
        "Show symbols at the bottom of the plot"
            + "\nto indicate to true arrival time of a particle.",
        false,
        false,
        "showPopulationMarkers");

    this.upperPointCountCutoff = new IntegerParameter(
        "Limit",
        "In the plot, too many visible markers make the graphs slow to respond."
            + "\nHence, you may specify a cutoff number here. "
            + "\nWhen a population or single mz has more highlighted points than the specified value,"
            + "\nno markers will be shown for the respective population or mz (i.e., only the line graph is visible)."
            + "\nYou can use this for instance, when you simulated a background"
            + "\nthat consists of many small particles."
            + "\nNote: To deactivate this behaviour, simply choose a large number",
        10000,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "upperPointCountCutoff"
    );

    this.showThresholdMarkers = new BooleanParameter(
        "Thresholds",
        "Show",
        "Show lines to indicate height-based thresholds.",
        false,
        false,
        "showThresholdMarkers");

    limitAxes = new BooleanParameter(
        "Axis limits",
        "Set limits",
        "Set custom limits for the y axis of the plot",
        false,
        true,
        "limitAxes"
    );

    upperYLimit = new DoubleParameter(
        "Upper y",
        "Choose the upper y-axis limit as a absolute value",
        1E3d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "upperYLimit");

    organize();
  }

  public MonteCarloRawDataParameters(MonteCarloRawDataParameters iclPeakViewer) {
    super(iclPeakViewer.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(iclPeakViewer.getCommentParameter());
    this.showEventMarkers = iclPeakViewer.showEventMarkers.copyWithoutChildren();
    this.showPopulationMarkers = iclPeakViewer.showPopulationMarkers.copyWithoutChildren();
    this.upperPointCountCutoff = iclPeakViewer.upperPointCountCutoff.copyWithoutChildren();
    this.showThresholdMarkers = iclPeakViewer.showThresholdMarkers.copyWithoutChildren();
    this.limitAxes = iclPeakViewer.limitAxes.copyWithoutChildren();
    this.upperYLimit = iclPeakViewer.upperYLimit.copyWithoutChildren();
    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new MonteCarloRawDataParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new MonteCarloRawDataParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new MonteCarloRawDataParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent

    if (SpTool3Main.getANALYZER()) {
      super.setParentParameters(
          showEventMarkers,
          showPopulationMarkers,
          upperPointCountCutoff,
          showThresholdMarkers,
          limitAxes
      );
    } else {
      super.setParentParameters(
          showEventMarkers,
          showPopulationMarkers,
          upperPointCountCutoff,
          limitAxes
      );
    }

    limitAxes.addConditionalChild(true, upperYLimit);

    showEventMarkers.setDecoration(new ImageDecoration<>("/img/eventMarker.png"));
    showPopulationMarkers.setDecoration(new ImageDecoration<>("/img/populationMarker.png"));
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

          case "showEventMarkers" -> showEventMarkers;
          case "showPopulationMarkers" -> showPopulationMarkers;
          case "upperPointCountCutoff" -> upperPointCountCutoff;

          case "showThresholdMarkers" -> showThresholdMarkers;

          case "limitAxes" -> limitAxes;
          case "upperYLimit" -> upperYLimit;

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


  // Empty should be fine since they are not used to instantiate.
  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.EMPTY;
  }

  @Override
  public FxParamSetImpl getObservableInstance() {
    return new MonteCarloRawDataViewer(this);
  }


  public Parameter<Boolean> getShowEventMarkers() {
    return showEventMarkers;
  }

  public Parameter<Boolean> getShowPopulationMarkers() {
    return showPopulationMarkers;
  }

  public Parameter<Integer> getUpperPointCountCutoff() {
    return upperPointCountCutoff;
  }

  public Parameter<Boolean> getShowThresholdMarkers() {
    return showThresholdMarkers;
  }

  public Parameter<Boolean> getLimitAxes() {
    return limitAxes;
  }

  public Parameter<Double> getUpperYLimit() {
    return upperYLimit;
  }
}
