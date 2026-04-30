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

package visualizer;

import analysis.PopulationID;
import core.RunTimeInstance;
import core.SpTool3Main;
import dataModelNew.Sample;
import gui.dialog.notification.NotificationFactory;
import gui.table.TableUtils;
import gui.util.UiUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.util.Pair;
import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import processing.options.MathMod;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;
import visualizer.ResultTableData.TraceCol;
import visualizer.charts.AxisLabel;

public abstract class ResultsTable {

  private static final Logger LOGGER = LogManager.getLogger(ResultsTable.class);

  private static final DoubleProperty STATIC_WIDTH = new SimpleDoubleProperty(120d);

  public static final String EMPTY_CELL = "";

  public static class TableRowData {

    private final TablePar tablePar;
    private final SimpleStringProperty label;
    private final Map<TraceCol, SimpleStringProperty> traceValues;

    public TableRowData(TablePar par, String label, List<TraceCol> traces, List<String> values) {
      this.tablePar = par;
      this.label = new SimpleStringProperty(label);
      this.traceValues = new LinkedHashMap<>();

      for (int i = 0; i < traces.size(); i++) {
        traceValues.put(traces.get(i), new SimpleStringProperty(values.get(i)));
      }
    }

    public TablePar getTablePar() {
      return tablePar;
    }

    public StringProperty labelProperty() {
      return label;
    }

    public StringProperty getValueProperty(TraceCol trace) {
      return traceValues.get(trace);
    }
  }

  public static TableView<TableRowData> buildNestedTable(Map<Sample, List<TraceCol>> entryMap) {
    TableView<TableRowData> tableView = new TableView<>();

    if (!entryMap.isEmpty()) {

      // === First column: Parameter ===
      TableColumn<TableRowData, String> paramCol = new TableColumn<>("Parameter");
      paramCol.setCellValueFactory(data -> data.getValue().labelProperty());
      tableView.getColumns().add(paramCol);

      paramCol.setCellFactory(col -> new TableCell<>() {

        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);

          if (empty || item == null) {
            setText(null);
            setGraphic(null);
          } else if (getTableRow() != null) {
            TableRowData rowData = getTableRow().getItem();
            if (rowData != null && rowData.getTablePar().getCategory().get() != null) {
              ImageView iconView = rowData.getTablePar().getCategory().get();
              iconView.setFitHeight(16);
              iconView.setFitWidth(16);
              setGraphic(iconView);
            } else {
              setGraphic(null);
            }
            setText(item); // keep the label next to the image
          }
        }
      });
      paramCol.setPrefWidth(150);

      // Collect traces in the same order they will appear in the columns
      List<TraceCol> allTraces = new ArrayList<>();
      for (Map.Entry<Sample, List<TraceCol>> entry : entryMap.entrySet()) {
        Sample sample = entry.getKey();
        List<TraceCol> traceCols = entry.getValue();

        // Parent column = Sample nickname
        TableColumn<TableRowData, ?> parentCol = new TableColumn<>(sample.getNickName());

        for (TraceCol traceCol : traceCols) {
          allTraces.add(traceCol);

          TableColumn<TableRowData, String> traceChildCol = new TableColumn<>(traceCol.getLabel());

          traceChildCol.setCellValueFactory(data -> {
            TableRowData row = data.getValue();
            return row.getValueProperty(traceCol);
          });

          traceChildCol.setCellFactory(col -> new TableCell<TableRowData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
              super.updateItem(item, empty);

              if (empty || item == null) {
                setText(null);
                setTooltip(null);
              } else {
                setText(item);

                // Add tooltip showing full string
                Tooltip tooltip = new Tooltip(item);
                tooltip.setStyle("-fx-font-size: 15");
                Duration duration = new Duration(60_000);
                tooltip.setShowDuration(duration);
                setTooltip(tooltip);
              }
            }
          });

