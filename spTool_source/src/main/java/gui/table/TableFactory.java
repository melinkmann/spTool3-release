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

import core.SpTool3Main;
import dataModelNew.MergedSample;
import dataModelNew.Sample;
import dataModelNew.fxImpl.FxChannel;
import dataModelNew.fxImpl.FxSample;
import dataModelNew.mz.ComputedChannel;
import gui.ReadonlyMethodView;
import gui.RerunMethodView;
import gui.StageFactory;
import gui.dialog.notification.NotificationFactory;
import gui.dialog.notification.PopupFactory;
import gui.listAndSearch.SampleListAndTable;
import gui.util.UiUtil;
import io.GlobalIO;
import io.SampleSet;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.Method;
import processing.parameterSets.action.Actions;
import util.ChannelTableComparator;
import util.WindowsSorter;
import visualizer.styles.Colors;
import visualizer.styles.CustomColorPicker;

public class TableFactory {

  private static final Logger LOGGER = LogManager.getLogger(TableFactory.class);

  public static final String EMPTYSTR = "N/A";
  public static final double EMPTYNUM = 0;

  // https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/table-view.htm#CJAGAAEE
  // https://stackoverflow.com/questions/24732883/javafx-table-cell-editing

  public static void setupSampleTable(TableView<FxSample> table,
                                      SampleListAndTable sampleListAndTable, TextField sampleSearchField) {

    Label placeholderLabel = new Label("No samples available");
    table.setPlaceholder(placeholderLabel);

    // set once, else all menu items are lost
    table.setContextMenu(new ContextMenu());
    addLikeDislikeMenu(table);
    addSeparator(table);
    addCloneMenu(table);
    addViewCommentField(table);
    addColorPicker(table);
    addSeparator(table);
    addGroupMenu(table);
    addUngroupMenu(table);
    // addViewGroupMenu(table);
    addSeparator(table);
    addLoadMethodMenu(table);
    addPreviewMethodMenu(table);
    addRerunMethodMenu(table);
    addRerunAllMethodMenu(table);
    addSeparator(table);
    addDeleteSampleMenu(sampleListAndTable);
    addRemoveMenu(sampleListAndTable);
    addSeparator(table);
    addSearchFieldMenu(table, sampleSearchField);

    List<TableColumn<FxSample, ?>> columns = new ArrayList<>();

    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    // ChatGPT suggestion to fix Bug that F2 after creating new grouped sample causes null pointer
    table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.F2) {
        var focusedCell = table.getFocusModel().getFocusedCell();
        if (focusedCell == null || focusedCell.getTableColumn() == null) {
          event.consume(); // stop JavaFX from trying to edit a null column
        }
      }
    });

    //////////////////////////////////////////////////////////////////////////////////////////////
    TableColumn<FxSample, Boolean> col1 = new TableColumn<>("");
    col1.setPrefWidth(30);
    col1.setMaxWidth(50);
    col1.setStyle("-fx-alignment: CENTER-LEFT;");
    // shown value: link to the highlighted property
    col1.setCellValueFactory(cellData -> cellData.getValue().highlightedProperty());

    col1.setCellFactory((tableColumn) -> {
      TableCell<FxSample, Boolean> tableCell = new TableCell<>() {
        @Override
        protected void updateItem(Boolean item, boolean empty) {
          super.updateItem(item, empty);

          // do not show string
          this.setText(null);

          // item refers to the boolean value of the cell, which represents "selected"y
          if (!empty && item) {
            ImageView view = UiUtil.getViewer("/img/fav.png");
            this.setGraphic(view);
          } else {
            this.setGraphic(null);
          }
        }
      };

      return tableCell;
    });

    // Do not edit this!
    col1.setEditable(false);

    columns.add(col1);

    //////////////////////////////////////////////////////////////////////////////////////////////

    TableColumn<FxSample, String> col = new TableColumn<>("Drift");

    col.setPrefWidth(40);
    col.setMaxWidth(75);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new WindowsSorter.WindowsExplorerComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxSample, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxSample, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getDriftFactorProperty();
            } else {
              cellContent = new SimpleStringProperty(" ");
            }
            return cellContent;
          }
        });

    // drag drop
    col.setCellFactory(
        new Callback<TableColumn<FxSample, String>, TableCell<FxSample, String>>() {
          @Override
          public TableCell<FxSample, String> call(TableColumn<FxSample, String> param) {
            TableCell<FxSample, String> cell = TextFieldTableCell.<FxSample>forTableColumn()
                .call(param);
            TableUtils.enableDragDropToAnyTargetByPassingADummyString(cell);
            return cell;
          }
        });

    // Do not edit this!
    col.setEditable(false);

    if (SpTool3Main.SHOW_DRIFT) {
      columns.add(col);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("# NMP");

    col.setPrefWidth(55);
    col.setMaxWidth(75);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new WindowsSorter.WindowsExplorerComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxSample, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxSample, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getEventCountProperty();
            } else {
              cellContent = new SimpleStringProperty(" ");
            }
            return cellContent;
          }
        });

    // drag drop
    col.setCellFactory(
        new Callback<TableColumn<FxSample, String>, TableCell<FxSample, String>>() {
          @Override
          public TableCell<FxSample, String> call(TableColumn<FxSample, String> param) {
            TableCell<FxSample, String> cell = TextFieldTableCell.<FxSample>forTableColumn()
                .call(param);
            TableUtils.enableDragDropToAnyTargetByPassingADummyString(cell);
            return cell;
          }
        });

    // Do not edit this!
    col.setEditable(false);

    columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////
    col = new TableColumn<>("Sample nick name");

    col.setPrefWidth(350);
    col.setMaxWidth(650);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new WindowsSorter.WindowsExplorerComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxSample, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxSample, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getNickNameProperty();
            } else {
              cellContent = new SimpleStringProperty(EMPTYSTR);
            }
            return cellContent;
          }
        });

    // icon and drag drop
    col.setCellFactory(param -> {
      TableCell<FxSample, String> cell = new TextFieldTableCell<>(new DefaultStringConverter()) {

        @Override
        public void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);

          if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
          }

          setText(item); // text stays in the cell

          FxSample rowItem = getTableRow().getItem();
          if (rowItem != null && rowItem.getPlainSample() instanceof MergedSample) {
            ImageView iconview = UiUtil.getViewer("/img/grouped.png");
            iconview.setFitHeight(16);
            iconview.setFitWidth(16);
            ComboBox<Sample> subsamples = new ComboBox<>();
            UiUtil.tooltip(subsamples,
                "Select 'main sample' of the group, e.g., for raw data view.");
            subsamples.setItems(FXCollections.observableArrayList(
                ((MergedSample) rowItem.getPlainSample()).getGroupedSamples()));
            subsamples.setPrefWidth(160);
            subsamples.setCellFactory(listView -> new ListCell<Sample>() {
              @Override
              protected void updateItem(Sample sample, boolean empty) {
                super.updateItem(sample, empty);
                if (empty || sample == null) {
                  setText(null);
                } else {
                  setText(sample.getNickName()
                      + " - " + sample.getSampleFile().getNameWithinFile()
                      + " - " + sample.getSampleFile().getFileName());
                }
              }
            });

            subsamples.setButtonCell(new ListCell<Sample>() {
              @Override
              protected void updateItem(Sample sample, boolean empty) {
                super.updateItem(sample, empty);
                if (empty || sample == null) {
                  setText(null);
                } else {
                  setText(sample.getNickName()
                      + " - " + sample.getSampleFile().getNameWithinFile()
                      + " - " + sample.getSampleFile().getFileName());
                }
              }
            });

            subsamples.setConverter(new StringConverter<Sample>() {
              @Override
              public String toString(Sample sample) {
                if (sample == null) {
                  return "";
                }
                return sample.getNickName()
                    + " - " + sample.getSampleFile().getNameWithinFile()
                    + " - " + sample.getSampleFile().getFileName();
              }

              @Override
              public Sample fromString(String string) {
                // Only needed if comboBox.setEditable(true)
                // You can parse or look up Sample objects here if necessary.
                return null;
              }
            });

            subsamples.getSelectionModel().select(rowItem.getPlainSample().getPrincipleSample());

            subsamples.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Sample>() {
                  @Override
                  public void changed(ObservableValue<? extends Sample> observable, Sample oldValue,
                                      Sample newValue) {
                    if (newValue != null) {
                      ((MergedSample) rowItem.getPlainSample()).setPrincipleSample(newValue);
                      sampleListAndTable.fireSampleChange();
                    }
                  }
                });

            HBox mergeBox = new HBox(2, iconview, subsamples);
            mergeBox.setAlignment(Pos.CENTER_LEFT);
            setGraphic(mergeBox);   // icon on the left
          } else {
            setGraphic(null);
          }
        }

        // Else, icon is not shown after cancelling.
        @Override
        public void cancelEdit() {
          super.cancelEdit();
          // Force a refresh of text+icon after cancel
          updateItem(getItem(), false);
        }

      };

      TableUtils.enableDragDropToAnyTargetByPassingADummyString(cell);

      return cell;
    });

    // drag drop
    //    col.setCellFactory(
    //        new Callback<TableColumn<FxSample, String>, TableCell<FxSample, String>>() {
    //          @Override
    //          public TableCell<FxSample, String> call(TableColumn<FxSample, String> param) {
    //            TableCell<FxSample, String> cell = TextFieldTableCell.<FxSample>forTableColumn()
    //                .call(param);
    //            TableUtils.enableDragDropToAnyTargetByPassingADummyString(cell);
    //            return cell;
    //          }
    //        });

    // if editable
    col.setOnEditCommit(
        (CellEditEvent<FxSample, String> e) -> {
          int rowIdx = e.getTablePosition().getRow();
          e.getTableView().getItems().get(rowIdx).setLabel(e.getNewValue());
          // This allows navigation with arrow keys after edit
          table.requestFocus();
        });

    columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("Sample name");

    col.setPrefWidth(300);
    col.setMaxWidth(650);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new WindowsSorter.WindowsExplorerComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxSample, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxSample, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getSampleNameProperty();
            } else {
              cellContent = new SimpleStringProperty(EMPTYSTR);
            }
            return cellContent;
          }
        });

    // drag drop
    col.setCellFactory(
        new Callback<TableColumn<FxSample, String>, TableCell<FxSample, String>>() {
          @Override
          public TableCell<FxSample, String> call(TableColumn<FxSample, String> param) {
            TableCell<FxSample, String> cell = TextFieldTableCell.<FxSample>forTableColumn()
                .call(param);
            TableUtils.enableDragDropToAnyTargetByPassingADummyString(cell);
            return cell;
          }
        });

    // Do not edit this!
    col.setEditable(false);

    columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("Comment");

    col.setPrefWidth(500);
    col.setMaxWidth(1200);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new WindowsSorter.WindowsExplorerComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxSample, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxSample, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getCommentProperty();
            } else {
              // Here, we just want an empty cell and not a "N/A" cell, I guess.
              cellContent = new SimpleStringProperty("");
            }
            return cellContent;
          }
        });

    // drag drop: keeps this, as this column is quite wide and it feels nicer if dragging is possible from
    // here
    col.setCellFactory(
        new Callback<TableColumn<FxSample, String>, TableCell<FxSample, String>>() {
          @Override
          public TableCell<FxSample, String> call(TableColumn<FxSample, String> param) {
            TableCell<FxSample, String> cell = TextFieldTableCell.<FxSample>forTableColumn()
                .call(param);
            TableUtils.enableDragDropToAnyTargetByPassingADummyString(cell);
            return cell;
          }
        });

    // if editable
    col.setOnEditCommit(
        (CellEditEvent<FxSample, String> e) -> {
          int rowIdx = e.getTablePosition().getRow();
          e.getTableView().getItems().get(rowIdx).setComment(e.getNewValue());
          // This allows navigation with arrow keys after edit
          table.requestFocus();
        });

    columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("Path");

    col.setPrefWidth(300);
    col.setMaxWidth(1500);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new WindowsSorter.WindowsExplorerComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxSample, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxSample, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getPathProperty();
            } else {
              cellContent = new SimpleStringProperty(EMPTYSTR);
            }
            return cellContent;
          }
        });

    // drag drop
    col.setCellFactory(
        new Callback<TableColumn<FxSample, String>, TableCell<FxSample, String>>() {
          @Override
          public TableCell<FxSample, String> call(TableColumn<FxSample, String> param) {
            TableCell<FxSample, String> cell = TextFieldTableCell.<FxSample>forTableColumn()
                .call(param);
            TableUtils.enableDragDropToAnyTargetByPassingADummyString(cell);
            return cell;
          }
        });

    // Do not edit this!
    col.setEditable(false);

    columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    table.getColumns().addAll(columns);
    table.setEditable(true);
    table.getSelectionModel().setCellSelectionEnabled(false);
    table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    // enable copy and paste
    TableUtils.installCopyPasteHandler(table);
    // double click select
    TableUtils.installDoubleClickSelect(table);

    TableUtils.installHeaderLeft(table);

    // initialize empty
    final ObservableList<FxSample> tabEntries = FXCollections.observableArrayList();
    table.setItems(tabEntries);

    // When the observable list is set, now activate the listener to an item change
    sampleListAndTable.addListenerForSize();
  }

  public static void addLoadMethodMenu(TableView<FxSample> table) {
    MenuItem load = UiUtil.getImageMenuItem("Load method", "/img/load.png");
    table.getContextMenu().getItems().add(load);

    load.setOnAction(e -> {

      NotificationFactory.openYesNo(
          "Replace current method in the method tab with the method from the selected sample? This is " +
              "irreversible.",
          () -> {
            FxSample fxSample = table.getSelectionModel().getSelectedItem();
            if (fxSample != null) {
              Sample sample = fxSample.getPlainSample();
              if (sample != null) {
                sample = sample.getPrincipleSample();

                Method method = sample.getMethod();
                // otherwise a change would affect the method stored in the sample file (it is referenced
                // by the pointer)
                method = method.getCopyWithoutFile();
                if (method != null) {
                  SpTool3Main.getRunTime().getMainWindowCtl().getMethodView().setMethod(method);
                }
              }
            }
          });
    });
  }

  public static void addPreviewMethodMenu(TableView<FxSample> table) {
    MenuItem view = UiUtil.getImageMenuItem("View method", "/img/show.png");
    table.getContextMenu().getItems().add(view);

    view.setOnAction(e -> {
      FxSample fxSample = table.getSelectionModel().getSelectedItem();
      if (fxSample != null) {
        Sample sample = fxSample.getPlainSample();
        if (sample != null) {
          sample = sample.getPrincipleSample();

          Method method = sample.getMethod();
          // otherwise a change would affect the method stored in the sample file (it is referenced by the
          // pointer)
          method = method.getCopyWithoutFile();
          if (method != null) {
            ReadonlyMethodView viewer = new ReadonlyMethodView(method);
            Stage popup = PopupFactory.showOnWindow(viewer, SpTool3Main.getMainStage().getScene(), 900d);
            viewer.setParentSceneForDialogs(popup.getScene());
          }
        }
      }
    });
  }

  public static void addRerunMethodMenu(TableView<FxSample> table) {
    MenuItem view = UiUtil.getImageMenuItem("Adjust", "/img/edit.png");
    table.getContextMenu().getItems().add(view);

    view.setOnAction(e -> {
      FxSample fxSample = table.getSelectionModel().getSelectedItem();
      if (fxSample != null) {
        Sample sample = fxSample.getPlainSample();
        if (sample != null) {
          sample = sample.getPrincipleSample();

          Method method = sample.getMethod();
          // otherwise a change would affect the method stored in the sample file (it is referenced by the
          // pointer)
          method = method.getCopyWithoutFile();
          if (method != null) {
            RerunMethodView viewer = new RerunMethodView(method, fxSample);
            Stage popup = PopupFactory.showOnWindow(viewer, SpTool3Main.getMainStage().getScene(), 900d);
            viewer.setParentSceneForDialogs(popup.getScene());
          }
        }
      }
    });
  }

  public static void addRerunAllMethodMenu(TableView<FxSample> table) {
    MenuItem view = UiUtil.getImageMenuItem("Reprocess", "/img/startAll.png");
    table.getContextMenu().getItems().add(view);

    view.setOnAction(e -> {
      FxSample fxSample = table.getSelectionModel().getSelectedItem();
      if (fxSample != null) {
        Sample sample = fxSample.getPlainSample();
        if (sample != null) {
          sample = sample.getPrincipleSample();

          Method method = sample.getMethod();
          // otherwise a change would affect the method stored in the sample file (it is referenced by the
          // pointer)
          if (method != null) {
            Method copyOfMethod = method.getCopyWithoutFile();
            List<FxSample> placeholderList = new ArrayList<>();
            placeholderList.add(fxSample);
            Actions.reprocess(copyOfMethod, placeholderList);
          }
        }
      }
    });
  }


  public static void addCloneMenu(TableView<FxSample> table) {
    MenuItem view = UiUtil.getImageMenuItem("Clone", "/img/clone.png");
    table.getContextMenu().getItems().add(view);

    view.setOnAction(e -> {
      List<FxSample> fxSamples = table.getSelectionModel().getSelectedItems();
      for (FxSample fxSample : fxSamples) {
        if (fxSample != null) {
          Sample sample = fxSample.getPlainSample();
          Sample copy = sample.copy();
          copy.setNickName("Clone of " + copy.getNickName());
          SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(copy);
        }
        SpTool3Main.getRunTime().getSampleReg().flushWaitingList();
      }
    });
  }


  public static void addSeparator(TableView<FxSample> table) {
    table.getContextMenu().getItems().add(new SeparatorMenuItem());
  }

  public static void addLikeDislikeMenu(TableView<FxSample> table) {
    MenuItem favouriteMenu = UiUtil.getImageMenuItem("Favorite", "/img/fav.png");
    table.getContextMenu().getItems().add(favouriteMenu);

    MenuItem notFavouriteMenu = UiUtil.getImageMenuItem("Undo", "/img/unfav.png");
    table.getContextMenu().getItems().add(notFavouriteMenu);

    favouriteMenu.setOnAction(e -> {
      table.getSelectionModel().getSelectedItems().forEach(s -> s.setFavorite(true));
    });

    notFavouriteMenu.setOnAction(e -> {
      table.getSelectionModel().getSelectedItems().forEach(s -> s.setFavorite(false));
    });
  }

  public static void addViewCommentField(TableView<FxSample> table) {
    MenuItem view = UiUtil.getImageMenuItem("Comment", "/img/edittext.png");
    table.getContextMenu().getItems().add(view);

    view.setOnAction(e -> {
      if (!table.getSelectionModel().getSelectedItems().isEmpty()) {
        FxSample sample = table.getSelectionModel().getSelectedItems().get(0);

        final Popup popup = new Popup();
        // Make the popup close when clicking outside
        popup.setAutoHide(true);
        popup.getScene().getWindow().setWidth(350);
        popup.getScene().getWindow().setHeight(300);

        final Button closeBtn = new Button("OK");
        closeBtn.setOnAction(event -> popup.hide());

        final TextArea textArea = new TextArea();
        textArea.setText(sample.getComment());
        textArea.setEditable(true);
        textArea.setWrapText(true);
        textArea.setPrefSize(300, 250); // Set preferred size for the TextArea
        textArea.textProperty().addListener(new ChangeListener<String>() {
          @Override
          public void changed(ObservableValue<? extends String> observable, String oldValue,
                              String newValue) {
            sample.setComment(newValue);

          }
        });
        textArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
          if (KeyCombination.keyCombination("Ctrl+Enter").match(event)) {
            popup.hide();
          }
        });

        final VBox box = new VBox(2);
        box.getChildren().addAll(textArea, closeBtn);
        popup.getContent().add(box);
        UiUtil.formatPopup(box);

        // Get current mouse position using java.awt.MouseInfo
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        double mouseX = mouseLocation.getX();
        double mouseY = mouseLocation.getY();

        if (!popup.isShowing()) {
          // Show popup near the mouse location -> // Adjust offset as needed
          popup.show(SpTool3Main.getMainStage(), mouseX, mouseY - 350);
        } else {
          popup.setX(mouseX);
          popup.setY(mouseY - 350);
        }
      }
    });
  }

  public static void addSearchFieldMenu(TableView<FxSample> table, TextField sampleSearchField) {

    // Wrap the TextField in a CustomMenuItem
    CustomMenuItem textFieldMenuItem = new CustomMenuItem(sampleSearchField);
    // Prevent the menu from closing when interacting with the TextField
    textFieldMenuItem.setHideOnClick(false);

    table.getContextMenu().getItems().add(textFieldMenuItem);
  }

  public static void addColorPicker(TableView<FxSample> table) {
    MenuItem view = UiUtil.getImageMenuItem("Color", "/img/pickColor.png");
    table.getContextMenu().getItems().add(view);

    view.setOnAction(e -> {
      if (!table.getSelectionModel().getSelectedItems().isEmpty()) {
        FxSample sample = table.getSelectionModel().getSelectedItems().get(0);
        Scene scene = table.getScene();
        Stage parent = null;
        if (scene != null) {
          parent = (Stage) scene.getWindow();
        }
        CustomColorPicker custom = new CustomColorPicker(
            Colors.awtToFx(sample.getPlainSample().getColor()),
            Colors.getDefaultColors(),
            new Consumer<Color>() {
              @Override
              public void accept(Color color) {
                sample.getPlainSample().setColor(Colors.fxToAwt(color));
              }
            },
            parent);
        custom.show();
      }
    });


    //    // Make Box.
//    CustomColorPicker colorPicker = new CustomColorPicker();
//    colorPicker.getCustomColors().clear();
//    colorPicker.getCustomColors().addAll(Colors.getDefaultColors().stream()
//        .map(Colors::getFX).collect(Collectors.toList()));

//    colorButton.setOnAction(e -> {
//      if (colorPicker.getPickerSkin() != null) {
//        popup.getContent().add(colorPicker.getPickerSkin().getPopupContent());
//      }
//      //
//      Optional<Sample> firstSelSample = table.getSelectionModel().getSelectedItems().stream()
//          .map(FxSample::getPlainSample)
//          .findFirst();
//      // try to parse
//      firstSelSample.ifPresent(sample -> colorPicker.setValue(
//          new SpColor(sample.getColor()).getFX()));
//
//      // Position the popup near the button
//      if (!popup.isShowing()) {
//        popup.show(SpTool3Main.getMainStage());
//      } else {
//        popup.hide();
//      }
//    });
//
//
//    // Change Listener.
//    colorPicker.valueProperty().addListener(new ChangeListener<Color>() {
//      @Override
//      public void changed(ObservableValue<? extends Color> observable, Color oldValue,
//          Color newValue) {
//        if (newValue != null && newValue != oldValue) {
//          Optional<Sample> firstSelSample = table.getSelectionModel().getSelectedItems().stream()
//              .map(FxSample::getPlainSample)
//              .findFirst();
//          firstSelSample.ifPresent(sample -> sample.setColor(new SpColor(newValue).get()));
//        }
//      }
//    });
  }

  public static void addDeleteSampleMenu(SampleListAndTable sampleListAndTable) {
    MenuItem notFavouriteMenu = UiUtil
        .getImageMenuItem("Delete Samples Globally", "/img/delete.png");
    notFavouriteMenu.setOnAction(e ->
        NotificationFactory
            .openYesCancel("Delete Sample from all Sets? This is irreversible.", () -> {
              List<Sample> selSamples = sampleListAndTable.getSelSamples();
              SampleSet selSet = sampleListAndTable.getSampleSetListView().getSelectionModel()
                  .getSelectedItem().unwrap();
              if (selSet != null) {
                SpTool3Main.getRunTime().getSampleReg().removeSamplesEntirely(selSamples);
              }
              sampleListAndTable.filterSampleSets();
            }));
    sampleListAndTable.getSampleTableView().getContextMenu().getItems().add(notFavouriteMenu);
  }

  public static void addRemoveMenu(SampleListAndTable sampleListAndTable) {
    MenuItem notFavouriteMenu = UiUtil.getImageMenuItem("Remove Samples from Set", "/img/remove.png");
    notFavouriteMenu.setOnAction(e ->
        NotificationFactory.openYesCancel("Remove selected samples from set? This is irreversible.",
            () -> {
              List<Sample> selSamples = sampleListAndTable.getSelSamples();
              SampleSet selSet = sampleListAndTable.getSampleSetListView().getSelectionModel()
                  .getSelectedItem().unwrap();
              if (selSet != null) {
                selSet.getSamples().removeAll(selSamples);
              }
              sampleListAndTable.filterSampleSets();
            }));
    sampleListAndTable.getSampleTableView().getContextMenu().getItems().add(notFavouriteMenu);
  }

  public static void addGroupMenu(TableView<FxSample> table) {

    MenuItem menu = UiUtil.getImageMenuItem("Group", "/img/group.png");
    table.getContextMenu().getItems().add(menu);

    menu.setOnAction(e -> {
      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      // Don't merge merged samples
      selSamples.removeIf(s -> s instanceof MergedSample);

      if (selSamples.size() > 1) {

        String seedName = "Group " + selSamples.get(0).getNickName();

        TextInputDialog dialog = new TextInputDialog(seedName);
        dialog.setTitle("Group selected samples");
        dialog.setHeaderText("Enter nick name of the merged group:");
        dialog.setContentText("Nick name:");

        try {
          Window window = dialog.getDialogPane().getScene().getWindow();
          if (window instanceof Stage) {
            ((Stage) window).getIcons().add(StageFactory.getSymbol());
          }
        } catch (IllegalArgumentException fe) {
          LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
        }

        // Show dialog and capture the result
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(input -> {
          SampleSet selSet = SpTool3Main.getRunTime().getMainWindowCtl().getSelSampleSet();
          if (selSet != null) {
            selSet.remove(selSamples);
            // create merged sample
            String nickName = GlobalIO.cleanupWindowsFileName(input);
            Sample merged = new MergedSample(nickName, selSamples);
            selSet.add(merged);
            // refresh UI
            SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
          }
        });
      }
    });
  }

  public static void addUngroupMenu(TableView<FxSample> table) {
    MenuItem menu = UiUtil.getImageMenuItem("Undo grouping", "/img/ungroup.png");
    table.getContextMenu().getItems().add(menu);

    menu.setOnAction(e -> {
      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      selSamples.removeIf(Predicate.not(s -> s instanceof MergedSample));

      if (!selSamples.isEmpty()) {
        NotificationFactory.openYesNo(
            "Undo grouping of selected samples? This is irreversible.",
            () -> {
              for (Sample selSample : selSamples) {
                List<Sample> containedSamples = ((MergedSample) selSample).getGroupedSamples();

                SampleSet selSet = SpTool3Main.getRunTime().getMainWindowCtl().getSelSampleSet();
                if (selSet != null) {
                  selSet.remove(selSample);
                  selSet.add(containedSamples);
                }
              }
              // refresh UI
              SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
            });
      }
    });
  }


  public static void addViewGroupMenu(TableView<FxSample> table) {
    MenuItem menu = UiUtil.getImageMenuItem("Select main sample", "/img/viewgroup.png");
    table.getContextMenu().getItems().add(menu);

    menu.setOnAction(e -> {
      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      selSamples.removeIf(Predicate.not(s -> s instanceof MergedSample));

      if (!selSamples.isEmpty()) {
        NotificationFactory.openYesNo(
            "Undo grouping of selected samples? This is irreversible.",
            () -> {
              for (Sample selSample : selSamples) {
                List<Sample> containedSamples = ((MergedSample) selSample).getGroupedSamples();

                SampleSet selSet = SpTool3Main.getRunTime().getMainWindowCtl().getSelSampleSet();
                if (selSet != null) {
                  selSet.remove(selSample);
                  selSet.add(containedSamples);
                }
              }
              // refresh UI
              SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
            });
      }
    });
  }

  /// ///////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////////////////////////////////////////////////////////////////

  public static void setupIsotopeTable(TableView<FxChannel> table) {

    table.setPrefWidth(50);

    Label placeholderLabel = new Label("No isotopes available");
    table.setPlaceholder(placeholderLabel);

    // set once, else all menu items are lost
    table.setContextMenu(new ContextMenu());
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    List<TableColumn<FxChannel, ?>> columns = new ArrayList<>();
    //////////////////////////////////////////////////////////////////////////////////////////////

    TableColumn<FxChannel, String> col = new TableColumn<>("MZ");

    col.setPrefWidth(60);
    col.setMaxWidth(100);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new ChannelTableComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxChannel, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxChannel, String> param) {
            StringProperty cellContent;
            if (param != null) {
              String num = param.getValue().getChannel().getMZStr();
              cellContent = new SimpleStringProperty(num);

            } else {
              cellContent = new SimpleStringProperty(EMPTYSTR);
            }
            return cellContent;
          }
        });

    columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("Element");

    col.setPrefWidth(60);
    col.setMaxWidth(100);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new ChannelTableComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxChannel, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxChannel, String> param) {
            StringProperty cellContent;
            if (param != null) {
              if (param.getValue().getChannel() instanceof ComputedChannel) {
                cellContent = new SimpleStringProperty(param.getValue().getChannel().getUIString());
              } else {
                // element is category, and we get the field name() of the enum here
                String element = param.getValue().getChannel().getCategory().getUIString();
                cellContent = new SimpleStringProperty(element);
              }
            } else {
              cellContent = new SimpleStringProperty(EMPTYSTR);
            }
            return cellContent;
          }
        });

    columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    boolean extendTable = SpTool3Main.getRunTime().getConfParams().getExtendChannelTable().getValue();

    col = new TableColumn<>("# NMP");

    col.setPrefWidth(55);
    col.setMaxWidth(75);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new ChannelTableComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxChannel, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxChannel, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getNmpCountProperty();
            } else {
              cellContent = new SimpleStringProperty(" ");
            }
            return cellContent;
          }
        });

    if (extendTable) columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("NetArea");

    col.setPrefWidth(60);
    col.setMaxWidth(80);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new ChannelTableComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxChannel, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxChannel, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getNetAreaProperty();
            } else {
              cellContent = new SimpleStringProperty(" ");
            }
            return cellContent;
          }
        });

    if (extendTable) columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("µ BG");

    col.setPrefWidth(60);
    col.setMaxWidth(80);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new ChannelTableComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxChannel, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxChannel, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getMuBGProperty();
            } else {
              cellContent = new SimpleStringProperty(" ");
            }
            return cellContent;
          }
        });

    if (extendTable) columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////

    col = new TableColumn<>("S/N");

    col.setPrefWidth(60);
    col.setMaxWidth(80);
    col.setStyle("-fx-alignment: CENTER-LEFT;");

    col.setComparator(new ChannelTableComparator());

    // shown value
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxChannel, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<FxChannel, String> param) {
            StringProperty cellContent;
            if (param != null) {
              cellContent = param.getValue().getSignalToNoiseProperty();
            } else {
              cellContent = new SimpleStringProperty(" ");
            }
            return cellContent;
          }
        });

    if (extendTable) columns.add(col);

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    table.getColumns().addAll(columns);
    table.setEditable(false);
    table.getSelectionModel().setCellSelectionEnabled(false);
    table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    // enable copy and paste
    TableUtils.installCopyPasteHandler(table);
    // double click select: no, when too many isotopes are present, this crashed the UI

    TableUtils.installHeaderLeft(table);

    // initialize empty
    final ObservableList<FxChannel> tabEntries = FXCollections.observableArrayList();
    table.setItems(tabEntries);
  }
}