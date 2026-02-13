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

package analysis;


import java.io.Serial;
import java.io.Serializable;

public class Baseline implements Serializable {

  @Serial
  static final long serialVersionUID = 1L; //assign a long value

  private StatCollection backgroundDistribution;
  private final String summary;

  // Dummy
  public Baseline() {
    this.backgroundDistribution = new StatCollectionRAM();
    this.summary = "No summary";
  }

  public Baseline(StatCollection backgroundDistribution, String summary) {
    this.backgroundDistribution = backgroundDistribution;
    this.summary = summary;
  }

  public Baseline copy() {
    return new Baseline(backgroundDistribution.copy(), summary);
  }


  public ThresholdSupplier getThreshold(ThresholdSupplierInstructions instructions) {
    return instructions.get(this.backgroundDistribution);
  }

  public StatCollection getBackgroundDistribution() {
    return backgroundDistribution;
  }

  public boolean hasBaseline() {
    return backgroundDistribution != null && backgroundDistribution.size() > 0;
  }


  public String getSummary() {
    return summary;
  }

  public void toRAM() {
    if (this.backgroundDistribution instanceof StatCollectionHDD) {
      this.backgroundDistribution = new StatCollectionRAM(backgroundDistribution);
    }
  }

  public void toHDD() {
    // We only store on HDD if there are >1 slices.
    if (this.backgroundDistribution.size() > 1
        && this.backgroundDistribution instanceof StatCollectionRAM) {
      this.backgroundDistribution = new StatCollectionHDD(backgroundDistribution);
    }
  }


  public enum ThrFormalism {
    AT_LOCATION {
      @Override
      public String translate() {
        return "Location";
      }
    },
    CRITICAL_LIMIT_FORMALISM {
      @Override
      public String translate() {
        return "LC";
      }
    },
    DETECTION_LIMIT_FORMALISM {
      @Override
      public String translate() {
        return "LD";
      }
    },
    STATIC_VALUE {
      @Override
      public String translate() {
        return "Custom";
      }
    };

    public abstract String translate();
  }

  public enum ThrMeasureOfSignificance {
    ALPHA {
      @Override
      public String translate() {
        return "α";
      }
    },

    FACTOR {
      @Override
      public String translate() {
        return "z";
      }
    };

    public abstract String translate();
  }

}
