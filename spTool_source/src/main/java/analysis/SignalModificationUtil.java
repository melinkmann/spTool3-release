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
import dataModelNew.mz.*;
import gui.util.UiUtil;
import io.nu.ShapeEstimator;
import sandbox.montecarlo.Isotope;
import util.Util;

import java.util.*;

public class SignalModificationUtil {
  public static void addIsotopeSum(Sample sample, boolean excludeIsobars, boolean selectedIsotopesOnly) {
    if (sample instanceof SampleImpl) {

      List<Element> elements = sample.listElements();

      List<Channel> allChannels = SpTool3Main.getRunTime().getMainWindowCtl().getAllChannels();
      List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

      HashMap<Isotope, Channel> allIsotopesMap = AnalysisUtils.getMatchMap(allChannels);
      HashMap<Isotope, Channel> selIsotopesMap = AnalysisUtils.getMatchMap(selChannels);

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

      for (Element element : elements) {

        // if not removed, still do not add isobars twice! (Signal in e.g. 48Ca and 48Ti is the same data!)
        // There are 2 aspects here: 1) chemically, Ti=Ca and 2) ToF reports same value for both 48 identities
        Set<Integer> hasBeenAdded = new HashSet<>();
        List<Isotope> isotopes = element.getIsotopes();

        // Extract isotopes as traces
        List<Trace> tracesOfElement = new ArrayList<>();
        for (Isotope isotope : isotopes) {

          if (!isobaricConflicts.contains(isotope.getIsotopicNumber())) {
            // either notSelOnly OR it is sel-only and it is selected
            if (!selectedIsotopesOnly || selIsotopesMap.containsKey(isotope)) {
              Trace trace = sample.getTrace(allIsotopesMap.get(isotope));
              if (trace != null) {
                tracesOfElement.add(trace);
              }
            }
          }
        }

        if (!tracesOfElement.isEmpty()) {
          double[] time = tracesOfElement.get(0).getTISeries().getTime();
          // Check if SIA shape has been computed before. If yes, do it again for the sum.
          boolean hasSIA = tracesOfElement.stream().anyMatch(t -> t.getSiaShape() > 0);

          List<Channel> contributingChannels = new ArrayList<>();
          double[] summedData = new double[time.length];
          for (Trace trace : tracesOfElement) {
            Channel ch = trace.getChannel();
            Isotope isotope = ch.getIsotope();
            if (isotope != null && !hasBeenAdded.contains(isotope.getIsotopicNumber())) {
              double[] intensity = trace.getTISeries().getIntensity();
              for (int i = 0; i < intensity.length && i < summedData.length; i++) {
                summedData[i] += intensity[i];
              }
              contributingChannels.add(ch);
              hasBeenAdded.add(isotope.getIsotopicNumber());
            }
          }

          TISeries sumSeries = new TISeriesHDD(time, summedData);
          Channel sumChannel = new ComputedChannel(element, "Σ" + element.getSymbol(), contributingChannels);
          Trace sumTrace;
          if (hasSIA) {
            // TODO: Consider reusing the blanker regions here (optional for better SIA estimation)
            double sia = ShapeEstimator.computeShape(sumSeries.getIntensity(), new ArrayList<>());
            sumTrace = new TraceImpl(sample, sumChannel, sumSeries, sia);
          } else {
            sumTrace = new TraceImpl(sample, sumChannel, sumSeries);
          }
          sample.addTrace(sumTrace);
        }
      }
    }
  }
}
