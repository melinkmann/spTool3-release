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

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;

public class NoFieldTextFxParameter extends AbstractFxParameter<String> implements
    FxParameter<String> {

  private final Label labelField;

  public NoFieldTextFxParameter(Parameter<String> plain, TextFormatter<String> formatter) {
    super(plain);

    // Make Field.
    formatter.setValue(super.plainParameter.getValue());
    labelField = new Label(formatter.getValue());
    labelField.setWrapText(true);
    labelField.setStyle("-fx-text-fill: black; ");

    // Tooltip
    super.addToolTip(labelField);
  }

  // So far, the only reason for an update would be a Date that is changed externally.
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
    if (!plainParameter.getValue().equals(labelField.getText())) {
      labelField.setText(super.plainParameter.getValue());
    }
  }

  @Override
  public Control getValueNode() {
    return labelField;
  }

  @Override
  public void setUneditable() {
    // Always uneditable.
    super.setUneditable();
  }

}