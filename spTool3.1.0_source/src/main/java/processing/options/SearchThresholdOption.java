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

public enum SearchThresholdOption {

  MEAN {
    @Override
    public String toString() {
      return "Mean [cts]";
    }
  },

   ALPHA {
    @Override
    public String toString() {
      return "Alpha (false positive rate) [-]";
    }
  },

  FACTOR {
    @Override
    public String toString() {
      return "z-value ('z·σ') [-]";
    }
  },

  CUSTOM_VALUE {
    @Override
    public String toString() {
      return "Custom value";
    }
  };

  public static SearchThresholdOption[] getLargerThanMu() {
    return new SearchThresholdOption[]{ALPHA,FACTOR,CUSTOM_VALUE};
  }

  public static SearchThresholdOption[] getAlphaZ() {
    return new SearchThresholdOption[]{ALPHA,FACTOR};
  }

  public static SearchThresholdOption[] getAlphaZMean() {
    return new SearchThresholdOption[]{MEAN, ALPHA,FACTOR};
  }

}
