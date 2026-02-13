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

package gui.dialog.caseImpl;

import gui.MethodView;
import gui.ParameterView;
import gui.dialog.DialogUtil;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.SimpleEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.mainImpl.ChooseMultipleFromListDialog;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import gui.viewerCells.ParamSetListCell;
import io.GlobalIO;
import io.XmlUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javax.annotation.Nullable;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.ParamSet;
import processing.parameters.FxParameter;

public class SubmethodEditor extends ChooseMultipleFromListDialog<FxParamSet> implements
    ParameterView {

  protected static final double PREF_WIDTH = 1200;
  protected static final double PREF_HEIGHT = 750;

  // Monitor change status
  private final ImageView statusImage;
  private final List<FxParamSet> setsSinceLastSave = new ArrayList<>();
  private final AtomicBoolean hasChanges = new AtomicBoolean(false);

  //Try to remember to scroll position and focus - works!!!
  private final HashMap<FxParamSet, ListView<FxParameter<?>>> viewMap = new HashMap<>();

  // Do not add() all the time to a Grid. This will pile up and also cause duplicate child error
  private final AnchorPane paramViewerPane = new AnchorPane();

  public SubmethodEditor(
      FxEntryFactory<FxParamSet> entryFactory,
      FxStageButton primaryButton,
      @Nullable String title,
      boolean closeOnDoubleClick) {
    super(entryFactory.create(readSubmethodsFromFile()),
        entryFactory,
        false,
        false,
        false,
        closeOnDoubleClick,
        false,
        primaryButton);

    // Customize appearance
    super.setTitle("Submethod editor."); // Override
    if (title != null) {
      super.setTopText(title);
    } else {
      super.setTopText("Edit and select submethods.");
    }

    // When the user is supposed to only select something
    if (closeOnDoubleClick) {
      listSearchView.getListView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    // Make Larger
    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    // put anchor pane for the parameter view in place
    super.contentGrid.add(paramViewerPane, 2, 1, 1, 1);

    // Monitor change status on the Toolbar
    statusImage = UiUtil.getLargeViewer("/img/isSaved.png");
    super.addToTopToolbar(statusImage);

    // Custom save button on the Toolbar
    Button saveBtn = super.showSaveButton();
    saveBtn.setOnAction(e -> executeSaveAndKeepWindow());
    /*
      Do not load these from the listAndSearchView. In that case, what is supposed to be a "backup"
      becomes a list of pointers to the same methods that are shown and thus changed...
      Instead, the original state must be represented by a newly loaded set of new instances.
     */

    setsSinceLastSave.addAll(readSubmethodsFromFile());
    setsSinceLastSave.forEach(fx -> fx.setController(this));

    // Create button
    Button newSubMethodButton = UiUtil.getToolbarBtn("/img/create.png", "Create new submethod");
    super.addToTopToolbar(newSubMethodButton);
    newSubMethodButton.setOnAction(e -> {

      List<FxParamSet> validSets = new ArrayList<>(AvailableParameterSets.getAllowedInstances());

      // Very important!
      validSets.forEach((fx -> fx.setController(this)));

      CreateMultipleSubMethodDialog dialog = new CreateMultipleSubMethodDialog(
          validSets, new SimpleEntryFactory<>());

      Optional<List<FxParamSet>> res = dialog.showAndWait();

      if (res.isPresent() && !res.get().isEmpty()) {
        List<FxParamSet> sets = res.get();
        listSearchView.addContent(sets);
      }
    });

    Button undo = UiUtil.getToolbarBtn(
        "/img/ctlZ.png",
        "Undo:\nUndo all changes and reset the library to its state"
            + "\nafter the last time that it has been saved.");
    super.addToTopToolbar(undo);

    undo.setOnAction(e -> {

      NotificationFactory.openYesNo("Reset library to previous state?", () -> {

        List<FxEntry<FxParamSet>> prevSel = new ArrayList<>(
            listSearchView.getListView().getSelectionModel().getSelectedItems());

        listSearchView.overrideContentEntry(entryFactory.create(readSubmethodsFromFile()));

        listSearchView.getListView().getSelectionModel().clearSelection();

        MethodView.reselectByID(listSearchView.getListView(), prevSel);

        updateParameterPane(listSearchView.getListView().getSelectionModel().getSelectedItem());
      });
    });

    // General context Menus
    MenuItem deleteMenu = DialogUtil.getDeleteSelectedItemsFromView(super.listSearchView);
    MenuItem asDefault = DialogUtil.getSetParameterSetAsDefaultMenuItem(this::getSelectedFxSets);
    MenuItem restoreDefault = DialogUtil.getRestoreParameterSetMenuItem(this::getSelectedFxSets);

    // Custom context Menus

    // ### ### ### ### ### ### ###
    MenuItem deleteDuplicateMenu = UiUtil.getImageMenuItem("Delete duplicates", "/img/delete.png");
    deleteDuplicateMenu.setOnAction(e ->
        NotificationFactory.openYesCancel("Search for duplicates in the selection and delete them?",
            () -> {
              List<FxEntry<FxParamSet>> toBeDeleted = new ArrayList<>();
              List<FxEntry<FxParamSet>> toBeKept = new ArrayList<>();

              List<FxEntry<FxParamSet>> all = new ArrayList<>(
                  listSearchView.getListView().getSelectionModel().getSelectedItems());

              for (FxEntry<FxParamSet> entry : all) {
                FxParamSet fxSet = entry.unwrap();
                if (fxSet != null) {
                  ParamSet set = fxSet.getPlainSet();

                  // Compare set against all others
                  for (FxEntry<FxParamSet> compareEntry : all) {
                    FxParamSet compareFxSet = compareEntry.unwrap();
                    if (compareFxSet != null) {
                      ParamSet compareSet = compareFxSet.getPlainSet();

                      // Compare but not with itself
                      if (set != compareSet && set.hasEqualParameters(compareSet)) {
                        toBeDeleted.add(entry);

                        // <0 if this is before the other
                        if (fxSet.getDate().compareTo(compareFxSet.getDate()) < 0) {
                          toBeKept.add(compareEntry);
                        } else if (fxSet.getDate().compareTo(compareFxSet.getDate()) > 0) {
                          toBeKept.add(entry);
                        }

                        // Same date? This is a bit more tricky as the condition is true on both directions.
                        if (fxSet.getDate().compareTo(compareFxSet.getDate()) == 0) {
                          if (!toBeKept.contains(entry) && !toBeKept.contains(compareEntry)) {
                            toBeKept.add(entry);
                          }
                        }
                      }
                    }
                  }
                }
              }
              // Remove from Storage List in Super Class
              toBeDeleted.removeAll(toBeKept);

              if (!toBeDeleted.isEmpty()) {
                StringBuilder message = new StringBuilder("This will delete these sub-methods:");
                toBeDeleted.forEach(
                    entry -> message.append("\n").append(entry.getCellLabelProperty().get()));
                NotificationFactory.openYesCancel(message.toString(),
                    () -> listSearchView.removeContent(toBeDeleted));
              }

              // Refresh ListView (based on the "AllOptions" list)
              listSearchView.filterList();
            }));

    // ### ### ### ### ### ### ###
    MenuItem cloneSet = UiUtil.getImageMenuItem("Clone", "/img/clone.png");
    cloneSet.setOnAction(e -> {
      List<FxParamSet> fxSets = getSelectedFxSets();
      for (FxParamSet fxSet : fxSets) {
        if (fxSet != null) {
          FxParamSet copy = fxSet.getPlainSet().getCopyWithNewDate().getObservableInstance();

          copy.setController(this);

          listSearchView.addContent(copy);
        }
      }
    });

    // Add the context menu items
    listSearchView.getListView().getContextMenu().getItems().addAll(
        cloneSet,
        new SeparatorMenuItem(),
        deleteMenu,
        deleteDuplicateMenu,
        new SeparatorMenuItem(),
        asDefault,
        restoreDefault
    );

    // Set listening Controller for each submethod
    listSearchView.getAllOptionsUnmodifiable().stream()
        .map(FxEntry::unwrap)
        .forEach(fxParamSet -> fxParamSet.setController(this));

    listSearchView.setCellFactory(l -> new ParamSetListCell());

    listSearchView.getListView().getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          updateParameterPane(newValue);
        });

    // When I edit the label in the grid on the right, I want the list view to change immediately:
    // https://stackoverflow.com/questions/28285507/is-there-a-way-to-bind-the-content-of-a-listproperty-in-javafx
    // https://stackoverflow.com/questions/31687642/callback-and-extractors-for-javafx-observablelist
    this.listSearchView.getListView().setItems(FXCollections.observableArrayList(
        new Callback<FxEntry<FxParamSet>, Observable[]>() {
          @Override
          public Observable[] call(FxEntry<FxParamSet> param) {
            return new Observable[]{param.getCellLabelProperty()};
          }
        }));

    // After "overriding" the setItems() method, we need to set the already existing items again.
    listSearchView.filterList();

    // ListView preferences
    listSearchView.getListView().getSelectionModel().selectFirst();

    // Initialize with content
    notifyItemChange();
  }


  // Is called from the ParamSet if parameters need to be reloaded
  @Override
  public void notifyItemChange() {
    FxEntry<FxParamSet> selSet = listSearchView.getListView().getSelectionModel()
        .getSelectedItem();
    updateParameterPane(selSet);
  }

  /*
  UPDATE: We need to call the indicator status b/c otherwise we do not see a value change there.

  UPDATE: We call notifyLabelChange() on FxParamSets when
  a) we set the current ParamSet state as default (b/c this updates the date!)
  b) the label LabelFxParameter triggers a focus or change listener
  If a listening fxEntry is set, this will cause this entry to set its label property again.
  The listview is linked to this property in the ListAndSearchView class.
  Otherwise: if we call notifyItemChange() for a label change,
  the ui rebuilds the parameterPane which causes the label field to put its caret position at the end.

  UPDATE: It is relevant if we allow the submethod list view to be editable...

  OLD:
  Not relevant for this class! Why?
  The method view itself shows a label, which requires refreshing the method view if the label is changed.
  As THIS class does not have such a label, this function does not have any target and does nothing.
  Instead, item change is the relevant listener for this class.
   */
  @Override
  public void notifyValueChange() {
    setChangeIndicatorStatus();
  }


  // Private helper methods
  @Nullable
  private FxParamSet getSelectedFxSet() {
    FxParamSet fxParamSet = null;
    FxEntry<FxParamSet> entry = listSearchView.getListView().getSelectionModel()
        .getSelectedItem();
    if (entry != null) {
      fxParamSet = entry.unwrap();
    }
    return fxParamSet;
  }

  private List<FxParamSet> getSelectedFxSets() {
    List<FxEntry<FxParamSet>> entries = listSearchView.getListView().getSelectionModel()
        .getSelectedItems();
    List<FxParamSet> fxParamSets = entries.stream()
        .filter(Objects::nonNull)
        .map(FxEntry::unwrap)
        .collect(Collectors.toList());
    return fxParamSets;
  }

  // Re-read from submethod file while respecting potential filters
  private static List<FxParamSet> readSubmethodsFromFile() {
    return XmlUtil.getSubMethodsFromFile(GlobalIO.makeSubMethodsFile());
  }

  private void updateParameterPane(FxEntry<FxParamSet> selSet) {
    if (selSet != null) {

      // just some caution about pile-up of the map (which should not happen as there are few FxSets)
      if (viewMap.size() > 1E5) {
        viewMap.clear();
      }

      ListView<FxParameter<?>> view;
      if (viewMap.containsKey(selSet.unwrap())) {
        view = viewMap.get(selSet.unwrap());
        view.getItems().clear();
        view.getItems().addAll(selSet.unwrap().getActiveFxParameters());
      } else {
        view = MethodView.createParamView(selSet.unwrap());
        viewMap.put(selSet.unwrap(), view);
      }

      paramViewerPane.getChildren().clear();
      paramViewerPane.getChildren().add(view);
      AnchorPane.setTopAnchor(view, 0d);
      AnchorPane.setRightAnchor(view, 0d);
      AnchorPane.setBottomAnchor(view, 0d);
      AnchorPane.setLeftAnchor(view, 0d);

      final ListView<FxEntry<FxParamSet>> submethodListView = super.listSearchView.getListView();
      distributeCurrentWidthToViews(submethodListView, view, super.getWidth());

      super.getDialogPane().widthProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue,
            Number newValue) {
          distributeCurrentWidthToViews(submethodListView, view, newValue.doubleValue());
        }
      });

    } else {
      paramViewerPane.getChildren().clear();
    }

    // After updating, call the method that checks for changes.
    setChangeIndicatorStatus();
  }

  // Show whether the current state has been changed or not.
  private void setChangeIndicatorStatus() {
    Image newImage;

    List<FxParamSet> submethodsOnDrive = setsSinceLastSave;

    List<FxParamSet> submethodsInDialog = listSearchView.getAllOptionsUnmodifiable().stream()
        .filter(Objects::nonNull)
        .map(FxEntry::unwrap)
        .collect(Collectors.toList());

    boolean noChange;

    if (submethodsInDialog.size() != submethodsOnDrive.size()) {
      noChange = false;
    } else {

      List<Boolean> booleans = new ArrayList<>();
      for (FxParamSet fxOnDrive : submethodsOnDrive) {
        ParamSet setOnDrive = fxOnDrive.getPlainSet();

        boolean aSetOnDriveCheck = false;
        for (FxParamSet fxInDialog : submethodsInDialog) {
          ParamSet setInDialog = fxInDialog.getPlainSet();
          if (setOnDrive.hasEqualParameters(setInDialog)) {
            aSetOnDriveCheck = true;
            break;
          }
        }
        booleans.add(aSetOnDriveCheck);
      }

      boolean driveHasPartnerInDialog = booleans.stream().allMatch(Boolean::booleanValue);

      //////////////////////////////////////
      booleans = new ArrayList<>();
      for (FxParamSet fxInDialog : submethodsInDialog) {
        ParamSet setInDialog = fxInDialog.getPlainSet();

        boolean aSetInDialogCheck = false;
        for (FxParamSet fxOnDrive : submethodsOnDrive) {
          ParamSet setOnDrive = fxOnDrive.getPlainSet();
          if (setOnDrive.hasEqualParameters(setInDialog)) {
            aSetInDialogCheck = true;
            break;
          }
        }
        booleans.add(aSetInDialogCheck);
      }

      boolean dialogHasPartnerOnDrive = booleans.stream().allMatch(Boolean::booleanValue);

      noChange = driveHasPartnerInDialog && dialogHasPartnerOnDrive;
    }
    if (noChange) {
      newImage = UiUtil.getImage("/img/isSaved.png");
      hasChanges.set(false);
    } else {
      newImage = UiUtil.getImage("/img/isNotSaved.png");
      hasChanges.set(true);
    }
    statusImage.setImage(newImage);
  }


  /*
 On a GridPane / Dialog, we need to specify the prefWidth.
 Normally, we simply put the viewer on an anchor pane and let the UI find its way.
 Also, to keep the submethod list wide enough, we need to specify some minimum width.
 */
  private void distributeCurrentWidthToViews(ListView<FxEntry<FxParamSet>> submethodListView,
      ListView<FxParameter<?>> parameterList, double totalWidth) {

    double methodListWidth = 0.4 * totalWidth;
    double parameterListWidth = 0.6 * totalWidth;

    // Also, to keep the submethod list wide enough, we need to specify some minimum width.
    if (submethodListView != null) {
      submethodListView.setPrefWidth(methodListWidth);
    }

    if (parameterList != null) {
      parameterList.setPrefWidth(parameterListWidth);
    }
  }


  // Cancel button or little "x"
  @Override
  public void closeAndCancelChanges() {
    if (hasChanges.get()) {
      // Prompt user to consider saving.
      NotificationFactory.openYesNo(
          "There are unsaved changes. Do you want to close the window?",
          super::closeAndCancelChanges);
    } else {
      super.closeAndCancelChanges();
    }
  }

  @Override
  public void saveAndSetResults() {
    /*
    Peculiarity of this class:
    We do not allow selecting a submethod with changes but not saving them to the library.
    This is just a decision I made to make sure that the user is always aware of when things are
    saved and an effort to just force things to be in sync.
    However, it is a bit annoying to get a "save????" notification for every selection,
    especially if there are no changes at all. Hence, check here  and do not call the save diaglog
    if nothing needs to be changed.
     */
    if (hasChanges.get()) {
      executeSave();
    } else {
      // Call the method that cals setResult(items) (NOT setResult(null)!) and killScene()
      super.closeAndContinue();
    }
  }


  @Override
  public void executeSave() {
    NotificationFactory.openYesNo(
        "Save changes to submethods and continue? This is irreversible.",
            () -> {
              Path subMethodPath = GlobalIO.makeSubMethodsFile();
              XmlUtil.writeSubMethodEntriesToFile(
                  new ArrayList<>(super.listSearchView.getListView().getItems()),
                  "Sub method collections",
                  subMethodPath);

              // Update the list against which we compare to identify changes
              setsSinceLastSave.clear();
              setsSinceLastSave.addAll(readSubmethodsFromFile());
              setsSinceLastSave.forEach(fx -> fx.setController(this));
              setChangeIndicatorStatus();

              // Call the method that cals setResult(items) (NOT setResult(null)!) and killScene()
              super.closeAndContinue();
            });
  }

  private void executeSaveAndKeepWindow() {
    /*
    Do not check
      if (hasChanges.get())
    since we want to reassure the user that they can safe at any time :-)
     */
    NotificationFactory.openYesNo("Save changes to submethods? This is irreversible.",
        () -> {
          Path subMethodPath = GlobalIO.makeSubMethodsFile();
          XmlUtil.writeSubMethodEntriesToFile(
              new ArrayList<>(super.listSearchView.getListView().getItems()),
              "Sub method collections",
              subMethodPath);

          // Update the list against which we compare to identify changes
          setsSinceLastSave.clear();
          setsSinceLastSave.addAll(readSubmethodsFromFile());
          setsSinceLastSave.forEach(fx -> fx.setController(this));
          setChangeIndicatorStatus();
        });
  }


}
