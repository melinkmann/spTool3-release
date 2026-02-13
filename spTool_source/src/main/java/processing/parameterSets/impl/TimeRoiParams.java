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
import java.io.Serial;
import java.nio.file.Path;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.BooleanParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.Parameter;
import util.NF;

public class TimeRoiParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "TimeRoiParams";

  private final Parameter<Boolean> reset;
  private final Parameter<Double> startTime;
  private final Parameter<Double> stopTime;


  public TimeRoiParams() {
    super("Time region", XML_ELEMENT_TAG);

    this.reset = new BooleanParameter(
        "Data",
        "Reset to original limits",
        """
            Reset to original limits
            """,
        false,
        false,
        "reset"
    );

    this.startTime = new DoubleParameter(
        "Start [s]",
        "Inclusive start",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "startTime");

    this.stopTime = new DoubleParameter(
        "End [s]",
        "Inclusive end",
        60d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "stopTime");

    organize();
  }

  public TimeRoiParams(TimeRoiParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.startTime = params.startTime.copyWithoutChildren();
    this.stopTime = params.stopTime.copyWithoutChildren();
    this.reset = params.reset.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new TimeRoiParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new TimeRoiParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new TimeRoiParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(
        reset
    );

    reset.addConditionalChild(false, startTime, stopTime);
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

          case "reset" ->reset;
          case "startTime" -> startTime;
          case "stopTime" -> stopTime;
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
    return AvailableParameterSets.TIME_ROI;
  }


  public Parameter<Double> getStartTime() {
    return startTime;
  }

  public Parameter<Double> getStopTime() {
    return stopTime;
  }

  public Parameter<Boolean> getReset() {
    return reset;
  }
}
