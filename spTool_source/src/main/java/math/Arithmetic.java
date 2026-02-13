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

package math;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import org.apache.commons.math3.random.MersenneTwister;

public class Arithmetic {

  public static boolean isNotZero(double d) {
    return (d < 0 || d > 0);
  }

  /**
   * @param inclusiveMinimum inclusive lower bound
   * @param exclusiveMaximum exclusive upper bound
   * @return random integer between min and max
   */
  public static int getRandomInteger(int inclusiveMinimum, int exclusiveMaximum) {
    return (int) ((Math.random() * (exclusiveMaximum - inclusiveMinimum)) + inclusiveMinimum);
  }

  public static int getFastRandomInteger(MersenneTwister mersenneTwister,
      int inclusiveMinimum, int exclusiveMaximum) {
    return (int) ((mersenneTwister.nextDouble() * (exclusiveMaximum - inclusiveMinimum))
        + inclusiveMinimum);
  }

  public static int getFastRandomInteger(XoRoShiRo128PlusRandom random,
      int inclusiveMinimum, int exclusiveMaximum) {
    return (int) ((random.nextDouble() * (exclusiveMaximum - inclusiveMinimum))
        + inclusiveMinimum);
  }

  public static double log2(double d) {
    return (Math.log(d) / Math.log(2));
  }

}
