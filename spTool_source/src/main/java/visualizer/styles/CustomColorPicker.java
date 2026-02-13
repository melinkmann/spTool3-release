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

package visualizer.styles;

import core.SpTool3Main;
import gui.StageFactory;
import gui.util.TextFieldUtils;
import gui.util.UiUtil;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.Callback;
import javafx.util.StringConverter;
import util.NF;
import util.SnF;

public class CustomColorPicker {

  private final Rectangle rect = new Rectangle(350, 300);

  private final BorderPane mainPane;

  private final Popup popup;
  private final Slider sliderRed;
  private final Slider sliderGreen;
  private final Slider sliderBlue;

  private final TextField redField;
  private final TextField greenField;
  private final TextField blueField;

  private final IntegerProperty red;
  private final IntegerProperty green;
  private final IntegerProperty blue;

  private Color current;

  public CustomColorPicker(Color current, List<Colors> defaults, Consumer<Color> consumer) {
    this.current = current;
    red = new SimpleIntegerProperty((int) Math.round(255 * current.getRed()));
    green = new SimpleIntegerProperty((int) Math.round(255 * current.getGreen()));
    blue = new SimpleIntegerProperty((int) Math.round(255 * current.getBlue()));

    this.popup = new Popup();
    this.mainPane = new BorderPane();
    mainPane.setPadding(new Insets(2));

    // Make the popup not close when clicking outside
    popup.setAutoHide(false);
    popup.getScene().getWindow().setWidth(350);
    popup.getScene().getWindow().setHeight(300);

    rect.setFill(current);

    ListView<Colors> colorList = new ListView<>(FXCollections.observableArrayList(defaults));
    colorList.setCellFactory(new Callback<>() {
      @Override
      public ListCell<Colors> call(ListView<Colors> listView) {
        return new ListCell<>() {
          private final Rectangle rect = new Rectangle(25, 25);

          @Override
          protected void updateItem(Colors color, boolean empty) {
            super.updateItem(color, empty);

            // or show hex/text if needed
            if (empty || color == null) {
              setGraphic(null);
            } else {
              rect.setFill(color.getFX());
              setGraphic(rect);
            }
            setText("");
          }
        };
      }
    });


    colorList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Colors>() {
      @Override
      public void changed(ObservableValue<? extends Colors> observableValue, Colors colors,
          Colors t1) {
        if (t1 != null) {
          red.setValue(t1.get().getRed());
          green.setValue(t1.get().getGreen());
          blue.setValue(t1.get().getBlue());
        }
      }
    });

    colorList.setPrefWidth(65);
    colorList.setPrefHeight(500);

    this.sliderRed = new Slider(0, 255, (int) Math.round(255 * current.getRed()));
    this.sliderGreen = new Slider(0, 255, (int) Math.round(255 * current.getRed()));
    this.sliderBlue = new Slider(0, 255, (int) Math.round(255 * current.getRed()));

    this.redField = new TextField();
    this.greenField = new TextField();
    this.blueField = new TextField();

    redField.setTextFormatter(
        TextFieldUtils.assurePositiveInteger((int) Math.round(255 * current.getRed())));

    greenField.setTextFormatter(
        TextFieldUtils.assurePositiveInteger((int) Math.round(255 * current.getGreen())));

    blueField.setTextFormatter(
        TextFieldUtils.assurePositiveInteger((int) Math.round(255 * current.getBlue())));

    redField.setAlignment(Pos.CENTER);
    greenField.setAlignment(Pos.CENTER);
    blueField.setAlignment(Pos.CENTER);

    HBox sliderBox = new HBox(5);

    sliderBox.getChildren().addAll(
        new VBox(2,
            new Label("Red"),
            setUpSlider(sliderRed, Color.rgb(255, 160, 160)),
            redField),

        new VBox(2,
            new Label("Green"),
            setUpSlider(sliderGreen, Color.rgb(90, 255, 90)),
            greenField
        ),

        new VBox(2,
            new Label("Blue"),
            setUpSlider(sliderBlue, Color.rgb(180, 180, 255)),
            blueField
        )
    );

    sliderRed.valueProperty().bindBidirectional(red);
    sliderGreen.valueProperty().bindBidirectional(green);
    sliderBlue.valueProperty().bindBidirectional(blue);

