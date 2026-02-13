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

package processing.parameters;

import io.XmlUtil;
import java.io.Serial;
import java.io.Serializable;
import javafx.scene.control.Button;
import org.w3c.dom.Element;
import util.SupplierSerializable;

public class ButtonParameter extends AbstractParameter<String> implements Serializable,
    Parameter<String> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private SupplierSerializable<Button> buttonSupplier;

  public ButtonParameter(
      String label,
      String explanation,
      SupplierSerializable<Button> buttonSupplier,
      boolean isLimitedToExperts,
      String xmlLabel) {
    super(label, explanation, "No value", isLimitedToExperts, xmlLabel);
    this.buttonSupplier = buttonSupplier;
  }

  // quasi constructor
  public Parameter<String> copyWithoutChildren() {
    Parameter<String> copy = new ButtonParameter(
        super.getLabel(),
        super.getExplanation(),
        buttonSupplier,
        super.isLimitedToExpert(),
        super.getXmlID());
    return copy;
  }

  @Override
  public void readFromXmlElement(Element xmlElement) {
    super.readFromXmlElement(xmlElement);
    String isExpertStr = xmlElement.getAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE);

    if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
      setLimitedToExperts(true);
    }
  }

  @Override
  public String getValue() {
    return "";
  }


  @Override
  public String getValueAsString() {
    return "";
  }

  @Override
  public String getDefaultValueAsString() {
    return "";
  }

  @Override
  public void setValue(String s) {
    // Do nothing
  }

  @Override
  public void trySetValue(Parameter<?> par) {
  // do nothing
  }

  @Override
  public FxParameter<String> getObservableInstance() {
    return new ButtonFxParameter(this);
  }

  // Specific methods


  public SupplierSerializable<Button> getButtonSupplier() {
    return buttonSupplier;
  }

  public void setButtonSupplier(SupplierSerializable<Button> buttonSupplier) {
    this.buttonSupplier = buttonSupplier;
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof ButtonParameter;
    return equivalent;
  }
}

