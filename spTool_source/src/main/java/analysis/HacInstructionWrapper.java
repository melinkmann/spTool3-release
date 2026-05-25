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
import processing.options.ZScoreTarget;
import sandbox.montecarlo.Isotope;

import java.util.ArrayList;
import java.util.List;

public class HacInstructionWrapper {

  private final HAC.FittedHAC fittedHAC;

  // inputs for comparison
  private final boolean useLog2;
  private final ZScoreTarget zScoreTarget;
  private final List<Isotope> validIsotopes;


  public HacInstructionWrapper(HAC.FittedHAC fittedHAC,
                               boolean useLog2,
                               ZScoreTarget zScoreTarget,
                               List<Isotope> validIsotopes) {
    this.fittedHAC = fittedHAC;
    this.useLog2 = useLog2;
    this.zScoreTarget = zScoreTarget;
    this.validIsotopes = new ArrayList<>(validIsotopes);
  }

  public HAC.FittedHAC getFit() {
    return fittedHAC;
  }

  public boolean isEqualInstructions(boolean useLog2,
                                     ZScoreTarget zScoreTarget,
                                     List<Isotope> validIsotopes) {
    boolean equal = this.useLog2 == useLog2;
    equal = equal && this.zScoreTarget == zScoreTarget;
    equal = equal && this.validIsotopes == validIsotopes;

    boolean equalElements = this.validIsotopes.size() == validIsotopes.size();
    if (equalElements) {
      for (int i = 0; i < this.validIsotopes.size(); i++) {
        Isotope thisIsotope = this.validIsotopes.get(i);
        Isotope thatIsotope = validIsotopes.get(i);
        equalElements = thisIsotope.equals(thatIsotope);
        if (!equalElements) break;
      }
    }
    equal = equal && equalElements;

    return equal;
  }
}
