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

import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Separator;

public class SeparatorFxParameter extends AbstractFxParameter<String> implements FxParameter<String> {

  private final Separator separator;

  // Initial value is passed inside the formatter!
  public SeparatorFxParameter(SeparatorParameter separatorParameter) {
    super(separatorParameter);
    this.separator = new Separator(Orientation.HORIZONTAL);
  }

  @Override
  public void forceUpdateExternally() {
    // Do nothing
  }

  @Override
  public Control getValueNode() {
    return separator;
  }

  @Override
  public void setUneditable() {
    super.setUneditable();
  }
}
