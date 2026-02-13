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
import io.XmlUtil;
import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.parameters.CommentParameter;
import processing.parameters.LabelParameter;
import processing.parameters.NoFieldTextParameter;
import processing.parameters.Parameter;
import util.Util;

public class ListMethod implements Method, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(ListMethod.class);

  protected final Parameter<String> label;

  protected final Parameter<String> comment;
  protected final Parameter<String> dateCreated;

  private boolean isFavourite;

  // Path is not serializable
  protected File associatedPathOnDrive = null;

  private final List<ParamSet> sets = new ArrayList<>();

  public ListMethod(String label) {
    this.label = new LabelParameter(
        "Label",
        "Label of this Method",
        label,
        TextFormatterOption.ASSURE_EXTD_NUMERAL_OR_LETTER,
        false,
        AbstractParamSet.LABEL_PAR_XML_ID);

    this.comment = new CommentParameter(
        "Comment",
        "",
        "",
        false,
        AbstractParamSet.COMMENT_PAR_XML_ID);

    this.dateCreated = new NoFieldTextParameter(
        "Date",
        "Date when parameter set was created",
        Util.dateToString(),
        TextFormatterOption.ALL_PASS,
        false,
        AbstractParamSet.DATE_PAR_XML_ID);
  }

  public ListMethod() {
    this("New method");
  }

  public ListMethod(Method method) {
    this.label = method.getLabelParam().copyWithoutChildren();
    this.comment = method.getCommentParam().copyWithoutChildren();
    this.dateCreated = method.getDateParam().copyWithoutChildren();
    this.isFavourite = method.isFavorite();

    this.associatedPathOnDrive =
        method.getAssociatedFile() == null ? null : method.getAssociatedFile().toFile();
    List<ParamSet> originalSets = method.getSets();
    originalSets.forEach(s -> this.sets.add(s.getCopyWithPreviousDateFileAndID()));
  }

  // When putting a copy to a Sample, do not store the file as this leads to asynchronous status of method (method in sample may differ from file!)


  @Override
  public Method getCopyWithoutFile() {
    Method method = new ListMethod(this);
    method.setAssociatedPath(null);
    return method;
  }

  public String getLabelString() {
    return label.getValue();
  }

  public String getDateString() {
    return dateCreated.getValue();
  }

  public void setLabel(String label) {
    this.label.setValue(label);
  }

  public void setDate(String dateCreated) {
    this.dateCreated.setValue(dateCreated);
  }

  public void setComment(String comment) {
    this.comment.setValue(comment);

  }

  @Override
  public void setFavorite(boolean isFavourite) {
    this.isFavourite = isFavourite;
  }

  @Override
  public boolean isFavorite() {
    return isFavourite;
  }

  @Override
  public List<ParamSet> getSets() {
    return sets;
  }

  @Override
  public void clearSets() {
    sets.clear();
  }

  // Put add/remove/clear here as a method in case there will be listener for such changes in the future.
  public void removeSets(List<ParamSet> sets) {
    this.sets.removeAll(sets);
  }

  @Override
  public void addSet(ParamSet set) {
    sets.add(set);
  }

  @Override
  public void addSets(List<ParamSet> sets) {
    this.sets.addAll(sets);
  }

  ///////

  @Override
  public void setAssociatedPath(@Nullable Path file) {
    if (file != null) {
      this.associatedPathOnDrive = file.toFile();
    } else {
      this.associatedPathOnDrive = null;
    }
  }

  public boolean hasAssociatedFileOnDrive() {
    return associatedPathOnDrive != null && Files.isRegularFile(associatedPathOnDrive.toPath());
  }

  public void executeOverridingSave() {
    if (hasAssociatedFileOnDrive()) {
      XmlUtil.writeToXml(this, associatedPathOnDrive.toPath());
    }
  }

  public void executeSaveAs(Path file, boolean updateDate) {
    setAssociatedPath(file);
    /*
     When saving a method from the data exporter, we want to keep the original date.
     When saving from the method editor, we want to replace with the new date.
     */
    if (updateDate) {
      // saving as new files implies that NOW should be the new Date (overwriting save keeps the old date!!!)
      getDateParam().setValue(Util.dateToString());
    }
    XmlUtil.writeToXml(this, file);
  }


  @Nullable
  @Override
  public Path getAssociatedFile() {
    if (associatedPathOnDrive != null) {
      return associatedPathOnDrive.toPath();
    } else {
      return null;
    }
  }

  ///////

  @Override
  public Parameter<String> getLabelParam() {
    return label;
  }

  @Override
  public Parameter<String> getCommentParam() {
    return comment;
  }

  @Override
  public Parameter<String> getDateParam() {
    return dateCreated;
  }

  @Override
  public FxMethod getObservableInstance() {
    return new FxListMethod(this);
  }

  @Override
  public boolean hasEqualParameters(Method that) {
    boolean isEqual = label.isEquivalent(that.getLabelParam());
    isEqual = isEqual && comment.isEquivalent(that.getCommentParam());
    isEqual = isEqual && (isFavourite == that.isFavorite()); // identical
    isEqual = isEqual && this.getSets().size() == that.getSets().size();
    if (isEqual) {
      // size is eq.
      for (int i = 0; i < this.getSets().size(); i++) {
        ParamSet thisSet = this.getSets().get(i);
        ParamSet thatSet = that.getSets().get(i);
        isEqual = isEqual && thisSet.hasEqualParameters(thatSet);
      }
    }
    return isEqual;
  }

  ///////


  @Override
  public void writeMetaDataToXmlElement(Element xmlElement) {
    xmlElement.setAttribute(AbstractParamSet.LABEL_PAR_XML_ID,
        StringEscapeUtils.escapeXml10(getLabelString()));
    xmlElement.setAttribute(AbstractParamSet.DATE_PAR_XML_ID,
        StringEscapeUtils.escapeXml10(getDateString()));
    xmlElement.setAttribute(AbstractParamSet.COMMENT_PAR_XML_ID,
        StringEscapeUtils.escapeXml10(getCommentParam().getValue()));
    xmlElement.setAttribute(AbstractParamSet.FAVOURITE_PAR_XML_ID,
        StringEscapeUtils.escapeXml10(Boolean.toString(isFavourite)));
  }


  @Override
  public void fillWithMetaDataFromXmlElement(NodeList nodeList, Path file) {
    this.associatedPathOnDrive = file.toFile();

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        /*
        Check if element contains key words: Provide default values in case older xml lacks a parameter.
        In the previous version, nothing was written if any single parameter was null or empty.
         */

        String value = element.getAttribute(AbstractParamSet.LABEL_PAR_XML_ID);
        value = value == null || value.isEmpty() ? "Label of this Method" : value;

        // Comment can be empty
        String comment = element.getAttribute(AbstractParamSet.COMMENT_PAR_XML_ID);

        String date = element.getAttribute(AbstractParamSet.DATE_PAR_XML_ID);
        date = date == null || date.isEmpty() ? Util.dateToString() : date;

        String isFav = element.getAttribute(AbstractParamSet.FAVOURITE_PAR_XML_ID);
        isFav = isFav == null || isFav.isEmpty() ? "false" : isFav;

        setLabel(StringEscapeUtils.unescapeXml(value));
        setDate(StringEscapeUtils.unescapeXml(date));
        setComment(StringEscapeUtils.unescapeXml(comment));
        setFavorite(StringEscapeUtils.unescapeXml(isFav).toLowerCase(Locale.ROOT).equals("true"));
      }
    }
  }

  public String getTooltip() {
    String label = getLabelString();
    String date = dateCreated.getValue();
    String[] commentWords = getCommentParam().getValueAsString().split(" ");
    StringBuilder builder = new StringBuilder();
    builder.append("Method: ").append(label).append("\n")
        .append("Date: ").append(date).append("\n")
        .append("Comments: ");
    for (int i = 0; i < commentWords.length; i++) {
      builder.append(commentWords[i]).append(" ");
      if (i > 0 && i % 7 == 0) {
        builder.append("\n");
      }
    }
    return builder.toString();
  }


}
