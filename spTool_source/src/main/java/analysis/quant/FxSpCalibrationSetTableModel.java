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

package analysis.quant;

import dataModelNew.mz.CalChannel;
import dataModelNew.mz.Channel;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import math.units.ConvertibleUnit;
import math.units.enums.SensitivityUnit;
import processing.options.CalibrationStrategy;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * This serves as the "FxInstance" to manage changes of ionic response affecting TE, ...
 */
public class FxSpCalibrationSetTableModel {

  private final ObservableList<ResponseTableRow> rows =
      FXCollections.observableArrayList();


  public ObservableList<ResponseTableRow> getRows() {
    return rows;
  }

  // All isotopes shown here must reflect the SpCalibrationSet or else this gets confusing
  public void fill(SpCalibrationSet spCalSet) {
    rows.clear();

    for (CalChannel calCh : spCalSet.listChannels()) {
      ResponseTableRow row = new ResponseTableRow(calCh, spCalSet);

      FxQuantity ionicFxQ = new FxQuantity(spCalSet.getOrCreateIonicResponse(calCh));
      FxQuantity npFxQ = new FxQuantity(spCalSet.getOrCreateNpResponse(calCh));
      FxQuantity aerosolTEFxQ = new FxQuantity(spCalSet.getOrCreateAerosolTE(calCh));
      FxQuantity pnTEFxQ = new FxQuantity(spCalSet.getOrCreateParticleNumberTE(calCh));

      row.setIonicResponse(ionicFxQ);
      row.setNpResponse(npFxQ);
      row.setAerosolTEPct(aerosolTEFxQ);
      row.setParticleNumberTEPct(pnTEFxQ);

      // listeners to refresh te when ionic or np response changes
      if (spCalSet.getCalibrationStrategy().equals(CalibrationStrategy.SIZE_METHOD)) {

        ionicFxQ.getValueProperty().addListener(new ChangeListener<Number>() {
          @Override
          public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                              Number newValue) {
            SensitivityUnit targetunit = SensitivityUnit.CTS_PER_FEMTOGRAM;

            double npCtsFg = npFxQ.getPlainQuantity().getUnit().convert(
                npFxQ.getPlainQuantity().getValue(), targetunit);
            double ionicCtsFg = ionicFxQ.getPlainQuantity().getUnit().convert(
                newValue.doubleValue(), targetunit);

            if (npCtsFg > 0 && Double.isFinite(npCtsFg) && Double.isFinite(ionicCtsFg)) {
              double aerosolTEVal = 100 * ionicCtsFg / npCtsFg;

              // important: set change via FX instance to trigger refresh
              aerosolTEFxQ.getValueProperty().set(aerosolTEVal);
              // Do not override PN TE: keep it at zero; later logic must default back to aerosol
              // particleNumberTE.get(iso).change(aerosolTEVal);
            }
          }
        });

        npFxQ.getValueProperty().addListener(new ChangeListener<Number>() {
          @Override
          public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                              Number newValue) {
            SensitivityUnit targetunit = SensitivityUnit.CTS_PER_FEMTOGRAM;

            double npCtsFg = npFxQ.getPlainQuantity().getUnit().convert(
                newValue.doubleValue(), targetunit);
            double ionicCtsFg = ionicFxQ.getPlainQuantity().getUnit().convert(
                ionicFxQ.getPlainQuantity().getValue(), targetunit);

            if (npCtsFg > 0 && Double.isFinite(npCtsFg) && Double.isFinite(ionicCtsFg)) {
              double aerosolTEVal = 100 * ionicCtsFg / npCtsFg;

              // important: set change via FX instance to trigger refresh
              aerosolTEFxQ.getValueProperty().set(aerosolTEVal);
              // Do not override PN TE: keep it at zero; later logic must default back to aerosol
              // particleNumberTE.get(iso).change(aerosolTEVal);
            }
          }
        });
      }

