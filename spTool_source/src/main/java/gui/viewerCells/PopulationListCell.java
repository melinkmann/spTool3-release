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

package gui.viewerCells;

import analysis.PopulationID;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.util.StringConverter;

public class PopulationListCell extends TextFieldListCell<PopulationID> {

  public PopulationListCell() {
    super();
  }

  private void refreshConverter() {
    StringConverter<PopulationID> converter = new StringConverter<>() {
      @Override
      public String toString(PopulationID entry) {
        return entry.toString();
      }

      // https://stackoverflow.com/questions/35963888/how-to-create-a-listview-of-complex-objects-and-allow-editing-a-field-on-the-obj
      @Override
      public PopulationID fromString(String string) {
        if (isEmpty()) {
          // Get T instance stored in the Cell.
          // Note: Here, return new Instance of sth. only makes sense,
          // if the user input actually creates a NEW instance. What we want here,
          // is returning the old object but call its setter method once.
          PopulationID t = getItem();
          return t;
        }
        PopulationID t = getItem();
        return t;
      }
    };
    setConverter(converter);
  }

  @Override
  public void updateItem(PopulationID entry, boolean empty) {
    super.updateItem(entry, empty);
    if (empty || entry == null) {
      setText("");
      setGraphic(null);
    } else {
      //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel
      setText(entry.toString());
    }
    refreshConverter();
  }


}
