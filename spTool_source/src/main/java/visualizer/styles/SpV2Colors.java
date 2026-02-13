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

public enum SpV2Colors implements Colors {

  BLACK {
    @Override
    public Color get(int alpha) {
      return new java.awt.Color(0, 0, 0, alpha);
    }

    @Override
    public Color get() {
      return new java.awt.Color(0, 0, 0);
    }
  },

  BLUE {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(13, 41, 158, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(13, 41, 158);
    }
  },

  YELLOW {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(183, 189, 24, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(183, 189, 24);
    }
  },

  RED {
    @Override
    public java.awt.Color get(int alpha) {
      return new java.awt.Color(134, 5, 5, alpha);
    }

    @Override
    public java.awt.Color get() {
      return new java.awt.Color(134, 5, 5);
    }
  },

  GREEN {
    @Override
    public Color get(int alpha) {
      return new java.awt.Color(38, 173, 69, alpha);
    }

    @Override
    public Color get() {
      return new java.awt.Color(38, 173, 69);
    }
  },

  GREY {
    @Override
    public Color get(int alpha) {
      return new java.awt.Color(102, 102, 107, alpha);
    }

    @Override
    public Color get() {
      return new java.awt.Color(102, 102, 107);
    }
  },

  MAGENTA {
    @Override
    public Color get(int alpha) {
      return new java.awt.Color(100, 15, 123, alpha);
    }

    @Override
    public Color get() {
      return new java.awt.Color(100, 15, 123);
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
        SpV2Colors.BLACK.get(alphaInt),
        SpV2Colors.BLUE.get(alphaInt),
        SpV2Colors.YELLOW.get(alphaInt),
        SpV2Colors.RED.get(alphaInt),
        SpV2Colors.GREEN.get(alphaInt),
        SpV2Colors.MAGENTA.get(alphaInt),
        SpV2Colors.GREY.get(alphaInt),
    };
    return cols;
  }

  public static java.awt.Color[] getColors() {
    return getColors(1);
  }

}
