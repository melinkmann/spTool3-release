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
import gui.util.GlobalFields;

import java.net.URL;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import gui.util.UiUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import math.transform.UiTransform;
import sandbox.montecarlo.Isotope;
import visualizer.styles.Colors;
import visualizer.styles.CustomColorPicker;

public class ColorFxParameter extends AbstractFxParameter<String> implements
    FxParameter<String> {

  Button pickerButton;

  public ColorFxParameter(ColorParameter plain) {
    super(plain);

    Colors currentColor = plain.getColor();
    Rectangle colorRect = new Rectangle(27, 18);
    colorRect.setFill(currentColor.getFX());

    URL resource = UiUtil.class.getResource("/img/pickColor.png");

    if (resource != null) {
      // Image image = new Image(resource.toString());
      // ImageView imageView = new ImageView(image);
      // imageView.setFitHeight(18);
      // imageView.setFitWidth(27);
      // imageView.setSmooth(true);
      // pickerButton = new Button("Open color picker", new HBox(imageView, colorRect));
      pickerButton = new Button("Open color picker",colorRect);
    } else {
      pickerButton = new Button("Color");
    }
    pickerButton.setPrefWidth(90);
    pickerButton.setMinWidth(10);
    UiUtil.tooltip(pickerButton, "Open color picker.");


    pickerButton.setOnAction(e -> {
      Colors oldColor = plain.getColor();
      CustomColorPicker custom = new CustomColorPicker(
          oldColor.getFX(),
          Colors.getDefaultColors(),
          new Consumer<Color>() {
            @Override
            public void accept(Color color) {
              if (color != null) {
                ColorFxParameter.super.plainParameter.setValue(Colors.colorToRgbForXML(color));
                colorRect.setFill(color);
              }
            }
          });
      custom.show();
    });


    // Tooltip
    super.addToolTip(pickerButton);
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
    // nada
  }


  @Override
  public Control getValueNode() {
    return pickerButton;
  }


  @Override
  public void setUneditable() {
    super.setUneditable();
    // Remove options to show only one item, i.e., prevent change.
    pickerButton.setDisable(true);
  }

}