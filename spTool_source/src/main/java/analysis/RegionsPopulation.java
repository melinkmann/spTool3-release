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

import dataModelNew.Trace;

import java.util.ArrayList;
import java.util.List;

public class RegionsPopulation {

  /*
  1) Event has pointer to Collection and Collection has pointer to Trace
  --> We can only generate Collections on-demand for each Trace
  get(Trace trace) -> EventCollection

  2) Thus, here we store a List<int[]> symbolizing the regions.
  3) Align Task: SpawnParameter (Element) -> Exclusion List to exclude from Align
  (I think that's better than listening to UI selection,
  as in processing, the user is used to processing all isotopes regardless selection).
   */

  private final List<Trace> contributingTraces;

  // Most efficient way to store ranges w/o overhead is just two lists with start and stop.
  private final List<Integer> inclusiveStartIndices;
  private final List<Integer> inclusiveEndIndices;

  public RegionsPopulation() {
    this.contributingTraces = new ArrayList<>();
    this.inclusiveStartIndices = new ArrayList<>();
    this.inclusiveEndIndices = new ArrayList<>();
  }

  public void add(List<Trace> traces) {
    this.contributingTraces.addAll(traces);
  }

  public void addRegion(int... indices) {
    if (indices.length > 0) {
      inclusiveStartIndices.add(indices[0]);
      inclusiveEndIndices.add(indices[indices.length - 1]);
    }
  }

  public EventCollection getEvents(Trace trace) {
    EventCollection collection = new MainEventCollection();
    if (contributingTraces.contains(trace)) {
      if (inclusiveStartIndices.size() == inclusiveEndIndices.size()) {
        for (int i = 0; i < inclusiveStartIndices.size(); i++) {
          collection.add(new NpEvent(collection, inclusiveStartIndices.get(i), inclusiveEndIndices.get(i)));
        }
      }
    }
    return collection;
  }

  /*
  Ideas
  - Event = indices
  - store the contributing traces
  - Can we create Events from these?
  - Can we create EventCollection with these (issue: getTrace() as there is no single Trace)


    TODO
      1) Define "smallest" condition, e.g., 1E-15 and use Math.max().
         This avoids ln(0) in the Fisher method
         Consider computing a thr @ 1E-15 and copare I>thr --> a=1E-15; which ensures steadiness
      2) Write array method, that computes P values for an array of values for the same distribution
      3) Embed these into the Baseline framework
      4) For starters and dynamic baselines, maybe just interpolate between alphas? (I know, not ideal, but easier
         than computing p for each data point where we interpolate mu and SD, b/c that creates too many distr instances)
   */


}
