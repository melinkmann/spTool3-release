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

import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.List;

import sandbox.montecarlo.Statistics;
import util.ArrUtils;
import util.storage.MemoryMapStorage;
import util.storage.StorageUtils;

public class StatCollectionHDD implements StatCollection, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  // Storage class. RAM instance for Serialization.
  private final DoubleBuffer location;
  private final DoubleBuffer spread;
  private final DoubleBuffer outlierFactor;
  private final IntBuffer distrKey;
  private final int sliceLength;
  private final int lastSliceLength;
  private final double siaShape;
  private final double continuityEpsilon;
  //
  private transient SoftReference<double[]> locationCache;
  private transient SoftReference<double[]> spreadCache;
  private transient SoftReference<double[]> outlierFactorCache;
  private transient SoftReference<int[]> distrKeyCache;

  public StatCollectionHDD(StatCollection dist) {
    this.location = StorageUtils.storeToDoubleBuffer(getStorage(), dist.getLocation());
    this.spread = StorageUtils.storeToDoubleBuffer(getStorage(), dist.getSpread());
    this.outlierFactor = StorageUtils.storeToDoubleBuffer(getStorage(), dist.getOutlierFactor());
    this.distrKey = StorageUtils.storeToIntBuffer(getStorage(), dist.getDistrKey());
    this.sliceLength = dist.getSliceLength();
    this.lastSliceLength = dist.getLastSliceLength();
    this.siaShape = dist.getSiaShape();
    this.continuityEpsilon = dist.getContinuityEpsilon();
    //
    this.locationCache = new SoftReference<>(null);
    this.spreadCache = new SoftReference<>(null);
    this.outlierFactorCache = new SoftReference<>(null);
    this.distrKeyCache = new SoftReference<>(null);
  }


  public StatCollectionHDD(List<StatDataSet> list, int sliceLength, int lastSliceLength,
                           double siaShape, double continuityEpsilon) {
    double[] locationArr = new double[list.size()];
    double[] spreadArr = new double[list.size()];
    double[] outlierArr = new double[list.size()];
    int[] distrArr = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      StatDataSet set = list.get(i);
      locationArr[i] = set.getLocation();
      spreadArr[i] = set.getSpread();
      outlierArr[i] = set.getZ();
      distrArr[i] = set.getDistrID();
    }
    this.location = StorageUtils.storeToDoubleBuffer(getStorage(), locationArr);
    this.spread = StorageUtils.storeToDoubleBuffer(getStorage(), spreadArr);
    this.outlierFactor = StorageUtils.storeToDoubleBuffer(getStorage(), outlierArr);
    this.distrKey = StorageUtils.storeToIntBuffer(getStorage(), distrArr);
    this.sliceLength = sliceLength;
    this.lastSliceLength = lastSliceLength;
    this.siaShape = siaShape;
    this.continuityEpsilon = continuityEpsilon;
    //
    this.locationCache = new SoftReference<>(ArrUtils.copy(locationArr));
    this.spreadCache = new SoftReference<>(ArrUtils.copy(spreadArr));
    this.outlierFactorCache = new SoftReference<>(ArrUtils.copy(outlierArr));
    this.distrKeyCache = new SoftReference<>(ArrUtils.copy(distrArr));
  }

  // Deep copy
  public StatCollectionHDD(double[] location,
                           double[] spread,
                           double[] outlierFactor,
                           int[] distrKey,
                           int sliceLength, int lastSliceLength, double siaShape, double continuityEpsilon) {
    this.location = StorageUtils.storeToDoubleBuffer(getStorage(), location);
    this.spread = StorageUtils.storeToDoubleBuffer(getStorage(), spread);
    this.outlierFactor = StorageUtils.storeToDoubleBuffer(getStorage(), outlierFactor);
    this.distrKey = StorageUtils.storeToIntBuffer(getStorage(), distrKey);
    this.sliceLength = sliceLength;
    this.lastSliceLength = lastSliceLength;
    this.siaShape = siaShape;
    this.continuityEpsilon = continuityEpsilon;
    //
    this.locationCache = new SoftReference<>(null);
    this.spreadCache = new SoftReference<>(null);
    this.outlierFactorCache = new SoftReference<>(null);
    this.distrKeyCache = new SoftReference<>(null);
  }

  @Override
  public StatCollection copy() {
    return new StatCollectionHDD(getLocation(), getSpread(), getOutlierFactor(), getDistrKey(),
        sliceLength, lastSliceLength, siaShape, continuityEpsilon);
  }

  @Override
  public StatDataSet getSlice(int sliceIndex) {
    return new FixedStatDataSet(getLocation()[sliceIndex], getSpread()[sliceIndex], siaShape,
        continuityEpsilon,
        getDistrKey()[sliceIndex]);
  }

  @Override
  public int size() {
    return location.capacity();
  }

  @Override
  public int getSliceLength() {
    return sliceLength;
  }

  @Override
  public double getSiaShape() {
    return siaShape;
  }

  @Override
  public int getLastSliceLength() {
    return lastSliceLength;
  }

  public double[] getLocation() {
    double[] arr;
    if (locationCache != null) {
      double[] cache = locationCache.get();
      if (cache != null) {
        // return copy in case changes are done to the array
        arr = ArrUtils.copy(cache);
      } else {
        arr = StorageUtils.getArray(location);
        locationCache = new SoftReference<>(arr);
      }
    } else {
      arr = StorageUtils.getArray(location);
      locationCache = new SoftReference<>(arr);
    }
    return arr;
  }


  public double[] getSpread() {
    double[] arr;
    if (spreadCache != null) {
      double[] cache = spreadCache.get();
      if (cache != null) {
        // return copy in case changes are done to the array
        arr = ArrUtils.copy(cache);
      } else {
        arr = StorageUtils.getArray(spread);
        spreadCache = new SoftReference<>(arr);
      }
    } else {
      arr = StorageUtils.getArray(spread);
      spreadCache = new SoftReference<>(arr);
    }
    return arr;
  }


  public int[] getDistrKey() {
    int[] arr;
    if (distrKeyCache != null) {
      int[] cache = distrKeyCache.get();
      if (cache != null) {
        // return copy in case changes are done to the array
        arr = ArrUtils.copy(cache);
      } else {
        arr = StorageUtils.getArray(distrKey);
        distrKeyCache = new SoftReference<>(arr);
      }
    } else {
      arr = StorageUtils.getArray(distrKey);
      distrKeyCache = new SoftReference<>(arr);
    }
    return arr;
  }

  @Override
  public double getContinuityEpsilon() {
    return continuityEpsilon;
  }

  public double[] getOutlierFactor() {
    double[] arr;
    if (outlierFactorCache != null) {
      double[] cache = outlierFactorCache.get();
      if (cache != null) {
        // return copy in case changes are done to the array
        arr = ArrUtils.copy(cache);
      } else {
        arr = StorageUtils.getArray(outlierFactor);
        outlierFactorCache = new SoftReference<>(arr);
      }
    } else {
      arr = StorageUtils.getArray(outlierFactor);
      outlierFactorCache = new SoftReference<>(arr);
    }
    return arr;
  }

  /**
   * Interpolates the underlying statistics (µ, SD) to generate a new distribution.
   * This is needed for the interpolating p value computation.
   */
  @Override
  public StatDataSet interpolate(int dpIdx, int rawLength) {

    StatDataSet interpolatedDataSet;
    int noOfSlices = getLocation().length; // representative for all

    // Trivial case: just one slice -> always return the same set
    if (noOfSlices == 1) {
      interpolatedDataSet = new FixedStatDataSet(getLocation()[0], getSpread()[0], siaShape,
          continuityEpsilon,
          getDistrKey()[0]);
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
          interpolatedDataSet = new FixedStatDataSet(getLocation()[0], getSpread()[0], siaShape,
              continuityEpsilon,
              getDistrKey()[0]);
        } else {
          // get slice to the left
          int leftSliceIdx = sliceIdx - 1;
          int leftCenterIdx = (leftSliceIdx) * sliceLength + sliceLength / 2;
          // interpolate all parameters
          double intpLocation = Statistics.interpolate_1D(leftCenterIdx, centerIdx,
              getLocation()[leftSliceIdx],
              getLocation()[sliceIdx], dpIdx);
          double intpSpread = Statistics.interpolate_1D(leftCenterIdx, centerIdx, getSpread()[leftSliceIdx],
              getSpread()[sliceIdx], dpIdx);
          // use the distribution type from the slice we are in currently @sliceIdx
          interpolatedDataSet = new FixedStatDataSet(intpLocation, intpSpread, siaShape,continuityEpsilon,
              getDistrKey()[sliceIdx]);
        }
      } else if (dpIdx > centerIdx) {
        // are we in the last slice? --> Cannot interpolate
        if (sliceIdx == noOfSlices - 1) {
          interpolatedDataSet = new FixedStatDataSet(getLocation()[noOfSlices - 1],
              getSpread()[noOfSlices - 1],
              siaShape, continuityEpsilon,getDistrKey()[noOfSlices - 1]);
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
          double intpLocation = Statistics.interpolate_1D(rightCenterIdx, centerIdx, getLocation()[sliceIdx],
              getLocation()[rightSliceIdx], dpIdx);
          double intpSpread = Statistics.interpolate_1D(rightCenterIdx, centerIdx, getSpread()[sliceIdx],
              getSpread()[rightSliceIdx], dpIdx);
          // use the distribution type from the slice we are in currently @sliceIdx
          interpolatedDataSet = new FixedStatDataSet(intpLocation, intpSpread, siaShape,continuityEpsilon,
              getDistrKey()[sliceIdx]);
        }
      } else {
        // we hit the center!
        interpolatedDataSet = new FixedStatDataSet(getLocation()[sliceIdx], getSpread()[sliceIdx], siaShape,
            continuityEpsilon, getDistrKey()[sliceIdx]);
      }
    }

    return interpolatedDataSet;
  }


  private MemoryMapStorage getStorage() {
    return SpTool3Main.getRunTime().getBackgroundBufferStorage();
  }
}
