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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Util;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class LPCParticleMatrix extends IncompleteParticleMatrix implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(LPCParticleMatrix.class);

  // RAFA: Column indices in the single_particle_info CSV format:
  //   0  particle id
  //   1  largest feret diameters [µm]
  //   2  smallest feret diameters [µm]
  //   3  largest legendre diameters [µm]
  //   4  smallest legendre diameters [µm]
  //   5  circle-equivalent diameters [µm]
  //   6  circularity [1]
  //   7  convexity [1]
  //   8  solidity [1]
  //   9  particle diameter aspect ratios [1]
  //  10  detection time [sec.]
  //  11  frame id
  //  12  centroid position row [pix]
  //  13  centroid position column [pix]


  /*
  TOM:

  0   id
  1   frame
  2   frame_count
  3   area
  4   aspect
  5   circular_equivalent_diameter
  6   circularity
  7   convexity
  8   intensity
  9   maximum_width
  10  minimum_width
  11  perimeter
  12  radius
  13  sharpness
  14  x
  15  y
   */

  private HashMap<String, List<Double>> allData;


  public LPCParticleMatrix() {
    this.allData = new LinkedHashMap<>();
  }

  // Deep copy
  public LPCParticleMatrix(HashMap<String, List<Double>> allData) {
    this.allData = new LinkedHashMap<>();
    for (String key : allData.keySet()) {
      this.allData.put(key, new ArrayList<>(allData.get(key)));
    }
  }

  public LPCParticleMatrix copy() {
    return new LPCParticleMatrix(allData);
  }

  public LPCParticleMatrix roi(List<Integer> indices) {
    HashMap<String, List<Double>> allRoiData = new LinkedHashMap<>();


    for (String key : this.allData.keySet()) {
      List<Double> data = this.allData.get(key);
      List<Double> roiData = new ArrayList<>();
      for (Integer index : indices) {
        if (index < data.size()) {
          roiData.add(data.get(index));
        }
      }
      allRoiData.put(key, roiData);
    }
    return new LPCParticleMatrix(allRoiData);
  }


  @Override
  public void add(double peakTime, double area, double height, double duration, double point) {
    LOGGER.error("Unsupported operation!");
  }

  public void add(String key, double value) {
    Util.put(allData, key, value);
  }

  public List<Double> getPeakTimes() {
    List<Double> data = new ArrayList<>();
    if (allData.containsKey("frame")) {
      data = allData.get("frame");
    } else if (allData.containsKey("detection_time")) {
      data = allData.get("detection_time");
    }
    return data;
  }

  public List<Double> getAreas() {
    List<Double> data = new ArrayList<>();
    if (allData.containsKey("circle_equivalent_diameter")) {
      data = allData.get("circle_equivalent_diameter");
    } else if (allData.containsKey("circular_equivalent_diameter")) {
      data = allData.get("circular_equivalent_diameter");
    }
    return data;
  }

  public List<Double> getHeights() {
    List<Double> data = new ArrayList<>();
    if (allData.containsKey("TARGET")) {
      data = allData.get("TARGET");
    }
    return data;
  }

  public List<Double> getDurations() {
    return new ArrayList<>();
  }

  public List<Double> getPoints() {
    return new ArrayList<>();
  }

  public int size() {
    int sz = 0;
    if (!allData.isEmpty()) {
      List<String> keys = new ArrayList<>(allData.keySet());
      sz = allData.get(keys.get(0)).size();
    }
    return sz;
  }

  public HashMap<String, List<Double>> getAllData() {
    return allData;
  }
}
