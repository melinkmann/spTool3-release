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


import gui.util.UiUtil;

import java.io.Serial;
import java.io.Serializable;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import util.Functional;

public class ButtonDecoration<T extends Serializable> implements Decoration<T>, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final String tooltip;
  private final String resource;
  private final Functional buttonFunction;

  public ButtonDecoration(String tooltip, String resource, Functional buttonFunction) {
    this.tooltip = tooltip;
    this.resource = resource;
    this.buttonFunction = buttonFunction;
  }

  public void setControlFxParameter(FxParameter<?> controlFxParameter) {
    // Do nothing.
  }

  @Override
  public Node getControl(FxParameter<T> fxParameter) {
    Button btn = UiUtil.getDecorationButton(resource, tooltip);

    btn.setOnAction(e -> {
      // needed for the PTOE popup
      Scene scene = btn.getScene();
      Stage parent = null;
      if (scene != null) {
        parent = (Stage) scene.getWindow();
      }

      if (parent != null) {
        buttonFunction.proceed(parent);
      } else {
        buttonFunction.proceed();
      }
    });
    return btn;
  }


}
