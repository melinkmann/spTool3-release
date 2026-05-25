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
import processing.options.ZScoreTarget;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.WardLinkage;
import util.ArrUtils;
import util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HAC {

  private static final Logger LOGGER = LogManager.getLogger(HAC.class);


  // My own pre-processing pipeline: one data[] per element (not isotope!)
  public static List<double[]> preprocess(List<double[]> data,
                                          boolean useLog2,
                                          ZScoreTarget zScoreTarget) {

    List<double[]> results = new ArrayList<>(data.size());
    for (double[] arr : data) {
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
     * 3. Global shift to ensure positivity for log transform.
     *    If smallest is larger than zero, no need to transform.
     *    (only needed if log transform is requested)
     */
    double shift = 0;
    if (useLog2) {
      if (globalMin > 0) {
        shift = 0;
      } else {
        shift = Math.abs(globalMin) + 1.0;
      }
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

    // This normalises each isotope
    if (zScoreTarget.equals(ZScoreTarget.ISOTOPE)) {

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
      // This normalises each particle,
    } else if (zScoreTarget.equals(ZScoreTarget.PARTICLES) && !results.isEmpty()) {

      int nParticles = results.get(0).length;
      int nIso = results.size();

      for (int p = 0; p < nParticles; p++) {

        // Collect this particle's isotope vector on-the-fly
        double[] vec = new double[nIso];
        for (int iso = 0; iso < nIso; iso++) {
          vec[iso] = results.get(iso)[p];
        }

        double mu = MeasureOfLocation.MEAN.calc(vec);
        double sd = MeasureOfSpread.SD.calc(vec);

        if (sd > 0) {
          for (int iso = 0; iso < nIso; iso++) {
            results.get(iso)[p] = (vec[iso] - mu) / sd;
          }
        }
        // sd == 0: leave particle as-is
      }
    }


    return results;
  }

  /**
   * Builds the Ward linkage and fits the full merge tree without cutting it.
   * This is the expensive step — call it once per unique preprocessing
   * configuration and cache the result in the sample via
   * {@code sample.putFittedHAC()}.
   *
   * @param data pre-processed spectral data .
   *             Size: nIsotopes × nParticles. All arrays must have equal length.
   * @return a {@link FittedHAC} ready for repeated cheap {@link FittedHAC#cut} calls.
   */
  public static FittedHAC fit(List<double[]> data) {

    int nIsotopes = data.size();
    int nParticles = data.get(0).length;

    /*
     * Transpose: mz-major -> particle-major.
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
  public record FittedHAC(HierarchicalClustering hc, int nParticles
  ) {

    /**
     * Cut the dendrogram at {@code threshold} and return flat cluster labels.
     * This is a pure in-memory operation on the already-computed merge tree.
     * No distance matrix is recomputed.
     *
     * @param threshold Ward distance at which to cut.
     * @return cluster assignment for each particle.
     */
    public ClusterResult cut(double threshold) {

      /*
       * Ask SMILE to cut the dendrogram at the given Ward distance.
       * Returns one cluster index per particle, 0-based and contiguous:
       * e.g. for 5 particles in 3 clusters: [0, 2, 1, 0, 2].
       *
       * labels is an array of length nParticles. Each slot is a particle, each value is which cluster that
       * particle belongs to:
        index:   0    1    2    3    4      ← particle index
                 ↓    ↓    ↓    ↓    ↓
        labels: [0,   2,   1,   0,   2]     ← cluster index
       */
      int[] labels = hc.partition(threshold);

      /*
       * Find how many clusters we got by taking the highest label + 1.
       * SMILE guarantees contiguous 0-based labels so this is safe.
       */
      int maxLabel = 0;
      for (int i = 0; i < labels.length; i++) {
        if (labels[i] > maxLabel) {
          maxLabel = labels[i];
        }
      }

      // k: number of cluster
      int k = maxLabel + 1;

      /*
       * Count how many particles landed in each cluster.
       * sizes[c] = number of particles with label c.
       */
      int[] sizes = new int[k];
      for (int i = 0; i < labels.length; i++) {
        int clusterIndex = labels[i];
        sizes[clusterIndex] = sizes[clusterIndex] + 1;
      }

      /*
       * Assign default names C1, C2, ... to each cluster.
       */
      String[] names = new String[k];
      for (int c = 0; c < k; c++) {
        names[c] = "C" + (c + 1);
      }

      /*
      What are the other fields we return here?

      __________________________________________________________________________
      hc.getHeight() — array of Ward distances at each merge step, in ascending order. L
      ength = nParticles - 1 (one merge per step).

      mergeHeights: [0.3,  1.1,  1.4,  3.1,  5.2]
               ↑                         ↑
            first merge             last merge
      __________________________________________________________________________
      hc.getTree() — the merge tree itself: A 2D array of shape [nParticles-1][2].
      Each row is one merge step, telling you which two things were merged:

      mergeTree[0] = [3, 4]    // step 0: particle 3 and particle 4 merged
      mergeTree[1] = [1, 5]    // step 1: particle 1 and internal node 5 merged

      Detail:

      n the example I used 5 particles, so particle indices are 0, 1, 2, 3, 4.
      After merge step 0 (particles 3 and 4 merge), that new combined cluster needs an index too.
      Smile assigns it nParticles + stepIndex = 5 + 0 = 5.
      So 5 just means "the thing created at merge step 0",
      which happens to be the cluster containing particles 3 and 4.

      step 0: merge particle 3 + particle 4  → internal node 5  (= 5+0)
      step 1: merge particle 1 + node 5      → internal node 6  (= 5+1)
      step 2: ...                            → internal node 7  (= 5+2)

      So internal node 6 contains particles 1, 3, 4. It just keeps chaining.
      The final internal node at step nParticles-2 is the root: The single cluster containing everyone.

       */

      return new ClusterResult(labels, k, sizes, hc.getTree(), hc.getHeight(), threshold, names);
    }
  }


  // ---------------------------------------------------------------------------
  // Result container
  // ---------------------------------------------------------------------------

  /**
   * Result of a hierarchical clustering run.
   * <p>
   * ClusterResult
   * ├── labels[]        int[nParticles]   — cluster index per particle (0-based)
   * │                                       e.g. [0, 0, 1, 2, 1, 0]
   * ├── k               int               — number of clusters after cut
   * │                                       = maxLabel + 1
   * ├── sizes[]         int[k]            — how many particles in each cluster
   * │                                       sizes[c] = count of labels[p]==c
   * ├── mergeTree[][]   int[nMerges][2]   — the full HAC tree, untouched
   * │                                       mergeTree[m][0/1] = left/right child of merge m
   * │                                       leaves: 0..nParticles-1
   * │                                       internal nodes: nParticles+m
   * ├── mergeHeights[]  double[nMerges]   — Ward distance at each merge (ascending)
   * ├── threshold       double            — the cut value used
   * └── clusterNames[]  String[k]         — "C1", "C2", ... by default
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
     * <p>
     * Example:
     * result.get(0) = [0, 3]   // cluster 0 contains particles 0 and 3
     * result.get(1) = [2]      // cluster 1 contains particle 2
     * result.get(2) = [1, 4]   // cluster 2 contains particles 1 and 4
     */
    public List<List<Integer>> indicesByCluster() {

      // Create one empty list per cluster.
      List<List<Integer>> result = new ArrayList<>(k);
      for (int c = 0; c < k; c++) {
        result.add(new ArrayList<>());
      }

      /*
       * Walk every particle and add its index to the correct cluster list.
       * labels[particleIndex] tells us which cluster that particle belongs to.
       */
      for (int particleIndex = 0; particleIndex < labels.length; particleIndex++) {
        int clusterIndex = labels[particleIndex];
        result.get(clusterIndex).add(particleIndex);
      }

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
        thr = 1; // some ok-ish default
        if (Double.isFinite(mid) && mid > 0.0) {
          thr = mid;
        } else {
          LOGGER.error("Threshold estimation failed.");
        }
      }
      return thr;
    }

    /**
     * Suggested default threshold: just below the largest gap between
     * consecutive merge heights ("maximum-acceleration cut").
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
     * Suggested threshold: maximum curvature approach (kneedle algorithm 'backwards').
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
     * <p>Data orientation of {@code raw} must match raw.get(iso)[particle]}.
     *
     * @param raw           raw (unprocessed) spectral data —
     *                      same {@code List<double[]>} orientation as the
     *                      input.
     *                      Size: nIsotopes × nParticles.
     * @param minSimilarity cosine similarity threshold in [0, 1].
     *                      Pairs with similarity ≥ this value are merged.
     *                      Typical useful range: 0.95 – 0.999.
     * @return a new {@link ClusterResult} with merged clusters, or
     * {@code this} unchanged if no pair exceeds the threshold.
     */
    public ClusterResult mergeByCosineSimilarity(List<double[]> raw, double minSimilarity) {

      boolean preventDrift = true;

      int nIsotopes = raw.size();
      if (nIsotopes == 0 || k < 2) {
        return this;
      }

      int nParticles = raw.get(0).length;

      // ── 1. Build centroids ────────────────────────────────────────────────
      /*
       * For each cluster, compute the mean raw spectrum across all member particles.
       * centroid[c][iso] = mean intensity of isotope iso in cluster c.
       * We use the RAW (unprocessed) data here, not the log/z-scored data that
       * Ward used — we want to compare real-world compositions, not statistical artifacts.
       */
      double[][] centroid = new double[k][nIsotopes];

      /*
       * workCounts[c] = number of particles currently in cluster c.
       * Called "work" because this array will be mutated during the merge loop —
       * it is a working copy, not the final sizes[].
       * When cluster B is absorbed into A, workCounts[B] is set to 0.
       */
      int[] workCounts = new int[k];

      for (int p = 0; p < nParticles; p++) {
        int clusterIndex = labels[p];
        workCounts[clusterIndex] = workCounts[clusterIndex] + 1;
      }

      // Sum up raw intensities per cluster (will divide by workCounts below)
      for (int iso = 0; iso < nIsotopes; iso++) {
        double[] channel = raw.get(iso);
        for (int p = 0; p < nParticles; p++) {
          int clusterIndex = labels[p];
          centroid[clusterIndex][iso] = centroid[clusterIndex][iso] + channel[p];
        }
      }

      // Divide by particle count to get the mean → centroid
      for (int c = 0; c < k; c++) {
        if (workCounts[c] == 0) {
          continue;
        }
        for (int iso = 0; iso < nIsotopes; iso++) {
          centroid[c][iso] = centroid[c][iso] / workCounts[c];
        }
      }

      // ── 2. Redirect table ─────────────────────────────────────────────────
      /*
       * redirect[c] tracks where cluster c has been redirected to after being absorbed.
       * Initially every cluster points to itself — identity mapping.
       *
       * When cluster B is absorbed into cluster A:
       *   redirect[B] = A
       *
       * Chains can form: if later A is absorbed into C:
       *   redirect[A] = C
       *   redirect[B] = A → C  (resolved by following the chain)
       *
       * A cluster is a "survivor" (still alive) if redirect[c] == c.
       * A cluster is "absorbed" if redirect[c] != c.
       */
      int[] redirect = new int[k];
      for (int c = 0; c < k; c++) {
        redirect[c] = c;
      }

      /*
       * workNames[c] = current display name of cluster c.
       * Called "work" because names are mutated during merging,
       * e.g. "C1" absorbs "C3" → workNames[C1] becomes "C1+3".
       * This is a working copy of clusterNames[].
       */
      String[] workNames = Arrays.copyOf(clusterNames, k);

      // ── 3. Iterative greedy merge ─────────────────────────────────────────
      /*
       * Repeatedly find the most similar pair of surviving clusters.
       * If their cosine similarity exceeds minSimilarity, merge them.
       * Stop when no qualifying pair remains.
       */
      boolean mergeHappened = true;
      while (mergeHappened) {
        mergeHappened = false;

        // Track the best (most similar) pair found in this iteration
        double bestSim = -1.0;
        int bestA = -1;
        int bestB = -1;

        for (int i = 0; i < k; i++) {
          boolean iIsAbsorbed = redirect[i] != i || workCounts[i] == 0;
          if (iIsAbsorbed) {
            continue;
          }

          for (int j = i + 1; j < k; j++) {
            boolean jIsAbsorbed = redirect[j] != j || workCounts[j] == 0;
            if (jIsAbsorbed) {
              continue;
            }

            double sim = cosineSimilarity(centroid[i], centroid[j]);
            if (sim > bestSim) {
              bestSim = sim;
              bestA = i;
              bestB = j;
            }
          }
        }

        // If the best pair is still below the threshold, we are done
        if (bestSim < minSimilarity) {
          break;
        }

        // ── Compute the merged centroid into a temporary array first ──────
        // We do NOT commit it yet — we check below whether it drifts too far.
        int nA = workCounts[bestA];
        int nB = workCounts[bestB];
        int nAB = nA + nB;

        double[] newCentroid = new double[nIsotopes];
        for (int iso = 0; iso < nIsotopes; iso++) {
          double weightedA = centroid[bestA][iso] * nA;
          double weightedB = centroid[bestB][iso] * nB;
          newCentroid[iso] = (weightedA + weightedB) / nAB;
        }

        /*
         * Sanity check: is the merged centroid still similar enough to both parents?
         * If not, the merge would create a "bridge" cluster that drifts away from
         * both original compositions — reject it and stop merging entirely.
         *
         * Example: A and C are dissimilar, but B bridges them.
         * sim(A,B)=0.97, sim(B,C)=0.96 → A+B merged → new centroid shifts toward C
         * → sim(AB,C) now exceeds threshold → C wrongly absorbed.
         * This check prevents that.
         */
        if (preventDrift) {
          double simToA = cosineSimilarity(newCentroid, centroid[bestA]);
          double simToB = cosineSimilarity(newCentroid, centroid[bestB]);
          boolean mergedCentroidDrifted = simToA < minSimilarity || simToB < minSimilarity;
          if (mergedCentroidDrifted) {
            break;
          }
        }

        // Centroid looks good — commit the merge
        for (int iso = 0; iso < nIsotopes; iso++) {
          centroid[bestA][iso] = newCentroid[iso];
        }
        workCounts[bestA] = nAB;
        workCounts[bestB] = 0;
        redirect[bestB] = bestA;

        // Update the display name: "C1" absorbs "C3" → "C1+3"
        // Strip the leading "C" from bestB's name to keep it compact
        String suffixB = workNames[bestB].startsWith("C")
            ? workNames[bestB].substring(1)
            : workNames[bestB];
        workNames[bestA] = workNames[bestA] + "+" + suffixB;

        mergeHappened = true;
      }

      // ── 4. Resolve redirect chains ────────────────────────────────────────
      /*
       * For each particle, follow the redirect chain until we reach a root
       * (a cluster that points to itself — i.e. a survivor).
       *
       * Example chain: particle → B → redirect[B]=A → redirect[A]=A  ✓
       */
      int[] newLabels = new int[nParticles];
      for (int p = 0; p < nParticles; p++) {
        int c = labels[p];
        while (redirect[c] != c) {
          c = redirect[c];
        }
        newLabels[p] = c;
      }

      // ── 5. Compact survivors into a new ClusterResult ─────────────────────
      /*
       * After merging, surviving clusters have non-contiguous indices,
       * e.g. clusters 0, 2, 4 survive while 1 and 3 were absorbed.
       * We remap them to clean 0-based contiguous indices: 0, 1, 2.
       */
      int newK = 0;
      for (int c = 0; c < k; c++) {
        boolean isSurvivor = redirect[c] == c && workCounts[c] > 0;
        if (isSurvivor) {
          newK = newK + 1;
        }
      }

      int[] newSizes = new int[newK];
      String[] newNames = new String[newK];

      /*
       * compactId[c] maps old cluster index c to its new compact index.
       * Initialised to -1 so absorbed clusters are easy to detect if
       * something goes wrong (an index of -1 will throw immediately).
       */
      int[] compactId = new int[k];
      Arrays.fill(compactId, -1);

      int compactIndex = 0;
      for (int c = 0; c < k; c++) {
        boolean isSurvivor = redirect[c] == c && workCounts[c] > 0;
        if (isSurvivor) {
          compactId[c] = compactIndex;
          newNames[compactIndex] = workNames[c];
          compactIndex = compactIndex + 1;
        }
      }

      // Remap every particle's label to the new compact index
      for (int p = 0; p < nParticles; p++) {
        newLabels[p] = compactId[newLabels[p]];
      }

      // Count particles per new cluster to fill newSizes[]
      for (int p = 0; p < nParticles; p++) {
        int clusterIndex = newLabels[p];
        newSizes[clusterIndex] = newSizes[clusterIndex] + 1;
      }

      return new ClusterResult(newLabels, newK, newSizes,
          mergeTree, mergeHeights, threshold, newNames);
    }

// ── Cosine similarity helper ──────────────────────────────────────────────

    /**
     * Cosine similarity between two intensity vectors.
     * Measures the angle between them — 1.0 means identical direction (composition),
     * 0.0 means completely orthogonal (no overlap in composition).
     * Returns 0.0 if either vector is all-zero (no signal → undefined similarity).
     */
    private static double cosineSimilarity(double[] a, double[] b) {
      double dot = 0.0;   // sum of a[i] * b[i]
      double normA = 0.0; // sum of a[i]^2, will be square-rooted below
      double normB = 0.0; // sum of b[i]^2, will be square-rooted below

      for (int i = 0; i < a.length; i++) {
        dot = dot + a[i] * b[i];
        normA = normA + a[i] * a[i];
        normB = normB + b[i] * b[i];
      }

      if (normA == 0.0 || normB == 0.0) {
        return 0.0;
      }

      return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

  }
}