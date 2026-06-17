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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import analysis.quant.Calibration;
import processing.options.PopulationType;
import processing.parameterSets.ListMethod;

public class PopulationID implements Serializable {

  /*
  TODO
    - currently, we cannot have two popIDs with exact same steps.
    - however, when e.g., changing just an alpha value for search,
      we get the same step history.
      As such, the HashMap storage structure only keeps one instance.
    - Idea: we could add the label of the search instructions to the popID.
      This gives more customization of pop-label AND if applied correctly avoids HashMap
      conflicts/makes them more controllable.
   */

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private String label;
  private final PopulationType seed;
  private final List<PopulationStep> history;

  // Dummy
  public PopulationID() {
    this.label = "";
    this.history = new ArrayList<>();
    this.seed = PopulationType.UNDEFINED;
  }

  public PopulationID(PopulationType seed) {
    this.label = "";
    this.history = new ArrayList<>();
    this.seed = seed;
  }

  public PopulationID(PopulationType seed, PopulationStep initializingStep) {
    this.label = "";
    this.history = new ArrayList<>();
    history.add(initializingStep);
    this.seed = seed;
  }

  // Copy
  public PopulationID(PopulationID id) {
    this.label = id.label;
    this.history = new ArrayList<>(id.history);
    this.seed = id.seed;
  }

  public void append(PopulationStep step) {
    history.add(step);
  }

  public PopulationType getType() {
    return seed;
  }

  public List<PopulationStep> getSteps() {
    return history;
  }


  public int getOrder() {
    return 10_000 * seed.getOrder() + history.size();
  }

  public String getString() {
    StringBuilder builder = new StringBuilder(seed.toString());
    if (!history.isEmpty()) {
      builder.append(": ");
      for (int i = 0; i < history.size(); i++) {
        PopulationStep step = history.get(i);
        builder.append(step.translate());
        // Append all except last
        if (i < history.size() - 1) {
          builder.append("->");
        }
      }
    }
    return builder.toString();
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    if (label != null && !label.isEmpty()) {
      return label;
    } else {
      return getString();
    }
  }

  @Override
  public int hashCode() {
    int[] customStepHashes = history.stream()
        .mapToInt(PopulationStep::customHash)
        .toArray();

    // treat label as "optional feature"
    // DONT Objects.hashCode(label)
    // Else: hash within hashmap changes after putting, leading to fail of get(key)
    int hash = 31 * seed.hashCode() + Arrays.hashCode(customStepHashes);
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    // cast is safe
    PopulationID that = (PopulationID) o;
    boolean isEqual = false;

    // treat label as "optional feature"
    // DONT Objects.equals(label, that.label)
    // Else: hash/equals contract is broken since we cannot have it in the hash() (see above)
    if (seed == that.seed && history.size() == that.history.size()) {
      isEqual = true;
      // only make sure each step is also equivalent
      for (int i = 0; i < history.size(); i++) {
        PopulationStep thisStep = history.get(i);
        PopulationStep thatStep = that.history.get(i);
        isEqual = isEqual && thisStep.isEquivalent(thatStep);
      }
    }
    return isEqual;
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // Default deserialization
    in.defaultReadObject();
    if (label == null) {
      label = "";
    }
  }

}
