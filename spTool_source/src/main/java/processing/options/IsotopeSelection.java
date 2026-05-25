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

public enum IsotopeSelection {

  ALL_SAMPLE {
    @Override
    public String toString() {
      return "All isotopes";
    }
  }, ALL_LOADED {
    @Override
    public String toString() {
      return "All loaded isotopes";
    }
  },
  SELECTED {
    @Override
    public String toString() {
      return "All selected isotopes";
    }
  },
  POSITIVE_LIST_SELECTION {
    @Override
    public String toString() {
      return "Set list";
    }
  },
  NEGATIVE_LIST_EXCLUSION {
    @Override
    public String toString() {
      return "Exclude";
    }
  };

  public static IsotopeSelection[] getUI() {
    return new IsotopeSelection[]{
        ALL_LOADED,
        SELECTED,
        POSITIVE_LIST_SELECTION,
        NEGATIVE_LIST_EXCLUSION
    };
  }

}
