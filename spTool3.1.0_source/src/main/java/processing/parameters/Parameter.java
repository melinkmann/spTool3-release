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

import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import processing.parameterSets.ParamBundle;

// https://stackoverflow.com/questions/16852247/how-to-serialize-a-generic-class-in-java
public interface Parameter<T extends Serializable> extends Serializable {

  Logger LOGGER = LogManager.getLogger(Parameter.class);

  void setValue(T t);

  void trySetValue(Parameter<?> par);

  public T getValue();

  public String getValueAsString();

  public T getDefaultValue();

  public String getDefaultValueAsString();

  public String getLabel();

  String getSecondaryLabel();

  public String getExplanation();

  // Type ("parameter") for the XML node
  default String getXmlType() {
    return XmlUtil.PAR_NODE;
  }

  // ID to be written/read for the XML file.
  String getXmlID();

  void writeToXmlElement(Element xmlElement);

  // when a parameter spawns a bundle, this is the corresponding ID to later assign this bundle to the
  // parameter when reading
  void writeBundleMetaDataToBundleNode(Element xmlElement);

  void readFromXmlElement(Element xmlElement);

  // "Ctl Z like behaviour, goes back to default setting (once step back only)
  void setDefaultValue(T t);

  void setCurrentValueAsDefault();

  public void resetToDefault();

  List<Parameter<T>> getSelf();

  // Returns all children ignoring the chosen condition and and calls getChildren() on these parameters.
  List<Parameter<?>> getAllChildrenAllGen();

  // Returns children that comply with the chosen condition and also calls getChildren() on these parameters.
  List<Parameter<?>> getActiveChildrenAllGen();

  // Returns all children ignoring the chosen condition and DOES NOT call getChildren() on these parameters.
  // Only to traverse the children in order to determine their level.
  List<Parameter<?>> getAllChildrenFirstGen();

  // Returns children that comply with the chosen condition and DOES NOT call getChildren() on these
  // parameters.
  List<Parameter<?>> getActiveChildrenFirstGen();

  // Returns parameter and its children. Children are only returned if the right condition is chosen.
  public List<Parameter<?>> getSelfAndActiveChildrenAllGen();

  // Returns parameter and its children. All children are returned.
  public List<Parameter<?>> getSelfAndAllChildrenAllGen();

  /**
   * Returns parameter and its children. All children are returned. Override this method in all
   * classes that spawn bundles! Why? To write an xml, normal instances will return all parameters,
   * all children, and children's children. For a bundle, however, we do not want to children to be
   * listed as normal parameters. Why? We use the getBundlesForXml() method to retrieve the bundled
   * methods and we put them in their own block. Thus, we must exclude the bundles from the method
   * that does the "give me all your methods to write them as an xml".
   */
  public List<Parameter<?>> getSelfAndAllChildrenAllGenForXml();

  List<ParamBundle> getActiveBundlesForProcessing();

  List<ParamBundle> getAllBundlesForXml();

  List<ParamBundle> getAllDefaultBundlesForXml();

  // Gets a Deep copy of the non-FX instance type
  public Parameter<T> copyWithoutChildren();

  // Creates new Instance of the corresponding FX instance
  FxParameter<T> getObservableInstance();

  // e.g., a calculator button on a textfield. FORCE BINDING of CORRECT FX PARAMETER HERE
  Decoration<T> getDecoration();

  void setDecoration(Decoration<T> decoration);

  // Add depending parameters: Override if it is possible, else do nothing.
  default void addConditionalChild(T t, List<Parameter<?>> conditionalParameters) {
    LOGGER.debug("API: Cannot add child to this parameter.");
  }

  default void putConditionalChild(T t, Parameter<?>... conditionalParameters) {
    LOGGER.debug("API: Cannot add child to this parameter.");
  }

  default void clearChildren() {
    LOGGER.debug("API: Cannot clear child.");
  }


  default void addConditionalChild(List<T> list, Parameter<?>... conditionalParameters) {
    LOGGER.debug("API: Cannot add child to this parameter.");
  }

  default void addConditionalChild(T[] arr, Parameter<?>... conditionalParameters) {
    LOGGER.debug("API: Cannot add child to this parameter.");
  }

  default void addConditionalChild(T t, Parameter<?>... conditionalParameters) {
    LOGGER.debug("API: Cannot add child to this parameter.");
  }

  default void addConditionalChild(T t, Parameter<?> conditionalParameter) {
    LOGGER.debug("API: Cannot add child to this parameter.");
  }

  void addUnconditionalChild(Parameter<?> unconditionalParameter);

  void addUnconditionalChild(Parameter<?>... unconditionalParameters);

  void addUnconditionalChild(List<Parameter<?>> unconditionalParameters);

  void setIsChild(boolean isChild);

  boolean isChild();

  void setLimitedToExperts(boolean limitedToExperts);

  Boolean isLimitedToExpert();

  int getChildLevel();

  void setChildLevel(int childLevel);

  void determineChildLevels(int ownLevel);

  // Equivalence check
  boolean isEquivalent(Parameter<?> other);

}
