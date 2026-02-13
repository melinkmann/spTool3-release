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

public class TabularBlockVertical implements TabularBlock {

  // Note: Columns are lines in this class.
  private final List<TabularColumn> lines;
  private Iterator<TabularColumn> contentIterator;
  //
  private int overheadLineCount = 0;
  private int labelLineCount = 0;
  private int totalLineCount = 0;
  private int totalColCount = 0;

  public TabularBlockVertical() {
    lines = new ArrayList<>();
    // Cycle over the lines.
    contentIterator = lines.listIterator();
  }

  public void updateIterators(int maxOverheadLineCount, int maxLabelLineCount) {
    this.overheadLineCount = maxOverheadLineCount;
    this.labelLineCount = maxLabelLineCount;
    updateIterators();
  }

  private void updateIterators() {
    contentIterator = lines.listIterator();
    for (TabularColumn col : lines) {
      col.fillSpace(overheadLineCount, labelLineCount);
      totalLineCount = lines.size();
      totalColCount = Math.max(totalLineCount, col.getElementCount());
    }
  }

  public TabularColumn nextColumn() {
    return contentIterator.next();
  }

  public boolean hasNextColumn(){
    return contentIterator.hasNext();
  }

  //
  public int getLineCount() {
    return totalLineCount;
  }

  public int getColumnCount() {
    return totalColCount;
  }

  public int getLabelLineCount() {
    return labelLineCount;
  }

  public int getOverheadLineCount() {
    return overheadLineCount;
  }


  /**
   * Main add methods
   *
   * @param overhead Overhead (ideally) of the first column. In the following cols, the missing
   *                 overhead is filled with blanks anyway.
   * @param labels   Labels, i.e. "Time [s]".
   * @param data     Data. Can have various length in each column.
   */
  @Override
  public void addColumn(List<String> overhead, List<String> labels, TabularData tabularData) {
    TabularColumn col = new TabularColumn(overhead, labels, tabularData);
    totalLineCount = Math.max(totalLineCount, col.getElementCount());
    overheadLineCount = Math.max(overheadLineCount, overhead.size());
    labelLineCount = Math.max(labelLineCount, labels.size());
    lines.add(col);
  }

  /**
   * Column
   */
  @Override
  public void addColumn(TabularColumn col) {
    totalLineCount = Math.max(totalLineCount, col.getElementCount());
    overheadLineCount = Math.max(overheadLineCount, col.getOverhead().size());
    labelLineCount = Math.max(labelLineCount, col.getLabels().size());
    lines.add(col);
  }


}
