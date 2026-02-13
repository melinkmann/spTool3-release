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

import core.SpTool3Main;
import java.awt.Font;

public enum FontStyles {

  SMALLEST {
    @Override
    public Font get(int stroke) {
      return new Font(FONT_NAME, stroke, 10);
    }
  },

  SMALLER {
    @Override
    public Font get(int stroke) {
      return new Font(FONT_NAME, stroke, 11);
    }
  },

  SMALL {
    @Override
    public Font get(int stroke) {
      return new Font(FONT_NAME, stroke, 12);
    }
  },

  NORMAL {
    @Override
    public Font get(int stroke) {
      return new Font(FONT_NAME, stroke, 13);
    }
  },

  LARGE {
    @Override
    public Font get(int stroke) {
      return new Font(FONT_NAME, stroke, 14);
    }
  },

  LARGER {
    @Override
    public Font get(int stroke) {
      return new Font(FONT_NAME, stroke, 16);
    }
  },

  LARGEST {
    @Override
    public Font get(int stroke) {
      return new Font(FONT_NAME, stroke, 18);
    }
  };

  public abstract Font get(int stroke);

  private static final String FONT_NAME = "Arial"; //Tahoma

  public Font get() {
    return get(Font.PLAIN);
  }

  public javafx.scene.text.Font getFX() {
    Font font = get(Font.PLAIN);
    return new javafx.scene.text.Font(font.getFontName(), font.getSize());
  }

  public static Font getPlain() {
    int size = SpTool3Main.getRunTime().getConfParams().getAxisFontSize();
    return new Font(FONT_NAME, Font.PLAIN, size);
  }

  public static Font getPlain(int decrease) {
    int size = SpTool3Main.getRunTime().getConfParams().getAxisFontSize()-decrease;
    return new Font(FONT_NAME, Font.PLAIN, size);
  }

  public static Font getBold() {
    int size = SpTool3Main.getRunTime().getConfParams().getAxisFontSize();
    return new Font(FONT_NAME, Font.BOLD, size);
  }


}
