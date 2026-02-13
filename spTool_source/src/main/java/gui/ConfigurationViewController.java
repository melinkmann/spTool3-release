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
import gui.dialog.DialogUtil;
import gui.dialog.FxStage;
import gui.dialog.FxStageButton;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import io.XmlUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.ConfParams;
import processing.parameterSets.uiParams.GuiParameterManager;
import processing.parameters.FxParameter;

public class ConfigurationViewController implements ParameterView, FxStage, Hotkeyable {

  private static final Logger LOGGER = LogManager
      .getLogger(ConfigurationViewController.class.getName());

  private final FxStageButton fxMainButton;
  private final FxStageButton fxSecondaryButton;

  protected final Stage stage;

  protected final ParamSet paramSet;
  protected final FxParamSet fxParamSet;

  // Keep one listview to prevent resetting position for each time we update anything.
  private ListView<FxParameter<?>> view = null;

  protected final ToolBar topToolbar;
  @FXML
  protected BorderPane borderPane;

  // Remember previous value, i.e., at construction
  private final Path oldCurrentMethodFile;

  public ConfigurationViewController(ParamSet paramSet, Stage stage) {

    this.oldCurrentMethodFile = SpTool3Main.getRunTime().getConfParams().getCurrentMethodFile();

    // This should be a copy in order to make sure that changes are only passed when calling SAVE
    this.paramSet = paramSet.getCopyWithPreviousDateFileAndID();

    // Keep the file! (the copy discards it, as it is intended as a new copy to edit (i.e., new file!)

    this.fxParamSet = this.paramSet.getObservableInstance();

    this.stage = stage;

    this.fxMainButton = FxStageButton.SAVE;
    this.fxSecondaryButton = FxStageButton.CANCEL;

    this.topToolbar = new ToolBar();
  }

  // After constructor call and building of pane this method is called and it can fill Views with Lists, ...
  public void initialize() {

    stage.setWidth(1000);
    stage.setHeight(750);

    // Else: "x" does not trigger any of the "goodbye" methods
    stage.setOnCloseRequest(event -> closeAndCancelChanges());

    fxParamSet.setController(this);
    borderPane.setTop(topToolbar);
    notifyItemChange();

    // Add Buttons to Top
    Button saveBtn = UiUtil.getToolbarBtn("/img/save.png", "Save");
    saveBtn.setOnAction(e -> executeSave());

    Button asDefaultBtn = DialogUtil.getSetParameterSetAsDefaultButton(
        () -> Collections.singletonList(fxParamSet));

    Button restoreDefaultBtn = DialogUtil.getRestoreParameterSetButton(
        () -> Collections.singletonList(fxParamSet));

    topToolbar.getItems().addAll(
        saveBtn,
        new Separator(Orientation.VERTICAL),
        asDefaultBtn,
        restoreDefaultBtn);

    // Bottom Buttons
    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().add(fxMainButton.getBold(this));
    buttonBar.getButtons().add(fxSecondaryButton.get(this));
    borderPane.setBottom(UiUtil.putOnAnchorWithInsets(buttonBar));
  }

  @Override
  public void notifyItemChange() {
    if (view == null) {
      view = MethodView.createParamView(fxParamSet);
    } else {
      view.getItems().clear();
      view.getItems().addAll(fxParamSet.getActiveFxParameters());
    }

    // Anchor(ing) Pane makes sure that resizing works. Note: Null Check b/c border pane is from fxml.
    if (borderPane != null) {
      borderPane.setCenter(UiUtil.putOnAnchorWithInsets(view));
    }
  }

  @Override
  public void notifyValueChange() {
    notifyItemChange();
  }

  @Override
  public void activateHotkeys(Scene scene) {
    scene.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
      // Save on control s
      if (StageFactory.KEY_CTL_S.match(event)) {
        executeSave();

        // If "continue" like Button is present and control enter is hot, call "close and continue"
      } else if (StageFactory.KEY_CTL_ENTER.match(event)
          && (fxMainButton.equals(FxStageButton.CONTINUE)
          || fxMainButton.equals(FxStageButton.SELECT)
          || fxMainButton.equals(FxStageButton.RUN))) {
        closeAndContinue();
      }
    });
  }

  /**
   * Currently, only static ParamSets like config are shown with this editor. Thus, the save()
   * method is the only one needed.
   */
  private void executeSave() {

    // Config file has an associatedFileOnDrive()
    NotificationFactory.openYesNo("Save settings? This is irreversible.",
        () -> {
          if (paramSet instanceof ConfParams) {
            // conf params are written to their respective static file
            paramSet.executeOverridingSave();
            // Also set to the runtime if save (to keep changes)
            SpTool3Main.getRunTime().setConfParams((ConfParams) paramSet);

            // Otherwise: check if there is a file and then write to that file
          } else if (paramSet.hasAssociatedFileOnDrive()) {
            paramSet.executeOverridingSave();
          }
        }
    );
  }


  // FxStage methods
  @Override
  public void closeAndKeepCurrentState() {
    closeAndContinue(); // ((Stage) scene.getWindow()).close(); ???
  }

  @Override
  public void closeAndCancelChanges() {
    stage.close();
  }

  @Override
  public void saveAndSetResults() {
    executeSave();
    closeAndContinue();
  }

  @Override
  public void closeAndContinue() {
    stage.close();

    // Also set to the runtime if continue (to keep changes)
    SpTool3Main.getRunTime().setConfParams((ConfParams) paramSet);

    // UI Buttons show/hide
    if (SpTool3Main.getRunTime().getGuiParameterManager() != null) {
      GuiParameterManager guiParameterManager = SpTool3Main.getRunTime().getGuiParameterManager();
      guiParameterManager.fillButtonList((ConfParams) paramSet);
    }

    // Method file was changed while editing the configuration -> load it.
    Path newCurrentMethodFile = SpTool3Main.getRunTime().getConfParams().getCurrentMethodFile();
    try {
      if (!Files.isSameFile(oldCurrentMethodFile, newCurrentMethodFile)) {
        MainWindowController controller = SpTool3Main.getRunTime().getMainWindowCtl();
        controller.getMethodView().setMethod(XmlUtil.readMethodFromXml(newCurrentMethodFile));
        controller.updateMethodMetaDataInUI();
      }
    } catch (IOException ioException) {
      LOGGER.error(ExceptionUtils.getStackTrace(ioException));
    }
  }


}

