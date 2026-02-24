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
import analysis.quant.*;
import com.google.common.util.concurrent.AtomicDouble;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.TISeries;
import dataModelNew.TISeriesRAM;
import dataModelNew.Trace;
import dataModelNew.mz.Element;
import dataModelNew.mz.IsotopeMZ;
import gui.MethodView;
import gui.ParameterView;
import gui.StageFactory;
import gui.dialog.notification.NotificationFactory;
import gui.util.NumberIterator;
import gui.util.UiUtil;

import java.awt.BasicStroke;
import java.awt.Font;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.annotation.Nullable;

import math.AverageUtils;
import math.Smoothing;
import math.SavitzkyGolay;
import math.regression.RegressionUtils;
import math.stat.MeasureOfLocation;
import math.stat.PeakSymmetry;
import math.stat.PreFilter;
import math.units.Unit;
import math.units.enums.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
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
import visualizer.charts.AxisLabel;
import visualizer.charts.AxisLabel.PlainLabel;
import visualizer.charts.AxisUtils;
import visualizer.charts.RendererOption;
import visualizer.charts.SpChartFactory;
import visualizer.charts.SpChartFactory.ChartComponent;
import visualizer.charts.SpChartFactory.ChartContainer;
import visualizer.charts.SpChartFactory.ChartData;
import visualizer.charts.SpChartFactory.ChartStyle;
import visualizer.charts.SpChartFactory.HistoChartStyle;
import visualizer.charts.SpChartFactory.HistogramChartData;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;
import visualizer.styles.FontStyles;
import visualizer.styles.LineGraphStyle;
import visualizer.styles.LineLineDashDefaults;
import visualizer.styles.LineWidthDefaults;
import visualizer.styles.MarkerSize.CustomMarkerSize;
import visualizer.styles.MarkerSizeDefaults;
import visualizer.styles.MarkerStyle;
import visualizer.styles.OkabeItoColors;
import visualizer.styles.UiColors;

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
            List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
            if (samples != null && !samples.isEmpty() && selIsotopes != null && !selIsotopes.isEmpty()) {

              ResultTableData tableData = new ResultTableData(samples, selIsotopes, selPops, false);
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
      SpTool3Main.getRunTime().getTaskManager().forceToHousekeepingPool(
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
            TableColumn<ResponseTableRow, Isotope> isotopeCol = new TableColumn<>("Isotope");
            isotopeCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getIsotope()));
            isotopeCol.setCellFactory(col -> new TableCell<>() {
              @Override
              protected void updateItem(Isotope item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(item.getName());
                }
              }
            });
            table.getColumns().add(isotopeCol);

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
            TableColumn<ResponseTableRow, Isotope> isotopeCol = new TableColumn<>("Isotope");
            isotopeCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getIsotope()));
            isotopeCol.setCellFactory(col -> new TableCell<>() {
              @Override
              protected void updateItem(Isotope item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(item.getName());
                }
              }
            });
            table.getColumns().add(isotopeCol);

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
            TableColumn<ResponseTableRow, Isotope> isotopeCol = new TableColumn<>("Isotope");
            isotopeCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getIsotope()));
            isotopeCol.setCellFactory(col -> new TableCell<>() {
              @Override
              protected void updateItem(Isotope item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(item.getName());
                }
              }
            });
            table.getColumns().add(isotopeCol);

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
                  double fraction = ChemistryUtils.massFractionCDK(result.get(), element.getSymbol());
                  if (fraction > 0 && fraction <= 1) {
                    subPar.getNpMassFraction().setValue(Math.round(fraction * 1_000_000d) / 1_000_000d);
                  } else {
                    // the other elements like are not present, thus set them 0; else: frac dont add up
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

            List<Element> selElements = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes().stream()
                .map(Isotope::getElement)
                .distinct().collect(Collectors.toList());

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
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
      List<Element> selElements = selIsotopes.stream()
          .map(Isotope::getElement)
          .distinct().collect(Collectors.toList());

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
        List<Element> allEleInSample = samples.get(0).listIsotopes().stream()
            .map(Isotope::getElement)
            .distinct().collect(Collectors.toList());
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
        tableModel.fill(spCalSet);  // removed "selIsotopes"

        // refresh the graph/containers
        currentRegressionDataContainer = new RegressionViewContainer(
            spCalSet,
            regTypeComboBox.getValue(),
            samples, selIsotopes, selPops);

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
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      List<ChartComponent> allRawComponents = new ArrayList<>();
      List<ChartComponent> allNpComponents = new ArrayList<>();
      List<ChartComponent> allBgComponents = new ArrayList<>();

      if (selSamples != null && !selSamples.isEmpty()
          && selIsotopes != null && !selIsotopes.isEmpty()) {

        // Extract the grouped samples; when averaging, the viewer can show more than 1 sample.
        List<Sample> allSamples = new ArrayList<>();
        selSamples.forEach(s -> allSamples.addAll(s.getAllSamples()));

        for (Sample sample : allSamples) {
          if (sample != null) {

            List<Trace> selTraces = sample.getTraces(selIsotopes);

            for (Trace trace : selTraces) {
              if (plainSet.getShowRawData().getValue()) {
                Colors color = AnalysisUtils.getColor(sample,
                    trace.getMzValue().getIsotope(), allSamples.size(), selIsotopes.size());
                List<ChartComponent> rawComponent = getRawComponent(trace, plainSet, color);
                allRawComponents.addAll(rawComponent);
              }

              for (PopulationID pop : selPops) {

                Colors color = AnalysisUtils.getColor(sample,
                    trace.getMzValue().getIsotope(), allSamples.size(), selIsotopes.size());

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

      TISeries average = AverageUtils.average(
          trace,
          windowSec,
          plainSet.getRawLocationMeasure().getValue(),
          plainSet.getRawPreFilter().getValue());

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
              trace.getMzValue().getName() + "_" + trace.getSample().getNickName(),
              average.getTime(),
              average.getIntensity(),
              "Time", TimeUnit.SECOND, MathMod.NONE,
              "Intensity", IntensityUnit.CTS, MathMod.NONE),
          new ChartStyle(color, 1,
              LineWidthDefaults.MEDIUM_THICK,
              LineLineDashDefaults.STRAIGHT,
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
                    + trace.getMzValue().getName() + "_" + trace.getSample().getNickName()
                    + ")",
                npReg.x,
                npReg.y,
                "Time", TimeUnit.SECOND, MathMod.NONE,
                "Intensity", IntensityUnit.CTS, MathMod.NONE),
            new ChartStyle(color, 0.75,
                LineWidthDefaults.THINNER,
                LineLineDashDefaults.STRAIGHT,
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
                "NP of " + trace.getMzValue().getName() + " from " + pop.toString()
                    + " in " + trace.getSample().getNickName(),
                npAverage.getTime(),
                npAverage.getIntensity(),
                "Time", TimeUnit.SECOND, MathMod.NONE,
                yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
            new ChartStyle(color, 1,
                LineWidthDefaults.MEDIUM_THICK,
                LineLineDashDefaults.STRAIGHT,
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
                      + " (" + trace.getMzValue().getName() + " from" + pop.toString()
                      + " in " + trace.getSample().getNickName()
                      + ")",
                  npReg.x,
                  npReg.y,
                  "Time", TimeUnit.SECOND, MathMod.NONE,
                  yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
              new ChartStyle(color, 0.75,
                  LineWidthDefaults.THINNER,
                  LineLineDashDefaults.STRAIGHT,
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
                "BG of " + trace.getMzValue().getName() + " from " + pop.toString()
                    + " in " + trace.getSample().getNickName(),
                bgAverage.getTime(),
                bgAverage.getIntensity(),
                "Time", TimeUnit.SECOND, MathMod.NONE,
                yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
            new ChartStyle(color, 1,
                LineWidthDefaults.MEDIUM_THICK,
                LineLineDashDefaults.STRAIGHT,
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
                      + "(" + trace.getMzValue().getName() + " from" + pop.toString()
                      + " in " + trace.getSample().getNickName()
                      + ")",
                  bgReg.x,
                  bgReg.y,
                  "Time", TimeUnit.SECOND, MathMod.NONE,
                  yAxisLabel.getLabel(), yAxisLabel.getUnit(), MathMod.NONE),
              new ChartStyle(color, 0.75,
                  LineWidthDefaults.THINNER,
                  LineLineDashDefaults.STRAIGHT,
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
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
      List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

      if (samples != null && !samples.isEmpty() && !selIsotopes.isEmpty() && !selPops.isEmpty()) {

        // so far we only show one sample in the MC raw view.
        Sample sample = samples.get(0);
        Isotope isotope = selIsotopes.get(0);
        List<Event> events = sample.getNPEvents(selIsotopes.get(0), selPops.get(0));

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

              eventSeries.add(event.getProfile());
              globalEvtIndex.add(i);
              eventColor.add(AnalysisUtils.getColor(
                  sample,
                  selIsotopes.get(0),
                  1,
                  selIsotopes.size()));
              previewSeries.add(event.getPreviousDP(previewWidth));
              postviewSeries.add(event.getFollowingDP(previewWidth));

              eventAreaLabels.add(event.getLabel());
            }
          }
        }

        List<JFreeChart> charts = new ArrayList<>();

        for (int i = 0; i < eventSeries.size(); i++) {

          // Create fresh for every single plot or else events accumualte in each view
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
                new ChartData("Evt # " + (globalEvtIndex.get(i) + 1) + " - " + isotope.getName(),
                    eventData,
                    "Time", TimeUnit.SECOND, MathMod.NONE,
                    "Intensity", IntensityUnit.CTS, MathMod.NONE),
                new ChartStyle(color, 1,
                    LineWidthDefaults.MEDIUM_THICK,
                    LineLineDashDefaults.STRAIGHT,
                    MarkerSizeDefaults.SMALL,
                    MarkerStyle.CROSS,
                    false,
                    RendererOption.LINE_AND_SHAPE,
                    LineGraphStyle.LINE_AND_MARKER)
            );
          } else {
            comp = new ChartComponent(
                new ChartData("Evt # " + (globalEvtIndex.get(i) + 1) + " - " + isotope.getName(),
                    eventData,
                    "Time", TimeUnit.SECOND, MathMod.NONE,
                    "Intensity", IntensityUnit.CTS, MathMod.NONE),
                new ChartStyle(color, 1,
                    LineWidthDefaults.MEDIUM_THICK,
                    LineLineDashDefaults.STRAIGHT,
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
                  "Intensity", IntensityUnit.CTS, MathMod.NONE),
              new ChartStyle(OkabeItoColors.BLACK_DARK, 1,
                  LineWidthDefaults.MEDIUM,
                  LineLineDashDefaults.STRAIGHT,
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
                  "Intensity", IntensityUnit.CTS, MathMod.NONE),
              new ChartStyle(OkabeItoColors.BLACK_DARK, 1,
                  LineWidthDefaults.MEDIUM,
                  LineLineDashDefaults.STRAIGHT,
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
            charts.forEach(c -> c.getXYPlot().getRangeAxis().setRange(new Range(0, maxY.get())));
          }
        }

        // Keep as much in the same pulse/runlater on the UI thread
        SpTool3Main.getRunTime().getGuiParameterManager().getEventTabPane()
            .setCenter(UiUtil.putOnAnchorWithoutInsets(gridPane));
      }
    } // refresh
  }


  public static class MonteCarloRawDataViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    public MonteCarloRawDataViewer(MonteCarloRawDataParameters plainSet) {
      super(plainSet);
    }

    @Override
    public void refreshGraph() {
      // The viewer exists in the background and we need to reset is stopped as well as progress each time
      progress.set(0d);
      isStopped.set(false);

      MonteCarloRawDataParameters plainSet = (MonteCarloRawDataParameters) super.getPlainSet();

      // update UI in FXThread
      UiUtil.showLoading(SpTool3Main.getRunTime().getGuiParameterManager().getRawDatMcTabPane());

      // retreive while in UI Thread
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
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

                if (selIsotopes != null && !selIsotopes.isEmpty()) {

                  List<Trace> selTraces = sample.getTraces(selIsotopes);

                  if (!selTraces.isEmpty()) {

                    // max events to show per population
                    int maxEventsToShow = plainSet.getUpperPointCountCutoff().getValue();
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

                      mainCharts.add(new ChartComponent(
                          new ChartData(trace.getXYSeries(),
                              "Time", TimeUnit.SECOND, MathMod.NONE,
                              "Intensity", IntensityUnit.CTS, MathMod.NONE),
                          new ChartStyle(color, 1,
                              LineWidthDefaults.MEDIUM_THICK,
                              LineLineDashDefaults.STRAIGHT,
                              MarkerSizeDefaults.SMALL,
                              MarkerStyle.CROSS,
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
                                  LineLineDashDefaults.STRAIGHT,
                                  MarkerSizeDefaults.LARGE,
                                  plotPop.getMarker() != null ? plotPop.getMarker()
                                      : markerIterator.next(),
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
                                  LineLineDashDefaults.STRAIGHT,
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
                                    LineLineDashDefaults.L,
                                    MarkerSizeDefaults.SMALL,
                                    MarkerStyle.BAR,
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
                                    LineLineDashDefaults.L_2,
                                    MarkerSizeDefaults.SMALL,
                                    MarkerStyle.BAR,
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
                                      LineLineDashDefaults.L_2,
                                      MarkerSizeDefaults.SMALL,
                                      MarkerStyle.BAR,
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
                                      LineLineDashDefaults.L_3,
                                      MarkerSizeDefaults.SMALL,
                                      MarkerStyle.BAR,
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
                        // detach old child
                        SpTool3Main.getRunTime().getGuiParameterManager().getRawDatMcTabPane().setCenter(null);

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

                        Node viewNode = SpChartFactory.bundleChartLegend(
                            chart,
                            allLegendComponents,
                            800, 500).combinedPane;

                        SpTool3Main.getRunTime().getGuiParameterManager().getRawDatMcTabPane()
                            .setCenter(viewNode);
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

      SpTool3Main.getRunTime().getTaskManager().forceToHousekeepingPool(
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
                LineLineDashDefaults.STRAIGHT,
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
                    LineLineDashDefaults.STRAIGHT,
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
                    LineLineDashDefaults.STRAIGHT,
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

      // update UI in FXThread
      UiUtil.showLoading(targetPane);

      // Sample selection: retrieve in UI thread
      List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
      List<PopulationID> selPop = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

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
              Unit unit = SpTool3Main.getRunTime().getMainWindowCtl().getUnit();
              AxisLabel xAxisLabel = AxisLabel.getUnit(plainSet.getEventParameterX().getValue(), unit);
              AxisLabel yAxisLabel = AxisLabel.getUnit(plainSet.getEventParameterY().getValue(), unit);
              String yLabel = yAxisLabel.getLabel();
              Unit yUnit = yAxisLabel.getUnit();
              String xLabel = xAxisLabel.getLabel();
              Unit xUnit = xAxisLabel.getUnit();

              // Show populations with the same scatter marker
              HashMap<PopulationID, MarkerStyle> markerMap = new HashMap<>();
              Iterator<MarkerStyle> markerIterator = MarkerStyle.getScatterAwtIterator().iterator();
              selPop.forEach(p -> markerMap.put(p, markerIterator.next()));

              // progress counter
              int totalSteps = samples.size() * selIsotopes.size();
              int step = 0;

              for (Sample sample : samples) {

                isotopeLoop:
                for (Isotope isotope : selIsotopes) {
                  // update progress
                  progress.set((double) step / totalSteps);
                  step++;
                  if (isStopped.get()) {
                    break isotopeLoop;
                  }

                  // Decide the color
                  Colors color = AnalysisUtils.getColor(sample, isotope, samples.size(),
                      selIsotopes.size());

                  for (PopulationID populationID : selPop) {

                    double[] xData = sample.getData(isotope, populationID,
                        EventType.NP, plainSet.getEventParameterX().getValue(), unit);

                    double[] yData = sample.getData(isotope, populationID,
                        EventType.NP, plainSet.getEventParameterY().getValue(), unit);

                    xData = plainSet.getMathModificationX().getValue().calc(xData);
                    yData = plainSet.getMathModificationY().getValue().calc(yData);

                    // create component
                    if (xData.length > 0 && yData.length > 0) {
                      chartComponents.add(new ChartComponent(
                          new ChartData(AnalysisUtils.getLabelForPlots(sample,
                              new IsotopeMZ(isotope),
                              populationID,
                              EventType.NP),
                              xData, yData,
                              xLabel, xUnit, plainSet.getMathModificationX().getValue(),
                              yLabel, yUnit, plainSet.getMathModificationY().getValue()),
                          new ChartStyle(color, plainSet.getColorAlpha().getValue(),
                              LineWidthDefaults.MEDIUM_THICK,
                              LineLineDashDefaults.STRAIGHT,
                              new CustomMarkerSize(plainSet.getDotSize().getValue()),
                              markerMap.get(populationID),
                              // when varying symbol from map, make sure to override
                              true,
                              RendererOption.LINE_AND_SHAPE,
                              LineGraphStyle.MARKER)
                      ));
                    }
                  }
                }
              }

              Platform.runLater(() -> {
                try {
                  // Define the chart
                  JFreeChart chart = SpChartFactory.createLineChart(
                      chartComponents);

                  // Check to adjust axis range
                  if (chart != null) {
                    if (plainSet.getLimitAxes().getValue()) {
                      Range xRange = chart.getXYPlot().getDomainAxis().getRange();
                      Range yRange = chart.getXYPlot().getRangeAxis().getRange();

                      double xLimUp = plainSet.getUpperXLimit().getValue();
                      double yLimUp = plainSet.getUpperYLimit().getValue();

                      // Make sure that upper > lower!
                      if (xLimUp > xRange.getLowerBound()) {
                        chart.getXYPlot().getDomainAxis().setRange(new Range(xRange.getLowerBound(), xLimUp));
                      }

                      if (yLimUp > yRange.getLowerBound()) {
                        chart.getXYPlot().getRangeAxis().setRange(new Range(yRange.getLowerBound(), yLimUp));
                      }
                    }
                  }

                  // Create node
                  ChartContainer container = SpChartFactory.bundleChartLegend(
                      chart,
                      chartComponents,
                      800,
                      500);
                  Node viewNode = container.combinedPane;

                  // TODO :)
                  ChartViewer viewer = container.viewer;
                  // PolygonOverlay.enablePolygon(viewer);

                  targetPane.setCenter(viewNode);
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

      SpTool3Main.getRunTime().getTaskManager().forceToHousekeepingPool(
          new SimpleLinearBatch<>(graphTask.getTaskName(), graphTask, false, new EmptyTaskResult()));

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
        List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
        List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

        List<ChartComponent> components = new ArrayList<>();

        if (selSamples != null && !selSamples.isEmpty()
            && selIsotopes != null && !selIsotopes.isEmpty()
            && selPops != null && !selPops.isEmpty()) {

          // Define labels
          Unit unit = SpTool3Main.getRunTime().getMainWindowCtl().getUnit();
          AxisLabel yAxisLabel = AxisLabel.getUnit(plainSet.getEventParameter(), unit);
          AxisLabel xAxisLabel = new PlainLabel("Index", ViewUnits.NONE);
          String yLabel = yAxisLabel.getLabel();
          Unit yUnit = yAxisLabel.getUnit();
          String xLabel = xAxisLabel.getLabel();
          Unit xUnit = xAxisLabel.getUnit();

          // Retrieve data
          EventType evtType = plainSet.getEventType().getValue();

          for (Sample sample : selSamples) {
            for (Isotope iso : selIsotopes) {

              Colors color = AnalysisUtils.getColor(sample, iso,
                  selSamples.size(), selIsotopes.size());

              Colors originalColor = color;

              for (PopulationID popID : selPops) {
                // variation with index=0 returns original!
                color = Colors.variationHSB(originalColor, color, selPops.indexOf(popID));

                // check data source
                List<double[]> combinedData = new ArrayList<>();

                // Check if we have to extract both? Then add the BG first.
                if (evtType.equals(EventType.BG_NP)) {

                  // Set event type to NP in order to plot NP outside if this if statement!
                  evtType = EventType.NP;

                  double[] data = sample.getData(iso, popID, EventType.BG,
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
                double[] data = sample.getData(iso, popID, evtType,
                    plainSet.getEventParameter(), unit);
                // check math operations
                data = plainSet.getMathModification().getValue().calc(data);
                combinedData.add(data);

                // Merge the data
                data = ArrUtils.merge(combinedData);

                ChartComponent component = new ChartComponent(
                    new HistogramChartData(
                        AnalysisUtils.getLabelForPlots(sample, new IsotopeMZ(iso), popID, evtType),
                        AnalysisUtils.getShortCodeNameForPlots(sample, selSamples, iso, popID, selPops),
                        data,
                        xLabel, xUnit, MathMod.NONE,
                        yLabel, yUnit, plainSet.getMathModification().getValue(), 0d),
                    new ChartStyle(color, 0.95,
                        LineWidthDefaults.THICKER,
                        LineLineDashDefaults.L,
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

  public static class MonteCarloHistoViewer extends ViewerFXParamSet {

    private final AtomicDouble progress = new AtomicDouble(0);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

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
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
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
              AxisLabel xAxisLabel = AxisLabel.getUnit(plainSet.getEventParameter(), qUnit);
              AxisLabel yAxisLabel = AxisLabel.getYUnit(plainSet.getHistoNormalization());
              String yLabel = yAxisLabel.getLabel();
              Unit yUnit = yAxisLabel.getUnit();
              String xLabel = xAxisLabel.getLabel();
              Unit xUnit = xAxisLabel.getUnit();

              // progress counter
              int totalSteps = samples.size() * selIsotopes.size();
              int step = 0;

              for (Sample sample : samples) {

                isotopeLoop:
                for (Isotope isotope : selIsotopes) {
                  // update progress
                  progress.set((double) step / totalSteps);
                  step++;
                  if (isStopped.get()) {
                    break isotopeLoop;
                  }

                  // Decide the color
                  Colors color = AnalysisUtils.getColor(sample, isotope, samples.size(),
                      selIsotopes.size());
                  Colors mainColor = new SpColor(color.get()); // root for the variations for populations

                  for (PopulationID populationID : selPops) {
                    // calc LOD
                    boolean isNet = plainSet.getEventParameter().equals(EventParameter.NET_AREA)
                        || plainSet.getEventParameter().equals(EventParameter.NET_HEIGHT);
                    double maxThrLOD = sample.getMaxThr(isotope, populationID, isNet, qUnit);
                    maxThrLOD = plainSet.getMathMod().calc(maxThrLOD);

                    EventType eventType = plainSet.getEventType();

                    // Check if we have to extract both? Then add the BG first.
                    if (eventType.equals(EventType.BG_NP)) {

                      // Set event type to NP in order to plot NP outside if this if statement!
                      eventType = EventType.NP;

                      double[] data = sample.getData(isotope, populationID, EventType.BG,
                          plainSet.getEventParameter(), qUnit);

                      if (plainSet.isJitterBG()) {
                        data = Statistics.quantileSampleWithMurmurHashedJitter(data,
                            plainSet.getNumberOfBGEvents(),
                            2);
                      }

                      // check math operations
                      data = plainSet.getMathMod().calc(data);

                      // create component
                      if (data.length > 1) {
                        histoComponents.add(new ChartComponent(
                            new HistogramChartData(AnalysisUtils.getLabelForPlots(sample,
                                new IsotopeMZ(isotope),
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
                    double[] data = sample.getData(isotope, populationID, eventType,
                        plainSet.getEventParameter(), qUnit);
                    // check math operations
                    data = plainSet.getMathMod().calc(data);

                    // create component
                    if (data.length > 0) {
                      histoComponents.add(new ChartComponent(
                          new HistogramChartData(AnalysisUtils.getLabelForPlots(sample,
                              new IsotopeMZ(isotope),
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
                            LineLineDashDefaults.STRAIGHT,
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
                            LineLineDashDefaults.STRAIGHT,
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
                              LineLineDashDefaults.L_3.get(),
                              0.0f)
                          );
                          chart.getXYPlot().addDomainMarker(marker);
                        }
                      }
                    }

                    // Create node
                    Node viewNode = SpChartFactory.bundleChartLegend(
                        chart,
                        legendComponents,
                        800,
                        500).combinedPane;

                    targetPane.setCenter(viewNode);
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
      SpTool3Main.getRunTime().getTaskManager().forceToHousekeepingPool(
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
        List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
        List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

        for (int i = 0; i < params.size(); i++) {
          EventParameter par = params.get(i);

          Node viewNode = getHistoNode(plainSet, samples, selIsotopes, selPops, par);

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
                              List<Sample> samples, List<Isotope> selIsotopes,
                              List<PopulationID> selPops,
                              EventParameter par) {

      // Chart components and additional containers
      List<ChartComponent> histoComponents = new ArrayList<>();
      List<ChartComponent> kdeComponents = new ArrayList<>();
      List<ChartComponent> cdfComponents = new ArrayList<>();

      List<Double> percentiles = new ArrayList<>();

      // Define labels
      Unit unitQ = SpTool3Main.getRunTime().getMainWindowCtl().getUnit();
      AxisLabel xAxisLabel = AxisLabel.getUnit(par, unitQ);
      AxisLabel yAxisLabel = AxisLabel.getYUnit(plainSet.getHistoNormalization());
      String yLabel = yAxisLabel.getLabel();
      Unit yUnit = yAxisLabel.getUnit();
      String xLabel = xAxisLabel.getLabel();
      Unit xUnit = xAxisLabel.getUnit();

      for (Sample sample : samples) {
        for (Isotope isotope : selIsotopes) {

          // Decide the color
          Colors color = AnalysisUtils.getColor(sample, isotope, samples.size(),
              selIsotopes.size());
          Colors mainColor = new SpColor(color.get()); // root for the variations for populations

          for (PopulationID populationID : selPops) {

            // no need to calc LOD as we do not show it here
            double maxThrLOD = 0;

            EventType eventType = EventType.NP;

            // Check if we have to extract both? Then add the BG first.
            if (eventType.equals(EventType.BG_NP)) {

              // Set event type to NP in order to plot NP outside if this if statement!
              eventType = EventType.NP;

              double[] data = sample.getData(isotope, populationID, EventType.BG, par, unitQ);

              // check math operations
              data = plainSet.getMathMod().calc(data);

              // create component
              if (data.length > 1) {
                histoComponents.add(new ChartComponent(
                    new HistogramChartData(AnalysisUtils.getLabelForPlots(sample,
                        new IsotopeMZ(isotope),
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
            double[] data = sample.getData(isotope, populationID, eventType, par, unitQ);
            // check math operations
            data = plainSet.getMathMod().calc(data);

            // create component
            if (data.length > 0) {
              histoComponents.add(new ChartComponent(
                  new HistogramChartData(
                      AnalysisUtils.getLabelForPlots(sample,
                          new IsotopeMZ(isotope),
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
                    LineLineDashDefaults.STRAIGHT,
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
                    LineLineDashDefaults.STRAIGHT,
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
