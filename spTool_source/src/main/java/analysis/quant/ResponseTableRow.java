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

import sandbox.montecarlo.Isotope;

public class ResponseTableRow {

  private final Isotope isotope;

  private final SpCalibrationSet spCalibrationSet; // to refresh TE on manual edit

  private FxQuantity ionicResponse;
  private FxQuantity npResponse;
  private FxQuantity aerosolTEPct;
  private FxQuantity particleNumberTEPct;

  public ResponseTableRow(Isotope isotope, SpCalibrationSet spCalibrationSet) {
    this.isotope = isotope;
    this.spCalibrationSet = spCalibrationSet;
  }

  public Isotope getIsotope() {
    return isotope;
  }

  public FxQuantity getIonicResponse() {
    return ionicResponse;
  }

  public FxQuantity getNpResponse() {
    return npResponse;
  }

  public FxQuantity getAerosolTEPct() {
    return aerosolTEPct;
  }

  public FxQuantity getParticleNumberTEPct() {
    return particleNumberTEPct;
  }

  public void setIonicResponse(FxQuantity q) {
    this.ionicResponse = q;
  }

  public void setNpResponse(FxQuantity q) {
    this.npResponse = q;
  }

  public void setAerosolTEPct(FxQuantity q) {
    this.aerosolTEPct = q;
  }

  public void setParticleNumberTEPct(FxQuantity q) {
    this.particleNumberTEPct = q;
  }

  public SpCalibrationSet getSpCalibrationSet() {
    return spCalibrationSet;
  }
}