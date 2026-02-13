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

package io.export;

import dataModelNew.mz.Element;
import it.unimi.dsi.fastutil.Hash;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import processing.options.EventParameter;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;

public class ExportSimulationEventContainer {

  private final List<String> labels = new ArrayList<>();

  private final List<Integer> nEvents = new ArrayList<>();

  private final HashMap<Element, double[]> icl = new LinkedHashMap<>();
  private final HashMap<Element, double[]> fwhm = new LinkedHashMap<>();
  private final HashMap<Element, double[]> diffCoeff = new LinkedHashMap<>();
  private final HashMap<Element, double[]> peakTime = new LinkedHashMap<>();

  private final HashMap<Isotope, double[]> randNetSignal = new LinkedHashMap<>();

  private final List<Double> velocity = new ArrayList<>();
  private final List<Double> yPos = new ArrayList<>();

  private final HashMap<Isotope, HashMap<EventParameter, double[]>> dataMap = new LinkedHashMap<>();

  private boolean hasSimData = false;

  public void merge(ExportSimulationEventContainer container) {
    labels.addAll(container.getLabels());
    nEvents.addAll(container.getnEvents());
    mergeIntoBase(icl, container.getIcl());
    mergeIntoBase(fwhm, container.getFwhm());
    mergeIntoBase(diffCoeff, container.getDiffCoeff());
    mergeIntoBase(peakTime, container.getPeakTime());
    mergeIntoBase(randNetSignal, container.getRandNetSignal());

    velocity.addAll(container.getVelocity());
    yPos.addAll(container.getYPos());

    // merge isotope data
    for (Isotope isotope : container.getDataMap().keySet()) {
      if (this.dataMap.containsKey(isotope)) {
        mergeIntoBase(dataMap.get(isotope), container.getDataMap().get(isotope));
      } else {
        dataMap.put(isotope, container.getDataMap().get(isotope));
      }
    }
  }

//  public void sort() {
//    sortElement(icl);
//    sortElement(fwhm);
//    sortElement(diffCoeff);
//    sortElement(peakTime);
//    sortIsotope(randNetSignal);
//  }

  public List<Element> listElements() {
    return new ArrayList<>(icl.keySet());
  }

  public List<String> getLabels() {
    return labels;
  }

  public String getLabel() {
    return labels.stream().distinct().collect(Collectors.joining(" --- "));
  }

  public HashMap<Element, double[]> getIcl() {
    return icl;
  }

  public HashMap<Element, double[]> getFwhm() {
    return fwhm;
  }

  public HashMap<Element, double[]> getDiffCoeff() {
    return diffCoeff;
  }

  public HashMap<Isotope, double[]> getRandNetSignal() {
    return randNetSignal;
  }

  public List<Integer> getnEvents() {
    return nEvents;
  }

  public int getEventCount() {
    return nEvents.stream().mapToInt(Integer::intValue).sum();
  }

  public HashMap<Element, double[]> getPeakTime() {
    return peakTime;
  }

  public List<Double> getVelocity() {
    return velocity;
  }

  public List<Double> getYPos() {
    return yPos;
  }

  public HashMap<Isotope, HashMap<EventParameter, double[]>> getDataMap() {
    return dataMap;
  }

  public boolean isSimData() {
    return hasSimData;
  }

  public void setHasSimData(boolean hasSimData) {
    this.hasSimData = hasSimData;
  }

  public static <T> void mergeIntoBase(Map<T, double[]> base, Map<T, double[]> other) {

    for (Map.Entry<T, double[]> entry : other.entrySet()) {
      T key = entry.getKey();
      double[] otherData = entry.getValue();

      if (base.containsKey(key)) {
        double[] baseData = base.get(key);
        base.put(key, ArrUtils.merge(baseData, otherData));
      } else {
        base.put(key, otherData);
      }
    }
  }

//  public static void sortElement(Map<Element, double[]> map) {
//    // 1. Extract the keys into a list
//    List<Element> keys = new ArrayList<>(map.keySet());
//
//    // 2. Sort the keys (natural order)
//    Collections.sort(keys, Comparator.comparingInt(Element::getAtomicNumber));
//
//    // 3. Build a new LinkedHashMap following the sorted key order
//    LinkedHashMap<Element, double[]> sortedMap = new LinkedHashMap<>();
//    for (Element key : keys) {
//      sortedMap.put(key, map.get(key));
//    }
//
//    // 4. Clear the original map
//    map.clear();
//
//    // 5. Reinsert all entries from the sorted map
//    map.putAll(sortedMap);
//  }
//
//  public static void sortIsotope(Map<Isotope, double[]> map) {
//    // 1. Extract the keys into a list
//    List<Isotope> keys = new ArrayList<>(map.keySet());
//
//    // 2. Sort the keys (natural order)
//    Collections.sort(keys, Comparator.comparingInt(Isotope::getIsotopicNumber));
//
//    // 3. Build a new LinkedHashMap following the sorted key order
//    LinkedHashMap<Isotope, double[]> sortedMap = new LinkedHashMap<>();
//    for (Isotope key : keys) {
//      sortedMap.put(key, map.get(key));
//    }
//
//    // 4. Clear the original map
//    map.clear();
//
//    // 5. Reinsert all entries from the sorted map
//    map.putAll(sortedMap);
//  }
//

}
