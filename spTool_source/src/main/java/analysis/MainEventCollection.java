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

import dataModelNew.TISeries;
import dataModelNew.Trace;
import dataModelNew.TraceImpl;

import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import processing.options.EventParameter;
import util.ArrUtils;

public class MainEventCollection implements EventCollection, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final Trace trace; // pointer to know where the data is for easy access
  private final List<Event> npEvents;

  // accelerate computation but remove if memory is low
  private transient SoftReference<HashMap<EventParameter, double[]>> eventParamRef;
  private transient SoftReference<HashMap<EventParameter, double[]>> bgParamRef;
  private transient SoftReference<List<Integer>> backgroundIndicesRef;

  // Dummy
  public MainEventCollection() {
    this.trace = new TraceImpl();
    this.npEvents = new ArrayList<>();
    this.eventParamRef = new SoftReference<>(null);
    this.bgParamRef = new SoftReference<>(null);
    this.backgroundIndicesRef = new SoftReference<>(null);
  }

  public MainEventCollection(Trace trace) {
    this.trace = trace;
    this.npEvents = new ArrayList<>();
    this.eventParamRef = new SoftReference<>(null);
    this.bgParamRef = new SoftReference<>(null);
    this.backgroundIndicesRef = new SoftReference<>(null);
  }

  // for the copy constructor AND subEventCollections
  public MainEventCollection(Trace trace, List<Event> npEvents) {
    this.npEvents = npEvents;
    this.trace = trace;
    this.eventParamRef = new SoftReference<>(null);
    this.bgParamRef = new SoftReference<>(null);
    this.backgroundIndicesRef = new SoftReference<>(null);
  }

  // Creates a deep copy.
  public MainEventCollection copy(Trace newTrace) {
    MainEventCollection newCollection = new MainEventCollection(newTrace);
    final List<Event> newEvents = new ArrayList<>();
    for (Event npEvent : npEvents) {
      newEvents.add(npEvent.copy(this));
    }
    newCollection.add(newEvents);
    return newCollection;
  }


  @Override
  public EventCollection getBackgroundDefiningCollection() {
    return this;
  }

  @Override
  public void add(Event event) {
    npEvents.add(event);
    backgroundIndicesRef = new SoftReference<>(null); // invalidate
  }

  @Override
  public void add(List<Event> events) {
    npEvents.addAll(events);
    backgroundIndicesRef = new SoftReference<>(null); // invalidate
  }

  @Override
  public List<Event> getNpEvents() {
    return Collections.unmodifiableList(npEvents);
  }

  // "obvious POJO" version that does not use the BitSet idea from gpt (see above)
  public List<Integer> getBackgroundIndices() {
    List<Integer> bgIndices = new ArrayList<>();
    if (backgroundIndicesRef != null) {
      List<Integer> refList = backgroundIndicesRef.get();
      if (refList != null) {
        bgIndices = refList;
      }
    }

    // Did we manage to extract something or is the list empty?
    if (bgIndices.isEmpty()) {

      // extract NP indices
      HashSet<Integer> allNpIndices = new HashSet<>();

      for (Event event : npEvents) {
        allNpIndices.addAll(event.getIndicesList());
      }

      // now, make the negative of it.
      bgIndices = new ArrayList<>(getTotalDataPoints() - allNpIndices.size());

      for (int i = 0; i < getTotalDataPoints(); i++) {
        if (!allNpIndices.contains(i)) {
          bgIndices.add(i);
        }
      }
    }
    backgroundIndicesRef = new SoftReference<>(bgIndices);
    return bgIndices;
  }

  @Override
  // Modified from ChatGPT4: BitSet implementation. For 90E6 DP about 2s faster (3 vs 5 s).
  public List<Integer> getBackgroundIndices_v2() {
    List<Integer> bgIndices = new ArrayList<>();
    if (backgroundIndicesRef != null) {
      List<Integer> refList = backgroundIndicesRef.get();
      if (refList != null) {
        bgIndices = refList;
      }
    }

    // Did we manage to extract something or is the list empty?
    if (bgIndices.isEmpty()) {
      BitSet npBitSet = new BitSet(getTotalDataPoints());

      /*
       Set bits for NP indices
          - bit set is initialized to at least hold n = totalDatapoints
          - all are initialized as false
          - set turns the respective position to true
       */
      for (Event event : npEvents) {
        for (int index : event.getIndicesList()) {
          npBitSet.set(index);
        }
      }

      /*
       Collect background indices
          - cardinality --> number bits set to true, i.e., number of np-indices
          - clear       --> sets the index to false
          - nextClearBit --> Returns the index of the first bit that is set to false
                             that occurs on or after the specified starting index.
       */
      bgIndices = new ArrayList<>(getTotalDataPoints() - npBitSet.cardinality());
      for (int i = npBitSet.nextClearBit(0);
           i >= 0 && i < getTotalDataPoints();
           i = npBitSet.nextClearBit(i + 1)) {
        bgIndices.add(i);
      }

    }
    backgroundIndicesRef = new SoftReference<>(bgIndices);
    return bgIndices;
  }

  // Above method optimized with claude sonnet 4.6
  public static List<Integer> getBGIndicesBitSet(int totalDP, List<Integer> npIndices, int subSampleSize) {
    BitSet npBitSet = new BitSet(totalDP);
    for (int index : npIndices) {
      npBitSet.set(index);
    }

    int bgCount = totalDP - npBitSet.cardinality();
    int capacity = Math.min(subSampleSize, bgCount);
    List<Integer> bgIndices = new ArrayList<>(capacity);

    for (int i = npBitSet.nextClearBit(0);
         i >= 0 && i < totalDP && bgIndices.size() < capacity;
         i = npBitSet.nextClearBit(i + 1)) {
      bgIndices.add(i);
    }
    return bgIndices;
  }

  @Override
  public int getTotalDataPoints() {
    return trace.getTISeries().size();
  }

  @Override
  public TISeries getCheckedTISeries() {
    return trace.getTISeries();
  }

  @Override
  public Trace getTrace() {
    return trace;
  }

  @Override
  public int size() {
    return npEvents.size();
  }

  @Override
  public double[] getNP(EventParameter parameter) {
    /*
     Ideally, a single zero would probably be more accurate. data = new double[0];
     If nothing is present and only zeros are returned, one may be tempted
     to believe that there actually are zeros.
     However, to be safe in iterations, may be smart to return something of the same size.
     */

    double[] data = new double[size()];
    HashMap<EventParameter, double[]> values = extractNPValuesChecked(parameter);
    if (values.containsKey(parameter)) {
      data = values.get(parameter);
      /*
      When exposing getters to outside, make sure to return a copy and the original!
      c.f. the getEvents() call returns an unmodifiableList (which does not exist for arrays)
      */
      data = ArrUtils.copy(data);
    }
    return data;
  }

  /**
   * Guarantees that at least the requested parameter is in the map. Maybe, if previous access has
   * been called, others may or may not still be there - depends on garbage collection on the weak
   * reference.
   */
  private HashMap<EventParameter, double[]> extractNPValuesChecked(EventParameter parameter) {
    HashMap<EventParameter, double[]> result;
    if (eventParamRef != null) {
      HashMap<EventParameter, double[]> values = eventParamRef.get();
      if (values != null) {
        // Check if key is already there - if so, just return
        if (values.containsKey(parameter)) {
          result = new HashMap<>(values);
        } else {
          // Key was not there - keep previous results but add the latest request.
          result = new HashMap<>(values);
          result.putAll(extractNPValues(parameter));
        }
      } else {
        result = extractNPValues(parameter);
        eventParamRef = new SoftReference<>(result);
      }
    } else {
      result = extractNPValues(parameter);
      eventParamRef = new SoftReference<>(result);
    }
    return result;
  }

  private HashMap<EventParameter, double[]> extractNPValues(EventParameter parameter) {
    HashMap<EventParameter, double[]> result = new HashMap<>();
    double[] data = AnalysisUtils.getNPsFromEvaluation(this, parameter);
    result.put(parameter, data);
    return result;
  }

  /**
   * Background data.
   */
  @Override
  public double[] getBG(EventParameter parameter) {
        /*
     Ideally, a single zero would probably be more accurate. data = new double[0];
     If nothing is present and only zeros are returned, one may be tempted
     to believe that there actually are zeros.
     However, to be safe in iterations, may be smart to return something of the same size.
     */

    double[] data = new double[Math.max(getCheckedTISeries().size() - size(), 0)];
    HashMap<EventParameter, double[]> values = extractBGValuesChecked(parameter);
    if (values.containsKey(parameter)) {
      data = values.get(parameter);
      /*
      When exposing getters to outside, make sure to return a copy and the original!
      c.f. the getEvents() call returns an unmodifiableList (which does not exist for arrays)
      */
      data = ArrUtils.copy(data);
    }
    return data;
  }

  /**
   * Guarantees that at least the requested parameter is in the map. Maybe, if previous access has
   * been called, others may or may not still be there - depends on garbage collection on the weak
   * reference.
   */
  private HashMap<EventParameter, double[]> extractBGValuesChecked(EventParameter parameter) {
    HashMap<EventParameter, double[]> result;
    if (bgParamRef != null) {
      HashMap<EventParameter, double[]> values = bgParamRef.get();
      if (values != null) {
        // Check if key is already there - if so, just return
        if (values.containsKey(parameter)) {
          result = new HashMap<>(values);
        } else {
          // Key was not there - keep previous results but add the latest request.
          result = new HashMap<>(values);
          result.putAll(extractBGValuesFromTrace(parameter));
          bgParamRef = new SoftReference<>(result);
        }
      } else {
        result = extractBGValuesFromTrace(parameter);
        bgParamRef = new SoftReference<>(result);
      }
    } else {
      result = extractBGValuesFromTrace(parameter);
      bgParamRef = new SoftReference<>(result);
    }
    return result;
  }

  private HashMap<EventParameter, double[]> extractBGValuesFromTrace(EventParameter parameter) {
    HashMap<EventParameter, double[]> result = new HashMap<>();

    List<Integer> bgIndices = getBackgroundDefiningCollection().getBackgroundIndices_v2();

    double[] data = AnalysisUtils.getBGsFromEvaluation(bgIndices, parameter,
        trace.getTISeries().getTime(),
        trace.getTISeries().getIntensity());

    result.put(parameter, data);
    return result;
  }
}
