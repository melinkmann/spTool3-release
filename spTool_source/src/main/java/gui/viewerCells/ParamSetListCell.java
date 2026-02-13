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
import gui.dialog.ListableColor;
import gui.dialog.SimpleFxEntry;
import gui.util.UiUtil;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import processing.parameterSets.EmptyParams;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.FxParamSetImpl;

public class ParamSetListCell extends TextFieldListCell<FxEntry<FxParamSet>> {

  // Custom container
  private final VBox content;
  private final HBox header;

  // Initialize these, or use null and later is null check, or setValue(). Else, the UI flickers.
  private final Label mainLabel = new Label();
  private boolean isBold = false;
  private Node graphic = null;
  private final List<Label> subLabels = new ArrayList<>();

  public ParamSetListCell() {
    super();
    refreshConverter();

    // Custom container
    header = new HBox(5, mainLabel);
    header.setAlignment(Pos.CENTER_LEFT);
    content = new VBox(2, header);
  }

  private void refreshConverter() {
    StringConverter<FxEntry<FxParamSet>> converter = new StringConverter<>() {

      // toString: what is shown during editing; don't show e.g. date or else it will become part of the label when editing finishes
      @Override
      public String toString(FxEntry<FxParamSet> set) {
        return set.getLabel();
      }

      // https://stackoverflow.com/questions/35963888/how-to-create-a-listview-of-complex-objects-and-allow-editing-a-field-on-the-obj
      @Override
      public FxEntry<FxParamSet> fromString(String string) {
        // Get T instance stored in the Cell.
        // Note: Here, return new Instance of sth. only makes sense,
        // if the user input actually creates a NEW instance. What we want here,
        // is returning the old object but call its setter method once.

        // If empty: (which should not happen...), return dummy instance.
        if (isEmpty()) {
          FxEntry<FxParamSet> t = new SimpleFxEntry<>(new FxParamSetImpl(new EmptyParams()));
          t.setLabel(string);
          return t;
        }
        FxEntry<FxParamSet> entry = getItem();
        // Overrides the value.
        entry.setLabel(string);
        return entry;
      }
    };
    setConverter(converter);
  }

  @Override
  public void updateItem(FxEntry<FxParamSet> fxEntry, boolean empty) {
    super.updateItem(fxEntry, empty);

    if (empty || fxEntry == null) {
      setText("");
      setGraphic(null);
    } else {
      //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html
      // https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel

      // Start with tool tip which serves as a preview
      Tooltip tip = new Tooltip(fxEntry.unwrap().getPlainSet().getTooltip());
      tip.setStyle("-fx-font-size: 15;");
      tip.setShowDuration(javafx.util.Duration.seconds(60));
      setTooltip(tip);

      // Go on
      setText(""); // Note that toString in Converter() still allows for copy paste and sorting!

      graphic = ((ListableColor) fxEntry.unwrap()).getShape();
      if (header.getChildren().size() > 1) {
        header.getChildren().set(0, graphic);
      } else {
        header.getChildren().add(0, graphic);
      }

      // This would also reduce the flicker issues...
      // header.setPrefHeight(18);

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
        for (int i = 0; i < parts.size(); i++) {
          subLabels.add(new Label());
        }
        content.getChildren().addAll(subLabels.subList(1, subLabels.size()));
      }
      for (int i = 1; i < parts.size(); i++) {
        subLabels.get(i).setText(parts.get(i));
      }

      // Call at the end to stabilize UI while filling the containers
      setGraphic(content);
    }
    refreshConverter();
  }

  public void updateItem1(FxEntry<FxParamSet> fxEntry, boolean empty) {
    super.updateItem(fxEntry, empty);
    if (empty || fxEntry == null) {
      setText(null);
      setGraphic(null);
    } else {
      //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html
      // https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel

      final HBox header = new HBox(5);
      final VBox content = new VBox(2, header);

      content.setAlignment(Pos.CENTER_LEFT);
      header.setAlignment(Pos.CENTER_LEFT);

      setText(null);// Note that toString in Converter() still allows for copy paste and sorting!
      header.getChildren().clear();
      content.getChildren().clear();
      content.getChildren().add(header);

      if (fxEntry.unwrap() instanceof ListableColor) {
        header.getChildren().add(((ListableColor) fxEntry.unwrap()).getShape());
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
      mainLabelLbl.setStyle("-fx-text-fill: black; ");
      // Make bold if more content it expected
      if (parts.size() > 1) {
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

  // Note: Before creating this class, the simpler version that only
  // added the rectangle and string label value was:
  //    setsInCategoryListView.setCellFactory(new Callback<ListView<ParamSet>, ListCell<ParamSet>>() {
  //      @Override
  //      public ListCell<ParamSet> call(ListView<ParamSet> operationListView) {
  //        return new ListCell<>() {
  //          @Override
  //          public void updateItem(ParamSet paramSet, boolean empty) {
  //            super.updateItem(paramSet, empty);
  //            if (empty || paramSet == null) {
  //              setText("");
  //              setGraphic(null);
  //            } else {
  //              //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel
  //              setText(paramSet.getLabelAsString());
  //              setGraphic(getRectangleForListView(paramSet.getCategory()));
  //            }
  //          }
  //        };
  //      }
  //    });

}
