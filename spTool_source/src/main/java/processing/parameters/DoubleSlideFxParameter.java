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
import gui.util.TextFormatterOption;
import gui.util.TextFormatterSupplier;
import gui.util.UiUtil;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.util.StringConverter;
import util.NF;
import util.SnF;

public class DoubleSlideFxParameter extends AbstractFxParameter<Double> implements
    FxParameter<Double> {

  private final DoubleSlideParameter plainParameter;

  private final Slider slider;
  private final NF format;
  private final TextFormatterOption supplier;
  private final NF supplierFormat;

  private final Label valueLabel;
  private final TextField popupValueField;
  private final TextField popupIncrementField;
  private final TextField popupMinField;
  private final TextField popupMaxField;
  private final Popup popup;

  // Initial value is passed inside the formatter!
  public DoubleSlideFxParameter(DoubleSlideParameter plain, NF format, TextFormatterOption supplier,
                                double min, double max, double majTick, int minTicks, boolean snapToTicks,
                                double increment,
                                NF supplierFormat) {

    super(plain);
    this.plainParameter = plain;
    this.format = format;
    this.supplier = supplier;
    this.supplierFormat = supplierFormat;

    // Unfortunately, sliders do not have a value.
    this.valueLabel = new Label();
    this.valueLabel.setText(SnF.doubleToString(plain.getValue(), format));
    this.valueLabel.setMouseTransparent(true); // control slider below the label
    this.valueLabel.setStyle("-fx-font-weight: bold");

    // popup
    this.popupValueField = new TextField();
    this.popupValueField.setTextFormatter(TextFormatterSupplier.get(supplier, plain.getValue()));

    this.popupIncrementField = new TextField();
    popupIncrementField.setTextFormatter(TextFormatterSupplier.get(supplier, increment));

    this.popupMinField = new TextField();
    popupMinField.setTextFormatter(TextFormatterSupplier.get(supplier, min));

    this.popupMaxField = new TextField();
    popupMaxField.setTextFormatter(TextFormatterSupplier.get(supplier, max));

    this.popup = new Popup();

    // make a bit faster, but not too fast (at 0.1, we still get weird JavaFX null pointer)
    final PauseTransition slowPause = new PauseTransition(Duration.seconds(0.2));

    // Make Slider
    this.slider = new Slider(min, max, plain.getValue());
    this.slider.setMajorTickUnit(majTick);
    this.slider.setMinorTickCount(minTicks);
    this.slider.setSnapToTicks(snapToTicks);
    this.slider.setShowTickLabels(true);
    this.slider.setBlockIncrement(increment);

    this.slider.setContextMenu(new ContextMenu());

//    // prevent weird formatting! (This attempt does not fix it)
//    slider.maxProperty().addListener(new ChangeListener<Number>() {
//      @Override
//      public void changed(ObservableValue<? extends Number> observableValue, Number number,
//          Number t1) {
//        slider.setMajorTickUnit(SnF.strToDouble(popupMaxField.getText()));
//      }
//    });

    // Open the popup on right click
    slider.setOnContextMenuRequested(e -> {
      if (!isUneditable()) {
        popupValueField.setText(SnF.doubleToString(plain.getValue(), format));
        popupIncrementField.setText(SnF.doubleToString(plain.getIncrement(), supplierFormat));
        popupValueField.requestFocus();

        // Create Set and Cancel buttons
        Button setButton = new Button("Set");
        Button cancelButton = new Button("Cancel");

        // Handle Enter key press in the TextField
        popupValueField.setOnKeyPressed(event -> {
          if (event.getCode() == KeyCode.ENTER) {
            setButton.fire();
          }
          if (event.getCode() == KeyCode.ESCAPE) {
            cancelButton.fire();
          }
        });

        popupIncrementField.setOnKeyPressed(event -> {
          if (event.getCode() == KeyCode.ENTER) {
            setButton.fire();
          }
          if (event.getCode() == KeyCode.ESCAPE) {
            cancelButton.fire();
          }
        });

        popupMinField.setOnKeyPressed(event -> {
          if (event.getCode() == KeyCode.ENTER) {
            setButton.fire();
          }
          if (event.getCode() == KeyCode.ESCAPE) {
            cancelButton.fire();
          }
        });

        popupMaxField.setOnKeyPressed(event -> {
          if (event.getCode() == KeyCode.ENTER) {
            setButton.fire();
          }
          if (event.getCode() == KeyCode.ESCAPE) {
            cancelButton.fire();
          }
        });

        // Handle button actions
        setButton.setOnAction(e2 -> {
          slider.setValue(SnF.strToDouble(popupValueField.getText()));
          slider.setBlockIncrement(SnF.strToDouble(popupIncrementField.getText()));
          slider.setMin(SnF.strToDouble(popupMinField.getText()));
          slider.setMax(SnF.strToDouble(popupMaxField.getText()));
          slider.setMajorTickUnit(SnF.strToDouble(popupMaxField.getText()));
          plain.setValue(SnF.strToDouble(popupValueField.getText()));
          plain.setIncrement(SnF.strToDouble(popupIncrementField.getText()));
          plain.setMin(SnF.strToDouble(popupMinField.getText()));
          plain.setMax(SnF.strToDouble(popupMaxField.getText()));
          popup.hide();
        });
        cancelButton.setOnAction(e2 -> popup.hide());

        // Layout for the popup
        HBox buttonBox = new HBox(5, setButton, cancelButton);
        VBox popupContent = new VBox(10,
            new Label("Value"), popupValueField,
            new Label("Increment"), popupIncrementField,

            new Label("Min"), popupMinField,
            new Label("Max"), popupMaxField,
            buttonBox);
        //      popupContent.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-border-color:
        //      black; -fx-border-width: 1;");
        UiUtil.formatPopup(popupContent);

        // Add content to popup
        popup.getContent().add(popupContent);

        // Make the popup close when clicking outside
        popup.setAutoHide(true);
        popup.getScene().getWindow().setWidth(150);
        popup.getScene().getWindow().setHeight(100);

        if (!popup.isShowing()) {
          // Get current mouse position using java.awt.MouseInfo
          Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
          double mouseX = mouseLocation.getX();
          double mouseY = mouseLocation.getY();

          // Show popup near the mouse location -> // Adjust offset as needed
          popup.show(SpTool3Main.getMainStage(), mouseX + 10, mouseY + 10);
        }
      }
    });

    // number format
    slider.setLabelFormatter(new StringConverter<Double>() {
      @Override
      public String toString(Double object) {
        return SnF.doubleToString(object, format);
      }

      @Override
      public Double fromString(String string) {
        return SnF.strToDouble(string);
      }
    });

    // Tooltip
    super.addToolTip(slider);

    // Change Listener.
    AtomicReference<Number> slowPauseNewValue = new AtomicReference<>(0);
    slowPause.setOnFinished(event -> {
      if (!slider.isDisable() && slowPauseNewValue.get() != null) {
              /*
               ONLY if we are in focus, write.
               If we are not in focus, we have just left the focus and the standard pattern formatted
               value was written into the field. Thus, do not act upon this "reset" action.
               */

        // Set value first since children depend on the value.

        // double value = SnF.strToDouble(newValue, super.plainParameter.getValue());
        double value = slowPauseNewValue.get().doubleValue();
        super.plainParameter.setValue(value);
        valueLabel.setText(SnF.doubleToString(value, format));
        // Update Children.
        super.notifyValueChange();

      }
    });

    slider.valueProperty().addListener(
        (observable, oldValue, newValue) -> {
          slowPause.stop();
          slowPauseNewValue.set(newValue);
          slowPause.playFromStart();

          slider.requestFocus();
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
    slider.setValue(plainParameter.getValue());
    slider.setBlockIncrement(plainParameter.getIncrement());
  }


  @Override
  public Node getValueNode() {
    StackPane pane = new StackPane(slider, valueLabel);
    StackPane.setAlignment(valueLabel, Pos.BOTTOM_CENTER);
    return pane;
  }

  @Override
  public void setUneditable() {
    super.setUneditable();
    slider.setDisable(true);
  }
}
