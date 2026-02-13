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
import gui.listAndSearch.FxWrapper;
import java.util.List;
import processing.parameters.FxParameter;
import processing.parameters.LabelFxParameter;

public class FxListMethod implements FxMethod {

  private final Method plainMethod;
  private final LabelFxParameter fxLabel;
  private final FxParameter<String> fxComment;
  private final FxParameter<String> fxDate;
  private ParameterView methodViewer = null;


  public FxListMethod(Method plainMethod) {
    this.plainMethod = plainMethod;
    this.fxLabel = (LabelFxParameter) plainMethod.getLabelParam().getObservableInstance();
    this.fxComment = plainMethod.getCommentParam().getObservableInstance();
    this.fxDate = plainMethod.getDateParam().getObservableInstance();
    // Allow the UI to show "method was changed" when label or comment change
    fxLabel.setListeningMethod(this);
    fxComment.setListeningMethod(this);
  }

  public LabelFxParameter getFxLabel() {
    return fxLabel;
  }

  public FxParameter<String> getFxComment() {
    return fxComment;
  }

  public FxParameter<String> getFxDate() {
    return fxDate;
  }

  @Override
  public Method getPlainMethod() {
    return plainMethod;
  }

  @Override
  public void addSetAndNotify(ParamSet set) {
    plainMethod.addSet(set);
    if (methodViewer != null) {
      methodViewer.notifyItemChange();
    }
  }

  @Override
  public void addSetsAndNotify(List<ParamSet> sets) {
    plainMethod.addSets(sets);
    if (methodViewer != null) {
      methodViewer.notifyItemChange();
    }
  }

  @Override
  public String getLabel() {
    return fxLabel.getPlainParameter().getValue();
  }

  @Override
  public void setLabel(String label) {
    this.fxLabel.getPlainParameter().setValue(label);
  }

  public void setMethodViewer(ParameterView methodViewer) {
    this.methodViewer = methodViewer;
  }

  @Override
  public void notifyValueChange() {
    if (methodViewer != null) {
      methodViewer.notifyValueChange();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean isEqualWrappedObject(FxWrapper that) {
    boolean isEqual = false;
    if (that instanceof FxListMethod) {
      isEqual = this.getPlainMethod().equals(((FxListMethod) that).getPlainMethod());
    }
    return isEqual;
  }

  @Override
  public void setFavorite(boolean isFavourite) {
    plainMethod.setFavorite(isFavourite);
  }

  @Override
  public boolean isFavorite() {
    return plainMethod.isFavorite();
  }


}
