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

import java.util.List;

import gui.util.UiUtil;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

public class ChooseSingleFromListDialog<T> extends AbstractListDialog<T> {

  private final String statement;

  public ChooseSingleFromListDialog(List<FxEntry<T>> optionList,
                                    FxEntryFactory<T> entryFactory,
                                    boolean doubleClickSelect,
                                    boolean closeOnDoubleClick,
                                    boolean isEditable,
                                    FxStageButton fxStageButton) {
    this(optionList,
        entryFactory,
        doubleClickSelect, closeOnDoubleClick,
        isEditable,
        "",
        "",
        fxStageButton);
  }

  public ChooseSingleFromListDialog(List<FxEntry<T>> optionList,
                                    FxEntryFactory<T> entryFactory,
                                    boolean doubleClickSelect,
                                    boolean closeOnDoubleClick,
                                    boolean isEditable,
                                    String statement,
                                    String searchIni,
                                    FxStageButton fxStageButton) {

    super(optionList,
        entryFactory,
        SelectionMode.SINGLE,
        false,
        false,
        doubleClickSelect,
        closeOnDoubleClick,
        isEditable,
        fxStageButton);

    // Context Custom Menu
    listSearchView.addViewContentOfItemMenu();

    // set starting value
    if (searchIni != null && !searchIni.isEmpty())
      listSearchView.getSearchFld().setText(searchIni);
    listSearchView.filterList();

    //-- pane style
    super.setTopText("Select one item.");

    //
    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    this.statement = statement;

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

    if (statement != null && !statement.isEmpty()) {
      contentGrid.add(new Label("Search for text"), 0, 1);
      contentGrid.add(listSearchView.getSearchFld(), 1, 1);

      TextField textField = new TextField(statement);
      UiUtil.makeLabelField(textField);
      contentGrid.add(textField, 1, 2);

      GridPane.setFillWidth(listSearchView.getSearchFld(), true);

      contentGrid.add(listSearchView.getListView(), 0, 3, 2, 1);
      GridPane.setFillWidth(listSearchView.getListView(), true);

      contentGrid.getColumnConstraints().forEach(c -> c.setHgrow(Priority.ALWAYS));
      Platform.runLater(listSearchView.getListView()::requestFocus);

    } else {
      contentGrid.add(new Label("Search for text"), 0, 1);
      contentGrid.add(listSearchView.getSearchFld(), 1, 1);
      GridPane.setFillWidth(listSearchView.getSearchFld(), true);

      contentGrid.add(listSearchView.getListView(), 0, 2, 2, 1);
      GridPane.setFillWidth(listSearchView.getListView(), true);

      contentGrid.getColumnConstraints().forEach(c -> c.setHgrow(Priority.ALWAYS));
      Platform.runLater(listSearchView.getListView()::requestFocus);
    }


  }


}
