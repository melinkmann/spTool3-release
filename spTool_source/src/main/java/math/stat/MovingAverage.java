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

package math.stat;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ArrUtils;

public class MovingAverage implements Serializable {

  // https://stackoverflow.com/questions/3793400/is-there-a-function-in-java-to-get-moving-average
  // Daniel Alexiuc, Here's a good implementation, using BigDecimal:

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(MovingAverage.class);

  private final Queue<Double> window;
  private final int period;
  private Double sum;

  public MovingAverage(int period) {
    // Same as "assert > 1.
    if (period < 1) {
      LOGGER.info("Period for moving average should be at least 1");
    }
    this.period = Math.max(1, period);
    this.window = new LinkedList<>();
    this.sum = 0.0;
  }

  public void add(double num) {
    sum += num;
    window.add(num);
    // after adding: full? then remove previous value.
    if (window.size() > period) {
      sum -= window.remove();
    }
  }

  // Keep this, as it has a very fast implementation via sum and size (other than median, ...)
  public Double getMean() {
    if (window.isEmpty()) {
      return 0.0; // technically the average is undefined
    }
    double size = window.size();
    return sum / size;
  }

  public double get(MeasureOfStat measure) {
    double res = 0;
    List<Double> values = new ArrayList<>(window);
    // must be strictly greater than one b/c (N-1)!
    if (values.size() > 1) {
      return measure.calc(values);
    }
    return res;
  }

  // For graphic representation without shifting all moav results against the actual data.
  // Ends get less precise but in the middle: better match.
  public double[] shift(double[] sma) {
    int shift = (int) Math.floor(period);
    double[] sSma = Arrays.stream(sma).toArray();
    if (sma.length > 2) {
      for (int i = 2; i < sma.length - shift; i++) {
        sSma[i] = sma[i + 2];
      }
    }
    return sSma;
  }

  public static List<Double> evaluate(double[] data, int moavWidth, int evaluatedPoints) {
    List<Double> moav = new ArrayList<>();

    // remove first and last entry.They may suffer from poor start/stop conditions
    if (data.length > 3) {
      data = ArrUtils.getPortion(data, 1, data.length - 2, LOGGER);

      // get quasi rolling average
      if (data.length > 1) {
        MovingAverage sma = new MovingAverage(moavWidth);
        int index = 0;
        while (index < data.length) {
          sma.add(data[index]);
          index++;
          if (index % evaluatedPoints == 0) {
            moav.add(sma.getMean());
          }
        }
      }
    }

    return moav;
  }

}
