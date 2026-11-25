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

public class LinLogTransformation {

  /**
   * Linear for x <= threshold (including all negative values),
   * logarithmic above (continuous, with matching slope).
   */
  public static double transform(double x, double threshold) {
    return (x <= threshold) ? x : threshold * (1.0 + Math.log10(x / threshold));
  }

  /**
   * Exact inverse of transform().
   */
  public static double inverse(double y, double threshold) {
    return (y <= threshold) ? y : threshold * Math.pow(10, (y / threshold - 1.0));
  }
}