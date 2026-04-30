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

import core.SpTool3Main;
import dataModelNew.*;
import dataModelNew.mz.Element;
import dataModelNew.mz.IsotopeMZ;
import sandbox.montecarlo.Isotope;
import util.Util;

import java.util.*;

public class SignalModificationUtil {
  public static void addIsotopeSum(Sample sample, boolean excludeIsobars, boolean selectedIsotopesOnly) {
    if (sample instanceof SampleImpl) {

      List<Element> elements = sample.listElements();

      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();

      // Check conflicts. Don't add these. (Yes, oxides matter, too, but isobaric is way more critical
      // usually)
      Set<Integer> isobaricConflicts = new HashSet<>();
      if (excludeIsobars) {
        List<Isotope> iso = new ArrayList<>();
        for (Element element : elements) {
          iso.addAll(element.getIsotopes());
        }
        HashMap<Integer, List<Element>> map = new LinkedHashMap<>();
        for (Isotope isotope : iso) {
          int nominalMass = isotope.getIsotopicNumber();
          Util.put(map, nominalMass, isotope.getElement());
        }
        map.keySet().removeIf(i -> map.get(i).size() < 2);
        //
        isobaricConflicts.addAll(map.keySet());
      }

      // Iterate to add traces

      // if not removed, still do not add isobars twice! (Signal in e.g. 48Ca and 48Ti is the same data!)
      Set<Integer> hasBeenAdded = new HashSet<>();
      for (Element element : elements) {
        List<Isotope> isotopes = element.getIsotopes();

        // Extract isotopes as traces
        List<Trace> traces = new ArrayList<>();
        for (Isotope isotope : isotopes) {

          if (!isobaricConflicts.contains(isotope.getIsotopicNumber())) {
            // either notSelOnly OR it is sel-only and it is selected
            if (!selectedIsotopesOnly
                || (selectedIsotopesOnly && selIsotopes.contains(isotope))) {
              Trace trace = sample.getTrace(isotope);
              if (trace != null) {
                traces.add(trace);
              }
            }
          }
        }

        if (!traces.isEmpty()) {
          double[] time = traces.get(0).getTISeries().getTime();

          double[] summedData = new double[time.length];
          for (Trace trace : traces) {
            Isotope isotope = trace.getMzValue().getIsotope();
            if (!hasBeenAdded.contains(isotope.getIsotopicNumber())) {
              double[] intensity = trace.getTISeries().getIntensity();
              for (int i = 0; i < intensity.length && i < summedData.length; i++) {
                summedData[i] += intensity[i];
              }
              hasBeenAdded.add(isotope.getIsotopicNumber());
            }
          }

          Isotope sumIso = new Isotope(element, 0, 0, 1);
          TISeries sumSeries = new TISeriesHDD(time, summedData);
          Trace sumTrace = new TraceImpl(sample, new IsotopeMZ(sumIso), sumSeries);
          sample.addTrace(sumTrace);
        }
      }
    }
  }
}
