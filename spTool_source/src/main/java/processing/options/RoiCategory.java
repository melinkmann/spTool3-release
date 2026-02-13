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

public enum RoiCategory {

  ABSOLUTE_VALUES {
    @Override
    public String toString() {
      return "Custom limits";
    }

    @Override
    public String getShortLabel() {
      return "Lim";
    }
  },

  PERCENTILES {
    @Override
    public String toString() {
      return "Percentiles";
    }


    @Override
    public String getShortLabel() {
      return "Pct";
    }
  },

  IQR {
    @Override
    public String toString() {
      return "Inter-quartile range";
    }


    @Override
    public String getShortLabel() {
      return "IQR";
    }
  },


  MAD {
    @Override
    public String toString() {
      return "Mean absolute deviation";
    }


    @Override
    public String getShortLabel() {
      return "MAD";
    }
  };


  public abstract String getShortLabel();
}
