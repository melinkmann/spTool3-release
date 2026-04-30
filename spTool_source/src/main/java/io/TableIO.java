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

package io;

import core.RunTimeInstance;
import core.SpTool3Main;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.TableParFactory;
import gui.dialog.FxStageButton;
import gui.dialog.mainImpl.ChooseMultipleFromListDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import visualizer.ResultsTable;
import visualizer.ResultsTable.TablePar;

public class TableIO {

  private static final Logger LOGGER = LogManager.getLogger(TableIO.class);

  private final Path filePath;
  private final List<TablePar> activeParameters;

  public TableIO() {
    this.filePath = GlobalIO.makeResultsTableFile();
    this.activeParameters = new ArrayList<>(read());
    fillIfEmpty();
  }

  private void fillIfEmpty() {
    if (this.activeParameters.isEmpty()) {
      activeParameters.add(TablePar.SAMPLE_NICK_NAME);
      activeParameters.add(TablePar.SAMPLE_FULL_PATH);
      activeParameters.add(TablePar.TRACE_MZ);
      activeParameters.add(TablePar.DWELL_TIME);
      activeParameters.add(TablePar.DURATION);
      activeParameters.add(TablePar.RAW_MEAN);
      activeParameters.add(TablePar.RAW_SD);
      activeParameters.add(TablePar.BLN_DISTR);
      activeParameters.add(TablePar.BLN_MEAN);
      activeParameters.add(TablePar.AEROSOL_TE);
      activeParameters.add(TablePar.POPULATION_NAME);
      activeParameters.add(TablePar.PNC);
      activeParameters.add(TablePar.NP_RATE);
      activeParameters.add(TablePar.NP_MEAN);
      activeParameters.add(TablePar.NP_MEAN_MASS);
      activeParameters.add(TablePar.NP_MEAN_SIZE);
      activeParameters.add(TablePar.LOD_AG);
      activeParameters.add(TablePar.LOD_NM);
      activeParameters.add(TablePar.SEARCH_HEIGHT);
      activeParameters.add(TablePar.SEARCH_HEIGHT_META);
      activeParameters.add(TablePar.GATES);
      activeParameters.add(TablePar.GATES_META);
    }
  }

  public List<TablePar> getActiveParameters() {
    return new ArrayList<>(activeParameters);
  }

  public void setActiveParameters(List<TablePar> params) {
    this.activeParameters.clear();
    this.activeParameters.addAll(params);
  }

  public List<TablePar> read() {
    List<TablePar> params = new ArrayList<>();

    try {
      // Avoid empty files causing problems
      if ((Files.size(filePath) > GlobalIO.MIN_XML_SIZE_BYTES)) {
        BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()));
        String line;
        while ((line = reader.readLine()) != null) {
          try {
            params.add(TablePar.valueOf(line)); // convert string back to enum
          } catch (Exception e) {
            LOGGER.error("Cannot find table parameter: " + line);
          }
        }
      }
    } catch (IOException e) {
      LOGGER.error("Cannot read default table status."
          + " Message: " + ExceptionUtils.getMessage(e)
          + " Details: " + ExceptionUtils.getStackTrace(e));
    }
    return params;
  }

  public void write() {
    // Write list to file
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toString()))) {
      for (TablePar c : activeParameters) {
        writer.write(c.name());  // write enum name
        writer.newLine();
      }
    } catch (IOException e) {
      LOGGER.error("Cannot read default table status."
          + " Message: " + ExceptionUtils.getMessage(e)
          + " Details: " + ExceptionUtils.getStackTrace(e));
    }
  }

  public void reset() {
    this.activeParameters.clear();
    activeParameters.addAll(TablePar.getValuesOrdered());
    fillIfEmpty();
    // write(); // should be up to the user
  }

  public void reload() {
    List<TablePar> read = read();
    this.activeParameters.clear();
    activeParameters.addAll(read);
    fillIfEmpty();
  }

  public void showDialog() {
    FxEntryFactory<TablePar> factory = new TableParFactory(false);

    ChooseMultipleFromListDialog<TablePar> dialog = new ChooseMultipleFromListDialog<>(
        factory.create(TablePar.getValuesOrdered()),
        factory.create(new ArrayList<>(activeParameters)),
        factory,
        true,
        false,
        true,
        false,
        false,
        FxStageButton.CONTINUE);

    Optional<List<TablePar>> result = dialog.showAndWait();
    if (result != null && result.isPresent()) {
      List<TablePar> results = result.get();
      this.activeParameters.clear();
      this.activeParameters.addAll(results);
      fillIfEmpty();
      SpTool3Main.getRunTime().getGuiParameterManager().notifyValueChange();
    }

  }
}
