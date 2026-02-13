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

import gui.util.UiString;
import java.io.Serializable;

/**
 * comma -> ',' semicolon -> ';' tab -> '\t' underscore -> '_' space -> ' ' line -> '|' custom: not
 * intended at the moment
 * <p>
 * \r (Carriage Return) → moves the cursor to the beginning of the line without advancing to the
 * next line \n (Line Feed) → moves the cursor down to the next line without returning to the
 * beginning of the line — In a *nix environment \n moves to the beginning of the line. \r\n (End Of
 * Line) → a combination of \r and \n
 */

public enum Delimiter implements Serializable, UiString {
  COMMA {
    @Override
    public char getDelimiter() {
      return ',';
    }

    @Override
    public String getUiString() {
      return "Comma";
    }
  },
  SEMICOLON {
    @Override
    public char getDelimiter() {
      return ';';
    }

    @Override
    public String getUiString() {
      return "Semicolon";
    }
  },
  TAB {
    @Override
    public char getDelimiter() {
      return '\t';
    }

    @Override
    public String getUiString() {
      return "Tab";
    }
  },
  UNDERSCORE {
    @Override
    public char getDelimiter() {
      return '_';
    }

    @Override
    public String getUiString() {
      return "Underscore";
    }
  },
  SPACE {
    @Override
    public char getDelimiter() {
      return ' ';
    }

    @Override
    public String getUiString() {
      return "Space";
    }
  },
  VERTICAL_BAR {
    @Override
    public char getDelimiter() {
      return '|';
    }

    @Override
    public String getUiString() {
      return "Vertical bar (|)";
    }
  },
  HASHTAG {
    @Override
    public char getDelimiter() {
      return '#';
    }

    @Override
    public String getUiString() {
      return "Hash (#)";
    }
  },
  CARRIAGE_RETURN {
    @Override
    public char getDelimiter() {
      return '\r';
    }

    @Override
    public String getUiString() {
      return "Carriage return";
    }
  },
  LINE_FEED {
    @Override
    public char getDelimiter() {
      return '\n';
    }

    @Override
    public String getUiString() {
      return "Line feed";
    }
  }, CUSTOM {
    @Override
    public char getDelimiter() {
      return ',';
    }

    @Override
    public String getUiString() {
      return "Custom";
    }
  };

  public abstract char getDelimiter();

  public abstract String getUiString();
}
