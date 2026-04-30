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

import math.HAC;
import sandbox.montecarlo.Isotope;

import java.util.ArrayList;
import java.util.List;

public class HacCrWrapper {

  private final HAC.FittedHAC fittedHAC;

  // inputs for comparison
  private final double intensityThreshold;
  private final boolean useLog2;
  private final boolean useZScore;
  private final boolean excludeElements;
  List<Isotope> excludedList;


  public HacCrWrapper(HAC.FittedHAC fittedHAC,
                      double intensityThreshold,
                      boolean useLog2,
                      boolean useZScore,
                      boolean excludeElements,
                      List<Isotope> excludedList) {
    this.fittedHAC = fittedHAC;
    this.intensityThreshold = intensityThreshold;
    this.useLog2 = useLog2;
    this.useZScore = useZScore;
    this.excludeElements = excludeElements;
    this.excludedList = new ArrayList<>(excludedList);
  }

  public HAC.FittedHAC getFit() {
    return fittedHAC;
  }

  public boolean isEqualInstructions(double intensityThreshold,
                                     boolean useLog2,
                                     boolean useZScore,
                                     boolean excludeElements,
                                     List<Isotope> excludedList) {
    boolean equal = this.useLog2 == useLog2;
    equal = equal && this.intensityThreshold == intensityThreshold;
    equal = equal && this.useZScore == useZScore;
    equal = equal && this.excludeElements == excludeElements;

    boolean equalElements = this.excludedList.size() == excludedList.size();
    if (equalElements) {
      for (int i = 0; i < this.excludedList.size(); i++) {
        Isotope thisIsotope = this.excludedList.get(i);
        Isotope thatIsotope = excludedList.get(i);
        equalElements = thisIsotope.equals(thatIsotope);
        if (!equalElements) break;
      }
    }
    equal = equal && equalElements;

    return equal;
  }
}
