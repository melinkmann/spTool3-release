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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.util.Duration;

public class BooleanFxParameter extends AbstractFxParameter<Boolean> implements
    FxParameter<Boolean> {

  private final CheckBox box;
  private final PauseTransition pause = new PauseTransition(Duration.millis(200));

  public BooleanFxParameter(Parameter<Boolean> plain) {
    super(plain);

    // Make Box.
    box = new CheckBox(plain.getSecondaryLabel());
    box.setSelected(super.plainParameter.getValue());

    // Tooltip
    super.addToolTip(box);

    // Change Listener.
    // Update children at end of pause
    pause.setOnFinished(e -> {
      BooleanFxParameter.super.notifyItemChange();
    });

    // Add the change Listener.
    box.selectedProperty().addListener((obsvblValue, oldV, newV) -> {
      pause.stop();

      // Programmatically revert the decision
      if (isUneditable()) {
        loadFromPlainWithFormat();
      } else if (newV != null) {
        // Set value first since children depend on the value.
        BooleanFxParameter.super.plainParameter.setValue(newV);
        // Update Children:
        pause.playFromStart();
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
    // prevent endless loop and stackoverflow
    if (plainParameter.getValue() != box.isSelected()) {
      box.setSelected(plainParameter.getValue());
    }
  }

  @Override
  public Control getValueNode() {
    return box;
  }


}
