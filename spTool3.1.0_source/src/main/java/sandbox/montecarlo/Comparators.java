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

package sandbox.montecarlo;

public enum Comparators {

  GREATER {
    @Override
    public boolean is(double d1, double d2) {
      return d1 > d2;
    }
  },
  SMALLER {
    @Override
    public boolean is(double d1, double d2) {
      return d1 < d2;
    }
  },
  GREATER_EQUAL {
    @Override
    public boolean is(double d1, double d2) {
      return d1 >= d2;
    }
  },
  SMALLER_EQUAL {
    @Override
    public boolean is(double d1, double d2) {
      return d1 <= d2;
    }
  },
  EQUAL {
    @Override
    public boolean is(double d1, double d2) {
      return d1 == d2;
    }
  };

  public abstract boolean is(double d1, double d2);
}
