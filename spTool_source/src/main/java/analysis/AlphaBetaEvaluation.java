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

import java.util.List;

public class AlphaBetaEvaluation {

  // "quick and dirty chatGPT" - appears to be solid , ofc. with simple 1-1 matching,i.e.,
  // no special care to cases where two events in b could potentially match a and so forth.
  public static void checkEvents(List<Event> aList, List<Event> bList,
                                 List<Event> fromA, List<Event> fromB,
                                 List<Event> noMatchA, List<Event> noMatchB) {

    // Keep track of which events in bList have been matched
    boolean[] matchedB = new boolean[bList.size()];

    // 1. Loop through each event in aList
    for (Event a : aList) {

      // Flag to see if this a event matches any b
      boolean foundMatch = false;

      for (int i = 0; i < bList.size(); i++) {
        Event b = bList.get(i);

        // 3. Check if a and b overlap
        if (eventsOverlap(a, b)) {

          // 4. If they overlap, add to matched lists
          fromA.add(a);
          fromB.add(b);

          // Mark this b event as matched so we don't count it again
          matchedB[i] = true;
          // Set flag so we don't add this a to noMatchA
          foundMatch = true;

          // Assuming 1-to-1 matching; remove 'break' if multiple matches allowed
          break;
        }
      }

      // 5. If this a event didn't match any b event, add to noMatchA
      if (!foundMatch) {
        noMatchA.add(a);
      }
    }

    // 6. After processing all a events, check for b events that never matched
    for (int i = 0; i < bList.size(); i++) {
      if (!matchedB[i]) {
        noMatchB.add(bList.get(i));
      }
    }


  }

  private static boolean eventsOverlap(Event a, Event b) {
    // Overlap if startIdx <= other.stopIdx and stopIdx >= other.startIdx
    return a.getStartIndexInclusive() <= b.getEndIndexInclusive() && a.getEndIndexInclusive() >= b.getStartIndexInclusive();
  }

}
