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
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


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

  private final EventCollection backgroundDefiningCollection;

  // Dummy
  public SubEventCollection() {
    super();
    this.backgroundDefiningCollection = new MainEventCollection();
  }

  public SubEventCollection(Trace trace, EventCollection backgroundDefiningCollection) {
    super(trace);
    this.backgroundDefiningCollection = backgroundDefiningCollection;
  }

  public SubEventCollection(Trace trace, List<Event> npEvents,
      Population parentPopulation) {
    super(trace, npEvents);
    this.backgroundDefiningCollection = parentPopulation.getBgDefiningCollection();
  }


  // Creates a deep copy.
  public SubEventCollection copy(Trace newTrace) {
    EventCollection copyMainBG = backgroundDefiningCollection.copy(newTrace);
    SubEventCollection newCollection = new SubEventCollection(newTrace, copyMainBG);
        final List<Event> newEvents = new ArrayList<>();
        for (Event npEvent : getNpEvents()) {
          newEvents.add(npEvent.copy(this));
        }
        newCollection.add(newEvents);
    return newCollection;
  }


  @Override
  public EventCollection getBackgroundDefiningCollection() {
    return backgroundDefiningCollection;
  }


}
