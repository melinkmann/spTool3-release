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
import gui.util.UiString;
import java.io.Serializable;

import javafx.animation.PauseTransition;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class ComboEnumFxParameter<T extends Serializable> extends AbstractFxParameter<T> implements
    FxParameter<T> {

  private final PauseTransition pause = new PauseTransition(Duration.millis(200));
  private final ComboBox<T> box;
  private final T[] options;

  public ComboEnumFxParameter(Parameter<T> plain, T[] options) {
    super(plain);
    this.options = options;

    // Make Box.
    box = new ComboBox<T>();
    box.setPrefSize(GlobalFields.FX_ITEM_WIDTH, GlobalFields.FX_ITEM_HEIGHT);
    box.getItems().addAll(options);
    box.getSelectionModel().select(super.plainParameter.getValue());

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

    // Tooltip
    super.addToolTip(box);

    // Change Listener.
    // Update children at end of pause
    pause.setOnFinished(e -> {
      ComboEnumFxParameter.super.notifyItemChange();
    });

    box.getSelectionModel().selectedItemProperty().addListener(
        (observableValue, oldValue, newVal) -> {
          pause.stop();
          if (newVal != null) {
            if (!isUneditable()) {
              // Set value first since children depend on the value.
              ComboEnumFxParameter.super.plainParameter.setValue(newVal);
              // Update Children:
              pause.playFromStart();
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
    if (plainParameter.getValue() != box.getSelectionModel().getSelectedItem()) {
      box.getSelectionModel().select(plainParameter.getValue());
    }
  }


  @Override
  public Control getValueNode() {
    return box;
  }


  @Override
  public void setUneditable() {
    super.setUneditable();
    // Remove options to show only one item, i.e., prevent change.
    box.getItems().clear();
    box.getItems().add(plainParameter.getValue());
    box.getSelectionModel().select(plainParameter.getValue());
  }

}