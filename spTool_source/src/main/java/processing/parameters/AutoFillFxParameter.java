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

import gui.AutoCompleteTextField;
import gui.dialog.Fillable;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.PauseTransition;
import javafx.scene.control.Control;
import javafx.scene.control.TextFormatter;
import javafx.util.Duration;

public class AutoFillFxParameter<T extends Serializable>
    extends AbstractFxParameter<Fillable<T>>
    implements FxParameter<Fillable<T>> {

  private final SortedSet<Fillable<T>> suggestions;

  private final AutoCompleteTextField<T> field;

  // slightly increase duration
  protected final PauseTransition slowPause = new PauseTransition(Duration.seconds(0.5));

  public AutoFillFxParameter(AutoFillParam<T> plain, TextFormatter<String> formatter) {
    super(plain);

    suggestions = new TreeSet<>(plain.getSuggestions().getItems());

    // Make Field.
    field = new AutoCompleteTextField<T>(super.plainParameter.getValue(), suggestions);
    field.setTextFormatter(formatter);

    // Tooltip
    super.addToolTip(field);

    // Change Listener.
    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    slowPause.setOnFinished(event -> {
      // Set LABEL value only if: editable, value not empty!
      if (field.editableProperty().get() && slowPauseNewValue.get() != null && !slowPauseNewValue.get().isEmpty()) {

        Fillable<T> match = plain.getSuggestions().getMatch(field.getText(), true);
        super.plainParameter.setValue(match);

        // Make red if not matching with element
        boolean isMatch = suggestions.stream()
            .anyMatch(e -> e.getStringValue().equals(field.getText()));

        if (!isMatch) {
          field.setStyle("-fx-control-inner-background: red");
        } else {
          field.setStyle("-fx-control-inner-background: white");
              /*
               Update Children if value is valid
               Note: Set value first since children depend on the value inside the PLAIN parameter.
               */
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

    // By re-loading when leaving, we prevent empty Label Fields
    field.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        loadFromPlainWithFormat();
      }
    });
  }


  @Override
  public void forceUpdateExternally() {
    // Safely load from plain parameter with checks (see method loadFromPlain()).
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
    Fillable<T> item = plainParameter.getValue();
    String stringValueOfItem = item.getStringValue();

    if (!stringValueOfItem.equals(field.getText())) {
      field.setText(super.plainParameter.getValue().getStringValue());
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