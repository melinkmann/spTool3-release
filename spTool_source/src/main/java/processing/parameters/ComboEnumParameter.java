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
import java.util.*;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import util.Util;

public class ComboEnumParameter<T extends Enum<T>> extends AbstractParameter<T> implements
    Serializable, Parameter<T> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private T value;
  private final T[] options;
  private final Class<T> enumClass;

  // Enums can have children. E.g., if parameter is true, more options are needed.
  private final HashMap<T, List<Parameter<?>>> children;

  public ComboEnumParameter(
      String label,
      String explanation,
      T value,
      T[] options,
      Class<T> enumClass,
      boolean isLimitedToExperts,
      String xmlID) {
    super(label, explanation, value, isLimitedToExperts, xmlID);
    this.value = value;
    this.options = options;
    this.enumClass = enumClass;
    this.children = new LinkedHashMap<>();
  }


  public T[] values() {
    return enumClass.getEnumConstants();
  }

  // quasi constructor
  public Parameter<T> copyWithoutChildren() {
    Parameter<T> copy = new ComboEnumParameter<>(super.getLabel(),
        super.getExplanation(),
        value,
        options,
        enumClass,
        super.isLimitedToExpert(),
        super.getXmlID());
    copy.setDefaultValue(getDefaultValue());
    return copy;
  }


  // https://www.baeldung.com/java-fix-no-enum-const-class
  // https://coderanch.com/t/417439/java/Generics-Enum-type-values
  @Override
  public void readFromXmlElement(Element xmlElement) {
    super.readFromXmlElement(xmlElement);
    String value = xmlElement.getAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE);
    String defaultValue = xmlElement.getAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE);
    String isExpertStr = xmlElement.getAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE);

    try {
      if (value != null && !value.isEmpty()) {
        setValue(Enum.valueOf(enumClass, StringEscapeUtils.unescapeXml(value).toUpperCase()));
      }

      if (defaultValue != null && !defaultValue.isEmpty()) {
        setDefaultValue(Enum.valueOf(enumClass,
            StringEscapeUtils.unescapeXml(defaultValue).toUpperCase()));
      }

      if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
        setLimitedToExperts(true);
      }

    } catch (Exception e1) {
      // Element has non-capitalized spelling. Check if this is the case.
      try {
        if (value != null && !value.isEmpty()) {
          setValue(Enum.valueOf(enumClass, StringEscapeUtils.unescapeXml(value)));
        }

        if (defaultValue != null && !defaultValue.isEmpty()) {
          setDefaultValue(Enum.valueOf(enumClass,
              StringEscapeUtils.unescapeXml(defaultValue)));
        }
      } catch (Exception e) {
        LOGGER.info("Could not find the literal match for a parameter: " + getLabel() + ". "
            + "You are probably loading an older method that does not have the new parameter "
            + "or their names may have changed. The issue was resolved by using the following values: "
            + "For the VALUE: Error parsing enum names from XML. Read from XML: " + value
            + ". Actually used: " + getValue().toString() + "." +
            " For the DEFAULT VALUE: Error parsing enum names from XML. Read from XML: "
            + defaultValue
            + ". Actually used: " + getDefaultValue().toString() + ".");
      }
    }
  }

  // Override because we need the (ideally capitalized) true enum NAME (.name() and not .toString())
  @Override
  public void writeToXmlElement(Element xmlElement) {
    xmlElement.setAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getXmlID()));
    xmlElement.setAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getValue().name())); // DIFFERS HERE
    xmlElement.setAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getDefaultValue().name())); // DIFFERS HERE
    xmlElement.setAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(isLimitedToExpert().toString().toLowerCase(Locale.ROOT)));
    xmlElement.setAttribute(XmlUtil.PAR_XML_LABEL_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getLabel()));
    // xmlElement.setAttribute("explanation", getExplanation());
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public List<Parameter<?>> getActiveChildrenAllGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getActiveChildrenAllGen());

    if (children.containsKey(value)) {
      List<Parameter<?>> matchedChildren = children.get(value);
      for (Parameter<?> child : matchedChildren) {
        // Add the child (first generation)
        setWithAll.add(child);
        // Now also call on the child's children
        setWithAll.addAll(child.getActiveChildrenAllGen());
      }
    }

    return new ArrayList<>(setWithAll);
  }


  @Override
  public List<Parameter<?>> getActiveChildrenFirstGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getActiveChildrenFirstGen());

    if (children.containsKey(value)) {
      List<Parameter<?>> matchedChildren = children.get(value);
      setWithAll.addAll(matchedChildren);
    }
    return new ArrayList<>(setWithAll);
  }

  @Override
  public List<Parameter<?>> getAllChildrenAllGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getAllChildrenAllGen());

    // Add children regardless the current value.
    for (T key : children.keySet()) {

      List<Parameter<?>> matchedChildren = children.get(key);
      for (Parameter<?> child : matchedChildren) {
        // Add the child (first generation)
        setWithAll.add(child);
        // Now also call on the child's children
        setWithAll.addAll(child.getAllChildrenAllGen());
      }
    }
    return new ArrayList<>(setWithAll);
  }

  @Override
  public List<Parameter<?>> getAllChildrenFirstGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getAllChildrenFirstGen());
    // Add children regardless the current value.
    for (T key : children.keySet()) {
      setWithAll.addAll(children.get(key));
    }
    return new ArrayList<>(setWithAll);
  }

  @Override
  public String getValueAsString() {
    return value.toString();
  }

  @Override
  public String getDefaultValueAsString() {
    return value.toString();
  }

  @Override
  public void setValue(T value) {
    if (value != null) {
      this.value = value;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof ComboEnumParameter<?>) {
      try {
        T val = ((ComboEnumParameter<T>) par).getValue();
        setValue(val);
      } catch (Exception e) {
        LOGGER.error("Cannot cast.");
      }
    }
  }

  @Override
  public FxParameter<T> getObservableInstance() {
    return new ComboEnumFxParameter<T>(this, options);
  }

  // Add depending parameters.
  public void addConditionalChild(T t, List<Parameter<?>> conditionalParameters) {
    Util.put(children, t, conditionalParameters);
    conditionalParameters.forEach(p -> p.setIsChild(true));
  }

  // Add depending parameters.
  public void putConditionalChild(T t, Parameter<?>... conditionalParameters) {
    List<Parameter<?>> parList = new ArrayList<>(Arrays.asList(conditionalParameters));
    children.put(t, parList);
    parList.forEach(p -> p.setIsChild(true));
  }

  @Override
  public void clearChildren() {
    children.clear();
  }

  public void addConditionalChild(List<T> list, Parameter<?>... conditionalParameters) {
    for (T t : list) {
      addConditionalChild(t, conditionalParameters);
    }
  }

  @Override
  public void addConditionalChild(T[] arr, Parameter<?>... conditionalParameters) {
    addConditionalChild(new ArrayList<>(Arrays.asList(arr)), conditionalParameters);
  }

  public void addConditionalChild(T t, Parameter<?>... conditionalParameters) {
    // Wrap the "as list" --> https://stackoverflow.com/questions/9320409/unsupportedoperationexception-at-java-util-abstractlist-add
    addConditionalChild(t, new ArrayList<>(Arrays.asList(conditionalParameters)));
  }

  public void addConditionalChild(T t, Parameter<?> conditionalParameter) {
    List<Parameter<?>> values = new ArrayList<>();
    values.add(conditionalParameter);
    Util.put(children, t, values);
    conditionalParameter.setIsChild(true);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof ComboEnumParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((ComboEnumParameter<?>) other).getValue());
    return equivalent;
  }

}
