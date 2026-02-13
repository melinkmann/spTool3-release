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

import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CsvExportWriter implements ExportWriter {

  private static final Logger LOGGER = LogManager.getLogger(CsvExportWriter.class);

  // static declarations
  private static final char delimiterCSV = ',';
  private static final String sep = "\r";

  private final File location;

  // idea: use stream --> FileWriter needs 3 methdos
  // (1) initialize writer with File
  // (2) Writer::write(List<Data>)
  // (3) close the writer

  private final CSVWriter csvWriter;

  public CsvExportWriter(File location) {
    this.location = location;
    CSVWriter tempWriter;
    try {
      FileWriter fileWriter = new FileWriter(location); // throws the IOException
      tempWriter = new CSVWriter(fileWriter, delimiterCSV, CSVWriter.NO_QUOTE_CHARACTER,
          CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
    } catch (IOException e) {
      tempWriter = null;
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    this.csvWriter = tempWriter;
  }

  @Override
  public void writeLine(String[] line) {
    if (csvWriter != null) {
      csvWriter.writeNext(line);
    }
  }

  @Override
  public void writeLine(List<String> line) {
    if (csvWriter != null) {
      csvWriter.writeNext(line.toArray(new String[0]));
    }
  }

  @Override
  public void writeLines(List<List<String>> lines) {
    if (csvWriter != null && !lines.isEmpty()) {
      for (List<String> line : lines) {
        writeLine(line);
      }
    }
  }

  @Override
  public void close() {
    if (csvWriter != null) {
      try {
        csvWriter.close();
        LOGGER.info("Wrote to file: " + location);
      } catch (IOException e) {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
      }
    }
  }


}




