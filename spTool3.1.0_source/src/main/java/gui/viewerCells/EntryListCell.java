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

package gui.viewerCells;

import gui.dialog.FxEntry;
import gui.dialog.SimpleFxEntry;
import gui.listAndSearch.ListAndSearchView;
import gui.util.UiUtil;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javax.annotation.Nullable;

public class EntryListCell<T> extends TextFieldListCell<FxEntry<T>> {

  // If no multi selection, we should not show the select/deselect logos.
  private final boolean isMultipleSelection;
  private final boolean useSelectionFromSelectable;
  private final boolean hideSymbols;
  private final ListAndSearchView<T> view;

  // Custom container
  private final VBox content;
  private final HBox header;

  // Initialize these, or use null and later is null check, or setValue(). Else, the UI flickers.
  private final Label mainLabel = new Label();
  private boolean isBold = false;
  private final List<Label> subLabels = new ArrayList<>();

  private final Node issueSymbol= UiUtil.getViewer("/img/issue.png");
  private final Node selectSymbol= UiUtil.getViewer("/img/yes.png");
  private final Node deselectSymbol= UiUtil.getViewer("/img/deselect.png");
  private final Node favSymbol= UiUtil.getViewer("/img/fav.png");

  public EntryListCell(@Nullable ListAndSearchView<T> view, SelectionMode selectionMode,
      boolean useSelectionFromSelectable, boolean hideSymbols) {
    super();
    refreshConverter();

    this.isMultipleSelection = selectionMode.equals(SelectionMode.MULTIPLE);
    this.useSelectionFromSelectable = useSelectionFromSelectable;
    this.hideSymbols = hideSymbols;
    this.view = view;

    // Custom container
    header = new HBox(5, mainLabel);
    header.setAlignment(Pos.CENTER_LEFT);
    content = new VBox(2, header);
  }

  private void refreshConverter() {
    StringConverter<FxEntry<T>> converter = new StringConverter<>() {
      @Override
      public String toString(FxEntry<T> entry) {
        return entry.getLabel();
      }

      // https://stackoverflow.com/questions/35963888/how-to-create-a-listview-of-complex-objects-and-allow-editing-a-field-on-the-obj
      @Override
      public FxEntry<T> fromString(String string) {
        if (isEmpty()) {
          // Get T instance stored in the Cell.
          // Note: Here, return new Instance of sth. only makes sense,
          // if the user input actually creates a NEW instance. What we want here,
          // is returning the old object but call its setter method once.
          FxEntry<T> t = new SimpleFxEntry<>(null);
          t.setLabel(string);
          return t;
        }
        FxEntry<T> entry = getItem();
        // Overrides the value.
        entry.setLabel(string);
        // Re-sort the list.
        if (view != null) {
          view.filterList();
        }
        return entry;
      }
    };
    setConverter(converter);
  }

  @Override
  public void updateItem(FxEntry<T> fxEntry, boolean empty) {
    super.updateItem(fxEntry, empty);
    if (empty || fxEntry == null) {
      setText("");
      setGraphic(null);
    } else {
      //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html
      // https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel

      setText(""); // Note that toString in Converter() still allows for copy paste and sorting!

      if (!hideSymbols) {
        if (header.getChildren().size() > 1) {
          if (fxEntry.isDisqualified()) {
            header.getChildren().set(0,issueSymbol);
            setTooltip(UiUtil.tooltip(fxEntry.getDisqualificationNote()));

            // Check if SELECTION symbols shall be shown at all:
          } else if (isMultipleSelection && useSelectionFromSelectable) {
            if (fxEntry.isSelected()) {
              header.getChildren().set(0,selectSymbol);
            } else {
              header.getChildren().set(0,deselectSymbol);
            }
          } else if (fxEntry.isFavorite()) {
            header.getChildren().set(0,favSymbol);
          } else {
            header.getChildren().remove(0);
          }
        }else {
          if (fxEntry.isDisqualified()) {
            header.getChildren().add(0,issueSymbol);
            setTooltip(UiUtil.tooltip(fxEntry.getDisqualificationNote()));

            // Check if SELECTION symbols shall be shown at all:
          } else if (isMultipleSelection && useSelectionFromSelectable) {
            if (fxEntry.isSelected()) {
              header.getChildren().add(0,selectSymbol);
            } else {
              header.getChildren().add(0,deselectSymbol);
            }
          } else if (fxEntry.isFavorite()) {
            header.getChildren().add(0,favSymbol);
          }
        }


      }

      //.e.g, in the sub method library we have newline for the date
      List<String> parts = UiUtil.splitByNewlineAndTrim(fxEntry.getCellLabelProperty().get());

      String mainLabelStr;
      if (!parts.isEmpty()) {
        mainLabelStr = parts.get(0);
      } else {
        mainLabelStr = fxEntry.getCellLabelProperty().get();
      }
      mainLabel.setText(mainLabelStr);
      // Make bold if more content it expected
      if (parts.size() > 1 && !isBold) {
        mainLabel.setStyle("-fx-font-weight: bold");
        isBold = true;
      }

      // Fill in remaining information
      if (subLabels.size() < parts.size()) {
        for (int i = 0; i < parts.size(); i++){
          subLabels.add(new Label());
        }
        content.getChildren().addAll(subLabels.subList(1,subLabels.size()));
      }
      for (int i = 1; i < parts.size(); i++) {
        subLabels.get(i).setText(parts.get(i));
      }

      // Call at the end to stabilize UI while filling the containers
      setGraphic(content);
    }
    refreshConverter();
  }


}
