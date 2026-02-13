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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.control.TextFormatter;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameterSets.ParamBundle;
import util.NF;
import util.SnF;
import util.WindowsSorter.WindowsExplorerComparator;


public class SpawnControlParameter extends AbstractParameter<Integer> implements Serializable,
    Parameter<Integer> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private Integer value;
  private NF format = NF.D1C0;
  // Determines, pos vs. neg, double vs. int, ... i.e more than just UI stuff.
  private final TextFormatterOption textFormatterOption; // has to be a SOURCE, i.e. a method reference!

  // This class can spawn bundles
  private final BundleSupplier bundleSupplier;

  private final List<ParamBundle> activeChildrenBundles;

  private final List<ParamBundle> defaultActiveChildrenBundles;


  public SpawnControlParameter(String label,
                               String explanation,
                               Integer value,
                               BundleSupplier bundleSupplier,
                               boolean isLimitedToExperts,
                               String xmlID) {

    this(label,
        explanation,
        value,
        bundleSupplier,
        new ArrayList<>(),
        new ArrayList<>(),
        isLimitedToExperts,
        xmlID);
  }

  /**
   * Copy constructor: Note: The bundles will be copied inside of this constructor since they need
   * the parent instance!
   */

  public SpawnControlParameter(String label,
                               String explanation,
                               Integer value,
                               BundleSupplier bundleSupplier,
                               List<ParamBundle> bundles,
                               List<ParamBundle> defaultBundles,
                               boolean isLimitedToExperts,
                               String xmlID) {

    super(label, explanation, defaultBundles.size(), isLimitedToExperts, xmlID);

    this.value = value;
    this.textFormatterOption = TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER;
    this.bundleSupplier = bundleSupplier;

    this.activeChildrenBundles = new ArrayList<>();
    this.defaultActiveChildrenBundles = new ArrayList<>();

    // fill list
    initializeLists(bundles, defaultBundles);
  }

  /*
   quasi constructor.
   Note: Here, we need to copy the children since dynamically created, i.e., not hard-coded children
   are only stored here and not anywhere else as usual, e.g., in the ParameterSet.
   */
  public Parameter<Integer> copyWithoutChildren() {
    return copyIncludingChildren();
  }

  public Parameter<Integer> copyIncludingChildren() {
    /*
     Note: We cannot call copy() on the children bundles here.
     Why? They need their parent as a constructor argument.
     The parent (the copy!) is not constructed here, yet.
     Hence, the copy constructor needs to take care of this.
     This feels a bit hacky and, if not remembered later, may cause 'weird' bugs.
     Hence, KEEP THIS IN MIND!!!
     */

    Parameter<Integer> copy = new SpawnControlParameter(
        super.getLabel(),
        super.getExplanation(),
        value,
        bundleSupplier,
        activeChildrenBundles,
        defaultActiveChildrenBundles,
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

    /*
     Check for the bundles: These are only filled/created inside of this class (nowhere outside).

     Example structure for orientation:

      <parameter defaultValue="1" label="Particle populations [#]" value="2" xmlID="numberOfParticlePopulations">
          <parameterBundle bundleID="elementBundle">
              <parameter defaultValue="Au (Gold)" label="Element" value="Ru (Ruthenium)" xmlID="elementHeaderParameter"/>
              <parameter defaultValue="0.25" label="Mass fraction" value="0.75" xmlID="massFraction"/>
              <parameter defaultValue="15.0" label="Particle event rate" value="15.0" xmlID="eventRate"/>
          </parameterBundle>
          <parameterBundle bundleID="elementBundle">
              <parameter defaultValue="Au (Gold)" label="Element" value="Mg (Magnesium)" xmlID="elementHeaderParameter"/>
              <parameter defaultValue="0.25" label="Mass fraction" value="0.25" xmlID="massFraction"/>
              <parameter defaultValue="15.0" label="Particle event rate" value="15.0" xmlID="eventRate"/>
          </parameterBundle>
      </parameter>
     */


    /*
     Check if the parameter node has a sub-node that contains bundled children, i.e., <parameterBundle bundleID="aBundle">.
     The bundleNodeList is a List of all bundles that the parameter node (i.e., THIS parameter) has.
     */
    NodeList bundleNodeList = xmlElement.getElementsByTagName(XmlUtil.BUNDLE_NODE);
    NodeList defaultBundleNodeList = xmlElement.getElementsByTagName(XmlUtil.BUNDLE_DEFAULT_NODE);

    List<ParamBundle> bundlesFromXml = new ArrayList<>();
    List<ParamBundle> defaultBundlesFromXml = new ArrayList<>();

    /*
       Store all the bundles that we get from the xml to override the "dummy" instances of bundles
       that were created earlier during construction.
       */

    if (bundleNodeList != null) {

      for (int bundleIdx = 0; bundleIdx < bundleNodeList.getLength(); bundleIdx++) {
        Node bundleNode = bundleNodeList.item(bundleIdx);

        // (1) Identify the bundle type and
        // (2) check for the parameter children of the bundleNode
        if (bundleNode.getNodeType() == Node.ELEMENT_NODE) {
          // (1)
          Element bundleElement = (Element) bundleNode;
          String bundleID = bundleElement.getAttribute(XmlUtil.BUNDLE_ID_ATTRIBUTE);

          // We only supply ONE type of bundle per spawn control parameter.
          // Check if the bundleID in the xml is the one that this parameter can provide.
          if (bundleID.equals(bundleSupplier.getBundleID())) {
            /*
            We create a new bundle instance. This makes sure that all binding/listening is correctly
            directed to THIS class. Then, we will override the "dummy" instances of bundles
            that were created earlier during construction. Note: Each bundle knows how to fill
            itself, which is similar to how a parameter set uses the setters of its parameters to
            be read from an xml.
             */
            ParamBundle bundle = bundleSupplier.createNewBundle(this);
            bundle.readFromXmlElement(bundleElement);
            bundlesFromXml.add(bundle);
          }
        }
      }
    }

    if (defaultBundleNodeList != null) {

      for (int bundleIdx = 0; bundleIdx < defaultBundleNodeList.getLength(); bundleIdx++) {
        Node bundleNode = defaultBundleNodeList.item(bundleIdx);

        // (1) Identify the bundle type and
        // (2) check for the parameter children of the bundleNode
        if (bundleNode.getNodeType() == Node.ELEMENT_NODE) {
          // (1)
          Element bundleElement = (Element) bundleNode;
          String bundleID = bundleElement.getAttribute(XmlUtil.BUNDLE_ID_ATTRIBUTE);

          // We only supply ONE type of bundle per spawn control parameter.
          // Check if the bundleID in the xml is the one that this parameter can provide.
          if (bundleID.equals(bundleSupplier.getBundleID())) {
            /*
            We create a new bundle instance. This makes sure that all binding/listening is correctly
            directed to THIS class. Then, we will override the "dummy" instances of bundles
            that were created earlier during construction. Note: Each bundle knows how to fill
            itself, which is similar to how a parameter set uses the setters of its parameters to
            be read from an xml.
             */
            ParamBundle bundle = bundleSupplier.createNewBundle(this);
            bundle.readFromXmlElement(bundleElement);
            defaultBundlesFromXml.add(bundle);
          }
        }
      }
    }

    // If we read something: clear the "dummy" instances from before and replace them.
    if (!bundlesFromXml.isEmpty()) {
      initializeLists(bundlesFromXml, defaultBundlesFromXml);
    }


  }

  @Override
  public void writeBundleMetaDataToBundleNode(Element xmlElement) {
    super.writeBundleMetaDataToBundleNode(xmlElement); // contains "do nothing"
    xmlElement.setAttribute(XmlUtil.BUNDLE_ID_ATTRIBUTE,
        StringEscapeUtils.escapeXml10(bundleSupplier.getBundleID()));
  }

  @Override
  public Integer getValue() {
    return value;
  }


  @Override
  public List<Parameter<?>> getActiveChildrenAllGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getActiveChildrenAllGen());

    for (ParamBundle bundle : activeChildrenBundles) {
      // Note that there is no list of all params but that they are stored within the "header" of the bundle
      Parameter<?> header = bundle.getHeaderParameter();
      setWithAll.add(header);
      setWithAll.addAll(header.getActiveChildrenAllGen());
    }

    setChildLevels();

    return new ArrayList<>(setWithAll);
  }


  @Override
  public List<Parameter<?>> getActiveChildrenFirstGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getActiveChildrenFirstGen());

    for (ParamBundle bundle : activeChildrenBundles) {
      // Note that there is no list of all params but that they are stored within the "header" of the bundle
      Parameter<?> header = bundle.getHeaderParameter();
      setWithAll.add(header);
      setWithAll.addAll(header.getActiveChildrenFirstGen());
    }

    setChildLevels();

    return new ArrayList<>(setWithAll);
  }

  @Override
  public List<Parameter<?>> getAllChildrenAllGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getAllChildrenAllGen());

    for (int i = 0; i < activeChildrenBundles.size(); i++) {
      ParamBundle bundle = activeChildrenBundles.get(i);
      // Note that there is no list of all params but that they are stored within the "header" of the bundle
      Parameter<?> header = bundle.getHeaderParameter();
      setWithAll.add(header);
      setWithAll.addAll(header.getAllChildrenAllGen());
    }

    setChildLevels();

    return new ArrayList<>(setWithAll);
  }

  @Override
  public List<Parameter<?>> getAllChildrenFirstGen() {
    // Set prevents accidental duplicates.
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(super.getAllChildrenFirstGen());

    for (int i = 0; i < activeChildrenBundles.size(); i++) {
      ParamBundle bundle = activeChildrenBundles.get(i);
      // Note that there is no list of all params but that they are stored within the "header" of the bundle
      Parameter<?> header = bundle.getHeaderParameter();
      setWithAll.add(header);
      setWithAll.addAll(header.getAllChildrenFirstGen());
    }

    setChildLevels();

    return new ArrayList<>(setWithAll);
  }

  @Override
  public List<Parameter<?>> getSelfAndAllChildrenAllGenForXml() {
    // Set prevents accidental duplicates.

    /*
     Essentially only return the self.
     If the getBundles() method returns bundles, they will be written to the xml in a different node!
     */
    Set<Parameter<?>> setWithAll = new LinkedHashSet<>(getSelf());

    /*
    Why "setWithAll.addAll(super.getAllChildrenFirstGen());"???
    Note that this method in the super class only adds the unconditional children.
    Any other parameter that may have conditional children has to override it and does so.
    Hence, we only call this here in the case that later, this parameter may get unconditional
    children.
     */
    setWithAll.addAll(super.getAllChildrenFirstGen());

    return new ArrayList<>(setWithAll);
  }

  @Override
  public List<ParamBundle> getActiveBundlesForProcessing() {
    List<ParamBundle> list = new ArrayList<>(activeChildrenBundles);
    return list;
  }

  @Override
  public List<ParamBundle> getAllBundlesForXml() {
    return activeChildrenBundles;
  }

  @Override
  public List<ParamBundle> getAllDefaultBundlesForXml() {
    List<ParamBundle> list = new ArrayList<>();
    list.addAll(defaultActiveChildrenBundles);
    return list;
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

    while (value > activeChildrenBundles.size()) {
      ParamBundle bundle = getNext();
      activeChildrenBundles.add(bundle);
    }

    sortChildrenList();
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof SpawnControlParameter) {
      int val = ((SpawnControlParameter) par).getValue();
      setValue(val);
    }
  }

  // We need to override the default handling!
  @Override
  public void resetToDefault() {
    this.activeChildrenBundles.clear();
    this.activeChildrenBundles.addAll(defaultActiveChildrenBundles);

    sortChildrenList();

    super.resetToDefault();
  }

  @Override
  public void setCurrentValueAsDefault() {
    // this.defaultActiveChildrenBundles.clear();
    this.defaultActiveChildrenBundles.addAll(activeChildrenBundles);

    super.setCurrentValueAsDefault();
  }

  // Specific method
  public void removeBundle(ParamBundle bundle) {
    // Note: notifyItemChange() is called by the BUTTON since both Bundle as THIS environment is non-FX

    activeChildrenBundles.remove(bundle);

    // call set only after removing as set() also modifies these lists.
    setValue(Math.max(0, value - 1));

    /*

      childrenBundles.remove(bundle);

      We use to remove the bundle from the bundle list. The idea was, that we always start with a
      neutral element when adding, e.g., Au.
      However, this does not make much sense. There is no "neutral" element and usually
      we have to modify all the parameters anyway when designing a NP.
      Hence, we might as well just keep the previously visible elements and let them reappear.
      This is not fully ideal (e.g., if you want an element 3 items further down the line,
      you'd have to add them all. But I think this is it possible.

      This also solves this issue:
      <<< Removing all the information means that when click "reset sub method",
      it will only add "Au" as the new default element...
      We would have to keep all of the options somewhere,
      and also store them in the xml as a "defaultChildren" list.
      And override "resetToDefault()" here. >>>
      */
  }


  private void initializeLists(List<ParamBundle> inputBundles,
                               List<ParamBundle> defaultInputBundles) {

    activeChildrenBundles.clear();

    defaultActiveChildrenBundles.clear();

    // Important: Clone the children inside the copy constructor since they need THIS as their parent!
    for (int i = 0; i < inputBundles.size(); i++) {
      if (activeChildrenBundles.size() < value) {
        activeChildrenBundles.add(inputBundles.get(i).copy(this));
      }
    }

    // In case we call this from the first fill (initialization) of the class, fill the list:
    // Check if there are enough bundles in the input.
    for (int i = activeChildrenBundles.size(); i < value; i++) {
      activeChildrenBundles.add(getNext());
    }

//    // Check if method has defaults. Else, override them. EDIT: this seems to cause weird out of sync bugs.
//    if (defaultInputBundles.isEmpty()) {
//      // if this is called from first fill (initialization), we must also fill the defaults list
//      if (!inputBundles.isEmpty()) {
//        defaultInputBundles = new ArrayList<>(inputBundles); // copy on children is called below
//      } else {
//        // there were no inputs since we are initializing
//        defaultInputBundles = new ArrayList<>(activeChildrenBundles); // copy on children is called below
//      }
//    }
    for (int i = 0; i < defaultInputBundles.size(); i++) {
      defaultActiveChildrenBundles.add(defaultInputBundles.get(i).copy(this));
    }

    /*
     DO NOT FILL THE DEFAULTS like we do here:
     for (int i = activeChildrenBundles.size(); i < value; i++) {
      activeChildrenBundles.add(getNext());
      }
     Why? The defaults are only a backup and should only be changes by "override" button.
     */

    // Go on
    setChildLevels();

    // Sort
    sortChildrenList();
  }

  /*
  This is weird! (TODO)
  I do not understand, why we have to use (childLevel + 1) here.
  Neither do I understand why is makes no difference whether we write

  "parentParams.forEach(par -> par.determineChildLevels(1));"
  or
  parentParams.forEach(par -> par.determineChildLevels(0));"
  in the AbstractFxParam class.


  Then, going on, I do not understand why in UiUtils: getIndentArrowPane()
  we use "double startX = (childLevel - 1) * offset;"
  I do not understand the "-1".

  This seems completely unnecessarily complicated but at the moment there is no time to check it.
  For now, it works...
   */
  private void setChildLevels() {
    for (ParamBundle activeChildrenBundle : activeChildrenBundles) {
      activeChildrenBundle.getHeaderParameter().setIsChild(true);
      int childLevel = this.getChildLevel();
      activeChildrenBundle.getHeaderParameter().determineChildLevels(childLevel + 1);
    }

    for (ParamBundle defaultBundle : defaultActiveChildrenBundles) {
      defaultBundle.getHeaderParameter().setIsChild(true);
      int childLevel = this.getChildLevel();
      defaultBundle.getHeaderParameter().determineChildLevels(childLevel + 1);
    }
  }

  /*
  Works well.
   */
  private void sortChildrenList() {
    activeChildrenBundles.sort(new Comparator<ParamBundle>() {
      @Override
      public int compare(ParamBundle o1, ParamBundle o2) {
        WindowsExplorerComparator com = new WindowsExplorerComparator();
        return com.compare(o1.getSortingString(), o2.getSortingString());
      }
    });
    defaultActiveChildrenBundles.sort(new Comparator<ParamBundle>() {
      @Override
      public int compare(ParamBundle o1, ParamBundle o2) {
        WindowsExplorerComparator com = new WindowsExplorerComparator();
        return com.compare(o1.getSortingString(), o2.getSortingString());
      }
    });
  }

  @Override
  public FxParameter<Integer> getObservableInstance() {
    TextFormatter<Integer> formatter = TextFormatterSupplier.get(textFormatterOption, value);
    formatter.setValue(value);
    return new SpawnControlFxParameter(this);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof SpawnControlParameter;
    // This is cast-safe :-)
    equivalent = equivalent && this.getValue().equals(((SpawnControlParameter) other).getValue());

    if (equivalent) {
      // sort or else order may be different and yield inequality
      this.sortChildrenList();
      ((SpawnControlParameter) other).sortChildrenList();

      List<ParamBundle> thatBundles = other.getAllBundlesForXml();
      List<ParamBundle> thisBundles = this.getAllBundlesForXml();
      if (thatBundles.size() == thisBundles.size()) {
        for (int i = 0; i < thisBundles.size(); i++) {
          ParamBundle thisBundle = thisBundles.get(i);
          ParamBundle thatBundle = thatBundles.get(i);
          equivalent = thatBundle.isEquivalent(thisBundle);
          if (!equivalent) {
            break;
          }
        }
      } else {
        equivalent = false;
      }
    }

    if (equivalent) {
      List<ParamBundle> thatDefaults = other.getAllDefaultBundlesForXml();
      List<ParamBundle> thisDefaults = this.getAllDefaultBundlesForXml();
      if (thatDefaults.size() == thisDefaults.size()) {
        for (int i = 0; i < thisDefaults.size(); i++) {
          ParamBundle thisBundle = thisDefaults.get(i);
          ParamBundle thatBundle = thatDefaults.get(i);

          // Troubleshooting:
          String thisName = thisBundle.getHeaderParameter().getValueAsString();
          String thatName = thatBundle.getHeaderParameter().getValueAsString();

          equivalent = thatBundle.isEquivalent(thisBundle);
          if (!equivalent) {
            break;
          }
        }
      } else {
        equivalent = false;
      }
    }

    return equivalent;
  }

  private ParamBundle getNext() {
    ParamBundle bundle = bundleSupplier.createNewBundle(this);
     /*
     Usually, we determine child levels when an FxSet is created.
     In the FxSet constructor, we take the list of (hard-coded) parent parameters
     and for each of them call determineChildLevels(1).
     Here, it is different: Here, we add non-hardcoded sets, i.e., we add programmatically.
     We add sets quasi "at runtime".
     We do not know, if this class is also a depending child. Hence, we pass its current level
     down to the bundles and let them figure out where they are.
     Note: The bundle does not really have a list of children. Why?
     It has a header parameter which has all relevant children as unconditional children.
     Hence, in order to determine the nesting, we only need to call that header parameter,
     since it will call "determineChildLevels()" on all of its children.
     */
    Parameter<?> headerPar = bundle.getHeaderParameter();
    headerPar.setIsChild(true);
    int childLevel = this.getChildLevel();
    headerPar.determineChildLevels(childLevel);
    return bundle;
  }


}