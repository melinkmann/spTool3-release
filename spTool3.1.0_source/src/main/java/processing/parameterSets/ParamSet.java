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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import org.w3c.dom.NodeList;
import processing.parameters.CommentParameter;
import processing.parameters.LabelParameter;
import processing.parameters.NoFieldTextParameter;
import processing.parameters.Parameter;

public interface ParamSet extends Serializable {

  ParamSet getNewInstance();

  AvailableParameterSets getEnum();

  // For the UI. Here, we need the "normal" way which uses all parents and calls getChildren()
  List<Parameter<?>> listAllParameters();

  // For the XML. Here, we need the parents, the children but NOT THE BUNDLES as they are written via getBundles().
  List<Parameter<?>> listAllParametersForXML();

  List<Parameter<?>> listParentParameters();

  List<Parameter<?>> listActiveParameters();

  //  For UI parameter sets, we do not need label, comment, ...
  List<Parameter<?>> listActiveParametersWithoutMeta();

  LabelParameter getLabelParameter();

  NoFieldTextParameter getDateParameter();

  NoFieldTextParameter getIdParameter();

  CommentParameter getCommentParameter();

  FxParamSet getObservableInstance();

  FxParamSet getUneditableObservableInstance();

  void setDateCreated(String dateCreated);

  void setComment(CommentParameter comment);

  String getDateCreatedAsString();

  String getCommentAsString();

  void setCurrentValuesAsDefault();

  void resetToDefault();

  String getXmlType();

  void fillFromXml(NodeList nodeList, Path file);

  /**
   * Since all submethods are now stored in one large xml file, this save function is currently only
   * used to store dedicated special files such as the configuration file.
   */
  void executeOverridingSave();

  /**
   * Since all submethods are now stored in one large xml file, this save function is currently only
   * used to store dedicated special files such as the configuration file.
   */
  void executeSaveAs(Path file);

  boolean hasAssociatedFileOnDrive();

  Path getAssociatedFileOndDrive();

  void setAssociatedFileOnDrive(Path file);

  boolean hasEqualParameters(ParamSet otherSet);

  boolean isEqualID(ParamSet paramSet);

  /**
   * For copies that are like a duplicate to modify the original ParamSet and hence get a new date.
   */
  ParamSet getCopyWithNewDate();

  /**
   * For copy constructors in the original sense, e.g., when a method makes a copy.
   */
  ParamSet getCopyWithPreviousDateFileAndID();

  /**
   * For the UI.
   */

  String getTooltip();


}
