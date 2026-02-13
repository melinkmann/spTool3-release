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

package visualizer.charts;

public class AxisLimits {

  private final double xLow;
  private final double xHigh;
  private final double yLow;
  private final double yHigh;

  public AxisLimits(double xLow, double xHigh, double yLow, double yHigh) {
    this.xLow = xLow;
    this.xHigh = xHigh;
    this.yLow = yLow;
    this.yHigh = yHigh;
  }

  public double getxLow() {
    return xLow;
  }

  public double getxHigh() {
    return xHigh;
  }

  public double getyLow() {
    return yLow;
  }

  public double getyHigh() {
    return yHigh;
  }
}
