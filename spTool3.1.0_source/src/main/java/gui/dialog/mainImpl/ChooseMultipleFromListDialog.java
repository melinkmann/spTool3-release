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

import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class ChooseMultipleFromListDialog<T> extends AbstractListDialog<T> {

  public ChooseMultipleFromListDialog(List<FxEntry<T>> optionList,
      FxEntryFactory<T> factory,
      boolean isEditable, FxStageButton fxStageButton) {

    this(optionList,
        factory,
        true,
        false,
        false,
        false,
        isEditable,
        fxStageButton);
  }

  public ChooseMultipleFromListDialog(List<FxEntry<T>> optionList,
      FxEntryFactory<T> factory,
      boolean useSelectionFromSelectable,
      boolean checkAlternateSelection,
      boolean doubleClickSelect,
      boolean closeOnDoubleClick,
      boolean isEditable,
      FxStageButton fxStageButton) {
    this(optionList,
        new ArrayList<>(),
        factory,
        useSelectionFromSelectable,
        checkAlternateSelection,
        doubleClickSelect,
        closeOnDoubleClick,
        isEditable,
        fxStageButton);
  }

  public ChooseMultipleFromListDialog(List<FxEntry<T>> optionList,
      List<FxEntry<T>> selectedInstances,
      FxEntryFactory<T> factory,
      boolean useSelectionFromSelectable,
      boolean checkAlternateSelection,
      boolean doubleClickSelect,
      boolean closeOnDoubleClick,
      boolean isEditable,
      FxStageButton fxStageButton) {

    super(optionList,
        factory,
        SelectionMode.MULTIPLE,
        useSelectionFromSelectable,
        checkAlternateSelection,
        doubleClickSelect,
        closeOnDoubleClick,
        isEditable,
        fxStageButton);

    // initialize with selection
    if (!selectedInstances.isEmpty()) {
      for (FxEntry<T> selectedInstance : selectedInstances) {
        T t = selectedInstance.unwrap();
        for (FxEntry<T> item : listSearchView.getListView().getItems()) {
          T itemT = item.unwrap();
          if (t.equals(itemT)) {
            listSearchView.getListView().getSelectionModel().select(item);
            if (useSelectionFromSelectable) {
              item.setSelected(true);
            }
          }
        }
      }
    }

    // Context Custom Menu
    if (useSelectionFromSelectable) {
      listSearchView.addSelectDeselectMenus();
    }
    listSearchView.addViewContentOfItemMenu();

    //-- pane style
    super.setTopText("Select one or multiple items.");

    updateGrid();

    super.listSearchView.getSearchFld().requestFocus();
  }

  /**************************************************************************
   *
   * Public API
   *
   **************************************************************************/

  // ********************************************************************** Private Implementation
  protected void updateGrid() {
    super.prepareGrid();

    contentGrid.add(new Label("Search for text"), 0, 0);
    contentGrid.add(listSearchView.getSearchFld(), 1, 0);
    GridPane.setFillWidth(listSearchView.getSearchFld(), true);

    contentGrid.add(listSearchView.getListView(), 0, 1, 2, 1);
    GridPane.setFillWidth(listSearchView.getListView(), true);

    contentGrid.getColumnConstraints().forEach(c -> c.setHgrow(Priority.ALWAYS));
    Platform.runLater(listSearchView.getListView()::requestFocus);
  }


}
