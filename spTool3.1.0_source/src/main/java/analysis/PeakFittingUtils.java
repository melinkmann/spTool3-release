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

/*
Idea:
average peaks
Gaussian smooth
Fit function

---

TODO:
 - Add tailing factor (a/b) to event properties in histo view
 - "Explore" mode: Window with 2 params: width, steps: varies D,v,y (µ and SD) with width and steps;
 - prompts how many samples this takes :D
 - allows comparison: calculates EMD (earth mover's displacement) and keep best matches.

 */

import io.export.ClipboardWriter;
import io.export.ExportWriter;
import io.fastExport.TabBlock;
import io.fastExport.TabBlockColl;
import io.fastExport.TabCol;
import math.Smoothing;
import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.util.Pair;
import sandbox.montecarlo.PeakFunction;
import util.ArrUtils;
import util.NF;
import util.SnF;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class PeakFittingUtils {
  public static void process(Population pop) {

    int maxPoints = pop.getEvents().getNpEvents().stream().map(event -> event.getProfile().size()).mapToInt(Integer::intValue).max().orElse(0);
    HashMap<Integer, double[]> avrgMap = new LinkedHashMap<>();

    TabBlock xyBlock = new TabBlock();

    for (Event event : pop.getEvents().getNpEvents()) {

      double[] y = event.getProfile().getY();
      //double[] y = Smoothing.gaussianSmooth(y, 5);
      ArrUtils.normalizeOverriding(y);

      if (!avrgMap.containsKey(event.getNoOfPoints())) {
        double[] avrg = new double[maxPoints];
        avrgMap.put(event.getNoOfPoints(), avrg);
      }

      double[] avrg = avrgMap.get(event.getNoOfPoints());

      for (int i = 0; i < y.length; i++) {
        avrg[i] = y[i];
      }
    }



    avrgMap.keySet().forEach(i-> xyBlock.addCol(new TabCol(i.toString(), SnF.doubleToStrArr(avrgMap.get(i), NF.D1C6))));

    ExportWriter writer = new ClipboardWriter();
    TabBlockColl coll = new TabBlockColl(writer, false);
    coll.add(xyBlock);
    coll.export();
  }


  /// ////////////////////////////////


}