          traceChildCol.setMinWidth(50);
          traceChildCol.setPrefWidth(STATIC_WIDTH.get());
          traceChildCol
              .setMaxWidth(Math.max(STATIC_WIDTH.get(), 55)); // enforce that you can always shrink
          traceChildCol.prefWidthProperty().bindBidirectional(STATIC_WIDTH);
          traceChildCol.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            STATIC_WIDTH.set(newWidth.doubleValue());
            traceChildCol.setMaxWidth(Double.MAX_VALUE);
          });

          parentCol.getColumns().add(traceChildCol);
        }

        tableView.getColumns().add(parentCol);
      }

      // === Build rows ===
      List<TableRowData> rows = new ArrayList<>();
      // Use first TraceCol to extract parameters (assuming consistent parameters across all except for
      // Gate which is filled according to max woe number)
      List<ResultTableData.TraceRowContainer> mappingList = allTraces.get(0).getMappingList();

      for (int paramIndex = 0; paramIndex < mappingList.size(); paramIndex++) {
        TablePar par = mappingList.get(paramIndex).getPar();
        String label = mappingList.get(paramIndex).getRowLabel();

        List<String> values = new ArrayList<>();
        for (TraceCol traceCol : allTraces) {
          values.add(traceCol.getMappingList().get(paramIndex).getValue());
        }

        rows.add(new TableRowData(par, label, allTraces, values));
      }

      tableView.setItems(FXCollections.observableArrayList(rows));

      // --- Utilities ---
      TableUtils.installCopyPasteHandler(tableView);
      tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
      TableUtils.installDoubleClickSelect(tableView);
      // Enable cell selection
      tableView.getSelectionModel().setCellSelectionEnabled(true);

      // Context menu
      tableView.setContextMenu(new ContextMenu());

      MenuItem selectMenu = UiUtil.getImageMenuItem("Select", "/img/method.png");
      selectMenu.setOnAction(e -> {
        if (RunTimeInstance.getParamTableDefaults() != null) {
          RunTimeInstance.getParamTableDefaults().showDialog();
        }
      });
      tableView.getContextMenu().getItems().add(selectMenu);

      MenuItem saveMenu = UiUtil.getImageMenuItem("Save", "/img/save.png");
      saveMenu.setOnAction(e -> {
        NotificationFactory.openYesNo("Save changes?", () -> {
          if (RunTimeInstance.getParamTableDefaults() != null) {
            RunTimeInstance.getParamTableDefaults().write();
          }
        });
      });
      tableView.getContextMenu().getItems().add(saveMenu);


      MenuItem reloadMenu = UiUtil.getImageMenuItem("Load", "/img/load.png");
      reloadMenu.setOnAction(e -> {
        NotificationFactory.openYesNo("Discard current selection and reload latest saved version?", () -> {
          if (RunTimeInstance.getParamTableDefaults() != null) {
            RunTimeInstance.getParamTableDefaults().reload();
            SpTool3Main.getRunTime().getGuiParameterManager().notifyValueChange();
          }
        });
      });
      tableView.getContextMenu().getItems().add(reloadMenu);

      MenuItem resetMenu = UiUtil.getImageMenuItem("All", "/img/ctlZ.png");
      resetMenu.setOnAction(e -> {
        NotificationFactory.openYesNo("Select all options and discard customized selection?", () -> {
          if (RunTimeInstance.getParamTableDefaults() != null) {
            RunTimeInstance.getParamTableDefaults().reset();
            SpTool3Main.getRunTime().getGuiParameterManager().notifyValueChange();
          }
        });
      });
      tableView.getContextMenu().getItems().add(resetMenu);
    }

    return tableView;
  }


  private enum TableCategory {

    UNKOWN {
      @Override
      public ImageView get() {
        ImageView view = UiUtil.getViewer("/img/tableSample.png");
        return view;
      }
    },

    RAW {
      @Override
      public ImageView get() {
        ImageView view = UiUtil.getViewer("/img/tableSample.png");
        return view;
      }
    },

    TRACE {
      @Override
      public ImageView get() {
        ImageView view = UiUtil.getViewer("/img/tableTrace.png");
        return view;
      }
    },

    POP {
      @Override
      public ImageView get() {
        ImageView view = UiUtil.getViewer("/img/tablePop.png");
        return view;
      }
    },

    BLN {
      @Override
      public ImageView get() {
        ImageView view = UiUtil.getViewer("/img/tableBLN.png");
        return view;
      }
    },

    QUANT {
      @Override
      public ImageView get() {
        ImageView view = UiUtil.getViewer("/img/tableQuant.png");
        return view;
      }
    };


    public abstract ImageView get();
  }

  public enum TablePar {
    SAMPLE_NAME {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSampleName();
      }
    },

    SAMPLE_NICK_NAME {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabNickName();
      }
    },

    SAMPLE_FOLDER {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSampleFolder();
      }
    },

    SAMPLE_FULL_PATH {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabFullPath();
      }
    },

    SAMPLE_COMMENT {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabComment();
      }
    },

    SAMPLE_HIGHLIGHT {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabHighlight();
      }
    },

    REMOVED_MZ {
      @Override
      protected String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabRemovedIsotopes();
      }
    },

    /// ///////////////////////////////////////////////////////////////////////////

    TRACE_MZ {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        String val = check(i) ? i.getName() : EMPTY_CELL;
        return val;
      }
    },

    DWELL_TIME {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabDwellTime(i);
      }
    },

    DWELL_TIME_MS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        String dtStr = s.tabDwellTime(i);
        double dtus = SnF.strToDoubleSilent(dtStr);
        if (dtus > 0) {
          return str(dtus / 1000, NF.D1C2);
        }else {
          return EMPTY_CELL;
        }
      }
    },

    DURATION {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabDuration(i);
      }
    },

    DATA_POINTS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPoints(i);
      }
    },

    TI_SERIES_LIMITS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabTISeriesLimits(i);
      }
    },

    RAW_MEAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabRawMean(i);
      }
    },

    RAW_MEAN_CPS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabRawMeanCPS(i);
      }
    },

    RAW_MEDIAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabRawMedian(i);
      }
    },

    RAW_MEDIAN_CPS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabRawMedianCPS(i);
      }
    },

    RAW_SD {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabRawSD(i);
      }
    },

    RAW_MAD {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabRawMAD(i);
      }
    },

    SIA_SHAPE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSIAShape(i);
      }
    },

    MEAN_SIA_SHAPE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMeanSIAShape(i);
      }
    },

    AEROSOL_TE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return SnF.doubleToString(s.getAerosolTEConvention(i), NF.D1C2);
      }
    },

    PNC_TE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return SnF.doubleToString(s.getPncTEConvention(i), NF.D1C2);
      }
    },

    BG_EQUIV_CONC {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabEquivBGConc(i);
      }
    },

    POPULATION_NAME {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopName(i, p);
      }
    },

    POPULATION_ADDITIONAL {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopAdditional(i, p);
      }
    },

    LOD_CTS {
      @Override
      protected String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabLodCts(i, p);
      }
    },

    LOD_AG {
      @Override
      protected String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabLodAg(i, p);
      }
    },

    LOD_AMOL {
      @Override
      protected String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabLodAmol(i, p);
      }
    },

    LOD_NM {
      @Override
      protected String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabLodNm(i, p);
      }
    },

    NP_COUNT {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopNpCount(i, p);
      }
    },

    PNC {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPNC(i, p);
      }
    },

    NP_RATE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopNpRate(i, p);
      }
    },

    NP_MEAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopNpMean(i, p);
      }
    },

    NP_SD {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabNpSD(i, p);
      }
    },


    NP_MEDIAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabNpMedian(i, p);
      }
    },

    NP_MEAN_HEIGHT {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMeanHeight(i, p);
      }
    },

    NP_SD_HEIGHT {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSdHeight(i, p);
      }
    },

    NP_MEAN_DURATION {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMeanDuration(i, p);
      }
    },

    NP_SD_DURATION {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSdDuration(i, p);
      }
    },

    // QUANT
    NP_MEAN_SIZE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMeanSize(i, p);
      }
    },
    NP_MEDIAN_SIZE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMedianSize(i, p);
      }
    },
    NP_SD_SIZE {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSizeSD(i, p);
      }
    },
    NP_MEAN_MASS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMeanMass(i, p);
      }
    },
    NP_MEDIAN_MASS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMedianMass(i, p);
      }
    },
    NP_SD_MASS {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMassSD(i, p);
      }
    },
    NP_MEAN_MOL {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMeanMol(i, p);
      }
    },
    NP_MEDIAN_MOL {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMedianMol(i, p);
      }
    },
    NP_SD_MOL {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabMolSD(i, p);
      }
    },


    NP_CUSTOM_MEAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        EventParameter par = SpTool3Main.getRunTime().getConfParams().getEventParameter().getValue();
        MathMod math = SpTool3Main.getRunTime().getConfParams().getEventMathModification().getValue();
        return s.tabPopNpCustomParamMean(i, p, par, math);
      }
    },

    NP_CUSTOM_SD {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        EventParameter par = SpTool3Main.getRunTime().getConfParams().getEventParameter().getValue();
        MathMod math = SpTool3Main.getRunTime().getConfParams().getEventMathModification().getValue();
        return s.tabNpCustomParamSD(i, p, par, math);
      }
    },


    NP_CUSTOM_MEDIAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        EventParameter par = SpTool3Main.getRunTime().getConfParams().getEventParameter().getValue();
        MathMod math = SpTool3Main.getRunTime().getConfParams().getEventMathModification().getValue();
        return s.tabNpCustomParamMedian(i, p, par, math);
      }
    },

    POP_BG_MEAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopBgMean(i, p);
      }
    },

    POP_BG_SD {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopBgSD(i, p);
      }
    },

    POP_BG_N {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopBgN(i, p);
      }
    },

    POP_DRIFT {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabPopDrift(i, p);
      }
    },

    BLN_DISTR {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabBlnDistr(i, p);
      }
    },

    BLN_MEAN {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabBlnMean(i, p);
      }
    },

    BLN_SD {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabBlnSD(i, p);
      }
    },

    BLN_OUTLIER_Z {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabBlnOutlierZ(i, p);
      }
    },

    SEARCH_START_META {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSearchStartMeta(i, p);
      }
    },

    SEARCH_START {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSearchStart(i, p);
      }
    },

    SEARCH_STOP_META {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSearchStopMeta(i, p);
      }
    },

    SEARCH_STOP {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSearchStop(i, p);
      }
    },

    SEARCH_HEIGHT_META {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSearchHeightMeta(i, p);
      }
    },

    SEARCH_HEIGHT {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        return s.tabSearchHeight(i, p);
      }
    },

    GATES_META {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        String val = EMPTY_CELL;
        return val;
      }

      public List<String> getValues(Sample s, Isotope i, PopulationID p, int fillRows) {
        List<String> results = s.tabGatesMeta(i, p);
        if (results.size() < fillRows) {
          while (results.size() < fillRows) {
            results.add(EMPTY_CELL);
          }
        }
        return results;
      }
    },

    GATES {
      public String getValue(Sample s, Isotope i, PopulationID p) {
        String val = EMPTY_CELL;
        return val;
      }

      public List<String> getValues(Sample s, Isotope i, PopulationID p, int fillRows) {
        List<String> results = s.tabGates(i, p);
        if (results.size() < fillRows) {
          while (results.size() < fillRows) {
            results.add(EMPTY_CELL);
          }
        }
        return results;
      }
    };

    public TableCategory getCategory() {
      TableCategory cat = TableCategory.UNKOWN;
      if (getSampleInfo().contains(this)) {
        cat = TableCategory.RAW;
      } else if (getTraceInfo().contains(this)) {
        cat = TableCategory.TRACE;
      } else if (getPopulationInfo().contains(this)) {
        cat = TableCategory.POP;
      } else if (getPopulationBLNInfo().contains(this)) {
        cat = TableCategory.BLN;
      } else if (getQuantInfo().contains(this)) {
        cat = TableCategory.QUANT;
      }
      return cat;
    }

    public static List<TablePar> getValuesOrdered() {
      List<TablePar> values = new ArrayList<>();
      values.addAll(TablePar.getSampleInfo());
      values.addAll(TablePar.getTraceInfo());
      values.addAll(TablePar.getQuantInfo());
      values.addAll(TablePar.getPopulationInfo());
      values.addAll(TablePar.getPopulationBLNInfo());
      return values;
    }


    public static List<TablePar> getSampleInfo() {
      return new ArrayList<>(Arrays.asList(
          SAMPLE_NICK_NAME,
          SAMPLE_NAME,
          SAMPLE_FOLDER,
          SAMPLE_FULL_PATH,
          SAMPLE_COMMENT,
          SAMPLE_HIGHLIGHT));
    }

    public static List<TablePar> getTraceInfo() {
      if (SpTool3Main.getANALYZER()) {
        return new ArrayList<>(Arrays.asList(
            TRACE_MZ,
            DWELL_TIME,
            DWELL_TIME_MS,
            DURATION,
            DATA_POINTS,
            TI_SERIES_LIMITS,
            RAW_MEAN,
            RAW_MEDIAN,
            RAW_SD,
            RAW_MAD,
            RAW_MEAN_CPS,
            RAW_MEDIAN_CPS,
            SIA_SHAPE,
            MEAN_SIA_SHAPE,
            BLN_DISTR,
            BLN_MEAN,
            BLN_SD,
            BLN_OUTLIER_Z
        ));
      } else {
        return new ArrayList<>(Arrays.asList(
            TRACE_MZ,
            DWELL_TIME,
            DWELL_TIME_MS,
            DURATION,
            DATA_POINTS,
            TI_SERIES_LIMITS,
            RAW_MEAN,
            RAW_MEDIAN,
            RAW_SD,
            RAW_MAD,
            RAW_MEAN_CPS,
            RAW_MEDIAN_CPS,
            SIA_SHAPE,
            MEAN_SIA_SHAPE
        ));
      }
    }

    public static List<TablePar> getPopulationInfo() {

      if (SpTool3Main.getANALYZER()) {
        List<TablePar> pars = new ArrayList<>(Arrays.asList(
            POPULATION_NAME,
            POPULATION_ADDITIONAL,
            NP_COUNT,
            PNC,
            NP_RATE,
            NP_MEAN,
            NP_SD,
            NP_MEDIAN,
            NP_MEAN_HEIGHT,
            NP_SD_HEIGHT,
            NP_MEAN_DURATION,
            NP_SD_DURATION,
            NP_MEAN_SIZE,
            NP_MEDIAN_SIZE,
            NP_SD_SIZE,
            NP_MEAN_MASS,
            NP_MEDIAN_MASS,
            NP_SD_MASS,
            NP_CUSTOM_MEAN,
            NP_CUSTOM_MEDIAN,
            NP_CUSTOM_SD,
            POP_BG_MEAN,
            POP_BG_SD,
            POP_BG_N,
            POP_DRIFT,
            LOD_CTS,
            LOD_NM,
            LOD_AG,
            LOD_AMOL
        ));
        if (!SpTool3Main.SHOW_DRIFT) {
          pars.remove(POP_DRIFT);
        }
        return pars;
      } else {
        return new ArrayList<>(Arrays.asList(
            POPULATION_NAME,
            POPULATION_ADDITIONAL,
            NP_COUNT,
            NP_RATE,
            NP_MEAN,
            NP_SD,
            NP_MEDIAN,
            NP_MEAN_HEIGHT,
            NP_SD_HEIGHT,
            NP_MEAN_DURATION,
            NP_SD_DURATION,
            NP_CUSTOM_MEAN,
            NP_CUSTOM_MEDIAN,
            NP_CUSTOM_SD,
            POP_BG_MEAN,
            POP_BG_SD
        ));
      }

    }

    public static List<TablePar> getPopulationBLNInfo() {
      if (SpTool3Main.getANALYZER()) {
        return new ArrayList<>(Arrays.asList(
            SEARCH_START_META,
            SEARCH_START,
            SEARCH_STOP_META,
            SEARCH_STOP,
            SEARCH_HEIGHT_META,
            SEARCH_HEIGHT,
            GATES_META,
            GATES
        ));
      } else {
        return new ArrayList<>();
      }
    }

    public static List<TablePar> getQuantInfo() {
      if (SpTool3Main.getANALYZER()) {
        return new ArrayList<>(Arrays.asList(
            AEROSOL_TE,
            PNC_TE,
            BG_EQUIV_CONC
        ));
      } else {
        return new ArrayList<>(Arrays.asList(
        ));
      }
    }


    // I think, we should keep the nice and tidy switch statement for the getLabel() case.
    public String rowLabel() {
      return rowLabel(this);
    }

    protected abstract String getValue(Sample s, Isotope i, PopulationID p);

    public List<String> getValues(Sample s, Isotope t, PopulationID p, int fillRows) {
      List<String> vals = new ArrayList<>();
      vals.add(getValue(s, t, p));
      return vals;
    }

    private static boolean check(Sample sample) {
      return sample != null;
    }

    private static boolean check(Isotope isotope) {
      return isotope != null;
    }

    private static String str(double d, NF nf) {
      return SnF.doubleToString(d, nf);
    }

    private static double mu(double[] data) {
      return MeasureOfLocation.MEAN.calc(data);
    }

    private static double sd(double[] data) {
      return MeasureOfSpread.SD.calc(data);
    }

    private static double md(double[] data) {
      return MeasureOfLocation.MEDIAN.calc(data);
    }

    public static String rowLabel(TablePar par) {

      String res = switch (par) {
        case SAMPLE_NICK_NAME -> "Nick name";
        case SAMPLE_NAME -> "Sample name";
        case SAMPLE_FOLDER -> "Folder";
        case SAMPLE_FULL_PATH -> "Path";
        case SAMPLE_COMMENT -> "Comment";
        case SAMPLE_HIGHLIGHT -> "Marked";
        case REMOVED_MZ -> "Removed m/z";

        case TRACE_MZ -> "Isotope m/z";

        case DWELL_TIME -> "Dwell time [µs]";
        case DWELL_TIME_MS -> "Dwell time [ms]";
        case DURATION -> "Duration [s]";
        case DATA_POINTS -> "Data points [-]";
        case TI_SERIES_LIMITS -> "Limits";
        case RAW_MEAN -> "Raw mean [cts]";
        case RAW_MEDIAN -> "Raw median [cts]";
        case RAW_MEAN_CPS -> "Raw mean [cts/s]";
        case RAW_MEDIAN_CPS -> "Raw median [cts/s]";
        case RAW_SD -> "Raw SD [cts]";
        case RAW_MAD -> "Raw MAD SD [cts]";
        case SIA_SHAPE -> "SIA shape [-]";
        case MEAN_SIA_SHAPE -> "Mean SIA shape [-]";

        case AEROSOL_TE -> "TE [%]";
        case PNC_TE -> "PNC TE [%]";
        case BG_EQUIV_CONC -> "BG conc. [µg/L]";
        case POPULATION_NAME -> "Population";
        case POPULATION_ADDITIONAL -> "Input parameters";
        case NP_COUNT -> "NP number [-]";
        case PNC -> "PNC [NP/mL]";
        case NP_RATE -> "NP rate [NP/s]";
        case NP_MEAN -> "NP mean net area [cts]";
        case NP_SD -> "NP net area SD [cts]";
        case NP_MEDIAN -> "NP median net area [cts]";
        case NP_MEAN_HEIGHT -> "NP mean height [cts]";
        case NP_SD_HEIGHT -> "NP height SD [cts]";
        case NP_MEAN_DURATION -> "NP mean width [µs]";
        case NP_SD_DURATION -> "NP width SD [µs]";

        case NP_MEAN_SIZE -> "Mean size [nm]";
        case NP_MEDIAN_SIZE -> "Median size [nm]";
        case NP_SD_SIZE -> "Size SD [nm]";
        case NP_MEAN_MASS -> "Mean elemental mass [fg]";
        case NP_MEDIAN_MASS -> "Median elemental mass [fg]";
        case NP_SD_MASS -> "Elemental mass SD [fg]";

        case NP_SD_MOL -> "Number of moles SD [amol]";
        case NP_MEAN_MOL -> "Mean number of moles [amol]";
        case NP_MEDIAN_MOL -> "Median number of moles [amol]";

        case NP_CUSTOM_MEAN -> {
          EventParameter p = SpTool3Main.getRunTime().getConfParams().getEventParameter().getValue();
          MathMod math = SpTool3Main.getRunTime().getConfParams().getEventMathModification().getValue();
          yield "Custom NP mean " + math.toString() + " " + p.toString() + " [" + AxisLabel.getUnit(p).getUnit().getUiString() + "]";
        }
        case NP_CUSTOM_MEDIAN -> {
          EventParameter p = SpTool3Main.getRunTime().getConfParams().getEventParameter().getValue();
          MathMod math = SpTool3Main.getRunTime().getConfParams().getEventMathModification().getValue();
          yield "Custom NP median " + math.toString() + " " + p.toString() + " [" + AxisLabel.getUnit(p).getUnit().getUiString() +
              "]";
        }
        case NP_CUSTOM_SD -> {
          EventParameter p = SpTool3Main.getRunTime().getConfParams().getEventParameter().getValue();
          MathMod math = SpTool3Main.getRunTime().getConfParams().getEventMathModification().getValue();
          yield "Custom NP SD " + math.toString() + " " + p.toString() + " [" + AxisLabel.getUnit(p).getUnit().getUiString() + "]";
        }

        case LOD_AG -> "LOD [ag/NP]";
        case LOD_NM -> "LOD [nm]";
        case LOD_CTS -> "LOD [cts/NP]";
        case LOD_AMOL -> "LOD [amol/NP]";

        case POP_BG_MEAN -> "BG mean height [cts]";
        case POP_BG_SD -> "BG height SD [cts]";
        case POP_BG_N -> "BG event count [-]";
        case POP_DRIFT -> "Drift [-]";

        case BLN_DISTR -> "BLN model";
        case BLN_MEAN -> "BLN µ [cts]";
        case BLN_SD -> "BLN SD [cts]";
        case BLN_OUTLIER_Z -> "BLN outlier z [-]";
        case SEARCH_START -> "Search start [cts]";
        case SEARCH_STOP -> "Search stop [cts]";
        case SEARCH_HEIGHT -> "Search height [cts]";
        case GATES -> "Gate value [cts or -]";
        case SEARCH_START_META -> "Search start";
        case SEARCH_STOP_META -> "Search stop";
        case SEARCH_HEIGHT_META -> "Search height";
        case GATES_META -> "Gate";
      };
      return res;
    }
  }


}
