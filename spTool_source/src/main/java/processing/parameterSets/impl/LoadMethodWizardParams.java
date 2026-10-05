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
import processing.parameterSets.*;
import processing.parameters.*;
import util.NF;

import java.io.Serial;
import java.nio.file.Path;

public class LoadMethodWizardParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "LoadMethodWizardParams";

  private final Parameter<Double> w_scatterHeightArea;
  private final Parameter<Double> w_emdArea;
  private final Parameter<Double> w_emdHeight;
  private final Parameter<Double> w_emdDuration;
  private final Parameter<Double> w_emdSym;

  public LoadMethodWizardParams() {
    this("Method wizard parameters");
  }

  public LoadMethodWizardParams(String label) {
    super(label, XML_ELEMENT_TAG);

    w_scatterHeightArea = new DoubleParameter("Weight area-height scatter",
        """
            Weight of the area/height scatter similarity score (grid similarity)
            in the combined match score. Weights are normalised internally,
            so they do not need to sum to 1. Set to 0 to ignore this metric""",
        0.2,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "w_scatterHeightArea");

    w_emdArea = new DoubleParameter("Weight area histogram",
        """
            Weight of the Wasserstein distance of the (log10) net area
            distribution, scaled by the SD of the measured sample.
            Set to 0 to ignore this metric""",
        0.05,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "w_emdArea");

    w_emdHeight = new DoubleParameter("Weight height histogram",
        """
            Weight of the Wasserstein distance of the (log10) net height
            distribution, scaled by the SD of the measured sample.
            Set to 0 to ignore this metric""",
        0.15,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "w_emdHeight");

    w_emdDuration = new DoubleParameter("Weight duration histogram",
        """
            Weight of the Wasserstein distance of the peak duration
            distribution, scaled by the SD of the measured sample.
            Set to 0 to ignore this metric""",
        0.25,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "w_emdDuration");

    w_emdSym = new DoubleParameter("Weight asymmetry histogram",
        """
            Weight of the Wasserstein distance of the peak asymmetry factor
            distribution, scaled by the SD of the measured sample.
            Set to 0 to ignore this metric""",
        0.35,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "w_emdSym");

    organize();
  }

  // Copy
  public LoadMethodWizardParams(LoadMethodWizardParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.w_scatterHeightArea = params.w_scatterHeightArea.copyWithoutChildren();
    this.w_emdArea = params.w_emdArea.copyWithoutChildren();
    this.w_emdHeight = params.w_emdHeight.copyWithoutChildren();
    this.w_emdDuration = params.w_emdDuration.copyWithoutChildren();
    this.w_emdSym = params.w_emdSym.copyWithoutChildren();

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new LoadMethodWizardParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new LoadMethodWizardParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new LoadMethodWizardParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(
        w_scatterHeightArea, w_emdArea, w_emdHeight, w_emdDuration, w_emdSym);
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

          case "w_scatterHeightArea" -> w_scatterHeightArea;
          case "w_emdArea" -> w_emdArea;
          case "w_emdHeight" -> w_emdHeight;
          case "w_emdDuration" -> w_emdDuration;
          case "w_emdSym" -> w_emdSym;

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
    return AvailableParameterSets.LOAD_METHOD_WIZARD;
  }

  // without comment and so forth
  @Override
  public FxParamSet getObservableInstance() {
    return new FxParamSetSlimImpl(this);
  }

  // without comment and so forth
  @Override
  public FxParamSet getUneditableObservableInstance() {
    FxParamSetImpl fxParamSet = new FxParamSetSlimImpl(this);
    fxParamSet.setUneditable();
    return fxParamSet;
  }


  /// /////////////////////////////////////
  public double getW_scatterHeightArea() {
    return w_scatterHeightArea.getValue();
  }

  public double getW_emdArea() {
    return w_emdArea.getValue();
  }

  public double getW_emdHeight() {
    return w_emdHeight.getValue();
  }

  public double getW_emdDuration() {
    return w_emdDuration.getValue();
  }

  public double getW_emdSym() {
    return w_emdSym.getValue();
  }

  /**
   * Weights in the order used by bestMatch: scatter, area, height, duration, asymmetry.
   */
  public double[] getWeights() {
    return new double[]{
        getW_scatterHeightArea(), getW_emdArea(), getW_emdHeight(),
        getW_emdDuration(), getW_emdSym()};
  }
}