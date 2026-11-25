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
import io.*;

import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.objectSerials.ImportDefaults;
import processing.objectSerials.SettingsSerializer;
import processing.options.StartupConfiguration;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.*;
import processing.parameterSets.uiParams.GuiParameterManager;
import processing.parameters.ComboEnumParameter;
import tasks.TaskManager;
import util.Util;
import util.storage.MemoryMapStorage;
import visualizer.styles.Colors;

import javax.annotation.Nullable;

import static core.SpTool3Main.VERSION_ID;

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
  private static final MemoryMapStorage SPECTRA_BUFFER_STORAGE = new MemoryMapStorage();
  private static final Iterator<Colors> SAMPLE_COLOR_ITERATOR
      = Colors.getDefaultIterator().iterator();
  private Path lastProjectFile = Path.of("");

  public TimeRoiParams timeRoiParams = new TimeRoiParams();
  public MethodWatcherParams methodWatcherParams = new MethodWatcherParams();
  public MethodDoeParams methodDoeParams = new MethodDoeParams();
  public LoadMethodWizardParams loadMethodWizardParams = new LoadMethodWizardParams();
  public EventDataRangeParams eventDataRangeParams = new EventDataRangeParams();
  public IsotopeCalculatorParams isotopeCalculatorParams = new IsotopeCalculatorParams();
  public DTGroupParams dtGroupParams = new DTGroupParams();

  private final AtomicDouble xPosition = new AtomicDouble(0);
  private final AtomicDouble yPosition = new AtomicDouble(0);

  public RunTimeInstance() {

    // Loading config: this surprisingly works; we can open the Dialog when constructing this class
    // as a static field inside class SpTool3Main
    Util.VersionCheckResult versionCheck = Util.checkVersion();
    /*
       feedback = "You are not using the latest version! " +
       "\nYour version: " + VERSION_ID + ", latest version online: " + latestVersion + "." +
       "\nCheck for updates: \n" + location;
     */
    try {

      Text subHeader = new Text("*Analyser includes generator capability");
      subHeader.setFont(new Font(12));

      TextFlow feedbackText;
      if (versionCheck.wasChecked() && !versionCheck.needsUpdate()) {
        Text versionFeedback = new Text("\n\nYou are using the latest version (" + VERSION_ID + ").");
        subHeader.setFont(new Font(12));
        feedbackText = new TextFlow(subHeader, versionFeedback);

      } else if (versionCheck.wasChecked() && versionCheck.needsUpdate()) {
        Text versionFeedback = new Text("\n\nYour version (" + VERSION_ID + ") is not up-to-date!" +
            "\nThe latest version on github is " + versionCheck.onlineVersion() + "." +
            "\nConsider downloading the latest version at\n");
        subHeader.setFont(new Font(12));

        Hyperlink hyperlink = new Hyperlink(versionCheck.url());
        hyperlink.setFont(new Font(12));
        hyperlink.setOnAction(e -> {
          try {
            Desktop.getDesktop().browse(new URI(versionCheck.url()));
          } catch (Exception ex) {
            LOGGER.info("Failed to open browser: " + ex.getMessage());
          }
        });

        feedbackText = new TextFlow(subHeader, versionFeedback, hyperlink);
      } else {
        feedbackText = new TextFlow(subHeader);
      }

      Dialog<StartupConfiguration> d = new SingleParameterDialog<>(
          feedbackText,
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

    // load run time parameter sets
    loadParameterSets();

    this.interferenceDatabase.addAll(InterferenceDatabase.loadFromCSV());
  }


  public void setMainWindowController(MainWindowController mainWindowController) {
    RunTimeInstance.mainWindowController = mainWindowController;
  }

  public void setLastProjectFile(Path currentProjectFile) {
    Platform.runLater(() -> {
      SpTool3Main.getMainStage().setTitle("spTool " + VERSION_ID + " - "
          + currentProjectFile.toString());
    });

    this.lastProjectFile = currentProjectFile;

    // This is less annoying: consider saving it, too, via force save config

    if (Files.exists(currentProjectFile)) {
      Path projectDir;
      if (Files.isDirectory(currentProjectFile)) {
        projectDir = currentProjectFile;
      } else {
        projectDir = currentProjectFile.getParent();
      }
      confParams.setDefaultProjectPath(projectDir);
    }
  }

  @Nullable
  public Path getLastProjectFile() {
    if (lastProjectFile.toString().isEmpty()) {
      return null;
    } else {
      return lastProjectFile;
    }
  }

  public void saveParameterSets() {
    Path methodFile = GlobalIO.makeRuntimeSubmethodFile();
    List<ParamSet> paramSets = new ArrayList<>();
    // Parameters
    paramSets.add(timeRoiParams);
    paramSets.add(methodWatcherParams);
    paramSets.add(methodDoeParams);
    paramSets.add(loadMethodWizardParams);
    paramSets.add(eventDataRangeParams);
    paramSets.add(isotopeCalculatorParams);
    paramSets.add(dtGroupParams);
    XmlUtil.writeSubMethodsToFile(paramSets, "Runtime parameter collection", methodFile);
  }

  public void loadParameterSets() {
    try {
      Path methodFile = GlobalIO.makeRuntimeSubmethodFile();
      List<FxParamSet> sets = XmlUtil.getSubMethodsFromFile(methodFile);
      for (FxParamSet set : sets) {
        switch (set.getPlainSet().getXmlType()) {
          case TimeRoiParams.XML_ELEMENT_TAG:
            timeRoiParams = (TimeRoiParams) set.getPlainSet();
            break;
          case MethodWatcherParams.XML_ELEMENT_TAG:
            methodWatcherParams = (MethodWatcherParams) set.getPlainSet();
            break;
          case MethodDoeParams.XML_ELEMENT_TAG:
            methodDoeParams = (MethodDoeParams) set.getPlainSet();
            break;
          case LoadMethodWizardParams.XML_ELEMENT_TAG:
            loadMethodWizardParams = (LoadMethodWizardParams) set.getPlainSet();
            break;
          case EventDataRangeParams.XML_ELEMENT_TAG:
            eventDataRangeParams = (EventDataRangeParams) set.getPlainSet();
            break;
          case IsotopeCalculatorParams.XML_ELEMENT_TAG:
            isotopeCalculatorParams = (IsotopeCalculatorParams) set.getPlainSet();
            break;
          case DTGroupParams.XML_ELEMENT_TAG:
            dtGroupParams = (DTGroupParams) set.getPlainSet();
            break;
          default:
            break;
        }
      }
    } catch (Exception e) {
      LOGGER.error("Message: " + e.getMessage()
          + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }
    LOGGER.info("Loaded run time action parameters.");
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

  public static MemoryMapStorage getSpectraBufferStorage() {
    return SPECTRA_BUFFER_STORAGE;
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

  public synchronized Colors getNextSampleColor() {
    return SAMPLE_COLOR_ITERATOR.next();
  }


  public AtomicDouble getXPosition() {
    return xPosition;
  }

  public AtomicDouble getYPosition() {
    return yPosition;
  }
}
