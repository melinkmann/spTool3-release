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

import core.SpTool3Main;
import gui.MainWindowController;
import gui.PlotPopup;
import gui.util.UiUtil;
import io.GlobalIO;
import io.XmlUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import javax.annotation.Nullable;

import processing.parameterSets.FxParamSet;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.ConfParams;
import visualizer.Browser;
import visualizer.TextAreaAppender;
import visualizer.charts.AxisLimits;

public class GuiParameterManager {

  private final HashMap<String, ViewerControllerFx> viewContrParamSets = new HashMap<>();
  private ParamSet uiLayoutParameters = new UiLayoutParameters();
  private ViewerControllerFx selectedController = null;

  private BorderPane mainViewerMain = new BorderPane(); // dummy
  private MainWindowController mainWindowController = new MainWindowController(); // dummy

  // popups
  private final HashMap<ToggleButton, PlotPopup> popupMap = new HashMap<>();

  private final HashMap<Toggle, AxisLimits> zoomMemory = new HashMap<>();
  private final ToggleGroup toggleGroup;
  private final VBox toggleBox = new VBox(5);
  private final ToggleButton readmeToggle;
  private final ToggleButton methodToggle;
  private final ToggleButton resultsTableToggle;
  private final ToggleButton rawMCToggle;
  private final ToggleButton rawToggle;
  private final ToggleButton blnToggle;
  private final ToggleButton compareHistoToggle;
  private final ToggleButton histoMCToggle;
  private final ToggleButton boxplotToggle;
  private final ToggleButton scatterMCToggle;
  private final ToggleButton eventToggle;
  private final ToggleButton alignToggle;
  private final ToggleButton iclToggle;
  private final ToggleButton averageMCToggle;
  private final ToggleButton quantToggle;
  private final ToggleButton loggerToggle;

  // TODO Remember the SPLIT-PANE position in order to have the same space when coming back to a "tab"
  private final BorderPane logTabPane = new BorderPane();
  private final BorderPane rawDatMcTabPane = new BorderPane();
  private final BorderPane resultsTableTabPane = new BorderPane();
  private final BorderPane rawDatTabPane = new BorderPane();
  private final BorderPane blnTabPane = new BorderPane();
  private final BorderPane compareHistoTabPane = new BorderPane();
  private final BorderPane histoMCTabPane = new BorderPane();
  private final BorderPane boxplotTabPane = new BorderPane();
  private final BorderPane scatterMCTabPane = new BorderPane();
  private final BorderPane averageTabPane = new BorderPane();
  private final BorderPane eventTabPane = new BorderPane();
  private final BorderPane iclTabPane = new BorderPane();
  private final BorderPane quantTabPane = new BorderPane();
  private final BorderPane alignTabPane = new BorderPane();

  public GuiParameterManager() {

    toggleGroup = UiUtil.getTabToggle();
    this.toggleBox.setAlignment(Pos.TOP_CENTER);

    /**
     *  [CHANGE HERE!!!]
     *
     *  [AND ALSO ADD to XmlInstanceDictionary] !!!
     */

    this.readmeToggle = UiUtil.getTabBtn("Start", "/img/readme2.png", toggleGroup);
    readmeToggle.setSelected(true);
    this.methodToggle = UiUtil.getTabBtn("MET", "/img/method.png", toggleGroup);
    this.resultsTableToggle = UiUtil.getTabBtn("TAB", "/img/tab.png", toggleGroup);
    this.rawMCToggle = UiUtil.getTabBtn("Data", "/img/rawViewMC.png", toggleGroup);
    this.rawToggle = UiUtil.getTabBtn("Data", "/img/rawView.png", toggleGroup);
    this.blnToggle = UiUtil.getTabBtn("BLN", "/img/blnView.png", toggleGroup);
    this.compareHistoToggle = UiUtil.getTabBtn("Hist", "/img/4by4histo.png", toggleGroup);
    this.histoMCToggle = UiUtil.getTabBtn("Hist", "/img/histoViewMC.png", toggleGroup);
    this.boxplotToggle = UiUtil.getTabBtn("Box", "/img/boxplot.png", toggleGroup);
    this.scatterMCToggle = UiUtil.getTabBtn("XY", "/img/scatter.png", toggleGroup);
    this.eventToggle = UiUtil.getTabBtn("EVT", "/img/singlePeakView.png", toggleGroup);
    this.alignToggle = UiUtil.getTabBtn("Align", "/img/align.png", toggleGroup);
    this.averageMCToggle = UiUtil.getTabBtn("AVG", "/img/averageView.png", toggleGroup);
    this.iclToggle = UiUtil.getTabBtn("Peak", "/img/ICL.png", toggleGroup);
    this.loggerToggle = UiUtil.getTabBtn("Log", "/img/warnInfo.png", toggleGroup);
    this.quantToggle = UiUtil.getTabBtn("CAL", "/img/quantTab.png", toggleGroup);

    // Read the "submethods" from the file
    Path methodFile = GlobalIO.makeGuiParameterFile();
    List<FxParamSet> sets = XmlUtil.getSubMethodsFromFile(methodFile);

    // Read the global settings
    for (FxParamSet set : sets) {
      if (set.getPlainSet().getXmlType().equals(uiLayoutParameters.getXmlType())) {
        uiLayoutParameters = set.getPlainSet();
        break;
      }
    }

    // Read the graphics controlling parameters, do not load the general UI parameters
    List<ViewerControllerFx> uiSets = sets.stream()
        .filter(s -> s instanceof ViewerControllerFx)
        .map(s -> (ViewerControllerFx) s)
        .collect(Collectors.toList());
    uiSets.forEach(s -> viewContrParamSets.put(s.getPlainSet().getXmlType(), s));

    initialize();
  }

