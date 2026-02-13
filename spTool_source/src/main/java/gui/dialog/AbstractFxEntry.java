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

import gui.util.UiUtil;
import java.util.Date;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import visualizer.styles.JaasColors;

public abstract class AbstractFxEntry<T> implements FxEntry<T> {

  // e.g. methods must maintain order of their sub-methods -> not sortable
  private boolean sortable = true;

  //  for the list view
  private final StringProperty cellLabelProperty = new SimpleStringProperty("Entry");

  // select and deselect with clicks, overriding the selection model of a listview
  private final BooleanProperty selected = new SimpleBooleanProperty(false);

  // e.g. for Files when not found on drive anymore
  private final BooleanProperty disqualified = new SimpleBooleanProperty(false);
  private String disqualificationNote = "Entry not found.";

  private final T t;

  public AbstractFxEntry(T t) {
    this.t = t;
  }

  public AbstractFxEntry(T t, boolean sortable) {
    this.t = t;
    this.sortable = sortable;
  }

  // Essentials

  public T unwrap() {
    return t;
  }

  public StringProperty getCellLabelProperty() {
    return cellLabelProperty;
  }

  public boolean isSortable() {
    return sortable;
  }

  public void setSelected(boolean selected) {
    this.selected.set(selected);
  }

  public void setCellLabelProperty(String string) {
    this.cellLabelProperty.set(string);
  }

  public boolean isSelected() {
    return selected.get();
  }

  public boolean isDisqualified() {
    return disqualified.get();
  }

  public String getDisqualificationNote() {
    return disqualificationNote;
  }

  public void setDisqualified(boolean disqualified) {
    this.disqualified.set(disqualified);
  }

  public void setDisqualificationNote(String disqualificationNote) {
    this.disqualificationNote = disqualificationNote;
  }

  public void setSortable(boolean sortable) {
    this.sortable = sortable;
  }

  @Override
  public boolean isEqual(FxEntry<?> other) {
    boolean equal = false;
    if (other != null) {
      if (this.unwrap() != null && other.unwrap() != null) {
        equal = this.unwrap().equals(other.unwrap());
      }
    }
    return equal;
  }

  /*
   Implemented methods from the List-specific methods. Here only defaults are used.
   If special cases are desired, then override in the subclasses!
   */

  @Override
  public String getLabel() {
    String label = t.toString();
    if (t instanceof ListableLabel) {
      label = ((ListableLabel) t).getLabel();
    }
    return label;
    // Override in sub classes
  }

  @Override
  public void setLabel(String label) {
    if (t instanceof EditableLabel) {
      ((EditableLabel) t).setLabel(label);
      notifyLabelChange(); // in subclass
    }
    // Override in sub classes
  }

  @Override
  public Color getColor() {
    Color col = JaasColors.BLACK.getFX();
    if (t instanceof ListableColor) {
      col = ((ListableColor) t).getColor();
    }
    return col;
    // Override in sub classes
  }

  @Override
  public Node getShape() {
    Node shape = UiUtil.getRectangleForListView(getColor());
    if (t instanceof ListableColor) {
      shape = ((ListableColor) t).getShape();
    }
    return shape;
    // Override in sub classes
  }

  @Override
  public boolean hasDate() {
    if (t instanceof ListableDate) {
      return ((ListableDate) t).hasDate();
    } else {
      return false;
    }
    // Override in sub classes
  }

  @Override
  public Date getDate() {
    Date date = new Date();
    if (t instanceof ListableDate) {
      date = ((ListableDate) t).getDate();
    }
    return date;
    // Override in sub classes
  }


  @Override
  public void setFavorite(boolean isFavourite) {
    if (t instanceof ListableFavourite) {
      ((ListableFavourite) t).setFavorite(isFavourite);
    }
    // Override in sub classes
  }

  @Override
  public boolean isFavorite() {
    boolean fav = false;
    if (t instanceof ListableFavourite) {
      fav = ((ListableFavourite) t).isFavorite();
    }
    return fav;
    // Override in sub classes
  }

}
