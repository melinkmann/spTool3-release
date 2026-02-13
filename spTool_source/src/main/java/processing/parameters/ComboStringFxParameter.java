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

package processing.parameters;


import gui.util.GlobalFields;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.Nullable;
import processing.parameters.ComboStringParameter.Matcher;

public class ComboStringFxParameter extends AbstractFxParameter<String> implements
    FxParameter<String> {

  private final ComboBox<String> box;
  private final String[] options;

  @javax.annotation.Nullable
  private final Matcher stringMatcher;

  private final TextField searchField;

  public ComboStringFxParameter(ComboStringParameter plain, String[] options,
      Matcher stringMatcher) {
    super(plain);
    this.options = options;
    this.stringMatcher = stringMatcher;

    // Make Box.
    box = new ComboBox<>();
    box.setVisibleRowCount(Math.min(20, options.length)); // show more options
    box.setPrefSize(GlobalFields.FX_ITEM_WIDTH, GlobalFields.FX_ITEM_HEIGHT);
    ObservableList<String> allOptions = FXCollections.observableArrayList(options);
    box.setItems(allOptions);
    box.getSelectionModel().select(super.plainParameter.getValue());

    // Tooltip
    super.addToolTip(box);

    // Change Listener.
    box.getSelectionModel().selectedItemProperty().addListener(
        (observableValue, oldValue, newVal) -> {
          if (newVal != null) {
            if (!isUneditable()) {
              // Set value first since children depend on the value.
              ComboStringFxParameter.super.plainParameter.setValue(newVal);
              // Update Children.
              ComboStringFxParameter.super.notifyItemChange();
            }
          }
        });

    // Scroll to list: show and select the selected item - Unsure what this was intended for.
    // When commented, List still shows sel item.
    // When active, however, the whole conf scroll bar jumps around crazily.
    // I supposed, this was necessary back then when the main ConfView ListView
    // was recreated each time a parameter changed.
    //    box.setOnShowing(e -> {
    //      ComboBoxListViewSkin<?> skin = (ComboBoxListViewSkin<?>) box.getSkin();
    //      ListView<?> list = (ListView<?>) skin.getPopupContent();
    //      if (list != null) {
    //        String option = box.getSelectionModel().getSelectedItem();
    //        list.getSelectionModel().select(list.getItems().indexOf(option));
    //        list.scrollTo(box.getItems().indexOf(option));
    //      }
    //    });

    // search
    this.searchField = new TextField();
    this.searchField.setPrefWidth(110);
    this.searchField.setMaxWidth(250);
    this.searchField.setPromptText("Type to search");

    AtomicReference<String> slowPauseNewValue = new AtomicReference<>("");
    super.slowPause.setOnFinished(event -> {
      if (!isUneditable()) {
        if (slowPauseNewValue.get() != null && !slowPauseNewValue.get().isEmpty()) {

          boolean useGuessingApproach = stringMatcher == null;
          if (!useGuessingApproach) {
            String option = stringMatcher.match(slowPauseNewValue.get(), options);
            if (option != null && Arrays.asList(options).contains(option)) {
              box.getSelectionModel().select(option);
            } else {
              useGuessingApproach = true;
            }
          }

          if (useGuessingApproach) {

            // Guessing
            String searchText = slowPauseNewValue.get().toLowerCase(Locale.ROOT);
            String option = null;

            // Perfect match?
            for (String strOption : allOptions) {
              if (strOption.toLowerCase(Locale.ROOT).startsWith(searchText)) {
                option = strOption;
                break;
              }
            }

            // contains all parts -two way-
            String[] searchParts = searchText.split(" ");
            // contains all parts -one way-
            if (option == null) {
              for (String strOption : allOptions) {
                boolean allPartsMatch = searchParts.length > 0;
                for (String searchPart : searchParts) {
                  allPartsMatch =
                      allPartsMatch && strOption.toLowerCase(Locale.ROOT)
                          .contains(searchPart);
                }
                if (allPartsMatch) {
                  option = strOption;
                  break;
                }
              }
            }

            // Simple contains?
            if (option == null) {
              for (String strOption : allOptions) {
                if (strOption.toLowerCase(Locale.ROOT).contains(searchText)) {
                  option = strOption;
                  break;
                }
              }
            }

            if (option != null) {
              box.getSelectionModel().select(option);
            }

          }
        }
      }
    });
    this.searchField.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          slowPause.stop();
          slowPauseNewValue.set(newValue);
          slowPause.playFromStart();
        }
    );
  }

  @Override
  public void forceUpdateExternally() {
    loadFromPlainWithFormat();
  }

  /**
   * This will force an update even if parameter is set uneditable. Uneditable only refers to the
   * possibility to access the field in the UI. If e.g., resetToDefaults etc., is called on the
   * plain instance, we do want to be allowed to load and see this here!
   * <p> Note: The equality check here serves to avoid endless looping by firing listeners -> Make
   * sure the new value is not equal to current. (Disclaimer: I am unsure if this is always
   * necessary as most listeners should listen to CHANGES anyway, but having a break here seems
   * reasonable):
   */
  private void loadFromPlainWithFormat() {
    if (plainParameter.getValue() != box.getSelectionModel().getSelectedItem()) {
      box.getSelectionModel().select(plainParameter.getValue());
    }
  }


  @Override
  public Control getValueNode() {
    return box;
  }

  @Nullable
  @Override
  public Node getDecoration() {
    if (plainParameter instanceof ComboStringParameter) {
      if (((ComboStringParameter) plainParameter).isShowSearchField()) {
        return searchField;
      }
    }
    return null;
  }

  @Override
  public void setUneditable() {
    super.setUneditable();
    // Remove options to show only one item, i.e., prevent change.
    box.getItems().clear();
    box.getItems().add(plainParameter.getValue());
    box.getSelectionModel().select(plainParameter.getValue());
  }

}