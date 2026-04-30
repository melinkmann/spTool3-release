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

import gui.util.TextFormatterOption;
import io.XmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.Parameter;
import util.NF;

import java.io.Serial;
import java.nio.file.Path;

public class EventDataRangeParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "EventDataRangeParams";

  private final Parameter<EventParameter> eventParameter;
  private final Parameter<Double> startValue;
  private final Parameter<Double> endValue;


  public EventDataRangeParams() {
    super("Manual selection", XML_ELEMENT_TAG);

    eventParameter = new ComboEnumParameter<>("Event parameter",
        "ROI based on which parameter",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameter");

    this.startValue = new DoubleParameter(
        "Start [?]",
        "Inclusive start",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "startValue");

    this.endValue = new DoubleParameter(
        "End [?]",
        "Inclusive end",
        100d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "endValue");

    organize();
  }

  public EventDataRangeParams(EventDataRangeParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.startValue = params.startValue.copyWithoutChildren();
    this.endValue = params.endValue.copyWithoutChildren();
    this.eventParameter = params.eventParameter.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new EventDataRangeParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new EventDataRangeParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new EventDataRangeParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(
        eventParameter,
        startValue, endValue
    );

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

          case "eventParameter" -> eventParameter;
          case "startValue" -> startValue;
          case "endValue" -> endValue;
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

  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.EVENT_DATA_ROI;
  }


  public Parameter<Double> getStartValue() {
    return startValue;
  }

  public Parameter<Double> getEndValue() {
    return endValue;
  }

  public Parameter<EventParameter> getEventParameter() {
    return eventParameter;
  }
}
