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

package dataModelNew.mz;

import java.io.Serial;
import java.io.Serializable;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;

public class IsotopeMZ implements MZValue, Serializable {

  // TODO: Create instance that considers charge state, e.g., Eu2+

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final NF MZ_FORMAT = NF.D1C3;
  private final Isotope isotope;

  public IsotopeMZ(Isotope isotope) {
    this.isotope = isotope;
  }

  @Override
  public double getMZ() {
    return isotope.getTheoreticalMass();
  }

  @Override
  public boolean isEqual(MZValue mz) {
    return isotope.equals(mz.getIsotope());
  }

  @Override
  public String getTransition() {
    return SnF.doubleToString(isotope.getTheoreticalMass(), MZ_FORMAT);
  }

  @Override
  public String getElementTransition() {
    return getIsotope().getName() + "(" + getTransition() + ")";
  }

  @Override
  public String getTypesafeElementTransition() {
    return getTransition() + "-" + getIsotope().getName();
  }

  @Override
  public boolean necessaryCheck() {
    return isotope != null;
  }

  @Override
  public boolean hasIsotope() {
    return isotope != null;
  }

  @Override
  public Isotope getIsotope() {
    return isotope;
  }

  @Override
  public MZValue copy() {
    return new IsotopeMZ(isotope);
  }
}
