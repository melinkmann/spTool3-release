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

package gui.util;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextField;
import util.Functional;
import util.SnF;

public class NumberIterator {

  private final Property<String> currentIdxProperty;
  private final int itemsShowing;
  private int stepSize;
  private int finalIdx;
  private Functional onChange;

  private boolean reachedMaximum;
  private boolean reachedMinimum;

  public NumberIterator(TextField valueField,
                        int initialIndex,
                        int itemsShowing,
                        int finalIdx) {
    this.currentIdxProperty = new SimpleStringProperty(SnF.intToString(initialIndex));
    this.itemsShowing = itemsShowing;
    this.stepSize = itemsShowing - 1;
    this.finalIdx = finalIdx;
    this.onChange = Functional.empty();
    //
    valueField.setTextFormatter(TextFieldUtils.assurePositiveInteger(initialIndex));
    valueField.textProperty().bindBidirectional(this.currentIdxProperty);
    this.currentIdxProperty.addListener((observable, oldValue, newValue) -> onChange.proceed());
    //
    reachedMinimum = initialIndex == 0;
    reachedMaximum = initialIndex == finalIdx;
  }

  public void increment() {
    reachedMinimum = false;
    int step = Math.max(1, stepSize);
    int currentIndex = getCurrentIdx();

    if (reachedMaximum) {
      currentIndex = 0;
      reachedMaximum = false;
      reachedMinimum = true;
    } else if (currentIndex + step >= finalIdx) {
      currentIndex = finalIdx;
      reachedMaximum = true;
    } else {
      currentIndex += step;
    }

    currentIdxProperty.setValue(Integer.toString(currentIndex));
  }

  public void decrement() {
    reachedMaximum = false;
    int step = Math.max(1, stepSize);
    int currentIndex = getCurrentIdx();

    if (reachedMinimum) {
      currentIndex = (finalIdx / step) * step;
      reachedMinimum = false;
      reachedMaximum = true;
    } else if (currentIndex - step <= 0) {
      currentIndex = 0;
      reachedMinimum = true;
    } else {
      currentIndex -= step;
    }

    currentIdxProperty.setValue(Integer.toString(currentIndex));
  }

  public int getCurrentIdx() {
    return Math.max(SnF.strToInt(currentIdxProperty.getValue()), 0);
  }

  public int getCurrentEndIdx() {
    return Math.min(getCurrentIdx() + stepSize, finalIdx);
  }

  public boolean hasValue() {
    return !currentIdxProperty.getValue().isEmpty();
  }

  public int getStepSize() {
    return stepSize;
  }

  public void setStepSize(int stepSize) {
    this.stepSize = stepSize;
  }

  public void setFinalIdx(int finalIdx) {
    this.finalIdx = finalIdx;
  }

  public void resetCurrentIndex() {
    this.currentIdxProperty.setValue(Integer.toString(0));
    this.reachedMinimum = true;
  }

  public void setOnChange(Functional onChange) {
    this.onChange = onChange;
    this.currentIdxProperty.addListener((observable, oldValue, newValue) -> onChange.proceed());

  }
}
