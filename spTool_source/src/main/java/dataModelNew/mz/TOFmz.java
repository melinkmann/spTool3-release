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

import com.google.common.math.DoubleMath;
import java.io.Serial;
import java.io.Serializable;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;

public class TOFmz implements MZValue, Serializable {


  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final NF MZ_FORMAT = NF.D1C3;

  private final double theoreticalMass;
  private final double nominalMass;
  private final Isotope isotope;

  public TOFmz(double nominalMass, double theoreticalMass, String isotope) {
    this.nominalMass = nominalMass;
    this.theoreticalMass = theoreticalMass;
    this.isotope = Isotope.getFromString(isotope);
  }

  public TOFmz(Isotope isotope) {
    this.isotope = isotope;
    this.nominalMass = isotope.getIsotopicNumber();
    this.theoreticalMass = isotope.getTheoreticalMass();
  }

  // Copy
  public TOFmz(double theoreticalMass, double nominalMass, Isotope isotope) {
    this.theoreticalMass = theoreticalMass;
    this.nominalMass = nominalMass;
    this.isotope = isotope;
  }

  @Override
  public MZValue copy() {
    return new TOFmz(theoreticalMass, nominalMass, isotope);
  }

  @Override
  public double getMZ() {
    return theoreticalMass;
  }

  @Override
  public boolean isEqual(MZValue mzValue) {
    if (mzValue != null && isotope != null && mzValue.hasIsotope()) {
      // do not check string&mzValue: the only reason to use the string is to harness existing m/z picking
      return isotope.equals(mzValue.getIsotope());
    } else if (mzValue != null) {
      return DoubleMath.fuzzyEquals(mzValue.getMZ(), theoreticalMass, MZValue.EPSILON);
    } else {
      return false;
    }
  }

  @Override
  public String getTransition() {
    return SnF.doubleToString(theoreticalMass, MZ_FORMAT);
  }

  @Override
  public String getElementTransition() {
    if (hasIsotope()) {
      return getIsotope().getName() + "(" + getTransition() + ")";
    } else {
      return getTransition();
    }
  }

  @Override
  public String getTypesafeElementTransition() {
    if (hasIsotope()) {
      return getTransition() + "-" + getIsotope().getName();
    } else {
      return getTransition();
    }
  }


  @Override
  public boolean necessaryCheck() {
    return theoreticalMass > 0 && nominalMass >= 0;
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
  public String toString() {
    return getTransition();
  }

}
