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

import java.awt.Color;

public enum UiColors implements Colors {

  BLACK {
    @Override
    public Color get(int alpha) {
      return OkabeItoColors.BLACK.get(alpha);
    }

    @Override
    public Color get() {
      return OkabeItoColors.BLACK.get();
    }
  },


  GRAY {
    @Override
    public Color get(int alpha) {
      return OkabeItoColors.BLACK_LIGHT.get(alpha);
    }

    @Override
    public Color get() {
      return OkabeItoColors.BLACK_LIGHT.get();
    }
  },

  BASELINE_BLUE {
    @Override
    public Color get(int alpha) {
      return new Color(3, 0, 183, alpha);
    }

    @Override
    public Color get() {
      return new Color(3, 0, 183);
    }
  },

  SEARCH_RED {
    @Override
    public Color get(int alpha) {
      return new Color(163, 0, 0, alpha);
    }

    @Override
    public Color get() {
      return new Color(163, 0, 0);
    }
  },

  GATING_MUSTARD {
    @Override
    public Color get(int alpha) {
      return new Color(172, 179, 13, alpha);
    }

    @Override
    public Color get() {
      return new Color(172, 179, 13);
    }
  },


  FILTERING_MUSTARD {
    @Override
    public Color get(int alpha) {
      return new Color(197, 208, 115, alpha);
    }

    @Override
    public Color get() {
      return new Color(197, 208, 115);
    }
  },

  PLOT_ANY_AXIS_MARKER {
    @Override
    public Color get(int alpha) {
      return new Color(8, 8, 35, alpha);
    }

    @Override
    public Color get() {
      return new Color(54, 54, 54);
    }
  };

  public abstract java.awt.Color get();

  public abstract Color get(int alpha);

  public Color get(double alpha) {
    int alphaInt = (int) (Math.min(Math.max(alpha, 0), 1) * 255);
    return get(alphaInt);
  }


}
