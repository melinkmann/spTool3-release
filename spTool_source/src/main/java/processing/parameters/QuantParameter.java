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

import analysis.quant.Cal;
import core.SpTool3Main;
import dataModelNew.Sample;
import gui.util.TextFormatterOption;
import gui.util.TextFormatterSupplier;
import io.XmlUtil;
import javafx.scene.control.TextFormatter;
import math.units.Unit;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import util.NF;
import util.SnF;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public class QuantParameter<T extends Enum<T> & Unit> extends AbstractParameter<Double> implements Serializable,
    Parameter<Double> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final String valueID = "QuantParameter_VALUE";
  private static final String unitID = "QuantParameter_UNIT";

  private Double value;
  private NF format;
  // Determines, pos vs. neg, double vs. int, ... i.e more than just UI stuff.
  // has to be a SOURCE, i.e. a method reference!
  private final TextFormatterOption textFormatterOption;

  private T unit;
  private T defaultUnit;
  private final T[] unitOptions;
  private final Class<T> unitClass;

  private final boolean armSetAllButtons; // remember for copy


  public QuantParameter(String label,
                        String explanation,
                        Double value,
                        NF format,
                        TextFormatterOption textFormatterOption,
                        T unit,
                        T[] unitOptions,
                        Class<T> unitClass,
                        boolean isLimitedToExperts,
                        boolean armSetAllButtons,
                        String xmlLabel) {
    super(label, explanation, value, isLimitedToExperts, xmlLabel);
    this.value = value;
    this.format = format;
    this.textFormatterOption = textFormatterOption;
    this.unit = unit;
    this.defaultUnit = unit;
    this.unitOptions = unitOptions;
    this.unitClass = unitClass;

    if (armSetAllButtons) {
      activateDecoration();
    }
    this.armSetAllButtons = armSetAllButtons;
  }

  // For the copy
  public QuantParameter(String label,
                        String explanation,
                        Double value,
                        NF format,
                        TextFormatterOption textFormatterOption,
                        T unit,
                        T defaultUnit,
                        T[] unitOptions,
                        Class<T> unitClass,
                        boolean isLimitedToExperts,
                        boolean armSetAllButtons,
                        String xmlLabel) {
    super(label, explanation, value, isLimitedToExperts, xmlLabel);
    this.value = value;
    this.format = format;
    this.textFormatterOption = textFormatterOption;
    this.unit = unit;
    this.defaultUnit = defaultUnit;
    this.unitOptions = unitOptions;
    this.unitClass = unitClass;
    this.armSetAllButtons = armSetAllButtons;

    if (armSetAllButtons) {
      activateDecoration();
    }
  }

  // quasi constructor
  @Override
  public Parameter<Double> copyWithoutChildren() {
    Parameter<Double> copy = new QuantParameter(
        super.getLabel(),
        super.getExplanation(),
        value,
        format,
        textFormatterOption,
        unit,
        defaultUnit,
        unitOptions,
        unitClass,
        super.isLimitedToExpert(),
        armSetAllButtons,
        super.getXmlID());
    copy.setDefaultValue(getDefaultValue());
    return copy;
  }


  @Override
  public void readFromXmlElement(Element xmlElement) {
    super.readFromXmlElement(xmlElement);
    String value = xmlElement.getAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE + "_" + valueID);
    String defaultValue = xmlElement.getAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE + "_" + valueID);

    String unit = xmlElement.getAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE + "_" + unitID);
    String defaultUnit = xmlElement.getAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE + "_" + unitID);

    String isExpertStr = xmlElement.getAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE);

    if (value != null && !value.isEmpty()) {
      setValue(SnF.strToDouble(StringEscapeUtils.unescapeXml(value)));
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      setDefaultValue(SnF.strToDouble(StringEscapeUtils.unescapeXml(defaultValue)));
    }

    if (unit != null && !unit.isEmpty()) {
      this.unit = Enum.valueOf(unitClass, StringEscapeUtils.unescapeXml(unit).toUpperCase());
    }

    try {
      if (defaultUnit != null && !defaultUnit.isEmpty()) {
        this.defaultUnit = Enum.valueOf(unitClass, StringEscapeUtils.unescapeXml(defaultUnit).toUpperCase());
      }

      if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
        setLimitedToExperts(true);
      }
    } catch (Exception e) {
      LOGGER.info("Could not find the literal match for a parameter: " + getUnit() + ". "
          + "You are probably loading an older method that does not have the new parameter "
          + "or their names may have changed. The issue was resolved by using the following values: "
          + "For the VALUE: Error parsing enum names from XML. Read from XML: " + unit
          + ". Actually used: " + getUnit().toString() + ".");
    }
  }

  @Override
  public void writeToXmlElement(Element xmlElement) {
    xmlElement.setAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getXmlID()));

    xmlElement.setAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE + "_" + valueID,
        StringEscapeUtils.escapeXml10(getValueAsString())); // DIFFERS HERE
    xmlElement.setAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE + "_" + valueID,
        StringEscapeUtils.escapeXml10(getDefaultValueAsString())); // DIFFERS HERE

    xmlElement.setAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE + "_" + unitID,
        StringEscapeUtils.escapeXml10(unit.name())); // DIFFERS HERE
    xmlElement.setAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE + "_" + unitID,
        StringEscapeUtils.escapeXml10(defaultUnit.name())); // DIFFERS HERE

    xmlElement.setAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(isLimitedToExpert().toString().toLowerCase(Locale.ROOT)));
    xmlElement.setAttribute(XmlUtil.PAR_XML_LABEL_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getLabel()));
  }

  @Override
  public Double getValue() {
    return value;
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
    if (par instanceof QuantParameter) {
      try {
        double val = ((QuantParameter) par).getValue();
        T unit = ((QuantParameter<T>) par).getUnit();
        setValue(val);
        setUnit(unit);
      } catch (Exception e) {
        LOGGER.error("Cannot cast.");
      }
    }
  }

  public T getUnit() {
    return unit;
  }

  public void setUnit(T unit) {
    this.unit = unit;
  }

  public T getDefaultUnit() {
    return defaultUnit;
  }

  public void setDefaultUnit(T defaultUnit) {
    this.defaultUnit = defaultUnit;
  }

  @Override
  public FxParameter<Double> getObservableInstance() {
    TextFormatter<Double> formatter = TextFormatterSupplier.get(textFormatterOption, value);
    formatter.setValue(value);
    return new QuantFxParameter(this, format, formatter, unitOptions);
  }

  // Specific methods

  public void setFormat(NF format) {
    this.format = format;
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof QuantParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((QuantParameter) other).getValue());
    equivalent = equivalent && this.unit.equals(((QuantParameter) other).unit);
    equivalent = equivalent && this.defaultUnit.equals(((QuantParameter) other).defaultUnit);
    return equivalent;
  }


  private void activateDecoration() {
    setDecoration(new ButtonDecoration<>(
        "Set this value to all selected samples",
        "/img/grouped.png",
        () -> {
          List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
          for (Sample selSample : selSamples) {
            Cal quant = selSample.getQuant();
            for (Parameter<?> par : quant.getExperimentalConditions().listAllParameters()) {
              if (par.getXmlID().equals(getXmlID())) {
                if (par instanceof QuantParameter) {
                  try {
                    par.trySetValue(this);
                    // this should be fine due to the xml check
                    // ((QuantParameter) par).setUnit(unit);
                    // ((QuantParameter) par).setValue(value);
                  } catch (Exception e) {
                    LOGGER.error("Cannot set value as types do not match!");
                  }
                }
              }
            }
          }
        }));
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    if (armSetAllButtons) {
      activateDecoration();
    }
  }

}
