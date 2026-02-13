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
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.SimpleEntryFactory;
import gui.util.GlobalFields;
import gui.util.UiUtil;
import gui.viewerCells.ParamSetListCell;
import io.GlobalIO;
import io.XmlUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.FxMethod;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameters.FxParameter;
import util.ClipboardUtils;

public class ReadonlyMethodView implements ParameterView {

  /*
  This is just a popup to show the method of given sample to decide whether to load it.
   */

  private static final Logger LOGGER = LogManager.getLogger(ReadonlyMethodView.class);

  // Link fx-instances to the native ones (natives do not have a pointer to any FX-related object)
  private final HashMap<ParamSet, FxEntry<FxParamSet>> fxMap = new HashMap<>();

  private Scene parentSceneForDialogs;
  private final BorderPane methodBorderPane;
  private final ToolBar topToolbar = new ToolBar();
  private final VBox leftMethodMetaDataBox;

  // only ever have a copy of the original method here to avoid changing
  // the method reference in the sample
  private final Method copyOfMethod;

  // FX-instance of the current method ("originalMethod" is only needed for an isEqual() comparison
  private final FxMethod fxCopyOfMethod;
  private final ListView<FxEntry<FxParamSet>> subMethodsListView = new ListView<>();

  // FXInstance labels for the sub method view
  private final FxEntryFactory<FxParamSet> factory;

  //Try to remember to scroll position and focus - works!!!
  private final HashMap<FxParamSet, ListView<FxParameter<?>>> viewMap = new HashMap<>();

  public ReadonlyMethodView(Method method) {

    this.parentSceneForDialogs = SpTool3Main.getMainStage().getScene();

    this.copyOfMethod = new ListMethod(method);
    this.fxCopyOfMethod = copyOfMethod.getObservableInstance();

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

    // Box is filled in the set method call
    setMethod(copyOfMethod); // fills in the listviews

    // ############################################################################################
    methodBorderPane.setTop(topToolbar);

    Label methodLbl = new Label("Method");
    methodLbl.setStyle("-fx-font-weight: bold");

    Button saveAs = UiUtil.getToolbarBtn(
        "/img/saveAs.png",
        "Save as:\nSave current method as a new file.");
    saveAs.setOnAction(e -> executeSaveAs());

    topToolbar.getItems().addAll(methodLbl,
        new Separator(Orientation.VERTICAL),
        saveAs,
        new Separator(Orientation.VERTICAL)
    );

    // ############################################################################################

    MenuItem saveSubMethodAs = UiUtil
        .getImageMenuItem("Add to library", "/img/save.png");
    saveSubMethodAs.setOnAction(e -> {
      List<ParamSet> selSubMethods = getSelectedSets();
      if (!selSubMethods.isEmpty()) {

        // Load
        List<FxParamSet> fxSubMethods = XmlUtil
            .getSubMethodsFromFile(GlobalIO.makeSubMethodsFile());

        // Add
        List<ParamSet> subMethods = fxSubMethods.stream()
            .map(FxParamSet::getPlainSet)
            .collect(Collectors.toList());

        subMethods.addAll(selSubMethods);

        // Save
        Path subMethodPath = GlobalIO.makeSubMethodsFile();
        XmlUtil.writeSubMethodsToFile(subMethods, "Sub method collection", subMethodPath);
      }
    });

    subMethodsListView.getContextMenu().getItems().addAll(
        saveSubMethodAs,
        new SeparatorMenuItem()
    );

    //
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


  private boolean executeSaveAs() {
    boolean wasSaved = false;
    Path methods = SpTool3Main.getRunTime().getConfParams().getDefaultMethodPath();
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().addAll(new ExtensionFilter("Method files",
        "*" + GlobalIO.METHOD_EXTENSION));

    if (Files.isDirectory(methods)) {
      chooser.setInitialDirectory(methods.toFile());
      chooser.setInitialFileName(copyOfMethod.getLabelParam().getValue());
    } else {
      methods = GlobalIO.makeMethodsFolder();
      if (Files.isDirectory(methods)) {
        chooser.setInitialDirectory(methods.toFile());
        chooser.setInitialFileName(copyOfMethod.getLabelParam().getValue());
      }
    }
    //
    File returnedDirectory = chooser.showSaveDialog(parentSceneForDialogs.getWindow());
    // make sure the returned directory is not null (e.g. user aborts choice)
    if (returnedDirectory != null) {
      copyOfMethod.executeSaveAs(returnedDirectory.toPath(), true);
      // setMethod kills the selection of the submethod list view
      List<FxEntry<FxParamSet>> prevSel = new ArrayList<>(subMethodsListView.getSelectionModel()
          .getSelectedItems());
      // Forces all views with Date and FileName to refresh and also sets "originalMethod = currentMethod;" and so on
      reselectByObjectEqualityOfPlain(subMethodsListView, prevSel);
      wasSaved = true;
    }
    return wasSaved;
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

            // NO!!! DO NOT EVER DO THIS! THIS CAUSES A VERY NASTY BUG! Sporadically you won't be able to select the nodes anymore!
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
        // This whole viewer is read-only
        fxEntry.unwrap().setUneditable();
        fxSets.add(fxEntry);
      }
      this.subMethodsListView.getItems().addAll(fxSets);
    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    // cheap reselect
    subMethodsListView.getSelectionModel().selectFirst();
    // refresh the labels on the left hand side
    Label methodFileLbl;
    if (method.hasAssociatedFileOnDrive() && method.getAssociatedFile() != null) {
      methodFileLbl = new Label("File: " + method.getAssociatedFile().toFile().getName());
      // If too long.
      UiUtil.tooltip(methodFileLbl, methodFileLbl.getText()
          + "\n\nPath: " + method.getAssociatedFile().toFile().getPath());
    } else {
      methodFileLbl = new Label("File: -");
      UiUtil.tooltip(methodFileLbl, "No file is associated with this method");
    }
    leftMethodMetaDataBox.getChildren().clear();
    leftMethodMetaDataBox.getChildren().addAll(
        fxCopyOfMethod.getFxLabel().getValueNode(),

        fxCopyOfMethod.getFxComment().getLabelNode(),
        fxCopyOfMethod.getFxComment().getValueNode(),

        methodFileLbl,
        fxCopyOfMethod.getFxDate().getValueNode(),

        new Separator(Orientation.HORIZONTAL),

        subMethodsListView);
  }

  public BorderPane getBorderPane() {
    return methodBorderPane;
  }

  public void setParentSceneForDialogs(Scene parentSceneForDialogs) {
    if (parentSceneForDialogs != null) {
      this.parentSceneForDialogs = parentSceneForDialogs;
    }
  }
}
