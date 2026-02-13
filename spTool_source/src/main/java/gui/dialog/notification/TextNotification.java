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

package gui.dialog.notification;


import gui.StageFactory;
import gui.dialog.DialogUtil;
import gui.util.UiUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.Util;

/**
 * Dialog class cannot be fired while another Dialog is showing and waiting (showAndWait()). Hence,
 * Error messages should be fired as an independent popup scene that does not block/wait.
 */

public class TextNotification {

  // Text
  private static final double MAX_WIDTH = 1000;
  private static final double MIN_WIDTH = 350;
  private static final double MAX_HEIGHT = 650;
  private static final double MIN_HEIGHT = 200;

  public enum NoteType {
    NOTIFICATION {
      @Override
      public String toString() {
        return "Notification";
      }
    },
    AUTOCLOSE_NOTIFICATION {
      @Override
      public String toString() {
        return "Notification";
      }
    },
    ERROR {
      @Override
      public String toString() {
        return "Error";
      }
    }
  }

  private final String message;
  private final String labelString;
  private final boolean showAreaInitially;
  private final NoteType notificationType;

  public TextNotification(NoteType notificationType, String message) {
    this.message = message;
    this.labelString = null;
    this.showAreaInitially = true;
    this.notificationType = notificationType;
  }

  public TextNotification(NoteType notificationType, String message, String labelString) {
    this.message = message;
    this.labelString = labelString;
    this.showAreaInitially = false;
    this.notificationType = notificationType;
  }

  public void show() {
    // Create a new stage for the popup window
    Stage popupStage = new Stage();
    popupStage.getIcons().add(StageFactory.getSymbol());
    popupStage.initModality(Modality.NONE); // Block events to other windows
    popupStage.setTitle(notificationType.toString());
    popupStage.setResizable(true);

    // Create a BorderPane and add the TextArea to the center
    BorderPane borderPane = new BorderPane();
    borderPane.setPadding(new Insets(5));

    // Prepare scene
    Scene scene = new Scene(
        UiUtil.putOnAnchorWithoutInsets(borderPane)); // Set desired width and height

    // Prepare text
    TextArea text = new TextArea(message);

    // Create a label
    if (labelString != null) {
      final Label label = createContentLabel(labelString);
      label.setWrapText(true);
      final Button showText = new Button("Show");
      showText.setOnAction(e -> {
        borderPane.setCenter(UiUtil.putOnAnchorWithInsets(text));
        double[] wh = DialogUtil.estimateWindowWidthHeight(message);
        scene.getWindow().setWidth(Util.restrict(wh[0], MIN_WIDTH, MAX_WIDTH));
        scene.getWindow().setHeight(Util.restrict(wh[1], MIN_HEIGHT, MAX_HEIGHT));
        scene.getWindow().centerOnScreen();
      });
      HBox box = new HBox(10);
      if (notificationType.equals(NoteType.ERROR)) {
        box.getChildren().addAll(
            UiUtil.getViewer("/img/issue.png"),
            label,
            showText);
      } else {
        box.getChildren().addAll(label, showText);
      }
      box.setAlignment(Pos.CENTER_LEFT);
      borderPane.setTop(UiUtil.putOnAnchorWithInsets(box));
    }

    // OK Btn
    Button ok = new Button("OK");
    ok.setOnAction(e -> popupStage.close());

    HBox buttonBox = new HBox(ok);
    buttonBox.setAlignment(Pos.CENTER_RIGHT); // Align the button to the right
    buttonBox.setPadding(new Insets(5)); // Padding around the button box

    borderPane.setBottom(buttonBox);

    // Text

    // Create a TextArea and set the message
    if (showAreaInitially) {
      borderPane.setCenter(UiUtil.putOnAnchorWithInsets(text));
    }
    text.setWrapText(true); // Enable line wrapping
    text.setEditable(false); // Set to read-only
    text.setBackground(Background.EMPTY);
    text.setFocusTraversable(false);

    // Estimate size
    double[] wh = DialogUtil.estimateTextWidthHeight(message);
    text.setPrefSize(Util.restrict(wh[0], MIN_WIDTH, MAX_WIDTH),
        Util.restrict(wh[1], MIN_HEIGHT, MAX_HEIGHT));

    // Create a scene for the BorderPane and set it on the stage
    popupStage.setScene(scene);
    activateHotkeys(popupStage, scene);

    // Make disappear
    if (notificationType.equals(NoteType.AUTOCLOSE_NOTIFICATION)) {
      int seconds = 5;
      final int[] secondsLeft = {seconds};

      // Initial title
      popupStage.setTitle("Closing in " + secondsLeft[0] + "s");
      popupStage.show();

      Timeline timeline = new Timeline(
          new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft[0]--;

            if (secondsLeft[0] > 0) {
              popupStage.setTitle("Closing in " + secondsLeft[0] + "s");
            } else {
              popupStage.close();
            }
          })
      );

      timeline.setCycleCount(seconds);
      timeline.play();
    } else {
      // Show the popup window
      popupStage.show();
    }

  }

  static private Label createContentLabel(String text) {
    Label label = new Label(text);
    label.setMaxWidth(Double.MAX_VALUE);
    label.setMaxHeight(Double.MAX_VALUE);
    label.getStyleClass().add("content");
    label.setWrapText(true);
    label.setPrefWidth(360);
    label.setPrefWidth(Region.USE_COMPUTED_SIZE);
    return label;
  }

  public void activateHotkeys(Stage popupStage, Scene scene) {
    DialogUtil.makeEscapeClosable(scene);
    scene.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
      // Save on control s
      // If "continue" like Button is present and control enter is hit, call "close and continue"
      if (StageFactory.KEY_CTL_ENTER.match(event)) {
        popupStage.close();
      }
    });
  }

}
