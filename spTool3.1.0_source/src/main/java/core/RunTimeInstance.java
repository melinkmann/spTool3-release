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

import com.google.common.util.concurrent.AtomicDouble;
import dataModelNew.mz.InterferenceDatabase;
import dataModelNew.mz.InterferenceDatabase.InterferenceEntry;
import gui.MainWindowController;
import gui.dialog.SingleParameterDialog;
import gui.dialog.caseImpl.ListPaneDialog;
import io.*;

import java.util.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableListBase;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.objectSerials.ImportDefaults;
import processing.objectSerials.SettingsSerializer;
import processing.options.StartupConfiguration;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.ConfParams;
import processing.parameterSets.impl.ExporterParams;
import processing.parameterSets.uiParams.GuiParameterManager;
import processing.parameters.ComboEnumParameter;
import tasks.TaskManager;
import util.storage.MemoryMapStorage;
import visualizer.styles.Colors;

public class RunTimeInstance {
  private static final Logger LOGGER = LogManager.getLogger(RunTimeInstance.class);

  private ConfParams confParams;
  private ExporterParams exportParams = new ExporterParams();
  private static ImportDefaults importDefaults = new ImportDefaults();
  private static TaskManager taskManager = new TaskManager(4); // just a dummy
  private static MainWindowController mainWindowController = null;
  private static SampleRegister sampleRegister = new SampleRegister();
  private static final TableIO PARAM_TABLE_DEFAULTS = new TableIO();
  private final QuantileData_V2 compoundPoissonQuantiles = new QuantileData_V2();
  private final SpCalDensity densityDatabase = new SpCalDensity();
  private final List<InterferenceEntry> interferenceDatabase = new ArrayList<>();
  private final GuiParameterManager guiParameterManager;
  private static final MemoryMapStorage RAW_BUFFER_STORAGE = new MemoryMapStorage();
  private static final MemoryMapStorage SIMULATION_BUFFER_STORAGE = new MemoryMapStorage();
  private static final MemoryMapStorage INDEX_BUFFER_STORAGE = new MemoryMapStorage();
  private static final MemoryMapStorage BACKGROUND_BUFFER_STORAGE = new MemoryMapStorage();
  private static final Iterator<Colors> SAMPLE_COLOR_ITERATOR
      = Colors.getDefaultIterator().iterator();

  private final AtomicDouble xPosition = new AtomicDouble(0);
  private final AtomicDouble yPosition = new AtomicDouble(0);

  public RunTimeInstance() {

    // Loading config: this surprisingly works; we can open the Dialog when constructing this class
    // as a static field inside class SpTool3Main
    try {

      Dialog<StartupConfiguration> d = new SingleParameterDialog<>(
          "*Analyser includes generator capability",
          new ComboEnumParameter<>(
              "Configuration",
              """
                  Choose startup configuration.
                  
                  (A) Analyser configuration focuses on data analysis and csv data import.
                      It also includes the data generator and allows to process in-silico data.
                  
                  (B) Generator configuration focuses on in-silico data generation
                      without analysis or csv import capabilities""",
              StartupConfiguration.ANALYZER,
              StartupConfiguration.values(),
              StartupConfiguration.class,
              false,
              "startUp"
          ));

      // Prompt user to decide configuration
      Optional<StartupConfiguration> res = d.showAndWait();
      if (res.isPresent()) {
        StartupConfiguration strRes = res.get();
        if (Objects.equals(strRes, StartupConfiguration.ANALYZER)) {
          SpTool3Main.setANALYZER(true);
        } else {
          SpTool3Main.setANALYZER(false);
        }
      } else {
        taskManager.shutdownThreadPools();
        Platform.exit();
        System.exit(0);
      }
    } catch (Exception e) {
      SpTool3Main.setANALYZER(true);
      LOGGER.warn("Error occurred while reading startup configuration. Continued with analyser = true."
          + " Message: " + ExceptionUtils.getMessage(e)
          + " Stack trace: " + ExceptionUtils.getStackTrace(e));
    }

    // ini after setting analyzer since these fields read from Analyzer
    confParams = new ConfParams();
    guiParameterManager = new GuiParameterManager();


    // Try to read the configuration.
    List<ParamSet> sets = XmlUtil.readSetsFromXml(ConfParams.CONFIG_FILE.toPath());
    if (!sets.isEmpty()) {
      if (sets.get(0) != null && sets.get(0) instanceof ConfParams) {
        confParams = (ConfParams) sets.get(0);
      }
    }

    // Try to read the export.
    sets = XmlUtil.readSetsFromXml(ExporterParams.EXPORT_FILE.toPath());
    if (!sets.isEmpty()) {
      if (sets.get(0) != null && sets.get(0) instanceof ExporterParams) {
        exportParams = (ExporterParams) sets.get(0);
      }
    }

    // Instantiate Task Manager with the loaded Configuration (as it needs parameters!)
    taskManager.shutdownThreadPools();
    taskManager = new TaskManager(confParams.calcParallelThreads());

    importDefaults = SettingsSerializer.loadImportDefaults();

    this.interferenceDatabase.addAll(InterferenceDatabase.loadFromCSV());
  }


