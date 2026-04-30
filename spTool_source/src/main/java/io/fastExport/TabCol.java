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

import java.util.List;

public class TabCol {

  private final String[] overhead;
  private final String[] label;
  private final String[] values;

  public TabCol(String[] overhead, String[] label, String[] values) {
    this.overhead = overhead;
    this.label = label;
    this.values = values;
  }

  public TabCol(String overhead, String label, String[] values) {
    this.overhead = new String[]{overhead};
    this.label = new String[]{label};
    this.values = values;
  }

  public TabCol(String[] label, String[] values) {
    this.overhead = new String[0];
    this.label = label;
    this.values = values;
  }

  public TabCol(String label, String[] values) {
    this.overhead = new String[0];
    this.label = new String[]{label};
    this.values = values;
  }

  public TabCol(List<String> values) {
    this.overhead = new String[0];
    this.label = new String[0];
    this.values = values.toArray(new String[0]);
  }

  public boolean isEmpty() {
    return overhead.length + label.length + values.length == 0;
  }

  public String[] getOverhead() {
    return overhead;
  }

  public String[] getLabel() {
    return label;
  }

  public String[] getValues() {
    return values;
  }

}