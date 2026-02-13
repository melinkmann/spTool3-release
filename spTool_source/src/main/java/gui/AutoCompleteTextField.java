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

package gui;

import gui.dialog.Fillable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * This class is a TextField which implements an "autocomplete" functionality, based on a supplied
 * list of entries. Major edit for spTool.
 * @author Caleb Brinkman
 * @author Fabian Ochmann
 * @author Matthias Elinkmann
 */
public class AutoCompleteTextField<S extends Serializable> extends TextField {

  private final ObjectProperty<Fillable<S>> lastSelectedItem = new SimpleObjectProperty<>();

  /**
   * The existing autocomplete entries.
   */
  private final SortedSet<Fillable<S>> entries;

  /**
   * The set of filtered entries:<br> Equal to the search results if search results are not empty,
   * equal to {@link #entries entries} otherwise.
   */
  private ObservableList<Fillable<S>> filteredEntries
      = FXCollections.observableArrayList();

  /**
   * The popup used to select an entry.
   */
  private ContextMenu entriesPopup;

  /**
   * Indicates whether the search is case sensitive or not. <br> Default: false
   */
  private boolean caseSensitive = false;

  /**
   * Indicates whether the Popup should be hidden or displayed. Use this if you want to filter an
   * existing list/set (for example values of a {@link javafx.scene.control.ListView ListView}). Do
   * this by binding {@link #getFilteredEntries() getFilteredEntries()} to the list/set.
   */
  private boolean popupHidden = false;

  /**
   * The CSS style that should be applied on the parts in the popup that match the entered text.
   * <br> Default: "-fx-font-weight: bold; -fx-fill: red;"
   * <p>
   * Note: This style is going to be applied on an {@link javafx.scene.text.Text Text} instance. See
   * the <i>JavaFX CSS Reference Guide</i> for available CSS Propeties.
   */
  private String textOccurenceStyle = "-fx-font-weight: bold; "
      + "-fx-fill: blue;";

  /**
   * The maximum Number of entries displayed in the popup.<br> Default: 10
   */
  private int maxEntries = 10;

  /**
   * Construct a new AutoCompleteTextField.
   *
   * @param entrySet
   */
  public AutoCompleteTextField(Fillable<S> initialValue, SortedSet<Fillable<S>> entrySet) {
    super(initialValue.getStringValue());
    this.entries = (entrySet == null ? new TreeSet<>(new ArrayList<>()) : entrySet);
    this.filteredEntries.addAll(entries);

    entriesPopup = new ContextMenu();

    textProperty().addListener(
        (ObservableValue<? extends String> observableValue, String s, String s2) -> {

          if (getText() == null || getText().length() == 0) {
            filteredEntries.clear();
            filteredEntries.addAll(entries);
            entriesPopup.hide();
          } else {
            LinkedList<Fillable<S>> searchResult = new LinkedList<>();

            //Check if the entered Text is part of some entry
            String currentText = getText();

            for (Fillable<S> entry : entries) {
              if (entry.getStringValue().toLowerCase(Locale.ROOT).contains(
                  currentText.toLowerCase(Locale.ROOT))) {
                searchResult.add(entry);
              }
            }
            if (!entries.isEmpty()) {
              filteredEntries.clear();
              filteredEntries.addAll(searchResult);
              //Only show popup if not in filter mode
              if (!isPopupHidden()) {
                populatePopup(searchResult, currentText);
                if (!entriesPopup.isShowing()) {
                  entriesPopup.show(AutoCompleteTextField.this, Side.BOTTOM, 0, 0);
                  // Else, the red formatting of the textfield will be carried on to this menu.
                  entriesPopup.setStyle("-fx-control-inner-background: white");
                  entriesPopup.setStyle("-fx-background-color: white");
                }
              }
            } else {
              entriesPopup.hide();
            }
          }
        });

    focusedProperty().addListener(
        (ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) ->
            entriesPopup.hide());

    // Enable the filling of the clicked on value as expected for autofill
    getEntryMenu().setOnAction(e -> {
      ((MenuItem) e.getTarget()).addEventHandler(Event.ANY, event -> {
        if (getLastSelectedObject() != null) {
          setText(getLastSelectedObject().getStringValue());
        }
      });
    });

  }

  /**
   * Get the existing set of autocomplete entries.
   *
   * @return The existing autocomplete entries.
   */
  public SortedSet<Fillable<S>> getEntries() {
    return entries;
  }

  /**
   * Populate the entry set with the given search results. Display is limited to 10 entries, for
   * performance.
   *
   * @param searchResult The set of matching strings.
   */
  private void populatePopup(List<Fillable<S>> searchResult, String text) {
    List<CustomMenuItem> menuItems = new LinkedList<>();
    int count = Math.min(searchResult.size(), getMaxEntries());
    for (int i = 0; i < count; i++) {
      final String result = searchResult.get(i).getStringValue();
      final Fillable<S> itemObject = searchResult.get(i);
      int occurence = result.toLowerCase().indexOf(text.toLowerCase());

      if (occurence < 0) {
        continue;
      }
      //Part before occurence (might be empty)
      Text pre = new Text(result.substring(0, occurence));
      //Part of (first) occurence
      Text in = new Text(result.substring(occurence, occurence + text.length()));
      in.setStyle(getTextOccurenceStyle());
      //Part after occurence
      Text post = new Text(result.substring(occurence + text.length(), result.length()));

      TextFlow entryFlow = new TextFlow(pre, in, post);

      CustomMenuItem item = new CustomMenuItem(entryFlow, true);
      item.setOnAction((ActionEvent actionEvent) -> {
        lastSelectedItem.set(itemObject);
        entriesPopup.hide();
      });
      menuItems.add(item);
    }
    entriesPopup.getItems().clear();
    entriesPopup.getItems().addAll(menuItems);

  }

  public Fillable<S> getLastSelectedObject() {
    return lastSelectedItem.get();
  }

  public ContextMenu getEntryMenu() {
    return entriesPopup;
  }

  public String getTextOccurenceStyle() {
    return textOccurenceStyle;
  }

  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public void setTextOccurenceStyle(String textOccurenceStyle) {
    this.textOccurenceStyle = textOccurenceStyle;
  }

  public boolean isPopupHidden() {
    return popupHidden;
  }

  public void setPopupHidden(boolean popupHidden) {
    this.popupHidden = popupHidden;
  }

  public ObservableList<Fillable<S>> getFilteredEntries() {
    return filteredEntries;
  }

  public int getMaxEntries() {
    return maxEntries;
  }

  public void setMaxEntries(int maxEntries) {
    this.maxEntries = maxEntries;
  }
}

