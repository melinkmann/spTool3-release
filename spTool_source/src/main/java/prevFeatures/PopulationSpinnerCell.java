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

package prevFeatures;

import analysis.Population;
import dataModelNew.Trace;
import gui.listAndSearch.SampleListAndTable;
import gui.table.TableFactory;
import gui.table.TableUtils;
import gui.util.UiUtil;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

public class PopulationSpinnerCell extends TableCell<Trace, String> {

  private final TableView<Trace> table;
  private final TableColumn<Trace, String> col;
  private final Spinner<Population> populationSpinner = new Spinner<>();
  private final ObservableList<Population> elements = FXCollections.observableArrayList();
  private final SpinnerValueFactory<Population> valueFactory =
      new SpinnerValueFactory.ListSpinnerValueFactory<>(elements);

  public PopulationSpinnerCell(TableColumn<Trace, String> col,
      TableView<Trace> tableView,
      SampleListAndTable sampleListAndTable) {
    super();

    this.table = tableView;
    this.col = col;

    TableUtils.enableDragDropToAnyTargetByPassingADummyString(this);

    valueFactory.setWrapAround(false); // whether it should loop or not
    valueFactory.setConverter(new StringConverter<Population>() {
      @Override
      public String toString(Population object) {
        if (object != null) {
          return object.getName();
        } else {
          return TableFactory.EMPTYSTR;
        }
      }

      @Override
      public Population fromString(String string) {
        return populationSpinner.getValue();
      }
    });

    populationSpinner.setValueFactory(valueFactory);
    populationSpinner.setPrefWidth(col.getWidth());
    // populationSpinner.setStyle("-fx-font-size: 10");

    // Make Spinner scrollable.
    populationSpinner.setOnScroll(e -> {
      // Ctrl + Scroll to avoid accidental change of population due to scrolling
      if (e.isControlDown()) {
        double deltaY = e.getDeltaY();
        if (deltaY < 0) {
          populationSpinner.decrement(1);
        } else {
          populationSpinner.increment(1);
        }
      }
      e.consume(); // Prevent scrolling of the table
    });

    populationSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
      // Add tooltip to spinner if text too long to read.
      if (populationSpinner.getValue() != null) {
        UiUtil.tooltip(populationSpinner, populationSpinner.getValue().getName());
      }
      // Send to the Trace
      if (getTableRow() != null && getTableRow().getItem() != null && newValue != null) {
        // TODO: Set Population
      }
    });

    // Context Menu for Spinner
    populationSpinner.getEditor().setContextMenu(new ContextMenu());
    MenuItem setToAllMenu = UiUtil.getImageMenuItem("Set for selected",
        "/img/selectAll.png");
    setToAllMenu.setOnAction(e -> {
      List<Trace> selTraces = tableView.getSelectionModel().getSelectedItems();
      for (Trace trace : selTraces) {
        Population value = populationSpinner.getValue();
        if (value != null) {
          // TODO: get Populations List<Population> allInTrace = trace.getPopulations();
          // for (Population population : allInTrace) {
          //   if (value.isEquivalent(population)) {
          // TODO: Set Population trace.getTrace().setSelectedPopulation(population);
          //   }
          // }
        }
      }
      sampleListAndTable.filterSampleSets();
    });
    populationSpinner.getEditor().getContextMenu().getItems().add(setToAllMenu);

    // Make the Spinner follow the column width
    col.widthProperty().addListener(e ->
        populationSpinner.setPrefWidth(col.getWidth()));
  }

  // Refreshes content shown in the container. Note that this essentially fulfills the task of
  // calling "setCellValueFactory()" but here its in conjunction with the CellFactory instead.
  private void refreshConverter() {
    if (getTableRow() != null && getTableRow().getItem() != null) {
      // TODO: getSelectedPopulation() and getAllPopulation()
//      Population selectedPopulation = getTableRow().getItem().getTrace().getSelectedPopulation();
//      List<Population> allPop = getTableRow().getItem().getTrace().getPopulations();
//      Collections.reverse(allPop);
//
//      /*
//      There is a bug here but I do not understand where it comes from.
//      When there are no elements in elements, clear() works fine.
//      When it is not empty, calling clear() will remove all element but leave behind
//      one single "null" item.
//      Note that removeIf(true) does not show this behaviour.
//      For safety, I have left the removeIf(isNull) call in.
//       */
//      //elements.clear();
//      elements.removeIf(p -> true);
//      elements.addAll(allPop);
//      elements.removeIf(Objects::isNull);
//
//      if (elements.contains(selectedPopulation)) {
//        valueFactory.setValue(selectedPopulation);
//      } else if (!elements.isEmpty()) {
//        // Spinner goes upwards for next but it feels better to go down in processing order
//        valueFactory.setValue(allPop.get(0));
//      }
    }
  }

  @Override
  protected void updateItem(String item, boolean empty) {
    super.updateItem(item, empty);

    if (empty) {
      setGraphic(null);
      setPrefHeight(20);
      elements.clear();
    } else {
      setGraphic(populationSpinner);
      setPrefHeight(20);
      //
      refreshConverter();
    }

    refreshConverter();
  }


}
