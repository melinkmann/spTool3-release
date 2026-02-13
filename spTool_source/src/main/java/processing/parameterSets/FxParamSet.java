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

package processing.parameterSets;

import gui.ParameterView;
import gui.dialog.EditableLabel;
import gui.dialog.FxEntry;
import gui.dialog.ListableColor;
import gui.dialog.ListableDate;
import gui.listAndSearch.FxWrapper;
import java.util.List;
import processing.parameters.FxParameter;

public interface FxParamSet extends FxWrapper, EditableLabel, ListableColor, ListableDate {

  // For the parameter of plots, we do not need the label, comment, ...
  List<FxParameter<?>> getActiveFxParametersWithoutMeta();

  List<FxParameter<?>> getActiveFxParameters();

  List<FxParameter<?>> getAllFxParameters();

  void notifyItemChange();

  void notifyValueChange();

  void notifyLabelChange();

  void setController(ParameterView controller);

  void setListeningEntry(FxEntry<FxParamSet> fxEntry);

  ParamSet getPlainSet();

  void setUneditable();

  boolean isEqualPlainObject(FxParamSet fxParamSet);

}
