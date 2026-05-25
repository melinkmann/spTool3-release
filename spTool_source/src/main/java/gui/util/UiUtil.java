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

package gui.util;

import analysis.PopulationID;
import com.google.common.math.DoubleMath;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.mz.Element;
import dataModelNew.mz.InterferenceDatabase;
import dataModelNew.mz.InterferenceDatabase.InterferenceEntry;
import gui.dialog.FxEntry;
import gui.dialog.notification.NotificationFactory;
import gui.dialog.notification.PopupFactory;
import gui.listAndSearch.FxWrapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import processing.options.IsotopeSelection;
import processing.parameterSets.AvailableParameterSets;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.Statistics;
import util.ArrUtils;
import util.NF;
import util.SnF;
import util.Util;
import visualizer.styles.FontStyles;
import visualizer.styles.UiColors;

public abstract class UiUtil {

  private static final Logger LOGGER = LogManager.getLogger(UiUtil.class);

  // #############################################################################################

  public static void installFastScroll(ScrollPane... panes) {
    /*
     * Scroll Speed
     * https://stackoverflow.com/questions/32739269/how-do-i-change-the-amount-by-which-scrollpane-scrolls
     */
    for (ScrollPane pane : panes) {
      pane.getContent().setOnScroll(new EventHandler<ScrollEvent>() {
        @Override
        public void handle(ScrollEvent event) {
          double deltaY = event.getDeltaY() * 3.5; // *6 to make the scrolling a bit faster
          double width = pane.getContent().getBoundsInLocal().getHeight();
          double vvalue = pane.getVvalue();
          pane.setVvalue(vvalue + -deltaY / width);
          // deltaY/width to make the scrolling equally fast regardless of the actual width of the component
        }
      });
    }
  }

  // #############################################################################################

  public static void installDoubleClickSelect(ListView<?> listView) {
    listView.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent click) {
        // @ double click
        if (click.getClickCount() == 2) {
          listView.getSelectionModel().selectAll();
        }
      }
    });
  }

