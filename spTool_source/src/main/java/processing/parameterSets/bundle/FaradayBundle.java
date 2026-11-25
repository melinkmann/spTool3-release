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

package processing.parameterSets.bundle;

import dataModelNew.mz.Element;
import gui.dialog.Fillable;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameterSets.ParamBundle;
import processing.parameters.AutoFillParam;
import processing.parameters.DoubleParameter;
import processing.parameters.Parameter;
import processing.parameters.SpawnControlParameter;
import sandbox.montecarlo.Isotope;
import util.NF;

import java.io.Serial;
import java.util.List;

public class FaradayBundle implements ParamBundle {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private int index = -1;
  private final SpawnControlParameter parentSpawnPar;
  //
  public final Parameter<Fillable<Isotope>> isotopeHeaderParameter;

  public final Parameter<Double> resistorParameter;
  public final Parameter<Double> temperatureParameter;
  public final Parameter<Double> tau1Parameter;
  public final Parameter<Double> tau2Parameter;
  public final Parameter<Double> tau1WeightParameter;
  public final Parameter<Double> tau2WeightParameter;

  public FaradayBundle(SpawnControlParameter parent) {
    this.parentSpawnPar = parent;

    this.isotopeHeaderParameter = new AutoFillParam<>("Isotope",
        "Chemical isotope",
        Element.U.getMostAbundant(),
        Element.U.getMostAbundant(),
        TextFormatterOption.ALL_PASS,
        false,
        wrapID("isotopeHeaderParameter"));

    this.resistorParameter = new DoubleParameter("R [Ohm]",
        """
            Resistor value of the Faraday cup.
            This is used to estimate Johnson noise.
            The decay curves of the cup are controlled via tau values""",
        1E10d,
        NF.D1C2Exp,
        TextFormatterOption.ASSURE_NONZERO_POS_EXP_DOUBLE,
        false,
        wrapID("resistorParameter"));

    this.temperatureParameter = new DoubleParameter("T [K]",
        """
            Detector temperature in K to estimate Johnson noise.
            Set to zero to disable Johnson noise""",
        293.17d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        wrapID("temperatureParameter"));

    this.tau1Parameter = new DoubleParameter("Tau 1 [ms]",
        """
            Decay time constants of the Faraday cup detection circuitry.
            Tau 1 is roughly Rf*Cf (feedback resistance and capacitance)""",
        5d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        wrapID("tau1Parameter"));

    this.tau2Parameter = new DoubleParameter("Tau 2 [ms]",
        """
            A real resistor shows a fast decay (about Rf*Cf) (feedback resistance and capacitance) 
            plus a small, slow tail. Tau 2 describes the tail""",
        500d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        wrapID("tau2Parameter"));

    this.tau1WeightParameter = new DoubleParameter("Tau 1 weight [-]",
        """
            Dimensionless weight for Tau 1 to weigh its contribution.
            Weights for Tau 1 and 2 should add to 1""",
        0.97,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        wrapID("tau1WeightParameter"));

    this.tau2WeightParameter = new DoubleParameter("Tau 2 weight [-]",
        """
            Dimensionless weight for Tau 2 to weigh its contribution.
            Weights for Tau 1 and 2 should add to 1.
            Set this weight to zero to only use Tau 1""",
        0.03,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        wrapID("tau2WeightParameter"));

    organize();
  }

  // Copy

  public FaradayBundle(FaradayBundle elementBundle, SpawnControlParameter parent) {
    this.parentSpawnPar = parent;
    this.isotopeHeaderParameter = elementBundle.isotopeHeaderParameter.copyWithoutChildren();
    this.resistorParameter = elementBundle.resistorParameter.copyWithoutChildren();
    this.temperatureParameter = elementBundle.temperatureParameter.copyWithoutChildren();
    this.tau1Parameter = elementBundle.tau1Parameter.copyWithoutChildren();
    this.tau2Parameter = elementBundle.tau2Parameter.copyWithoutChildren();
    this.tau1WeightParameter = elementBundle.tau1WeightParameter.copyWithoutChildren();
    this.tau2WeightParameter = elementBundle.tau2WeightParameter.copyWithoutChildren();

    organize();
  }

  private void organize() {
    // Header gets the button that has removal capability
    isotopeHeaderParameter.setDecoration(new RemoveBundleDecoration<>(this));

    // header carries the children
    isotopeHeaderParameter.addUnconditionalChild(resistorParameter);
    isotopeHeaderParameter.addUnconditionalChild(temperatureParameter);
    isotopeHeaderParameter.addUnconditionalChild(tau1Parameter);
    isotopeHeaderParameter.addUnconditionalChild(tau2Parameter);
    isotopeHeaderParameter.addUnconditionalChild(tau1WeightParameter);
    isotopeHeaderParameter.addUnconditionalChild(tau2WeightParameter);
  }


