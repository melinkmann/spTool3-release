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
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import sandbox.montecarlo.Isotope;
import util.Util;

public class ComboStringParameter extends AbstractParameter<String> implements
    Serializable, Parameter<String> {

  public static interface Matcher extends Serializable {

    @Serial
    long serialVersionUID = 1_000_000L;

    @Nullable
    String match(String inputString, String[] options);


    // TODO: make this a channel matcher
    // If direct lookup (e.g., for Isotope) fails, strip letters and make numeric comparison
    public static Matcher getIsotopeMatcher() {
      Matcher isotopeMatcher = new Matcher() {
        @Override
        public @Nullable
        String match(String inputString, String[] options) {
          Isotope searchItem = Isotope.guessFromString(inputString);
          // We assume that the options are isotopes with Full UI names
          for (String option : options) {
            Isotope candidate = Isotope.getFromFullUIName(option);
            if (candidate != null) {
              // can parse directly?
              if (candidate.equals(searchItem)) {
                return candidate.getFullUIName();
              }
            }
          }
          return null;
        }
      };
      return isotopeMatcher;
    }

  }

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private String value;
  private final String[] options;
  @Nullable
  private final Matcher stringMatcher;

  private final boolean showSearchField;

  // Enums can have children. E.g., if parameter is true, more options are needed.
  private final HashMap<String, List<Parameter<?>>> children;

  public ComboStringParameter(
      String label,
      String explanation,
      String value,
      String[] options,
      boolean showSearchField,
      @Nullable Matcher matcher,
      boolean isLimitedToExperts,
      String xmlID) {
    super(label, explanation, value, isLimitedToExperts, xmlID);
    this.value = value;
    this.options = options;
    this.showSearchField = showSearchField;
    this.children = new LinkedHashMap<>();
    this.stringMatcher = matcher;
  }


  public String[] values() {
    return options;
  }

  // quasi constructor
  public Parameter<String> copyWithoutChildren() {
    Parameter<String> copy = new ComboStringParameter(super.getLabel(),
        super.getExplanation(),
        value,
        options,
        showSearchField,
        stringMatcher,
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
    return value.toString();
  }

  @Override
  public String getDefaultValueAsString() {
    return value.toString();
  }

  @Override
  public void setValue(String value) {
    if (value != null) {
      this.value = value;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof ComboStringParameter) {
      String val = ((ComboStringParameter) par).getValue();
      setValue(val);
    }
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
    for (String key : children.keySet()) {

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
    for (String key : children.keySet()) {
      setWithAll.addAll(children.get(key));
    }
    return new ArrayList<>(setWithAll);
  }

  @Override
  public FxParameter<String> getObservableInstance() {
    return new ComboStringFxParameter(this, options, stringMatcher);
  }

  // Add depending parameters.
  public void addConditionalChild(String t, List<Parameter<?>> conditionalParameters) {
    Util.put(children, t, conditionalParameters);
    conditionalParameters.forEach(p -> p.setIsChild(true));
  }

  public void addConditionalChild(List<String> list, Parameter<?>... conditionalParameters) {
    for (String t : list) {
      addConditionalChild(t, conditionalParameters);
    }
  }

  @Override
  public void addConditionalChild(String[] arr, Parameter<?>... conditionalParameters) {
    addConditionalChild(new ArrayList<>(Arrays.asList(arr)), conditionalParameters);
  }

  public void addConditionalChild(String t, Parameter<?>... conditionalParameters) {
    // Wrap the "as list" --> https://stackoverflow.com/questions/9320409/unsupportedoperationexception-at-java-util-abstractlist-add
    addConditionalChild(t, new ArrayList<>(Arrays.asList(conditionalParameters)));
  }

  public void addConditionalChild(String t, Parameter<?> conditionalParameter) {
    Util.put(children, t, conditionalParameter);
    conditionalParameter.setIsChild(true);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof ComboStringParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((ComboStringParameter) other).getValue());
    return equivalent;
  }

  // Show/Hide the decoration for this special case
  public boolean isShowSearchField() {
    return showSearchField;
  }

}
