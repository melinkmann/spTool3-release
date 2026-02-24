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

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import sandbox.montecarlo.Statistics;
import util.ArrUtils;

public class StatCollectionRAM implements StatCollection, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  // Storage class. RAM instance for Serialization.
  private final double[] location;
  private final double[] spread;
  private final double[] outlierFactor;
  private final int[] distrKey;
  private final int sliceLength;
  private final int lastSliceLength; // may be 998 or 1002 @ length = 1000 ...
  private final double siaShape;
  private final double continuityEpsilon;

  // Dummy
  public StatCollectionRAM() {
    this.location = new double[0];
    this.spread = new double[0];
    this.outlierFactor = new double[0];
    this.distrKey = new int[0];
    this.sliceLength = 0;
    this.lastSliceLength = 0;
    this.siaShape = 0.47;
    this.continuityEpsilon = 0;
  }

  // Shallow copy, depending on instance of StatCollection (HDD passes new array, RAM passes pointer)
  public StatCollectionRAM(StatCollection collection) {
    this.location = collection.getLocation();
    this.spread = collection.getSpread();
    this.outlierFactor = collection.getOutlierFactor();
    this.distrKey = collection.getDistrKey();
    this.sliceLength = collection.getSliceLength();
    this.lastSliceLength = collection.getLastSliceLength();
    this.siaShape = collection.getSiaShape();
    this.continuityEpsilon = collection.getContinuityEpsilon();
  }

  // Deep copy
  public StatCollectionRAM(double[] location, double[] spread, double[] outlierFactor,
                           int[] distrKey,
                           int sliceLength, int lastSliceLength, double siaShape,
                           double continuityEpsilon) {
    this.location = ArrUtils.copy(location);
    this.spread = ArrUtils.copy(spread);
    this.outlierFactor = ArrUtils.copy(outlierFactor);
    this.distrKey = ArrUtils.copy(distrKey);
    this.sliceLength = sliceLength;
    this.lastSliceLength = lastSliceLength;
    this.siaShape = siaShape;
    this.continuityEpsilon = continuityEpsilon;
  }

  public StatCollectionRAM(StatDataSet set, int sliceLength, int lastSliceLength, double siaShape,
                           double continuityEpsilon) {
    this(Collections.singletonList(set), sliceLength, lastSliceLength, siaShape, continuityEpsilon);
  }

  public StatCollectionRAM(List<StatDataSet> list, int sliceLength, int lastSliceLength,
                           double siaShape, double continuityEpsilon) {
    this.location = new double[list.size()];
    this.spread = new double[list.size()];
    this.outlierFactor = new double[list.size()];
    this.distrKey = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      StatDataSet set = list.get(i);
      location[i] = set.getLocation();
      spread[i] = set.getSpread();
      outlierFactor[i] = set.getZ();
      distrKey[i] = set.getDistrID();
    }
    this.sliceLength = sliceLength;
    this.lastSliceLength = lastSliceLength;
    this.siaShape = siaShape;
    this.continuityEpsilon = continuityEpsilon;
  }

  @Override
  public StatCollection copy() {
    return new StatCollectionRAM(location, spread, outlierFactor,
        distrKey, sliceLength, lastSliceLength, siaShape, continuityEpsilon);
  }

  @Override
  public StatDataSet getSlice(int sliceIndex) {
    return new FixedStatDataSet(location[sliceIndex], spread[sliceIndex], siaShape, continuityEpsilon,
        distrKey[sliceIndex]);
  }

  @Override
  public int size() {
    return location.length;
  }

  @Override
  public int getSliceLength() {
    return sliceLength;
  }

  @Override
  public int getLastSliceLength() {
    return lastSliceLength;
  }

  @Override
  public double getSiaShape() {
    return siaShape;
  }

  public double[] getLocation() {
    return location;
  }

  public double[] getSpread() {
    return spread;
  }

  public double[] getOutlierFactor() {
    return outlierFactor;
  }

  public int[] getDistrKey() {
    return distrKey;
  }

  @Override
  public double getContinuityEpsilon() {
    return continuityEpsilon;
  }

  /**
   * Interpolates the underlying statistics (µ, SD) to generate a new distribution.
   * This is needed for the interpolating p value computation.
   */
  @Override
  public StatDataSet interpolate(int dpIdx, int rawLength) {

    StatDataSet interpolatedDataSet;
    int noOfSlices = location.length; // representative for all

    // Trivial case: just one slice -> always return the same set
    if (noOfSlices == 1) {
      interpolatedDataSet = new FixedStatDataSet(location[0], spread[0], siaShape, continuityEpsilon,
          distrKey[0]);
    } else {
      // How many regular slices?
      int numRegularSlices = (rawLength - lastSliceLength) / sliceLength;

      // Compute slice index
      int sliceIdx;
      int centerIdx;

      // We are within the region of regular slices
      if (dpIdx < numRegularSlices * sliceLength) {
        sliceIdx = dpIdx / sliceLength;
        centerIdx = sliceIdx * sliceLength + sliceLength / 2;
      } else {
        // We are in the final slice: slice to the left is regular --> calc with regular logic
        sliceIdx = noOfSlices - 1;
        centerIdx = rawLength - lastSliceLength / 2;
      }

      // Are we left or right or center?

      // to the left:
      if (dpIdx < centerIdx) {
        // are we in the first slice? --> Cannot interpolate
        if (sliceIdx == 0) {
          interpolatedDataSet = new FixedStatDataSet(location[0], spread[0], siaShape, continuityEpsilon,
              distrKey[0]);
        } else {
          // get slice to the left
          int leftSliceIdx = sliceIdx - 1;
          int leftCenterIdx = (leftSliceIdx) * sliceLength + sliceLength / 2;
          // interpolate all parameters
          double intpLocation = Statistics.interpolate_1D(leftCenterIdx, centerIdx, location[leftSliceIdx],
              location[sliceIdx], dpIdx);
          double intpSpread = Statistics.interpolate_1D(leftCenterIdx, centerIdx, spread[leftSliceIdx],
              spread[sliceIdx], dpIdx);
          // use the distribution type from the slice we are in currently @sliceIdx
          interpolatedDataSet = new FixedStatDataSet(intpLocation, intpSpread, siaShape, continuityEpsilon,
              distrKey[sliceIdx]);
        }
      } else if (dpIdx > centerIdx) {
        // are we in the last slice? --> Cannot interpolate
        if (sliceIdx == noOfSlices - 1) {
          interpolatedDataSet = new FixedStatDataSet(location[noOfSlices - 1], spread[noOfSlices - 1],
              siaShape, continuityEpsilon, distrKey[noOfSlices - 1]);
        } else {
          int rightSliceIdx = sliceIdx + 1;
          int rightCenterIdx;
          // It is possible that the slice to the right is irregular --> check

          // Right slice is final i.e. irregular slice!
          if (rightSliceIdx == noOfSlices - 1) {
            rightCenterIdx = rawLength - lastSliceLength / 2;
          } else {
            rightCenterIdx = (rightSliceIdx) * sliceLength + sliceLength / 2;
          }
          // interpolate all parameters
          double intpLocation = Statistics.interpolate_1D(rightCenterIdx, centerIdx, location[sliceIdx],
              location[rightSliceIdx], dpIdx);
          double intpSpread = Statistics.interpolate_1D(rightCenterIdx, centerIdx, spread[sliceIdx],
              spread[rightSliceIdx], dpIdx);
          // use the distribution type from the slice we are in currently @sliceIdx
          interpolatedDataSet = new FixedStatDataSet(intpLocation, intpSpread, siaShape, continuityEpsilon,
              distrKey[sliceIdx]);
        }
      } else {
        // we hit the center!
        interpolatedDataSet = new FixedStatDataSet(location[sliceIdx], spread[sliceIdx], siaShape,
            continuityEpsilon,
            distrKey[sliceIdx]);
      }
    }

    return interpolatedDataSet;
  }
}
