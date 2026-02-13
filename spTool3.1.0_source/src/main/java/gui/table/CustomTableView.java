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

package gui.table;

import javafx.scene.control.TableView;

public class CustomTableView<S> extends TableView<S> {

  public CustomTableView() {
    widthProperty().addListener((obs, old, tableWidth) -> {
      // Deduct 2px from the total table width for borders. Otherwise you will see a horizontal scroll bar.
      double width = tableWidth.doubleValue() - 2;
      getColumns().stream().filter(col -> col instanceof CustomTableColumn)
          .map(col -> (CustomTableColumn) col)
          .forEach(col -> col.setPrefWidth(width * (col.getPercentWidth() / 100)));
    });
  }

}
