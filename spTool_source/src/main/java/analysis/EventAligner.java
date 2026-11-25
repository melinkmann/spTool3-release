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

import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import math.stat.Median;
import processing.options.EventParameter;
import util.ArrUtils;

import java.util.*;

public class EventAligner {


  // this allows to tolerate n missing contributing traces/channels/collections

  // Finds the index ranges that (almost) all collections have an event on.
  //
  // An index is kept if every collection has at least one event covering it.
  // `tolerateMissing` says how many collections are allowed to have NO event there.
  // Example: 3 collections and tolerateMissing = 1 -> at least 2 collections must cover the index.
  //
  // Kept indices that sit directly next to each other (e.g. 4, 5, 6) become one event.
  // If an index in between is not kept, the result is split into two events.
  public static List<Event> mergeByAllCollections(
      List<EventCollection> collections, int tolerateMissing) {

    // A negative number makes no sense here, so treat it as 0 (= nobody may be missing)
    tolerateMissing = Math.max(0, tolerateMissing);

    List<Event> result = new ArrayList<>();
    if (!collections.isEmpty()) {

      // STEP 1: For each collection, write down which indices it covers.
      // We use one BitSet per collection. A BitSet is a list of on/off switches:
      // switch number 5 is "on" if the collection has at least one event covering index 5.
      // If several events in the same collection cover index 5, the switch is still just "on",
      // so that collection counts only once for index 5.
      // We also remember the highest index we see anywhere (maxIndex), to size the array in step 2.
      List<BitSet> covered = new ArrayList<>();
      int maxIndex = -1; // -1 means "no index seen yet"

      for (EventCollection coll : collections) {
        BitSet bits = new BitSet();
        for (Event e : coll.getNpEvents()) {
          List<Integer> indices = Optional.ofNullable(e.getIndicesList()).orElse(Collections.emptyList());
          for (int idx : indices) {
            if (idx >= 0) { // negative indices are invalid, skip them
              bits.set(idx);
            }
          }
        }
        covered.add(bits);
        maxIndex = Math.max(maxIndex, bits.length() - 1); // bits.length() - 1 = highest "on" switch
      }

      // STEP 2: For each index, count how many collections cover it.
      // coverage[5] == 2 means: exactly 2 collections have an event covering index 5.
      // If no index is covered at all, maxIndex is -1 and the array is simply empty.
      int[] coverage = new int[maxIndex + 1];
      for (BitSet bits : covered) {
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
          coverage[i]++;
        }
      }

      // STEP 3: Work out how many collections must cover an index for us to keep it.
      // Normally: number of collections minus the ones allowed to be missing.
      // But never less than 1. Otherwise, a large tolerateMissing would give 0 (or less),
      // and then even indices that no collection covers would be kept.
      int required = Math.max(1, collections.size() - tolerateMissing);

      // STEP 4: Walk through the indices from 0 upwards and build the result events.
      // - Index has too few collections: skip it.
      // - Index has enough collections: collect it and all following indices that are also
      //   good enough into one block. That block becomes one event.
      EventCollection dummyCollection = new MainEventCollection();

      int i = 0;
      while (i <= maxIndex) {
        if (coverage[i] < required) {
          i++; // not enough collections cover this index, skip it
        } else {
          List<Integer> block = new ArrayList<>();
          while (i <= maxIndex && coverage[i] >= required) {
            block.add(i);
            i++;
          }
          result.add(new NpEvent(dummyCollection, block));
        }
      }
    }
    return result;
  }


  /// minCollections is the minimum number of events that must cover an index for it to survive
  public static List<Event> mergeByCoverage(List<EventCollection> collections, int minCollections) {
    List<Event> result = new ArrayList<>();

    minCollections = Math.max(1, minCollections);

    // 1. Gather all indices and determine max index
    int maxIndex = -1;
    List<List<Integer>> allIndexLists = new ArrayList<>();

    for (EventCollection coll : collections) {
      for (Event e : coll.getNpEvents()) {
        List<Integer> indices = Optional.ofNullable(e.getIndicesList()).orElse(Collections.emptyList());

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
      if (coverage[i] < minCollections) {
        i++;
      } else {
        List<Integer> block = new ArrayList<>();
        while (i <= maxIndex && coverage[i] >= minCollections) {
          block.add(i);
          i++;
        }
        result.add(new NpEvent(dummyCollection, block));
      }
    }

    return result;
  }

  // Like mergeByCoverage, but for each merged event it also returns which collections
  // have at least one event of their own inside it.
  // A collection that is NOT in the list only got this event because other collections had one.
  // minCollections is the minimum number of events that must cover an index for it to survive.
  public static Map<Event, List<EventCollection>> mergeByCoverageWithContributors(
      List<EventCollection> collections, int minCollections) {

    // LinkedHashMap keeps the events in the order they were added (= sorted by index)
    Map<Event, List<EventCollection>> result = new LinkedHashMap<>();

    // At least 1, otherwise indices that no event covers would survive
    minCollections = Math.max(1, minCollections);

    // 1. Gather the indices of all events and determine the highest index.
    //    Same as before, but we also remember which collection each event belongs to
    //    (ownerOfEvent has the same order as allIndexLists).
    int maxIndex = -1;
    List<List<Integer>> allIndexLists = new ArrayList<>();
    List<Integer> ownerOfEvent = new ArrayList<>(); // position of the collection in `collections`

    for (int c = 0; c < collections.size(); c++) {
      for (Event e : collections.get(c).getNpEvents()) {
        List<Integer> indices = Optional.ofNullable(e.getIndicesList()).orElse(Collections.emptyList());

        allIndexLists.add(indices);
        ownerOfEvent.add(c);

        for (int idx : indices) {
          if (idx > maxIndex) {
            maxIndex = idx;
          }
        }
      }
    }

    // 2. Build the coverage array: coverage[5] = number of events covering index 5.
    //    At the same time, switch on index 5 in the BitSet of the collection the event belongs to.
    //    (If there are no indices at all, maxIndex is -1 and the array is simply empty.)
    int[] coverage = new int[maxIndex + 1];

    List<BitSet> indicesPerCollection = new ArrayList<>();
    for (int c = 0; c < collections.size(); c++) {
      indicesPerCollection.add(new BitSet());
    }

    for (int e = 0; e < allIndexLists.size(); e++) {
      BitSet ownerBits = indicesPerCollection.get(ownerOfEvent.get(e));
      for (int idx : allIndexLists.get(e)) {
        if (idx >= 0) { // negative indices are invalid, skip them
          coverage[idx]++;
          ownerBits.set(idx);
        }
      }
    }

    // 3. Extract contiguous blocks at or above the threshold.
    //    For each block, note which collections have an event of their own inside it.
    EventCollection dummyCollection = new MainEventCollection();

    int i = 0;
    while (i <= maxIndex) {
      if (coverage[i] < minCollections) {
        i++;
      } else {
        int blockStart = i;
        List<Integer> block = new ArrayList<>();
        while (i <= maxIndex && coverage[i] >= minCollections) {
          block.add(i);
          i++;
        }
        int blockEnd = i - 1;

        // A collection contributes if it has any switched-on index between blockStart and blockEnd:
        // nextSetBit(blockStart) gives its first covered index at or after blockStart.
        List<EventCollection> contributors = new ArrayList<>();
        for (int c = 0; c < collections.size(); c++) {
          int firstCovered = indicesPerCollection.get(c).nextSetBit(blockStart);
          if (firstCovered >= 0 && firstCovered <= blockEnd) {
            contributors.add(collections.get(c));
          }
        }

        result.put(new NpEvent(dummyCollection, block), contributors);
      }
    }

    return result;
  }

  // From the events a trace only received from other traces ("borrowed" events), pick those
  // whose net area is at or below mean + nSigma * SD of all borrowed net areas.
  // These are treated as background and should get their net area set to zero.
  // Needs net areas, so call this AFTER the net correction was applied to the events.
  // With fewer than minEvents borrowed events the statistics are not reliable: zero none.
  public static List<Event> selectBorrowedEventsToZero(List<Event> borrowedEvents,
                                                       int minEvents,
                                                       double zFactor) {

    List<Event> toZero = new ArrayList<>();
    if (borrowedEvents.size() >= minEvents) {

      double[] netAreas = new double[borrowedEvents.size()];
      for (int k = 0; k < netAreas.length; k++) {
        netAreas[k] = borrowedEvents.get(k).get(EventParameter.NET_AREA);
      }

      // Remove potential outliers
      // 1 find the first large gap
      double[] sortedNetAreas = ArrUtils.copy(netAreas);
      Arrays.sort(sortedNetAreas);
      // trim to avoid trouble with extreme outliers
      double lowerPercentile = Median.q(sortedNetAreas, 5);
      double upperPercentile = Median.q(sortedNetAreas, 95);
      sortedNetAreas = BaselineGenerator.removeOutliers(sortedNetAreas, lowerPercentile, upperPercentile);
      // get the first minEvents data points
      double[] diff = ArrUtils.differences(sortedNetAreas);
      int upperIndex = Math.min(diff.length - 1, minEvents);
      double diffLim = 0;
      int counter = 0;
      for (int i = 0; i <= upperIndex; i++) {
        counter++;
        diffLim += diff[i];
      }
      diffLim = 10d * diffLim / counter;
      // split array at 10 * meanDiff or so
      int limIdx = sortedNetAreas.length-1;
      for (int i = 0; i < diff.length; i++) {
        // cut if values themselves are positive and difference is large
        if (diff[i] > diffLim && sortedNetAreas[Math.max(i - 1, 0)] > 0) {
          limIdx = i - 1;
          break;
        }
      }
      limIdx = Math.max(limIdx, 0);

      // estimation base
      double[] estimRange = ArrUtils.getPortion(sortedNetAreas, 0, limIdx, null);

      double mean = MeasureOfLocation.MEAN.calc(estimRange);
      double sd = MeasureOfSpread.SD.calc(estimRange);
      double limit = mean + 1 * sd;
      double[] filteredAreas = BaselineGenerator.removeOutliers(netAreas, 0, limit);
      // update the limit
      mean = MeasureOfLocation.MEAN.calc(filteredAreas);
      sd = MeasureOfSpread.SD.calc(filteredAreas);
      limit = mean + zFactor * sd;

      for (int k = 0; k < netAreas.length; k++) {
        if (netAreas[k] < limit) {
          toZero.add(borrowedEvents.get(k));
        }
      }
    } else {
      toZero = new ArrayList<>(borrowedEvents);
    }
    return toZero;
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
