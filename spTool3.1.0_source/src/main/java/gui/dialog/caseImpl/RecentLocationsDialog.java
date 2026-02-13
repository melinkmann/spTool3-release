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
import io.FxVisitedFile;
import io.VisitedFile;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import processing.objectSerials.ImportDefaults;

public class RecentLocationsDialog extends ChooseSingleFromListDialog<FxVisitedFile> {

  private final ImportDefaults importDefaults;

  public RecentLocationsDialog(ImportDefaults importDefaults,
      FxEntryFactory<FxVisitedFile> entryFactory) {

    super(entryFactory.create(importDefaults.getCopyOfPreviousLocations().stream()
            .map(VisitedFile::getObservableInstance)
            .collect(Collectors.toList())),
        entryFactory,
        false,
        true,
        false,
        FxStageButton.SELECT);

    this.importDefaults = importDefaults;

    // Customize
    super.setTopText("Select one folder from the list.");

    // Save Button
    Button saveBtn = showSaveButton();
    saveBtn.setOnAction(e -> executeSaveAndKeepWindow());

    Button clearBtn = super.showClearButton();
    clearBtn.setOnAction(e -> {
      listSearchView.clear();
      NotificationFactory.openOK("Cleared entries. Click save to keep changes.", this.getOwner());
    });


    // Context menu
    listSearchView.addLikeDislikeMenu();

    // Custom Menu that requires special casting and access
    MenuItem deleteMenu = UiUtil.getImageMenuItem("Remove", "/img/delete.png");
    listSearchView.getListView().getContextMenu().getItems().add(deleteMenu);

    deleteMenu.setOnAction(e -> NotificationFactory.openYesCancel("Delete Item.",
        () -> {
          List<FxEntry<FxVisitedFile>> toBeDeleted = new ArrayList<>(
              listSearchView.getListView().getSelectionModel().getSelectedItems());

          // Remove from Storage List in Super Class
          listSearchView.removeContent(toBeDeleted);

          // Remove from Storage List in Storage Organizing Class
          importDefaults.remPreviousLocationVisitedFile(toBeDeleted.stream()
              .map(FxEntry::unwrap)
              .map(FxVisitedFile::getPlainVisitedFile)
              .collect(Collectors.toList()));
        }));
  }

  @Override
  public void saveAndSetResults() {
    executeSaveAndKeepWindow();
    super.saveAndSetResults();
  }

  @Override
  public void closeAndContinue() {
    executeSaveAndKeepWindow();
    super.closeAndContinue();
  }

  @Override
  public void executeSave() {
    executeSaveAndKeepWindow();
  }

  private void executeSaveAndKeepWindow() {
    // Replace the options in the Storage class (ImportDefaults) with the current options.
    importDefaults.overridePreviousLocations(listSearchView.getAllOptionsUnmodifiable()
        .stream()
        .map(FxEntry::unwrap)
        .map(FxVisitedFile::getPlainVisitedFile)
        .collect(Collectors.toList()));

    // STORE!
    SpTool3Main.getRunTime().storeImportDefaults();
  }
}
