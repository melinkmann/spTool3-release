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

package io.fastExport;

import io.export.ExportWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TabBlockColl {

  private final ExportWriter writer;
  private final List<TabBlock> blocks = new ArrayList<>();
  private final boolean introduceSpacers;

  public TabBlockColl(ExportWriter writer, boolean introduceSpacers) {
    this.writer = writer;
    this.introduceSpacers = introduceSpacers;
  }

  public void add(TabBlock block) {
    if (!block.isEmpty()) {
      blocks.add(block);
    }
  }


  // I think, here we should not create yet another massive array but just write the lines...
  public void export() {

    List<String[]> columns = new ArrayList<>();

    // Determine spacing for the overhead and label
    int globalMetadataLength = 0;
    int globalOverheadLength = 0;
    int globalLabelLength = 0;

    for (TabBlock block : blocks) {
      globalMetadataLength = Math.max(globalMetadataLength, block.getLocalMetadataLength());
      globalOverheadLength = Math.max(globalOverheadLength, block.getLocalOverheadLength());
      globalLabelLength = Math.max(globalLabelLength, block.getLocalLabelLength());
    }

    for (TabBlock block : blocks) {
      block.setGlobalMetadataLength(globalMetadataLength);
      block.setGlobalOverheadLength(globalOverheadLength);
      block.setGlobalLabelLength(globalLabelLength);
    }

    // Check spacer rules
    if (introduceSpacers && blocks.size() > 1) {
      for (int i = 0; i < blocks.size() - 1; i++) {
        blocks.get(i).setAddSpacer(true);
      }
    }

    // Extract the columns
    for (int i = 0; i < blocks.size(); i++) {
      // Add spacer only if this is not the last block
      // boolean needsSpacer = blocks.size() > 1 && !(i == blocks.size() - 1);
      columns.addAll(blocks.get(i).getColumns());
    }

    int maxColLength = columns.stream()
        .map(arr -> arr.length)
        .mapToInt(Integer::intValue)
        .max().orElse(0);

    // Iterate over each line
    int currentLine = 0;

    while (currentLine < maxColLength) {

      String[] line = new String[columns.size()];
      Arrays.fill(line, ""); // avoid "null" everywhere

      for (int colIdx = 0; colIdx < columns.size(); colIdx++) {

        String[] col = columns.get(colIdx);

        if (currentLine < col.length) {
          line[colIdx] = col[currentLine];
        }
      }
      currentLine++;

      writer.writeLine(line);
    }

    writer.close();
  }

  public void write(List<String> l) {
    writer.writeLine(l);
  }

}
