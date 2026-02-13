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

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockCollectionHorizontal implements BlockCollection {

  private final List<TabularBlockHorizontal> blocks;
  private Iterator<TabularBlockHorizontal> blockIterator;
  private int lineCount;
  private int overheadLineCount;
  private int labelLineCount;

  public BlockCollectionHorizontal(List<TabularBlockHorizontal> blocks) {
    this.blocks = blocks;
    this.blockIterator = Iterables.cycle(blocks).iterator();
    lineCount = 0;
    overheadLineCount = 0;
    labelLineCount = 0;
  }

  public BlockCollectionHorizontal() {
    this.blocks = new ArrayList<>();
    this.blockIterator = Iterables.cycle(blocks).iterator();
    lineCount = 0;
    overheadLineCount = 0;
    labelLineCount = 0;
  }

  @Override
  public void addBlock(TabularBlock block) {
    if (block instanceof TabularBlockHorizontal) {
      // store to dynamically adjust spacing
      overheadLineCount = Math.max(overheadLineCount, block.getOverheadLineCount());
      labelLineCount = Math.max(labelLineCount, block.getLabelLineCount());
      blocks.add((TabularBlockHorizontal) block);
      setupIterators();
    }
  }

  public void setupIterators() {
    this.blockIterator = Iterables.cycle(blocks).iterator();
    for (TabularBlockHorizontal block : blocks) {
      block.updateIterators(overheadLineCount, labelLineCount);
      lineCount = Math.max(lineCount, block.getLineCount());
    }
  }

  @Override
  public void exportBlock(ExportWriter writer) {
    List<String> line = new ArrayList<>();
    int lineCounter = 0;
    while (lineCounter < lineCount) {
      lineCounter++;
      int blockCounter = 0;
      while (blockCounter < blocks.size()) {
        blockCounter++;
        TabularBlockHorizontal block = blockIterator.next();
        int columnCounter = 0;
        while (columnCounter < block.getColumnCount()) {
          columnCounter++;
          TabularColumn col = block.nextColumn();
          line.add(cleanCSVField(col.getNext()));
        }
        line.add("");
      }
      writer.writeLine(line);
      line.clear();
    }
    writer.writeLine(new String[]{""});
  }
}