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

import gui.listAndSearch.FxWrapper;

import java.io.Serializable;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import javax.annotation.Nullable;

import processing.parameterSets.FxMethod;
import processing.parameterSets.FxParamSet;

public interface FxParameter<T extends Serializable> extends FxWrapper {

  /**
   * Forces the parameter to transfer its value to the UI element, usually when the value (IN THE
   * PLAIN INSTANCE) has been changed externally. Note: Earlier, each update forced a new call to
   * the getObservableInstance() method of the plain parameter. This is disadvantageous as it makes
   * the connection between FX instance and plain instance very hard to keep track of. i.e.: We
   * don't re-instantiate the parameters anymore. Thus, we have to update, e.g., when defaults are
   * restored.
   */

  void forceUpdateExternally();

  /*
  NOTE: Do not allow value changes with a setter. This may end up in weird discussions whether
  the setter should trigger even if the field is set uneditable. If you really want to update,
  you should change the plain instance and reload via externalUpdate();
   */

  /**
   * Tell a parameter that a change has occurred, usually within a listener.
   */
  void notifyItemChange();

  /**
   * Tell a parameter that a change has occurred, usually within a listener.
   */
  void notifyValueChange();

  /**
   * Special case: The labelParameter is a special parameter, as it is also represented in a
   * ListView.
   */
  void notifyLabelChange();

  HBox getViewerBox(ListView<?> paramView);

  // Limited width, outside of a listview
  void clearViewerBox();

  Label getLabelNode();

  @Nullable
  Node getDecoration();

  void setDecoration(Decoration<T> decoration);

  public Node getValueNode();

  public boolean isHighlightFX();

  public void setHighlightFX(boolean isFxHighlight);

  Parameter<T> getPlainParameter();

  /**
   * Essentially the parent set, i.e., the parameter set that contains the parameter. It has to be
   * notified when, e.g., a boolean parameter is selected and now reveals sub-options.
   */
  void setListeningFxSet(FxParamSet parentSet);

  void setListeningMethod(FxMethod method);

  void setUneditable();

  boolean isUneditable();
}
