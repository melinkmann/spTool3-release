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

import dataModelNew.IncompleteParticleMatrix;
import dataModelNew.TISeries;
import dataModelNew.Trace;
import dataModelNew.TraceImpl;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import util.ArrUtils;

public class IncompleteEventCollection implements EventCollection, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(IncompleteEventCollection.class);

  private final Trace trace; // pointer to know where the data is for easy access
  private final IncompleteParticleMatrix incompleteMatrix;

  // Dummy
  public IncompleteEventCollection() {
    this.trace = new TraceImpl();
    this.incompleteMatrix = new IncompleteParticleMatrix();
  }

  public IncompleteEventCollection(Trace trace, IncompleteParticleMatrix incompleteMatrix) {
    this.trace = trace;
    this.incompleteMatrix = incompleteMatrix;
  }

  @Override
  public EventCollection getBackgroundDefiningCollection() {
    return this;
  }

  @Override
  public void add(Event event) {
    // so far, we do not manage adding of events in these types of collection
    LOGGER.error("Cannot add events to loaded collections! "
        + "This error hints to unexpected and unintended operations and/or a bug in the code.");

  }

  @Override
  public void add(List<Event> events) {
    // so far, we do not manage adding of events in these types of collection
    LOGGER.error("Cannot add events to loaded collections! "
        + "This error hints to unexpected and unintended operations and/or a bug in the code.");
  }

  // Creates a deep copy.
  public IncompleteEventCollection copy(Trace newTrace) {
    IncompleteEventCollection newCollection = new IncompleteEventCollection(newTrace,
        incompleteMatrix.copy());
    return newCollection;
  }

  @Override
  public List<Event> getNpEvents() {
    return Collections.unmodifiableList(new ArrayList<>());
  }

  // so far, we have no format or reader that would also read BG
  public List<Integer> getBackgroundIndices() {
    return new ArrayList<>();
  }

  @Override
  // so far, we have no format or reader that would also read BG
  public List<Integer> getBackgroundIndices_v2() {
    return new ArrayList<>();
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
    return incompleteMatrix.size();
  }


  @Override
  public double[] getNP(EventParameter parameter) {
    double[] data = AnalysisUtils.getFromIncomplete(incompleteMatrix, parameter);
      /*
      When exposing getters to outside, make sure to return a copy and the original!
      c.f. the getEvents() call returns an unmodifiableList (which does not exist for arrays)
      */
    data = ArrUtils.copy(data);
    return data;
  }

  /**
   * We do not really have a background data point distribution in the MC data...
   * However, it would make sense, to just use all DP that are not part of an event and go with
   * that definition. Note that this relies on the assumption that the threshold is 1.
   */
  @Override
  public double[] getBG(EventParameter parameter) {
    return new double[0];
  }

}