  /**
   * Adds a new instance if not already present.
   */
  public void initialize() {
    /*
     Create a List of all parameter sets that we want to provide
     (i.e., here we put a new instance that later can be written to the XML file).
     Note that the first initialization (if not present in the XML) happens here.
     Also note that the UiLayout Parameters are dummy initialized at the top before the constructor,
     hence we do not have to check here if they are present.
     */

    /**
     *  [CHANGE HERE!!!]
     */
    List<ParamSet> availableUiSets = new ArrayList<>();
    availableUiSets.add(new TableParameters());
    availableUiSets.add(new MonteCarloRawDataParameters());
    availableUiSets.add(new MonteCarloHistoParameters());
    availableUiSets.add(new BoxPlotParameters());
    availableUiSets.add(new MonteCarloScatterPlotParameters());
    availableUiSets.add(new SingleEventViewerParameters());
    availableUiSets.add(new AverageViewerParameters());
    availableUiSets.add(new IclPeakParameters());
    availableUiSets.add(new CompareHistoParams());
    availableUiSets.add(new QuantViewerParams());

    //
    for (ParamSet uiSet : availableUiSets) {
      if (!viewContrParamSets.containsKey(uiSet.getXmlType())) {
        FxParamSet fxSet = uiSet.getObservableInstance();
        if (fxSet instanceof ViewerControllerFx) {
          ViewerControllerFx viewerControllerFx = (ViewerControllerFx) fxSet;
          viewContrParamSets.put(uiSet.getXmlType(), viewerControllerFx);
        }
      }
    }

    writeTargetPanesToControllers();
  }

  private void writeTargetPanesToControllers() {
    // Make a copy of the key set to avoid concurrent confusion, then assign the border pane.
    List<String> keys = new ArrayList<>(viewContrParamSets.keySet());
    for (String key : keys) {
      ViewerControllerFx viewerControllerFx = viewContrParamSets.get(key);
      // Assign the border pane
      BorderPane pane = findPaneOfController(viewerControllerFx);
      if (pane != null) {
        viewerControllerFx.setTargetPane(pane);
      }
    }
  }

  @Nullable
  public BorderPane findPaneOfController(ViewerControllerFx viewerControllerFx) {
    BorderPane pane = switch (viewerControllerFx.getPlainSet().getXmlType()) {

      /**
       *  [CHANGE HERE!!!]
       */
      case TableParameters.XML_ELEMENT_TAG -> getResultsTableTabPane();
      case MonteCarloRawDataParameters.XML_ELEMENT_TAG -> getRawDatMcTabPane();
      case IclPeakParameters.XML_ELEMENT_TAG -> getIclTabPane();
      case MonteCarloScatterPlotParameters.XML_ELEMENT_TAG -> getScatterMCTabPane();
      case MonteCarloHistoParameters.XML_ELEMENT_TAG -> getHistoMCTabPane();
      case SingleEventViewerParameters.XML_ELEMENT_TAG -> getEventTabPane();
      case AverageViewerParameters.XML_ELEMENT_TAG -> getAverageTabPane();
      case BoxPlotParameters.XML_ELEMENT_TAG -> getBoxplotTabPane();
      case QuantViewerParams.XML_ELEMENT_TAG -> getQuantTabPane();
      case CompareHistoParams.XML_ELEMENT_TAG -> getCompareHistoTabPane();


      default -> null;
    };
    return pane;
  }


