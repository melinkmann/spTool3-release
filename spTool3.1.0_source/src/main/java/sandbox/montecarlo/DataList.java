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
import java.util.stream.Collectors;

public class DataList<T extends Number> {

  private final List<DataPoint<T>> data = new ArrayList<>();

  public void add(T x, double y) {
    data.add(new DataPoint<>(x, y));
  }

  public List<DataPoint<T>> getData() {
    return data;
  }

  public List<T> getX() {
    return data.stream().map(DataPoint::getX).collect(Collectors.toList());
  }

  public List<Double> getY() {
    return data.stream().map(DataPoint::getY).collect(Collectors.toList());
  }


  public double ySum() {
    double sum = data.stream().mapToDouble(DataPoint::getY).sum();
    return sum;
  }

  public double yMax() {
    double max = data.stream().mapToDouble(DataPoint::getY).max().orElse(0);
    return max;
  }
  public void sort() {
    data.sort((o1, o2) -> {
      double d1 = o1.getX().doubleValue();
      double d2 = o2.getX().doubleValue();
      return Double.compare(d1, d2);
    });
  }

  public void normalizeOverriding(double normalizationFactor) {
    for (DataPoint<T> dataPoint : data) {
      dataPoint.setY(dataPoint.getY() * normalizationFactor);
    }
  }

  public DataList<T> copy() {
    DataList<T> newList = new DataList<>();
    for (DataPoint<T> dataPoint : data) {
      newList.add(dataPoint.getX(), dataPoint.getY());
    }
    return newList;
  }

  public static class DataPoint<T> {

    private final T x;
    private double y;

    public DataPoint(T x, double y) {
      this.x = x;
      this.y = y;
    }

    public T getX() {
      return x;
    }

    public Double getY() {
      return y;
    }

    public void setY(double y) {
      this.y = y;
    }
  }


}


