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

package gui.table;

import gui.dialog.FxEntry;
import gui.dialog.SimpleFxEntry;
import gui.listAndSearch.FxWrapper;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;

public class MethodFxTableEntry implements FxTableEntry<Method> {

  private final Method method;
  // Column properties
  private final SimpleStringProperty label;
  private final SimpleStringProperty date;
  private final SimpleStringProperty file;
  private final SimpleStringProperty comment;

  public MethodFxTableEntry(Method method) {
    this.method = method;

    this.label = new SimpleStringProperty(method.getLabelParam().getValue());

    this.date = new SimpleStringProperty(method.getDateParam().getValue());

    this.file = new SimpleStringProperty(
        method.getAssociatedFile() != null ? method.getAssociatedFile().toString()
            : TableUtils.UNAVAILABLE_STR);

    this.comment = new SimpleStringProperty(method.getCommentParam().getValue());
  }

  @Override
  public Method unwrap() {
    return method;
  }

  @Override
  public boolean isEqualWrappedObject(FxWrapper that) {
    boolean isEqual = false;
    if (that instanceof MethodFxTableEntry) {
      isEqual = this.unwrap().equals(((MethodFxTableEntry) that).unwrap());
    }
    return isEqual;
  }

  // ##########################################################################################
  // ######################### Instance-specific getters ######################################
  // ##########################################################################################

  public SimpleStringProperty getLabel() {
    return label;
  }

  public SimpleStringProperty getFile() {
    return file;
  }

  public SimpleStringProperty getDate() {
    return date;
  }

  public SimpleStringProperty getComment() {
    return comment;
  }

  public List<FxEntry<ParamSet>> getListableContent() {
    List<FxEntry<ParamSet>> list = method.getSets().stream()
        .map(SimpleFxEntry::new)
        .collect(Collectors.toList());
    return list;
  }


}
