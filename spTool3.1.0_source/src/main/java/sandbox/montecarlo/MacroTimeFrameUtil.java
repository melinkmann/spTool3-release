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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ArrUtils;

public class MacroTimeFrameUtil {

  public static final Logger LOGGER = LogManager.getLogger(MacroTimeFrameUtil.class.getName());

  public static double[] groupSignal(double microDT, double macroDT, double[] microData) {
    double[] result;

    List<Double> macroFrame = new ArrayList<>();

    // somehow, there were cases that earlier checks failed...
    if (microDT > macroDT) {
      LOGGER.error(
          "Micro dwell time is larger than macro dwell time. "
              + "Cannot create macro time frame.");
      result = microData;
    } else {

      int blockSize = (int) (macroDT / microDT);
      // if off by 5 pct, add one
      if (blockSize * microDT < macroDT * 0.95) {
        blockSize += 1;
      }

      double sum = 0;
      for (int i = 0; i < microData.length; i++) {
        sum += microData[i];

        if (i > 0 && (i + 1) % blockSize == 0) {
          macroFrame.add(sum);
          sum = 0;
        }
      }
      result = ArrUtils.doubleListToArr(macroFrame);
    }
    return result;
  }

  public static double[] groupTime(double microDT, double macroDT, double[] microData) {
    double[] result;

    List<Double> macroFrame = new ArrayList<>();

    // somehow, there were cases that earlier checks failed...
    if (microDT > macroDT) {
      LOGGER.error(
          "Micro dwell time is larger than macro dwell time. "
              + "Cannot create macro time frame.");
      result = microData;
    } else {
      int blockSize = (int) (macroDT / microDT);
      // if off by 5 pct, add one
      if (blockSize * microDT < macroDT * 0.95) {
        blockSize += 1;
      }

      for (int i = 0; i < microData.length; i++) {
        if (i > 0 && (i + 1) % blockSize == 0) {
          macroFrame.add(microData[i]);
        }
      }
      result = ArrUtils.doubleListToArr(macroFrame);
    }

    return result;
  }


}
