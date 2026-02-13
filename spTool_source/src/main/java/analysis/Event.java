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
import java.io.Serializable;
import java.util.List;
import processing.options.EventParameter;

public interface Event extends Serializable {

  Event copy(EventCollection parentCollection);

  EventCollection getCollection();

  void setBgPerNP(double bgPerNP);

  double getBgPerNP();

  int getNoOfPoints();

  int[] getIndices();

  int getFirst();

  int getLast();

  List<Integer> getIndicesList();

  int getStartIndexInclusive();

  int getEndIndexInclusive();

  int getPeak();

  // Call from outside for the whole collection. This avoid getting data from HDD each time.
  void calcPeakIndex(double[] intensities);

  int getCenter();

  TISeries getProfile();

  TISeries getPreviousDP(int preview);

  TISeries getFollowingDP(int preview);

  String getLabel();

  double get(EventParameter param);

  double get(EventParameter param, double[] time, double[] intensity);
}
