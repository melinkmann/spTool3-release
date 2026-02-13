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

import gui.util.TextFieldUtils;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;

import java.util.concurrent.atomic.AtomicReference;

public class CharacterFxParameter extends AbstractFxParameter<Character> implements
    FxParameter<Character> {

  private final TextField field;

  public CharacterFxParameter(Parameter<Character> plain) {
    super(plain);

    // Make Field.
    field = new TextField(plain.getValue().toString());
    field.setTextFormatter(TextFieldUtils.assureCharacter(plain.getValueAsString()));

    // Tooltip
    super.addToolTip(field);

    // Change Listener.
    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    super.slowPause.setOnFinished(event -> {
      if (field.editableProperty().get() && slowPauseNewValue.get() != null && !slowPauseNewValue.get().isEmpty()) {
        // First set.
        super.plainParameter.setValue(slowPauseNewValue.get().charAt(0));
        // Then update Children.
        super.notifyValueChange();
      }

    });
    field.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          super.slowPause.stop();
          slowPauseNewValue.set(newValue);
          super.slowPause.playFromStart();
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
    // Exclude empty field or else field.getText().charAt(0) -> index out of range!
    if (!field.getText().isEmpty()) {
      if (plainParameter.getValue() != field.getText().charAt(0)) {
        field.setText(super.plainParameter.getValue().toString());
      }
    }
  }


  @Override
  public Control getValueNode() {
    return field;
  }

  @Override
  public void setUneditable() {
    field.setEditable(false);
    super.setUneditable();
  }

}