// #############################################################################################

  public static void makeDroppable(TextField fld) {

    fld.setOnDragOver(new EventHandler<DragEvent>() {
      public void handle(DragEvent event) {
        if (event.getDragboard().hasString()) {
          event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
      }
    });

    fld.setOnDragDropped((DragEvent event) -> {
      Dragboard db = event.getDragboard();
      if (db.hasString()) {
        if (fld.getTextFormatter().getValueConverter() instanceof IntegerStringConverter) {
          String str = db.getString();
          int val = (int) SnF.strToDouble(str); //drag drop will return double!
          String intStr = Integer.toString(val);
          fld.setText(intStr);
        } else {
          fld.setText(db.getString());
        }
        event.setDropCompleted(true);
      } else {
        event.setDropCompleted(false);
      }
      event.consume();
    });
  }

  // #############################################################################################

  public static void prefWidth(double prefHeight, Control control, Pane pane) {
    prefSize(prefHeight, GlobalFields.FX_ITEM_WIDTH, pane);
    prefSize(prefHeight, GlobalFields.FX_ITEM_WIDTH, control);
  }

  public static void prefSize(double prefHeight, double prefWidth, Control control) {
    control.setPrefWidth(prefWidth);
    control.setPrefHeight(prefHeight);
  }

  public static void maxSize(double prefHeight, double prefWidth, Control control) {
    control.setMaxWidth(prefWidth);
    control.setMaxHeight(prefHeight);
  }

  public static void prefSize(double prefHeight, double prefWidth, Pane pane) {
    pane.setPrefWidth(prefWidth);
    pane.getChildren().stream()
        .filter(c -> c instanceof Control)
        .map(c -> (Control) c)
        .forEach(c -> c.setPrefWidth(prefWidth));
    pane.setPrefHeight(prefHeight);
  }


  // #############################################################################################

  public static void formatTitle(TextTitle title) {
    title.setFont(FontStyles.getBold());
    title.setPaint(java.awt.Color.BLACK);
    title.setPosition(RectangleEdge.TOP);
    title.setHorizontalAlignment(HorizontalAlignment.CENTER);
  }

  // #############################################################################################


  public static void formatScrollPane(ScrollPane scrollPane) {
    scrollPane.fitToWidthProperty().set(true);
    scrollPane.fitToHeightProperty().set(true);
    scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
    shrinkScrollBar(scrollPane);
  }

  public static void formatLegendScrollPane(ScrollPane scrollPane) {
    scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
    shrinkScrollBar(scrollPane);
  }

  public static void makePaneBright(Pane pane) {
    pane.setStyle(
        "-fx-padding: 0;" +
            "-fx-border-width: 0.75;" +
            "-fx-background-color: rgb(250,250,250);" +
            "-fx-border-color: rgb(250,250,250);"
    );
  }

  public static void makePaneBrightAndRound(Control pane) {
    pane.setStyle(
        "-fx-padding: 3;" +
            "-fx-background-color: rgb(250,250,250);" +
            "-fx-border-color: rgb(250,250,250);" +
            "-fx-border-width: 0.75;" +
            "-fx-background-radius: 3;" +
            "-fx-border-radius: 3;"
    );
  }

  public static void makePaneRound(Control pane) {
    pane.setStyle(
        "-fx-padding: 3;" +
            "-fx-border-width: 0.75;" +
            "-fx-background-radius: 3;" +
            "-fx-border-radius: 3;"
    );
  }

  public static void formatPopup(Pane pane) {
    pane.setStyle(
        "-fx-padding: 5;" +
            "-fx-background-color: rgb(250,250,250);" +
            "-fx-border-color: gray;" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: 3;" +
            "-fx-border-radius: 3;"
    );
  }

  public static void formatListView(ListView<?> view) {
    view.setStyle(
        "-fx-border-color: rgb(250,250,250); "
            + "-fx-border-width: 0.5; "
            + "-fx-background-radius: 3; "
            + "-fx-border-radius: 3; ");
  }

  public static void formatListCellGray(ListCell<?> cell) {
    cell.setStyle(
        "-fx-background-color: #F4F4F4; "
            + "-fx-border-color: #F4F4F4; "
            + "-fx-text-fill: black; "
            + "");
  }

  public static void formatListCellGrayBold(ListCell<?> cell) {
    cell.setStyle(
        "-fx-background-color: #F4F4F4; "
            + "-fx-border-color: #F4F4F4; "
            + "-fx-text-fill: black; "
            + "-fx-font-weight: bold; "
            + "");
  }


  public static void formatListCellWhite(ListCell<?> cell) {
    cell.setStyle(
        "-fx-border-color: rgb(250,250,250); "
            + "-fx-border-color: rgb(250,250,250);"
            + "-fx-border-width: 0.5; "
            + "-fx-background-radius: 3; "
            + "-fx-border-radius: 3; ");
  }


  public static void formatElementFieldBlue(Label label) {
    label.setStyle(
        "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +       // text color
            "-fx-font-size: 18;" +
            "-fx-background-color: #332288;" + // background color
            "-fx-padding: 1 1 1 1;" +       // top right bottom left
            "-fx-background-radius: 5;"   // makes corners rounded
    );
  }

  public static void formatElementFieldGreen(Label label) {
    label.setStyle(
        "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +       // text color
            "-fx-font-size: 18;" +
            "-fx-background-color: #117733;" + // background color
            "-fx-padding: 1 1 1 1;" +       // top right bottom left
            "-fx-background-radius: 5;"   // makes corners rounded
    );
  }

  public static void shrinkScrollBar(Control pane) {
    pane.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
      @Override
      public void onChanged(Change<? extends Node> change) {
        while (change.next()) {
          if (change.wasAdded()) {
            change.getAddedSubList().forEach(node -> {
              if (node instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) node;
                bar.setPrefWidth(8);
              }
            });
          }
        }
      }
    });
  }

  public static void requestShrinkScrollBar(ListView<?> view) {
    double prefWidth = 8;
    ScrollBar vertical = (ScrollBar) view.lookup(".scroll-bar:vertical");
    ScrollBar horizontal = (ScrollBar) view.lookup(".scroll-bar:horizontal");
    if (vertical != null && vertical.getPrefWidth() != prefWidth) {
      vertical.setPrefWidth(prefWidth);
    }
    if (horizontal != null && horizontal.getPrefHeight() != prefWidth) {
      horizontal.setPrefHeight(prefWidth);
    }
  }

  public static void makeLabelField(TextField textField) {
    textField.setEditable(false);  // prevents user from changing text
    textField.setFocusTraversable(false); // optional, so Tab skips it
    textField.setBackground(Background.EMPTY); // makes background transparent
    textField.setBorder(Border.EMPTY);        // removes border
    textField.setStyle("-fx-text-fill: black; -fx-font-size: 12;"); // match text style
  }

  public static void showLoading(BorderPane targetPane) {

    List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
    List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
    List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
    boolean hasSelection = !samples.isEmpty() || !selIsotopes.isEmpty() || !selPops.isEmpty();

    Label loadingLabel;
    if (hasSelection) {
      loadingLabel = new Label("Loading data...");
    } else {
      loadingLabel = new Label("No data to show.");
    }
    loadingLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

    AnchorPane pane = new AnchorPane(loadingLabel);
    AnchorPane.setTopAnchor(loadingLabel, 0.0);
    AnchorPane.setBottomAnchor(loadingLabel, 0.0);
    AnchorPane.setLeftAnchor(loadingLabel, 0.0);
    AnchorPane.setRightAnchor(loadingLabel, 0.0);

    loadingLabel.setAlignment(Pos.CENTER);
    loadingLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

    targetPane.setCenter(pane);
  }


  public static boolean isStageOffScreen(Stage stage) {
    Rectangle2D windowBounds = new Rectangle2D(
        stage.getX(),
        stage.getY(),
        stage.getWidth(),
        stage.getHeight()
    );

    // Check against all screens
    for (Screen screen : Screen.getScreens()) {
      Rectangle2D screenBounds = screen.getVisualBounds();
      if (screenBounds.intersects(windowBounds)) {
        // Stage is at least partially on this screen
        return false;
      }
    }

    // No screen intersects → completely off-screen
    return true;
  }


  // #############################################################################################

  public static AnchorPane putOnAnchorWithInsets(Node itemOnAnchor) {
    AnchorPane anchorPane = new AnchorPane(itemOnAnchor);
    anchorPane.setPadding(new Insets(5, 5, 5, 5));
    AnchorPane.setLeftAnchor(itemOnAnchor, 0D);
    AnchorPane.setRightAnchor(itemOnAnchor, 0D);
    AnchorPane.setTopAnchor(itemOnAnchor, 0.0);
    AnchorPane.setBottomAnchor(itemOnAnchor, 0.0);
    return anchorPane;
  }

  public static AnchorPane putOnAnchorWithInsets(Region itemOnAnchor) {
    AnchorPane anchorPane = new AnchorPane(itemOnAnchor);
    anchorPane.setPadding(new Insets(5, 5, 5, 5));
    AnchorPane.setLeftAnchor(itemOnAnchor, 0D);
    AnchorPane.setRightAnchor(itemOnAnchor, 0D);
    AnchorPane.setTopAnchor(itemOnAnchor, 0.0);
    AnchorPane.setBottomAnchor(itemOnAnchor, 0.0);
    return anchorPane;
  }

  public static AnchorPane putOnAnchorWithInsets(Control itemOnAnchor, double insets) {
    AnchorPane anchorPane = new AnchorPane(itemOnAnchor);
    anchorPane.setPadding(new Insets(insets));
    AnchorPane.setLeftAnchor(itemOnAnchor, 0D);
    AnchorPane.setRightAnchor(itemOnAnchor, 0D);
    AnchorPane.setTopAnchor(itemOnAnchor, 0.0);
    AnchorPane.setBottomAnchor(itemOnAnchor, 0.0);
    return anchorPane;
  }

  public static AnchorPane putOnAnchorWithInsets(Pane itemOnAnchor, double insets) {
    AnchorPane anchorPane = new AnchorPane(itemOnAnchor);
    anchorPane.setPadding(new Insets(insets));
    AnchorPane.setLeftAnchor(itemOnAnchor, 0D);
    AnchorPane.setRightAnchor(itemOnAnchor, 0D);
    AnchorPane.setTopAnchor(itemOnAnchor, 0.0);
    AnchorPane.setBottomAnchor(itemOnAnchor, 0.0);
    return anchorPane;
  }


  public static AnchorPane putOnAnchorWithInsets(Pane itemOnAnchor) {
    AnchorPane anchorPane = new AnchorPane(itemOnAnchor);
    anchorPane.setPadding(new Insets(5, 5, 5, 5));
    AnchorPane.setLeftAnchor(itemOnAnchor, 0D);
    AnchorPane.setRightAnchor(itemOnAnchor, 0D);
    AnchorPane.setTopAnchor(itemOnAnchor, 0.0);
    AnchorPane.setBottomAnchor(itemOnAnchor, 0.0);
    return anchorPane;
  }

  public static AnchorPane putOnAnchorWithoutInsets(Node itemOnAnchor) {
    AnchorPane anchorPane = new AnchorPane(itemOnAnchor);
    AnchorPane.setLeftAnchor(itemOnAnchor, 0D);
    AnchorPane.setRightAnchor(itemOnAnchor, 0D);
    AnchorPane.setTopAnchor(itemOnAnchor, 0.0);
    AnchorPane.setBottomAnchor(itemOnAnchor, 0.0);
    return anchorPane;
  }

  // #############################################################################################

  public static Tooltip getDefaultStyleTooltip() {
    Tooltip tip = new Tooltip();
    tip.setStyle("-fx-font-size: 15");
    javafx.util.Duration duration = new Duration(60_000);
    tip.setShowDuration(duration);
    return tip;
  }

  public static void tooltip(Control control, String toolTip) {
    if (control != null && toolTip != null) {
      Tooltip tip = new Tooltip(toolTip);
      tip.setStyle("-fx-font-size: 15");
      javafx.util.Duration duration = new Duration(60_000);
      tip.setShowDuration(duration);
      control.setTooltip(tip);
    }
  }

  public static Tooltip tooltip(String toolTip) {
    String tipString = "Tooltip";
    if (toolTip != null) {
      tipString = toolTip;
    }
    Tooltip tip = new Tooltip(tipString);
    tip.setStyle("-fx-font-size: 15");
    javafx.util.Duration duration = new Duration(60_000);
    tip.setShowDuration(duration);
    return tip;
  }

  // #############################################################################################


  public static MenuItem getImageMenuItem(String text, String resourceString) {
    URL resource = UiUtil.class.getResource(resourceString);
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    MenuItem menuItem;
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(20);
      imageView.setFitWidth(30);
      imageView.setSmooth(true);
      menuItem = new MenuItem(text, imageView);
    } else {
      menuItem = new MenuItem("");
    }
    return menuItem;
  }

  public static Menu getImageMenu(String text, String resourceString) {
    URL resource = UiUtil.class.getResource(resourceString);
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    Menu menuItem;
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(18);
      imageView.setFitWidth(27);
      imageView.setSmooth(true);
      menuItem = new Menu(text, imageView);
    } else {
      menuItem = new Menu("");
    }
    return menuItem;
  }


  public static Button getImageButton(String label, String resourceString, String tooltip) {
    URL resource = UiUtil.class.getResource(resourceString);
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    Button button;
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(18);
      imageView.setFitWidth(27);
      imageView.setSmooth(true);
      button = new Button(label, imageView);
    } else {
      button = new Button(label);
    }
    button.setPrefWidth(90);
    button.setMinWidth(10);
    if (!tooltip.isEmpty()) {
      tooltip(button, tooltip);
    }
    return button;
  }

  public static Button getSquareImageButton(String label, String resourceString, String tooltip) {
    URL resource = UiUtil.class.getResource(resourceString);
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    Button button;
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(18);
      imageView.setFitWidth(18);
      imageView.setSmooth(true);
      button = new Button(label, imageView);
    } else {
      button = new Button(label);
    }
    button.setPrefWidth(90);
    button.setMinWidth(10);
    if (!tooltip.isEmpty()) {
      tooltip(button, tooltip);
    }
    return button;
  }

  public static Button getDecorationButton(String resourceString, String tooltip) {
    URL resource = UiUtil.class.getResource(resourceString);
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    Button button;
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(15);
      imageView.setFitWidth(15);
      imageView.setSmooth(true);
      button = new Button("", imageView);
    } else {
      button = new Button("");
    }
    button.setMaxWidth(20);
    button.setMinWidth(20);
    button.setMaxHeight(20);
    button.setMinHeight(20);
    if (!tooltip.isEmpty()) {
      tooltip(button, tooltip);
    }
    return button;
  }

  public static Button getToolbarBtn(String resourceString, String tooltip) {
    URL resource = UiUtil.class.getResource(resourceString);
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    Button button;
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(18);
      imageView.setFitWidth(27);
      imageView.setSmooth(true);
      button = new Button("", imageView);
    } else {
      button = new Button("");
    }
    button.setMaxWidth(40);
    button.setMinWidth(40);
    button.setMaxHeight(25);
    button.setMinHeight(25);
    if (!tooltip.isEmpty()) {
      tooltip(button, tooltip);
    }
    return button;
  }


  public static ToggleButton getTabBtn(String label, String resourceString,
                                       ToggleGroup toggleGroup) {

    ToggleButton tabBtn = new ToggleButton(label);
    tabBtn.setToggleGroup(toggleGroup);
    tabBtn.setPrefWidth(80);
    tabBtn.setPrefHeight(35);
    tabBtn.setMinHeight(10);
    // tabBtn.setStyle("-fx-font-weight: bold");

    tabBtn.selectedProperty().addListener(new ChangeListener<Boolean>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                          Boolean newValue) {
        if (newValue) {
          tabBtn.setStyle("-fx-font-weight: bold");
        } else {
          tabBtn.setStyle("-fx-font-weight: normal");
        }
      }
    });

    ImageView iv = getViewer(resourceString);
    iv.setFitHeight(22);
    iv.setFitWidth(29);
    tabBtn.setGraphic(iv);

    return tabBtn;
  }

  public static ToggleGroup getTabToggle() {
    ToggleGroup toggleGroup = new ToggleGroup();
    toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
      @Override
      public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue,
                          Toggle newValue) {
        if (newValue == null) {
          toggleGroup.selectToggle(oldValue);
        }
      }
    });
    return toggleGroup;
  }

  // #############################################################################################

  public static Separator createSeparator(double width) {
    Separator s = new Separator(Orientation.VERTICAL);
    s.setPrefWidth(width);
    return s;
  }

