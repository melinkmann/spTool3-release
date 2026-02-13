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

import javafx.animation.PauseTransition;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicReference;

public class LabelFxParameter extends AbstractFxParameter<String> implements FxParameter<String> {

  private final TextField field;

  // slightly increase duration
  protected final PauseTransition slowPause = new PauseTransition(Duration.seconds(0.5));

  public LabelFxParameter(Parameter<String> plain, TextFormatter<String> formatter) {
    super(plain);

    // Make Field.
    field = new TextField(super.plainParameter.getValue());
    field.setTextFormatter(formatter);

    // Tooltip
    super.addToolTip(field);

    // Change Listener.
    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    slowPause.setOnFinished(event -> {

      // Set LABEL value only if: editable, value not empty!
      if (field.editableProperty().get() && slowPauseNewValue.get() != null && !slowPauseNewValue.get().isEmpty()) {

        super.plainParameter.setValue(slowPauseNewValue.get());

       /*
        Update Children.
        Note: Set value first since children depend on the value inside the PLAIN parameter.
        */
        super.notifyValueChange();
        // Also call label update for the linked listviews  ui items
        LabelFxParameter.super.notifyLabelChange();
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
        /*
          Update listening classes, i.e., the method view that has the label in its listview.
          By doing this when leaving focus, we are not interrupted while editing.
         */
        LabelFxParameter.super.notifyLabelChange();
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
    if (!plainParameter.getValue().equals(field.getText())) {
      field.setText(super.plainParameter.getValue());
    }
  }

  @Override
  public Control getValueNode() {
    /*
     LabelledParameter is virtually the only one that gets a change from an external source.
     The ListView is editable, hence the label may change.
     This getValueNode() method is called whenever the grid of parameters is repopulated.
     Hence, when this method is called, make sure that the field shows the right string value.
     */
    field.setText(super.plainParameter.getValue());
    return field;
  }


  @Override
  public void setUneditable() {
    super.setUneditable();
    field.setEditable(false);
  }


}