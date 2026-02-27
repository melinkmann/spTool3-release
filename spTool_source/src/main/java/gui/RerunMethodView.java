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

import core.SpTool3Main;
import dataModelNew.fxImpl.FxSample;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.SimpleEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.caseImpl.CreateMultipleSubMethodDialog;
import gui.dialog.caseImpl.SubmethodViewer;
import gui.dialog.notification.NotificationFactory;
import gui.util.GlobalFields;
import gui.util.UiUtil;
import gui.viewerCells.ParamSetListCell;
import io.GlobalIO;
import io.XmlUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.*;
import processing.parameterSets.action.Actions;
import processing.parameters.FxParameter;
import util.ClipboardUtils;

public class RerunMethodView implements ParameterView {

  /*
  This is just a popup to show the method of given sample to decide whether to load it.
   */

  private static final Logger LOGGER = LogManager.getLogger(RerunMethodView.class);

  // Link fx-instances to the native ones (natives do not have a pointer to any FX-related object)
  private final HashMap<ParamSet, FxEntry<FxParamSet>> fxMap = new HashMap<>();

  private Scene parentSceneForDialogs;
  private final BorderPane methodBorderPane;
  private final ToolBar topToolbar = new ToolBar();
  private final VBox leftMethodMetaDataBox;
  private final HBox subMethodUpDownBox;

  // only ever have a copy of the original method here to avoid changing
  // the method reference in the sample
  private final Method copyOfMethod;
  private final FxSample fxSample;

  // FX-instance of the current method ("originalMethod" is only needed for an isEqual() comparison
  private final FxMethod fxCopyOfMethod;
  private final ListView<FxEntry<FxParamSet>> subMethodsListView = new ListView<>();

  // FXInstance labels for the sub method view
  private final FxEntryFactory<FxParamSet> factory;

  //Try to remember to scroll position and focus - works!!!
  private final HashMap<FxParamSet, ListView<FxParameter<?>>> viewMap = new HashMap<>();

  public  RerunMethodView(Method method, FxSample fxSample) {

    this.parentSceneForDialogs = SpTool3Main.getMainStage().getScene();

    this.copyOfMethod = method.getCopyWithoutFile();
    this.fxCopyOfMethod = copyOfMethod.getObservableInstance();

    this.fxSample = fxSample;

    this.methodBorderPane = new BorderPane();
    //  Border around the Pane?
    //this.methodBorderPane.setStyle("-fx-border-color: #999999; -fx-border-width: 1.75;");

    // FXInstance labels for the sub method view
    this.factory = new SimpleEntryFactory<>();

    // Functionalities:

    // ### Sub method list view
    this.subMethodsListView.setCellFactory(l -> new ParamSetListCell());
    ClipboardUtils.installCopyHandler(subMethodsListView);
    subMethodsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    subMethodsListView.setPrefWidth(GlobalFields.METHOD_LIST_WIDTH);
    subMethodsListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> notifyItemChange());

    // ListView
    subMethodsListView.setContextMenu(new ContextMenu());

    // Left
    // ############################################################################################
    leftMethodMetaDataBox = new VBox(5);
    leftMethodMetaDataBox.setPrefWidth(GlobalFields.METHOD_LIST_WIDTH);
    methodBorderPane.setLeft(UiUtil.putOnAnchorWithInsets(leftMethodMetaDataBox));
    leftMethodMetaDataBox.setFillWidth(true);

    Label subMethodLbl = new Label("Sub-methods");
    subMethodLbl.setStyle("-fx-font-weight: bold");

    // ####################### add/move ############################################

    Button moveUpBtn = new Button("▲");
    moveUpBtn.setOnAction(e -> {
      List<FxEntry<FxParamSet>> prevSel =
          new ArrayList<>(subMethodsListView.getSelectionModel().getSelectedItems());
      moveUp(subMethodsListView, prevSel);
      reselectByObjectEqualityOfPlain(subMethodsListView, prevSel);
    });

    Button moveDownBtn = new Button("▼");
    moveDownBtn.setOnAction(e -> {
      List<FxEntry<FxParamSet>> prevSel =
          new ArrayList<>(subMethodsListView.getSelectionModel().getSelectedItems());
      moveDown(subMethodsListView, prevSel);
      reselectByObjectEqualityOfPlain(subMethodsListView, prevSel);
    });

