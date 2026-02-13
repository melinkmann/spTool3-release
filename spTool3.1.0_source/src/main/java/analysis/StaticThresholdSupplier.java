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

public class StaticThresholdSupplier implements ThresholdSupplier, Serializable {

  static final long serialVersionUID = 1L; //assign a long value

  private final double staticThresholdValue;

  public StaticThresholdSupplier(double staticThresholdValue, double offset) {
    this.staticThresholdValue = staticThresholdValue + offset;
  }

  // for deep copies
  public StaticThresholdSupplier(StaticThresholdSupplier supplier) {
    this.staticThresholdValue = supplier.getStaticThresholdValue();
  }

  @Override
  public double[] getThresholdSlices() {
    return new double[]{staticThresholdValue};
  }

  @Override
  public double getSliceValue(int index) {
    // Yield at least 0.1. There are issues with e.g. CompoundPoisson where 0 may be returned.
    // return Math.max(staticThresholdValue, LEAST_THR);
    return staticThresholdValue;
  }


  @Override
  public double interpolate(int i, int rawLength) {
    // Yield at least 0.1. There are issues with e.g. CompoundPoisson where 0 may be returned.
    // return Math.max(staticThresholdValue, LEAST_THR);
    return staticThresholdValue;
  }

  @Override
  public int getSliceCount() {
    return 1;
  }

  @Override
  public int[] getAnchorIndices(int rawLength) {
    return new int[]{0, rawLength - 1};
  }

  public double getStaticThresholdValue() {
    return staticThresholdValue;
  }


}
