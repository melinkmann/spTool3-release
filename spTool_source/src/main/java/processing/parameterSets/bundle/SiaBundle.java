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
import java.io.Serial;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameterSets.ParamBundle;
import processing.parameters.AutoFillParam;
import processing.parameters.DoubleParameter;
import processing.parameters.Parameter;
import processing.parameters.SpawnControlParameter;
import util.NF;

public class SiaBundle implements ParamBundle {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private int index = -1;
  private final SpawnControlParameter parentSpawnPar;
  //
  public final Parameter<Fillable<Element>> elementHeaderParameter;

  public final Parameter<Double> siaShapeParameter;

  public SiaBundle(SpawnControlParameter parent) {
    this.parentSpawnPar = parent;

    this.elementHeaderParameter = new AutoFillParam<>("Element",
        "Chemical element",
        Element.Au,
        Element.Au,
        TextFormatterOption.ALL_PASS,
        false,
        wrapID("elementHeaderParameter"));

    this.siaShapeParameter = new DoubleParameter("SIA shape",
        "The single ion area histogram is assumed to have a lognormal distribution."
            + "\nIts location parameter mu is calculated to yield an expected value of one."
            + "\nThe shape parameter is usually called 'sigma' and is on the order of 0.47",
        0.47,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        wrapID("siaShapeParameter"));

    organize();
  }

  // Copy

  public SiaBundle(SiaBundle elementBundle, SpawnControlParameter parent) {
    this.parentSpawnPar = parent;
    this.elementHeaderParameter = elementBundle.elementHeaderParameter.copyWithoutChildren();
    this.siaShapeParameter = elementBundle.siaShapeParameter.copyWithoutChildren();

    organize();
  }

  private void organize() {
    // Header gets the button that has removal capability
    elementHeaderParameter.setDecoration(new RemoveBundleDecoration<>(this));

    // header carries the children
    elementHeaderParameter.addUnconditionalChild(siaShapeParameter);
  }


  @Override
  public ParamBundle copy(SpawnControlParameter newParent) {
    ParamBundle copy = new SiaBundle(this, newParent);
    return copy;
  }

  @Override
  public void readFromXmlElement(org.w3c.dom.Element xmlBundleElement) {
    /*
    Previously, the class that spawns this bundle parsed the xml file down to the "bundle node".

     <parameterBundle bundleID="elementBundle">
         <parameter defaultValue="Au (Gold)" label="Element" value="Mg (Magnesium)" xmlID="elementHeaderParameter"/>
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

        if (paramXmlId.equals(wrapID("siaShapeParameter"))) {
          par = siaShapeParameter;
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
    return id + "_in_" + ParamBundle.BUNDLE_ID_SIA;
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
}
