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

package gui.dialog.caseImpl;

import gui.dialog.FxStageButton;
import gui.util.UiUtil;
import io.GlobalIO;
import io.XmlUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.CsvInterpreterParams;

public class CsvLoader extends ExecuteSubmethod {

  private static final Logger LOGGER = LogManager.getLogger(CsvLoader.class);

  private final ListView<Path> pathView;
  private final TableView<String[]> tablePreview;

  public CsvLoader(List<Path> files) {
    super(
        null,
        loadDefaultCsv(),
        AvailableParameterSets.getOptionAsList(AvailableParameterSets.CSV_READER),
        null,
        FxStageButton.RUN,
        null,
        null);

    // Force super to call this function when its own update method is called
    super.addTaskForRefresh(this::updatePreview);

    setTopText("Load data from files");

    // specials
    BorderPane container = new BorderPane();
    container.setPadding(new Insets(2));
    subContentPane.setCenter(UiUtil.putOnAnchorWithInsets(container));

    subContentPane.setTop(UiUtil.putOnAnchorWithoutInsets(new Label("Preview")));

    pathView = new ListView<>(FXCollections.observableArrayList(files));
    pathView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    pathView.getSelectionModel().selectFirst();
    pathView.setEditable(false);

    subContentPane.setCenter(UiUtil.putOnAnchorWithInsets(pathView, 2));

    pathView.getSelectionModel().getSelectedIndices().addListener(
        new ListChangeListener<Integer>() {
          @Override
          public void onChanged(Change<? extends Integer> c) {
            updatePreview();
          }
        });

    tablePreview = new TableView<>();
    tablePreview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tablePreview.setEditable(false);

    subContentPane.setRight(UiUtil.putOnAnchorWithInsets(tablePreview, 2));

    updatePreview();
  } // end of constructor

  private void updatePreview() {
    ParamSet currentSet = getCurrentMethod();
    tablePreview.getColumns().clear();
    tablePreview.getItems().clear();

    if (currentSet instanceof CsvInterpreterParams) {
      CsvInterpreterParams interpreterParams = (CsvInterpreterParams) currentSet;

      Path previewPath = pathView.getSelectionModel().getSelectedItem();

      if (previewPath != null && Files.isReadable(previewPath)) {
        List<String[]> lines = interpreterParams.getInterpreter().readPreview(previewPath, 100);
        if (!lines.isEmpty()) {

          int maxCols = lines.stream()
              .filter(Objects::nonNull)
              .mapToInt(arr -> arr.length)
              .max()
              .orElse(0);

          // Create columns
          for (int i = 0; i < maxCols; i++) {
            final int colIndex = i;
            TableColumn<String[], String> column = new TableColumn<>(String.valueOf(i + 1));
            column.setCellValueFactory(cellData -> {
              String[] row = cellData.getValue();
              String value = (row != null && colIndex < row.length) ? row[colIndex] : "";
              // in case we seriously misread the csv and all ends up in one cell
              value = value.substring(0, Math.min(1000, value.length()));
              return new SimpleStringProperty(value);
            });
            // pref width
            column.setPrefWidth(110);
            tablePreview.getColumns().add(column);
          }

          // Add data
          tablePreview.getItems().addAll(lines);
        }
      }
    }

    // take care of height
    distributeCurrentHeightToViews(pathView, tablePreview, super.getHeight());
    distributeCurrentWidthToViews(pathView, tablePreview, super.getWidth());
    super.getDialogPane().heightProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue,
          Number newValue) {
        distributeCurrentHeightToViews(pathView, tablePreview, newValue.doubleValue());
      }
    });
    super.getDialogPane().widthProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue,
          Number newValue) {
        distributeCurrentWidthToViews(pathView, tablePreview, newValue.doubleValue());
      }
    });
  }

  /*
 On a GridPane / Dialog, we need to specify the prefWidth.
 Normally, we simply put the viewer on an anchor pane and let the UI find its way.
 Also, to keep the submethod list wide enough, we need to specify some minimum width.
 */
  private void distributeCurrentHeightToViews(Control left, Control right, double totalHeight) {

    // only the bottom half is available
    double topHeight = 0.60 * totalHeight;
    double bottomHeight = 0.40 * totalHeight;

    paramViewerPane.setPrefHeight(topHeight);

    // Also, to keep the submethod list wide enough, we need to specify some minimum width.
    if (left != null) {
      left.setPrefHeight(bottomHeight);
    }

    if (right != null) {
      right.setPrefHeight(bottomHeight);
    }
  }

  private void distributeCurrentWidthToViews(Control left, Control right, double totalWidth) {

    // only the bottom half is available
    double leftWidth = 0.55 * totalWidth;
    double rightWidth = 0.45 * totalWidth;

    // Also, to keep the submethod list wide enough, we need to specify some minimum width.
    if (left != null) {
      left.setPrefWidth(leftWidth);
    }

    if (right != null) {
      right.setPrefWidth(rightWidth);
    }
  }


  @Override
  public void closeAndContinue() {
    super.closeAndContinue();
    // also store the current csv
    if (super.getCurrentMethod() != null) {
      // get default path
      Path defaultCsvPath = GlobalIO.makeDefaultCsvReaderFile();
      // get copy of parameter set in order not to override path in the existing version
      ParamSet currentSet = super.getCurrentMethod().getCopyWithPreviousDateFileAndID();
      currentSet.setAssociatedFileOnDrive(defaultCsvPath);
      currentSet.executeOverridingSave();
      LOGGER.trace("Noted current csv reader parameters as default at " + defaultCsvPath);
    }
  }

  @Nullable
  public static ParamSet loadDefaultCsv() {
    ParamSet defaultSet = null;
    // get the default csv
    Path defaultCsvPath = GlobalIO.makeDefaultCsvReaderFile();
    // also store the current csv
    if (defaultCsvPath != null && Files.isReadable(defaultCsvPath)) {
      // Try to read the default param set.
      List<ParamSet> sets = XmlUtil.readSetsFromXml(defaultCsvPath);
      if (!sets.isEmpty()) {
        if (sets.get(0) != null && sets.get(0) instanceof CsvInterpreterParams) {
          defaultSet = sets.get(0);
          LOGGER.trace("Successfully read default csv reader parameters from " + defaultCsvPath);
        }
      }
    }

    if (defaultSet == null) {
      LOGGER.trace("Cannot read default csv reader parameters from " + defaultCsvPath);
    }

    return defaultSet;
  }
}
