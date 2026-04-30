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
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.BooleanParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;
import util.NF;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;

public class IsotopeRemoverParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "IsotopeRemoverParams";

  private final Parameter<Boolean> enableBoolean;
  private final Parameter<Boolean> removeFewerIsotopesThan;
  private final Parameter<Boolean> removeMoreIsotopesThan;
  private final Parameter<Integer> lowerIsotopeAbsoluteCutoff;
  private final Parameter<Double> upperIsotopeRateCutoff;

  public IsotopeRemoverParams() {
    super("Remove isotopes parameters", XML_ELEMENT_TAG);

    enableBoolean = new BooleanParameter("On/Off",
        "Enable",
        "Activate this sub method",
        true,
        false,
        "enableBoolean"
    );

    this.removeFewerIsotopesThan = new BooleanParameter("Remove",
        "Isotopes with fewer events",
        "Remove isotopes from the sample entirely",
        false,
        false,
        "removeFewerIsotopesThan"
    );

    this.lowerIsotopeAbsoluteCutoff = new IntegerParameter(
        "Number of events",
        "Isotope must have at least this number of events [NP] to be kept",
        50,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "lowerIsotopeAbsoluteCutoff"
    );


    this.removeMoreIsotopesThan = new BooleanParameter("Remove",
        "Isotopes with higher event rate",
        "Remove isotopes from the sample entirely",
        false,
        false,
        "removeMoreIsotopesThan"
    );

    this.upperIsotopeRateCutoff = new DoubleParameter(
        "Event rate",
        "Isotope must have at least this event rate [NP/s] to be kept",
        100d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "upperIsotopeRateCutoff"
    );

    //

    organize();
  }

  public IsotopeRemoverParams(IsotopeRemoverParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    enableBoolean = params.enableBoolean.copyWithoutChildren();
    removeFewerIsotopesThan = params.removeFewerIsotopesThan.copyWithoutChildren();
    removeMoreIsotopesThan = params.removeMoreIsotopesThan.copyWithoutChildren();
    lowerIsotopeAbsoluteCutoff = params.lowerIsotopeAbsoluteCutoff.copyWithoutChildren();
    upperIsotopeRateCutoff = params.upperIsotopeRateCutoff.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new IsotopeRemoverParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new IsotopeRemoverParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new IsotopeRemoverParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Parent parameters
    super.setParentParameters(
        enableBoolean
    );

    enableBoolean.addConditionalChild(true, removeFewerIsotopesThan, removeMoreIsotopesThan);
    removeFewerIsotopesThan.addConditionalChild(true, lowerIsotopeAbsoluteCutoff);
    removeMoreIsotopesThan.addConditionalChild(true, upperIsotopeRateCutoff);

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

          case "enableBoolean" -> enableBoolean;
          case "removeFewerIsotopesThan" -> removeFewerIsotopesThan;
          case "removeMoreIsotopesThan" -> removeMoreIsotopesThan;
          case "fewerIsotopesNumber" -> lowerIsotopeAbsoluteCutoff;
          case "moreIsotopesRate" -> upperIsotopeRateCutoff;

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
    return AvailableParameterSets.ISOTOPE_REMOVER;
  }

  //------------------------------------------------------------------------------------------

  public Parameter<Boolean> getEnableBoolean() {
    return enableBoolean;
  }

  public Parameter<Boolean> getRemoveFewerIsotopesThan() {
    return removeFewerIsotopesThan;
  }

  public Parameter<Boolean> getRemoveMoreIsotopesThan() {
    return removeMoreIsotopesThan;
  }

  public Parameter<Integer> getLowerIsotopeAbsoluteCutoff() {
    return lowerIsotopeAbsoluteCutoff;
  }

  public Parameter<Double> getUpperIsotopeRateCutoff() {
    return upperIsotopeRateCutoff;
  }

  //------------------------------------------------------------------------------------------


  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final IsotopeRemoverParams defaults = new IsotopeRemoverParams();


  }

}
