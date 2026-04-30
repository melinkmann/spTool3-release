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

package analysis;

import java.util.*;

public class EventAligner {

  public static List<Event> mergeByCoverage(List<EventCollection> collections, int threshold) {
    List<Event> result = new ArrayList<>();

    // 1. Gather all indices and determine max index
    int maxIndex = -1;
    List<List<Integer>> allIndexLists = new ArrayList<>();

    for (EventCollection coll : collections) {
      for (Event e : coll.getNpEvents()) {
        List<Integer> indices =
            Optional.ofNullable(e.getIndicesList())
                .orElse(Collections.emptyList());

        allIndexLists.add(indices);

        for (int idx : indices) {
          if (idx > maxIndex) {
            maxIndex = idx;
          }
        }
      }
    }

    if (maxIndex < 0) {
      return result; // no indices at all
    }

    // 2. Build coverage array
    int[] coverage = new int[maxIndex + 1];

    for (List<Integer> indices : allIndexLists) {
      for (int idx : indices) {
        coverage[idx]++;
      }
    }

    // 3. Extract contiguous blocks above threshold
    EventCollection dummyCollection = new MainEventCollection();

    int i = 0;
    while (i <= maxIndex) {
      if (coverage[i] < threshold) {
        i++;
      } else {
        List<Integer> block = new ArrayList<>();
        while (i <= maxIndex && coverage[i] >= threshold) {
          block.add(i);
          i++;
        }
        result.add(new NpEvent(dummyCollection, block));
      }
    }

    return result;
  }

  // assisted with claude sonnet 4.6 & chatGPT
  public static List<Event> mergeByConnection(List<EventCollection> collections) {
    List<Event> result = new ArrayList<>();

    // 1. Gather all events
    List<Event> allEvents = new ArrayList<>();
    for (EventCollection coll : collections) {
      allEvents.addAll(coll.getNpEvents());
    }

    int n = allEvents.size();
    if (n == 0) {
      // nothing to merge
      result = new ArrayList<>();
    } else {

      // 2. Precompute index sets for each event for fast intersection
      List<Set<Integer>> eventIndexSets = new ArrayList<>();
      for (Event e : allEvents) {
        eventIndexSets.add(new HashSet<>(Optional.ofNullable(e.getIndicesList()).orElse(Collections.emptyList())));
      }

      // 3. Union-Find: parent[i] < 0 -> root and size = -parent[i], else points to parent
      int[] parent = new int[n];
      Arrays.fill(parent, -1); // each node is its own root with size 1

      // 4. Compare all pairs of events
      for (int i = 0; i < n; i++) {
        Set<Integer> indicesI = eventIndexSets.get(i);
        if (indicesI.isEmpty()) {
          continue;
        }

        for (int j = i + 1; j < n; j++) {
          Set<Integer> indicesJ = eventIndexSets.get(j);
          if (indicesJ.isEmpty()) {
            continue;
          }

          // Compute intersection size
          Set<Integer> intersection = new HashSet<>(indicesI);
          intersection.retainAll(indicesJ);
          int shared = intersection.size();

          boolean shouldMerge = false;
          if (indicesI.size() <= 3 || indicesJ.size() <= 3) {
            // small sets merge if any overlap
            if (shared >= 1) {
              shouldMerge = true;
            }
          } else {
            // large sets merge only if at least 2 shared indices
            if (shared >= 2) {
              shouldMerge = true;
            }
          }

          if (shouldMerge) {
            union(parent, i, j);
          }
        }
      }

      // 5. Group events by root
      Map<Integer, Set<Integer>> groups = new HashMap<>();
      for (int i = 0; i < n; i++) {
        int root = find(parent, i);
        groups.computeIfAbsent(root, k -> new TreeSet<>())
            .addAll(eventIndexSets.get(i));
      }

      // 6. Build merged events
      EventCollection dummyCollection = new MainEventCollection();
      List<Event> merged = new ArrayList<>();
      for (Set<Integer> indices : groups.values()) {
        merged.add(new NpEvent(dummyCollection, new ArrayList<>(indices)));
      }

      result = merged;
    }
    return result;
  }

  // Find with path compression
  private static int find(int[] parent, int x) {
    if (parent[x] < 0) {
      return x;
    } else {
      parent[x] = find(parent, parent[x]);
      return parent[x];
    }
  }

  // Union by size
  private static void union(int[] parent, int a, int b) {
    int ra = find(parent, a);
    int rb = find(parent, b);
    if (ra == rb) {
      return;
    }

    // smaller tree attaches to larger tree
    if (parent[ra] > parent[rb]) {
      int tmp = ra;
      ra = rb;
      rb = tmp;
    }
    parent[ra] += parent[rb]; // update size
    parent[rb] = ra;          // attach
  }

}
