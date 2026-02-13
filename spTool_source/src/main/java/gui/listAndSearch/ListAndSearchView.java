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

package gui.listAndSearch;

import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.ListContainer;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import gui.viewerCells.EntryListCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;
import util.ClipboardUtils;
import util.NF;
import util.SnF;

public class ListAndSearchView<T> {

  private final FxEntryFactory<T> entryFactory;

  protected final List<FxEntry<T>> allOptions;

  private final ListView<FxEntry<T>> listView;

  private final TextField searchField = new TextField();

  private final Text sizeTxt = new Text("0 / 0");
  private final StackPane viewStackPane = new StackPane();

  public ListAndSearchView(List<FxEntry<T>> optionList,
                           FxEntryFactory<T> entryFactory,
                           SelectionMode selectionMode,
                           boolean useSelectionFromSelectable,
                           boolean hideSymbols,
                           boolean isEditable) {
    this(new ListView<>(), entryFactory, optionList, selectionMode, useSelectionFromSelectable,
        hideSymbols, isEditable);
  }

  public ListAndSearchView(ListView<FxEntry<T>> view,
                           FxEntryFactory<T> entryFactory,
                           List<FxEntry<T>> optionList,
                           SelectionMode selectionMode,
                           boolean useSelectionFromSelectable,
                           boolean hideSymbols,
                           boolean isEditable) {

    this.listView = view;

    this.entryFactory = entryFactory;

    // General features
    ClipboardUtils.installCopyHandler(listView);

    //
    this.allOptions = new ArrayList<>(optionList);

    //
    //https://stackoverflow.com/questions/28285507/is-there-a-way-to-bind-the-content-of-a-listproperty-in-javafx
    this.listView.setItems(FXCollections.observableArrayList(
        fxEntry -> new Observable[]{
            fxEntry.getCellLabelProperty()
        }));
    listView.getItems().addAll(allOptions);

    listView.getSelectionModel().setSelectionMode(selectionMode);

    // Sort the List View
    UiUtil.sortListAndSearchView(listView);
    // Set first AFTER sorting
    listView.getSelectionModel().selectFirst();

    // Use default implementation of Selectable List Cell
    listView.setCellFactory(l -> new EntryListCell<>(this, selectionMode,
        useSelectionFromSelectable, hideSymbols));
    listView.setEditable(isEditable);

    // Listener to refresh the size text field
    listView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> updateSizeText());

    listView.getItems().addListener((ListChangeListener<FxEntry<T>>) c -> updateSizeText());
    sizeTxt.setStyle("-fx-font-size: 10");

    // Context Menu
    provideContextMenu();

    // Fill
    filterList();

    /*
    Stuff concerning the TextField
     */

    searchField.setEditable(true);
    searchField.setPromptText("Search for content");

