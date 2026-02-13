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
import java.io.Serializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import math.transform.Conversion;

public class ConversionButtonDecoration<T extends Serializable> implements Decoration<T> {

  private final String resource;
  private final String tooltip;
  private final Conversion<T> conversion;


  public ConversionButtonDecoration(String resource, String tooltip, Conversion<T> conversion) {
    this.resource = resource;
    this.tooltip = tooltip;
    this.conversion = conversion;
  }


  public void setControlFxParameter(FxParameter<?> controlFxParameter) {
    // Do nothing.
  }

  @Override
  public Node getControl(FxParameter<T> fxParameter) {
    Button calcBtn = UiUtil.getDecorationButton(resource, tooltip);

    calcBtn.setOnAction(e -> {
//
//      Optional<T> result = conversion.openDialog(fxParameter.getPlainParameter().getValue());
//      result.ifPresent(t -> {
//        fxParameter.getPlainParameter().setValueWithFormat(t);
//        fxParameter.externalUpdate();
//      });
    });
    return calcBtn;
  }

}
