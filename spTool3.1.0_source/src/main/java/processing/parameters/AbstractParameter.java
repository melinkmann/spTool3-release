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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import processing.parameterSets.ParamBundle;

public abstract class AbstractParameter<T extends Serializable> implements Serializable,
    Parameter<T> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final String label;
  private final String explanation;
  private T defaultValue;

  // Hide certain options from the UI
  private boolean isLimitedToExperts = false;

  // Add label to the box specifically
  private final String secondaryLabel;

  private final String xmlID;

  private boolean isChild = false;
  private int childLevel = 0;

  protected final List<Parameter<?>> unconditionalChildren;

  // Do not serialize this as decorations may contain FX stuff.
  // Keyword transient makes sure, initialize to null (just like here).
  // If required, the value is set in the organize() function of a ParamSet.
  private transient Decoration<T> decoration = null;

  public AbstractParameter(String label,
      String explanation,
      T defaultValue,
      boolean isLimitedToExperts,
      String xmlID) {
    this.label = label;
    this.secondaryLabel = label;
    this.explanation = explanation;
    this.defaultValue = defaultValue;
    this.isLimitedToExperts = isLimitedToExperts;
    this.xmlID = xmlID;
    this.unconditionalChildren = new ArrayList<>();
  }

  public AbstractParameter(String label,
      String secondaryLabel,
      String explanation,
      T defaultValue,
      boolean isLimitedToExperts,
      String xmlID) {
    this.label = label;
    this.secondaryLabel = secondaryLabel;
    this.explanation = explanation;
    this.defaultValue = defaultValue;
    this.isLimitedToExperts = isLimitedToExperts;
    this.xmlID = xmlID;
    this.unconditionalChildren = new ArrayList<>();
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public String getSecondaryLabel() {
    return secondaryLabel;
  }

  @Override
  public String getExplanation() {
    return explanation;
  }

  @Override
  public T getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String getXmlID() {
    return xmlID;
  }

  @Override
  public void writeToXmlElement(Element xmlElement) {
    xmlElement.setAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getXmlID()));
    xmlElement.setAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getValueAsString()));
    xmlElement.setAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getDefaultValueAsString()));
    xmlElement.setAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(isLimitedToExpert().toString().toLowerCase(Locale.ROOT)));
    xmlElement.setAttribute(XmlUtil.PAR_XML_LABEL_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(getLabel()));
  }

  @Override
  public void writeBundleMetaDataToBundleNode(Element xmlElement) {
    // A bundle spawning parameter must override this method
  }

  @Override
  public void readFromXmlElement(Element xmlElement) {
    // so far, do nothing.
  }

  @Override
  public void setDefaultValue(T initialValue) {
    this.defaultValue = initialValue;
  }

  @Override
  public void setCurrentValueAsDefault() {
    setDefaultValue(getValue());
  }

  @Override
  public void resetToDefault() {
    setValue(defaultValue);
  }

  public void setIsChild(boolean isChild) {
    this.isChild = isChild;
  }

  @Override
  public boolean isChild() {
    return isChild;
  }

  @Override
  public void setLimitedToExperts(boolean limitedToExperts) {
    isLimitedToExperts = limitedToExperts;
  }

  @Override
  public Boolean isLimitedToExpert() {
    return isLimitedToExperts;
  }

  @Override
  public void determineChildLevels(int ownLevel) {
    if (!isChild) {
      ownLevel = 0; // used to be 1. But I think it should be 0 as this is the highest level?
    }
    this.childLevel = ownLevel;
    List<Parameter<?>> children = getAllChildrenFirstGen();
    for (Parameter<?> child : children) {
      // They are all NEXT generation.
      child.determineChildLevels(ownLevel + 1);
    }
  }

  @Override
  public int getChildLevel() {
    return childLevel;
  }

  @Override
  public void setChildLevel(int childLevel) {
    this.childLevel = childLevel;
  }


  @Override
  public List<Parameter<T>> getSelf() {
    return new ArrayList<>(Collections.singleton(this));
  }


  // Returns all children ignoring the chosen condition and and calls getChildren() on these parameters.
  @Override
  public List<Parameter<?>> getAllChildrenAllGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>();

    for (Parameter<?> child : unconditionalChildren) {
      setWithAll.add(child);
      // Now also call on the children generation
      setWithAll.addAll(child.getSelfAndAllChildrenAllGen());
    }

    return new ArrayList<>(setWithAll);
  }

  // Returns children that comply with the chosen condition and also calls getChildren() on these parameters.
  @Override
  public List<Parameter<?>> getActiveChildrenAllGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>();

    for (Parameter<?> child : unconditionalChildren) {
      setWithAll.add(child);
      // Now also call on the children generation
      setWithAll.addAll(child.getSelfAndActiveChildrenAllGen());
    }

    return new ArrayList<>(setWithAll);
  }


  // Returns all children ignoring the chosen condition and DOES NOT call getChildren() on these parameters.
  // Only to traverse the children in order to determine their level.
  @Override
  public List<Parameter<?>> getAllChildrenFirstGen() {
    return new ArrayList<>(unconditionalChildren);
  }

  // Returns children that comply with the chosen condition and DOES NOT call getChildren() on these parameters.
  @Override
  public List<Parameter<?>> getActiveChildrenFirstGen() {
    return new ArrayList<>(unconditionalChildren);
  }

  // Returns parameter and its children. All children are returned.
  public List<Parameter<?>> getSelfAndAllChildrenAllGen() {
    List<Parameter<?>> parameters = new ArrayList<>(getSelf());
    parameters.addAll(getAllChildrenAllGen());
    return parameters;
  }

  // Returns parameter and its children. Children are only returned if the right condition is chosen.
  public List<Parameter<?>> getSelfAndActiveChildrenAllGen() {
    List<Parameter<?>> parameters = new ArrayList<>(getSelf());
    parameters.addAll(getActiveChildrenAllGen());
    return parameters;
  }


  /**
   * Returns parameter and its children. All children are returned. Override this method in all
   * classes that spawn bundles! Why? To write an xml, normal instances will return all parameters,
   * all children, and children's children. For a bundle, however, we do not want to children to be
   * listed as normal parameters. Why? We use the getBundlesForXml() method to retrieve the bundled
   * methods and we put them in their own block. Thus, we must exclude the bundles from the method
   * that does the "give me all your methods to write them as an xml".
   */
  public List<Parameter<?>> getSelfAndAllChildrenAllGenForXml() {
    List<Parameter<?>> parameters = new ArrayList<>(getSelf());
    parameters.addAll(getAllChildrenAllGen());
    return parameters;
  }

  @Override
  public List<ParamBundle> getActiveBundlesForProcessing() {
    return new ArrayList<>();
  }

  @Override
  public List<ParamBundle> getAllBundlesForXml() {
    return new ArrayList<>();
  }

  @Override
  public List<ParamBundle> getAllDefaultBundlesForXml() {
    return new ArrayList<>();
  }

  @Override
  public void addUnconditionalChild(Parameter<?> unconditionalParameter) {
    unconditionalParameter.setIsChild(true);
    this.unconditionalChildren.add(unconditionalParameter);
  }

  @Override
  public  void addUnconditionalChild(Parameter<?> ... unconditionalParameters){
    for (int i = 0; i < unconditionalParameters.length; i++) {
      Parameter<?> par = unconditionalParameters[i];
      par.setIsChild(true);
      this.unconditionalChildren.add(par);
    }
  }

  @Override
  public void addUnconditionalChild(List<Parameter<?>> unconditionalParameters) {
    unconditionalParameters.forEach(p -> p.setIsChild(true));
    this.unconditionalChildren.addAll(unconditionalParameters);
  }

  @Override
  public Decoration<T> getDecoration() {
    return decoration;
  }

  public void setDecoration(Decoration<T> decoration) {
    this.decoration = decoration;
  }


}
