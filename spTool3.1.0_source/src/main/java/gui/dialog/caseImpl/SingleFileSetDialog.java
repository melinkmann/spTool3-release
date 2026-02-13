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
import gui.dialog.mainImpl.ChooseSingleFromListDialog;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import io.FileSet;
import io.FxFileSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import processing.objectSerials.ImportDefaults;

public class SingleFileSetDialog extends ChooseSingleFromListDialog<FxFileSet> {

  private final ImportDefaults importDefaults;

  public SingleFileSetDialog(ImportDefaults importDefaults,
      FxEntryFactory<FxFileSet> factory) {

    /*
    Note: We do not want to immediately affect the import defaults object.
    Why? We only want to keep changes after the change button is hit. When closing otherwise,
    discard changes.
    Thus only pass a copy of the FileSets and later override these in the ImportDefaults.
     */

    super(factory.create(
        importDefaults.getCopyOfPreviousFileSets().stream().
            map(FileSet::getObservableInstance)
            .collect(Collectors.toList())),
        factory,
        false,
        false,
        true,
        FxStageButton.SELECT);

    this.importDefaults = importDefaults;

    // Custom save button on the Toolbar
    Button saveBtn = super.showSaveButton();
    saveBtn.setOnAction(e -> executeSaveAndKeepWindow());

    Button clearBtn = super.showClearButton();
    clearBtn.setOnAction(e -> {
      listSearchView.clear();
      NotificationFactory.openOK("Cleared entries. Click save to keep changes.", this.getOwner());
    });

    // Customize
    super.setTopText("Select one set of previously imported files from the list.");
    listSearchView.addLikeDislikeMenu();

    // Custom Menu that requires special casting and access
    MenuItem deleteMenu = UiUtil.getImageMenuItem("Remove", "/img/delete.png");
    listSearchView.getListView().getContextMenu().getItems().add(deleteMenu);

    deleteMenu.setOnAction(e -> NotificationFactory.openYesCancel("Delete Item.",
        () -> {
          List<FxEntry<FxFileSet>> toBeDeleted = new ArrayList<>(
              listSearchView.getListView().getSelectionModel().getSelectedItems());

          // Remove from Storage List in Super Class
          listSearchView.removeContent(toBeDeleted);

          // Refresh ListView (happens in super class based on the "AllOptions" list)
          listSearchView.filterList();
        }));

  }


  @Override
  public void executeSave() {
    executeSaveAndKeepWindow();
  }


  private void executeSaveAndKeepWindow() {
    // Replace the options in the Storage class (ImportDefaults) with the current options.
    importDefaults.overridePreviousSets(listSearchView.getAllOptionsUnmodifiable()
        .stream()
        .map(FxEntry::unwrap)
        .map(FxFileSet::getPlainFileSet)
        .collect(Collectors.toList()));

    // STORE!
    SpTool3Main.getRunTime().storeImportDefaults();
  }

  @Override
  public void closeAndContinue() {
    executeSaveAndKeepWindow();
    super.closeAndContinue();
  }
}
