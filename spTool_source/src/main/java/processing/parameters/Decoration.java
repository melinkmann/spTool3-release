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

import java.io.Serializable;
import javafx.scene.Node;

public interface Decoration<T extends Serializable> {

  Node getControl(FxParameter<T> fxParameter);

  /*
   Essentially, this is a mixed class between FX and plain.
   In the future this may be changed but at the moment, we do not store a pointer to the plain
   that would be saved (I guess). So we can store an FX item in it.
   At the moment, this is primarily to get access to the isEditable information.
   */
  void setControlFxParameter(FxParameter<?> controlFxParameter);

}
