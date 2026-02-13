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

import gui.dialog.ListableFavourite;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import processing.parameters.Parameter;

public interface Method extends ListableFavourite, Serializable {

  public static final String XML_ELEMENT_TAG = "method";

  Method getCopyWithoutFile();

  List<ParamSet> getSets();

  void clearSets();

  void removeSets(List<ParamSet> sets);

  void addSet(ParamSet set);

  void addSets(List<ParamSet> sets);

  Parameter<String> getLabelParam();

  Parameter<String>  getCommentParam();

  Parameter<String>  getDateParam();

  void executeSaveAs(Path file, boolean updateDate);

  void executeOverridingSave();

  boolean hasAssociatedFileOnDrive();


  @Nullable
  Path getAssociatedFile();

  void setAssociatedPath(@Nullable Path path);

  void writeMetaDataToXmlElement(Element xmlElement);

  void fillWithMetaDataFromXmlElement(NodeList nodeList, Path file);

  String getTooltip();

  FxMethod getObservableInstance();

  boolean hasEqualParameters(Method method);


}
