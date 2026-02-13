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

import core.SpTool3Main;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.DirectoryChooser;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathFxParameter extends AbstractFxParameter<String> implements FxParameter<String> {

  public static final Logger LOGGER = LogManager.getLogger(FileFxParameter.class.getName());

  private final TextField field;

  public PathFxParameter(Parameter<String> plain, TextFormatter<String> formatter) {
    super(plain);

    // Make Field.
    field = new TextField(super.plainParameter.getValueAsString());
    field.setTextFormatter(formatter);

    // Add Button to Context: https://jenkov.com/tutorials/javafx/contextmenu.html
    MenuItem menuSearch = UiUtil.getImageMenuItem("Browse", "/img/searchFile.png");
    MenuItem menuOpenExplorer = UiUtil.getImageMenuItem("Show in explorer",
        "/img/openExplorer.png");

    // Tooltip
    super.addToolTip(field);

    menuSearch.setOnAction(e -> {
      // DirectoryChooser: open dialogue
      DirectoryChooser chooser = new DirectoryChooser();
      try {
        String plainStr = super.plainParameter.getValue();
        if (plainStr != null && plainStr.length() > 1) {
          Path currentValue = Path.of(plainStr);
          if (Files.isDirectory(currentValue)) {
            chooser.setInitialDirectory(currentValue.toFile());
          } else if (Files.isDirectory(currentValue.getParent())) {
            chooser.setInitialDirectory(currentValue.getParent().toFile());
          }
        }
        File returnedDirectory = chooser.showDialog(SpTool3Main.getMainStage());
        // make sure thj returned directory is not null (e.g. user aborts choice)
        if (returnedDirectory != null && returnedDirectory.isDirectory()) {
          field.setText(returnedDirectory.getAbsolutePath());
          super.plainParameter.setValue(returnedDirectory.getAbsolutePath());
        }

      } catch (Exception ex) {
        NotificationFactory.openError(ex);
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }
    });

    menuOpenExplorer.setOnAction(e -> {
      String plainStr = super.plainParameter.getValue();
      if (plainStr != null && plainStr.length() > 1) {
        Path currentValue = Path.of(plainStr);
        if (Files.isDirectory(currentValue)) {
          try {
            Runtime.getRuntime().exec("explorer " + currentValue);
          } catch (IOException ee) {
            NotificationFactory.openError(ee);
            LOGGER.error(ExceptionUtils.getStackTrace(ee));
          }
        }
      }
    });

    field.setContextMenu(new ContextMenu());
    field.getContextMenu().getItems().addAll(menuSearch, menuOpenExplorer);

    // Change Listener.
    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    super.slowPause.setOnFinished(event -> {
      if (field.editableProperty().get() && slowPauseNewValue.get() != null && !slowPauseNewValue.get().isEmpty()) {
        // Set value first since children depend on the value.
        super.plainParameter.setValue(slowPauseNewValue.get());
        // Update Children.
        super.notifyValueChange();
      }
    });
    field.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          slowPause.stop();
          slowPauseNewValue.set(newValue);
          slowPause.playFromStart();
        }
    );

    // By re-loading when leaving, we prevent empty Label Fields
    field.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        loadFromPlainWithFormat();
      }
    });
  }

  @Override
  public void forceUpdateExternally() {
    loadFromPlainWithFormat();
  }

  /**
   * This will force an update even if parameter is set uneditable. Uneditable only refers to the
   * possibility to access the field in the UI. If e.g., resetToDefaults etc., is called on the
   * plain instance, we do want to be allowed to load and see this here!
   * <p> Note: The equality check here serves to avoid endless looping by firing listeners -> Make
   * sure the new value is not equal to current. (Disclaimer: I am unsure if this is always
   * necessary as most listeners should listen to CHANGES anyway, but having a break here seems
   * reasonable):
   */

  private void loadFromPlainWithFormat() {
    if (!(plainParameter.getValue().equals(field.getText()))) {
      field.setText(super.plainParameter.getValue());
    }
  }

  @Override
  public Control getValueNode() {
    return field;
  }

  @Override
  public void setUneditable() {
    super.setUneditable();
    field.setEditable(false);
  }

}