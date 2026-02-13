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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.jfree.data.Range;

public enum NumberFormats {

  USA_3_DECIMAL {
    @Override
    public DecimalFormat get() {
      return new DecimalFormat("#0.###",
          new DecimalFormatSymbols(new Locale("us")));
    }
  },

  USA_5_DECIMAL {
    @Override
    public DecimalFormat get() {
      return new DecimalFormat("#0.#####",
          new DecimalFormatSymbols(new Locale("us")));
    }
  },

  USA_SCIENTIFIC {
    @Override
    public DecimalFormat get() {
      return new DecimalFormat("##0.0#E0##",
          new DecimalFormatSymbols(new Locale("us")));
    }
  };

  public abstract DecimalFormat get();

  public static DecimalFormat automatic(Range range) {
    if ((Math.abs(range.getLowerBound()) > 0 && Math.abs(range.getLowerBound()) < 0.001)
        || Math.abs(range.getUpperBound()) > 9999) {
      return USA_SCIENTIFIC.get();
    } else {
      return USA_3_DECIMAL.get();
    }
  }


}
