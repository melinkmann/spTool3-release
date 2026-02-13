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

import javafx.scene.control.Button;
import javafx.scene.control.Control;

public class ButtonFxParameter extends AbstractFxParameter<String> implements FxParameter<String> {

  private final Button button;

  // Initial value is passed inside the formatter!
  public ButtonFxParameter(ButtonParameter buttonParameter) {
    super(buttonParameter);
    this.button = buttonParameter.getButtonSupplier().get();

    // Works in principle, but in the ListView, we override the style and the formatting is lost...
    button.setStyle("-fx-font-weight: bold");
  }

  @Override
  public void forceUpdateExternally() {
    // Do nothing
  }

  @Override
  public Control getValueNode() {
    return button;
  }

  @Override
  public void setUneditable() {
    super.setUneditable();
    button.setDisable(true);
  }
}
