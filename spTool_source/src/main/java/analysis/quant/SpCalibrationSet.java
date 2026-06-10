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

import analysis.AnalysisUtils;
import dataModelNew.mz.Channel;
import dataModelNew.mz.CalChannel;
import math.units.enums.QuantityUnit;
import math.units.enums.SensitivityUnit;
import org.checkerframework.checker.units.qual.A;
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

  // TODO: Change this to channel or ElementChannel
  private CalibrationStrategy calibrationStrategy;
  private final LinkedHashMap<CalChannel, Quantity> ionicResponse;
  private final LinkedHashMap<CalChannel, Quantity> npResponse;
  private final LinkedHashMap<CalChannel, Quantity> aerosolTE;
  private final LinkedHashMap<CalChannel, Quantity> particleNumberTE;
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
                          LinkedHashMap<CalChannel, Quantity> ionicResponse,
                          LinkedHashMap<CalChannel, Quantity> npResponse,
                          LinkedHashMap<CalChannel, Quantity> aerosolTE,
                          LinkedHashMap<CalChannel, Quantity> particleNumberTE,
                          boolean isValid) {
    this.ionicResponse = ionicResponse;
    this.npResponse = npResponse;
    this.aerosolTE = aerosolTE;
    this.particleNumberTE = particleNumberTE;
    this.calibrationStrategy = calibrationStrategy;
    this.isValid = isValid;
  }

  public SpCalibrationSet copy() {
    LinkedHashMap<CalChannel, Quantity> ionicResponse = new LinkedHashMap<>();
    LinkedHashMap<CalChannel, Quantity> npResponse = new LinkedHashMap<>();
    LinkedHashMap<CalChannel, Quantity> aerosolTE = new LinkedHashMap<>();
    LinkedHashMap<CalChannel, Quantity> particleNumberTE = new LinkedHashMap<>();

    for (CalChannel iso : this.ionicResponse.keySet()) {
      ionicResponse.put(iso, this.ionicResponse.get(iso).copy());
    }

    for (CalChannel iso : this.npResponse.keySet()) {
      npResponse.put(iso, this.npResponse.get(iso).copy());
    }

    for (CalChannel iso : this.aerosolTE.keySet()) {
      aerosolTE.put(iso, this.aerosolTE.get(iso).copy());
    }

    for (CalChannel iso : this.particleNumberTE.keySet()) {
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

    for (CalChannel iso : ionicResponse.keySet()) {
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

  public void populateWithChannels(List<CalChannel> calChannels) {
    for (CalChannel calChannel : calChannels) {
      getOrCreateIonicResponse(calChannel);
      getOrCreateNpResponse(calChannel);
      getOrCreateAerosolTE(calChannel);
      getOrCreateParticleNumberTE(calChannel);
    }
  }

  public List<CalChannel> listChannels() {
    HashSet<CalChannel> uniqueIsotopes = new HashSet<>();
    uniqueIsotopes.addAll(ionicResponse.keySet());
    uniqueIsotopes.addAll(npResponse.keySet());
    uniqueIsotopes.addAll(aerosolTE.keySet());
    uniqueIsotopes.addAll(particleNumberTE.keySet());
    List<CalChannel> sortedList = new ArrayList<>(uniqueIsotopes);
    // make sure the list exposed to the outside has defined order
    sortedList.sort(Comparator.comparingDouble(CalChannel::getOrdinalNumber));
    return sortedList;
  }

  public Quantity getOrCreateIonicResponse(CalChannel channel) {
    if (ionicResponse.containsKey(channel)) {
      return ionicResponse.get(channel);
    } else {
      Quantity quant = new Quantity(0d, SensitivityUnit.CTS_PER_FEMTOGRAM);
      ionicResponse.put(channel, quant);
      return quant;
    }
  }

  public void clearIonicResponse() {
    ionicResponse.clear();
  }

  public boolean hasIonicResponse(CalChannel calChannel) {
    return ionicResponse.containsKey(calChannel);
  }

  public Quantity getOrCreateNpResponse(CalChannel channel) {
    if (npResponse.containsKey(channel)) {
      return npResponse.get(channel);
    } else {
      Quantity quant = new Quantity(0d, SensitivityUnit.CTS_PER_FEMTOGRAM);
      npResponse.put(channel, quant);
      return quant;
    }
  }

  public void clearNpResponse() {
    npResponse.clear();
  }

  public boolean hasNpResponse(CalChannel calChannel) {
    return npResponse.containsKey(calChannel);
  }

  public Quantity getOrCreateAerosolTE(CalChannel channel) {
    if (aerosolTE.containsKey(channel)) {
      return aerosolTE.get(channel);
    } else {
      Quantity quant = new Quantity(0d, QuantityUnit.PERCENT);
      aerosolTE.put(channel, quant);
      return quant;
    }
  }

  public void clearAerosolTE() {
    aerosolTE.clear();
  }

  public boolean hasAerosolTE(CalChannel calChannel) {
    return aerosolTE.containsKey(calChannel);
  }

  public Quantity getOrCreateParticleNumberTE(CalChannel channel) {
    if (particleNumberTE.containsKey(channel)) {
      return particleNumberTE.get(channel);
    } else {
      Quantity quant = new Quantity(0d, QuantityUnit.PERCENT);
      particleNumberTE.put(channel, quant);
      return quant;
    }
  }

  public void clearParticleNumberTE() {
    particleNumberTE.clear();
  }

  public boolean hasParticleNumberTE(CalChannel channel) {
    return particleNumberTE.containsKey(channel);
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
