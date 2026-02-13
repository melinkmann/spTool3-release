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

package visualizer.styles;



public enum MarkerSizeDefaults implements MarkerSize{

  SMALLEST {
    @Override
    public int get() {
      return 1;
    }
  },

  SMALLER {
    @Override
    public int get() {
      return 2;
    }
  },

  SMALL {
    @Override
    public int get() {
      return 3;
    }
  },

  MEDIUM {
    @Override
    public int get() {
      return 4;
    }
  },

  LARGE {
    @Override
    public int get() {
      return 5;
    }
  },

  LARGER {
    @Override
    public int get() {
      return 6;
    }
  },

  LARGEST {
    @Override
    public int get() {
      return 8;
    }
  };


  public abstract int get();

}
