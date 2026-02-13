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

package gui;

import core.SpTool3Main;
import gui.dialog.notification.PopupFactory;
import gui.util.UiUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.uiParams.UiLayoutParameters;
import processing.parameterSets.uiParams.ViewerControllerFx;

import javax.annotation.Nullable;

public class PlotPopup {

  private static final Logger LOGGER = LogManager.getLogger(PlotPopup.class);

  private final String title;
  private final BorderPane outsideMainViewerPane;
  private final BorderPane insideGraphPane;
  private final ToggleButton toggle;
  @Nullable
  private final ViewerControllerFx fxController;

  private final Stage stage;
  private final BorderPane popupRoot;
  private final Scene scene;

  private boolean isShowing;
  private boolean isFirstTimeShowing;

  private double lastX = Double.NaN;
  private double lastY = Double.NaN;
  private double lastW = 800;
  private double lastH = 600;

  public PlotPopup(String title,
                   BorderPane outsideMainViewerPane,
                   BorderPane insideGraphPane,
                   ToggleButton toggle,
                   @Nullable ViewerControllerFx fxController) {

    this.title = title;

    this.outsideMainViewerPane = outsideMainViewerPane;
    this.insideGraphPane = insideGraphPane;
    this.toggle = toggle;
    this.fxController = fxController;

    // create stage
    stage = new Stage();
    stage.setTitle("spTool " + SpTool3Main.VERSION_ID);
    stage.initStyle(StageStyle.DECORATED); // No OS title bar: UTILITY
    try {
      stage.getIcons().add(StageFactory.getSymbol());
    } catch (IllegalArgumentException fe) {
      LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
    }

    Scene owner = SpTool3Main.getMainStage().getScene();
    stage.initStyle(StageStyle.DECORATED); // ensures win top bar
    stage.initModality(Modality.NONE); // NONE: parent does not freeze
    // Dont put as parent: then we do not get them as symbols in the task bar
    // Yet, if not parent, they dont close when closing main...
     stage.initOwner(owner.getWindow()); // makes mainStage parent

    // create scene
    popupRoot = new BorderPane();
    scene = new Scene(UiUtil.putOnAnchorWithoutInsets(popupRoot));
    UiUtil.formatPopup(popupRoot);
    stage.setScene(scene);
    stage.setTitle(title);

    // manage update behavior
    isShowing = false;

    // manage docking
    final Button closeBtn = new Button("Dock");
    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().add(closeBtn);
    buttonBar.setPadding(new Insets(3, 0, 3, 0));
    popupRoot.setBottom(buttonBar);


    closeBtn.setOnAction(event -> {
      hide();
    });

    stage.setOnCloseRequest(e -> {
      hide();
    });

    // Read and keep old size
    UiLayoutParameters layoutPar = SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters();
    if (SpTool3Main.getRunTime().getConfParams().getLoadDockingSizes().getValue()) {
      if (layoutPar != null) {
        double[] sizes = layoutPar.getPlotPopupPositionsXYWH(title);
        if (sizes.length == 4) {
          lastX = sizes[0];
          lastY = sizes[1];
          lastW = sizes[2];
          lastH = sizes[3];
          isFirstTimeShowing = false;
          LOGGER.trace("Successfully loaded previous dimensions.");
          if (UiUtil.isStageOffScreen(stage)){
            isFirstTimeShowing = true;
            LOGGER.trace("Previous dimensions are offscreen. Default dimensions will be used.");
          }
        } else {
          isFirstTimeShowing = true;
          LOGGER.trace("Previous dimension data incomplete. Cannot load.");
        }
      }
    } else {
      isFirstTimeShowing = true;
      LOGGER.trace("Did not load previous dimension data according to the settings in the configuration.");
    }


  }

  @Nullable
  public ViewerControllerFx getFxController() {
    return fxController;
  }

  public void notifyPaneSelected() {
    if (isShowing && fxController != null) {
      fxController.notifyPaneSelected();
    }
  }

  public ToggleButton getToggle() {
    return toggle;
  }

  public boolean isShowing() {
    return isShowing;
  }

  public void show() {
    isShowing = true;

    // Detach pane from its current parent (if any)
    if (insideGraphPane.getParent() instanceof Pane) {
      Pane parent = (Pane) insideGraphPane.getParent();
      parent.getChildren().remove(insideGraphPane);
    }

    // attach graph pane to popup
    popupRoot.setCenter(insideGraphPane);

    outsideMainViewerPane.setCenter(new AnchorPane());
    outsideMainViewerPane.setLeft(new AnchorPane());

    // show the stage
    stage.show();

    // Keep old size
    if (!isFirstTimeShowing) {
      stage.setX(lastX);
      stage.setY(lastY);
      stage.setWidth(lastW);
      stage.setHeight(lastH);
    } else {
      isFirstTimeShowing = false;
      // Initial size
      stage.setWidth(800);
      stage.setHeight(600);
      // stage.sizeToScene(); // does not work well for initial size

      // Must be called AFTER show() and AFTER sizing
      stage.centerOnScreen();
    }
  }

  public void hide() {
    saveBounds();

    isShowing = false;

    // Detach pane from its parent in the popup
    popupRoot.setCenter(null);

    // add to new parent
    outsideMainViewerPane.setCenter(insideGraphPane);
    // match by selecting the toggle
    toggle.setSelected(true);

    // hide the stage
    stage.hide();
  }

  private void saveBounds() {
    if (!stage.isMaximized()) {
      lastX = stage.getX();
      lastY = stage.getY();
      lastW = stage.getWidth();
      lastH = stage.getHeight();

      UiLayoutParameters layoutPar = SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters();
      if (layoutPar != null) {
        layoutPar.setPlotPopupPositionsXYWH(title, new double[]{lastX, lastY, lastW, lastH});
      }
    }
  }





}
