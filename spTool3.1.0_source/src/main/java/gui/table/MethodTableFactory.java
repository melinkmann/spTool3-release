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

import gui.MethodView;
import gui.dialog.FxEntry;
import gui.dialog.SimpleFxEntry;
import gui.viewerCells.DeselectableEntryListCell;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameters.FxParameter;
import util.Util;
import util.WindowsSorter.WindowsExplorerComparator;

public abstract class MethodTableFactory {

  public static final String COL_FAV = "Fav.";
  public static final String COL_METHOD = "Name";
  public static final String COL_FILE = "File";
  public static final String COL_DATE = "Date";
  public static final String COL_COMMENT = "Comment";
  public static final String COL_SUBMETHODS = "Submethods";

  private static final double ROW_HEIGHT = 125;
  private static final double ROW_HEIGHT_EXTD = ROW_HEIGHT * 2.25;
  private static final double LIST_VIEW_WIDTH_PROPORTION = 0.25;
  private static final double PARAMETER_WIDTH_PROPORTION = 1 - LIST_VIEW_WIDTH_PROPORTION;


  // https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/table-view.htm#CJAGAAEE
  // https://stackoverflow.com/questions/24732883/javafx-table-cell-editing

  public static TableView<FxTableEntry<Method>> create(List<Method> content) {

    // ##########################################################################################
    // ######################### Create FxEntries from the content
    // ##########################################################################################

    final List<FxTableEntry<Method>> fxContent = content.stream()
        .map(MethodFxTableEntry::new)
        .collect(Collectors.toList());

    List<TableColumn<FxTableEntry<Method>, String>> columns = new ArrayList<>();

    TableView<FxTableEntry<Method>> table = new TableView<>();

    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    SimpleObjectProperty<FxTableEntry<Method>> selectedRowProperty = new SimpleObjectProperty<>();

    //////////////////////////////////////////////////////////////////////////////////////////////
    TableColumn<FxTableEntry<Method>, String> colFav = TableUtils.createColumnRibbon(
        COL_FAV,
        50, 100,
        new WindowsExplorerComparator());

    TableColumn<FxTableEntry<Method>, String> colMethod = TableUtils.createColumnRibbon(
        COL_METHOD,
        250, 500,
        new WindowsExplorerComparator());

    columns.add(colMethod);

    TableColumn<FxTableEntry<Method>, String> colFile = TableUtils.createColumnRibbon(
        COL_FILE,
        150, 900,
        new WindowsExplorerComparator());

    columns.add(colFile);

    TableColumn<FxTableEntry<Method>, String> colDate = TableUtils.createColumnRibbon(
        COL_DATE,
        80, 300,
        Util.DATE_COMPARATOR);

    columns.add(colDate);

    TableColumn<FxTableEntry<Method>, String> colComment = TableUtils.createColumnRibbon(
        COL_COMMENT,
        175, 900,
        new WindowsExplorerComparator());

    columns.add(colComment);

    TableColumn<FxTableEntry<Method>, String> colSubmethods = TableUtils.createColumnRibbon(
        COL_SUBMETHODS,
        700, 1500,
        new WindowsExplorerComparator());

    columns.add(colSubmethods);

    // ####################################################################################

    // How to make the String for the Cell ("What to do with an entry??")
    TableUtils.installSimpleCellValueFactory(colMethod,
        entry -> {
          StringProperty stringProperty;
          if (entry instanceof MethodFxTableEntry) {
            MethodFxTableEntry methodEntry = (MethodFxTableEntry) entry;
            stringProperty = methodEntry.getLabel();
          } else {
            stringProperty = new SimpleStringProperty(TableUtils.UNAVAILABLE_STR);
          }
          return stringProperty;
        });

    // How to make the String for the Cell ("What to do with an entry??")
    TableUtils.installSimpleCellValueFactory(colFile,
        entry -> {
          StringProperty stringProperty;
          if (entry instanceof MethodFxTableEntry) {
            MethodFxTableEntry methodEntry = (MethodFxTableEntry) entry;
            stringProperty = methodEntry.getFile();
          } else {
            stringProperty = new SimpleStringProperty(TableUtils.UNAVAILABLE_STR);
          }
          return stringProperty;
        });

    // How to make the String for the Cell ("What to do with an entry??")
    TableUtils.installSimpleCellValueFactory(colDate,
        entry -> {
          StringProperty stringProperty;
          if (entry instanceof MethodFxTableEntry) {
            MethodFxTableEntry methodEntry = (MethodFxTableEntry) entry;
            stringProperty = methodEntry.getDate();
          } else {
            stringProperty = new SimpleStringProperty(TableUtils.UNAVAILABLE_STR);
          }
          return stringProperty;
        });

    //////////////////////////////////////////////////////////////////////////////////////////

    // custom cell factory: Put TextArea for the comment in the TableCell
    colFile.setCellFactory((tableColumn) -> {
      TableCell<FxTableEntry<Method>, String> tableCell = new TableCell<>() {

        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);

          // Show node only
          this.setText("");
          setPrefHeight(ROW_HEIGHT);

          if (!empty) {
            Label text = new Label(item);
            text.setWrapText(true);
            setGraphic(text);
          }
        }


      };
      return tableCell;
    });

    // custom cell factory: Put TextArea for the comment in the TableCell
    colComment.setSortable(false); // cannot sort cols with content: null pointers in comparator
    colComment.setCellFactory((tableColumn) -> {
      TableCell<FxTableEntry<Method>, String> tableCell = new TableCell<>() {

        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);

          // Show node only
          this.setText("");
          setPrefHeight(ROW_HEIGHT);

          if (!empty) {
            TextArea textArea = new TextArea();
            textArea.setWrapText(true);
            textArea.setEditable(false);

            if (getTableRow() != null && getTableRow().getItem() != null) {
              FxTableEntry<Method> selectedEntry = getTableRow().getItem();
              if (selectedEntry instanceof MethodFxTableEntry) {
                MethodFxTableEntry methodFxEntry = (MethodFxTableEntry) selectedEntry;
                textArea.setText(methodFxEntry.getComment().getValue());
              }
            }

            // Make the ListView follow the column width
            tableColumn.widthProperty().addListener(e ->
                textArea.setPrefWidth(tableColumn.getWidth()));

            // select cell if clicked on area. This makes sure that when we click on the area, the row is selected for method selection.
            textArea.focusedProperty().addListener(new ChangeListener<Boolean>() {
              @Override
              public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                  Boolean newValue) {
                /*
                 Only set if is focused. Why? If we also set false when losing focus,
                 we'd also deselect by clicking anywhere else on the iu.
                 However, we only want selection change when clicking on another line IN the table.
                 */

                if (newValue) {
                  getTableView().getSelectionModel().select(getTableRow().getItem());
                }
              }
            });

            // Expand when selected: check if newly selected row is THIS row
            selectedRowProperty.addListener(new ChangeListener<FxTableEntry<Method>>() {
              @Override
              public void changed(ObservableValue<? extends FxTableEntry<Method>> observable,
                  FxTableEntry<Method> oldValue, FxTableEntry<Method> newValue) {

//                System.out.println(getTableRow().getItem()!=null? getTableRow().getItem().unwrap().getLabelParam().getValue():"Empty");
//                System.out.println(newValue.unwrap()!=null? newValue.unwrap().getLabelParam().getValue():"Empty");

                if (newValue != null && getTableRow().getItem() != null) {
                  Method methodOfCell = getTableRow().getItem().unwrap();
                  if (methodOfCell.hasEqualParameters(newValue.unwrap())) {
                    setPrefHeight(ROW_HEIGHT_EXTD);
                  } else {
                    setPrefHeight(ROW_HEIGHT);
                  }
                } else {
                  setPrefHeight(ROW_HEIGHT);
                }
              }
            });

            this.setGraphic(textArea);
          } else {
            setGraphic(null);
            setPrefHeight(25);
          }
        }
      };

      return tableCell;
    });

    // custom cell factory
    //https://www.java-forum.org/thema/listview-in-tablecell.191427/
    colSubmethods.setSortable(false); // cannot sort cols with content: null pointers in comparator

    colSubmethods.setCellFactory((tableColumn) -> {
      TableCell<FxTableEntry<Method>, String> tableCell = new TableCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);

          // Show node only
          this.setText("");
          setPrefHeight(ROW_HEIGHT);

          if (!empty) {
            ListView<FxEntry<FxParamSet>> listView = new ListView<>();

            if (getTableRow() != null && getTableRow().getItem() != null) {
              FxTableEntry<Method> selectedEntry = getTableRow().getItem();
              if (selectedEntry instanceof MethodFxTableEntry) {
                MethodFxTableEntry methodFxEntry = (MethodFxTableEntry) selectedEntry;

                List<FxEntry<ParamSet>> subMethods = methodFxEntry.getListableContent();
                // Note: Only the FxInstance implements ListableLabel, which is required for the EntryListCell & the FxEntry
                List<FxEntry<FxParamSet>> subMethodsFx = subMethods.stream()
                    .map(FxEntry::unwrap)
                    .filter(Objects::nonNull)
                    // Has to be uneditable since we only want to show what's there w/o editing
                    .map(ParamSet::getUneditableObservableInstance)
                    .map(SimpleFxEntry::new)
                    .collect(Collectors.toList());

                // How the ListView should build its cell.
                listView.setCellFactory(
                    c -> new DeselectableEntryListCell<>(null, SelectionMode.SINGLE, true));

                // What to show.
                listView.setItems(FXCollections.observableArrayList(subMethodsFx));
              }
            }

            // Show the submethod content
            final HBox box = new HBox(5, listView);

            listView.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<FxEntry<FxParamSet>>() {
                  @Override
                  public void changed(ObservableValue<? extends FxEntry<FxParamSet>> observable,
                      FxEntry<FxParamSet> oldValue, FxEntry<FxParamSet> newValue) {
                    if (newValue != null) {
                      ListView<FxParameter<?>> contentView =
                          MethodView.createParamView(newValue.unwrap());
                      contentView.setPrefWidth(PARAMETER_WIDTH_PROPORTION * tableColumn.getWidth());
                      box.getChildren().clear();
                      box.getChildren().addAll(listView, contentView);

                      // select cell if clicked on area. This makes sure that when we click on the area, the row is selected for method selection.
                      contentView.focusedProperty().addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(ObservableValue<? extends Boolean> observable,
                            Boolean oldValue,
                            Boolean newValue) {

                        /*
                         Only set if is focused. Why? If we also set false when losing focus,
                         we'd also deselect by clicking anywhere else on the iu.
                         However, we only want selection change when clicking on another line IN the table.
                         */

                          if (newValue) {
                            getTableView().getSelectionModel().select(getTableRow().getItem());
                          }
                        }
                      });

                    } else {
                      // less busy if we remove the settings panel
                      box.getChildren().clear();
                      box.getChildren().add(listView);
                    }
                  }
                });

            // Width
            box.setPrefWidth(tableColumn.getWidth());
            listView.setPrefWidth(LIST_VIEW_WIDTH_PROPORTION * tableColumn.getWidth());

            // Make the ListView follow the column width
            tableColumn.widthProperty().addListener(e -> {
              box.setPrefWidth(tableColumn.getWidth());
              listView.setPrefWidth(LIST_VIEW_WIDTH_PROPORTION * tableColumn.getWidth());

              // get the other listview with the parameters
              if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof Control) {
                ((Control) box.getChildren().get(1)).setPrefWidth(
                    PARAMETER_WIDTH_PROPORTION * tableColumn.getWidth());
              }
            });

            // select cell if clicked on area. This makes sure that when we click on the area, the row is selected for method selection.
            listView.focusedProperty().addListener(new ChangeListener<Boolean>() {
              @Override
              public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                  Boolean newValue) {
                /*
                 Only set if is focused. Why? If we also set false when losing focus,
                 we'd also deselect by clicking anywhere else on the iu.
                 However, we only want selection change when clicking on another line IN the table.
                 */

                if (newValue) {
                  getTableView().getSelectionModel().select(getTableRow().getItem());
                }
              }
            });

            // Expand when selected: check if newly selected row is THIS row
            selectedRowProperty.addListener(new ChangeListener<FxTableEntry<Method>>() {
              @Override
              public void changed(ObservableValue<? extends FxTableEntry<Method>> observable,
                  FxTableEntry<Method> oldValue, FxTableEntry<Method> newValue) {
                if (newValue != null && getTableRow().getItem() != null) {
                  Method methodOfCell = getTableRow().getItem().unwrap();
                  if (methodOfCell.hasEqualParameters(newValue.unwrap())) {
                    setPrefHeight(ROW_HEIGHT_EXTD);
                  } else {
                    setPrefHeight(ROW_HEIGHT);
                  }
                } else {
                  setPrefHeight(ROW_HEIGHT);
                }
              }
            });

            this.setGraphic(box);
          } else {
            setGraphic(null);
            setPrefHeight(25);
          }
        }
      };
      return tableCell;
    });

    ///////////////////////////////////////////////////////////////////////////////////
    // if editable
    // How to make the String for the Cell ("What to do with an entry and a new String??")
    TableUtils.installEditableNameOnCommit(colMethod,
        (entry, string) -> {
          if (entry instanceof MethodFxTableEntry) {
            MethodFxTableEntry methodEntry = (MethodFxTableEntry) entry;
            methodEntry.unwrap().getLabelParam().setValue(string); // Set to the method instance
            methodEntry.getLabel().set(string); // stet he the property
          }
        });

    // How to make the String for the Cell ("What to do with an entry and a new String??")
    TableUtils.installEditableNameOnCommit(colComment,
        (entry, string) -> {
          if (entry instanceof MethodFxTableEntry) {
            MethodFxTableEntry methodEntry = (MethodFxTableEntry) entry;
            methodEntry.unwrap().getCommentParam().setValue(string); // Set to the method instance
            methodEntry.getComment().set(string); // set the the property
          }
        });

    //////////////////////////////////////////////////////////////////////////////////////////////

    table.getSelectionModel().selectedItemProperty().addListener(
        new ChangeListener<FxTableEntry<Method>>() {
          @Override
          public void changed(ObservableValue<? extends FxTableEntry<Method>> observable,
              FxTableEntry<Method> oldValue, FxTableEntry<Method> newValue) {
            selectedRowProperty.set(newValue);
          }
        });

    //////////////////////////////////////////////////////////////////////////////////////////////

    table.getColumns().addAll(columns);
    table.setEditable(false);

    // Allows to select single cells; visually, FALSE means that the entire row is highlighted
    table.getSelectionModel().setCellSelectionEnabled(false);
    table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    // enable copy and paste
    TableUtils.installCopyPasteHandler(table);
    // double click select
    TableUtils.installDoubleClickSelect(table);

    TableUtils.installHeaderLeft(table);

    // initialize empty
    final ObservableList<FxTableEntry<Method>> tabEntries = FXCollections.observableArrayList();
    tabEntries.addAll(fxContent);
    table.setItems(tabEntries);

    // Sort once the entries are set: Problem: we do not set the entries here. Samples are continuously added later... Find out where we do it...
    table.sort();

    return table;
  }


}

