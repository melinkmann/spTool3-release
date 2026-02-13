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
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public final class ResponseTableModel {

  private final ObservableList<ResponseTableRow> rows =
      FXCollections.observableArrayList();

  public ObservableList<ResponseTableRow> getRows() {
    return rows;
  }

  public void fill(SpCalibrationSet quant) {
    fill(quant, quant.listIsotopes());
  }


  public void fill(
      SpCalibrationSet quant,
      List<Isotope> isotopes) {

    rows.clear();

    for (Isotope iso : isotopes) {
      ResponseTableRow row = new ResponseTableRow(iso);

      row.setIonicResponse(quant.getIonicResponse(iso));
      row.setNpResponse(quant.getNpResponse(iso));
      row.setAerosolTEPct(quant.getAerosolTE(iso));
      row.setParticleNumberTEPct(quant.getParticleNumberTE(iso));

      rows.add(row);
    }
  }

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
      FxQuantity q = extractor.apply(row);

      if (q != null && event.getNewValue() != null) {
        q.getValueProperty().set(event.getNewValue().doubleValue());
      }
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
            return value == null ? "" : SnF.doubleToString(value.doubleValue(), NF.D1C1, NF.D1C2Exp);
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