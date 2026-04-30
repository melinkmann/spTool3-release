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

package dataModelNew;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IncompleteParticleMatrix implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final List<Double> peakTimes;
  private final List<Double> areas;
  private final List<Double> heights;
  private final List<Double> durations;
  private final List<Double> points;

  public IncompleteParticleMatrix(int size) {
    peakTimes = new ArrayList<>(size);
    areas = new ArrayList<>(size);
    heights = new ArrayList<>(size);
    durations = new ArrayList<>(size);
    points = new ArrayList<>(size);
  }

  public IncompleteParticleMatrix() {
    peakTimes = new ArrayList<>();
    areas = new ArrayList<>();
    heights = new ArrayList<>();
    durations = new ArrayList<>();
    points = new ArrayList<>();
  }

  // Deep copy
  public IncompleteParticleMatrix(List<Double> peakTimes, List<Double> areas,
                                  List<Double> heights, List<Double> durations, List<Double> points) {
    this.peakTimes = new ArrayList<>(peakTimes);
    this.areas = new ArrayList<>(areas);
    this.heights = new ArrayList<>(heights);
    this.durations = new ArrayList<>(durations);
    this.points = new ArrayList<>(points);
  }

  public IncompleteParticleMatrix copy() {
    return new IncompleteParticleMatrix(peakTimes, areas, heights, durations, points);
  }

  public IncompleteParticleMatrix roi(List<Integer> indices) {
    List<Double> roiPeakTimes = new ArrayList<>();
    List<Double> roiAreas = new ArrayList<>();
    List<Double> roiHeights = new ArrayList<>();
    List<Double> roiDurations = new ArrayList<>();
    List<Double> roiPoints = new ArrayList<>();
    for (Integer index : indices) {
      if (index < peakTimes.size()) {
        roiPeakTimes.add(peakTimes.get(index));
      }
      if (index < areas.size()) {
        roiAreas.add(areas.get(index));
      }
      if (index < heights.size()) {
        roiHeights.add(heights.get(index));
      }
      if (index < durations.size()) {
        roiDurations.add(durations.get(index));
      }
      if (index < points.size()) {
        roiPoints.add(points.get(index));
      }
    }
    return new IncompleteParticleMatrix(roiPeakTimes, roiAreas,
        roiHeights, roiDurations, roiPoints);
  }

  public void add(double peakTime, double area, double height, double duration, double point) {
    peakTimes.add(peakTime);
    areas.add(area);
    heights.add(height);
    durations.add(duration);
    points.add(point);
  }

  public List<Double> getPeakTimes() {
    return peakTimes;
  }

  public List<Double> getAreas() {
    return areas;
  }

  public List<Double> getHeights() {
    return heights;
  }

  public List<Double> getDurations() {
    return durations;
  }

  public List<Double> getPoints() {
    return points;
  }

  public int size() {
    return points.size();
  }
}