    Button subMethodFromListBtn = UiUtil.getToolbarBtn("/img/searchList.png", "Add existing sub-method");
    subMethodFromListBtn.setOnAction(e -> {

      SubmethodViewer dialog = new SubmethodViewer(new FxEntryFactory.ParamSetWithDateEntryFactory(),
          AvailableParameterSets.getAllowedOptions(), FxStageButton.SELECT, null, true);

      Optional<List<FxParamSet>> result = dialog.showAndWait();

      if (result != null && result.isPresent()) {
        List<FxParamSet> results = result.get();
        if (!results.isEmpty()) {
          results.forEach(fx -> {
            FxEntry<FxParamSet> entry = getFromMapOrCreate(fx.getPlainSet());
            copyOfMethod.addSet(entry.unwrap().getPlainSet());
            subMethodsListView.getItems().add(entry);
          });
        }
      }
    });

    Button newSubMethodBtn = UiUtil.getToolbarBtn("/img/create.png", "Create new sub-method");

    newSubMethodBtn.setOnAction(e -> {
      CreateMultipleSubMethodDialog dialog =
          new CreateMultipleSubMethodDialog(AvailableParameterSets.getAllowedInstances(), factory);

      Optional<List<FxParamSet>> res = dialog.showAndWait();
      if (res.isPresent() && !res.get().isEmpty()) {
        List<FxParamSet> fxSets = res.get();
        List<ParamSet> sets = fxSets.stream().map(FxParamSet::getPlainSet).collect(Collectors.toList());

        copyOfMethod.addSets(sets);
        List<FxEntry<FxParamSet>> newEntries =
            sets.stream().map(this::getFromMapOrCreate).collect(Collectors.toList());
        // List's change listener (selection change) will trigger the update of the viewers
        subMethodsListView.getItems().addAll(newEntries);
        subMethodsListView.getSelectionModel().clearSelection();
        newEntries.forEach(entry -> {
          subMethodsListView.getSelectionModel().select(entry);
        });
      }
    });

    subMethodUpDownBox = new HBox(10, subMethodLbl, moveUpBtn, moveDownBtn, newSubMethodBtn,
        subMethodFromListBtn);
    subMethodUpDownBox.setAlignment(Pos.CENTER_LEFT);

    // context menu
    MenuItem removeSubMethod = UiUtil.getImageMenuItem("Remove", "/img/remove.png");
    removeSubMethod.setOnAction(e -> {
      List<FxEntry<FxParamSet>> subMethods = getSelectedEntries();
      if (!subMethods.isEmpty()) {
        NotificationFactory.openYesCancel("Remove sub-methods from method? This is irreversible.", () -> {
          copyOfMethod.removeSets(subMethods.stream().map(FxEntry::unwrap).filter(Predicate.not(Objects::isNull)).map(FxParamSet::getPlainSet).collect(Collectors.toList()));
          subMethodsListView.getItems().removeAll(subMethods);
        });
      }
    });

    MenuItem cloneSubMethod = UiUtil.getImageMenuItem("Clone", "/img/clone.png");
    cloneSubMethod.setOnAction(e -> {
      List<FxEntry<FxParamSet>> subMethods = getSelectedEntries();
      if (!subMethods.isEmpty()) {

        // Make a new copy with new date (i.e., a new instance copy and not an exact copy)
        List<ParamSet> sets =
            subMethods.stream().map(FxEntry::unwrap).map(FxParamSet::getPlainSet).map(ParamSet::getCopyWithNewDate).collect(Collectors.toList());

        // Add copies
        copyOfMethod.addSets(sets);
        List<FxEntry<FxParamSet>> newEntries =
            sets.stream().map(this::getFromMapOrCreate).collect(Collectors.toList());
        // List's change listener (selection change) will trigger the update of the viewers
        subMethodsListView.getItems().addAll(newEntries);
        subMethodsListView.getSelectionModel().clearSelection();
        newEntries.forEach(entry -> {
          subMethodsListView.getSelectionModel().select(entry);
        });
      }
    });

    subMethodsListView.getContextMenu().getItems().addAll(cloneSubMethod, new SeparatorMenuItem(),
        removeSubMethod);

    // Box is filled in the set method call
    setMethod(copyOfMethod); // fills in the listviews

    // ############################################################################################
    methodBorderPane.setTop(topToolbar);

    Label methodLbl = new Label("Method");
    methodLbl.setStyle("-fx-font-weight: bold");

    Button runButton = UiUtil.getToolbarBtn(
        "/img/start.png",
        "Reprocess the sample.");
    runButton.setOnAction(e -> process());

    topToolbar.getItems().addAll(methodLbl,
        new Separator(Orientation.VERTICAL),
        runButton,
        new Separator(Orientation.VERTICAL)
    );

    // ############################################################################################

