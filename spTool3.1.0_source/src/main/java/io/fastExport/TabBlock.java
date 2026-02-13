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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabBlock {

  private final String[] meta;
  private final List<TabCol> columns;
  private final Orientation orientation;

  private int globalMetadataLength = 0;
  private int globalOverheadLength = 0;
  private int globalLabelLength = 0;

  private boolean addSpacer = false;

  public TabBlock(String[] meta, List<TabCol> columns, Orientation orientation) {
    this.meta = meta;
    this.columns = columns;
    this.orientation = orientation;
  }

  public TabBlock() {
    this.meta = new String[0];
    this.columns = new ArrayList<>();
    this.orientation = Orientation.HORZ;
  }

  public TabBlock(String[] metaData) {
    this.meta = metaData;
    this.columns = new ArrayList<>();
    this.orientation = Orientation.HORZ;
  }

  public boolean isEmpty() {
    return meta.length == 0 && columns.stream().allMatch(TabCol::isEmpty);
  }

  public void addCol(TabCol col) {
    this.columns.add(col);
  }

  public static enum Orientation {
    HORZ, VERT
  }

  public void setAddSpacer(boolean addSpacer) {
    this.addSpacer = addSpacer;
  }

  public void setGlobalMetadataLength(int globalMetadataLength) {
    this.globalMetadataLength = globalMetadataLength;
  }

  public void setGlobalOverheadLength(int globalOverheadLength) {
    this.globalOverheadLength = globalOverheadLength;
  }

  public void setGlobalLabelLength(int globalLabelLength) {
    this.globalLabelLength = globalLabelLength;
  }

  public int getLocalMetadataLength() {
    return meta.length;
  }

  public int getLocalLabelLength() {
    int labelLength = columns.stream()
        .map(c -> c.getLabel().length)
        .mapToInt(Integer::intValue)
        .max().orElse(0);
    return labelLength;
  }


  public int getLocalOverheadLength() {
    int overheadLength = columns.stream()
        .map(c -> c.getOverhead().length)
        .mapToInt(Integer::intValue)
        .max().orElse(0);

    return overheadLength;
  }


  public List<String[]> getColumns() {
    List<String[]> cols = new ArrayList<>();

    if (orientation.equals(Orientation.HORZ)) {

      // Find out how long the label, ... sections are
      int metadataLength = Math.max(globalMetadataLength, getLocalMetadataLength());
      int overheadLength = Math.max(globalOverheadLength, getLocalOverheadLength());
      int labelLength = Math.max(globalLabelLength, getLocalLabelLength());

      int dataLength = columns.stream()
          .map(c -> c.getValues().length)
          .mapToInt(Integer::intValue)
          .max().orElse(0);

      int colLength = metadataLength + overheadLength + labelLength + dataLength;

      for (int i = 0; i < columns.size(); i++) {

        TabCol col = columns.get(i);

        String[] colStr = new String[colLength];
        Arrays.fill(colStr, ""); // avoid "null" everywhere

        int currentPosition = 0;

        if (i == 0) {
          System.arraycopy(meta, 0, colStr, currentPosition, meta.length);
        } // else: just leave empty
        currentPosition += metadataLength;

        String[] overhead = col.getOverhead();
        System.arraycopy(overhead, 0, colStr, currentPosition, overhead.length);
        currentPosition += overheadLength;

        String[] labels = col.getLabel();
        // for the labels, indentation should be "bottom"
        int labelOffset = globalLabelLength - labels.length;
        currentPosition += labelOffset;
        System.arraycopy(labels, 0, colStr, currentPosition, labels.length);
        currentPosition -= labelOffset; // undo the label offset
        currentPosition += labelLength;

        String[] data = col.getValues();
        System.arraycopy(data, 0, colStr, currentPosition, data.length);
        currentPosition += dataLength;

        cols.add(colStr);
      }

      // Add one free col at the end of each block as a spacer
      if (addSpacer) {
        String[] colStr = new String[colLength];
        Arrays.fill(colStr, ""); // avoid "null" everywhere
        cols.add(colStr);
      }
    }

    return cols;
  }


}
