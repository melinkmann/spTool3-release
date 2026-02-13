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

public class TabularColumn {

    /*
  Overhead
   - only in the first column
   - additional information

  Label
   - actual label of the column

   Data
    - data, converted to String


      Overhead  Background contribution per NP event.
      Label     Population=SplitCorrected|Gate:MaxI|	 Population=SplitCorrected|Gate:MaxI|OverRange|
      Data..    0.663	                                 0.663
                0.878	                                 0.878

   */

  private final List<String> overhead;
  private final List<String> labels;
  private final TabularData data;

  private Iterator<Iterator<String>> contentIterator;
  private Iterator<String> currentIterator;

  public TabularColumn(List<String> overhead, List<String> labels, TabularData data) {
    // Earlier version had a LinkedList as they are better when appending. May consider this!
    this.overhead = overhead;
    this.labels = labels;
    this.data = data;
    //
    updateIterators();
  }

  public void fillSpace(int overheadLineCount, int labelLineCount) {
    int missingOverheadLines = overheadLineCount - overhead.size();
    int missingLabelLines = labelLineCount - labels.size();
    for (int i = 0; i < missingLabelLines; i++) {
      labels.add(0, "");
    }
    for (int i = 0; i < missingOverheadLines; i++) {
      overhead.add("");
    }
    updateIterators();
  }

  public void updateIterators() {
    // get iterator for each list
    Iterator<String> overheadIterator = overhead.listIterator();
    Iterator<String> labelIterator = labels.listIterator();
    Iterator<String> dataIterator = data.getContent().listIterator();
    // overall iterator
    List<Iterator<String>> content = new ArrayList<>();
    content.add(overheadIterator);
    content.add(labelIterator);
    content.add(dataIterator);
    contentIterator = content.listIterator();
    currentIterator = contentIterator.next();
  }

  /**
   * @return the next String in downward direction or an empty String if end of column.
   */
  public String getNext() {
    if (currentIterator.hasNext()) {
      return currentIterator.next();
    } else if (contentIterator.hasNext()) {
      currentIterator = contentIterator.next();
      return getNext();
    } else {
      return "";
    }
  }

  /**
   * Central method must get largest among all Columns and only iterate this long.
   *
   * @return number of lines needed for this column.
   */
  public int getElementCount() {
    return overhead.size() + labels.size() + data.getContent().size();
  }


  /**
   * Allow merging of columns
   */

  public List<String> getOverhead() {
    return overhead;
  }

  public List<String> getLabels() {
    return labels;
  }

  public TabularData getData() {
    return data;
  }


}
