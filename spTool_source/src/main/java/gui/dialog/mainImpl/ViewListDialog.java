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

import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.ListContainer;
import gui.util.UiUtil;
import javafx.application.Platform;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class ViewListDialog<T> extends AbstractListDialog<T> {

  // Let the view window appear on top of the old window, i.e., a bit smaller
  private static final double PREF_WIDTH = AbstractListDialog.PREF_WIDTH - 100;

  public ViewListDialog(ListContainer<T> container, FxEntryFactory<T> factory) {
    super(container,
        factory,
        SelectionMode.MULTIPLE,
        false,
        false,
        false,
        false,
        false,
        FxStageButton.CLOSE);

    //-- pane style
    setTitle("View List");
    Stage dialogStage = (Stage) getDialogPane().getScene().getWindow();
    super.setTopText("Content.");
    dialogStage.getIcons().add(UiUtil.getImage("/img/searchList.png"));

    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    updateGrid();
  }

  /**************************************************************************
   *
   * Public API
   *
   **************************************************************************/

  // ********************************************************************** Private Implementation
  protected void updateGrid() {
    super.prepareGrid();

    contentGrid.add(new Label("Search for text"), 0, 1);
    contentGrid.add(listSearchView.getSearchFld(), 1, 1);
    GridPane.setFillWidth(listSearchView.getSearchFld(), true);

    contentGrid.add(listSearchView.getListView(), 0, 2, 2, 1);
    GridPane.setFillWidth(listSearchView.getListView(), true);

    contentGrid.getColumnConstraints().forEach(c -> c.setHgrow(Priority.ALWAYS));
    Platform.runLater(listSearchView.getListView()::requestFocus);
  }


}
