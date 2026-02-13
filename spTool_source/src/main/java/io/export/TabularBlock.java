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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface TabularBlock {

  void updateIterators(int maxOverheadLineCount, int maxLabelLineCount);

  public TabularColumn nextColumn();

  boolean hasNextColumn();

  public int getLineCount();

  public int getColumnCount();

  public int getLabelLineCount();

  public int getOverheadLineCount();

  public void addColumn(List<String> overhead, List<String> labels, TabularData tabularData);

  public void addColumn(TabularColumn col);

  /**
   * Add Lists
   */
  default void addColumn(List<String> overhead, List<String> labels, List<String> data) {
    addColumn(overhead, labels, new HomogenousTabularData(data));
  }

  default void addColumn(String overhead, String label, List<String> data) {
    List<String> overheads = new ArrayList<>(Collections.singleton(overhead));
    List<String> labels = new ArrayList<>(Collections.singleton(label));
    addColumn(overheads, labels, data);
  }

  default void addColumn(String overhead, String label, String datum) {
    List<String> overheads = new ArrayList<>(Collections.singleton(overhead));
    List<String> labels = new ArrayList<>(Collections.singleton(label));
    List<String> data = new ArrayList<>(Collections.singleton(datum));
    addColumn(overheads, labels, data);
  }

  default void addColumn(List<String> overheads, String label, List<String> data) {
    List<String> labels = new ArrayList<>(Collections.singleton(label));
    addColumn(overheads, labels, data);
  }

  // ... no overhead.
  default void addColumn(List<String> labels, List<String> data) {
    // no overhead provided
    List<String> emptyOverhead = new ArrayList<>();
    addColumn(emptyOverhead, labels, data);
  }

  default void addColumn(String label, List<String> data) {
    List<String> labels = new ArrayList<>(Collections.singleton(label));
    addColumn(labels, data);
  }

  default void addColumn(String label, String datum) {
    List<String> labels = new ArrayList<>(Collections.singleton(label));
    List<String> data = new ArrayList<>(Collections.singleton(datum));
    addColumn(labels, data);
  }


  // Arrays
  default void addColumn(List<String> overheads, String label, String[] data) {
    addColumn(overheads, label, new ArrayList<>(Arrays.asList(data)));
  }

  default void addColumn(List<String> overheads, List<String> labels, String[] data) {
    addColumn(overheads, labels, new ArrayList<>(Arrays.asList(data)));
  }

  default void addColumn(String overhead, String label, String[] data) {
    addColumn(overhead, label, new ArrayList<>(Arrays.asList(data)));
  }

  default void addColumn(String label, String[] data) {
    addColumn(label, new ArrayList<>(Arrays.asList(data)));
  }

      /*
  TODO
    If necessary, consider add methods for array iterators at least for the DATA.
    Iterator<Integer> iterator = Ints.asList(arr).iterator(); (List and Iterator e.g. from Guava)
   */


}
