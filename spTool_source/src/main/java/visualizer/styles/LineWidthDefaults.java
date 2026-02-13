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

public enum LineWidthDefaults implements LineWidth {

  NONE {
    @Override
    public float get() {
      return 0;
    }
  },

  GRID {
    @Override
    public float get() {
      return 0.25f;
    }
  },

  THINNEST {
    @Override
    public float get() {
      return 0.5f;
    }
  },

  THINNER {
    @Override
    public float get() {
      return 0.75f;
    }
  },

  THIN {
    @Override
    public float get() {
      return 1f;
    }
  },

  MEDIUM {
    @Override
    public float get() {
      return 1.5f;
    }
  },

  MEDIUM_THICK {
    @Override
    public float get() {
      return 1.75f;
    }
  },

  THICK {
    @Override
    public float get() {
      return 2f;
    }
  },

  THICKER {
    @Override
    public float get() {
      return 3f;
    }
  },

  THICKEST {
    @Override
    public float get() {
      return 4f;
    }
  };


  public abstract float get();

}
