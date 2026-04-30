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

import core.RunTimeInstance;
import core.SpTool3Main;
import dataModelNew.mz.Element;
import javafx.util.Pair;
import math.stat.MeasureOfLocation;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sandbox.montecarlo.Isotope;
import smile.neighbor.lsh.Hash;
import util.ArrUtils;
import util.NF;
import util.SnF;
import util.storage.StorageUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpectralArray implements Serializable {
  static final long serialVersionUID = 8224173247737069932L; //assign a long value

  private static final Logger LOGGER = LogManager.getLogger(SpectralArray.class);

  private Isotope isotope;
  private final double mz;
  private double[] intensities; // only set to non-null when writing to object.
  private transient DoubleBuffer intensityBuffer; // stores values after deserialization
  private transient SoftReference<double[]> intensityCache; // quick-access reference

  private HashMap<String, double[]> additionalFeatures;
  private transient HashMap<String, DoubleBuffer> additionalFeatureBuffers;

  // Dummy
  public SpectralArray() {
    this.isotope = Element.UNKNOWN.getIsotopes().get(0);
    this.mz = 0;
    this.intensities = new double[0];
    this.intensityBuffer = null;
    this.intensityCache = new SoftReference<>(intensities);

    this.additionalFeatures = new HashMap<>();
    this.additionalFeatureBuffers = new HashMap<>();
  }


  public SpectralArray(@Nullable Isotope isotope, double mz, double[] intensities,
                       Map<String, double[]> extraFeatures) {
    this.isotope = isotope;
    this.mz = mz;
    this.intensities = null;
    this.intensityBuffer = StorageUtils.storeToDoubleBuffer(RunTimeInstance.getSpectraBufferStorage(),
        intensities);
    this.intensityCache = new SoftReference<>(intensities);

    this.additionalFeatureBuffers = new HashMap<>();
    this.additionalFeatures = new HashMap<>();
    if (extraFeatures != null) {
      for (String key : extraFeatures.keySet()) {
        double[] extraFeature = extraFeatures.get(key);
        additionalFeatureBuffers.put(key,
            StorageUtils.storeToDoubleBuffer(RunTimeInstance.getSpectraBufferStorage(), extraFeature));
      }
    }
  }

  public SpectralArray(double mz, double[] intensities,
                       Map<String, double[]> extraFeatures) {
    // we must hard-code this decision when creating the object or else we depend on the settings in config
    // dynamically
    this.isotope = SpTool3Main.getRunTime().getConfParams().resolveConflictOrGet((int) Math.round(mz));
    this.mz = mz;
    this.intensities = null;
    this.intensityBuffer = StorageUtils.storeToDoubleBuffer(RunTimeInstance.getSpectraBufferStorage(),
        intensities);
    this.intensityCache = new SoftReference<>(intensities);

    this.additionalFeatureBuffers = new HashMap<>();
    this.additionalFeatures = new HashMap<>();
    if (extraFeatures != null) {
      for (String key : extraFeatures.keySet()) {
        double[] extraFeature = extraFeatures.get(key);
        additionalFeatureBuffers.put(key,
            StorageUtils.storeToDoubleBuffer(RunTimeInstance.getSpectraBufferStorage(), extraFeature));
      }
    }
  }

  // for the copy constructor
  public SpectralArray(Isotope isotope, double mz, double[] intensities,
                       HashMap<String, double[]> additionalFeatures) {
    this.isotope = isotope;
    this.mz = mz;
    this.intensities = null;
    this.intensityBuffer = StorageUtils.storeToDoubleBuffer(RunTimeInstance.getSpectraBufferStorage(),
        intensities);
    this.intensityCache = new SoftReference<>(intensities);

    this.additionalFeatures = additionalFeatures;
    this.additionalFeatureBuffers = new HashMap<>();
    for (String key : additionalFeatures.keySet()) {
      double[] featureArray = additionalFeatures.get(key);
      additionalFeatureBuffers.put(key,
          StorageUtils.storeToDoubleBuffer(RunTimeInstance.getSpectraBufferStorage(), featureArray));
    }
    // free memory!
    this.additionalFeatures.clear();
  }


  public SpectralArray copy() {
    double[] intensityArr = getIntensity();
    HashMap<String, double[]> additionalFeaturesCopy = new HashMap<>();
    for (String key : additionalFeatureBuffers.keySet()) {
      DoubleBuffer buffer = additionalFeatureBuffers.get(key);
      double[] array = StorageUtils.getArray(buffer);
      additionalFeaturesCopy.put(key, array);
    }
    return new SpectralArray(isotope, mz, ArrUtils.copy(intensityArr), additionalFeaturesCopy);
  }

  public boolean isEmpty() {
    return getIntensity().length == 0;
  }

  public double getMean() {
    double mean = 0;
    double[] intensityArr = getIntensity();
    if (intensityArr != null && intensityArr.length > 0) {
      mean = MeasureOfLocation.MEAN.calc(intensityArr);
    }
    return mean;
  }

  public double[] getIntensity() {
    double[] intensityArr;
    if (intensities != null) {
      intensityArr = intensities;
    } else if (intensityCache != null && intensityCache.get() != null) {
      intensityArr = intensityCache.get();
    } else {
      intensityArr = StorageUtils.getArray(intensityBuffer);
      intensityCache = new SoftReference<>(intensityArr);
    }
    return intensityArr;
  }

  public List<String> listAdditionalFeatures() {
    return new ArrayList<>(additionalFeatureBuffers.keySet());
  }

  @Nullable
  public double[] getAdditionalFeature(String key) {
    double[] result = null;
    DoubleBuffer buffer = additionalFeatureBuffers.get(key);
    if (buffer != null) {
      result = StorageUtils.getArray(buffer);
    }
    return result;
  }

  public String getName() {
    if (isotope != null) {
      return isotope.getName();
    } else {
      return SnF.doubleToString(mz, NF.D1C3);
    }
  }

  public Isotope getIsotope() {
    if (isotope == null) {
      LOGGER.trace("Unexpected instance of null isotope. Had to guess isotope from mz using the current " +
          "configuration of spTool. This may cause unexpected matching of mz to isotopes. This indicates " +
          "that the project was loaded from an earlier version.");
      isotope = SpTool3Main.getRunTime().getConfParams().resolveConflictOrGet((int) Math.round(mz));
    }
    return isotope;
  }


  public double getMz() {
    return mz;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject(); // reads the original double[]
    this.intensityCache = new SoftReference<>(intensities); // caches the double[]
    this.intensityBuffer = StorageUtils.storeToDoubleBuffer(RunTimeInstance.getSpectraBufferStorage(),
        intensities); // the actual storage when in deserialized state
    this.intensities = null; // set the hard stored array to null to set free GC
    //
    if (additionalFeatures == null) {
      additionalFeatures = new HashMap<>();
    }

    if (additionalFeatureBuffers == null) {
      additionalFeatureBuffers = new HashMap<>();
    }

    // populate buffers from the deserialized double[] arrays, then clear the HashMap
    for (String key : additionalFeatures.keySet()) {
      additionalFeatureBuffers.put(key,
          StorageUtils.storeToDoubleBuffer(
              RunTimeInstance.getSpectraBufferStorage(), additionalFeatures.get(key)));
    }
    additionalFeatures.clear(); // keep RAM free
  }

  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {

    // make sure that the double[] is present before serialization!
    this.intensities = getIntensity();

    // populate additionalFeatures HashMap from buffers so it gets serialized
    for (String key : additionalFeatureBuffers.keySet()) {
      additionalFeatures.put(key, StorageUtils.getArray(additionalFeatureBuffers.get(key)));
    }

    // the transient fields are not written, i.e., no taking care of buffer and reference
    // Default serialization
    out.defaultWriteObject();

    // release the intensity double[] again, in case we just saved project and spTool keeps running
    this.intensities = null;
    this.additionalFeatures.clear();
  }

  // end
}