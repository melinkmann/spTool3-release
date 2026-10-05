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

import core.SpTool3Main;
import gui.dialog.FxStageButton;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import io.MethodSchedulerTask;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.MethodWatcherParams;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class MethodSchedulerView extends ExecuteSubmethod {

  private static final Logger LOGGER = LogManager.getLogger(MethodSchedulerView.class);

  protected static final double USER_WIDTH = 600;
  protected static final double USER_HEIGHT = 200;

  private MethodSchedulerTask methodWatcher;
  private final ToggleButton watchButton = new ToggleButton("Watch");

  public MethodSchedulerView(MethodWatcherParams params) {
    super(params,
        params,
        AvailableParameterSets.getOptionAsList(AvailableParameterSets.METHOD_WATCH),
        "Method scheduler",
        FxStageButton.CLOSE,
        USER_WIDTH,
        USER_HEIGHT);

    this.methodWatcher = new MethodSchedulerTask();

    watchButton.setOnAction(e -> {
      if (watchButton.isSelected()) {
        // new state: active
        ParamSet paramSet = super.getCurrentMethod();
        if (paramSet instanceof MethodWatcherParams) {
          MethodWatcherParams watchParams = (MethodWatcherParams) paramSet;
          String pathStr = watchParams.getCurrentMethodWatchPath().getValue();
          Path dir = Path.of(pathStr);

          if (!dir.toString().isEmpty() && Files.exists(dir) && Files.isDirectory(dir) && Files.isReadable(dir)) {

            NotificationFactory.openYesCancel(
                "Do you want to schedule method execution from this directory:" +
                    "\n" + pathStr + "?" +
                    "\n\nBe aware that spTool will move any '.spm' file in the directory to a new directory" +
                    "\nthat will be created in the input path called 'scheduled':" +
                    "\n" + pathStr + File.separator + "scheduled",
                () -> {
                  SpTool3Main.getRunTime().saveParameterSets();
                  methodWatcher.arm(dir, watchButton);
                },
                () -> {
                  watchButton.setSelected(false);
                  methodWatcher.disarm();
                },
                UiUtil.getOwner(watchButton));
          } else {
            // new state: not active
            methodWatcher.disarm();
            watchButton.setSelected(false);
            NotificationFactory.openInfo("Enter a valid path that exists on your system!",
                UiUtil.getOwner(watchButton));
          }
        }

      } else {
        // new state: not active
        methodWatcher.disarm();
      }
    });


    setTopText("Schedule execution of methods");

    // specials
    BorderPane container = new BorderPane();
    container.setPadding(new Insets(2));
    container.setCenter(watchButton);
    subContentPane.setCenter(UiUtil.putOnAnchorWithInsets(container));

  } // end of constructor


  @Override
  public void closeAndKeepCurrentState() {
    super.closeAndKeepCurrentState();
    methodWatcher.disarm();
  }

  // Cancel button or little "x"
  @Override
  public void closeAndCancelChanges() {
    super.closeAndCancelChanges();
    methodWatcher.disarm();
  }

  @Override
  public void saveAndSetResults() {
    super.saveAndSetResults();
    methodWatcher.disarm();
  }


  @Override
  public void closeAndContinue() {
    super.closeAndContinue();
    methodWatcher.disarm();
  }

}
