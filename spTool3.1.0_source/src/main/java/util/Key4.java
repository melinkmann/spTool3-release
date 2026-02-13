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

package util;


// chatGPT
public class Key4 implements MultiKey {
  final int distKey;
  final double mu;
  final double sd;
  final double y;

  public Key4(int distKey, double mu, double sd, double y) {
    this.distKey = distKey;
    this.mu = mu;
    this.sd = sd;
    this.y = y;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Key4)) return false;
    Key4 k = (Key4) o;
    return distKey == k.distKey
        && Double.doubleToLongBits(mu) == Double.doubleToLongBits(k.mu)
        && Double.doubleToLongBits(sd) == Double.doubleToLongBits(k.sd)
        && Double.doubleToLongBits(y) == Double.doubleToLongBits(k.y);
  }

  @Override
  public int hashCode() {
    int h = Integer.hashCode(distKey);
    h = 31 * h + Long.hashCode(Double.doubleToLongBits(mu));
    h = 31 * h + Long.hashCode(Double.doubleToLongBits(sd));
    h = 31 * h + Long.hashCode(Double.doubleToLongBits(y));
    return h;
  }
}