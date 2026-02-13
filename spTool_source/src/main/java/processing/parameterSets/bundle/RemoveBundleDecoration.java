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

package processing.parameterSets.bundle;


import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import java.io.Serial;
import java.io.Serializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import processing.parameterSets.ParamBundle;
import processing.parameters.Decoration;
import processing.parameters.FxParameter;

public class RemoveBundleDecoration<T extends Serializable> implements Decoration<T>, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final String resource = "/img/removeSmall.png";
  private final String tooltip = "Remove this option";
  private final ParamBundle bundle;
  private FxParameter<?> controlFxParameter = null;

  public RemoveBundleDecoration(ParamBundle bundle) {
    this.bundle = bundle;
  }

  public void setControlFxParameter(FxParameter<?> controlFxParameter) {
    this.controlFxParameter = controlFxParameter;
  }

  @Override
  public Node getControl(FxParameter<T> fxParameter) {
    Button btn = UiUtil.getDecorationButton(resource, tooltip);

    btn.setOnAction(e -> {
      // Default: assume we can change
      boolean isUneditable = false;

      // check if there may be other information
      if (controlFxParameter != null) {
        isUneditable = controlFxParameter.isUneditable();
      }

      if (!isUneditable) {
        NotificationFactory.openYesNo("Remove option?",
            () -> {
              bundle.requestRemoveSelf();
              fxParameter.notifyItemChange();
            });
      }
    });
    return btn;
  }
}
