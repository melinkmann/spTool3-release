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

import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.fxImpl.FxSample;
import gui.dialog.FxEntry;
import gui.listAndSearch.SampleListAndTable;
import io.SampleSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.StringConverter;

public class SampleSetListCell extends TextFieldListCell<FxEntry<SampleSet>> {

  private final SampleListAndTable view;
  // The table is needed to enable drag and drop into sample sets
  private final TableView<FxSample> sampleTableView;

  public SampleSetListCell(SampleListAndTable view,
      TableView<FxSample> sampleTableView) {
    super();
    this.view = view;
    this.sampleTableView = sampleTableView;

    // Setup Drag Drop
    // READ THIS: DRAG DROP ON THE LIST CELL --> WE NEED CUSTOM LIST CELL FACTORY FOR _THIS_ CLASS
    // https://stackoverflow.com/questions/25390888/dragging-and-dropping-list-view-items-between-different-javafx-windows
    // https://coderanch.com/t/658527/java/Implement-drag-drop-ListView-custom
    this.setOnDragOver(new EventHandler<DragEvent>() {
      public void handle(DragEvent event) {
        if (event.getDragboard().hasString()) {
          event.acceptTransferModes(TransferMode.COPY_OR_MOVE);

          Object target = event.getGestureTarget();
          if (target instanceof ListCell<?>) {
            // How the color changes when dragging
            ((ListCell<?>) target).setBlendMode(BlendMode.EXCLUSION);
          }
        }
        event.consume();
      }
    });

    this.setOnDragExited(new EventHandler<DragEvent>() {
      public void handle(DragEvent event) {
        // Reset color when end of dragging
        setBlendMode(null);
        event.consume();
      }
    });

//https://coderanch.com/t/658527/java/Implement-drag-drop-ListView-custom
    this.setOnDragDropped((DragEvent event) -> {
      Dragboard db = event.getDragboard();
      if (db.hasString()) {
        if (getItem() != null) {

          // gets the cell in which is dropped, i.e., the target
          SampleSet targetSet = getItem().unwrap();
          // get the cell which is currently selected, i.e., whose samples are shown in the tableView
          SampleSet selectedSet = getListView().getSelectionModel().getSelectedItem().unwrap();

          // The list of the unique samples, that we want to move/copy
          List<Sample> uniqueSamples = new ArrayList<>();

          Set<TransferMode> modes = db.getTransferModes();
          if (modes.contains(TransferMode.COPY) ||modes.contains(TransferMode.MOVE)) {

            // Cheap workaround to avoid sending the samples across the serializer or the clipboard in general
            sampleTableView.getSelectionModel().getSelectedItems().stream()
                .filter(Objects::nonNull)
                .filter(s -> !uniqueSamples.contains(s.getPlainSample()))
                .filter(s -> !targetSet.getSamples().contains(s.getPlainSample()))
                .forEach(s -> uniqueSamples.add(s.getPlainSample()));
            targetSet.getSamples().addAll(uniqueSamples);

            // check if it was a) not the main set and b) "move action": then remove from original set
            SampleSet mainSet = SpTool3Main.getRunTime().getSampleReg().getMainSet();
            boolean selectedSetIsMain = selectedSet == mainSet;
            if (!selectedSetIsMain && modes.contains(TransferMode.MOVE)){
             if (selectedSet != null) {
               selectedSet.getSamples().removeAll(uniqueSamples);
             }
            }

            // Reset color when end of dragging
            Object target = event.getGestureTarget();
            if (target instanceof ListCell<?>) {
              ((ListCell<?>) target).setBlendMode(null);
            }

          }

          event.setDropCompleted(true);
        }
      } else {
        event.setDropCompleted(false);
      }
      event.consume();
    });

    // ChatGPT suggests call here to prevent bug that cell is uneditable
    refreshConverter();
  }

  private void refreshConverter() {
    StringConverter<FxEntry<SampleSet>> converter = new StringConverter<>() {
      @Override
      public String toString(FxEntry<SampleSet> set) {
        return set.getLabel();
      }

      // https://stackoverflow.com/questions/35963888/how-to-create-a-listview-of-complex-objects-and-allow-editing-a-field-on-the-obj
      @Override
      public FxEntry<SampleSet> fromString(String string) {
        if (isEmpty()) {
          // Get T instance stored in the Cell.
          // Note: Here, return new Instance of sth. only makes sense,
          // if the user input actually creates a NEW instance. What we want here,
          // is returning the old object but call its setter method once.
          FxEntry<SampleSet> t = getItem();
          t.setLabel(string);
          return t;
        }
        FxEntry<SampleSet> set = getItem();
        // Overrides the value.
        set.setLabel(string);
        // Re-sort the list.
        view.filterSampleSets();
        return set;
      }
    };
    setConverter(converter);
  }

  @Override
  public void updateItem(FxEntry<SampleSet> fxEntry, boolean empty) {
    super.updateItem(fxEntry, empty);
    if (empty || fxEntry == null) {
      setText("");
      setGraphic(null);
    } else {
      //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html https://stackoverflow.com/questions/62897231/javafx-change-listviews-focusmodel
      setText(fxEntry.getCellLabelProperty().get());
    }
    refreshConverter();
  }


}
