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

package sandbox.montecarlo;

import processing.options.DeadTimeModel;
import util.ArrUtils;

public class DeadTimeUtil {

  public static double[] applyDeadTime(double[] signal, double tau, DeadTimeModel type,
      double dwellTime) {

    if (type.equals(DeadTimeModel.PARALYZING)) {
      // Convert to cps
      ArrUtils.divideOverriding(signal, dwellTime);
      for (int i = 0; i < signal.length; i++) {
        signal[i] = signal[i] * Math.exp(-signal[i] * tau);
      }
      // Convert to cts
      ArrUtils.multiplyOverriding(signal, dwellTime);
    } else if (type.equals(DeadTimeModel.NON_PARALYZING)) {
      // Convert to cps
      ArrUtils.divideOverriding(signal, dwellTime);
      // micro_signal_sum = [x / (1 + x * tau_s) for x in micro_signal_sum]
      for (int i = 0; i < signal.length; i++) {
        double upper_fraction = 1 + signal[i] * tau;
        signal[i] = signal[i] / upper_fraction;
      }
      // Convert to cts
      ArrUtils.multiplyOverriding(signal, dwellTime);
    }
    return signal;

  }


}
