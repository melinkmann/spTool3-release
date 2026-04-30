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

import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.WardLinkage;
import util.ArrUtils;
import util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HAC {

  private static final Logger LOGGER = LogManager.getLogger(HAC.class);


  // My own pre-processing pipeline:
  public static List<double[]> preprocess(List<double[]> data,
                                          double preprocessThreshold,
                                          boolean useLog2,
                                          boolean useZScore) {

    List<double[]> results = new ArrayList<>();

    /*
     * 1. Filter spectra:
     * Ignore spectra where no m/z exceeds the threshold.
     */
    for (double[] arr : data) {

      int aboveThrCounter = 0;

      for (double v : arr) {
        if (v > preprocessThreshold)
          aboveThrCounter++;
      }

      if (aboveThrCounter >= 1)
        results.add(ArrUtils.copy(arr));
    }

    /*
     * 2. Compute global minimum across all spectra
     */
    double globalMin = Double.POSITIVE_INFINITY;

    for (double[] arr : results) {
      for (double v : arr) {
        globalMin = Math.min(v, globalMin);
      }
    }

    /*
     * 3. Global shift to ensure positivity for log transform
     *    (only needed if log transform is requested)
     */
    double shift = 0.0;
    if (useLog2) {
      shift = -globalMin + 1.0;
    }

    /*
     * 4. Apply shift and log transform
     */
    if (useLog2) {

      for (int i = 0; i < results.size(); i++) {

        double[] arr = results.get(i);
        double[] transformed = new double[arr.length];

        for (int j = 0; j < arr.length; j++) {
          double v = arr[j] + shift;
          transformed[j] = Util.log2(v);
        }

        results.set(i, transformed);
      }
    }

    /*
     * 5. Z-score normalization (per spectrum)
     */
    if (useZScore) {

      for (int i = 0; i < results.size(); i++) {

        double[] arr = results.get(i);

        double mu = MeasureOfLocation.MEAN.calc(arr);
        double sd = MeasureOfSpread.SD.calc(arr);

        double[] scores = new double[arr.length];

        if (sd > 0) {

          for (int j = 0; j < arr.length; j++) {
            scores[j] = (arr[j] - mu) / sd;
          }
        }

        results.set(i, scores);
      }
    }

    return results;
  }

  //////////// CLAUDE SONNET 4.6 ///////////////

  /**
   * Holds a fitted (but not yet cut) Ward hierarchical clustering.
   * The expensive Ward matrix computation has already been done;
   * calling {@link #cut(double)} is cheap and can be repeated
   * with different thresholds at negligible cost.
   *
   * @param hc         the fitted SMILE object (keeps the merge tree alive)
   * @param nParticles number of observations that were clustered
   */
  public record FittedHAC(
      HierarchicalClustering hc,
      int nParticles
  ) {

    /**
     * Cut the dendrogram at {@code threshold} and return flat cluster labels.
     * This is a pure in-memory operation on the already-computed merge tree —
     * no distance matrix is recomputed.
     *
     * @param threshold Ward distance at which to cut.
     * @return cluster assignment for each particle.
     */
    public ClusterResult cut(double threshold) {
      int[] labels = hc.partition(threshold);

      int maxLabel = 0;
      for (int label : labels)
        if (label > maxLabel) maxLabel = label;

      int k = maxLabel + 1;
      int[] sizes = new int[k];
      for (int label : labels) sizes[label]++;

      String[] names = new String[k];
      for (int c = 0; c < k; c++) {
        names[c] = "C" + (c + 1);
      }

      return new ClusterResult(labels, k, sizes, hc.getTree(), hc.getHeight(), threshold, names);
    }
  }

  /**
   * Builds the Ward linkage and fits the full merge tree without cutting it.
   * This is the expensive step — call it once per unique preprocessing
   * configuration and cache the result in the sample via
   * {@code sample.putFittedHAC()}.
   *
   * @param data pre-processed spectral data — output of {@link #preprocess}.
   *             Size: nIsotopes × nParticles. All arrays must have equal length.
   * @return a {@link FittedHAC} ready for repeated cheap {@link FittedHAC#cut} calls.
   */
  public static FittedHAC fit(List<double[]> data) {

    int nIsotopes = data.size();
    int nParticles = data.get(0).length;

    /*
     * Transpose: mz-major → particle-major.
     *
     *    Input:  data.get(iso)[particle]
     *    Output: matrix[particle][iso]
     *
     *    WardLinkage.of() expects rows = observations, cols = features.
     */
    double[][] matrix = new double[nParticles][nIsotopes];

    for (int iso = 0; iso < nIsotopes; iso++) {
      double[] channel = data.get(iso);
      for (int p = 0; p < nParticles; p++) {
        matrix[p][iso] = channel[p];
      }
    }

    WardLinkage ward = WardLinkage.of(matrix);
    HierarchicalClustering hc = HierarchicalClustering.fit(ward);

    return new FittedHAC(hc, nParticles);
  }

  /**
   * Convenience one-shot method: fit + cut in a single call.
   * Prefer {@link #fit} + {@link FittedHAC#cut} when the same data
   * may be cut at more than one threshold.
   *
   * @param data              pre-processed spectral data.
   * @param distanceThreshold Ward distance at which to cut the dendrogram.
   * @return cluster result at the requested threshold.
   */
  public static ClusterResult cluster(List<double[]> data, double distanceThreshold) {
    return fit(data).cut(distanceThreshold);
  }


  // ---------------------------------------------------------------------------
  // Result container
  // ---------------------------------------------------------------------------

  /**
   * Result of a hierarchical clustering run.
   *
   * @param labels       Flat cluster assignment, one per particle (0-based).
   * @param k            Number of clusters after the tree cut.
   * @param sizes        {@code sizes[c]} = number of particles in cluster c.
   * @param mergeTree    {@code mergeTree[m][0/1]} = left/right child of merge
   *                     step m. Leaf indices are 0..nParticles-1; internal
   *                     node m is at index nParticles+m.
   * @param mergeHeights Ward distance at each merge step (ascending).
   * @param threshold    Distance threshold used to cut the tree.
   */
  public record ClusterResult(
      int[] labels,
      int k,
      int[] sizes,
      int[][] mergeTree,
      double[] mergeHeights,
      double threshold,
      String[] clusterNames
  ) {

    /**
     * Returns the indices (into the original input data) for each cluster.
     * {@code indicesByCluster.get(c)} is the list of all particle indices
     * assigned to cluster c.
     *
     * @return one list per cluster (0-based), in cluster order.
     */
    public List<List<Integer>> indicesByCluster() {
      List<List<Integer>> result = new ArrayList<>(k);
      for (int c = 0; c < k; c++)
        result.add(new ArrayList<>());

      for (int i = 0; i < labels.length; i++)
        result.get(labels[i]).add(i);

      return result;
    }

    /**
     * Suggested default threshold: midpoint of the merge-height range.
     */
    public double getHalfThreshold() {
      double thr;
      if (mergeHeights.length < 2) {
        thr = mergeHeights[0] * 0.9;
      } else {
        double min = mergeHeights[0];
        double max = mergeHeights[mergeHeights.length - 1];
        double mid = (min + max) / 2.0;
        thr = Double.isFinite(mid) && mid > 0.0 ? mid : 1e-3;
      }
      return thr;
    }

    /**
     * Suggested default threshold: just below the largest gap between
     * consecutive merge heights ("elbow" / maximum-acceleration cut).
     */
    public double getSuggestedStepThreshold() {
      double thr;
      if (mergeHeights.length < 2) {
        thr = mergeHeights[0] * 0.9;
      } else {
        int bestIdx = 0;
        double bestGap = Double.NEGATIVE_INFINITY;

        for (int i = 1; i < mergeHeights.length; i++) {
          double gap = mergeHeights[i] - mergeHeights[i - 1];
          if (gap > bestGap) {
            bestGap = gap;
            bestIdx = i;
          }
        }

        thr = (mergeHeights[bestIdx] + mergeHeights[bestIdx - 1]) / 2.0;
      }
      return thr;
    }

    /**
     * Suggested threshold: maximum curvature approach (kneedle algorithm).
     */
    public double getSuggestedCurvatureThreshold() {
      if (mergeHeights.length < 2) {
        return mergeHeights[0] * 0.9;
      }

      int n = mergeHeights.length;
      int topK = Math.min(n, 100);
      int offset = n - topK;

      double maxH = mergeHeights[n - 1];
      if (maxH == 0) return mergeHeights[0];

      double y0 = 1.0;
      double y1 = mergeHeights[offset] / maxH;
      double dx = 1.0;
      double dy = y1 - y0;
      double len = Math.sqrt(dx * dx + dy * dy);

      int elbowIdx = offset;
      double maxDist = Double.NEGATIVE_INFINITY;

      for (int i = 1; i < topK - 1; i++) {
        double x = (double) i / (topK - 1);
        double y = mergeHeights[n - 1 - i] / maxH;

        double dist = ((x - 0) * dy - (y - y0) * dx) / len;
        if (dist > maxDist) {
          maxDist = dist;
          elbowIdx = n - 1 - i;
        }
      }

      return mergeHeights[elbowIdx];
    }

    /**
     * Post-processing pass: merges clusters whose mean raw-intensity spectra are
     * too similar to be considered distinct compositions.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Build one centroid per cluster — mean of raw intensities across all
     *       member particles.</li>
     *   <li>Compute the full pairwise cosine-similarity matrix.</li>
     *   <li>Iteratively merge the most-similar pair that exceeds
     *       {@code minSimilarity}, updating the merged centroid as a
     *       size-weighted average. Repeat until no pair qualifies.</li>
     *   <li>Return a new {@link ClusterResult} with compactly renumbered
     *       labels (0-based, no gaps). The original {@code mergeTree},
     *       {@code mergeHeights}, and {@code threshold} are carried over
     *       unchanged — they are HAC properties, not affected by this
     *       post-processing step.</li>
     * </ol>
     *
     * <p>Data orientation of {@code raw} must match
     * {@link HAC#preprocess} input: {@code raw.get(iso)[particle]}.
     *
     * @param raw           raw (unprocessed) spectral data —
     *                      same {@code List<double[]>} orientation as the
     *                      input to {@link HAC#preprocess}.
     *                      Size: nIsotopes × nParticles.
     * @param minSimilarity cosine similarity threshold in [0, 1].
     *                      Pairs with similarity ≥ this value are merged.
     *                      Typical useful range: 0.95 – 0.999.
     * @return a new {@link ClusterResult} with merged clusters, or
     * {@code this} unchanged if no pair exceeds the threshold.
     */
    public ClusterResult mergeByCosineSimilarity(List<double[]> raw, double minSimilarity) {

      int nIsotopes = raw.size();
      if (nIsotopes == 0 || k < 2) return this;

      int nParticles = raw.get(0).length;

      // ── 1. Build centroids ────────────────────────────────────────────────
      double[][] centroid = new double[k][nIsotopes];
      int[] workCounts = new int[k];

      for (int p = 0; p < nParticles; p++)
        workCounts[labels[p]]++;

      for (int iso = 0; iso < nIsotopes; iso++) {
        double[] channel = raw.get(iso);
        for (int p = 0; p < nParticles; p++)
          centroid[labels[p]][iso] += channel[p];
      }
      for (int c = 0; c < k; c++) {
        if (workCounts[c] == 0) continue;
        for (int iso = 0; iso < nIsotopes; iso++)
          centroid[c][iso] /= workCounts[c];
      }

      // ── 2. Mutable working state ──────────────────────────────────────────
      // redirect[c]: which live cluster index c's particles belong to.
      // Initially identity; when bestB is absorbed into bestA, redirect[bestB] = bestA.
      // Chains (A absorbs B, then C absorbs A) are resolved by following the chain.
      int[] redirect = new int[k];
      for (int c = 0; c < k; c++) redirect[c] = c;

      String[] workNames = Arrays.copyOf(clusterNames, k);

      // ── 3. Iterative greedy merge ─────────────────────────────────────────
      boolean merged = true;
      while (merged) {
        merged = false;

        double bestSim = -1.0;
        int bestA = -1, bestB = -1;

        for (int i = 0; i < k; i++) {
          if (redirect[i] != i || workCounts[i] == 0) continue; // skip absorbed
          for (int j = i + 1; j < k; j++) {
            if (redirect[j] != j || workCounts[j] == 0) continue;
            double sim = cosineSimilarity(centroid[i], centroid[j]);
            if (sim > bestSim) {
              bestSim = sim;
              bestA = i;
              bestB = j;
            }
          }
        }

        if (bestSim < minSimilarity) break;

        // Size-weighted centroid update
        int nA = workCounts[bestA];
        int nB = workCounts[bestB];
        int nAB = nA + nB;
        for (int iso = 0; iso < nIsotopes; iso++)
          centroid[bestA][iso] = (centroid[bestA][iso] * nA
              + centroid[bestB][iso] * nB) / nAB;
        workCounts[bestA] = nAB;
        workCounts[bestB] = 0;
        redirect[bestB] = bestA; // bestB now points to bestA

        // Name: strip leading "C" from bestB's suffix and append
        String suffixB = workNames[bestB].startsWith("C")
            ? workNames[bestB].substring(1)
            : workNames[bestB];
        workNames[bestA] = workNames[bestA] + "+" + suffixB;

        merged = true;
      }

      // ── 4. Build new labels[] resolving redirect chains ───────────────────
      int[] newLabels = new int[nParticles];
      for (int p = 0; p < nParticles; p++) {
        int c = labels[p];
        while (redirect[c] != c) c = redirect[c]; // follow chain to root
        newLabels[p] = c;
      }

      // ── 5. Compact survivors into new ClusterResult ───────────────────────
      // Survivors: indices where redirect[c] == c and workCounts[c] > 0
      int newK = 0;
      for (int c = 0; c < k; c++)
        if (redirect[c] == c && workCounts[c] > 0) newK++;

      int[] newSizes = new int[newK];
      String[] newNames = new String[newK];

      // Map old survivor index → new compact index
      int[] compactId = new int[k];
      Arrays.fill(compactId, -1);
      int idx = 0;
      for (int c = 0; c < k; c++) {
        if (redirect[c] == c && workCounts[c] > 0) {
          compactId[c] = idx;
          newNames[idx] = workNames[c];
          idx++;
        }
      }

      // Remap newLabels to compact indices
      for (int p = 0; p < nParticles; p++)
        newLabels[p] = compactId[newLabels[p]];

      for (int p = 0; p < nParticles; p++)
        newSizes[newLabels[p]]++;

      return new ClusterResult(newLabels, newK, newSizes,
          mergeTree, mergeHeights, threshold, newNames);
    }

// ── Cosine similarity helper ──────────────────────────────────────────────

    /**
     * Cosine similarity between two non-negative intensity vectors.
     * Returns 0.0 if either vector is all-zero.
     */
    private static double cosineSimilarity(double[] a, double[] b) {
      double dot = 0.0, normA = 0.0, normB = 0.0;
      for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
      }
      if (normA == 0.0 || normB == 0.0) return 0.0;
      return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
  }

}