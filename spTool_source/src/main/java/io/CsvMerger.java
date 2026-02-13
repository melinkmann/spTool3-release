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

package io;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import gui.dialog.notification.NotificationFactory;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CsvMerger {

  private static final Logger LOGGER = LogManager.getLogger(CsvMerger.class.getName());

  // Method 1: Open a JavaFX dialog to choose a folder and list all .csv files
  public static Pair<File[], File> selectCsvFiles(Stage stage) {
    Pair<File[], File> result = new Pair<>(new File[]{}, new File(""));

    DirectoryChooser directoryChooser = new DirectoryChooser();
    File selectedDirectory = directoryChooser.showDialog(stage);

    if (selectedDirectory != null) {
      File[] files = selectedDirectory
          .listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
      return new Pair<>(files, selectedDirectory);
    }
    return result;
  }

  // Method 2: Read CSV files and merge them with filenames as headers
  public static List<String[]> mergeCsvFiles(File[] csvFiles) {
    List<List<String[]>> allData = new ArrayList<>();
    List<String> headers = new ArrayList<>();

    if (csvFiles.length == 0) {
      LOGGER.info("No CSV files found.");
      NotificationFactory.openInfo("No CSV files found.");
      return new ArrayList<>();
    }

    for (File file : csvFiles) {
      try (CSVReader reader = new CSVReader(new FileReader(file))) {
        LOGGER.trace("Reading file ... " + file.toString() + ".");
        //List<String[]> data = reader.readAll();
        List<String[]> data = new ArrayList<>();
        String[] arr = reader.readNext();
        while (arr != null) {
          arr = reader.readNext();
        }

//        // clean-up.. should be cleaned up, else csv reader cannot read
//        for (String[] arr : data) {
//          for (int j = 0; j < arr.length; j++) {
//            arr[j] = arr[j].replaceAll(",", " ");
//            arr[j] = arr[j].replaceAll("\n", " ");
//            arr[j] = arr[j].replaceAll("\t", " ");
//          }
//        }
        headers.add(file.getName()); // Store filename as header
        allData.add(data);
      } catch (IOException | CsvException e) {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
      }
    }

    return mergeData(allData, headers);
  }

  // Helper method to merge CSV data from left to right with headers
  private static List<String[]> mergeData(List<List<String[]>> allData, List<String> headers) {
    List<String[]> mergedList = new ArrayList<>();

    // Add header row with filenames
    mergedList.add(headers.toArray(new String[0]));

    // Determine the max number of rows
    int maxRows = allData.stream().mapToInt(List::size).max().orElse(0);

    for (int i = 0; i < maxRows; i++) {
      List<String> mergedRow = new ArrayList<>();

      for (List<String[]> csvData : allData) {
        if (i < csvData.size()) {
          mergedRow.addAll(Arrays.asList(csvData.get(i)));
        } else {
          // Fill missing rows with empty entries
          int cols = csvData.get(0).length;
          mergedRow.addAll(Collections.nCopies(cols, ""));
        }
      }
      mergedList.add(mergedRow.toArray(new String[0]));
    }
    return mergedList;
  }

  // Method 3: Write merged data to a CSV file
  public static void writeMergedCsv(List<String[]> mergedData, File directory) {
    if (mergedData.isEmpty()) {
      LOGGER.info("No data to write.");
      NotificationFactory.openInfo("No data to write.");
      return;
    }

    File outputFile = new File(directory, "merged_output.csv");

    try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
      writer.writeAll(mergedData);
      LOGGER.info("Merged CSV saved to: " + outputFile.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }

/*
    List<String[]> mergedData = mergeCsvFiles(stage);
    if (!mergedData.isEmpty()) {
      writeMergedCsv(mergedData, new File(".")); // Save in current directory
    }
 */

  /*
      MenuItem mergeCVSMenuItem = new MenuItem("Merge CSV files");
    toolsMenu.getItems().addAll(mergeCVSMenuItem);
    mergeCVSMenuItem.setOnAction(e -> {
      Stage stage = SpTool3Main.getMainStage();

      Pair<File[], File> csvFiles = CsvMerger.selectCsvFiles(stage);

      List<String[]> mergedData = CsvMerger.mergeCsvFiles(csvFiles.getKey());
      if (!mergedData.isEmpty()) {
        CsvMerger.writeMergedCsv(mergedData, csvFiles.getValue()); // Save in current directory
      }
    });
   */

}
