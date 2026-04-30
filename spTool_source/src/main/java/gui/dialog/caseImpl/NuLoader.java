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

import com.google.common.util.concurrent.AtomicDouble;
import gui.dialog.FxStageButton;
import gui.util.UiUtil;
import io.GlobalIO;
import io.XmlUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.CsvInterpreterParams;
import processing.parameterSets.impl.NuInterpreterParams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class NuLoader extends ExecuteSubmethod {

  private static final Logger LOGGER = LogManager.getLogger(NuLoader.class);

  public NuLoader() {
    super(
        null,
        loadDefaultNu(),
        AvailableParameterSets.getOptionAsList(AvailableParameterSets.NU_READER),
        null,
        FxStageButton.RUN,
        null,
        null);

    // Force super to call this function when its own update method is called

    setTopText("Load data from files");
  } // end of constructor


  @Override
  public void closeAndContinue() {
    super.closeAndContinue();
    // also store the current csv
    if (super.getCurrentMethod() != null) {
      // get default path
      Path defaultNuPath = GlobalIO.makeDefaultNuReaderFile();
      // get copy of parameter set in order not to override path in the existing version
      ParamSet currentSet = super.getCurrentMethod().getCopyWithPreviousDateFileAndID();
      currentSet.setAssociatedFileOnDrive(defaultNuPath);
      currentSet.executeOverridingSave();
      LOGGER.trace("Set current nu reader parameters as default at " + defaultNuPath);
    }
  }

  @Nullable
  public static ParamSet loadDefaultNu() {
    ParamSet defaultSet = null;
    // get the default csv
    Path defaultNuPath = GlobalIO.makeDefaultNuReaderFile();
    // also store the current csv
    if (defaultNuPath != null && Files.isReadable(defaultNuPath)) {
      // Try to read the default param set.
      List<ParamSet> sets = XmlUtil.readSetsFromXml(defaultNuPath);
      if (!sets.isEmpty()) {
        if (sets.get(0) != null && sets.get(0) instanceof NuInterpreterParams) {
          defaultSet = sets.get(0);
          LOGGER.trace("Successfully read default nu reader parameters from " + defaultNuPath);
        }
      }
    }

    if (defaultSet == null) {
      LOGGER.trace("Cannot read default csv reader parameters from " + defaultNuPath);
    }

    return defaultSet;
  }
}
