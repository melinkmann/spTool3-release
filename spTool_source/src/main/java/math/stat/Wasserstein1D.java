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

import java.util.Arrays;

// 100% Claude Sonnet 5.5
public final class Wasserstein1D {

  private Wasserstein1D() {}

  /**
   * Wasserstein-1 distance (Earth Mover's Distance) between two 1D empirical
   * distributions given by samples a and b. Sizes may differ; every point has
   * equal weight within its sample.
   *
   * Computed as the integral of |F_a(t) - F_b(t)| dt over the merged support.
   * Runs in O(n log n + m log m). Inputs are not modified.
   */
  public static double distance(double[] a, double[] b) {
    if (a.length == 0 || b.length == 0)
      throw new IllegalArgumentException("Datasets must not be empty.");

    double[] sa = Arrays.copyOf(a, a.length);
    double[] sb = Arrays.copyOf(b, b.length);
    Arrays.sort(sa);
    Arrays.sort(sb);

    int n = sa.length, m = sb.length;
    int i = 0, j = 0;
    double cdfA = 0, cdfB = 0;
    double prev = Math.min(sa[0], sb[0]);
    double total = 0;

    while (i < n || j < m) {
      double next;
      if (j >= m || (i < n && sa[i] <= sb[j])) next = sa[i];
      else next = sb[j];

      // Area of the rectangle between the previous and the current point.
      total += Math.abs(cdfA - cdfB) * (next - prev);

      // Consume all points equal to 'next' in both samples.
      while (i < n && sa[i] == next) { i++; cdfA += 1.0 / n; }
      while (j < m && sb[j] == next) { j++; cdfB += 1.0 / m; }

      prev = next;
    }
    return total;
  }
}