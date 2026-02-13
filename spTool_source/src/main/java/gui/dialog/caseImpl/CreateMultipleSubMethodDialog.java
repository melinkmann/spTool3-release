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

import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.mainImpl.ChooseMultipleFromListDialog;
import gui.viewerCells.ParamSetListCell;
import java.util.Arrays;
import java.util.List;
import javafx.scene.control.DialogPane;
import processing.parameterSets.FxParamSet;

public class CreateMultipleSubMethodDialog extends ChooseMultipleFromListDialog<FxParamSet> {

  // Make smaller
  protected static final double PREF_WIDTH = 600;
  protected static final double PREF_HEIGHT = 500;

  public CreateMultipleSubMethodDialog(FxParamSet[] values, FxEntryFactory<FxParamSet> factory) {
    this(Arrays.asList(values), factory);
  }

  public CreateMultipleSubMethodDialog(List<FxParamSet> values,
      FxEntryFactory<FxParamSet> factory) {
    super(factory.create(values),
        factory,
        false,
        false,
        false,
        true,
        false,
        FxStageButton.CONTINUE);

    // Custom cell
    setCellFactory(l -> new ParamSetListCell());

    // Make Smaller
    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    // Override text
    super.setTopText("Double click to continue with selected item.");

  }


}