  public void save() {
    Path methodFile = GlobalIO.makeGuiParameterFile();
    List<ParamSet> paramSets = new ArrayList<>();
    // Global parameters
    paramSets.add(uiLayoutParameters);
    // Plot and graphics parameters
    paramSets.addAll(viewContrParamSets.values().stream()
        .map(FxParamSet::getPlainSet)
        .collect(Collectors.toList()));
    XmlUtil.writeSubMethodsToFile(paramSets, "GUI parameter collection", methodFile);
  }

  public void reset() {
    // Make a copy of the key set to avoid concurrent modification of set
    List<String> keys = new ArrayList<>(viewContrParamSets.keySet());
    for (String key : keys) {
      ViewerControllerFx controllerFx = viewContrParamSets.get(key);
      // Get the plain set and create a new instance
      ParamSet plainSet = controllerFx.getPlainSet();
      plainSet = plainSet.getNewInstance();

      // Global settings
      if (plainSet.getXmlType().equals(uiLayoutParameters.getXmlType())) {
        uiLayoutParameters = plainSet;
      }

      // For graphs: Create an observable instance and cast safely
      FxParamSet fxSet = plainSet.getObservableInstance();
      if (fxSet instanceof ViewerControllerFx) {
        ViewerControllerFx viewerControllerFx = (ViewerControllerFx) fxSet;
        // Override in the HasMap
        viewContrParamSets.put(key, viewerControllerFx);
      }
    }

    // New instances of the Viewers must be matched with their target panes
    writeTargetPanesToControllers();
    // Since the parameters probably changed, also redraw the graph --> Refreshes the parameters and the graph
    viewContrParamSets.values().forEach(ViewerControllerFx::externalUpdate);

  }

