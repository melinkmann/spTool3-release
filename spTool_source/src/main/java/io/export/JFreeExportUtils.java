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

package io.export;

import core.SpTool3Main;
import gui.util.TextFieldUtils;
import io.GlobalIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Pair;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.ExportUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import processing.parameters.DoubleParameter;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;
import processing.parameters.StringParameter;
import util.NF;
import util.SnF;
import util.Util;

public abstract class JFreeExportUtils {

  public static final Logger LOGGER = LogManager.getLogger(JFreeExportUtils.class.getName());

  public static void makeExportable(ChartViewer chartViewer, Pane legend) {

    Menu exportGraphic = new Menu("Save graphic");
    Menu exportGraphicAs = new Menu("Save graphic as");
    MenuItem exportData = new MenuItem("Export data");
    MenuItem exportDataAs = new MenuItem("Export data as");
    MenuItem toClipboard = new MenuItem("Copy data to clipboard");
    MenuItem plotToClipboard = new MenuItem("Copy plot to clipboard");
    MenuItem legendToClipboard = new MenuItem("Copy legend to clipboard");
    MenuItem imageToClipboard = new MenuItem("Copy plot and legend to clipboard");

    final TextField nameField = new TextField();
    final TextField widthField = new TextField();
    final TextField heightField = new TextField();

    HBox widthBox = createLabeledTextField("Width:", widthField,
        SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters().getDefaultGraphWidth());
    CustomMenuItem widthItem = new CustomMenuItem(widthBox, false); // 'false' keeps it open

    HBox heightBox = createLabeledTextField("Height:", heightField,
        SpTool3Main.getRunTime().getGuiParameterManager().getLayoutParameters().getDefaultGraphHeight());
    CustomMenuItem heightItem = new CustomMenuItem(heightBox, false); // 'false' keeps it open

    HBox nameBox = createLabeledTextField("Name:", nameField, extractFileName(chartViewer));
    CustomMenuItem nameItem = new CustomMenuItem(nameBox, false); // 'false' keeps it open

    chartViewer.getContextMenu().getItems().clear();
    chartViewer.getContextMenu().getItems().addAll(
        exportData,
        exportDataAs,
        toClipboard,
        exportGraphic,
        exportGraphicAs,
        plotToClipboard,
        legendToClipboard,
        imageToClipboard,
        nameItem,
        widthItem,
        heightItem
    );

    exportData.setOnAction(e -> {
      Path folder = GlobalIO.makeExportDataFolder();
      if (folder != null) {
        String folderName = folder.toFile().toString();
        String fileName = getNameFromField(nameField) + ".csv";
        Path path = Path.of(folderName);
        path = path.resolve(fileName);
        File file = path.toFile();

        ExportWriter exportWriter = new CsvExportWriter(file);
        writeDataAsTable(chartViewer.getChart(), exportWriter);
        LOGGER.info("Wrote file: " + file);
      }
    });

    exportDataAs.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Export to csv");
      FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
          "Comma separated values (csv)", "*.csv");
      chooser.getExtensionFilters().add(filter);
      File file = chooser.showSaveDialog(chartViewer.getScene().getWindow());
      if (file != null) {

        ExportWriter exportWriter = new CsvExportWriter(file);
        writeDataAsTable(chartViewer.getChart(), exportWriter);
        LOGGER.info("Wrote file: " + file);
      }
    });

    toClipboard.setOnAction(e -> {
      ExportWriter clipboardWriter = new ClipboardWriter();
      writeDataAsTable(chartViewer.getChart(), clipboardWriter);
    });


    imageToClipboard.setOnAction(e -> {
      handleExportToClipboard(chartViewer, legend, widthField, heightField);
    });

    plotToClipboard.setOnAction(e -> {
      handleExportToClipboard(chartViewer, widthField, heightField);
    });

    legendToClipboard.setOnAction(e -> {
      handleExportToClipboard(legend);
    });

    /*
    SAVE AS
     */
    MenuItem pngSaveAsItem = new MenuItem("PNG...");
    pngSaveAsItem.setOnAction(e -> handleExportToPNG(chartViewer, legend, widthField, heightField));
    exportGraphicAs.getItems().add(pngSaveAsItem);

    MenuItem jpegSaveAsItem = new MenuItem("JPEG...");
    jpegSaveAsItem
        .setOnAction(e -> handleExportToJPEG(chartViewer, legend, widthField, heightField));
    exportGraphicAs.getItems().add(jpegSaveAsItem);

    if (ExportUtils.isOrsonPDFAvailable()) {
      MenuItem pdfSaveAsItem = new MenuItem("PDF...");
      pdfSaveAsItem
          .setOnAction(e -> handleExportToPDF(chartViewer, legend, widthField, heightField));
      exportGraphicAs.getItems().add(pdfSaveAsItem);
    }
    if (ExportUtils.isJFreeSVGAvailable()) {
      MenuItem svgSaveAsItem = new MenuItem("SVG...");
      svgSaveAsItem
          .setOnAction(e -> handleExportToSVG(chartViewer, legend, widthField, heightField));
      exportGraphicAs.getItems().add(svgSaveAsItem);
    }

    /*
    SAVE
     */
    MenuItem pngSaveDefaultItem = new MenuItem("PNG...");
    pngSaveDefaultItem.setOnAction(
        e -> handleDefaultExportToPNG(chartViewer, legend, nameField, widthField, heightField));
    exportGraphic.getItems().add(pngSaveDefaultItem);

    MenuItem jpegSaveDefaultItem = new MenuItem("JPEG...");
    jpegSaveDefaultItem.setOnAction(
        e -> handleDefaultExportToJPEG(chartViewer, legend, nameField, widthField, heightField));
    exportGraphic.getItems().add(jpegSaveDefaultItem);

    if (ExportUtils.isOrsonPDFAvailable()) {
      MenuItem pdfSaveDefaultItem = new MenuItem("PDF...");
      pdfSaveDefaultItem.setOnAction(
          e -> handleDefaultExportToPDF(chartViewer, legend, nameField, widthField, heightField));
      exportGraphic.getItems().add(pdfSaveDefaultItem);
    }
    if (ExportUtils.isJFreeSVGAvailable()) {
      MenuItem svgSaveDefaultItem = new MenuItem("SVG...");
      svgSaveDefaultItem.setOnAction(
          e -> handleDefaultExportToSVG(chartViewer, legend, nameField, widthField, heightField));
      exportGraphic.getItems().add(svgSaveDefaultItem);
    }

    // Keep pdf, ... export

    // Custom menu item with size!

    // PDF, PNG,

    // Export all Series as new file using the dialog

    // Export all series to default path with sample name and current date

    // All Series to clipboard
  }

  /// SAVE TO DEFAULT EXPORT PATH

  /**
   * A handler for the export to SVG option in the context menu.
   */
  private static void handleDefaultExportToSVG(ChartViewer chartViewer, Pane legend,
                                               TextField nameField,
                                               TextField widthField,
                                               TextField heightField) {
    Path folder = GlobalIO.makeExportGraphFolder();
    if (folder != null) {
      String folderName = folder.toFile().toString();
      String fileName = getNameFromField(nameField) + ".svg";
      Path path = Path.of(folderName);
      path = path.resolve(fileName);
      File file = path.toFile();
      ExportUtils.writeAsSVG(chartViewer.getCanvas().getChart(),
          getWidth(chartViewer, widthField),
          getHeight(chartViewer, heightField), file);
      LOGGER.info("Wrote file: " + file);
      //
      String legendFileName = getNameFromField(nameField) + "_KEY_" + ".png";
      Path legendPath = Path.of(folderName);
      legendPath = legendPath.resolve(legendFileName);
      File legendFile = legendPath.toFile();
      saveAsPNG(legend, legendFile);
    }
  }

  /**
   * A handler for the export to PNG option in the context menu.
   */
  private static void handleDefaultExportToPNG(ChartViewer chartViewer, Pane legend,
                                               TextField nameField,
                                               TextField widthField,
                                               TextField heightField) {
    Path folder = GlobalIO.makeExportGraphFolder();
    if (folder != null) {
      String folderName = folder.toFile().toString();
      String fileName = getNameFromField(nameField) + ".png";
      Path path = Path.of(folderName);
      path = path.resolve(fileName);
      File file = path.toFile();
      try {
        // ExportUtils
        //     .writeAsPNG(chartViewer.getCanvas().getChart(), getWidth(chartViewer, widthField),
        //         getHeight(chartViewer, heightField), file);
        writeAsPNGHighDPI(
            chartViewer.getCanvas().getChart(),
            getWidth(chartViewer, widthField),   // logical size, e.g. 750
            getHeight(chartViewer, heightField), // logical size, e.g. 500
            SpTool3Main.getRunTime().getConfParams().getImageExportDpiLevelFloat(), // 3x = ~288 DPI. 10.0f
            file);
        LOGGER.info("Wrote file: " + file);
      } catch (IOException ex) {
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }

      String legendFileName = getNameFromField(nameField) + "_KEY_" + ".png";
      Path legendPath = Path.of(folderName);
      legendPath = legendPath.resolve(legendFileName);
      File legendFile = legendPath.toFile();
      saveAsPNG(legend, legendFile);
    }
  }

  //  private static void saveAsPNG(Pane flowPane, File file) {
  //    try {
  //      WritableImage writableImage = new WritableImage((int) flowPane.getWidth(),
  //          (int) flowPane.getHeight());
  //      flowPane.snapshot(new SnapshotParameters(), writableImage);
  //
  //      // Convert WritableImage to BufferedImage manually
  //      BufferedImage bufferedImage = new BufferedImage((int) flowPane.getWidth(),
  //          (int) flowPane.getHeight(), BufferedImage.TYPE_INT_ARGB);
  //      PixelReader pixelReader = writableImage.getPixelReader();
  //
  //      for (int y = 0; y < bufferedImage.getHeight(); y++) {
  //        for (int x = 0; x < bufferedImage.getWidth(); x++) {
  //          int argb = pixelReader.getArgb(x, y);
  //          bufferedImage.setRGB(x, y, argb);
  //        }
  //      }
  //
  //      // Save BufferedImage as PNG
  //      try {
  //        ImageIO.write(bufferedImage, "png", file);
  //        LOGGER.info("Saved legend to: " + file.getAbsolutePath());
  //      } catch (IOException e) {
  //        LOGGER.error("Cannot write. " + ExceptionUtils.getStackTrace(e));
  //      }
  //    } catch (Exception e) {
  //      LOGGER.error("Cannot create image. " + ExceptionUtils.getStackTrace(e));
  //    }
  //  }


  // Claude Sonnet 4.6
  private static void saveAsPNG(Pane flowPane, File file) {
    try {
      double scale = 5;
      int width = (int) Math.ceil(flowPane.getWidth() * scale);
      int height = (int) Math.ceil(flowPane.getHeight() * scale);

      SnapshotParameters params = new SnapshotParameters();
      params.setTransform(javafx.scene.transform.Scale.scale(scale, scale));

      WritableImage writableImage = new WritableImage(width, height);
      flowPane.snapshot(params, writableImage);

      BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      PixelReader pixelReader = writableImage.getPixelReader();
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
        }
      }

      ImageIO.write(bufferedImage, "png", file);
      LOGGER.info("Saved legend to: " + file.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.error("Cannot write. " + ExceptionUtils.getStackTrace(e));
    } catch (Exception e) {
      LOGGER.error("Cannot create image. " + ExceptionUtils.getStackTrace(e));
    }
  }

  // Claude Sonnet 4.6
  private static void writeAsPNGHighDPI(JFreeChart chart, int w, int h, float dpiScale, File file)
      throws IOException {
    int pixelW = Math.round(w * dpiScale);
    int pixelH = Math.round(h * dpiScale);

    BufferedImage image = new BufferedImage(pixelW, pixelH, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();

    // High quality rendering
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

    // Scale everything — fonts, lines, markers — proportionally
    g2.scale(dpiScale, dpiScale);

    // Draw at logical size; scaling handles the rest
    chart.draw(g2, new Rectangle(w, h));
    g2.dispose();

    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      ImageIO.write(image, "png", out);
    }
  }


  //  private static void saveAsJpeg(Pane flowPane, File file) {
  //    try {
  //      int width = (int) flowPane.getWidth();
  //      int height = (int) flowPane.getHeight();
  //
  //      // Capture snapshot as WritableImage
  //      WritableImage writableImage = new WritableImage(width, height);
  //      flowPane.snapshot(new SnapshotParameters(), writableImage);
  //
  //      // Convert WritableImage to BufferedImage manually
  //      BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
  //      PixelReader pixelReader = writableImage.getPixelReader();
  //
  //      // Set background color for transparency replacement
  //      Color backgroundColor = Color.WHITE; // Change to any background color if needed
  //      int bgRgb = new java.awt.Color((float) backgroundColor.getRed(),
  //          (float) backgroundColor.getGreen(),
  //          (float) backgroundColor.getBlue()).getRGB();
  //
  //      for (int y = 0; y < height; y++) {
  //        for (int x = 0; x < width; x++) {
  //          int argb = pixelReader.getArgb(x, y);
  //
  //          // Extract alpha channel (transparency)
  //          int alpha = (argb >> 24) & 0xFF;
  //
  //          // Convert ARGB to RGB by removing transparency (using background color)
  //          int rgb = (alpha == 0) ? bgRgb : (argb & 0xFFFFFF);
  //          bufferedImage.setRGB(x, y, rgb);
  //        }
  //      }
  //
  //      // Save BufferedImage as JPEG
  //      try {
  //        ImageIO.write(bufferedImage, "jpg", file);
  //        LOGGER.info("JPEG file saved: " + file.getAbsolutePath());
  //      } catch (IOException e) {
  //        LOGGER.error("Cannot write. " + ExceptionUtils.getStackTrace(e));
  //      }
  //    } catch (Exception e) {
  //      LOGGER.error("Cannot create image. " + ExceptionUtils.getStackTrace(e));
  //    }
  //  }

  // Claude sonnet 4.6
  private static void saveAsJpeg(Pane flowPane, File file) {
    try {
      double scale = 5;
      int width = (int) Math.ceil(flowPane.getWidth() * scale);
      int height = (int) Math.ceil(flowPane.getHeight() * scale);

      SnapshotParameters params = new SnapshotParameters();
      params.setTransform(javafx.scene.transform.Scale.scale(scale, scale));

      WritableImage writableImage = new WritableImage(width, height);
      flowPane.snapshot(params, writableImage);

      BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      PixelReader pixelReader = writableImage.getPixelReader();

      int bgRgb = java.awt.Color.WHITE.getRGB();
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int argb = pixelReader.getArgb(x, y);
          int alpha = (argb >> 24) & 0xFF;
          bufferedImage.setRGB(x, y, (alpha == 0) ? bgRgb : (argb & 0xFFFFFF));
        }
      }

      float quality = 0.99f;
      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(quality);
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
        writer.setOutput(ImageIO.createImageOutputStream(out));
        writer.write(null, new IIOImage(bufferedImage, null, null), param);
        writer.dispose();
      }

      LOGGER.info("Saved legend to: " + file.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.error("Cannot write. " + ExceptionUtils.getStackTrace(e));
    } catch (Exception e) {
      LOGGER.error("Cannot create image. " + ExceptionUtils.getStackTrace(e));
    }
  }

  // Claude sonnet 4.6
  private static void writeAsJPEGHighDPI(JFreeChart chart, int w, int h, float dpiScale, File file)
      throws IOException {
    int pixelW = Math.round(w * dpiScale);
    int pixelH = Math.round(h * dpiScale);

    // JPEG has no alpha channel, use RGB
    BufferedImage image = new BufferedImage(pixelW, pixelH, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = image.createGraphics();

    // White background (JPEG has no transparency)
    g2.setColor(java.awt.Color.WHITE);
    g2.fillRect(0, 0, pixelW, pixelH);

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

    g2.scale(dpiScale, dpiScale);
    chart.draw(g2, new Rectangle(w, h));
    g2.dispose();

    // JPEG quality control (0.0f–1.0f)
    float quality = 0.99f;
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(quality);

    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      writer.setOutput(ImageIO.createImageOutputStream(out));
      writer.write(null, new IIOImage(image, null, null), param);
      writer.dispose();
    }
  }

  /**
   * A handler for the export to JPEG option in the context menu.
   */
  private static void handleDefaultExportToJPEG(ChartViewer chartViewer, Pane legend,
                                                TextField nameField,
                                                TextField widthField,
                                                TextField heightField) {
    Path folder = GlobalIO.makeExportGraphFolder();
    if (folder != null) {
      String folderName = folder.toFile().toString();
      String fileName = getNameFromField(nameField) + ".jpeg";
      Path path = Path.of(folderName);
      path = path.resolve(fileName);
      File file = path.toFile();
      try {
//        ExportUtils
//            .writeAsJPEG(chartViewer.getCanvas().getChart(), getWidth(chartViewer, widthField),
//                getHeight(chartViewer, heightField), file);
        writeAsJPEGHighDPI(
            chartViewer.getCanvas().getChart(),
            getWidth(chartViewer, widthField),
            getHeight(chartViewer, heightField),
            SpTool3Main.getRunTime().getConfParams().getImageExportDpiLevelFloat(), // 3x = ~288 DPI. 10.0f
            file);
        LOGGER.info("Wrote file: " + file);
      } catch (IOException ex) {
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }

      String legendFileName = getNameFromField(nameField) + "_KEY_" + ".jpeg";
      Path legendPath = Path.of(folderName);
      legendPath = legendPath.resolve(legendFileName);
      File legendFile = legendPath.toFile();
      saveAsJpeg(legend, legendFile);
    }
  }

  /**
   * A handler for the export to PDF option in the context menu.
   */
  private static void handleDefaultExportToPDF(ChartViewer chartViewer, Pane legend,
                                               TextField nameField,
                                               TextField widthField,
                                               TextField heightField) {
    Path folder = GlobalIO.makeExportGraphFolder();
    if (folder != null) {
      String folderName = folder.toFile().toString();
      String fileName = getNameFromField(nameField) + ".pdf";
      Path path = Path.of(folderName);
      path = path.resolve(fileName);
      File file = path.toFile();
      ExportUtils.writeAsPDF(chartViewer.getCanvas().getChart(), getWidth(chartViewer, widthField),
          getHeight(chartViewer, heightField), file);
      LOGGER.info("Wrote file: " + file);
      //
      String legendFileName = getNameFromField(nameField) + "_KEY_" + ".png";
      Path legendPath = Path.of(folderName);
      legendPath = legendPath.resolve(legendFileName);
      File legendFile = legendPath.toFile();
      saveAsPNG(legend, legendFile);
    }
  }

  /// OPEN DIALOG TO SAVE AS

  /**
   * A handler for the export to PDF option in the context menu.
   */
  private static void handleExportToPDF(ChartViewer chartViewer, Pane legend,
                                        TextField widthField,
                                        TextField heightField) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to PDF");
    Path projPath = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    chooser.setInitialDirectory(projPath.toFile());
    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
        "Portable Document Format (PDF)", "*.pdf");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(chartViewer.getScene().getWindow());
    if (file != null) {
      ExportUtils.writeAsPDF(chartViewer.getCanvas().getChart(), getWidth(chartViewer, widthField),
          getHeight(chartViewer, heightField), file);
      LOGGER.info("Wrote file: " + file);
      // Legend
      Path folder = Path.of(file.getParent());
      String fileName = file.getName(); // Get only the file name (e.g., "sample.txt")
      int lastDotIndex = fileName.lastIndexOf('.'); // Find the last dot
      // Extract the name without the extension
      String nameWithoutExtension =
          (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
      Path legendPath = folder.resolve(nameWithoutExtension + "_KEY_" + ".png");
      File legendFile = legendPath.toFile();
      if (!legend.getChildren().isEmpty() && legend.getWidth() > 0 && legend.getHeight() > 0) {
        saveAsPNG(legend, legendFile);
      }
    }
  }

  /**
   * A handler for the export to PNG option in the context menu.
   */
  private static void handleExportToPNG(ChartViewer chartViewer, Pane legend,
                                        TextField widthField,
                                        TextField heightField) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to PNG");
    Path projPath = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    chooser.setInitialDirectory(projPath.toFile());
    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
        "Portable Network Graphics (PNG)", "*.png");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(chartViewer.getScene().getWindow());
    if (file != null) {
      try {
//        ExportUtils
//            .writeAsPNG(chartViewer.getCanvas().getChart(), getWidth(chartViewer, widthField),
//                getHeight(chartViewer, heightField), file);

        writeAsPNGHighDPI(
            chartViewer.getCanvas().getChart(),
            getWidth(chartViewer, widthField),   // logical size, e.g. 750
            getHeight(chartViewer, heightField), // logical size, e.g. 500
            SpTool3Main.getRunTime().getConfParams().getImageExportDpiLevelFloat(), // 3x = ~288 DPI. 10.0f
            file);

        LOGGER.info("Wrote file: " + file);
      } catch (IOException ex) {
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }
      // Legend
      Path folder = Path.of(file.getParent());
      String fileName = file.getName(); // Get only the file name (e.g., "sample.txt")
      int lastDotIndex = fileName.lastIndexOf('.'); // Find the last dot
      // Extract the name without the extension
      String nameWithoutExtension =
          (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
      Path legendPath = folder.resolve(nameWithoutExtension + "_KEY_" + ".png");
      File legendFile = legendPath.toFile();
      // protect against plots w/o legend
      if (!legend.getChildren().isEmpty() && legend.getWidth() > 0 && legend.getHeight() > 0) {
        saveAsPNG(legend, legendFile);
      }
    }
  }

  /**
   * A handler for the export to JPEG option in the context menu.
   */
  private static void handleExportToJPEG(ChartViewer chartViewer, Pane legend,
                                         TextField widthField,
                                         TextField heightField) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to JPEG");
    Path projPath = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    chooser.setInitialDirectory(projPath.toFile());
    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("JPEG", "*.jpg");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(chartViewer.getScene().getWindow());
    if (file != null) {
      try {
        // ExportUtils
        //     .writeAsJPEG(chartViewer.getCanvas().getChart(), getWidth(chartViewer, widthField),
        //         getHeight(chartViewer, heightField), file);
        writeAsJPEGHighDPI(
            chartViewer.getCanvas().getChart(),
            getWidth(chartViewer, widthField),
            getHeight(chartViewer, heightField),
            SpTool3Main.getRunTime().getConfParams().getImageExportDpiLevelFloat(), // 3x = ~288 DPI. 10.0f
            file);
        LOGGER.info("Wrote file: " + file);
      } catch (IOException ex) {
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }
      // Legend
      Path folder = Path.of(file.getParent());
      String fileName = file.getName(); // Get only the file name (e.g., "sample.txt")
      int lastDotIndex = fileName.lastIndexOf('.'); // Find the last dot
      // Extract the name without the extension
      String nameWithoutExtension =
          (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
      Path legendPath = folder.resolve(nameWithoutExtension + "_KEY_" + ".jpeg");
      File legendFile = legendPath.toFile();
      if (!legend.getChildren().isEmpty() && legend.getWidth() > 0 && legend.getHeight() > 0) {
        saveAsJpeg(legend, legendFile);
      }
    }
  }

  /**
   * A handler for the export to SVG option in the context menu.
   */
  private static void handleExportToSVG(ChartViewer chartViewer, Pane legend,
                                        TextField widthField,
                                        TextField heightField) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to SVG");
    Path projPath = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    chooser.setInitialDirectory(projPath.toFile());
    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
        "Scalable Vector Graphics (SVG)", "*.svg");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(chartViewer.getScene().getWindow());
    if (file != null) {
      ExportUtils.writeAsSVG(chartViewer.getCanvas().getChart(), getWidth(chartViewer, widthField),
          getHeight(chartViewer, heightField), file);
      LOGGER.info("Wrote file: " + file);

      // Legend
      Path folder = Path.of(file.getParent());
      String fileName = file.getName(); // Get only the file name (e.g., "sample.txt")
      int lastDotIndex = fileName.lastIndexOf('.'); // Find the last dot
      // Extract the name without the extension
      String nameWithoutExtension =
          (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
      Path legendPath = folder.resolve(nameWithoutExtension + "_KEY_" + ".png");
      File legendFile = legendPath.toFile();
      if (!legend.getChildren().isEmpty() && legend.getWidth() > 0 && legend.getHeight() > 0) {
        saveAsPNG(legend, legendFile);
      }
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////////

  // Claude Sonnet 4.6
  private static void handleExportToClipboard(ChartViewer chartViewer, Pane legend,
                                              TextField widthField,
                                              TextField heightField) {
    try {
      // float dpiScale = 10.0f;
      float dpiScale = SpTool3Main.getRunTime().getConfParams().getImageExportDpiLevelFloat();
      int w = getWidth(chartViewer, widthField);
      int h = getHeight(chartViewer, heightField);

      // --- Render chart to BufferedImage ---
      int chartPixelW = Math.round(w * dpiScale);
      int chartPixelH = Math.round(h * dpiScale);

      BufferedImage chartImage = new BufferedImage(chartPixelW, chartPixelH, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = chartImage.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      g2.scale(dpiScale, dpiScale);
      chartViewer.getCanvas().getChart().draw(g2, new Rectangle(w, h));
      g2.dispose();

      // --- Snapshot legend Pane to BufferedImage ---
      int legendPixelW = (int) Math.ceil(legend.getWidth() * dpiScale);
      int legendPixelH = (int) Math.ceil(legend.getHeight() * dpiScale);

      SnapshotParameters params = new SnapshotParameters();
      params.setTransform(javafx.scene.transform.Scale.scale(dpiScale, dpiScale));
      WritableImage legendFxImage = legend.snapshot(params, new WritableImage(legendPixelW, legendPixelH));

      BufferedImage legendImage = new BufferedImage(legendPixelW, legendPixelH, BufferedImage.TYPE_INT_ARGB);
      PixelReader pixelReader = legendFxImage.getPixelReader();
      for (int y = 0; y < legendPixelH; y++) {
        for (int x = 0; x < legendPixelW; x++) {
          legendImage.setRGB(x, y, pixelReader.getArgb(x, y));
        }
      }

      // --- Merge: same width, pad narrower one with white ---
      int mergedW = Math.max(chartPixelW, legendPixelW);
      int mergedH = chartPixelH + legendPixelH;

      BufferedImage merged = new BufferedImage(mergedW, mergedH, BufferedImage.TYPE_INT_ARGB);
      Graphics2D mg = merged.createGraphics();
      mg.setColor(java.awt.Color.WHITE);
      mg.fillRect(0, 0, mergedW, mergedH);

      // Center narrower image horizontally, or just left-align — your choice
      int chartOffsetX = (mergedW - chartPixelW) / 2;
      int legendOffsetX = (mergedW - legendPixelW) / 2;

      mg.drawImage(chartImage, chartOffsetX, 0, null);
      mg.drawImage(legendImage, legendOffsetX, chartPixelH, null);
      mg.dispose();

      // --- Copy merged image to clipboard ---
      WritableImage fxImage = new WritableImage(mergedW, mergedH);
      PixelWriter pixelWriter = fxImage.getPixelWriter();
      for (int y = 0; y < mergedH; y++) {
        for (int x = 0; x < mergedW; x++) {
          pixelWriter.setArgb(x, y, merged.getRGB(x, y));
        }
      }

      ClipboardContent content = new ClipboardContent();
      content.putImage(fxImage);
      Clipboard.getSystemClipboard().setContent(content);

      LOGGER.info("Copied chart + legend to clipboard.");
    } catch (Exception e) {
      LOGGER.error("Cannot copy to clipboard. " + ExceptionUtils.getStackTrace(e));
    }
  }

  // Claude Sonnet 4.6
  private static void handleExportToClipboard(ChartViewer chartViewer,
                                              TextField widthField,
                                              TextField heightField) {
    try {
      //float dpiScale = 10.0f;
      float dpiScale = SpTool3Main.getRunTime().getConfParams().getImageExportDpiLevelFloat();
      int w = getWidth(chartViewer, widthField);
      int h = getHeight(chartViewer, heightField);

      int pixelW = Math.round(w * dpiScale);
      int pixelH = Math.round(h * dpiScale);

      BufferedImage image = new BufferedImage(pixelW, pixelH, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = image.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      g2.scale(dpiScale, dpiScale);
      chartViewer.getCanvas().getChart().draw(g2, new Rectangle(w, h));
      g2.dispose();

      WritableImage fxImage = new WritableImage(pixelW, pixelH);
      PixelWriter pixelWriter = fxImage.getPixelWriter();
      for (int y = 0; y < pixelH; y++) {
        for (int x = 0; x < pixelW; x++) {
          pixelWriter.setArgb(x, y, image.getRGB(x, y));
        }
      }

      ClipboardContent content = new ClipboardContent();
      content.putImage(fxImage);
      Clipboard.getSystemClipboard().setContent(content);

      LOGGER.info("Copied chart to clipboard.");
    } catch (Exception e) {
      LOGGER.error("Cannot copy to clipboard. " + ExceptionUtils.getStackTrace(e));
    }
  }

  private static void handleExportToClipboard(Pane legend) {
    // float dpiScale = 10.0f;
    float dpiScale = SpTool3Main.getRunTime().getConfParams().getImageExportDpiLevelFloat();
    try {
      int legendPixelW = (int) Math.ceil(legend.getWidth() * dpiScale);
      int legendPixelH = (int) Math.ceil(legend.getHeight() * dpiScale);

      SnapshotParameters params = new SnapshotParameters();
      params.setTransform(javafx.scene.transform.Scale.scale(dpiScale, dpiScale));
      WritableImage fxImage = legend.snapshot(params, new WritableImage(legendPixelW, legendPixelH));

      ClipboardContent content = new ClipboardContent();
      content.putImage(fxImage);
      Clipboard.getSystemClipboard().setContent(content);

      LOGGER.info("Copied legend to clipboard.");
    } catch (Exception e) {
      LOGGER.error("Cannot copy to clipboard. " + ExceptionUtils.getStackTrace(e));
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////////

  public static String extractFileName(ChartViewer chartViewer) {
    String fileName = "";
    if (chartViewer.getChart() != null) {
      JFreeChart chart = chartViewer.getChart();
      StringBuilder builder = new StringBuilder();

      List<String> seriesNames = extractSeriesLabels(chart);

      for (int i = 0; i < seriesNames.size(); i++) {
        String s = seriesNames.get(i);
        if (builder.length() + s.length() < GlobalIO.MAX_CHAR_FILENAME) {
          builder.append(s);
          if (i < seriesNames.size() - 1) {
            builder.append("_");
          }
        } else {
          builder.append(s);
          break;
        }
      }

      fileName = builder.toString();
    }
    return fileName;
  }

  // Modified from chatgpt4.
  public static List<String> extractSeriesLabels(JFreeChart chart) {
    List<String> seriesLabels = new ArrayList<>();

    if (chart.getPlot() instanceof XYPlot) {
      int dataSetCount = chart.getXYPlot().getDatasetCount();
      for (int d = 0; d < dataSetCount; d++) {
        XYDataset dataset = chart.getXYPlot().getDataset(d);
        if (dataset instanceof XYSeriesCollection) {
          XYSeriesCollection xyDataset = (XYSeriesCollection) dataset;

          for (int i = 0; i < xyDataset.getSeriesCount(); i++) {
            String serLabel = xyDataset.getSeries(i).getKey().toString();
            serLabel = GlobalIO.cleanupWindowsFileName(serLabel);
            seriesLabels.add(serLabel);
          }
        } else if (dataset instanceof HistogramDataset) {
          HistogramDataset hDataset = (HistogramDataset) dataset;
          for (int i = 0; i < hDataset.getSeriesCount(); i++) {
            String serLabel = hDataset.getSeriesKey(i).toString();
            serLabel = GlobalIO.cleanupWindowsFileName(serLabel);
            seriesLabels.add(serLabel);
          }
        } else if (dataset instanceof XYIntervalSeriesCollection) {
          XYIntervalSeriesCollection intervalDataset = (XYIntervalSeriesCollection) dataset;
          for (int i = 0; i < intervalDataset.getSeriesCount(); i++) {
            String serLabel = intervalDataset.getSeries(i).getKey().toString();
            serLabel = GlobalIO.cleanupWindowsFileName(serLabel);
            seriesLabels.add(serLabel);
          }
        }
      }
    } else if (chart.getPlot() instanceof CategoryPlot) {
      int dataSetCount = chart.getCategoryPlot().getDatasetCount();
      for (int d = 0; d < dataSetCount; d++) {
        CategoryDataset dataset = chart.getCategoryPlot().getDataset(d);
        if (dataset instanceof DefaultBoxAndWhiskerCategoryDataset) {
          BoxAndWhiskerCategoryDataset xyDataset = (BoxAndWhiskerCategoryDataset) dataset;
          for (int i = 0; i < xyDataset.getColumnCount(); i++) {
            String serLabel = xyDataset.getColumnKey(i).toString();
            serLabel = GlobalIO.cleanupWindowsFileName(serLabel);
            seriesLabels.add(serLabel);
          }
        }
      }
    }

    // make sure that the Labels are unique, else spCal refuses to load.
    seriesLabels = generateUniqueList(seriesLabels);


    return seriesLabels;
  }

  public static List<String> generateUniqueList(List<String> inputList) {
    List<String> uniqueList = new ArrayList<>();
    Map<String, Integer> countMap = new HashMap<>();

    for (String item : inputList) {
      if (countMap.containsKey(item)) {
        int count = countMap.get(item) + 1;
        countMap.put(item, count);
        uniqueList.add(item + "_" + count);
      } else {
        countMap.put(item, 1);
        uniqueList.add(item);
      }
    }

    return uniqueList;
  }


  public static void writeDataAsTable(JFreeChart chart, ExportWriter exportWriter) {
    TabularSheet sheet = new TabularSheet(exportWriter);

    if (chart.getPlot() instanceof XYPlot) {
      // --- XY plot export ---
      List<Pair<double[], double[]>> seriesData = extractSeriesData(chart);
      List<String> seriesNames = extractSeriesLabels(chart);

      XYPlot plot = chart.getXYPlot();
      ValueAxis xAxis = plot.getDomainAxis();
      String xAxisLabel = xAxis.getLabel();
      if (xAxisLabel != null) {
        xAxisLabel = xAxisLabel.replaceFirst("/", "[");
        xAxisLabel += "]";
      } else {
        xAxisLabel = "x-Axis [-]";
      }


      ValueAxis yAxis = plot.getRangeAxis();
      String yAxisLabel = yAxis.getLabel();
      yAxisLabel = yAxisLabel.replaceFirst("/", "[");
      yAxisLabel += "]";

      if (seriesData.size() == seriesNames.size()) {
        BlockCollection blockCollection = new BlockCollectionHorizontal();
        TabularBlockHorizontal hBlock = new TabularBlockHorizontal();

        for (int i = 0; i < seriesData.size(); i++) {
          double[] x = seriesData.get(i).getKey();
          double[] y = seriesData.get(i).getValue();
          String label = seriesNames.get(i);
          // Empty header for x column, series label as header for y column
          hBlock.addColumn("", xAxisLabel, SnF.doubleToStrArr(x, NF.D1C6));
          hBlock.addColumn(label, yAxisLabel, SnF.doubleToStrArr(y, NF.D1C6));
        }
        blockCollection.addBlock(hBlock);
        sheet.addBlockCollection(blockCollection);
      }

    } else if (chart.getPlot() instanceof PiePlot<?> piePlot) {
      // --- Pie chart export ---
      PieDataset<?> pieDataset = piePlot.getDataset();

      BlockCollection blockCollection = new BlockCollectionHorizontal();
      TabularBlockHorizontal hBlock = new TabularBlockHorizontal();

      List<String> keys = new ArrayList<>();
      List<String> values = new ArrayList<>();

      for (int i = 0; i < pieDataset.getItemCount(); i++) {
        keys.add(pieDataset.getKey(i).toString());
        values.add(SnF.doubleToString(pieDataset.getValue(i).doubleValue(), NF.D1C6));
      }

      String title = chart.getTitle() != null ? chart.getTitle().getText() : "Value";
      hBlock.addColumn("", "Key", keys.toArray(new String[0]));
      hBlock.addColumn(title, "Fraction [-]", values.toArray(new String[0]));

      blockCollection.addBlock(hBlock);
      sheet.addBlockCollection(blockCollection);
    }

    sheet.export();
  }


  // Modified from chatgpt4 & Claude Sonnet 4.6.
  public static List<Pair<double[], double[]>> extractSeriesData(JFreeChart chart) {
    List<Pair<double[], double[]>> seriesData = new ArrayList<>();

    if (chart.getPlot() instanceof XYPlot) {

      int dataSetCount = chart.getXYPlot().getDatasetCount();
      for (int d = 0; d < dataSetCount; d++) {
        XYDataset dataset = chart.getXYPlot().getDataset(d);
        if (dataset instanceof XYSeriesCollection) {
          XYSeriesCollection xyDataset = (XYSeriesCollection) dataset;

          for (int i = 0; i < xyDataset.getSeriesCount(); i++) {
            XYSeries series = xyDataset.getSeries(i);
            int itemCount = series.getItemCount();

            double[] x = new double[itemCount];
            double[] y = new double[itemCount];

            for (int j = 0; j < itemCount; j++) {
              x[j] = series.getX(j).doubleValue();
              y[j] = series.getY(j).doubleValue();
            }
            seriesData.add(new Pair<>(x, y));
          }
        } else if (dataset instanceof HistogramDataset) {
          HistogramDataset histogramDataset = (HistogramDataset) dataset;

          // fill array --> series count of histogram = 1 (should be the case for all). Why?
          // as of now, histograms have only one series and the overlay is done by adding datasets to the
          // XYPlot.
          int seriesCount = histogramDataset.getSeriesCount();
          // counting the bins: item=bar=bin
          int barCount = 0;
          if (seriesCount > 0) { // just crash safety
            barCount = histogramDataset.getItemCount(0);
          }
          double[] x = new double[barCount];
          double[] y = new double[barCount];

          for (int barIdx = 0; barIdx < barCount; barIdx++) {
            x[barIdx] = histogramDataset.getX(0, barIdx).doubleValue();
            y[barIdx] = histogramDataset.getY(0, barIdx).doubleValue();
          }
          seriesData.add(new Pair<>(x, y));
        } else if (dataset instanceof XYIntervalSeriesCollection) {
          XYIntervalSeriesCollection intervalDataset = (XYIntervalSeriesCollection) dataset;

          for (int i = 0; i < intervalDataset.getSeriesCount(); i++) {
            int itemCount = intervalDataset.getItemCount(i);

            double[] x = new double[itemCount];
            double[] y = new double[itemCount];

            for (int j = 0; j < itemCount; j++) {
              x[j] = intervalDataset.getX(i, j).doubleValue();
              y[j] = intervalDataset.getY(i, j).doubleValue();
            }
            seriesData.add(new Pair<>(x, y));
          }
        }
      }
    }

    return seriesData;
  }


  // Helper method to create an HBox with Label and TextField
  private static <T extends Serializable> HBox createLabeledTextField(String labelText, TextField textField,
                                                                      Parameter<T> param) {
    Label label = new Label(labelText);
    textField.setPrefWidth(60); // Adjust as needed
    textField.setPromptText("Enter value");


    if (param instanceof DoubleParameter) {
      double value = ((DoubleParameter) param).getValue();
      TextFormatter<Double> formatter = TextFieldUtils.assureNonzeroPositiveDouble(value);
      textField.setTextFormatter(formatter);
      textField.setText(SnF.doubleToString(formatter.getValue(), NF.D1C3, NF.D1C3Exp));
    } else if (param instanceof IntegerParameter) {
      int value = ((IntegerParameter) param).getValue();
      value = Math.max(1, value);
      TextFormatter<Integer> formatter = TextFieldUtils.assureNonzeroPositiveInteger(value);
      textField.setTextFormatter(formatter);
      textField.setText(SnF.intToString(formatter.getValue(), NF.D1C0));
    } else if (param instanceof StringParameter) {
      String value = ((StringParameter) param).getValue();
      textField.setText(value);
    }

    textField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
        if (t1 != null && !t1.isEmpty()) {
          try {
            if (param instanceof DoubleParameter) {
              ((DoubleParameter) param).setValue(Double.parseDouble(t1));
            } else if (param instanceof IntegerParameter) {
              ((IntegerParameter) param).setValue(Integer.parseInt(t1));
            } else if (param instanceof StringParameter) {
              ((StringParameter) param).setValue(t1);
            }

          } catch (NumberFormatException e) {
            // ignore invalid input — formatter should prevent this anyway
          }
        }
      }
    });

    HBox hbox = new HBox(5, label, textField);
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.setPadding(new Insets(5));
    return hbox;
  }

  // Helper method to create an HBox with Label and TextField
  private static HBox createLabeledTextField(String labelText, TextField textField, String value) {
    Label label = new Label(labelText);
    textField.setPrefWidth(60); // Adjust as needed
    textField.setPromptText("Enter value");

    TextFormatter<String> formatter = TextFieldUtils.allPass(value);
    textField.setTextFormatter(formatter);

    textField.setText(formatter.getValue());

    textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                          Boolean newValue) {
        if (newValue) {
          textField.setPrefWidth(300); // Adjust as needed
        } else {
          // It seems annoying when the field shrinks
          // textField.setPrefWidth(60); // Adjust as needed
        }
      }
    });

    HBox hbox = new HBox(5, label, textField);
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.setPadding(new Insets(5));
    return hbox;
  }

  public static int getWidth(ChartViewer chartViewer, TextField widthField) {
    int defaultValue = (int) chartViewer.getWidth();
    defaultValue = SnF.strToInt(widthField.getText(), defaultValue);
    return defaultValue;
  }

  public static int getHeight(ChartViewer chartViewer, TextField heightField) {
    int defaultValue = (int) chartViewer.getHeight();
    defaultValue = SnF.strToInt(heightField.getText(), defaultValue);
    return defaultValue;
  }

  public static String getNameFromField(TextField nameField) {
    String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
    String name = nameField.getText();
    name = GlobalIO.cleanupWindowsFileName(name);
    name = dateTag + "_" + name;
    return name;
  }

}


/*
            if (doExport) {
              //final String literalPath = "F:\\";
              Path path = Paths.get(exportPath);
              LocalDateTime now = LocalDateTime.now();
              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
              String formattedDateTime = now.format(formatter);
              path = path.resolve(formattedDateTime + "_simulatedData.csv");
              ExportWriter writer = new CsvExportWriter(path.toFile());
              TabularSheet sheet = new TabularSheet(writer);

              BlockCollection h = new BlockCollectionHorizontal();
              TabularBlockHorizontal hBlock = new TabularBlockHorizontal();
              for (String key : results.keySet()) {
                hBlock
                    .addColumn(key,
                        SnF.doubleToStrList(StorageUtils.getArray(results.get(key)), NF.D1C6));
                hBlock.addColumn(key,
                    SnF.doubleToStrList(StorageUtils.getArray(results.get(key)), NF.D1C6));
              }
              h.addBlock(hBlock);
              sheet.addBlockCollection(h);

              sheet.export();
            }
 */
