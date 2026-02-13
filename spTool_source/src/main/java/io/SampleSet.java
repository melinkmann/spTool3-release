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

package io;

import dataModelNew.Sample;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class  SampleSet implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final List<Sample> samples;

  private String label = "Set of Samples";
  private final boolean canRename;

  public SampleSet() {
    this.samples = new ArrayList<>();
    canRename = true;
  }

  public SampleSet(List<Sample> samples) {
    this.samples = samples;
    canRename = true;
  }

  public SampleSet(String label, List<Sample> samples) {
    this.label = label;
    this.samples = new ArrayList<>(samples);
    canRename = true;
  }

  public SampleSet(String label, Sample sample) {
    this.label = label;
    this.samples = new ArrayList<>(Collections.singletonList(sample));
    canRename = true;
  }

  public SampleSet(String label, List<Sample> samples, boolean canRename) {
    this.label = label;
    this.samples = new ArrayList<>(samples);
    this.canRename = canRename;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isCanRename() {
    return canRename;
  }

  public List<Sample> getSamples() {
    return samples;
  }


  public void add(Sample sample) {
    samples.add(sample);
  }

  public void add(List<Sample> samples) {
    this.samples.addAll(samples);
  }


  public void remove(Sample sample){
    this.samples.remove(sample);
  }


  public void remove(List<Sample> samples){
    this.samples.removeAll(samples);
  }

}