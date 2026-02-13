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

package core;


import gui.util.UiUtil;
import io.export.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import math.units.enums.SensitivityUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.Statistics;
import util.NF;


public class SpTool3TestMain extends Application {

  private static final Logger LOGGER = LogManager.getLogger(SpTool3TestMain.class);

  private static Stage mainStage = new Stage();

  // **Main - - Java**
  public static void main(String[] args) {
    launch(args);
  } // main end

  @Override
  public void start(Stage stage) throws Exception {

  } // start
} // End of Main