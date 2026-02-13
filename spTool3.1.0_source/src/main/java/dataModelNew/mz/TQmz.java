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

public class TQmz implements MZValue, Serializable {


  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final NF MZ_FORMAT = NF.D1C0;

  private final double q1MZ;
  private final double q3MZ;
  private final Isotope isotope;

  public TQmz(double q1MZ, double q3MZ, String element) {
    this.q3MZ = q3MZ;
    MZValue mz1 = FileInterpreterUtils.safelyGetMZ1(element, q1MZ).getValue();

    this.q1MZ = mz1.getMZ();
    this.isotope = mz1.getIsotope();
  }


  public TQmz(String q1ElementAndMZ, double q3MZ) {
    this.q3MZ = q3MZ;
    MZValue mz1 = FileInterpreterUtils.safelyGetMZ1(q1ElementAndMZ).getValue();

    this.q1MZ = mz1.getMZ();
    this.isotope = mz1.getIsotope();
  }

  // Copy
  public TQmz(double q1MZ, double q3MZ, Isotope isotope) {
    this.q1MZ = q1MZ;
    this.q3MZ = q3MZ;
    this.isotope = isotope;
  }

  @Override
  public MZValue copy() {
    return new TQmz(q1MZ, q3MZ, isotope);
  }

  @Override
  public double getMZ() {
    return q3MZ;
  }

  public boolean isEqual(MZValue mzValue) {
    if (mzValue != null && isotope != null && mzValue.hasIsotope()) {
      // do not check string&mzValue: the only reason to use the string is to harness existing m/z picking
      return isotope.equals(mzValue.getIsotope());
    } else if (mzValue != null) {
      return DoubleMath.fuzzyEquals(mzValue.getMZ(), q3MZ, MZValue.EPSILON);
    } else {
      return false;
    }
  }

  @Override
  public String getTransition() {
    return SnF.doubleToString(q1MZ, MZ_FORMAT)
        + "->" +
        SnF.doubleToString(q3MZ, MZ_FORMAT);
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
      return SnF.doubleToString(q1MZ, MZ_FORMAT)
          + "-" +
          SnF.doubleToString(q3MZ, MZ_FORMAT)
          + "-" + getIsotope().getName();
    } else {
      return SnF.doubleToString(q1MZ, MZ_FORMAT)
          + "-" +
          SnF.doubleToString(q3MZ, MZ_FORMAT);
    }
  }

  @Override
  public boolean necessaryCheck() {
    return q1MZ > 0 & q3MZ > 0 && q1MZ < q3MZ;
  }

  @Override
  public boolean hasIsotope() {
    return isotope.getElement() != Element.UNKNOWN;
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
