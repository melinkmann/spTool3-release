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
import io.FileInterpreterUtils;
import java.io.Serial;
import java.io.Serializable;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;

public class SQmz implements MZValue, Serializable {


  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final NF MZ_FORMAT = NF.D1C0;

  private final double mz;
  private final Isotope isotope;

  public SQmz() {
    this(1, "H");
  }

  public SQmz(Isotope isotope) {
    this.mz = isotope.getIsotopicNumber();
    this.isotope = isotope;
  }

  public SQmz(String letterAndNumber) {
    this(FileInterpreterUtils.safelyGetMZ1(letterAndNumber).getValue().getIsotope());
  }

  public SQmz(double mz, String element) {
    this(FileInterpreterUtils.safelyGetMZ1(element, mz).getValue().getIsotope());
  }

  // Copy
  public SQmz(double mz, Isotope isotope) {
    this.mz = mz;
    this.isotope = isotope;
  }

  @Override
  public MZValue copy() {
    return new SQmz(mz, isotope);
  }

  @Override
  public double getMZ() {
    return mz;
  }

  public boolean isEqual(MZValue mzValue) {
    if (mzValue != null && isotope != null && mzValue.hasIsotope()) {
      // do not check string&mzValue: the only reason to use the string is to harness existing m/z picking
      return isotope.equals(mzValue.getIsotope());
    } else if (mzValue != null) {
      return DoubleMath.fuzzyEquals(mzValue.getMZ(), mz, MZValue.EPSILON);
    } else {
      return false;
    }
  }

  @Override
  public String getTransition() {
    return SnF.doubleToString(mz, MZ_FORMAT);
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
    return mz > 0;
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
