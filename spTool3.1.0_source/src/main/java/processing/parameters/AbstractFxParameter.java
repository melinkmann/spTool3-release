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
import gui.util.GlobalFields;
import gui.util.UiUtil;

import java.io.Serializable;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javax.annotation.Nullable;

import processing.parameterSets.FxMethod;
import processing.parameterSets.FxParamSet;


public abstract class AbstractFxParameter<T extends Serializable> implements FxParameter<T> {

  protected final Parameter<T> plainParameter;
  protected FxParamSet parentSet;

  private final HBox viewerBox = new HBox();

  // When parameters are label/comment of a method, we need to notify about changes.
  protected FxMethod method;

  private boolean isUneditable = false;

  protected final PauseTransition slowPause = new PauseTransition(Duration.seconds(0.2));

  // highlighting
  private final Rectangle rectangleHighlight;

  // allow highlighting in the UI when changing things. this is temporary and not saved
  private BooleanProperty isFxHighlight = new SimpleBooleanProperty(false);

  public AbstractFxParameter(Parameter<T> plainParameter) {
    this.plainParameter = plainParameter;
    this.parentSet = null;
    this.method = null;

    // Box for the editor views
    this.viewerBox.setSpacing(2);
    this.viewerBox.setAlignment(Pos.CENTER_LEFT);
    this.viewerBox.setPrefWidth(200); // sizing only works when this is smaller than label+option

    // highlighting rectangle
    rectangleHighlight = new Rectangle(25, 10);
    rectangleHighlight.setFill(Color.BLACK);
  }

  @Override
  public void forceUpdateExternally() {
    // Do nothing
  }

  @Override
  public HBox getViewerBox(ListView<?> paramView) {
    double width = paramView.getWidth();
    if (viewerBox.getChildren().isEmpty()) {
      // I think we have to do everything step by step to avoid flickering of the UI

      Label label = getLabelNode();
      if (plainParameter.isChild() && !label.getText().isEmpty()) {
        Pane arrows = UiUtil.getIndentArrowPane(plainParameter.getChildLevel());
        label.setGraphic(arrows);
      }

      HBox valueBox = new HBox(2);
      valueBox.setAlignment(Pos.CENTER_LEFT);

      Node valueNode = getValueNode();
      valueNode.setStyle("-fx-text-fill: black; ");

      Node decoration = getDecoration();

      if (decoration != null) {
        valueBox.getChildren().addAll(valueNode, decoration);
      } else {
        valueBox.getChildren().addAll(valueNode);
      }

      viewerBox.getChildren().addAll(label, valueBox);
      updateLabelAndItemSize(label, valueBox, width);

      // Listen to change of list view size
      paramView.widthProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                            Number newValue) {
          updateLabelAndItemSize(label, valueBox, newValue.doubleValue());
        }
      });
    }

    return viewerBox;
  }


  public void clearViewerBox() {
    viewerBox.getChildren().clear();
  }

  private static void updateLabelAndItemSize(Control label, Pane value, double listViewWidth) {
    double sum = GlobalFields.FX_LABEL_WIDTH + GlobalFields.FX_ITEM_WIDTH;
    double difference;
    if (listViewWidth > sum) {
      difference = listViewWidth - sum;
    } else {
      difference = 0d;
    }
    double labelSize = GlobalFields.FX_LABEL_WIDTH + difference * 0.10;
    double valueSize = GlobalFields.FX_ITEM_WIDTH + difference * 0.90;
    UiUtil.prefSize(GlobalFields.FX_ITEM_HEIGHT, labelSize, label);
    UiUtil.prefSize(GlobalFields.FX_ITEM_HEIGHT, valueSize, value);
  }

  @Override
  public Label getLabelNode() {
    String labelString = plainParameter.getLabel();
    Label lbl;
    if (labelString.isEmpty()) {
      lbl = new Label("");
    } else {
      lbl = new Label(plainParameter.getLabel() + ":");
    }

    // Make bold when a) is not a child or b) has more than 1 child itself
    if (plainParameter.isChild() && plainParameter.getAllChildrenFirstGen().size() < 2) {
      lbl.setStyle("-fx-text-fill: black; ");
    } else {
      lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: black; ");
    }

    return lbl;
  }

  @Nullable
  @Override
  public Node getDecoration() {
    // e.g., the calculator symbol on the TextFields
    Decoration<T> decoration = getPlainParameter().getDecoration();
    if (decoration != null) {
      Node decorationControl = decoration.getControl(this);
      decoration.setControlFxParameter(this);
      return decorationControl;
    }
    return null;
  }

  @Override
  public void setDecoration(Decoration<T> decoration) {
    decoration.setControlFxParameter(this);
  }

  protected void addToolTip(Control node) {
    if (!plainParameter.getExplanation().isEmpty()) {
      String tooltip = plainParameter.getExplanation() + ".";
      UiUtil.tooltip(node, tooltip);
    }
  }

  @Override
  public void setListeningFxSet(FxParamSet parentSet) {
    this.parentSet = parentSet;
  }

  @Override
  public void setListeningMethod(FxMethod method) {
    this.method = method;
  }


  /*
  Note: When item change occurs, we do not need to also trigger a contentChange.
  This is redundant and will only result in unnecessary calls in the viewer instance.
   */
  @Override
  public void notifyItemChange() {
    if (parentSet != null) {
      parentSet.notifyItemChange();
    }
  }

  /*
  Note: see above (cf itemChange): Only call when no item change occurs anyway, i.e.,
  all other instance except for Boolean and Enum.
   */
  @Override
  public void notifyValueChange() {
    if (parentSet != null) {
      parentSet.notifyValueChange();
    }
    if (method != null) {
      method.notifyValueChange();
    }
  }

  @Override
  public void notifyLabelChange() {
    if (parentSet != null) {
      parentSet.notifyLabelChange();
    }
  }

  public Parameter<T> getPlainParameter() {
    return plainParameter;
  }

  @Override
  public void setUneditable() {
    isUneditable = true;
  }

  @Override
  public boolean isUneditable() {
    return isUneditable;
  }

  @Override
  public boolean isHighlightFX() {
    return isFxHighlight.get();
  }

  @Override
  public void setHighlightFX(boolean isFxHighlight) {
    notifyItemChange();
    this.isFxHighlight.set(isFxHighlight);
  }

  /// /////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean isEqualWrappedObject(FxWrapper that) {
    boolean isEqual = false;
    if (that instanceof FxParameter<?>) {
      isEqual = this.getPlainParameter().equals(((FxParameter<?>) that).getPlainParameter());
    }
    return isEqual;
  }


}
