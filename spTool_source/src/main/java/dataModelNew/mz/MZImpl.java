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

import util.NF;
import util.SnF;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class MZImpl implements MZ, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final double mz;
  private final int charge;
  private final NF stringPrecision;

  // Dummy

  public MZImpl() {
    this(0);
  }

  public MZImpl(double mz) {
    this.mz = mz;
    this.charge = 1;
    this.stringPrecision = NF.D1C0;
  }


  public MZImpl(double mz, int charge) {
    this.mz = mz;
    this.charge = charge;
    this.stringPrecision = NF.D1C0;
  }

  public MZImpl(double mz, NF stringPrecision) {
    this.mz = mz;
    this.charge = 1;
    this.stringPrecision = stringPrecision;
  }

  // Copy constructor
  public MZImpl(double mz, int charge, NF stringPrecision) {
    this.mz = mz;
    this.charge = charge;
    this.stringPrecision = stringPrecision;
  }

  @Override
  public double getMz() {
    return mz;
  }

  @Override
  public int getCharge() {
    return charge;
  }

  @Override
  public MZImpl copy() {
    return new MZImpl(mz, charge, stringPrecision);
  }

  @Override
  public String getUIString() {
    return SnF.doubleToString(mz, stringPrecision);
  }

  @Override
  public String getUniqueXmlString() {
    return Channel.wrap(SnF.doubleToString(mz, stringPrecision) + "(" + translateCharge() + ")");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MZImpl ms1 = (MZImpl) o;
    return Double.compare(ms1.mz, mz) == 0
        && ms1.charge == charge;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mz, charge);
  }
}
