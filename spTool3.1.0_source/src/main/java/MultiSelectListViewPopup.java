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

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

public class MultiSelectListViewPopup extends Application {

  @Override
  public void start(Stage primaryStage) {
    // List of options
    List<String> options = List.of("Option 1", "Option 2", "Option 3", "Option 4");

    // Main ListView
    ListView<ObservableList<String>> mainListView = new ListView<>();
    mainListView.getItems().add(FXCollections.observableArrayList()); // Initial empty selection

    // Set custom cell factory
    mainListView.setCellFactory(listView -> new MultiSelectListCell(options));

    // Root layout
    VBox root = new VBox(mainListView);
    Scene scene = new Scene(root, 400, 300);

    primaryStage.setScene(scene);
    primaryStage.setTitle("JavaFX Multi-Select ListView Popup");
    primaryStage.show();
  }

  static class MultiSelectListCell extends ListCell<ObservableList<String>> {
    private final List<String> options;
    private Popup popup;
    private ObservableList<String> selectedOptions;

    public MultiSelectListCell(List<String> options) {
      this.options = new ArrayList<>(options);
    }

    @Override
    protected void updateItem(ObservableList<String> item, boolean empty) {
      super.updateItem(item, empty);

      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        selectedOptions = item;
        setText(String.join(", ", item)); // Display selected options as a comma-separated string
        setOnMouseClicked(event -> showPopup());
      }
    }

    private void showPopup() {
      if (popup != null && popup.isShowing()) {
        popup.hide();
      }

      // Create the popup ListView with multiple selection enabled
      ListView<String> popupListView = new ListView<>();
      popupListView.getItems().addAll(options);
      popupListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

      // Preselect current selections in the popup
      for (String option : selectedOptions) {
        popupListView.getSelectionModel().select(option);
      }

      // OK and Cancel buttons
      Button okButton = new Button("OK");
      Button cancelButton = new Button("Cancel");

      okButton.setOnAction(event -> {
        selectedOptions.setAll(popupListView.getSelectionModel().getSelectedItems());
        setText(String.join(", ", selectedOptions)); // Update the cell's text
        commitEdit(selectedOptions); // Commit changes to the main ListView
        popup.hide();
      });

      cancelButton.setOnAction(event -> popup.hide());

      // Layout for buttons
      HBox buttonBox = new HBox(10, okButton, cancelButton);
      buttonBox.setAlignment(Pos.CENTER);

      // VBox to hold ListView and buttons
      VBox popupContent = new VBox(10, popupListView, buttonBox);
      popupContent.setStyle(
          "-fx-padding: 10;" +
              "-fx-background-color: rgb(120, 145, 175);" +
              "-fx-border-color: rgb(10, 10, 10);" +  // Dark gray border
              "-fx-border-width: 1;" +
              "-fx-border-radius: 10;" +      // Rounded corners for the border
              "-fx-background-radius: 10;"   // Rounded corners for the background
      );
      popupContent.setPrefSize(200, 300);

      // Request focus to capture the Enter key
      popupContent.setFocusTraversable(true);  // Make the popup content focusable
      popupContent.requestFocus();              // Request focus explicitly

      // Handle Enter key to close the popup and confirm the selection
      popupContent.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
        if (event.getCode() == KeyCode.ENTER) { // Detect Enter key press
          selectedOptions.setAll(popupListView.getSelectionModel().getSelectedItems());
          setText(String.join(", ", selectedOptions)); // Update the cell's text
          commitEdit(selectedOptions); // Commit changes to the main ListView
          popup.hide();
          event.consume(); // Prevent further propagation of the event
        }
      });

      // Create and configure the popup
      popup = new Popup();
      popup.getContent().add(popupContent);
      popup.setAutoHide(true); // Automatically close the popup when clicking outside

      // Position the popup near the cell
      popup.show(getScene().getWindow(),
          getLayoutX() + getScene().getX() + getScene().getWindow().getX(),
          getLayoutY() + getScene().getY() + getScene().getWindow().getY() + getHeight());
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
