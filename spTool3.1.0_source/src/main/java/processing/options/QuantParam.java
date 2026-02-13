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

public enum QuantParam {
  RAW_MEAN {
    @Override
    public String toString() {
      return "Raw data mean";
    }
  },
  RAW_MEDIAN {
    @Override
    public String toString() {
      return "Raw data median";
    }
  },
  NP_AREA_MEAN {
    @Override
    public String toString() {
      return "Particle area mean";
    }
  },
  NP_AREA_MEDIAN {
    @Override
    public String toString() {
      return "Particle area median";
    }
  },
  NP_HEIGHT_MEAN {
    @Override
    public String toString() {
      return "Particle height mean";
    }
  },
  NP_HEIGHT_MEDIAN {
    @Override
    public String toString() {
      return "Particle height median";
    }
  };

  public static QuantParam[] getIonic() {
    return new QuantParam[]{
        RAW_MEAN,
        RAW_MEDIAN
    };
  }

  public static QuantParam[] getNP() {
    return new QuantParam[]{
        NP_AREA_MEAN,
        NP_AREA_MEDIAN,
        NP_HEIGHT_MEAN,
        NP_HEIGHT_MEDIAN
    };
  }
}
