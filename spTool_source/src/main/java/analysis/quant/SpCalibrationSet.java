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

package analysis.quant;

import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import math.units.enums.QuantityUnit;
import math.units.enums.SensitivityUnit;
import processing.options.CalibrationStrategy;
import sandbox.montecarlo.Isotope;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class SpCalibrationSet implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  /*
  Note: We must allow isotopes that are  not present in a sample.
  Why? Assume you need TE based on Au,
  you still want to apply that TE in context of a sample that does not have Au data.
  Hence, we cannot prefill the isotopes or bind them to sample.listIsotopes() or sth. like that.
   */

  private CalibrationStrategy calibrationStrategy;
  private final LinkedHashMap<Isotope, Quantity> ionicResponse;
  private final LinkedHashMap<Isotope, Quantity> npResponse;
  private final LinkedHashMap<Isotope, Quantity> aerosolTE;
  private final LinkedHashMap<Isotope, Quantity> particleNumberTE;
  private boolean isValid;

  public SpCalibrationSet() {
    this.calibrationStrategy = CalibrationStrategy.MASS; // simplest case, just ref NP
    this.ionicResponse = new LinkedHashMap<>();
    this.npResponse = new LinkedHashMap<>();
    this.aerosolTE = new LinkedHashMap<>();
    this.particleNumberTE = new LinkedHashMap<>();
    this.isValid = false;
  }

  // Copy: copy() on Quantity is done in the calling method "copy()" below
  public SpCalibrationSet(CalibrationStrategy calibrationStrategy,
                          LinkedHashMap<Isotope, Quantity> ionicResponse,
                          LinkedHashMap<Isotope, Quantity> npResponse,
                          LinkedHashMap<Isotope, Quantity> aerosolTE,
                          LinkedHashMap<Isotope, Quantity> particleNumberTE,
                          boolean isValid) {
    this.ionicResponse = ionicResponse;
    this.npResponse = npResponse;
    this.aerosolTE = aerosolTE;
    this.particleNumberTE = particleNumberTE;
    this.calibrationStrategy = calibrationStrategy;
    this.isValid = isValid;
  }

  public SpCalibrationSet copy() {
    LinkedHashMap<Isotope, Quantity> ionicResponse = new LinkedHashMap<>();
    LinkedHashMap<Isotope, Quantity> npResponse = new LinkedHashMap<>();
    LinkedHashMap<Isotope, Quantity> aerosolTE = new LinkedHashMap<>();
    LinkedHashMap<Isotope, Quantity> particleNumberTE = new LinkedHashMap<>();

    for (Isotope iso : this.ionicResponse.keySet()) {
      ionicResponse.put(iso, this.ionicResponse.get(iso).copy());
    }

    for (Isotope iso : this.npResponse.keySet()) {
      npResponse.put(iso, this.npResponse.get(iso).copy());
    }

    for (Isotope iso : this.aerosolTE.keySet()) {
      aerosolTE.put(iso, this.aerosolTE.get(iso).copy());
    }

    for (Isotope iso : this.particleNumberTE.keySet()) {
      particleNumberTE.put(iso, this.particleNumberTE.get(iso).copy());
    }

    return new SpCalibrationSet(
        calibrationStrategy,
        ionicResponse,
        npResponse,
        aerosolTE,
        particleNumberTE,
        isValid
    );
  }


  public CalibrationStrategy getCalibrationStrategy() {
    return calibrationStrategy;
  }

  public void setCalibrationStrategy(CalibrationStrategy calibrationStrategy) {
    this.calibrationStrategy = calibrationStrategy;
  }


  public void deriveAerosolTE() {
    SensitivityUnit targetunit = SensitivityUnit.CTS_PER_FEMTOGRAM;

    for (Isotope iso : ionicResponse.keySet()) {
      if (npResponse.containsKey(iso)) {
        double npCtsFg = npResponse.get(iso).getUnit()
            .convert(npResponse.get(iso).getValue(), targetunit);
        double ionicCtsFg = ionicResponse.get(iso).getUnit()
            .convert(ionicResponse.get(iso).getValue(), targetunit);

        if (npCtsFg > 0 && Double.isFinite(npCtsFg) && Double.isFinite(ionicCtsFg)) {
          double aerosolTEVal = 100 * ionicCtsFg / npCtsFg;

          aerosolTE.get(iso).change(aerosolTEVal);
          // Do not override PN TE: keep it at zero; later logic must default back to aerosol
          // particleNumberTE.get(iso).change(aerosolTEVal);
        }
      }
    }
  }

  public void populateWithIsotopes(List<Isotope> isotopes) {
    for (Isotope isotope : isotopes) {
      getOrCreateIonicResponse(isotope);
      getOrCreateNpResponse(isotope);
      getOrCreateAerosolTE(isotope);
      getOrCreateParticleNumberTE(isotope);
    }
  }

  public List<Isotope> listIsotopes() {
    HashSet<Isotope> uniqueIsotopes = new HashSet<>();
    uniqueIsotopes.addAll(ionicResponse.keySet());
    uniqueIsotopes.addAll(npResponse.keySet());
    uniqueIsotopes.addAll(aerosolTE.keySet());
    uniqueIsotopes.addAll(particleNumberTE.keySet());
    List<Isotope> sortedList = new ArrayList<>(uniqueIsotopes);
    // make sure the list exposed to the outside has defined order
    sortedList.sort(Comparator.comparingDouble(Isotope::getIsotopicNumber));
    return sortedList;
  }

  public Quantity getOrCreateIonicResponse(Isotope isotope) {
    if (ionicResponse.containsKey(isotope)) {
      return ionicResponse.get(isotope);
    } else {
      Quantity quant = new Quantity(0d, SensitivityUnit.CTS_PER_FEMTOGRAM);
      ionicResponse.put(isotope, quant);
      return quant;
    }
  }

  public void clearIonicResponse() {
    ionicResponse.clear();
  }

  public boolean hasIonicResponse(Isotope isotope) {
    return ionicResponse.containsKey(isotope);
  }

  public Quantity getOrCreateNpResponse(Isotope isotope) {
    if (npResponse.containsKey(isotope)) {
      return npResponse.get(isotope);
    } else {
      Quantity quant = new Quantity(0d, SensitivityUnit.CTS_PER_FEMTOGRAM);
      npResponse.put(isotope, quant);
      return quant;
    }
  }

  public void clearNpResponse() {
    npResponse.clear();
  }

  public boolean hasNpResponse(Isotope isotope) {
    return npResponse.containsKey(isotope);
  }

  public Quantity getOrCreateAerosolTE(Isotope isotope) {
    if (aerosolTE.containsKey(isotope)) {
      return aerosolTE.get(isotope);
    } else {
      Quantity quant = new Quantity(0d, QuantityUnit.PERCENT);
      aerosolTE.put(isotope, quant);
      return quant;
    }
  }

  public void clearAerosolTE() {
    aerosolTE.clear();
  }

  public boolean hasAerosolTE(Isotope isotope) {
    return aerosolTE.containsKey(isotope);
  }

  public Quantity getOrCreateParticleNumberTE(Isotope isotope) {
    if (particleNumberTE.containsKey(isotope)) {
      return particleNumberTE.get(isotope);
    } else {
      Quantity quant = new Quantity(0d, QuantityUnit.PERCENT);
      particleNumberTE.put(isotope, quant);
      return quant;
    }
  }

  public void clearParticleNumberTE() {
    particleNumberTE.clear();
  }

  public boolean hasParticleNumberTE(Isotope isotope) {
    return particleNumberTE.containsKey(isotope);
  }


  // Flag to state that there is actual calibration data
  public void setValid(boolean valid) {
    isValid = valid;
  }

  // Flag to state that there is actual calibration data
  public boolean isValid() {
    return isValid;
  }


}
