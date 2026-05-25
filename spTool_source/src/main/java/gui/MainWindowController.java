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

import analysis.PopulationID;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.fxImpl.FxSample;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.ParamSetWithDateEntryFactory;
import gui.dialog.FxEntryFactory.RecentLocationsDialogEntryFactory;
import gui.dialog.FxEntryFactory.SimpleEntryFactory;
import gui.dialog.FxEntryFactory.SingleFileSetEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.caseImpl.*;
import gui.dialog.notification.NotificationFactory;
import gui.listAndSearch.SampleListAndTable;
import gui.util.TextFieldUtils;
import gui.util.UiUtil;
import io.FileSet;
import io.FxFileSet;
import io.FxVisitedFile;
import io.GlobalIO;
import io.PathUtil;
import io.SampleSet;
import io.Serializer;
import io.XmlUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

import javax.annotation.Nullable;

import javafx.stage.Stage;
import javafx.util.StringConverter;
import math.units.Unit;
import math.units.enums.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.objectSerials.ImportDefaults;
import processing.parameterSets.Method;
import processing.parameterSets.action.Actions;
import processing.parameterSets.impl.ConfParams;
import processing.parameterSets.impl.CsvInterpreterParams;
import processing.parameterSets.impl.ExporterParams;
import processing.parameterSets.impl.NuInterpreterParams;
import processing.parameterSets.uiParams.GuiParameterManager;
import sandbox.montecarlo.Isotope;
import tasks.BatchTask;
import tasks.Task;
import tasks.WorkingTask;
import tasks.batch.SimpleLinearBatch;
import tasks.results.EmptyTaskResult;
import tasks.single.CsvImportTask;
import tasks.single.NuImportTask;
import util.Util;

public class MainWindowController {


  private static final Logger LOGGER = LogManager.getLogger(MainWindowController.class);

  @FXML
  public ProgressBar progressIndicator;
  public Text progressIndicatorPercent;
  public Text ramStatusLbl;
  public Text storageStatusLbl;
  public Button stopBtn;
  public Button executeMethodBtn;
  public Button reprocessSamplesBtn;
  public Spinner<Integer> repetitionSpinner;
  public MenuBar mainMenuBar;
  public ComboBox<NMPUnit> mainUnitComboBox;
  private Menu fileMenu = new Menu("File");
  private Menu importMenu = new Menu("Import");
  private Menu exportMenu = new Menu("Export");
  private Menu actionMenu = new Menu("Action");
  private Menu toolsMenu = new Menu("Tools");
  private Menu editMenu = new Menu("Edit");
  private Menu aboutMenu = new Menu("Help");
  //
  MenuItem mainImportMenu = new MenuItem("Select files");
  MenuItem exportMenuItem = new MenuItem("Show dialog");
  //
  public BorderPane mainBorderPane;
  public BorderPane mainViewerPane;
  public SplitPane sampleChooserSplitPane;
  public TableView<FxSample> sampleTableView;
  public TableView<Isotope> isotopeTableView;
  public ListView<FxEntry<SampleSet>> sampleSetListView;
  public ListView<PopulationID> populationView;
  public BorderPane sampleSetBorderPane;
  public BorderPane sampleBorderPane;
  public BorderPane populationBorderPane;
  public Text methodLabel;
  public VBox tabLikeBtnVbox;

  private SampleListAndTable combinedSampleListAndSearchView;

  private final MethodView methodView = new MethodView();

  private final AtomicReference<MenuItem> lastMenuItem = new AtomicReference<>(null);

