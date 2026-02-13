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

public enum OkabeItoColors implements Colors {

  BLACK {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(0, 0, 0, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(0, 0, 0);
    }
  },

  BLACK_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(122, 122, 122, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(122, 122, 122);
    }
  },

  BLACK_DARK { //61, 61, 61
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(36, 36, 36, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(36, 36, 36);
    }
  },

  ORANGE {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(230, 159, 0, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(230, 159, 0);
    }
  },

  ORANGE_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(255, 199, 66, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(255, 199, 66);
    }
  },

  ORANGE_DARK {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(155, 108, 0, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(155, 108, 0);
    }
  },

  SKY_BLUE {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(86, 180, 233, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(86, 180, 233);
    }
  },

  SKY_BLUE_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(129, 201, 239, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(129, 201, 239);
    }
  },

  SKY_BLUE_DARK { //24, 132, 190
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(20, 111, 160, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(20, 111, 160);
    }
  },

  GREEN_BLUE {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(0, 158, 115, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(0, 158, 115);
    }
  },

  GREEN_BLUE_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(26, 255, 192, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(26, 255, 192);
    }
  },

  GREEN_BLUE_DARK {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(0, 106, 78, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(0, 106, 78);
    }
  },

  YELLOW {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(240, 228, 66, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(240, 228, 66);
    }
  },

  YELLOW_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(244, 235, 117, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(244, 235, 117);
    }
  },

  YELLOW_DARK {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(191, 179, 15, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(191, 179, 15);
    }
  },

  VIOLET {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(0, 114, 178, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(0, 114, 178);
    }
  },

  VIOLET_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(36, 179, 255, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(36, 179, 255);
    }
  },

  VIOLET_DARK { //0, 78, 119
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(0, 70, 104, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(0, 70, 104);
    }
  },

  VERMILION {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(213, 94, 0, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(213, 94, 0);
    }
  },

  VERMILION_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(255, 146, 55, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(255, 146, 55);
    }
  },

  VERMILION_DARK { //145, 65, 0
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(121, 54, 0, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(121, 54, 0);
    }
  },

  PINK {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(204, 121, 167, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(204, 121, 167);
    }
  },

  PINK_LIGHT {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(217, 153, 188, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(217, 153, 188);
    }
  },

  PINK_DARK {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(158, 61, 115, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(158, 61, 115);
    }
  };

  public abstract java.awt.Color get();

  public Color get(double alpha) {
    int alphaInt = (int) (Math.min(Math.max(alpha, 0), 1) * 255);
    return get(alphaInt);
  }

  public abstract Color get(int alpha);

  public static java.awt.Color[] getColors(double alpha) {
    int alphaInt = (int) (Math.min(Math.max(alpha, 0), 1) * 255);
    java.awt.Color[] cols = new java.awt.Color[]{
        OkabeItoColors.ORANGE.get(alphaInt),
        OkabeItoColors.GREEN_BLUE.get(alphaInt),
        OkabeItoColors.VIOLET.get(alphaInt),
        OkabeItoColors.VERMILION.get(alphaInt),
        OkabeItoColors.SKY_BLUE.get(alphaInt),
        OkabeItoColors.BLACK.get(alphaInt),
        OkabeItoColors.YELLOW.get(alphaInt),
        OkabeItoColors.PINK.get(alphaInt),
    };
    return cols;
  }

  public static java.awt.Color[] getColors() {
    return getColors(1);
  }

  public static java.awt.Color[] getColorsBright(double alpha) {
    int alphaInt = (int) (Math.min(Math.max(alpha, 0), 1) * 255);
    java.awt.Color[] cols = new java.awt.Color[]{
        OkabeItoColors.ORANGE_LIGHT.get(alphaInt),
        OkabeItoColors.GREEN_BLUE_LIGHT.get(alphaInt),
        OkabeItoColors.VIOLET_LIGHT.get(alphaInt),
        OkabeItoColors.VERMILION_LIGHT.get(alphaInt),
        OkabeItoColors.SKY_BLUE_LIGHT.get(alphaInt),
        OkabeItoColors.BLACK_LIGHT.get(alphaInt),
        OkabeItoColors.YELLOW_LIGHT.get(alphaInt),
        OkabeItoColors.PINK_LIGHT.get(alphaInt),
    };
    return cols;
  }

  public static java.awt.Color[] getColorsBright() {
    return getColorsBright(1);
  }

  public static java.awt.Color[] getColorsDark(double alpha) {
    int alphaInt = (int) (Math.min(Math.max(alpha, 0), 1) * 255);
    java.awt.Color[] cols = new java.awt.Color[]{
        OkabeItoColors.ORANGE_DARK.get(alphaInt),
        OkabeItoColors.GREEN_BLUE_DARK.get(alphaInt),
        OkabeItoColors.VIOLET_DARK.get(alphaInt),
        OkabeItoColors.VERMILION_DARK.get(alphaInt),
        OkabeItoColors.SKY_BLUE_DARK.get(alphaInt),
        OkabeItoColors.BLACK_DARK.get(alphaInt),
        OkabeItoColors.YELLOW_DARK.get(alphaInt),
        OkabeItoColors.PINK_DARK.get(alphaInt),
    };
    return cols;
  }

  public static java.awt.Color[] getColorsDark() {
    return getColorsDark(1);
  }


}