    final PauseTransition searchFldPause = new PauseTransition(Duration.seconds(0.5));
    searchFldPause.setOnFinished(event -> filterList());
    searchField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        searchFldPause.stop();
        searchFldPause.playFromStart();
      }
    });

  }


  private void provideContextMenu() {
    if (listView.getContextMenu() == null) {
      listView.setContextMenu(new ContextMenu());
    }
  }

  public void addSelectDeselectMenus() {
    MenuItem selectMenu = UiUtil.getImageMenuItem("Select", "/img/yes.png");
    MenuItem deselectMenu = UiUtil.getImageMenuItem("Deselect", "/img/deselect.png");

    listView.getContextMenu().getItems().add(selectMenu);
    listView.getContextMenu().getItems().add(deselectMenu);

    selectMenu.setOnAction(e -> {
      List<FxEntry<T>> selection = listView.getSelectionModel().getSelectedItems();
      selection.stream().filter(Objects::nonNull).forEach(s -> s.setSelected(true));
      UiUtil.sortListAndSearchView(listView);
    });

    deselectMenu.setOnAction(e -> {
      List<FxEntry<T>> selection = listView.getSelectionModel().getSelectedItems();
      selection.stream().filter(Objects::nonNull).forEach(s -> s.setSelected(false));
      UiUtil.sortListAndSearchView(listView);
    });
  }

  public void addSelectAllMenu() {
    MenuItem selectAllMenu = UiUtil.getImageMenuItem("Select all", "/img/selectAll.png");
    listView.getContextMenu().getItems().add(selectAllMenu);

    selectAllMenu.setOnAction(e -> {
      listView.getItems().forEach(s -> s.setSelected(true));
      UiUtil.sortListAndSearchView(listView);
    });
  }

  public void addViewContentOfItemMenu() {

    // adds feature quasi automatically if selectable is present
    if (allOptions.stream().anyMatch(t -> t.unwrap() instanceof ListContainer<?>)) {

      MenuItem childrenMenu = UiUtil.getImageMenuItem("View Content", "/img/load.png");
      listView.getContextMenu().getItems().add(childrenMenu);

      childrenMenu.setOnAction(e -> {
        FxEntry<T> selT = listView.getSelectionModel().getSelectedItem();
        if (selT != null && selT.unwrap() instanceof ListContainer<?>) {
          Dialog<?> childrenDialog = ((ListContainer<?>) selT.unwrap()).getListDialog();
          childrenDialog.showAndWait();
        }
      });
    }
  }

  public void addLikeDislikeMenu() {
    MenuItem favouriteMenu = UiUtil.getImageMenuItem("Favorite", "/img/fav.png");
    listView.getContextMenu().getItems().add(favouriteMenu);

    MenuItem notFavouriteMenu = UiUtil.getImageMenuItem("Undo", "/img/unfav.png");
    listView.getContextMenu().getItems().add(notFavouriteMenu);

    favouriteMenu.setOnAction(e -> {
      listView.getSelectionModel().getSelectedItems().forEach(s -> s.setFavorite(true));
      // Refresh ListView (happens in super class based on the "ALlOptions" list
      filterList();
    });

    notFavouriteMenu.setOnAction(e -> {
      listView.getSelectionModel().getSelectedItems().forEach(s -> s.setFavorite(false));
      // Refresh ListView (happens in super class based on the "ALlOptions" list
      filterList();
    });
  }

  public void addRemoveMenu() {
    MenuItem notFavouriteMenu = UiUtil.getImageMenuItem("Delete", "/img/delete.png");
    notFavouriteMenu.setOnAction(e ->
        NotificationFactory.openYesCancel("Delete? This is irreversible.", () -> {
          removeContent(listView.getSelectionModel().getSelectedItems());
        }));
    listView.getContextMenu().getItems().add(notFavouriteMenu);
  }


  public void addAddMenu(Supplier<T> supplier) {
    MenuItem notFavouriteMenu = UiUtil.getImageMenuItem("Create", "/img/create.png");
    notFavouriteMenu.setOnAction(e -> {
      allOptions.add(entryFactory.create(supplier.get()));
      filterList();
    });
    listView.getContextMenu().getItems().add(notFavouriteMenu);
  }

  public void addMenuItem(MenuItem menuItem) {
    listView.getContextMenu().getItems().add(menuItem);
  }

  // get the view on a stack pane with the field
  public StackPane getViewAndSizePane() {
    if (!viewStackPane.getChildren().contains(sizeTxt)) {
      viewStackPane.getChildren().addAll(listView, sizeTxt);
      StackPane.setAlignment(sizeTxt, Pos.BOTTOM_RIGHT);
      StackPane.setMargin(sizeTxt, new Insets(0, 22, 5, 0));
    }
    return viewStackPane;
  }

  private void updateSizeText() {
    String size =
        SnF.doubleToString(listView.getSelectionModel().getSelectedItems().size(), NF.D1C0)
            + " / "
            + SnF.doubleToString(listView.getItems().size(), NF.D1C0);
    sizeTxt.setText(size);
  }

  // Easier access in sub classes
  public void filterList() {
    UiUtil.filterListAndSearchViewEntries(searchField, listView, allOptions);
  }


  // Access in Subclasses that have more options to modify the content
  public void overrideContentEntry(List<FxEntry<T>> newContent) {
    // Clear previously listed files and then update
    allOptions.clear();
    allOptions.addAll(newContent);
    filterList();
  }

  public void overrideContent(List<T> newContent) {
    // Clear previously listed files and then update
    allOptions.clear();
    allOptions.addAll(entryFactory.create(newContent));
    filterList();
  }

  public void removeContent(List<FxEntry<T>> remContent) {
    allOptions.removeAll(remContent);
    filterList();
  }

  public void clear() {
    allOptions.clear();
    filterList();
  }

  public void addEntry(FxEntry<T> content) {
    allOptions.add(content);
    filterList();
  }

  public void addContent(T content) {
    FxEntry<T> entry = entryFactory.create(content);
    allOptions.add(entry);
    filterList();
  }

  public void addContent(List<T> content) {
    List<FxEntry<T>> entries = entryFactory.create(content);
    allOptions.addAll(entries);
    filterList();
  }

  public ListView<FxEntry<T>> getListView() {
    return listView;
  }

  public TextField getSearchFld() {
    return searchField;
  }

  public List<FxEntry<T>> getAllOptionsUnmodifiable() {
    return Collections.unmodifiableList(allOptions);
  }

  public void setCellFactory(Callback<ListView<FxEntry<T>>, ListCell<FxEntry<T>>> factory) {
    listView.setCellFactory(factory);
  }

  public FxEntryFactory<T> getEntryFactory() {
    return entryFactory;
  }
}
