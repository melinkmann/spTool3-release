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

import util.ArrUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mode {

  // Modified from chatGPT.
  public static double calc(double[] values) {
    double mode = 0;
    if (values != null && values.length != 0) {

      Map<Double, Integer> countMap = new HashMap<>();
      int maxCount = 0;

      for (double num : values) {
        int count = countMap.getOrDefault(num, 0) + 1;
        countMap.put(num, count);

        if (count > maxCount) {
          maxCount = count;
          mode = num;
        }
      }
    }
    return mode;
  }

  public static double calcNonzero(double[] values) {
    double mode = 0;

    if (values != null && values.length != 0) {

      Map<Double, Integer> countMap = new HashMap<>();
      int maxCount = 0;

      for (double num : values) {
        if (num != 0) {
          int count = countMap.getOrDefault(num, 0) + 1;
          countMap.put(num, count);

          if (count > maxCount) {
            maxCount = count;
            mode = num;
          }
        }
      }
    }
    return mode;
  }

  // Modified from chatGPT.
  public static double calc(List<Double> values) {
    double mode = 0;
    if (values != null && !values.isEmpty()) {

      Map<Double, Integer> countMap = new HashMap<>();
      int maxCount = 0;

      for (double num : values) {
        int count = countMap.getOrDefault(num, 0) + 1;
        countMap.put(num, count);

        if (count > maxCount) {
          maxCount = count;
          mode = num;
        }
      }
    }
    return mode;
  }

}
