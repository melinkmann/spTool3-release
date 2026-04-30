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

import com.opencsv.bean.CsvRecurse;
import core.SpTool3Main;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.MethodDateFileEntryFactory;
import gui.dialog.FxEntryFactory.ParamSetWithDateEntryFactory;
import gui.dialog.FxEntryFactory.SimpleEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.caseImpl.CreateMultipleSubMethodDialog;
import gui.dialog.caseImpl.MethodWizardUtils;
import gui.dialog.caseImpl.SubmethodViewer;
import gui.dialog.mainImpl.ChooseMultipleFromListDialog;
import gui.dialog.mainImpl.ChooseSingleFromListDialog;
import gui.dialog.notification.NotificationFactory;
import gui.util.GlobalFields;
import gui.util.UiUtil;
import gui.viewerCells.ParamSetListCell;
import io.GlobalIO;
import io.PathUtil;
import io.XmlUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.SearchThresholdOption;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxMethod;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.*;
import processing.parameters.ButtonFxParameter;
import processing.parameters.FxParameter;
import util.ClipboardUtils;
import visualizer.styles.Colors;
import visualizer.styles.SpV2Colors;

public class MethodView implements ParameterView {


  private static final Logger LOGGER = LogManager.getLogger(MethodView.class);

  // Link fx-instances to the native ones (natives do not have a pointer to any FX-related object)
  private final HashMap<ParamSet, FxEntry<FxParamSet>> fxMap = new HashMap<>();

  private final BorderPane methodBorderPane;

  private final ToolBar topToolbar = new ToolBar();
  private final HBox subMethodUpDownBox;
  private final VBox leftMethodMetaDataBox;
  private final ImageView statusImage = UiUtil.getLargeViewer("/img/isSaved.png");

  // Check if the user needs to save
  private Method currentMethod = new ListMethod();
  // copy to revert to in case of cancel
  private Method originalMethod = new ListMethod(currentMethod);

  // FX-instance of the current method ("originalMethod" is only needed for an isEqual() comparison
  private FxMethod fxCurrentMethod = currentMethod.getObservableInstance();
  private final ListView<FxEntry<FxParamSet>> subMethodsListView = new ListView<>();

  // FXInstance labels for the sub method view
  private final FxEntryFactory<FxParamSet> factory;

  //Try to remember to scroll position and focus - works!!!
  private final HashMap<FxParamSet, ListView<FxParameter<?>>> viewMap = new HashMap<>();


