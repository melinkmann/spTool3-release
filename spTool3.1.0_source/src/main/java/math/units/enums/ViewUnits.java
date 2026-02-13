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

package math.units.enums;

import math.units.ConvertibleUnit;
import math.units.Unit;

public enum ViewUnits implements Unit {


  NONE {
    @Override
    public String getLiteralString() {
      return "-";
    }
  },

  ABS_FREQUENCY {
    @Override
    public String getLiteralString() {
      return "#";
    }
  },

  NP_PER_SECOND {
    @Override
    public String getLiteralString() {
      return "NP/s";
    }
  },

  NP {
    @Override
    public String getLiteralString() {
      return "NP";
    }
  }

}
