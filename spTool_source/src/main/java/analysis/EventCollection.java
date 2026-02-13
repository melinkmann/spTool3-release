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
import java.io.Serializable;
import java.util.List;
import processing.options.EventParameter;
import processing.options.EventType;

public interface EventCollection extends Serializable {

  EventCollection copy(Trace newTrace);

  EventCollection getBackgroundDefiningCollection();

  public int getTotalDataPoints();

  public TISeries getCheckedTISeries();

  public Trace getTrace();

  public int size();

  void add(Event event);

  void add(List<Event> events);

  public List<Integer> getBackgroundIndices_v2();

  List<Event> getNpEvents();

  default double[] get(EventType type, EventParameter parameter) {
    if (type.equals(EventType.BG)) {
      return getBG(parameter);
    } else {
      return getNP(parameter);
    }
  }

  double[] getNP(EventParameter parameter);

  double[] getBG(EventParameter parameter);
}
