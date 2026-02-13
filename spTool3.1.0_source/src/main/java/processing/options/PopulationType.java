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

package processing.options;

public enum PopulationType {

  /*
  Principle: Check how many pops we have?
  Then, check if e.g. PNC is there.
  If not, use size for both size and PNC.
  I think, we should implement some kind of order here
  as to how we lookup.

  Maybe:
  - only one --> use that one
  - event overlap is bad for mass and clustering
     --> they should replace each other
  - if cluster and number are present, use cluster for size
    as this would be more likely to later occur in a comparison
    (sizes of clusters). N(Cluster) is a whole different story and
    does not connect with N(NP).
   */

  SIMULATION {
    @Override
    public String toString() {
      return "Synthetic population";
    }

    @Override
    public int getOrder() {
      return 0;
    }
  },

  EXTERNAL {
    @Override
    public String toString() {
      return "External evaluation";
    }

    @Override
    public int getOrder() {
      return 0;
    }
  },

  SIZE {
    @Override
    public String toString() {
      return "Mass";
    }

    @Override
    public int getOrder() {
      return 1;
    }
  },

  NUMBER {
    @Override
    public String toString() {
      return "Number";
    }

    @Override
    public int getOrder() {
      return 2;
    }
  },

  CLUSTER {
    @Override
    public String toString() {
      return "Clusters";
    }

    @Override
    public int getOrder() {
      return 3;
    }
  },

  // If the user specifies 2 time the same typ
  UNDEFINED {
    @Override
    public String toString() {
      return "Undefined";
    }

    @Override
    public int getOrder() {
      return 4;
    }
  };

  public static PopulationType[] getEvaluationCases() {
    return new PopulationType[]{SIZE, NUMBER, CLUSTER};
  }

  public abstract int getOrder();

}
