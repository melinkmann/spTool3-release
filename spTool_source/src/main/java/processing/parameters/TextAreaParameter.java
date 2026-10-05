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
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;

import java.io.Serial;
import java.io.Serializable;

public class TextAreaParameter extends AbstractParameter<String> implements Serializable,
    Parameter<String> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private String value;

  public TextAreaParameter(String label,
                           String explanation,
                           String value,
                           boolean isLimitedToExperts,
                           String xmlLabel) {
    super(label, explanation, value,isLimitedToExperts, xmlLabel);
    this.value = value;
  }

  // quasi constructor
  public Parameter<String> copyWithoutChildren() {
    Parameter<String> copy = new TextAreaParameter(super.getLabel(),
        super.getExplanation(),
        value,
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
      setValue(StringEscapeUtils.unescapeXml(value));
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      setDefaultValue(StringEscapeUtils.unescapeXml(defaultValue));
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
    if (s != null) {
      this.value = s;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof TextAreaParameter) {
      String val = ((TextAreaParameter) par).getValue();
      setValue(val);
    }
  }


  @Override
  public FxParameter<String> getObservableInstance() {
    return new CommentFxParameter(this);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof TextAreaParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((TextAreaParameter) other).getValue());
    return equivalent;
  }
}