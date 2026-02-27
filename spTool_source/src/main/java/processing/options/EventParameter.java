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

public enum EventParameter {

  AREA {
    @Override
    public String toString() {
      return "Area";
    }
  },

  NET_AREA {
    @Override
    public String toString() {
      return "Net area";
    }
  },

  HEIGHT {
    @Override
    public String toString() {
      return "Height";
    }
  },

  NET_HEIGHT {
    @Override
    public String toString() {
      return "Net height";
    }
  },

  DURATION {
    @Override
    public String toString() {
      return "Duration";
    }
  },

  NO_OF_POINTS {
    @Override
    public String toString() {
      return "Number of data points";
    }
  },

  BACKGROUND_PER_NP {
    @Override
    public String toString() {
      return "Background per event";
    }
  },

  NO_OF_EVENTS {
    @Override
    public String toString() {
      return "Number of events";
    }
  },

  ASYMMETRY_FACTOR {
    @Override
    public String toString() {
      return "Asymmetry";
    }
  },

  START_INDEX {
    @Override
    public String toString() {
      return "Event start";
    }
  },

  END_INDEX {
    @Override
    public String toString() {
      return "Event end";
    }
  },

  CENTER_TIME {
    @Override
    public String toString() {
      return "Center time";
    }
  };


  public static EventParameter[] intensity() {
    return new EventParameter[]{AREA, NET_AREA, HEIGHT, NET_HEIGHT, BACKGROUND_PER_NP};
  }

  public static EventParameter[] monteCarloHisto() {
    return new EventParameter[]{NET_AREA, NET_HEIGHT, DURATION, NO_OF_POINTS};
  }

  public static EventParameter[] histo() {
    return new EventParameter[]{AREA, NET_AREA, HEIGHT, NET_HEIGHT, DURATION, BACKGROUND_PER_NP,
        NO_OF_POINTS, ASYMMETRY_FACTOR};
  }

  public static EventParameter[] smooth() {
    return new EventParameter[]{NO_OF_EVENTS, AREA, NET_AREA, HEIGHT, NET_HEIGHT, DURATION, BACKGROUND_PER_NP,
        NO_OF_POINTS};
  }

  public static boolean canQuantify(EventParameter par) {
//    return par.equals(AREA)
//        || par.equals(NET_AREA)
//        || par.equals(HEIGHT)
//        || par.equals(NET_HEIGHT);

    // prevent quantification based on gross values to avoid confusion
    return par.equals(NET_AREA)
        || par.equals(NET_HEIGHT);
  }

  public static boolean isAreaOrHeight(EventParameter par) {
    return par.equals(AREA)
        || par.equals(NET_AREA)
        || par.equals(HEIGHT)
        || par.equals(NET_HEIGHT);
  }


}
