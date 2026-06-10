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

import java.io.Serializable;

public interface MSID extends Serializable {

  static final double EPSILON = 0.000_001d;

  MSID copy();

  MZ getMainMZ();

  // This is the readable UI string, i.e., "197" or "56-72"
  String getUIString();

  String getUniqueXmlString();

  ChannelCategory getCategory();


}
