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
import java.util.Collections;
import java.util.List;

public class HeterogeneousTabularData implements TabularData {

  private final List<ContentEntry> content;

  public HeterogeneousTabularData() {
    this.content = new ArrayList<>();
  }

  public void add(String label, String value) {
    content.add(new ContentEntry(label, value));
  }

  public void add(List<String> label, String value) {
    content.add(new ContentEntry(label, value));
  }

  public void add(String label, List<String> value) {
    content.add(new ContentEntry(label, value));
  }

  public void add(List<String> label, List<String> value) {
    content.add(new ContentEntry(label, value));
  }


  @Override
  public List<String> getContent() {
    List<String> listedContent = new ArrayList<>();
    content.stream().map(ContentEntry::getContent).forEach(listedContent::addAll);
    return listedContent;
  }


  public static class ContentEntry {

    private final List<String> subLabel;
    private final List<String> subData;

    public ContentEntry(List<String> subLabel, List<String> subData) {
      this.subLabel = subLabel;
      this.subData = subData;
    }

    public ContentEntry(String subLabel, List<String> subData) {
      this.subLabel = new ArrayList<>(Collections.singletonList(subLabel));
      this.subData = subData;
    }

    public ContentEntry(List<String> subLabel, String subData) {
      this.subLabel = new ArrayList<>(subLabel);
      this.subData = new ArrayList<>(Collections.singletonList(subData));
    }

    public ContentEntry(String subLabel, String subData) {
      this.subLabel = new ArrayList<>(Collections.singletonList(subLabel));
      this.subData = new ArrayList<>(Collections.singletonList(subData));
    }

    public List<String> getSubLabel() {
      return subLabel;
    }

    public List<String> getSubData() {
      return subData;
    }

    public List<String> getContent() {
      final List<String> content = new ArrayList<>(subLabel);
      content.addAll(subData);
      return content;
    }

  }

}