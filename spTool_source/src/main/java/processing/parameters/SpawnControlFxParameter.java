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
import javafx.scene.control.Button;
import javafx.scene.control.Control;

public class SpawnControlFxParameter extends AbstractFxParameter<Integer> implements
    FxParameter<Integer> {

  private final Button showMoreBtn;

  public SpawnControlFxParameter(SpawnControlParameter plain) {
    super(plain);

    // Make Box.
    showMoreBtn = UiUtil.getToolbarBtn("/img/create.png", "Add option");

    // Tooltip
    super.addToolTip(showMoreBtn);

    // Change Listener.
    showMoreBtn.setOnAction(e -> {
      // Do not fire if the instance is uneditable.
      if (!isUneditable()) {
        plain.setValue(plain.getValue() + 1);
        super.notifyItemChange();
      }
    });
  }


  @Override
  public void forceUpdateExternally() {
  }

  @Override
  public Control getValueNode() {
    return showMoreBtn;
  }

}
