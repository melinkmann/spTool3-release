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

package visualizer;


import gui.util.UiUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import processing.options.LogLevel;

public class TextAreaAppender extends AbstractAppender {

  private static final Logger LOGGER = LogManager.getLogger(TextAreaAppender.class);

  private static ComboBox<LogLevel> logLevelComboBox = new ComboBox<>();
  private static TextFlow textFlow;
  private static volatile Level currentLevel = Level.INFO; // Default log level

  public static void setTextFlow(TextFlow flow) {
    textFlow = flow;
  }

  protected TextAreaAppender(String name, Layout<? extends Serializable> layout,
      boolean ignoreExceptions) {
//    super(name, null, layout, ignoreExceptions);
    super(name, null, layout, ignoreExceptions, Property.EMPTY_ARRAY);
  }

  @PluginFactory
  public static TextAreaAppender createAppender(
      @PluginAttribute("name") String name,
      @PluginElement("Layout") Layout<? extends Serializable> layout) {

    if (name == null) {
      LOGGER.error("No name provided for TextAreaAppender");
      return null;
    }

    // Default layout with timestamp, level, logger, and message
    if (layout == null) {
      layout = PatternLayout.newBuilder()
          .withPattern("%d{yyyy-MM-dd HH:mm:ss} %5p %c{1}:%L - %m%n")
          .build();
    }

    return new TextAreaAppender(name, layout, true);
  }

  @Override
  public void append(LogEvent event) {
    if (textFlow != null) {
      if (event.getLevel().isMoreSpecificThan(currentLevel)) {
        String message = new String(getLayout().toByteArray(event));
        message += "\n";
        Text textNode = new Text(message);

        // Apply colors based on log level
        if (event.getLevel() == Level.INFO) {
          textNode.setStyle("-fx-fill: rgb(4, 4, 150); -fx-font-weight: bold;");   // INFO is orange
        } else if (event.getLevel() == Level.WARN) {
          textNode.setStyle("-fx-fill: rgb(220, 145, 0); -fx-font-weight: bold;");
        } else if (event.getLevel() == Level.ERROR) {
          textNode.setStyle("-fx-fill: red; -fx-font-weight: bold;");  // ERROR is red and bold
        } else if (event.getLevel() == Level.FATAL) {
          textNode.setStyle("-fx-fill: rgb(255, 30, 145); -fx-font-weight: bold;");
        } else if (event.getLevel() == Level.DEBUG) {
          textNode.setStyle("-fx-fill: black;");  // DEBUG is blue
        } else if (event.getLevel() == Level.TRACE) {
          textNode.setStyle("-fx-fill: rgb(70,70,70);");  // TRACE is cyan
        } else {
          textNode.setStyle("-fx-fill: black;");  // Default color for other cases
        }

        // Update TextArea on the JavaFX Application Thread
        Platform.runLater(() -> {
          int maxEntries = (int) 1E4;
          int reducedListLength = 1000;
          int reducedIndex = maxEntries - reducedListLength;

          // prevent RAM overflow
          if (textFlow.getChildren().size() > maxEntries) {

            List<Node> allNodes = new ArrayList<>(textFlow.getChildren());

            // Either way, keep last 1000
            List<Node> keepAnyway = allNodes.subList(reducedIndex, allNodes.size());
            List<Node> discardCandidates = allNodes.subList(0, reducedIndex);
            List<Node> highLevelNodes = new ArrayList<>();

            // Also keep all info and above
            List<Node> keepNodes = new ArrayList<>();

            for (Node child : discardCandidates) {
              if (child instanceof Text) {
                Text text = (Text) child;
                if (text.getText().toLowerCase(Locale.ROOT).contains("info")
                    || text.getText().toLowerCase(Locale.ROOT).contains("warn")
                    || text.getText().toLowerCase(Locale.ROOT).contains("error")
                    || text.getText().toLowerCase(Locale.ROOT).contains("fatal")) {
                  highLevelNodes.add(child);
                }
              }
            }

            // If we're trying to keep a total of 5E3, the entire removal is senseless.
            // Hence, cleanup the list of 'they should be removed but we kept them because
            // of their high log level. Reduce the list.
            if (highLevelNodes.size() + keepAnyway.size() >= maxEntries) {
              highLevelNodes = highLevelNodes.subList(highLevelNodes.size() - reducedListLength,
                  highLevelNodes.size());
            }

            keepNodes.addAll(highLevelNodes);
            keepNodes.addAll(keepAnyway);

            textFlow.getChildren().clear();
            textFlow.getChildren().addAll(keepNodes);
          }
          textFlow.getChildren().add(textNode);
        });
      }
    }
  }

  // This sets the actual level that is used by the text appender to check if it needs to be logged.
  public static void setLogLevel(Level level) {
    currentLevel = level; // Update the log level filter
  }

  // From the default settings
  public static void setComboBoxLevel(LogLevel level) {
    logLevelComboBox.setValue(level);
    setLogLevel(level.getLevel()); // also update the current level
  }

