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

public enum JaasColors implements Colors {

  BLACK {
    @Override
    public Color get(int alpha) {
      return new Color(0, 0, 0, alpha);
    }

    @Override
    public Color get() {
      return new Color(0, 0, 0);
    }
  },

  DATA_GRAY {
    @Override
    public Color get(int alpha) {
      return new Color(85, 85, 85, alpha);
    }

    @Override
    public Color get() {
      return new Color(85, 85, 85);
    }
  },

  BLUE {
    @Override
    public Color get(int alpha) {
      return new Color(22, 29, 228, alpha);
    }

    @Override
    public Color get() {
      return new Color(22, 29, 228);
    }
  },

  BLUE_DARK {
    @Override
    public Color get(int alpha) {
      return new Color(20, 11, 115, alpha);
    }

    @Override
    public Color get() {
      return new Color(20, 11, 115);
    }
  },

  GREEN {
    @Override
    public Color get(int alpha) {
      return new Color(21, 179, 74, alpha);
    }

    @Override
    public Color get() {
      return new Color(21, 179, 74);
    }
  },

  GREEN_DARK {
    @Override
    public Color get(int alpha) {
      return new Color(13, 122, 49, alpha);
    }

    @Override
    public Color get() {
      return new Color(13, 122, 49);
    }
  },

  RED_HISTO_EDGE {
    @Override
    public Color get(int alpha) {
      return new Color(94, 81, 81, alpha);
    }

    @Override
    public Color get() {
      return new Color(94, 81, 81);
    }
  },

  RED_HISTO_FACE {
    @Override
    public Color get(int alpha) {
      return new Color(125, 107, 107, alpha);
    }

    @Override
    public Color get() {
      return new Color(125, 107, 107);
    }
  },

  BLUE_HISTO_EDGE {
    @Override
    public Color get(int alpha) {
      return new Color(74, 84, 99, alpha);
    }

    @Override
    public Color get() {
      return new Color(74, 84, 99);
    }
  },

  BLUE_HISTO_FACE {
    @Override
    public Color get(int alpha) {
      return new Color(98, 111, 130, alpha);
    }

    @Override
    public Color get() {
      return new Color(98, 111, 130);
    }
  };


  public abstract Color get();

  // DOUBLE ALPHA 0 - 1 (1: normal color)
  public Color get(double alpha) {
    int alphaInt = (int) (Math.min(Math.max(alpha, 0), 1) * 255);
    return get(alphaInt);
  }

  // INT ALPHA: 0 - 255 (255: normal color)t      q qq
  public abstract Color get(int alpha);


  public static java.awt.Color[] getColors(double alpha) {
    int alphaInt = (int) (Math.min(Math.max(alpha, 0), 1) * 255);
    java.awt.Color[] cols = new java.awt.Color[]{
        JaasColors.DATA_GRAY.get(alphaInt),
        JaasColors.BLUE.get(alphaInt),
        JaasColors.GREEN.get(alphaInt),
        JaasColors.BLUE_DARK.get(alphaInt),
        JaasColors.GREEN_DARK.get(alphaInt),
        JaasColors.RED_HISTO_FACE.get(alphaInt),
        JaasColors.RED_HISTO_EDGE.get(alphaInt),
        JaasColors.BLUE_HISTO_FACE.get(alphaInt),
        JaasColors.BLUE_HISTO_EDGE.get(alphaInt),
    };
    return cols;
  }


  public static java.awt.Color[] getColors() {
    return getColors(1);
  }

}