  public MethodView() {

    this.methodBorderPane = new BorderPane();
    //  Border around the Pane?
    //this.methodBorderPane.setStyle("-fx-border-color: #999999; -fx-border-width: 1.75;");

    // FXInstance labels for the sub method view
    this.factory = new SimpleEntryFactory<>();

    // Functionalities:

    // ### Sub method list view
    // When I edit the label in the grid on the right, I want the list view to change immediately:
    // https://stackoverflow.com/questions/28285507/is-there-a-way-to-bind-the-content-of-a-listproperty-in-javafx
    // https://stackoverflow.com/questions/31687642/callback-and-extractors-for-javafx-observablelist
    this.subMethodsListView.setItems(FXCollections.observableArrayList(new Callback<FxEntry<FxParamSet>,
        Observable[]>() {
      @Override
      public Observable[] call(FxEntry<FxParamSet> param) {
        return new Observable[]{param.getCellLabelProperty()};
      }
    }));

    //
    this.subMethodsListView.setCellFactory(l -> new ParamSetListCell());
    ClipboardUtils.installCopyHandler(subMethodsListView);
    subMethodsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    subMethodsListView.setPrefWidth(GlobalFields.METHOD_LIST_WIDTH);
    subMethodsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue,
                                                                               newValue) -> notifyItemChange());

    // From the original methodEditor popup INITIALIZE

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

      SubmethodViewer dialog = new SubmethodViewer(new ParamSetWithDateEntryFactory(),
          AvailableParameterSets.getAllowedOptions(), FxStageButton.SELECT, null, true);

      Optional<List<FxParamSet>> result = dialog.showAndWait();

      if (result != null && result.isPresent()) {
        List<FxParamSet> results = result.get();
        if (!results.isEmpty()) {
          results.forEach(fx -> {
            FxEntry<FxParamSet> entry = getFromMapOrCreate(fx.getPlainSet());
            currentMethod.addSet(entry.unwrap().getPlainSet());
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

        currentMethod.addSets(sets);
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

    // Box is filled in the set method call
    setMethod(new ListMethod()); // fills in the listviews

    // ############################################################################################
    methodBorderPane.setTop(topToolbar);

    Label methodLbl = new Label("Method");
    methodLbl.setStyle("-fx-font-weight: bold");

    Button saveAs = UiUtil.getToolbarBtn("/img/saveAs.png", "Save as:\nSave current method as a new file.");
    saveAs.setOnAction(e -> executeSaveAs());

    Button saveAsProject = UiUtil.getToolbarBtn("/img/saveInProject.png", "Save as:\nSave current method as" +
        " a new file in the project folder.");
    saveAsProject.setOnAction(e -> {
      Path projectPath = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
      executeSaveAs(projectPath);
    });

    Button deleteMethod = UiUtil.getToolbarBtn("/img/delete.png", "Delete method file:\nMove current method" +
        " file to the recycle folder.");
    deleteMethod.setOnAction(e -> callDeleteDialog());

    Button save = UiUtil.getToolbarBtn("/img/save.png", "Save:\nOverwrite the current method and write to " +
        "the same file.");
    save.setOnAction(e -> callSaveDialog());

    Button undo = UiUtil.getToolbarBtn("/img/ctlZ.png", "Undo:\nUndo all changes and reset the method to " +
        "its state" + "\nafter the last time that it has been saved.");
    undo.setOnAction(e -> {

      NotificationFactory.openYesNo("Reset method to previous version?", () -> {
        List<FxEntry<FxParamSet>> prevSel =
            new ArrayList<>(subMethodsListView.getSelectionModel().getSelectedItems());

        setMethod(originalMethod);
        reselectByID(subMethodsListView, prevSel);
      });
    });

    Button openMethodBtn = UiUtil.getToolbarBtn("/img/searchList.png", "Select method:\nSelect method from " +
        "the list of existing methods");

    openMethodBtn.setOnAction(e -> {
      boolean proceedAndDiscard = callProceedAndDiscardDialog();
      if (proceedAndDiscard) {
        Path methodPath = SpTool3Main.getRunTime().getConfParams().getDefaultMethodPath();
        if (Files.isDirectory(methodPath)) {
          List<Path> methodPaths = new ArrayList<>(PathUtil.listFiles(methodPath, true));
          List<Method> methods = new ArrayList<>();
          methodPaths.forEach(p -> methods.add(XmlUtil.readMethodFromXml(p)));

          FxEntryFactory<FxMethod> factory = new MethodDateFileEntryFactory();

          ChooseMultipleFromListDialog<FxMethod> dialog =
              new ChooseMultipleFromListDialog<>(factory.create(methods.stream().map(Method::getObservableInstance).collect(Collectors.toList())), factory, false, false, false, true, false, FxStageButton.CONTINUE);

          // Custom buttons
          dialog.getListSearchView().addLikeDislikeMenu();

          MenuItem deleteMenu = UiUtil.getImageMenuItem("Delete", "/img/delete.png");
          deleteMenu.setOnAction(btnEvent -> {
            List<FxEntry<FxMethod>> selected =
                new ArrayList<>(dialog.getListSearchView().getListView().getSelectionModel().getSelectedItems());

            if (!selected.isEmpty()) {
              NotificationFactory.openYesCancel("Move method files to recycle folder?" + " This is " +
                  "irreversible.", () -> {
                for (FxEntry<FxMethod> entry : selected) {
                  Method method = entry.unwrap().getPlainMethod();
                  if (method.getAssociatedFile() != null && method.hasAssociatedFileOnDrive()) {
                    Path recyclePath = GlobalIO.makeMethodsRecycleFolder();
                    if (Files.isDirectory(recyclePath)) {
                      File file = method.getAssociatedFile().toFile();
                      String fileName = file.getName();
                      Path newFile = recyclePath.resolve(fileName);
                      try {
                        Files.move(file.toPath(), newFile, StandardCopyOption.REPLACE_EXISTING);
                        method.setAssociatedPath(newFile);
                      } catch (IOException ioException) {
                        LOGGER.warn(ExceptionUtils.getStackTrace(ioException));
                      }
                    }
                  }
                }
                // Re-load to show new list
                if (Files.isDirectory(methodPath)) {
                  List<Path> paths = new ArrayList<>(PathUtil.listFiles(methodPath, false));
                  List<FxMethod> newMethodList =
                      paths.stream().map(XmlUtil::readMethodFromXml).map(Method::getObservableInstance).collect(Collectors.toList());
                  dialog.getListSearchView().overrideContent(newMethodList);
                }
              });
            }
          });
          dialog.getListSearchView().addMenuItem(deleteMenu);

          Button saveBtn = dialog.showSaveButton();
          saveBtn.setOnAction(evt -> {
            List<FxEntry<FxMethod>> selected =
                dialog.getListSearchView().getListView().getSelectionModel().getSelectedItems();
            selected.stream().map(FxEntry::unwrap).map(FxMethod::getPlainMethod).forEach(Method::executeOverridingSave);
          });

          Optional<List<FxMethod>> result = dialog.showAndWait();
          if (result != null && result.isPresent()) {
            List<FxMethod> results = result.get();
            if (!results.isEmpty()) {
              setMethod(results.get(0).getPlainMethod());
            }
          }
        }
      }
    });
//
    Button lastSavedMethodBtn = UiUtil.getToolbarBtn("/img/load.png", "Load default method from " +
        "configuration.");

    lastSavedMethodBtn.setOnAction(e -> {
      boolean proceedAndDiscard = callProceedAndDiscardDialog();
      if (proceedAndDiscard) {
        Path methodFilePath = SpTool3Main.getRunTime().getConfParams().getCurrentMethodFile();
        File asFile = methodFilePath.toFile();
        if (asFile.exists() && asFile.isFile() && asFile.canRead()) {
          Method m = XmlUtil.readMethodFromXml(methodFilePath);
          if (m != null) {
            setMethod(m);
          }
        }
      }
    });

    Button openMethodFromProjectBtn = UiUtil.getToolbarBtn("/img/loadFromProject.png", "Select " +
        "method:\nSelect method from the list of existing methods in the project folder");

    openMethodFromProjectBtn.setOnAction(e -> {
      boolean proceedAndDiscard = callProceedAndDiscardDialog();
      if (proceedAndDiscard) {
        Path methodPath = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
        if (Files.isDirectory(methodPath)) {
          List<Path> methodPaths = new ArrayList<>(PathUtil.listFiles(methodPath, true));
          methodPaths = PathUtil.retainType(methodPaths, GlobalIO.METHOD_EXTENSION);

          List<Method> methods = new ArrayList<>();
          methodPaths.forEach(p -> methods.add(XmlUtil.readMethodFromXml(p)));
          // here, prevent failed files (which pass empty method in reader) from passing empty methods.
          // methods.removeIf(m->m.getSets().isEmpty());

          FxEntryFactory<FxMethod> factory = new MethodDateFileEntryFactory();

          ChooseSingleFromListDialog<FxMethod> dialog =
              new ChooseSingleFromListDialog<>(factory.create(methods.stream().map(Method::getObservableInstance).collect(Collectors.toList())), factory, false, true, false, FxStageButton.CONTINUE);

          // Custom buttons
          dialog.getListSearchView().addLikeDislikeMenu();

          Button saveBtn = dialog.showSaveButton();
          saveBtn.setOnAction(evt -> {
            List<FxEntry<FxMethod>> selected =
                dialog.getListSearchView().getListView().getSelectionModel().getSelectedItems();
            selected.stream().map(FxEntry::unwrap).map(FxMethod::getPlainMethod).forEach(Method::executeOverridingSave);
          });

          Optional<List<FxMethod>> result = dialog.showAndWait();
          if (result != null && result.isPresent()) {
            List<FxMethod> results = result.get();
            if (!results.isEmpty()) {
              setMethod(results.get(0).getPlainMethod());
            }
          }
        }
      }
    });

    Button browseMethodBtn = UiUtil.getToolbarBtn("/img/searchFile.png", "Browse method:\nBrowse for a " +
        "method file on the drive.");
    browseMethodBtn.setOnAction(e -> {
      boolean proceedAndDiscard = callProceedAndDiscardDialog();
      if (proceedAndDiscard) {
        Path methods = SpTool3Main.getRunTime().getConfParams().getDefaultMethodPath();
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(new ExtensionFilter("Method files",
            "*" + GlobalIO.METHOD_EXTENSION));

        if (Files.isDirectory(methods)) {
          chooser.setInitialDirectory(methods.toFile());
        } else {
          methods = GlobalIO.makeMethodsFolder();
          if (Files.isDirectory(methods)) {
            chooser.setInitialDirectory(methods.toFile());
          }
        }
        //
        File returnedDirectory = chooser.showOpenDialog(SpTool3Main.getMainStage());
        // make sure thj returned directory is not null (e.g. user aborts choice)
        if (returnedDirectory != null) {
          Method method = XmlUtil.readMethodFromXml(returnedDirectory.toPath());
          setMethod(method);
        }
      }
    });

    Button newMethodBtn = UiUtil.getToolbarBtn("/img/create.png", "Create method:\nCreate a new method.");
    newMethodBtn.setOnAction(e -> {

      // Don't annoy with 2 ask screens:

      // (1) check if methods are equal and ask about that:
      if (!currentMethod.hasEqualParameters(originalMethod)) {

        // If the the methods are different, ask if user wants to proceed.
        AtomicBoolean doNotGoBackAndSave = NotificationFactory.openYesNo("There are unsaved changes in your" +
            " method." + "\nDo you want to proceed and discard all changes?");

        if (doNotGoBackAndSave.get()) {
          Method newMethod = new ListMethod();
          setMethod(newMethod);
        }
      } else {
        // (2) If the methods are not equivalent, ask if new instance is desired.
        AtomicBoolean replace = NotificationFactory.openYesNo("Do you want to create a new method?" +
            "\nThis will replace the current method with a new one.");
        if (replace.get()) {
          Method newMethod = new ListMethod();
          setMethod(newMethod);
        }
      }


    });

    topToolbar.getItems().addAll(methodLbl, new Separator(Orientation.VERTICAL), statusImage,
        new Separator(Orientation.VERTICAL), save, saveAs, UiUtil.createSeparator(20),
        undo, lastSavedMethodBtn,
        new Separator(Orientation.VERTICAL), deleteMethod, UiUtil.createSeparator(20),
        openMethodBtn, browseMethodBtn, new Separator(Orientation.VERTICAL),
        newMethodBtn, UiUtil.createSeparator(20),
        saveAsProject, openMethodFromProjectBtn);

    // ############################################################################################

    MenuItem saveSubMethodAs = UiUtil.getImageMenuItem("Add to library", "/img/save.png");
    saveSubMethodAs.setOnAction(e -> {
      List<ParamSet> selSubMethods = getSelectedSets();
      if (!selSubMethods.isEmpty()) {

        // Load
        List<FxParamSet> fxSubMethods = XmlUtil.getSubMethodsFromFile(GlobalIO.makeSubMethodsFile());

        // Add
        List<ParamSet> subMethods =
            fxSubMethods.stream().map(FxParamSet::getPlainSet).collect(Collectors.toList());

        subMethods.addAll(selSubMethods);

        // Save
        Path subMethodPath = GlobalIO.makeSubMethodsFile();
        XmlUtil.writeSubMethodsToFile(subMethods, "Sub method collection", subMethodPath);
      }
    });

    MenuItem removeSubMethod = UiUtil.getImageMenuItem("Remove", "/img/remove.png");
    removeSubMethod.setOnAction(e -> {
      List<FxEntry<FxParamSet>> subMethods = getSelectedEntries();
      if (!subMethods.isEmpty()) {
        NotificationFactory.openYesCancel("Remove sub-methods from method? This is irreversible.", () -> {
          currentMethod.removeSets(subMethods.stream().map(FxEntry::unwrap).filter(Predicate.not(Objects::isNull)).map(FxParamSet::getPlainSet).collect(Collectors.toList()));
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
        currentMethod.addSets(sets);
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

    MenuItem asDefault = UiUtil.getImageMenuItem("Override defaults in sub-methods", "/img/overridechange" +
        ".png");
    asDefault.setOnAction(e -> {
      List<FxEntry<FxParamSet>> entries = getSelectedEntries();
      if (!entries.isEmpty()) {
        NotificationFactory.openYesCancel("Override default with current settings. This is irreversible.",
            () -> {
              for (FxEntry<FxParamSet> entry : entries) {
                FxParamSet fxSet = entry.unwrap();
                ParamSet set = fxSet.getPlainSet();
                set.setCurrentValuesAsDefault();
                fxSet.notifyItemChange(); // The date changes and we need to refresh the UI!
              }
            });
      }
    });

    MenuItem restoreDefault = UiUtil.getImageMenuItem("Reset sub-methods", "/img/ignorechange.png");
    restoreDefault.setOnAction(e -> {
      List<FxEntry<FxParamSet>> entries = getSelectedEntries();
      if (!entries.isEmpty()) {
        NotificationFactory.openYesCancel("Restore default state and override current settings. This is " +
            "irreversible.", () -> {
          for (FxEntry<FxParamSet> entry : entries) {
            FxParamSet set = entry.unwrap();
            set.getPlainSet().resetToDefault();
            set.notifyItemChange();
          }
        });
      }
    });

    subMethodsListView.getContextMenu().getItems().addAll(saveSubMethodAs, new SeparatorMenuItem(),
        cloneSubMethod, new SeparatorMenuItem(), removeSubMethod, new SeparatorMenuItem(), asDefault,
        restoreDefault);

    //
    notifyItemChange();
  }

  private boolean callSaveDialog() {
    AtomicBoolean wasSaved = new AtomicBoolean(false);
    NotificationFactory.openYesCancel("Save method? This is irreversible.", () -> {
      wasSaved.set(executeSave());
    });
    return wasSaved.get();
  }

  public void callDeleteDialog() {
    if (currentMethod.hasAssociatedFileOnDrive()) {
      NotificationFactory.openYesCancel("Move method file to recycle folder?" + " This is irreversible.",
          () -> {
            if (currentMethod.getAssociatedFile() != null) {
              Path recyclePath = GlobalIO.makeMethodsRecycleFolder();
              if (Files.isDirectory(recyclePath)) {
                File file = currentMethod.getAssociatedFile().toFile();
                String fileName = file.getName();
                Path newFile = recyclePath.resolve(fileName);
                try {
                  Files.move(file.toPath(), newFile, StandardCopyOption.REPLACE_EXISTING);
                  currentMethod.setAssociatedPath(newFile);
                } catch (IOException ioException) {
                  LOGGER.warn(ExceptionUtils.getStackTrace(ioException));
                }
              }

            }
          });
    } else {
      NotificationFactory.openInfo("The method is not associated with a file " + "that exists on your hard " +
          "drive.");
    }
  }

  private boolean callProceedAndDiscardDialog() {
    // Initialize as true. If equal check is equal, we can proceed.
    AtomicBoolean proceedAndIgnore = new AtomicBoolean(true);
    // If the the methods are different, ask if user wants to proceed.
    if (!currentMethod.hasEqualParameters(originalMethod)) {
      AtomicBoolean result = NotificationFactory.openYesNo("There are unsaved changes in your method." +
          "\nDo you want to proceed and discard unsaved changes?");
      proceedAndIgnore.set(result.get());
    }
    return proceedAndIgnore.get();
  }


  // Getters as helplers to get the unwrapped entries.
  private List<FxParamSet> getSelectedFxSets() {
    List<FxParamSet> selSets =
        subMethodsListView.getSelectionModel().getSelectedItems().stream()
            .filter(Objects::nonNull)
            .map(FxEntry::unwrap)
            .filter(Predicate.not(Objects::isNull))
            .collect(Collectors.toList());
    return selSets;
  }

  private List<ParamSet> getSelectedSets() {
    List<ParamSet> selSets =
        subMethodsListView.getSelectionModel().getSelectedItems().stream()
            .filter(Objects::nonNull)
            .map(FxEntry::unwrap)
            .filter(Predicate.not(Objects::isNull))
            .map(FxParamSet::getPlainSet)
            .collect(Collectors.toList());
    return selSets;
  }

  private List<FxEntry<FxParamSet>> getSelectedEntries() {
    List<FxEntry<FxParamSet>> selEntries =
        subMethodsListView.getSelectionModel().getSelectedItems().stream().filter(Predicate.not(Objects::isNull)).collect(Collectors.toList());
    return selEntries;
  }

  public boolean executeSave() {
    boolean wasSaved = false;
    if (currentMethod.hasAssociatedFileOnDrive()) {
      currentMethod.executeOverridingSave();
      wasSaved = true;
      // setMethod kills the selection of the submethod list view
      List<FxEntry<FxParamSet>> prevSel =
          new ArrayList<>(subMethodsListView.getSelectionModel().getSelectedItems());
      // Forces all views with Date and FileName to refresh and also sets "originalMethod = currentMethod;"
      // and so on
      setMethod(currentMethod);
      reselectByObjectEqualityOfPlain(subMethodsListView, prevSel);
    } else {
      wasSaved = executeSaveAs();
    }
    return wasSaved;
  }

  private boolean executeSaveAs() {
    Path methodPath = SpTool3Main.getRunTime().getConfParams().getDefaultMethodPath();
    boolean wasSaved = executeSaveAs(methodPath);
    return wasSaved;
  }

  private boolean executeSaveAs(Path methodPath) {
    boolean wasSaved = false;
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().addAll(new ExtensionFilter("Method files",
        "*" + GlobalIO.METHOD_EXTENSION));

    if (Files.isDirectory(methodPath)) {
      chooser.setInitialDirectory(methodPath.toFile());
      chooser.setInitialFileName(currentMethod.getLabelParam().getValue());
    } else {
      PathUtil.createDir(methodPath);
      if (Files.isDirectory(methodPath)) {
        chooser.setInitialDirectory(methodPath.toFile());
        chooser.setInitialFileName(currentMethod.getLabelParam().getValue());
      }
    }
    //
    File returnedDirectory = chooser.showSaveDialog(SpTool3Main.getMainStage());
    // make sure the returned directory is not null (e.g. user aborts choice)
    if (returnedDirectory != null) {
      currentMethod.executeSaveAs(returnedDirectory.toPath(), true);
      // setMethod kills the selection of the submethod list view
      List<FxEntry<FxParamSet>> prevSel =
          new ArrayList<>(subMethodsListView.getSelectionModel().getSelectedItems());
      // Forces all views with Date and FileName to refresh and also sets "originalMethod = currentMethod;"
      // and so on
      setMethod(currentMethod);
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
      // test: narrowing the method view with a dummy pane on a splitPane
      SplitPane splitPane = new SplitPane();
      Pane emptyPane = new Pane();   // stays empty
      splitPane.getItems().addAll(view, emptyPane);
      double sliderPos =
          SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters().getMethodDivider();
      splitPane.setDividerPositions(sliderPos); // left
      if (!splitPane.getDividers().isEmpty()) {
        splitPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
          SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters().setMethodDivider(newValue.doubleValue());
        });
      }

      // continue
      methodBorderPane.setCenter(UiUtil.putOnAnchorWithInsets(splitPane, 10));
    } else {
      methodBorderPane.setCenter(new AnchorPane()); // just something empty
    }

    // Add quick Button if method is totally blank
    if (currentMethod.getSets().isEmpty()) {
      Button createBasicWorkingMethod = new Button("Quick start new method");
      // Font: bold, size 18
      createBasicWorkingMethod.setFont(Font.font(
          "System", FontWeight.BOLD, 18
      ));
      createBasicWorkingMethod.setPrefSize(200, 100);

      createBasicWorkingMethod.setOnAction(e -> {
        MethodWizardUtils.show(this);
      });
      methodBorderPane.setCenter(UiUtil.putOnAnchorWithoutInsets(createBasicWorkingMethod)); // just
      // something empty
    }
    // Update the "has the method been changed" sign
    notifyValueChange();
  }

  @Override
  public void notifyValueChange() {
    // Show whether the current method has been changed or not.
    Image newImage;
    if (currentMethod.hasEqualParameters(originalMethod)) {
      newImage = UiUtil.getImage("/img/isSaved.png");
    } else {
      newImage = UiUtil.getImage("/img/isNotSaved.png");
    }
    statusImage.setImage(newImage);
  }

  public static ListView<FxParameter<?>> createParamView(FxParamSet subMethod) {

    ObservableList<FxParameter<?>> obsList = FXCollections.observableArrayList();
    ListView<FxParameter<?>> paramView = new ListView<>(obsList);
    // paramView.setStyle("-fx-border-color: #332288; -fx-border-width: 1.0;");
    UiUtil.formatListView(paramView);

    paramView.setPrefHeight(40); //200

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

          // Cell-owned highlight indicator
          private final Rectangle highlightRect = new Rectangle(35, 12);

          {
            highlightRect.setFill(SpV2Colors.BLUE.getFX());
          }


          @Override
          public void updateItem(FxParameter<?> fxPar, boolean empty) {
            super.updateItem(fxPar, empty);

            // Make BG always gray and text black
            if (fxPar instanceof ButtonFxParameter) {
              UiUtil.formatListCellGrayBold(this);
            } else {
              UiUtil.formatListCellGray(this);
            }

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

              HBox viewerBox = fxPar.getViewerBox(paramView);
              // Enforce invariant on every update
              viewerBox.getChildren().removeIf(n -> n instanceof Rectangle);
              if (fxPar.isHighlightFX()) {
                viewerBox.getChildren().add(1, highlightRect);
              }

              setText(null);
              setGraphic(viewerBox);

              UiUtil.requestShrinkScrollBar(paramView);

              /* ============================
               Context menu (toggle)
               ============================ */

              ContextMenu menu = getContextMenu();
              MenuItem toggleItem;

              if (menu == null || menu.getItems().isEmpty()) {
                toggleItem = new MenuItem();
                menu = new ContextMenu(toggleItem);
                setContextMenu(menu);
              } else {
                toggleItem = menu.getItems().get(0);
              }

              // Update label each time (cell reuse!)
              toggleItem.setText(
                  fxPar.isHighlightFX() ? "Remove Highlight" : "Add Highlight"
              );

              // Replace handler safely (no accumulation)
              toggleItem.setOnAction(e -> {
                FxParameter<?> current = getItem();
                if (current != null) {
                  current.setHighlightFX(!current.isHighlightFX());

                  // FIX: update UI immediately
                  if (current.isHighlightFX()) {
                    if (!viewerBox.getChildren().contains(highlightRect)) {
                      viewerBox.getChildren().add(1, highlightRect);
                    }
                  } else {
                    viewerBox.getChildren().remove(highlightRect);
                  }
                }
              });
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

  public void setMethod(Method method) {
    // Method
    this.currentMethod = method;
    this.fxCurrentMethod = method.getObservableInstance();
    this.fxCurrentMethod.setMethodViewer(this); // to update about label/comment change
    this.originalMethod = new ListMethod(method); // copy to revert to in case of cancel

    // Update the UI. Check for null, because MainWindowController and MethodView have dummy instance at
    // initialization.
    if (SpTool3Main.getRunTime() != null && SpTool3Main.getRunTime().getMainWindowCtl() != null) {
      SpTool3Main.getRunTime().getMainWindowCtl().notifyMethodChange(currentMethod);
    }

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
    Label methodFileLbl;
    if (method.hasAssociatedFileOnDrive() && method.getAssociatedFile() != null) {
      methodFileLbl = new Label("File: " + method.getAssociatedFile().toFile().getName());
      // If too long.
      UiUtil.tooltip(methodFileLbl,
          methodFileLbl.getText() + "\n\nPath: " + method.getAssociatedFile().toFile().getPath());
    } else {
      methodFileLbl = new Label("File: -");
      UiUtil.tooltip(methodFileLbl, "No file is associated with this method");
    }
    leftMethodMetaDataBox.getChildren().clear();
    leftMethodMetaDataBox.getChildren().addAll(fxCurrentMethod.getFxLabel().getValueNode(),

        fxCurrentMethod.getFxComment().getLabelNode(), fxCurrentMethod.getFxComment().getValueNode(),

        methodFileLbl, fxCurrentMethod.getFxDate().getValueNode(),

        new Separator(Orientation.HORIZONTAL), subMethodUpDownBox,

        subMethodsListView);
  }

  public Method getCurrentMethod() {
    return currentMethod;
  }

  /**
   * In some cases, when e.g. the current method is somehow replaced, reselection cannot be done by
   * object comparison. The objects will be different.
   */
  public static void reselectBySimilarity(ListView<FxEntry<FxParamSet>> subMethodsListView,
                                          List<FxEntry<FxParamSet>> prevSel) {

   /*
    Note that the original method has COPIES (but not the same instances)!
    Also, the instances are not equivalent, if the selected set is the one that
    was changed. Hence, first try to check if an equivalent set is present.
    Else, select those of the same class (which is the best guess).
   */

    // This method is called AFTER changes were set via setMethod(), which refreshes the listview.
    List<FxEntry<FxParamSet>> newSets = new ArrayList<>(subMethodsListView.getItems());

    List<FxEntry<FxParamSet>> equivalentSel = new ArrayList<>();
    List<FxEntry<FxParamSet>> sameClass = new ArrayList<>();

    for (FxEntry<FxParamSet> fxEntry : prevSel) {
      FxParamSet fxSet = fxEntry.unwrap();
      for (FxEntry<FxParamSet> newSetEntry : newSets) {
        FxParamSet newSet = newSetEntry.unwrap();
        if (fxSet.isEqualPlainObject(newSet)) {
          equivalentSel.add(newSetEntry);
        }
        // Note: getClass().equals is always true as all sets are FxParamSetImpl.
        if (newSet.getPlainSet().getEnum().equals(fxSet.getPlainSet().getEnum())) {
          sameClass.add(newSetEntry);
        }
      }
    }
    /*
    We must clear the selection and call selectFirst() since the previous call to setMethod()
    includes a call to selectFirst(), which is fulfilled when called here above.
    */
    subMethodsListView.getSelectionModel().clearSelection();
    if (!equivalentSel.isEmpty()) {
      reselectByObjectEqualityOfPlain(subMethodsListView, equivalentSel);
    } else if (!sameClass.isEmpty()) {
      reselectByObjectEqualityOfPlain(subMethodsListView, sameClass);
    } else {
      subMethodsListView.getSelectionModel().selectFirst();
    }
  }

  public static void reselectByID(ListView<FxEntry<FxParamSet>> subMethodsListView,
                                  List<FxEntry<FxParamSet>> prevSel) {

    // This method is called AFTER changes were set via setMethod(), which refreshes the listview.
    List<FxEntry<FxParamSet>> newSets = new ArrayList<>(subMethodsListView.getItems());

    List<FxEntry<FxParamSet>> equivalentSel = new ArrayList<>();

    for (FxEntry<FxParamSet> prevSelFxEntry : prevSel) {
      FxParamSet prevSelFxSet = prevSelFxEntry.unwrap();
      for (FxEntry<FxParamSet> newSetEntry : newSets) {
        FxParamSet newSet = newSetEntry.unwrap();
        if (prevSelFxSet.getPlainSet().isEqualID(newSet.getPlainSet())) {
          equivalentSel.add(newSetEntry);
        }
      }
    }
    subMethodsListView.getSelectionModel().clearSelection();
    if (equivalentSel.isEmpty()) {
      // at least select something
      subMethodsListView.getSelectionModel().selectFirst();
    } else {
      equivalentSel.forEach(entry -> subMethodsListView.getSelectionModel().select(entry));
    }
  }

  /**
   * Reselects by literally looking if an FxInstance hast the same PlainParameterSet by calling POJO
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

  private void moveDown(ListView<FxEntry<FxParamSet>> view, List<FxEntry<FxParamSet>> moving) {
    // Do not fire for listener for every change
    List<FxEntry<FxParamSet>> setsInView = new ArrayList<>(view.getItems());
    // Change order
    moveDown(setsInView, moving);
    // Set to ListView
    view.getItems().clear();
    view.getItems().addAll(setsInView);
    // Set to Method
    currentMethod.clearSets();
    currentMethod.addSets(setsInView.stream().map(FxEntry::unwrap).map(FxParamSet::getPlainSet).collect(Collectors.toList()));
  }

  private void moveUp(ListView<FxEntry<FxParamSet>> view, List<FxEntry<FxParamSet>> moving) {
    // Do not fire for listener for every change
    List<FxEntry<FxParamSet>> setsInView = new ArrayList<>(view.getItems());
    // Change order
    moveUp(setsInView, moving);
    // Set to ListView
    view.getItems().clear();
    view.getItems().addAll(setsInView);
    // Set to Method
    currentMethod.clearSets();
    currentMethod.addSets(setsInView.stream().map(FxEntry::unwrap).map(FxParamSet::getPlainSet).collect(Collectors.toList()));
  }

  public static <T> void moveDown(List<T> list, List<T> moving) {
    /*
     For downward movement, we need to start at the bottom of the moving lis, i.e., reverse it.
     Otherwise it would go:
     A B c d E F (with moving = c, d)
     A B d c E F : c is first entry in moving and overtakes d
     A B c d E F : now d is the next entry in moving and overtakes c
     */

    // New instance to avoid backpropagation of the reversal
    moving = new ArrayList<>(moving);
    Collections.reverse(moving);
    for (T t : moving) {
      if (t != null && list.contains(t)) {
        int idx = list.indexOf(t);
        if (idx == list.size() - 1 && idx > -1) {
          // T t is at end
        } else {
          Collections.swap(list, idx, idx + 1);
        }
      }
    }
  }

  public static <T> void moveUp(List<T> list, List<T> moving) {
    /*
     For upward movement, things are easier!
     A B c d E F (with moving = c, d)
     A c B d E F : c is first entry in moving and overtakes B
     A c d B E F : now d is the next entry in moving and overtakes B
     */
    for (T t : moving) {
      if (t != null && list.contains(t)) {
        int idx = list.indexOf(t);
        if (idx < 1) {
          // T t is at beginning or not in the list
        } else {
          Collections.swap(list, idx, idx - 1);
        }
      }
    }
  }

  public BorderPane getBorderPane() {
    return methodBorderPane;
  }


}
