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

import core.SpTool3Main;

import java.util.ArrayList;
import java.util.List;

public enum GatingOption {

  HEIGHT {
    @Override
    public String toString() {
      return "Peak height";
    }

    @Override
    public String shortString() {
      return "height";
    }
  },

  AREA {
    @Override
    public String toString() {
      return "Peak area";
    }

    @Override
    public String shortString() {
      return "area";
    }
  },

  NET_AREA {
    @Override
    public String toString() {
      return "Net peak area";
    }

    @Override
    public String shortString() {
      return "netArea";
    }
  },


  /*
   Width area was deleted!
   Why? Was never useful in SpTool2.
   Why? Main issue is high 1-2 DP long BG spikes. Width-Area criterion punished events
   that are long and wide. However, these are the REAL events that are hardest to detect.
   At the same time, these spiky BG events have ideal S/N, hence they were not rejected by the filter
   at all.
   Essentially, the criterion made things worse by punishing long&flat events and benefiting single DP
   BG spikes.
   */

  MORE_POINTS_THAN {
    @Override
    public String toString() {
      return "More points than";
    }

    @Override
    public String shortString() {
      return "minPoints";
    }
  },


  FEWER_POINTS_THAN {
    @Override
    public String toString() {
      return "Fewer points than";
    }

    @Override
    public String shortString() {
      return "maxPoints";
    }
  },

  PEAK_DOMINANCE {
    @Override
    public String toString() {
      return "Peak dominance";
    }

    @Override
    public String shortString() {
      return "spike";
    }
  },

  ACCUMULATED_P{
    @Override
    public String toString() {
      return "Accumulated probability";
    }

    @Override
    public String shortString() {
      return "accProb";
    }
  },

  MEAN_SIGNAL {
    @Override
    public String toString() {
      return "Mean signal of peak profile";
    }

    @Override
    public String shortString() {
      return "mean";
    }
  };


  public abstract String shortString();

  public static GatingOption[] getActiveValues() {
    List<GatingOption> active = new ArrayList<>();
    active.add(HEIGHT);
    active.add(AREA);
    active.add(NET_AREA);
    active.add(MORE_POINTS_THAN);
    active.add(FEWER_POINTS_THAN);
    active.add(MEAN_SIGNAL);

    if (SpTool3Main.SHOW_WINDOW) {
      active.add(PEAK_DOMINANCE);
    }

    if (SpTool3Main.SHOW_ACCUMULATION) {
      active.add(ACCUMULATED_P);
    }
    return active.toArray(new GatingOption[]{});
  }

}
