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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.DataList.DataPoint;

public class IndexBufferCollection {

  private static final Logger LOGGER = LogManager.getLogger(IndexBufferCollection.class);

  private final List<ContinuousIndexBuffer> buffers = new ArrayList<>();

  public void addData(int[] indices, double[] values) {
    buffers.add(new ContinuousIndexBuffer(indices, values));
  }

  public void transferToSignalArray(double[] signal) {
    for (ContinuousIndexBuffer buffer : buffers) {
      int[] indices = buffer.getIndices();
      double[] values = buffer.getValues();
      for (int i = 0; i < indices.length; i++) {
        int index = indices[i];
        if (index < signal.length) {
          signal[index] = signal[index] + values[i];
        }
      }
    }
  }

  // Sets NAN where there is particle data
  public double getEmpiricalBG(double[] signal, int blockSize, double thr) {
    double[] filteredValues = new double[signal.length];
    System.arraycopy(signal, 0, filteredValues, 0, filteredValues.length);

    for (ContinuousIndexBuffer buffer : buffers) {
      //LOGGER.trace("index: " +buffers.indexOf(buffer));
      int[] indices = buffer.getIndices();
      double[] values = buffer.getValues();
      for (int i = 0; i < indices.length; i++) {
        int index = indices[i] / blockSize;
        if (index < signal.length) {
          // due to peak overlap at the low flanks, anything would be flagged here...
          if (values[i] > thr) {
            filteredValues[index] = Double.NaN;
          }
        }
      }
    }

    double empiricalBG = 0;
    int counter = 0;
    for (int i = 0; i < filteredValues.length; i++) {
      if (!Double.isNaN(filteredValues[i])) {
        empiricalBG += filteredValues[i];
        counter++;
      }
    }

    return empiricalBG / counter;
  }

  public void addData(DataList<Integer> dataList) {
    List<DataPoint<Integer>> dataPoints = dataList.getData();

    int[] indices = new int[dataPoints.size()];
    double[] values = new double[dataPoints.size()];

    for (int i = 0; i < dataPoints.size(); i++) {
      indices[i] = dataPoints.get(i).getX();
      values[i] = dataPoints.get(i).getY();
    }
    buffers.add(new ContinuousIndexBuffer(indices, values));
  }

  public void addData(IndexBufferCollection bufferedData) {
    buffers.addAll(bufferedData.getBuffers());
  }

  /**
   * Warning: When there are many events (or whatever) stored here, there may be large overlap
   * between the indices. In that case, returning all indices (with a lot of equal entries) is quite
   * inefficient and may result in an array that is longer than the RAM handles.
   */
  public int[] getIndices() {
    // I guess, this is fastest...
    int length = 0;
    for (ContinuousIndexBuffer buffer : buffers) {
      length += buffer.length();
    }

    int[] indices = new int[length];

    int offset = 0;
    for (ContinuousIndexBuffer buffer : buffers) {
      int[] bufferIndices = buffer.getIndices();
      System.arraycopy(bufferIndices, 0, indices, offset, bufferIndices.length);
      offset += bufferIndices.length;
    }

    return indices;
  }

  /**
   * Warning: When there are many events (or whatever) stored here, there may be large overlap
   * between the indices. In that case, returning all indices (with a lot of equal entries) is quite
   * inefficient and may result in an array that is longer than the RAM handles.
   */
  public double[] getValues() {
    // I guess, this is fastest...
    int length = 0;
    for (ContinuousIndexBuffer buffer : buffers) {
      length += buffer.length();
    }

    double[] values = new double[length];

    int offset = 0;
    for (ContinuousIndexBuffer buffer : buffers) {
      double[] bufferIndices = buffer.getValues();
      System.arraycopy(bufferIndices, 0, values, offset, bufferIndices.length);
      offset += bufferIndices.length;
    }
    return values;
  }

  public List<ContinuousIndexBuffer> getBuffers() {
    return buffers;
  }

  // ##########################################################################################
  public static void test() {
    int[] testI1 = new int[]{5, 6, 7, 8};
    double[] testD1 = new double[]{1, 1, 1, 1};
    int[] testI2 = new int[]{12, 13, 14, 15, 16};
    double[] testD2 = new double[]{2, 2, 2, 2, 2};
    int[] testI3 = new int[]{15, 16, 17, 18, 19, 20};
    double[] testD3 = new double[]{3, 3, 3, 3, 3, 3};
    int[] testI4 = new int[]{22};
    double[] testD4 = new double[]{4};

    IndexBufferCollection bufferCollection = new IndexBufferCollection();
    bufferCollection.addData(testI1, testD1);
    bufferCollection.addData(testI2, testD2);
    bufferCollection.addData(testI3, testD3);
    bufferCollection.addData(testI4, testD4);

    for (int i = 0; i < bufferCollection.getIndices().length; i++) {
      System.out.println(bufferCollection.getIndices()[i]);
    }

    for (int i = 0; i < bufferCollection.getValues().length; i++) {
      System.out.println(bufferCollection.getValues()[i]);
    }
  }

}
