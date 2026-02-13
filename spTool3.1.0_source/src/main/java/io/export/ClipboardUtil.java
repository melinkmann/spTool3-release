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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.NF;
import util.SnF;

public abstract class ClipboardUtil {

  private static final Logger LOGGER = LogManager.getLogger(ClipboardUtil.class);

  public static HashMap<String, List<Double>> getCalDataFromClipboard() {

    HashMap<String, List<Double>> map = new HashMap<>();

    final Clipboard clipboard = Clipboard.getSystemClipboard();
    String val = clipboard.getString();
    if (val != null && !val.isEmpty()) {
      String[] lines = val.split(System.lineSeparator());

      List<Double> x = new ArrayList<>();
      List<Double> y = new ArrayList<>();
      List<Double> w = new ArrayList<>();

      for (String col : lines) {
        // v46: if only one column is selected without accordingly no tab sep:
        String[] line;
        if (!col.contains("\t")) {
          line = new String[]{col}; // only thing in the col is the single cell
        } else {
          line = (col.split("\\t"));
        }
        if (line.length > 0) {
          x.add(SnF.strToDouble(line[0], 0));
        }
        if (line.length > 1) {
          y.add(SnF.strToDouble(line[1], 0));
        }
        if (line.length > 2) {
          w.add(SnF.strToDouble(line[2], 0));
        }
      }
      map.put("x", x);
      map.put("y", y);
      map.put("w", w);
    }
    return map;
  }

  public static void addToClipboard(ClipboardContent clipboardContent) {
    // set clipboard content
    Clipboard.getSystemClipboard().setContent(clipboardContent);
    LOGGER.info("Copied arrays to clipboard.");
  }

  public static void copy(String[][] rows) {
    StringBuilder clipboardString = new StringBuilder();
    for (String[] row : rows) {
      for (String cell : row) {
        clipboardString.append(cell);
        clipboardString.append('\t');
      }
      // newline
      clipboardString.append('\n');
    }

    // create clipboard content
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(clipboardString.toString());

    // set clipboard content
    Clipboard.getSystemClipboard().setContent(clipboardContent);

    LOGGER.info("Copied arrays to clipboard.");
  }

  public static void copy(List<String[]> rows) {
    StringBuilder clipboardString = new StringBuilder();
    for (String[] row : rows) {
      for (String cell : row) {
        clipboardString.append(cell);
        clipboardString.append('\t');
      }
      // newline
      clipboardString.append('\n');
    }

    // create clipboard content
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(clipboardString.toString());

    // set clipboard content
    Clipboard.getSystemClipboard().setContent(clipboardContent);

    LOGGER.info("Copied arrays to clipboard.");
  }

  public static String[][] createArray(double[][] rows, NF format) {
    String[][] strings;
    if (rows.length != 0) {
      strings = new String[rows.length][rows[0].length];
      for (int rowIdx = 0; rowIdx < rows.length; rowIdx++) {
        for (int lineIdx = 0; lineIdx < rows[rowIdx].length; lineIdx++) {
          strings[rowIdx][lineIdx] = SnF
              .doubleToString(rows[rowIdx][lineIdx], format);
        }
      }
    } else {
      strings = new String[1][1];
      strings[0][0] = "no data copied";
    }
    return strings;
  }

  public static double[][] asSingleRow(double[] arr) {
    double[][] arrs = new double[1][arr.length];
    arrs[0] = arr;
    return arrs;
  }

  public static double[][] asSingleCol(double[] arr) {
    double[][] arrs = new double[arr.length][1];
    for (int i = 0; i < arr.length; i++) {
      arrs[i][0] = arr[i];
    }
    return arrs;
  }

  public static void toClipboard(double[] numbers, NF nf) {
    // Build the string with one value per line
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numbers.length; i++) {
      sb.append(SnF.doubleToString(numbers[i], nf));
      if (i < numbers.length - 1) {
        sb.append("\n");
      }
    }

    // Copy to clipboard using JavaFX
    ClipboardContent content = new ClipboardContent();
    content.putString(sb.toString());
    Clipboard.getSystemClipboard().setContent(content);

    LOGGER.info("Double array copied to clipboard (one per line).");
  }


}
