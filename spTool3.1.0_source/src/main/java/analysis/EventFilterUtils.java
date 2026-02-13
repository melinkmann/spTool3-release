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

import java.util.ArrayList;
import java.util.List;
import math.stat.MovingAverage;

public abstract class EventFilterUtils {

  public static int checkForMultiParticleEvent(double[] tiIntensity, Event event, double zIntensity,
      double zSequence, int moavPeriod) {

    // only check if 3 or more DT (if not, there cannot be a valley)
    int numberOfEvents = 1;

    // only check if 3 or more DT (if not, there cannot be a double peak)
    if (event.getNoOfPoints() > 3) {
      // get data
      double bgPerDT = event.getBgPerNP() / event.getNoOfPoints();
      int[] indices = event.getIndices();
      double[] values = new double[indices.length];
      for (int i = 0; i < indices.length; i++) {
        // clamp to valid indices
        int idx = Math.max(0, indices[i]);
        idx = Math.min(idx, tiIntensity.length - 1);
        values[i] = tiIntensity[idx];
      }

      // prepare analysis
      double[] differences = new double[values.length - 1];
      double[] thr = new double[values.length - 1];

      // Use the ABSOLUTE signal here, not the net signal (Poisson depends on ABSOLUTE and GROSS!!)
      for (int i = 0; i < values.length - 1; i++) {
        differences[i] = values[i + 1] - values[i];
        thr[i] = zIntensity * Math.sqrt(values[i + 1]);
      }

      List<Double> filteredDifferences = new ArrayList<>();

      for (int i = 0; i < differences.length; i++) {
        if (Math.abs(differences[i]) > thr[i]) {
          filteredDifferences.add(differences[i]);
        }
      }

      // Average moavPeriod = 3 (according to first pub version)
      MovingAverage moav = new MovingAverage(moavPeriod);
      for (int i = 0; i < filteredDifferences.size(); i++) {
        moav.add(filteredDifferences.get(i));
        filteredDifferences.set(i, moav.getMean());
      }

      // Identify sign
      List<String> sign = new ArrayList<>();

      for (Double d : filteredDifferences) {
        if (d < 0) {
          sign.add("-");
        } else {
          sign.add("+");
        }
      }

      // Assume that peak has to rise first and thus has positive sign (+)
      int changes = 0;
      String currentSign = "+";
      double segmentSum = 0;
      int segmentLength = 0;

      for (int i = 0; i < sign.size(); i++) {

        // Make sure that negative bits are not just one DP small dips
        boolean negSegLargeEnough = true; // assume OK
        // segmentSum must be negative (NOT zero, i.e., at least one DP has been added)
        if (sign.get(i).equals("-") && segmentSum < 0) {
          negSegLargeEnough = Math.abs(segmentSum) > zSequence * Math.sqrt(segmentLength * bgPerDT);
          //if (!negSegLargeEnough) {
          // System.out.println("Negative segment was rejected by filter." );
          // System.out.println(i+ "\t\t segmentSum= "+segmentSum +"\t\t THR= "+zSequence * Math.sqrt(segmentLength * bgPerDT));
          //}
        }

        // This is the conventional check plus an additional "negSegLargeEnough"
        if (negSegLargeEnough && sign.get(i).equals(currentSign)) {
          segmentSum += filteredDifferences.get(i);
          segmentLength++;

          /*
          Note:
          This else condition only triggers when we go from + -> -
          When the event ends in -, it never triggers.
          This means, that we never count the final negative flank at all!
           */
        } else {

          /*
          Note that the segmentSum is negative for falling flanks.
          This means, that here within the else statement, we never actually count the negative flank.
          Implicitly, the negative flank is registered, since without it,
          we would not have had the change of sign that allowed us to identify another rising flank.
          All in all, since peaks RISE way more sharply than they fall, focusing on the rising edge
          likely makes sense as the signal-to-noise ratio is to out advantage.
          Overall, this way, we can quite elegantly only count rising edges.
          This also means, that we do not require any elaborate formula to calculate how many
          changes would be caused by n peaks in case of counting both falling and rising edges.
          I think, this is quite a good idea.

          Also note: the change of sign criterion does not include the segment filter.
          This means that even a smaller dip will cause a change of sign that is counted as such.
          However, the resulting sections of positive rising edge will then be checked.
          I tested with the "exact" criterion (abs(segmentSum) AND also checking the final
          sign where i==signs.size()-1 and it did not work well at all.

          The segment criterion was just an idea to use the background as a base:
          We take the mean sum signal of the sequence and calculate a Poisson threshold based on
          the sequence to say "at a total background of µ_sum = n·µ_B" the differences within this
          segment summed up must be at least z·µ_sum.
           */
          if (segmentSum > zSequence * Math.sqrt(segmentLength * bgPerDT)) {
           /*
              - previous condition was based on the peak height, but this is more sample dependent..:
              if (segmentSum > thrInput / 100 * netMaxInt) {
           */
            changes++;
          }
          // else {
            // System.out.println("Segment was removed by filter." );
            // System.out.println(i+ "\t\t segmentSum= "+segmentSum +"\t\t THR= "+zSequence * Math.sqrt(segmentLength * bgPerDT));
          // }

          // "Else: Start new segment"
          segmentSum = filteredDifferences.get(i);
          segmentLength = 1;
        }
        currentSign = sign.get(i);
      }

      // isNeighbour = changes > 1;
      // numberOfEvents = (int) Math.ceil((changes - 1) / 2d) + 1;
      numberOfEvents = changes;
    }

    return numberOfEvents;
  }

}
