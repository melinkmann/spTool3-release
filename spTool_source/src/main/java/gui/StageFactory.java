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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.ExporterParams;

public class StageFactory {

  private static final Logger LOGGER = LogManager.getLogger(StageFactory.class);

  // Key Combinations

  public static final KeyCombination KEY_CTL_S = new KeyCodeCombination(
      KeyCode.S, KeyCombination.CONTROL_DOWN);

  public static final KeyCombination KEY_CTL_ENTER = new KeyCodeCombination(
      KeyCode.ENTER, KeyCombination.CONTROL_DOWN);

  public static final KeyCombination KEY_SHIFT_ESCAPE = new KeyCodeCombination(
      KeyCode.ESCAPE, KeyCombination.SHIFT_DOWN);

  public static final KeyCombination KEY_CTL_UP = new KeyCodeCombination(
      KeyCode.UP, KeyCombination.CONTROL_DOWN);

  public static final KeyCombination KEY_CTL_DOWN = new KeyCodeCombination(
      KeyCode.DOWN, KeyCombination.CONTROL_DOWN);

  public static final KeyCombination KEY_CTL_LEFT = new KeyCodeCombination(
      KeyCode.LEFT, KeyCombination.CONTROL_DOWN);

  public static final KeyCombination KEY_CTL_RIGHT = new KeyCodeCombination(
      KeyCode.RIGHT, KeyCombination.CONTROL_DOWN);

  public static final KeyCombination KEY_CTL_Z = new KeyCodeCombination(
      KeyCode.Z, KeyCombination.CONTROL_DOWN);

  /*
   * Load gui main stage (create storage and settings files first!)
   */

  public static void createMainWindow(Stage mainStage) {

    final FXMLLoader loader = new FXMLLoader(
        StageFactory.class.getResource("/gui/mainWindow.fxml"));
    mainStage.setTitle("spTool " + SpTool3Main.VERSION_ID);
    mainStage.setMaximized(true);

    try {
      mainStage.getIcons().add(getSymbol());
    } catch (IllegalArgumentException fe) {
      LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
    }

    // FXMLLoader for the primaryStage
    try {
      Pane root = loader.load();
      MainWindowController controller = loader.getController();
      SpTool3Main.getRunTime().setMainWindowController(controller);
      Scene scene = new Scene(root);

      // WORKS: setListView.getStyleClass().add("list-view-unselect");
      //https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel scene.getStylesheets().add(StageFactory.class.getResource("/styles/style.css").toExternalForm());

      mainStage.setScene(scene);
      mainStage.show();
    } catch (Exception e) {
      LOGGER.fatal(ExceptionUtils.getStackTrace(e));
    }
    mainStage.setOnCloseRequest(e -> {
      LOGGER.info("Shutdown command to thread pools.");
      SpTool3Main.getRunTime().getTaskManager().shutdownThreadPools();
    });
  }

  public static void showConfigView(ParamSet paramSet) {
    Stage popupStage = new Stage();
    popupStage.setTitle("Configuration editor");
    FXMLLoader loader = getLoader(popupStage, "/gui/ConfigurationView.fxml");
    loader.setControllerFactory(
        c -> new ConfigurationViewController(paramSet, popupStage));
    // Fire
    launchAsDynamicStage(loader, popupStage);
    ((ConfigurationViewController) loader.getController()).activateHotkeys(popupStage.getScene());
  }

  public static void showExportView(ExporterParams paramSet) {
    Stage popupStage = new Stage();
    popupStage.setTitle("Export");
    FXMLLoader loader = getLoader(popupStage, "/gui/ExportView.fxml");
    loader.setControllerFactory(
        c -> new ExportViewController(paramSet, popupStage));
    // Fire
    launchAsDynamicStage(loader, popupStage);
    ((ExportViewController) loader.getController()).activateHotkeys(popupStage.getScene());
  }
  // ___________  -----------   ___________
  // ----------- Helper Methods -----------
  // -----------  -----------   -----------

  public static Image getSymbol() {
    return new Image(StageFactory.class.getResource("/img/20240418_symbol.png").toString());
  }

  private static FXMLLoader getLoader(Stage popStage, String resource) {
    final FXMLLoader loader = new FXMLLoader(StageFactory.class.getResource(resource));
    try {
      popStage.getIcons().add(getSymbol());
    } catch (IllegalArgumentException fe) {
      LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
    }
    return loader;
  }

  private static void launchAsDynamicStage(FXMLLoader loader, Stage stage) {
    try {
      Pane primordialRoot = loader.load();
      if (primordialRoot != null) {
        Scene scene = new Scene(primordialRoot);

        // Close on ESC
        scene.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
          if (KeyCode.ESCAPE == event.getCode() && event.isShiftDown()) {
            stage.close();
          }
        });

        // further hotkeys
        if (loader.getController() instanceof Hotkeyable) {
          ((Hotkeyable) loader.getController()).activateHotkeys(scene);
        }

        stage.initOwner(SpTool3Main.getMainStage()); // makes mainStage parent
        stage.initModality(Modality.NONE); // NONE: parent does not freeze
        stage.setScene(scene);
        stage.showAndWait();
      } else {
        LOGGER.fatal("Cannot load primary stage.");
      }
    } catch (Exception e) {
      LOGGER.fatal(ExceptionUtils.getStackTrace(e));
    }
  }
}
