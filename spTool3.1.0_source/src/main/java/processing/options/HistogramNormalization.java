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

import java.io.Serializable;
import org.jfree.data.statistics.HistogramType;

public enum  HistogramNormalization implements Serializable {

  FREQUENCY {
    @Override
    public String toString() {
      return "Frequency";
    }

    @Override
    public HistogramType get() {
      return HistogramType.FREQUENCY;
    }
  },
  RELATIVE_FREQUENCY {
    @Override
    public String toString() {
      return "Relative frequency";
    }

    @Override
    public HistogramType get() {
      return HistogramType.RELATIVE_FREQUENCY;
    }
  },
  SCALE_AREA_TO_1 {
    @Override
    public String toString() {
      return "Probability density (area=1)";
    }

    @Override
    public HistogramType get() {
      return HistogramType.SCALE_AREA_TO_1;
    }

  };

  public abstract HistogramType get();


}
