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

public class DTGroupParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "DTGroupParams";

  private final Parameter<Double> targetDwellTime;
  private final Parameter<Boolean> exportIntermediateSteps;


  public DTGroupParams() {
    super("Group DT", XML_ELEMENT_TAG);

    this.targetDwellTime = new DoubleParameter(
        "Target DT [ms]",
        """
            Converts a sample into a new sample with this time resolution.
            Note that the new time resolution must be more coarse (greater 'dwell time')
            than the actual dwell time""",
        1d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "targetDwellTime");

    this.exportIntermediateSteps = new BooleanParameter(
        "Steps",
        "Keep intermediate steps",
        """
            When the specified target dwell time (DT) is several times larger than the original DT,
             there are steps in between that are kept when this box is checked""",
        false,
        false,
        "exportIntermediateSteps"
    );

    organize();
  }

  public DTGroupParams(DTGroupParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.targetDwellTime = params.targetDwellTime.copyWithoutChildren();
    this.exportIntermediateSteps = params.exportIntermediateSteps.copyWithoutChildren();

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new DTGroupParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new DTGroupParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new DTGroupParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(
        targetDwellTime,
        exportIntermediateSteps
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

          case "windowWidth" -> targetDwellTime;
          case "exportIntermediateSteps" -> exportIntermediateSteps;
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
    return AvailableParameterSets.DT_GROUPING;
  }


  public Parameter<Double> getTargetDwellTime() {
    return targetDwellTime;
  }

  public Parameter<Boolean> getExportIntermediateSteps() {
    return exportIntermediateSteps;
  }
}
