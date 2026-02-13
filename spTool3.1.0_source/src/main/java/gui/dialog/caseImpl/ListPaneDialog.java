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

package gui.dialog.caseImpl;

import gui.Hotkeyable;
import gui.StageFactory;
import gui.dialog.DialogUtil;
import gui.util.UiUtil;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;

public class ListPaneDialog extends Dialog<Boolean> implements Hotkeyable {

  protected static final double PREF_WIDTH = 300;
  protected static final double PREF_HEIGHT = 250;

  protected final BorderPane mainBorderPane;
  private final BorderPane listContentPanePane;

  private final List<Node> nodeList;
  private int currentIndex = 0;

  protected final ButtonBar buttonBar;
  protected final Button primaryButton;
  protected final Button cancelButton;
  private final Button backButton;

  public ListPaneDialog(List<Node> nodeList) {
    this.nodeList = nodeList;

    this.mainBorderPane = new BorderPane();
    this.listContentPanePane = new BorderPane();

    // Buttons
    this.buttonBar = new ButtonBar();
    this.backButton = new Button("Back");
    this.cancelButton = new Button("Cancel");
    this.primaryButton = new Button("Next");
    this.cancelButton.setOnAction(e -> cancelClose());
    this.buttonBar.getButtons().addAll(cancelButton);
    if (nodeList.size() > 1) {
      buttonBar.getButtons().addAll(backButton);
    }
    this.buttonBar.getButtons().addAll(primaryButton);

    // just one
    if (nodeList.size() == 1) {
      primaryButton.setText("Finish");
    }

    primaryButton.setOnAction(e -> {
      // check exit condition first
      if (currentIndex == nodeList.size() - 1) {
        closeAndContinue();
      } else {
        currentIndex++;
        currentIndex = Math.min(nodeList.size() - 1, currentIndex);
        currentIndex = Math.max(0, currentIndex);
        if (currentIndex == nodeList.size() - 1) {
          primaryButton.setText("Finish");
        } else {
          primaryButton.setText("Next");
        }
        if (!nodeList.isEmpty()) {
          listContentPanePane.setCenter(UiUtil.putOnAnchorWithoutInsets(nodeList.get(currentIndex)));
        }
      }
    });

    backButton.setOnAction(e -> {
      currentIndex--;
      currentIndex = Math.min(nodeList.size() - 1, currentIndex);
      currentIndex = Math.max(0, currentIndex);
      if (currentIndex == nodeList.size() - 1) {
        primaryButton.setText("Finish");
      } else {
        primaryButton.setText("Next");
      }
      if (!nodeList.isEmpty()) {
        listContentPanePane.setCenter(UiUtil.putOnAnchorWithoutInsets(nodeList.get(currentIndex)));
      }
    });


    // Styling
    setTitle("Wizard.");
    Stage dialogStage = (Stage) getDialogPane().getScene().getWindow();
    dialogStage.getIcons().add(UiUtil.getImage("/img/20240418_symbol.png"));
    dialogStage.setResizable(true);

    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    dialogPane.getButtonTypes().clear();
    dialogPane.setPadding(Insets.EMPTY);
    dialogPane.setContent(UiUtil.putOnAnchorWithInsets(mainBorderPane));

    activateHotkeys(dialogPane.getScene());
    DialogUtil.makeEscapeClosable(this);

    // Close on Windows close symbol
    dialogPane.getScene().getWindow().setOnCloseRequest(event -> cancelClose());

    // Fill
    if (!nodeList.isEmpty()) {
      listContentPanePane.setCenter(UiUtil.putOnAnchorWithoutInsets(nodeList.get(0)));
    }

    mainBorderPane.setCenter(UiUtil.putOnAnchorWithInsets(listContentPanePane));
    mainBorderPane.setBottom(buttonBar);
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
    super.setResult(true);
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
