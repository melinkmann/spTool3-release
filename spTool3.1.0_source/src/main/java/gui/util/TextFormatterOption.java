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

package gui.util;

import java.io.Serializable;

public enum TextFormatterOption implements Serializable {

  ASSURE_LETTER,
  ASSURE_NUMERAL_OR_LETTER,
  ASSURE_EXTD_NUMERAL_OR_LETTER,
  ASSURE_CAPITAL_LETTER,
  ASSURE_CHARACTER,

  ASSURE_INTEGER,
  ASSURE_POSITIVE_INTEGER,
  ASSURE_NONZERO_POSITIVE_INTEGER,

  ASSURE_EXP_INTEGER,
  ASSURE_POSITIVE_EXP_INTEGER,
  ASSURE_NONZERO_POSITIVE_EXP_INTEGER,

  ASSURE_DOUBLE,
  ASSURE_POSITIVE_DOUBLE,
  ASSURE_NONZERO_POSITIVE_DOUBLE,

  ASSURE_ANY_DOUBLE_TOLERATING_SPACE,

  ASSURE_EXP_DOUBLE,
  ASSURE_POS_EXP_DOUBLE,
  ASSURE_NONZERO_POS_EXP_DOUBLE,

  ALL_PASS;

}