    notifyItemChange();
  }

  // Getters as helplers to get the unwrapped entries.
  private List<FxParamSet> getSelectedFxSets() {
    List<FxParamSet> selSets = subMethodsListView.getSelectionModel().getSelectedItems().stream()
        .map(FxEntry::unwrap)
        .filter(Predicate.not(Objects::isNull))
        .collect(Collectors.toList());
    return selSets;
  }

  private List<ParamSet> getSelectedSets() {
    List<ParamSet> selSets = subMethodsListView.getSelectionModel().getSelectedItems().stream()
        .map(FxEntry::unwrap)
        .filter(Predicate.not(Objects::isNull))
        .map(FxParamSet::getPlainSet)
        .collect(Collectors.toList());
    return selSets;
  }

  private List<FxEntry<FxParamSet>> getSelectedEntries() {
    List<FxEntry<FxParamSet>> selEntries = subMethodsListView.getSelectionModel().getSelectedItems()
        .stream()
        .filter(Predicate.not(Objects::isNull))
        .collect(Collectors.toList());
    return selEntries;
  }


  public void process() {
    List<FxSample> placeholderList = new ArrayList<>();
    placeholderList.add(fxSample);
    Actions.reprocess(copyOfMethod, placeholderList);
  }

  public void notifyItemChange() {
    List<FxParamSet> fxSubMethods = getSelectedFxSets();
    if (!fxSubMethods.isEmpty()) {
      FxParamSet selFxSubMethod = fxSubMethods.get(0);

      ListView<FxParameter<?>> view;
      if (viewMap.containsKey(selFxSubMethod)) {
        view = viewMap.get(selFxSubMethod);
        view.getItems().clear();
        view.getItems().addAll(selFxSubMethod.getActiveFxParameters());
      } else {
        view = createParamView(selFxSubMethod);
        viewMap.put(selFxSubMethod, view);
      }
      methodBorderPane.setCenter(UiUtil.putOnAnchorWithInsets(view, 10));
    } else {
      methodBorderPane.setCenter(new AnchorPane()); // just something empty
    }
    // Update the "has the method been changed" sign
    notifyValueChange();
  }

  @Override
  public void notifyValueChange() {

  }

  public static ListView<FxParameter<?>> createParamView(FxParamSet subMethod) {

    ObservableList<FxParameter<?>> obsList = FXCollections.observableArrayList();
    ListView<FxParameter<?>> paramView = new ListView<>(obsList);
    // paramView.setStyle("-fx-border-color: #332288; -fx-border-width: 1.0;");
    UiUtil.formatListView(paramView);

    paramView.setPrefHeight(200);

    /*
     Clear the HBox with the label/parameter inside whenever we redraw this.
     Maybe this is too rigorous? If weird bugs occur, reconsider this in any case!
     Idea: By forcing an update, we make sure that there are no weird/unused pointers left.
     */
    subMethod.getAllFxParameters().forEach(FxParameter::clearViewerBox);

    paramView.setCellFactory(new Callback<ListView<FxParameter<?>>, ListCell<FxParameter<?>>>() {
      @Override
      public ListCell<FxParameter<?>> call(ListView<FxParameter<?>> operationListView) {
        return new ListCell<>() {
          @Override
          public void updateItem(FxParameter<?> fxPar, boolean empty) {
            super.updateItem(fxPar, empty);

            // Make BG always gray and text black
            UiUtil.formatListCellGray(this);

            // NO!!! DO NOT EVER DO THIS! THIS CAUSES A VERY NASTY BUG! Sporadically you won't be able to
            // select the nodes anymore!
            // "Prevent cells from being selected which causes formatting issues (e.g. text gets white, ...)
            // updateSelected(false);"

            if (empty || fxPar == null) {
              setText(null);
              setGraphic(null);
            } else {
              // https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html
              // https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel

              // Add these lines to make the listview essentially read-only
              // setMouseTransparent(true); // cannot be reached by mouse
              // setFocusTraversable(false); // cannot be reached with tab/ctl+tab
              setText(null);
              setGraphic(fxPar.getViewerBox(paramView));

              UiUtil.requestShrinkScrollBar(paramView);

            }
          }
        };
      }
    });

    if (subMethod != null) {

      List<FxParameter<?>> fxParameters = subMethod.getActiveFxParameters();
      obsList.addAll(fxParameters);
    }
    return paramView;
  }


  private FxEntry<FxParamSet> getFromMapOrCreate(ParamSet set) {
    FxEntry<FxParamSet> fxParamSetFxEntry;
    if (fxMap.containsKey(set) && fxMap.get(set) != null) {
      fxParamSetFxEntry = fxMap.get(set);
    } else {
      FxParamSet fxParamSet = set.getObservableInstance();
      fxParamSet.setController(this);
      fxParamSetFxEntry = factory.create(fxParamSet);
      fxParamSet.setListeningEntry(fxParamSetFxEntry);
      fxMap.put(set, fxParamSetFxEntry);
    }
    return fxParamSetFxEntry;
  }


  /**
   * Re-selects by literally looking if an FxInstance hast the same PlainParameterSet by calling POJO
   * equals() methods.
   */
  public static <T> void reselectByObjectEqualityOfPlain(ListView<FxEntry<T>> listView,
                                                         List<FxEntry<T>> oldFxEntries) {
    // Old instance of Selectable
    if (!oldFxEntries.isEmpty()) {
      // Clear the selection since the setMethod() call will always try to selectFirst()
      listView.getSelectionModel().clearSelection();
      for (FxEntry<T> oldFxEntry : oldFxEntries) {
        T oldSelection = oldFxEntry.unwrap();
        // Reselect
        for (int i = 0; i < listView.getItems().size(); i++) {
          FxEntry<T> entry = listView.getItems().get(i);
          if (entry.unwrap().equals(oldSelection)) {
            listView.getSelectionModel().select(entry);
          } else if (entry.unwrap() instanceof FxParamSet && oldSelection instanceof FxParamSet) {
            if (((FxParamSet) entry.unwrap()).isEqualPlainObject(((FxParamSet) oldSelection))) {
              listView.getSelectionModel().select(entry);
            }
          }
        }
      }
    }
  }

  public void setMethod(Method method) {
    // Clear the fxMap to avoid pile-up
    fxMap.clear();
    viewMap.clear();

    // refresh the list view:
    // clear
    subMethodsListView.getItems().clear();
    // add
    try {
      List<FxEntry<FxParamSet>> fxSets = new ArrayList<>();
      List<ParamSet> setsInMethod = method.getSets();
      for (ParamSet setInMethod : setsInMethod) {
        FxEntry<FxParamSet> fxEntry = getFromMapOrCreate(setInMethod);
        fxSets.add(fxEntry);
      }
      this.subMethodsListView.getItems().addAll(fxSets);
    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    // cheap reselect
    subMethodsListView.getSelectionModel().selectFirst();
    // refresh the labels on the left hand side
    Label sampleLabel;
    if (fxSample != null) {
      sampleLabel = new Label("Sample: " + fxSample.getPlainSample().getNickName());
      // Add info.
      UiUtil.tooltip(sampleLabel, sampleLabel.getText()
          + "\n\nPath: " + fxSample.getPlainSample().getSampleFile().getPath()
          + "\n\nFile: " + fxSample.getPlainSample().getSampleFile().getFileName()
          + "\n\nName: " + fxSample.getPlainSample().getSampleFile().getNameWithinFile()
          + "\n\nComment: " + fxSample.getPlainSample().getComment());
    } else {
      sampleLabel = new Label("Sample: -");
      UiUtil.tooltip(sampleLabel, "No valid sample is present.");
    }

    leftMethodMetaDataBox.getChildren().clear();
    leftMethodMetaDataBox.getChildren().addAll(
        fxCopyOfMethod.getFxLabel().getValueNode(),

        fxCopyOfMethod.getFxComment().getLabelNode(),
        fxCopyOfMethod.getFxComment().getValueNode(),

        sampleLabel,
        fxCopyOfMethod.getFxDate().getValueNode(),

        new Separator(Orientation.HORIZONTAL), subMethodUpDownBox,

        new Separator(Orientation.HORIZONTAL),

        subMethodsListView);
  }

  private void moveDown(ListView<FxEntry<FxParamSet>> view, List<FxEntry<FxParamSet>> moving) {
    // Do not fire for listener for every change
    List<FxEntry<FxParamSet>> setsInView = new ArrayList<>(view.getItems());
    // Change order
    MethodView.moveDown(setsInView, moving);
    // Set to ListView
    view.getItems().clear();
    view.getItems().addAll(setsInView);
    // Set to Method
    copyOfMethod.clearSets();
    copyOfMethod.addSets(setsInView.stream().map(FxEntry::unwrap).map(FxParamSet::getPlainSet).collect(Collectors.toList()));
  }

  private void moveUp(ListView<FxEntry<FxParamSet>> view, List<FxEntry<FxParamSet>> moving) {
    // Do not fire for listener for every change
    List<FxEntry<FxParamSet>> setsInView = new ArrayList<>(view.getItems());
    // Change order
    MethodView.moveUp(setsInView, moving);
    // Set to ListView
    view.getItems().clear();
    view.getItems().addAll(setsInView);
    // Set to Method
    copyOfMethod.clearSets();
    copyOfMethod.addSets(setsInView.stream().map(FxEntry::unwrap).map(FxParamSet::getPlainSet).collect(Collectors.toList()));
  }

  public BorderPane getBorderPane() {
    return methodBorderPane;
  }

  public void setParentSceneForDialogs(Scene parentSceneForDialogs) {
    if (parentSceneForDialogs != null) {
      this.parentSceneForDialogs = parentSceneForDialogs;
    }
  }

  //////

}
