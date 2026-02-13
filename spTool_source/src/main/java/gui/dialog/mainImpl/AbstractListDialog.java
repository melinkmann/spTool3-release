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

package gui.dialog.mainImpl;

import gui.Hotkeyable;
import gui.StageFactory;
import gui.dialog.DialogUtil;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxStage;
import gui.dialog.FxStageButton;
import gui.dialog.ListContainer;
import gui.listAndSearch.ListAndSearchView;
import gui.util.UiUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

public abstract class AbstractListDialog<T> extends Dialog<List<T>> implements Hotkeyable, FxStage {

  protected static final double PREF_WIDTH_LIST = 1500;
  protected static final double PREF_HEIGHT_LIST = 750;

  protected static final double PREF_WIDTH = 1000;
  protected static final double PREF_HEIGHT = 750;

  protected final ListAndSearchView<T> listSearchView;

  protected final BorderPane mainBorderPane;
  protected final GridPane contentGrid;

  private final ToolBar topToolbar = new ToolBar();
  private final Button saveBtn;
  private final Button clearBtn;

  protected final ButtonBar buttonBar;
  protected final FxStageButton primaryButtonType;
  protected final Button primaryButton;
  protected final Button cancelButton;

  // Decides if the selection of the listview is returned completely or filtered by the isSelected property
  private final boolean useSelectionFromSelectable;
  private final boolean checkAlternateSelection;

  public AbstractListDialog(ListContainer<T> container,
      FxEntryFactory<T> entryFactory,
      SelectionMode selectionMode,
      boolean useSelectionFromSelectable,
      boolean checkAlternateSelection,
      boolean doubleClickSelect,
      boolean closeOnDoubleClick,
      boolean isEditable,
      FxStageButton primaryButtonType) {

    this(container.getList(entryFactory),
        entryFactory,
        selectionMode,
        useSelectionFromSelectable,
        checkAlternateSelection,
        doubleClickSelect,
        closeOnDoubleClick,
        isEditable,
        primaryButtonType);
  }

  public AbstractListDialog(List<FxEntry<T>> optionList,
      FxEntryFactory<T> entryFactory,
      SelectionMode selectionMode,
      boolean useSelectionFromSelectable,
      boolean checkAlternateSelection,
      boolean doubleClickSelect,
      boolean closeOnDoubleClick,
      boolean isEditable,
      FxStageButton primaryButtonType) {

    // Functional

    // Keep List of All Entries to enable searching through the list
    this.listSearchView = new ListAndSearchView<>(optionList,
        entryFactory,
        selectionMode,
        useSelectionFromSelectable,
        false,
        isEditable);

    this.mainBorderPane = new BorderPane();

    // Buttons
    this.buttonBar = new ButtonBar();
    this.primaryButton = primaryButtonType.getBold(this);
    this.cancelButton = new Button("Cancel");
    this.buttonBar.getButtons().addAll(primaryButton, cancelButton);
    this.cancelButton.setOnAction(e -> closeAndCancelChanges());
    // this.cancelButton.setDefaultButton(true);

    // Button at the top to save if possible
    this.saveBtn = UiUtil.getToolbarBtn(
        "/img/save.png",
        "Save changes to the list and options");

    this.clearBtn = UiUtil.getToolbarBtn(
        "/img/clear.png",
        "Clear list");

    // Grid
    this.contentGrid = new GridPane();

    this.useSelectionFromSelectable = useSelectionFromSelectable;
    this.checkAlternateSelection = checkAlternateSelection;

    //
    // Behaviour on double click
    listSearchView.getListView().setOnMouseClicked(e -> {
      if (e.getClickCount() == 2) {
        if (!isEditable && (doubleClickSelect && closeOnDoubleClick)) {
          // Select All
          listSearchView.getListView().getSelectionModel().selectAll();
          // Then close
          primaryButton.fire(); // Button knows how to close (save, cancel, continue, ...)
        } else if (!isEditable && doubleClickSelect) {
          // Select All
          listSearchView.getListView().getSelectionModel().selectAll();
        } else if (!isEditable && closeOnDoubleClick) {
          // Just Close
          primaryButton.fire(); // Button knows how to close (save, cancel, continue, ...)
        }
      }
    });

    this.primaryButtonType = primaryButtonType;

    // Style
    makeStyles();

  }