  public static void build(BorderPane logTabPane) {
    // Logger
    // Create a TextArea for displaying logs
    TextFlow logTextFlow = new TextFlow();

    MenuItem clearMenu = UiUtil.getImageMenuItem("Clear", "/img/delete.png");
    clearMenu.setOnAction(e -> {
      Platform.runLater(() -> {
        logTextFlow.getChildren().clear();
      });
    });

    Button clearButton = UiUtil.getToolbarBtn("/img/delete.png", "Clear log");
    clearButton.setOnAction(event -> {
      logTextFlow.getChildren().clear();
    });

    // Create a ComboBox for log level selection
    logLevelComboBox.getItems().addAll(LogLevel.values());

    logLevelComboBox.setValue(LogLevel.INFO); // Default level

    // Handle level selection
    logLevelComboBox.setOnAction(event -> {
      Level selectedLevel = logLevelComboBox.getValue().getLevel();
      TextAreaAppender.setLogLevel(selectedLevel);

      LOGGER.trace("Demo for logger trace level.");
      LOGGER.debug("Demo for logger debug level.");
      LOGGER.info("Demo for logger info level.");
      LOGGER.warn("Demo for logger warn level.");
      LOGGER.error("Demo for logger error level.");
      LOGGER.fatal("Demo for logger fatal level.");
    });

    // Set the custom appender to the TextArea
    TextAreaAppender.setTextFlow(logTextFlow);

    // Configure Log4j 2 programmatically
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    Configuration config = context.getConfiguration();

    // Create the TextAreaAppender with the desired layout and filter
    TextAreaAppender appender = TextAreaAppender.createAppender("TextAreaAppender", null);
    if (appender != null) {
      appender.start();
    }

    // Attach the appender to the root logger
    config.getRootLogger().addAppender(appender, Level.TRACE, null);
    context.updateLoggers();

    HBox buttonBar = new HBox(5,
        new Label("Logger level"),
        logLevelComboBox,
        new Separator(Orientation.VERTICAL),
        clearButton);
    buttonBar.setAlignment(Pos.CENTER_LEFT);
    logTabPane.setBottom(buttonBar);

    // Wrap the TextFlow in a ScrollPane
    ScrollPane scrollPane = new ScrollPane();
    scrollPane.setContent(logTextFlow);
    scrollPane.setFitToWidth(true); // Ensure content width matches the ScrollPane width
    scrollPane.setPrefHeight(600); // Set preferred height to prevent vertical growth
    // Show scrollbar only when needed
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

//    scrollPane.setStyle(
//        "-fx-border-color: rgb(250,250,250); " +
//            "-fx-padding: 5;" +
//            "-fx-border-width: 1;" +
//            "-fx-background-radius: 3;" +
//            "-fx-border-radius: 3;"
//    );

    UiUtil.formatScrollPane(scrollPane);
    UiUtil.makePaneRound(scrollPane);

    // Auto-scroll to the bottom when TextFlow changes
    logTextFlow.getChildren()
        .addListener((javafx.collections.ListChangeListener.Change<?> change) -> {
          Platform.runLater(() -> {
            // Scroll to bottom unless clicked on (annoying when trying to read))
            if (!scrollPane.isFocused()) {
              scrollPane.setVvalue(scrollPane.getVmax());
            }
          });
        });

    logTabPane.setCenter(UiUtil.putOnAnchorWithInsets(scrollPane));
  }


}

// WORKS!
//import javafx.application.Platform;
//import javafx.scene.control.TextArea;
//import org.apache.logging.log4j.core.*;
//import org.apache.logging.log4j.core.appender.AbstractAppender;
//import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
//import org.apache.logging.log4j.core.config.plugins.PluginElement;
//import org.apache.logging.log4j.core.config.plugins.PluginFactory;
//import org.apache.logging.log4j.core.layout.PatternLayout;
//import org.apache.logging.log4j.core.util.Throwables;
//
//import java.io.Serializable;
//
//public class TextAreaAppender extends AbstractAppender {
//  private static TextArea textArea;
//
//  public static void setTextArea(TextArea area) {
//    textArea = area;
//  }
//
//  protected TextAreaAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
//    super(name, filter, layout, ignoreExceptions);
//  }
//
//  @PluginFactory
//  public static TextAreaAppender createAppender(
//      @PluginAttribute("name") String name,
//      @PluginElement("Filter") Filter filter,
//      @PluginElement("Layout") Layout<? extends Serializable> layout) {
//
//    if (name == null) {
//      LOGGER.error("No name provided for TextAreaAppender");
//      return null;
//    }
//
//    if (layout == null) {
//      layout = PatternLayout.createDefaultLayout();
//    }
//
//    return new TextAreaAppender(name, filter, layout, true);
//  }
//
//  @Override
//  public void append(LogEvent event) {
//    if (textArea != null) {
//      String message = new String(getLayout().toByteArray(event));
//      // Update TextArea on the JavaFX Application Thread
//      Platform.runLater(() -> textArea.appendText(message));
//    }
//  }
//}