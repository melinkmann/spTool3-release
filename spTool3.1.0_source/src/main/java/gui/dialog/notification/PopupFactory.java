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
import gui.StageFactory;
import gui.util.UiUtil;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.HashMap;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import javax.annotation.Nullable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.uiParams.ViewerControllerFx;

public class PopupFactory {

  private static final Logger LOGGER = LogManager.getLogger(PopupFactory.class);

  public static Popup showOnPopup(Control control) {
    final Popup popup = new Popup();
    // Make the popup close when clicking outside
    popup.setAutoHide(false);
    popup.getScene().getWindow().setWidth(350);
    popup.getScene().getWindow().setHeight(300);

    final Button closeBtn = new Button("OK");
    closeBtn.setOnAction(event -> popup.hide());

    final VBox box = new VBox(2);
    box.getChildren().addAll(control, closeBtn);
    popup.getContent().add(box);
    UiUtil.formatPopup(box);

    // Get current mouse position using java.awt.MouseInfo
    Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
    double mouseX = mouseLocation.getX();
    double mouseY = mouseLocation.getY();

    if (!popup.isShowing()) {
      // Show popup near the mouse location -> // Adjust offset as needed
      popup.show(SpTool3Main.getMainStage(), mouseX, mouseY - 350);
    } else {
      popup.setX(mouseX);
      popup.setY(mouseY - 350);
    }
    return popup;
  }

  public static Popup showOnPopup(Node control) {
    final Popup popup = new Popup();
    // Make the popup close when clicking outside
    popup.setAutoHide(false);
    popup.getScene().getWindow().setWidth(350);
    popup.getScene().getWindow().setHeight(300);

    final Button closeBtn = new Button("Close");
    closeBtn.setOnAction(event -> popup.hide());

    final VBox box = new VBox(2);
    box.getChildren().addAll(control, closeBtn);
    popup.getContent().add(box);
    UiUtil.formatPopup(box);

    // Get current mouse position using java.awt.MouseInfo
    Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
    double mouseX = mouseLocation.getX();
    double mouseY = mouseLocation.getY();

    if (!popup.isShowing()) {
      // Show popup near the mouse location -> // Adjust offset as needed
      popup.show(SpTool3Main.getMainStage(), mouseX, mouseY - 350);
    } else {
      popup.setX(mouseX);
      popup.setY(mouseY - 350);
    }
    return popup;
  }

  public static Stage showOnWindow(Node control) {
    return showOnWindow(control, null, null);
  }

  public static Stage showOnWindow(Node control, @Nullable Scene owner, @Nullable Double width) {
    Stage stage = new Stage(); // Create a new window (Stage)

    BorderPane mainPane = new BorderPane(control);
    Scene scene = new Scene(UiUtil.putOnAnchorWithoutInsets(mainPane));

    UiUtil.formatPopup(mainPane);

    final Button closeBtn = new Button("Close");
    closeBtn.setOnAction(event -> stage.close());
    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().add(closeBtn);
    buttonBar.setPadding(new Insets(3, 0, 3, 0));
    mainPane.setBottom(buttonBar);

    stage.setTitle("spTool " + SpTool3Main.VERSION_ID);
    stage.initStyle(StageStyle.DECORATED); // No OS title bar: UTILITY
    try {
      stage.getIcons().add(StageFactory.getSymbol());
    } catch (IllegalArgumentException fe) {
      LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
    }
    if (owner == null) {
      owner = SpTool3Main.getMainStage().getScene();
    }
    stage.initOwner(owner.getWindow()); // makes mainStage parent
    stage.initModality(Modality.NONE); // NONE: parent does not freeze
    stage.setScene(scene);

    if (width != null) {
      mainPane.setPrefWidth(width);
    }

    // Automatically size to fit content
    stage.sizeToScene();

    stage.showAndWait();

    return stage;
  }

  public static void openPaneInPopup(String title, Pane pane, BorderPane targetBorderPane,
                                     HashMap<ToggleButton, ViewerControllerFx> popupMap,
                                     ToggleButton button) {
    // Detach pane from its current parent (if any)
    if (pane.getParent() instanceof Pane) {
      Pane parent = (Pane) pane.getParent();
      parent.getChildren().remove(pane);
    }

    Stage stage = new Stage();
    BorderPane popupRoot = new BorderPane();
    popupRoot.setCenter(pane);

    Scene scene = new Scene(UiUtil.putOnAnchorWithoutInsets(popupRoot));
    UiUtil.formatPopup(popupRoot);

    final Button closeBtn = new Button("Dock");
    closeBtn.setOnAction(event -> {
      // Remove from popup and place back into target BorderPane
      popupRoot.setCenter(null);
      targetBorderPane.setCenter(pane);
      popupMap.remove(button);
      stage.close();
      button.setSelected(true);
    });

    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().add(closeBtn);
    buttonBar.setPadding(new Insets(3, 0, 3, 0));
    popupRoot.setBottom(buttonBar);

    stage.setTitle("spTool " + SpTool3Main.VERSION_ID);
    stage.initStyle(StageStyle.DECORATED); // No OS title bar: UTILITY
    try {
      stage.getIcons().add(StageFactory.getSymbol());
    } catch (IllegalArgumentException fe) {
      LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
    }

    Scene owner = SpTool3Main.getMainStage().getScene();
    stage.initOwner(owner.getWindow()); // makes mainStage parent
    stage.initModality(Modality.NONE); // NONE: parent does not freeze
    stage.setScene(scene);

    stage.setScene(scene);
    stage.setTitle(title);
    stage.setOnCloseRequest((WindowEvent event) -> {
      // Remove from popup and place back into target BorderPane
      popupRoot.setCenter(null);
      targetBorderPane.setCenter(pane);
      popupMap.remove(button);
      // button.setSelected(true);  // rather only on minimize click
    });

    // Automatically size to fit content
    //
    if (pane.getChildren().isEmpty()) {
      stage.setWidth(800);
      stage.setHeight(600);
    } else {
      stage.sizeToScene();
    }

    // bad: when selecting sth else, this one is closed, too.
    //    button.selectedProperty().addListener(new ChangeListener<Boolean>() {
    //      @Override
    //      public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
    //        stage.close();
    //      }
    //    });

    stage.show();
  }

  // Similar but less styled
  //  /**
//   * Shows a resizable popup window containing the given AnchorPane.
//   *
//   * @param ownerScene The current scene (used to position the popup relative to its window)
//   * @param content    The AnchorPane containing the UI to display
//   */
//  public static Scene showResizablePopup(Scene ownerScene, AnchorPane content) {
//    // Create a new Stage (popup window)
//    Stage popupStage = new Stage();
//    popupStage.initModality(Modality.NONE); // Non-blocking
//    popupStage.setResizable(true);          // Make it resizable
//    popupStage.setTitle("Viewer");
//
//    try {
//      popupStage.getIcons().add(StageFactory.getSymbol());
//    } catch (IllegalArgumentException fe) {
//      LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
//    }
//
//    // Wrap the AnchorPane in a Scene
//    Scene popupScene = new Scene(content);
//
//    // Set the scene to the popup stage
//    popupStage.setScene(popupScene);
//
//    // Position the popup relative to the current window
//    Window ownerWindow = ownerScene.getWindow();
//    popupStage.setX(ownerWindow.getX() + 25);
//    popupStage.setY(ownerWindow.getY() + 25);
//
//    popupStage.setWidth(1200);
//    popupStage.setHeight(800);
//
//    // Show the popup
//    popupStage.show();
//
//    //
//    return popupStage.getScene();
//  }

}