  protected void makeStyles() {
    DialogUtil.makeEscapeClosable(this);

    //Important to actually fill the grid that gets a smaller size (see below)
    listSearchView.getListView().setPrefSize(PREF_WIDTH_LIST, PREF_HEIGHT_LIST);

    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    dialogPane.getButtonTypes().clear();

    dialogPane.setHeader(topToolbar);
    dialogPane.setPadding(Insets.EMPTY);

    dialogPane.setContent(UiUtil.putOnAnchorWithInsets(mainBorderPane));
    mainBorderPane.setCenter(UiUtil.putOnAnchorWithInsets(contentGrid));
    mainBorderPane.setBottom(buttonBar);

    activateHotkeys(dialogPane.getScene());

    // Close on Windows close symbol
    // Else: behaviour on exit via "x" is not defined
    dialogPane.getScene().getWindow().setOnCloseRequest(event -> {
      event.consume();
      closeAndCancelChanges();
    });

    // Grid
    this.contentGrid.setHgap(10);
    this.contentGrid.setVgap(10);
    this.contentGrid.setMaxWidth(Double.MAX_VALUE);
    this.contentGrid.setAlignment(Pos.CENTER_LEFT);

    // Dialog
    //-- pane style
    setTitle("Select from list.");
    Stage dialogStage = (Stage) getDialogPane().getScene().getWindow();
    dialogStage.getIcons().add(UiUtil.getImage("/img/load.png"));
    dialogStage.setResizable(true);
  }

  //////////////////////////////////////////////////////////////////////////////////

  /**
   * Returns the currently selected item in the dialog.
   */
  public final List<T> getSelectedItems() {
    List<T> result = new ArrayList<>();
    boolean useAlternate = checkAlternateSelection;
    // It is a bit annoying to always mark the files, hence,
    // why not allow "pass normal selection" when no selectable selection was made
    if (useSelectionFromSelectable) {
      // Use selected options only
      result = listSearchView.getListView().getItems().stream()
          .filter(FxEntry::isSelected)
          .map(FxEntry::unwrap)
          .collect(Collectors.toList());
    }else {
      useAlternate = true;
      result = new ArrayList<>(); // make sure it is empty
    }

    // If we allow alternate, check if sth was found, if not, return alternate
    if (useAlternate && result.isEmpty()) {
      // Use selection from list view
      result = listSearchView.getListView().getSelectionModel().getSelectedItems().stream()
          .map(FxEntry::unwrap)
          .collect(Collectors.toList());

      // Still annoying having to mark sth if you just wanted to drop sth, hence, even continue at not selection
      if (result.isEmpty()) {
        result.addAll(listSearchView.getListView().getItems().stream()
        .map(FxEntry::unwrap)
        .collect(Collectors.toList()));
      }
    }
    return result;
  }

  public ListAndSearchView<T> getListSearchView() {
    return listSearchView;
  }

  //////////////////////////////////////////////////////////////////////////////////


  public void setCellFactory(Callback<ListView<FxEntry<T>>, ListCell<FxEntry<T>>> factory) {
    listSearchView.setCellFactory(factory);
  }

  protected void prepareGrid() {
    contentGrid.getChildren().clear();
  }

  protected void setTopText(String topText) {
    Label top = new Label(topText);
    top.setStyle("-fx-font-weight: bold");
    top.setFont(new Font(15));
    top.setMinHeight(30);
    mainBorderPane.setTop(top);
    BorderPane.setAlignment(top, Pos.CENTER);
  }

  /**
   * @return the save button in order to set a lambda to the onAction() listener.
   */
  public Button showSaveButton() {
    topToolbar.getItems().add(saveBtn);
    return saveBtn;
  }

  public Button showClearButton() {
    topToolbar.getItems().add(clearBtn);
    return clearBtn;
  }


  protected void addToTopToolbar(Node node) {
    topToolbar.getItems().add(node);
  }

  //////////////////////////////////////////////////////////////////////////////////

  // FxStage methods
  @Override
  public void closeAndKeepCurrentState() {
    super.setResult(getSelectedItems());
    killScene();
  }

  // Cancel button or little "x"
  @Override
  public void closeAndCancelChanges() {
    super.setResult(null);
    killScene();
  }

  @Override
  public void saveAndSetResults() {
    super.setResult(getSelectedItems());
    executeSave();
  }

  @Override
  public void closeAndContinue() {
    super.setResult(getSelectedItems());
    killScene();
    // Idea: If, e.g., the calling class has a second step after THIS dialog,
    // call it by overriding this method in the respective dialog instance.
  }

  //////////////////////////////////////////////////////////////////////////////////


  public void executeSave() {
    // Do nothing: Override this in sub classes that can be saved.

    /*
     Note: whether to kill the scene should be decided here. Why?
     If user cancels the Save?-Dialog, we want to return to dialog and NOT close it.
     */
    killScene();
  }

  /////////////////////////////////////////


  protected void killScene() {
    Scene scene = getDialogPane().getScene();
    if (scene != null) {
      Stage dialogStage = (Stage) scene.getWindow();
      dialogStage.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////

  @Override
  public void activateHotkeys(Scene scene) {
    scene.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
      // Save on control s
      if (StageFactory.KEY_CTL_S.match(event)) {
        executeSave();

        // If "continue" like Button is present and control enter is hit, call "close and continue"
      } else if (StageFactory.KEY_CTL_ENTER.match(event)
          && (primaryButtonType.equals(FxStageButton.CONTINUE)
          || primaryButtonType.equals(FxStageButton.SELECT)
          || primaryButtonType.equals(FxStageButton.RUN))) {
        closeAndContinue();
      }
    });
  }

}
