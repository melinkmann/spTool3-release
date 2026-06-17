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

import analysis.AnalysisUtils;
import analysis.Population;
import analysis.PopulationID;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.Trace;
import dataModelNew.mz.Channel;
import gui.listAndSearch.SampleListAndTable;
import gui.util.UiUtil;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

import java.util.List;

public class PopulationListCell extends TextFieldListCell<PopulationID> {

  private final List<PopulationID> lockedPopulations;
  private final SampleListAndTable sampleListAndTable;

  public PopulationListCell(SampleListAndTable sampleListAndTable, List<PopulationID> lockedPopulations) {
    super();
    this.lockedPopulations = lockedPopulations;
    this.sampleListAndTable = sampleListAndTable;
    refreshConverter();
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
          PopulationID id = getItem();
          return id;
        }
        PopulationID id = getItem();
        if (id != null) {
          id.setLabel(string);
          // check the other sample's pop IDs, too (name is not part of the hash/equals routine)
          for (Sample sample : sampleListAndTable.getSelSamples()) {
            for (Sample subSample : sample.getAllSamples()) {
              for (Channel channel : sampleListAndTable.getSelChannels()) {
                Trace t = subSample.getTrace(channel);
                if (t != null) {
                  Population p = t.getPopulation(id);
                  if (p != null) {
                    p.setName(string);
                  }
                }

              }
            }

          }
        }
        return id;
      }
    };
    setConverter(converter);
  }

//  @Override
//  public void updateItem(PopulationID entry, boolean empty) {
//    super.updateItem(entry, empty);
//    if (empty || entry == null) {
//      setText("");
//      setGraphic(null);
//    } else {
//      //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html https://stackoverflow
//      // .com/questions/62897231/javafx-change-listviews-focusmodel
//      setText(entry.toString());
//    }
//    refreshConverter();
//  }

  @Override
  public void updateItem(PopulationID item, boolean empty) {

    // Can be reused, text is updated
    // final Tooltip tooltip = UiUtil.getDefaultStyleTooltip();

    super.updateItem(item, empty);
    if (empty || item == null) {
      setText("");
      setGraphic(null); // reset graphic if not locked, else random symbol appear
      //setTooltip(null);
    } else {
      //https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Cell.html https://stackoverflow
      // .com/questions/62897231/javafx-change-listviews-focusmodel
      setText(item.toString());
      //tooltip.setText(String.valueOf(getItem().getOrder()));
      //setTooltip(tooltip);
      if (lockedPopulations.contains(item)) {
        ImageView view = UiUtil.getViewer("/img/lock.png");
        this.setGraphic(view);
      } else {
        setGraphic(null); // reset graphic if not locked, else random symbol appear
      }
      // check if no sample contains
      boolean anySampleContains;
      List<Sample> selSamples = sampleListAndTable.getSelSamples();
      ;
      List<Channel> selIsotopes = sampleListAndTable.getSelChannels();
      List<PopulationID> availableIDs = AnalysisUtils.listPopulations(selSamples,
          selIsotopes);
      anySampleContains = availableIDs.contains(item);
      if (!anySampleContains) {
        ImageView view = UiUtil.getViewer("/img/softIssue.png");
        setGraphic(view);
      }
    }
    refreshConverter();
  }


}
