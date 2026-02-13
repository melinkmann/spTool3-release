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

import gui.util.TextFormatterOption;
import gui.util.TextFormatterSupplier;
import io.XmlUtil;
import java.io.Serial;
import java.io.Serializable;
import javafx.scene.control.TextFormatter;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;

public class ReadOnlyTextParameter extends AbstractParameter<String> implements Serializable,
    Parameter<String> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private String value;
  // Supplier allows to call the same method again. Otherwise, the same formatter will be used in various UI controls at the same time.
  private final TextFormatterOption textFormatterOption; // has to be a SOURCE, i.e. a method reference!

  public ReadOnlyTextParameter(String label,
      String explanation,
      String value,
      TextFormatterOption textFormatterOption,
      boolean isLimitedToExperts,
      String xmlLabel) {
    super(label, explanation, value, isLimitedToExperts, xmlLabel);
    this.value = value;
    this.textFormatterOption = textFormatterOption;
  }

  // quasi constructor
  public Parameter<String> copyWithoutChildren() {
    Parameter<String> copy = new ReadOnlyTextParameter(super.getLabel(),
        super.getExplanation(),
        value,
        textFormatterOption,
        super.isLimitedToExpert(),
        super.getXmlID());
    copy.setDefaultValue(getDefaultValue());
    return copy;
  }

  @Override
  public void readFromXmlElement(Element xmlElement) {
    super.readFromXmlElement(xmlElement);
    String value = xmlElement.getAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE);
    String defaultValue = xmlElement.getAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE);
    String isExpertStr = xmlElement.getAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE);


    if (value != null && !value.isEmpty()) {
      this.value = StringEscapeUtils.unescapeXml(value);
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      super.setDefaultValue(StringEscapeUtils.unescapeXml(defaultValue));
    }

    if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
      setLimitedToExperts(true);
    }

  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String getValueAsString() {
    return value;
  }

  @Override
  public String getDefaultValueAsString() {
    return getDefaultValue();
  }

  @Override
  public void setValue(String s) {
    //Do nothing!
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    // Do nothing!
  }

  @Override
  public void resetToDefault() {
    // Do not do this!
  }

  @Override
  public FxParameter<String> getObservableInstance() {
    TextFormatter<String> formatter = TextFormatterSupplier.get(textFormatterOption, value);
    formatter.setValue(value);
    return new ReadOnlyTextFxParameter(this, formatter);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof ReadOnlyTextParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((ReadOnlyTextParameter) other).getValue());
    return equivalent;
  }
}