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

import analysis.AlphaBetaEvaluation;
import analysis.Event;
import analysis.PopulationID;
import core.SpTool3Main;
import dataModelNew.IncompleteSample;
import dataModelNew.LPCParticleMatrix;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.mz.Channel;
import gui.dialog.DialogUtil;
import gui.dialog.FxStage;
import gui.dialog.FxStageButton;
import gui.dialog.notification.NotificationFactory;
import gui.table.TableUtils;
import gui.util.UiUtil;
import io.GlobalIO;
import io.PathUtil;
import io.export.BlockCollection;
import io.export.BlockCollectionHorizontal;
import io.export.ClipboardWriter;
import io.export.CsvExportWriter;
import io.export.DataExport;
import io.export.ExportWriter;
import io.export.TabularBlock;
import io.export.TabularSheet;
import io.fastExport.TabBlock;
import io.fastExport.TabBlockColl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import io.fastExport.TabCol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import math.stat.sigTest.EMD;
import math.units.enums.NMPUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.*;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.ExporterParams;
import processing.parameterSets.impl.ExporterParams.ExportTarget;
import processing.parameterSets.impl.NormalSearchParams;
import processing.parameters.FxParameter;
import sandbox.montecarlo.Isotope;
import tasks.BatchTask;
import tasks.batch.SimpleLinearBatch;
import tasks.results.EmptyTaskResult;
import tasks.results.FunctionalTaskResult;
import tasks.single.FunctionalTask;
import util.*;
import visualizer.ResultTableData;
import visualizer.ResultsTable;

import static math.stat.MeasureOfLocation.MEAN;
import static math.stat.MeasureOfSpread.SD;

public class ExportViewController implements ParameterView, FxStage, Hotkeyable {

  private static final Logger LOGGER = LogManager
      .getLogger(ExportViewController.class.getName());

  protected final Stage stage;
  @FXML
  protected BorderPane borderPane;

  private final FxStageButton fxMainButton;
  private final FxStageButton fxSecondaryButton;
  protected final ToolBar topToolbar;

  // Export Buttons
  private final Button exportRawDataBtn = new Button("Export raw data");
  private final Button exportPValuesBtn = new Button("Export p values");
  private final Button exportPrecRecallBtn = new Button("Export precision/recall");
  private final Button exportEMDBtn = new Button("Export EMD comparison");
  private final Button exportExpectedValuesBtn = new Button("Export expected values");
  private final Button exportPopulationsBtn = new Button("Export event data");
  private final Button exportCustomEventDataBtn = new Button("Export custom event data");
  private final Button exportMethodBtn = new Button("Export method as csv");
  private final Button exportMethodSpmBtn = new Button("Export method as spm");
  private final Button exportResultsTable = new Button("Export results table");
  private final Button exportRegionSpectra = new Button("Export region spectra");

  protected final ExporterParams exportParamSet;
  protected final FxParamSet fxParamSet;

  // Keep one listview to prevent resetting position for each time we update anything.
  private ListView<FxParameter<?>> view = null;


  public ExportViewController(ExporterParams ps, Stage stage) {

    // This should be a copy in order to make sure that changes are only passed when calling SAVE
    this.exportParamSet = (ExporterParams) ps.getCopyWithPreviousDateFileAndID();

    // raw data export
    exportParamSet.setRawExportButton((SupplierSerializable<Button>) () -> exportRawDataBtn);

    exportParamSet.setEMDExportButton((SupplierSerializable<Button>) () -> exportEMDBtn);

    // custom event parameter export
    exportParamSet.setCustomEventExportButton((SupplierSerializable<Button>) () -> exportCustomEventDataBtn);

    Path currPath = Paths.get(this.exportParamSet.getCurrentExportPath().getValue());
    if (!Files.isDirectory(currPath)) {
      Path defPath = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
      this.exportParamSet.getCurrentExportPath().setValue(defPath.toString());
    }

    // tooltips:
    UiUtil.tooltip(exportPopulationsBtn,
        """
            Export values for each event including height, area, width, ...""");

    UiUtil.tooltip(exportExpectedValuesBtn,
        """
            For the in-silico data generator: Export expected values of central parameters.
            These values represent the expected outcome of an analysis under ideal conditions.""");

    UiUtil.tooltip(exportPrecRecallBtn,
        """
            Export precision recall curves to compare the results of a data evaluation method
            with the expected values from the in-silico data.""");

    UiUtil.tooltip(exportPValuesBtn,
        """
            Export the p-value matrix.""");

    UiUtil.tooltip(exportMethodSpmBtn,
        """
            Export the method as a .spm file that can be re-loaded later.""");

    UiUtil.tooltip(exportMethodBtn,
        """
            Export the method as a human-readable .csv file that can be loaded into MS excel.""");

    UiUtil.tooltip(exportResultsTable,
        """
            Export the results table as a human-readable .csv file that can be loaded into MS excel.""");

    UiUtil.tooltip(exportRegionSpectra,
        """
            Export mass spectrum of an aligned population.""");


    this.fxParamSet = this.exportParamSet.getObservableInstance();

    this.stage = stage;

    this.fxMainButton = FxStageButton.SAVE;
    this.fxSecondaryButton = FxStageButton.CANCEL;

    this.topToolbar = new ToolBar();
  }

