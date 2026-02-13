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

public enum RoiType {
  INCLUDE {
    @Override
    public String toString() {
      return "Include";
    }

    @Override
    public boolean valid(double start, double end, double value) {
      return start <= value && value <= end;
    }

    @Override
    public String getShortLabel() {
      return "In";
    }
  },

  EXCLUDE {
    @Override
    public String toString() {
      return "Exclude";
    }

    @Override
    public boolean valid(double start, double end, double value) {
      return value < start || end < value;
    }

    @Override
    public String getShortLabel() {
      return "Ex";
    }
  };

  public abstract boolean valid(double start, double end, double value);


  public abstract String getShortLabel();
}
