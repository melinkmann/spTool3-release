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

import java.io.Serializable;

import sandbox.montecarlo.Statistics;
import util.ArrUtils;

public class DataDependentThresholdSupplier implements ThresholdSupplier, Serializable {

  static final long serialVersionUID = 1L; //assign a long value

  // For each slice (which may be several hundreds of data point long) we only have 1 value!
  private final double[] thresholdSlices;
  private final int sliceLength;
  private final int lastSliceLength; // may be 998 or 1002 @ length = 1000 ...

  public DataDependentThresholdSupplier(StatCollection backgroundDistribution,
      double[] thresholdSlices, double offset) {
    this.sliceLength = backgroundDistribution.getSliceLength();
    this.lastSliceLength = backgroundDistribution.getLastSliceLength();
    this.thresholdSlices = thresholdSlices;
    if (offset > 0 || offset < 0) {
      ArrUtils.addOverriding(thresholdSlices, offset);
    }
  }

  // For deep copies
  public DataDependentThresholdSupplier(DataDependentThresholdSupplier supplier) {
    this.thresholdSlices = supplier.thresholdSlices;
    this.sliceLength = supplier.sliceLength;
    this.lastSliceLength = supplier.lastSliceLength;
  }


  public double[] getThresholdSlices() {
    return thresholdSlices;
  }

  @Override
  public int getSliceCount() {
    return thresholdSlices.length;
  }

  @Override
  public double getSliceValue(int index) {
    // integer division
    int sliceIndex = index / sliceLength;
    sliceIndex = Math.max(0, sliceIndex);
    /*
     This makes sure that the final slice is within bounds.
     Assume case 1: there are 130 DP more than would fit in a sliceLength grid.
     e.g. sliceLength=1000, and finalIndex=1130.
     --> 1130/1000 --> 1 --> assigned to the second slice (index = 1, -> second slice)
     e.g. sliceLength=1000, and finalIndex=1050.
     --> 1050/1000 --> 1 BUT there is no second slice as 50<100 which is minimum slice size.
     Now, we catch this using: Math.min(sliceIndex, thresholdSlices.length - 1);
     */
    sliceIndex = Math.min(sliceIndex, thresholdSlices.length - 1);

    // Yield at least 0.1. There are issues with e.g. CompoundPoisson where 0 may be returned.
    // return Math.max(thresholdSlices[sliceIndex], LEAST_THR);
    return thresholdSlices[sliceIndex];
  }


  // TODO: Double check the interpolation.
  @Override
  public double interpolate(int dpIdx, int rawLength) {

    double thr;

    // Trivial case: just one slice
    if (thresholdSlices.length == 1) {
      thr = thresholdSlices[0];
    } else {
      // How many regular slices?
      int numRegularSlices = (rawLength - lastSliceLength) / sliceLength;

      // Compute slice index
      int sliceIndex;
      int centerIdx;

      // We are within the region of regular slices
      if (dpIdx < numRegularSlices * sliceLength) {
        sliceIndex = dpIdx / sliceLength;
        centerIdx = sliceIndex * sliceLength + sliceLength / 2;
      } else {
        // We are in the final slice: slice to the left is regular --> calc with regular logic
        sliceIndex = thresholdSlices.length - 1;
        centerIdx = rawLength - lastSliceLength / 2;
      }

      // Are we left or right or center?

      // to the left:
      if (dpIdx < centerIdx) {
        // are we in the first slice? --> Cannot interpolate
        if (sliceIndex == 0) {
          thr = thresholdSlices[0];
        } else {
          // get slice to the left
          int leftSliceIndex = sliceIndex - 1;
          int leftCenterIdx = (leftSliceIndex) * sliceLength + sliceLength / 2;
          thr = Statistics.interpolate_1D(leftCenterIdx, centerIdx,
              thresholdSlices[leftSliceIndex], thresholdSlices[sliceIndex], dpIdx);
//          thr = Statistics.interpolate(leftCenterIdx, centerIdx,
//              getMeanSliceValue(leftSliceIndex), getMeanSliceValue(sliceIndex), dpIdx);
        }
      } else if (dpIdx > centerIdx) {
        // are we in the last slice? --> Cannot interpolate
        if (sliceIndex == thresholdSlices.length - 1) {
          thr = thresholdSlices[thresholdSlices.length - 1];
        } else {
          int rightSliceIndex = sliceIndex + 1;
          int rightCenterIdx;
          // It is possible that the slice to the right is irregular --> check

          // Right slice is final i.e. irregular slice!
          if (rightSliceIndex == thresholdSlices.length - 1) {
            rightCenterIdx = rawLength - lastSliceLength / 2;
          } else {
            rightCenterIdx = (rightSliceIndex) * sliceLength + sliceLength / 2;
          }
          thr = Statistics.interpolate_1D(centerIdx, rightCenterIdx,
              thresholdSlices[sliceIndex], thresholdSlices[rightSliceIndex], dpIdx);
//          thr = Statistics.interpolate(centerIdx, rightCenterIdx,
//              getMeanSliceValue(sliceIndex), getMeanSliceValue(rightSliceIndex), dpIdx);
        }
      } else {
        // we hit the center!
        thr = thresholdSlices[sliceIndex];
      }
    }

    // Yield at least 0.1. There are issues with e.g. CompoundPoisson where 0 may be returned.
    // return Math.max(thr, LEAST_THR);
    return thr;
  }

  // For plotting
  @Override
  public int[] getAnchorIndices(int rawLength) {

    int[] indices;
    int noOfAnchors = 2 * thresholdSlices.length + 1;

    // Trivial case: just one slice, i.e., no space needed
    if (thresholdSlices.length == 1) {
      indices = new int[2];
      indices[indices.length - 1] = Math.max(rawLength - 1, 0); // if empty, don't return neg idx.
    } else {
      // Initialize array with start and end according to raw data length
      indices = new int[noOfAnchors];
      indices[0] = 0;
      indices[indices.length - 1] = rawLength - 1;

      int currentIndex = 0;
      for (int i = 0; i < noOfAnchors - 2; i++) {
        // Still a regular slice
        if (i < noOfAnchors - 3) {
          currentIndex += sliceLength / 2;
        } else {
          // final slice
          currentIndex += lastSliceLength / 2;
        }
        indices[i + 1] = currentIndex;
      }
    }

    return indices;
  }


}
