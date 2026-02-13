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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class Aligner {

  public static List<MergedEvent> merge(List<Population> populations) {
    List<MergedEvent> mergedEvents = new ArrayList<>();

    if (!populations.isEmpty()) {
      int rawDataLength = populations.get(0).getEvents().getTrace().getTISeries().size();

      @SuppressWarnings("unchecked")
      List<Event>[] markerArray = new ArrayList[rawDataLength];

      for (Population population : populations) {
        EventCollection mainEventCollection = population.getEvents();

        for (Event event : mainEventCollection.getNpEvents()) {
          for (int idx : event.getIndices()) {
            if (markerArray[idx] == null) {
              markerArray[idx] = new ArrayList<>();
            }
            markerArray[idx].add(event);
          }

        }

      }

      // "Search" events
      int lastIndex =markerArray.length - 1;
      List<Integer> currentEventIndices = new ArrayList<>();
      HashSet<Event> currentContributingEvents = new LinkedHashSet<>();
      for (int i = 0; i < markerArray.length; i++) {
        List<Event> markerList = markerArray[i];
        if (markerList != null) {
          currentEventIndices.add(i);
          currentContributingEvents.addAll(markerList);
          if (i == lastIndex) {
            MergedEvent mgEv =new MergedEvent(currentEventIndices);
            mgEv.addEvents(currentContributingEvents);
            mergedEvents.add(mgEv);
            currentEventIndices.clear();
            currentContributingEvents.clear();
          }
        }else {
          if (!currentEventIndices.isEmpty()) {
            MergedEvent mgEv =new MergedEvent(currentEventIndices);
            mgEv.addEvents(currentContributingEvents);
            mergedEvents.add(mgEv);
            currentEventIndices.clear();
            currentContributingEvents.clear();
          }
        }
      }
    }

    /*
     TODO:
      Make merged event class that extends NP EVEnt
      Set bg per np (considering new number of points!)
      Set height
      Keep list of contributing events in the "merged event"
     */
    return mergedEvents;
  }


  public static class MergedEvent {

    private final List<Event> contributingEvents = new ArrayList<>();
    private final int startInclusive;
    private final int endInclusive;

    public MergedEvent(List<Integer> indices) {
      if (!indices.isEmpty()) {
        Collections.sort(
            indices); // make sure we are in order (see e.g. window search which may interfere here)
        this.startInclusive = indices.get(0);
        this.endInclusive = indices.get(indices.size() - 1);
      } else {
        this.startInclusive = -1;
        this.endInclusive = -1;
      }
    }

    public void addEvents (Collection<Event> contributingEvents){
      this.contributingEvents.addAll(contributingEvents);
    }


    // TODO: calc BG per NP for new length of the final event.
    // TODO: Organize how to add Trace/Collection as this event does not only have one Trace associated with it.
    // TODO: Ideally, similar to PartPopMatrix, stores the aligned populations at sample level.
  }


}
