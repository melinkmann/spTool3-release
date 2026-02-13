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

import io.XmlUtil;
import java.io.Serial;
import java.nio.file.Path;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.options.MathMod;
import processing.options.SigTests;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.ComboEnumParameter;
import processing.parameters.Parameter;

public class SignificanceTestParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "SignificanceTestParams";

  private final Parameter<SigTests> testType;
  private final Parameter<EventParameter> eventParameter;
  private final Parameter<MathMod> mathModification;

  public SignificanceTestParams() {
    this("Significance test parameters");
  }

  public SignificanceTestParams(String label) {
    super(label, XML_ELEMENT_TAG);

    this.testType = new ComboEnumParameter<>("Test",
        "Which test shall be executed?",
        SigTests.KS,
        SigTests.values(),
        SigTests.class,
        false,
        "testType");

    mathModification = new ComboEnumParameter<>(
        "Math",
        "Transform the data before plotting",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "mathModification"
    );

    eventParameter = new ComboEnumParameter<>(
        "Data",
        "Choose which data shall be shown",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameter"
    );

    // Load UI parameters from histogram pane

    organize();
  }

  // Copy
  public SignificanceTestParams(SignificanceTestParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.testType = params.testType.copyWithoutChildren();
    this.mathModification = params.mathModification.copyWithoutChildren();
    this.eventParameter = params.eventParameter.copyWithoutChildren();

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new SignificanceTestParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new SignificanceTestParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new SignificanceTestParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(testType,mathModification, eventParameter);
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

          case "testType" -> testType;
          case "mathModification" -> mathModification;
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


  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.SIGNIFICANCE_TEST;
  }

  ////////////////////////////////////////
  public Parameter<SigTests> getTestType() {
    return testType;
  }

  public Parameter<EventParameter> getEventParameter() {
    return eventParameter;
  }

  public Parameter<MathMod> getMathModification() {
    return mathModification;
  }
}
