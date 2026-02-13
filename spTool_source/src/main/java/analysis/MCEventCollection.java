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
import dataModelNew.TraceMC;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import sandbox.montecarlo.ParticlePopulationMatrix;
import util.ArrUtils;

public class MCEventCollection implements EventCollection,
    Serializable {


  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(MCEventCollection.class);

  private final TraceMC trace;
  private transient SoftReference<List<Event>> npEventsRef;
  private transient SoftReference<HashMap<EventParameter, double[]>> eventParameterRef;
  private transient SoftReference<HashMap<EventParameter, double[]>> bgParamRef;
  private transient SoftReference<List<Integer>> backgroundIndicesRef;

  // Dummy
  public MCEventCollection() {
    this.trace = new TraceMC();
    this.npEventsRef = new SoftReference<>(null);
    this.eventParameterRef = new SoftReference<>(null);
    this.bgParamRef = new SoftReference<>(null);
    this.backgroundIndicesRef = new SoftReference<>(null);
  }

  // For the copy method
  public MCEventCollection(TraceMC trace) {
    this.trace = trace;
    this.npEventsRef = new SoftReference<>(null);
    this.eventParameterRef = new SoftReference<>(null);
    this.bgParamRef = new SoftReference<>(null);
    this.backgroundIndicesRef = new SoftReference<>(null);
  }

  // Creates a deep copy.
  public MCEventCollection copy(Trace newTrace) {
    //    // Events
    //    final List<Event> newEvents = new ArrayList<>();
    //    List<Event> list = npEventsRef.get();
    //    final List<Event> oldEvents = new ArrayList<>();
    //    if (npEventsRef != null && list != null) {
    //      oldEvents.addAll(list);
    //    }
    //    for (Event npEvent : oldEvents) {
    //      newEvents.add(npEvent.copy(this));
    //    }
    //
    //    // Data
    //    HashMap<EventParameter, double[]> data = new HashMap<>();
    //    if (eventParameterRef != null) {
    //      HashMap<EventParameter, double[]> oldData;
    //      oldData = eventParameterRef.get();
    //      if (oldData != null) {
    //        data = new HashMap<>();
    //        for (EventParameter key : oldData.keySet()) {
    //          data.put(key, ArrUtils.copy(oldData.get(key)));
    //        }
    //      }
    //    }
    MCEventCollection newCollection;
    if (newTrace instanceof TraceMC) {
      newCollection = new MCEventCollection((TraceMC) newTrace);
    } else {
      newCollection = new MCEventCollection();
      LOGGER.warn("Cannot copy event collection. Expected in-silico trace but got bad type. "
          + "If you are seeing this message, this indicates a coding error or an unexpected scenario.");
    }
    return newCollection;
  }

  @Override
  public EventCollection getBackgroundDefiningCollection() {
    return this;
  }

  @Override
  public void add(Event event) {
    // do nothing: any change will be overwritten as soon as the weak reference dissolves
    LOGGER.error("Cannot add events to simulated collections! "
        + "This error hints to unexpected and unintended operations and/or a bug in the code.");
  }

  @Override
  public void add(List<Event> events) {
    // do nothing: any change will be overwritten as soon as the weak reference dissolves
    LOGGER.error("Cannot add events to simulated collections! "
        + "This error hints to unexpected and unintended operations and/or a bug in the code.");
  }


  @Override
  public List<Event> getNpEvents() {
    List<Event> events = getEventsChecked();
    return Collections.unmodifiableList(events);
  }

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

      List<Event> events = getEventsChecked();

      for (Event event : events) {
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

      List<Event> events = getEventsChecked();

      for (Event event : events) {
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

  @Override
  public int getTotalDataPoints() {
    return trace.getTISeries().size();
  }

  @Override
  public TISeries getCheckedTISeries() {
    TISeries tiSeries = trace.getTISeries();
    if (trace.hasLimits()) {
      tiSeries = trace.getOriginalTISeries();
    }
    return tiSeries;
  }

  @Override
  public Trace getTrace() {
    return trace;
  }

  @Override
  public int size() {
    /*
     Although slightly "inconsistent", when we only want the size, we do not have to calculate everything.
     Otherwise we call the method that integrates all peaks. Unsure if it is possible that these
     yield different results if some checks for indices or so fail.
    */
    List<ParticlePopulationMatrix> particleMatrices = trace.getSample().getMatrices(trace);

    int size = particleMatrices.stream()
        .map(ParticlePopulationMatrix::getNumberOfEvents)
        .mapToInt(Integer::intValue)
        .sum();
    return size;
  }

  @Override
  public double[] getNP(EventParameter parameter) {

    /*
     Ideally, a single zero would probably be more accurate.
     If nothing is present and only zeros are returned, one may be tempted
     to believe that there actually are zeros.
     However, to be safe in iterations, may be smart to return something of the same size.
     */

    double[] data = new double[size()];

    HashMap<EventParameter, double[]> values = extractValuesChecked();
    if (values.containsKey(parameter)) {
      data = values.get(parameter);
      /*
      When exposing getters to outside, make sure to return a copy and the original!
      c.f. the getEvents() call returns an unmodifiableList (which does not exist for arrays)
       */
      data = ArrUtils.copy(data);
    } else {
      LOGGER.trace("Event collection does not provide this parameter type.");
    }

    return data;
  }

  private List<Event> getEventsChecked() {
    List<Event> result = new ArrayList<>();
    if (npEventsRef != null) {
      List<Event> events = npEventsRef.get();
      if (events != null) {
        result.addAll(events);
      } else {
        result = extractEventsFromTrace();
        npEventsRef = new SoftReference<>(result);
      }
    } else {
      result = extractEventsFromTrace();
      npEventsRef = new SoftReference<>(result);
    }
    return result;
  }

  /**
   * Note: If an event has no data points b/c the integration had nothing >1 cts, then we get
   * startIdx = -1 and endIdx = -1 as return values from the integrator. In that case, we do not
   * list the event. Hence, this function may yield very different values than the parameter
   * getter.
   */
  private List<Event> extractEventsFromTrace() {
    List<Event> result = new ArrayList<>();
    HashMap<EventParameter, double[]> values = extractValuesChecked();
    double[] startIndices = values.get(EventParameter.START_INDEX);
    double[] endIndices = values.get(EventParameter.END_INDEX);
    if (startIndices != null && endIndices != null
        && startIndices.length == endIndices.length) {
      for (int i = 0; i < startIndices.length; i++) {
        // For type reasons, we must return all data as double[]; indices are converted back and forth
        int startIdx = (int) startIndices[i];
        int endIdx = (int) endIndices[i];
        if ((startIdx < endIdx || startIdx == endIdx) && startIdx >= 0) {
          Event event = new NpEvent(this, startIdx, endIdx);
          // new feature: allowing BG in simulated events
          event.setBgPerNP(trace.getEmpiricalMeanBG() * event.getNoOfPoints());
          result.add(event);
        }
      }
    }
    return result;
  }

  private HashMap<EventParameter, double[]> extractValuesChecked() {
    HashMap<EventParameter, double[]> result;
    if (eventParameterRef != null) {
      HashMap<EventParameter, double[]> values = eventParameterRef.get();
      if (values != null) {
        result = new HashMap<>(values);
      } else {
        result = extractValuesFromTrace();
        eventParameterRef = new SoftReference<>(result);
      }
    } else {
      result = extractValuesFromTrace();
      eventParameterRef = new SoftReference<>(result);
    }
    return result;
  }

  private HashMap<EventParameter, double[]> extractValuesFromTrace() {
    HashMap<EventParameter, double[]> result = AnalysisUtils.getFromSimulation(trace);
    return result;
  }

  /**
   * Background data. We do not really have a background data point distribution in the MC data...
   * However, it would make sense, to just use all DP that are not part of an event and go with that
   * definition. Note that this relies on the assumption that the threshold is 1.
   */
  @Override
  public double[] getBG(EventParameter parameter) {

    // We cannot easily say how many DP there will be as BG unless we get sum of all NP DP first.
    // Given that we likely yield a good result here anyway, I think estimating a an expected value
    // is not necessary and new double[0] is fine;

    double[] data = new double[0];
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


