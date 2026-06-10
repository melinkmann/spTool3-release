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

package dataModelNew.mz;

import org.jetbrains.annotations.Nullable;
import sandbox.montecarlo.Isotope;

import java.io.Serializable;

public interface Channel extends Serializable {

  @Nullable Isotope getIsotope();

  double getMZ();

  String getMZStr();

  String getShortUIString();

  // Default String to show on the UI: Isotope -> 197Au
  String getUIString();

  // Category: For an isotope, we show its element.
  ChannelCategory getCategory();

  // This is for parsing into an XML file, e.g., to store default colors.
  String getColorXmlIDString();

  String getColorMatcherString();

  boolean isColorIDString(String str);

  Channel copy();

  static String wrap(String str) {
    return "{" + str + "}";
  }
}