  @Override
  public ParamBundle copy(SpawnControlParameter newParent) {
    ParamBundle copy = new FaradayBundle(this, newParent);
    return copy;
  }

  @Override
  public void readFromXmlElement(org.w3c.dom.Element xmlBundleElement) {
    /*
    Previously, the class that spawns this bundle parsed the xml file down to the "bundle node".

     <parameterBundle bundleID="elementBundle">
         <parameter defaultValue="Au (Gold)" label="Element" value="Mg (Magnesium)"
         xmlID="elementHeaderParameter"/>
         <parameter defaultValue="0.25" label="Mass fraction" value="0.25" xmlID="massFraction"/>
         <parameter defaultValue="15.0" label="Particle event rate" value="15.0" xmlID="eventRate"/>
     </parameterBundle>

     Now, we traverse through the parameters in the bundle node, similar to how a parameter set
     fills its parameter values from an xml.
     */

    // All parameters in this bundle
    NodeList parameterOfBundleList = xmlBundleElement.getElementsByTagName(XmlUtil.PAR_NODE);

    for (int i = 0; i < parameterOfBundleList.getLength(); i++) {

      // Each of these nodes should be a parameter.
      Node paramNode = parameterOfBundleList.item(i);

      // Check if the node really is an element.
      // (We already made sure that the tag "XmlUtil.PAR_NODE" is correct above).
      if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
        org.w3c.dom.Element paramElement = (org.w3c.dom.Element) paramNode;

        // This is the ID that tells us which parameter to fill.
        String paramXmlId = paramElement.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE);

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = null;
        if (paramXmlId.equals(wrapID("elementHeaderParameter"))) {
          par = isotopeHeaderParameter;
        }

        if (paramXmlId.equals(wrapID("resistorParameter"))) {
          par = resistorParameter;
        }

        if (paramXmlId.equals(wrapID("temperatureParameter"))) {
          par = temperatureParameter;
        }

        if (paramXmlId.equals(wrapID("tau1Parameter"))) {
          par = tau1Parameter;
        }

        if (paramXmlId.equals(wrapID("tau2Parameter"))) {
          par = tau2Parameter;
        }

        if (paramXmlId.equals(wrapID("tau1WeightParameter"))) {
          par = tau1WeightParameter;
        }

        if (paramXmlId.equals(wrapID("tau2WeightParameter"))) {
          par = tau2WeightParameter;
        }

        if (par != null) {
          par.readFromXmlElement(paramElement);
        }
      }
    }

  }

  /*
  When traversing the xml by retrieving all Parameter nodes,
  the xml will also return the bundled parameters.
  Hence, we must protect them to avoid confusion with a parameter of the same name somewhere at
  a higher level.
   */

  private String wrapID(String id) {
    return id + "_in_" + ParamBundle.BUNDLE_ID_FARADAY;
  }

  /*
  The parent/header parameter has all children (Otherwise, this would not be/feel like a bundle.
  "Self" refers to the header parameter.
  This method is required to get all parameters of a bundle when writing to an xml file
   */
  @Override
  public List<Parameter<?>> getSelfAndAllChildrenAllGenForXml() {
    List<Parameter<?>> parameters = isotopeHeaderParameter.getSelfAndAllChildrenAllGenForXml();
    return parameters;
  }

  @Override
  public Parameter<?> getHeaderParameter() {
    return isotopeHeaderParameter;
  }

  @Override
  public void requestRemoveSelf() {
    /*
     notifyItemChange() is called by the BUTTON that calls this method
     since both Bundle and the plain parentSpawnParameter environment is non-FX
     */
    parentSpawnPar.removeBundle(this);
  }


  @Override
  public String getSortingString() {
    return isotopeHeaderParameter.getValueAsString();
  }


  public Isotope getIsotope() {
    return isotopeHeaderParameter.getValue().unwrap();
  }

  public double[] getTauArr() {
    if (tau2WeightParameter.getValue() > 0) {
      return new double[]{tau1Parameter.getValue(),
          tau2Parameter.getValue()};
    } else {
      return new double[]{tau1Parameter.getValue()};
    }
  }

  public double[] getTauWeightArr() {
    if (tau2WeightParameter.getValue() > 0) {
      return new double[]{tau1WeightParameter.getValue(),
          tau2WeightParameter.getValue()};
    } else {
      return new double[]{tau1WeightParameter.getValue()};
    }
  }
}
