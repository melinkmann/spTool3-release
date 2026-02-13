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

package sandbox.montecarlo;

import core.SpTool3Main;
import java.nio.DoubleBuffer;
import util.storage.MemoryMapStorage;
import util.storage.StorageUtils;


public class ContinuousIndexBuffer {

  private final int startInclusive;
  private final int endInclusive;
  private final DoubleBuffer values;

  public ContinuousIndexBuffer(int startInclusive, int endInclusive, double[] values) {
    this.startInclusive = startInclusive;
    this.endInclusive = endInclusive;

    MemoryMapStorage storage = SpTool3Main.getRunTime().getIndexBufferStorage();
    this.values = StorageUtils.storeToDoubleBuffer(storage, values);
  }

  public ContinuousIndexBuffer(int[] indices, double[] values) {
    this(indices[0], indices[indices.length - 1], values);
  }

  public int[] getIndices() {
    int length = endInclusive - startInclusive + 1;
    int[] indices;

    if (startInclusive == endInclusive) {
      indices = new int[]{startInclusive};
    } else {
      indices = new int[length];
      for (int index = startInclusive; index <= endInclusive; index++) {
        indices[index - startInclusive] = index;
      }
    }
    return indices;
  }

  public double[] getValues() {
    return StorageUtils.getArray(values);
  }

  public int length() {
    int length = endInclusive - startInclusive + 1;
    return length;
  }

}
