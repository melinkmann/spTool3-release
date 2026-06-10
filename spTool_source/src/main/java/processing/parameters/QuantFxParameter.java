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

package processing.parameters;

import core.SpTool3Main;
import gui.util.GlobalFields;
import gui.util.UiString;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import math.units.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.NF;
import util.SnF;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class QuantFxParameter<T extends Enum<T> & Unit> extends AbstractFxParameter<Double> implements FxParameter<Double> {

  private static final Logger LOGGER = LogManager.getLogger(QuantFxParameter.class);

  private final TextField field;
  private final NF format;
  private final AtomicBoolean isFocused = new AtomicBoolean(false);

  private final ComboBox<T> box;
  private final T[] options;
  // box needs plain parameter
  private final QuantParameter<T> plain;

  // Initial value is passed inside the formatter!
  public QuantFxParameter(QuantParameter<T> plain, NF format, TextFormatter<Double> formatter,
                          T[] options) {
    super(plain);
    this.format = format;

    // Make Field.
    field = new TextField();
    field.setTextFormatter(formatter);

    // Tooltip
    super.addToolTip(field);

    // Load data from plot
    field.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        if (event.isControlDown()) {
          double x = SpTool3Main.getRunTime().getXPosition().get();
          LOGGER.trace("Loading x data: " + x + ".");
          plain.setValue(x);
          loadFromPlainWithFormat();
        } else if (event.isShiftDown()) {
          double y = SpTool3Main.getRunTime().getYPosition().get();
          LOGGER.trace("Loading y data: " + y + ".");
          plain.setValue(y);
          loadFromPlainWithFormat();
        }
      }
    });

    // Change Listener.
    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    super.slowPause.setOnFinished(event -> {
      if (field.editableProperty().get() && slowPauseNewValue.get() != null && SnF
          .isValidDoubleSilent(slowPauseNewValue.get())) {
              /*
               ONLY if we are in focus, write.
               If we are not in focus, we have just left the focus and the standardmain pattern formatted
               value was written into the field. Thus, do not act upon this "reset" action.
               */

        if (isFocused.get()) {
          // Set value first since children depend on the value.

          // double value = SnF.strToDouble(newValue, super.plainParameter.getValue());
          double value = formatter.getValueConverter().fromString(slowPauseNewValue.get());
          super.plainParameter.setValue(value);
          // Update Children.
          super.notifyValueChange();
        }

      }
    });
    field.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          slowPause.stop();
          slowPauseNewValue.set(newValue);
          slowPause.playFromStart();
        }
    );

    // Force the format onto the field
    field.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        isFocused.set(true);
      } else {
        isFocused.set(false);
      }
      loadFromPlainWithFormat();
    });

    // COMBOBOX
    this.plain = plain;
    this.options = options;
    box = new ComboBox<T>();
    // box.setPrefSize(GlobalFields.FX_ITEM_WIDTH, GlobalFields.FX_ITEM_HEIGHT);
    box.getItems().addAll(options);
    box.getSelectionModel().select(plain.getUnit());
    // some min width
    box.setMinWidth(90);

    // To String methods
    box.setConverter(new StringConverter<T>() {
      @Override
      public String toString(T object) {
        String value = "N/A";
        // Is null when clear() is called during setUneditable()
        if (object != null) {
          value = object.toString();
          if (object instanceof UiString) {
            value = ((UiString) object).getUiString();
          }
        }
        return value;
      }

      @Override
      public T fromString(String string) {
        return box.getSelectionModel().getSelectedItem();
      }
    });

    // Change Listener.
    box.getSelectionModel().selectedItemProperty().addListener(
        (observableValue, oldValue, newVal) -> {
          if (newVal != null) {
            if (!isUneditable()) {
              // Set value first since children depend on the value.
              plain.setUnit(newVal);
              // Update Children.
              super.notifyItemChange();
            }
          }
        });
  }


  @Override
  public void forceUpdateExternally() {
    loadFromPlainWithFormat();
  }

  /**
   * This will force an update even if parameter is set uneditable. Uneditable only refers to the
   * possibility to access the field in the UI. If e.g., resetToDefaults etc., is called on the
   * plain instance, we do want to be allowed to load and see this here!
   * <p> Note: The equality check here serves to avoid endless looping by firing listeners -> Make
   * sure the new value is not equal to current. (Disclaimer: I am unsure if this is always
   * necessary as most listeners should listen to CHANGES anyway, but having a break here seems
   * reasonable):
   */
  private void loadFromPlainWithFormat() {

    // Also here, enforce format
    if (isFocused.get()) {
      String currentFieldValue = field.getText();
      String valueInPlain = SnF.doubleFldToString(plainParameter.getValue(), NF.getMax(format));

//      System.out.println("\nisFocused: currentFieldValue=" + currentFieldValue);
//      System.out.println("isFocused: valueInPlain=" + valueInPlain);

      if (!valueInPlain.equals(currentFieldValue)) {
        String value = SnF.doubleFldToString(super.plainParameter.getValue(), NF.getMax(format));
//        System.out.println("if!_1: value=" + value);
        field.setText(value);
      }
    } else {
      String currentFieldValue = field.getText();
      String valueInPlain = SnF.doubleFldToString(plainParameter.getValue(), format);

//      System.out.println("\nelse: currentFieldValue=" + currentFieldValue);
//      System.out.println("else: valueInPlain=" + valueInPlain);

      if (!valueInPlain.equals(currentFieldValue)) {
        String value = SnF.doubleFldToString(super.plainParameter.getValue(), format);
//        System.out.println("if!_2: value=" + value);
        field.setText(value);
      }
    }

    // ComboBox
    if (plain.getUnit() != box.getSelectionModel().getSelectedItem()) {
      box.getSelectionModel().select(plain.getUnit());
    }
  }


  @Override
  public Node getValueNode() {
//    return new HBox(
//        new VBox(new Label("Value"),field),
//        new VBox(new Label("Value"),new TextField("Option B")),
//        new VBox(new Label("Value"),new TextField("Option B"),
//        new Separator(Orientation.HORIZONTAL)));
    return new HBox(3, field, box); // do not add decoration here!
  }

  @Override
  public void setUneditable() {
    super.setUneditable();
    field.setEditable(false);
    // Remove options to show only one item, i.e., prevent change.
    box.getItems().clear();
    box.getItems().add(plain.getUnit());
    box.getSelectionModel().select(plain.getUnit());
  }
}
