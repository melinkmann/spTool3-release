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
import processing.options.*;
import processing.parameterSets.*;
import processing.parameters.*;
import util.NF;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;

public class MethodDoeParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "MethodDoeParams";

  private final Parameter<DoeModel> doeSampling;
  public final Parameter<Integer> nConditions;

  private final Parameter<Double> d_mu_Start;
  private final Parameter<Double> d_mu_Stop;
  private final Parameter<Double> d_SD_Start;
  private final Parameter<Double> d_SD_Stop;

  private final Parameter<Double> v_mu_Start;
  private final Parameter<Double> v_mu_Stop;
  private final Parameter<Double> v_SD_Start;
  private final Parameter<Double> v_SD_Stop;

  private final Parameter<Double> y_mu_Start;
  private final Parameter<Double> y_mu_Stop;
  private final Parameter<Double> y_SD_Start;
  private final Parameter<Double> y_SD_Stop;

  public MethodDoeParams() {
    this("Method design of experiment (DOE) parameters");
  }

  public MethodDoeParams(String label) {
    super(label, XML_ELEMENT_TAG);

    this.doeSampling = new ComboEnumParameter<>(
        "Sampling method",
        """
            Choose the sampling method for the design of experiments.
            
            Sobol series: deterministic low-discrepancy sequence that fills the
            parameter space evenly. Gives good coverage and fast convergence,
            and works best when the number of samples is a power of two.
            
            Latin hypercube: randomized stratified sampling. Each parameter range
            is split into N equal intervals and each interval is sampled exactly
            once, so every parameter is covered evenly. Works well with any number
            of samples, but results vary with the random seed.
            """,
        DoeModel.LATIN_HYPERCUBE,
        DoeModel.values(),
        DoeModel.class,
        false,
        "doeSampling");

    nConditions = new IntegerParameter("Number of conditions",
        """
            Number of sets (conditions) generated for the DOE.
            Each condition is one combination of values for the mean and SD of
            the diffusion coefficient (D), the plasma velocity (v), and the distance
            of vaporization (y), drawn from the Start/Stop ranges defined above.
            More conditions cover the parameter space more densely
            but increase the total simulation time proportionally""",
        64,
        TextFormatterOption.ASSURE_POSITIVE_INTEGER,
        false,
        "nConditions");

    d_mu_Start = new DoubleParameter("Start diffusion coefficient",
        """
            DOE lower limit for the
            mean diffusion coefficient (D) [cm2/s] of the element""",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "d_mu_Start");

    d_mu_Stop = new DoubleParameter("Stop diffusion coefficient",
        """
            DOE upper limit for the
            mean diffusion coefficient (D) [cm2/s] of the element""",
        60d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "d_mu_Stop");

    d_SD_Start = new DoubleParameter("Start diffusion coefficient SD",
        """
            DOE lower limit for the
            standard deviation of the diffusion coefficient (D) [cm2/s] of the element""",
        0d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "d_SD_Start");


    d_SD_Stop = new DoubleParameter("Stop diffusion coefficient SD",
        """
            DOE upper limit for the
            standard deviation of the diffusion coefficient (D) [cm2/s] of the element""",
        20d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "d_SD_Stop");

    v_mu_Start = new DoubleParameter("Start plasma velocity (v) [m/s]",
        """
            DOE lower limit for the
            mean plasma gas linear velocity (v) [m/s].
            Higher values lead to narrower and more symmetric peaks,
            since the residence time in the plasma is shorter""",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "v_mu_Start");

    v_mu_Stop = new DoubleParameter("Stop plasma velocity (v) [m/s]",
        """
            DOE upper limit for the
            mean plasma gas linear velocity (v) [m/s].
            Higher values lead to narrower and more symmetric peaks,
            since the residence time in the plasma is shorter""",
        25d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "v_mu_Stop");

    v_SD_Start = new DoubleParameter("Start plasma velocity SD [m/s]",
        """
            DOE lower limit for the
            standard deviation of the plasma velocity (v) [m/s].
            This number specifies how broadly the random numbers vary""",
        0d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "v_SD_Start");

    v_SD_Stop = new DoubleParameter("Stop plasma velocity SD [m/s]",
        """
            DOE upper limit for the
            standard deviation of the plasma velocity (v) [m/s].
            This number specifies how broadly the random numbers vary""",
        8d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "v_SD_Stop");

    y_mu_Start = new DoubleParameter("Start distance of vaporization (y) [mm]",
        """
            DOE lower limit for the
            mean distance from the sampler orifice where a particle is completely ionized [mm].
            Higher values lead to broader peaks,
            since the residence time in the plasma increases and the ions have more time to diffuse""",
        3d,
        NF.D1C4,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "y_mu_Start");

    y_mu_Stop = new DoubleParameter("Stop distance of vaporization (y) [mm]",
        """
            DOE upper limit for the
            mean distance from the sampler orifice where a particle is completely ionized [mm].
            Higher values lead to broader peaks,
            since the residence time in the plasma increases and the ions have more time to diffuse""",
        30d,
        NF.D1C4,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "y_mu_Stop");

    y_SD_Start = new DoubleParameter("Start distance of vaporization SD [mm]",
        """
            DOE lower limit for the
            standard deviation of the distance of vaporization (y) [mm].
            This number specifies how broadly the random numbers vary""",
        0d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "y_SD_Start");

    y_SD_Stop = new DoubleParameter("Stop distance of vaporization SD [mm]",
        """
            DOE upper limit for the
            standard deviation of the distance of vaporization (y) [mm].
            This number specifies how broadly the random numbers vary""",
        10d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "y_SD_Stop");

    organize();
  }

  // Copy
  public MethodDoeParams(MethodDoeParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.doeSampling = params.doeSampling.copyWithoutChildren();
    this.nConditions = params.nConditions.copyWithoutChildren();

    this.d_mu_Start = params.d_mu_Start.copyWithoutChildren();
    this.d_mu_Stop = params.d_mu_Stop.copyWithoutChildren();
    this.d_SD_Start = params.d_SD_Start.copyWithoutChildren();
    this.d_SD_Stop = params.d_SD_Stop.copyWithoutChildren();

    this.v_mu_Start = params.v_mu_Start.copyWithoutChildren();
    this.v_mu_Stop = params.v_mu_Stop.copyWithoutChildren();
    this.v_SD_Start = params.v_SD_Start.copyWithoutChildren();
    this.v_SD_Stop = params.v_SD_Stop.copyWithoutChildren();

    this.y_mu_Start = params.y_mu_Start.copyWithoutChildren();
    this.y_mu_Stop = params.y_mu_Stop.copyWithoutChildren();
    this.y_SD_Start = params.y_SD_Start.copyWithoutChildren();
    this.y_SD_Stop = params.y_SD_Stop.copyWithoutChildren();

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new MethodDoeParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new MethodDoeParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new MethodDoeParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(
        doeSampling,
        nConditions,
        new SeparatorParameter(),
        d_mu_Start, d_mu_Stop,
        d_SD_Start, d_SD_Stop,
        v_mu_Start, v_mu_Stop,
        v_SD_Start, v_SD_Stop,
        y_mu_Start, y_mu_Stop,
        y_SD_Start, y_SD_Stop,
        new SeparatorParameter()
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

          case "doeSampling" -> doeSampling;
          case "nConditions" -> nConditions;

          case "d_mu_Start" -> d_mu_Start;
          case "d_mu_Stop" -> d_mu_Stop;
          case "d_SD_Start" -> d_SD_Start;
          case "d_SD_Stop" -> d_SD_Stop;

          case "v_mu_Start" -> v_mu_Start;
          case "v_mu_Stop" -> v_mu_Stop;
          case "v_SD_Start" -> v_SD_Start;
          case "v_SD_Stop" -> v_SD_Stop;

          case "y_mu_Start" -> y_mu_Start;
          case "y_mu_Stop" -> y_mu_Stop;
          case "y_SD_Start" -> y_SD_Start;
          case "y_SD_Stop" -> y_SD_Stop;

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
    return AvailableParameterSets.METHOD_DOE;
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
  public DoeModel getDoeSamplingModel() {
    return doeSampling.getValue();
  }

  public int getNConditions() {
    return nConditions.getValue();
  }

  public double getD_mu_Start() {
    return d_mu_Start.getValue();
  }

  public double getD_mu_Stop() {
    return d_mu_Stop.getValue();
  }

  public double getD_SD_Start() {
    return d_SD_Start.getValue();
  }

  public double getD_SD_Stop() {
    return d_SD_Stop.getValue();
  }

  public double getV_mu_Start() {
    return v_mu_Start.getValue();
  }

  public double getV_mu_Stop() {
    return v_mu_Stop.getValue();
  }

  public double getV_SD_Start() {
    return v_SD_Start.getValue();
  }

  public double getV_SD_Stop() {
    return v_SD_Stop.getValue();
  }

  public double getY_mu_Start() {
    return y_mu_Start.getValue();
  }

  public double getY_mu_Stop() {
    return y_mu_Stop.getValue();
  }

  public double getY_SD_Start() {
    return y_SD_Start.getValue();
  }

  public double getY_SD_Stop() {
    return y_SD_Stop.getValue();
  }

}
