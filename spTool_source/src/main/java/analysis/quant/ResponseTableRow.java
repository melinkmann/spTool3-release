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

public final class ResponseTableRow {

  private final Isotope isotope;

  private FxQuantity ionicResponse;
  private FxQuantity npResponse;
  private FxQuantity aerosolTEPct;
  private FxQuantity particleNumberTEPct;

  public ResponseTableRow(Isotope isotope) {
    this.isotope = isotope;
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

  /* package */ void setIonicResponse(Quantity q) {
    this.ionicResponse = q == null ? null : new FxQuantity(q);
  }

  /* package */ void setNpResponse(Quantity q) {
    this.npResponse = q == null ? null : new FxQuantity(q);
  }

  /* package */ void setAerosolTEPct(Quantity q) {
    this.aerosolTEPct = q == null ? null : new FxQuantity(q);
  }

  /* package */ void setParticleNumberTEPct(Quantity q) {
    this.particleNumberTEPct = q == null ? null : new FxQuantity(q);
  }

}