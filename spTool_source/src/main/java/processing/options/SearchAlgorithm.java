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

public enum SearchAlgorithm {

  SIMPLE {
    @Override
    public String toString() {
      return "Simple search (if DT >> 1 ms)";
    }

    @Override
    public String shortString() {
      return "Point search";
    }
  },

  SPLIT_CORRECTION {
    @Override
    public String toString() {
      return "Split correction";
    }

    @Override
    public String shortString() {
      return "Peak search";
    }
  },

  SPLIT_CORRECTION_SMOOTH {
    @Override
    public String toString() {
      return "Smooth and split correction";
    }

    @Override
    public String shortString() {
      return "Smoothed search";
    }
  },

  SPLIT_CORRECTION_WINDOW {
    @Override
    public String toString() {
      return "Split correction with window";
    }

    @Override
    public String shortString() {
      return "Window search";
    }
  },

  P_VALUE_ACCUMULATION {
    @Override
    public String toString() {
      return "Accumulated p value";
    }

    @Override
    public String shortString() {
      return "Accumulated p-search";
    }
  };


  public static SearchAlgorithm[] getActiveValues() {
    List<SearchAlgorithm> active = new ArrayList<>();
    active.add(SIMPLE);
    active.add(SPLIT_CORRECTION);

    if (SpTool3Main.SHOW_WINDOW) {
      active.add(SPLIT_CORRECTION_SMOOTH);
      active.add(SPLIT_CORRECTION_WINDOW);
    }

    if (SpTool3Main.SHOW_ACCUMULATION) {
      active.add(P_VALUE_ACCUMULATION);
    }
    return active.toArray(new SearchAlgorithm[]{});
  }

  public abstract String shortString();

}
