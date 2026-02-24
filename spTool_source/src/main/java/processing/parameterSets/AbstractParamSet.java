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

package processing.parameterSets;

import gui.util.TextFormatterOption;
import java.io.File;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import processing.parameters.CommentParameter;
import processing.parameters.LabelParameter;
import processing.parameters.NoFieldTextParameter;
import processing.parameters.Parameter;
import util.Util;

public abstract class AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  // Associated location on the drive.
  protected File associatedPathOnDrive = null;

  /*
    xmlIDs for the parameters that are always present in each set.
   */
  public static final String DATE_PAR_XML_ID = "dateCreated";
  public static final String LABEL_PAR_XML_ID = "labelOfSet";
  public static final String COMMENT_PAR_XML_ID = "commentForSet";
  public static final String FAVOURITE_PAR_XML_ID = "isFavouriteSet";
  public static final String UUID_PAR_XML_ID = "universallyUniqueIdentifier";

  // Hard-coded parameter without children
  protected final List<Parameter<?>> parentParameters;
  protected final LabelParameter label;
  protected final CommentParameter comment;
  protected final NoFieldTextParameter dateCreated;
  //Note that submethodCategory parameter needs no XML storage as it comes from an enum in respective implementation.
  protected final NoFieldTextParameter submethodCategory;
  protected final NoFieldTextParameter uuidString;
  protected final String xmlElementTag;

  public AbstractParamSet(String label, String xmlElementTag) {

    this.label = new LabelParameter(
        "Label",
        "Label of this Method",
        label,
        TextFormatterOption.ASSURE_EXTD_NUMERAL_OR_LETTER,
        false,
        LABEL_PAR_XML_ID);

    this.comment = new CommentParameter(
        "Comment",
        "",
        "",
        false,
        COMMENT_PAR_XML_ID);

    this.dateCreated = new NoFieldTextParameter(
        "Created",
        "Date when parameter set was created"
            + "\nor when the defaults were overwritten, respectively.",
        Util.dateToString(),
        TextFormatterOption.ALL_PASS,
        false,
        DATE_PAR_XML_ID);

    this.submethodCategory = new NoFieldTextParameter(
        "Sub method category",
        "Type of this submethod",
        getEnum().toString(),
        TextFormatterOption.ALL_PASS,
        false,
        "subMethodCategory");

    this.uuidString = new NoFieldTextParameter(
        "ID",
        "Universal ID of this submethod",
        UUID.randomUUID().toString(),
        TextFormatterOption.ALL_PASS,
        false,
        UUID_PAR_XML_ID);

    this.parentParameters = new ArrayList<>();

    this.xmlElementTag = xmlElementTag;
  }

  @Override
  public List<Parameter<?>> listAllParameters() {
    Set<Parameter<?>> pars = new LinkedHashSet<>();

    for (Parameter<?> par : parentParameters) {
      pars.addAll(par.getSelfAndAllChildrenAllGen());
    }
    return new ArrayList<>(pars);
  }

  // As listAllParameters() but excluded the bundles!
  @Override
  public List<Parameter<?>> listAllParametersForXML() {
    Set<Parameter<?>> pars = new LinkedHashSet<>();

    for (Parameter<?> par : parentParameters) {
      pars.addAll(par.getSelfAndAllChildrenAllGenForXml());
    }
    return new ArrayList<>(pars);
  }

  @Override
  public List<Parameter<?>> listActiveParameters() {
    List<Parameter<?>> pars = new ArrayList<>();
    for (Parameter<?> par : parentParameters) {
      pars.addAll(par.getSelfAndActiveChildrenAllGen());
    }
    // Do not show this in UI. I hope this does not cause any Bugs..
    pars.remove(uuidString);
    return pars;
  }


  @Override
  public List<Parameter<?>> listActiveParametersWithoutMeta() {
    List<Parameter<?>> pars = new ArrayList<>();
    for (Parameter<?> par : parentParameters) {
      pars.addAll(par.getSelfAndActiveChildrenAllGen());
    }
    // Do not show this in UI. I hope this does not cause any Bugs..
    pars.remove(label);
    pars.remove(comment);
    pars.remove(dateCreated);
    pars.remove(submethodCategory);
    pars.remove(uuidString);
    return pars;
  }


  @Override
  public List<Parameter<?>> listParentParameters() {
    return parentParameters;
  }


  // For the ListViews to get the StringProperty
  @Override
  public LabelParameter getLabelParameter() {
    return label;
  }

  @Override
  public NoFieldTextParameter getDateParameter() {
    return dateCreated;
  }

  @Override
  public NoFieldTextParameter getIdParameter() {
    return uuidString;
  }

  @Override
  public CommentParameter getCommentParameter() {
    return comment;
  }

  /**
   * Now, when defaults are overwritten, that date will appear as the new date.
   */
  @Override
  public void setCurrentValuesAsDefault() {
    List<Parameter<?>> params = listActiveParameters();
    params.forEach(Parameter::setCurrentValueAsDefault);
    dateCreated.setValue(Util.dateToString());
    uuidString.setValue(UUID.randomUUID().toString());
  }

  @Override
  public void resetToDefault() {
    listActiveParameters().forEach(Parameter::resetToDefault);
  }

  /*
          Warning: This must create a new instance because otherwise FX and Plain would be mixed.
          But this means, that all listener references are lost if no proper management like a hashmap is done.
          */
  @Override
  public FxParamSet getObservableInstance() {
    return new FxParamSetSlimImpl(this);
  }

  @Override
  public FxParamSet getUneditableObservableInstance() {
    FxParamSetImpl fxParamSet = new FxParamSetImpl(this);
    fxParamSet.setUneditable();
    return fxParamSet;
  }


  // Must be called in order to add parameters.
  protected void setParentParameters(Parameter<?>... parameters) {
    parentParameters.clear();
    parentParameters.add(label);
    parentParameters.addAll(Arrays.asList(parameters));
    parentParameters.add(comment);
    // Add date as last parameter in the list. It should strictly appear at the bottom.
    parentParameters.add(dateCreated);
    // In case the name (label) is chosen super poorly and the user forgets what this was all about...
    parentParameters.add(submethodCategory);
    parentParameters.add(uuidString);
  }

  @Override
  public void setDateCreated(String dateCreated) {
    this.dateCreated.setValue(dateCreated);
  }

  @Override
  public void setComment(CommentParameter comment) {
    this.comment.setValue(comment.getValue());
    this.comment.setDefaultValue(comment.getDefaultValue());
  }

  @Override
  public String getDateCreatedAsString() {
    return dateCreated.getValueAsString();
  }

  @Override
  public String getCommentAsString() {
    return comment.getValueAsString();
  }

  @Override
  public String getXmlType() {
    return xmlElementTag;
  }

  @Override
  public boolean hasAssociatedFileOnDrive() {
    return associatedPathOnDrive != null;
  }

  @Override
  public Path getAssociatedFileOndDrive() {
    if (associatedPathOnDrive != null){
      return associatedPathOnDrive.toPath();
    } else {
      return null;
    }
  }

  @Override
  public void setAssociatedFileOnDrive(Path file) {
    if (file != null) {
      this.associatedPathOnDrive = file.toFile();
    } else {
      this.associatedPathOnDrive = null;
    }
  }

  @Override
  public void executeOverridingSave() {
    if (hasAssociatedFileOnDrive()) {
      executeSaveAs(associatedPathOnDrive.toPath());
    }
  }

  @Override
  public boolean hasEqualParameters(ParamSet otherSet) {
    List<Parameter<?>> thisParams = listAllParameters();
    List<Parameter<?>> otherParams = otherSet.listAllParameters();

    // I think, it makes sense to exclude the date & UUID (see below)
    thisParams.remove(getDateParameter());
    otherParams.remove(otherSet.getDateParameter());

    thisParams.remove(uuidString);
    otherParams.remove(otherSet.getIdParameter());

    boolean isEq = thisParams.size() == otherParams.size();
    if (isEq) {
      isEq = label.isEquivalent(otherSet.getLabelParameter());
      isEq = isEq && comment.isEquivalent(otherSet.getCommentParameter());

      /*
       I think, it makes sense to exclude the date & UUID :
       E.g., when two are equal because they were cloned or simply have all the same parameters
       and names but only differ in date&ID, we still may want to delete them.
       */
      // isEq = isEq && dateCreated.isEquivalent(otherSet.getDateParameter());

      // equal sizes were check above
      for (int i = 0; i < thisParams.size(); i++) {
        Parameter<?> thisPar = thisParams.get(i);
        Parameter<?> otherPar = otherParams.get(i);
        isEq = isEq && thisPar.isEquivalent(otherPar);
      }
    }
    return isEq;
  }

  @Override
  public boolean isEqualID(ParamSet paramSet) {
    return paramSet.getIdParameter().getValue().equals(this.getIdParameter().getValue());
  }

  @Override
  public String getTooltip() {
    return "";
  }
}