      rows.add(row);
    }
  }


  /// Static column builder methods
  public static TableColumn<ResponseTableRow, Number> quantityColumn(
      String title,
      Function<ResponseTableRow, FxQuantity> extractor,
      ToggleButton toggle) {
    TableColumn<ResponseTableRow, Number> col =
        new TableColumn<>();

    col.setPrefWidth(145);

    ComboBox<ConvertibleUnit> unitCombo = new ComboBox<>();
    unitCombo.getItems().setAll(SensitivityUnit.values());
    unitCombo.getSelectionModel().select(SensitivityUnit.CTS_PER_FEMTOGRAM);
    unitCombo.setCellFactory(cb -> new ListCell<>() {
      @Override
      protected void updateItem(ConvertibleUnit unit, boolean empty) {
        super.updateItem(unit, empty);
        setText(empty || unit == null ? null : unit.getUiString());
      }
    });
    unitCombo.setButtonCell(new ListCell<>() {
      @Override
      protected void updateItem(ConvertibleUnit unit, boolean empty) {
        super.updateItem(unit, empty);
        setText(empty || unit == null ? null : unit.getUiString());
      }
    });

    col.setGraphic(quantityHeader(title, unitCombo, toggle));

    col.setCellValueFactory(cd -> {
      FxQuantity q = extractor.apply(cd.getValue());
      return q == null ? null : q.getValueProperty();
    });

    col.setCellFactory(tc -> new TextFieldTableCell<>(
        new StringConverter<>() {

          @Override
          public String toString(Number value) {
            return value == null ? "" : SnF.doubleToString(value.doubleValue(), NF.D1C3, NF.D1C3Exp);
          }

          @Override
          public Number fromString(String text) {
            if (text == null || text.isBlank()) {
              return null;
            }
            try {
              return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
              return null; // is handled in the onCommit lambda!
            }
          }
        }
    ));

    col.setOnEditCommit(event -> {

      if (event.getNewValue() == null) {
        event.getTableView().refresh(); // just reload old value(s)
        return;
      }


      ResponseTableRow row = event.getRowValue();
      // Send the manual value to the Quantity
      FxQuantity q = extractor.apply(row);

      if (q != null && event.getNewValue() != null) {
        q.getValueProperty().set(event.getNewValue().doubleValue());
      }

      /*
      ISSUE: this affects the plain instance but not FX.
      Instead, just write manual to click "fill Column" button
       */
      // AFTER setting the value in the Quantity: refresh aerosol TE on manual edit
      // if (row != null) {
      //   row.getSpCalibrationSet().deriveAerosolTE();
      //   event.getTableView().refresh(); // load new values
      // }
    });

    // Unit change propagates to all FxQuantity instances in this column
    unitCombo.valueProperty().addListener((obs, oldU, newU) -> {
      if (newU == null) {
        return; // should not happen as we cannot create new values and the box is always filled
      }

      // traverse all items in the column, i.e., in every row, get the column's item
      for (ResponseTableRow row : col.getTableView().getItems()) {
        FxQuantity q = extractor.apply(row);
        if (q != null) {
          q.getUnitProperty().set(newU);
        }
      }
    });

    return col;
  }

  private static Node quantityHeader(
      String title,
      ComboBox<ConvertibleUnit> unitCombo,
      @Nullable ToggleButton toggle
  ) {
    Label label = new Label(title);
    label.setStyle("-fx-font-weight: bold;");

    HBox unitBox;
    if (toggle != null) {
      unitBox = new HBox(2, unitCombo, toggle);
    } else {
      unitBox = new HBox(2, unitCombo);
    }

    unitBox.setAlignment(Pos.CENTER);

    VBox mainBox = new VBox(2, label, unitBox);
    mainBox.setAlignment(Pos.CENTER);

    return mainBox;
  }


  /// Static column builder methods
  public static TableColumn<ResponseTableRow, Number> numericColumn(
      String title,
      Function<ResponseTableRow, FxQuantity> extractor,
      ToggleButton toggleButton,
      double width) {
    TableColumn<ResponseTableRow, Number> col =
        new TableColumn<>();

    col.setPrefWidth(width);

    col.setGraphic(numericHeader(title, toggleButton));

    col.setCellValueFactory(cd -> {
      FxQuantity q = extractor.apply(cd.getValue());
      return q == null ? null : q.getValueProperty();
    });


    col.setCellFactory(tc -> new TextFieldTableCell<>(
        new StringConverter<>() {

          @Override
          public String toString(Number value) {
            return value == null ? "" : SnF.doubleToString(value.doubleValue(), NF.D1C2, NF.D1C2Exp);
          }

          @Override
          public Number fromString(String text) {
            if (text == null || text.isBlank()) {
              return null;
            }
            try {
              return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
              return null; // handled on commit
            }
          }
        }
    ));

    col.setOnEditCommit(event -> {
      if (event.getNewValue() == null) {
        event.getTableView().refresh(); // just reload old value(s)
        return;
      }

      ResponseTableRow row = event.getRowValue();
      FxQuantity q = extractor.apply(row);

      if (q != null && event.getNewValue() != null) {
        q.getValueProperty().set(event.getNewValue().doubleValue());
      }
    });

    return col;
  }

  private static Node numericHeader(
      String title,
      ToggleButton toggle
  ) {
    Label label = new Label(title);
    label.setStyle("-fx-font-weight: bold;");

    VBox box;
    if (toggle != null) {
      box = new VBox(label, toggle);
    } else {
      box = new VBox(label);
    }

    box.setSpacing(2);
    box.setAlignment(Pos.CENTER);

    return box;
  }


}