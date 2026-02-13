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
import java.util.List;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javax.annotation.Nullable;

public class DeselectableEntryListCell<T> extends TextFieldListCell<FxEntry<T>> {

  // If no multi selection, we should not show the select/deselect logos.
  private final boolean isMultipleSelection;
  private final boolean hideSymbols;
  private final ListAndSearchView<T> view;

  public DeselectableEntryListCell(@Nullable ListAndSearchView<T> view, SelectionMode selectionMode,
      boolean hideSymbols) {
    super();
    this.isMultipleSelection = selectionMode.equals(SelectionMode.MULTIPLE);
    this.hideSymbols = hideSymbols;
    this.view = view;

    // Deselect when clicking on it again.
    /*
    https://stackoverflow.com/questions/19490868/how-to-unselect-a-selected-table-row-upon-second-click-selection-in-javafx

    tableView.setRowFactory(new Callback<TableView<Person>, TableRow<Person>>() {
        @Override
        public TableRow<Person> call(TableView<Person> tableView2) {
            final TableRow<Person> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    final int index = row.getIndex();
                    if (index >= 0 && index < tableView.getItems().size() && tableView.getSelectionModel().isSelected(index)  ) {
                        tableView.getSelectionModel().clearSelection();
                        event.consume();
                    }
                }
            });
            return row;
        }
    });
     */
    addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        final int index = getIndex();
        if (index >= 0 && index < getListView().getItems().size() && getListView()
            .getSelectionModel().isSelected(index)) {
          getListView().getSelectionModel().clearSelection();
          event.consume();
        }
      }
    });
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

      // Custom container
      HBox header = new HBox(5);
      header.setAlignment(Pos.CENTER_LEFT);
      VBox content = new VBox(2, header);

      setText(""); // Note that toString in Converter() still allows for copy paste and sorting!

      if (!hideSymbols) {
        if (fxEntry.isDisqualified()) {
          header.getChildren().add(UiUtil.getViewer("/img/issue.png"));
          setTooltip(UiUtil.tooltip(fxEntry.getDisqualificationNote()));

          // Check if SELECTION symbols shall be shown at all:
        } else if (isMultipleSelection) {
          if (fxEntry.isSelected()) {
            header.getChildren().add(UiUtil.getViewer("/img/yes.png"));
          } else {
            header.getChildren().add(UiUtil.getViewer("/img/deselect.png"));
          }
        } else if (fxEntry.isFavorite()) {
          header.getChildren().add(UiUtil.getViewer("/img/fav.png"));
        }
      }

      //.e.g, in the sub method library we have newline for the date
      List<String> parts = UiUtil.splitByNewlineAndTrim(fxEntry.getCellLabelProperty().get());

      String mainLabel;
      if (!parts.isEmpty()) {
        mainLabel = parts.get(0);
      } else {
        mainLabel = fxEntry.getCellLabelProperty().get();
      }

      // Install main label
      Label mainLabelLbl = new Label(mainLabel);
      // Make bold if more content it expected
      if (parts.size()>1) {
        mainLabelLbl.setStyle("-fx-font-weight: bold");
      }
      header.getChildren().add(mainLabelLbl);

      // Fill in remaining information
      for (int i = 1; i < parts.size(); i++) {
        content.getChildren().add(new Label(parts.get(i)));
      }

      // Call at the end to stabilize UI while filling the containers
      setGraphic(content);
    }
    refreshConverter();
  }


}
