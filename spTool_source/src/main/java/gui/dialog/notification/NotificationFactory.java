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

import core.SpTool3Main;
import gui.dialog.DialogUtil;
import gui.dialog.notification.TextNotification.NoteType;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Functional;

public class NotificationFactory {

  private static final Logger LOGGER = LogManager.getLogger(NotificationFactory.class);

  public static void openYesCancel(String message, Functional yes) {
    openYesCancel(message, yes, Functional.empty());
  }

  public static void openYesCancel(String message, Functional yes, Functional cancel) {
    Platform.runLater(() -> {

      Alert alert = new Alert(
          AlertType.CONFIRMATION,
          message,
          ButtonType.YES,
          ButtonType.CANCEL);

      alert.setTitle("Confirmation required.");

      alert.setResizable(true);

      DialogUtil.makeEscapeClosable(alert);

      double[] wh = DialogUtil.estimateWindowWidthHeight(message);
      alert.setWidth(Math.max(400, wh[0]));
      alert.setHeight(Math.max(300, wh[1]));

      alert.initOwner(SpTool3Main.getMainStage().getScene().getWindow());

      //Deactivate Default behavior for yes-Button:
      Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
      yesButton.setDefaultButton(false);
      //Activate Default behavior for no-Button:
      Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
      noButton.setDefaultButton(true);

      yesButton.setText("Yes");  // force labels to stay correct
      noButton.setText("Cancel");

      //
      Optional<ButtonType> result = alert.showAndWait();

      result.ifPresent(bt -> {
        if (bt.getText().equalsIgnoreCase("yes")) {
          yes.proceed();
        } else {
          cancel.proceed();
        }
      });

    });
  }

  public static AtomicBoolean openYesNo(String message, Functional yes) {
    // empty "No"
    return openYesNo(message, yes, Functional.empty());
  }


  public static AtomicBoolean openYesNo(String message, Functional yes, Functional no) {
    AtomicBoolean trueIfYes = new AtomicBoolean(false);
    Alert alert = new Alert(
        AlertType.CONFIRMATION,
        message,
        ButtonType.YES,
        ButtonType.NO);
    alert.setTitle("Confirmation required.");

    alert.setResizable(true);

    DialogUtil.makeEscapeClosable(alert);

    double[] wh = DialogUtil.estimateWindowWidthHeight(message);
    alert.setWidth(Math.max(400, wh[0]));
    alert.setHeight(Math.max(300, wh[1]));

    alert.initOwner(SpTool3Main.getMainStage().getScene().getWindow());

    //Deactivate Default behavior for yes-Button:
    Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
    yesButton.setDefaultButton(false);
    //Activate Default behavior for no-Button:
    Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
    noButton.setDefaultButton(true);

    yesButton.setText("Yes");  // force labels to stay correct
    noButton.setText("No");

    //
    Optional<ButtonType> result = alert.showAndWait();

    result.ifPresent(bt -> {
      if (bt.getText().equalsIgnoreCase("yes")) {
        yes.proceed();
        trueIfYes.set(true);
      } else {
        no.proceed();
        trueIfYes.set(false);
      }
    });

    return trueIfYes;
  }

  public static AtomicBoolean openYesNo(String message) {
    // Empty yes  no
    return openYesNo(message, Functional.empty(), Functional.empty());
  }

  public static void openError(Exception exception) {
    Platform.runLater(() -> {
      StringBuilder builder = new StringBuilder();
      builder.append("Complete error message:").append("\n");
      Arrays.stream(exception.getStackTrace()).forEach(s -> builder.append(s).append("\n"));

      String context = builder.toString();

      TextNotification textNotification = new TextNotification(NoteType.ERROR, context,
          exception.getLocalizedMessage());
      textNotification.show();
    });
  }

  public static void openError(String message) {
    Platform.runLater(() -> {
      TextNotification textNotification = new TextNotification(NoteType.ERROR, message);
      textNotification.show();
    });
  }


  public static void openInfo(String message) {
    Platform.runLater(() -> {
      TextNotification textNotification = new TextNotification(NoteType.NOTIFICATION, message);
      textNotification.show();
    });
  }

  public static void openAutocloseInfo(String message) {
    Platform.runLater(() -> {
      TextNotification textNotification = new TextNotification(NoteType.AUTOCLOSE_NOTIFICATION, message);
      textNotification.show();
    });
  }

  public static void openOK(String message) {
    openOK(message, SpTool3Main.getMainStage().getScene().getWindow());
  }

  public static void openOK(String message, Window parentWindow) {
    Platform.runLater(() -> {
      Alert alert = new Alert(
          AlertType.INFORMATION,
          message,
          ButtonType.OK);
      alert.setTitle("Information");

      alert.setResizable(true);

      DialogUtil.makeEscapeClosable(alert);

      double[] wh = DialogUtil.estimateWindowWidthHeight(message);
      alert.setWidth(Math.max(400, wh[0]));
      alert.setHeight(Math.max(300, wh[1]));

      alert.initOwner(parentWindow);

      //Activate Default behavior for yes-Button:
      Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
      yesButton.setDefaultButton(true);

      //
      Optional<ButtonType> result = alert.showAndWait();
    });
  }


}
