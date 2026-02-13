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
import java.util.stream.Collectors;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.paint.Color;
import visualizer.styles.Colors;

public class ColorFxParameter extends AbstractFxParameter<String> implements
    FxParameter<String> {

  private final ColorPicker colorPicker;

  public ColorFxParameter(ColorParameter plain) {
    super(plain);

    // Make Box.
    colorPicker = new ColorPicker();
    colorPicker.setPrefSize(GlobalFields.FX_ITEM_WIDTH, GlobalFields.FX_ITEM_HEIGHT);
    colorPicker.getCustomColors().clear();
    colorPicker.getCustomColors().addAll(Colors.getDefaultColors().stream()
    .map(Colors::getFX).collect(Collectors.toList()));

    // try to parse
    colorPicker.setValue(Colors.rgbFromXmlToColor(super.plainParameter.getValue()));

    // Tooltip
    super.addToolTip(colorPicker);

    // Change Listener.
    colorPicker.valueProperty().addListener(new ChangeListener<Color>() {
      @Override
      public void changed(ObservableValue<? extends Color> observable, Color oldValue,
          Color newValue) {
        if (newValue != null && newValue != oldValue) {
          ColorFxParameter.super.plainParameter.setValue(Colors.colorToRgbForXML(newValue));
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
    String rgb = Colors.colorToRgbForXML(colorPicker.getValue());
    if (plainParameter.getValue() != rgb) {
      colorPicker.setValue(Colors.rgbFromXmlToColor(super.plainParameter.getValue()));
    }
  }


  @Override
  public Control getValueNode() {
    return colorPicker;
  }


  @Override
  public void setUneditable() {
    super.setUneditable();
    // Remove options to show only one item, i.e., prevent change.
    colorPicker.setDisable(true);
  }

}