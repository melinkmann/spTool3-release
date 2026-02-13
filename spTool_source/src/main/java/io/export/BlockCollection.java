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

public interface BlockCollection {

  void addBlock(TabularBlock block);

  void exportBlock(ExportWriter writer);


  public default String cleanCSVField(String input) {
    input = input.replaceAll("µ", "u");
    input = input.replaceAll("σ", "s");
    input = input.replaceAll(",", ";");
    input = input.replaceAll("'", "-");
    input = input.replaceAll("±", "+/-");
    input = input.replaceAll("\n", " ");
    input = input.replaceAll("\t", " ");
    input = input.replaceAll("[^\\x20-\\x7E]", "_"); // Removes non-printable ASCII chars
    return input;
  }

}
