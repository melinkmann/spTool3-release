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

package gui.dialog;

import gui.Hotkeyable;
import gui.StageFactory;
import gui.util.UiUtil;

import java.io.Serializable;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import processing.parameters.FxParameter;
import processing.parameters.Parameter;

public class SingleParameterDialog<T extends Serializable> extends Dialog<T> implements Hotkeyable {

  protected static final double PREF_WIDTH = 250;
  protected static final double PREF_HEIGHT = 150;

  protected final BorderPane mainBorderPane;

  protected final Parameter<T> doubleParameter;
  protected final FxParameter<T> doubleFxParameter;
  private final String explanationText;

  protected final ButtonBar buttonBar;
  protected final Button primaryButton;
  protected final Button cancelButton;

  public SingleParameterDialog(String explanationText, Parameter<T> parameter) {
    this.doubleParameter = parameter;
    this.doubleFxParameter = parameter.getObservableInstance();
    this.explanationText = explanationText;

    this.mainBorderPane = new BorderPane();

    // Buttons
    this.buttonBar = new ButtonBar();
    this.primaryButton = new Button("OK");
    this.cancelButton = new Button("Close");
    this.buttonBar.getButtons().addAll(primaryButton, cancelButton);
    this.cancelButton.setOnAction(e -> cancelClose());
    this.primaryButton.setOnAction(e -> closeAndContinue());

    // Styling
    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    dialogPane.getButtonTypes().clear();

    dialogPane.setPadding(Insets.EMPTY);

    dialogPane.setContent(UiUtil.putOnAnchorWithInsets(mainBorderPane));

    activateHotkeys(dialogPane.getScene());

    DialogUtil.makeEscapeClosable(this);

    // Close on Windows close symbol
    dialogPane.getScene().getWindow().setOnCloseRequest(event -> cancelClose());

    setTitle("Enter value.");
    Stage dialogStage = (Stage) getDialogPane().getScene().getWindow();
    dialogStage.getIcons().add(UiUtil.getImage("/img/20240418_symbol.png"));
    dialogStage.setResizable(true);

    VBox centerBox = new VBox(5);
    centerBox.setAlignment(Pos.CENTER_RIGHT);

    Label explanationLabel = new Label(explanationText);
    explanationLabel.setFont(new Font(12));
    explanationLabel.setMinHeight(30);

    Node valueNode = doubleFxParameter.getValueNode();
    centerBox.getChildren().addAll(valueNode, explanationLabel);

    // continue when pressing enter
    centerBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER) {
        closeAndContinue();
        event.consume();
      }
    });

    mainBorderPane.setCenter(UiUtil.putOnAnchorWithInsets(centerBox));
    mainBorderPane.setBottom(buttonBar);

    Label top = (Label) doubleFxParameter.getLabelNode();
    top.setStyle("-fx-font-weight: bold");
    top.setFont(new Font(12));
    top.setMinHeight(30);
    BorderPane.setAlignment(top, Pos.CENTER);
    mainBorderPane.setTop(top);

  }

  @Override
  public void activateHotkeys(Scene scene) {
    scene.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
      // Save on control s
      if (StageFactory.KEY_CTL_ENTER.match(event)) {
        closeAndContinue();
      }
    });
  }

  public void closeAndContinue() {
    super.setResult(doubleParameter.getValue());
    killScene();
  }

  public void cancelClose() {
    super.setResult(null);
    killScene();
  }

  private void killScene() {
    Scene scene = getDialogPane().getScene();
    if (scene != null) {
      Stage dialogStage = (Stage) scene.getWindow();
      dialogStage.close();
    }
  }
}
