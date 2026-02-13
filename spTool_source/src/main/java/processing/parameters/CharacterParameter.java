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
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;

public class CharacterParameter extends AbstractParameter<Character> implements Serializable,
    Parameter<Character> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private Character value;

  public CharacterParameter(String label,
      String explanation,
      Character value,
      boolean isLimitedToExperts,
      String xmlLabel) {
    super(label, explanation, value, isLimitedToExperts, xmlLabel);
    this.value = value;
  }

  // quasi constructor
  public Parameter<Character> copyWithoutChildren() {
    Parameter<Character> copy = new CharacterParameter(super.getLabel(),
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
      setValue(StringEscapeUtils.unescapeXml(value).charAt(0));
    }
    if (defaultValue != null && !defaultValue.isEmpty()) {
      setDefaultValue(StringEscapeUtils.unescapeXml(defaultValue).charAt(0));
    }

    if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
      setLimitedToExperts(true);
    }

  }

  @Override
  public Character getValue() {
    return value;
  }


  @Override
  public String getValueAsString() {
    return String.valueOf(value);
  }

  @Override
  public String getDefaultValueAsString() {
    return String.valueOf(getDefaultValue());
  }

  @Override
  public void setValue(Character character) {
    if (character != null) {
      this.value = character;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof CharacterParameter) {
      char val = ((CharacterParameter) par).getValue();
      setValue(val);
    }
  }

  @Override
  public FxParameter<Character> getObservableInstance() {
    return new CharacterFxParameter(this);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof CharacterParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((CharacterParameter) other).getValue());
    return equivalent;
  }
}
