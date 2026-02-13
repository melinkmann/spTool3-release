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

import java.util.List;
import javafx.application.Platform;
import javafx.scene.input.ClipboardContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClipboardWriter implements ExportWriter {

  private static final Logger LOGGER = LogManager.getLogger(ClipboardWriter.class);

  private final StringBuilder clipboardString = new StringBuilder();

  public ClipboardWriter() {
  }

  @Override
  public void writeLine(String[] line) {
    for (String cell : line) {
      clipboardString.append(cell);
      // separator
      clipboardString.append('\t');
    }
    // newline
    clipboardString.append('\n');
  }

  @Override
  public void writeLine(List<String> line) {
    for (String cell : line) {
      clipboardString.append(cell);
      // separator
      clipboardString.append('\t');
    }
    // newline
    clipboardString.append('\n');
  }

  @Override
  public void writeLines(List<List<String>> lines) {
    if (!lines.isEmpty()) {
      for (List<String> line : lines) {
        writeLine(line);
      }
    }
  }

  @Override
  public void close() {
    // create clipboard content
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(clipboardString.toString());
    // Clipboard seems to be a JavaFX UI object that crashes if accessed from other threads.
    Platform.runLater(()->{
      ClipboardUtil.addToClipboard(clipboardContent);
      LOGGER.info("### Clipboard writer is done. ###");
    });
  }

  public void directWrite(String content) {
    // create clipboard content
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(content);
    // Clipboard seems to be a JavaFX UI object that crashes if accessed from other threads.
    Platform.runLater(()->{
      ClipboardUtil.addToClipboard(clipboardContent);
      LOGGER.info("### Clipboard writer is done. ###");
    });

  }


}
