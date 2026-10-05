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

package processing.parameterSets.impl;

import io.XmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.options.MathMod;
import processing.options.SigTests;
import processing.parameterSets.*;
import processing.parameters.ComboEnumParameter;
import processing.parameters.Parameter;
import processing.parameters.PathParameter;

import java.io.Serial;
import java.nio.file.Path;

public class MethodWatcherParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "MethodWatcherParams";

  private final Parameter<String> currentMethodWatchPath;

  public MethodWatcherParams() {
    this("Method watch parameters");
  }

  public MethodWatcherParams(String label) {
    super(label, XML_ELEMENT_TAG);

    this.currentMethodWatchPath = new PathParameter(
        "Method path",
        "This is the directory where the method files (.spm) are dropped",
        "",
        false,
        "currentMethodWatchPath");

    organize();
  }

  // Copy
  public MethodWatcherParams(MethodWatcherParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.currentMethodWatchPath = params.currentMethodWatchPath.copyWithoutChildren();

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new MethodWatcherParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new MethodWatcherParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new MethodWatcherParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    super.setParentParameters(currentMethodWatchPath);
  }


  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = switch (element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE)) {
          case LABEL_PAR_XML_ID -> super.label;
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "currentMethodWatchPath" -> currentMethodWatchPath;

          default -> null;
        };

        if (par != null) {
          par.readFromXmlElement(element);
        }
      }
    }
  }

  @Override
  public void executeSaveAs(Path file) {
    super.setAssociatedFileOnDrive(file);
    XmlUtil.writeToXml(this, file);
  }


  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.METHOD_WATCH;
  }

  // without comment and so forth
  @Override
  public FxParamSet getObservableInstance() {
    return new FxParamSetSlimImpl(this);
  }

  // without comment and so forth
  @Override
  public FxParamSet getUneditableObservableInstance() {
    FxParamSetImpl fxParamSet = new FxParamSetSlimImpl(this);
    fxParamSet.setUneditable();
    return fxParamSet;
  }

  /// /////////////////////////////////////
  public Parameter<String> getCurrentMethodWatchPath() {
    return currentMethodWatchPath;
  }
}
