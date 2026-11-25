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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


/**
 * Purpose of this subclass: We define background as "anything that is not event". Here, the problem
 * is that ROIs or filtered populations (e.g. MultiEvents with n=3 or  over range events) have many
 * DP that are in fact events but not events of that population. Hence, we need a reference to the
 * original population where the true decision "event or not" is stored.
 */
public class SubEventCollection extends MainEventCollection implements EventCollection,
    Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(SubEventCollection.class);

  private final EventCollection backgroundDefiningCollection;

  // Dummy
  public SubEventCollection() {
    super();
    this.backgroundDefiningCollection = new MainEventCollection();
  }


  // Also called by copy constructor
  public SubEventCollection(Trace trace, EventCollection backgroundDefiningCollection) {
    super(trace);
    // added .copy(trace); to produce consistent references with the copy() method of this [???]
    this.backgroundDefiningCollection = backgroundDefiningCollection;
  }

  // Creates a deep copy.
  public SubEventCollection copy(Trace newTrace) {
    EventCollection copyMainBG = backgroundDefiningCollection.copy(newTrace);
    SubEventCollection newCollection = new SubEventCollection(newTrace, copyMainBG);
    final List<Event> newEvents = new ArrayList<>();
    for (Event npEvent : getNpEvents()) {
      newEvents.add(npEvent.copy(newCollection));
    }
    newCollection.add(newEvents);
    return newCollection;
  }


  @Override
  public EventCollection getBackgroundDefiningCollection() {
    return backgroundDefiningCollection;
  }

  @Override
  public void add(Event event) {
    if (!Objects.equals(event.getCollection(), this)) {
      super.add(event.copy(this));
      LOGGER.error("Cannot add event to sub event-collection if event does not expose this collection as " +
          "its" +
          "referenced collection. The events were copied and copy was added. This indicates a coding error.");
    } else {
      super.add(event);
    }
  }

  @Override
  public void add(List<Event> events) {
    int badEventCounter = 0;
    for (Event event : events) {
      if (Objects.equals(event.getCollection(), this)) {
        super.add(event);
      } else {
        super.add(event.copy(this));
        badEventCounter++;
      }
    }
    if (badEventCounter > 0) {
      LOGGER.error("Cannot add events to sub event-collection if event does not expose this collection as " +
              "its" +
              "referenced collection. The events were copied and copy was added. Number of bad events {} / " +
              "{}. This indicates a coding error.",
          badEventCounter, events.size());
    }
  }

  public boolean remove(Event event) {
    // remove
    boolean rem = super.npEvents.remove(event);
    // invalidate all cashes
    invalidateCashes();
    return rem;
  }

  public boolean removeIf(Predicate<Event> filter) {
    // remove
    boolean rem = super.npEvents.removeIf(filter);
    // invalidate all cashes
    invalidateCashes();
    return rem;
  }


}
