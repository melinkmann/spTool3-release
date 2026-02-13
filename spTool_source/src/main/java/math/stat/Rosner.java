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

import analysis.ResettableStatDataSet;
import com.google.common.math.Stats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.ArrUtils;

public abstract class Rosner {

  /*
  TODO. Double check all of this - unsure if it was all copied correctly form version 2...
  Especially: call to getCurrentData().. which changes occur in tool2 in the background?!
   */

  private static final Logger LOGGER = LogManager.getLogger(Rosner.class);


  public static void find(
      double[] data,
      ResettableStatDataSet statData,
      double alpha,
      double maxOutlierPercent) {

    iterate(data, statData, maxOutlierPercent, alpha);

  }

  public static void iterate(
      double[] data,
      ResettableStatDataSet distribution,
      double maxOutlierPercent,
      double alpha) {

    // -3 as a safety margin for the degree of freedom and such math.
    int maxOutliers = Math.min((int) Math.ceil(maxOutlierPercent * data.length), data.length - 3);
    List<Double> testValues = new ArrayList<>();
    List<Double> critValues = new ArrayList<>();

    double critVal = 0;

    // The actual math:
    if (data.length > 5 && maxOutliers < data.length) {

      // Wrap the collection in an array list for sorting OUTSIDE the loop.
      List<Double> values = ArrUtils.arrToList(data);

      // Iterate over data
      for (int i = 0; i < maxOutliers; i++) {

        /*
         First sort the data. This speeds the entire process up by a lot.
         We only ever remove one data point so the order is maintained.
         */
        Collections.sort(values); // natural order

        // Calc stat parameters
        double mean = Stats.meanOf(values);
        // Note: previous attempt in spTool2 that aimed at using root(µ) instead of SD(data) did not work well.
        double sd = Stats.of(values).sampleStandardDeviation();

        /*
         Find observation with largest distance to the mean (both directions + & -)
         Note: This is either the lowest or the highest point. Since the list is sorted,
         it is either the first or the last entry.
         */
        double minVal = values.get(0);
        double maxVal = values.get(values.size() - 1);
        // Find out which one is bigger, calculate the test value and remove this from the data set.
        double testValue;
        double lowerDist = Math.abs(minVal - mean);
        double upperDist = Math.abs(maxVal - mean);
        if (lowerDist > upperDist) {
          testValue = lowerDist / sd;
          values.remove(0); // remove lower value
        } else {
          testValue = upperDist / sd;
          values.remove(values.size() - 1); // remove upper value
        }

        // Degrees of freedom must be positive & greater than zero!
        int df = GESD.dfGESD(values.size(), i);
        if (df < 1) {
          // Probably severely underestimated/overestimated the number of outliers.
          LOGGER.warn("Rosner GESD test ran out of degrees of freedom and stopped at"
              + " N=" + values.size()
              + " i=" + i
              + ".");
          break;
        }

        testValues.add(testValue);
        critVal = GESD.critGESD(values.size(), alpha, i);
        critValues.add(critVal);

        // END OF ITERATION
      }

      /*
       Now we know all critical value and test values in the range of max iterations.
       The task now is to find the "last" test value that is higher than the critical value.
       From https://www.itl.nist.gov/div898/handbook/eda/section3/eda35h3.htm:
       H0:  there are no outliers in the data
       Ha:  there are up to 10 outliers in the data
       Critical region:  Reject H0 if test value Ri > critical value
       --> "For the generalized ESD test above, there are essentially
       10 separate tests being performed.
       For this example, the largest number of outliers for which the test statistic
       is greater than the critical value (at the 5 % level) is three.
       We therefore conclude that there are three outliers in this data set.
       */

      int outlierCount = 0;
      for (int i = testValues.size() - 1; i >= 0; i--) {
        if (testValues.get(i) > critValues.get(i)) {
          // Index of each entry is the number of iterations from the calculations before,
          // i.e. the number of outliers that were removed.
          outlierCount = i;
          break;
        }
      }

      // Now "repeat" the process form before to identify said outlierCount outliers.
      values = ArrUtils.arrToList(data);
      /*
        First sort the data. This speeds the entire process up by a lot.
        We only ever remove one data point so the order is maintained.
        */
      Collections.sort(values); // natural order

      for (int i = 0; i <= outlierCount; i++) {
        // Calc stat parameters
        double mean = Stats.meanOf(values);
      /*
        Find observation with largest distance to the mean (both directions + & -)
        Note: This is either the lowest or the highest point. Since the list is sorted,
        it is either the first or the last entry.
        */
        double minVal = values.get(0);
        double maxVal = values.get(values.size() - 1);
        // Find out which one is bigger, calculate the test value and remove this from the data set.
        double lowerDist = Math.abs(minVal - mean);
        double upperDist = Math.abs(maxVal - mean);
        if (lowerDist > upperDist) {
          // remove here for the remove via min/max/mean trick
          values.remove(0); // remove lower value
        } else {
          // remove here for the remove via min/max/mean trick
          values.remove(values.size() - 1); // remove upper value
        }
      }

      // update distr
      distribution.updateMuSD(values);
    }
  }


}
