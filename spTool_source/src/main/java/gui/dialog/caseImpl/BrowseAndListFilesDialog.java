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

import core.SpTool3Main;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.mainImpl.AbstractListDialog;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import io.FileSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.objectSerials.ImportDefaults;
import util.Util;

public class BrowseAndListFilesDialog extends AbstractListDialog<Path> {

  private static final Logger LOGGER = LogManager.getLogger(BrowseAndListFilesDialog.class);

  // Additional fields to manage file selection
  private final Button listFilesBtn;
  private final TextField currentDirFld;
  private final CheckBox limitTypeBox;
  private final TextField typeFld;
  private final CheckBox browseSubdirBox;
  private final Button clearBtn;

  private final ImportDefaults importDefaults;

  public BrowseAndListFilesDialog(ImportDefaults importDefaults,
                                  FxEntryFactory<Path> factory) {

    super(new ArrayList<>(),
        factory,
        SelectionMode.MULTIPLE,
        true,
        true,
        true,
        false,
        false,
        FxStageButton.CONTINUE);

    this.importDefaults = importDefaults;

    listSearchView.addSelectAllMenu();
    listSearchView.addSelectDeselectMenus();
    listSearchView.addViewContentOfItemMenu();

    final DialogPane dialogPane = getDialogPane();

    // Additional fields to manage file selection
    listFilesBtn = new Button("List Files");
    listFilesBtn.setStyle("-fx-font-weight: bold");
    currentDirFld = new TextField(importDefaults.getCurrentDir().toString());
    limitTypeBox = new CheckBox("Limit file type");
    limitTypeBox.setSelected(importDefaults.isLimitIsSelected());
    typeFld = new TextField(importDefaults.getLimitType());
    browseSubdirBox = new CheckBox("Include subdirectories");
    browseSubdirBox.setSelected(importDefaults.isBrowseSubdirectoryIsSelected());
    this.clearBtn = UiUtil.getToolbarBtn("/img/clear.png", "Clear list");

    // save changes
    limitTypeBox.setOnAction(e -> {
      importDefaults.setLimitIsSelected(limitTypeBox.isSelected());
    });

    PauseTransition limitTypePause = new PauseTransition(Duration.seconds(0.5));
    AtomicReference<String> pendingNewTypeLimitation = new AtomicReference<>("");
    limitTypePause.setOnFinished(event -> importDefaults.setLimitType(pendingNewTypeLimitation.get()));

    limitTypeBox.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        limitTypePause.stop();
        pendingNewTypeLimitation.set(newValue);
        limitTypePause.playFromStart();
      }
    });

    browseSubdirBox.setOnAction(e -> {
      importDefaults.setBrowseSubdirectoryIsSelected(browseSubdirBox.isSelected());
    });

    clearBtn.setOnAction(e -> {
      listSearchView.clear();
    });

    // make field browsable
    MenuItem menuBrowse = UiUtil.getImageMenuItem("Browse", "/img/searchFile.png");

    menuBrowse.setOnAction(e -> {
      // DirectoryChooser: open dialogue
      DirectoryChooser chooser = new DirectoryChooser();
      try {
        Path currentValue = Path.of(currentDirFld.getText());
        if (Files.isDirectory(currentValue)) {
          chooser.setInitialDirectory(currentValue.toFile());
        } else if (Files.isDirectory(currentValue.getParent())) {
          chooser.setInitialDirectory(currentValue.getParent().toFile());
        } else if (Files.isDirectory(importDefaults.getCurrentDir())) {
          chooser.setInitialDirectory(importDefaults.getCurrentDir().toFile());
        }

        File returnedDirectory = chooser.showDialog(dialogPane.getScene().getWindow());
        // make sure the returned directory is not null (e.g. user aborts choice)
        if (returnedDirectory != null && returnedDirectory.isDirectory()) {
          currentDirFld.setText(returnedDirectory.getAbsolutePath());
          importDefaults.setCurrentDir(returnedDirectory.toPath());
        }
      } catch (Exception ex) {
        NotificationFactory.openError(ex);
      }
    });

    currentDirFld.setContextMenu(new ContextMenu());
    currentDirFld.getContextMenu().getItems().add(menuBrowse);

    // Change of Field should also affect the storage
    PauseTransition currentDirPause = new PauseTransition(Duration.seconds(0.5));
    AtomicReference<String> pendingOldDir = new AtomicReference<>("");
    AtomicReference<String> pendingNewDir = new AtomicReference<>("");
    currentDirPause.setOnFinished(event -> {
      Path newPath = Paths.get(pendingNewDir.get());
      if (Files.isDirectory(newPath)) {
        importDefaults.setCurrentDir(newPath);
      } else if (!pendingNewDir.get().equals(pendingOldDir.get())) {
        currentDirFld.setText(importDefaults.getCurrentDir().toString());
        LOGGER.error("No such directory: " + pendingNewDir.get());
      }
    });
    currentDirFld.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        currentDirPause.stop();
        pendingOldDir.set(oldValue);
        pendingNewDir.set(newValue);
        currentDirPause.playFromStart();
      }
    });

    // List files functionality
    listFilesBtn.setOnAction(e -> {
      List<Path> recursivePaths = new ArrayList<>();
      int levels;
      if (browseSubdirBox.isSelected()) {
        levels = Integer.MAX_VALUE;
      } else {
        levels = 1;
      }

      //https://stackoverflow.com/questions/2056221/recursively-list-files-in-java
      try {
        Files.find(importDefaults.getCurrentDir(),
                levels,
                (filePath, fileAttr) -> fileAttr.isRegularFile())
            .forEach(recursivePaths::add);
      } catch (IOException ioException) {
        LOGGER.error(ExceptionUtils.getStackTrace(ioException));
      }

      List<Path> filteredPaths = new ArrayList<>();
      if (limitTypeBox.isSelected()) {
        String typeWithoutDot = typeFld.getText();
        for (Path path : recursivePaths) {
          String stringValue = path.getFileName().toString();
          String type = stringValue.substring(stringValue.lastIndexOf(".") + 1);
          if (type.equals(typeWithoutDot)) {
            filteredPaths.add(path);
          }
        }
      } else {
        filteredPaths.addAll(recursivePaths);
      }

      Util.windowsSortFile(filteredPaths);

      // Translate to Selectable Instances
      List<FxEntry<Path>> optionList = new ArrayList<>();
      filteredPaths.forEach(t -> optionList.add(factory.create(t)));

      // Override old content
      listSearchView.overrideContentEntry(optionList);

      // Store latest changes
      importDefaults.addPreviousLocation(importDefaults.getCurrentDir());
    });

    // MAKE DROPPABLE
    listSearchView.getListView().setOnDragOver(new EventHandler<DragEvent>() {
      public void handle(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
          event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
      }
    });

    listSearchView.getListView().setOnDragDropped((DragEvent event) -> {
      Dragboard db = event.getDragboard();

      if (db.hasFiles()) {
        List<File> filesFromClipboard = db.getFiles();

        // Collect the paths
        List<Path> recursivePaths = new ArrayList<>();

        for (File fileFromClipboard : filesFromClipboard) {
          // Check if Dir --> then list files.
          if (fileFromClipboard.isDirectory()) {
            importDefaults.addPreviousLocation(fileFromClipboard.toPath());

            // Check how deep to look
            int levels;
            if (browseSubdirBox.isSelected()) {
              levels = Integer.MAX_VALUE;
            } else {
              levels = 1;
            }

            // And look into the folder structure.
            try {
              Files.find(fileFromClipboard.toPath(),
                      levels,
                      (filePath, fileAttr) -> fileAttr.isRegularFile())
                  .forEach(recursivePaths::add);
            } catch (IOException ioException) {
              ioException.printStackTrace();
            }

            // Check if File --> then list it.
          } else if (fileFromClipboard.isFile() && fileFromClipboard.canRead()) {
            recursivePaths.add(fileFromClipboard.toPath());
          }
        }

        // Filter by file type
        List<Path> filteredPaths = new ArrayList<>();
        if (limitTypeBox.isSelected()) {
          String typeWithoutDot = typeFld.getText();
          for (Path path : recursivePaths) {
            String stringValue = path.getFileName().toString();
            String type = stringValue.substring(stringValue.lastIndexOf(".") + 1);
            if (type.equals(typeWithoutDot)) {
              filteredPaths.add(path);
            }
          }
        } else {
          filteredPaths.addAll(recursivePaths);
        }

        // Sort
        // Quasi "append"
        filteredPaths.addAll(listSearchView.getListView().getItems().stream()
            .map(FxEntry::unwrap)
            .collect(Collectors.toList()));

        // Sort
        Util.windowsSortFile(filteredPaths);

        // Translate to Selectable Instances
        List<FxEntry<Path>> optionList = new ArrayList<>();
        filteredPaths.forEach(t -> optionList.add(factory.create(t)));

        // Override old content
        listSearchView.overrideContentEntry(optionList);
        // TODO: Check here if this is the right way (create new, override and so on)

        event.setDropCompleted(true);
      }
      event.consume();
    });

    //-- pane style
    super.setTopText("Select files to continue.");

    updateGrid();
  }

  /**************************************************************************
   *
   * Public API
   *
   **************************************************************************/

  // ********************************************************************** Private Implementation
  protected void updateGrid() {
    /*
    setColumnIndex [0,...]  setRowIndex [0,...] setColumnSpan [1,...]  setRowSpan [1,...]
     */
    super.prepareGrid();
    contentGrid.add(new Label("Current directory"), 0, 0);
    contentGrid.add(currentDirFld, 1, 0);
    GridPane.setFillWidth(currentDirFld, true);

    HBox optionsBox = new HBox(5);
    optionsBox.setAlignment(Pos.CENTER_LEFT);

    optionsBox.getChildren().addAll(
        listFilesBtn,
        new Separator(Orientation.VERTICAL), limitTypeBox, typeFld,
        new Separator(Orientation.VERTICAL), browseSubdirBox,
        new Separator(Orientation.VERTICAL), clearBtn
    );

    contentGrid.add(optionsBox, 1, 1);

    contentGrid.add(new Label("Search for text"), 0, 2);
    contentGrid.add(listSearchView.getSearchFld(), 1, 2);
    GridPane.setFillWidth(listSearchView.getSearchFld(), true);

    contentGrid.add(listSearchView.getViewAndSizePane(), 0, 3, 2, 1);
    GridPane.setFillWidth(listSearchView.getViewAndSizePane(), true);

    contentGrid.getColumnConstraints().forEach(c -> c.setHgrow(Priority.ALWAYS));
    Platform.runLater(listSearchView.getListView()::requestFocus);

    //contentGrid.setGridLinesVisible(true);
  }

  @Override
  public void closeAndKeepCurrentState() {
    if (!getSelectedItems().isEmpty()) {
      importDefaults.addPreviousFileSet(new FileSet(getSelectedItems()));
    }
    // at the end: write (even w/o isEmpty() check as default directory should be stored
    SpTool3Main.getRunTime().storeImportDefaults();
    super.closeAndKeepCurrentState();
  }

  @Override
  public void closeAndCancelChanges() {
    super.closeAndCancelChanges();
  }

  @Override
  public void saveAndSetResults() {
    if (!getSelectedItems().isEmpty()) {
      importDefaults.addPreviousFileSet(new FileSet(getSelectedItems()));
    }
    // at the end: write (even w/o isEmpty() check as default directory should be stored
    SpTool3Main.getRunTime().storeImportDefaults();
    super.saveAndSetResults();
  }

  @Override
  public void closeAndContinue() {
    if (!getSelectedItems().isEmpty()) {
      importDefaults.addPreviousFileSet(new FileSet(getSelectedItems()));
    }
    // at the end: write (even w/o isEmpty() check as default directory should be stored
    SpTool3Main.getRunTime().storeImportDefaults();
    super.closeAndContinue();
  }

  // helpers to manage pause behavior

}
