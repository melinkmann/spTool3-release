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

import gui.util.GlobalFields;
import gui.util.TextFieldUtils;
import javafx.scene.control.Control;
import javafx.scene.control.TextArea;

import java.util.concurrent.atomic.AtomicReference;

public class CommentFxParameter extends AbstractFxParameter<String> implements
    FxParameter<String> {

  private final TextArea area;

  public CommentFxParameter(Parameter<String> plain) {
    super(plain);

    // Make Field.
    area = new TextArea(plainParameter.getValue());
    // Prevent special characters that may confuse the XML file???
    area.setTextFormatter(TextFieldUtils.allPass());
    // default value of the formatter is "" --> set value afterwards
    area.setText(plainParameter.getValue());
    area.setEditable(true);
    area.setMinHeight(70);
    area.setPrefHeight(GlobalFields.FX_COMMENT_HEIGHT);

    // Tooltip
    super.addToolTip(area);

    // Change Listener.
    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    super.slowPause.setOnFinished(event -> {
      // Set value first since children depend on the value; ALLOW EMPTY STRINGS HERE!!!
      if (area.editableProperty().get() && slowPauseNewValue.get() != null) {
        super.plainParameter.setValue(slowPauseNewValue.get());
        // Notify listening classes.
        super.notifyValueChange();
      }
    });
    area.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          slowPause.stop();
          slowPauseNewValue.set(newValue);
          slowPause.playFromStart();
        }
    );

    // By re-loading when leaving, we prevent empty Label Fields
    area.focusedProperty().addListener((observable, oldValue, newValue) -> {
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
    if (!plainParameter.getValue().equals(area.getText())) {
      area.setText(plainParameter.getValue());
    }
  }


  @Override
  public Control getValueNode() {
    return area;
  }


  @Override
  public void setUneditable() {
    super.setUneditable();
    area.setEditable(false);
  }
}