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

import analysis.Baseline.ThrFormalism;
import analysis.Baseline.ThrMeasureOfSignificance;

import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.Statistics;

public class ThresholdSupplierInstructions implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(ThresholdSupplierInstructions.class);

  private final String description;
  private final ThrFormalism thrFormalism;
  private final ThrMeasureOfSignificance thrMeasureOfSignificance;
  private final double significance;
  private final double factor; // for gating where we multiply
  private final double offset;
  private final boolean isHeight; // ensure BG pop is based on intensity
  private final boolean isIntensity; // exclude points from intensity plot

  //
  private transient SoftReference<ThresholdSupplier> supplierCache;

  // Dummy instance
  public ThresholdSupplierInstructions() {
    this.description = "N/A";
    this.thrFormalism = ThrFormalism.STATIC_VALUE;
    this.thrMeasureOfSignificance = ThrMeasureOfSignificance.FACTOR;
    this.significance = 0;
    this.factor = 0;
    this.offset = 0;
    this.isHeight = true;
    this.isIntensity = true;
    this.supplierCache = new SoftReference<>(null);
  }

  public ThresholdSupplierInstructions(String description,
                                       ThrFormalism thrFormalism,
                                       ThrMeasureOfSignificance thrMeasureOfSignificance,
                                       double significance,
                                       double offset) {
    this.description = description;
    this.thrFormalism = thrFormalism;
    this.thrMeasureOfSignificance = thrMeasureOfSignificance;
    this.significance = significance;
    this.factor = 1;
    this.offset = offset;
    this.isHeight = true; // constructor for search context -> always height
    this.isIntensity = true; // constructor for search context -> always intensity
    this.supplierCache = new SoftReference<>(null);

  }

  public ThresholdSupplierInstructions(
      String description,
      ThrFormalism thrFormalism,
      ThrMeasureOfSignificance thrMeasureOfSignificance,
      double significance,
      double factor,
      boolean isHeight,
      boolean isIntensity) {
    this.description = description;
    this.thrFormalism = thrFormalism;
    this.thrMeasureOfSignificance = thrMeasureOfSignificance;
    this.significance = significance;
    this.factor = factor;
    this.offset = 0;
    this.isHeight = isHeight;
    this.isIntensity = isIntensity;
    this.supplierCache = new SoftReference<>(null);
  }

  // Deep copy
  public ThresholdSupplierInstructions(String description,
                                       ThrFormalism thrFormalism,
                                       ThrMeasureOfSignificance thrMeasureOfSignificance,
                                       double significance, double factor, double offset, boolean isHeight,
                                       boolean isIntensity) {
    this.description = description;
    this.thrFormalism = thrFormalism;
    this.thrMeasureOfSignificance = thrMeasureOfSignificance;
    this.significance = significance;
    this.factor = factor;
    this.offset = offset;
    this.isHeight = isHeight;
    this.isIntensity = isIntensity;
    this.supplierCache = new SoftReference<>(null);
  }

  public ThresholdSupplierInstructions copy() {
    return new ThresholdSupplierInstructions(description, thrFormalism, thrMeasureOfSignificance,
        significance, factor, offset, isHeight, isIntensity);
  }

  public ThresholdSupplier get(StatCollection bgDistribution) {

    ThresholdSupplier result = null;

    if (supplierCache != null) {
      ThresholdSupplier cache = supplierCache.get();
      if (cache != null) {
        if (cache instanceof StaticThresholdSupplier) {
          result = new StaticThresholdSupplier((StaticThresholdSupplier) cache);
        } else {
          result = new DataDependentThresholdSupplier((DataDependentThresholdSupplier) cache);
        }
      }
    }

    if (thrFormalism != null && result == null) {

      // Is it just a static threshold?
      if (thrFormalism.equals(ThrFormalism.STATIC_VALUE)) {
        result = new StaticThresholdSupplier(significance, offset);
        supplierCache = new SoftReference<>(result);
      } else if (thrMeasureOfSignificance != null) {
        // We have to deal with data dependant AND dynamic thresholding.

        // neutral as µ should be returned
        double z = 0;
        double alpha = 0.5;

        // just decide which we way of calculating we use
        if (thrMeasureOfSignificance.equals(ThrMeasureOfSignificance.ALPHA)) {
          alpha = significance;
          z = Statistics.alphaToZ(alpha);
        } else if (thrMeasureOfSignificance.equals(ThrMeasureOfSignificance.FACTOR)) {
          z = significance;
          alpha = Statistics.zToAlpha(z);
        }

        // faster access: load all into ram
        bgDistribution = new StatCollectionRAM(bgDistribution);

        double[] thresholdSlices = new double[bgDistribution.size()];
        if (thrFormalism.equals(ThrFormalism.AT_LOCATION)) {
          for (int i = 0; i < bgDistribution.size(); i++) {
            StatDataSet slice = bgDistribution.getSlice(i);
            // thresholdSlices[i] = factor * slice.getLocation();
            thresholdSlices[i] = slice.getLocation();
          }
        } else if (thrFormalism.equals(ThrFormalism.DETECTION_LIMIT_FORMALISM)) {
          for (int i = 0; i < bgDistribution.size(); i++) {
            StatDataSet slice = bgDistribution.getSlice(i);
            // update with current significance
            slice.setSignificance(z, alpha);
            // Here we need the factor!
            thresholdSlices[i] = factor * slice.calcDetectionLimit();
          }
        } else if (thrFormalism.equals(ThrFormalism.CRITICAL_LIMIT_FORMALISM)) {
          for (int i = 0; i < bgDistribution.size(); i++) {
            StatDataSet slice = bgDistribution.getSlice(i);
            // update with current significance
            slice.setSignificance(z, alpha);
            // thresholdSlices[i] = factor * slice.calcCriticalLimit();
            thresholdSlices[i] = slice.calcCriticalLimit();
          }
        }
        result = new DataDependentThresholdSupplier(bgDistribution, thresholdSlices, offset);
        supplierCache = new SoftReference<>(result);
      }
    }

    // Avoid null instantiation with this default case
    if (result == null) {
      result = new StaticThresholdSupplier(0, 0);
      LOGGER.warn("Instructions for threshold supplier were not consistent. "
          + "Returned default case [THR=0]");
    }
    return result;
  }



  public String getDescription() {
    return description;
  }

  public boolean isHeight() {
    return isHeight;
  }

  public boolean isIntensity() {
    return isIntensity;
  }

  public String translate() {
    StringBuilder tableString = new StringBuilder();

    if (!description.toLowerCase(Locale.ROOT).contains("search")
        && !description.toLowerCase(Locale.ROOT).contains("start")
        && !description.toLowerCase(Locale.ROOT).contains("stop")) {
      tableString.append(description)
          .append(" ");
      if (isHeight) {
        tableString.append("[cts] ");
      } else {
        tableString.append("[-] ");
      }
    }

    tableString.append("Model: ").append(thrFormalism.translate());
    if (!isHeight) {
      // more points than/fewer points than
      tableString.append("(val")
          .append("=")
          .append(significance)
          .append(")");
    } else if (thrFormalism.equals(ThrFormalism.STATIC_VALUE)) {
      // custom value
      tableString.append("(val")
          .append("=")
          .append(significance)
          .append(")");
    } else {
      tableString.append("(")
          .append(thrMeasureOfSignificance.translate())
          .append("=")
          .append(significance)
          .append(")");
    }

    if (thrFormalism.equals(ThrFormalism.DETECTION_LIMIT_FORMALISM) && factor > 0) {
      tableString.append("(·").append(factor).append(")");
    }
    if (offset > 0) {
      tableString.append("(+").append(offset).append(")");
    }

    return tableString.toString();
  }


  public boolean isEquivalent(ThresholdSupplierInstructions that) {
    boolean isEquivalent = thrFormalism.equals(that.thrFormalism);
    isEquivalent = isEquivalent && thrMeasureOfSignificance.equals(that.thrMeasureOfSignificance);
    isEquivalent = isEquivalent && significance == that.significance;
    isEquivalent = isEquivalent && factor == that.factor;
    isEquivalent = isEquivalent && isHeight == that.isHeight;
    return isEquivalent;
  }
}
