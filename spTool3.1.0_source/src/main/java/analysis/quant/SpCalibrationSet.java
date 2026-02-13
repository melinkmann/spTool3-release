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

import math.units.enums.QuantityUnit;
import math.units.enums.SensitivityUnit;
import processing.options.CalibrationStrategy;
import sandbox.montecarlo.Isotope;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SpCalibrationSet implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private CalibrationStrategy calibrationStrategy;
  private final HashMap<Isotope, Quantity> ionicResponse;
  private final HashMap<Isotope, Quantity> npResponse;
  private final HashMap<Isotope, Quantity> aerosolTE;
  private final HashMap<Isotope, Quantity> particleNumberTE;
  private boolean isValid;

  public SpCalibrationSet() {
    this.calibrationStrategy = CalibrationStrategy.MASS; // simple case, just ref NP
    this.ionicResponse = new HashMap<>();
    this.npResponse = new HashMap<>();
    this.aerosolTE = new HashMap<>();
    this.particleNumberTE = new HashMap<>();
    this.isValid = false;

  }

  // Copy: copy() on Quantity is done in the calling method "copy()" below
  public SpCalibrationSet(CalibrationStrategy calibrationStrategy,
                          HashMap<Isotope, Quantity> ionicResponse,
                          HashMap<Isotope, Quantity> npResponse,
                          HashMap<Isotope, Quantity> nmIonicResponse,
                          HashMap<Isotope, Quantity> aerosolTE,
                          HashMap<Isotope, Quantity> particleNumberTE,
                          boolean isValid) {
    this.ionicResponse = ionicResponse;
    this.npResponse = npResponse;
    this.aerosolTE = aerosolTE;
    this.particleNumberTE = particleNumberTE;
    this.calibrationStrategy = calibrationStrategy;
    this.isValid = isValid;
  }

  public SpCalibrationSet copy() {
    HashMap<Isotope, Quantity> ionicResponse = new HashMap<>();
    HashMap<Isotope, Quantity> npResponse = new HashMap<>();
    HashMap<Isotope, Quantity> nmIonicResponse = new HashMap<>();
    HashMap<Isotope, Quantity> aerosolTE = new HashMap<>();
    HashMap<Isotope, Quantity> particleNumberTE = new HashMap<>();

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
        nmIonicResponse,
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
          // just override
          particleNumberTE.get(iso).change(aerosolTEVal);
        }
      }
    }
  }

  public List<Isotope> listIsotopes() {
    HashSet<Isotope> uniqueIsotopes = new HashSet<>();
    uniqueIsotopes.addAll(ionicResponse.keySet());
    uniqueIsotopes.addAll(npResponse.keySet());
    uniqueIsotopes.addAll(aerosolTE.keySet());
    uniqueIsotopes.addAll(particleNumberTE.keySet());
    return new ArrayList<>(uniqueIsotopes);
  }

  // Flag to state that there is actual calibration data
  public void setValid(boolean valid) {
    isValid = valid;
  }

  // Flag to state that there is actual calibration data
  public boolean isValid() {
    return isValid;
  }

  public Quantity getIonicResponse(Isotope isotope) {
    Quantity quant = new Quantity(0d, SensitivityUnit.CTS_PER_FEMTOGRAM);
    if (ionicResponse.containsKey(isotope)) {
      return ionicResponse.get(isotope);
    } else {
      ionicResponse.put(isotope, quant);
      return quant;
    }
  }

  public boolean hasIonicResponse(Isotope isotope) {
    return ionicResponse.containsKey(isotope);
  }

  public Quantity getNpResponse(Isotope isotope) {
    Quantity quant = new Quantity(0d, SensitivityUnit.CTS_PER_FEMTOGRAM);
    if (npResponse.containsKey(isotope)) {
      return npResponse.get(isotope);
    } else {
      npResponse.put(isotope, quant);
      return quant;
    }
  }

  public boolean hasNpResponse(Isotope isotope) {
    return npResponse.containsKey(isotope);
  }

  public Quantity getAerosolTE(Isotope isotope) {
    Quantity quant = new Quantity(0d, QuantityUnit.PERCENT);
    if (aerosolTE.containsKey(isotope)) {
      return aerosolTE.get(isotope);
    } else {
      aerosolTE.put(isotope, quant);
      return quant;
    }
  }

  public boolean hasAerosolTE(Isotope isotope) {
    return aerosolTE.containsKey(isotope);
  }

  public Quantity getParticleNumberTE(Isotope isotope) {
    Quantity quant = new Quantity(0d, QuantityUnit.PERCENT);
    if (particleNumberTE.containsKey(isotope)) {
      return particleNumberTE.get(isotope);
    } else {
      particleNumberTE.put(isotope, quant);
      return quant;
    }
  }

  public boolean hasParticleNumberTE(Isotope isotope) {
    return particleNumberTE.containsKey(isotope);
  }


}
