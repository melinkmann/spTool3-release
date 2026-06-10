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

package processing.parameterSets.uiParams;

import analysis.*;
import analysis.Event;
import analysis.quant.*;
import com.google.common.util.concurrent.AtomicDouble;
import core.SpTool3Main;
import dataModelNew.*;
import dataModelNew.mz.*;
import gui.HACCharts;
import gui.MethodView;
import gui.ParameterView;
import gui.StageFactory;
import gui.dialog.notification.NotificationFactory;
import gui.util.NumberIterator;
import gui.util.UiUtil;
import io.QuantStringParser;
import io.export.ClipboardWriter;
import io.export.ExportWriter;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fastExport.TabBlock;
import io.fastExport.TabBlockColl;
import io.fastExport.TabCol;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Pair;
import math.AverageUtils;
import math.HAC;
import math.SavitzkyGolay;
import math.Smoothing;
import math.regression.RegressionUtils;
import math.stat.MeasureOfLocation;
import math.stat.PeakSymmetry;
import math.stat.PreFilter;
import math.units.Unit;
import math.units.enums.IntensityUnit;
import math.units.enums.TimeUnit;
import math.units.enums.ViewUnits;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import processing.options.*;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.ExperimentalConditions;
import processing.parameterSets.impl.ExperimentalSubConditions;
import processing.parameters.FxParameter;
import sandbox.montecarlo.DataList;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.PeakFunction;
import sandbox.montecarlo.Statistics;
import smile.stat.distribution.KernelDensity;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.batch.SimpleLinearBatch;
import tasks.results.EmptyTaskResult;
import util.ArrUtils;
import util.Functional;
import util.NF;
import util.SnF;
import visualizer.ResultTableData;
import visualizer.ResultsTable;
import visualizer.ResultsTable.TableRowData;
import visualizer.charts.*;
import visualizer.charts.AxisLabel.PlainLabel;
import visualizer.charts.SpChartFactory.*;
import visualizer.styles.*;
import visualizer.styles.Colors.SpColor;
import visualizer.styles.MarkerSize.CustomMarkerSize;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static math.regression.RegressionUtils.getOLS;


public abstract class Viewers {

  private static final Logger LOGGER = LogManager.getLogger(Viewers.class);

  public static class TableViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    public TableViewer(TableParameters plainSet) {
      super(plainSet);
    }

    @Override
    public void refreshParameterView() {
      // do nothing: there are no parameters
    }

