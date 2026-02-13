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
import java.util.List;

public class TabularSheet {

  private final List<BlockCollection> blockCollections;
  private final ExportWriter writer;


  public TabularSheet(ExportWriter writer) {
    this.blockCollections = new ArrayList<>();
    this.writer = writer;
  }

  public void addBlockCollection(BlockCollection blockCollection) {
    blockCollections.add(blockCollection);
  }

  public void addBlockCollection(BlockCollection... blockCollections) {
    this.blockCollections.addAll(new ArrayList<>(Arrays.asList(blockCollections)));
  }

  public void addBlockCollection(List<BlockCollection> blockCollections) {
    this.blockCollections.addAll(new ArrayList<>(blockCollections));
  }


  public void export() {
    for (BlockCollection blockCollection : blockCollections) {
      blockCollection.exportBlock(writer);
    }
    writer.close();
  }


  public ExportWriter getWriter() {
    return writer;
  }




}