  // After constructor call and building of pane this method is called and it can fill Views with Lists, ...
  public void initialize() {

    // ################################# UNIT ######################

    ObservableList<NMPUnit> availableUnits;
    if (SpTool3Main.getANALYZER()) {
      availableUnits = FXCollections.observableArrayList(NMPUnit.values());
    } else {
      availableUnits = FXCollections.observableArrayList(NMPUnit.CTS);
    }
    mainUnitComboBox.setItems(availableUnits);
    mainUnitComboBox.getSelectionModel().select(NMPUnit.CTS);
    // To String methods
    mainUnitComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(NMPUnit u) {
        String value = "N/A";
        if (u != null) {
          value = NMPUnit.getPrefix(u) + ": " + u.getUiString();
        }
        return value;
      }

      @Override
      public NMPUnit fromString(String string) {
        return mainUnitComboBox.getSelectionModel().getSelectedItem();
      }
    });

    // Change Listener.
    mainUnitComboBox.getSelectionModel().selectedItemProperty().addListener(
        (observableValue, oldValue, newVal) -> {
          if (newVal != null) {
            // refresh the UI -> tell the manager that change occurred
            SpTool3Main.getRunTime().getGuiParameterManager()
                .notifySampleOrPopulationSelectionChange();
          }
        });

    // ################################# BUILD TAB STRUCTURE FOR THE MAIN UI ######################

    UiUtil.addImageOnlyToButton(executeMethodBtn, "Create", "/img/startCreate.png", "");

    UiUtil.addImageOnlyToButton(reprocessSamplesBtn, "Process", "/img/start.png", "");

    repetitionSpinner.setTooltip(
        new Tooltip("Execute method multiple times to simulate replication."));
    SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
    repetitionSpinner.setValueFactory(valueFactory);
    repetitionSpinner.getEditor().setTextFormatter(TextFieldUtils.assureNonzeroPositiveInteger(1));
    repetitionSpinner.setEditable(true); // Allow user to manually input values
    // Add a listener to ensure valid input
    repetitionSpinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
      try {
        int value = Integer.parseInt(newValue);
        if (value < 1) {
          repetitionSpinner.getEditor().setText("1");
        }
      } catch (NumberFormatException e) {
        repetitionSpinner.getEditor().setText(oldValue); // Restore previous valid value
      }
    });

    methodLabel.setStyle("-fx-font-weight: bold");

    UiUtil.addImageOnlyToButton(stopBtn, "Stop", "/img/stop.png", "Stop process.");
    stopBtn.setOnAction(e -> SpTool3Main.getRunTime().getTaskManager().stop());

    VBox tabs = SpTool3Main.getRunTime().getGuiParameterManager().linkButtonsWithMainViewer(this);
    tabLikeBtnVbox.setAlignment(Pos.TOP_CENTER);
    tabLikeBtnVbox.getChildren().add(tabs);

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<< BUILD TAB STRUCTURE FOR THE MAIN UI <<<<<<<<<<<<<<<<<<<<<<<<

    // Main ListViews
    enableDividerPositions();
    sampleChooserSplitPane.getDividers().forEach(d -> {
      d.positionProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                            Number newValue) {
          List<Double> positions = sampleChooserSplitPane.getDividers().stream()
              .map(Divider::getPosition)
              .collect(Collectors.toList());
          // set
          SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters()
              .setBottomSliders(positions);
        }
      });
    });

    this.combinedSampleListAndSearchView = new SampleListAndTable(
        sampleSetListView,
        sampleTableView,
        isotopeTableView,
        populationView,
        SelectionMode.SINGLE,
        true);

    sampleSetBorderPane.setTop(combinedSampleListAndSearchView.getSearchFld());

    // Layout AND empty list

    // Show size label of the Table
    sampleBorderPane.setCenter(UiUtil.putOnAnchorWithoutInsets(
        combinedSampleListAndSearchView.getViewAndLengthPane()));

    //
    executeMethodBtn.setOnAction(e -> {
      Method current = getMethodView().getCurrentMethod();
      if (current != null) {
        Actions.executeMethodCreateBtn(current, repetitionSpinner.getValue());
      }
    });

    reprocessSamplesBtn.setOnAction(e -> {
      Method current = getMethodView().getCurrentMethod();
      if (current != null) {
        Actions.reprocess(current, sampleTableView.getSelectionModel().getSelectedItems());
      }
    });

    methodView.setMethod(XmlUtil.readMethodFromXml(
        SpTool3Main.getRunTime().getConfParams().getCurrentMethodFile()));

    // Replace initial dummy instances
    SpTool3Main.getRunTime().getTaskManager().setMainProgressIndicator(progressIndicator,
        progressIndicatorPercent);
    SpTool3Main.getRunTime().getTaskManager().setRamStatusLbl(ramStatusLbl);
    SpTool3Main.getRunTime().getTaskManager().setStorageStatusLbl(storageStatusLbl);

    ///////////////////////////////////////// MENUS /////////////////////////////////////////
    if (SpTool3Main.getANALYZER()) {
      mainMenuBar.getMenus().addAll(fileMenu, importMenu, exportMenu, editMenu, actionMenu,
          toolsMenu, aboutMenu);
    } else {
      mainMenuBar.getMenus().addAll(fileMenu, exportMenu, editMenu, actionMenu, toolsMenu,
          aboutMenu);
      // remove the reprocess button
      Parent parent = reprocessSamplesBtn.getParent();
      if (parent instanceof Pane) {
        Pane paneParent = ((Pane) parent);
        paneParent.getChildren().remove(reprocessSamplesBtn);
      }
    }

    MenuItem saveUiParameters = new MenuItem("Save appearance");
    saveUiParameters.setOnAction(e -> {
      GuiParameterManager parameterManager = SpTool3Main.getRunTime().getGuiParameterManager();
      NotificationFactory.openYesNo("Do you want to save the current settings"
          + " of the plots and the user interface?", parameterManager::save);
    });

    MenuItem resetUiParameters = new MenuItem("Reset appearance");
    resetUiParameters.setOnAction(e -> {
      GuiParameterManager parameterManager = SpTool3Main.getRunTime().getGuiParameterManager();
      NotificationFactory.openYesNo("Do you want to reset the current settings"
          + " of the plots and the user interface to the defaults?", parameterManager::reset);
    });

    MenuItem checkSampleFiles = new MenuItem("Check sample files");
    checkSampleFiles.setOnAction(e -> {
      List<Sample> allSamples = SpTool3Main.getRunTime().getSampleReg().getAllSets().stream()
          .map(SampleSet::getSamples)
          .flatMap(Collection::stream)
          .distinct()
          .toList();
      Util.updateSamplePath(allSamples);
    });


    MenuItem saveProject = new MenuItem("Save project as");
    saveProject.setOnAction(e -> {
      NotificationFactory.openYesNo("Do you want to save the project?",
          Serializer::saveSampleRegister);
    });

    MenuItem loadProject = new MenuItem("Open project");
    loadProject.setOnAction(e -> {
      NotificationFactory.openYesNo("Do you want to load a project and discard current project?",
          Serializer::loadSampleRegister);
    });

    fileMenu.getItems().addAll(saveProject, loadProject, saveUiParameters, resetUiParameters,
        checkSampleFiles);

    // "Show Dialog"
    exportMenuItem.setOnAction(e -> {
          ExporterParams exportParams = SpTool3Main.getRunTime().getExportParams();
          StageFactory.showExportView(exportParams);
        }
    );

    exportMenu.getItems().addAll(exportMenuItem);

    // Action

    Actions.makeIncreaseDTMenuItem(actionMenu, lastMenuItem);
    Actions.makeTimeRoiMenuItem(actionMenu, lastMenuItem);
    Actions.makeBasicRoiMenuItem(actionMenu, lastMenuItem);
    Actions.makeIsotopeRatioMenu(actionMenu, lastMenuItem);

    MenuItem confMenu = new MenuItem("Configuration");
    confMenu.setOnAction(e -> {
      ConfParams conf = SpTool3Main.getRunTime().getConfParams();
      StageFactory.showConfigView(conf);
    });

    MenuItem subMethodMenu = new MenuItem("Submethod editor");
    subMethodMenu.setOnAction(e -> {
      SubmethodEditor dialog = new SubmethodEditor(
          new ParamSetWithDateEntryFactory(),
          FxStageButton.SAVE,
          null,
          false);
      dialog.showAndWait();
    });

    editMenu.getItems().addAll(subMethodMenu, confMenu);


    //---------------------------------------------------------------------------------------------
    importMenu.getItems().add(mainImportMenu); //"list files"
    mainImportMenu.setOnAction(e -> {

      ImportDefaults importDefaults = SpTool3Main.getRunTime().getImportDefaults();
      FxEntryFactory<Path> factory = new SimpleEntryFactory<>();

      BrowseAndListFilesDialog filesDialog = new BrowseAndListFilesDialog(
          importDefaults, factory);

      Optional<List<Path>> result = filesDialog.showAndWait();
      result.ifPresent(r -> startImportNewFiles(result.get(), SpTool3Main.getMainStage()));
    });

    MenuItem browseMenu = new MenuItem("Browse folder");
    importMenu.getItems().add(browseMenu);
    browseMenu.setOnAction(e -> {
      DirectoryChooser chooser = new DirectoryChooser();
      ImportDefaults importDefaults = SpTool3Main.getRunTime().getImportDefaults();
      // directoryChooser: set start point for the chooser
      Path defaultImport = SpTool3Main.getRunTime().getConfParams().getDefaultImportPath();
      if (Files.isDirectory(defaultImport)) {
        chooser.setInitialDirectory(defaultImport.toFile());
      } else {
        defaultImport = SpTool3Main.getRunTime().getImportDefaults().getCurrentDir();
        if (Files.isDirectory(defaultImport)) {
          chooser.setInitialDirectory(defaultImport.toFile());
        }
      }
      // directoryChooser: open dialogue
      File returnedDirectory = chooser.showDialog(SpTool3Main.getMainStage());
      // make sure thj returned directory is not null (e.g. user aborts choice)
      if (returnedDirectory != null) {
        importDefaults.setCurrentDir(Paths.get(returnedDirectory.toURI()));
        // Do not store or write now, let the follow up window handle that

        FxEntryFactory<Path> factory = new SimpleEntryFactory<>();
        BrowseAndListFilesDialog filesDialog = new BrowseAndListFilesDialog(
            importDefaults, factory);

        Optional<List<Path>> result = filesDialog.showAndWait();
        result.ifPresent(r -> startImportNewFiles(result.get(), SpTool3Main.getMainStage()));
      }
    });

    MenuItem recentLocationsMenu = new MenuItem("Recent folders");
    importMenu.getItems().add(recentLocationsMenu);
    recentLocationsMenu.setOnAction(e -> {

      RecentLocationsDialog dialog = new RecentLocationsDialog(
          SpTool3Main.getRunTime().getImportDefaults(),
          new RecentLocationsDialogEntryFactory());

      Optional<List<FxVisitedFile>> file = dialog.showAndWait();
      if (file.isPresent()) {
        ImportDefaults importDefaults = SpTool3Main.getRunTime().getImportDefaults();
        if (!file.get().isEmpty()) {
          importDefaults.setCurrentDir(file.get().get(0).getPlainVisitedFile().getPath());

          FxEntryFactory<Path> factory = new SimpleEntryFactory<>();
          BrowseAndListFilesDialog filesDialog = new BrowseAndListFilesDialog(
              importDefaults, factory);

          Optional<List<Path>> result = filesDialog.showAndWait();
          result.ifPresent(r -> startImportNewFiles(result.get(), SpTool3Main.getMainStage()));
        }
      }
    });

    MenuItem recentSetsMenu = new MenuItem("Recent files");
    importMenu.getItems().add(recentSetsMenu);
    recentSetsMenu.setOnAction(e -> {

      FxEntryFactory<FxFileSet> factory = new SingleFileSetEntryFactory();

      SingleFileSetDialog dialog = new SingleFileSetDialog(
          SpTool3Main.getRunTime().getImportDefaults(), factory);
      Optional<List<FxFileSet>> result = dialog.showAndWait();
      if (result.isPresent()) {
        List<FxFileSet> sets = result.get();
        if (!sets.isEmpty()) {
          FileSet set = sets.get(0).getPlainFileSet();
          this.startImportNewFiles(set.getFiles().stream().map(Path::of).collect(Collectors.toList()),
              SpTool3Main.getMainStage());
        }
      }
    });
    //---------------------------------------------------------------------------------------------

    MenuItem isotopeOverlapMenuItem = new MenuItem("List isobars");
    isotopeOverlapMenuItem.setOnAction(e -> UiUtil.showIsobaricInterferencePopup());

    MenuItem isotopeNumberMenuItem = new MenuItem("Isotope search");
    isotopeNumberMenuItem.setOnAction(e -> UiUtil.showIsotopePopup());

    MenuItem interferenceMenuItem = new MenuItem("Interference library");
    interferenceMenuItem.setOnAction(e -> UiUtil.showInterferencePopup());

    MenuItem sigTestMenu = new MenuItem("Significance test");
    sigTestMenu.setOnAction(e -> {

      SignificanceTester launcher = new SignificanceTester();
      launcher.show(); //non-blocking
    });

    MenuItem zAlphaConversionMenu = new MenuItem("z/alpha converter");
    zAlphaConversionMenu.setOnAction(e -> UiUtil.showZAlphaConversionPopup());

    MenuItem abundanceCalculatorMenu = new MenuItem("Abundance calculator");
    abundanceCalculatorMenu.setOnAction(e -> UiUtil.showIsotopeSignalConversionPopup());

    toolsMenu.getItems().addAll(zAlphaConversionMenu);
    toolsMenu.getItems().addAll(abundanceCalculatorMenu, new SeparatorMenuItem());
    toolsMenu.getItems().addAll(isotopeNumberMenuItem);
    if (SpTool3Main.SHOW_INTERFERENCE_DB) {
      toolsMenu.getItems().addAll(interferenceMenuItem);
    }
    toolsMenu.getItems().addAll(isotopeOverlapMenuItem);
    toolsMenu.getItems().addAll(new SeparatorMenuItem(), sigTestMenu);
    Actions.makeAlphaBetaMenu(toolsMenu);

    ////////

    MenuItem aboutMenuItem = new MenuItem("About");
    aboutMenu.getItems().add(aboutMenuItem);
    aboutMenuItem.setOnAction(evt -> {
      UiUtil.showAboutPopup();
      // startStressTest();
    });

    // Show the method name of whatever method is loaded first
    updateMethodMetaDataInUI();

    // Drag-Drop files into the UI
    mainBorderPane.setOnDragOver(event -> {
      if (event.getDragboard().hasFiles()) {
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      event.consume();
    });

    mainBorderPane.setOnDragDropped((
        DragEvent event) -> {
      tryBlock:
      try {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
          List<File> filesFromClipboard = db.getFiles();

          // no later null checks needed
          filesFromClipboard.removeIf(Objects::isNull);

          if (!filesFromClipboard.isEmpty()) {
            File importSource = filesFromClipboard.get(0);
            Path importFile = Path.of(importSource.toURI());

            // Check spTool system fil types first as these have clear indications what to do.
            if (PathUtil.getExtensionWithDot(importFile).equals(GlobalIO.METHOD_EXTENSION)) {
              if (Files.isReadable(importFile)) {
                try {
                  Method method = XmlUtil.readMethodFromXml(importFile);
                  if (method != null && !method.getSets().isEmpty()) {
                    methodView.setMethod(method);
                  }
                } catch (Exception e) {
                  LOGGER.info("Cannot read method file. Details: "
                      + ExceptionUtils.getStackTrace(e));
                }
              }
            } else if (PathUtil.getExtensionWithDot(importFile)
                .equals(GlobalIO.SERIALIZED_PROJECT_EXTENSION)) {
              if (Files.isReadable(importFile)) {
                try {
                  Serializer.loadSampleRegister(importSource.getAbsoluteFile());
                } catch (Exception e) {
                  LOGGER.info("Cannot read project file. Details: "
                      + ExceptionUtils.getStackTrace(e));
                }
              }
            } else {
              // SpTool3 custom files exhausted, now lets try nu import

              List<Path> nuFolders = new ArrayList<>();
              for (File fileFromClipboard : filesFromClipboard) {
                if (fileFromClipboard.isDirectory() &&
                    fileFromClipboard.canRead()) {

                  // "generate" the run.info file and check if it exists
                  File runInfoFile = new File(fileFromClipboard, "run.info");

                  // Check if folder is readable and contains readable "run.info" file
                  if (runInfoFile.exists() && runInfoFile.isFile() && runInfoFile.canRead()) {
                    nuFolders.add(fileFromClipboard.toPath());
                  }
                }
              }

              if (!nuFolders.isEmpty()) {
                // These lines are from the CSV reader and show how we remember previous places...
                Util.windowsSortFile(nuFolders);
                this.startImportNewFiles(nuFolders, SpTool3Main.getMainStage());
                // store the previous files
                List<Path> runInfoFiles = new ArrayList<>();
                for (Path nuFolder : nuFolders) {
                  // in the locations, we store previous folders, i.e., the NU folder in this case
                  SpTool3Main.getRunTime().getImportDefaults().addPreviousLocation(nuFolder.getParent());
                  // we must store the run.info file in prev files (I assume...)
                  File runInfoFile = new File(nuFolder.toFile(), "run.info");
                  runInfoFiles.add(runInfoFile.toPath());
                }
                SpTool3Main.getRunTime().getImportDefaults().addPreviousFileSet(new FileSet(runInfoFiles));
                SpTool3Main.getRunTime().storeImportDefaults();

                // Neither .spm, ... or NU folder -> try csv import
              } else {

                // Collect the paths
                List<Path> recursivePaths = new ArrayList<>();

                for (File fileFromClipboard : filesFromClipboard) {
                  // Check if Dir --> then list files.
                  if (fileFromClipboard.isDirectory() && fileFromClipboard.canRead()) {

                    // Check how deep to look
                    // int levels = Integer.MAX_VALUE; // else: levels =1 --> just the content
                    int levels = SpTool3Main.getRunTime().getConfParams().getDragDropFolderDepth().getValue();

                    // And look into the folder structure.
                    try {
                      Files.find(fileFromClipboard.toPath(),
                              levels,
                              (filePath, fileAttr) -> fileAttr.isRegularFile())
                          .forEach(recursivePaths::add);
                    } catch (IOException ioException) {
                      ioException.printStackTrace();
                    }

                    // Check if File --> then list it.
                  } else if (fileFromClipboard.isFile() && fileFromClipboard.canRead()) {
                    recursivePaths.add(fileFromClipboard.toPath());
                  }
                }

                // check file type
                String limitingType = SpTool3Main.getRunTime().getConfParams()
                    .getDragDropImportFileType().getValue();
                // Check if limit is given
                if (!limitingType.isEmpty()) {
                  recursivePaths.removeIf(p -> !PathUtil.getExtensionWithDot(p).equals(limitingType));
                }

                Util.windowsSortFile(recursivePaths);
                this.startImportNewFiles(recursivePaths, SpTool3Main.getMainStage());
                // store the previous files
                SpTool3Main.getRunTime().getImportDefaults().addPreviousFileSet(new FileSet(recursivePaths));
                SpTool3Main.getRunTime().storeImportDefaults();
              }
            }
          }
        }
        event.setDropCompleted(true);
      } catch (Exception e) {
        LOGGER.info("Drag/drop failed. Details: "
            + ExceptionUtils.getStackTrace(e));
      }
      event.consume();
    });

    // Field to identify ctl+A
    SpTool3Main.getMainStage().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
      public void handle(KeyEvent ke) {

        if (new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN).match(ke)) {
          if (SpTool3Main.getANALYZER()) {
            importMenu.show();
            ke.consume();
          }
        }
        if (new KeyCodeCombination(KeyCode.I, KeyCombination.ALT_DOWN).match(ke)) {
          if (SpTool3Main.getANALYZER()) {
            mainImportMenu.fire();
            ke.consume();
          }
        }

        if (new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN).match(ke)) {
          exportMenuItem.fire();
          ke.consume();
        }

        // "Generate"
        if (new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN).match(ke)) {
          executeMethodBtn.fire();
          ke.consume();
        }

        // "Run"
        if (new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN).match(ke)) {
          reprocessSamplesBtn.fire();
          ke.consume();
        }

        // "Open"
        if (new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN).match(ke)) {
          loadProject.fire();
          ke.consume();
        }

        // Save appearance
        if (new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN).match(ke)) {
          saveUiParameters.fire();
          ke.consume();
        }

        // Save project
        if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(ke)) {
          saveProject.fire();
          ke.consume();
        }

        // Save method
        if (new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN).match(ke)) {

          NotificationFactory.openYesNo("Do you want to save the method?",
              methodView::executeSave);
          ke.consume();
        }

        // Skim through samples
        if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN).match(ke)) {
          combinedSampleListAndSearchView.decrementSelectedSample();
          ke.consume();
        }

        // Skim through samples
        if (new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN).match(ke)) {
          combinedSampleListAndSearchView.incrementSelectedSample();
          ke.consume();
        }

        // refire last menu
        if (new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN).match(ke)) {
          MenuItem item = lastMenuItem.get();
          if (item != null) {
            item.fire();
          }
          ke.consume();
        }


      }
    });
  }


  /**
   * This is meant to be used, when the Config File has been changed externally and the only things
   * that needs to be done is updating the main window UI.
   */
  public void updateMethodMetaDataInUI() {
    String executeToolTip = """
        Execute the current method.
        - If the method contains, e.g., 'Monte Carlo data generator' or 'import' submethods,
        they will be executed and a new  sample will be created.
        - The sample will be processed with the processing submethods (baseline, ...)
        if there are such submethods in the current method.
        - This approach ignores the current selection of samples in the user interface.""";

    String processToolTip = """
        Process currently selected samples with the current method.
        - If the method contains any 'Monte Carlo data generator' or 'Import' submethods,
        they will be skipped.""";

    Method method = methodView.getCurrentMethod();
    if (method != null) {
      methodLabel.setText("Method: " + method.getLabelParam().getValue());
      UiUtil.tooltip(executeMethodBtn, executeToolTip + "\n\n\n" + method.getTooltip());
      UiUtil.tooltip(reprocessSamplesBtn, processToolTip + "\n\n\n" + method.getTooltip());

    } else {
      methodLabel.setText("Method loaded is invalid.");
      UiUtil.tooltip(executeMethodBtn, executeToolTip);
      UiUtil.tooltip(reprocessSamplesBtn, processToolTip);
    }
  }

  /**
   * This is meant to be used, when a change regarding the current method has been undertaken
   * externally (e.g., change of file, new method created) and the UI and config file need to be
   * adjusted.
   */
  public void notifyMethodChange(Method newMethod) {
    // stores in config
    if (newMethod != null) {
      if (newMethod.hasAssociatedFileOnDrive()) {
        SpTool3Main.getRunTime().getConfParams()
            .setCurrentMethodFile(newMethod.getAssociatedFile());
        LOGGER.info("Configuration: the current method was updated: "
            + newMethod.getLabelParam().getValue()
            + ". Writing new method path to config file...");
        SpTool3Main.getRunTime().getConfParams().executeOverridingSave();
      }
      // Update UI
      updateMethodMetaDataInUI();
    }
  }

  public MethodView getMethodView() {
    return methodView;
  }

  public BorderPane getMainViewerPane() {
    return mainViewerPane;
  }

  //
  public void startImportNewFiles(List<Path> files, @Nullable Stage parentStage) {
    if (!files.isEmpty()) {
      List<Task> importTasks = new ArrayList<>();

      boolean useMethodCSV = SpTool3Main.getRunTime().getConfParams().getUseMethodsCsvReader().getValue();

      Method current = null;
      if (useMethodCSV) {
        current = methodView.getCurrentMethod();
      }

      // Pre instantiate
      NuInterpreterParams nuParams = null;
      CsvInterpreterParams csvParams = null;

      for (int i = 0; i < files.size(); i++) {
        Path file = files.get(i);

        boolean isNuTof = Util.isNuPath(file);

        if (isNuTof) {
          /// NU TOF

          // Default: read from method
          if (nuParams == null && current != null) {
            nuParams = Util.getNuParametersFromMethod(current);
          }
          // If still null, try dialog. Dialog is only called once, then params!=null.
          if (nuParams == null) {
            nuParams = Util.getNuParametersFromDialog();
          }
          // check if !=null at last
          if (nuParams != null) {
            // make sure we pass the folder and not the file!
            file = Util.getCheckedNuPathDir(file);
            WorkingTask csv = new NuImportTask(nuParams, file, parentStage);
            importTasks.add(csv);
          } else {
            // else the window opens for every file
            break;
          }


        } else {
          /// CSV
          // Default: read from method
          if (csvParams == null && current != null) {
            csvParams = Util.getCSVParamsFromMethod(current);
          }
          // If still null, try dialog. Dialog is only called once, then params!=null.
          if (csvParams == null) {
            csvParams = Util.getCSVParamsFromDialog(files);
          }
          // check if !=null at last
          if (csvParams != null) {
            if (Util.isCsvLikeFile(file)) {
              WorkingTask csv = new CsvImportTask(csvParams, file);
              importTasks.add(csv);
            }
          } else {
            // else the window opens for every file
            break;
          }
        }
      }

      if (!importTasks.isEmpty()) {
        BatchTask linear = new SimpleLinearBatch<>("Raw data import", importTasks, false,
            () -> SpTool3Main.getRunTime().getSampleReg().createSetAndFlushWaitingList("Import"));
        SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(linear);
      } else {
        LOGGER.info("Import did not receive proper files or parameters to start. Are the files still " +
            "available on your system?");
      }
    }
  }


  public List<Boolean> startImportLoadAgain(List<Path> files, List<Method> methods,
                                            @Nullable Stage parentStage) {
    List<Boolean> successes = new ArrayList<>();

    List<Task> importTasks = new ArrayList<>();

    if (files.size() == methods.size()) {
      boolean success = false;

      for (int i = 0; i < files.size(); i++) {
        Path file = files.get(i);
        Method method = methods.get(i);

        boolean isNuTof = Util.isNuPath(file);

        if (isNuTof) {
          /// NU TOF
          NuInterpreterParams nuParams = Util.getNuParametersFromMethod(method);
          if (nuParams != null) {
            // make sure we pass the folder and not the file!
            file = Util.getCheckedNuPathDir(file);
            WorkingTask csv = new NuImportTask(nuParams, file, parentStage);
            importTasks.add(csv);
            success = true;
          }
        } else {
          /// CSV
          if (Util.isCsvLikeFile(file)) {
            CsvInterpreterParams csvParams = Util.getCSVParamsFromMethod(method);
            if (csvParams != null) {
              WorkingTask csv = new CsvImportTask(csvParams, file);
              importTasks.add(csv);
              success = true;
            }
          }
        }
        successes.add(success);
      }
    }

    if (!importTasks.isEmpty()) {
      BatchTask linear = new SimpleLinearBatch<>("Raw data import", importTasks, false,
          () -> SpTool3Main.getRunTime().getSampleReg().createSetAndFlushWaitingList("Import"));
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(linear);
    } else {
      LOGGER.info("Import did not receive proper files or parameters to start. Are the files still " +
          "available on your system?");
    }

    return successes;
  }

  public void startImportReplaceMZ(List<Sample> sampleRefs, List<Path> files,
                                   List<Method> methods, @Nullable Stage parentStage) {
    List<Task> importTasks = new ArrayList<>();

    if (files.size() == methods.size() && files.size() == sampleRefs.size()) {
      boolean success = false;

      for (int i = 0; i < files.size(); i++) {
        Path file = files.get(i);
        Method method = methods.get(i);

        boolean isNuTof = Util.isNuPath(file);

        if (isNuTof) {
          /// NU TOF
          NuInterpreterParams nuParams = Util.getNuParametersFromMethod(method);
          if (nuParams != null) {
            // make sure we pass the folder and not the file!
            file = Util.getCheckedNuPathDir(file);
            Sample existingSample = sampleRefs.get(i);
            WorkingTask csv = new NuImportTask(existingSample, nuParams, file, parentStage);
            importTasks.add(csv);
          }
        } else {
          /// CSV
          if (Util.isCsvLikeFile(file)) {
            LOGGER.info("This options is meant for TOF data only.");
          }
        }
      }
    }

    if (!importTasks.isEmpty()) {
      BatchTask linear = new SimpleLinearBatch<>("Raw data import", importTasks, false,
          EmptyTaskResult::new);
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(linear);
    } else {
      LOGGER.info("Import did not receive proper files or parameters to start. Are the files still " +
          "available on your system?");
    }
  }


  public void enableDividerPositions() {
    // Initial positions
    sampleChooserSplitPane.setDividerPositions(SpTool3Main.getRunTime().getGuiParameterManager()
        .getLayoutParameters().getBottomSliders());

    /*
    There seems to be a "Bug" in javaFx that does not set the expected divider positions when resizing
    the window. There is always some space left after the final divider as if the layout for the
    split pane was calculated earlier, when the window was still smaller than, e.g., after maximizing.
    This workaround makes sure to set the FINAL divider always at maximum position.
    When it already is there, nothing happens (to avoid endless looping)
    UPDATE: Wrong thought: There should not be any divider at the very right... This was only
    an issue, since no Population list view is showing at the moment of writing this code.

    for (Divider divider : sampleChooserSplitPane.getDividers()) {
      divider.positionProperty().addListener((observable, oldValue, newValue) -> {
        List<Divider> dividers = sampleChooserSplitPane.getDividers();
        if (dividers.get(dividers.size() - 1).getPosition() < 1) {
          GuiParameterManager ui = SpTool3Main.getRunTime().getGuiParameterManager();
          UiLayoutParameters uiPar = ui.getLayoutParameters();
          double[] sliders = uiPar.getBottomSliders();
          sampleChooserSplitPane.setDividerPositions(sliders);
        }
      });
    }
     */
  }

  public void updateSampleSets() {
    Platform.runLater(() -> combinedSampleListAndSearchView.filterSampleSets());
  }

  public void updatePopulations() {
    Platform.runLater(() -> {
      combinedSampleListAndSearchView.fillAndReselectPopulations();
    });
  }


  public void updateIsotopes() {
    Platform.runLater(() -> {
      combinedSampleListAndSearchView.fireSampleChange();
    });
  }

  public List<Sample> getSelSamples() {
    List<Sample> sel = sampleTableView.getSelectionModel().getSelectedItems()
        .stream()
        .map(FxSample::getPlainSample)
        .collect(Collectors.toList());
    return sel;
  }

  @Nullable
  public SampleSet getSelSampleSet() {
    SampleSet sel = sampleSetListView.getSelectionModel().getSelectedItem().unwrap();
    return sel;
  }

  public void selectFirstSampleSet() {
    if (sampleSetListView.getSelectionModel().getSelectedItems().isEmpty()) {
      sampleSetListView.getSelectionModel().selectFirst();
    }
  }

  public List<Sample> getSamples() {
    List<Sample> sel = sampleTableView.getItems()
        .stream()
        .map(FxSample::getPlainSample)
        .collect(Collectors.toList());
    return sel;
  }

  public List<Isotope> getSelIsotopes() {
    List<Isotope> sel = new ArrayList<>(isotopeTableView.getSelectionModel().getSelectedItems());
    return sel;
  }

  public List<Isotope> getAllIsotopes() {
    List<Isotope> sel = new ArrayList<>(isotopeTableView.getItems());
    return sel;
  }

  public List<PopulationID> getSelPops() {
    List<PopulationID> sel = new ArrayList<>(
        populationView.getSelectionModel().getSelectedItems());
    return sel;
  }

  public Unit getUnit() {
    return mainUnitComboBox.getSelectionModel().getSelectedItem().getUnit();
  }

  private void startStressTest() {
    // STRESS TEST: randomly select isotopes to trigger refreshGraph() repeatedly
    // Remove this block after confirming the NPE fix holds
    Thread stressThread = new Thread(() -> {
      long endTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes
      java.util.Random random = new java.util.Random();

      while (System.currentTimeMillis() < endTime) {
        try {
          // Random delay between 5ms and 1000ms
          int delay = 5 + random.nextInt(996);
          Thread.sleep(delay);

          Platform.runLater(() -> {
            try {
              int itemCount = isotopeTableView.getItems().size();
              if (itemCount == 0) return;

              // Randomly choose single or multi selection
              boolean multiSelect = random.nextBoolean();
              isotopeTableView.getSelectionModel().clearSelection();

              if (multiSelect) {
                // Select 1 to min(5, itemCount) random isotopes
                int count = 1 + random.nextInt(Math.min(5, itemCount));
                for (int i = 0; i < count; i++) {
                  int idx = random.nextInt(itemCount);
                  isotopeTableView.getSelectionModel().select(idx);
                }
              } else {
                // Single selection
                int idx = random.nextInt(itemCount);
                isotopeTableView.getSelectionModel().select(idx);
              }
            } catch (Exception ex) {
              LOGGER.warn("Stress test selection failed: " + ex.getMessage());
            }
          });

        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
      LOGGER.info("Stress test complete.");
    });

    stressThread.setDaemon(true); // don't block app shutdown
    stressThread.setName("isotope-stress-test");
    stressThread.start();

    Thread stressThread2 = new Thread(() -> {
      long endTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes
      java.util.Random random = new java.util.Random();

      while (System.currentTimeMillis() < endTime) {
        try {
          // Random delay between 5ms and 1000ms
          int delay = 5 + random.nextInt(496);
          Thread.sleep(delay);

          Platform.runLater(() -> {
            try {
              int itemCount = populationView.getItems().size();
              if (itemCount == 0) return;

              // Randomly choose single or multi selection
              boolean multiSelect = random.nextBoolean();
              populationView.getSelectionModel().clearSelection();

              if (multiSelect) {
                // Select 1 to min(5, itemCount) random isotopes
                int count = 1 + random.nextInt(Math.min(5, itemCount));
                for (int i = 0; i < count; i++) {
                  int idx = random.nextInt(itemCount);
                  populationView.getSelectionModel().select(idx);
                }
              } else {
                // Single selection
                int idx = random.nextInt(itemCount);
                populationView.getSelectionModel().select(idx);
              }
            } catch (Exception ex) {
              LOGGER.warn("Stress test selection failed: " + ex.getMessage());
            }
          });

        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
      LOGGER.info("Stress test complete.");
    });

    stressThread2.setDaemon(true); // don't block app shutdown
    stressThread2.setName("isotope-stress-test");
    stressThread2.start();
  }
}

