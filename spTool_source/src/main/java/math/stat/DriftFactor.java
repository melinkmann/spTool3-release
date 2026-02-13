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

package math.stat;

import analysis.NpPopulation;
import analysis.PopulationID;
import dataModelNew.Trace;
import java.util.List;
import math.AverageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.PopulationType;
import util.ArrUtils;

public class DriftFactor {


  private static final Logger LOGGER = LogManager.getLogger(DriftFactor.class);

  // Note: By averaging first with 300 ms, the points and frame length can stay like this.
  public static final double MILLISEC = 100; // 300
  public static final int TOTAL_POINTS = 9; //9
  public static final int FRAME_RSD = 1; // 1: seems to capture low-freq. oscillations better
  public static final int FRAME_TREND = 3; // original value

  public static double calculateDriftFactor(Trace trace, PopulationID populationID) {
    double drift = NpPopulation.DEFAULT_DRIFT;

    // Exclude these from calculations to make sure that the fail value is returned.
    if (!populationID.getType().equals(PopulationType.SIMULATION)
        && !populationID.getType().equals(PopulationType.EXTERNAL)) {
      double windowSec = MILLISEC / 1000d;


    /*
     Min/Max: use finer degree of averaging as and not the MOAV to capture high oscillation trends.
     Still: Min/Max should be smoothed to get rid of falsely picked event and such as much as possible.
     */

      double averageDistance = 1;
      double[] yBK = AverageUtils.averageDrift(trace, populationID, windowSec / 3).getY();
      if (yBK.length > 3) {
        double max = ArrUtils.getMax(yBK);
        double min = ArrUtils.getMin(yBK);
        double mean = MeasureOfLocation.MEAN.calc(yBK);
        averageDistance = 1 + (max - min) / (mean + 1);
      }

    /*
    RSD

    Use the RSD of the signal to estimate variability.
    The Drift factor is calculated by means of an average of 300 ms which is well above the DT.
    This means, that the pure Poisson-nature (VMR!) does not apply as much, and we should prefer RSD.
     Assuming RSD of the moav smoothed data: RSD Usually around 1-2 %
     --> calc times 100 to yield a value near 1 for the multiplication later.

     Some background: Assuming pure poisson,
     (1) sqrt(x)/x goes to zero at inf --> +1 at the beginning
     (2) Without (x+1), the function goes to infinity at x->0.
     Note: Using the non-smoothed RSD does not work so nicely.
    */

      double rsd = 1;
      List<Double> moav = MovingAverage.evaluate(yBK, TOTAL_POINTS, FRAME_RSD);
      if (moav.size() > 1) {

        double sd = MeasureOfSpread.SD.calc(moav);
        double mean = MeasureOfLocation.MEAN.calc(moav);
        rsd = 100 * sd / (mean + 1);
      }

     /*
     Trend

      1) This works best when moving average AND 300 ms averaging are combined.
      2) Why +1 and 0.25: First, +1 because 1 is neutral in the multiplication later. If we find
       0 events in sequence, then we have ideal conditions, i.e., 0+1 = "neutral case"

       Why -0.25: For a symmetric distribution e.g. Poisson>10, we have equal chances to be above
       or below the mean. Note that the averaging leads to quite high mean values per averaged DT.
       Normally, for 4 data points in row, one would have to calculate the permutations (2/4!),
       as of all possible 4! orderings, only 2 (ascending and descending) are relevant.
       Now, this would imply 8% of data points being in order. However, we do not analyze the raw
       data but Sliding Windows (MOAV) Instead of Random Subsets. Hence, empirically, the
       percentage seems more on the order of 25%.
       */

      double continuityCounter = 1;
      yBK = AverageUtils.averageDrift(trace, populationID, windowSec).getY();
      moav = MovingAverage.evaluate(yBK, TOTAL_POINTS, FRAME_TREND);
      if (moav.size() > 1) {
        // Check for continuity (drift)
        int counter = 0;
        for (int i = 3; i < moav.size(); i++) {
          double a = moav.get(i - 3);
          double b = moav.get(i - 2);
          double c = moav.get(i - 1);
          double d = moav.get(i);

          if (a < b && b < c && c < d) {
            counter++;
          }

          if (a > b && b > c && c > d) {
            counter++;
          }
        }
        continuityCounter = 1 + (double) counter / (double) moav.size() - 0.25;
        continuityCounter = Math.max(1, continuityCounter);
      }

      drift = averageDistance * rsd * continuityCounter;

      //    System.out.println(trace.getMzValue().getName());
      //    System.out.println("rsd:" + rsd);
      //    System.out.println("continuityCounter:" + continuityCounter);
      //    System.out.println("averageDistance:" + averageDistance);
      //    System.out.println("drift:" + drift + "\n");
    }
    return drift;
  }

  // "RMSD" does not contribute well to this consideration as point-to-point variability is
  // not really a subject of matter here.
  public static double calculateRMSD(double[] data) {
    double result = 1;
    if (data != null && data.length > 2) {

      double sumOfSquares = 0.0;
      for (int i = 0; i < data.length - 1; i++) {
        double diff = data[i + 1] - data[i];
        sumOfSquares += diff * diff;
      }
      result = Math.sqrt(sumOfSquares / (data.length - 1));

    }

    return result;
  }

  // "RMSD" does not contribute well to this consideration as point-to-point variability is
  // not really a subject of matter here.
  public static double calculateRMSD(List<Double> data) {
    double result = 1;
    if (data != null && data.size() > 2) {

      double sumOfSquares = 0.0;
      for (int i = 0; i < data.size() - 1; i++) {
        double diff = data.get(i + 1) - data.get(i);
        sumOfSquares += diff * diff;
      }
      result = Math.sqrt(sumOfSquares / (data.size() - 1));

    }

    return result;
  }

}
