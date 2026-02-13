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
import io.XmlUtil;
import java.io.Serial;
import java.io.Serializable;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import util.NF;
import util.SnF;

public class DoubleSlideParameter extends AbstractParameter<Double> implements Serializable,
    Parameter<Double> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private Double value;
  private Double increment;
  private NF format;
  private NF incrementFormat;
  private final TextFormatterOption textFormatterOption;
  private Double min;
  private Double max;
  private double majTick;
  private final int minTicks;
  private final boolean snapToTicks;


  public DoubleSlideParameter(
      String label,
      String explanation,
      Double value,
      NF format,
      double increment,
      NF incrementFormat,
      TextFormatterOption textFormatterOption,
      double min, double max,
      boolean isLimitedToExperts,
      String xmlLabel) {
    super(label, explanation, value, isLimitedToExperts, xmlLabel);
    this.value = value;
    this.format = format;
    this.min = min;
    this.max = max;
    this.majTick = max;
    this.textFormatterOption = textFormatterOption;
    this.minTicks = -1;
    this.snapToTicks = false;
    this.increment = increment;
    this.incrementFormat = incrementFormat;
  }

  // quasi constructor
  public Parameter<Double> copyWithoutChildren() {
    Parameter<Double> copy = new DoubleSlideParameter(
        super.getLabel(),
        super.getExplanation(),
        value,
        format,
        increment,
        incrementFormat,
        textFormatterOption,
        min,
        max,
        super.isLimitedToExpert(),
        super.getXmlID()
    );
    copy.setDefaultValue(getDefaultValue());
    return copy;
  }

  @Override
  public void readFromXmlElement(Element xmlElement) {
    super.readFromXmlElement(xmlElement);
    String value = xmlElement.getAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE);
    String increment = xmlElement.getAttribute(XmlUtil.PAR_INCREMENT_ATTRIBUTE);
    String min = xmlElement.getAttribute(XmlUtil.PAR_MIN_ATTRIBUTE);
    String max = xmlElement.getAttribute(XmlUtil.PAR_MAX_ATTRIBUTE);
    String defaultValue = xmlElement.getAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE);
    String isExpertStr = xmlElement.getAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE);

    if (value != null && !value.isEmpty()) {
      setValue(SnF.strToDouble(StringEscapeUtils.unescapeXml(value)));
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      setDefaultValue(SnF.strToDouble(StringEscapeUtils.unescapeXml(defaultValue)));
    }

    if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
      setLimitedToExperts(true);
    }

    if (increment != null && !increment.isEmpty()) {
      setIncrement(SnF.strToDouble(StringEscapeUtils.unescapeXml(increment)));
    }

    if (min != null && !min.isEmpty()) {
      setMin(SnF.strToDouble(StringEscapeUtils.unescapeXml(min)));
    }

    if (max != null && !max.isEmpty()) {
      setMax(SnF.strToDouble(StringEscapeUtils.unescapeXml(max)));
      this.majTick = SnF.strToDouble(StringEscapeUtils.unescapeXml(max));
    }
  }

  @Override
  public void writeToXmlElement(Element xmlElement) {
    super.writeToXmlElement(xmlElement);
    xmlElement.setAttribute(XmlUtil.PAR_INCREMENT_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(SnF.doubleToString(increment, format)));
    xmlElement.setAttribute(XmlUtil.PAR_MIN_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(SnF.doubleToString(min, format)));
    xmlElement.setAttribute(XmlUtil.PAR_MAX_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(SnF.doubleToString(max, format)));
  }

  @Override
  public Double getValue() {
    return value;
  }

  public void setIncrement(Double increment) {
    this.increment = increment;
  }

  public Double getIncrement() {
    return increment;
  }

  public void setMax(Double max) {
    this.max = max;
  }

  public void setMin(Double min) {
    this.min = min;
  }

  @Override
  public String getValueAsString() {
    return SnF.doubleToString(value, format);
  }

  @Override
  public String getDefaultValueAsString() {
    return SnF.doubleToString(getDefaultValue(), format);
  }

  @Override
  public void setValue(Double d) {
    if (d != null) {
      this.value = d;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof DoubleSlideParameter) {
      double val = ((DoubleSlideParameter) par).getValue();
      setValue(val);
    }
  }

  @Override
  public FxParameter<Double> getObservableInstance() {
    return new DoubleSlideFxParameter(
        this,
        format,
        textFormatterOption,
        min, max,
        majTick,
        minTicks,
        snapToTicks,
        increment,
        incrementFormat);
  }

  // Specific methods

  public void setFormat(NF format) {
    this.format = format;
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof DoubleSlideParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((DoubleSlideParameter) other).getValue());
    return equivalent;
  }
}

