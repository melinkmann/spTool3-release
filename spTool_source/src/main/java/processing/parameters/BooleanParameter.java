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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import util.Util;

public class BooleanParameter extends AbstractParameter<Boolean> implements Serializable,
    Parameter<Boolean> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private Boolean value;

  // Booleans can have children. E.g., if parameter is true, more options are needed.
  private final HashMap<Boolean, List<Parameter<?>>> children;


  public BooleanParameter(String label,
                          String explanation,
                          Boolean value,
                          boolean isLimitedToExperts,
                          String xmlLabel) {
    super(label, explanation, value, isLimitedToExperts, xmlLabel);
    this.value = value;
    this.children = new LinkedHashMap<>();
  }

  public BooleanParameter(String label,
                          String boxLabel,
                          String explanation,
                          Boolean value,
                          boolean isLimitedToExperts,
                          String xmlLabel) {
    super(label, boxLabel, explanation, value, isLimitedToExperts, xmlLabel);
    this.value = value;
    this.children = new LinkedHashMap<>();
  }

  // quasi constructor
  public Parameter<Boolean> copyWithoutChildren() {
    Parameter<Boolean> copy = new BooleanParameter(
        super.getLabel(),
        super.getSecondaryLabel(),
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
      setValue(Boolean.valueOf(StringEscapeUtils.unescapeXml(value)));
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      setDefaultValue(Boolean.valueOf(StringEscapeUtils.unescapeXml(defaultValue)));
    }

    if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
      setLimitedToExperts(true);
    }

  }

  @Override
  public Boolean getValue() {
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
    for (Boolean key : children.keySet()) {

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
    for (Boolean key : children.keySet()) {
      setWithAll.addAll(children.get(key));
    }
    return new ArrayList<>(setWithAll);
  }

  @Override
  public String getValueAsString() {
    return Boolean.toString(value);
  }

  @Override
  public String getDefaultValueAsString() {
    return Boolean.toString(getDefaultValue());
  }

  @Override
  public void setValue(Boolean value) {
    if (value != null) {
      this.value = value;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof BooleanParameter) {
      boolean val = ((BooleanParameter) par).getValue();
      setValue(val);
    }
  }

  @Override
  public FxParameter<Boolean> getObservableInstance() {
    return new BooleanFxParameter(this);
  }

  // Add depending parameters.
  public void addConditionalChild(Boolean b, List<Parameter<?>> conditionalParameters) {
    Util.put(children, b, conditionalParameters);
    conditionalParameters.forEach(p -> p.setIsChild(true));
  }

  public void putConditionalChild(Boolean b, Parameter<?>... conditionalParameters) {
    List<Parameter<?>> parList = new ArrayList<>(Arrays.asList(conditionalParameters));
    children.put(b, parList);
    parList.forEach(p -> p.setIsChild(true));
  }

  @Override
  public void clearChildren() {
    children.clear();
  }

  public void addConditionalChild(Boolean b, Parameter<?>... conditionalParameters) {
    children.put(b, new ArrayList<>(Arrays.asList(conditionalParameters)));
    Arrays.stream(conditionalParameters).forEach(p -> p.setIsChild(true));
  }

  public void addConditionalChild(Boolean b, Parameter<?> conditionalParameter) {
    Util.put(children, b, conditionalParameter);
    conditionalParameter.setIsChild(true);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof BooleanParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((BooleanParameter) other).getValue());
    return equivalent;
  }
}
