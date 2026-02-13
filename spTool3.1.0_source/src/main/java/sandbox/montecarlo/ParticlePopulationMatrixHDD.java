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
import dataModelNew.mz.Element;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import util.storage.MemoryMapStorage;
import util.storage.StorageUtils;
import visualizer.styles.Colors;
import visualizer.styles.MarkerStyle;

public class ParticlePopulationMatrixHDD implements ParticlePopulationMatrix, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  // All the data for a particle except for the integration of the peak.

  private final String label;

  private final Colors color;
  private final MarkerStyle marker;

  private final int numberOfEvents;

  private final DoubleBuffer plasmaVelocities;
  private final DoubleBuffer yPositions;

  private final HashMap<Element, DoubleBuffer> arrivalTimeMap = new LinkedHashMap<>();

  private final HashMap<Element, DoubleBuffer> plasmaDiffusionDMap = new LinkedHashMap<>();

  private final HashMap<Isotope, DoubleBuffer> intensityMap = new LinkedHashMap<>();

  //
  private transient SoftReference<ParticlePopulationMatrixRAM> ramCache;

  public ParticlePopulationMatrixHDD(ParticlePopulationMatrix matrix) {
    MemoryMapStorage storage = SpTool3Main.getRunTime().getSimulationBufferStorage();

    this.label = matrix.getLabel();
    this.color = matrix.getColor();
    this.marker = matrix.getMarker();

    this.numberOfEvents = matrix.getNumberOfEvents();

    this.plasmaVelocities = StorageUtils.storeToDoubleBuffer(storage, matrix.getPlasmaVelocities());
    this.yPositions = StorageUtils.storeToDoubleBuffer(storage, matrix.getYPositions());

    for (Element element : matrix.getArrivalTimeMap().keySet()) {
      double[] array = matrix.getArrivalTimeMap().get(element);
      this.arrivalTimeMap.put(element, StorageUtils.storeToDoubleBuffer(storage, array));
    }

    for (Element element : matrix.getPlasmaDiffusionDMap().keySet()) {
      double[] array = matrix.getPlasmaDiffusionDMap().get(element);
      this.plasmaDiffusionDMap.put(element, StorageUtils.storeToDoubleBuffer(storage, array));
    }

    for (Isotope isotope : matrix.getIntensityMap().keySet()) {
      double[] array = matrix.getIntensityMap().get(isotope);
      this.intensityMap.put(isotope, StorageUtils.storeToDoubleBuffer(storage, array));
    }

    // Initialize as null to keep RAM clean at the beginning
    ramCache = new SoftReference<>(null);
  }

  @Override
  public ParticlePopulationMatrix copy() {
    // Easiest approach: make a ram instance and transfer that to a hdd instance
    ParticlePopulationMatrix ram = new ParticlePopulationMatrixRAM(this);
    return ram.getNewHddInstance();
  }

  // Getter
  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public Colors getColor() {
    return color;
  }

  @Override
  public MarkerStyle getMarker() {
    return marker;
  }

  @Override
  public int getNumberOfEvents() {
    return numberOfEvents;
  }

  @Override
  public double[] getPlasmaVelocities() {
    return StorageUtils.getArray(plasmaVelocities);
  }

  @Override
  public double[] getYPositions() {
    return StorageUtils.getArray(yPositions);
  }

  @Override
  public HashMap<Element, double[]> getArrivalTimeMap() {
    final HashMap<Element, double[]> arrayMap = new LinkedHashMap<>();
    for (Element element : arrivalTimeMap.keySet()) {
      DoubleBuffer buffer = arrivalTimeMap.get(element);
      arrayMap.put(element, StorageUtils.getArray(buffer));
    }
    return arrayMap;
  }

  @Override
  public HashMap<Element, double[]> getPlasmaDiffusionDMap() {
    final HashMap<Element, double[]> arrayMap = new LinkedHashMap<>();
    for (Element element : plasmaDiffusionDMap.keySet()) {
      DoubleBuffer buffer = plasmaDiffusionDMap.get(element);
      arrayMap.put(element, StorageUtils.getArray(buffer));
    }
    return arrayMap;
  }

  @Override
  public HashMap<Isotope, double[]> getIntensityMap() {
    final HashMap<Isotope, double[]> arrayMap = new LinkedHashMap<>();
    for (Isotope isotope : intensityMap.keySet()) {
      DoubleBuffer buffer = intensityMap.get(isotope);
      arrayMap.put(isotope, StorageUtils.getArray(buffer));
    }
    return arrayMap;
  }

  @Override
  public List<Element> listElements() {
    return new ArrayList<>(arrivalTimeMap.keySet());
  }

  @Override
  public List<Isotope> listIsotopes() {
    return new ArrayList<>(intensityMap.keySet());
  }

  @Override
  public boolean hasIsotope(Isotope isotope) {
    return intensityMap.containsKey(isotope);
  }

  @Override
  public boolean hasElement(Element element) {
    return arrivalTimeMap.containsKey(element);
  }

  @Override
  public ParticlePopulationMatrixHDD getNewHddInstance() {
    return new ParticlePopulationMatrixHDD(this);
  }

  @Override
  public ParticlePopulationMatrixRAM getNewRamInstance() {
    ParticlePopulationMatrixRAM ramInstance;
    if (ramCache != null) {
      ParticlePopulationMatrixRAM cached = ramCache.get();
      if (cached != null) {
        ramInstance = cached.copy();
      } else {
        ramInstance = new ParticlePopulationMatrixRAM(this);
      }
    } else {
      ramInstance = new ParticlePopulationMatrixRAM(this);
    }
    ramCache = new SoftReference<>(ramInstance);
    return ramInstance;
  }
}
