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

public enum SmoothType {

  NONE {
    @Override
    public String toString() {
      return "No smooth";
    }
  },

  MOAV {
    @Override
    public String toString() {
      return "Moving average";
    }
  },

  SAVITZKY_GOLAY {
    @Override
    public String toString() {
      return "Savitzky-Golay";
    }
  },

  GAUSSIAN_KERNEL {
    @Override
    public String toString() {
      return "Gaussian kernel";
    }
  },

  LOESS {
    @Override
    public String toString() {
      return "LOESS";
    }
  },

  SINC_KERNEL {
    @Override
    public String toString() {
      return "Sinc kernel";
    }
  };


}