    // RED
    Bindings.bindBidirectional(redField.textProperty(), red, new StringConverter<>() {
      @Override
      public String toString(Number object) {
        return SnF.doubleToString(object.doubleValue(), NF.D1C0);
      }

      @Override
      public Number fromString(String string) {
        try {
          return Double.parseDouble(string);
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    });

    red.addListener((observableValue, number, t1) -> {
      if (t1.doubleValue() > 255) {
        redField.setText("255");
        sliderRed.setValue(255);
        red.setValue(255);
      }
      setCurrent(Color.rgb(red.get(), green.get(), blue.get()));
      setCurrentToRectangle();
    });

    // GREEN
    Bindings.bindBidirectional(greenField.textProperty(), green, new StringConverter<>() {
      @Override
      public String toString(Number object) {
        return SnF.doubleToString(object.doubleValue(), NF.D1C0);
      }

      @Override
      public Number fromString(String string) {
        try {
          return Double.parseDouble(string);
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    });

    green.addListener((observableValue, number, t1) -> {
      if (t1.doubleValue() > 255) {
        greenField.setText("255");
        sliderGreen.setValue(255);
        green.setValue(255);
      }
      setCurrent(Color.rgb(red.get(), green.get(), blue.get()));
      setCurrentToRectangle();
    });

    // BLUE
    Bindings.bindBidirectional(blueField.textProperty(), blue, new StringConverter<>() {
      @Override
      public String toString(Number object) {
        return SnF.doubleToString(object.doubleValue(), NF.D1C0);
      }

      @Override
      public Number fromString(String string) {
        try {
          return Double.parseDouble(string);
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    });

    blue.addListener((observableValue, number, t1) -> {
      if (t1.doubleValue() > 255) {
        blueField.setText("255");
        sliderBlue.setValue(255);
        blue.setValue(255);
      }
      setCurrent(Color.rgb(red.get(), green.get(), blue.get()));
      setCurrentToRectangle();
    });

    final Button selectBtn = new Button("Select");
    final Button cancelBtn = new Button("Cancel");

    cancelBtn.setOnAction(event -> popup.hide());

    selectBtn.setOnAction(e -> {
      consumer.accept(getCurrent());
      popup.hide();
    });

    final HBox buttonBox = new HBox(2);
    buttonBox.getChildren().addAll(selectBtn, cancelBtn);

    mainPane.setBottom(buttonBox);
    mainPane.setTop(sliderBox);
    mainPane.setCenter(rect);
    mainPane.setRight(colorList);

    popup.getContent().add(UiUtil.putOnAnchorWithInsets(mainPane));
    UiUtil.formatPopup(mainPane);

    popup.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
      // Fire on ctl enter
      if (StageFactory.KEY_CTL_ENTER.match(event)) {
        selectBtn.fire();
      }
    });

    colorList.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        if (event.getClickCount()==2){
          selectBtn.fire();
        }
      }
    });
  }

  private static StackPane setUpSlider(Slider s, Color color) {
    s.setMajorTickUnit(255);
    s.setMinorTickCount(254);
    s.setSnapToTicks(true);
    s.setShowTickLabels(true);
    s.setBlockIncrement(1);
    s.setContextMenu(new ContextMenu());

    StackPane sliderWrapper = new StackPane();
    // Padding to make the background color visible around the slider
    sliderWrapper.setPadding(new Insets(5));

    // Set background color
    sliderWrapper
        .setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
    sliderWrapper.getChildren().add(s);

    return sliderWrapper;
  }

  public void show() {
    // Get current mouse position using java.awt.MouseInfo
    Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
    double mouseX = mouseLocation.getX();
    double mouseY = mouseLocation.getY();

    if (!popup.isShowing()) {
      // Show popup near the mouse location -> // Adjust offset as needed
      popup.show(SpTool3Main.getMainStage(), mouseX, mouseY - 500);
    } else {
      popup.setX(mouseX);
      popup.setY(mouseY - 500);
    }
  }

  private void setCurrent(Color color) {
    current = color;
  }

  private void setCurrentToRectangle() {
    rect.setFill(current);
  }

  private Color getCurrent() {
    return current;
  }

}
