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

package gui.table;

import core.SpTool3Main;
import io.SampleSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TableUtils {

  public static final String UNAVAILABLE_STR = "N/A";
  public static final String EMPTY_STR = "";
  public static final double EMPTY_NUM = 0;

  //https://stackoverflow.com/questions/31708840/save-table-in-clipboad-javafx
  private static final Logger LOGGER = LogManager.getLogger(TableUtils.class);

  // ############################################################################################

  // ############################################################################################


  public static <T> void installDoubleClickSelect(TableView<T> tableView) {
    tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent click) {
        // @ double click
        if (click.getClickCount() == 2) {
          tableView.getSelectionModel().selectAll();
        }
      }
    });
  }

  // ############################################################################################


  public static void installHeaderLeft(TableView<?> tableView) {
    // hard code hack to align the header with column (left)
    // https://stackoverflow.com/questions/23576867/javafx-how-to-align-only-one-column-header-in-tableview
    tableView.widthProperty().addListener((src, o, n) -> Platform.runLater(() -> {
      if (o != null && o.intValue() > 0) {
        return; // already aligned
      }
      for (Node node : tableView.lookupAll(".column-header > .label")) {
        if (node instanceof Label) {
          ((Label) node).setAlignment(Pos.CENTER_LEFT);
        }
      }
    }));
  }

  public static <T> void calColumnsBold(TableView<T> calTable) {
    List<TableColumn<T, ?>> cols = calTable.getColumns();
    int n = 0;
    for (TableColumn<T, ?> col : cols) {
      // make three raw cols gray
      if (0 < n && n <= 3) {
        col.setStyle("-fx-text-fill: #666666");
      }
      // Only make transformed cols (three at the right) bold
      if (n > 3) {
        col.setStyle("-fx-font-weight: bold");
      }
      n++;
    }
  }

  // ############################################################################################


  public static <S, T> TableColumn<S, T> createColumnRibbon(
      String header,
      double prefWidth,
      double maxWidth,
      Comparator<T> comparator) {
    TableColumn<S, T> col = new TableColumn<>(header);
    col.setPrefWidth(prefWidth);
    col.setMaxWidth(maxWidth);
    col.setStyle("-fx-alignment: CENTER-LEFT;");
    col.setComparator(comparator);
    return col;
  }

  // ############################################################################################

  public static interface FxTableLabelGenerator<T> {

    public StringProperty createLabel(FxTableEntry<T> entry);
  }

  public static interface FxTableEditableLabelHandler<T> {

    public void handleOnCommit(FxTableEntry<T> entry, String value);
  }

  // Specify how the TableEntry shall be converted into a string.
  public static <T> void installSimpleCellValueFactory(TableColumn<FxTableEntry<T>, String> col,
      FxTableLabelGenerator<T> stringPropertyGenerator) {
    col.setCellValueFactory(
        new Callback<CellDataFeatures<FxTableEntry<T>, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(
              CellDataFeatures<FxTableEntry<T>, String> param) {
            FxTableEntry<T> entry = param.getValue();
            StringProperty cellContent = stringPropertyGenerator.createLabel(entry);
            return cellContent;
          }
        });
  }

  // Note: This has to be installed in addition to the cellValueFactory!
  public static <T> void installEditableNameOnCommit(TableColumn<FxTableEntry<T>, String> col,
      FxTableEditableLabelHandler<T> stringPropertyGenerator) {
    col.setOnEditCommit((CellEditEvent<FxTableEntry<T>, String> editEvent) -> {
      FxTableEntry<T> entry = editEvent.getTableView()
          .getItems()
          .get(editEvent.getTablePosition().getRow());
      stringPropertyGenerator.handleOnCommit(entry, editEvent.getNewValue());
    });
  }

  // ############################################################################################


  /**
   * A series of default methods to build a cell
   */

  public static <T> void installDragDropCellFactory(TableColumn<FxTableEntry<T>, String> col) {
    col.setCellFactory(
        new Callback<TableColumn<FxTableEntry<T>, String>, TableCell<FxTableEntry<T>, String>>() {

          @Override
          public TableCell<FxTableEntry<T>, String> call(
              TableColumn<FxTableEntry<T>, String> param) {

            TableCell<FxTableEntry<T>, String> cell
                = TextFieldTableCell.<FxTableEntry<T>>forTableColumn().call(param);
            enableDragDropToAnyTargetByPassingADummyString(cell);
            return cell;
          }

        });
  }


  /**
   * The SampleListView only registers the drag/drop and does not actually receive any object via
   * drag drop. Instead, we need to allow the table to perform a drag, the listview receives a drop,
   * and simply checks getSelectedTraces() and uses this input to organize the grouped samples.
   */
  public static <T> void enableDragDropToAnyTargetByPassingADummyString(TableCell<T, String> cell) {

    // Enable Drag Samples into the set list view
    cell.setOnDragDetected((MouseEvent event) -> {
      // Do not move from the main set
      SampleSet mainSet = SpTool3Main.getRunTime().getSampleReg().getMainSet();
      SampleSet selSet = SpTool3Main.getRunTime().getMainWindowCtl().getSelSampleSet();
      boolean isMain = mainSet.equals(selSet);

      Dragboard db;
      String mode = "Move";
      if (isMain || event.isControlDown()) {
        db = cell.startDragAndDrop(TransferMode.COPY);
        mode = "Copy";
      } else if (event.isShiftDown()) {
        db = cell.startDragAndDrop(TransferMode.MOVE);
      } else {
        // assume move sample
        db = cell.startDragAndDrop(TransferMode.MOVE);
      }

      ClipboardContent content = new ClipboardContent();
      content.putString("Placeholder");
      db.setContent(content);

      // Show guidance
      Text textNode = new Text(mode);
      textNode.setFont(Font.font("Arial", FontWeight.BOLD, 14)); // bigger font
      // Snapshot to image
      WritableImage dragView = textNode.snapshot(null, null);
      // Offset so it appears above and slightly to the side of the cursor
      double offsetX = dragView.getWidth() / 2;
      double offsetY = dragView.getHeight() + 5; // higher above cursor
      db.setDragView(dragView, offsetX, offsetY);
    });
  }

  // ############################################################################################

  /**
   * Install the keyboard handler: + CTRL + C = copy to clipboard
   *
   * @param table
   */
  public static void installCopyPasteHandler(TableView<?> table) {

    // install copy/paste keyboard handler
    table.setOnKeyPressed(new TableKeyEventHandler());

  }

  /**
   * Copy/Paste keyboard event handler. The handler uses the keyEvent's source for the clipboard
   * data. The source must be of type TableView.
   */
  public static class TableKeyEventHandler implements EventHandler<KeyEvent> {

    KeyCodeCombination copyKeyCodeCombination =
        new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

    @Override
    public void handle(final KeyEvent keyEvent) {

      if (copyKeyCodeCombination.match(keyEvent)) {

        if (keyEvent.getSource() instanceof TableView) {

          // copy to clipboard
          extractNestedTableViewData((TableView<?>) keyEvent.getSource());

          LOGGER.debug("Selection copied to clipboard");

          // event is handled, consume it
          keyEvent.consume();

        }
      }
    }
  }

  /**
   * Get table selection and copy it to the clipboard.
   *
   * @param table
   */
  public static void copySelectionToClipboard(TableView<?> table) {

    StringBuilder clipboardString = new StringBuilder();

    ObservableList<TablePosition> positionList = table.getSelectionModel().getSelectedCells();

    int prevRowIdx = -1;

    for (TablePosition position : positionList) {

      int rowIdx = position.getRow();
      int colIdx = position.getColumn();

      Object cell = null;

      if (-1 < colIdx && colIdx < table.getColumns().size()) {
        if (-1 < rowIdx) {
          cell = table.getColumns().get(colIdx).getCellData(rowIdx);
        }
      }

      // null-check: provide empty string for nulls
      if (cell == null) {
        cell = "";
      }

      // determine whether we advance in a row (tab) or a column
      // (newline).
      if (prevRowIdx == rowIdx) {

        clipboardString.append('\t');

      } else if (prevRowIdx != -1) {

        /*
         v46 mimicking excels line sep adding clipboardString.append('\r');
         Reason: \r\n is recognized as splitStringBy(regex: "\\t") in Java under windows apparently.
         Thus, using this combination, a single column can be copied from and pasted into the same
         calibration table.
         */

        clipboardString.append('\r');
        clipboardString.append('\n');

      }

      // create string from cell
      String text = cell.toString();

      // add new item to clipboard
      clipboardString.append(text);

      // remember previous
      prevRowIdx = rowIdx;
    }

    // create clipboard content
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(clipboardString.toString());

    // set clipboard content
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  // ChatGPT4
  public static <T> void extractTableViewData(TableView<T> tableView) {

    StringBuilder sb = new StringBuilder();

    // Get column headers
    List<TableColumn<T, ?>> columns = tableView.getColumns();
    String headerRow = columns.stream()
        .map(TableColumn::getText)
        .collect(Collectors.joining("\t"));
    sb.append(headerRow).append("\n");

    // Get selected rows
    List<T> selectedItems = tableView.getSelectionModel().getSelectedItems();

    // Get row data
    for (T item : selectedItems) {
      String row = columns.stream()
          .map(column -> {
            Object cellData = column.getCellData(item);
            return cellData != null ? cellData.toString() : "";
          })
          .collect(Collectors.joining("\t"));
      sb.append(row).append("\n");
    }

    // create clipboard content
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(sb.toString());

    // set clipboard content
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  // ############################################################################################

  // ChatGPT4:
  /*
  tableView.getColumns() only returns the top-level columns, not recursively all nested children.
  In JavaFX, a TableColumn can itself contain other columns via getColumns(), which is why your method misses them.
  To fix this, you need to flatten the column hierarchy before processing.
   */
  public static <T> void extractNestedTableViewData(TableView<T> tableView) {
    StringBuilder sb = new StringBuilder();

    // Build aligned header rows (every row has the same number of cells = #leaf columns)
    List<List<String>> headerRows = buildAlignedHeaderRows(tableView.getColumns());

    // Write header rows
    for (List<String> row : headerRows) {
      sb.append(String.join("\t", row)).append("\n");
    }

    // Flatten leaf columns for data extraction
    List<TableColumn<T, ?>> flatColumns = getLeafColumns(tableView.getColumns());

    // Selected rows
    List<T> selectedItems = tableView.getSelectionModel().getSelectedItems();

    // Data rows
    for (T item : selectedItems) {
      String row = flatColumns.stream()
          .map(col -> {
            Object val = col.getCellData(item);
            return val != null ? val.toString() : "";
          })
          .collect(Collectors.joining("\t"));
      sb.append(row).append("\n");
    }

    // To clipboard
    ClipboardContent content = new ClipboardContent();
    content.putString(sb.toString());
    Clipboard.getSystemClipboard().setContent(content);
  }

  /** Builds header rows where:
   *  - Row 0 repeats each top-level header once per leaf it spans.
   *  - Deeper rows contain child headers; if a branch ends earlier, blanks are inserted.
   *  This guarantees an empty cell under a single-leaf top-level column (e.g., "Parameter").
   */
  private static <T> List<List<String>> buildAlignedHeaderRows(List<TableColumn<T, ?>> roots) {
    // 1) Collect paths (root -> leaf) in left-to-right order
    List<List<String>> leafPaths = new ArrayList<>();
    for (TableColumn<T, ?> root : roots) {
      collectLeafPaths(root, new ArrayList<>(), leafPaths);
    }

    // 2) Determine depth (number of header rows)
    int maxDepth = 0;
    for (List<String> path : leafPaths) {
      maxDepth = Math.max(maxDepth, path.size());
    }

    // 3) Build aligned rows by depth, padding with ""
    List<List<String>> rows = new ArrayList<>();
    for (int level = 0; level < maxDepth; level++) {
      rows.add(new ArrayList<>());
      List<String> current = rows.get(level);
      for (List<String> path : leafPaths) {
        current.add(level < path.size() ? path.get(level) : "");
      }
    }
    return rows;
  }

  /** Collects (root..leaf) header text path for each leaf column, preserving visual order. */
  private static <T> void collectLeafPaths(TableColumn<T, ?> col,
      List<String> prefix,
      List<List<String>> out) {
    List<String> cur = new ArrayList<>(prefix);
    cur.add(safe(col.getText()));
    if (col.getColumns().isEmpty()) {
      out.add(cur);
    } else {
      for (TableColumn<T, ?> child : col.getColumns()) {
        collectLeafPaths(child, cur, out);
      }
    }
  }

  /** Returns leaf columns left-to-right. */
  private static <T> List<TableColumn<T, ?>> getLeafColumns(List<TableColumn<T, ?>> columns) {
    List<TableColumn<T, ?>> result = new ArrayList<>();
    for (TableColumn<T, ?> c : columns) {
      if (c.getColumns().isEmpty()) {
        result.add(c);
      } else {
        result.addAll(getLeafColumns(c.getColumns()));
      }
    }
    return result;
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  // ############################################################################################

}
