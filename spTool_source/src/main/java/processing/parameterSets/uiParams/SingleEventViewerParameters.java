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

import gui.util.TextFormatterOption;
import io.XmlUtil;

import java.io.Serial;
import java.nio.file.Path;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameterSets.uiParams.Viewers.SingleEventViewer;
import processing.parameters.BooleanParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;

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

public class SingleEventViewerParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "SingleEventViewerParameters";

  private final Parameter<Boolean> useCommonYAxis;
  private final Parameter<Boolean> showPeakParameters;
  private final Parameter<Integer> numberOfBGEvents;
  private final Parameter<Boolean> sortBoolean;
  private final Parameter<EventParameter> eventParameter;
  private final Parameter<Boolean> logYAXis;
  private final Parameter<Integer> numberOfEventsShown;

  public SingleEventViewerParameters() {
    super("Monte carlo raw data viewer parameters", XML_ELEMENT_TAG);

    this.useCommonYAxis = new BooleanParameter(
        "y-axis",
        "Common",
        "All plots have the same y axis limit, which is the max value",
        false,
        false,
        "useCommonYAxis");

    logYAXis = new BooleanParameter(
        "y-axis",
        "log-scale",
        "Show log10(intensity + 1) on the y-axis for better visibility",
        false,
        false,
        "logYAXis");

    this.showPeakParameters = new BooleanParameter(
        "Parameters",
        "Show",
        "Shows relevant parameters of the event peak in the plot",
        false,
        false,
        "showPeakParameters");

    this.numberOfBGEvents = new IntegerParameter(
        "Window",
        "Plot some additional data points of the time series"
            + "\nfrom before an after the event peak (pre/post) to see peak in its context",
        10,
        TextFormatterOption.ASSURE_POSITIVE_INTEGER,
        false,
        "numberOfBGEvents"
    );

    numberOfEventsShown = new IntegerParameter(
        "Show n",
        "How many events are shown (max 4x3)",
        12,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "numberOfEventsShown"
    );


    this.sortBoolean = new BooleanParameter(
        "Sort",
        "Ascending",
        "Sort events in ascending order according to the chosen parameter",
        false,
        false,
        "sortBoolean");

    this.eventParameter = new ComboEnumParameter<>(
        "Sort by",
        "Choose which parameter is used for sorting",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameter"
    );

    organize();
  }

  public SingleEventViewerParameters(SingleEventViewerParameters iclPeakViewer) {
    super(iclPeakViewer.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(iclPeakViewer.getCommentParameter());
    this.useCommonYAxis = iclPeakViewer.useCommonYAxis.copyWithoutChildren();
    this.logYAXis = iclPeakViewer.logYAXis.copyWithoutChildren();
    this.showPeakParameters = iclPeakViewer.showPeakParameters.copyWithoutChildren();
    this.numberOfBGEvents = iclPeakViewer.numberOfBGEvents.copyWithoutChildren();
    this.sortBoolean = iclPeakViewer.sortBoolean.copyWithoutChildren();
    this.eventParameter = iclPeakViewer.eventParameter.copyWithoutChildren();
    this.numberOfEventsShown = iclPeakViewer.numberOfEventsShown.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new SingleEventViewerParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new SingleEventViewerParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new SingleEventViewerParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(
         numberOfEventsShown,
        useCommonYAxis,
        logYAXis,
        showPeakParameters,
        numberOfBGEvents,
        sortBoolean
    );

    sortBoolean.addConditionalChild(true, eventParameter);
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

          case "numberOfEventsShown" -> numberOfEventsShown;
          case "logYAXis" -> logYAXis;
          case "useCommonYAxis" -> useCommonYAxis;
          case "showPeakParameters" -> showPeakParameters;
          case "numberOfBGEvents" -> numberOfBGEvents;
          case "sortBoolean" -> sortBoolean;
          case "eventParameter" -> eventParameter;

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
    return new SingleEventViewer(this);
  }

  public Parameter<Integer> getNumberOfEventsShown() {
    return numberOfEventsShown;
  }

  public Parameter<Boolean> getUseCommonYAxis() {
    return useCommonYAxis;
  }

  public Parameter<Boolean> getAnnotatePeakParameters() {
    return showPeakParameters;
  }

  public Parameter<Integer> getNumberOfBGEvents() {
    return numberOfBGEvents;
  }

  public Parameter<Boolean> getSortBoolean() {
    return sortBoolean;
  }

  public Parameter<EventParameter> getEventParameter() {
    return eventParameter;
  }

  public Parameter<Boolean> getLogYAXis() {
    return logYAXis;
  }
}
