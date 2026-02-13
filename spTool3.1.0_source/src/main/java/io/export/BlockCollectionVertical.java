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
import java.util.Iterator;
import java.util.List;

public class BlockCollectionVertical implements BlockCollection {

  private final List<TabularBlockVertical> blocks;
  private Iterator<TabularBlockVertical> blockIterator;
  private int lineCount;
  private int overheadLineCount;
  private int labelLineCount;

  public BlockCollectionVertical(List<TabularBlockVertical> blocks) {
    this.blocks = blocks;
    this.blockIterator = blocks.listIterator();
    lineCount = 0;
    overheadLineCount = 0;
    labelLineCount = 0;
  }

  public BlockCollectionVertical() {
    this.blocks = new ArrayList<>();
    this.blockIterator = blocks.listIterator();
    lineCount = 0;
    overheadLineCount = 0;
    labelLineCount = 0;
  }

  @Override
  public void addBlock(TabularBlock block) {
    if (block instanceof TabularBlockVertical) {
      // store to dynamically adjust spacing
      overheadLineCount = Math.max(overheadLineCount, block.getOverheadLineCount());
      labelLineCount = Math.max(labelLineCount, block.getLabelLineCount());
      blocks.add((TabularBlockVertical) block);
      setupIterators();
    }
  }

  public void setupIterators() {
    this.blockIterator = blocks.listIterator();
    for (TabularBlockVertical block : blocks) {
      block.updateIterators(overheadLineCount, labelLineCount);
      lineCount = Math.max(lineCount, block.getLineCount());
    }
  }

  @Override
  public void exportBlock(ExportWriter writer) {
    while (blockIterator.hasNext()) {
      TabularBlockVertical block = blockIterator.next();

      while (block.hasNextColumn()) {
        TabularColumn col = block.nextColumn();
        List<String> line = new ArrayList<>(col.getOverhead());
        line.addAll(col.getLabels());
        line.addAll(col.getData().getContent());
        for (int i = 0; i < line.size(); i++) {
          line.set(i, cleanCSVField(line.get(i)));
        }
        writer.writeLine(line);
      }
      writer.writeLine(new String[]{""});
    }
  }
}