  public VBox linkButtonsWithMainViewer(MainWindowController windowController) {
    this.mainWindowController = windowController;
    this.mainViewerMain = windowController.getMainViewerPane();

    /**
     *  [CHANGE HERE!!!]
     */

    readmeToggle.setOnAction(e -> {
      this.selectedController = null;
      Browser browser = new Browser();
      mainViewerMain.setCenter(UiUtil.putOnAnchorWithInsets(
          new VBox(5, browser.getBtnBox(), browser)));
    });

    methodToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Method viewer", methodToggle,
          mainWindowController.getMethodView().getBorderPane(), "");
    });

    rawToggle.setOnAction(e -> {
      this.selectedController = null;
      mainViewerMain.setCenter(rawDatTabPane);
    });

    rawMCToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Raw data viewer", rawMCToggle, rawDatMcTabPane,
          MonteCarloRawDataParameters.XML_ELEMENT_TAG);
    });

    blnToggle.setOnAction(e -> {
      this.selectedController = null;
      mainViewerMain.setCenter(blnTabPane);
    });

    compareHistoToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Multi histogram viewer", compareHistoToggle, compareHistoTabPane,
          CompareHistoParams.XML_ELEMENT_TAG);
    });

    histoMCToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Histogram viewer", histoMCToggle, histoMCTabPane,
          MonteCarloHistoParameters.XML_ELEMENT_TAG);
    });

    scatterMCToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Scatter plot viewer", scatterMCToggle, scatterMCTabPane,
          MonteCarloScatterPlotParameters.XML_ELEMENT_TAG);
    });

    boxplotToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Boxplot viewer", boxplotToggle, boxplotTabPane,
          BoxPlotParameters.XML_ELEMENT_TAG);
    });

    eventToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Single event viewer", eventToggle, eventTabPane,
          SingleEventViewerParameters.XML_ELEMENT_TAG);
    });

    alignToggle.setOnAction(e -> {
      this.selectedController = null;
      mainViewerMain.setCenter(alignTabPane);
    });

    iclToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Peak profile viewer", iclToggle, iclTabPane,
          IclPeakParameters.XML_ELEMENT_TAG);
    });

    quantToggle.setOnMouseClicked(e -> {
      // this.selectedController = viewContrParamSets.get(QuantViewerParams.XML_ELEMENT_TAG);
      // mainViewerMain.setCenter(quantTabPane);
      // notifyPaneSelected(QuantViewerParams.XML_ELEMENT_TAG);
      handleToggleSelection(e, "Calibration and quantification view", quantToggle, quantTabPane,
          QuantViewerParams.XML_ELEMENT_TAG);
    });

    averageMCToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Average trend viewer", averageMCToggle, averageTabPane,
          AverageViewerParameters.XML_ELEMENT_TAG);
    });

    resultsTableToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Table viewer", resultsTableToggle, resultsTableTabPane,
          TableParameters.XML_ELEMENT_TAG);
    });

    loggerToggle.setOnMouseClicked(e -> {
      handleToggleSelection(e, "Logger viewer", loggerToggle, logTabPane, "");
    });
    TextAreaAppender.build(logTabPane);

    // Load defaults from configuration
    if (SpTool3Main.getRunTime() != null && SpTool3Main.getRunTime().getConfParams() != null) {
      ConfParams confParams = SpTool3Main.getRunTime().getConfParams();
      fillButtonList(confParams);
      TextAreaAppender.setComboBoxLevel(confParams.getLogLevel());
    }
    return toggleBox;
  }

  private void notifyPaneSelected(String key) {
    if (viewContrParamSets.containsKey(key)) {
      viewContrParamSets.get(key).notifyPaneSelected();
    }
  }

  /**
   * When we notify that a pane is selected, a new graph is only drawn when there is no graph. This
   * makes sure, that when the selection was not changed, we can access the graphs with their zoom
   * history and in the latest state. Now, when we change selection (sample or trace), we want the
   * SETTINGS to remain but the graph to refresh. However, to maintain performance, we should only
   * refresh what is visible, i.e., not all the viewers at once. Hence, I believe, the easiest way
   * would be to just clear the center of every pane in order to force a refresh.
   */
  public void notifySampleOrPopulationSelectionChange() {
    viewContrParamSets.values().forEach(ViewerControllerFx::clearGraph);
    if (selectedController != null) {
      selectedController.notifyPaneSelected();
    }
    // allow popup to react to sample change
    popupMap.values().forEach(popup -> popup.notifyPaneSelected());
  }

  public void notifyValueChange() {
    if (selectedController != null) {
      selectedController.notifyValueChange();
    }
    // allow popup to react to value change
    popupMap.values().forEach(popup -> popup.notifyPaneSelected());
  }

  public void fillButtonList(ConfParams confParams) {
    toggleBox.getChildren().clear();

    toggleBox.getChildren().add(new Separator(Orientation.HORIZONTAL));

    /**
     *  [CHANGE HERE!!!]
     */

    if (confParams.isShowReadme()) {
      toggleBox.getChildren().add(readmeToggle);
    }

    if (confParams.isShowMethod()) {
      toggleBox.getChildren().add(methodToggle);
    }

    if (confParams.isShowTable()) {
      toggleBox.getChildren().add(resultsTableToggle);
    }

    if (confParams.isShowRawMC()) {
      toggleBox.getChildren().add(rawMCToggle);
    }

    if (confParams.isShowHistoMC()) {
      toggleBox.getChildren().add(histoMCToggle);
    }

    if (confParams.isShowScatterMC()) {
      toggleBox.getChildren().add(scatterMCToggle);
    }

    if (confParams.isShowBoxPlot()) {
      toggleBox.getChildren().add(boxplotToggle);
    }

    if (confParams.isShowSingleEventView()) {
      toggleBox.getChildren().add(eventToggle);
    }

    if (confParams.getShowAverageView().getValue()) {
      toggleBox.getChildren().add(averageMCToggle);
    }

    if (confParams.isShowIcl() && SpTool3Main.SHOW_PEAK_MODEL) {
      toggleBox.getChildren().add(iclToggle);
    }

    if (confParams.getShowCompareHistoToggle().getValue()) {
      toggleBox.getChildren().add(compareHistoToggle);
    }

    if (confParams.isShowQuantToggle() && SpTool3Main.getANALYZER()) {
      toggleBox.getChildren().add(quantToggle);
    }

    if (confParams.isShowLogger()) {
      toggleBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
      toggleBox.getChildren().add(loggerToggle);
    }

    // Fire first instance in the list
    for (Node child : toggleBox.getChildren()) {
      if (child instanceof ToggleButton) {
        ((ToggleButton) child).fire(); // fire after telling it what that means
        break;
      }
    }

    // Full version
    //    tabLikeBtnVbox.getChildren().addAll(
    //        new Separator(Orientation.HORIZONTAL),
    //        readmeTab,methodTab, rawTab, blnTab, histoTab, boxplotTab, eventTab, alignTab);

  }

  public UiLayoutParameters getLayoutParameters() {
    return (UiLayoutParameters) uiLayoutParameters;
  }

  /**
   * [CHANGE HERE!!!]
   */

  public BorderPane getRawDatMcTabPane() {
    return rawDatMcTabPane;
  }

  public BorderPane getBlnTabPane() {
    return blnTabPane;
  }

  public BorderPane getHistoMCTabPane() {
    return histoMCTabPane;
  }

  public BorderPane getScatterMCTabPane() {
    return scatterMCTabPane;
  }

  public BorderPane getBoxplotTabPane() {
    return boxplotTabPane;
  }

  public BorderPane getEventTabPane() {
    return eventTabPane;
  }

  public BorderPane getIclTabPane() {
    return iclTabPane;
  }

  public BorderPane getAlignTabPane() {
    return alignTabPane;
  }

  public BorderPane getResultsTableTabPane() {
    return resultsTableTabPane;
  }

  public BorderPane getAverageTabPane() {
    return averageTabPane;
  }

  public BorderPane getQuantTabPane() {
    return quantTabPane;
  }

  public BorderPane getCompareHistoTabPane() {
    return compareHistoTabPane;
  }

  /**
   * Getting the sets with the neutral getter below is somewhat tedious, as a lot of casting is
   * involved. Hence, we provide these easy access methods below.
   */
  @Nullable
  public ViewerControllerFx getSet(String string) {
    return viewContrParamSets.get(string);
  }

  @Nullable
  public MonteCarloHistoParameters getMonteCarloHistoParameters() {
    MonteCarloHistoParameters histoSet = null;
    ViewerControllerFx histoController = getSet(MonteCarloHistoParameters.XML_ELEMENT_TAG);
    if (histoController != null) {
      ParamSet plainSet = histoController.getPlainSet();
      if (plainSet instanceof MonteCarloHistoParameters) {
        histoSet = ((MonteCarloHistoParameters) plainSet);
      }
    }
    return histoSet;
  }

  public Toggle getSelectedToggle() {
    return toggleGroup.getSelectedToggle();
  }

  public HashMap<Toggle, AxisLimits> getZoomMemory() {
    return zoomMemory;
  }

  public void updateZoom(AxisLimits currentLimits) {
    zoomMemory.put(getSelectedToggle(), currentLimits);
  }

  @Nullable
  public AxisLimits getZoom() {
    // Locking for the event view is just annoying
    if (getSelectedToggle().equals(eventToggle)) {
      return null;
    } else {
      return zoomMemory.get(getSelectedToggle());
    }
  }

  private void handleToggleSelection(MouseEvent e,
                                     String title,
                                     ToggleButton toggle,
                                     BorderPane viewPane,
                                     String xml_element_tag) {

    this.selectedController = viewContrParamSets.get(xml_element_tag);

    if (e.getButton() == MouseButton.SECONDARY) {
      // naturally, it is not selected on right click
      toggle.setSelected(true);
      // create popup (i.e. the stage) if not already done
      if (!popupMap.containsKey(toggle)) {
        popupMap.put(toggle, new PlotPopup(title, mainViewerMain, viewPane,
            toggle,
            this.selectedController));
      }
      PlotPopup popup = popupMap.get(toggle);
      popup.show();
    } else {
      if (popupMap.containsKey(toggle) && popupMap.get(toggle).isShowing()) {
        // Hide puts the rawDatMcTabPane back on to the mainViewerMain
        popupMap.get(toggle).hide();
      } else {
        mainViewerMain.setCenter(viewPane);
      }
    }
    notifyPaneSelected(xml_element_tag);
  }


}
