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

package util;


import gui.dialog.FxEntry;
import java.util.List;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClipboardUtils {

  private static final Logger LOGGER = LogManager.getLogger(ClipboardUtils.class);


  public static void installCopyHandler(ListView<?>... listViews) {
    // install copy/paste keyboard handler
    for (ListView<?> listView : listViews) {
      listView.setOnKeyPressed(new EventHandler<KeyEvent>() {

        final KeyCodeCombination copyKeyCodeCombination =
            new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

        @Override
        public void handle(final KeyEvent keyEvent) {
          if (copyKeyCodeCombination.match(keyEvent)) {
            if (keyEvent.getSource() instanceof ListView<?>) {
              // copy to clipboard
              copySelectionToClipboard((ListView<?>) keyEvent.getSource());
              LOGGER.debug("Selection copied to clipboard");
              // event is handled, consume it
              keyEvent.consume();
            }
          }
        }
      });
    }
  }


  /**
   * Get table selection and copy it to the clipboard.
   *
   * @param listView
   */
  public static void copySelectionToClipboard(ListView<?> listView) {

    StringBuilder clipboardString = new StringBuilder();
    List<?> selectedItems = listView.getSelectionModel().getSelectedItems();
    for (Object o : selectedItems) {
      // Only a few types of objets are known in ListViews --> allow for special cases.
      if (o instanceof FxEntry<?>) {
        clipboardString.append(((FxEntry<?>) o).getCellLabelProperty().get());
      } else {
        clipboardString.append(o.toString());
      }
      clipboardString.append('\n');
    }

    // create clipboard content
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(clipboardString.toString());
    // set clipboard content
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

}
