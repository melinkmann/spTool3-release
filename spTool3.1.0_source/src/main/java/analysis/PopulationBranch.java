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

import analysis.PopulationStep.SearchSubtype;
import dataModelNew.Trace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import processing.options.PopulationType;
import processing.parameterSets.impl.NormalSearchParams;

public class PopulationBranch {

  private final Map<Trace, PopulationID> branchMap = Collections.synchronizedMap(new HashMap<>());

  /*
    We do not want to allow duplicates of the same type.
    Hence, we have to keep track of what has already been specified.
   */
  private final Map<Trace, List<PopulationType>> usedTypes = Collections
      .synchronizedMap(new HashMap<>());


  /**
   * A search operation initializes an evaluation branch.
   */
  public synchronized PopulationID startBranchForTrace(Trace trace,
      NormalSearchParams searchParams) {

    PopulationType type = searchParams.getTargetPopulation().getValue();

    // Check if the type is already taken, i.e., a duplicate. "The first takes it all"
    synchronized (usedTypes) {
      List<PopulationType> typeList = usedTypes.get(trace);
      if (typeList != null) {
        if (typeList.contains(type)) {
          type = PopulationType.UNDEFINED;
        } else {
          usedTypes.get(trace).add(type);
        }
      } else {
        typeList = Collections.synchronizedList(new ArrayList<>());
        typeList.add(type);
        usedTypes.put(trace, typeList);
      }
    }

    PopulationStep step = new SearchSubtype(searchParams.getSearchAlgorithm().getValue());
    PopulationID newID = new PopulationID(type, step);

    synchronized (branchMap) {
      branchMap.put(trace, newID);
    }

    return newID;
  }

  @Nullable
  public synchronized PopulationID getID(Trace trace) {
    PopulationID id;
    synchronized (branchMap) {
      id = branchMap.get(trace);
    }
    return id;
  }

  /**
   * e.g., Filter has created a new Population (and ID) and replaces the existing ID.
   */
  public synchronized void overrideID(Trace trace, PopulationID newID) {
    synchronized (branchMap) {
      branchMap.put(trace, newID);
    }
  }

  public synchronized void clear() {
    synchronized (branchMap) {
      branchMap.clear();
    }
  }

}
