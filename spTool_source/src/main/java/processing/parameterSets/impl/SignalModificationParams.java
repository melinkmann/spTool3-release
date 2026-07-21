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

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;

public class SignalModificationParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "SignalModificationParams";

  private Parameter<Boolean> isotopeSumBoolean;
  private Parameter<Boolean> excludeIsobars;
  private Parameter<Boolean> onlyUseSelectedIsotopesForSum;

  public SignalModificationParams() {
    super("Signal modification parameters", XML_ELEMENT_TAG);

    this.isotopeSumBoolean = new BooleanParameter("Calculate isotope sum",
        "Calculate isotope sum",
        "For each element, sum all isotopes",
        false,
        false,
        "isotopeSumBoolean"
    );

    this.excludeIsobars = new BooleanParameter("Isobaric conflicts",
        "Skip conflicts",
        "Do not add isotopes with expected isobaric conflicts",
        true,
        false,
        "excludeIsobars"
    );

    onlyUseSelectedIsotopesForSum = new BooleanParameter("Isotopes",
        "Only use selected",
        """
            For summation, all unselected isotopes will be ignored""",
        false,
        false,
        "onlyUseSelectedIsotopesForSum");

    //

    organize();
  }

  public SignalModificationParams(SignalModificationParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.isotopeSumBoolean = params.isotopeSumBoolean.copyWithoutChildren();
    this.excludeIsobars = params.excludeIsobars.copyWithoutChildren();
    this.onlyUseSelectedIsotopesForSum = params.onlyUseSelectedIsotopesForSum.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new SignalModificationParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new SignalModificationParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new SignalModificationParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Parent parameters
    super.setParentParameters(
        isotopeSumBoolean
    );

    isotopeSumBoolean.addConditionalChild(true, excludeIsobars, onlyUseSelectedIsotopesForSum);


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

          case "isotopeSumBoolean" -> isotopeSumBoolean;
          case "excludeIsobars" -> excludeIsobars;
          case "onlyUseSelectedIsotopesForSum" -> onlyUseSelectedIsotopesForSum;

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
    return AvailableParameterSets.SIGNAL_MOD;
  }

  //------------------------------------------------------------------------------------------

  public Parameter<Boolean> getIsotopeSumBoolean() {
    return isotopeSumBoolean;
  }

  public Parameter<Boolean> getExcludeIsobars() {
    return excludeIsobars;
  }

  public Parameter<Boolean> getOnlyUseSelectedIsotopesForSum() {
    return onlyUseSelectedIsotopesForSum;
  }

  //------------------------------------------------------------------------------------------


  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final SignalModificationParams defaults = new SignalModificationParams();

    if (excludeIsobars == null) {
      excludeIsobars = defaults.excludeIsobars;
    }

    if (onlyUseSelectedIsotopesForSum == null) {
      onlyUseSelectedIsotopesForSum = defaults.onlyUseSelectedIsotopesForSum;
    }

  }

}