  public void setMainWindowController(MainWindowController mainWindowController) {
    RunTimeInstance.mainWindowController = mainWindowController;
  }

  // Getter because field is not final (declaration above) and thus private
  public MainWindowController getMainWindowCtl() {
    return mainWindowController;
  }

  public synchronized TaskManager getTaskManager() {
    return taskManager;
  }

  public ExporterParams getExportParams() {
    return exportParams;
  }

  public void setExportParams(ExporterParams exportParams) {
    this.exportParams = exportParams;
  }

  public ConfParams getConfParams() {
    return confParams;
  }

  public void setConfParams(ConfParams confParams) {
    this.confParams = confParams;
  }

  public ImportDefaults getImportDefaults() {
    return importDefaults;
  }

  public SampleRegister getSampleReg() {
    return sampleRegister;
  }

  public static void setSampleRegister(SampleRegister sampleRegister) {
    RunTimeInstance.sampleRegister = sampleRegister;
    // calls a refresh
    mainWindowController.updateSampleSets();
  }

  public static TableIO getParamTableDefaults() {
    return PARAM_TABLE_DEFAULTS;
  }

  public QuantileData_V2 getCompoundPoissonQuantiles() {
    return compoundPoissonQuantiles;
  }

  public SpCalDensity getDensityDatabase() {
    return densityDatabase;
  }

  public GuiParameterManager getGuiParameterManager() {
    return guiParameterManager;
  }

  public MemoryMapStorage getRawBufferStorage() {
    return RAW_BUFFER_STORAGE;
  }

  public MemoryMapStorage getBackgroundBufferStorage() {
    return BACKGROUND_BUFFER_STORAGE;
  }

  public MemoryMapStorage getIndexBufferStorage() {
    return INDEX_BUFFER_STORAGE;
  }

  public MemoryMapStorage getSimulationBufferStorage() {
    return SIMULATION_BUFFER_STORAGE;
  }

  // Writes the current state as a copy.
  public void storeImportDefaults() {
    SettingsSerializer.write(GlobalIO.makeImportWindow(), importDefaults);
  }

  public List<InterferenceEntry> getInterferenceDatabase() {
    // Assume, something went wrong while first reading attempt..
    // We know, the data base is some 3000 entries, thus at least >100
    if (interferenceDatabase.size() < 100) {
      List<InterferenceEntry> entries = InterferenceDatabase.loadFromCSV();
      interferenceDatabase.clear();
      interferenceDatabase.addAll(entries);
    }
    return interferenceDatabase;
  }

  public Colors getNextSampleColor() {
    return SAMPLE_COLOR_ITERATOR.next();
  }


  public AtomicDouble getXPosition() {
    return xPosition;
  }

  public AtomicDouble getYPosition() {
    return yPosition;
  }
}
