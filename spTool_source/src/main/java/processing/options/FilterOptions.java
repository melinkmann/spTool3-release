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

package processing.options;

import core.SpTool3Main;

import java.util.ArrayList;
import java.util.List;

public enum FilterOptions {

  OVERLAP {
    @Override
    public String toString() {
      return "Coincidence";
    }
  },

  OVER_RANGE {
    @Override
    public String toString() {
      return "Over range";
    }
  },


  /*
  ROI based on user input - for each element
   */
  ROI_REGION {
    @Override
    public String toString() {
      return "ROI";
    }
  },

  ALIGNED_FILTER {
    @Override
    public String toString() {
      return "Aligned filtering";
    }
  },

  MATCH_SIM {
    @Override
    public String toString() {
      return "Match with synthetic population";
    }
  };

  public static FilterOptions[] getActiveValues() {
    List<FilterOptions> active = new ArrayList<>();
    active.add(OVERLAP);
    active.add(ROI_REGION);
    active.add(MATCH_SIM);

    if (SpTool3Main.getANALYZER()) {
      active.add(OVER_RANGE);
    }

    if (SpTool3Main.SHOW_ACCUMULATION) {
    }
    return active.toArray(new FilterOptions[]{});
  }

  /*
  ROI:
  - include/exclude
  - target parameters (area, height, ...)
  - A) start / end + unit
  - ALTERNATIVE:
  - B)
    # Unit (maybe global parameter?)
    # Stats: Percentile, 1.5 IQR (needs unit!), 3xMAD (needs unit!)
   */

}