// #############################################################################################


  // E.g. STOP button
  public static void addImageOnlyToButton(Button button, String resourceString, String tooltip) {
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    URL resource = UiUtil.class.getResource(resourceString);
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(15);
      imageView.setFitWidth(15);
      imageView.setSmooth(true);
      button.setGraphic(imageView);
    }
    button.setMaxWidth(25);
    button.setMinWidth(25);
    button.setMaxHeight(25);
    button.setMinHeight(25);
    button.setPadding(new Insets(0, 1, 0, 1));
    tooltip(button, tooltip);
  }

  // E.g. PROCESS button
  public static void addImageOnlyToButton(Button button, String label, String resourceString,
                                          String tooltip) {
    // https://copyprogramming.com/howto/java-javafx-load-image-from-resource-folder
    // ListParameterSetFxTemplate.class.getResource("/image/create.png").toString()
    // https://stackoverflow.com/questions/9380690/how-to-see-if-resource-file-exists-in-java
    URL resource = UiUtil.class.getResource(resourceString);
    if (resource != null) {
      Image image = new Image(resource.toString());
      ImageView imageView = new ImageView(image);
      imageView.setFitHeight(15);
      imageView.setFitWidth(15);
      imageView.setSmooth(true);
      button.setText(label);
      button.setGraphic(imageView);
    } else {
      button.setText(label);
    }
    button.setMaxHeight(30);
    button.setMinWidth(25);
    button.setPrefWidth(80);
    tooltip(button, tooltip);
  }

  // #############################################################################################

  public static Image getImage(String resourceString) {
    Image image = null;
    try {
      URL resource = UiUtil.class.getResource(resourceString);
      image = new Image(resource.toString());
    } catch (Exception e) {
      LOGGER.error("Cannot load image");
    }
    return image;
  }


  public static ImageView getLargeViewer(String resourceString) {
    Image image = getImage(resourceString);
    ImageView imageView = new ImageView();
    if (image != null) {
      imageView = new ImageView(image);
      imageView.setFitHeight(25);
      imageView.setFitWidth(40);
      imageView.setSmooth(true);
    }
    return imageView;
  }

  public static ImageView getViewer(String resource) {
    return getViewer(getImage(resource));
  }

  public static ImageView getSquareViewer(String resource) {
    Image image = getImage(resource);
    ImageView imageView = new ImageView();
    if (image != null) {
      imageView = new ImageView(image);
      imageView.setFitHeight(17); //18
      imageView.setFitWidth(17);
      imageView.setSmooth(true);
    }
    return imageView;
  }

  public static ImageView getViewer(Image image) {
    ImageView imageView = new ImageView();
    if (image != null) {
      imageView = new ImageView(image);
      imageView.setFitHeight(17); //18
      imageView.setFitWidth(25);
      imageView.setSmooth(true);
    }
    return imageView;
  }

  // #############################################################################################

  // With a little help from gpt for arrow drawing
  public static AnchorPane getIndentArrowPane(int childLevel) {
    // Define arrow parameters
    double arrowLength = 7.5; // length of arrow line
    double lineWidth = 1.25;    // width of the line
    double arrowHeadSize = 3; // size of the arrow head

    double offset = 1.5 * arrowLength;
    double startY = 5;
    double startX = (childLevel - 1) * offset;

    // Create main line of the arrow
    Line arrowLine = new Line(startX, startY, startX + arrowLength, startY);
    arrowLine.setStrokeWidth(lineWidth);
    arrowLine.setStroke(Color.GRAY);

    // Create arrow head
    double endX = startX + arrowLength;
    double endY = startY;

    Line arrowHead1 = new Line(endX, endY, endX - arrowHeadSize, endY - arrowHeadSize);
    arrowHead1.setStrokeWidth(lineWidth);
    arrowHead1.setStroke(Color.GRAY);

    Line arrowHead2 = new Line(endX, endY, endX - arrowHeadSize, endY + arrowHeadSize);
    arrowHead2.setStrokeWidth(lineWidth);
    arrowHead2.setStroke(Color.GRAY);

    // Add all parts to the pane
    AnchorPane anchorPane = new AnchorPane(arrowLine, arrowHead1, arrowHead2);
    return anchorPane;
  }

  // #############################################################################################

  public static Node getRectangle(AvailableParameterSets category) {
    Rectangle rect = new Rectangle(4, 600);
    Color col = UiColors.BLACK.getFX();
    if (category != null) {
      col = category.getColor();
    }
    rect.setFill(col);
    return rect;
  }

  public static Node getRectangleForListView(AvailableParameterSets category) {
    Node rect;
    if (category != null) {
      rect = category.getShape();
    } else {
      rect = getRectangleForListView(UiColors.BLACK.getFX());
    }
    return rect;
  }

  public static Node getRectangleForListView(Color col) {
    Rectangle rect = new Rectangle(10, 10);
    rect.setFill(col);
    return rect;
  }

  public static Node getCircleForListView(Color col) {
    Ellipse cir = new Ellipse(4, 2);
    cir.setFill(col);
    return cir;
  }

  // #############################################################################################

  public static <T> Node getFlowOfSettings(String label, String graph, T[] options, T choice) {
    FlowPane flow = new FlowPane();
    flow.setHgap(10);

    Label histoTypeLbl = new Label(label);
    histoTypeLbl.setGraphic(UiUtil.getViewer(graph));
    histoTypeLbl.setStyle("-fx-font-weight: bold");
    histoTypeLbl.setGraphicTextGap(5);

    ComboBox<T> typeComboBox = new ComboBox<>();
    typeComboBox.getItems().addAll(options);
    typeComboBox.getSelectionModel().select(choice);
    typeComboBox.setMaxWidth(100);

    flow.getChildren().addAll(histoTypeLbl, typeComboBox);
    return flow;
  }

  // #############################################################################################

  public static List<String> splitByNewlineAndTrim(String label) {
    List<String> parts = new ArrayList<>(Arrays.asList(label.split("\n")));
    parts = parts.stream().map(String::trim).collect(Collectors.toList());
    return parts;
  }

  // Specific: Manage, Filter, Sort, ... a List(Search)View

  /**
   * For the search field in a combined ListView with a SearchField. Note that the reselection call
   * here is slow compared to re-selecting by index.
   */
  public static <T> void filterListAndSearchViewEntries(TextField searchField,
                                                        ListView<FxEntry<T>> listView,
                                                        List<FxEntry<T>> allOptions) {

    // for reselection
    List<T> prevSel = listView.getSelectionModel().getSelectedItems().stream()
        .filter(Objects::nonNull)
        .map(FxEntry::unwrap)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    String searchText = searchField.getText();
    if (searchText.isEmpty()) {
      listView.getItems().clear();
      listView.getItems().addAll(allOptions);
    } else {
      listView.getItems().clear();
      listView.getItems().addAll(allOptions.stream()
          .filter(s -> UiUtil.searchStringIncludeNot(searchText, s.getCellLabelProperty().get()))
          .collect(Collectors.toList()));
    }
    sortListAndSearchView(listView);

    // reselect
    prevSel.forEach(t -> {
      for (FxEntry<T> entry : listView.getItems()) {
        if (entry != null && t != null) {
          if (entry.unwrap().equals(t)) {
            listView.getSelectionModel().select(entry);
            // Reselect for FX-Instances (these are wrappers for the actual object)
          } else if (entry.unwrap() instanceof FxWrapper && t instanceof FxWrapper) {
            if (((FxWrapper) entry.unwrap()).isEqualWrappedObject((FxWrapper) t)) {
              listView.getSelectionModel().select(entry);
            }
          }
        }
      }
    });
  }

  public static <T extends FxWrapper> void reselect(TableView<T> table, List<T> prevSel) {
    if (!prevSel.isEmpty()) {
      // reselect
      prevSel.forEach(selWrapper -> {
        for (T entry : table.getItems()) {
          if (entry != null && selWrapper != null) {
            if (entry.isEqualWrappedObject(selWrapper)) {
              table.getSelectionModel().select(entry);
            }
          }
        }
      });
    } else {
      table.getSelectionModel().selectLast();
    }
  }

  public static <T> void sortListAndSearchView(ListView<FxEntry<T>> listView) {

    // Some cases shall not be sorted.
    if (listView.getItems().stream().allMatch(FxEntry::isSortable)) {

      List<FxEntry<T>> selected = new ArrayList<>();
      List<FxEntry<T>> deselected = new ArrayList<>();
      List<FxEntry<T>> issues = new ArrayList<>();
      List<FxEntry<T>> favourites = new ArrayList<>();

      listView.getItems().forEach(s -> {
        if (s.isDisqualified()) {
          issues.add(s);
        } else if (s.isSelected()) {
          selected.add(s);
        } else if (s.isFavorite()) {
          favourites.add(s);
        } else {
          deselected.add(s);
        }
      });

      // Check if implements the ListableDate interface (then not-null is returned) or not:
      if (listView.getItems().stream().allMatch(FxEntry::hasDate)) {
        Util.dateSortEntry(issues);
        Util.dateSortEntry(selected);
        Util.dateSortEntry(deselected);
        Util.dateSortEntry(favourites);
      } else {
        Util.windowsSortEntry(issues);
        Util.windowsSortEntry(selected);
        Util.windowsSortEntry(deselected);
        Util.windowsSortEntry(favourites);
      }

      // Show selected ones at top
      selected.addAll(issues);

      // then show favourites
      selected.addAll(favourites);

      // then deselected at last
      selected.addAll(deselected);

      /*
       Restore original selection only if fewer than 1000 instances or we get really slow.
       Also see comment below regarding the select() method!
       */
      List<Integer> prevSelIndices = new ArrayList<>();
      if (listView.getSelectionModel().getSelectedItems().size() < 1000) {
        // Contains is probably fastest on HashSets.
        HashSet<FxEntry<T>> previouslySelected = new HashSet<>(
            listView.getSelectionModel().getSelectedItems());
        for (int i = 0; i < selected.size(); i++) {
          if (previouslySelected.contains(selected.get(i))) {
            prevSelIndices.add(i);
          }
        }
      }

      listView.getItems().clear();
      listView.getItems().addAll(selected);

      /*
       Reselection using prevSel.forEach(listView.getSelectionModel()::select);
       becomes quite slow at large list sizes, likely due to indexOf() calls or sth like that.
       Hence: https://stackoverflow.com/questions/44981917/how-do-i-program-select-multiple-items-in-a
       -listview-in-javafx
       */
      listView.getSelectionModel().selectIndices(-1, ArrUtils.integerListToArr(prevSelIndices));
    } else {
      // This should at least refresh cells if selection has changes but sorting has not
      listView.refresh();
    }
  }

  public static boolean searchString(String searchTerm, String candidate) {
    boolean isMatch = false;
    if (searchTerm != null && candidate != null) {
      isMatch = candidate.toLowerCase(Locale.ROOT).contains(
          searchTerm.toLowerCase(Locale.ROOT));
    }
    return isMatch;
  }

  public static boolean searchStringIncludeNot(String searchTerm, String candidate) {
    boolean isMatch = false;
    if (searchTerm != null && candidate != null) {

      // Replace with lower case
      candidate = candidate.toLowerCase(Locale.ROOT);
      searchTerm = searchTerm.toLowerCase(Locale.ROOT);

      // Split by space into words
      String[] searchTermSplit = searchTerm.split(" ");

      List<String> searchTermNotDesired = new ArrayList<>();
      List<String> searchTermIsDesired = new ArrayList<>();

      // Define words starting with a minus sign as in "exclude this", remove the minus
      for (String subString : searchTermSplit) {
        if (subString.startsWith("-")) {
          if (subString.length() > 1) {
            String subWithoutMinus = subString.substring(1);
            searchTermNotDesired.add(subWithoutMinus);
          }
        } else {
          searchTermIsDesired.add(subString);
        }
      }

      String[] candidateSplit = candidate.split(" ");

      // Check if candidate has any match that disqualifies it
      boolean isDisqualified = Arrays.stream(candidateSplit)
          .anyMatch(candidateSubString -> searchTermNotDesired.stream()
              .anyMatch(candidateSubString::contains));

      // Else, check if any match qualifies it
      if (!isDisqualified) {
        // Note: check if there are positive search terms. If not, user just wants to exclude a term!
        if (!searchTermIsDesired.isEmpty()) {
          isMatch = Arrays.stream(candidateSplit)
              .anyMatch(candidateSubStr -> searchTermIsDesired.stream()
                  .anyMatch(candidateSubStr::contains));
        } else {
          // There was a disqualifier that did not match but there was no qualifying search term.
          // --> The user just wants to exclude the disqualifier (which was NOT matched), hence, isMatch!
          isMatch = true;
        }


      }
    }
    return isMatch;
  }

  // #############################################################################################

  // #############################################################################################


  // Special Main Window Controller Popups
  public static void showAboutPopup() {
    NotificationFactory.openInfo(SpTool3Main.ABOUT_STRING
        + SpTool3Main.COAUTHORS_STRING
        + SpTool3Main.AUTHOR_STATEMENT);
  }

  public static void showIsobaricInterferencePopup() {
    StringBuilder builder = new StringBuilder();
    builder.append("Isotopic number\t\tConflicting elements\n");
    HashMap<Integer, Element[]> conflicts = Element.getAllConflictingIsotopicNumbers();
    for (int key : conflicts.keySet()) {
      Element[] elements = conflicts.get(key);
      builder.append(key).append("\t\t\t\t\t");
      for (Element element : elements) {
        Isotope isotope = getIsotope(key, element);

        builder.append(element.getSymbol())
            .append("\t");

        // Say how much it may contribute
        if (isotope != null) {
          builder.append("(").append(SnF.doubleToString(100 * isotope.getAbundance(), NF.D1C1))
              .append("%)").append("\t");
        } else {
          builder.append("(").append("    ")
              .append(")").append("\t");
        }
      }
      builder.append("\n");
    }
    TextArea textArea = new TextArea();
    textArea.setText(builder.toString());
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setPrefSize(900, 500);

    //final Popup popup = PopupFactory.showOnPopup(textArea);
    final Stage stage = PopupFactory.showOnWindow(textArea);
  }

  @Nullable
  private static Isotope getIsotope(int isotopeNumber, Element element) {
    Isotope iso = null;
    for (Isotope isotope : element.getIsotopes()) {
      if (isotope.getIsotopicNumber() == isotopeNumber) {
        iso = isotope;
        break;
      }
    }
    return iso;
  }

  public static void showIsotopePopup() {

    BorderPane isoBorder = new BorderPane();
    isoBorder.setPadding(new Insets(5));

    TextField isoField = new TextField();
    isoField.setPromptText("Enter isotope...");

    TextField massShiftField = new TextField();
    massShiftField.setPromptText("Enter number...");
    massShiftField.setTextFormatter(TextFieldUtils.assurePositiveInteger(0));

    CheckBox doublyChargedBox = new CheckBox("Doubly charged");
    CheckBox dimerBox = new CheckBox("Dimers");
    doublyChargedBox.setSelected(true);
    dimerBox.setSelected(true);

    HBox hBox = new HBox(5, new Label("Isotope name or number"), isoField,
        new Label("Interfering mass shift: "), massShiftField, doublyChargedBox, dimerBox);
    hBox.setAlignment(Pos.CENTER_LEFT);
    hBox.setPrefHeight(30);

    isoBorder.setTop(hBox);

    List<Isotope> isotopes = Element.getAllIsotopes();

    TextArea textArea = new TextArea();
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setPrefSize(900, 500);

    massShiftField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        // cheap trigger
        String ogText = isoField.getText();
        isoField.setText("");
        isoField.setText(ogText);
      }
    });

    doublyChargedBox.setOnAction(e -> {
      // cheap trigger
      String ogText = isoField.getText();
      isoField.setText("");
      isoField.setText(ogText);
    });

    dimerBox.setOnAction(e -> {
      // cheap trigger
      String ogText = isoField.getText();
      isoField.setText("");
      isoField.setText(ogText);
    });

    isoField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observableValue, String s,
                          String t1) {

        StringBuilder builder = new StringBuilder();
        if (t1 != null && !t1.isEmpty()) {

          if (SnF.isValidDoubleSilent(t1)) {
            double isoNum = SnF.strToDouble(t1);
            int shiftNum = SnF.strToInt(massShiftField.getText(), 0);
            boolean accountForDoublyCharged = doublyChargedBox.isSelected();
            boolean accountForDimers = dimerBox.isSelected();

            for (Isotope isotope : isotopes) {
              if (isotope.getIsotopicNumber() == isoNum
                  || (shiftNum > 0 && isotope.getIsotopicNumber() + shiftNum == isoNum)
                  || (accountForDoublyCharged && DoubleMath
                  .fuzzyEquals(isotope.getIsotopicNumber() / 2d, isoNum, 1E-6))
                  || (accountForDimers && DoubleMath
                  .fuzzyEquals(isotope.getIsotopicNumber() * 2, isoNum, 1E-6))
              ) {

                builder.append(String.format("%-6s%-3s", isotope.getIsotopicNumber(),
                    isotope.getElement().getSymbol()));
                builder.append("    "); // extra space

                // Mark label
                String label;
                if (shiftNum > 0 && isotope.getIsotopicNumber() + shiftNum == isoNum) {
                  label = isotope.getElement().getLongName() + " + " + shiftNum;
                } else if (accountForDoublyCharged && DoubleMath
                    .fuzzyEquals(isotope.getIsotopicNumber() / 2d, isoNum, 1E-6)) {
                  label = isotope.getElement().getLongName() + " [2+]";
                } else if (accountForDimers && DoubleMath
                    .fuzzyEquals(isotope.getIsotopicNumber() * 2, isoNum, 1E-6)) {
                  label = isotope.getElement().getLongName() + " [Dimer]";
                } else {
                  label = isotope.getElement().getLongName();
                }
                // fixed width column for name/label
                builder.append(String.format("%-20s", label));

                // Abundance
                builder.append("Abundance [%]: ");
                builder.append(String.format("%16s",
                    SnF.doubleToString(isotope.getAbundance() * 100, NF.D1C3)));

                // Exact mass
                builder.append("\t\tExact mass: ");
                builder.append(String.format("%16s",
                    SnF.doubleToString(isotope.getTheoreticalMass(), NF.D1C6)));
                builder.append("\n");
              }
            }
          } else {
            // Assume you want to learn about an element
            List<Element> matches = new ArrayList<>();

            for (Element element : Element.values()) {
              if (element.getSymbol().toLowerCase(Locale.ROOT)
                  .equals(t1.toLowerCase(Locale.ROOT))) {
                matches.add(element);
              }
            }

            if (matches.isEmpty()) {
              for (Element element : Element.values()) {
                if (element.getLongName().toLowerCase(Locale.ROOT)
                    .contains(t1.toLowerCase(Locale.ROOT))) {
                  matches.add(element);
                }
              }
            }

            for (Element element : matches) {
              builder.append(element.getSymbol())
                  .append("\t")
                  .append(element.getLongName());

              for (Isotope isotope : element.getIsotopes()) {
                builder.append("\n\t\t")
                    .append(isotope.getIsotopicNumber())
                    .append("\t")
                    .append("Abundance [%]:")
                    .append("\t")
                    .append(SnF.doubleToString(isotope.getAbundance() * 100, NF.D1C3))
                    .append("\t")
                    .append("Exact mass:")
                    .append("\t")
                    .append(SnF.doubleToString(isotope.getTheoreticalMass(), NF.D1C6));
              }
              builder.append("\n");

            }

          }
        }
        textArea.setText(builder.toString());
      }
    });

    isoBorder.setCenter(textArea);

    //final Popup popup = PopupFactory.showOnPopup(isoBorder);
    final Stage stage = PopupFactory.showOnWindow(isoBorder);

  }

  public static void showInterferencePopup() {

    BorderPane isoBorder = new BorderPane();
    isoBorder.setPadding(new Insets(5));

    TextField mzField = new TextField();
    mzField.setPromptText("Enter mz...");
    mzField.setTextFormatter(TextFieldUtils.assurePositiveDouble(56d));

    TextField analyteField = new TextField();
    analyteField.setPromptText("Enter analyte...");
    analyteField.setTextFormatter(TextFieldUtils.assureLetter("Fe"));

    HBox hBox = new HBox(5, new Label("Analyte"), analyteField,
        new Label("Analyte mz "), mzField);
    hBox.setAlignment(Pos.CENTER_LEFT);

    isoBorder.setTop(hBox);

    TextArea textArea = new TextArea();
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setPrefSize(900, 500);

    List<InterferenceEntry> entries = SpTool3Main.getRunTime().getInterferenceDatabase();

    analyteField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        StringBuilder builder = new StringBuilder(InterferenceDatabase.SOURCE + "\n");

        String analyteStr = analyteField.getText();

        for (InterferenceEntry interf : entries) {
          if (interf.analyteElementIon.equals(analyteStr)) {
            builder.append(interf.toString())
                .append("\n");
          }
        }

        textArea.setText(builder.toString());
      }
    });

    mzField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        StringBuilder builder = new StringBuilder(InterferenceDatabase.SOURCE + "\n");

        // remove the ".0" part in "23.0"
        String analyteStr = Integer.toString((int) SnF.strToDouble(mzField.getText()));
        // Include adjacent masses in case of half-masses
        String floor = Integer.toString((int) Math.floor(SnF.strToDouble(mzField.getText())));
        String ceil = Integer.toString((int) Math.ceil(SnF.strToDouble(mzField.getText())));

        for (InterferenceEntry interf : entries) {
          boolean isDoublyCharged = interf.ionType.equals("doubly charged")
              && (interf.mz.equals(floor) || interf.mz.equals(ceil));

          if (interf.mz.equals(analyteStr) || isDoublyCharged) {
            builder.append(interf.toString())
                .append("\n");
          }
        }

        textArea.setText(builder.toString());
      }
    });

    isoBorder.setCenter(textArea);

    //final Popup popup = PopupFactory.showOnPopup(isoBorder);
    final Stage stage = PopupFactory.showOnWindow(isoBorder);
  }

  public static void showZAlphaConversionPopup() {
    BorderPane isoBorder = new BorderPane();
    isoBorder.setPadding(new Insets(5));

    TextField zField = new TextField();
    zField.setPromptText("Enter z-value [-]...");
    zField.setPrefWidth(200);

    TextField alphaField = new TextField();
    alphaField.setPromptText("Enter alpha-value [-]...");
    alphaField.setPrefWidth(200);

    Label alphaLabel = new Label("Alpha [-]");
    Label zLabel = new Label("z [-]");
    alphaLabel.setPrefWidth(50);
    zLabel.setPrefWidth(50);

    VBox vBox = new VBox(5, new Label("Convert between z-value ('z·σ') to alpha-value."),
        new HBox(10, zLabel, zField),
        new HBox(10, alphaLabel, alphaField));
    vBox.setAlignment(Pos.CENTER_LEFT);
    isoBorder.setCenter(vBox);

    TextFormatter<Double> zFormatter = TextFormatterSupplier
        .get(TextFormatterOption.ASSURE_POSITIVE_DOUBLE, 3d);
    zField.setTextFormatter(zFormatter);

    TextFormatter<Double> alphaFormatter = TextFormatterSupplier
        .get(TextFormatterOption.ASSURE_POS_EXP_DOUBLE, 1E-10);
    alphaField.setTextFormatter(alphaFormatter);


    // Change Listener.
    PauseTransition zFieldPause = new PauseTransition(Duration.seconds(0.2));
    AtomicReference<String> zFieldPauseNewValue = new AtomicReference<>("");
    zFieldPause.setOnFinished(event -> {
      if (zField.isFocused() && SnF.isValidDouble(zFieldPauseNewValue.get())) {
        double z = SnF.strToDouble(zFieldPauseNewValue.get());
        double alpha = Statistics.zToAlpha(z);
        String alphaStr = SnF.doubleFldToString(alpha, NF.D1C6Exp);
        alphaField.setText(alphaStr);
      }
    });

    zField.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          zFieldPause.stop();
          zFieldPauseNewValue.set(newValue);
          zFieldPause.playFromStart();
        }
    );

    PauseTransition alphaFieldPause = new PauseTransition(Duration.seconds(0.2));
    AtomicReference<String> alphaFieldPauseNewValue = new AtomicReference<>("");
    alphaFieldPause.setOnFinished(event -> {
      if (alphaField.isFocused() && SnF.isValidDouble(alphaFieldPauseNewValue.get())) {
        double alpha = SnF.strToDouble(alphaFieldPauseNewValue.get());
        double z = Statistics.alphaToZ(alpha);
        String zStr = SnF.doubleFldToString(z, NF.D1C4);
        zField.setText(zStr);
      }
    });
    alphaField.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          alphaFieldPause.stop();
          alphaFieldPauseNewValue.set(newValue);
          alphaFieldPause.playFromStart();
        }
    );

    // final Popup popup = PopupFactory.showOnPopup(isoBorder);
    final Stage stage = PopupFactory.showOnWindow(isoBorder);
    // We do not really need the result
  }


  public static void showIsotopeSignalConversionPopup() {
    BorderPane isoBorder = new BorderPane();
    isoBorder.setPadding(new Insets(5));

    AtomicReference<Isotope> currentIsotope = new AtomicReference<>(Element.H.getMostAbundant());

    TextField isoField = new TextField();
    isoField.setPromptText("Enter isotope ('13C')");
    isoField.setPrefWidth(200);

    TextField isoSignalField = new TextField();
    isoSignalField.setPromptText("Signal level");
    isoSignalField.setPrefWidth(200);

    TextField totalSignalField = new TextField();
    totalSignalField.setPromptText("");
    totalSignalField.setPrefWidth(200);

    Label isotopeLbl = new Label("Isotope");
    Label isoSignalLbl = new Label("Isotope signal");
    Label totalSignalLbl = new Label("Total signal");
    Label parsedIsotopeLabel = new Label("Parsed isotope");
    Label parsedIsotopeResult = new Label("...");
    isotopeLbl.setPrefWidth(90);
    isoSignalLbl.setPrefWidth(90);
    parsedIsotopeLabel.setPrefWidth(90);
    totalSignalLbl.setPrefWidth(90);

    VBox vBox = new VBox(5, new Label("Calculate total required signal from isotope signal."),
        new HBox(10, isotopeLbl, isoField),
        new HBox(10, parsedIsotopeLabel, parsedIsotopeResult),
        new HBox(10, isoSignalLbl, isoSignalField),
        new HBox(10, totalSignalLbl, totalSignalField));
    vBox.setAlignment(Pos.CENTER_LEFT);
    isoBorder.setCenter(vBox);

    TextFormatter<Double> isoSignalFormatter = TextFormatterSupplier
        .get(TextFormatterOption.ASSURE_POS_EXP_DOUBLE, 1d);
    isoSignalField.setTextFormatter(isoSignalFormatter);


    // Change Listener.
    PauseTransition isoPause = new PauseTransition(Duration.seconds(0.2));
    AtomicReference<String> isoPauseValue = new AtomicReference<>("");
    isoPause.setOnFinished(event -> {
      if (isoField.isFocused()) {
        Isotope isotope = Isotope.guessFromString(isoPauseValue.get());
        if (isotope != null && !isotope.equals(Element.UNKNOWN.getMostAbundant())) {
          currentIsotope.set(isotope);
          parsedIsotopeResult.setText(isotope.getFullUIName());
        }
      }
    });
    isoField.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          isoPause.stop();
          isoPauseValue.set(newValue);
          isoPause.playFromStart();
        }
    );

    PauseTransition isoSignalPause = new PauseTransition(Duration.seconds(0.2));
    AtomicReference<String> isoSignalPauseValue = new AtomicReference<>("");
    isoSignalPause.setOnFinished(event -> {
      if (isoSignalField.isFocused() && SnF.isValidDouble(isoSignalPauseValue.get())) {
        double isoSignal = SnF.strToDouble(isoSignalPauseValue.get());
        isoSignal = isoSignal / currentIsotope.get().getAbundance();
        totalSignalField.setText(SnF.doubleToString(isoSignal, NF.D1C1));
      }
    });

    isoSignalField.textProperty().addListener(
        (observable, oldValue, newValue) -> {
          isoSignalPause.stop();
          isoSignalPauseValue.set(newValue);
          isoSignalPause.playFromStart();
        }
    );

    // final Popup popup = PopupFactory.showOnPopup(isoBorder);
    final Stage stage = PopupFactory.showOnWindow(isoBorder);
    // We do not really need the result
  }

  // #############################################################################################

  // #############################################################################################


}
