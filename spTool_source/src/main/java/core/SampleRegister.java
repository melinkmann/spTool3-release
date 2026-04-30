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

package core;

import dataModelNew.Sample;
import io.SampleSet;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SampleRegister implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final SampleSet mainSet = new SampleSet("Samples", new ArrayList<>(), false);

  private String versionInfo = "";

  private final Set<SampleSet> customSets = new LinkedHashSet<>(); // HashSet: all values are unique
  private final AtomicInteger counterOfImports = new AtomicInteger(0);

  // Organize import of newly added samples.
  private final List<Sample> waitingSamples = Collections.synchronizedList(new ArrayList<>());

  //

  public SampleRegister() {
  }

  public List<SampleSet> getCustomSetsUnmodifiable() {
    // Note that copyOf() gives an unmodifiable list.
    return List.copyOf(customSets);
  }

  public void addSetDirectly(SampleSet set) {
    // Only add new samples!
    List<Sample> existingSamples = mainSet.getSamples();
    set.getSamples().stream()
        .filter(s -> !existingSamples.contains(s))
        .forEach(s -> mainSet.getSamples().add(s));
    //
    customSets.add(set);
    // refresh UI
    SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
  }

  public void addToExistingSetDirectly(SampleSet set) {
    // Only add new samples!
    List<Sample> existingSamples = mainSet.getSamples();

    // add to main anyway
    set.getSamples().stream()
        .filter(s -> !existingSamples.contains(s))
        .forEach(s -> mainSet.getSamples().add(s));

    // check if already exists
    String incomingName = set.getLabel();
    List<SampleSet> setsWithSameName = customSets.stream()
        .filter(s -> s.getLabel().equals(incomingName))
        .collect(Collectors.toList());

    if (!setsWithSameName.isEmpty()) {
      SampleSet firstMatch = setsWithSameName.get(0);
      firstMatch.getSamples().addAll(set.getSamples());
    } else {
      customSets.add(set);
    }
    // refresh UI
    SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
  }

  public void removeSetDirectly(List<SampleSet> sets) {
    customSets.removeAll(sets);
  }


  public void removeSamplesEntirely(List<Sample> samples) {
    mainSet.getSamples().removeAll(samples);
    customSets.forEach(s -> s.getSamples().removeAll(samples));
  }

  public SampleSet getMainSet() {
    return mainSet;
  }

  public List<SampleSet> getAllSets() {
    List<SampleSet> sets = new ArrayList<>();
    sets.add(mainSet);
    sets.addAll(customSets);
    return sets;
  }

  // Synchronized access to the lists.
  public synchronized void addNewSampleToWaitingList(Sample sample) {
    synchronized (waitingSamples) {
      waitingSamples.add(sample);
    }
  }

  public synchronized void addNewSampleToWaitingList(List<Sample> samples) {
    synchronized (waitingSamples) {
      waitingSamples.addAll(samples);
    }
  }

  /**
   * Adds the new samples to the main list and also creates a new set with the label.
   */
  public synchronized void createSetAndFlushWaitingList(String label) {
    synchronized (waitingSamples) {
      synchronized (customSets) {
        if (!waitingSamples.isEmpty()) {

          // check if creation of new set is desired
          boolean addAsSet = SpTool3Main.getRunTime().getConfParams().getCreateNewSampleSet().getValue();
          boolean increment = SpTool3Main.getRunTime().getConfParams().getIncrementNewSampleSet().getValue();

          if (addAsSet) {
            String labelStr = label;
            if (increment) {
              labelStr += (" " + counterOfImports.incrementAndGet());
            }

            // Check if set of that name already exists
            boolean foundExistingSet = false;
            for (SampleSet customSet : customSets) {
              if (customSet.getLabel().equals(labelStr)) {
                customSet.add(waitingSamples);
                foundExistingSet = true;
                break;
              }
            }

            // was not added to old set -> add to new set
            if (!foundExistingSet) {
              customSets.add(new SampleSet(labelStr, waitingSamples));
            }
          }
          mainSet.getSamples().addAll(waitingSamples);
          waitingSamples.clear();
        }
      }
    }
    // refresh UI
    SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
  }

  /**
   * Adds the new samples to the main list without creating a new set.
   */
  public synchronized void flushWaitingList() {
    synchronized (waitingSamples) {
      mainSet.getSamples().addAll(waitingSamples);
      waitingSamples.clear();
    }
    // refresh UI
    SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
  }


  // For the serializer
  public String getVersionInfo() {
    return versionInfo;
  }

  public void setVersionInfo(String versionInfo) {
    this.versionInfo = versionInfo;
  }
}