  // After constructor call and building of pane this method is called and it can fill Views with Lists, ...
  public void initialize() {

    stage.setWidth(700);
    stage.setHeight(550);

    // Else: "x" does not trigger any of the "goodbye" methods
    stage.setOnCloseRequest(event -> closeAndCancelChanges());

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

    borderPane.setTop(topToolbar);

    // Bottom Buttons
    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().add(fxMainButton.getBold(this));
    buttonBar.getButtons().add(fxSecondaryButton.get(this));
    borderPane.setBottom(UiUtil.putOnAnchorWithInsets(buttonBar));

    // Export buttons
    List<Node> buttonBoxItems = new ArrayList<>();
    Label mainBtnLbl = new Label("Exporters without settings");
    mainBtnLbl.setStyle("-fx-font-weight: bold");
    mainBtnLbl.setPrefWidth(180);
    buttonBoxItems.add(mainBtnLbl);
    buttonBoxItems.add(exportPopulationsBtn);
    buttonBoxItems.add(exportResultsTable);
    buttonBoxItems.add(new Separator(Orientation.HORIZONTAL));
    buttonBoxItems.add(exportExpectedValuesBtn);
    if (SpTool3Main.SHOW_PVALUE_EXPORT) {
      buttonBoxItems.add(new Separator(Orientation.HORIZONTAL));
      buttonBoxItems.add(exportPValuesBtn);
      buttonBoxItems.add(exportRegionSpectra);
    }
    if (SpTool3Main.SHOW_PRECISION_RECALL_EXPORT) {
      buttonBoxItems.add(new Separator(Orientation.HORIZONTAL));
      buttonBoxItems.add(exportPrecRecallBtn);
    }
    buttonBoxItems.add(new Separator(Orientation.HORIZONTAL));
    buttonBoxItems.add(exportMethodBtn);
    buttonBoxItems.add(exportMethodSpmBtn);

    final VBox btnBox = new VBox(10);
    btnBox.getChildren().addAll(buttonBoxItems);

    btnBox.getChildren().stream()
        .filter(n -> n instanceof Button)
        .map(n -> (Control) n)
        .peek(c -> c.setStyle("-fx-font-weight: bold"))
        .forEach(c -> c.setPrefWidth(150));

    borderPane.setRight(UiUtil.putOnAnchorWithInsets(btnBox));

    exportRawDataBtn.setOnAction(e -> {
      // THE HEAVY LIFTING (Note: We could parallelize this,
      // but I doubt it helps much as the bottle neck may be drive writing speed
      // or getting the data out from memory on to the drive.

      FunctionalTask task = new FunctionalTask("Export",
          () -> {

            final double height = exportParamSet.getSpCalHeightMarker();

            int i = 0;
            List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
            List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
            List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

            List<SampleImpl> samples = selSamples.stream()
                .map(Sample::getAllSamples)
                .flatMap(List::stream)
                .filter(s -> s instanceof SampleImpl)
                .map(s -> (SampleImpl) s)
                .toList();

            if (!samples.isEmpty()) {

              for (SampleImpl sample : samples) {

                i++;

                String sampleName = sample.getNickName();
                sampleName = GlobalIO.cleanupWindowsFileName(sampleName);
                String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
                sampleName = dateTag + "_Raw_" + i + "_" + sampleName;

                ExportWriter writer = new ClipboardWriter();
                if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
                  String pathStr = exportParamSet.getCurrentExportPath().getValue();
                  Path path = Paths.get(pathStr);
                  path = PathUtil.addDir(path, "Data");
                  PathUtil.createDir(path);
                  if (Files.isDirectory(path)) {
                    path = path.resolve(sampleName + ".csv");
                    File file = path.toFile();
                    writer = new CsvExportWriter(file);
                  }
                }

                TabBlockColl coll = new TabBlockColl(writer, false);
                List<TabBlock> blocks = DataExport.extractRawData(sample, selChannels, selPops,
                    height,
                    exportParamSet.isSelectedIsotopesForRaw(),
                    exportParamSet.getIncludePopulationMarkers().getValue(),
                    exportParamSet.getSpCalCompatible().getValue(),
                    exportParamSet.getIncludeEventMarkers().getValue());
                blocks.forEach(coll::add);

                if (!exportParamSet.getSpCalCompatible().getValue()) {
                  coll.write(DataExport.getShortMeta(sample));
                }

                coll.export();
              }
            }

          }, new FunctionalTaskResult(() -> {
        Platform.runLater(() -> {
          NotificationFactory.openAutocloseInfo("Export finished");
        });
      }));

      BatchTask parallel = new SimpleLinearBatch<>("Export",
          task, false, new EmptyTaskResult());
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);

    });


    exportResultsTable.setOnAction(e -> {
      FunctionalTask task = new FunctionalTask("Export",
          () -> {

            // Check which type of events to show?
            List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
            List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
            List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
            if (samples != null && !samples.isEmpty() && selChannels != null && !selChannels.isEmpty()) {

              ResultTableData tableData = new ResultTableData(samples, selChannels, selPops, true);

              TableView<ResultsTable.TableRowData> table =
                  ResultsTable.buildNestedTable(tableData.getEntryMap());

              ExportWriter writer = new ClipboardWriter();
              if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
                String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
                String pathStr = exportParamSet.getCurrentExportPath().getValue();
                Path path = Paths.get(pathStr);
                path = PathUtil.addDir(path, "Data");
                PathUtil.createDir(path);
                if (Files.isDirectory(path)) {
                  path = path.resolve(dateTag + "ResultsTable" + ".csv");
                  File file = path.toFile();
                  writer = new CsvExportWriter(file);
                }
              }

              writer.writeLine(DataExport.getShortMeta(null));
              TableUtils.exportNestedTableViewToCsv(table, writer);
              writer.close();

            }
          }, new FunctionalTaskResult(() -> {
        Platform.runLater(() -> {
          NotificationFactory.openAutocloseInfo("Export finished");
        });
      }));

      BatchTask parallel = new SimpleLinearBatch<>("Export",
          task, false, new EmptyTaskResult());
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
    });

    exportPValuesBtn.setOnAction(e -> {

      FunctionalTask task = new FunctionalTask("Export",
          () -> {

            List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
            List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

            List<SampleImpl> samples = selSamples.stream()
                .map(Sample::getAllSamples)
                .flatMap(List::stream)
                .filter(s -> s instanceof SampleImpl)
                .map(s -> (SampleImpl) s)
                .toList();

            Method currentMethod =
                SpTool3Main.getRunTime().getMainWindowCtl().getMethodView().getCurrentMethod();
            NormalSearchParams params = null;
            for (ParamSet set : currentMethod.getSets()) {
              if (set instanceof NormalSearchParams) {
                if (((NormalSearchParams) set).getSearchAlgorithm().getValue().equals(SearchAlgorithm.P_VALUE_ACCUMULATION)) {
                  params = (NormalSearchParams) set;
                }
              }
            }

            if (params == null) {
              LOGGER.error("No Search parameters with P value search strategy were found in the current " +
                  "method.");
            }

            if (!samples.isEmpty() && params != null) {

              int i = 0; // increment counter
              for (SampleImpl sample : samples) {

                i++;

                String sampleName = sample.getNickName();
                sampleName = GlobalIO.cleanupWindowsFileName(sampleName);
                String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
                sampleName = dateTag + "_PVal_" + i + "_" + sampleName;

                ExportWriter writer = new ClipboardWriter();
                if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
                  String pathStr = exportParamSet.getCurrentExportPath().getValue();
                  Path path = Paths.get(pathStr);
                  path = PathUtil.addDir(path, "Data");
                  PathUtil.createDir(path);
                  if (Files.isDirectory(path)) {
                    path = path.resolve(sampleName + ".csv");
                    File file = path.toFile();
                    writer = new CsvExportWriter(file);
                  }
                }

                DataExport.exportPValues(sample, selChannels, params, writer);
              }
            }

          }, new FunctionalTaskResult(() -> {
        Platform.runLater(() -> {
          NotificationFactory.openAutocloseInfo("Export finished");
        });
      }));

      BatchTask parallel = new SimpleLinearBatch<>("Export",
          task, false, new EmptyTaskResult());
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);

    });

    exportRegionSpectra.setOnAction(e -> {


      FunctionalTask task = new FunctionalTask("Export",
          () -> {

            List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
            List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

            int i = 0; // increment counter
            for (Sample sample : selSamples) {

              i++;

              String sampleName = sample.getNickName();
              sampleName = GlobalIO.cleanupWindowsFileName(sampleName);
              String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
              sampleName = dateTag + "_Spectra_" + i + "_" + sampleName;

              if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
                String pathStr = exportParamSet.getCurrentExportPath().getValue();
                Path path = Paths.get(pathStr);
                path = PathUtil.addDir(path, "Data", sampleName);
                PathUtil.createDir(path);

                DataExport.exportRegionSpectra(sample, selPops, path);
              }

            }

            // Check if we also export as lines for MIA
            List<Sample> lpcSamples = new ArrayList<>();
            for (Sample selSample : selSamples) {
              if (selSample instanceof IncompleteSample) {
                if (((IncompleteSample) selSample).getMatrix() instanceof LPCParticleMatrix) {
                  lpcSamples.add(selSample);
                }
              }
            }
            if (!lpcSamples.isEmpty()) {
              if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
                String pathStr = exportParamSet.getCurrentExportPath().getValue();
                Path path = Paths.get(pathStr);
                String time = Util.getYearMonthDateDayHourMinuteSecond();
                path = PathUtil.addDir(path, "Data", time + "_LPC");
                PathUtil.createDir(path);
                DataExport.exportLpcAsLines(lpcSamples, path);
              }
            }

          }, new FunctionalTaskResult(() -> {
        Platform.runLater(() -> {
          NotificationFactory.openAutocloseInfo("Export finished");
        });
      }));

      BatchTask parallel = new SimpleLinearBatch<>("Export",
          task, false, new EmptyTaskResult());
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);

    });


    exportPrecRecallBtn.setOnAction(e -> {

      List<PopulationID> selIDs = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

      List<PopulationID> simIDs = selIDs.stream()
          .filter(id -> id.getType().equals(PopulationType.SIMULATION))
          .toList();

      List<PopulationID> evalIDs = new ArrayList<>(selIDs);
      evalIDs.removeIf(id -> id.getType().equals(PopulationType.SIMULATION));

      List<String> sampleNames = new ArrayList<>();
      List<String> isotopeNames = new ArrayList<>();
      List<String> popIDNames = new ArrayList<>();
      List<String> precisions = new ArrayList<>();
      List<String> recalls = new ArrayList<>();

      HashMap<Key3, List<Double>> averagePrecision = new LinkedHashMap<>();
      HashMap<Key3, List<Double>> averageRecall = new LinkedHashMap<>();
      HashMap<Key3, List<Double>> averageFScore = new LinkedHashMap<>();

      if (!simIDs.isEmpty() && !evalIDs.isEmpty()) {

        FunctionalTask task = new FunctionalTask("Export",
            () -> {

              for (Sample selSample : selSamples) {

                for (Channel selChannel : selChannels) {
                  List<Event> simEvts = selSample.getNPEvents(selChannel, simIDs.get(0));

                  for (PopulationID evalID : evalIDs) {

                    sampleNames.add(selSample.getNickName());
                    isotopeNames.add(selChannel.getUIString());
                    popIDNames.add(evalID.toString());

                    List<Event> evalEvts = selSample.getNPEvents(selChannel, evalID);

                    List<Event> matchFromSim = new ArrayList<>();
                    List<Event> matchFromEval = new ArrayList<>();
                    List<Event> noMatchSim = new ArrayList<>();
                    List<Event> noMatchEval = new ArrayList<>();

                    AlphaBetaEvaluation.checkEvents(simEvts, evalEvts, matchFromSim, matchFromEval,
                        noMatchSim,
                        noMatchEval);

                    double tp = matchFromEval.size();
                    double fp = noMatchEval.size();
                    double fn = noMatchSim.size();

                    double precision;
                    if (tp + fp > 0) {
                      precision = tp / (tp + fp);
                    } else {
                      precision = 1; // When tp+fn=0 --> tp=0 --> "0/0" is a correct assignment aka
                      // "nothing found"
                    }

                    double recall;
                    if (tp + fn > 0) {
                      recall = tp / (tp + fn);
                    } else {
                      recall = 1; // When tp+fn=0 --> tp=0 --> "0/0" is a correct assignment aka "nothing
                      // found"
                    }


                    precisions.add(SnF.doubleToString(precision, NF.D1C6));
                    recalls.add(SnF.doubleToString(recall, NF.D1C6));

                    Key3 key = new Key3(selSample.getNickName(),
                        selChannel.getUIString(),
                        evalID.toString());
                    if (!averagePrecision.containsKey(key)) {
                      averagePrecision.put(key, new ArrayList<>());
                    }
                    if (!averageRecall.containsKey(key)) {
                      averageRecall.put(key, new ArrayList<>());
                    }
                    if (!averageFScore.containsKey(key)) {
                      averageFScore.put(key, new ArrayList<>());
                    }

                    averagePrecision.get(key).add(precision);
                    averageRecall.get(key).add(recall);
                    averageFScore.get(key).add(2 * precision * recall / (precision + recall));
                  }
                }
              }

              ExportWriter writer = new ClipboardWriter();
              if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
                String pathStr = exportParamSet.getCurrentExportPath().getValue();
                Path path = Paths.get(pathStr);
                path = PathUtil.addDir(path, "Data");
                PathUtil.createDir(path);
                if (Files.isDirectory(path)) {
                  String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
                  path = path.resolve(dateTag + "precisionRecall" + ".csv");
                  File file = path.toFile();
                  writer = new CsvExportWriter(file);
                }
              }

              TabBlockColl coll = new TabBlockColl(writer, true);
              coll.write(DataExport.getShortMeta(null));

              TabBlock rawBlock = new TabBlock();

              rawBlock.addCol(new TabCol("Individual samples", "Sample",
                  ArrUtils.stringListToArr(sampleNames)));
              rawBlock.addCol(new TabCol("Isotope", ArrUtils.stringListToArr(isotopeNames)));
              rawBlock.addCol(new TabCol("Population", ArrUtils.stringListToArr(popIDNames)));
              rawBlock.addCol(new TabCol("Precision", ArrUtils.stringListToArr(precisions)));
              rawBlock.addCol(new TabCol("Recall", ArrUtils.stringListToArr(recalls)));

              coll.add(rawBlock);

              ///

              TabBlock averageBlock = new TabBlock();
              List<String> meanPrec = new ArrayList<>();
              List<String> sdPrec = new ArrayList<>();

              List<String> meanRec = new ArrayList<>();
              List<String> sdRec = new ArrayList<>();

              List<String> meanF = new ArrayList<>();
              List<String> sdF = new ArrayList<>();

              List<String> averageSampleNames = new ArrayList<>();
              List<String> averageIsotopeNames = new ArrayList<>();
              List<String> averagePopIDNames = new ArrayList<>();

              for (Key3 key : averagePrecision.keySet()) {
                averageSampleNames.add(key.s1);
                averageIsotopeNames.add(key.s2);
                averagePopIDNames.add(key.s3);

                meanPrec.add(SnF.doubleToString(MEAN.calc(averagePrecision.get(key)), NF.D1C6));
                sdPrec.add(SnF.doubleToString(SD.calc(averagePrecision.get(key)), NF.D1C6));

                meanRec.add(SnF.doubleToString(MEAN.calc(averageRecall.get(key)), NF.D1C6));
                sdRec.add(SnF.doubleToString(SD.calc(averageRecall.get(key)), NF.D1C6));

                meanF.add(SnF.doubleToString(MEAN.calc(averageFScore.get(key)), NF.D1C6));
                sdF.add(SnF.doubleToString(SD.calc(averageFScore.get(key)), NF.D1C6));
              }

              averageBlock.addCol(new TabCol("Averages", "Samples",
                  ArrUtils.stringListToArr(averageSampleNames)));
              averageBlock.addCol(new TabCol("Isotope", ArrUtils.stringListToArr(averageIsotopeNames)));
              averageBlock.addCol(new TabCol("Population", ArrUtils.stringListToArr(averagePopIDNames)));
              averageBlock.addCol(new TabCol("Precision mean", ArrUtils.stringListToArr(meanPrec)));
              averageBlock.addCol(new TabCol("Precision SD", ArrUtils.stringListToArr(sdPrec)));
              averageBlock.addCol(new TabCol("Recall mean", ArrUtils.stringListToArr(meanRec)));
              averageBlock.addCol(new TabCol("Recall SD", ArrUtils.stringListToArr(sdRec)));
              averageBlock.addCol(new TabCol("F score mean", ArrUtils.stringListToArr(meanRec)));
              averageBlock.addCol(new TabCol("F score SD", ArrUtils.stringListToArr(sdRec)));

              coll.add(averageBlock);

              coll.export();
            }, new FunctionalTaskResult(() -> {
          Platform.runLater(() -> {
            NotificationFactory.openAutocloseInfo("Export finished");
          });
        }));

        BatchTask parallel = new SimpleLinearBatch<>("Export",
            task, false, new EmptyTaskResult());
        SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
      }

    });


    exportEMDBtn.setOnAction(e -> {

      FunctionalTask task = new FunctionalTask("Export",
          () -> {

            List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
            List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
            List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

            EventParameter eventParameter = exportParamSet.getEmdEventParameter().getValue();
            MathMod mathMod = exportParamSet.getEmdMathParameter().getValue();

            if (!samples.isEmpty() && !selChannels.isEmpty() && !selPops.isEmpty()) {

              ExportWriter writer = new ClipboardWriter();
              if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
                String pathStr = exportParamSet.getCurrentExportPath().getValue();
                Path path = Paths.get(pathStr);
                path = PathUtil.addDir(path, "Data");
                PathUtil.createDir(path);
                if (Files.isDirectory(path)) {
                  String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
                  path = path.resolve(dateTag + "_EmdMetric" + ".csv");
                  File file = path.toFile();
                  writer = new CsvExportWriter(file);
                }
              }

              // prepare export
              TabBlockColl coll = new TabBlockColl(writer, true);
              coll.write(DataExport.getShortMeta(null));

              for (Channel channel : selChannels) {

                List<String> sampleCol1 = new ArrayList<>();
                List<String> populationCol1 = new ArrayList<>();
                List<String> sampleCol2 = new ArrayList<>();
                List<String> populationCol2 = new ArrayList<>();
                List<String> isotopeCol = new ArrayList<>();

                for (Sample sample1 : samples) {
                  for (PopulationID populationID1 : selPops) {
                    EventType eventType = EventType.NP;
                    // Default way to retrieve data.
                    double[] data1 = sample1.getData(channel, populationID1, eventType, eventParameter);
                    // check math operations
                    data1 = mathMod.calc(data1);

                    for (Sample sample2 : samples) {
                      for (PopulationID populationID2 : selPops) {
                        // Default way to retrieve data.
                        double[] data2 = sample2.getData(channel, populationID2, eventType,
                            eventParameter);
                        // check math operations
                        data2 = mathMod.calc(data2);

                        sampleCol1.add("[" + samples.indexOf(sample1) + "] " + sample1.getNickName());
                        populationCol1.add("[" + selPops.indexOf(populationID1) + "] " + populationID1.toString());
                        sampleCol2.add("[" + samples.indexOf(sample2) + "] " + sample2.getNickName());
                        populationCol2.add("[" + selPops.indexOf(populationID2) + "] " + populationID2.toString());

                        double emd = EMD.similarity(data1, data2);
                        isotopeCol.add(SnF.doubleToString(emd, NF.D1C6));

                      }
                    }
                  }
                }

                // Add Block for each isotope
                TabBlock block = new TabBlock();
                block.addCol(new TabCol("Sample A", ArrUtils.stringListToArr(sampleCol1)));
                block.addCol(new TabCol("Population A", ArrUtils.stringListToArr(populationCol1)));

                block.addCol(new TabCol("Sample B", ArrUtils.stringListToArr(sampleCol2)));
                block.addCol(new TabCol("Population B", ArrUtils.stringListToArr(populationCol2)));

                block.addCol(new TabCol("Similarity [0-1]", channel.getUIString(),
                    ArrUtils.stringListToArr(isotopeCol)));

                coll.add(block);
              }

              // Finish export
              coll.export();
            }

          }, new FunctionalTaskResult(() -> {
      }));

      BatchTask parallel = new SimpleLinearBatch<>("Export",
          task, false, new EmptyTaskResult());
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);

    });

    exportPopulationsBtn.setOnAction(e -> {
      int i = 0;
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      // At least export the simulation...
      if (selPops.isEmpty()) {
        selPops.add(new PopulationID(PopulationType.SIMULATION));
      }

      if (!samples.isEmpty()) {

        for (Sample sample : samples) {
          i++;

          String sampleName = sample.getNickName();
          sampleName = GlobalIO.cleanupWindowsFileName(sampleName);
          String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
          sampleName = dateTag + "_Events_" + i + "_" + sampleName;

          ExportWriter writer = new ClipboardWriter();
          if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
            String pathStr = exportParamSet.getCurrentExportPath().getValue();
            Path path = Paths.get(pathStr);
            path = PathUtil.addDir(path, "Data");
            PathUtil.createDir(path);
            if (Files.isDirectory(path)) {
              path = path.resolve(sampleName + ".csv");
              File file = path.toFile();
              writer = new CsvExportWriter(file);
            }
          }

          TabularSheet sheet = new TabularSheet(writer);
          BlockCollection hColl = new BlockCollectionHorizontal();

          // Now check what to export
          for (PopulationID id : selPops) {
            List<TabularBlock> blocks;
            if (id.getType().equals(PopulationType.SIMULATION)) {
              blocks = DataExport.extractSyntheticPopulationData(sample, id);
            } else {
              blocks = new ArrayList<>();
              for (Channel channel : selChannels) {
                blocks.addAll(DataExport.extractPopulationData(sample, channel, id));
              }
            }
            blocks.forEach(hColl::addBlock);
          }
          sheet.addBlockCollection(hColl);
          sheet.getWriter().writeLine(DataExport.getShortMeta(sample));
          sheet.export();
        }
      }

    });

    exportCustomEventDataBtn.setOnAction(e -> {
      int i = 0;
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      boolean exportBG = exportParamSet.getExportBackgroundData().getValue();
      boolean applyJitter = exportParamSet.getApplyJitterSampling().getValue();
      int noOfBgDP = exportParamSet.getJitterDataPoints().getValue();
      boolean exportNP = exportParamSet.getExportParticleData().getValue();
      EventParameter npPar = exportParamSet.getParticleEventParameter().getValue();
      NMPUnit NMPUnit = exportParamSet.getParticleUnitParameter().getValue();
      if (!samples.isEmpty() && !selChannels.isEmpty() && !selPops.isEmpty()) {
        String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
        String fileName = dateTag + "EventData";
        fileName = GlobalIO.cleanupWindowsFileName(fileName);


        ExportWriter writer = new ClipboardWriter();
        if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
          String pathStr = exportParamSet.getCurrentExportPath().getValue();
          Path path = Paths.get(pathStr);
          path = PathUtil.addDir(path, "Data");
          PathUtil.createDir(path);
          if (Files.isDirectory(path)) {
            path = path.resolve(fileName + ".csv");
            File file = path.toFile();
            writer = new CsvExportWriter(file);
          }
        }

        TabularSheet sheet = new TabularSheet(writer);
        BlockCollection hColl = new BlockCollectionHorizontal();

        // fill block for each sample
        List<TabularBlock> blocks = new ArrayList<>();
        for (Sample sample : samples) {
          blocks.addAll(DataExport.extractCustomPopulationData(
              sample,
              selChannels,
              selPops,
              exportBG,
              applyJitter,
              noOfBgDP,
              exportNP,
              npPar,
              NMPUnit
          ));
        }

        blocks.forEach(hColl::addBlock);
        sheet.addBlockCollection(hColl);
        sheet.getWriter().writeLine(DataExport.getShortMeta(null));
        sheet.export();
      }
    });

    exportExpectedValuesBtn.setOnAction(e ->

    {
      int i = 0;
      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<SampleImpl> samples = selSamples.stream()
          .map(Sample::getAllSamples)
          .flatMap(List::stream)
          .filter(s -> s instanceof SampleImpl)
          .map(s -> (SampleImpl) s)
          .collect(Collectors.toList());

      if (!samples.isEmpty()) {

        for (Sample sample : samples) {
          i++;

          String sampleName = sample.getNickName();
          sampleName = GlobalIO.cleanupWindowsFileName(sampleName);
          String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
          sampleName = dateTag + "_Expected_" + i + "_" + sampleName;

          ExportWriter writer = new ClipboardWriter();
          if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
            String pathStr = exportParamSet.getCurrentExportPath().getValue();
            Path path = Paths.get(pathStr);
            path = PathUtil.addDir(path, "Data");
            PathUtil.createDir(path);
            if (Files.isDirectory(path)) {
              path = path.resolve(sampleName + ".csv");
              File file = path.toFile();
              writer = new CsvExportWriter(file);
            }
          }

          TabularSheet sheet = new TabularSheet(writer);

          List<BlockCollection> blocks = DataExport.extractExpectedValues(sample);
          sheet.addBlockCollection(blocks);
          sheet.getWriter().writeLine(DataExport.getShortMeta(sample));
          sheet.export();
        }
      }
    });

    exportMethodBtn.setOnAction(e ->

    {
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      if (!samples.isEmpty()) {

        String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
        String fileName = dateTag + "_Methods";

        ExportWriter writer = new ClipboardWriter();
        if (exportParamSet.getExportFormat().getValue().equals(ExportTarget.CSV)) {
          String pathStr = exportParamSet.getCurrentExportPath().getValue();
          Path path = Paths.get(pathStr);
          path = PathUtil.addDir(path, "Data");
          PathUtil.createDir(path);
          if (Files.isDirectory(path)) {
            path = path.resolve(fileName + ".csv");
            File file = path.toFile();
            writer = new CsvExportWriter(file);
          }
        }

        TabularSheet sheet = new TabularSheet(writer);
        BlockCollection hColl = new BlockCollectionHorizontal();

        for (int i = 0; i < samples.size(); i++) {
          Sample sample = samples.get(i);
          // boolean isFirst = i == 0; Idea was to only export labels for first sample..
          // but that is not possible as we do not always have the same number of lines across methods.
          Method method = sample.getMethod();
          if (method != null) {
            TabularBlock block = DataExport.extractMethodSettings(sample, method);
            hColl.addBlock(block);
          }
        }

        sheet.addBlockCollection(hColl);
        sheet.getWriter().writeLine(DataExport.getShortMeta(null));
        sheet.export();
      }
    });

    exportMethodSpmBtn.setOnAction(e ->

    {
      int i = 0;
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      if (!samples.isEmpty()) {

        for (Sample sample : samples) {
          i++;

          String sampleName = sample.getNickName();
          sampleName = GlobalIO.cleanupWindowsFileName(sampleName);
          String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
          sampleName = dateTag + "_Method_" + i + "_" + sampleName;

          String pathStr = exportParamSet.getCurrentExportPath().getValue();
          Path path = Paths.get(pathStr);
          path = PathUtil.addDir(path, "Data");
          PathUtil.createDir(path);
          if (Files.isDirectory(path)) {
            path = path.resolve(sampleName + GlobalIO.METHOD_EXTENSION);

            Method method = sample.getMethod().getCopyWithoutFile();
            method.executeSaveAs(path, false);
          }
        }
      }
    });

    //
    fxParamSet.setController(this);

    notifyItemChange();
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
          if (exportParamSet != null) {
            // conf params are written to their respective static file
            exportParamSet.executeOverridingSave();
            // Also set to the runtime if save (to keep changes)
            SpTool3Main.getRunTime().setExportParams(exportParamSet);
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
    // Config file has an associatedFileOnDrive()
    NotificationFactory.openYesNo("Save settings? This is irreversible.",
        () -> {
          if (exportParamSet != null) {
            // conf params are written to their respective static file
            exportParamSet.executeOverridingSave();
            // Also set to the runtime if save (to keep changes)
            SpTool3Main.getRunTime().setExportParams(exportParamSet);
            //
            closeAndContinue();
          }
        }
    );
  }

  @Override
  public void closeAndContinue() {
    stage.close();

    // Also set to the runtime if continue (to keep changes)
    SpTool3Main.getRunTime().setExportParams((ExporterParams) exportParamSet);
  }


}

