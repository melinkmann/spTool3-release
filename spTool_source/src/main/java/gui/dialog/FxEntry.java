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

package gui.dialog;

import javafx.beans.property.StringProperty;

/**
 * This is a wrapper class to show items on the UI. In most cases, we do not want to mix UI
 * functionality with simple data classes. For instance, colors, styles, in some cases dates, ...,
 * and especially FxProperty classes (SimpleStringProperty, ...) should be kept outside of simple
 * data classes. Hence, we have this wrapper class which can be used by views to display.
 */
public interface FxEntry<T> extends EditableLabel, ListableDate, ListableColor,
    ListableFavourite {

  T unwrap();

  StringProperty getCellLabelProperty();

  boolean isSortable();

  void setSortable(boolean sortable);

  boolean isSelected();

  void setSelected(boolean selected);

  void setCellLabelProperty(String string);

  boolean isDisqualified();

  void setDisqualified(boolean disqualified);

  String getDisqualificationNote();

  void setDisqualificationNote(String disqualificationNote);

  boolean isEqual(FxEntry<?> other);

  void notifyLabelChange();

  void formatCellLabel();
}