    @Override
    public void refreshGraph() {
      // The viewer exists in the background and we need to reset is stopped as well as progress each time
      progress.set(0d);
      isStopped.set(false);

      // update UI in FXThread
      UiUtil.showLoading(SpTool3Main.getRunTime().getGuiParameterManager().getResultsTableTabPane());

      WorkingTask graphTask = new WorkingTask() {
        @Override
        public String getTaskName() {
          return "Show table";
        }

        @Override
        public AtomicDouble getProgress() {
          return progress;
        }

        @Override
        public void stop() {
          isStopped.set(true);
        }

        @Override
        public TaskResult call() throws Exception {
          try {
            Node viewNode = new AnchorPane();

            // Check which type of events to show?
            List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
            List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
            List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
            if (samples != null && !samples.isEmpty() && selChannels != null && !selChannels.isEmpty()) {

              ResultTableData tableData = new ResultTableData(samples, selChannels, selPops, false);
              progress.set(0.4);


              TableView<TableRowData> table = ResultsTable.buildNestedTable(tableData.getEntryMap());
              viewNode = UiUtil.putOnAnchorWithoutInsets(table);

              progress.set(0.8);
            }

            Node finalViewNode = viewNode;
            Platform.runLater(() -> {
              try {
                SpTool3Main.getRunTime().getGuiParameterManager().getResultsTableTabPane()
                    .setCenter(finalViewNode);
              } catch (Exception e) {
                LOGGER.error("Error while plotting! "
                    + " Message: " + ExceptionUtils.getMessage(e)
                    + " Details: " + ExceptionUtils.getStackTrace(e));
              }
            });

            progress.set(1.0);
            return new EmptyTaskResult();
          } catch (Exception e) {
            LOGGER.error("Error while plotting! "
                + " Message: " + ExceptionUtils.getMessage(e)
                + " Details: " + ExceptionUtils.getStackTrace(e));
            return new EmptyTaskResult();
          }
        }
      };
      SpTool3Main.getRunTime().getTaskManager().forceToGraphPool(
          new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false, new EmptyTaskResult()));

    }
  }

  public static class QuantificationViewer extends ViewerFXParamSet implements ParameterView {

    // container for data
    private SpCalibrationSet spCalSet = new SpCalibrationSet();
    private final Button loadCalFromSampleBtn = new Button();
    private final Button sendCalToSamplesBtn = new Button("Apply");
    private final Button clearSpCalSetBtn = new Button("");
    private final ComboBox<LinRegType> regTypeComboBox =
        new ComboBox<>(FXCollections.observableArrayList(LinRegType.values()));
    RegressionViewContainer currentRegressionDataContainer = new RegressionViewContainer(spCalSet,
        LinRegType.OLS);

    // enable copy-pasting of calibration parameters to other samples
    private final AtomicReference<Cal> clipboardCal = new AtomicReference<>(null);

  /* Box and ScrollPane for the Parameters need to be initialized here.
   The refresher method must only set their children.
   Otherwise, if we create Box and Scroll new each time the content change fires,
   sliders and others cannot request focus after firing a change.
   */

    private final BorderPane mainBorderPane = new BorderPane();
    private final BorderPane sampleLevelParPane = new BorderPane();
    private final BorderPane subLevelParPane = new BorderPane();
    private final BorderPane graphPane = new BorderPane();
    private final VBox setBox = new VBox(8);
    private final Label sampleNameLbl = new Label("Sample...");
    private final ScrollPane paramScroll = new ScrollPane(UiUtil.putOnAnchorWithoutInsets(setBox));

    private final VBox subQuantBox = new VBox(5);
    private final ScrollPane subQuantScroll = new ScrollPane(UiUtil.putOnAnchorWithoutInsets(subQuantBox));

    // FxSets keep FxParams alive internally to keep focus
    private FxParamSet sampleLevelConditionsFxSet;
    private ExperimentalConditions sampleLevelConditions;
    private final HashMap<Element, FxParamSet> eleParamFxSets = new LinkedHashMap<>();
    private final HashMap<FxParamSet, ListView<FxParameter<?>>> eleViewMap = new LinkedHashMap<>();

    // Table
    private final TableView<ResponseTableRow> table;
    private final FxSpCalibrationSetTableModel tableModel;

    // in table split: organize the model and send to table button
    private final ComboBox<CalibrationStrategy> calibrationStrategyComboBox =
        new ComboBox<>(FXCollections.observableArrayList(CalibrationStrategy.values()));
    private final ToggleButton npResponseActiveToggle = new ToggleButton();
    private final ToggleButton ionicResponseActiveToggle = new ToggleButton();
    private final ToggleButton teActiveToggle = new ToggleButton();
    private final ToggleButton pnTeActiveToggle = new ToggleButton();
    private final Button fillTableWithCalBtn = new Button("Fill column");

    final Button copyCalBtn = UiUtil.getSquareImageButton("Copy", "/img/copy.png",
        "Copy current calibration parameters");

    final Button pasteCalBtn = UiUtil.getSquareImageButton("Paste", "/img/paste.png",
        "Paste copied calibration parameters to all selected samples");

    final Button wizardConcSizeBtn = UiUtil.getImageButton("", "/img/wizard.png",
        "Try to read concentration and size data from sample");


    final Button refreshBtn = UiUtil.getImageButton("", "/img/refresh.png",
        """
            Refresh with updated data: Use this button when you change sample parameters
            such as density. Alternatively, an update will be triggered when 
            selecting another sample or isotope.""");

    final Button sumFormulaBtn = UiUtil.getImageButton("", "/img/sumFormula.png",
        """
            Opens sum formula dialog. Enter a sum formula. When the formula can be parsed,
            the corresponding mass fraction will be calculated and set for every element.""");

    final Label sampleSectionLabel = new Label("Define sample");

    final HBox sendParamButtonBox = new HBox(4,
        sampleSectionLabel,
        copyCalBtn, pasteCalBtn,
        new Separator(Orientation.VERTICAL),
        wizardConcSizeBtn,
        new Separator(Orientation.VERTICAL),
        refreshBtn,
        new Separator(Orientation.VERTICAL),
        sumFormulaBtn);

    public QuantificationViewer(QuantViewerParams plainSet) {
      // this puts the general set on the left hand side of the UI pane
      super(plainSet);

      // Horizontal SplitPane (outer)
      SplitPane paramSplitTD = new SplitPane();
      paramSplitTD.setOrientation(Orientation.VERTICAL);
      paramSplitTD.getItems().addAll(
          UiUtil.putOnAnchorWithoutInsets(sampleLevelParPane),
          UiUtil.putOnAnchorWithoutInsets(subLevelParPane));
      paramSplitTD.setDividerPositions(0.4);

      // sample section
      sampleSectionLabel.setStyle("-fx-font-weight: bold;");
      sampleSectionLabel.setMinWidth(85);

      // load/send button
      loadCalFromSampleBtn.setGraphic(UiUtil.getViewer("/img/extract.png"));
      UiUtil.tooltip(loadCalFromSampleBtn, "Load calibration table from the selected sample.");
      sendCalToSamplesBtn.setGraphic(UiUtil.getViewer("/img/send.png"));
      sendCalToSamplesBtn.setStyle(
          "-fx-border-color: #117733;" +
              "-fx-border-width: 1.5;" +
              "-fx-border-radius: 20;" +
              "-fx-background-radius: 20;" +
              "-fx-font-weight: bold;"
      );
      UiUtil.tooltip(sendCalToSamplesBtn, "Send calibration table to all selected samples.");

      clearSpCalSetBtn.setGraphic(UiUtil.getViewer("/img/delete.png"));
      UiUtil.tooltip(clearSpCalSetBtn, "Clear current calibration table.");

      loadCalFromSampleBtn.setPrefWidth(35);
      loadCalFromSampleBtn.setOnAction(e -> {
        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        if (!selSamples.isEmpty()) {
          // copy, or else changes propagate
          this.spCalSet = selSamples.get(0).getQuant().getResponses().copy();
          calibrationStrategyComboBox.getSelectionModel().select(this.spCalSet.getCalibrationStrategy());
        }
        refreshGraph();
      });

      sendCalToSamplesBtn.setPrefWidth(100);
      sendCalToSamplesBtn.setOnAction(e -> {
        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        for (Sample selSample : selSamples) {
          // copy, or else changes propagate back
          selSample.getQuant().setResponses(spCalSet.copy());
        }
      });

      clearSpCalSetBtn.setOnAction(e -> {
        spCalSet = new SpCalibrationSet();
        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        if (!selSamples.isEmpty()) {
          selSamples.get(0).getQuant().setResponses(spCalSet);
        }
        refreshGraph();
      });

      UiUtil.tooltip(regTypeComboBox, "Linear regression: weighted or ordinary.");
      regTypeComboBox.getSelectionModel().select(LinRegType.OLS);
      regTypeComboBox.setPrefWidth(100);
      regTypeComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<LinRegType>() {
        @Override
        public void changed(ObservableValue<? extends LinRegType> observable, LinRegType oldValue,
                            LinRegType newValue) {
          // force refresh of graph, else we do not override the data set
          refreshGraph();
        }
      });

      // Table
      table = new TableView<>();
      tableModel = new FxSpCalibrationSetTableModel();
      table.setEditable(true);
      table.setItems(tableModel.getRows());

      // toggles for table header
      ToggleGroup toggleGroup = new ToggleGroup();
      toggleGroup.getToggles().addAll(
          npResponseActiveToggle,
          ionicResponseActiveToggle,
          teActiveToggle,
          pnTeActiveToggle);

      // toggle tool tip
      String toggleString = "This toggle determines which data are shown in the graph and loaded to the " +
          "table.";
      UiUtil.tooltip(npResponseActiveToggle, toggleString);
      UiUtil.tooltip(ionicResponseActiveToggle, toggleString);
      UiUtil.tooltip(teActiveToggle, toggleString);
      UiUtil.tooltip(pnTeActiveToggle, toggleString);

      UiUtil.tooltip(fillTableWithCalBtn,
          """
              Based on the selected samples, isotopes and populations,
              the graph is updated to calculate instrument response and/or transport efficiency.
              This button transfers the currently selected data (which are shown in the graph) into the table.""");
      fillTableWithCalBtn.setGraphic(UiUtil.getSquareViewer("/img/fillActive.png"));
      fillTableWithCalBtn.setPrefWidth(100);

      fillTableWithCalBtn.setOnAction(e -> {
        // make sure that the calibration set has elements in it
        currentRegressionDataContainer.sendSlopesToCalSetAndTable(
            table,
            tableModel,
            npResponseActiveToggle,
            ionicResponseActiveToggle,
            teActiveToggle,
            pnTeActiveToggle
        );
      });


      calibrationStrategyComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends CalibrationStrategy> observable,
                            CalibrationStrategy oldValue, CalibrationStrategy newValue) {

          // store
          spCalSet.setCalibrationStrategy(newValue);

          if (newValue.equals(oldValue)) {
            // nada
          } else if (newValue.equals(CalibrationStrategy.SIZE_METHOD)) {
            // when updating, remove cols
            table.getColumns().clear();

            // add the element column
            TableColumn<ResponseTableRow, CalChannel> channelCol = new TableColumn<>("Isotope");
            channelCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getCalChannel()));
            channelCol.setCellFactory(col -> new TableCell<>() {
              @Override
              protected void updateItem(CalChannel item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(item.channel().getUIString());
                }
              }
            });
            table.getColumns().add(channelCol);

            table.getColumns().add(FxSpCalibrationSetTableModel.quantityColumn(
                "Ionic response",
                ResponseTableRow::getIonicResponse,
                ionicResponseActiveToggle));

            table.getColumns().add(FxSpCalibrationSetTableModel.quantityColumn(
                "NP response",
                ResponseTableRow::getNpResponse,
                npResponseActiveToggle));

            table.getColumns().add(FxSpCalibrationSetTableModel.numericColumn(
                "TE [%]",
                ResponseTableRow::getAerosolTEPct,
                null,
                90));

            table.getColumns().add(FxSpCalibrationSetTableModel.numericColumn("Number TE [%]",
                ResponseTableRow::getParticleNumberTEPct,
                pnTeActiveToggle,
                105));

            teActiveToggle.setSelected(false); // not available here

          } else if (newValue.equals(CalibrationStrategy.FREQUENCY_METHOD)) {
            // when updating, remove cols
            table.getColumns().clear();

            // add the element column
            TableColumn<ResponseTableRow, CalChannel> channelCol = new TableColumn<>("Isotope");
            channelCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getCalChannel()));
            channelCol.setCellFactory(col -> new TableCell<>() {
              @Override
              protected void updateItem(CalChannel item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(item.channel().getUIString());
                }
              }
            });
            table.getColumns().add(channelCol);

            table.getColumns().add(FxSpCalibrationSetTableModel.quantityColumn(
                "Ionic response",
                ResponseTableRow::getIonicResponse,
                ionicResponseActiveToggle));

            table.getColumns().add(FxSpCalibrationSetTableModel.numericColumn(
                "TE [%]",
                ResponseTableRow::getAerosolTEPct,
                teActiveToggle,
                90));

            table.getColumns().add(FxSpCalibrationSetTableModel.numericColumn("Number TE [%]",
                ResponseTableRow::getParticleNumberTEPct,
                pnTeActiveToggle,
                105));

            npResponseActiveToggle.setSelected(false); // not available here

          } else if (newValue.equals(CalibrationStrategy.MASS)) {
            // when updating, remove cols
            table.getColumns().clear();

            // add the elememt column
            TableColumn<ResponseTableRow, CalChannel> channelCol = new TableColumn<>("Isotope");
            channelCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getCalChannel()));
            channelCol.setCellFactory(col -> new TableCell<>() {
              @Override
              protected void updateItem(CalChannel item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(item.channel().getUIString());
                }
              }
            });
            table.getColumns().add(channelCol);

            table.getColumns().add(FxSpCalibrationSetTableModel.quantityColumn(
                "NP response",
                ResponseTableRow::getNpResponse,
                npResponseActiveToggle));

            npResponseActiveToggle.setSelected(true);
            ionicResponseActiveToggle.setSelected(false); // not available here
            teActiveToggle.setSelected(false); // not available here
            pnTeActiveToggle.setSelected(false); // not available here
          }
        }
      });

      // this should trigger filling the table
      calibrationStrategyComboBox.getSelectionModel().select(CalibrationStrategy.SIZE_METHOD);

      // Calibration strategy box
      Label calSectionLabel = new Label("Calibration");
      calSectionLabel.setStyle("-fx-font-weight: bold;");
      HBox calibrationStrategyContainer = new HBox(5,
          calSectionLabel,
          calibrationStrategyComboBox,
          regTypeComboBox,
          fillTableWithCalBtn,
          new Separator(Orientation.VERTICAL),
          sendCalToSamplesBtn,
          loadCalFromSampleBtn,
          clearSpCalSetBtn
      );
      calibrationStrategyContainer.setAlignment(Pos.CENTER_LEFT);

      /// manage toggle buttons
      ImageView npResponseActiveToggleImageOn = UiUtil.getSquareViewer("/img/active.png");
      ImageView npResponseActiveToggleImageOff = UiUtil.getSquareViewer("/img/notActive.png");
      ImageView ionicResponseActiveToggleImageOn = UiUtil.getSquareViewer("/img/active.png");
      ImageView ionicResponseActiveToggleImageOff = UiUtil.getSquareViewer("/img/notActive.png");
      ImageView teActiveToggleImageOn = UiUtil.getSquareViewer("/img/active.png");
      ImageView teActiveToggleImageOff = UiUtil.getSquareViewer("/img/notActive.png");
      ImageView pnTeActiveToggleImageOn = UiUtil.getSquareViewer("/img/active.png");
      ImageView pnTeActiveToggleImageOff = UiUtil.getSquareViewer("/img/notActive.png");

      // manage toggling: images
      npResponseActiveToggle.graphicProperty().bind(
          Bindings.when(npResponseActiveToggle.selectedProperty())
              .then(npResponseActiveToggleImageOn)
              .otherwise(npResponseActiveToggleImageOff)
      );

      ionicResponseActiveToggle.graphicProperty().bind(
          Bindings.when(ionicResponseActiveToggle.selectedProperty())
              .then(ionicResponseActiveToggleImageOn)
              .otherwise(ionicResponseActiveToggleImageOff)
      );

      teActiveToggle.graphicProperty().bind(
          Bindings.when(teActiveToggle.selectedProperty())
              .then(teActiveToggleImageOn)
              .otherwise(teActiveToggleImageOff)
      );

      pnTeActiveToggle.graphicProperty().bind(
          Bindings.when(pnTeActiveToggle.selectedProperty())
              .then(pnTeActiveToggleImageOn)
              .otherwise(pnTeActiveToggleImageOff)
      );

      // manage toggling: change behavior
      npResponseActiveToggle.selectedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                            Boolean newValue) {
          refreshGraph();
        }
      });

      ionicResponseActiveToggle.selectedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                            Boolean newValue) {
          refreshGraph();
        }
      });

      teActiveToggle.selectedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                            Boolean newValue) {
          refreshGraph();
        }
      });

      pnTeActiveToggle.selectedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                            Boolean newValue) {
          refreshGraph();
        }
      });

      // inner split panes
      SplitPane tableGraphSplit = new SplitPane();
      tableGraphSplit.setOrientation(Orientation.VERTICAL);
      tableGraphSplit.setDividerPositions(0.4);
      BorderPane tableBorder = new BorderPane();
      tableBorder.setTop(UiUtil.putOnAnchorWithInsets(calibrationStrategyContainer));
      tableBorder.setCenter(UiUtil.putOnAnchorWithoutInsets(table));
      tableGraphSplit.getItems().addAll(
          UiUtil.putOnAnchorWithoutInsets(tableBorder),
          UiUtil.putOnAnchorWithoutInsets(graphPane));

      // Vertical SplitPane (outer)
      SplitPane mainSplitLR = new SplitPane();
      mainSplitLR.setOrientation(Orientation.HORIZONTAL);
      mainSplitLR.getItems().addAll(
          UiUtil.putOnAnchorWithoutInsets(paramSplitTD),
          UiUtil.putOnAnchorWithoutInsets(tableGraphSplit));
      mainSplitLR.setDividerPositions(0.35);
      mainBorderPane.setCenter(mainSplitLR);

      //
      subQuantBox.setPadding(new Insets(0, 0, 0, 5));

      copyCalBtn.setOnAction(e -> {
        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        if (!selSamples.isEmpty()) {
          Cal calData = selSamples.get(0).getQuant();
          LOGGER.info("Copied calibration data of sample + {}", selSamples.get(0).getNickName());
          clipboardCal.set(calData.copy());
        }
      });
      copyCalBtn.setPrefWidth(80);

      pasteCalBtn.setOnAction(e -> {
        Cal calData = clipboardCal.get();
        if (calData != null) {
          List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
          for (Sample sample : selSamples) {
            sample.setCalibration(calData.copy());
          }
        }
        refreshGraph();
      });

      pasteCalBtn.setPrefWidth(80);

      wizardConcSizeBtn.setOnAction(e -> {
        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        for (Sample sample : selSamples) {
          QuantStringParser.parseIntoSample(sample.getNickName(), sample);
        }
        refreshGraph();
      });

      String wizardTooltip = """
          If sample label contains units, spTool tries to read these.
          The naming convention supported is (numbers are just examples):
          
          1) All unit blocks must be separated by underscores.
          2) Available units:
          - Flow rate: µL/min and mL/min
          - Particle size: µm and nm
          - Ionic concentration: ppm [i.e., mg/L] and ppb [i.e., µg/L]
          - Particle number concentration: NP/mL
          
          In order to be read, these have to be given as a block,
          where the prefix 'q' indicates flow rate, 'd' indicates diameter,
          'c' indicates ionic concentration and 'n' indicates particle number concentration.
          
          Examples:
          - Flow rate:_q50µlmin_ _q50ulmin_ _q50mlmin_
          - Particle size: _d3p6µm_ _d3p6nm_ _d3p6nm_
          - Ionic conc.:  _c10ppb_ _c10ppm_
          - Particle conc: _n1E5NPml_
          
          So an example in a sample name would be:
          '2026_TestSample1_c20ppb_q10uLmin'
          '2026_TestSample_NP_d100nm_n5p5E5NPmL_q10uLmin'
          
          Format hints:
          Decimal: use 'p' as decimal separator, e.g. 3p6 = 3.6
          Scientific: 1p5E5 = 1.5E5
          
          The parser is not case sensitive: all letters are converted to minor case.
          
          'µ' can also be written as 'u'
          """;
      UiUtil.tooltip(wizardConcSizeBtn, wizardTooltip);

      wizardConcSizeBtn.setPrefWidth(50);

      refreshBtn.setOnAction(e -> {
        refreshGraph();
      });
      refreshBtn.setPrefWidth(50);

      sumFormulaBtn.setOnAction(e -> {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mass fraction calculator");
        dialog.setHeaderText("Enter sum formula");
        dialog.setContentText("Sum formula");
        try {
          Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
          stage.getIcons().add(StageFactory.getSymbol());
        } catch (IllegalArgumentException fe) {
          LOGGER.info("Cannot find symbol. Stack trace: " + ExceptionUtils.getStackTrace(fe));
        }
        Optional<String> result = dialog.showAndWait();

        List<Element> elements = new ArrayList<>(eleParamFxSets.keySet());

        result.ifPresent(input -> {
          for (Element element : elements) {
            FxParamSet fxEleSet = eleParamFxSets.get(element);
            if (fxEleSet != null) {
              ParamSet plainEleSet = fxEleSet.getPlainSet();
              if (plainEleSet instanceof ExperimentalSubConditions) {
                try {
                  ExperimentalSubConditions subPar = (ExperimentalSubConditions) plainEleSet;
                  double fraction = ChemistryUtils.massFraction(result.get(), element.getSymbol());
                  if (fraction > 0 && fraction <= 1) {
                    subPar.getNpMassFraction().setValue(Math.round(fraction * 1_000_000d) / 1_000_000d);
                  } else {
                    // the other elements like are not present, thus set them 0; else: frac do not add up
                    subPar.getNpMassFraction().setValue(0d);
                  }
                } catch (Exception exception) {
                  LOGGER.info("Cannot parse sum formula."
                      + " Message: " + ExceptionUtils.getMessage(exception)
                      + " Details: " + ExceptionUtils.getStackTrace(exception));
                  NotificationFactory.openError(exception);
                  break;
                }
              }
            }
          }
          refreshGraph();
        });
      });
      sumFormulaBtn.setPrefWidth(50);
    }

    @Override
    public double getValueWidth() {
      return 150d;
    }


    @Override
    public void notifyItemChange() {
      refreshParameterView();
    }

    @Override
    public void notifyValueChange() {
      refreshParameterView();
    }

    /// Handles value/item change -> changing sample role, ...
    @Override
    public void refreshParameterView() {

      // make sure this is updated
      spCalSet.setCalibrationStrategy(calibrationStrategyComboBox.getSelectionModel().getSelectedItem());

      /// (1) Create the typical ListView for a ParameterSet to be shown
      if (sampleLevelConditions != null && sampleLevelConditionsFxSet != null && !eleParamFxSets.isEmpty()) {
        // (1) Show the sample's general settings at the left
        // (code is essentially refreshParameterView() from super)
        ///
        final double hSpace = 2;
        final double labelWidth = 70;
        final double valueWidth = getValueWidth() + 5 - hSpace;
        // final double valueWidth = 90 + 5 - hSpace;
        final double paneWidth = valueWidth + 12;

        List<FxParameter<?>> sampleLevelFxPars =
            sampleLevelConditionsFxSet.getActiveFxParametersWithoutMeta();
        sampleLevelFxPars.forEach(FxParameter::clearViewerBox); // I hope this is a good choice to include
        // it here.

        sendParamButtonBox.setAlignment(Pos.CENTER_LEFT);
        setBox.getChildren().clear();
        setBox.getChildren().addAll(sendParamButtonBox, new Separator(Orientation.HORIZONTAL));
        setBox.getChildren().add(sampleNameLbl);

        UiUtil.makePaneBright(setBox);
        UiUtil.formatScrollPane(paramScroll);
        UiUtil.makePaneBrightAndRound(paramScroll);

        // Populate the list view
        for (FxParameter<?> fxPar : sampleLevelFxPars) {
          HBox parBox = new HBox(hSpace);
          parBox.setAlignment(Pos.CENTER_LEFT);
          Label labelPar = fxPar.getLabelNode();
          Node valuePar = fxPar.getValueNode();
          // force certain width
          AnchorPane valuePane = new AnchorPane(valuePar);
          labelPar.setMinWidth(labelWidth);

          // Sliders return a stack pane (unfortunately)
          if (valuePar instanceof Control) {
            ((Control) valuePar).setPrefWidth(valueWidth);
          } else if (valuePar instanceof Pane) {
            ((Pane) valuePar).getChildren().stream()
                // Do make width of label wider!
                .filter(c -> c instanceof Slider)
                .forEach(c -> ((Control) c).setPrefWidth(valueWidth));
          }

          valuePane.setMinWidth(paneWidth);
          if (fxPar.getDecoration() != null) {
            parBox.getChildren().addAll(labelPar, valuePane, fxPar.getDecoration());
          } else {
            parBox.getChildren().addAll(labelPar, valuePane);
          }

          setBox.getChildren().add(parBox);
        }
        sampleLevelParPane.setCenter(UiUtil.putOnAnchorWithInsets(paramScroll));
        // END of populating main param set list view

        /// (2) Create the typical ListView for each  ParameterSet
        // --> Show the element-specific settings in the middle

        // force update on subSet to reflect current selection in sample-level set.
        sampleLevelConditions.organizeSubParams();

        UiUtil.formatScrollPane(subQuantScroll);
        UiUtil.installFastScroll(subQuantScroll);
        subQuantBox.getChildren().clear();

        // Populate the list views
        for (Element element : eleParamFxSets.keySet()) {
          FxParamSet fxEleSet = eleParamFxSets.get(element);

          // check if new view is needed: else, change creates new view and focus is lost
          ListView<FxParameter<?>> elementView;
          if (eleViewMap.containsKey(fxEleSet)) {
            elementView = eleViewMap.get(fxEleSet);
            elementView.getItems().clear();
            elementView.getItems().addAll(fxEleSet.getActiveFxParameters());
          } else {
            elementView = MethodView.createParamView(fxEleSet);
            eleViewMap.put(fxEleSet, elementView);
          }

          AnchorPane eleViewAnchor = UiUtil.putOnAnchorWithoutInsets(elementView);

          // consume scroll
          elementView.setOnScroll(javafx.event.Event::consume);
          eleViewAnchor.setPrefWidth(500);
          eleViewAnchor.setPrefHeight(Math.min(400, fxEleSet.getActiveFxParameters().size() * 36 + 1));
          eleViewAnchor.setMinHeight(Math.min(10, fxEleSet.getActiveFxParameters().size()) * 36 + 1);

          Label elementLabel = new Label(element.getShortName());
          elementLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 18");
          elementLabel.setMinWidth(35); // does not shrink  //32 without color and padding
          elementLabel.setPrefWidth(35); // 32 without color and padding
          if (fxEleSet.getPlainSet() instanceof ExperimentalSubConditions) {
            ExperimentalSubConditions expSub = ((ExperimentalSubConditions) fxEleSet.getPlainSet());
            expSub.setStyle(sampleLevelConditions.getCalibratorRole().getValue(),
                sampleLevelConditions.getSampleType().getValue(), elementLabel);
          }

          final Button sendToSubSets = UiUtil.getSquareImageButton("", "/img/copyMZ.png",
              "Copy element-specific parameters from this element to the other selected elements.");
          sendToSubSets.setOnAction(e -> {

            List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
            List<CalChannel> selElementChannels = AnalysisUtils.getCalChannel(selChannels);

            List<Element> selElements = selElementChannels.stream()
                .map(CalChannel::element)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

            if (!selElements.isEmpty()) {
              ParamSet thisSet = fxEleSet.getPlainSet();
              if (thisSet instanceof ExperimentalSubConditions) {
                ExperimentalSubConditions thisExSubSet = (ExperimentalSubConditions) thisSet;

                for (Element selElement : selElements) {
                  ExperimentalSubConditions thatSet =
                      sampleLevelConditions.getElementSpecificQuantParams().get(selElement);
                  ExperimentalSubConditions thatExSubSet = thatSet;
                  thatExSubSet.setAllValues(thisExSubSet);
                }

              }
            }
            refreshGraph();
          });


          VBox lblBtnBox = new VBox(2, elementLabel, sendToSubSets);
          lblBtnBox.setAlignment(Pos.CENTER);

          HBox eleBox = new HBox(5, lblBtnBox, UiUtil.putOnAnchorWithoutInsets(eleViewAnchor));
          eleBox.setAlignment(Pos.CENTER_LEFT);
          subQuantBox.getChildren().add(eleBox);
        } // end of populating listview of sub param sets

        subLevelParPane.setCenter(UiUtil.putOnAnchorWithoutInsets(subQuantScroll));
      }
    }

    /// Handles sample or isotope selection change
    @Override
    public void refreshGraph() {

      // make sure this is updated
      spCalSet.setCalibrationStrategy(calibrationStrategyComboBox.getSelectionModel().getSelectedItem());

      // PREPARE
      // Check which type of events to show?
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      List<CalChannel> selElementChannels = AnalysisUtils.getCalChannel(selChannels);

      List<Element> selElements = selElementChannels.stream()
          .map(CalChannel::element)
          .filter(Objects::nonNull)
          .distinct()
          .toList();

      // just make sure elements are present
      if (!samples.isEmpty() && !selElements.isEmpty()) {

        // update sample lable on the left
        sampleNameLbl.setText("Sample: " + samples.get(0).getNickName());

        // load the param sets
        sampleLevelConditions = samples.get(0).getQuant().getExperimentalConditions();
        sampleLevelConditionsFxSet = sampleLevelConditions.getObservableInstance();
        sampleLevelConditionsFxSet.setController(this); // funnel changes to this viewer

        // Prefill the quantification class in the sample with the elements (could be done after
        // processing, too)
        List<Element> allEleInSample = samples.get(0).listElements();
        for (Element element : allEleInSample) {
          samples.get(0).getQuant().getExperimentalConditions().getOrCreateElementSpecificQuantParams(element);
        }

        // clear maps with old element-specific subsample level experimental condition parameter set
        eleParamFxSets.clear();
        eleViewMap.clear();
        for (Element selElement : selElements) {
          ExperimentalSubConditions elementPars =
              samples.get(0).getQuant().getExperimentalConditions().getOrCreateElementSpecificQuantParams(selElement);
          if (elementPars != null) {
            FxParamSet fxEleSet = elementPars.getObservableInstance();
            fxEleSet.setController(this); // funnel changes to this viewer
            eleParamFxSets.put(selElement, fxEleSet);
          }
        }

        // parameters have been updated, call refresh
        refreshParameterView();

        /// refresh the graph and fill the table
        tableModel.fill(spCalSet);

        // refresh the graph/containers
        currentRegressionDataContainer = new RegressionViewContainer(
            spCalSet,
            regTypeComboBox.getValue(),
            samples, selElementChannels, selPops);

        // internally checks which data are needed
        currentRegressionDataContainer.refreshSlopes(
            npResponseActiveToggle,
            ionicResponseActiveToggle,
            teActiveToggle,
            pnTeActiveToggle);

        // important: set new values
        currentRegressionDataContainer.refreshTableAndGraph(graphPane, table);
      } else {
        // invalidate
        sampleLevelParPane.setCenter(null);
        subLevelParPane.setCenter(null);
        graphPane.setCenter(null);
        eleParamFxSets.clear();
        sampleLevelConditionsFxSet = null; // else, secondary call that checks for parameter pane would
        // trigger refill
        sampleLevelConditions = null; // else, secondary call that checks for parameter pane would trigger
        // refill
        eleViewMap.clear();
        // refresh the graph/containers
        currentRegressionDataContainer = new RegressionViewContainer(spCalSet, LinRegType.OLS);
        currentRegressionDataContainer.refreshTableAndGraph(graphPane, table);
      }

      // make sure pane is in place
      try {
        SpTool3Main.getRunTime().getGuiParameterManager().getQuantTabPane().setCenter(mainBorderPane);
      } catch (Exception e) {
        LOGGER.error("Error while plotting! "
            + " Message: " + ExceptionUtils.getMessage(e)
            + " Details: " + ExceptionUtils.getStackTrace(e));
      }
    }
  }

  public static class AverageViewer extends ViewerFXParamSet {

    public AverageViewer(AverageViewerParameters plainSet) {
      super(plainSet);
    }

    @Override
    public double getValueWidth() {
      return 135d;
    }

    @Override
    public void refreshGraph() {

      AverageViewerParameters plainSet = (AverageViewerParameters) super.getPlainSet();

      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      List<ChartComponent> allRawComponents = new ArrayList<>();
      List<ChartComponent> allNpComponents = new ArrayList<>();
      List<ChartComponent> allBgComponents = new ArrayList<>();

      if (selSamples != null && !selSamples.isEmpty()
          && selChannels != null && !selChannels.isEmpty()) {

        // Extract the grouped samples; when averaging, the viewer can show more than 1 sample.
        boolean concatenate = true;

        if (concatenate) {

          // isotope by isotope
          for (Channel channel : selChannels) {
            // dont collect all
            for (Sample sample : selSamples) {

              // concatenate all subsample
              List<ChartComponent> subRawComponents = new ArrayList<>();
              List<ChartComponent> subNpComponents = new ArrayList<>();
              List<ChartComponent> subBgComponents = new ArrayList<>();


              for (Sample subSample : sample.getAllSamples()) {
                Trace trace = subSample.getTrace(channel);
                if (trace != null) {

                  if (plainSet.getShowRawData().getValue()) {
                    Colors color = AnalysisUtils.getColor(sample,
                        trace.getChannel(), selSamples.size(), selChannels.size());
                    List<ChartComponent> rawComponent = getRawComponent(trace, plainSet, color);
                    subRawComponents.addAll(rawComponent);
                  }
                }
              }

              if (!subRawComponents.isEmpty()) {
                allRawComponents.add(new ChartComponent(subRawComponents));
              }

              for (PopulationID pop : selPops) {
                for (Sample subSample : sample.getAllSamples()) {
                  Trace trace = subSample.getTrace(channel);
                  if (trace != null) {

                    Colors color = AnalysisUtils.getColor(sample,
                        trace.getChannel(), selSamples.size(), selChannels.size());

                    Pair<List<ChartComponent>, List<ChartComponent>> eventComponents = getEventComponents(
                        trace, pop, plainSet, selPops.indexOf(pop), color);
                    if (eventComponents != null) {
                      subNpComponents.addAll(eventComponents.getKey());
                      subBgComponents.addAll(eventComponents.getValue());
                    }
                  }
                }
                if (!subNpComponents.isEmpty()) {
                  allNpComponents.add(new ChartComponent(subNpComponents));
                }
                if (!subBgComponents.isEmpty()) {
                  allBgComponents.add(new ChartComponent(subBgComponents));
                }
                subNpComponents = new ArrayList<>();
                subBgComponents = new ArrayList<>();
              }

            }
          }

        } else {
          List<Sample> allSamples = new ArrayList<>();
          selSamples.forEach(s -> allSamples.addAll(s.getAllSamples()));

          for (Sample sample : allSamples) {
            if (sample != null) {

              List<Trace> selTraces = sample.getTraces(selChannels);

              for (Trace trace : selTraces) {
                if (plainSet.getShowRawData().getValue()) {
                  Colors color = AnalysisUtils.getColor(sample,
                      trace.getChannel(), allSamples.size(), selChannels.size());
                  List<ChartComponent> rawComponent = getRawComponent(trace, plainSet, color);
                  allRawComponents.addAll(rawComponent);
                }

                for (PopulationID pop : selPops) {

                  Colors color = AnalysisUtils.getColor(sample,
                      trace.getChannel(), allSamples.size(), selChannels.size());

                  Pair<List<ChartComponent>, List<ChartComponent>> eventComponents = getEventComponents(
                      trace, pop, plainSet, selPops.indexOf(pop), color);
                  if (eventComponents != null) {
                    allNpComponents.addAll(eventComponents.getKey());
                    allBgComponents.addAll(eventComponents.getValue());
                  }
                }
              }
            }
          }
        }
      }

      HBox topBox = new HBox(1);
      HBox bottomBox = new HBox(1);
      VBox vBox = new VBox(1);
      vBox.getChildren().addAll(topBox, bottomBox);

      List<ValueAxis> xAxes = new ArrayList<>();
      List<ChartComponent> legendComponents = new ArrayList<>();
      legendComponents.addAll(allNpComponents); // legendComponents.addAll(allRawComponents);
      ScrollPane legend = null;

      if (!allNpComponents.isEmpty()) {
        JFreeChart chart = SpChartFactory.createLineChart(allNpComponents);
        xAxes.add(chart.getXYPlot().getDomainAxis());
        ((NumberAxis) chart.getXYPlot().getRangeAxis())
            .setAutoRangeIncludesZero(plainSet.getAutorangeWithZero().getValue());

        ChartContainer chartContainer = SpChartFactory.bundleChartLegend(
            chart,
            legendComponents,
            800, 500, true, false);

        TextTitle topRightText = new TextTitle("Particles");
        UiUtil.formatTitle(topRightText);
        chart.addSubtitle(topRightText);

        // We need to render the legend pane on the UI else, we cannot save it
        bottomBox.getChildren().add(chartContainer.combinedPane);
        chartContainer.legend.setMaxHeight(1);
        //        bottomBox.getChildren().add(chartContainer.viewer);
        legend = chartContainer.legend;
      }

      if (!allBgComponents.isEmpty()) {
        JFreeChart chart = SpChartFactory.createLineChart(allBgComponents);
        xAxes.add(chart.getXYPlot().getDomainAxis());
        ((NumberAxis) chart.getXYPlot().getRangeAxis())
            .setAutoRangeIncludesZero(plainSet.getAutorangeWithZero().getValue());

        ChartContainer chartContainer = SpChartFactory.bundleChartLegend(
            chart,
            legendComponents,
            800, 500, true, false);

        TextTitle topRightText = new TextTitle("Background");
        UiUtil.formatTitle(topRightText);
        chart.addSubtitle(topRightText);

        // We need to render the legend pane on the UI else, we cannot save it
        bottomBox.getChildren().add(chartContainer.combinedPane);
        chartContainer.legend.setMaxHeight(1);
        //    bottomBox.getChildren().add(chartContainer.viewer);
        legend = chartContainer.legend;
      }

      if (!allRawComponents.isEmpty()) {
        JFreeChart chart = SpChartFactory.createLineChart(allRawComponents);
        xAxes.add(chart.getXYPlot().getDomainAxis());
        ((NumberAxis) chart.getXYPlot().getRangeAxis())
            .setAutoRangeIncludesZero(plainSet.getAutorangeWithZero().getValue());

        TextTitle topRightText = new TextTitle("Raw data");
        UiUtil.formatTitle(topRightText);
        chart.addSubtitle(topRightText);

        ChartContainer chartContainer = SpChartFactory.bundleChartLegend(
            chart,
            legendComponents,
            1920, 500, true, false);
        legend = chartContainer.legend;

        topBox.getChildren().add(chartContainer.viewer);
      }

      if (legend != null) {
        legend.setMinHeight(75);
        legend.setPrefHeight(100);
        vBox.getChildren().add(legend);
      }

      AxisUtils.linkAxes(xAxes);

      // Here, many UI items are created; hence, I would tend to keep everything on the same runlater/pulse
      // in the UI thread
      SpTool3Main.getRunTime().getGuiParameterManager().getAverageTabPane().setCenter(vBox);
    }

    private static List<ChartComponent> getRawComponent(Trace trace,
                                                        AverageViewerParameters plainSet, Colors color) {

      List<ChartComponent> result = new ArrayList<>();

      double regressionRatio = plainSet.getRegressionViewRatio().getValue();
      regressionRatio = Math.max(0, regressionRatio);
      regressionRatio = Math.min(1, regressionRatio);

      double windowSec = Math.max(trace.getTISeries().getDT(),
          plainSet.getGeneralWindowMillisec().getValue() / 1000);

      TISeries average;
      if (windowSec == trace.getTISeries().getDT()) {
        average = new TISeriesRAM(trace.getTISeries());
      } else {
        average = AverageUtils.average(
            trace,
            windowSec,
            plainSet.getRawLocationMeasure().getValue(),
            plainSet.getRawPreFilter().getValue());
      }

      average = decideSmooth(average,
          plainSet.getRawSmoothType().getValue(),
          plainSet.getRawSGDegree().getValue(),
          plainSet.getRawSGWidth().getValue(),
          plainSet.getRawMOAVWidth().getValue(),
          plainSet.getRawKernelSigma().getValue(),
          plainSet.getRawLoessBandwidth().getValue(),
          plainSet.getSmoothPositive().getValue()
      );

      ChartComponent raw = new ChartComponent(
          new ChartData(
              trace.getChannel().getUIString() + "_" + trace.getSample().getNickName(),
              average.getTime(),
              average.getIntensity(),
              "Time", TimeUnit.SECOND, MathMod.NONE,
              "Intensity", IntensityUnit.CTS, MathMod.NONE),
          new ChartStyle(color, 1,
              LineWidthDefaults.MEDIUM_THICK,
              LineDashDefaults.STRAIGHT, 0f,
              MarkerSizeDefaults.SMALL,
              MarkerStyle.CROSS,
              false,
              RendererOption.SAMPLING_LINE_AND_SHAPE,
              LineGraphStyle.LINE_AND_MARKER)
      );
      result.add(raw);

      if (plainSet.getAddRegression().getValue()) {
        RegressionUtils.LSResult npReg = getOLS(average.getTime(), average.getIntensity(),
            regressionRatio);

        ChartComponent rawRegressionComponent = new ChartComponent(
            new ChartData(
                "Raw s/i/R:" + SnF.doubleToString(npReg.slope, NF.D1C1) + " / "
                    + SnF.doubleToString(npReg.intercept, NF.D1C1) + " / "
                    + SnF.doubleToString(npReg.rSquare, NF.D1C3)
                    + "("
                    + trace.getChannel().getUIString() + "_" + trace.getSample().getNickName()
                    + ")",
                npReg.x,
                npReg.y,
                "Time", TimeUnit.SECOND, MathMod.NONE,
                "Intensity", IntensityUnit.CTS, MathMod.NONE),
            new ChartStyle(color, 0.75,
                LineWidthDefaults.THINNER,
                LineDashDefaults.STRAIGHT, 0f,
                MarkerSizeDefaults.SMALL,
                MarkerStyle.CIRCLE,
                false,
                RendererOption.LINE_AND_SHAPE,
                LineGraphStyle.LINE_AND_MARKER)
        );
        result.add(rawRegressionComponent);
      }

      return result;
    }

    @Nullable
    private static Pair<List<ChartComponent>, List<ChartComponent>> getEventComponents(
        Trace trace,
        PopulationID pop,
        AverageViewerParameters plainSet,
        int counter,
        Colors color) {

      Colors originalColor = color;

      double regressionRatio = plainSet.getRegressionViewRatio().getValue();
      regressionRatio = Math.max(0, regressionRatio);
      regressionRatio = Math.min(1, regressionRatio);

      Pair<List<ChartComponent>, List<ChartComponent>> result = null;

      double windowSec = Math.max(trace.getTISeries().getDT(),
          plainSet.getNpWindowMillisec().getValue() / 1000);

      List<TISeries> npAverages = AverageUtils.average(trace,
          pop,
          plainSet.getNpEventParameter().getValue(),
          plainSet.getBgEventParameter().getValue(),
          windowSec,
          plainSet.getEventsLocationMeasure().getValue(),
          PreFilter.NONE);

      color = Colors.variationHSB(originalColor, color, counter);

      if (npAverages.size() == 2) {
        TISeries npAverage = npAverages.get(0);
        npAverage = decideSmooth(npAverage,
            plainSet.getNpSmoothType().getValue(),
            plainSet.getNpSGDegree().getValue(),
            plainSet.getNpSGWidth().getValue(),
            plainSet.getNpMOAVWidth().getValue(),
            plainSet.getNpKernelSigma().getValue(),
            plainSet.getNpLoessBandwidth().getValue(),
            plainSet.getSmoothPositive().getValue()
        );

        AxisLabel yAxisLabel = AxisLabel.getUnit(plainSet.getNpEventParameter().getValue());
        if (plainSet.getNpEventParameter().getValue().equals(EventParameter.NO_OF_EVENTS)) {
          yAxisLabel.setUnit(ViewUnits.NP_PER_SECOND);
        }

        ChartComponent npComponent = new ChartComponent(
            new ChartData(
                "NP of " + trace.getChannel().getUIString() + " from " + pop.toString()
                    + " in " + trace.getSample().getNickName(),
                npAverage.getTime(),
                npAverage.getIntensity(),
                "Time", TimeUnit.SECOND, MathMod.NONE,
                yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
            new ChartStyle(color, 1,
                LineWidthDefaults.MEDIUM_THICK,
                LineDashDefaults.STRAIGHT, 0f,
                MarkerSizeDefaults.SMALL,
                MarkerStyle.CROSS,
                false,
                RendererOption.SAMPLING_LINE_AND_SHAPE,
                LineGraphStyle.LINE_AND_MARKER)
        );

        ChartComponent npRegressionComponent = null;
        if (plainSet.getAddRegression().getValue()) {
          RegressionUtils.LSResult npReg = getOLS(npAverage.getTime(), npAverage.getIntensity(),
              regressionRatio);

          npRegressionComponent = new ChartComponent(
              new ChartData(
                  "OLS for NP"
                      + " s=" + SnF.doubleToString(npReg.slope, NF.D1C1)
                      + " b=" + SnF.doubleToString(npReg.intercept, NF.D1C1)
                      + " R2=" + SnF.doubleToString(npReg.rSquare, NF.D1C3)
                      + " (" + trace.getChannel().getUIString() + " from" + pop.toString()
                      + " in " + trace.getSample().getNickName()
                      + ")",
                  npReg.x,
                  npReg.y,
                  "Time", TimeUnit.SECOND, MathMod.NONE,
                  yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
              new ChartStyle(color, 0.75,
                  LineWidthDefaults.THINNER,
                  LineDashDefaults.STRAIGHT, 0f,
                  MarkerSizeDefaults.SMALL,
                  MarkerStyle.CIRCLE,
                  false,
                  RendererOption.LINE_AND_SHAPE,
                  LineGraphStyle.LINE_AND_MARKER)
          );
        }

        TISeries bgAverage = npAverages.get(1);
        bgAverage = decideSmooth(bgAverage,
            plainSet.getNpSmoothType().getValue(),
            plainSet.getNpSGDegree().getValue(),
            plainSet.getNpSGWidth().getValue(),
            plainSet.getNpMOAVWidth().getValue(),
            plainSet.getNpKernelSigma().getValue(),
            plainSet.getNpLoessBandwidth().getValue(),
            plainSet.getSmoothPositive().getValue()
        );

        yAxisLabel = AxisLabel.getUnit(plainSet.getBgEventParameter().getValue());

        ChartComponent bgComponent = new ChartComponent(
            new ChartData(
                "BG of " + trace.getChannel().getUIString() + " from " + pop.toString()
                    + " in " + trace.getSample().getNickName(),
                bgAverage.getTime(),
                bgAverage.getIntensity(),
                "Time", TimeUnit.SECOND, MathMod.NONE,
                yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
            new ChartStyle(color, 1,
                LineWidthDefaults.MEDIUM_THICK,
                LineDashDefaults.STRAIGHT, 0f,
                MarkerSizeDefaults.SMALL,
                MarkerStyle.CROSS,
                false,
                RendererOption.SAMPLING_LINE_AND_SHAPE,
                LineGraphStyle.LINE_AND_MARKER)
        );

        ChartComponent bgRegressionComponent = null;
        if (plainSet.getAddRegression().getValue()) {
          RegressionUtils.LSResult bgReg = getOLS(bgAverage.getTime(), bgAverage.getIntensity(),
              regressionRatio);

          bgRegressionComponent = new ChartComponent(
              new ChartData(
                  "BG s/i/R:" + SnF.doubleToString(bgReg.slope, NF.D1C1) + " / "
                      + SnF.doubleToString(bgReg.intercept, NF.D1C1) + " / "
                      + SnF.doubleToString(bgReg.rSquare, NF.D1C3)
                      + "(" + trace.getChannel().getUIString() + " from" + pop.toString()
                      + " in " + trace.getSample().getNickName()
                      + ")",
                  bgReg.x,
                  bgReg.y,
                  "Time", TimeUnit.SECOND, MathMod.NONE,
                  yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
              new ChartStyle(color, 0.75,
                  LineWidthDefaults.THINNER,
                  LineDashDefaults.STRAIGHT, 0f,
                  MarkerSizeDefaults.SMALL,
                  MarkerStyle.CIRCLE,
                  false,
                  RendererOption.LINE_AND_SHAPE,
                  LineGraphStyle.LINE_AND_MARKER)
          );
        }

        List<ChartComponent> npComp = new ArrayList<>();
        npComp.add(npComponent);
        if (npRegressionComponent != null) {
          npComp.add(npRegressionComponent);
        }

        List<ChartComponent> bgComp = new ArrayList<>();
        bgComp.add(bgComponent);
        if (bgRegressionComponent != null) {
          bgComp.add(bgRegressionComponent);
        }

        result = new Pair<>(npComp, bgComp);

      }

      return result;
    }


    private static TISeries decideSmooth(TISeries averagedData, SmoothType smoothType, int sgDegree,
                                         int sgWidth, int moavWidth,
                                         double kernelWidth, double loessWidth, boolean nonzero) {
      TISeries series = averagedData;

      if (smoothType.equals(SmoothType.GAUSSIAN_KERNEL)) {
        double[] intensity = averagedData.getIntensity();
        intensity = Smoothing.gaussianSmooth(intensity, kernelWidth);
        //        if (nonzero) {
        //          ArrUtils.replaceNegativeWithZero(intensity);
        //        }
        series = new TISeriesRAM(averagedData.getTime(), intensity);

      } else if (smoothType.equals(SmoothType.MOAV)) {
        double[] intensity = averagedData.getIntensity();
        intensity = Smoothing.moavSmooth(intensity, moavWidth);
        series = new TISeriesRAM(averagedData.getTime(), intensity);

      } else if (smoothType.equals(SmoothType.SAVITZKY_GOLAY)) {
        double[] intensity = averagedData.getIntensity();
        intensity = SavitzkyGolay.smoothSG(intensity, sgWidth, sgDegree);
        if (nonzero) {
          ArrUtils.replaceNegativeWithZero(intensity);
        }
        series = new TISeriesRAM(averagedData.getTime(), intensity);
      } else if (smoothType.equals(SmoothType.SINC_KERNEL)) {
        double[] intensity = averagedData.getIntensity();
        intensity = Smoothing.sincSmooth(intensity, sgDegree, sgWidth);
        if (nonzero) {
          ArrUtils.replaceNegativeWithZero(intensity);
        }
        series = new TISeriesRAM(averagedData.getTime(), intensity);
      } else if (smoothType.equals(SmoothType.LOESS)) {
        double[] intensity = averagedData.getIntensity();
        intensity = Smoothing.loessSmoothWithPadding(intensity, loessWidth);
        if (nonzero) {
          ArrUtils.replaceNegativeWithZero(intensity);
        }
        series = new TISeriesRAM(averagedData.getTime(), intensity);
      }

      return series;
    }
  }


  public static class SingleEventViewer extends ViewerFXParamSet {

    private static final TextField currentStartIndexFld = new TextField();
    private static final NumberIterator iteratorController = new NumberIterator(
        currentStartIndexFld,
        0,
        12,
        0
    );

    private final Button next;
    private final Button prev;
    private final Button reset;
    private final ToolBar toolbar = new ToolBar();

    public SingleEventViewer(SingleEventViewerParameters plainSet) {
      super(plainSet);
      this.next = new Button("Next");
      this.prev = new Button("Previous");
      this.reset = new Button("Reset");

      next.setOnAction(e -> iteratorController.increment());
      prev.setOnAction(e -> iteratorController.decrement());
      reset.setOnAction(e -> iteratorController.resetCurrentIndex());
      this.toolbar.getItems().addAll(prev, currentStartIndexFld, next, reset);

      iteratorController.setOnChange(new Functional() {
        @Override
        public void proceed() {
          refreshGraph();
        }
      });
    }


    @Override
    public void refreshGraph() {

      SingleEventViewerParameters plainSet = (SingleEventViewerParameters) super.getPlainSet();

      SpTool3Main.getRunTime().getGuiParameterManager().getEventTabPane()
          .setTop(UiUtil.putOnAnchorWithoutInsets(toolbar));

      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      int nEventsToShow = Math.min(12, plainSet.getNumberOfEventsShown().getValue());
      nEventsToShow = Math.max(1, nEventsToShow);
      iteratorController.setStepSize(nEventsToShow - 1);

      MathMod mathModYAxis = plainSet.getLogYAXis().getValue() ? MathMod.LOG10 : MathMod.NONE;

      if (samples != null && !samples.isEmpty() && !selPops.isEmpty()) {

        // so far we only show one sample.
        Sample sample = samples.get(0);
        PopulationID firstID = selPops.get(0);

        // remove non-aligned isotopes or those with no events. Else, equal length check fails.
        // this is meant for cases where not all available isotopes are aligned and hence
        // some selected isotopes may yield zero events
        selChannels.removeIf(iso -> sample.getNPEvents(iso, firstID).isEmpty());

        // make sure that this is not empty are the removal step!
        if (!selChannels.isEmpty()) {

          // Check if we are dealing with aligned population and multiple selected isotopes
          boolean multiIsotopeMode = AnalysisUtils.isAlignedOrPVal(firstID) && selChannels.size() > 1;

          if (multiIsotopeMode) {

            // Length checks
            boolean equalLength = selChannels.stream()
                .map(iso -> sample.getNPEvents(iso, firstID))
                .map(List::size)
                .distinct()
                .count() == 1;

            if (equalLength) {
              List<Event> firstEventList = sample.getNPEvents(selChannels.get(0), firstID);
              int length = firstEventList.size();
              if (length > 0) {

                // Get the total signal per region of the requested parameter
                double[] regionBasedEventParameter = new double[length];
                for (Channel ch : selChannels) {
                  List<Event> events = sample.getNPEvents(ch, firstID);
                  for (int i = 0; i < events.size(); i++) {
                    Event event = events.get(i);
                    regionBasedEventParameter[i] += event.get(plainSet.getEventParameter().getValue());
                  }
                }

                // consider changing this in case it performs too slowly
                HashMap<Channel, List<Event>> eventMap = new HashMap<>();

                // iterate over all regions by assuming the first region as a model (length checked above)
                if (plainSet.getSortBoolean().getValue()) {
                  for (Channel ch : selChannels) {
                    List<Event> events = sample.getNPEvents(ch, firstID);
                    events = IntStream.range(0, length)
                        .boxed()
                        .sorted(Comparator.comparingDouble(i -> regionBasedEventParameter[i]))
                        .map(events::get)
                        .toList();
                    eventMap.put(ch, events);
                  }
                } else {
                  for (Channel ch : selChannels) {
                    eventMap.put(ch, sample.getNPEvents(ch, firstID));
                  }
                }

                /// Now start building the plots
                // for some reason we should +1 (I think b/c we show 1 DP overlapping with event)
                int previewWidth = plainSet.getNumberOfBGEvents().getValue() + 1;

                // Create a GridPane and put the viewers on that grid
                GridPane gridPane = new GridPane();
                gridPane.setHgap(5);
                gridPane.setVgap(5);
                // Fill the GridPane (3 rows x 5 columns)
                int numCols = 4;
                int counter = 0;

                HashMap<Channel, List<TISeries>> eventSeries = new HashMap<>();
                HashMap<Channel, List<Integer>> globalEvtIndex = new HashMap<>();
                HashMap<Channel, List<Colors>> eventColor = new HashMap<>();
                HashMap<Channel, List<TISeries>> previewSeries = new HashMap<>();
                HashMap<Channel, List<TISeries>> postviewSeries = new HashMap<>();

                // Build the data sets
                for (Channel ch : selChannels) {
                  List<Event> events = eventMap.get(ch);
                  if (iteratorController.hasValue()) {
                    iteratorController.setFinalIdx(events.size() - 1);
                    for (int i = iteratorController.getCurrentIdx();
                         i <= iteratorController.getCurrentEndIdx(); i++) {

                      if (events.size() > i) {
                        Event event = events.get(i);

                        if (plainSet.getLogYAXis().getValue()) {
                          eventSeries.computeIfAbsent(ch, k -> new ArrayList<>()).add(event.getLogProfile());
                        } else {
                          eventSeries.computeIfAbsent(ch, k -> new ArrayList<>()).add(event.getProfile());
                        }

                        globalEvtIndex.computeIfAbsent(ch, k -> new ArrayList<>()).add(i);
                        eventColor.computeIfAbsent(ch, k -> new ArrayList<>()).add(AnalysisUtils.getColor(
                            sample,
                            ch,
                            1,
                            selChannels.size()));

                        if (plainSet.getLogYAXis().getValue()) {
                          previewSeries.computeIfAbsent(ch, k -> new ArrayList<>()).add(event.getLogPreviousDP(previewWidth));
                          postviewSeries.computeIfAbsent(ch, k -> new ArrayList<>()).add(event.getLogFollowingDP(previewWidth));
                        } else {
                          previewSeries.computeIfAbsent(ch, k -> new ArrayList<>()).add(event.getPreviousDP(previewWidth));
                          postviewSeries.computeIfAbsent(ch, k -> new ArrayList<>()).add(event.getFollowingDP(previewWidth));
                        }
                      }
                    }
                  }
                }

                // Build the charts
                List<JFreeChart> charts = new ArrayList<>();

                int numEvents = 0;
                List<TISeries> serList = eventSeries.get(selChannels.get(0));
                if (serList != null) {
                  numEvents = serList.size();
                }

                for (int i = 0; i < numEvents; i++) {

                  List<ChartComponent> chartComponents = new ArrayList<>();
                  List<ChartComponent> legendComponents = new ArrayList<>();

                  for (Channel ch : selChannels) {
                    List<TISeries> isoEventSeries = eventSeries.get(ch);

                    if (isoEventSeries != null && i < isoEventSeries.size()) {

                      TISeries eventData = eventSeries.get(ch).get(i);
                      Colors color = eventColor.get(ch).get(i);
                      TISeries prevData = previewSeries.get(ch).get(i);
                      TISeries postData = postviewSeries.get(ch).get(i);
                      int evtIndex = globalEvtIndex.get(ch).get(i);

                      ChartComponent comp;

                      if (eventData.size() < 1E3) {
                        comp = new ChartComponent(
                            new ChartData("Evt # " + (evtIndex + 1) + " - " + ch.getShortUIString(),
                                eventData,
                                "Time", TimeUnit.SECOND, MathMod.NONE,
                                "Intensity", IntensityUnit.CTS, mathModYAxis),
                            new ChartStyle(color, 1,
                                LineWidthDefaults.MEDIUM_THICK,
                                LineDashDefaults.STRAIGHT, 0f,
                                MarkerSizeDefaults.SMALL,
                                MarkerStyle.CROSS,
                                false,
                                RendererOption.LINE_AND_SHAPE,
                                LineGraphStyle.LINE_AND_MARKER)
                        );
                      } else {
                        comp = new ChartComponent(
                            new ChartData("Evt # " + (evtIndex + 1) + " - " + ch.getShortUIString(),
                                eventData,
                                "Time", TimeUnit.SECOND, MathMod.NONE,
                                "Intensity", IntensityUnit.CTS, mathModYAxis),
                            new ChartStyle(color, 1,
                                LineWidthDefaults.MEDIUM_THICK,
                                LineDashDefaults.STRAIGHT, 0f,
                                MarkerSizeDefaults.SMALL,
                                MarkerStyle.CROSS,
                                false,
                                RendererOption.SAMPLING_LINE_AND_SHAPE,
                                LineGraphStyle.LINE_AND_MARKER)
                        );
                      }

                      chartComponents.add(comp);
                      legendComponents.add(comp);

                      comp = new ChartComponent(
                          new ChartData("BG",
                              prevData,
                              "Time", TimeUnit.SECOND, MathMod.NONE,
                              "Intensity", IntensityUnit.CTS, mathModYAxis),
                          new ChartStyle(color, 1,
                              LineWidthDefaults.MEDIUM,
                              LineDashDefaults.STRAIGHT, 0f,
                              MarkerSizeDefaults.SMALL,
                              MarkerStyle.CIRCLE,
                              false,
                              RendererOption.LINE_AND_SHAPE,
                              LineGraphStyle.LINE)
                      );
                      chartComponents.add(comp);
                      // legendComponents.add(comp); // for multiple isotopes, this just spams...

                      chartComponents.add(new ChartComponent(
                          new ChartData("BG",
                              postData,
                              "Time", TimeUnit.SECOND, MathMod.NONE,
                              "Intensity", IntensityUnit.CTS, mathModYAxis),
                          new ChartStyle(color, 1,
                              LineWidthDefaults.MEDIUM,
                              LineDashDefaults.STRAIGHT, 0f,
                              MarkerSizeDefaults.SMALL,
                              MarkerStyle.CIRCLE,
                              false,
                              RendererOption.LINE_AND_SHAPE,
                              LineGraphStyle.LINE)
                      ));
                    }
                  }

                  JFreeChart chart = SpChartFactory.createLineChart(chartComponents);
                  charts.add(chart);


                  ChartContainer chartContainer = SpChartFactory.bundleChartLegend(
                      chart,
                      legendComponents,
                      800, 500);
                  Node viewNode = chartContainer.combinedPane;

                  int row = counter / numCols;
                  int col = counter % numCols;
                  counter++;

                  GridPane.setHgrow(viewNode, Priority.ALWAYS);
                  GridPane.setVgrow(viewNode, Priority.ALWAYS);

                  gridPane.add(viewNode, col, row);

                  if (plainSet.getUseCommonYAxis().getValue()) {
                    AtomicReference<Double> maxY = new AtomicReference<>((double) 0);
                    eventSeries.values().stream()
                        .flatMap(List::stream)
                        .forEach(s -> maxY.set(Math.max(maxY.get(), ArrUtils.getMax(s.getIntensity()))));
                    if (maxY.get() > 0) {
                      charts.forEach(c -> c.getXYPlot().getRangeAxis().setRange(new Range(0, maxY.get())));
                    }
                  }
                }

                SpTool3Main.getRunTime().getGuiParameterManager().getEventTabPane()
                    .setCenter(UiUtil.putOnAnchorWithoutInsets(gridPane));
              }
            }
          } else {

            // just one isotope
            Channel channel = selChannels.get(0);
            List<Event> events = sample.getNPEvents(channel, firstID);

            if (plainSet.getSortBoolean().getValue()) {
              events.sort((o1, o2) -> {
                double val1 = o1.get(plainSet.getEventParameter().getValue());
                double val2 = o2.get(plainSet.getEventParameter().getValue());
                return Double.compare(val1, val2);
              });
            }

            // for some reason we should +1 (I think b/c we show 1 DP overlapping with event)
            int previewWidth = plainSet.getNumberOfBGEvents().getValue() + 1;
            List<TISeries> eventSeries = new ArrayList<>();
            List<Integer> globalEvtIndex = new ArrayList<>();
            List<Colors> eventColor = new ArrayList<>();
            List<TISeries> previewSeries = new ArrayList<>();
            List<TISeries> postviewSeries = new ArrayList<>();
            List<String> eventAreaLabels = new ArrayList<>();

            // Create a GridPane and put the viewers on that grid
            GridPane gridPane = new GridPane();
            gridPane.setHgap(5);
            gridPane.setVgap(5);
            // Fill the GridPane (3 rows x 5 columns)
            int numCols = 4;
            int counter = 0;

            if (iteratorController.hasValue()) {
              iteratorController.setFinalIdx(events.size() - 1);
              for (int i = iteratorController.getCurrentIdx();
                   i <= iteratorController.getCurrentEndIdx(); i++) {

                if (events.size() > i) {
                  Event event = events.get(i);

                  if (plainSet.getLogYAXis().getValue()) {
                    eventSeries.add(event.getLogProfile());
                  } else {
                    eventSeries.add(event.getProfile());
                  }
                  globalEvtIndex.add(i);
                  eventColor.add(AnalysisUtils.getColor(
                      sample,
                      channel,
                      1,
                      selChannels.size()));

                  if (plainSet.getLogYAXis().getValue()) {
                    previewSeries.add(event.getLogPreviousDP(previewWidth));
                    postviewSeries.add(event.getLogFollowingDP(previewWidth));
                  } else {
                    previewSeries.add(event.getPreviousDP(previewWidth));
                    postviewSeries.add(event.getFollowingDP(previewWidth));
                  }

                  eventAreaLabels.add(event.getLabel());
                }
              }
            }

            List<JFreeChart> charts = new ArrayList<>();

            for (int i = 0; i < eventSeries.size(); i++) {

              // Create fresh for every single plot or else events accumulate in each view
              List<ChartComponent> chartComponents = new ArrayList<>();
              List<ChartComponent> legendComponents = new ArrayList<>();

              TISeries eventData = eventSeries.get(i);
              Colors color = eventColor.get(i);
              TISeries prevData = previewSeries.get(i);
              TISeries postData = postviewSeries.get(i);
              String areaLabel = eventAreaLabels.get(i);

              ChartComponent comp;

              // Avoid freezing when too large
              if (eventData.size() < 1E3) {
                comp = new ChartComponent(
                    new ChartData("Evt # " + (globalEvtIndex.get(i) + 1) + " - " + channel.getShortUIString(),
                        eventData,
                        "Time", TimeUnit.SECOND, MathMod.NONE,
                        "Intensity", IntensityUnit.CTS, mathModYAxis),
                    new ChartStyle(color, 1,
                        LineWidthDefaults.MEDIUM_THICK,
                        LineDashDefaults.STRAIGHT, 0f,
                        MarkerSizeDefaults.SMALL,
                        MarkerStyle.CROSS,
                        false,
                        RendererOption.LINE_AND_SHAPE,
                        LineGraphStyle.LINE_AND_MARKER)
                );
              } else {
                comp = new ChartComponent(
                    new ChartData("Evt # " + (globalEvtIndex.get(i) + 1) + " - " + channel.getShortUIString(),
                        eventData,
                        "Time", TimeUnit.SECOND, MathMod.NONE,
                        "Intensity", IntensityUnit.CTS, mathModYAxis),
                    new ChartStyle(color, 1,
                        LineWidthDefaults.MEDIUM_THICK,
                        LineDashDefaults.STRAIGHT, 0f,
                        MarkerSizeDefaults.SMALL,
                        MarkerStyle.CROSS,
                        false,
                        RendererOption.SAMPLING_LINE_AND_SHAPE,
                        LineGraphStyle.LINE_AND_MARKER)
                );
              }

              chartComponents.add(comp);
              legendComponents.add(comp);

              comp = new ChartComponent(
                  new ChartData("BG",
                      prevData,
                      "Time", TimeUnit.SECOND, MathMod.NONE,
                      "Intensity", IntensityUnit.CTS, mathModYAxis),
                  new ChartStyle(OkabeItoColors.BLACK_DARK, 1,
                      LineWidthDefaults.MEDIUM,
                      LineDashDefaults.STRAIGHT, 0f,
                      MarkerSizeDefaults.SMALL,
                      MarkerStyle.CIRCLE,
                      false,
                      RendererOption.LINE_AND_SHAPE,
                      LineGraphStyle.LINE)
              );
              chartComponents.add(comp);
              legendComponents.add(comp);

              chartComponents.add(new ChartComponent(
                  new ChartData("BG",
                      postData,
                      "Time", TimeUnit.SECOND, MathMod.NONE,
                      "Intensity", IntensityUnit.CTS, mathModYAxis),
                  new ChartStyle(OkabeItoColors.BLACK_DARK, 1,
                      LineWidthDefaults.MEDIUM,
                      LineDashDefaults.STRAIGHT, 0f,
                      MarkerSizeDefaults.SMALL,
                      MarkerStyle.CIRCLE,
                      false,
                      RendererOption.LINE_AND_SHAPE,
                      LineGraphStyle.LINE)
              ));

              JFreeChart chart = SpChartFactory.createLineChart(chartComponents);
              charts.add(chart);

              if (plainSet.getAnnotatePeakParameters().getValue()) {
                XYPlot plot = chart.getXYPlot();
                //
                double x = 0.2 * plot.getDomainAxis().getRange().getLowerBound()
                    + 0.8 * plot.getDomainAxis().getRange().getUpperBound();
                double y = 0.85 * plot.getRangeAxis().getRange().getUpperBound();
                String annotTxt = "Σ " + areaLabel;
                XYTextAnnotation annotation = new XYTextAnnotation(annotTxt, x, y);
                annotation.setFont(FontStyles.getPlain());
                //
                plot.addAnnotation(annotation);
              }

              ChartContainer chartContainer = SpChartFactory.bundleChartLegend(
                  chart,
                  legendComponents,
                  800, 500);
              Node viewNode = chartContainer.combinedPane;

              int row = counter / numCols;
              int col = counter % numCols;
              counter++;

              //viewNode.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Let viewer expand

              // Ensure the viewer grows to fill its cell
              GridPane.setHgrow(viewNode, Priority.ALWAYS);
              GridPane.setVgrow(viewNode, Priority.ALWAYS);

              // Add to grid
              gridPane.add(viewNode, col, row);

              if (plainSet.getUseCommonYAxis().getValue()) {
                AtomicReference<Double> maxY = new AtomicReference<>((double) 0);
                eventSeries.forEach(s ->
                    maxY.set(Math.max(maxY.get(), ArrUtils.getMax((s.getIntensity())))));
                if (maxY.get() > 0) {
                  charts.forEach(c -> c.getXYPlot().getRangeAxis().setRange(new Range(0, maxY.get())));
                }
              }
            }

            // Keep as much in the same pulse/runlater on the UI thread
            SpTool3Main.getRunTime().getGuiParameterManager().getEventTabPane()
                .setCenter(UiUtil.putOnAnchorWithoutInsets(gridPane));
          }
        }
      }
    } // refresh
  }


  public static class MonteCarloRawDataViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    private final AtomicReference<ChartViewer> viewerRef = new AtomicReference<>(null);

    public MonteCarloRawDataViewer(MonteCarloRawDataParameters plainSet) {
      super(plainSet);
    }

    @Override
    public void refreshGraph() {
      MonteCarloRawDataParameters plainSet = (MonteCarloRawDataParameters) super.getPlainSet();
      if (plainSet.getUseChartFX().getValue()) {
        refreshGraphChartFX();
      } else {
        refreshGraphJFree();
      }
    }

    public void refreshGraphChartFX() {
      // The viewer exists in the background and we need to reset is stopped as well as progress each time
      progress.set(0d);
      isStopped.set(false);

      MonteCarloRawDataParameters plainSet = (MonteCarloRawDataParameters) super.getPlainSet();
      final List<DoubleDataSet> lineGraphs = new ArrayList<>();
      final List<DoubleDataSet> peakMarkers = new ArrayList<>();
      final List<DoubleDataSet> popMarkers = new ArrayList<>();
      final List<DoubleDataSet> thresholdMarkers = new ArrayList<>();

      // update UI in FXThread
      UiUtil.showLoading(SpTool3Main.getRunTime().getGuiParameterManager().getRawDatMcTabPane());

      // retrieve while in UI Thread
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      // Check which type of events to show?
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      WorkingTask graphTask = new WorkingTask() {
        @Override
        public String getTaskName() {
          return "Show raw data";
        }

        @Override
        public AtomicDouble getProgress() {
          return progress;
        }

        @Override
        public void stop() {
          isStopped.set(true);
        }

        @Override
        public TaskResult call() throws Exception {
          // LOGGER.trace("Refreshing raw data view.");

          try {
            if (samples != null && !samples.isEmpty()) {
              // so far we only show one sample in the MC raw view.
              Sample sample = samples.get(0);

              if (sample != null) {
                // merged samples will return the main sample
                sample = sample.getPrincipleSample();

                if (selChannels != null && !selChannels.isEmpty()) {

                  List<Trace> selTraces = sample.getTraces(selChannels);

                  if (!selTraces.isEmpty()) {

                    // max events to show per population
                    int maxEventsToShow = plainSet.getUpperPointCountCutoff().getValue().intValue();
                    Iterator<Colors> colorIterator = Colors.getDefaultIterator().iterator();
                    Iterator<MarkerStyle> markerIterator = MarkerStyle.getDefaultAwtIterator().iterator();

                    /////////////////////////////////////////////////////////////////////////
                    //////////////////////// Simple Raw Data plot ///////////////////////////
                    /////////////////////////////////////////////////////////////////////////

                    traceLoop:
                    for (Trace trace : selTraces) {
                      // update progress
                      progress.set(0.5 * (double) selTraces.indexOf(trace) / (selTraces.size()));
                      if (isStopped.get()) {
                        break traceLoop;
                      }

                      String seriesLabel = trace.getChannel().getIsotope().getNumberAndElement();
                      Colors color = trace.getColor(sample);
                      String colorHex = Colors.toHex(color);

                      DoubleDataSet ds = new DoubleDataSet(seriesLabel);
                      ds.add(trace.getTISeries().getTime(), trace.getTISeries().getIntensity());
                      ds.setStyle("fx-stroke: " + colorHex + "; fx-fill: transparent;");
                      lineGraphs.add(new DoubleDataSet(ds));

                    }

                    if (plainSet.getShowEventMarkers().getValue() ||
                        plainSet.getShowPopulationMarkers().getValue()) {

                      List<PlottableEventMarkers> plottableEventMarkers =
                          AnalysisUtils.getEventMarkers(selTraces, selPops);

                      List<PlottableSubPopulation> plottablePopulations =
                          AnalysisUtils.getPopulationMarkers(selTraces, selPops);

                      plottableEventMarkers.removeIf(
                          p -> p.getEventMarkerData().size() > maxEventsToShow);

                      plottablePopulations.removeIf(p -> p.getTimerMarkers().length > maxEventsToShow);

                      /////////////////////////////////////////////////////////////////////////
                      //////////////////////// POPULATION MARKERS ///////////////////////////
                      /////////////////////////////////////////////////////////////////////////

                      if (plainSet.getShowPopulationMarkers().getValue()
                          && !plottablePopulations.isEmpty()) {

                        popLoop:
                        for (int i = 0; i < plottablePopulations.size(); i++) {
                          // update progress
                          progress.set(0.6 * (double) i / (plottablePopulations.size()));
                          if (isStopped.get()) {
                            break popLoop;
                          }

                          PlottableSubPopulation plotPop = plottablePopulations.get(i);

                          DoubleDataSet ds = new DoubleDataSet(plotPop.getPopLabel());
                          ds.add(plotPop.getTimerMarkers(), ArrUtils.fillArray(-(2 + 2 * i),
                              plotPop.size()));

                          Colors col = plotPop.getColor() != null ? plotPop.getColor() :
                              colorIterator.next();

                          ds.setStyle("fx-stroke: " + Colors.toHex(col) + ";"
                              + "fx-fill: " + Colors.toHex(col) + ";");

                          peakMarkers.add(ds);
                        }
                      }

                      /////////////////////////////////////////////////////////////////////////
                      //////////////////////////////// PEAK MARKERS ///////////////////////////
                      /////////////////////////////////////////////////////////////////////////
                      if (plainSet.getShowEventMarkers().getValue() && !plottableEventMarkers.isEmpty()) {

                        eventLoop:
                        for (int i = 0; i < plottableEventMarkers.size(); i++) {
                          // update progress
                          progress.set(0.7 * (double) i / (plottableEventMarkers.size()));
                          if (isStopped.get()) {
                            break eventLoop;
                          }

                          PlottableEventMarkers eventMarkerData = plottableEventMarkers.get(i);

                          PlottableSubPopulation plotPop = plottablePopulations.get(i);

                          DoubleDataSet ds = new DoubleDataSet(eventMarkerData.getSeriesLabel());
                          ds.add(eventMarkerData.getEventMarkerData().getTime(),
                              eventMarkerData.getEventMarkerData().getIntensity());

                          Colors col = plotPop.getColor() != null ? plotPop.getColor() :
                              colorIterator.next();

                          ds.setStyle("fx-stroke: " + Colors.toHex(col) + ";"
                              + "fx-fill: " + Colors.toHex(col) + ";");

                          popMarkers.add(ds);
                        }
                      }
                    }

                    /////////////////////////////////////////////////////////////////////////
                    //////////////////////////////// THRESHOLDS ///////////////////////////
                    /////////////////////////////////////////////////////////////////////////
                    if (plainSet.getShowThresholdMarkers().getValue()) {
                      // BASELINE
                      List<PopulationID> selPopList = SpTool3Main.getRunTime().getMainWindowCtl()
                          .getSelPops();

                      traceLoop:
                      for (Trace trace : selTraces) {
                        // update progress:
                        progress.set(0.8 + (double) selTraces.indexOf(trace) / (selTraces.size()));
                        if (isStopped.get()) {
                          break traceLoop;
                        }
                        for (PopulationID id : selPopList) {
                          Population pop = trace.getPopulation(id);
                          if (pop != null && !id.getType().equals(PopulationType.SIMULATION)
                              && !id.getType().equals(PopulationType.EXTERNAL)) {
                            // same instances for all
                            StatCollection bgDist = trace.getBaseline().getBackgroundDistribution();
                            TISeries tiSeries = trace.getTISeries();
                            double[] globalTime = tiSeries.getTime();

                            // SEARCH HEIGHT
                            ThresholdSupplier thr = pop.getHeightInstructions().get(bgDist);
                            TISeries xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);

                            DoubleDataSet ds = new DoubleDataSet("Search height");
                            ds.add(xyData.getTime(), xyData.getIntensity());

                            // strokeDashArray drives the dashed appearance
                            ds.setStyle("fx-stroke: " + Colors.toHex(UiColors.SEARCH_RED) + ";"
                                + "fx-fill: transparent;"
                                + "strokeDashArray: 8 4;");
                            thresholdMarkers.add(ds);


                            boolean searchAreEqual = pop.getStartInstructions().isEquivalent(
                                pop.getStopInstructions());
                            String startLabel = searchAreEqual ? "Search start and stop" : "Search start";

                            // SEARCH START
                            thr = pop.getStartInstructions().get(bgDist);
                            xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);

                            ds = new DoubleDataSet(startLabel);
                            ds.add(xyData.getTime(), xyData.getIntensity());

                            // strokeDashArray drives the dashed appearance
                            ds.setStyle("fx-stroke: " + Colors.toHex(UiColors.BASELINE_BLUE) + ";"
                                + "fx-fill: transparent;"
                                + "strokeDashArray: 8 4;");
                            thresholdMarkers.add(ds);


                            if (!searchAreEqual) {
                              // SEARCH STOP
                              thr = pop.getStopInstructions().get(bgDist);
                              xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);

                              ds = new DoubleDataSet("Search stop");
                              ds.add(xyData.getTime(), xyData.getIntensity());

                              // strokeDashArray drives the dashed appearance
                              ds.setStyle("fx-stroke: " + Colors.toHex(OkabeItoColors.BLACK_DARK) + ";"
                                  + "fx-fill: transparent;"
                                  + "strokeDashArray: 8 4;");
                              thresholdMarkers.add(ds);
                            }

                            // GATE
                            Colors gateColor = UiColors.GATING_MUSTARD;
                            Colors originalColor = gateColor;
                            List<ThresholdSupplierInstructions> gates = pop.getGatingInstr().stream()
                                .filter(ThresholdSupplierInstructions::isHeight)
                                .collect(Collectors.toList());

                            for (ThresholdSupplierInstructions instr : gates) {
                              thr = instr.get(bgDist);
                              xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);

                              ds = new DoubleDataSet(instr.getDescription());
                              ds.add(xyData.getTime(), xyData.getIntensity());

                              // strokeDashArray drives the dashed appearance
                              ds.setStyle("fx-stroke: " + Colors.toHex(gateColor) + ";"
                                  + "fx-fill: transparent;"
                                  + "strokeDashArray: 8 4;");
                              thresholdMarkers.add(ds);
                              gateColor = Colors.variationHSB(originalColor, gateColor, 1);
                            }
                          }
                        }
                      }
                      // BASELINE
                    }


                    Platform.runLater(() -> {
                      try {
                        // Build new chart FIRST
                        DefaultNumericAxis xAxis = new DefaultNumericAxis("Time", "s");
                        DefaultNumericAxis yAxis = new DefaultNumericAxis("Intensity",
                            IntensityUnit.CTS.getUiString());

                        XYChart chart = new XYChart(xAxis, yAxis);
                        chart.setTitle("");

                        Zoomer zoomer = new Zoomer();
                        zoomer.setSliderVisible(false);   // cleaner look without range sliders
                        zoomer.setPannerEnabled(true);  // pan with middle-click drag or ctrl+left-drag
                        chart.getPlugins().add(zoomer);
                        chart.getPlugins().add(new DataPointTooltip()); // hover to see values

                        /// LINES
                        ErrorDataSetRenderer lineRenderer = new ErrorDataSetRenderer();
                        lineRenderer.setDrawMarker(false);
                        lineRenderer.setErrorStyle(ErrorStyle.NONE); // disables error markers
                        lineRenderer.setPolyLineStyle(LineStyle.NORMAL);
                        for (DoubleDataSet ds : lineGraphs) {
                          lineRenderer.getDatasets().add(ds);
                        }
                        chart.getRenderers().add(lineRenderer);

                        ///  DOTS
                        ErrorDataSetRenderer markerRenderer = new ErrorDataSetRenderer();
                        markerRenderer.setDrawMarker(true);
                        markerRenderer.setPolyLineStyle(LineStyle.NONE);
                        //markerRenderer.setMarker(DefaultMarker.CIRCLE);
                        //markerRenderer.setMarkerSize(8);

                        for (DoubleDataSet ds : popMarkers) {
                          markerRenderer.getDatasets().add(ds);
                        }
                        for (DoubleDataSet ds : peakMarkers) {
                          markerRenderer.getDatasets().add(ds);
                        }

                        chart.getRenderers().add(markerRenderer);


                        /// THRESHOLDS
                        ErrorDataSetRenderer dashRenderer = new ErrorDataSetRenderer();
                        dashRenderer.setDrawMarker(false);
                        dashRenderer.setPolyLineStyle(LineStyle.NORMAL);

                        for (DoubleDataSet ds : thresholdMarkers) {
                          dashRenderer.getDatasets().add(ds);
                        }

                        chart.getRenderers().add(dashRenderer);

                        // Replace atomically — do NOT setCenter(null) first.
                        // Detaching while Prism render is in flight causes RTTexture NPE.
                        BorderPane tabPane = SpTool3Main.getRunTime().getGuiParameterManager()
                            .getRawDatMcTabPane();
                        tabPane.setCenter(chart);

                      } catch (Exception e) {
                        LOGGER.error("Error while plotting! "
                            + " Message: " + ExceptionUtils.getMessage(e)
                            + " Details: " + ExceptionUtils.getStackTrace(e));
                      }
                    });
                  }
                }
              }
            }
            progress.set(1d);
            return new EmptyTaskResult();
          } catch (Exception e) {
            LOGGER.error("Error while plotting! "
                + " Message: " + ExceptionUtils.getMessage(e)
                + " Details: " + ExceptionUtils.getStackTrace(e));
            return new EmptyTaskResult();
          }
        }
      };

      SpTool3Main.getRunTime().getTaskManager().forceToGraphPool(
          new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false, new EmptyTaskResult()));


    } // CHECK: There is data to show!

    public void refreshGraphJFree() {
      // The viewer exists in the background and we need to reset is stopped as well as progress each time
      progress.set(0d);
      isStopped.set(false);

      MonteCarloRawDataParameters plainSet = (MonteCarloRawDataParameters) super.getPlainSet();

      // update UI in FXThread
      UiUtil.showLoading(SpTool3Main.getRunTime().getGuiParameterManager().getRawDatMcTabPane());

      // retreive while in UI Thread
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      // Check which type of events to show?
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      WorkingTask graphTask = new WorkingTask() {
        @Override
        public String getTaskName() {
          return "Show raw data";
        }

        @Override
        public AtomicDouble getProgress() {
          return progress;
        }

        @Override
        public void stop() {
          isStopped.set(true);
        }

        @Override
        public TaskResult call() throws Exception {
          // LOGGER.trace("Refreshing raw data view.");

          try {
            if (samples != null && !samples.isEmpty()) {
              // so far we only show one sample in the MC raw view.
              Sample sample = samples.get(0);

              if (sample != null) {
                // merged samples will return the main sample
                sample = sample.getPrincipleSample();

                if (selChannels != null && !selChannels.isEmpty()) {

                  List<Trace> selTraces = sample.getTraces(selChannels);

                  if (!selTraces.isEmpty()) {

                    // max events to show per population
                    int maxEventsToShow = plainSet.getUpperPointCountCutoff().getValue().intValue();
                    Iterator<Colors> colorIterator = Colors.getDefaultIterator().iterator();
                    Iterator<MarkerStyle> markerIterator = MarkerStyle.getDefaultAwtIterator().iterator();

                    List<ChartComponent> mainCharts = new ArrayList<>();
                    List<ChartComponent> thresholdCharts = new ArrayList<>();
                    List<ChartComponent> populationMarkers = new ArrayList<>();
                    List<ChartComponent> peakTipMarkers = new ArrayList<>();

                    List<ChartComponent> allComponents = new ArrayList<>();
                    List<ChartComponent> peakTipMarkerLegendComponents = new ArrayList<>();
                    List<ChartComponent> allLegendComponents = new ArrayList<>();

                    /////////////////////////////////////////////////////////////////////////
                    //////////////////////// Simple Raw Data plot ///////////////////////////
                    /////////////////////////////////////////////////////////////////////////

                    traceLoop:
                    for (Trace trace : selTraces) {
                      // update progress
                      progress.set(0.5 * (double) selTraces.indexOf(trace) / (selTraces.size()));
                      if (isStopped.get()) {
                        break traceLoop;
                      }
                      // TISeries tiSeries = AnalysisUtils.getRawDataTISeries(trace);
                      // String seriesLabel = trace.getMzValue().getIsotope().getName();

                      Colors color = trace.getColor(sample);

                      // Check if log10. In that case, remove all zeros.
                      org.jfree.data.xy.XYSeries xySeries = trace.getXYSeries();

                      if (plainSet.getYLogScale().getValue()) {
                        org.jfree.data.xy.XYSeries copy = new org.jfree.data.xy.XYSeries(xySeries.getKey(),
                            xySeries.getAutoSort(), xySeries.getAllowDuplicateXValues());

                        for (int i = 0; i < xySeries.getItemCount(); i++) {
                          double x = xySeries.getX(i).doubleValue();
                          double y = xySeries.getY(i).doubleValue();

                          if (y == 0.0) {
                            y = 1E-3;
                          }

                          copy.add(x, y);
                        }
                        xySeries = copy;
                      }

                      mainCharts.add(new ChartComponent(
                          new ChartData(xySeries,
                              "Time", TimeUnit.SECOND, MathMod.NONE,
                              "Intensity", IntensityUnit.CTS, MathMod.NONE),
                          new ChartStyle(color, 1,
                              LineWidthDefaults.MEDIUM_THICK,
                              LineDashDefaults.STRAIGHT, 0f,
                              MarkerSizeDefaults.SMALL,
                              MarkerStyle.CROSS,
                              plainSet.getYLogScale().getValue(),
                              false,
                              RendererOption.SAMPLING_LINE_AND_SHAPE,
                              LineGraphStyle.LINE_AND_MARKER)
                      ));

                    }

                    if (plainSet.getShowEventMarkers().getValue() ||
                        plainSet.getShowPopulationMarkers().getValue()) {

                      List<PlottableEventMarkers> plottableEventMarkers =
                          AnalysisUtils.getEventMarkers(selTraces, selPops);

                      List<PlottableSubPopulation> plottablePopulations =
                          AnalysisUtils.getPopulationMarkers(selTraces, selPops);

                      plottableEventMarkers
                          .removeIf(p -> p.getEventMarkerData().size() > maxEventsToShow);
                      plottablePopulations.removeIf(p -> p.getTimerMarkers().length > maxEventsToShow);

                      /////////////////////////////////////////////////////////////////////////
                      //////////////////////// POPULATION MARKERS ///////////////////////////
                      /////////////////////////////////////////////////////////////////////////

                      if (plainSet.getShowPopulationMarkers().getValue()
                          && !plottablePopulations.isEmpty()) {

                        popLoop:
                        for (int i = 0; i < plottablePopulations.size(); i++) {
                          // update progress
                          progress.set(0.6 * (double) i / (plottablePopulations.size()));
                          if (isStopped.get()) {
                            break popLoop;
                          }

                          PlottableSubPopulation plotPop = plottablePopulations.get(i);

                          populationMarkers.add(new ChartComponent(
                              new ChartData(plotPop.getPopLabel(),
                                  plotPop.getTimerMarkers(),
                                  ArrUtils.fillArray(-(2 + 2 * i),
                                      plotPop.size()),
                                  "Time", TimeUnit.SECOND, MathMod.NONE,
                                  "Intensity", IntensityUnit.CTS, MathMod.NONE),
                              new ChartStyle(
                                  plotPop.getColor() != null ? plotPop.getColor() : colorIterator.next(),
                                  1,
                                  LineWidthDefaults.NONE,
                                  LineDashDefaults.STRAIGHT, 0f,
                                  MarkerSizeDefaults.LARGE,
                                  plotPop.getMarker() != null ? plotPop.getMarker()
                                      : markerIterator.next(),
                                  plainSet.getYLogScale().getValue(),
                                  true,
                                  RendererOption.LINE_AND_SHAPE,
                                  LineGraphStyle.MARKER)
                          ));
                        }
                      }

                      /////////////////////////////////////////////////////////////////////////
                      //////////////////////////////// PEAK MARKERS ///////////////////////////
                      /////////////////////////////////////////////////////////////////////////
                      if (plainSet.getShowEventMarkers().getValue() && !plottableEventMarkers.isEmpty()) {

                        eventLoop:
                        for (int i = 0; i < plottableEventMarkers.size(); i++) {
                          // update progress
                          progress.set(0.7 * (double) i / (plottableEventMarkers.size()));
                          if (isStopped.get()) {
                            break eventLoop;
                          }

                          PlottableEventMarkers eventMarkerData = plottableEventMarkers.get(i);
                          ChartComponent comp = new ChartComponent(
                              new ChartData(eventMarkerData.getSeriesLabel(),
                                  eventMarkerData.getEventMarkerData().getTime(),
                                  eventMarkerData.getEventMarkerData().getIntensity(),
                                  "Time", TimeUnit.SECOND, MathMod.NONE,
                                  "Intensity", IntensityUnit.CTS, MathMod.NONE),
                              new ChartStyle(Colors.variationHSB(eventMarkerData.getColor(),
                                  eventMarkerData.getColor(), i - 1), 0.8,
                                  LineWidthDefaults.NONE,
                                  LineDashDefaults.STRAIGHT, 0f,
                                  MarkerSizeDefaults.LARGE,
                                  eventMarkerData.getMarker(),
                                  true,
                                  RendererOption.LINE_AND_SHAPE,
                                  LineGraphStyle.MARKER)
                          );
                          peakTipMarkers.add(comp);

                          // If we have sim and eval, show triangle and bar.
                          boolean needsLegendSymbol = peakTipMarkerLegendComponents.stream()
                              .noneMatch(c -> c.getStyle().getMarkerStyle()
                                  .equals(comp.getStyle().getMarkerStyle()));
                          if (needsLegendSymbol) {
                            peakTipMarkerLegendComponents.add(comp);
                          }
                        }
                      }
                    }

                    /////////////////////////////////////////////////////////////////////////
                    //////////////////////////////// THRESHOLDS ///////////////////////////
                    /////////////////////////////////////////////////////////////////////////
                    if (plainSet.getShowThresholdMarkers().getValue()) {
                      // BASELINE
                      List<PopulationID> selPopList = SpTool3Main.getRunTime().getMainWindowCtl()
                          .getSelPops();

                      traceLoop:
                      for (Trace trace : selTraces) {
                        // update progress:
                        progress.set(0.8 + (double) selTraces.indexOf(trace) / (selTraces.size()));
                        if (isStopped.get()) {
                          break traceLoop;
                        }
                        for (PopulationID id : selPopList) {
                          Population pop = trace.getPopulation(id);
                          if (pop != null && !id.getType().equals(PopulationType.SIMULATION)
                              && !id.getType().equals(PopulationType.EXTERNAL)) {
                            // same instances for all
                            StatCollection bgDist = trace.getBaseline().getBackgroundDistribution();
                            TISeries tiSeries = trace.getTISeries();
                            double[] globalTime = tiSeries.getTime();

                            // SEARCH HEIGHT
                            ThresholdSupplier thr = pop.getHeightInstructions().get(bgDist);
                            TISeries xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);
                            thresholdCharts.add(new ChartComponent(
                                new ChartData("Search height",
                                    xyData.getTime(),
                                    xyData.getIntensity(),
                                    "Time", TimeUnit.SECOND, MathMod.NONE,
                                    "Intensity", IntensityUnit.CTS, MathMod.NONE),
                                new ChartStyle(UiColors.SEARCH_RED, 0.95,
                                    LineWidthDefaults.THICKER,
                                    LineDashDefaults.XL, 2f,
                                    MarkerSizeDefaults.SMALL,
                                    MarkerStyle.BAR,
                                    plainSet.getYLogScale().getValue(),
                                    true,
                                    RendererOption.SAMPLING_LINE_AND_SHAPE,
                                    LineGraphStyle.LINE_AND_MARKER)
                            ));

                            boolean searchAreEqual = pop.getStartInstructions().isEquivalent(
                                pop.getStopInstructions());
                            String startLabel = searchAreEqual ? "Search start and stop" : "Search start";

                            // SEARCH START
                            thr = pop.getStartInstructions().get(bgDist);
                            xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);
                            thresholdCharts.add(new ChartComponent(
                                new ChartData(startLabel,
                                    xyData.getTime(),
                                    xyData.getIntensity(),
                                    "Time", TimeUnit.SECOND, MathMod.NONE,
                                    "Intensity", IntensityUnit.CTS, MathMod.NONE),
                                new ChartStyle(UiColors.BASELINE_BLUE, 0.7,
                                    LineWidthDefaults.THICKER,
                                    LineDashDefaults.L, 1f,
                                    MarkerSizeDefaults.SMALL,
                                    MarkerStyle.BAR,
                                    plainSet.getYLogScale().getValue(),
                                    true,
                                    RendererOption.SAMPLING_LINE_AND_SHAPE,
                                    LineGraphStyle.LINE_AND_MARKER)
                            ));

                            if (!searchAreEqual) {
                              // SEARCH STOP
                              thr = pop.getStopInstructions().get(bgDist);
                              xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);
                              thresholdCharts.add(new ChartComponent(
                                  new ChartData("Search stop",
                                      xyData.getTime(),
                                      xyData.getIntensity(),
                                      "Time", TimeUnit.SECOND, MathMod.NONE,
                                      "Intensity", IntensityUnit.CTS, MathMod.NONE),
                                  new ChartStyle(OkabeItoColors.BLACK_DARK, 0.5,
                                      LineWidthDefaults.THICKER,
                                      LineDashDefaults.L, 1f,
                                      MarkerSizeDefaults.SMALL,
                                      MarkerStyle.BAR,
                                      plainSet.getYLogScale().getValue(),
                                      true,
                                      RendererOption.SAMPLING_LINE_AND_SHAPE,
                                      LineGraphStyle.LINE_AND_MARKER)
                              ));
                            }

                            // GATE
                            Colors gateColor = UiColors.GATING_MUSTARD;
                            Colors originalColor = gateColor;
                            List<ThresholdSupplierInstructions> gates = pop.getGatingInstr().stream()
                                .filter(ThresholdSupplierInstructions::isHeight)
                                .collect(Collectors.toList());

                            for (ThresholdSupplierInstructions instr : gates) {
                              thr = instr.get(bgDist);
                              xyData = AnalysisUtils.getThresholdXY(thr, tiSeries, globalTime);
                              thresholdCharts.add(new ChartComponent(
                                  new ChartData(instr.getDescription(),
                                      xyData.getTime(),
                                      xyData.getIntensity(),
                                      "Time", TimeUnit.SECOND, MathMod.NONE,
                                      "Intensity", IntensityUnit.CTS, MathMod.NONE),
                                  new ChartStyle(gateColor, 0.6,
                                      LineWidthDefaults.THICKER,
                                      LineDashDefaults.L, 3f,
                                      MarkerSizeDefaults.SMALL,
                                      MarkerStyle.BAR,
                                      plainSet.getYLogScale().getValue(),
                                      true,
                                      RendererOption.SAMPLING_LINE_AND_SHAPE,
                                      LineGraphStyle.LINE_AND_MARKER)
                              ));
                              gateColor = Colors.variationHSB(originalColor, gateColor, 1);
                            }
                          }
                        }
                      }
                      // BASELINE
                    }

                    // Collections.reverse(mainCharts); // ascending isotope order

                    allComponents.addAll(mainCharts);
                    allComponents.addAll(thresholdCharts); // plot thr ABOVE data
                    allComponents.addAll(populationMarkers);
                    allComponents.addAll(peakTipMarkers);

                    allLegendComponents.addAll(mainCharts);
                    allLegendComponents.addAll(peakTipMarkerLegendComponents);
                    allLegendComponents.addAll(populationMarkers);

                    // As thresholds have the same color coding, just add the legend item once
                    HashMap<String, ChartComponent> thresholdLegends = new LinkedHashMap<>();
                    for (ChartComponent thresholdChart : thresholdCharts) {
                      thresholdLegends.put(thresholdChart.getData().getSeriesName(), thresholdChart);
                    }
                    allLegendComponents.addAll(thresholdLegends.values());

                    Platform.runLater(() -> {
                      try {
                        // Build new chart FIRST
                        JFreeChart chart = SpChartFactory.createLineChart(allComponents);

                        // axis limit
                        // Check to adjust axis range
                        if (plainSet.getLimitAxes().getValue()) {
                          Range yRange = chart.getXYPlot().getRangeAxis().getRange();
                          if (plainSet.getUpperYLimit().getValue() > yRange.getLowerBound()) {
                            chart.getXYPlot().getRangeAxis().setRange(new Range(yRange.getLowerBound(),
                                plainSet.getUpperYLimit().getValue()));
                          }
                        }

                        ChartContainer container = SpChartFactory.bundleChartLegend(
                            viewerRef.get(),
                            chart,
                            allLegendComponents,
                            800, 500,
                            false,
                            Orientation.VERTICAL,
                            false);

                        viewerRef.set(container.viewer);

                        BorderPane tabPane = SpTool3Main.getRunTime().getGuiParameterManager()
                            .getRawDatMcTabPane();

                        // Replace atomically — do NOT setCenter(null) first.
                        // Detaching while Prism render is in flight causes RTTexture NPE.
                        tabPane.setCenter(container.combinedPane);

                      } catch (Exception e) {
                        LOGGER.error("Error while plotting! "
                            + " Message: " + ExceptionUtils.getMessage(e)
                            + " Details: " + ExceptionUtils.getStackTrace(e));
                      }
                    });
                  }
                }
              }
            }
            progress.set(1d);
            return new EmptyTaskResult();
          } catch (Exception e) {
            LOGGER.error("Error while plotting! "
                + " Message: " + ExceptionUtils.getMessage(e)
                + " Details: " + ExceptionUtils.getStackTrace(e));
            return new EmptyTaskResult();
          }
        }
      };

      SpTool3Main.getRunTime().getTaskManager().forceToGraphPool(
          new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false, new EmptyTaskResult()));


    } // CHECK: There is data to show!
  } // END OF FUNCTION


  public static class IclPeakViewer extends ViewerFXParamSet {

    public IclPeakViewer(IclPeakParameters plainSet) {
      super(plainSet);
    }

    @Override
    public void refreshGraph() {
      IclPeakParameters plainSet = (IclPeakParameters) super.getPlainSet();

      if (plainSet != null) {
        PeakFunction peak = plainSet.getPeakFunction();

        double windowViewMilliSec = plainSet.getViewerTimeWindow() / 1E3;
        double dtSec = plainSet.getDwellTime() / 1E6;
        double peakMillis = plainSet.getPeakPosition() / 1E3;

        BaseAbstractUnivariateIntegrator integrator = new RombergIntegrator();
        DataList<Integer> data = peak.integrateEntirePeak(integrator,
            dtSec, 50, (int) 1E9, peakMillis / 1E3);

        data = PeakFunction.normalizeArea(data, plainSet.getArea());
        double yMaxBeforeRandom = data.yMax();
        double extdYMaxBeforeRandom = yMaxBeforeRandom + 3.29 * Math.sqrt(yMaxBeforeRandom);

        // make sure, there is always a zero at the left and the right of the peak
        double[] signal = new double[data.getData().size() + 2];
        double[] integTime = new double[data.getData().size() + 2];

        /*
        Consider DT = 10:
              t=    10, 20, 30, ...
              idx=  0,  1,  2,

        How do we obtain time stamps from the DT index?: (index+1) · DT.
         */
        for (int i = 0; i < data.getData().size(); i++) {
          integTime[i + 1] = (data.getData().get(i).getX() + 1) * dtSec * 1E3 - peakMillis;
          signal[i + 1] = data.getData().get(i).getY();
        }

        integTime[0] = integTime[1] - dtSec * 1E3;
        integTime[integTime.length - 1] = integTime[integTime.length - 2] + dtSec * 1E3;

        // add bg
        double bgLevel = plainSet.getBackgroundLevel();
        ArrUtils.addOverriding(signal, bgLevel);

        if (plainSet.isResample()) {
          // This sets seed for Poisson
          if (plainSet.isUseRNGSeed()) {
            Statistics.setXoroSeed(plainSet.getSeed());
          }
          Statistics.resamplePoissonDynamically(signal);

          if (plainSet.getPDF().equals(PDF.COMPOUND_POISSON)) {
            double siaShape = plainSet.getSiaShape();
            if (plainSet.isUseRNGSeed()) {
              Statistics.resampleCompoundPoisson(signal, siaShape, plainSet.getSeed());
            } else {
              Statistics.resampleCompoundPoisson(signal, siaShape);
            }
          }
        }

        List<ChartComponent> charts = new ArrayList<>();

        double iclSecAt5Pct = peak.getIcl();

        double skew = PeakSymmetry.calculateAsymmetryFactor(signal);

        ChartComponent chartComponent = new ChartComponent(
            new ChartData(
                "Integrated ion cloud profile."
                    + " FWHM=" + SnF.doubleToString(peak.getFwhm() * 1E6, NF.D1C0) + "µs,"
                    + " Width at 5%: "
                    + SnF.doubleToString(iclSecAt5Pct * 1E6, NF.D1C0) + "µs,"
                    + " AF="
                    + SnF.doubleToString(skew, NF.D1C2),
                integTime,
                signal,
                "Time", TimeUnit.MILLISECOND, MathMod.NONE,
                "Intensity", IntensityUnit.CTS, MathMod.NONE
            ),
            new ChartStyle(OkabeItoColors.ORANGE_DARK, 1,
                LineWidthDefaults.THICKER, //MEDIUM_THICK
                LineDashDefaults.STRAIGHT, 0f,
                MarkerSizeDefaults.SMALL,
                MarkerStyle.CROSS,
                false,
                RendererOption.LINE_AND_SHAPE,
                LineGraphStyle.LINE_AND_MARKER
            )
        );
        charts.add(chartComponent);

        // Check if second peak with gridlines is to be shown
        if (plainSet.isShowDTGrid()) {
          double gridDTSec = plainSet.getGridIntegrationTime() / 1E6;
          if (gridDTSec < dtSec) {

            DataList<Integer> dataMicro = peak.integrateEntirePeak(integrator,
                gridDTSec, 50, (int) 1E9, peakMillis / 1E3);
            // Although incorrect, normalize to same height
            dataMicro = PeakFunction.normalizeHeight(dataMicro, yMaxBeforeRandom);

            // Would be more accurate but looks bad
            // data1us = PeakFunction.normalizeArea(data1us, plainSet.getArea());

            double[] signalMicro = new double[dataMicro.getData().size()];
            double[] integTimeMicro = new double[dataMicro.getData().size()];

            // Here we can ignore to add zeros, since the integration is at low time res.
            for (int i = 0; i < dataMicro.getData().size(); i++) {
              integTimeMicro[i] =
                  (dataMicro.getData().get(i).getX() + 1) * gridDTSec * 1E3 - peakMillis;
              signalMicro[i] = dataMicro.getData().get(i).getY();
            }

            // add bg
            /*
            This would be mathematically more accurate but the point of the plot is
            not to show the true intensity at the lower DT but to give a visual.
            double bgLevelMicro = bgLevel * gridDTSec/dtSec;
             */
            ArrUtils.addOverriding(signalMicro, bgLevel);

            charts.add(new ChartComponent(
                new ChartData(
                    "'True' peak shape.",
                    integTimeMicro,
                    signalMicro,
                    "Time", TimeUnit.MILLISECOND, MathMod.NONE,
                    "Intensity", IntensityUnit.CTS, MathMod.NONE
                ),
                new ChartStyle(
                    OkabeItoColors.VIOLET_DARK, 0.4,
                    LineWidthDefaults.THICKER,
                    LineDashDefaults.STRAIGHT, 0f,
                    MarkerSizeDefaults.SMALL,
                    MarkerStyle.CROSS,
                    false,
                    RendererOption.AREA,
                    LineGraphStyle.LINE
                )
            ));

            // Show markers that indicate the macro dwell times
            // (essentially, every DP in the macro frame, but show as vertical lines).
            List<Double> xMarker = new ArrayList<>();
            List<Double> yMarker = new ArrayList<>();

            double markerHeight = Math.max(yMaxBeforeRandom, ArrUtils.getMax(signal));

            for (int i = 0; i < integTime.length; i++) {

              // check each time stamp that the integrated peak has:
              double tMillis = integTime[i];

              xMarker.add(tMillis);
              yMarker.add(0d);

              xMarker.add(tMillis);
              yMarker.add(markerHeight);

              xMarker.add(tMillis);
              yMarker.add(0d);

            }

            charts.add(new ChartComponent(
                new ChartData(
                    "Dwell time limits.",
                    ArrUtils.doubleListToArr(xMarker),
                    ArrUtils.doubleListToArr(yMarker),
                    "Time", TimeUnit.MILLISECOND, MathMod.NONE,
                    "Intensity", IntensityUnit.CTS, MathMod.NONE
                ),
                new ChartStyle(
                    UiColors.PLOT_ANY_AXIS_MARKER, 0.75,
                    LineWidthDefaults.THINNER,
                    LineDashDefaults.STRAIGHT, 0f,
                    MarkerSizeDefaults.SMALL,
                    MarkerStyle.RECTANGLE,
                    true,
                    RendererOption.LINE_AND_SHAPE,
                    LineGraphStyle.LINE
                )
            ));
          }
        }

        Collections.reverse(charts);
        JFreeChart chart = SpChartFactory.createLineChart(charts);
        Node viewNode = SpChartFactory.bundleChartLegend(chart,
            charts, 800, 500).combinedPane;

        double upX = windowViewMilliSec * 1.3;
        // cannot go lower than -peakMillis, which is the "zero" in the ref frame of the peak function
        double lowX = Math.max(-windowViewMilliSec, -peakMillis * 1.2);
        chart.getXYPlot().getDomainAxis().setRange(new Range(lowX, upX));

        if (plainSet.isResample() && plainSet.isShowDTGrid()) {
          double lowY = chart.getXYPlot().getRangeAxis().getRange().getLowerBound();
          double upY = chart.getXYPlot().getRangeAxis().getRange().getUpperBound();

          // Use one scale for all perspectives (extdYMaxBeforeRandom), unless random number was larger.
          // Hence the Math.max();
          upY = Math.max(upY, extdYMaxBeforeRandom);
          chart.getXYPlot().getRangeAxis().setRange(new Range(lowY, upY));
        }

        if (plainSet.isShowDTGrid()) {
          chart.getXYPlot().setRangeGridlinesVisible(false);
          chart.getXYPlot().setDomainGridlinesVisible(false);
        }

        // Keep all in the same pulse/runlater of the UI thread
        targetPane.setCenter(viewNode);
      }
    }
  }


  public static class MonteCarloScatterPlotViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    private final AtomicReference<ChartViewer> viewerRef = new AtomicReference<>(null);

    private final Button startPolygonBtn = UiUtil.getImageButton("Draw", "/img/drawPolygon.png",
        "Start drawing a polygon in the scatter plot");
    private final Button addPopulation = UiUtil.getImageButton("Store", "/img/selectAll.png",
        "Store polygons as new populations");
    private final Button clearPolygons = UiUtil.getImageButton("Clear", "/img/clearPolygon.png",
        "Clear all polygons from the scatter plot");
    private final Button removeAllPolygonPopsBtn = UiUtil.getImageButton("Delete", "/img/delete.png",
        "Delete all populations in the sample that are of type 'polygon'");

    private final HBox hbox = new HBox(5, startPolygonBtn, addPopulation, clearPolygons,
        removeAllPolygonPopsBtn);
    private final List<Pair<PolygonOverlay, AtomicBoolean>> polygons = new ArrayList<>();
    private final List<Pair<PolygonOverlay, AtomicBoolean>> backupPolygons = new ArrayList<>();

    public MonteCarloScatterPlotViewer(MonteCarloScatterPlotParameters plainSet) {
      super(plainSet);
    }

    @Override
    public double getValueWidth() {
      return 100;
    }

    @Override
    public void refreshGraph() {
      MonteCarloScatterPlotParameters plainSet = (MonteCarloScatterPlotParameters) super.getPlainSet();

      // The viewer exists in the background and we need to reset is stopped as well as progress each time
      progress.set(0d);
      isStopped.set(false);

      // Sample selection: retrieve in UI thread
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      List<PopulationID> selPop = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
      Unit unit = SpTool3Main.getRunTime().getMainWindowCtl().getUnit();

      // update UI in FXThread
      UiUtil.showLoading(targetPane);

      // delete the points
      for (Pair<PolygonOverlay, AtomicBoolean> polygon : polygons) {
        polygon.getKey().clear();
      }
      polygons.clear();

      startPolygonBtn.setOnAction(e -> {
        // unlisten prev polygons
        for (Pair<PolygonOverlay, AtomicBoolean> polygon : polygons) {
          polygon.getValue().set(false);
        }

        // make new polygon
        PolygonOverlay overlay = new PolygonOverlay();
        AtomicBoolean listen = new AtomicBoolean(true);
        PolygonOverlay.enablePolygon(viewerRef.get(), overlay, listen);
        Pair<PolygonOverlay, AtomicBoolean> pair = new Pair<>(overlay, listen);
        polygons.add(pair);
        backupPolygons.add(pair);
      });

      clearPolygons.setOnAction(e -> {
        NotificationFactory.openYesCancel(
            "Clear polygons drawn in scatter plot? This is irreversible.", () -> {
              // delete the points
              for (Pair<PolygonOverlay, AtomicBoolean> polygon : polygons) {
                polygon.getValue().set(false); // silence all listeners
                polygon.getKey().clear();
              }
              polygons.clear();
              // sometimes polygons get stuck - this helps to definitively clear them
              ChartViewer viewer = viewerRef.get();
              if (viewer != null) {
                ChartCanvas chartPanel = (ChartCanvas) viewer.getCanvas();
                for (Pair<PolygonOverlay, AtomicBoolean> backupPolygon : backupPolygons) {
                  backupPolygon.getValue().set(false); // silence all listeners
                  backupPolygon.getKey().clear();
                  chartPanel.removeOverlay(backupPolygon.getKey());
                }
                backupPolygons.clear();
              }
            });
      });

      // TODO: Clean this up!
      // TODO 2: consider moving parts of the chart creation out of the parallel task (c.f. raw plot)
      addPopulation.setOnAction(e -> {
        /*
        ##############################################################################
        ################## Scatter all Isotopes ##############################
        ##############################################################################
        */

        if (!plainSet.getScatterIsotopes().getValue()) {

          // check if aligned version is present
          boolean isAligned = false;
          if (selChannels.size() == 1) {
            isAligned = AnalysisUtils.isAnyAlignedOrPVal(selPop);
          }

          if (isAligned) {
            for (Sample sample : samples) {

              for (PopulationID populationID : selPop) {

                for (int i = 0; i < polygons.size(); i++) {
                  HashSet<Integer> regionsInPolygon = new HashSet<>();

                  Pair<PolygonOverlay, AtomicBoolean> polygon = polygons.get(i);
                  // construct double precision path from the polygon points
                  List<Point2D> pts = polygon.getKey().getPoints();
                  if (pts.size() >= 3) {

                    // keep for the population
                    double[] xVertices = new double[pts.size()];
                    double[] yVertices = new double[pts.size()];

                    // construct path
                    java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                    path.moveTo(pts.get(0).getX(), pts.get(0).getY());
                    xVertices[0] = pts.get(0).getX();
                    yVertices[0] = pts.get(0).getY();

                    for (int j = 1; j < pts.size(); j++) {
                      xVertices[j] = pts.get(j).getX();
                      yVertices[j] = pts.get(j).getY();
                      path.lineTo(xVertices[j], yVertices[j]);
                    }
                    path.closePath();

                    for (Channel channel : selChannels) {

                      double[] xData = sample.getData(channel, populationID,
                          EventType.NP, plainSet.getEventParameterX().getValue(), unit);

                      double[] yData = sample.getData(channel, populationID,
                          EventType.NP, plainSet.getEventParameterY().getValue(), unit);

                      List<Integer> validIndices = new ArrayList<>();
                      if (plainSet.isComputeNonzero()) {
                        Pair<double[], double[]> nz = ArrUtils.strictlyGreaterThan(xData, yData,
                            validIndices, 0);
                        xData = nz.getKey();
                        yData = nz.getValue();
                      }

                      xData = plainSet.getMathModificationX().getValue().calc(xData);
                      yData = plainSet.getMathModificationY().getValue().calc(yData);

                      Trace trace = sample.getTrace(channel);
                      if (trace != null) {
                        Population pop = trace.getPopulation(populationID);
                        if (pop != null) {
                          EventCollection collection = pop.getEvents();
                          List<Event> events = collection.getNpEvents();

                          // create component
                          if (xData.length > 0 && yData.length > 0 && !events.isEmpty()
                              && xData.length == yData.length && events.size() == xData.length) {

                            for (int n = 0; n < xData.length; n++) {
                              if (path.contains(xData[n], yData[n])) {
                                regionsInPolygon.add(n);
                              }
                            }
                          }
                        }
                      }

                      // if we remove events according to nonzero criterion, we have to remove them here too
                      if (plainSet.isComputeNonzero()) {
                        regionsInPolygon.removeIf(idx -> !validIndices.contains(idx));
                      }
                    }

                    // now construct population for all isotopes
                    List<Channel> allChannels = sample.listChannels();
                    for (Channel allChannel : allChannels) {

                      // create population
                      if (!regionsInPolygon.isEmpty()) {

                        Trace trace = sample.getTrace(allChannel);
                        if (trace != null) {
                          Population pop = trace.getPopulation(populationID);
                          // If pop==null, we know that the isotope never had this population (e.g. b/c
                          // exclusion rule)
                          if (pop != null) {
                            EventCollection collection = pop.getEvents();
                            List<Event> events = collection.getNpEvents();

                            MainEventCollection newCollection = new SubEventCollection(trace, collection);
                            for (Integer idx : regionsInPolygon) {
                              if (idx < events.size()) {
                                newCollection.add(events.get(idx));
                              }
                            }

                            PopulationID idCopy = new PopulationID(populationID);
                            idCopy.append((new PopulationStep.PolygonSubtype(
                                "PLGN_" + i, xVertices, yVertices)));

                            trace.addOverridePopulation(idCopy,
                                new NpPopulation(
                                    idCopy,
                                    pop,
                                    newCollection,
                                    idCopy.toString(),
                                    pop.getContributingChannels()
                                ),
                                true);

                          }
                        }
                      }
                    }
                  }
                }
              }
            }

          /*
          ##############################################################################
          ############## Scatter all isotopes but is not aligned nor p-value ###########
          ##############################################################################
           */
          } else {

            for (Sample sample : samples) {
              for (Channel channel : selChannels) {
                for (PopulationID populationID : selPop) {

                  double[] xData = sample.getData(channel, populationID,
                      EventType.NP, plainSet.getEventParameterX().getValue(), unit);

                  double[] yData = sample.getData(channel, populationID,
                      EventType.NP, plainSet.getEventParameterY().getValue(), unit);

                  List<Integer> validIndices = new ArrayList<>();
                  if (plainSet.isComputeNonzero()) {
                    Pair<double[], double[]> nz = ArrUtils.strictlyGreaterThan(xData, yData, validIndices, 0);
                    xData = nz.getKey();
                    yData = nz.getValue();
                  }

                  xData = plainSet.getMathModificationX().getValue().calc(xData);
                  yData = plainSet.getMathModificationY().getValue().calc(yData);

                  Trace trace = sample.getTrace(channel);
                  if (trace != null) {
                    Population pop = trace.getPopulation(populationID);
                    // If pop==null, we know that the isotope never had this population (e.g. b/c
                    // exclusion rule)
                    if (pop != null) {
                      EventCollection collection = pop.getEvents();
                      List<Event> events = collection.getNpEvents();
                      List<Event> validEvents = new ArrayList<>();

                      // if we remove events according to nonzero criterion, we have to remove them here too
                      if (plainSet.isComputeNonzero()) {
                        for (Integer validIndex : validIndices) {
                          if (validIndex < events.size()) {
                            validEvents.add(events.get(validIndex));
                          }
                        }
                        events = validEvents;
                      }

                      // create component
                      if (xData.length > 0 && yData.length > 0 && !events.isEmpty()
                          && xData.length == yData.length && events.size() == xData.length) {

                        for (int i = 0; i < polygons.size(); i++) {

                          Pair<PolygonOverlay, AtomicBoolean> polygon = polygons.get(i);
                          List<Event> eventsInPolygon = new ArrayList<>();

                          // construct double precision path from the polygon points
                          List<Point2D> pts = polygon.getKey().getPoints();
                          if (pts.size() >= 3) {

                            // keep for the population
                            double[] xVertices = new double[pts.size()];
                            double[] yVertices = new double[pts.size()];

                            // construct path
                            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                            path.moveTo(pts.get(0).getX(), pts.get(0).getY());
                            xVertices[0] = pts.get(0).getX();
                            yVertices[0] = pts.get(0).getY();

                            for (int j = 1; j < pts.size(); j++) {
                              xVertices[j] = pts.get(j).getX();
                              yVertices[j] = pts.get(j).getY();
                              path.lineTo(xVertices[j], yVertices[j]);
                            }
                            path.closePath();


                            for (int n = 0; n < xData.length; n++) {
                              if (path.contains(xData[n], yData[n])) {
                                eventsInPolygon.add(events.get(n));
                              }
                            }

                            // create population
                            if (!eventsInPolygon.isEmpty()) {

                              MainEventCollection newCollection = new SubEventCollection(trace, collection);
                              newCollection.add(eventsInPolygon);
                              // isotope!!!

                              PopulationID idCopy = new PopulationID(populationID);
                              idCopy.append((new PopulationStep.PolygonSubtype(
                                  "PLGN_" + i, xVertices, yVertices)));

                              trace.addOverridePopulation(idCopy,
                                  new NpPopulation(
                                      idCopy,
                                      pop,
                                      newCollection,
                                      idCopy.toString(),
                                      pop.getContributingChannels()
                                  ),
                                  true);

                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          SpTool3Main.getRunTime().getMainWindowCtl().updatePopulations();

          /*
          ##############################################################################
          ############## Scatter 2 isotopes only #######################################
          ##############################################################################
           */
        } else {
          for (Sample sample : samples) {

            if (selChannels.size() > 1) {
              // add the regions (i.e., data on all isotopes) based on these 2 selected isotopes
              Channel channelX = selChannels.get(0);
              Channel channelY = selChannels.get(1);

              for (int i = 0; i < selPop.size(); i++) {
                PopulationID populationID = selPop.get(i);

                // check if aligned
                boolean isAlignedOrP = AnalysisUtils.isAlignedOrPVal(populationID);

                if (isAlignedOrP) {

                  double[] xData = sample.getData(channelX, populationID,
                      EventType.NP, plainSet.getEventParameterX().getValue(), unit);

                  double[] yData = sample.getData(channelY, populationID,
                      EventType.NP, plainSet.getEventParameterY().getValue(), unit);

                  List<Integer> validIndices = new ArrayList<>();
                  if (plainSet.isComputeNonzero()) {
                    Pair<double[], double[]> nz = ArrUtils.strictlyGreaterThan(xData, yData, validIndices, 0);
                    xData = nz.getKey();
                    yData = nz.getValue();
                  }

                  xData = plainSet.getMathModificationX().getValue().calc(xData);
                  yData = plainSet.getMathModificationY().getValue().calc(yData);

                  // iterate over all isotopes and add events that are within the region
                  // (we have to assume that indices on event (i.e., region) in one isotope matches all
                  // others)
                  List<Channel> allChannels = sample.listChannels();

                  for (int ij = 0; ij < allChannels.size(); ij++) {
                    Channel channel = allChannels.get(ij);
                    Trace trace = sample.getTrace(channel);
                    if (trace != null) {
                      Population pop = trace.getPopulation(populationID);
                      // If pop==null, we know that the isotope never had this population (e.g. b/c
                      // exclusion rule)
                      if (pop != null) {
                        EventCollection collection = pop.getEvents();
                        List<Event> events = collection.getNpEvents();
                        List<Event> validEvents = new ArrayList<>();

                        // if we remove events according to nonzero criterion, we have to remove them here too
                        if (plainSet.isComputeNonzero()) {
                          for (Integer validIndex : validIndices) {
                            if (validIndex < events.size()) {
                              validEvents.add(events.get(validIndex));
                            }
                          }
                          events = validEvents;
                        }

                        // create component
                        if (!events.isEmpty() && xData.length > 0 && yData.length > 0
                            && xData.length == yData.length && events.size() == xData.length) {

                          for (int ip = 0; ip < polygons.size(); ip++) {

                            Pair<PolygonOverlay, AtomicBoolean> polygon = polygons.get(ip);
                            List<Event> eventsInPolygon = new ArrayList<>();

                            // construct double precision path from the polygon points
                            List<Point2D> pts = polygon.getKey().getPoints();
                            if (pts.size() >= 3) {

                              // keep for the population
                              double[] xVertices = new double[pts.size()];
                              double[] yVertices = new double[pts.size()];

                              // construct path
                              java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                              path.moveTo(pts.get(0).getX(), pts.get(0).getY());
                              xVertices[0] = pts.get(0).getX();
                              yVertices[0] = pts.get(0).getY();

                              for (int j = 1; j < pts.size(); j++) {
                                xVertices[j] = pts.get(j).getX();

                                yVertices[j] = pts.get(j).getY();
                                path.lineTo(xVertices[j], yVertices[j]);
                              }
                              path.closePath();


                              for (int n = 0; n < xData.length; n++) {
                                if (path.contains(xData[n], yData[n])) {
                                  eventsInPolygon.add(events.get(n));
                                }
                              }

                              // create population
                              if (!eventsInPolygon.isEmpty()) {
                                MainEventCollection coll = new SubEventCollection(trace, collection);
                                coll.add(eventsInPolygon);

                                PopulationID idCopy = new PopulationID(populationID);
                                idCopy.append((new PopulationStep.PolygonSubtype("PLGN_" + ip,
                                    xVertices, yVertices)));

                                trace.addOverridePopulation(idCopy,
                                    new NpPopulation(
                                        idCopy,
                                        pop,
                                        coll,
                                        idCopy.toString(),
                                        pop.getContributingChannels()
                                    ),
                                    true);
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          SpTool3Main.getRunTime().getMainWindowCtl().updatePopulations();
        }

        // Finally clear the previous polygons
        // delete the points
        for (Pair<PolygonOverlay, AtomicBoolean> polygon : polygons) {
          polygon.getValue().set(false); // silence all listeners
          polygon.getKey().clear();
        }
        polygons.clear();
      });


      removeAllPolygonPopsBtn.setOnAction(e -> {

        NotificationFactory.openYesCancel(
            "Remove all populations of polygon-type? This is irreversible.", () -> {
              for (Sample sample : samples) {
                List<Channel> isotopes = sample.listChannels();
                List<PopulationID> popIDs = sample.listAllPopulations().stream()
                    .filter(populationID -> populationID.getSteps().stream()
                        .anyMatch(step -> step instanceof PopulationStep.PolygonSubtype))
                    .toList();

                popIDs.forEach(id -> sample.removePopulations(isotopes, id));
              }
              SpTool3Main.getRunTime().getMainWindowCtl().updatePopulations();
            });
      });

      // add buttons
      targetPane.setTop(hbox);

      WorkingTask graphTask = new WorkingTask() {
        @Override
        public String getTaskName() {
          return "Show scatter plot";
        }

        @Override
        public AtomicDouble getProgress() {
          return progress;
        }

        @Override
        public void stop() {
          isStopped.set(true);
        }

        @Override
        public TaskResult call() throws Exception {
          try {
            List<ChartComponent> chartComponents = new ArrayList<>();

            if (plainSet != null) {
              // Define axis labels
              AxisLabel xAxisLabel = AxisLabel.getUnit(plainSet.getEventParameterX().getValue(), unit,
                  samples);
              AxisLabel yAxisLabel = AxisLabel.getUnit(plainSet.getEventParameterY().getValue(), unit,
                  samples);
              String yLabel = yAxisLabel.getLabel();
              Unit yUnit = yAxisLabel.getUnit();
              String xLabel = xAxisLabel.getLabel();
              Unit xUnit = xAxisLabel.getUnit();

              // Show populations with the same scatter marker
              HashMap<PopulationID, MarkerStyle> markerMap = new HashMap<>();
              Iterator<MarkerStyle> markerIterator = MarkerStyle.getScatterAwtIterator().iterator();
              selPop.forEach(p -> markerMap.put(p, markerIterator.next()));

              // progress counter
              int totalSteps = samples.size() * selChannels.size();
              int step = 0;

              if (!plainSet.getScatterIsotopes().getValue()) {
                for (Sample sample : samples) {

                  isotopeLoop:
                  for (Channel channel : selChannels) {
                    // update progress
                    progress.set((double) step / totalSteps);
                    step++;
                    if (isStopped.get()) {
                      break isotopeLoop;
                    }

                    // Decide the color
                    Colors color = AnalysisUtils.getColor(sample, channel, samples.size(),
                        selChannels.size());

                    for (PopulationID populationID : selPop) {

                      double[] xData = sample.getData(channel, populationID,
                          EventType.NP, plainSet.getEventParameterX().getValue(), unit);

                      double[] yData = sample.getData(channel, populationID,
                          EventType.NP, plainSet.getEventParameterY().getValue(), unit);

                      List<Integer> validIndices = new ArrayList<>();
                      if (plainSet.isComputeNonzero()) {
                        Pair<double[], double[]> nz = ArrUtils.strictlyGreaterThan(xData, yData,
                            validIndices, 0);
                        xData = nz.getKey();
                        yData = nz.getValue();
                      }

                      xData = plainSet.getMathModificationX().getValue().calc(xData);
                      yData = plainSet.getMathModificationY().getValue().calc(yData);

                      // create component
                      if (xData.length > 0 && yData.length > 0) {
                        chartComponents.add(new ChartComponent(
                            new ChartData(
                                AnalysisUtils.getLabelForPlots(
                                    sample,
                                    channel,
                                    populationID,
                                    EventType.NP),
                                xData, yData,
                                xLabel, xUnit, plainSet.getMathModificationX().getValue(),
                                yLabel, yUnit, plainSet.getMathModificationY().getValue()),
                            new ChartStyle(color, plainSet.getColorAlpha().getValue(),
                                LineWidthDefaults.MEDIUM_THICK,
                                LineDashDefaults.STRAIGHT, 0f,
                                new CustomMarkerSize(plainSet.getDotSize().getValue()),
                                markerMap.get(populationID),
                                // when varying symbol from map, make sure to override
                                true,
                                RendererOption.LINE_AND_SHAPE,
                                LineGraphStyle.MARKER)
                        ));

                        // Add regression line
                        if (plainSet.getAddRegression().getValue()) {
                          double regressionRatio = plainSet.getRegressionViewRatio().getValue();
                          regressionRatio = Math.max(0, regressionRatio);
                          regressionRatio = Math.min(1, regressionRatio);
                          RegressionUtils.LSResult npReg = getOLS(xData, yData, regressionRatio);

                          ChartComponent rawRegressionComponent = new ChartComponent(
                              new ChartData(
                                  "Raw s/i/R:" + SnF.doubleToString(npReg.slope, NF.D1C1) + " / "
                                      + SnF.doubleToString(npReg.intercept, NF.D1C1) + " / "
                                      + SnF.doubleToString(npReg.rSquare, NF.D1C3)
                                      + "("
                                      + channel.getShortUIString() + "_" + sample.getNickName() + ")",
                                  npReg.x,
                                  npReg.y,
                                  xLabel, xUnit, plainSet.getMathModificationX().getValue(),
                                  yLabel, yUnit, plainSet.getMathModificationY().getValue()),
                              new ChartStyle(color, 0.75,
                                  LineWidthDefaults.THINNER,
                                  LineDashDefaults.STRAIGHT, 0f,
                                  MarkerSizeDefaults.SMALL,
                                  MarkerStyle.CIRCLE,
                                  false,
                                  RendererOption.LINE_AND_SHAPE,
                                  LineGraphStyle.LINE_AND_MARKER)
                          );
                          chartComponents.add(rawRegressionComponent);
                        }
                      }
                    }
                  }
                }
              } else {

                boolean needsAxisLabel = true;
                for (Sample sample : samples) {

                  if (selChannels.size() > 1) {
                    Channel channelX = selChannels.get(0);
                    Channel channelY = selChannels.get(1);

                    // Decide the color
                    Colors color = new SpColor(sample.getColor());

                    for (int i = 0; i < selPop.size(); i++) {
                      PopulationID populationID = selPop.get(i);

                      if (i > 0) {
                        color = Colors.variationHSB(color, new SpColor(Colors.paletteColor(i)), i);
                      }

                      double[] xData = sample.getData(channelX, populationID,
                          EventType.NP, plainSet.getEventParameterX().getValue(), unit);

                      double[] yData = sample.getData(channelY, populationID,
                          EventType.NP, plainSet.getEventParameterY().getValue(), unit);

                      List<Integer> validIndices = new ArrayList<>();
                      if (plainSet.isComputeNonzero()) {
                        Pair<double[], double[]> nz = ArrUtils.strictlyGreaterThan(xData, yData,
                            validIndices, 0);
                        xData = nz.getKey();
                        yData = nz.getValue();
                      }

                      xData = plainSet.getMathModificationX().getValue().calc(xData);
                      yData = plainSet.getMathModificationY().getValue().calc(yData);

                      // Else we append as "Si Si Si Si Si Si Element mass /fg"
                      if (needsAxisLabel) {
                        xLabel = channelX.getShortUIString() + " " + xLabel;
                        yLabel = channelY.getShortUIString() + " " + yLabel;
                        needsAxisLabel = false;
                      }

                      // check if aligned
                      boolean isAlignedOrP = analysis.AnalysisUtils.isAlignedOrPVal(populationID);

                      if (isAlignedOrP && xData.length > 0 && yData.length > 0) {
                        chartComponents.add(new ChartComponent(
                            new ChartData(AnalysisUtils.getLabelForPlots(sample,
                                null,
                                populationID,
                                EventType.NP),
                                xData, yData,
                                xLabel, xUnit, plainSet.getMathModificationX().getValue(),
                                yLabel, yUnit, plainSet.getMathModificationY().getValue()),
                            new ChartStyle(color, plainSet.getColorAlpha().getValue(),
                                LineWidthDefaults.MEDIUM_THICK,
                                LineDashDefaults.STRAIGHT, 0f,
                                new CustomMarkerSize(plainSet.getDotSize().getValue()),
                                markerMap.get(populationID),
                                // when varying symbol from map, make sure to override
                                true,
                                RendererOption.LINE_AND_SHAPE,
                                LineGraphStyle.MARKER)
                        ));

                        if (plainSet.getAddRegression().getValue()) {
                          double regressionRatio = plainSet.getRegressionViewRatio().getValue();
                          regressionRatio = Math.max(0, regressionRatio);
                          regressionRatio = Math.min(1, regressionRatio);
                          RegressionUtils.LSResult npReg = getOLS(xData, yData, regressionRatio);

                          ChartComponent rawRegressionComponent = new ChartComponent(
                              new ChartData(
                                  "Raw s/i/R:" + SnF.doubleToString(npReg.slope, NF.D1C1) + " / "
                                      + SnF.doubleToString(npReg.intercept, NF.D1C1) + " / "
                                      + SnF.doubleToString(npReg.rSquare, NF.D1C3)
                                      + "(" + sample.getNickName() + ")",
                                  npReg.x,
                                  npReg.y,
                                  xLabel, xUnit, plainSet.getMathModificationX().getValue(),
                                  yLabel, yUnit, plainSet.getMathModificationY().getValue()),
                              new ChartStyle(color, 0.75,
                                  LineWidthDefaults.THINNER,
                                  LineDashDefaults.STRAIGHT, 0f,
                                  MarkerSizeDefaults.SMALL,
                                  MarkerStyle.CIRCLE,
                                  false,
                                  RendererOption.LINE_AND_SHAPE,
                                  LineGraphStyle.LINE_AND_MARKER)
                          );
                          chartComponents.add(rawRegressionComponent);
                        }

                      }
                    }
                  }
                }
              }

              Platform.runLater(() -> {
                try {
                  // Define the chart
                  JFreeChart chart = SpChartFactory.createLineChart(chartComponents);

                  // Check to adjust axis range
                  if (plainSet.getLimitAxes().getValue()) {
                    Range xRange = chart.getXYPlot().getDomainAxis().getRange();
                    Range yRange = chart.getXYPlot().getRangeAxis().getRange();

                    double xLimUp = plainSet.getUpperXLimit().getValue();
                    double yLimUp = plainSet.getUpperYLimit().getValue();
                    double xLimLow = plainSet.getLowerXLimit().getValue();
                    double yLimLow = plainSet.getLowerYLimit().getValue();


                    // Make sure that upper > lower!
                    if (xLimUp != 0 && xLimUp > xRange.getLowerBound()) {
                      chart.getXYPlot().getDomainAxis().setRange(new Range(xRange.getLowerBound(),
                          xLimUp));
                    }

                    if (yLimUp != 0 && yLimUp > yRange.getLowerBound()) {
                      chart.getXYPlot().getRangeAxis().setRange(new Range(yRange.getLowerBound(),
                          yLimUp));
                    }

                    // get new range handles
                    xRange = chart.getXYPlot().getDomainAxis().getRange();
                    yRange = chart.getXYPlot().getRangeAxis().getRange();

                    if (xLimLow != 0 && xLimLow < xRange.getUpperBound()) {
                      chart.getXYPlot().getDomainAxis().setRange(new Range(xLimLow,
                          xRange.getUpperBound()));
                    }

                    if (yLimLow != 0 && yLimLow < yRange.getUpperBound()) {
                      chart.getXYPlot().getRangeAxis().setRange(new Range(yLimLow,
                          yRange.getUpperBound()));
                    }
                  }

                  // Create node
                  ChartContainer container = SpChartFactory.bundleChartLegend(
                      viewerRef.get(),
                      chart,
                      chartComponents,
                      800, 500,
                      false,
                      Orientation.VERTICAL,
                      false);

                  viewerRef.set(container.viewer);

                  // TODO :)
                  ChartViewer viewer = container.viewer;
                  // PolygonOverlay.enablePolygon(viewer);

                  // Replace atomically — do NOT setCenter(null) first.
                  // Detaching while Prism render is in flight causes RTTexture NPE.
                  targetPane.setCenter(container.combinedPane);
                } catch (Exception ex) {
                  LOGGER.error("Error while plotting! "
                      + " Message: " + ExceptionUtils.getMessage(ex)
                      + " Details: " + ExceptionUtils.getStackTrace(ex));
                }
              });
            } else {
              // just some empty dummy
              Platform.runLater(() -> {
                try {
                  targetPane.setCenter(new AnchorPane());
                } catch (Exception e) {
                  LOGGER.error("Error while plotting! "
                      + " Message: " + ExceptionUtils.getMessage(e)
                      + " Details: " + ExceptionUtils.getStackTrace(e));
                }
              });
            }
            progress.set(1d);
            return new EmptyTaskResult();
          } catch (Exception e) {
            LOGGER.error("Error while plotting! "
                + " Message: " + ExceptionUtils.getMessage(e)
                + " Details: " + ExceptionUtils.getStackTrace(e));
            return new EmptyTaskResult();
          }
        }
      };

      SpTool3Main.getRunTime().getTaskManager().forceToGraphPool(
          new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false,
              new EmptyTaskResult()));

    }
  }

  public static class BoxPlotViewer extends ViewerFXParamSet {

    public BoxPlotViewer(BoxPlotParameters plainSet) {
      super(plainSet);
    }

    @Override
    public double getValueWidth() {
      return 135d;
    }

    @Override
    public void refreshGraph() {
      BoxPlotParameters plainSet = (BoxPlotParameters) super.getPlainSet();

      if (plainSet != null) {

        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
        List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

        List<ChartComponent> components = new ArrayList<>();

        if (selSamples != null && !selSamples.isEmpty()
            && selChannels != null && !selChannels.isEmpty()
            && selPops != null && !selPops.isEmpty()) {

          // Define labels
          Unit unit = SpTool3Main.getRunTime().getMainWindowCtl().getUnit();
          AxisLabel yAxisLabel = AxisLabel.getUnit(plainSet.getEventParameter(), unit, selSamples);
          AxisLabel xAxisLabel = new PlainLabel("Index", ViewUnits.NONE);
          String yLabel = yAxisLabel.getLabel();
          Unit yUnit = yAxisLabel.getUnit();
          String xLabel = xAxisLabel.getLabel();
          Unit xUnit = xAxisLabel.getUnit();

          // Retrieve data
          EventType evtType = plainSet.getEventType().getValue();

          for (Sample sample : selSamples) {
            for (Channel ch : selChannels) {

              Colors color = AnalysisUtils.getColor(sample, ch,
                  selSamples.size(), selChannels.size());

              Colors originalColor = color;

              int variationCounter = -1;
              for (PopulationID popID : selPops) {

                // Do not waste color variations on pops that have no data...
                if (sample.hasPopulation(popID, ch)) {
                  variationCounter++;
                  // variation with index=0 returns original!
                  color = Colors.variationHSB(originalColor, color, variationCounter);
                }

                // check data source
                List<double[]> combinedData = new ArrayList<>();

                // Check if we have to extract both? Then add the BG first.
                if (evtType.equals(EventType.BG_NP)) {

                  // Set event type to NP in order to plot NP outside if this if statement!
                  evtType = EventType.NP;

                  double[] data = sample.getData(ch, popID, EventType.BG,
                      plainSet.getEventParameter(), unit);

                  // reduce BG (?)
                  if (plainSet.getJitterBackground().getValue()) {
                    data = Statistics.quantileSampleWithMurmurHashedJitter(data,
                        plainSet.getNumberOfBackgroundEvents().getValue(),
                        2);
                  }

                  // check math operations (after reducing size)
                  data = plainSet.getMathModification().getValue().calc(data);
                  combinedData.add(data);
                }

                // Default way to retrieve data; in case of NG&NP, this adds the NP data
                double[] data = sample.getData(ch, popID, evtType,
                    plainSet.getEventParameter(), unit);
                // check math operations
                data = plainSet.getMathModification().getValue().calc(data);
                combinedData.add(data);

                // Merge the data
                data = ArrUtils.merge(combinedData);

                // Empty string will not be written.
                String shortNameIDs;
                if (plainSet.getShowIDsOnPlot().getValue()) {
                  shortNameIDs = AnalysisUtils.getShortCodeNameForPlots(sample, selSamples, ch,
                      popID, selPops);
                } else {
                  shortNameIDs = "";
                }

                ChartComponent component = new ChartComponent(
                    new HistogramChartData(
                        AnalysisUtils.getLabelForPlots(sample, ch, popID, evtType),
                        shortNameIDs,
                        data,
                        xLabel, xUnit, MathMod.NONE,
                        yLabel, yUnit, plainSet.getMathModification().getValue(), 0d),
                    new ChartStyle(color, 0.95,
                        LineWidthDefaults.THICKER,
                        LineDashDefaults.XL, 0f,
                        MarkerSizeDefaults.SMALL,
                        MarkerStyle.BAR,
                        false,
                        RendererOption.SAMPLING_LINE_AND_SHAPE,
                        LineGraphStyle.LINE_AND_MARKER)
                );
                if (data.length > 0) {
                  components.add(component);
                }
              }
            }
          }
        }

        JFreeChart chart = SpChartFactory.createBoxplot(components);

        // Create node
        Node viewNode = SpChartFactory.bundleChartLegend(
            chart,
            components,
            800,
            500,
            false,
            true).combinedPane;

        // Keep in the same pulse/runlater of the UI thread
        targetPane.setCenter(viewNode);
      } else {
        // Keep in the same pulse/runlater of the UI thread
        targetPane.setCenter(new AnchorPane());
      }

      // Show all isotopes in VBox (?)

    }
  }

  public static class SpectrumViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    private final AtomicReference<ChartViewer> viewerRef = new AtomicReference<>(null);

    private List<Isotope> excludedFromLabel;

    public SpectrumViewer(SpectrumViewerParameters plainSet) {
      super(plainSet);
    }

    @Override
    public double getValueWidth() {
      return 135d;
    }

    @Override
    public void refreshGraph() {
      SpectrumViewerParameters plainSet = (SpectrumViewerParameters) super.getPlainSet();

      progress.set(0d);
      isStopped.set(false);

      UiUtil.showLoading(targetPane);

      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      List<ChartComponent> components = new ArrayList<>();

      // Built by claude sonnet 4.6 based on the histogram template
      WorkingTask graphTask = new WorkingTask() {
        @Override
        public String getTaskName() {
          return "Show spectrum plot";
        }

        @Override
        public AtomicDouble getProgress() {
          return progress;
        }

        @Override
        public void stop() {
          isStopped.set(true);
        }

        @Override
        public TaskResult call() throws Exception {
          try {
            if (plainSet != null) {

              double stickWidth = plainSet.getStickWidth().getValue();

              if (selSamples != null && !selSamples.isEmpty()
                  && selPops != null && !selPops.isEmpty()) {

                // Define labels
                boolean normalizeY = plainSet.getNormalizeSignal().getValue();
                AxisLabel yAxisLabel;
                if (normalizeY) {
                  yAxisLabel = new PlainLabel("Rel. intensity", ViewUnits.NONE);
                } else {
                  yAxisLabel = new PlainLabel("Intensity", ViewUnits.REGION_COUNTS);
                }
                AxisLabel xAxisLabel = new PlainLabel("m/z", ViewUnits.AMU);
                String yLabel = yAxisLabel.getLabel();
                Unit yUnit = yAxisLabel.getUnit();
                String xLabel = xAxisLabel.getLabel();
                Unit xUnit = xAxisLabel.getUnit();
                double alpha = plainSet.getColorAlpha().getValue();
                NormalizationType type = plainSet.getNormalizeSignalType().getValue();
                if (plainSet.getExcludeIsotopes().getValue()) {
                  excludedFromLabel = plainSet.listExcludedIsotopes();
                } else {
                  excludedFromLabel = new ArrayList<>();
                }

                int totalSteps = selSamples.size() * selPops.size();
                int step = 0;

                // Retrieve data
                sampleLoop:
                for (Sample sample : selSamples) {

                  int variationCounter = -1;
                  for (PopulationID popID : selPops) {

                    // update progress
                    progress.set((double) step / totalSteps);
                    step++;

                    if (isStopped.get()) {
                      break sampleLoop;
                    }

                    double[] specMZ = sample.getSpectralMZ(popID);
                    double[] specInt = sample.getSpectralValues(popID);

                    if (specMZ.length < 1 || specInt.length < 1) {
                      // try to compute the spectra
                      LOGGER.trace("No spectral data. Loading data from available sources.");
                      SpectralUtil.computeSpectra(sample, popID);
                      // try load again
                      specMZ = sample.getSpectralMZ(popID);
                      specInt = sample.getSpectralValues(popID);
                    }

                    if (specMZ.length > 0 && specMZ.length == specInt.length) {
                      // Avoid incrementing color for populations that do not exist
                      variationCounter++; // for the color
                      Colors col = new SpColor(sample.getColor());
                      col = Colors.variationHSB(col, col, variationCounter);
                      // col = Colors.variationHSB(col, new SpColor(Colors.paletteColor(counter)), counter);

                      if (normalizeY) {
                        if (type == NormalizationType.SUM) {
                          double yMax = ArrUtils.doubleSum(specInt);
                          specInt = ArrUtils.normalize(specInt, yMax);
                        } else {
                          double yMax = ArrUtils.getMax(specInt);
                          specInt = ArrUtils.normalize(specInt, yMax);
                        }
                      }

                      if (specMZ.length > 0 && specMZ.length == specInt.length) {
                        ChartComponent component = new ChartComponent(
                            new ChartData(
                                AnalysisUtils.getLabelForPlots(sample, null, popID, EventType.NP),
                                AnalysisUtils.getShortCodeNameForPlots(sample, selSamples, null,
                                    popID, selPops),
                                specMZ, specInt,
                                xLabel, xUnit, MathMod.NONE,
                                yLabel, yUnit, MathMod.NONE),
                            new ChartStyle(col, alpha,
                                LineWidthDefaults.THICKER,
                                LineDashDefaults.XL, 0f,
                                MarkerSizeDefaults.SMALL,
                                MarkerStyle.BAR,
                                false,
                                RendererOption.SAMPLING_LINE_AND_SHAPE,
                                LineGraphStyle.LINE_AND_MARKER,
                                plainSet.getyLog().getValue())
                        );
                        components.add(component);
                      }
                    }
                  }
                }
              }

              Platform.runLater(() -> {
                try {
                  JFreeChart chart = SpChartFactory.createMassSpectrumChart(components,
                      plainSet.getShowLabels().getValue(),
                      plainSet.getFilterAbundance().getValue(),
                      plainSet.getMinAbundancePct().getValue(),
                      excludedFromLabel,
                      plainSet.getFilterSignal().getValue(),
                      plainSet.getMinSignalNormalized().getValue(),
                      stickWidth);

                  // Check to adjust axis range
                  if (chart != null) {

                    // find min in data for log axis
                    double minOfData = Double.MAX_VALUE;
                    for (int i = 0; i < components.size(); i++) {
                      ChartComponent component = components.get(i);
                      minOfData =
                          Math.min(ArrUtils.getMin(ArrUtils.strictlyGreaterThan(component.getData().getY(),
                                  0)),
                              minOfData);
                    }

                    if (plainSet.getLimitAxes().getValue()) {
                      double xLimUp = plainSet.getUpperXLimit().getValue();
                      double xLimLow = plainSet.getLowerXLimit().getValue();

                      double yLimUp = plainSet.getUpperYLimit().getValue();
                      double yLimLow = plainSet.getLowerYLimit().getValue();

                      Range currentXRng = chart.getXYPlot().getDomainAxis().getRange();
                      Range currentYRng = chart.getXYPlot().getRangeAxis().getRange();

                      /// X
                      // xMax zero indicates neutral
                      if (xLimUp == 0) {
                        xLimUp = currentXRng.getUpperBound();
                      }

                      // if current min < 0 --> zero is just the neutral indicator
                      if (xLimLow == 0) {
                        xLimLow = currentXRng.getLowerBound();
                      }

                      // are they swapped? Do nothing
                      if (!(xLimLow < xLimUp)) {
                        xLimLow = currentXRng.getLowerBound();
                        xLimUp = currentXRng.getUpperBound();
                      }

                      // safeguard: still bad?
                      if (!(xLimLow < xLimUp)) {
                        xLimUp = Math.nextUp(xLimLow);
                      }

                      /// Y
                      if (!plainSet.getyLog().getValue()) {
                        // xMax zero indicates neutral
                        if (yLimUp == 0) {
                          yLimUp = currentYRng.getUpperBound();
                        }

                        // if current min < 0 --> zero is just the neutral indicator
                        if (yLimLow == 0) {
                          yLimLow = currentYRng.getLowerBound();
                        }

                        // are they swapped? Do nothing
                        if (!(yLimLow < yLimUp)) {
                          yLimLow = currentYRng.getLowerBound();
                          yLimUp = currentYRng.getUpperBound();
                        }

                        // safeguard: still bad?
                        if (!(yLimLow < yLimUp)) {
                          yLimUp = Math.nextUp(yLimLow);
                        }

                        // Last check: logscale!
                      } else {

                        // Neutral state: initialize log with min of data b/c ini bound of JFree is too
                        // high
                        if (yLimLow == 0) {
                          yLimLow = Math.max(Math.nextUp(0d), minOfData);
                        } else {
                          // use input as limit and not the data min
                          yLimLow = Math.max(Math.nextUp(0d), yLimLow);
                        }

                        // is upper limit neutral (=0) or bad (<0)? Keep existing bound
                        if (yLimUp <= 0) {
                          yLimUp = currentYRng.getUpperBound();
                        }

                        // safeguard: with log scale, upper must be at least 2x nextUp for 0 < lower <
                        // upper
                        yLimUp = Math.max(Math.nextUp(Math.nextUp(0d)), yLimUp);

                        // safeguard: still bad?
                        if (!(yLimLow < yLimUp)) {
                          yLimUp = Math.nextUp(yLimLow);
                        }

                        // fix autoscale
                        if (chart.getXYPlot().getRangeAxis() instanceof LogAxis) {
                          chart.getXYPlot().getRangeAxis().setDefaultAutoRange(new Range(yLimLow,
                              yLimUp));
                        }
                      }
                      chart.getXYPlot().getDomainAxis().setRange(new Range(xLimLow, xLimUp));
                      chart.getXYPlot().getRangeAxis().setRange(new Range(yLimLow, yLimUp));
                    } else {
                      // override bad ini value of JFree
                      if (plainSet.getyLog().getValue()) {
                        Range currentYRng = chart.getXYPlot().getRangeAxis().getRange();
                        double yLimLow = Math.max(Math.nextUp(0d), minOfData);
                        double yLimUp = currentYRng.getUpperBound();
                        // safeguard
                        if (!(yLimLow < yLimUp)) {
                          yLimUp = Math.nextUp(yLimLow);
                        }
                        chart.getXYPlot().getRangeAxis().setRange(new Range(yLimLow, yLimUp));
                      }
                    }

                    ChartContainer container = SpChartFactory.bundleChartLegend(
                        viewerRef.get(),
                        chart,
                        components,
                        800, 500,
                        false,
                        Orientation.VERTICAL,
                        false);


                    viewerRef.set(container.viewer);
                    targetPane.setCenter(container.combinedPane);

                  } else {
                    // just some empty dummy
                    Platform.runLater(() -> {
                      try {
                        targetPane.setCenter(new AnchorPane());
                      } catch (Exception e) {
                        LOGGER.error("Error while plotting! "
                            + " Message: " + ExceptionUtils.getMessage(e)
                            + " Details: " + ExceptionUtils.getStackTrace(e));
                      }
                    });
                  }
                } catch (Exception e) {
                  LOGGER.error("Error while plotting! "
                      + " Message: " + ExceptionUtils.getMessage(e)
                      + " Details: " + ExceptionUtils.getStackTrace(e));
                }
              });

            } else {
              Platform.runLater(() -> targetPane.setCenter(new AnchorPane()));
            }

            progress.set(1d);
            return new EmptyTaskResult();

          } catch (Exception e) {
            LOGGER.error("Error while plotting! "
                + " Message: " + ExceptionUtils.getMessage(e)
                + " Details: " + ExceptionUtils.getStackTrace(e));
            return new EmptyTaskResult();
          }
        }
      };

      SpTool3Main.getRunTime().

          getTaskManager().

          forceToGraphPool(
              new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false, new

                  EmptyTaskResult()));
    }
  }

  public static class HACViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    private final AtomicReference<VBox> plotBoxRef = new AtomicReference<>(new VBox(10));

    public HACViewer(HACViewerParameters plainSet) {
      super(plainSet);
    }

    @Override
    public double getValueWidth() {
      return 135d;
    }

    @Override
    public void refreshGraph() {
      if (super.getPlainSet() instanceof HACViewerParameters) {
        HACViewerParameters plainSet = (HACViewerParameters) super.getPlainSet();

        progress.set(0d);
        isStopped.set(false);

        UiUtil.showLoading(targetPane);

        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
        List<Channel> loadedChannels = SpTool3Main.getRunTime().getMainWindowCtl().getAllChannels();
        List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

        double minFractionPct = plainSet.getMinFractionPct().getValue();
        int minClusterSizePie = plainSet.getMinClusterSizePie().getValue();
        IsotopeSelection isotopeSelection = plainSet.getIsotopeSelection().getValue();
        List<Isotope> excludedList = plainSet.listExcludedIsotopes();
        List<Isotope> includedList = plainSet.listIncludedIsotopes();
        boolean removeIsobaricConflicts = plainSet.getFlattenIsobarsToDefaults().getValue();

        double intensityThreshold = plainSet.getIntensityThreshold().getValue();
        boolean useLog2 = plainSet.getUseLog2().getValue();
        ZScoreTarget zScoreTarget = plainSet.getzScoreType().getValue();
        DistanceOptions distanceOption = plainSet.getOverrideThreshold().getValue();
        double distanceThreshold = plainSet.getDistanceThreshold().getValue();
        boolean useLogScaleDendrogram = plainSet.getUseLogScaleDendrogram().getValue();
        boolean showClusterNumbers = plainSet.getShowClusterNumbers().getValue();
        boolean applyCosine = plainSet.getApplyCosineFlattening().getValue();
        double cosineMergeThreshold = plainSet.getCosineScore().getValue();
        boolean useLog2ForCosine = plainSet.getUseLog2ForCosine().getValue();

        // Copied from spectrum viewer, which was built by claude sonnet 4.6 based on the histogram
        // template
        WorkingTask graphTask = new WorkingTask() {
          @Override
          public String getTaskName() {
            return "Show HAC plot";
          }

          @Override
          public AtomicDouble getProgress() {
            return progress;
          }

          @Override
          public void stop() {
            isStopped.set(true);
          }

          @Override
          public TaskResult call() {
            try {
              if (selSamples != null && !selSamples.isEmpty()
                  && selPops != null && !selPops.isEmpty()) {

                // Only apply to first selected so far to keep it easy: Later consider scroll/tab pane?
                Sample sample = selSamples.get(0);
                PopulationID popID = selPops.get(0);

                // Convert to spectral region
                List<SpectralArray> spectralRegionsData = new ArrayList<>(sample.getSpectralData(popID));
                // when creating populations, we shall add complete unfiltered regions data
                final List<SpectralArray> spectralRegionsDataCopy = new ArrayList<>(spectralRegionsData);

                if (spectralRegionsData.isEmpty()) {
                  // try to compute the spectra from drive b/c the cache inside the samlpe was empty
                  LOGGER.trace("No spectral data. Loading data from available sources.");
                  SpectralUtil.computeSpectra(sample, popID);
                  // try load again
                  spectralRegionsData = new ArrayList<>(sample.getSpectralData(popID));
                }

                // check for blacklisted elements
                // enable exclusion of certain isotopes
                switch (isotopeSelection) {
                  case ALL_SAMPLE -> {
                  }
                  case ALL_LOADED -> {
                    spectralRegionsData.removeIf(spectArr -> !loadedChannels.contains(spectArr.getChannel()));
                  }
                  case SELECTED -> {
                    spectralRegionsData.removeIf(spectArr -> !selChannels.contains(spectArr.getChannel()));
                  }
                  case POSITIVE_LIST_SELECTION -> {
                    spectralRegionsData.removeIf(spectArr -> !includedList.contains(spectArr.getChannel().getIsotope()));
                  }
                  case NEGATIVE_LIST_EXCLUSION -> {
                    spectralRegionsData.removeIf(spectArr -> excludedList.contains(spectArr.getChannel().getIsotope()));
                  }
                  default -> {
                    // keep as is, we should not reach this branch
                  }
                }

                /*
                 * Filter spectra: Ignore spectra where no m/z exceeds the threshold.
                 * Filter here at top level (and not inside preprocess) to keep labels for slices correct
                 */
                spectralRegionsData.removeIf(spectArr -> {
                  int aboveThrCounter = 0;
                  for (double v : spectArr.getIntensity()) {
                    if (v > intensityThreshold) {
                      aboveThrCounter = aboveThrCounter + 1;
                    }
                    if (aboveThrCounter > 1) {
                      return false; // keep
                    }
                  }
                  return true; // drop
                });

                List<Channel> validChannels = spectralRegionsData.stream()
                    .map(SpectralArray::getChannel)
                    .toList();

                // transpose to regions and elements:
                SpectralRegionElement sre = new SpectralRegionElement(spectralRegionsData);

                // are there data?
                if (!sre.getIntensities().isEmpty()) {
                  List<double[]> data = sre.getIntensities();

                  // Fit (expensive): reuse cached FittedHAC if preprocessing inputs are unchanged
                  HacInstructionWrapper wrapper = sample.getHacWrapper(popID);
                  HAC.FittedHAC fitted;

                  // check if valid wrapper
                  if (wrapper != null && wrapper.isEqualInstructions(
                      useLog2,
                      zScoreTarget,
                      validChannels)) {
                    fitted = wrapper.getFit();
                  } else {

                    // 2. Preprocess [my own implementation!] -> here we apply the intensity threshold
                    List<double[]> processed = HAC.preprocess(
                        data,
                        useLog2,
                        zScoreTarget
                    );

                    // Here we execute the HAC fit!
                    fitted = HAC.fit(processed);

                    sample.putHacWrapper(popID, new HacInstructionWrapper(
                        fitted,
                        useLog2,
                        zScoreTarget,
                        validChannels));
                  }

                  // **Claude Sonnet implementation of SMILES HAC**
                  // Cut (cheap): derive threshold from the uncut tree, then cut once at the chosen
                  // threshold
                  HAC.ClusterResult crForThreshold = fitted.cut(0.0);

                  double threshold = distanceThreshold;
                  if (distanceOption.equals(DistanceOptions.HALF)) {
                    threshold = crForThreshold.getHalfThreshold();
                  } else if (distanceOption.equals(DistanceOptions.STEP_DIFFERENCE)) {
                    threshold = crForThreshold.getSuggestedStepThreshold();
                  } else if (distanceOption.equals(DistanceOptions.CURVATURE)) {
                    threshold = crForThreshold.getSuggestedCurvatureThreshold();
                  }

                  /* crWard: pure Ward cut, kept unchanged for dendrogram + knee plot.
                   cr      : working result; may be reassigned by cosine post-processing below.
                            Only labels, k, sizes and names are replaced; mergeTree and mergeHeights stay.
                    */
                  HAC.ClusterResult crWard = fitted.cut(threshold);
                  HAC.ClusterResult cr = crWard;

                  // ── Cosine similarity post-processing ────────────────────────────────────
                  // Merge clusters whose mean raw spectra are compositionally indistinguishable.
                  // Only applied when the user has set a threshold below 1.0 (i.e., opted in).
                  if (applyCosine) {
                    List<double[]> dataForCosine;
                    if (useLog2ForCosine) {
                      dataForCosine = HAC.preprocess(
                          data,
                          useLog2,
                          ZScoreTarget.NONE   // never z-score before cosine
                      );
                    } else {
                      dataForCosine = data;
                    }

                    cr = cr.mergeByCosineSimilarity(dataForCosine, cosineMergeThreshold);
                  }

                  // ___________________________________________________________________________________

                  HBox dendroAndKneeBox = new HBox(10);
                  plotBoxRef.get().getChildren().add(UiUtil.putOnAnchorWithoutInsets(dendroAndKneeBox));

                  // #################################################################################
                  // ######################## DENDROGRAM ##########################################
                  // #################################################################################
                  ChartComponent dendroComponent = new ChartComponent(
                      new SpChartFactory.DendrogramChartData(crWard, popID.toString(), popID.toString()),
                      new SpChartFactory.DendrogramChartStyle(LineWidthDefaults.MEDIUM,
                          useLogScaleDendrogram, showClusterNumbers));

                  JFreeChart dendroChart = SpChartFactory.createDendrogram(dendroComponent);

                  List<ChartContainer> dendroChartContainers = new ArrayList<>();
                  dendroChartContainers.add(SpChartFactory.bundleChartLegend(
                      dendroChart,
                      List.of(dendroComponent),
                      800, 500));

                  for (ChartContainer cont : dendroChartContainers) {
                    dendroAndKneeBox.getChildren().add(UiUtil.putOnAnchorWithoutInsets(cont.viewer));
                  }

                  ValueMarker markerHorz;
                  if (useLogScaleDendrogram) {
                    markerHorz = new ValueMarker(Math.log1p(crWard.threshold()));
                  } else {
                    markerHorz = new ValueMarker(crWard.threshold());
                  }
                  markerHorz.setLabel("THR = " + SnF.doubleToString(crWard.threshold(), NF.D1C2));
                  markerHorz.setPaint(OkabeItoColors.BLACK.get());
                  markerHorz.setStroke(SpChartFactory.getStroke(
                      LineWidthDefaults.MEDIUM,
                      LineDashDefaults.S));
                  dendroChart.getXYPlot().addRangeMarker(markerHorz);

                  markerHorz.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_RIGHT);
                  markerHorz.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_RIGHT);
                  markerHorz.setLabelFont(new Font("Tahoma", Font.BOLD,
                      SpTool3Main.getRunTime().getConfParams().getAxisFontSize() - 3));
                  markerHorz.setLabelPaint(OkabeItoColors.BLACK_DARK.get());

                  // #################################################################################
                  // ######################## KNEE PLOT ##########################################
                  // #################################################################################
                  int maxSteps = Math.max(crWard.k() + 10, 50);
                  Pair<double[], double[]> kneeData = HACCharts.getKneePlotXY(crWard, maxSteps);

                  ChartComponent comp = new ChartComponent(
                      new ChartData(popID.toString(),
                          kneeData.getKey(),
                          kneeData.getValue(),
                          "Merge step (from top)", ViewUnits.NONE, MathMod.NONE,
                          "Ward distance", ViewUnits.NONE, MathMod.NONE),
                      new ChartStyle(OkabeItoColors.BLACK, 1,
                          LineWidthDefaults.MEDIUM_THICK,
                          LineDashDefaults.STRAIGHT, 0f,
                          MarkerSizeDefaults.LARGE,
                          MarkerStyle.DIAMOND,
                          false,
                          RendererOption.LINE_AND_SHAPE,
                          LineGraphStyle.LINE_AND_MARKER)
                  );

                  JFreeChart kneeChart = SpChartFactory.createLineChart(List.of(comp));

                  // add the marker
                  ValueMarker marker = new ValueMarker(crWard.k());
                  marker.setLabel("k = " + crWard.k());
                  marker.setPaint(Colors.LABEL_DARK);
                  marker.setStroke(SpChartFactory.getStroke(
                      LineWidthDefaults.MEDIUM,
                      LineDashDefaults.S));
                  kneeChart.getXYPlot().addDomainMarker(marker);

                  marker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_RIGHT);
                  marker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_LEFT);
                  marker.setLabelFont(new Font("Tahoma", Font.BOLD,
                      SpTool3Main.getRunTime().getConfParams().getAxisFontSize() - 3));
                  marker.setLabelPaint(OkabeItoColors.BLACK_DARK.get());

                  // make the chart.
                  ChartContainer chartContainer = SpChartFactory.bundleChartLegend(
                      kneeChart,
                      List.of(comp),
                      800, 500);

                  // Here, no legend is needed for the knee plot
                  dendroAndKneeBox.getChildren().add(UiUtil.putOnAnchorWithoutInsets(chartContainer.viewer));
                  // Divide at 1/3 between dendrogram and knee plot
                  chartContainer.viewer.minWidthProperty().bind(dendroAndKneeBox.widthProperty().divide(3));
                  chartContainer.viewer.maxWidthProperty().bind(dendroAndKneeBox.widthProperty().divide(3));


                  // #################################################################################
                  // ######################## PIE CHARTS ##########################################
                  // #################################################################################
                  String[] elementNames = sre.getNames().toArray(new String[0]);
                  List<SpChartFactory.PieChartData> pieChartData = HACCharts.getPieChartData(
                      cr,
                      elementNames,
                      data,
                      minClusterSizePie,
                      minFractionPct);

                  // get colors for element by most abundant isotope
                  Map<String, Colors> elementColors = HACCharts.getPieChartColors(sre.getCategories());

                  List<ChartComponent> pieChartComponents = new ArrayList<>();
                  for (SpChartFactory.PieChartData pieChartDat : pieChartData) {
                    pieChartComponents.add(new ChartComponent(
                        pieChartDat,
                        new SpChartFactory.PieChartStyle(elementColors)));
                  }

                  List<JFreeChart> pieCharts = SpChartFactory.createPieCharts(pieChartComponents);

                  List<ChartContainer> pieChartContainers = new ArrayList<>();
                  if (pieChartComponents.size() == pieCharts.size()) {
                    for (int i = 0; i < pieCharts.size(); i++) {
                      pieChartContainers.add(SpChartFactory.bundleChartLegend(
                          pieCharts.get(i),
                          List.of(pieChartComponents.get(i)),
                          800, 500));
                    }
                  }

                  VBox pieGroupBox = new VBox(10);
                  ScrollPane pieScroll = new ScrollPane(pieGroupBox);
                  UiUtil.formatScrollPane(pieScroll);
                  UiUtil.makePaneBrightAndRound(pieScroll);
                  UiUtil.installFastScroll(pieScroll);
                  // pieScroll.setFitToWidth(false);
                  // pieScroll.setFitToHeight(false); // would prevent pies from shrinking
                  plotBoxRef.get().getChildren().add(UiUtil.putOnAnchorWithoutInsets(pieScroll));
                  // Keep scroll pane at 50%
                  pieScroll.minHeightProperty().bind(plotBoxRef.get().heightProperty().divide(1.5));
                  pieScroll.maxHeightProperty().bind(plotBoxRef.get().heightProperty().divide(1.5));

                  HBox pieBox = new HBox(10);
                  for (int i = 0; i < pieChartContainers.size(); i++) {
                    if (i % 4 == 0) {
                      pieBox = new HBox(10);
                      pieGroupBox.getChildren().add(pieBox);
                    }
                    pieBox.getChildren().add(UiUtil.putOnAnchorWithoutInsets(pieChartContainers.get(i).viewer));
                    pieChartContainers.get(i).viewer.setMinHeight(150); // or else, pies shrink to nothing
                  }

                  // #################################################################################
                  // ######################## THE BUTTONS ##########################################
                  // #################################################################################
                  Button savePopulationsBtn = UiUtil.getImageButton("Store", "/img/selectAll.png",
                      "Store clusters as new populations");

                  // Capture for use in button action lambdas (must be effectively final for lambda
                  // capture)
                  final List<SpectralArray> capturedSpectra = new ArrayList<>(spectralRegionsDataCopy);

                  HAC.ClusterResult finalCr = cr;
                  savePopulationsBtn.setOnAction(e -> {

                    /*
                     TODO:
                      1) Put this into a utility class
                      2) in general: while at it, clean up this chaos in viewer to compute outside
                     */

                    List<List<Integer>> allRegionIndices = finalCr.indicesByCluster();

                    List<Integer> hacIndices = new ArrayList<>();
                    List<List<Integer>> regionIndices = new ArrayList<>();

                    for (int c = 0; c < allRegionIndices.size(); c++) {
                      List<Integer> list = allRegionIndices.get(c);
                      // only use clusters that are larger than min size, i.e., that are shown in UI
                      if (list.size() >= minClusterSizePie) {
                        hacIndices.add(c);
                        regionIndices.add(list);
                      }
                    }


                    // check which isotopes to show as population
                    List<Channel> channels = new ArrayList<>(sample.listChannels());
                    // only remove if it has an isotope assigned AND that isotope is blacklisted
                    channels.removeIf(ch -> ch.getIsotope() != null
                        && !validChannels.contains(ch));

                    // get the Event list for each isotope
                    for (Channel channel : channels) {
                      Trace trace = sample.getTrace(channel);
                      if (trace != null) {
                        Population pop = trace.getPopulation(popID);
                        if (pop != null) {
                          EventCollection collection = pop.getEvents();
                          List<Event> events = collection.getNpEvents();


                          // create cluster population for each cluster
                          for (int i = 0; i < regionIndices.size(); i++) {
                            int hacClusterLabel = hacIndices.get(i);

                            MainEventCollection coll = new SubEventCollection(trace, collection);
                            List<Integer> cluster = regionIndices.get(i);
                            List<Event> eventsInCluster = new ArrayList<>();
                            for (Integer idx : cluster) {
                              if (idx < events.size()) {
                                eventsInCluster.add(events.get(idx));
                              }
                            }
                            coll.add(eventsInCluster);

                            // create new population
                            PopulationID idCopy = new PopulationID(popID);
                            idCopy.append(new PopulationStep.ClusterSubtype(sample, hacClusterLabel,
                                finalCr.clusterNames()[hacClusterLabel]));
                            trace.addOverridePopulation(idCopy,
                                new NpPopulation(
                                    idCopy,
                                    pop,
                                    coll,
                                    idCopy.toString(),
                                    new ArrayList<>(channels)
                                ),
                                true);
                          }
                        }
                      }
                    }

                    // -----------------------------------------------------------------------
                    // Mirror the clustering into SpectralArray data for each new sub-population
                    // -----------------------------------------------------------------------
                    // spectralRegionsData is already available in the enclosing WorkingTask scope:
                    // it holds one SpectralArray per isotope, each with intensity[i] = particle i.
                    for (int i = 0; i < regionIndices.size(); i++) {
                      int hacClusterLabel = hacIndices.get(i);
                      List<Integer> cluster = regionIndices.get(i);

                      PopulationID idCopy = new PopulationID(popID);
                      idCopy.append(new PopulationStep.ClusterSubtype(sample, hacClusterLabel,
                          finalCr.clusterNames()[hacClusterLabel]));

                      List<SpectralArray> clusterSpectra = new ArrayList<>();
                      for (SpectralArray sarr : capturedSpectra) {
                        double[] allIntensities = sarr.getIntensity();
                        double[] clusterIntensities = new double[cluster.size()];
                        for (int j = 0; j < cluster.size(); j++) {
                          int particleIdx = cluster.get(j);
                          clusterIntensities[j] = particleIdx < allIntensities.length
                              ? allIntensities[particleIdx]
                              : 0.0;
                        }

                        // Slice additional features by the same cluster indices
                        HashMap<String, double[]> clusterFeatures = new HashMap<>();
                        for (String key : sarr.listAdditionalFeatures()) {
                          double[] allFeatureValues = sarr.getAdditionalFeature(key);
                          double[] clusterFeatureValues = new double[cluster.size()];
                          for (int j = 0; j < cluster.size(); j++) {
                            int particleIdx = cluster.get(j);
                            clusterFeatureValues[j] =
                                (allFeatureValues != null && particleIdx < allFeatureValues.length)
                                    ? allFeatureValues[particleIdx]
                                    : 0.0;
                          }
                          clusterFeatures.put(key, clusterFeatureValues);
                        }

                        clusterSpectra.add(new SpectralArray(
                            sarr.getChannel(),
                            clusterIntensities,
                            clusterFeatures));
                      }

                      sample.addSpectralData(idCopy, clusterSpectra);
                    }
                    // -----------------------------------------------------------------------


                    SpTool3Main.getRunTime().getMainWindowCtl().updatePopulations();
                  });

                  Button clearMergedBtn = UiUtil.getImageButton("Delete", "/img/delete.png",
                      "Delete all populations in the sample that are of type 'cluster'");

                  clearMergedBtn.setOnAction(e -> {
                    NotificationFactory.openYesCancel(
                        "Remove all populations of cluster-type? This is irreversible.", () -> {
                          List<Channel> channels = sample.listChannels();
                          List<PopulationID> popIDs = sample.listAllPopulations().stream()
                              .filter(populationID -> populationID.getSteps().stream()
                                  .anyMatch(step -> step instanceof PopulationStep.ClusterSubtype))
                              .toList();
                          popIDs.forEach(id -> sample.removePopulations(channels, id));
                          SpTool3Main.getRunTime().getMainWindowCtl().updatePopulations();
                        });
                  });

                  Button clustersToClipboardBtn = UiUtil.getSquareImageButton("Clipboard", "/img/copy.png",
                      "Copy composition results to clipboard");

                  clustersToClipboardBtn.setOnAction(ev -> {
                    ExportWriter writer = new ClipboardWriter();

                    // data is already element-major — no isotope collapse needed here.
                    // elementNames aligns 1:1 with data (both from SpectralRegionElement).
                    int nElements = elementNames.length;
                    int nParticles = data.get(0).length;

                    // meanRaw[cluster][element] — accumulated sum, divided by counts below
                    double[][] meanRaw = new double[finalCr.k()][nElements];
                    int[] counts = new int[finalCr.k()];

                    for (int p = 0; p < nParticles; p++) {
                      int clusterIndex = finalCr.labels()[p];
                      counts[clusterIndex] = counts[clusterIndex] + 1;
                    }

                    for (int e = 0; e < nElements; e++) {
                      double[] channel = data.get(e);
                      for (int p = 0; p < nParticles; p++) {
                        int clusterIndex = finalCr.labels()[p];
                        meanRaw[clusterIndex][e] = meanRaw[clusterIndex][e] + channel[p];
                      }
                    }

                    // Divide to get means, then clamp negatives.
                    // Small negatives are noise from background subtraction — clamp silently.
                    // Large negatives (> 5% of the cluster's max element mean) suggest
                    // background overshot badly — log a warning.
                    for (int c = 0; c < finalCr.k(); c++) {
                      if (counts[c] == 0) {
                        continue;
                      }

                      for (int e = 0; e < nElements; e++) {
                        meanRaw[c][e] = meanRaw[c][e] / counts[c];
                      }

                      double maxMean = 0.0;
                      for (int e = 0; e < nElements; e++) {
                        if (meanRaw[c][e] > maxMean) {
                          maxMean = meanRaw[c][e];
                        }
                      }

                      for (int e = 0; e < nElements; e++) {
                        if (meanRaw[c][e] < 0.0) {
                          boolean isLargeNegative = Math.abs(meanRaw[c][e]) > 0.25 * maxMean;
                          if (isLargeNegative) {
                            LOGGER.warn(
                                "Clipboard export — cluster {} '{}': element '{}' has a strongly "
                                    + "negative mean ({}) — background subtraction may have overshot. "
                                    + "Clamping to 0.",
                                c, finalCr.clusterNames()[c], elementNames[e], meanRaw[c][e]);
                          }
                          meanRaw[c][e] = 0.0;
                        }
                      }
                    }

                    // Build columns: first column is element names, then one column per cluster
                    List<List<String>> columns = new ArrayList<>();

                    List<String> elementCol = new ArrayList<>();
                    elementCol.add("Element");
                    for (int e = 0; e < nElements; e++) {
                      elementCol.add(elementNames[e]);
                    }
                    columns.add(elementCol);

                    for (int c = 0; c < finalCr.k(); c++) {
                      if (finalCr.sizes()[c] < minClusterSizePie) {
                        continue;
                      }

                      double total = 0.0;
                      for (int e = 0; e < nElements; e++) {
                        total = total + meanRaw[c][e];
                      }

                      List<String> col = new ArrayList<>();
                      col.add(finalCr.clusterNames()[c] + " (n="
                          + finalCr.sizes()[c]
                          + " µ=" + SnF.doubleToString(total, NF.D1C1) + ")");
                      for (int e = 0; e < nElements; e++) {
                        double pct = (total > 0.0) ? meanRaw[c][e] / total * 100.0 : 0.0;
                        col.add(SnF.doubleToString(pct, NF.D1C2));
                      }
                      columns.add(col);
                    }

                    TabBlockColl coll = new TabBlockColl(writer, false);
                    TabBlock block = new TabBlock();
                    for (List<String> column : columns) {
                      block.addCol(new TabCol(column));
                    }
                    coll.add(block);
                    coll.export();
                  });

                  plotBoxRef.get().getChildren().add(UiUtil.putOnAnchorWithoutInsets(new HBox(3,
                      clustersToClipboardBtn, savePopulationsBtn, clearMergedBtn)));
                }
              }

              Platform.runLater(() -> {
                try {
                  if (plotBoxRef.get() != null) {
                    targetPane.setCenter(plotBoxRef.get());
                    plotBoxRef.set(new VBox());

                  } else {
                    // just some empty dummy
                    Platform.runLater(() -> {
                      try {
                        targetPane.setCenter(new AnchorPane());
                      } catch (Exception e) {
                        Platform.runLater(() -> targetPane.setCenter(new AnchorPane()));
                        LOGGER.error("Error while plotting! "
                            + " Message: " + ExceptionUtils.getMessage(e)
                            + " Details: " + ExceptionUtils.getStackTrace(e));
                      }
                    });
                  }
                } catch (Exception e) {
                  Platform.runLater(() -> targetPane.setCenter(new AnchorPane()));
                  LOGGER.error("Error while plotting! "
                      + " Message: " + ExceptionUtils.getMessage(e)
                      + " Details: " + ExceptionUtils.getStackTrace(e));
                }
              });


              progress.set(1d);
              return new EmptyTaskResult();

            } catch (Exception e) {
              Platform.runLater(() -> targetPane.setCenter(new AnchorPane()));
              LOGGER.error("Error while plotting! "
                  + " Message: " + ExceptionUtils.getMessage(e)
                  + " Details: " + ExceptionUtils.getStackTrace(e));
              return new EmptyTaskResult();
            }
          }
        };

        SpTool3Main.getRunTime().getTaskManager().forceToGraphPool(
            new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false,
                new EmptyTaskResult()));
      } else {
        Platform.runLater(() -> targetPane.setCenter(new AnchorPane()));
      }
    }
  }

  public static class MonteCarloHistoViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    private final AtomicReference<ChartViewer> viewerRef = new AtomicReference<>(null);

    public MonteCarloHistoViewer(MonteCarloHistoParameters plainSet) {
      super(plainSet);
    }

    @Override
    public double getValueWidth() {
      return 135d;
    }

    @Override
    public void refreshGraph() {
      MonteCarloHistoParameters plainSet = (MonteCarloHistoParameters) super.getPlainSet();

      // The viewer exists in the background and we need to reset is stopped as well as progress each time
      progress.set(0d);
      isStopped.set(false);

      // update UI in FXThread
      UiUtil.showLoading(targetPane);

      // inside the anonymous working task we cannot access this directly: make pointer
      FxParamSetImpl fxParent = this;

      //
      // Extract some previous parameters
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      // Chart components and additional containers
      List<ChartComponent> histoComponents = new ArrayList<>();
      List<ChartComponent> kdeComponents = new ArrayList<>();
      List<ChartComponent> cdfComponents = new ArrayList<>();

      WorkingTask graphTask = new WorkingTask() {
        @Override
        public String getTaskName() {
          return "Show histogram plot";
        }

        @Override
        public AtomicDouble getProgress() {
          return progress;
        }

        @Override
        public void stop() {
          isStopped.set(true);
        }

        @Override
        public TaskResult call() throws Exception {
          try {
            if (plainSet != null) {

              List<Double> percentiles = new ArrayList<>();
              List<Pair<Double, Colors>> rulerRoots = new ArrayList<>();
              // Custom ruler: only show once!
              if (plainSet.isShowRuler()) {
                if (plainSet.getRulerPosition().equals(MeasureOfLocation.CUSTOM)) {
                  rulerRoots.add(new Pair<>(plainSet.getCustomRulerPosition(), OkabeItoColors.BLACK));
                }
              }

              Unit qUnit = SpTool3Main.getRunTime().getMainWindowCtl().getUnit();

              // Define labels
              AxisLabel xAxisLabel = AxisLabel.getUnit(plainSet.getEventParameter(), qUnit, samples);
              AxisLabel yAxisLabel = AxisLabel.getYUnit(plainSet.getHistoNormalization());
              String yLabel = yAxisLabel.getLabel();
              Unit yUnit = yAxisLabel.getUnit();
              String xLabel = xAxisLabel.getLabel();
              Unit xUnit = xAxisLabel.getUnit();

              // progress counter
              int totalSteps = samples.size() * selChannels.size();
              int step = 0;

              for (Sample sample : samples) {

                isotopeLoop:
                for (Channel channel : selChannels) {
                  // update progress
                  progress.set((double) step / totalSteps);
                  step++;
                  if (isStopped.get()) {
                    break isotopeLoop;
                  }

                  // Decide the color
                  Colors color = AnalysisUtils.getColor(sample, channel, samples.size(),
                      selChannels.size());
                  Colors mainColor = new SpColor(color.get()); // root for the variations for populations

                  int variationCounter = -1;
                  for (PopulationID populationID : selPops) {
                    // Avoid incrementing color for populations that do not exist
                    variationCounter++;
                    if (sample.hasPopulation(populationID, channel)) {
                      // update color: makes it brighter or darker for the next plot
                      color = Colors.variationHSB(mainColor, mainColor, variationCounter);
                      // calc LOD
                      boolean isNet = plainSet.getEventParameter().equals(EventParameter.NET_AREA)
                          || plainSet.getEventParameter().equals(EventParameter.NET_HEIGHT);
                      double maxThrLOD = sample.getMaxThr(channel, populationID, isNet, qUnit);
                      maxThrLOD = plainSet.getMathMod().calc(maxThrLOD);

                      EventType eventType = plainSet.getEventType();

                      // Check if we have to extract both? Then add the BG first.
                      if (eventType.equals(EventType.BG_NP)) {

                        // Set event type to NP in order to plot NP outside if this if statement!
                        eventType = EventType.NP;

                        double[] data = sample.getData(channel, populationID, EventType.BG,
                            plainSet.getEventParameter(), qUnit);

                        if (plainSet.isJitterBG()) {
                          data = Statistics.quantileSampleWithMurmurHashedJitter(data,
                              plainSet.getNumberOfBGEvents(),
                              2);
                        }

                        // check if compute nonzero before applying math operations
                        if (plainSet.isComputeNonzero()) {
                          data = ArrUtils.strictlyGreaterThan(data, 0);
                        }

                        // check math operations
                        data = plainSet.getMathMod().calc(data);

                        // create component
                        if (data.length > 1) {
                          histoComponents.add(new ChartComponent(
                              new HistogramChartData(AnalysisUtils.getLabelForPlots(sample,
                                  channel,
                                  populationID,
                                  EventType.BG),
                                  data,
                                  xLabel, xUnit, plainSet.getMathMod(),
                                  yLabel, yUnit, MathMod.NONE,
                                  maxThrLOD),
                              new HistoChartStyle(
                                  plainSet.bgHighlight().getValue().getCol(EventType.BG, color),
                                  Math.max(plainSet.getColorAlpha() - 0.075 * samples.indexOf(sample), 0.3),
                                  plainSet.bgHighlight().getValue().getPattern(EventType.BG_NP)
                              )));
                        }
                      }

                      // Default way to retrieve data.
                      double[] data = sample.getData(channel, populationID, eventType,
                          plainSet.getEventParameter(), qUnit);

                      // check if compute nonzero before applying math operations
                      if (plainSet.isComputeNonzero()) {
                        data = ArrUtils.strictlyGreaterThan(data, 0);
                      }

                      // check math operations
                      data = plainSet.getMathMod().calc(data);

                      // create component
                      if (data.length > 0) {
                        histoComponents.add(new ChartComponent(
                            new HistogramChartData(AnalysisUtils.getLabelForPlots(sample,
                                channel,
                                populationID,
                                EventType.NP),
                                data,
                                xLabel, xUnit, plainSet.getMathMod(),
                                yLabel, yUnit, MathMod.NONE,
                                maxThrLOD),
                            new HistoChartStyle(
                                plainSet.bgHighlight().getValue().getCol(EventType.NP, color),
                                Math.max(plainSet.getColorAlpha() - 0.075 * samples.indexOf(sample), 0.3),
                                plainSet.bgHighlight().getValue().getPattern(EventType.NP)
                            )));

                      }

                      // Calculate percentiles in case we need to set axis limits
                      if (plainSet.isLimitAxes() && data.length > 0) {
                        DescriptiveStatistics stats = new DescriptiveStatistics(data);
                        double pctile = plainSet.getUpperXLimitPercentile();
                        pctile = Math.max(0, pctile);
                        pctile = Math.min(100, pctile);
                        percentiles.add(stats.getPercentile(pctile));
                      }

                      // calculate ruler position (based on NP data...) with color
                      if (plainSet.isShowRuler()) {
                        if (!plainSet.getRulerPosition().equals(MeasureOfLocation.CUSTOM)) {
                          double pos = plainSet.getRulerPosition().calc(data);
                          rulerRoots.add(new Pair<>(pos, color));
                        }
                      }
                    }
                  }
                }
              }

              List<ChartComponent> legendComponents = new ArrayList<>();

              // If KDE plots are to be shown.
              if (plainSet.getShowKernelDensity().getValue()) {
                // Also make KDE plots (based on the histo data)
                for (ChartComponent chartComponent : histoComponents) {
                  double[] data = chartComponent.getData().getX();
                  if (data.length > 2) {
                    double bandwidth = BinWidthEstimator.silvermanRule(data)
                        * plainSet.getCustomKernelBandwidth();
                    KernelDensity kd = new KernelDensity(data, bandwidth);
                    double maxVal = ArrUtils.getMax(data);
                    // reduce step size if small range or log plot
                    double stepSize = 0.1;
                    if (maxVal < 100 || plainSet.getMathMod().equals(MathMod.LOG10)) {
                      stepSize = 0.001;
                    }
                    double[] x = ArrUtils.fillArrayExclusive(0, 1.5 * maxVal, stepSize);
                    double[] y = new double[x.length];
                    for (int i = 0; i < x.length; i++) {
                      y[i] = kd.p(x[i]);
                    }
                    ChartComponent kdeComponent = new ChartComponent(new ChartData(
                        chartComponent.getData().getSeriesName() + " (KDE)",
                        x, y,
                        chartComponent.getData().getxLbl(),
                        chartComponent.getData().getxUnit(),
                        chartComponent.getData().getxMath(),
                        "Probability density",
                        ViewUnits.NONE,
                        chartComponent.getData().getyMath()),
                        new ChartStyle(new SpColor(chartComponent.getStyle().getColorFX()), 1,
                            LineWidthDefaults.THICKER,
                            LineDashDefaults.STRAIGHT, 0f,
                            MarkerSizeDefaults.SMALL,
                            MarkerStyle.CROSS,
                            false,
                            RendererOption.LINE_AND_SHAPE,
                            LineGraphStyle.LINE));

                    kdeComponents.add(kdeComponent);
                  }
                }
                // Add for legend
                legendComponents.addAll(kdeComponents);
              }

              // If CDF is to be shown
              if (plainSet.getShowCumulative().getValue()) {
                // Also make KDE plots (based on the histo data)
                for (ChartComponent chartComponent : histoComponents) {
                  double[] x = ArrUtils.copy(chartComponent.getData().getX());
                  if (x.length > 0) {
                    Arrays.sort(x);
                    double[] y = ArrUtils.cdf(x);
                    ChartComponent cdfComponent = new ChartComponent(new ChartData(
                        chartComponent.getData().getSeriesName() + " (CDF)",
                        x, y,
                        chartComponent.getData().getxLbl(),
                        chartComponent.getData().getxUnit(),
                        chartComponent.getData().getxMath(),
                        "Cumulative probability",
                        ViewUnits.NONE,
                        chartComponent.getData().getyMath()),
                        new ChartStyle(new SpColor(chartComponent.getStyle().getColorFX()), 1,
                            LineWidthDefaults.THICK,
                            LineDashDefaults.STRAIGHT, 0f,
                            MarkerSizeDefaults.SMALL,
                            MarkerStyle.CROSS,
                            false,
                            RendererOption.LINE_AND_SHAPE,
                            LineGraphStyle.LINE));

                    cdfComponents.add(cdfComponent);
                  }
                }
                // Add for legend
                legendComponents.addAll(cdfComponents);
              }


              Platform.runLater(() -> {
                try {
                  // Define the chart
                  JFreeChart chart = null;

                  // If histograms are to be shown.
                  if (plainSet.getShowHistogram().getValue()) {
                    // Add for legend
                    legendComponents.addAll(histoComponents);

                    // create histograms
                    Pair<JFreeChart, Double> chartPair = SpChartFactory.createHistogram(
                        histoComponents,
                        plainSet.getHistoNormalization(),
                        plainSet.getBinWidthEstimator(),
                        plainSet.getCustomBinWidth());
                    // Extract chart:
                    chart = chartPair.getKey();
                    // Extract & set estimated bin width in the parameter set: use "parent" as we are in
                    // anonymous class
                    plainSet.setEstimatedBinWidth(chartPair.getValue(), fxParent);

                    // Overlay KDE?
                    if (plainSet.getShowKernelDensity().getValue()) {
                      SpChartFactory.addLineData(chart, kdeComponents);
                    }
                    // Overlay CDF?
                    if (plainSet.getShowCumulative().getValue()) {
                      SpChartFactory.addLineData(chart, cdfComponents);
                    }

                    //Show only KDE?
                  } else if (plainSet.getShowKernelDensity().getValue()) {
                    chart = SpChartFactory.createLineChart(kdeComponents);
                    // Overlay CDF?
                    if (plainSet.getShowCumulative().getValue()) {
                      SpChartFactory.addLineData(chart, cdfComponents);
                    }
                    // Show only CDF?
                  } else if (plainSet.getShowCumulative().getValue()) {
                    chart = SpChartFactory.createLineChart(cdfComponents);
                  }

                  // Check to adjust axis range
                  if (chart != null) {
                    if (plainSet.isLimitAxes()) {
                      Range yRange = chart.getXYPlot().getRangeAxis().getRange();

                      double xLimUp;
                      if (plainSet.getUsePercentileForX().getValue()) {
                        xLimUp = percentiles.stream()
                            .filter(Double::isFinite) // exclude NaNs from being passed as bin width cand.
                            .max(Double::compareTo)
                            .orElse(0d);
                        if (xLimUp == 0 && !percentiles.isEmpty()) {
                          LOGGER.trace("Percentile limit gave zero. Cannot set limit.");
                        }
                      } else {
                        xLimUp = plainSet.getUpperXLimitAbsolut().getValue();
                      }

                      if (plainSet.getLowerXLimit() < xLimUp) {
                        chart.getXYPlot().getDomainAxis().setRange(new Range(plainSet.getLowerXLimit(),
                            xLimUp));
                      }

                      if (plainSet.getUpperYLimit() > yRange.getLowerBound()) {
                        chart.getXYPlot().getRangeAxis().setRange(new Range(yRange.getLowerBound(),
                            plainSet.getUpperYLimit()));
                      }
                    }

                    // Check to put ruler
                    if (plainSet.isShowRuler()) {
                      for (Pair<Double, Colors> rulerRoot : rulerRoots) {
                        // rewind the math
                        double plainPos = plainSet.getMathMod().invert(rulerRoot.getKey());
                        double yMax = 0.9 * chart.getXYPlot().getRangeAxis().getRange().getUpperBound();
                        // add the ruler positions in correct math-space
                        double[] positions = new double[]{1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 40, 50};
                        for (int i = 0; i < positions.length; i++) {
                          double m = positions[i];
                          ValueMarker marker = new ValueMarker(plainSet.getMathMod().calc(plainPos * m));
                          marker.setPaint(rulerRoot.getValue().get());
                          marker.setStroke(LineWidthDefaults.THICKER.getStroke());
                          chart.getXYPlot().addDomainMarker(marker);

                          XYTextAnnotation annotation = new XYTextAnnotation("n=" + m, marker.getValue(),
                              yMax * Math.pow(0.95, i));
                          annotation.setFont(new Font("Tahoma", Font.BOLD,
                              SpTool3Main.getRunTime().getConfParams().getAxisFontSize() - 3));
                          chart.getXYPlot().addAnnotation(annotation);
                        }

                      }
                    }

                    // check LOD
                    if (plainSet.isShowMaxThrLOD()) {
                      for (ChartComponent histoComponent : histoComponents) {
                        ChartData data = histoComponent.getData();
                        if (data instanceof HistogramChartData) {
                          double thr = ((HistogramChartData) data).getThrLOD();

                          ValueMarker marker = new ValueMarker(thr);
                          marker.setPaint(histoComponent.getStyle().getPaint());
                          marker.setStroke(new BasicStroke(
                              LineWidthDefaults.THICKER.get(),
                              BasicStroke.CAP_ROUND,
                              BasicStroke.JOIN_ROUND,
                              1.0f,
                              LineDashDefaults.L.get(),
                              0.0f)
                          );
                          chart.getXYPlot().addDomainMarker(marker);
                        }
                      }
                    }

                    ChartContainer container = SpChartFactory.bundleChartLegend(
                        viewerRef.get(),
                        chart,
                        legendComponents,
                        800, 500,
                        false,
                        Orientation.VERTICAL,
                        false);

                    viewerRef.set(container.viewer);
                    targetPane.setCenter(container.combinedPane);

                  } else {
                    // just some empty dummy
                    Platform.runLater(() -> {
                      try {
                        targetPane.setCenter(new AnchorPane());
                      } catch (Exception e) {
                        LOGGER.error("Error while plotting! "
                            + " Message: " + ExceptionUtils.getMessage(e)
                            + " Details: " + ExceptionUtils.getStackTrace(e));
                      }
                    });
                  }
                } catch (Exception e) {
                  LOGGER.error("Error while plotting! "
                      + " Message: " + ExceptionUtils.getMessage(e)
                      + " Details: " + ExceptionUtils.getStackTrace(e));
                }
              });

            }
            progress.set(1d);
            return new EmptyTaskResult();
          } catch (Exception e) {
            LOGGER.error("Error while plotting! "
                + " Message: " + ExceptionUtils.getMessage(e)
                + " Details: " + ExceptionUtils.getStackTrace(e));
            return new EmptyTaskResult();
          }
        }

      };
      SpTool3Main.getRunTime().getTaskManager().forceToGraphPool(
          new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false, new EmptyTaskResult()));


    }
  }


  public static class HistoCompareViewer extends ViewerFXParamSet {


    public HistoCompareViewer(CompareHistoParams plainSet) {
      super(plainSet);
    }

    @Override
    public double getValueWidth() {
      return 135d;
    }

    @Override
    public void refreshGraph() {

      CompareHistoParams plainSet = (CompareHistoParams) super.getPlainSet();

      if (plainSet != null) {

        // Extract some previous parameters

        // Create a GridPane and put the viewers on that grid
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        int numCols = 2;

        // 4 params to show
        List<EventParameter> params = new ArrayList<>();
        params.add(EventParameter.AREA);
        params.add(EventParameter.HEIGHT);
        params.add(EventParameter.DURATION);
        params.add(EventParameter.ASYMMETRY_FACTOR);

        // content (how to overlay the simulated "event only samples"?)
        List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
        List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

        for (int i = 0; i < params.size(); i++) {
          EventParameter par = params.get(i);

          Node viewNode = getHistoNode(plainSet, samples, selChannels, selPops, par);

          // Ensure the viewer grows to fill its cell
          GridPane.setHgrow(viewNode, Priority.ALWAYS);
          GridPane.setVgrow(viewNode, Priority.ALWAYS);

          int row = i / numCols;
          int col = i % numCols;
          gridPane.add(viewNode, row, col); // col, row
        }

        // keep in same pulse/runlater of the UI thread
        targetPane.setCenter(gridPane);
      }
    }


    private Node getHistoNode(CompareHistoParams plainSet,
                              List<Sample> samples, List<Channel> selChannels,
                              List<PopulationID> selPops,
                              EventParameter par) {

      // Chart components and additional containers
      List<ChartComponent> histoComponents = new ArrayList<>();
      List<ChartComponent> kdeComponents = new ArrayList<>();
      List<ChartComponent> cdfComponents = new ArrayList<>();

      List<Double> percentiles = new ArrayList<>();

      // Define labels
      Unit unitQ = SpTool3Main.getRunTime().getMainWindowCtl().getUnit();
      AxisLabel xAxisLabel = AxisLabel.getUnit(par, unitQ, samples);
      AxisLabel yAxisLabel = AxisLabel.getYUnit(plainSet.getHistoNormalization());
      String yLabel = yAxisLabel.getLabel();
      Unit yUnit = yAxisLabel.getUnit();
      String xLabel = xAxisLabel.getLabel();
      Unit xUnit = xAxisLabel.getUnit();

      for (Sample sample : samples) {
        for (Channel channel : selChannels) {

          // Decide the color
          Colors color = AnalysisUtils.getColor(sample, channel, samples.size(),
              selChannels.size());
          Colors mainColor = new SpColor(color.get()); // root for the variations for populations

          for (PopulationID populationID : selPops) {

            // no need to calc LOD as we do not show it here
            double maxThrLOD = 0;

            EventType eventType = EventType.NP;

            // Check if we have to extract both? Then add the BG first.
            if (eventType.equals(EventType.BG_NP)) {

              // Set event type to NP in order to plot NP outside if this if statement!
              eventType = EventType.NP;

              double[] data = sample.getData(channel, populationID, EventType.BG, par, unitQ);

              // check math operations
              data = plainSet.getMathMod().calc(data);

              // create component
              if (data.length > 1) {
                histoComponents.add(new ChartComponent(
                    new HistogramChartData(AnalysisUtils.getLabelForPlots(sample,
                        channel,
                        populationID,
                        EventType.BG),
                        data,
                        xLabel, xUnit, plainSet.getMathMod(),
                        yLabel, yUnit, MathMod.NONE,
                        maxThrLOD),
                    new HistoChartStyle(
                        BackgroundHighlight.DARKER.getCol(EventType.BG, color),
                        Math.max(plainSet.getColorAlpha() - 0.075 * samples.indexOf(sample), 0.3),
                        BackgroundHighlight.DARKER.getPattern(EventType.BG_NP)
                    )));
              }
            }

            // Default way to retrieve data.
            double[] data = sample.getData(channel, populationID, eventType, par, unitQ);
            // check math operations
            data = plainSet.getMathMod().calc(data);

            // create component
            if (data.length > 0) {
              histoComponents.add(new ChartComponent(
                  new HistogramChartData(
                      AnalysisUtils.getLabelForPlots(sample,
                          channel,
                          populationID,
                          EventType.NP),
                      data,
                      xLabel, xUnit, plainSet.getMathMod(),
                      yLabel, yUnit, MathMod.NONE,
                      maxThrLOD),
                  new HistoChartStyle(
                      BackgroundHighlight.DARKER.getCol(EventType.NP, color),
                      Math.max(plainSet.getColorAlpha() - 0.075 * samples.indexOf(sample), 0.3),
                      BackgroundHighlight.DARKER.getPattern(EventType.NP)
                  )));

              // update color: makes it brighter or darker for the next plot
              color = Colors.variationHSB(mainColor, mainColor, selPops.indexOf(populationID) + 1);
            }

            // Calculate percentiles in case we need to set axis limits
            if (plainSet.isLimitAxes() && data.length > 0) {
              DescriptiveStatistics stats = new DescriptiveStatistics(data);
              double pctile = plainSet.getUpperXLimitPercentile();
              pctile = Math.max(0, pctile);
              pctile = Math.min(100, pctile);
              percentiles.add(stats.getPercentile(pctile));
            }

          }
        }
      }

      List<ChartComponent> legendComponents = new ArrayList<>();

      // If KDE plots are to be shown.
      if (plainSet.getShowKernelDensity().getValue()) {
        // Also make KDE plots (based on the histo data)
        for (ChartComponent chartComponent : histoComponents) {
          double[] data = chartComponent.getData().getX();
          if (data.length > 2) {
            double bandwidth = BinWidthEstimator.silvermanRule(data)
                * plainSet.getCustomKernelBandwidth();
            KernelDensity kd = new KernelDensity(data, bandwidth);
            double maxVal = ArrUtils.getMax(data);
            // reduce step size if small range or log plot
            double stepSize = 0.1;
            if (maxVal < 100 || plainSet.getMathMod().equals(MathMod.LOG10)) {
              stepSize = 0.001;
            }
            double[] x = ArrUtils.fillArrayExclusive(0, 1.5 * maxVal, stepSize);
            double[] y = new double[x.length];
            for (int i = 0; i < x.length; i++) {
              y[i] = kd.p(x[i]);
            }
            ChartComponent kdeComponent = new ChartComponent(new ChartData(
                chartComponent.getData().getSeriesName() + " (KDE)",
                x, y,
                chartComponent.getData().getxLbl(),
                chartComponent.getData().getxUnit(),
                chartComponent.getData().getxMath(),
                "Probability density",
                ViewUnits.NONE,
                chartComponent.getData().getyMath()),
                new ChartStyle(new SpColor(chartComponent.getStyle().getColorFX()), 1,
                    LineWidthDefaults.THICKER,
                    LineDashDefaults.STRAIGHT, 0f,
                    MarkerSizeDefaults.SMALL,
                    MarkerStyle.CROSS,
                    false,
                    RendererOption.LINE_AND_SHAPE,
                    LineGraphStyle.LINE));

            kdeComponents.add(kdeComponent);
          }
        }
        // Add for legend
        legendComponents.addAll(kdeComponents);
      }

      // If CDF is to be shown
      if (plainSet.getShowCumulative().getValue()) {
        // Also make KDE plots (based on the histo data)
        for (ChartComponent chartComponent : histoComponents) {
          double[] x = ArrUtils.copy(chartComponent.getData().getX());
          if (x.length > 0) {
            Arrays.sort(x);
            double[] y = ArrUtils.cdf(x);
            ChartComponent cdfComponent = new ChartComponent(new ChartData(
                chartComponent.getData().getSeriesName() + " (CDF)",
                x, y,
                chartComponent.getData().getxLbl(),
                chartComponent.getData().getxUnit(),
                chartComponent.getData().getxMath(),
                "Cumulative probability",
                ViewUnits.NONE,
                chartComponent.getData().getyMath()),
                new ChartStyle(new SpColor(chartComponent.getStyle().getColorFX()), 1,
                    LineWidthDefaults.THICK,
                    LineDashDefaults.STRAIGHT, 0f,
                    MarkerSizeDefaults.SMALL,
                    MarkerStyle.CROSS,
                    false,
                    RendererOption.LINE_AND_SHAPE,
                    LineGraphStyle.LINE));

            cdfComponents.add(cdfComponent);
          }
        }
        // Add for legend
        legendComponents.addAll(cdfComponents);
      }

      // Define the chart
      JFreeChart chart = null;

      // If histograms are to be shown.
      if (plainSet.getShowHistogram().getValue()) {
        // Add for legend
        legendComponents.addAll(histoComponents);

        // create histograms
        Pair<JFreeChart, Double> chartPair = SpChartFactory.createHistogram(
            histoComponents,
            plainSet.getHistoNormalization(),
            plainSet.getBinWidthEstimator(),
            plainSet.getCustomBinWidth());
        // Extract chart:
        chart = chartPair.getKey();
        // Extract & set estimated bin width in the parameter set
        plainSet.setEstimatedBinWidth(chartPair.getValue(), this);

        // Overlay KDE?
        if (plainSet.getShowKernelDensity().getValue()) {
          SpChartFactory.addLineData(chart, kdeComponents);
        }
        // Overlay CDF?
        if (plainSet.getShowCumulative().getValue()) {
          SpChartFactory.addLineData(chart, cdfComponents);
        }

        //Show only KDE?
      } else if (plainSet.getShowKernelDensity().getValue()) {
        chart = SpChartFactory.createLineChart(kdeComponents);
        // Overlay CDF?
        if (plainSet.getShowCumulative().getValue()) {
          SpChartFactory.addLineData(chart, cdfComponents);
        }
        // Show only CDF?
      } else if (plainSet.getShowCumulative().getValue()) {
        chart = SpChartFactory.createLineChart(cdfComponents);
      }

      // Check to adjust axis range
      if (chart != null) {
        if (plainSet.isLimitAxes()) {
          Range yRange = chart.getXYPlot().getRangeAxis().getRange();

          double xLimUp;
          if (plainSet.getUsePercentileForX().getValue()) {
            xLimUp = percentiles.stream()
                .filter(Double::isFinite) // exclude NaNs from being passed as bin width cand.
                .max(Double::compareTo)
                .orElse(0d);
            if (xLimUp == 0 && !percentiles.isEmpty()) {
              LOGGER.trace("Percentile limit gave zero. Cannot set limit.");
            }
          } else {
            xLimUp = plainSet.getUpperXLimitAbsolut().getValue();
          }

          if (plainSet.getLowerXLimit() < xLimUp) {
            chart.getXYPlot().getDomainAxis().setRange(new Range(plainSet.getLowerXLimit(),
                xLimUp));
          }

          if (plainSet.getUpperYLimit() > yRange.getLowerBound()) {
            chart.getXYPlot().getRangeAxis().setRange(new Range(yRange.getLowerBound(),
                plainSet.getUpperYLimit()));
          }
        }


        // Create node
        Node viewNode = SpChartFactory.bundleChartLegend(
            chart,
            legendComponents,
            800,
            500).combinedPane;

        return viewNode;
      } else {
        // just some empty dummy
        return new AnchorPane();
      }
    }

  }

}
