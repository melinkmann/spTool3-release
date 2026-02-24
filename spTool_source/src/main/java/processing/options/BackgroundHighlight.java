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

import visualizer.charts.BarFillPattern;
import visualizer.styles.Colors;

public enum BackgroundHighlight {
  DARKER {
    @Override
    public String toString() {
      return "Darker color";
    }

    @Override
    public BarFillPattern getPattern(EventType eventType) {
      return BarFillPattern.NONE;
    }

    @Override
    public Colors getCol(EventType eventType, Colors color) {
      if (eventType.equals(EventType.NP)) {
        return color;
      } else {
        return Colors.blacker(color);
      }
    }
  },

  BRIGHTER {
    @Override
    public String toString() {
      return "Brighter color";
    }

    @Override
    public BarFillPattern getPattern(EventType eventType) {
      return BarFillPattern.NONE;
    }

    @Override
    public Colors getCol(EventType eventType, Colors color) {
      if (eventType.equals(EventType.NP)) {
        return color;
      } else {
        return Colors.whiter(color);
      }
    }
  },

  DOTS {
    @Override
    public String toString() {
      return "Dots";
    }

    @Override
    public BarFillPattern getPattern(EventType eventType) {
      if (eventType.equals(EventType.NP)) {
        return BarFillPattern.NONE;
      } else {
        return BarFillPattern.DOTS;
      }
    }

    @Override
    public Colors getCol(EventType eventType, Colors color) {
      return color;
    }
  },

  DIAMONDS {
    @Override
    public String toString() {
      return "Diamonds";
    }

    @Override
    public BarFillPattern getPattern(EventType eventType) {
      if (eventType.equals(EventType.NP)) {
        return BarFillPattern.NONE;
      } else {
        return BarFillPattern.DIAMONDS;
      }
    }

    @Override
    public Colors getCol(EventType eventType, Colors color) {
      return color;
    }
  },

  STRIPES {
    @Override
    public String toString() {
      return "Stripes";
    }

    @Override
    public BarFillPattern getPattern(EventType eventType) {
      if (eventType.equals(EventType.NP)) {
        return BarFillPattern.NONE;
      } else {
        return BarFillPattern.STRIPES;
      }
    }

    @Override
    public Colors getCol(EventType eventType, Colors color) {
      return color;
    }
  };


  public abstract BarFillPattern getPattern(EventType eventType);

  public abstract Colors getCol(EventType eventType, Colors color);

}
