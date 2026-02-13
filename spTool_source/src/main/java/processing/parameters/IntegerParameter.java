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
import util.NF;
import util.SnF;

public class IntegerParameter extends AbstractParameter<Integer> implements Serializable,
    Parameter<Integer> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private Integer value;
  private NF format = NF.D1C0;
  // Determines, pos vs. neg, double vs. int, ... i.e more than just UI stuff.
  private final TextFormatterOption textFormatterOption; // has to be a SOURCE, i.e. a method reference!

  public IntegerParameter(String label,
      String explanation,
      Integer value,
      TextFormatterOption textFormatterOption,
      boolean isLimitedToExperts,
      String xmlID) {
    super(label, explanation, value, isLimitedToExperts, xmlID);
    this.value = value;
    this.textFormatterOption = textFormatterOption;
  }


  // quasi constructor
  public Parameter<Integer> copyWithoutChildren() {
    Parameter<Integer> copy = new IntegerParameter(super.getLabel(),
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
      setValue(SnF.strToInt(StringEscapeUtils.unescapeXml(value)));
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      setDefaultValue(SnF.strToInt(StringEscapeUtils.unescapeXml(defaultValue)));
    }

    if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
      setLimitedToExperts(true);
    }
  }

  @Override
  public Integer getValue() {
    return value;
  }

  @Override
  public String getValueAsString() {
    return SnF.intToString(value, format);
  }

  @Override
  public String getDefaultValueAsString() {
    return SnF.intToString(getDefaultValue(), format);
  }

  @Override
  public void setValue(Integer i) {
    if (i != null) {
      this.value = i;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof IntegerParameter) {
      int val = ((IntegerParameter) par).getValue();
      setValue(val);
    }
  }

  @Override
  public FxParameter<Integer> getObservableInstance() {
    TextFormatter<Integer> formatter = TextFormatterSupplier.get(textFormatterOption, value);
    formatter.setValue(value);
    return new IntegerFxParameter(this, format, formatter);
  }

  // Specific methods

  public void setFormat(NF format) {
    this.format = format;
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof IntegerParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((IntegerParameter) other).getValue());
    return equivalent;
  }

}