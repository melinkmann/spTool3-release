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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.MCSimIclShapeParameters;
import processing.parameterSets.ParamBundle;
import processing.parameters.AutoFillParam;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.Parameter;
import processing.parameters.SeparatorParameter;
import processing.parameters.SpawnControlParameter;
import util.NF;

public class ElementBundle implements ParamBundle {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private int index = -1;
  private final SpawnControlParameter parentSpawnPar;
  //
  public final Parameter<Fillable<Element>> elementHeaderParameter;
  public final Parameter<Double> backgroundSignalIntensity;
  public final Parameter<Double> massFraction;
  public final Parameter<Double> peakDelay;
  public final Parameter<MCSimIclShapeParameters> peakShape;

  public final Parameter<Double> d_mu;
  public final Parameter<Double> d_SD;

  public ElementBundle(SpawnControlParameter parent) {
    this.parentSpawnPar = parent;

    this.elementHeaderParameter = new AutoFillParam<>("Element",
        "Chemical element",
        Element.Au,
        Element.Au,
        TextFormatterOption.ALL_PASS,
        false,
        wrapID("elementHeaderParameter"));

    this.backgroundSignalIntensity = new DoubleParameter("Background [cts/DT]",
        """
            Background signal intensity of the respective element.
            The background signal is given in counts, i.e., counts per dwell time.
            The total background signal is calculated as the sum of all particle populations.""",
        5d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        wrapID("backgroundSignalIntensity"));

    massFraction = new DoubleParameter("Intensity fraction [0-1]",
        "'Mass' fraction of the respective element. Make sure that the fraction add up to 1.0."
            + "\nFor the scope of the data generator, we use the intensity fraction as a proxy for the mass" +
            " fraction"
            + "\nbecause the relationship of mass and intensity strongly depends on instrument tuning and " +
            "element",
        1d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        wrapID("massFraction"));

    peakDelay = new DoubleParameter("Peak delay [µs]",
        "Shift the maximum of an event peak with respect to the true simulated time of arrival",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_DOUBLE,
        true,
        wrapID("peakDelay"));

    peakShape = new ComboEnumParameter<>("Peak shape (D)",
        """
            Choose how the ion cloud profiles are generated.
            The key parameters are:
            Linear plasma flow velocity 'v', diffusion coefficient 'D', distance of vaporization 'y'.\
            Each parameter has a mean value ± a standard deviation (SD) to simulate random ion clouds.
            The default values can be changed in the configuration of spTool
            (Edit -> Configuration -> *scroll down*).
            Alternatively, it is also possible to select custom values here in this method
            """,
        MCSimIclShapeParameters.DEFAULT,
        MCSimIclShapeParameters.validOptions(),
        MCSimIclShapeParameters.class,
        true,
        wrapID("peakShape"));

    // custom pars of peakShape
    d_mu = new DoubleParameter("Diffusion coefficient (D) [cm2/s]",
        """
            Mean diffusion coefficient of the element in the plasma.
            Higher values lead to broader peaks.
            Why? When the diffusion coefficient of an ion is greater,
            the peak will spread out more strongly (i.e., the ion cloud become more diffuse).
            A good starting point is D=25 cm2/s
            but values closer to 100 cm2/s are also plausible (to obtain broader peaks)""",
        21d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        wrapID("d_mu"));

    d_SD = new DoubleParameter("Diffusion coefficient (±SD)",
        """
            Standard deviation of D.
            This number specifies how broadly the random numbers vary
            A good starting point is SD=5 cm2/s""",
        4d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        wrapID("d_SD"));

    organize();
  }

  // Copy

  public ElementBundle(ElementBundle elementBundle, SpawnControlParameter parent) {
    this.parentSpawnPar = parent;
    this.elementHeaderParameter = elementBundle.elementHeaderParameter.copyWithoutChildren();
    this.backgroundSignalIntensity = elementBundle.backgroundSignalIntensity.copyWithoutChildren();
    this.massFraction = elementBundle.massFraction.copyWithoutChildren();
    this.peakDelay = elementBundle.peakDelay.copyWithoutChildren();
    this.peakShape = elementBundle.peakShape.copyWithoutChildren();

    this.d_mu = elementBundle.d_mu.copyWithoutChildren();
    this.d_SD = elementBundle.d_SD.copyWithoutChildren();

    organize();
  }

  private void organize() {
    // Header gets the button that has removal capability
    elementHeaderParameter.setDecoration(new RemoveBundleDecoration<>(this));

    // header carries the children
    elementHeaderParameter.addUnconditionalChild(backgroundSignalIntensity);
    elementHeaderParameter.addUnconditionalChild(massFraction);
    elementHeaderParameter.addUnconditionalChild(peakDelay);
    elementHeaderParameter.addUnconditionalChild(peakShape);
    //
    peakShape.addConditionalChild(MCSimIclShapeParameters.CUSTOM, d_mu);
    peakShape.addConditionalChild(MCSimIclShapeParameters.CUSTOM, d_SD);
    //

    // Hacky way to add a separator line at the end of each element bundle
    // TODO: write wrapper classes that organize/replace "bundles".
    //  and would allow e.g., showing all bundle params as an editable table
    elementHeaderParameter.addUnconditionalChild(new SeparatorParameter());

  }


  @Override
  public ParamBundle copy(SpawnControlParameter newParent) {
    ParamBundle copy = new ElementBundle(this, newParent);
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
          par = elementHeaderParameter;
        }

        if (paramXmlId.equals(wrapID("backgroundSignalIntensity"))) {
          par = backgroundSignalIntensity;
        }

        if (paramXmlId.equals(wrapID("massFraction"))) {
          par = massFraction;
        }

        if (paramXmlId.equals(wrapID("peakDelay"))) {
          par = peakDelay;
        }

        if (paramXmlId.equals(wrapID("peakShape"))) {
          par = peakShape;
        }

        //
        if (paramXmlId.equals(wrapID("d_mu"))) {
          par = d_mu;
        }

        if (paramXmlId.equals(wrapID("d_SD"))) {
          par = d_SD;
        }
        //

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
    return id + "_in_" + ParamBundle.BUNDLE_ID_ELEMENT;
  }

  /*
  The parent/header parameter has all children (Otherwise, this would not be/feel like a bundle.
  "Self" refers to the header parameter.
  This method is required to get all parameters of a bundle when writing to an xml file
   */
  @Override
  public List<Parameter<?>> getSelfAndAllChildrenAllGenForXml() {
    List<Parameter<?>> parameters = elementHeaderParameter.getSelfAndAllChildrenAllGenForXml();
    return parameters;
  }

  @Override
  public Parameter<?> getHeaderParameter() {
    return elementHeaderParameter;
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
    return elementHeaderParameter.getValueAsString();
  }


  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // Default deserialization
    in.defaultReadObject();

    // backwards compatibility
    if (peakShape != null) {
      if (MCSimIclShapeParameters.isDeprecatedEnum(peakShape.getValue())) {
        peakShape.setValue(MCSimIclShapeParameters.DEFAULT);
      }
      if (MCSimIclShapeParameters.isDeprecatedEnum(peakShape.getDefaultValue())) {
        peakShape.setValue(MCSimIclShapeParameters.DEFAULT);
      }
    }
  }
}