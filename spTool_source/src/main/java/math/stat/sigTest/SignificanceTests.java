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

package math.stat.sigTest;

import java.util.List;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hipparchus.stat.inference.MannWhitneyUTest;
import org.hipparchus.stat.inference.TTest;
import org.hipparchus.stat.inference.WilcoxonSignedRankTest;
import processing.options.SigTests;
import util.ArrUtils;

public abstract class SignificanceTests {

  private static final Logger LOGGER = LogManager.getLogger(SignificanceTests.class);

  public static double calculate(SigTests test, List<double[]> data) {
    double p = 1;
    if (data.size() > 1 && data.stream().allMatch(d -> d != null && d.length > 1)) {
      p = switch (test) {
        case KS -> ksTest(data);
        case MANN_WHITNEY -> mannWhitneyTest(data);
        case WILCOXON -> wilcoxonTest(data);
        case STUDENT_T -> tTest(data);
        case BRUNNER_MUNZEL -> brunnerMunzelTest(data);
        case ANOVA1 -> anova1(data);
        case KRUSKAL_WALLIS -> kruskalWallisTest(data);
        // case EMD -> emdPermutation(data);
      };
    }
    return p;
  }

  public static String checkAndWarn(SigTests test, List<double[]> data) {
    String s = "Not enough input data!\n\n";
    if (data.size() > 1) {
      s = "";
      if (test == SigTests.WILCOXON) {
        if (data.get(0).length != data.get(1).length) {
          s = "WARNING: Input data does not have equal length! Random resampling was applied!\n\n";
        }
      }
    }
    return s;
  }

  public static double ksTest(List<double[]> data) {
    double[] x = data.get(0);
    double[] y = data.get(1);
    KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
    return ks.kolmogorovSmirnovTest(x, y);
  }

  public static double mannWhitneyTest(List<double[]> data) {
    MannWhitneyUTest mwu = new MannWhitneyUTest(); // apache is not good...
    double[] x = data.get(0);
    double[] y = data.get(1);
    return mwu.mannWhitneyUTest(x, y);
  }

  /**
   * Requires equal length...
   */
  public static double wilcoxonTest(List<double[]> data) {
    double p = 1;
    WilcoxonSignedRankTest wcx = new WilcoxonSignedRankTest();
    double[] x = data.get(0);
    double[] y = data.get(1);
    if (x.length != y.length) {
      // The idea is not bad but the p value floats from 0.2 - 0.6 ... seems a bit unstable to me.
      if (x.length > y.length) {
        x = ArrUtils.sampleWithoutReplacement(x, y.length);
        LOGGER.info(
            "X data was longer than Y data. Applied random sampling without replacement to reduce X.");
      } else {
        y = ArrUtils.sampleWithoutReplacement(y, x.length);
        LOGGER.info(
            "Y data was longer than X data. Applied random sampling without replacement to reduce Y.");
      }
      p = wcx.wilcoxonSignedRankTest(x, y, false);
    } else {
      p = wcx.wilcoxonSignedRankTest(x, y, false);
    }
    return p;
  }

  public static double tTest(List<double[]> data) {
    TTest ttest = new TTest();
    double[] x = data.get(0);
    double[] y = data.get(1);
    return ttest.tTest(x, y);
  }

  public static double brunnerMunzelTest(List<double[]> data) {
    double[] x = data.get(0);
    double[] y = data.get(1);
    BrunnerMunzelGPT.Result bm = BrunnerMunzelGPT.brunnerMunzel(x, y);
    return bm.pValue;
  }

  public static double anova1(List<double[]> data) {
    OneWayAnova owa = new OneWayAnova();
    return owa.anovaPValue(data);
  }

  public static double kruskalWallisTest(List<double[]> data) {
    KruskalWallisGPT.Result kw = KruskalWallisGPT.kruskalWallis(data);
    return kw.pValue;
  }

  public static double emdPermutation(List<double[]> data) {
    double[] x = data.get(0);
    double[] y = data.get(1);
    return EMD.calcPermutationDifferences(x, y, 1000);
  }


}
