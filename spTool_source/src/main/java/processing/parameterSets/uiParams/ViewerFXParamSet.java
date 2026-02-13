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

package processing.parameterSets.uiParams;

import gui.util.UiUtil;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameters.FxParameter;

public abstract class ViewerFXParamSet extends FxParamSetImpl implements ViewerControllerFx {

  protected BorderPane targetPane;
  /*
   Box and ScrollPane for the Parameters need to be initialized here.
   The refresher method must only set their children.
   Otherwise, if we create Box and Scroll new each time the content change fires,
   sliders cannot request focus after firing a change.
   */

  private final VBox setBox = new VBox(8);
  private final ScrollPane paramScroll = new ScrollPane(UiUtil.putOnAnchorWithoutInsets(setBox));

  public ViewerFXParamSet(ParamSet plainSet) {
    super(plainSet);
    this.targetPane = null;
  }

  public void setTargetPane(BorderPane targetPane) {
    this.targetPane = targetPane;
  }

  @Override
  public void externalUpdate() {
    refreshParameterView();
    refreshGraph();
  }

  @Override
  public void notifyPaneSelected() {
    if (targetPane != null) {
      // Is there a graph in the center? If no, that means that we must build one. Else keep it.
      if (targetPane.getCenter() == null) {
        refreshGraph();
      }
      // Are there parameters on the left? If no, that means that we must build one. Else keep it.
      if (targetPane.getLeft() == null) {
        refreshParameterView();
      }
    }
  }

  @Override
  public void notifyValueChange() {
    super.notifyValueChange();
    refreshParameterView();
    refreshGraph();
  }

  @Override
  public void notifyItemChange() {
    super.notifyItemChange();
    refreshParameterView();
    refreshGraph();
  }

  // Allow for customizable width of options
  public double getValueWidth() {
    return 90d;
  }


  @Override
  public void refreshParameterView() {

    final double hSpace = 2;
    final double labelWidth = 70;
    final double valueWidth = getValueWidth() + 5 - hSpace;
    // final double valueWidth = 90 + 5 - hSpace;
    final double paneWidth = valueWidth + 12;

    List<FxParameter<?>> sets = getActiveFxParametersWithoutMeta();
    sets.forEach(FxParameter::clearViewerBox); // I hope this is a good choice to include it here.
    if (targetPane != null) {

      setBox.getChildren().clear();

      UiUtil.makePaneBright(setBox);
      UiUtil.formatScrollPane(paramScroll);
      UiUtil.makePaneBrightAndRound(paramScroll);

      for (FxParameter<?> fxPar : sets) {
        HBox parBox = new HBox(hSpace);
        parBox.setAlignment(Pos.CENTER_LEFT);
        Label labelPar = fxPar.getLabelNode();
        Node valuePar = fxPar.getValueNode();
        // force certain width
        AnchorPane valuePane = new AnchorPane(valuePar);
        labelPar.setMinWidth(labelWidth);

        // Sliders return a stack pane (unfortunately)
        if (valuePar instanceof Control) {
          ((Control) valuePar).setPrefWidth(valueWidth);
        } else if (valuePar instanceof Pane) {
          ((Pane) valuePar).getChildren().stream()
              // Do make width of label wider!
              .filter(c -> c instanceof Slider)
              .forEach(c -> ((Control) c).setPrefWidth(valueWidth));
        }

        valuePane.setMinWidth(paneWidth);
        if (fxPar.getDecoration() != null) {
          parBox.getChildren().addAll(labelPar, valuePane, fxPar.getDecoration());
        } else {
          parBox.getChildren().addAll(labelPar, valuePane);
        }

        setBox.getChildren().add(parBox);
      }

      targetPane.setLeft(UiUtil.putOnAnchorWithInsets(paramScroll));
    }
  }


  @Override
  public void refreshGraph() {
    // Override in subclasses
  }

  @Override
  public void clearGraph() {
    targetPane.setCenter(null);
  }
}
