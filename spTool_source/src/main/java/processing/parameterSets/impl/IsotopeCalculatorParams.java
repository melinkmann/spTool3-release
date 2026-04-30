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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.BooleanParameter;
import processing.parameters.Parameter;

import java.io.Serial;
import java.nio.file.Path;

public class IsotopeCalculatorParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "IsotopeCalculatorParams";

  private final Parameter<Boolean> invertIsotopeRatio;


  public IsotopeCalculatorParams() {
    super("Calculate isotope", XML_ELEMENT_TAG);

    this.invertIsotopeRatio = new BooleanParameter(
        "Ratio",
        "Invert",
        """
            Normally, the first selected isotope will be divided by the second selected isotope.
            If this box is checked, the order will be inverted, i.e., second selected isotope divided by
            first.""",
        false,
        false,
        "invertIsotopeRatio"
    );

    organize();
  }

  public IsotopeCalculatorParams(IsotopeCalculatorParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.invertIsotopeRatio = params.invertIsotopeRatio.copyWithoutChildren();

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new IsotopeCalculatorParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new IsotopeCalculatorParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new IsotopeCalculatorParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(
        invertIsotopeRatio
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

          case "invertIsotopeRatio" -> invertIsotopeRatio;
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
    return AvailableParameterSets.ISOTOPE_CALCULATOR;
  }


  public Parameter<Boolean> getInvertIsotopeRatio() {
    return invertIsotopeRatio;
  }
}
