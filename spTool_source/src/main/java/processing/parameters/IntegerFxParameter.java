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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.NF;
import util.SnF;

public class IntegerFxParameter extends AbstractFxParameter<Integer> implements
    FxParameter<Integer> {

  private static final Logger LOGGER = LogManager.getLogger(IntegerFxParameter.class);

  private final TextField field;
  private final NF format;

  private final AtomicBoolean isFocused = new AtomicBoolean(false);

  public IntegerFxParameter(Parameter<Integer> plain, NF format, TextFormatter<Integer> formatter) {
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
          plain.setValue((int) Math.round(x));
          loadFromPlainWithFormat();
        } else if (event.isShiftDown()) {
          double y = SpTool3Main.getRunTime().getYPosition().get();
          LOGGER.trace("Loading y data: " + y + ".");
          plain.setValue((int) Math.round(y));
          loadFromPlainWithFormat();
        }

      }
    });

    // Change Listener.
    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    super.slowPause.setOnFinished(event -> {
      if (field.editableProperty().get() && slowPauseNewValue.get() != null && SnF.isValidInt(slowPauseNewValue.get())) {
              /*
               ONLY if we are in focus, write.
               If we are not in focus, we have just left the focus and the standard pattern formatted
               value was written into the field. Thus, do not act upon this "reset" action.
               */

        if (isFocused.get()) {
          // Set value first since children depend on the value.
                /*
                There is a very weird bug with the NONZERO integer case.
                Apparently, when the user enters 0 or 1 and either hits enter or leaves focus,
                the formatter does return 1 in both cases (as it should for nonzeros) but
                the value somehow never gets pushed to the field itself. This is obvious,
                since the listener is not responding.
                However, if we enter a value greater than 1, and then enter zero,
                the zero will be replaced by a 1.
                Also, when you enter a number smaller than 100 and hit enter (many times),
                the listener will not trigger.
                If you enter a number larger than 100 and hit enter,
                the listener will trigger.
                Also, if you set the default nonzero alternative (if 0 is entered) to e.g., 1000
                (instead of 1), the listener will also trigger.
                If have no idea where this comes from honestly.
                My proposal is: use the formatter to simply format the number and use that value
                in the setter for the plain parameter. This should essentially return the same value
                as would have been anyway but it does not require the Converter to trigger
                the listener again to actually set the value.

                Previous code:
                int value = SnF.strToInt(newValue, super.plainParameter.getValue());
                 */

          int value = formatter.getValueConverter().fromString(slowPauseNewValue.get());

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
      String valueInPlain = SnF.intFldToString(plainParameter.getValue(), NF.getMax(format));

      if (!valueInPlain.equals(currentFieldValue)) {
        String value = SnF.intFldToString(super.plainParameter.getValue(), NF.getMax(format));
        field.setText(value);
      }

    } else {
      String currentFieldValue = field.getText();
      String valueInPlain = SnF.intFldToString(plainParameter.getValue(), format);
      if (!valueInPlain.equals(currentFieldValue)) {
        String value = SnF.intFldToString(super.plainParameter.getValue(), format);
        field.setText(value);
      }
    }
  }

  @Override
  public Control getValueNode() {
    return field;
  }

  @Override
  public void setUneditable() {
    super.setUneditable();
    field.setEditable(false);
  }

}